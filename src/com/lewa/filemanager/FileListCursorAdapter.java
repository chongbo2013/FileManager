package com.lewa.filemanager;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import com.lewa.filemanager.FileCategoryFragment.FileListIndex;
import com.lewa.filemanager.FileCategoryHelper.FileCategory;
import com.lewa.filemanager.R;
import com.lewa.filemanager.ItemHighLightScope;

public class FileListCursorAdapter extends CursorAdapter {

    private final LayoutInflater mFactory;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileIconHelper mFileIcon;

    private HashMap<Integer, FileInfo> mFileNameList = new HashMap<Integer, FileInfo>();

    private Context mContext;
    private SortCursor mCursor = null;
    public FileListCursorAdapter(Context context, Cursor cursor,
            FileViewInteractionHub f, FileIconHelper fileIcon) {
        super(context, cursor, false /* auto-requery */);
        mFactory = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
        mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Log.d("yx", "bindView cursor.getPosition(): " +
        // cursor.getPosition());
        int pos = cursor.getPosition();
        FileInfo fileInfo = getFileItem(cursor.getPosition());
        if (fileInfo == null) {
            // file is not existing, create a fake info
            fileInfo = new FileInfo();
            fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
            fileInfo.filePath = cursor
                    .getString(FileCategoryHelper.COLUMN_PATH);
            fileInfo.fileName = Util.getNameFromFilepath(fileInfo.filePath);
            fileInfo.fileSize = cursor.getLong(FileCategoryHelper.COLUMN_SIZE);
            fileInfo.ModifiedDate = cursor
                    .getLong(FileCategoryHelper.COLUMN_DATE);
        }
        //
        switch (((FileManagerMainActivity) mContext).getFileListIndex()) {
            case NoIndex :
                setListNoIndex(view, context, cursor, fileInfo);
                break;
            case Name :
                // added by weihong
//                setListNameIndex(view, context, cursor, fileInfo, pos);
//                break;
            case Date :
                setListDateIndex(view, context, cursor, fileInfo, pos);
                break;
            case Size : // setListSizeIndex
                setListSizeIndex(view, context, cursor, fileInfo, pos);
                break;
            default :
                setListNoIndex(view, context, cursor, fileInfo);
        }

        FileListItem.setupFileListItemInfo(mContext, view, fileInfo, mFileIcon,
                mFileViewInteractionHub);
        view.findViewById(R.id.file_checkbox).setOnClickListener(
                new FileListItem.FileItemOnClickListener(mContext,
                        mFileViewInteractionHub));
    }

    @Override
    public Filter getFilter() {
        // TODO Auto-generated method stub
        return super.getFilter();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mFactory.inflate(R.layout.category_file_browser_item, parent,
                false);
    }

    @Override
    public View getView(int arg0, View arg1, ViewGroup arg2) {
        // TODO Auto-generated method stub
        return super.getView(arg0, arg1, arg2);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if (isMusicCategory()
                && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name) {
            mCursor = new SortCursor(cursor, "sort_music", true);
        } else if (isMusicCategory()
                && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.NoIndex) {
            mCursor = new SortCursor(cursor, "sort_music", false);
        }
        mFileNameList.clear();
        super.changeCursor(cursor);
    }

    public Collection<FileInfo> getAllFiles() {
        if (mFileNameList.size() == getCount())
            return mFileNameList.values();

        Cursor cursor = getCursor();
        mFileNameList.clear();

        if (cursor.moveToFirst()) {
            do {
                Integer position = Integer.valueOf(cursor.getPosition());
                if (mFileNameList.containsKey(position))
                    continue;
                FileInfo fileInfo = getFileInfo(cursor);

                if (fileInfo != null) {
                    // fileInfo.sort_key =
                    // cursor.getString(cursor.getColumnIndexOrThrow(FileCategoryHelper.SORT_INDEX_KEY));
                    mFileNameList.put(position, fileInfo);
                } else {
                    if (cursor != null && cursor.getCount() != 0) {
                        if (Util.getExtFromFilename(
                                cursor.getString(FileCategoryHelper.COLUMN_PATH))
                                .toLowerCase().equals("apk")
                                || Util.getExtFromFilename(
                                        cursor.getString(FileCategoryHelper.COLUMN_PATH))
                                        .toLowerCase().equals("lwt")) {
                            Util.notifyFileRemoveDB(mContext, cursor
                                    .getString(FileCategoryHelper.COLUMN_PATH));
                        } else {
                            Util.notifyFileRemoveBroadcast(mContext, cursor
                                    .getString(FileCategoryHelper.COLUMN_PATH));
                        }
                    }
                }
            } while (cursor.moveToNext());
        }

        return mFileNameList.values();
    }

    public FileInfo getFileItem(int pos) {
        Integer position = Integer.valueOf(pos);
        if (mFileNameList.containsKey(position))
            return mFileNameList.get(position);
        FileInfo fileInfo = null;

        if ((isMusicCategory() && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.Name)
                || (isMusicCategory() && ((FileManagerMainActivity) mContext).getFileListIndex() == FileListIndex.NoIndex)) {
            if (!mCursor.CursorIsNULL() && getCount() > pos) {
                mCursor.moveToPosition(pos);
                fileInfo = getFileInfo(pos, fileInfo, mCursor);
                if (fileInfo != null) {
                    fileInfo.sort_key = "." + mCursor.getKey();
                }
            }

        } else {
            if(getCount() > pos){
                Cursor cursor = (Cursor) getItem(pos);
                cursor.moveToPosition(pos);
                fileInfo = getFileInfo(pos, fileInfo, cursor);
            }
        }
        mFileNameList.put(position, fileInfo);
        return fileInfo;
    }

    private boolean isMusicCategory() {
        return (((FileCategoryFragment) mFileViewInteractionHub
                .getFileViewListener()).getFileCategoryHelper()
                .getCurCategory() == FileCategory.Music ? true : false);
    }

    private FileInfo getFileInfo(int pos, FileInfo fileInfo, Cursor cursor) {
        fileInfo = getFileInfo(cursor);
        if (fileInfo == null) {
            if (cursor != null && cursor.getCount() != 0) {
                if (Util.getExtFromFilename(
                        cursor.getString(FileCategoryHelper.COLUMN_PATH))
                        .toLowerCase().equals("apk")
                        || Util.getExtFromFilename(
                                cursor.getString(FileCategoryHelper.COLUMN_PATH))
                                .toLowerCase().equals("lwt")) {
                    Util.notifyFileRemoveDB(mContext,
                            cursor.getString(FileCategoryHelper.COLUMN_PATH));
                } else {
                    Util.notifyFileRemoveBroadcast(mContext,
                            cursor.getString(FileCategoryHelper.COLUMN_PATH));
                }
            }
            return null;
        }
        fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
        if (cursor.getColumnIndex(FileCategoryHelper.SORT_INDEX_KEY) > 0) {
            fileInfo.sort_key = cursor.getString(cursor
                    .getColumnIndexOrThrow(FileCategoryHelper.SORT_INDEX_KEY));
        }
        return fileInfo;
    }

    private FileInfo getFileInfo(Cursor cursor) {
        return (cursor == null || cursor.getCount() == 0) ? null : Util
                .GetFileInfo(cursor.getString(FileCategoryHelper.COLUMN_PATH));

    }

    private void setListNoIndex(View view, Context context, Cursor cursor,
            FileInfo fileInfo) {
        TextView index = (TextView) view.findViewById(R.id.date_header_text);
        index.setVisibility(View.GONE);
    }

    private void setListNameIndex(View view, Context context, Cursor cursor,
            FileInfo fileInfo, int pos) {
        TextView index = (TextView) view.findViewById(R.id.date_header_text);
        if (index != null) {
            String fileSortKey = fileInfo.sort_key;
            if (fileSortKey == null)
                return; // to cover the fc when null.
            if (pos == 0) {
                if (fileSortKey.substring(1, 2).toUpperCase().compareTo("A") < 0) {
                    index.setText(context.getString(R.string.name_index_head));
                } else if (fileSortKey.substring(1, 2).toUpperCase()
                        .compareTo("A") >= 0
                        && fileSortKey.substring(1, 2).toUpperCase()
                                .compareTo("Z") <= 0) {
                    index.setText(fileSortKey.substring(1, 2).toUpperCase());
                } else {
                    index.setText(context.getString(R.string.name_index_tail));
                }
                index.setVisibility(View.VISIBLE);

            } else if (pos > 0) {
                FileInfo prefileInfo = null;
                prefileInfo = getFileItem(pos - 1);
                if (prefileInfo != null && prefileInfo.sort_key != null) { // prefileInfo
                                                                           // !=
                                                                           // null
                                                                           // &&
                                                                           // prefileInfo.sort_key
                                                                           // !=
                                                                           // null
                                                                           // &&
                                                                           // fileInfo.sort_key
                                                                           // !=
                                                                           // null
                                                                           // &&
                    index.setVisibility(View.VISIBLE);
                    if (fileSortKey.substring(1, 2).toUpperCase()
                            .compareTo("A") >= 0
                            && fileSortKey.substring(1, 2).toUpperCase()
                                    .compareTo("Z") <= 0) {// &&
                                                           // prefileInfo.sort_key.substring(1,
                                                           // 2).toUpperCase().compareTo("A")
                                                           // < 0
                        if (!fileSortKey
                                .substring(1, 2)
                                .toUpperCase()
                                .equals(prefileInfo.sort_key.substring(1, 2)
                                        .toUpperCase())) {
                            index.setText(fileSortKey.substring(1, 2)
                                    .toUpperCase());
                        } else {
                            index.setVisibility(View.GONE);
                        }
                    } else if (fileSortKey.substring(1, 2).toUpperCase()
                            .compareTo("Z") > 0
                            && prefileInfo.sort_key.substring(1, 2)
                                    .toUpperCase().compareTo("Z") <= 0) {
                        index.setText(context
                                .getString(R.string.name_index_tail)); // Others
                    } else {
                        index.setVisibility(View.GONE);
                    }
                } else {
                    index.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setListSizeIndex(View view, Context context, Cursor cursor,
            FileInfo fileInfo, int pos) {
        // 大小排序由大至小排序，索引值：大于10MB，5MB – 10MB，1MB - 5MB，500KB - 1MB，小于500KB
        // 0-512000; 512000-1024000; 1024000-5242880; 5242880-10485760;
        // 10485760- ~
        TextView index = (TextView) view.findViewById(R.id.date_header_text);
        if (index != null) {
            // Log.d("yx", "CursorAdapter -> fileInfo.fileSize: " +
            // fileInfo.fileSize);
            // Log.d("yx",
            // "CursorAdapter -> fileInfo.fileSize cursor.getPosition(): " +
            // cursor.getPosition());
            long fileSize = fileInfo.fileSize;
            if (pos == 0) {
                if (fileSize < 512000) {
                    index.setText(context.getString(R.string.size_index_01)); //
                } else if (512000 <= fileSize && fileSize < 1024000) {
                    index.setText(context.getString(R.string.size_index_02));
                } else if (1024000 <= fileSize && fileSize < 5242880) {
                    index.setText(context.getString(R.string.size_index_03));
                } else if (5242880 <= fileSize && fileSize < 10485760) {
                    index.setText(context.getString(R.string.size_index_04));
                } else if (fileSize >= 10485760) {
                    index.setText(context.getString(R.string.size_index_05));
                }
                index.setVisibility(View.VISIBLE);
            } else if (pos > 0) {
                FileInfo prefileInfo = getFileItem(pos - 1);
                if (prefileInfo != null) {
                    long preFileSize = prefileInfo.fileSize;
                    index.setVisibility(View.VISIBLE);
                    // if(fileInfo.fileSize < 512000){
                    // index.setText(context.getString(R.string.size_index_01));
                    // //
                    // }else
                    if (512000 <= fileSize && fileSize < 1024000
                            && preFileSize < 512000) {
                        index.setText(context.getString(R.string.size_index_02));
                    } else if (1024000 <= fileSize && fileSize < 5242880
                            && preFileSize < 1024000) {
                        index.setText(context.getString(R.string.size_index_03));
                    } else if (5242880 <= fileSize && fileSize < 10485760
                            && preFileSize < 5242880) {
                        index.setText(context.getString(R.string.size_index_04));
                    } else if (fileSize >= 10485760 && preFileSize < 10485760) {
                        index.setText(context.getString(R.string.size_index_05));
                    } else {
                        index.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void setListDateIndex(View view, Context context, Cursor cursor,
            FileInfo fileInfo, int pos) {
        // 时间排序由近至远排序，索引值：今天，昨天，一周内，一个月内，更早
        TextView index = (TextView) view.findViewById(R.id.date_header_text);

        // SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date()); // 设置时间为当前时间
        Date now = ca.getTime();
        ca.set(now.getYear() + 1900, now.getMonth(), now.getDate(), 00, 00, 00);
        Date criticalValue = ca.getTime(); // point separate yesterday and today
        // Log.d("yx", "criticalValue: " + sf.format(criticalValue));

        ca.add(Calendar.DATE, -1);// point separate last week and yesterday
        Date lastDay = ca.getTime();
        // Log.d("yx", "now time: " + sf.format(now) + " last day: " +
        // sf.format(lastDay));

        ca.add(Calendar.DATE, -6);// point separate last week and yesterday
        Date lastWeek = ca.getTime();
        // Log.d("yx", "now time: " + sf.format(now) + " last week: " +
        // sf.format(lastWeek));

        // point separate last month and last week
        ca.set(criticalValue.getYear() + 1900, criticalValue.getMonth() - 1,
                criticalValue.getDate(), 00, 00, 00);
        Date lastMonth = ca.getTime();
        // Log.d("yx", "now time: " + sf.format(now) + " last month: " +
        // sf.format(lastMonth));
        // boolean compare = lastMonth.before(criticalValue);
        // Log.d("yx", "now time: " + sf.format(now) + "; lastMonth: " +
        // sf.format(lastMonth) + "; compare: " + compare);
        //
        if (index != null) {
            // Log.d("yx", "CursorAdapter -> fileInfo.ModifiedDate: " +
            // fileInfo.ModifiedDate);
            // Log.d("yx",
            // "CursorAdapter -> fileInfo.ModifiedDate cursor.getPosition(): " +
            // cursor.getPosition());
            long fileDate = fileInfo.ModifiedDate;
            if (pos == 0) {
                if (fileDate >= criticalValue.getTime()) { // Today
                    index.setText(context.getString(R.string.date_index_01));
                } else if (lastDay.getTime() <= fileDate
                        && fileDate < criticalValue.getTime()) {
                    index.setText(context.getString(R.string.date_index_02));
                } else if (lastWeek.getTime() <= fileDate
                        && fileDate < lastDay.getTime()) {
                    index.setText(context.getString(R.string.date_index_03));
                } else if (lastMonth.getTime() <= fileDate
                        && fileDate < lastWeek.getTime()) {
                    index.setText(context.getString(R.string.date_index_04));
                } else if (fileDate < lastMonth.getTime()) {
                    index.setText(context.getString(R.string.date_index_05));
                }
                index.setVisibility(View.VISIBLE);
            } else if (pos > 0) {
                FileInfo prefileInfo = getFileItem(pos - 1);
                if (prefileInfo != null) {
                    long preFileDate = prefileInfo.ModifiedDate;
                    index.setVisibility(View.VISIBLE);
                    if (lastDay.getTime() <= fileDate
                            && fileDate < criticalValue.getTime()
                            && preFileDate > criticalValue.getTime()) {
                        index.setText(context.getString(R.string.date_index_02));
                    } else if (lastWeek.getTime() <= fileDate
                            && fileDate < lastDay.getTime()
                            && preFileDate > lastDay.getTime()) {
                        index.setText(context.getString(R.string.date_index_03));
                    } else if (lastMonth.getTime() <= fileDate
                            && fileDate < lastWeek.getTime()
                            && preFileDate > lastWeek.getTime()) {
                        index.setText(context.getString(R.string.date_index_04));
                    } else if (fileDate < lastMonth.getTime()
                            && preFileDate > lastMonth.getTime()) {
                        index.setText(context.getString(R.string.date_index_05));
                    } else {
                        index.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}
