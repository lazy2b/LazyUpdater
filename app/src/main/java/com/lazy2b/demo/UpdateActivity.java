package com.lazy2b.demo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSON;
import com.lazylibs.updater.SimpleVersionHelperListener;
import com.lazylibs.updater.VersionUpdateHelper;
import com.lazylibs.updater.interfaces.DownloadWay;
import com.lazylibs.updater.interfaces.IUpgradeModel;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UpdateActivity extends AppCompatActivity {

    TextView tv_check;

    IUpgradeModel vModel;

    VersionUpdateHelper vHelper;
    void print() {
        new Thread(new Runnable() {
            String Tag = "FileChooser";

            void printFile(File file) {
                if (file != null && file.exists()) {
                    Log.e(Tag,
                            "\nr:" + file.canRead() + "|w:" + file.canWrite()
                                    + "\n" + file.toString() + "\n" +
                                    FileProvider.getUriForFile(UpdateActivity.this,
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
//                printFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
//                printFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
//                printFile(Environment.getRootDirectory());
//                printFile(Environment.getDownloadCacheDirectory());
//                printFile(Environment.getDataDirectory());
//                printFile(Environment.getExternalStorageDirectory());
            }
        }).start();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        print();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_check = findViewById(R.id.tv_check);
        doCheckVersion(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                tips(getString(R.string.get_info_fail));
                                VersionUpdateHelper.destroy(vHelper);
                            }
                        }
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if ((vModel = parseBody(response.body())) == null) {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    tips(getString(R.string.get_info_fail));
                                    VersionUpdateHelper.destroy(vHelper);
                                }
                            }
                    );
                } else {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    // TODO Direction 1
                                    vHelper = VersionUpdateHelper.create(UpdateActivity.this,
                                            new SimpleVersionHelperListener() {
                                                @Override
                                                public boolean isAnotherWay() {
                                                    return true;
                                                }
                                            }, vModel);
                                    tv_check.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            onSuccess();
                                        }
                                    });
                                }
                            }
                    );
                }
            }
        });

//        SparseIntArray sia = new SparseIntArray();
//
//        for (int i = 0; i < 10; i++) {
//            sia.put(i, i + 1);
//        }

//        System.out.println("json--->" + JSON.toJSONString(sia));
//        pingIpAddr();

//        new NetPing().execute();
//        LDNetPing ping = new LDNetPing(new LDNetPing.LDNetPingListener() {
//            @Override
//            public void OnNetPingFinished(String log) {
//                Log.w("json--->",  log);
//            }
//        }, 3);
//        ping.exec("lazy2b.com", false);
//        CommandUtil.execute(String.format("ping -c 10 -i 10 -w 1 %s", "14.215.177.39"));
//        new Thread(() -> ShellUtils.execCmd(String.format("ping -c 10 -w 1000 %s", "14.215.177.39"), resp -> {
//            runOnUiThread(() -> Log.w("json--->", resp));
//        })).start();
//        Log.w("json--->", .successMsg);

    }

    IUpgradeModel parseBody(ResponseBody body) throws IOException {
        if (body != null && body.contentLength() != 0) {
            return JSON.parseObject(body.string(), VersionUpdateModel.class);
        }
        return null;
    }

    void onSuccess() {
        if (vModel == null) {
            doCheckVersion(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    tips(getString(R.string.get_info_fail));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if ((vModel = parseBody(response.body())) == null) {
                        tips(getString(R.string.get_info_fail));
                    } else {
                        onSuccess();
                    }
                }
            });
        } else {
            if (vHelper != null) {
                VersionUpdateHelper.destroy(vHelper);
            }
            // TODO Direction 2
            tv_check.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            vHelper = VersionUpdateHelper.create(UpdateActivity.this, new SimpleVersionHelperListener() {
                                @Override
                                public void sConnectState(int state) {
                                    if (state == 1) {
                                        vHelper.doHasVersionModel(vModel, DownloadWay.defaultWay);
                                    } else {
                                        vHelper = null;
                                    }
                                }
                            });
                        }
                    }
                    , vHelper == null ? 0 : 100);
        }
    }


    void doCheckVersion(Callback callback) {
        new OkHttpClient.Builder()
                .build()
                .newCall(
                        new Request.Builder()
                                .url("https://appapi.byyjp.com/api/app/version?id=378")
                                .get()
                                .build()
                )
                .enqueue(callback);
    }

    void tips(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UpdateActivity.this);
        builder.setTitle(R.string.tips);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    protected void onDestroy() {
        VersionUpdateHelper.destroy(vHelper);
        super.onDestroy();
    }
}

