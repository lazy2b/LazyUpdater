package com.lazylibs.updater.model;

/**
 * Created by lazy2b on 18/3/29.
 */

public class DownloadProgress {
    public int progress;
    public boolean done;

    public DownloadProgress(int progress, boolean done) {
        this.done = done;
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public static class ProgressBuilder {
        public static final int DOWNLOAD_PROGRESS_STEP = 2;
        long current = 0L;
        long step = -1L;
        int lastProgress = 1;
        int currProgress = 0;

        public ProgressBuilder(long len) {
            step = len / 100L;
        }

        public DownloadProgress get(long bytesRead) {
            current += bytesRead != -1 ? bytesRead : 0;
            currProgress = (int) (current / step);
            if (lastProgress < currProgress && currProgress % DOWNLOAD_PROGRESS_STEP == 0) {
                lastProgress = currProgress;
                System.out.println("step:" + step + "|progress:" + currProgress);
                return new DownloadProgress(currProgress, bytesRead == -1);
            }
            return null;
        }
    }
}
