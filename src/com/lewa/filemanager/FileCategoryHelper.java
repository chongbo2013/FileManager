package com.lewa.filemanager;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.lewa.filemanager.R;

import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;

import com.lewa.filemanager.FileCategoryFragment.FileListIndex;
import com.lewa.filemanager.FileSortHelper.SortMethod;
import com.lewa.filemanager.MediaFile.MediaFileType;

public class FileCategoryHelper {
    public static final int COLUMN_ID = 0;

    public static final int COLUMN_PATH = 1;

    public static final int COLUMN_SIZE = 2;

    public static final int COLUMN_DATE = 3;
    //#69060 modify begin by bin.dong
    public static final String COLUMN_CH_NAME = "title_key"/*"title_pinyin_key"*/;// column:title_pinyin_key
    //#69060 modify end by bin.dong
    public static final String COLUMN_ARTIST = "album_artist";
    private static final String LOG_TAG = "FileCategoryHelper";
    public static final String SORT_INDEX_KEY = "sort_key";
    public static final String SORT_NAME_NOINDEX_KEY = "sort_key2";
    private FileViewInteractionHub mFileViewInteractionHub;

    public enum FileCategory {
        All, Music, Video, Picture, Theme, Doc, Zip, Apk, Custom, Other, Favorite
    }

    private static String APK_EXT = "apk";
    private static String THEME_EXT = "lwt";
    private static String[] ZIP_EXTS = new String[] { "zip", "rar" };

    public static HashMap<FileCategory, FilenameExtFilter> filters = new HashMap<FileCategory, FilenameExtFilter>();

    public static HashMap<FileCategory, Integer> categoryNames = new HashMap<FileCategory, Integer>();

    static {
        // categoryNames.put(FileCategory.All, R.string.category_all);
        categoryNames.put(FileCategory.Music, R.string.category_music);
        categoryNames.put(FileCategory.Video, R.string.category_video);
        categoryNames.put(FileCategory.Picture, R.string.category_picture);
        categoryNames.put(FileCategory.Theme, R.string.category_theme);
        categoryNames.put(FileCategory.Doc, R.string.category_document);
        // categoryNames.put(FileCategory.Zip, R.string.category_zip);
        categoryNames.put(FileCategory.Apk, R.string.category_apk);
        // categoryNames.put(FileCategory.Other, R.string.category_other);
        // categoryNames.put(FileCategory.Favorite, R.string.category_favorite);
    }

    public static FileCategory[] sCategories = new FileCategory[] {
            FileCategory.Music, FileCategory.Picture, FileCategory.Video,
            FileCategory.Doc, FileCategory.Apk, FileCategory.Theme,
            FileCategory.Other
    // FileCategory.Doc, FileCategory.Zip, FileCategory.Apk, FileCategory.Other
    // FileCategory.Music, FileCategory.Video, FileCategory.Picture,
    // FileCategory.Theme,
    // FileCategory.Doc, FileCategory.Zip, FileCategory.Apk, FileCategory.Other
    };

    private FileCategory mCategory;

    private Context mContext;

    public FileCategoryHelper(Context context,
            FileViewInteractionHub fileViewInteractionHub) {
        mContext = context;

        mCategory = FileCategory.All;
        mFileViewInteractionHub = fileViewInteractionHub;
    }

    public FileCategory getCurCategory() {
        return mCategory;
    }

    public void setCurCategory(FileCategory c) {
        mCategory = c;
    }

    public int getCurCategoryNameResId() {
        return categoryNames.get(mCategory);
    }

    public void setCustomCategory(String[] exts) {
        mCategory = FileCategory.Custom;
        if (filters.containsKey(FileCategory.Custom)) {
            filters.remove(FileCategory.Custom);
        }

        filters.put(FileCategory.Custom, new FilenameExtFilter(exts));
    }

    public FilenameFilter getFilter() {
        return filters.get(mCategory);
    }

    private HashMap<FileCategory, CategoryInfo> mCategoryInfo = new HashMap<FileCategory, CategoryInfo>();

    public HashMap<FileCategory, CategoryInfo> getCategoryInfos() {
        return mCategoryInfo;
    }

    public CategoryInfo getCategoryInfo(FileCategory fc) {
        if (mCategoryInfo.containsKey(fc)) {
            return mCategoryInfo.get(fc);
        } else {
            CategoryInfo info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
            return info;
        }
    }

