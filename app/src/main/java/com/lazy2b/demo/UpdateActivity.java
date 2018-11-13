package com.lazy2b.demo;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.caimao.network.diagnosis.activity.NetworkDiagnosisActivity;
import com.lazylibs.updater.R;
import com.lazylibs.updater.SimpleVersionHelperListener;
import com.lazylibs.updater.VersionUpdateHelper;
import com.lazylibs.updater.interfaces.IUpgradeModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

        PingUtils.ping("lazy2b.com", pingResponse);
    }

    PingUtils.OnPingResponse pingResponse = new PingUtils.OnPingResponse() {
        @Override
        public void updateResponse(String result) {
            Log.w("ping--->", result);
        }

        @Override
        public void postResult(PingUtils.PingResult result) {
            if (isPingBaidu) {
                isPingBaidu = false;
                PingUtils.ping("www.baidu.com", pingResponse);
            }
        }
    };

    boolean isPingBaidu = true;


    public String Ping(String str) {
        String resault = "";
        Process p;
        try {
            //ping -c 3 -w 100  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 100  以秒为单位指定超时间隔，是指超时时间为100秒
            p = Runtime.getRuntime().exec("ping -c 3 -w 100 " + str);
            int status = p.waitFor();

            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null) {
                buffer.append(line).append("\n\r");
            }
            Log.i("ping", "Return ============" + buffer.toString());

            if (status == 0) {
                resault = "success";
            } else {
                resault = "faild";
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return resault;
    }

    private class NetPing extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            String s = "";
            s = Ping("lazy2b.com");
            Log.i("ping", s);
            return s;
        }
    }

    IUpgradeModel parseBody(ResponseBody body) throws IOException {
        if (body != null && body.contentLength() != 0) {
            return JSON.parseObject(body.string(), VersionUpdateModel.class);
        }
        return null;
    }

    void onSuccess() {


        NetworkDiagnosisActivity.startNetworkDiagnosis(this,"","","lazy2b.com","");


//        if (vModel == null) {
//            doCheckVersion(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    e.printStackTrace();
//                    tips(getString(R.string.get_info_fail));
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    if ((vModel = parseBody(response.body())) == null) {
//                        tips(getString(R.string.get_info_fail));
//                    } else {
//                        onSuccess();
//                    }
//                }
//            });
//        } else {
//            if (vHelper != null) {
//                VersionUpdateHelper.destroy(vHelper);
//            }
//            // TODO Direction 2
//            tv_check.postDelayed(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            vHelper = VersionUpdateHelper.create(UpdateActivity.this, new SimpleVersionHelperListener() {
//                                @Override
//                                public void sConnectState(int state) {
//                                    if (state == 1) {
//                                        vHelper.doHasVersionModel(vModel);
//                                    } else {
//                                        vHelper = null;
//                                    }
//                                }
//                            });
//                        }
//                    }
//                    , vHelper == null ? 0 : 100);
//        }
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

