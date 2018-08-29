package com.lazylibs.updater;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.lazylibs.updater.interfaces.IUpgradeModel;
import com.lazylibs.updater.model.DownloadProgress;
import com.lazylibs.updater.model.DownloadResponseBody;
import com.lazylibs.updater.model.UpdateResult;
import com.lazylibs.updater.utils.VersionUpdateUtils;
import com.lazylibs.updater.view.DownloadProgressDialogFragment;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class VersionUpdateHelper implements ServiceConnection {//}, IUpgradeModel.IUpgradeModelListener {
    //    public final static String FORCE_EXIT_APP_ACTION = "com.caimao.update.v1.ACTION.force.exitApp";
    private Context mContext;
    private AlertDialog mNewVersionDialog;
    private IUpgradeModel mVersionModel;
    private VersionUpdateService mUpdateService;
    //    private Single<IUpgradeModel> mRequestVersionSingle;
    private VersionHelperListener mHelperCallBack;
    private DownloadProgressDialogFragment mDownloadProgressDialogFragment;
//    public static final int VERSION_UPDATE_REQUEST_PERMISSION = 13558;

    private boolean isInitialization = false;
    private boolean isToast = false;

    /**
     * 下载Apk时的回调
     */
    public interface DownloadApkListener extends DownloadResponseBody.DownloadProgressListener {
        void onStart();

        void onSuccess(File apk);

        void onError(Throwable throwable);
    }

    /**
     * 版本更新工具类回调
     */
    public interface VersionHelperListener {
        /**
         * 下载服务绑定状态改变回调
         *
         * @param state onServiceConnected=1，onServiceDisconnected=0
         */
        void sConnectState(int state);

        /**
         * 版本更新工具各种状态回调
         *
         * @param result @see {@link UpdateResult}
         */
        void vHelperCallBack(UpdateResult result);
    }

//    public static VersionUpdateHelper create(@NonNull Activity context, @NonNull VersionHelperListener callback, Single<IUpgradeModel> single) {
//        return new VersionUpdateHelper(context, callback, single).bindService();
//    }

    public static VersionUpdateHelper create(@NonNull Activity context, @NonNull VersionHelperListener callback, IUpgradeModel versionModel) {
        return new VersionUpdateHelper(context, callback, versionModel).bindService();
    }

    public static VersionUpdateHelper create(@NonNull Activity context, @NonNull VersionHelperListener callback) {
        return new VersionUpdateHelper(context, callback).bindService();
    }

    public static void destroy(VersionUpdateHelper helper) {
        if (helper != null) {
            helper.vCallBack(UpdateResult.Destroy);
            if (helper.unbindService()) {
                helper = null;
            }
        }
    }

    private static void clear(VersionUpdateHelper helper, UpdateResult result) {
        if (helper != null) {
            helper.vCallBack(result);
            if (helper.unbindService()) {
                helper = null;
            }
        }
    }

    private void vCallBack(UpdateResult result) {
        if (mHelperCallBack != null) {
            mHelperCallBack.vHelperCallBack(result);
        }
    }

    /**
     * 是否吐司更新消息
     *
     * @param toast
     */
    public void setToast(boolean toast) {
        this.isToast = toast;
    }

    public void doHasVersionModel(IUpgradeModel vModel) {
        if (vModel == null || isWaitForUpdate() || isWaitForDownload() || !isInitialization) {
            return;
        }
        mVersionModel = vModel;
        mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
        if (!mVersionModel.isNeedUpgrade()) {
//            if (isToast) {
//                Toast.makeText(mContext, "暂无新版本", Toast.LENGTH_SHORT).show();
//            }
            clear(VersionUpdateHelper.this, UpdateResult.Success);
//            unbindService();
            return;
        }
        if (mContext == null) return;
        updateNow();
//        mNewVersionDialog = getUpgradeAlertDialog();
//        mNewVersionDialog.show();
    }

    private void doHasVersionModel() {
        if (mVersionModel == null) return;
        if (!mVersionModel.isNeedUpgrade()) {
            if (isToast) {
                Toast.makeText(mContext, R.string.updater_no_new_version, Toast.LENGTH_SHORT).show();
            }
            clear(VersionUpdateHelper.this, UpdateResult.Success);
//            unbindService();
            return;
        }
        if (mContext == null) return;
        mNewVersionDialog = getUpgradeAlertDialog();
        mNewVersionDialog.show();
    }

//    private VersionUpdateHelper(@NonNull Activity context, @NonNull VersionHelperListener callback, Single<IUpgradeModel> single) {//}, DisposableSingleObserver<IUpgradeModel> observer) {//}, OnPermissionsListener listener) {
//        this(context, callback);
//        this.mRequestVersionSingle = single;
//    }

    private VersionUpdateHelper(@NonNull Activity context, @NonNull VersionHelperListener callback, IUpgradeModel versionModel) {//}, OnPermissionsListener listener) {
        this(context, callback);
        this.mVersionModel = versionModel;
    }

    private VersionUpdateHelper(@NonNull Activity context, @NonNull VersionHelperListener callback) {
        this.mContext = context;
        this.mHelperCallBack = callback;
    }

    private boolean isWaitForUpdate() {
        return mNewVersionDialog != null && mNewVersionDialog.isShowing();
    }

    private boolean isWaitForDownload() {
        return (mUpdateService != null && mUpdateService.isDownloading());// || mDownloadProgressDialogFragment != null && (mDownloadProgressDialogFragment.isVisible());
    }

    private void doDownloadApkSuccess(@NonNull final File apk) {
        if (!TextUtils.isEmpty(apk.getAbsolutePath())) {
            VersionUpdateUtils.chmod(apk.getAbsolutePath());
            new Timer()
                    .schedule(new TimerTask() {
                        @Override
                        public void run() {
                            VersionUpdateUtils.installApp(mContext.getApplicationContext(), apk, mContext.getPackageName() + ".provider");
                            clear(VersionUpdateHelper.this, UpdateResult.Success);
                        }
                    }, 10);
//            Flowable
//                    .zip(
//                            Flowable.just(apk.getAbsolutePath())
//                                    .doOnNext(VersionUpdateUtils::chmod),
//                            Flowable.just(VersionUpdateUtils.apkDir(mContext))
//                                    .doOnNext(VersionUpdateUtils::chmod),
//                            Flowable.just(apk)
//                                    .delay(100L, TimeUnit.MICROSECONDS),
//                            (apkPath, apkDir, apkFile) -> {//安装apk
//                                VersionUpdateUtils.installApp(mContext.getApplicationContext(), apkFile, mContext.getPackageName() + ".provider");
//                                return true;
//                            })
//                    .singleOrError()
//                    .subscribeWith(new DisposableSingleObserver<Boolean>() {
//                        @Override
//                        public void onSuccess(Boolean aBoolean) {
//                            clear(VersionUpdateHelper.this, UpdateResult.Success);
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            clear(VersionUpdateHelper.this, UpdateResult.Success);
//                            e.printStackTrace();
//                        }
//                    });

            //安装apk
//            VersionUpdateUtils.installApp(mContext.getApplicationContext(), apk, mContext.getPackageName() + ".provider");
        }
//        clear(VersionUpdateHelper.this, UpdateResult.Success);
//        unbindService();
    }

    private void showNotWifiDownloadDialog() {
        AlertDialog.Builder builer = new AlertDialog.Builder(mContext);
        builer.setTitle(R.string.updater_found_new_version);
        builer.setMessage(R.string.updater_unwifi_tips);
        builer.setNegativeButton(R.string.updater_download_later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //exit app
                dialog.cancel();
                clear(VersionUpdateHelper.this, UpdateResult.Cancel);
            }
        });
        builer.setPositiveButton(R.string.updater_continue_download, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                doDownLoadTask();
            }
        });
        builer.setCancelable(false);
        builer.show();
    }

    private void updateNow() {
        if (VersionUpdateUtils.isWifi(mContext)) {
            doDownLoadTask();
        } else {
            showNotWifiDownloadDialog();
        }
    }

    private AlertDialog getUpgradeAlertDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.updater_version_upgrade);
        builder.setMessage(mVersionModel.getUpdateInfo());
        //当点确定按钮时从服务器上下载新的apk 然后安装
        builder.setPositiveButton(R.string.updater_update_now,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        updateNow();
                    }
                }
        );
        if (!mVersionModel.isForceUpdate()) {
            builder.setNegativeButton(R.string.updater_dont_update,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.cancel();
                            clear(VersionUpdateHelper.this, UpdateResult.Cancel);
                        }
                    }
            );
        }
        builder.setCancelable(false);
        return builder.create();
    }

