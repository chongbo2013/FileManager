package com.lewa.filemanager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class FileOperationHelper {
    private static final String LOG_TAG = "FileOperation";

    private ArrayList<FileInfo> mCurFileNameList = new ArrayList<FileInfo>();

    private boolean mMoving;

    private IOperationProgressListener mOperationListener;

    private FilenameFilter mFilter;

    public interface IOperationProgressListener {
        void onFinish();

        void onFileChanged(String path);
    }

    public FileOperationHelper(IOperationProgressListener l) {
        mOperationListener = l;
    }

    public void setFilenameFilter(FilenameFilter f) {
        mFilter = f;
    }


    /*
     *
     * -1: create failed:bad file name 0: create success 1: create failed:file
     * already exits 2: name too long
     */
    public int CreateFolder(FileViewInteractionHub fileViewInteractionHub,String path, String name) {    
        if(name.equals(".") || name.equals("..") || (name.indexOf("//")!= -1)
                ||name.equals("/") || name.endsWith("/") || name.startsWith("/") || name.contains(" ")){
            return -1;
        }

        if (name.length() > GlobalConsts.MAX_FILE_NAME_LEN) {
            return 2;
        }

        File f = new File(Util.makePath(path, name));
        if (f.exists()){
            return 1;
        }
        int istrue = 0;
        // #69531 modify begin by bin.dong
        /*
         * if(isSDCardPath(path)){ 
         * fileViewInteractionHub.getFileViewFragmentObj().mkDir(path,name); }
         * else{
         */
        try {
            f.canWrite();
            istrue = f.mkdirs() ? 0 : -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // }
        // #943494 add begin by bin.dong
        fileViewInteractionHub.notifyFileSystemChanged(f.getAbsolutePath());
        //#943494 add end by bin.dong
        //#69531 modify end by bin.dong
        return istrue;
    }
    private boolean isSDCardPath(String path){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
        .equals(android.os.Environment.MEDIA_MOUNTED); 
        if (sdCardExist){
            sdDir = Environment.getExternalStorageDirectory();
        }
        if(!path.contains(sdDir.toString()))
            return true;
        return false;
    }
    public void Copy(ArrayList<FileInfo> files) {
        copyFileList(files);
    }

    public boolean Paste(final FileViewInteractionHub fileViewInteractionHub,String path, final String rootPath) {
        if (mCurFileNameList.size() == 0)
            return false;

        final String _path = path;
        /*boolean isPasterToSdCard = false;
        if (isSDCardPath(_path))
            isPasterToSdCard = true;*/
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                for (FileInfo f : mCurFileNameList) {
                    /*#70478 modify begin by bin.dong
                    if (isSDCardPath(_path)) {
                        fileViewInteractionHub.getFileViewFragmentObj().copyFileToSDCard(f);
                    } else {*/
                        if (!CopyFile(f, _path)) {
                            //#955135 add begin bu bin.dong
                            Context mContext = ((FileManagerMainActivity) mOperationListener).getContext();
                            fileViewInteractionHub.setToastHint(mContext.getString(R.string.copy_file_fail));
                            //#955135 add end by bin.dong
                            break;
                        }                                               
                    //}
                }
				for (FileInfo file : mCurFileNameList) {
                            mOperationListener.onFileChanged(_path + File.separator + file.fileName);
                        }
                clear();
            }
        });
        /*if(isPasterToSdCard)
            return false;*/
      //#70478 modify end by bin.dong
        return true;
    }

    public boolean canPaste() {
//    	Log.d("yx", "OperHelper -> canPaste  size: " + mCurFileNameList.size());
        return mCurFileNameList.size() != 0;
    }

    public void StartMove(ArrayList<FileInfo> files) {
        if (mMoving)
            return;

        mMoving = true;
        copyFileList(files);
    }

    public boolean isMoveState() {
//    	Log.d("yx", "OperHelper -> isMoveState: " + mMoving);
        return mMoving;
    }

    public boolean canMove(String path) {
        for (FileInfo f : mCurFileNameList) {
            if (!f.IsDir)
                continue;

            if (Util.containsPath(f.filePath, path))
                return false;
        }

        return true;
    }

    public void clear() {
        synchronized (mCurFileNameList) {
            mCurFileNameList.clear();
        }
    }

    public boolean EndMove(final FileViewInteractionHub fileViewInteractionHub,String path, final String rootPath) {
        if (!mMoving)
            return false;
        mMoving = false;

        if (TextUtils.isEmpty(path))
            return false;

        for (FileInfo f : mCurFileNameList) {
            //bug fix #67329 by fan.yang
            if (path.startsWith(f.filePath)) {
                return false;
            }
            if (isSubFile(path, f.filePath)) {
                clear();
                return false;
            }
        }

        final String _path = path;
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                for (FileInfo f : mCurFileNameList) {
                    if (!MoveFile(fileViewInteractionHub,f, _path)) {
                        break;
                    }
                }
                for (FileInfo f : mCurFileNameList) {
                    mOperationListener.onFileChanged(_path +File.separator+ f.fileName);
                }
                clear();
            }
        });

        /*if(isSDCardPath(_path))
            return false;*/
        
        return true;
    }

    private boolean isSubFile(String root, String target) {
        File rootFile = new File(root);
        File targetFile = new File(target);
        if (!rootFile.isDirectory() || !targetFile.isDirectory()) {
            return false;
        }

        String[] source = root.split("/");
        String[] destination = target.split("/");
        int size = source.length;
        if (destination.length <= size) {
            for (int i = 0; i < size; i++) {
                if (!destination[i].equals(source[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public ArrayList<FileInfo> getFileList() {
        return mCurFileNameList;
    }

    private void asnycExecute(Runnable r) {
        final Runnable _r = r;
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                synchronized (mCurFileNameList) {
                    _r.run();
                }
                if (mOperationListener != null) {
                    mOperationListener.onFinish();
                }
//                Toast.makeText(mOperationListener, "onfinish Done.", Toast.LENGTH_SHORT).show();
                return null;
            }
        }.execute();
    }

    public boolean isFileSelected(String path) {
        synchronized (mCurFileNameList) {
            for (FileInfo f : mCurFileNameList) {
                if (f.filePath.equalsIgnoreCase(path))
                    return true;
            }
        }
        return false;
    }

    public boolean Rename(FileInfo f, String newName) {
        if (f == null || newName == null) {
            Log.e(LOG_TAG, "Rename: null parameter");
            return false;
        }

        File file = new File(f.filePath);
        String newPath = Util.makePath(Util.getPathFromFilepath(f.filePath), newName);
        final boolean needScan = file.isFile();
        try {
            boolean ret;
            if (file.renameTo(new File(newPath))) ret = true;
            else ret = false;
            if (ret) {
                if (needScan) {
                    // #69406 modify begin by bin.dong
                    mOperationListener.onFileChanged((new File(newPath))/*.toString()*/.getParent());
                    // #69406 modify end by bin.dong
                }
                //#938617 add begin by bin.dong
                mOperationListener.onFileChanged(f.filePath);
              //#938617 add end by bin.dong
                mOperationListener.onFileChanged(newPath);
                //#944100 add begin by bin.dong
                if((new File(newPath)).isHidden()){
                    Context mContext = ((FileManagerMainActivity) mOperationListener).getContext();
                    Toast.makeText(mContext, mContext.getString(R.string.modify_to_hiddenfile), Toast.LENGTH_SHORT)
                              .show();
                }
                //#944100 add end by bin.dong
            }
            return ret;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Fail to rename file," + e.toString());
        }
        return false;
    }

    public boolean Delete(final FileViewInteractionHub mfileViewInteractionHub,ArrayList<FileInfo> files, final String path) {
        copyFileList(files);
        asnycExecute(new Runnable() {
            @Override
            public void run() {
                for (FileInfo f : mCurFileNameList) {
                    /*if (isSDCardPath(f.filePath)) {
                        mfileViewInteractionHub.getFileViewFragmentObj().deleteFileFromSDCard(f);                                 
                    } else {*/
                        DeleteFile(f);
                        mOperationListener.onFileChanged(/*path*/f.filePath);
                    //}
                }
               // mOperationListener.onFileChanged(path);
                clear();
            }
        });  
        return true;
    }

    protected void DeleteFile(FileInfo f) {
        if (f == null) {
            Log.e(LOG_TAG, "DeleteFile: null parameter");
            return;
        }

        File file = new File(f.filePath);
//        Log.d("yx", "OperHelper -> Delete(f) -> f.filePath: " + f.filePath);
        boolean directory = file.isDirectory();
        if (directory) {
            for (File child : file.listFiles(mFilter)) {
                if (Util.isNormalFile(child.getAbsolutePath())) {
                    DeleteFile(Util.GetFileInfo(child, mFilter, true));
                }
            }
        }

        if (file.delete()) { //to improve the speed for deleting files, add notifyFileRemove here.
            Util.notifyFileRemoveDB(((FileManagerMainActivity) mOperationListener).getContext(), f.filePath);
        }
    }

    private boolean CopyFile(FileInfo f, String dest) {
        if (f == null || dest == null) {
            Log.e(LOG_TAG, "CopyFile: null parameter");
            return false;
        }
//        Log.d("yx", "OperHelper -> CopyFile -> sourcePath: " + f.filePath + ", destPath: " + dest.toString());
        File file = new File(f.filePath);
        if (file.isDirectory()) {

            // directory exists in destination, rename it
            String destPath = Util.makePath(dest, f.fileName);
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                destPath = Util.makePath(dest, f.fileName + "_" + i++);
                destFile = new File(destPath);
            }

            if (0 == file.list().length) {//if source folder is empty.
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                for (File child : file.listFiles(mFilter)) {
                    if(destPath == null){
                        return false;
                    }
                    if (!child.isHidden() && Util.isNormalFile(child.getAbsolutePath())) {
                        CopyFile(Util.GetFileInfo(child, mFilter, Settings.instance().getShowDotAndHiddenFiles()), destPath);
                    }
                }
            }
        } else {
            String destFile = null;
            if (dest != null) {
                destFile = Util.copyFile(
                        (FileManagerMainActivity) mOperationListener,
                        f.filePath, dest);
            }
            if (destFile != null && destFile.equals("false"))
                return false;
            //#955135 add begin bu bin.dong
            if(destFile == null)
                return false;
            //#955135 add end bu bin.dong
            mOperationListener.onFileChanged(destFile);
        }
        return true;
    }

    /**
     * renameTo: Both paths be on the same mount point.
     * On Android, applications are most likely to hit this restriction
     * when attempting to copy between internal storage and an SD card.
     *
     * @param f:    source file
     * @param dest: destination path
     * @return
     */
    private boolean MoveFile(FileViewInteractionHub fileViewInteractionHub,final FileInfo f, final String dest) {
        Log.e(LOG_TAG, "OperHelpler -> MoveFile f.path: " + f.filePath + ", destPath: " + dest);
        if (f == null || dest == null) {
            Log.e(LOG_TAG, "CopyFile: null parameter");
            return false;
        }
        Boolean noNeedCopyDelete = true; //Don't delete by default.
        String[] source = f.filePath.split("/");
        String[] destination = dest.split("/");
        for (int i = 0; i <= 2; i++) {
            noNeedCopyDelete = noNeedCopyDelete && (source[i].toString().equals(destination[i].toString()));
        }
        if (!noNeedCopyDelete) {
            try {
                /*
                 * #70478 modify begin by bin.dong if (isSDCardPath(dest)) {
                 * fileViewInteractionHub
                 * .getFileViewFragmentObj().moveFileToSDCard(f); } else {
                 */
                if (CopyFile(f, dest)) {
                    DeleteFile(f); // Then delete it.
                } else {
                    return false;
                }
                mOperationListener.onFileChanged(f.filePath);
                // }
            } catch (SecurityException e) {
                Log.e("yx", "Fail to move file," + e.toString());
            }
         // #70478 modify begin by bin.dong
        } else {
            File file = new File(f.filePath);
            String newPath = Util.makePath(dest, f.fileName);
            try {
                File newfile = new File(newPath);
                if (newfile.exists() && newfile.isDirectory()) {
                    return false;
                }
                boolean ret = file.renameTo(new File(newPath));
                Util.notifyFileRemoveBroadcast(((FileManagerMainActivity) mOperationListener).getContext(), newPath);
                return ret;
            } catch (SecurityException e) {
                Log.e(LOG_TAG, "Fail to move file," + e.toString());
            }
        }
        return true;
    }

    /*
     * if the old file path has existed
     * get the new file name
     * by jidongdong
     */
    private String GetNewFileName(String filename, int i) {
        String filepath = filename + "(" + i + ")";
        File file_temp = new File(filepath);
        if (file_temp.exists()) {
            return GetNewFileName(filename, ++i);
        } else {
            return filepath;
        }
    }

    private void copyFileList(ArrayList<FileInfo> files) {
        synchronized (mCurFileNameList) {
            mCurFileNameList.clear();
            for (FileInfo f : files) {
                mCurFileNameList.add(f);
            }
        }
    }

}