    public class CategoryInfo {
        public long count;

        public long size;

        public EachSDInfo[] sdInfo;

        // 0: internal sdcard; 1: external sdcard; other values: all sdcard.
        public int sdcardIndex;

        public class EachSDInfo {
            public long count_sd;

            public long size_sd;
        }

        public CategoryInfo() {
            sdInfo = new EachSDInfo[2];
            sdInfo[0] = new EachSDInfo();
            sdInfo[1] = new EachSDInfo();
        }

        public void setCategorySDCardIndex(int sdIndex) {
            sdcardIndex = sdIndex;
        }

        public int getCategorySDCardIndex() {
            return sdcardIndex;
        }
    }

    private void setCategoryInfo(FileCategory fc, long count, long size) {
        CategoryInfo info = mCategoryInfo.get(fc);
        if (info == null) {
            info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
        }
        info.count = count;
        info.size = size;
    }

    /**
     * 
     * @param fc
     * @param sdIndex
     *            : 0:internal sdcard; 1:external sdcard.
     * @param count
     * @param size
     */
    private void setCategoryInfo(FileCategory fc, int sdIndex, long count,
            long size) {
        CategoryInfo info = mCategoryInfo.get(fc);
        if (info == null) {
            info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
        }
        info.sdcardIndex = sdIndex;
        if (sdIndex >= 0 && sdIndex < 2) {
            info.sdInfo[sdIndex].count_sd = count;
            info.sdInfo[sdIndex].size_sd = size;
        }
        info.count = 0;
        info.size = 0;
        for (int i = 0; i < 2; i++) {
            info.count += info.sdInfo[i].count_sd;
            info.size += info.sdInfo[i].size_sd;
        }
    }

    private String buildDocSelection() {
        StringBuilder selection = new StringBuilder();
        Iterator<String> iter = Util.sDocMimeTypesSet.iterator();
        selection.append("(");
        while (iter.hasNext()) {
            selection.append("(" + FileColumns.MIME_TYPE + "=='" + iter.next()
                    + "') OR ");
        }
        return selection.substring(0, selection.lastIndexOf(")") + 1) + ")"
                + " and _size > 0";
    }

    private String buildSelectionByCategory(FileCategory cat) {
        String selection = null;
        float filterPicSize = FileManagerPreferenceActivity
                .getPicFilter(mContext) * 1024;
        String hideSysFolder = " and format<>12289 and " + FileColumns.DATA
                + " not like '%/.%' ";
        switch (cat) {
        case Theme:
            //#943625 modify begin by bin.dong
            selection = "((" + FileColumns.MIME_TYPE
                    + " = 'application/lewa-theme') or ("
                    + FileColumns.MIME_TYPE
                    + " = 'vnd.lewa.cursor.dir/theme') or ("
                    + FileColumns.DATA
                    + " LIKE '%.lwt'     ))" + hideSysFolder;
          //#943625 modify end by bin.dong
            break;
        case Doc:
            selection = buildDocSelection() + hideSysFolder;
            break;
        case Zip:
            selection = "(" + FileColumns.MIME_TYPE + " == '"
                    + Util.sZipFileMimeType + "')" + hideSysFolder;
            break;
        case Apk:
            selection = FileColumns.DATA + " LIKE '%.apk'" + hideSysFolder;
            break;
        case Picture:
            selection = " ((mime_type  is 'image/png')  or (mime_type  is 'image/jpeg' )  or  (mime_type  is 'image/gif') or (mime_type  is 'image/x-ms-bmp' ))"
                    + " and "
                    + FileColumns.SIZE
                    + " > "
                    + filterPicSize
                    + hideSysFolder;
            break;
        case Video:
            selection = "(" + FileColumns.MIME_TYPE + " LIKE 'video/%'"   
                    //#944384 add begin by bin.dong
                    +" and " + AudioColumns.DURATION + " > 0 " + ")"
                    + hideSysFolder;
                    //#944384 add end by bin.dong
           
            break;

        case Music:
            selection = "(" + FileColumns.MIME_TYPE + " LIKE 'audio/%'"
                    + " and " + FileColumns.SIZE + " > " + filterPicSize
                    /*+ " and is_music is not null "*/ + ")" + hideSysFolder;
            break;
        default:
            selection = null;
        }
        return selection;
    }