//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == VersionUpdateHelper.VERSION_UPDATE_REQUEST_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                doDownLoadTask();
//            } else {
//                Toast.makeText(mContext, "应用更新需要读写文件权限！", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

//    public static boolean checkSelfPermission(Activity context) {
//        //申请权限
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            return true;
//        }
//        ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, VERSION_UPDATE_REQUEST_PERMISSION);
//        return false;
//    }

    private void doDownLoadTask() {
//        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
//            if (mHelperCallBack == null || !mHelperCallBack.checkSelfPermission((Activity) mContext)) {
//                return;
//            }
//        }
        String apkDir = VersionUpdateUtils.apkDir(mContext);
        String apkName = apkDir + VersionUpdateUtils.apkFile(mVersionModel.getNewVersionName());
        File apkFile = new File(apkName);
        if (VersionUpdateUtils.checkAPKIsExists(mContext, apkName)) {
            doDownloadApkSuccess(apkFile);
        } else {
            VersionUpdateUtils.cleanApk(apkDir);
            mUpdateService.downloadApk(
                    mVersionModel.getDownloadUrl(),
                    apkName,
                    new DownloadApkListener() {
                        @Override
                        public void onStart() {
                            if (mContext != null && mContext instanceof FragmentActivity) {
                                if (mDownloadProgressDialogFragment == null) {
                                    mDownloadProgressDialogFragment = DownloadProgressDialogFragment.newInstance(mVersionModel.isForceUpdate());
                                }
                                mDownloadProgressDialogFragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), "loading");
                            }
                        }

                        @Override
                        public void onSuccess(File apk) {
                            if (mDownloadProgressDialogFragment != null)
                                mDownloadProgressDialogFragment.dismissAllowingStateLoss();
                            doDownloadApkSuccess(apk);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (mDownloadProgressDialogFragment != null)
                                mDownloadProgressDialogFragment.dismissAllowingStateLoss();
                            clear(VersionUpdateHelper.this, UpdateResult.Error);
//                            unbindService();
                        }

                        @Override
                        public void updateProgress(DownloadProgress progress) {
                            if (mDownloadProgressDialogFragment != null) {
                                mDownloadProgressDialogFragment.updateProgress(progress);
                            }
                        }
                    }
            );
        }
    }

    private VersionUpdateHelper bindService() {
        if (isWaitForUpdate() || isWaitForDownload()) {
            return this;
        }
        if (mUpdateService == null && mContext != null) {
            mContext.bindService(new Intent(mContext, VersionUpdateService.class), this, Context.BIND_AUTO_CREATE);
        }
        return this;
    }

    private boolean unbindService() {
        if (isWaitForUpdate() || isWaitForDownload()) {
            return false;
        }
        if (!isInitialization) {
            return false;
        }
        if (mUpdateService != null && !mUpdateService.isDownloading()) {
            mContext.unbindService(this);
            isInitialization = false;
        }

        if (mVersionModel != null && mVersionModel.isNeedUpgrade() && mVersionModel.isForceUpdate()) {
            android.os.Process.killProcess(android.os.Process.myPid());
//            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(FORCE_EXIT_APP_ACTION));
        }
        return true;
    }

