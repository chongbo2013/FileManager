package com.lewa.filemanager;

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ApkItem {
    public String versionName;

    public String installInfo;

    Context context;

    public ApkItem(Context c, String apk_path) {
        try {
            context = c;
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(apk_path,
                    PackageManager.GET_ACTIVITIES);
            // ApplicationInfo appInfo = packageInfo.applicationInfo;
            if (packageInfo == null) {
                setUnknownInfo(c);
                return;
            }
            /** 得到包名 */
            String packageName = packageInfo.packageName;

            /** apk的版本名称 String */
            versionName = packageInfo.versionName;

            /** apk的版本号码 int */
            int versionCode = packageInfo.versionCode;

            installInfo = this.getApkInstallInfo(pm, packageName, versionCode);
        } catch (ActivityNotFoundException e) {
            setUnknownInfo(c);
            // Log.e("yx", "ApkItem " + e.toString());
        }
    }
    private void setUnknownInfo(Context c) {
        versionName = c.getString(R.string.apk_version_unknown);
        installInfo = c.getString(R.string.apk_installed_unknown);
    }

    /**
     * 
     * @param pm
     *            : PackageManager
     * @param packageName
     * @param versionCode
     * @return INSTALLED: 表示已经安装，且跟现在这个apk文件是一个版本 UNINSTALLED: 表示未安装
     *         INSTALLED_UPDATE: 表示已经安装，版本比现在这个版本要低，可以点击按钮更新
     */
    private String getApkInstallInfo(PackageManager pm, String packageName,
            int versionCode) {
        try {

            PackageInfo pi = pm.getPackageInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);

            if (pi != null) {
                // added by weihong, #60214
                if (packageName.equalsIgnoreCase(pi.packageName) && versionCode <= pi.versionCode) {
                    return context.getString(R.string.apk_installed);// INSTALLED;
                } else if (versionCode > pi.versionCode) {
                    return context.getString(R.string.apk_installed_update);// INSTALLED_UPDATE;
                }
            }

        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block

        }
        return context.getString(R.string.apk_uninstalled);// UNINSTALLED;
    }
}
