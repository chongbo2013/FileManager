package com.lewa.filemanager;

import java.io.File;
import java.util.HashMap;

import com.lewa.filemanager.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class InformationDialog extends AlertDialog {
    protected static final int ID_USER = 100;
    private FileInfo mFileInfo;
    private Context mContext;
    private View mView;
    private boolean mIsApk;
    private static HashMap<String, Integer> fileExtToIconsInfo = new HashMap<String, Integer>();
    static {
    	addItem(new String[] {
        		"mp3", "wma", "wav", "mid", "amr", "mmf", "imy", "m4a", "aac"
        }, R.drawable.infoic_music);
        addItem(new String[] {
                "mp4", "wmv", "mpeg", "m4v", "3gpp", "3g2", "3gpp2", "asf", "avi", "3gp"
        }, R.drawable.infoic_video);
        addItem(new String[] {
                "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
        }, R.drawable.infoic_picture);
        addItem(new String[] {
                "txt", "log", "xml", "ini", "lrc"
        }, R.drawable.infoic_doc);
        addItem(new String[] {
                "doc", "ppt", "docx", "pptx", "xsl", "xslx",
        }, R.drawable.infoic_doc);
        addItem(new String[] {
            "pdf"
        }, R.drawable.infoic_doc);
        addItem(new String[] {
            "zip"
        }, R.drawable.infoic_zip);
        addItem(new String[] {
            "rar"
        }, R.drawable.infoic_zip);
        addItem(new String[] {
                "lwt"
        }, R.drawable.infoic_theme);
        addItem(new String[] {
                "apk"
        }, R.drawable.infoic_apk);
    }
    private static void addItem(String[] exts, int resId) {
        if (exts != null) {
            for (String ext : exts) {
                fileExtToIconsInfo.put(ext.toLowerCase(), resId);
            }
        }
    }
    public InformationDialog(Context context, FileInfo f, FileIconHelper iconHelper, boolean isApk) {
        super(context);
        mFileInfo = f;
        mContext = context;
        mIsApk = isApk;
    }
    
    public static int getFileIcon(String ext) {
        Integer i = fileExtToIconsInfo.get(ext.toLowerCase());
        if (i != null) {
            return i.intValue();
        } else {
            return R.drawable.infoic_other;
        }

    }
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.information_dialog, null);
        String extFromFilename = Util.getExtFromFilename(mFileInfo.fileName);
        
        if (mFileInfo.IsDir) {
            //#69399 deleted by bin.dong 
            //setIcon(R.drawable.infoic_folder);
            asyncGetSize();
        } else {
            //#69399 deleted by bin.dong 
        	//int id = getFileIcon(extFromFilename);
        	//setIcon(id);
        }
        setTitle(mFileInfo.fileName);

        ((TextView) mView.findViewById(R.id.information_size))
                .setText(formatFileSizeString(mFileInfo.fileSize));
        ((TextView) mView.findViewById(R.id.information_location))
                .setText(mFileInfo.filePath);
        ((TextView) mView.findViewById(R.id.information_modified)).setText(Util
                .formatDateString(mContext, mFileInfo.ModifiedDate));
        ((TextView) mView.findViewById(R.id.information_canread))
                .setText(mFileInfo.canRead ? R.string.yes : R.string.no);
        ((TextView) mView.findViewById(R.id.information_canwrite))
                .setText(mFileInfo.canWrite ? R.string.yes : R.string.no);
        if(mIsApk){
        	ApkItem apkItem = new ApkItem(mContext, mFileInfo.filePath);
        	((TextView)mView.findViewById(R.id.information_status_row)).setVisibility(View.VISIBLE);
        	TextView view = (TextView) mView.findViewById(R.id.information_status);
        	view.setVisibility(View.VISIBLE);        	
            view.setText(apkItem.installInfo);
        }
        ((TextView) mView.findViewById(R.id.information_ishidden))
                .setText(mFileInfo.isHidden ? R.string.yes : R.string.no);

        setView(mView);
        setButton(BUTTON_NEGATIVE, mContext.getString(R.string.confirm_know), (DialogInterface.OnClickListener) null);

        super.onCreate(savedInstanceState);
    }

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_USER:
                    Bundle data = msg.getData();
                    long size = data.getLong("SIZE");
                    ((TextView) mView.findViewById(R.id.information_size)).setText(formatFileSizeString(size));
            }
        };
    };

    private AsyncTask task;

    @SuppressWarnings("unchecked")
    private void asyncGetSize() {
        task = new AsyncTask() {
            private long size;

            @Override
            protected Object doInBackground(Object... params) {
                String path = (String) params[0];
                size = 0;
                getSize(path);
                task = null;
                return null;
            }

            private void getSize(String path) {
                if (isCancelled())
                    return;
                File file = new File(path);
                if (file.isDirectory()) {
                    File[] listFiles = file.listFiles();
                    if (listFiles == null)
                        return;

                    for (File f : listFiles) {
                        if (isCancelled())
                            return;

                        getSize(f.getPath());
                    }
                } else {
                    size += file.length();
                    onSize(size);
                }
            }

        }.execute(mFileInfo.filePath);
    }

    private void onSize(final long size) {
        Message msg = new Message();
        msg.what = ID_USER;
        Bundle bd = new Bundle();
        bd.putLong("SIZE", size);
        msg.setData(bd);
        mHandler.sendMessage(msg); // 向Handler发送消息,更新UI
    }

    private String formatFileSizeString(long size) {
        String ret = "";
        if (size >= 1024) {
            ret = Util.convertStorage(size);
            ret += (" (" + mContext.getResources().getString(R.string.file_size, size) + ")");
        } else {
            ret = mContext.getResources().getString(R.string.file_size, size);
        }

        return ret;
    }
}
