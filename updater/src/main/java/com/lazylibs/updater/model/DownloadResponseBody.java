package com.lazylibs.updater.model;

import android.support.annotation.MainThread;
import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;


public class DownloadResponseBody extends ResponseBody {
    private ResponseBody responseBody;
    private BufferedSource bufferedSource;
    private DownloadProgressListener progressListener;
    private DownloadProgress.ProgressBuilder progressBuilder;

    public DownloadResponseBody(ResponseBody responseBody, DownloadProgressListener progressListener) {
        this.responseBody = responseBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                if (null == progressListener) {
                    return super.read(sink, byteCount);
                } else {
                    if (progressBuilder == null) {
                        progressBuilder = new DownloadProgress.ProgressBuilder(contentLength());
                    }
                    DownloadProgress progress = progressBuilder.get(byteCount = super.read(sink, byteCount));
                    if (progress != null) {
                        progressListener.updateProgress(progress);
                    }
                    return byteCount;
                }
            }
        };
    }

    /**
     * 成功回调处理
     */
    public interface DownloadProgressListener {
        /**
         * 下载进度
         */
        @MainThread
        void updateProgress(@Nullable DownloadProgress progress);
    }
}