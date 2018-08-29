package com.lazylibs.updater;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.lazylibs.updater.model.DownloadProgress;
import com.lazylibs.updater.model.DownloadResponseBody;
import com.lazylibs.updater.utils.VersionUpdateUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VersionUpdateService extends Service {
    private static final int NOTIFICATION_ID = 100;
    private boolean mDownLoading = false;
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;

    public VersionUpdateService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotificationManager != null)
            mNotificationManager.cancelAll();
        mDownLoading = false;
    }

    public boolean isDownloading() {
        return mDownLoading;
    }

    void buildNotification() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getString(R.string.updater_downloading);
        // The PendingIntent to launch our activity if the user selects this notification
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);
        if (Build.VERSION.SDK_INT >= 26) {
            //ChannelId为"1",ChannelName为"Channel1"
            NotificationChannel channel = new NotificationChannel("1",
                    getString(R.string.updater_download), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableVibration(false);
            channel.enableLights(false);
//            channel.setSound();
            channel.enableLights(false); //是否在桌面icon右上角展示小红点
            channel.setVibrationPattern(null);
            channel.setSound(null, null);
//            channel.setLightColor(Color.GREEN); //小红点颜色
            channel.setShowBadge(false); //是否在久按桌面图标时显示此渠道的通知
            mNotificationManager.createNotificationChannel(channel);
            mNotificationBuilder = new Notification.Builder(this, channel.getId());
        } else {
            mNotificationBuilder = new Notification.Builder(this);
        }
        mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);  // the status icon
        mNotificationBuilder.setTicker(text);  // the status text
        mNotificationBuilder.setWhen(System.currentTimeMillis());  // the time stamp
        mNotificationBuilder.setContentText(text);  // the contents of the entry
//        mNotificationBuilder.setContentIntent(contentIntent);  // The intent to send when the entry is clicked
        mNotificationBuilder.setContentTitle(getString(R.string.updater_download_progress, 0) + "%"); // the label of the entry
        mNotificationBuilder.setProgress(100, 0, false);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setAutoCancel(true);
        mNotificationBuilder.setVibrate(null);
        mNotificationBuilder.setSound(null);
    }

    void updateNotification(int progress) {
        if (mNotificationBuilder != null) {
            mNotificationBuilder.setContentTitle(getString(R.string.updater_download_progress, progress) + "%"); // the label of the entry
            mNotificationBuilder.setProgress(100, progress, false);
            mNotificationBuilder.setVibrate(null);
            mNotificationBuilder.setSound(null);
            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
        }
    }

    void cancelNotification() {
        stopForeground(true);
    }

    @SuppressLint("StaticFieldLeak")
    @MainThread
    public void downloadApk(String url, String apk, @NonNull final VersionUpdateHelper.DownloadApkListener listener) {
        if (TextUtils.isEmpty(url)) return;
        buildNotification();
        startForeground(NOTIFICATION_ID, mNotificationBuilder.getNotification());
//        CLog.w(url);
        new AsyncTask<String, Integer, File>() {

            Throwable throwable;

            @Override
            protected void onPreExecute() {
                listener.onStart();
                mDownLoading = true;
            }

            @Override
            protected File doInBackground(String... args) {
                try {
                    // 过滤器，做下载进度
                    Interceptor interceptor =
                            new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Response originalResponse = chain.proceed(chain.request());
                                    return originalResponse.newBuilder()
                                            .body(
                                                    new DownloadResponseBody(
                                                            originalResponse.body(),
                                                            new DownloadResponseBody.DownloadProgressListener() {
                                                                @Override
                                                                public void updateProgress(@Nullable DownloadProgress progress) {
                                                                    if (progress == null)
                                                                        return;
                                                                    listener.updateProgress(progress);
                                                                    updateNotification(progress.getProgress());
                                                                }
                                                            }
                                                    )
                                            )
                                            .build();
                                }
                            };
                    OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(interceptor).build();
                    Request request = new Request.Builder().url(args[0]).get().build();
                    Response response = httpClient.newCall(request).execute();
                    return VersionUpdateUtils.writeResponseBodyToDisk(args[1], response.body());
                } catch (IOException e) {
                    e.printStackTrace();
                    throwable = e.getCause();
                }
                return null;
            }

            @Override
            protected void onPostExecute(File apk) {
                if (apk != null) {
                    listener.onSuccess(apk);
                    VersionUpdateUtils.showToast(getApplicationContext(), getString(R.string.updater_download_success));
                    mDownLoading = false;
                    cancelNotification();
                } else {
                    listener.onError(throwable == null ? new Throwable(getString(R.string.updater_download_file_is_empty)) : throwable);
                    VersionUpdateUtils.showToast(getApplicationContext(), getString(R.string.updater_download_fail));
                    mDownLoading = false;
                    cancelNotification();
                }
            }
        }.execute(url, apk);


//        VersionUpdateUtils.newDefaultRetrofitBuilderByNoConverter()
//                .client(new OkHttpClient.Builder()
//                        .addInterceptor(chain -> {
//                            Response originalResponse = chain.proceed(chain.request());
//                            return originalResponse.newBuilder()
//                                    .body(new DownloadResponseBody(originalResponse.body(), progress -> {
//                                        if (progress == null) return;
//                                        if (listener != null) {
//                                            listener.updateProgress(progress);
//                                        }
//                                        updateNotification(progress.getProgress());
//                                    })).build();
//                        })
//                        .build()
//                )
//                .baseUrl("https://www.baidu.com")
//                .build()
//                .create(DownloadApkApi.class)
//                .downloadFileWithDynamicUrlSync(url)
//                .singleOrError()
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .map(responseBody -> VersionUpdateUtils.writeResponseBodyToDisk(apk, responseBody))
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeWith(new DisposableSingleObserver<File>() {
//                    @Override
//                    protected void onStart() {
//                        listener.onStart();
//                        mDownLoading = true;
//                    }
//
//                    @Override
//                    public void onSuccess(File file) {
//                        VersionUpdateUtils.showToast(getApplicationContext(), "下载成功！");
//                        listener.onSuccess(file);
//                        mDownLoading = false;
//                        cancelNotification();
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//                        throwable.printStackTrace();
//                        VersionUpdateUtils.showToast(getApplicationContext(), "下载失败！");
//                        listener.onError(throwable);
//                        mDownLoading = false;
//                        cancelNotification();
//                    }
//                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new VersionUpdateBinder();
    }

    class VersionUpdateBinder extends Binder {
        VersionUpdateService getService() {
            return VersionUpdateService.this;
        }
    }
}
