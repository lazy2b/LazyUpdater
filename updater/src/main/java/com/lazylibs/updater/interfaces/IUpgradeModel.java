package com.lazylibs.updater.interfaces;

/**
 * Created by lazy2b on 18/6/12.
 */

public interface IUpgradeModel {

    void setNeedUpgrade(Object versionName);

    boolean isNeedUpgrade();

    boolean isForceUpdate();

    String getUpdateInfo();

    String getNewVersionName();

    String getDownloadUrl();
}
