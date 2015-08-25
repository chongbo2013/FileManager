package com.lewa.filemanager;


import com.lewa.filemanager.R;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;

import com.lewa.filemanager.FileCategoryHelper.FileCategory;
import com.lewa.filemanager.FileIconLoader.IconLoadFinishListener;

public class FileIconHelper implements IconLoadFinishListener {

    private static final String LOG_TAG = "FileIconHelper";

    private static HashMap<ImageView, ImageView> imageFrames = new HashMap<ImageView, ImageView>();

    private static HashMap<String, Integer> fileExtToIcons = new HashMap<String, Integer>();

    private FileIconLoader mIconLoader;

    static {
    	addItem(new String[] {
        		"mp3", "wma", "wav", "mid", "amr", "mmf", "imy", "m4a", "aac", "ogg", "smf"
        }, R.drawable.fic_music);
        addItem(new String[] {
                "mp4", "wmv", "mpeg", "m4v", "3gpp", "3g2", "3gpp2", "asf", "avi", "3gp", "mpg"
        }, R.drawable.fic_video);
        addItem(new String[] {
                "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
        }, R.drawable.fic_picture);
        addItem(new String[] {
                "txt", "log", "xml", "ini", "lrc","vcf"
        }, R.drawable.fic_doc);
        addItem(new String[] {
                "doc", "ppt", "docx", "pptx", "xls", "xlsx",
        }, R.drawable.fic_doc);
        addItem(new String[] {
            "pdf"
        }, R.drawable.fic_doc);
        addItem(new String[] {
            "zip"
        }, R.drawable.fic_zip);
        addItem(new String[] {
            "rar"
        }, R.drawable.fic_zip);
        addItem(new String[] {
                "lwt"
        }, R.drawable.fic_theme);
        addItem(new String[] {
                "apk"
        }, R.drawable.fic_apk);
    }

    public FileIconHelper(Context context) {
        mIconLoader = new FileIconLoader(context, this);
    }

    private static void addItem(String[] exts, int resId) {
        if (exts != null) {
            for (String ext : exts) {
                fileExtToIcons.put(ext.toLowerCase(), resId);
            }
        }
    }

    public static int getFileIcon(String ext) {
        Integer i = fileExtToIcons.get(ext.toLowerCase());
        if (i != null) {
            return i.intValue();
        } else {
            return R.drawable.fic_other;
        }

    }

    public void setIcon(FileInfo fileInfo, ImageView fileImage, ImageView fileImageFrame) {
        String filePath = fileInfo.filePath;
        long fileId = fileInfo.dbId;
        String extFromFilename = Util.getExtFromFilename(filePath);
        FileCategory fc = FileCategoryHelper.getCategoryFromPath(filePath);
        fileImageFrame.setVisibility(View.GONE);
        boolean set = false;
        int id = getFileIcon(extFromFilename);
        Util.setImageResource(fileImage, id, filePath);
//        fileImage.setImageResource(id);

        mIconLoader.cancelRequest(fileImage);
        switch (fc) {
            case Apk:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                break;          	
            case Music:
//            	set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
            	break;
            case Picture:
            	set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
            	if(set){
            		fileImageFrame.setVisibility(View.VISIBLE);
            	}else{
            		Util.setImageResource(fileImage, R.drawable.fic_picture, filePath);
//            		fileImage.setImageResource(R.drawable.fic_picture);
            		imageFrames.put(fileImage, fileImageFrame);
                    set = true;
            	}
            	break;
            case Video:
                set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
                if (set)
                    fileImageFrame.setVisibility(View.VISIBLE);
                else {
                	Util.setImageResource(fileImage, R.drawable.fic_video, filePath);
//                    fileImage.setImageResource(R.drawable.fic_video);
                    		//fc == FileCategory.Picture ? R.drawable.file_icon_picture: 
                    imageFrames.put(fileImage, fileImageFrame);
                    set = true;
                }
                break;
            case Theme:
//            	Log.d("yx", "IconHelper -> setIcon -> Theme");
            	set = mIconLoader.loadIcon(fileImage, filePath, fileId, fc);
            	break;
            default:
                set = true;
                break;
        }

        if (!set){
        	Util.setImageResource(fileImage, id, filePath);
//            fileImage.setImageResource(id);
        }
    }
    
    @Override
    public void onIconLoadFinished(ImageView view) {
        ImageView frame = imageFrames.get(view);
        if (frame != null) {
            frame.setVisibility(View.VISIBLE);
            imageFrames.remove(view);
        }
    }

}
