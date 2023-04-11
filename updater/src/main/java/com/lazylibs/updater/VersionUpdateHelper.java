package com.lazylibs.updater;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.lazylibs.updater.interfaces.DownloadWay;
import com.lazylibs.updater.interfaces.IUpgradeModel;
import com.lazylibs.updater.model.DownloadProgress;
import com.lazylibs.updater.model.DownloadResponseBody;
import com.lazylibs.updater.model.UpdateResult;
import com.lazylibs.updater.utils.VersionUpdateUtils;
import com.lazylibs.updater.view.DownloadProgressDialogFragment;

import java.io.File;

public class VersionUpdateHelper implements ServiceConnection, VersionUpdateService.OnDestroyListener {

    public static final String FILE_PROVIDER = ".update.provider";

    private Context mContext;
    private Handler mHandler;
    private AlertDialog mNewVersionDialog;
    private IUpgradeModel mVersionModel;
    private VersionUpdateService mUpdateService;
    private VersionHelperListener mHelperCallBack;
    private DownloadProgressDialogFragment mDownloadProgressDialogFragment;
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

        /**
         * 是否需要其他下载渠道
         * <br/>1 默认使用浏览器跳转并下载
         * <br/>2 可自定义，见：{@link #anotherWay(Context, IUpgradeModel)}
         *
         * @return
         */
        default boolean isAnotherWay() {
            return false;
        }

        /**
         * @param context
         * @param upgrade
         * @return 是否需要自动关闭服务
         */
        default boolean anotherWay(Context context, IUpgradeModel upgrade) {
            if (context == null || TextUtils.isEmpty(upgrade.getDownloadUrl())) return true;
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(upgrade.getDownloadUrl())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Toast.makeText(context, R.string.updater_no_browser, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            return true;
        }
    }

    public static VersionUpdateHelper create(@NonNull Activity context, @NonNull VersionHelperListener callback, IUpgradeModel versionModel) {
        return new VersionUpdateHelper(context, callback, versionModel).bindService();
    }

    public static VersionUpdateHelper create(@NonNull Activity context, @NonNull VersionHelperListener callback) {
        return new VersionUpdateHelper(context, callback).bindService();
    }

    public static void destroy(VersionUpdateHelper helper) {
        clear(helper, UpdateResult.Destroy);
    }

    public static void clear(VersionUpdateHelper helper, UpdateResult result) {
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

    public void doUpdateNow(IUpgradeModel vModel, @DownloadWay int... downloadWay) {
        if (vModel == null || isWaitForUpdate() || isWaitForDownload() || !isInitialization) {
            return;
        }
        mVersionModel = vModel;
        mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
        if (!mVersionModel.isNeedUpgrade()) {
            clear(VersionUpdateHelper.this, UpdateResult.NoNews);
            return;
        }
        if (mContext == null) return;
        if (downloadWay != null && downloadWay.length > 0) {
            this.downloadWay = downloadWay[0];
        }
        updateNow();
    }

    public void doHasVersionModel(IUpgradeModel vModel, @DownloadWay int... downloadWay) {
        if (vModel == null || isWaitForUpdate() || isWaitForDownload() || !isInitialization) {
            return;
        }
        mVersionModel = vModel;
        mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
        if (downloadWay != null && downloadWay.length > 0) {
            this.downloadWay = downloadWay[0];
        }
        doHasVersionModel();
    }

    private void doHasVersionModel() {
        if (mVersionModel == null) return;
        if (!mVersionModel.isNeedUpgrade()) {
            if (isToast) {
                Toast.makeText(mContext, R.string.updater_no_new_version, Toast.LENGTH_SHORT).show();
            }
            clear(VersionUpdateHelper.this, UpdateResult.NoNews);
            return;
        }

        if (TextUtils.isEmpty(mVersionModel.getDownloadUrl())) {
            if (isToast) {
                Toast.makeText(mContext, R.string.updater_no_new_version, Toast.LENGTH_SHORT).show();
            }
            clear(VersionUpdateHelper.this, UpdateResult.NoNews);
            return;
        }

        if (mContext == null) return;
        mNewVersionDialog = getUpgradeAlertDialog();
        mNewVersionDialog.show();
    }

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
        return (mUpdateService != null && mUpdateService.isDownloading());
    }

