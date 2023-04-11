package com.lazylibs.updater.model;

/**
 * Created by lazy2b on 18/6/14.
 */

public enum UpdateResult {
    /**
     * 有新版 且： 1应用本身下载完成，2启动外部浏览器下载
     */
    Success,
    /**
     * 有新版但取消下载(没wifi提示窗、暂不更新按钮)
     */
    Cancel,
    /**
     * 有新版但出现异常(下载过程失败)
     */
    Error,
    /**
     * 无新版本(不需要更新、没有新包地址)
     */
    NoNews,
    /**
     * 销毁
     */
    Destroy
}
