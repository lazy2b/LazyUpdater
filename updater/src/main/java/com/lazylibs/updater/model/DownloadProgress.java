package com.lazylibs.updater.model;

/**
 * Created by lazy2b on 18/3/29.
 */

public class DownloadProgress {
    private int progress;

    DownloadProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    static class ProgressBuilder {
        static final int DOWNLOAD_PROGRESS_STEP = 2;
        long current = 0L;
        long step = -1L;
        int lastProgress = 1;
        int currProgress = 0;

        ProgressBuilder(long len) {
            if (len == 0) {
                step = 1;
            } else {
                step = len / 100L;
            }
        }

        DownloadProgress get(long bytesRead) {
            current += bytesRead != -1 ? bytesRead : 0;
            currProgress = (int) (current / step);
            if (lastProgress < currProgress && currProgress % DOWNLOAD_PROGRESS_STEP == 0) {
                lastProgress = currProgress;
                System.out.println("step:" + step + "|progress:" + currProgress);
                return new DownloadProgress(currProgress);
            }
            return null;
        }
    }
}
