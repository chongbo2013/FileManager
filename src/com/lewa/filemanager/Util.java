package com.lewa.filemanager;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.MediaStore.Files;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
//LEWA ADD BEGIN
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
//LEWA ADD END
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lewa.filemanager.R;
import com.lewa.filemanager.HanziToPinyin.Token;
import com.lewa.themes.ThemeManager;

public class Util {
    private static String OTG_PATH = "/storage/usbotg";
    private static String ANDROID_SECURE = "/mnt/sdcard/.android_secure";
    private static final String LOG_TAG = "Util";
    // add by luoyongxing
    private static boolean[] sSDCardReady = new boolean[GlobalConsts.MAX_SDCARD_NUM];
    public static String[] sSDCardDir = null;

    public static boolean sSupportDualSD = false;
    public static boolean sDualSDMounted = false;

    public static int sSupportSDNum = 0;
    public static int sdcardMountedNum = 0;

    public static int internalStorageIndex = 0;
    public static int sdcardIndex = 1;
    public static int usbOtgIndex = -1;

    public static final Executor DELETE_EXECUTOR = Executors
            .newSingleThreadExecutor();
    public static final String FILE_PICK_ACTION = "com.lewa.filemanager.PICK";

    // sSDCardDir.length maybe 3, sdcard0 sdcard1, usbotg, liuhao modify for
    // android 6589 4.2
    private static int sSDCardDirMaxLen = GlobalConsts.MAX_SDCARD_NUM;

    //系统初始化或者接收到广播之后，会调用此方法初始化或者更新
    public static void initOrUpdateStaticValue(Context ctx) {
        StorageManager storageManager = (StorageManager) ctx
                .getSystemService(Context.STORAGE_SERVICE);
        sSDCardDir = storageManager.getVolumePaths();
        String[] sdcardlist = new String[sSDCardDirMaxLen];

        // sSDCardDir.length maybe 3, sdcard0 sdcard1, usbotg
        sSDCardDirMaxLen = (sSDCardDir.length <= GlobalConsts.MAX_SDCARD_NUM)
                ? sSDCardDir.length
                : GlobalConsts.MAX_SDCARD_NUM;
        if (sSDCardDir == null || sSDCardDirMaxLen <= 0) {
            Log.e(LOG_TAG, " getVolumePaths() failed.");
            return;
        }

        for (int i = 0; i < sSDCardDirMaxLen && i < sSDCardDir.length; i++) {
            sdcardlist[i] = sSDCardDir[i];
        }

        updateIndex(sdcardlist, storageManager);//更新各个目录的Index
        isSupportDualSD();//判断是否支持外置SD卡
        isDualSDMounted();//判断外置SD卡是否装载
        reversalSDcardPath(sdcardlist);//MTK的机器在插入SD卡或者USBOTG后，内置SD卡会和外置SD卡交换位置
        updateInfoForJianyu(); //佳域的机型没有内置SD卡
    }

    public static void updateIndex(String[] sdcardlist, StorageManager storageManager) {
        StorageVolume[] storageVolumeList = storageManager.getVolumeList();
        usbOtgIndex = -1;
        String state;
        int version = Util.getAndroidSDKVersion();
        int count = 0;
        for (int i = 0; i < sSDCardDirMaxLen && i < sSDCardDir.length; i++) {
            state = storageManager.getVolumeState(sSDCardDir[i]);
            sSDCardReady[i] = Environment.MEDIA_MOUNTED.equals(state);
            if (sSDCardReady[i]) {
                if (version >= 15) {
                    if (!storageVolumeList[i].isRemovable()) {//内置SD卡不能被移除
                        internalStorageIndex = i;
                    } else if (sSDCardDir[i].startsWith(OTG_PATH)) {//usbotg的路径是固定的（有风险）
                        usbOtgIndex = i;
                    } else {
                        sdcardIndex = i;//如果不是上两者，就只能是外置SD卡了
                    }
                }
                count++;
            } else {
                sdcardlist[i] = "";
            }
        }

        if (usbOtgIndex != -1) {
            sdcardMountedNum = count - 1;
        } else {
            sdcardMountedNum = count;
        }

    }

    //对于MTK平台，判断是否支持外置SD卡
    public static boolean isSupportDualSDOnMtk() {
        String supportDusalSDStr = SystemProperties.get("ro.sys.dualSD",
                "false");
        return supportDusalSDStr.equals("true");
    }

