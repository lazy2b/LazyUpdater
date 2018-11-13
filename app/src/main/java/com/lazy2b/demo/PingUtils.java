package com.lazy2b.demo;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public interface PingUtils {

    static void ping(String host, OnPingResponse pingResponse) {
        new AsyncPingTask(pingResponse).execute(host);
    }

    interface OnPingResponse {
        void updateResponse(String result);

        default void postResult(PingResult result) {
            Log.i(AsyncPingTask.class.getSimpleName(), result.toString());
        }
    }

    /**
     * 返回的命令结果
     */
    final class PingResult {
        /**
         * 结果码
         **/
        public int result;
        /**
         * 成功信息
         **/
        public String successMsg;
        /**
         * 错误信息
         **/
        public String errorMsg;

        public PingResult(final int result, final String successMsg, final String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        @Override
        public String toString() {
            return "PingResult{" +
                    "result=" + result +
                    ", successMsg='" + successMsg + '\'' +
                    ", errorMsg='" + errorMsg + '\'' +
                    '}';
        }
    }


    final class AsyncPingTask extends AsyncTask<Object, String, PingResult> {

        private static final String PING_CMD = "ping -c 10 %s";
        private static final String LINE_SEP = System.getProperty("line.separator");
        private static final String MATCH_PING_HOST_IP = "(?<=\\().*?(?=\\))";

        private OnPingResponse pingResponse;

        private AsyncPingTask(@NonNull OnPingResponse pingResponse) {
            this.pingResponse = pingResponse;
        }

        private String getHost(String host) {
            Matcher m = Pattern.compile(MATCH_PING_HOST_IP).matcher(host);
            return m.find() ? m.group() : host;
        }

        @Override
        protected PingResult doInBackground(Object... args) {
            int result = -1;
            if (args.length > 0 && args[0] instanceof String) {
                String host = (String) args[0];
                if (!TextUtils.isEmpty(host)) {
                    String command = String.format(PING_CMD, getHost(host));
                    Process process = null;
                    BufferedReader successResult = null;
                    BufferedReader errorResult = null;
                    StringBuilder successMsg = null;
                    StringBuilder errorMsg = null;
                    DataOutputStream dos = null;
                    try {
                        onProgressUpdate(command);
                        process = Runtime.getRuntime().exec("sh");
                        dos = new DataOutputStream(process.getOutputStream());
                        dos.write(command.getBytes());
                        dos.writeBytes(LINE_SEP);
                        dos.flush();
                        dos.writeBytes("exit" + LINE_SEP);
                        dos.flush();
                        successMsg = new StringBuilder();
                        errorMsg = new StringBuilder();
                        successResult = new BufferedReader(new InputStreamReader(process.getInputStream(),
                                "UTF-8"));
                        errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                                "UTF-8"));
                        String line;
                        if ((line = successResult.readLine()) != null) {
                            successMsg.append(line);
                            while ((line = successResult.readLine()) != null) {
                                successMsg.append(LINE_SEP).append(line);
                                onProgressUpdate(line);
                            }
                        }
                        if ((line = errorResult.readLine()) != null) {
                            errorMsg.append(line);
                            while ((line = errorResult.readLine()) != null) {
                                errorMsg.append(LINE_SEP).append(line);
                                onProgressUpdate(line);
                            }
                        }
                        result = process.waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        closeIO(dos, successResult, errorResult);
                        if (process != null) {
                            process.destroy();
                        }
                    }
                    return new PingResult(
                            result,
                            successMsg == null ? null : successMsg.toString(),
                            errorMsg == null ? null : errorMsg.toString()
                    );
                }
            }
            return new PingResult(result, "", "");
        }

        @Override
        protected void onProgressUpdate(String... values) {
            this.pingResponse.updateResponse(values[0]);
        }

        @Override
        protected void onPostExecute(PingResult pingResult) {
            this.pingResponse.postResult(pingResult);
        }

        @MainThread
        private AsyncTask<Object, String, PingResult> execute(@NonNull String host) {
            return super.execute(host);
        }

        /**
         * 关闭 IO
         *
         * @param closeables closeables
         */
        private void closeIO(final Closeable... closeables) {
            if (closeables == null) return;
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
