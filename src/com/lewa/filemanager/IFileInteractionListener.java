package com.lewa.filemanager;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;

import java.util.Collection;

public interface IFileInteractionListener {
    
	public View getViewById(int id);

    public Context getContext();

    public void startActivity(Intent intent);

    public void onDataChanged();

    public void onPick(FileInfo f);

    public boolean shouldShowOperationPane();

    /**
     * Handle operation listener.
     * @param id
     * @return true: indicate have operated it; false: otherwise.
     */
    public boolean onOperation(int id);

//    public String getDisplayPath(String path);
//
//    public String getRealPath(String displayPath);
    
    public void runOnUiThread(Runnable r);

    // return true indicates the navigation has been handled
//    public boolean onNavigation(String path);

    public boolean shouldHideMenu(int menu);

    public FileIconHelper getFileIconHelper();

    public FileInfo getItem(int pos);

    public void sortCurrentList(FileSortHelper sort);

    public Collection<FileInfo> getAllFiles();

    public void addSingleFile(FileInfo file);

    public boolean onRefreshFileList(String path, FileSortHelper sort, int sdcardIndex);
    public boolean onRefreshFileList(String path, FileSortHelper sort);
    public boolean onRefreshEditFileList(String path, FileSortHelper sort);
    public boolean onRefreshEditFileList(String path, FileSortHelper sort,int position);
    
    public boolean onRefreshPathBar(String path, int id);
    public void addTab(String path);

    public int getItemCount();

	public ProgressBar getProgressBar();
//	public View getConfirmOperationBar();
//	public View getPasteLayout();
	public FileViewInteractionHub getFileViewInteractionHub();
	
	public String getFragmentTag();
	
}
