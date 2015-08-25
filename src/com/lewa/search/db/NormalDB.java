package com.lewa.search.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.lewa.search.bean.NormalFileInfo;
import com.lewa.search.system.Constants;
import com.lewa.search.system.SoftCache;
import com.lewa.search.util.SearchUtil;
import com.lewa.search.util.StringUtil;

/**
 * This class defines a method for searching files(except for media files) from database.
 *
 * @author wangfan
 * @version 2012.07.04
 */

public class NormalDB {

    //uri for searching music files in database
    private static Uri uri = Constants.URI_NORMAL;
    //these thumbnails caches in softCache, load when system starts
    private static Drawable thumbnail_Doc;
    private static Drawable thumbnail_Theme;
    private static Drawable thumbnail_Apk;

    //add 2013-03-26
    private static Drawable thumbnail_dir;
    //this list contains results for return
    public static List<NormalFileInfo> list = new ArrayList<NormalFileInfo>();

    /**
     * This method search matched files
     *
     * @param key      key in searching music files
     * @param sortMode defines how to sort the list
     */
    public static List<NormalFileInfo> searchNormals(String key, String sortMode) {
        //load thumbnail
        thumbnail_Doc = SoftCache.otherThumb;
        thumbnail_Theme = SoftCache.themeThumb;
        thumbnail_Apk = SoftCache.apkThumb;
        thumbnail_dir = SoftCache.directoryThumb;

        //clear list before each time of searching
        list.clear();
        //this projection tells which column are to get from database
        String[] projection = new String[]{"_data"};
        //this selectinIds records which columns have searching clauses
        //in this case, index "PROJECTION_ZERO"("_data") has searching clause
        int[] selectionIds;

        selectionIds = new int[]{SearchUtil.PROJECTION_ZERO};

        //get selection
        String selection = SearchUtil.getSelection(projection, selectionIds, "OR", SearchUtil.SEARCH_MODE_BLURRED);
        //get selection arguments
        String[] selectionArgs = SearchUtil.getMultipleSelectionArgs(key, selectionIds.length, SearchUtil.SEARCH_MODE_BLURRED);

        //search matched results from database
        Cursor cur = DBUtil.searchByKey(uri, projection, selection, selectionArgs, sortMode);
        if (cur == null) {
            return null;
        }
        //create an empty item, it is a temp item
        NormalFileInfo normalItem;
        String text;
        String title;
        String filePath;

        //load thumbnails only at the first item of each fileType
        boolean loadApkThumb = true;
        boolean loadThemeThumb = true;
        boolean loadDocThumb = true;
        boolean loadDirectory = true;


        if (cur.moveToFirst()) {
            int index_Text = cur.getColumnIndex("_data");

            do {
                //get file information from search results
                filePath = cur.getString(index_Text);
                text = filePath;
                //get fileName and file extension from filePath
                //apk file and lwt file should be dealt with differently
                title = StringUtil.getFileName(text);

                String extension = StringUtil.getFileExtension(title);

                File tmpFile = new File(text);
                if (extension.equals("apk")) {
                    if (loadApkThumb == true)    //create new apk file item with thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, thumbnail_Apk, filePath, Constants.CLASS_NORMAL);
                            //only the first apk file item has thumbnail

                            //2013-03-28 All Items SHOW its thumbnail
                            //Hide the following code
                            //loadApkThumb = false;

                            list.add(normalItem);
                        }

                    } else    //create new apk file item without thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, null, filePath, Constants.CLASS_NORMAL);

                            list.add(normalItem);
                        }
                    }

                } else if (extension.equals("lwt"))    //create new lwt file item with thumbnail
                {
                    if (loadThemeThumb == true) {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, thumbnail_Theme, filePath, Constants.CLASS_NORMAL);
                            //only the first lwt file item has thumbnail

                            //2013-03-28 All Items SHOW its thumbnail
                            //Hide the following code
                            //loadThemeThumb = false;

                            list.add(normalItem);
                        }
                    } else    //create new lwt file item without thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, null, filePath, Constants.CLASS_NORMAL);

                            list.add(normalItem);
                        }
                    }
                } else if (tmpFile.isDirectory()) {
                    if (loadDirectory == true)    //create new doc file item with thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, thumbnail_dir, filePath, Constants.CLASS_NORMAL);
                            //only the first lwt file item has thumbnail

                            //2013-03-28 All Items SHOW its thumbnail
                            //Hide the following code
                            //loadDirectory = false;

                            list.add(normalItem);
                        }
                    } else    //create new doc file item without thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, null, filePath, Constants.CLASS_NORMAL);

                            list.add(normalItem);
                        }
                    }
                } else {
                    if (loadDocThumb == true)    //create new doc file item with thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, thumbnail_Doc, filePath, Constants.CLASS_NORMAL);
                            //only the first lwt file item has thumbnail

                            //2013-03-28 All Items SHOW its thumbnail
                            //Hide the following code
                            //loadDocThumb = false;

                            list.add(normalItem);
                        }
                    } else    //create new doc file item without thumbnail
                    {
                        if (title.toLowerCase().contains(key.toLowerCase())) {
                            normalItem = new NormalFileInfo(title, text, null, filePath, Constants.CLASS_NORMAL);

                            list.add(normalItem);
                        }
                    }
                }

            } while (cur.moveToNext());
        }
        if (cur != null) {
            cur.close();
        }

        return list;
    }
}