//    public void onSuccess(IUpgradeModel iUpgradeModel) {
//        mVersionModel = iUpgradeModel;
//        mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
//        doHasVersionModel();
//    }
//
//    public void onError(Throwable throwable) {
//        throwable.printStackTrace();
//        if (isToast) {
//            VersionUpdateUtils.showToast(mContext, "数据连接错误，请检查网络");
//        }
//        clear(VersionUpdateHelper.this, UpdateResult.Error);
////                            unbindService();
//    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        mUpdateService = ((VersionUpdateService.VersionUpdateBinder) binder).getService();
        isInitialization = true;
        if (mVersionModel != null) {
            mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
            doHasVersionModel();
        }
//        else if (mRequestVersionSingle != null) {
//            mRequestVersionSingle
//                    .subscribeWith(new DisposableSingleObserver<IUpgradeModel>() {
//                        @Override
//                        public void onSuccess(IUpgradeModel iUpgradeModel) {
//                            mVersionModel = iUpgradeModel;
//                            mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
//                            doHasVersionModel();
//                        }
//
//                        @Override
//                        public void onError(Throwable throwable) {
//                            throwable.printStackTrace();
//                            if (isToast) {
//                                VersionUpdateUtils.showToast(mContext, "数据连接错误，请检查网络");
//                            }
//                            clear(VersionUpdateHelper.this, UpdateResult.Error);
////                            unbindService();
//                        }
//                    });
//        }
        if (mHelperCallBack != null) {
            mHelperCallBack.sConnectState(1);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (mDownloadProgressDialogFragment != null) {
            mDownloadProgressDialogFragment.dismissAllowingStateLoss();
        }
        if (mNewVersionDialog != null && mNewVersionDialog.isShowing()) {
            mNewVersionDialog.cancel();
        }
        mDownloadProgressDialogFragment = null;
        mNewVersionDialog = null;
        mUpdateService = null;
        mVersionModel = null;
        mContext = null;
        if (mHelperCallBack != null) {
            mHelperCallBack.sConnectState(0);
        }
        mHelperCallBack = null;
    }
}
