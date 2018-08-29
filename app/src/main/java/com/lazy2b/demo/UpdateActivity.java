package com.lazy2b.demo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.lazylibs.updater.R;
import com.lazylibs.updater.SimpleVersionHelperListener;
import com.lazylibs.updater.VersionUpdateHelper;
import com.lazylibs.updater.interfaces.IUpgradeModel;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                                            new SimpleVersionHelperListener(), vModel);
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
                                        vHelper.doHasVersionModel(vModel);
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
                                .url("https://appapi.byyapp.com/api/app/version?id=378")
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