    //对于高通平台，判断主存卡
    public static boolean isMainStorage() {
        String main_storage = SystemProperties.get("epersist.sys.main_storag",
                "");
        return main_storage.equals("internal_sd");
    }

    //判断是否支持双卡
    public static void isSupportDualSD() {
        sSupportDualSD = isSupportDualSDOnMtk() || isMainStorage();

        if (sSupportDualSD) sSupportSDNum = sSDCardDirMaxLen;
        else sSupportSDNum = 1;

        if (2 == sdcardMountedNum && 1 == sSupportSDNum) {
            sSupportSDNum = 2;
            sSupportDualSD = true;
        } else if (1 == sdcardMountedNum && 2 == sSupportSDNum) {
            sSupportSDNum = 2;
            sSupportDualSD = false;
        }
    }

    public static void reversalSDcardPath(String[] sdcardList) {
        int count = 0;

        for (int i = 0; i < sSDCardDir.length; i++) {
            sSDCardDir[i] = "";
        }
        if (internalStorageIndex != -1) {
            sSDCardDir[count++] = sdcardList[internalStorageIndex];
        }

        for (int i = 0; i < sdcardList.length; i++) {
            if (sdcardList[i] != null && !sdcardList[i].equals("")
                    && internalStorageIndex != i) {
                sSDCardDir[count++] = sdcardList[i];
            }
        }
    }

    //佳域的机型没有内置SD卡，所以要进行特殊的处理
    public static void updateInfoForJianyu() {
        if (sSDCardDir[0].equals("")) {// add by yue,for no internal memory, jiayu_g2f_jb5
            sSupportSDNum = 1;
        } else {
            sSupportSDNum = 0;
        }

        for (String path : sSDCardDir) {
            if (!TextUtils.isEmpty(path)) {
                if (path.startsWith(OTG_PATH)) {
                    usbOtgIndex = sSupportSDNum;
                }
                sSupportSDNum++;
            }
        }
    }

    //判断外置SD卡是否已经挂载
    public static void isDualSDMounted() {
        if (2 == sdcardMountedNum) {
            sDualSDMounted = true;
        } else if (1 == sdcardMountedNum) {
            sDualSDMounted = false;
        }
    }

    public static String[] getsSDCardDirs(Context context) {
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        return storageManager.getVolumePaths();
    }

    //判断sd卡路径是否装载
    public static boolean checkSDCardMount(Context ctx, String mountPoint) {
        StorageManager mStorageManager = (StorageManager) ctx
                .getSystemService(Context.STORAGE_SERVICE);
        if (mountPoint == null) {
            return false;
        }
        String state = null;
        state = mStorageManager.getVolumeState(mountPoint);
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    //判断所有的SD卡是否可用，包括sd0和sd1
    public static boolean hasSDCardReady(Context ctx) {
        StorageManager storageManager = (StorageManager) ctx
                .getSystemService(Context.STORAGE_SERVICE);
        boolean hadsdcardready = false;
        String state;
        for (int i = 0; i < sSDCardDir.length && i < sSupportSDNum; i++) {
            state = storageManager.getVolumeState(sSDCardDir[i]);
            sSDCardReady[i] = Environment.MEDIA_MOUNTED.equals(state);
            hadsdcardready = sSDCardReady[i] || hadsdcardready;
        }
        return hadsdcardready;
    }

    public static int getSDIndex(String path) {
        if (path == null) {
            return 0;
        }
        for (int i = 0; i < sSupportSDNum; i++) {
            if (path.equals(sSDCardDir[i])) {
                return i;
            }
        }
        return 0;
    }

    //判断是否支持双sd卡
    public static boolean isSupportDualSDCard() {
        return sSupportDualSD;
    }

    //判断所有装载的sd卡是否可用
    public static boolean isAllSDCardReady() {
        for (int i = 0; i < sSupportSDNum; i++) {
            if (!sSDCardReady[i]) {
                return false;
            }
        }
        return true;
    }

    //获得已经装载的sd卡的数量
    public static int getSdcardMountedNum() {
        return sdcardMountedNum;
    }

    public static boolean isSDCardReady(int sdIndex) {
        if (sdIndex >= 0 && sdIndex < sSupportSDNum) {
            if (sSDCardDir[0].equals("")) {// add by yue,for no internal memory,
                // jiayu_g2f_jb5
                return sSDCardReady[sdIndex];
            } else {
                return sSDCardReady[0];
            }
        }
        return false;
    }

    // if path1 contains path2
    public static boolean containsPath(String path1, String path2) {
        String path = path2;
        while (path != null) {
            if (path.equalsIgnoreCase(path1))
                return true;

            if (path.equals(GlobalConsts.ROOT_PATH))
                break;
            path = new File(path).getParent();
        }

        return false;
    }

    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return (path1 + path2).trim();

        return (path1 + File.separator + path2).trim();
    }