    void postDelayed(Runnable runnable, long delay) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        mHandler.postDelayed(runnable, delay);
    }

    private void doDownloadApkSuccess(@NonNull final File apk) {
        if (!TextUtils.isEmpty(apk.getAbsolutePath())) {
            VersionUpdateUtils.chmod(apk.getAbsolutePath());
            postDelayed(() -> {
                VersionUpdateUtils.installApp(mContext.getApplicationContext(), apk, mContext.getPackageName() + FILE_PROVIDER);
                postDelayed(() -> clear(VersionUpdateHelper.this, UpdateResult.Success), 10);
            }, 10);
        }
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

    @DownloadWay
    private int downloadWay = DownloadWay.defaultWay;

    private AlertDialog getUpgradeAlertDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.updater_version_upgrade);
        builder.setMessage(mVersionModel.getUpdateInfo());
        //当点确定按钮时从服务器上下载新的apk 然后安装
        if (mHelperCallBack != null && mHelperCallBack.isAnotherWay()) {
            builder.setPositiveButton(R.string.updater_another_way, (dialog, which) -> {
                dialog.cancel();
                downloadWay = DownloadWay.anotherWay;
                updateNow();
            });
            builder.setNegativeButton(R.string.updater_update_now, (dialog, which) -> {
                dialog.cancel();
                downloadWay = DownloadWay.defaultWay;
                updateNow();
            });
        } else {
            builder.setPositiveButton(R.string.updater_update_now, (dialog, which) -> {
                dialog.cancel();
                downloadWay = DownloadWay.defaultWay;
                updateNow();
            });
        }
        if (!mVersionModel.isForceUpdate()) {
            builder.setNeutralButton(R.string.updater_dont_update, (dialog, which) -> {
                dialog.cancel();
                downloadWay = DownloadWay.cancel;
                clear(VersionUpdateHelper.this, UpdateResult.Cancel);
            });
        }
        builder.setCancelable(false);
        return builder.create();
    }

    private void doDownLoadTask() {
        if (downloadWay == DownloadWay.anotherWay) {
            if (mHelperCallBack != null) {
                if (mHelperCallBack.anotherWay(mContext, mVersionModel)) {
                    postDelayed(() -> clear(this, UpdateResult.Success), 5);
                }
            }
        } else {
            String apkDir = VersionUpdateUtils.apkDir(mContext);
            String apkName = apkDir + VersionUpdateUtils.apkFile(mVersionModel.getNewVersionName());
            File apkFile = new File(apkName);
            if (VersionUpdateUtils.checkAPKIsExists(mContext, apkName)) {
                doDownloadApkSuccess(apkFile);
            } else {
                VersionUpdateUtils.cleanApk(apkDir);
                mUpdateService.downloadApk(mVersionModel.getDownloadUrl(), apkName, new DownloadApkListener() {
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
                    }

                    @Override
                    public void updateProgress(DownloadProgress progress) {
                        if (mDownloadProgressDialogFragment != null) {
                            mDownloadProgressDialogFragment.updateProgress(progress);
                        }
                    }
                });
            }
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

    @Override
    public void onDestroy() {
        if (mDownloadProgressDialogFragment != null) {
            mDownloadProgressDialogFragment.dismissAllowingStateLoss();
        }
        if (mNewVersionDialog != null && mNewVersionDialog.isShowing()) {
            mNewVersionDialog.cancel();
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(null);
            mHandler = null;
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
        }
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        mUpdateService = ((VersionUpdateService.VersionUpdateBinder) binder).getService();
        if (mUpdateService != null) {
            mUpdateService.setOnDestroyListener(this);
        }
        isInitialization = true;
        if (mVersionModel != null) {
            mVersionModel.setNeedUpgrade(VersionUpdateUtils.getAppVersionName(mUpdateService));
            doHasVersionModel();
        }
        if (mHelperCallBack != null) {
            mHelperCallBack.sConnectState(1);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        onDestroy();
    }
}