    /**
     * 
     * @param index
     *            : 0:internal sdcard; 1:external sdcard.
     * @return
     */
    public static int getStorageId(int index) {
        // storage ID is 0x00010001 for primary storage,
        // then 0x00020001, 0x00030001, etc. for secondary storages
        return ((index + 1) << 16) + 1;
    }

    /**
     * 
     * @param cat
     *            : category
     * @param index
     *            : 0:internal sdcard; 1:external sdcard.
     * @return
     */
    private String buildSelectionByCategoryAndStorageId(FileCategory cat,
            int index) {
        StringBuilder selection = new StringBuilder();
        float filterPicSize = FileManagerPreferenceActivity
                .getPicFilter(mContext) * 1024;
        String hideSysFolder = " and format<>12289 and " + FileColumns.DATA
                + " not like '%/.%'";
        boolean needAddCondition = false;
        switch (cat) {
        case Theme:
            selection = selection.append("((").append(FileColumns.MIME_TYPE)
                    .append(" = 'application/lewa-theme'").append(")")
                    .append(" or (").append(FileColumns.MIME_TYPE)
                    .append(" = 'vnd.lewa.cursor.dir/theme'")              
                    //#943625 add begin by bin.dong
                    .append(")")
                    .append(" or (").append(FileColumns.DATA)
                    .append(" LIKE '%.lwt'")
                    //#943625 add end by bin.dong  
                    .append("))")
                    .append(hideSysFolder);
            needAddCondition = true;
            break;
        case Doc:
            selection = selection.append(buildDocSelection()).append(
                    hideSysFolder);
            needAddCondition = true;
            break;
        case Zip:
            selection = selection.append('(').append(FileColumns.MIME_TYPE)
                    .append(" == '").append(Util.sZipFileMimeType).append("')")
                    .append(hideSysFolder);
            needAddCondition = true;
            break;
        case Apk:
            selection = selection.append(FileColumns.DATA)
                    .append(" LIKE '%.apk'").append(hideSysFolder);
            needAddCondition = true;
            break;
        case Music:
            selection = selection.append(FileColumns.MIME_TYPE)
                    .append(" LIKE 'audio/%'")
                    .append(" and " + FileColumns.SIZE).append(" > 0")
                    /*.append(" and is_music is not null ")*/.append(hideSysFolder);
            needAddCondition = true;
            break;
        case Video:
            selection = selection.append(FileColumns.MIME_TYPE)
                    .append(" LIKE 'video/%'")
                    //#944384 add begin by bin.dong
                    .append(" and ").append(AudioColumns.DURATION).append(" > 0 ")
                    //#944384 add end by bin.dong
                    .append(hideSysFolder);
            needAddCondition = true;
            break;
        case Picture:
            selection = selection
                    .append(" ((mime_type  is 'image/png')  or (mime_type  is 'image/jpeg')  or  (mime_type  is 'image/gif') or (mime_type  is 'image/x-ms-bmp'))")
                    .append(" and " + FileColumns.SIZE)
                    .append(" > " + filterPicSize).append(hideSysFolder);
            needAddCondition = true;
            break;

        default:

            break;
        }

        if (needAddCondition) {
            if (selection.length() > 0) {
                selection.insert(0, '(').append(") AND ");
            }
            selection.append('(').append(FileColumns.STORAGE_ID).append("==")
                    .append(getStorageId(index)).append(')');
        }
        return selection.toString();
    }

    /**
     * 
     * @param cat
     *            : category
     * @param index
     *            : 0:internal sdcard; 1:external sdcard.
     * @return
     */
    private String buildSpecifiedSelectionByCateAndStorId(FileCategory cat,
            int index) {
        StringBuilder selection = new StringBuilder();
        String hideSysFolder = " and " + FileColumns.DATA + " not like '%/.%'";
        boolean needAddCondition = false;
        switch (cat) {
        case Picture:
            selection = selection.append(FileColumns.MIME_TYPE)
                    .append(" LIKE 'image/%'")
                    .append(" and " + FileColumns.SIZE).append(" > 0")
                    .append(hideSysFolder);
            needAddCondition = true;
            break;

        default:

            break;
        }

        if (needAddCondition) {
            if (selection.length() > 0) {
                selection.insert(0, '(').append(") AND ");
            }
            selection.append('(').append(FileColumns.STORAGE_ID).append("==")
                    .append(getStorageId(index)).append(')');
        }
        return selection.toString();
    }