    public static String getSdDirectory(int index) {
        if (index >= 0 && index < sSupportSDNum) {
            return sSDCardDir[index];
        }
        Log.e(LOG_TAG, "getSdDirectory(), sdIndex out of bounds: sdIndex="
                + index + " sSupportSDNum=" + sSupportSDNum);
        return null;
    }

    public static boolean isNormalFile(String fullName) {
        return !fullName.equals(ANDROID_SECURE);
    }

    public static FileInfo GetFileInfo(String filePath) {
        File lFile = new File(filePath);
        if (!lFile.exists()) {
            return null;
        }

        FileInfo lFileInfo = new FileInfo();
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = Util.getNameFromFilepath(filePath);
        lFileInfo.ModifiedDate = lFile.lastModified();
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        lFileInfo.fileSize = lFile.length();
        return lFileInfo.fileSize > 0 ? lFileInfo : null;
    }

    public static FileInfo GetFileInfo(File f, FilenameFilter filter,
                                       boolean showHidden) {
        FileInfo lFileInfo = new FileInfo();
        String filePath = f.getPath();
        File lFile = new File(filePath);
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = f.getName();
        lFileInfo.ModifiedDate = lFile.lastModified();
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        String chName = Util.getPinyinFromCH(lFileInfo.fileName, false);
        lFileInfo.fileSDsortName = (chName.equals("")
                ? lFileInfo.fileName
                : chName);
        if (lFileInfo.IsDir) {
            int lCount = 0;
            File[] files = lFile.listFiles(filter);

            // null means we cannot access this dir
            if (files == null) {
                return null;
            }

            for (File child : files) {
                if ((!child.isHidden() || showHidden)
                        && Util.isNormalFile(child.getAbsolutePath())) {
                    lCount++;
                }
            }
            lFileInfo.Count = lCount;

        } else {
            lFileInfo.fileSize = lFile.length();
        }
        return lFileInfo;
    }

