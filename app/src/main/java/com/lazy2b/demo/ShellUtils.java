/*
 * Copyright 2017 Blankj
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lazy2b.demo;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Created by chenyp on 2018/1/8.
 * Email : chenyp1994@gmail.com
 */

public final class ShellUtils {

    private static final String LINE_SEP = System.getProperty("line.separator");

    private ShellUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 是否是在 root 下执行命令
     *
     * @param command 命令
     * @return CommandResult
     */
    public static CommandResult execCmd(final String command, @NonNull final OnCmdResponse cmdResponse) {
        return execCmd(new String[]{command}, false, true, cmdResponse);
    }

    interface OnCmdResponse {
        void updateResponse(String resp);
    }

    OnCmdResponse cmdResponse;

    /**
     * 是否是在 root 下执行命令
     *
     * @param commands        命令数组
     * @param isRoot          是否需要 root 权限执行
     * @param isNeedResultMsg 是否需要结果消息
     * @return CommandResult
     */
    public static CommandResult execCmd(@NonNull final String[] commands,
                                        final boolean isRoot,
                                        final boolean isNeedResultMsg,
                                        @NonNull final OnCmdResponse cmdResponse) {
        int result = -1;
        if (commands.length == 0) {
            return new CommandResult(result, null, null);
        }
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        DataOutputStream dos = null;
        try {
            process = Runtime.getRuntime().exec(isRoot ? "su" : "sh");
            dos = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) continue;
                dos.write(command.getBytes());
                dos.writeBytes(LINE_SEP);
                dos.flush();
            }
            dos.writeBytes("exit" + LINE_SEP);
            dos.flush();
//            result = process.waitFor();
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream(),
                        "UTF-8"));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream(),
                        "UTF-8"));
                String line;
                long time;
                String tag = "ping-time";
                Log.e(tag, (time = System.currentTimeMillis()) + "");
                if ((line = successResult.readLine()) != null) {
                    successMsg.append(line);
                    while ((line = successResult.readLine()) != null) {
                        successMsg.append(LINE_SEP).append(line);
                        Log.e(tag, (System.currentTimeMillis() - time) + "");
                        cmdResponse.updateResponse(line);
                        Log.e(tag, (System.currentTimeMillis() - time) + "");
                    }
                }
                if ((line = errorResult.readLine()) != null) {
                    errorMsg.append(line);
                    while ((line = errorResult.readLine()) != null) {
                        errorMsg.append(LINE_SEP).append(line);
                        Log.e(tag, (System.currentTimeMillis() - time) + "");
                        cmdResponse.updateResponse(line);
                        Log.e(tag, (System.currentTimeMillis() - time) + "");
                    }
                }
                Log.e(tag, (System.currentTimeMillis() - time) + "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(dos, successResult, errorResult);
            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(
                result,
                successMsg == null ? null : successMsg.toString(),
                errorMsg == null ? null : errorMsg.toString()
        );
    }

    /**
     * 返回的命令结果
     */
    public static class CommandResult {
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

        public CommandResult(final int result, final String successMsg, final String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }

}
