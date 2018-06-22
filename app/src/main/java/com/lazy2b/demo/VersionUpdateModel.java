package com.lazy2b.demo;

import android.text.TextUtils;

import com.alibaba.fastjson.annotation.JSONField;
import com.lazylibs.updater.interfaces.IUpgradeModel;

/**
 * Created by lazy2b on 18/2/6.
 */

public class VersionUpdateModel implements IUpgradeModel {

    @JSONField(name = "appId")
    public int appId = 0;
    @JSONField(name = "version")
    public String version = "";
    @JSONField(name = "appUrl")
    public String appUrl = "";
    @JSONField(name = "packageUrl")
    public String packageUrl = "";
    @JSONField(name = "appStoreUrl")
    public String appStoreUrl = "";
    @JSONField(name = "updateInfo")
    public String updateInfo = "";
    @JSONField(name = "IsForceUpdate")
    public boolean isForceUpdate = false;
    @JSONField(name = "Notice")
    public String notice = "";
    @JSONField(name = "PackageSize")
    public String packageSize = "";

    @JSONField(name = "isNeedUpgrade")
    private boolean isNeedUpgrade = false;

    @Override
    public void setNeedUpgrade(Object versionName) {
        isNeedUpgrade = hasNewVersion(this.version, (String) versionName);
    }

    public boolean isNeedUpgrade() {
        return isNeedUpgrade;
    }

    @Override
    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    @Override
    public String getUpdateInfo() {
        return updateInfo;
    }

    @Override
    public String getNewVersionName() {
        return version;
    }

    @Override
    public String getDownloadUrl() {
        return packageUrl;
    }

    public static boolean hasNewVersion(String server, String client) {
        /* 2 */
        if (TextUtils.isEmpty(server) || TextUtils.isEmpty(client) || server.equals(client)) {
            return false;
        }
        String[] serverArr = server.split("\\.");
        String[] clientArr = client.split("\\.");
        for (int i = 0; i < serverArr.length; i++) {
            int iServer = Integer.valueOf(serverArr[i]), iClient = Integer.valueOf(clientArr[i]);
            if (iClient > iServer) return false;
            if (iServer > iClient) {
                return true;
            }
        }
        return false;
        /* 2 */
    }

}