    /*
     * 采用了新的办法获取APK图标，之前的失败是因为android中存在的一个BUG,通过 appInfo.publicSourceDir =
     * apkPath;来修正这个问题，详情参见:
     * http://code.google.com/p/android/issues/detail?id=9151
     */
    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return /*appInfo.loadIcon(pm)*/ThemeManager.getInstance(context).loadIcon(appInfo);
            } catch (OutOfMemoryError e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        return null;
    }

    private static Drawable showUninstallAPKIcon(Context context, String apkPath) {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        String PATH_AssetManager = "android.content.res.AssetManager";
        try {
            // apk包的文件路径
            // 这是一个Package 解释器, 是隐藏的
            // 构造函数的参数只有一个, apk文件的路径
            // PackageParser packageParser = new PackageParser(apkPath);
            Class pkgParserCls = Class.forName(PATH_PackageParser);
            Class[] typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
            Object[] valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            Object pkgParser = pkgParserCt.newInstance(valueArgs);
            // Log.d("ANDROID_LAB", "pkgParser:" + pkgParser.toString());
            // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();

            // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
            // File(apkPath), apkPath,
            // metrics, 0);
            typeArgs = new Class[4];
            typeArgs[0] = File.class;
            typeArgs[1] = String.class;
            typeArgs[2] = DisplayMetrics.class;
            typeArgs[3] = Integer.TYPE;
            Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod(
                    "parsePackage", typeArgs);
            valueArgs = new Object[4];
            valueArgs[0] = new File(apkPath);
            valueArgs[1] = apkPath;
            valueArgs[2] = metrics;
            valueArgs[3] = 0;
            Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser,
                    valueArgs);
            // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
            // ApplicationInfo info = mPkgInfo.applicationInfo;
            Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
                    "applicationInfo");
            ApplicationInfo info = (ApplicationInfo) appInfoFld
                    .get(pkgParserPkg);

            Class assetMagCls = Class.forName(PATH_AssetManager);
            Constructor assetMagCt = assetMagCls.getConstructor((Class[]) null);

            Object assetMag = assetMagCt.newInstance((Object[]) null);
            typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
                    "addAssetPath", typeArgs);
            valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);

            Resources res = context.getResources();
            typeArgs = new Class[3];
            typeArgs[0] = assetMag.getClass();
            typeArgs[1] = res.getDisplayMetrics().getClass();
            typeArgs[2] = res.getConfiguration().getClass();
            Constructor resCt = Resources.class.getConstructor(typeArgs);
            valueArgs = new Object[3];
            valueArgs[0] = assetMag;
            valueArgs[1] = res.getDisplayMetrics();
            valueArgs[2] = res.getConfiguration();
            res = (Resources) resCt.newInstance(valueArgs);
            // 这里就是读取一个apk程序的图标
            if (info.icon != 0) {
                return res.getDrawable(info.icon);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get Theme Icon.
     *
     * @param context
     * @param themePath
     * @return Theme Icon
     */
    public static Drawable getThemeIcon(Context context, String themePath) {
        return showUninstallAPKIcon(context, themePath);
    }

    public static Bitmap getMusicThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            // retriever.setMode(MediaMetadataRetriever.MODE_GET_METADATA_ONLY);
            retriever.setDataSource(filePath);
            byte[] art = retriever.getEmbeddedPicture();// .extractAlbumArt();
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
        } catch (IllegalArgumentException ex) {
        } catch (RuntimeException ex) {
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return bitmap;
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        } else {
            return filename;
        }
        // return "";
    }

    public static String getPathFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(0, pos);
        }
        return "";
    }

    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }

    // return new file path if successful, or return null
    public static String copyFile(Context context, String src, String dest) {
        File file = new File(src);
        SDCardInfo sdCardInfo;
        if (!file.exists() || file.isDirectory()) {
            Log.v(LOG_TAG, "copyFile: file not exist or is directory, " + src);
            return null;
        }
        if (((FileManagerMainActivity) context).getCurrentItemIndex() - 1 < 0) {
            return null;
        }
        sdCardInfo = Util.getSDCardInfo((FileManagerMainActivity) context,
                ((FileManagerMainActivity) context)
                        .getCurrentItemIndex() - 1);
        if (sdCardInfo.free <= file.length()) {
            ((IFileInteractionListener) ((FileManagerMainActivity) context)
                    .getFragment(((FileManagerMainActivity) context)
                            .getCurrentItemIndex()))
                    .getFileViewInteractionHub().setToastHint(
                    context.getString(R.string.space_limit));
            return "false";
        }

        FileInputStream fi = null;
        FileOutputStream fo = null;
        File errorDeleteFile = null;
        try {
            fi = new FileInputStream(file);
            File destPlace = new File(dest);
            if (!destPlace.exists()) {
                if (!destPlace.mkdirs())
                    return null;
            }

            String destPath = Util.makePath(dest, file.getName());
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                String destName = Util.getNameFromFilename(file.getName())
                        + "_" + i++ + "."
                        + Util.getExtFromFilename(file.getName());
                destPath = Util.makePath(dest, destName);
                destFile = new File(destPath);
            }

            if (!destFile.createNewFile())
                return null;
            errorDeleteFile = destFile;
            fo = new FileOutputStream(destFile);
            int count = 102400;
            byte[] buffer = new byte[count];
            int read = 0;
            while ((read = fi.read(buffer, 0, count)) != -1) {
                fo.write(buffer, 0, read);
            }

            // TODO: set access privilege

            return destPath;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "copyFile: file not found, " + src);
            e.printStackTrace();
        } catch (IOException e) {
            if (errorDeleteFile != null && errorDeleteFile.exists()) {
                errorDeleteFile.delete();
            }
            Log.e(LOG_TAG, "copyFile: " + e.toString());
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // does not include sd card folder
    private static String[] SysFileDirs = new String[]{"miren_browser/imagecaches"};

    public static boolean shouldShowFile(String filepath, String sdcardpath) {
        return shouldShowFile(new File(filepath), sdcardpath);
    }

    public static boolean shouldShowFile(File file, String sdcardpath) {
        boolean show = Settings.instance().getShowDotAndHiddenFiles();
        if (show)
            return true;

        if (file.isHidden())
            return false;

        if (file.getName().startsWith("."))
            return false;

        // String sdFolder = getSdDirectory();
        String sdFolder = sdcardpath;
        for (String s : SysFileDirs) {
            if (file.getPath().startsWith(makePath(sdFolder, s)))
                return false;
        }

        return true;
    }

    public static boolean setText(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    public static boolean setText(View view, int id, int text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    // comma separated number
    public static String convertNumber(long number) {
        return String.format("%,d", number);
    }

    // storage, G M K B
    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.2f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.2f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.2f KB", f);
        } else
            return String.format("%d B", size);
    }

    public static class SDCardInfo {
        public long total;

        public long free;
    }

    public static boolean isDoubleSDCard(Context ctx) {
        StorageManager storageManager = (StorageManager) ctx
                .getSystemService(Context.STORAGE_SERVICE);
        String[] SDCardList = storageManager.getVolumePaths();
        return SDCardList.length >= 2 ? true : false;
    }

    public static SDCardInfo getSDCardInfo(Context ctx, int sdIndex) {
        StorageManager storageManager = (StorageManager) ctx
                .getSystemService(Context.STORAGE_SERVICE);
        // String sDcString = android.os.Environment.getExternalStorageState();
        // //value: mounted.
        String sDcString = storageManager.getVolumeState(sSDCardDir[sdIndex]);
        if (sDcString.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File pathFile = new File(getSdDirectory(sdIndex));// new
            // File("/mnt/sdcard2");
            // //android.os.Environment.getExternalStorageDirectory();
            try {
                android.os.StatFs statfs = new android.os.StatFs(
                        pathFile.getPath());

                // 获取SDCard上BLOCK总数
                long nTotalBlocks = statfs.getBlockCount();

                // 获取SDCard上每个block的SIZE
                long nBlocSize = statfs.getBlockSize();

                // 获取可供程序使用的Block的数量
                long nAvailaBlock = statfs.getAvailableBlocks();

                // 获取剩下的所有Block的数量(包括预留的一般程序无法使用的块)
                long nFreeBlock = statfs.getFreeBlocks();

                SDCardInfo info = new SDCardInfo();
                // 计算SDCard 总容量大小MB
                info.total = nTotalBlocks * nBlocSize;

                // 计算 SDCard 剩余大小MB
                info.free = nAvailaBlock * nBlocSize;

                return info;
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }

        return null;
    }

    public static void showNotification(Context context, Intent intent,
                                        String title, String body, int drawableId) {
        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(drawableId, body,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_SOUND;
        if (intent == null) {
            // FIXEME: category tab is disabled
            intent = new Intent(context, FileViewFragment.class);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);
        notification.setLatestEventInfo(context, title, body, contentIntent);
        manager.notify(drawableId, notification);
    }

    public static String formatDateString(Context context, long time) {
        /*
         * DateFormat dateFormat = android.text.format.DateFormat
         * .getDateFormat(context); DateFormat timeFormat =
         * android.text.format.DateFormat .getTimeFormat(context);
         */
        SimpleDateFormat sdf = new SimpleDateFormat(
                context.getString(R.string.FileListDateFormat));
        Date date = new Date(time);
        return sdf.format(date) + " "
                + FileManagerMainActivity.timeFormat.format(date);
    }

    public static String formatDateStringMusic(Context context, long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");// yyyy-MM-dd
        // HH:mm:ss");
        Date date = new Date(time);
        return sdf.format(date);
    }

    public static void updateActionModeTitle(ActionMode mode, Context context,
                                             int selectedNum) {
        if (mode != null) {
            mode.setTitle(context.getString(R.string.multi_select_title,
                    selectedNum));

            if (selectedNum == 0) {
                mode.finish();
            }
        }
    }

    public static void updateActionModeMenuTitle(ActionMode mode,
                                                 Context context, int selectedNum) {
        if (mode != null) {
            String format;
            try {
                format = context.getResources().getString(
                        lewa.R.string.title_actionbar_selected_items);
            } catch (Exception e) {
                // TODO: handle exception
                format = "";
            }

            mode.setTitle(String.format(format, selectedNum));
        }
    }

    public static void updateActionModeMenuState(ActionMode mode,
                                                 Context context) {
        Menu menu = mode.getMenu();
//        MenuItem selectAll = menu.findItem(R.id.action_select_all);
//        if (mode != null) {
//            selectAll.setIcon(lewa.R.drawable.ic_menu_select_all);
//            // Delete for standalone by Fan.Yang
//            mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
//        }
    }

    public static HashSet<String> sDocMimeTypesSet = new HashSet<String>() {
        {
            add("text/plain");
            add("application/pdf");
            add("application/msword");
            add("application/vnd.ms-excel");
            add("application/vnd.ms-excel");
        }
    };

    public static String sZipFileMimeType = "application/zip";

    public static int CATEGORY_TAB_INDEX = 0;
    public static int SDCARD_TAB_INDEX = 1;

    static public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    static public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    public static void notifyFileRemoveDB(final Context context,
                                          final String path) {
        DELETE_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                String where = "";
                try {
                    // String path =
                    // file.getAbsolutePath().replaceFirst(".*/?sdcard",
                    // "/mnt/sdcard");
                    where = MediaStore.Images.Media.DATA + " = '" + path + "'";// +
                    // "%'";
                    String volumeName = "external";
                    Uri uri = Files.getContentUri(volumeName);
                    int count = context.getContentResolver().delete(uri, where,
                            null);
                    // Log.d("yx", "OperHelper -> notifyFileRemove -> path: "+
                    // path + " count: " + count);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    public static void notifyFileRemoveBroadcast(Context context, String path) {
        try {
            Intent intent;// .ACTION_DELETE);//.ACTION_PACKAGE_REMOVED);//ACTION_MEDIA_REMOVED);.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(new File(path.toString())));
            // Log.d("yx", "Util -> notifyFileRemoveBroadcast, send broadcast:"
            // + intent.toString());
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        MediaScannerConnection.scanFile(context,
                new String[]{ path }, null, null);
    }

    public static void notifyRootScanBroadcast(Context context, String root) {
        try {
            Intent intent;
            intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
            intent.setClassName("com.android.providers.media",
                    "com.android.providers.media.MediaScannerReceiver");
            intent.setData(Uri.fromFile(new File(root)));
            context.sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param name Input Chinese name.
     * @return If name is not Chinese, the return value will be empty.
     */
    public static String getPinyinFromCH(String name, boolean mixEngCh) {
        ArrayList<Token> nameTokens = HanziToPinyin.getInstance().get(name);
        StringBuilder sb = new StringBuilder();
        for (Token token : nameTokens) {
            if (Token.PINYIN == token.type) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                if (!mixEngCh) {
                    sb.append('~');
                }
                sb.append(token.target);
                sb.append('.');
                // sb.append(token.source);
            } else {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(token.source);
            }
        }
        if (sb == null || sb.equals(""))
            return "";
        return sb.toString();
    }

    /**
     * This function to get sdk version of android to split 4.0 and 4.1 for
     * internal-storage and sdcard.
     *
     * @return
     */
    public static int getAndroidSDKVersion() {
        int version = 0;
        try {
            version = Integer.valueOf(android.os.Build.VERSION.SDK);
        } catch (NumberFormatException e) {
        }
        return version;
    }

    public static boolean imageViewTagIsEqual(ImageView imageView, String tag) {
        // Log.d("fm", "imageView.tag:"+imageView.getTag()+" filePath:: "+tag);
        if (imageView != null && imageView.getTag() != null && tag != null) {
            try {
                // if (!tag.equals((String)imageView.getTag())) {
                // Log.e("fm",
                // "not equal, imageView.tag:"+imageView.getTag()+" filePath:: "+tag);
                // }
                return tag.equals((String) imageView.getTag());
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    public static boolean hasUsbOtg() {
        return usbOtgIndex != -1;
    }

    public static void setImageResource(ImageView imageView, int resId,
                                        String filePath) {
        // Log.d("fm", "resId:"+resId+" filePath:: "+filePath);
        if (Util.imageViewTagIsEqual(imageView, filePath)) {
            imageView.setImageResource(resId);
        }
    }

    public static boolean sIsSdCardNeedRefresh = false;

    public static void setIsSdCardNeedRefresh(boolean isSdCardRefreshed) {
        sIsSdCardNeedRefresh = isSdCardRefreshed;
    }

    public static boolean isSdCardNeedRefresh() {
        return sIsSdCardNeedRefresh;
    }

    public static boolean sIsMemoryListNeedRefresh = false;

    public static void setMemoryListNeedRefresh(boolean isMemoryListNeedRefresh) {
        sIsMemoryListNeedRefresh = isMemoryListNeedRefresh;
    }

    public static boolean isMemoryListNeedRefresh() {
        return sIsMemoryListNeedRefresh;
    }
}
