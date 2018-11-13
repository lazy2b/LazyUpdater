//package com.lazy2b.demo;
//
//import android.os.AsyncTask;
//import android.support.annotation.MainThread;
//import android.support.annotation.NonNull;
//import android.util.Log;
//
//import java.io.BufferedReader;
//import java.io.Closeable;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
///**
// * Created by lazy2b on 2018/11/5.
// */
//
//public class AsyncPingTask extends AsyncTask<Object, String, AsyncPingTask.PingResult> {
//
//    private static final String LINE_SEP = System.getProperty("line.separator");
//
//    private OnPingResponse pingResponse;
//
//    public AsyncPingTask(@NonNull OnPingResponse pingResponse) {
//        this.pingResponse = pingResponse;
//    }
//
//    @Override
//    protected PingResult doInBackground(Object... args) {
//        return ping((String) args[0]);
//    }
//
////    @MainThread
////    public AsyncTask<Object, String, AsyncPingTask.PingResult> execute(@NonNull String host, @NonNull OnPingResponse onPingResponse) {
////        return super.execute(String.format("ping -c 10 %s", host), pingResponse);
////    }
//
//    @MainThread
//    public AsyncTask<Object, String, AsyncPingTask.PingResult> execute(@NonNull String host) {
//        return super.execute(host);
//    }
//
//    @Override
//    protected void onProgressUpdate(String... values) {
//        this.pingResponse.updateResponse(values[0]);
//    }
//
//    @Override
//    protected void onPostExecute(PingResult pingResult) {
//        this.pingResponse.postResult(pingResult);
//    }
//
//    public interface OnPingResponse {
//        void updateResponse(String result);
//
//        default void postResult(PingResult result) {
//            Log.i(AsyncPingTask.class.getSimpleName(), result.toString());
//        }
//    }
//
//    /**
//     * 返回的命令结果
//     */
//    public static class PingResult {
//        /**
//         * 结果码
//         **/
//        public int result;
//        /**
//         * 成功信息
//         **/
//        public String successMsg;
//        /**
//         * 错误信息
//         **/
//        public String errorMsg;
//
//        public PingResult(final int result, final String successMsg, final String errorMsg) {
//            this.result = result;
//            this.successMsg = successMsg;
//            this.errorMsg = errorMsg;
//        }
//
//        @Override
//        public String toString() {
//            return "PingResult{" +
//                    "result=" + result +
//                    ", successMsg='" + successMsg + '\'' +
//                    ", errorMsg='" + errorMsg + '\'' +
//                    '}';
//        }
//    }
//
//    private static String PING_CMD = "ping -c 10 %s";
//
//    /**
//     * 是否是在 root 下执行命令
//     *
//     * @return CommandResult
//     */
//    private PingResult ping(@NonNull final String host) {
//        int result = -1;
//        Process process = null;
//        BufferedReader successResult = null;
//        BufferedReader errorResult = null;
//        StringBuilder successMsg = null;
//        StringBuilder errorMsg = null;
//        DataOutputStream dos = null;
//        try {
//            process = Runtime.getRuntime().exec("sh");
//            dos = new DataOutputStream(process.getOutputStream());
//            dos.write(String.format(PING_CMD, host).getBytes());
//            dos.writeBytes(LINE_SEP);
//            dos.flush();
//            dos.writeBytes("exit" + LINE_SEP);
//            dos.flush();
////            result = process.waitFor();
//            successMsg = new StringBuilder();
//            errorMsg = new StringBuilder();
//            successResult = new BufferedReader(new InputStreamReader(process.getInputStream(),
//                    "UTF-8"));
//            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream(),
//                    "UTF-8"));
//            String line;
//            if ((line = successResult.readLine()) != null) {
//                successMsg.append(line);
//                while ((line = successResult.readLine()) != null) {
//                    successMsg.append(LINE_SEP).append(line);
//                    onProgressUpdate(line);
//                }
//            }
//            if ((line = errorResult.readLine()) != null) {
//                errorMsg.append(line);
//                while ((line = errorResult.readLine()) != null) {
//                    errorMsg.append(LINE_SEP).append(line);
//                    onProgressUpdate(line);
//                }
//            }
//            process.waitFor();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            closeIO(dos, successResult, errorResult);
//            if (process != null) {
//                process.destroy();
//            }
//        }
//        return new PingResult(
//                result,
//                successMsg == null ? null : successMsg.toString(),
//                errorMsg == null ? null : errorMsg.toString()
//        );
//    }
//
//    /**
//     * 关闭 IO
//     *
//     * @param closeables closeables
//     */
//    public static void closeIO(final Closeable... closeables) {
//        if (closeables == null) return;
//        for (Closeable closeable : closeables) {
//            if (closeable != null) {
//                try {
//                    closeable.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//}
