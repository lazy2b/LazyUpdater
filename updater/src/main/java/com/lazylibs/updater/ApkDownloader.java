package com.lazylibs.updater;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

public class ApkDownloader extends BroadcastReceiver {

    public static class Helper {
        private static ApkDownloader apkDownloader;

        public static void start(Context context, String apkUrl) {
            start(context, apkUrl, false);
        }

        public static void start(Context context, String apkUrl, boolean onlyWifi) {
            start(context, apkUrl, onlyWifi, false);
        }

        public static void start(Context context, String apkUrl, boolean onlyWifi, boolean forceReNew) {
            if (forceReNew) {
                onDestroy(context);
            }
            if (apkDownloader != null) {
                return;
            }
            if (apkUrl == null || apkUrl.trim().equals("") || !apkUrl.endsWith(".apk")) return;
            apkDownloader = new ApkDownloader(Helper::onDestroy);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            context.getApplicationContext().registerReceiver(apkDownloader, intentFilter);
            apkDownloader.start(context, apkUrl, onlyWifi);
        }

        public static void onDestroy(Context context) {
            if (apkDownloader != null) {
                context.getApplicationContext().unregisterReceiver(apkDownloader);
                apkDownloader = null;
            }
        }
    }

    public interface OnDownloadSuccessor {
        void onDownloadSucceed(Context context);
    }

    public ApkDownloader() {

    }

    public ApkDownloader(OnDownloadSuccessor onDownloadSuccessor) {
        this.onDownloadSuccessor = onDownloadSuccessor;
    }

    long downloadIdUpdate = 0L;

    OnDownloadSuccessor onDownloadSuccessor;

    String getAppName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationContext().getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getApplicationContext().getString(stringId);
        return TextUtils.isEmpty(appName) ? "" : appName;
    }

    public void start(Context context, String apkUrl, boolean onlyWifi) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(getAppName(context));
//        request.setDescription("downloading...");
        request.setAllowedOverMetered(!onlyWifi);
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, context.getPackageName() + "_new.apk");
        downloadIdUpdate = ((DownloadManager) context.getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == downloadIdUpdate) {
                    DownloadManager downloadManager = (DownloadManager) context.getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                    if (uri != null) {
                        installApp(context, downloadManager.getMimeTypeForDownloadedFile(downloadId), uri);
                    }
                    if (onDownloadSuccessor != null) {
                        onDownloadSuccessor.onDownloadSucceed(context);
                    }
                }
            }
        }
    }

    public static void installApp(Context context, String type, Uri apkUri) {
        if (TextUtils.isEmpty(type)) {
            type = "application/vnd.android.package-archive";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(apkUri, type);
        context.getApplicationContext().startActivity(intent);
    }
}
