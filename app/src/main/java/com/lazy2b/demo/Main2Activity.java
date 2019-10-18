package com.lazy2b.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;


public class Main2Activity extends Activity {
    void print() {
        new Thread(new Runnable() {
            String Tag = "FileChooser";

            void printFile(File file) {
                if (file != null && file.exists()) {
                    Log.e(Tag,
                            "\nr:" + file.canRead() + "|w:" + file.canWrite()
                                    + "\n" + file.toString() + "\n" +
                                    FileProvider.getUriForFile(Main2Activity.this,
                                            getPackageName() + ".provider",
                                            file).toString()
                    );
//                    File[] childs = file.listFiles();
//                    if (childs != null && childs.length > 0) {
//                        Log.e(Tag, "childs:");
//                        for (File child : childs) {
//                            printFile(child);
//                        }
//                    }
                }
            }

            @Override
            public void run() {
                printFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
                printFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//                printFile(Environment.getRootDirectory());
//                printFile(Environment.getDownloadCacheDirectory());
//                printFile(Environment.getDataDirectory());
//                printFile(Environment.getExternalStorageDirectory());
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        print();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        WebView webView = findViewById(R.id.webview);
        webView.loadUrl("file:///android_asset/index.html");
        webView.setWebChromeClient(new WebChromeClient() {
            // For Android  >= 5.0
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                uploadFiles = filePathCallback;
                Log.i("test", "openFileChooser 4:" + filePathCallback.toString());
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                if (fileChooserParams.getAcceptTypes() != null && fileChooserParams.getAcceptTypes().length > 0) {
                    i.setType(TextUtils.isEmpty(fileChooserParams.getAcceptTypes()[0]) ? "*/*" : fileChooserParams.getAcceptTypes()[0]);
                } else {
                    i.setType("*/*");
                }
                startActivityForResult(Intent.createChooser(i, "test"), 10086);
                return true;
            }
        });
    }

    protected ValueCallback<Uri[]> uploadFiles;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10086) {
            if (resultCode == Activity.RESULT_OK) {
                if (null != uploadFiles) {
                    Uri result = data == null ? null
                            : data.getData();
                    uploadFiles.onReceiveValue(new Uri[]{result});
                    uploadFiles = null;
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (null != uploadFiles) {
                    uploadFiles.onReceiveValue(null);
                    uploadFiles = null;
                }

            }
        }
    }
}