    private Uri getContentUriByCategory(FileCategory cat) {
        Uri uri;
        String volumeName = "external";
        switch (cat) {
        case Theme:
        case Doc:
        case Zip:
        case Apk:
        case Music:
            // uri = Audio.Media.getContentUri(volumeName);
        case Video:
        case Picture:
            uri = Files.getContentUri(volumeName);
            break;
        /*
         * case Music: uri = Audio.Media.getContentUri(volumeName); break; case
         * Video: uri = Video.Media.getContentUri(volumeName); break; case
         * Picture: uri = Images.Media.getContentUri(volumeName); uri =
         * Files.getContentUri(volumeName); break;
         */
        default:
            uri = null;
        }
        return uri;
    }

    private String buildSortOrder(SortMethod sort, boolean isMusic) {
        String sortOrder = null;
        switch (sort) {
        case name:
            if (isMusic) {
                sortOrder = FileColumns.DISPLAY_NAME + " asc, "
                        + COLUMN_CH_NAME + " asc, " + FileColumns.TITLE
                        + " asc ";
            } else {
                if (((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
                    sortOrder = "sort_key collate NOCASE asc";
                } else {
                    // sortOrder = FileColumns.TITLE + " collate NOCASE asc, " +
                    // COLUMN_CH_NAME + " collate NOCASE asc ";
                    sortOrder = "sort_key2 collate NOCASE asc, "
                            + COLUMN_CH_NAME + " collate NOCASE asc ";
                }
            }
            break;
        case size:
            sortOrder = FileColumns.SIZE + " asc";
            break;
        case date:
            sortOrder = FileColumns.DATE_MODIFIED + " desc";
            break;
        case type:
            sortOrder = FileColumns.MIME_TYPE + " collate NOCASE asc, "
                    + FileColumns.TITLE + " collate NOCASE asc";
            break;
        }
        return sortOrder;
    }

    public Cursor query(FileCategory fc, SortMethod sort) {
        // Log.d("yx", "cateHelper -> query");
        Uri uri = getContentUriByCategory(fc);
        String selection = buildSelectionByCategory(fc);
        String sortOrder = null;
        String[] columns = null;
        //#69060 modify begin by bin.dong
        String mixEngChinese = "(CASE  WHEN  title_key/*title_pinyin_key*/ is null THEN ('.'||title) ELSE title_key/*title_pinyin_key*/ END) as sort_key";
        String splitEngChinese = "(CASE  WHEN  title_key/*title_pinyin_key*/ is not null THEN ('啊') ELSE title END) as sort_key2";
        String sortMusic = "(CASE  WHEN  _display_name is not null THEN _display_name ELSE title END) as sort_music";
        //#69060 modify end by bin.dong
        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        if (fc == FileCategory.Music) { // Only Music sorted by display name.
            sortOrder = buildSortOrder(sort, true);
            if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            } else if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() != FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            } else {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            }
            // selection = selection + " and title <> ''";
        } else {
            sortOrder = buildSortOrder(sort, false);
            if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST, mixEngChinese };
            } else if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() != FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST, splitEngChinese };
            } else {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST };
            }
        }

        try {
            return mContext.getContentResolver().query(uri, columns, selection,
                    null, sortOrder);
        } catch (SQLiteDiskIOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 
     * @param fc
     * @param sort
     * @param sdindex
     *            : 0:internal sdcard; 1:external sdcard.
     * @return
     */
    public Cursor query(FileCategory fc, SortMethod sort, int sdindex) {
        // Log.d("yx", "cateHelper -> query2");
        Uri uri = getContentUriByCategory(fc);
        String selection = buildSelectionByCategoryAndStorageId(fc, sdindex);
        String sortOrder = null;
        String[] columns = null;
        String mixEngChinese = "(CASE  WHEN  title_key/*title_pinyin_key*/ is null THEN ('.'||title) ELSE title_key/*title_pinyin_key*/ END) as sort_key";
        String splitEngChinese = "(CASE  WHEN  title_key/*title_pinyin_key*/ is not null THEN ('啊') ELSE title END) as sort_key2";
        String sortMusic = "(CASE  WHEN  _display_name is not null THEN _display_name ELSE title END) as sort_music";

        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        if (fc == FileCategory.Music) {
            sortOrder = buildSortOrder(sort, true); // Only Music sorted by
                                                    // display name.
            if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            } else if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() != FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            } else {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, FileColumns.DISPLAY_NAME,
                        COLUMN_ARTIST, sortMusic };
            }
        } else {
            sortOrder = buildSortOrder(sort, false);
            if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST, mixEngChinese };
            } else if (sort == SortMethod.name
                    && ((FileManagerMainActivity) mContext).getFileListIndex() != FileListIndex.Name) {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST, splitEngChinese };
            } else {
                columns = new String[] { FileColumns._ID, FileColumns.DATA,
                        FileColumns.SIZE, FileColumns.DATE_MODIFIED,
                        COLUMN_CH_NAME, COLUMN_ARTIST };
            }
        }

        try {
            return mContext.getContentResolver().query(uri, columns, selection,
                    null, sortOrder);
        } catch (SQLiteDiskIOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void refreshCategoryInfo() {
        // clear
        for (FileCategory fc : sCategories) {
            setCategoryInfo(fc, 0, 0);
        }

        // query database
        String volumeName = "external";

        Uri uri = Files.getContentUri(volumeName);
        // Uri uri = Audio.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Music, uri);

        // uri = Video.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Video, uri);

        // uri = Images.Media.getContentUri(volumeName);
        // uri = Files.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Picture, uri);

        //
        refreshMediaCategory(FileCategory.Theme, uri);
        refreshMediaCategory(FileCategory.Doc, uri);
        refreshMediaCategory(FileCategory.Apk, uri);
    }

    private boolean refreshMediaCategory(FileCategory fc, Uri uri) {
        String[] columns = new String[] { "COUNT(*)", "SUM(_size)" };
        Cursor c1 = null;
        // get for sdcard 1;
        try {
            if (fc == FileCategory.Picture) {
                c1 = mContext.getContentResolver().query(uri, columns,
                        buildSpecifiedSelectionByCateAndStorId(fc, 0), null,
                        null);
            } else {
                c1 = mContext.getContentResolver()
                        .query(uri, columns,
                                buildSelectionByCategoryAndStorageId(fc, 0),
                                null, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (c1 == null) {
            Log.e("fM", "fail to query uri:" + uri);
            return false;
        }

        if (c1.moveToNext()) {
            setCategoryInfo(fc, 0, c1.getLong(0), c1.getLong(1));
            c1.close();
        }
        // get for sdcard 2;
        Cursor c2 = null;
        try {
            if (fc == FileCategory.Picture) {
                c2 = mContext.getContentResolver().query(uri, columns,
                        buildSpecifiedSelectionByCateAndStorId(fc, 1), null,
                        null);
            } else {
                c2 = mContext.getContentResolver()
                        .query(uri, columns,
                                buildSelectionByCategoryAndStorageId(fc, 1),
                                null, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (c2 == null) {
            Log.e("fM", "fail to query uri:" + uri);
            return false;
        }

        if (c2.moveToNext()) {
            setCategoryInfo(fc, 1, c2.getLong(0), c2.getLong(1));
            c2.close();
            return true;
        }
        // This line is for show all files including 2 cards.
        CategoryInfo info = mCategoryInfo.get(fc);
        info.sdcardIndex = 2;
        return false;
    }

    public static FileCategory getCategoryFromPath(String path) {
        MediaFileType type = MediaFile.getFileType(path);
        if (type != null) {
            if (MediaFile.isAudioFileType(type.fileType))
                return FileCategory.Music;
            if (MediaFile.isVideoFileType(type.fileType))
                return FileCategory.Video;
            if (MediaFile.isImageFileType(type.fileType))
                return FileCategory.Picture;
            if (Util.sDocMimeTypesSet.contains(type.mimeType))
                return FileCategory.Doc;
        }

        int dotPosition = path.lastIndexOf('.');
        if (dotPosition < 0) {
            return FileCategory.Other;
        }

        String ext = path.substring(dotPosition + 1);
        if (ext.equalsIgnoreCase(APK_EXT)) {
            return FileCategory.Apk;
        }
        if (ext.equalsIgnoreCase(THEME_EXT)) {
            return FileCategory.Theme;
        }

        if (matchExts(ext, ZIP_EXTS)) {
            return FileCategory.Zip;
        }

        return FileCategory.Other;
    }

    private static boolean matchExts(String ext, String[] exts) {
        for (String ex : exts) {
            if (ex.equalsIgnoreCase(ext))
                return true;
        }
        return false;
    }

}
