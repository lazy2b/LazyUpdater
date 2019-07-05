package com.lazylibs.updater.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * Created by lazy2b on 18/6/12.
 */

public class VersionUpdateUtils {

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private static final String TAG = "VersionUpdateUtils";

    public static File writeResponseBodyToDisk(String path, ResponseBody body) {
        Log.d(TAG, "contentType:>>>>" + body.contentType().toString());
//        String type = body.contentType().toString();
//
//        if (type.equals(APK_CONTENTTYPE)) {
//            fileSuffix = ".apk";
//        } else if (type.equals(PNG_CONTENTTYPE)) {
//            fileSuffix = ".png";
//        }
        // 其他类型同上 自己判断加入.....
//        path += fileSuffix;

        Log.d(TAG, "path:>>>>" + path);

        try {
            File futureStudioIconFile = new File(path);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

//                long fileSize = body.contentLength();
//                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
//                    fileSizeDownloaded += read;
//                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }
                outputStream.flush();

                return futureStudioIconFile;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean checkAPKIsExists(Context context, String downloadPath) {
        File file = new File(downloadPath);
        boolean result = false;
        if (file.exists()) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo info = pm.getPackageArchiveInfo(downloadPath,
                        PackageManager.GET_ACTIVITIES);
                //判断安装包存在并且包名一样并且版本号不一样
                Log.e("HFL-upd", "ApkFile version：" + info.versionCode + "\n Current App version：" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
                if (context.getPackageName().equalsIgnoreCase(info.packageName) && context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode != info.versionCode) {
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;

    }

    private static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取 App 版本号
     *
     * @param context context
     * @return App 版本号
     */
    public static String getAppVersionName(Context context) {
        return getAppVersionName(context, context.getPackageName());
    }

    /**
     * 获取 App 版本号
     *
     * @param context     context
     * @param packageName 包名
     * @return App 版本号
     */
    public static String getAppVersionName(Context context, final String packageName) {
        if (isSpace(packageName)) return null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            return pi == null ? null : pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断是否是wifi连接
     */
    @SuppressLint("MissingPermission")
    public static boolean isWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null || cm.getActiveNetworkInfo() == null)
            return false;
        else
            return cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;

    }

    /**
     * 安装 App（支持 8.0）
     * <p>8.0 需添加权限
     * {@code <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />}</p>
     *
     * @param context   context
     * @param file      文件
     * @param authority 7.0 及以上安装需要传入清单文件中的{@code <provider>}的 authorities 属性
     *                  <br>参看 https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     */
    public static void installApp(Context context, File file, String authority) {
        if (file == null || !file.exists() || !file.isFile()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri apkUri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            apkUri = Uri.fromFile(file);
        } else {
            apkUri = FileProvider.getUriForFile(context, authority, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static String apkDir(@NonNull Context context) {
        return context.getCacheDir().getAbsolutePath() + "/";//apkDir + "/apk/";
    }

    public static String apkFile(String newVersionName) {
        return "v" + newVersionName + ".apk";
    }

    public static void chmod(String file) {
        String[] command = {"chmod", "777", file};
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cleanApk(String apkDir) {
        File dir = new File(apkDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".apk")) {
                        file.delete();
                    }
                }
            }
        } else {
            dir.mkdir();
        }
        if (dir.exists() && (!dir.canWrite() || !dir.canRead())) {
            chmod(dir.getAbsolutePath());
        }
    }
}
