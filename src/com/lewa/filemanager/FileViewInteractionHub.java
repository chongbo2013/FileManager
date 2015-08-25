package com.lewa.filemanager;

import java.io.File;
import java.util.ArrayList;

import com.lewa.filemanager.CustomMenu.DropDownMenu;
import com.lewa.filemanager.FileCategoryFragment.FileListIndex;
import com.lewa.filemanager.FileCategoryHelper.CategoryInfo;
import com.lewa.filemanager.FileCategoryHelper.FileCategory;
import com.lewa.filemanager.FileOperationHelper.IOperationProgressListener;
import com.lewa.filemanager.FileSortHelper.SortMethod;
import com.lewa.filemanager.FileViewFragment.SelectFilesCallback;
import com.lewa.filemanager.TextInputDialog.OnFinishListener;
import com.lewa.filemanager.clearFolder.Constants;
import com.lewa.filemanager.clearFolder.EmptyDirectoryCleaner;
import com.lewa.filemanager.clearFolder.NavigationPool;
import com.lewa.filemanager.clearFolder.TypeFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
//LEWA ADD BEGIN
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
//LEWA ADD END
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import android.os.HandlerThread;

import com.lewa.filemanager.R;

// modify by jjlin for HotKnot Begin
import lewa.hotknot.HotKnotHelper;
// modify by jjlin for HotKnot End
public class FileViewInteractionHub
        implements
        IOperationProgressListener,
        OnMenuItemClickListener,
        OnClickListener {

    AbsListView fileView;
    ArrayList<String> historyString = null;
    private static final String LOG_TAG = "FileViewInteractionHub";
    private IFileInteractionListener mFileViewListener;
    private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();
    private FileOperationHelper mFileOperationHelper;
    private FileSortHelper mFileSortHelper;
    public View mConfirmOperationBar;// luoyonxing
    private Context mContext;

    protected View pasteToolbar;
    protected Button pasteBar_paste;
    protected Button pasteBar_cancel;
    public NavigationPool navTool = new NavigationPool();
    private String mCurrentPath;
    private String mRoot;
    FileViewInteractionHub mfileViewInteractionHub;
    //#952402 modify begin by bin.dong
    private static String operationHint;
    public static ProgressDialog progressDialog;
    //#952402 modify end by bin.dong
/// M: added for HotKnot @{
    public boolean mHotKnotWaitSend = false;
    Uri contentUri=null;
    Uri[] uris;
    private Handler mHandler;
    //add for hotknot feature
    private HotKnotHelper mHotKnotHelper = null; // modify by jjlin for HotKnot
    //add by chenyongjun for HOTKNOT feature;
    private Uri[] mHotknotUris ; 
    /// @}
    // added by weihong, #62735
    private int mCurPosition;

    public enum Mode {
        View, Pick, Edit
    };

    private int CurrentShowListCardIndex = -1;
    
    private FileViewFragment fileViewFragment;
    
    private int checkSortItem = 0;
    // modify by jjlin for HotKnot Begin    
    private static final int MSG_HOTKNOT_COMPLETE = 1;
    private static final int MSG_HOTKNOT_MODECHANGED = 2;
    // modify by jjlin for HotKnot End

    public FileViewInteractionHub(IFileInteractionListener fileViewListener) {
        assert (fileViewListener != null);
        mFileViewListener = fileViewListener;
        mContext = mFileViewListener.getContext();
        setup();
        mFileOperationHelper = new FileOperationHelper(this);
        mFileSortHelper = new FileSortHelper();
        mfileViewInteractionHub = this;
        mFileSortHelper.setSortMethog(((FileManagerMainActivity)mContext).getSortMethod());
    }

    public void setFileViewFragmentObj(FileViewFragment fileViewFragment){
        this.fileViewFragment = fileViewFragment;
    }
    public FileViewFragment getFileViewFragmentObj(){
        return fileViewFragment;
    }
    
    public View getConfirmOperationBar() {
        return mConfirmOperationBar;
    }

    public IFileInteractionListener getFileViewListener() {
        return mFileViewListener;
    }

    private void showProgress(String msg) {
        progressDialog = new ProgressDialog(mContext);
        // dialog.setIcon(R.drawable.icon);
        progressDialog.setMessage(msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void sortCurrentList() {
        mFileViewListener.sortCurrentList(mFileSortHelper);
    }

    private void showConfirmOperationBar(boolean show) {
        ((FileManagerMainActivity) mFileViewListener.getContext())
                .setCanShowCheckBox(show);
        ((FileManagerMainActivity) mFileViewListener.getContext())
                .showConfirmOperationBar(show ? View.VISIBLE : View.GONE);
    }

    public void addContextMenuSelectedItem() {
//        if (mCheckedFileNameList.size() == 0) {
//            int pos = mListViewContextMenuSelectedItem;
//            if (pos != -1) {
//                FileInfo fileInfo = mFileViewListener.getItem(pos);
//                if (fileInfo != null) {
//                    mCheckedFileNameList.add(fileInfo);
//                    Log.i("Gracker", "addContextMenuSelectedItem  ");
//
//                }
//            }
//        }
    }

    public ArrayList<FileInfo> getSelectedFileList() {
        return mCheckedFileNameList;
    }

    public boolean canPaste() {
        return ((FileManagerMainActivity) mContext).getFileOperationHelper()
                .canPaste();
        // mFileOperationHelper.canPaste();
    }

    // operation finish notification
    @Override
    public void onFinish() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        final int sourceFragIndex = ((FileManagerMainActivity) mContext)
                .getSourceFragmentIndex();
        ((FileManagerMainActivity) mContext).setSourceFragmentIndex(-1);
        mFileViewListener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clearSelection();

                if (operationHint != null && !operationHint.equals("")) {
                    Toast.makeText(mContext, operationHint, Toast.LENGTH_SHORT)
                            .show();
                }
                if (((FileManagerMainActivity) mContext).getTabsAdapter()
                        .getCount() == 3) {
                    for (int i = 0; i < 3; i++) {
                        if (((IFileInteractionListener) ((FileManagerMainActivity) mContext)
                                .getFragment(i)) != null
                                && ((IFileInteractionListener) ((FileManagerMainActivity) mContext)
                                .getFragment(i))
                                .getFileViewInteractionHub() != null) {
                            ((IFileInteractionListener) ((FileManagerMainActivity) mContext)
                                    .getFragment(i))
                                    .getFileViewInteractionHub()
                                    .refreshFileList();
                        }
                    }
                } else if (sourceFragIndex != -1
                        && ((FileManagerMainActivity) mContext)
                        .getSourceFragmentIndex() != ((FileManagerMainActivity) mContext)
                        .getCurrentItemIndex()) {
                    ((IFileInteractionListener) ((FileManagerMainActivity) mContext)
                            .getFragment(sourceFragIndex))
                            .getFileViewInteractionHub().refreshFileList();
                    refreshFileList();
                } else {
                    refreshFileList();
                }
                // refresh the category total info on category page.
                ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                        .getFragment(0)).refreshCategoryInfo();

            }
        });
    }

    public void setToastHint(String str) {
        operationHint = str;
    }

    public FileInfo getItem(int pos) {
        return mFileViewListener.getItem(pos);
    }

    public boolean isInSelection() {
        return mCheckedFileNameList.size() > 0 || getMode() == Mode.Edit;
    }

    public boolean isMoveState() {
        return ((FileManagerMainActivity) mContext).getFileOperationHelper()
                .isMoveState()
                || ((FileManagerMainActivity) mContext)
                .getFileOperationHelper().canPaste();
        // mFileOperationHelper.isMoveState()
        // || mFileOperationHelper.canPaste();
    }

    private void setup() {
        setupFileListView();
        setupOperationPane();
    }

    // buttons
    private void setupOperationPane() {
        mConfirmOperationBar = mFileViewListener.getViewById(R.id.layout_paste);
        // mConfirmOperationBar.setBackgroundDrawable(this.mContext.getResources().getDrawable(lewa.R.drawable.android_ab_bottom_solid_light_holo));
        pasteBar_paste = (Button) ((FileManagerMainActivity) mConfirmOperationBar
                .getContext()).findViewById(R.id.toolbar_paste);
        pasteBar_cancel = (Button) ((FileManagerMainActivity) mConfirmOperationBar
                .getContext()).findViewById(R.id.pastebar_cancel);
        // setupClick(mConfirmOperationBar, R.id.toolbar_paste);
        // setupClick(mConfirmOperationBar, R.id.pastebar_cancel);

    }

    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : mFileViewListener
                .getViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.toolbar_paste:// button_moving_confirm:
                    onOperationButtonConfirm();
                    break;
                case R.id.pastebar_cancel:// button_moving_cancel:
                    onOperationButtonCancel();
                    break;
            }
        }
    };

    private void onOperationReferesh() {
        refreshFileList();
        Toast.makeText(mContext, R.string.operation_refresh_completed,
                Toast.LENGTH_SHORT)
                .show();
    }

    private void onOperationSetting() {
        Intent intent = new Intent(mContext,
                FileManagerPreferenceActivity.class);
        if (intent != null) {
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to start setting: " + e.toString());
            }
        }
    }

    private void onOperationShowSysFiles() {
        Settings.instance().setShowDotAndHiddenFiles(
                !Settings.instance().getShowDotAndHiddenFiles());
        refreshFileList();
        Util.setIsSdCardNeedRefresh(true);
        Util.setMemoryListNeedRefresh(true);
    }

    public void onOperationSelectAllOrCancel() {
        if (!isSelectedAll()) {
            onOperationSelectAll();
        } else {
            clearSelection();
        }
    }

    public void onOperationSelectAll() {
        mCheckedFileNameList.clear();
        for (FileInfo f : mFileViewListener.getAllFiles()) {
            if (f != null) {
                f.Selected = true;
                mCheckedFileNameList.add(f);
            }
        }
        mFileViewListener.onDataChanged();
    }

    public boolean onOperationUpLevel() {
        // showDropdownNavigation(false);

        if (mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL)) {
            return true;
        }

        if (!mRoot.equals(mCurrentPath)) {
            mCurrentPath = new File(mCurrentPath).getParent();
            refreshFileList();
            return true;
        }

        return false;
    }

    public void onOperationCreateFolder() {
        TextInputDialog dialog = new TextInputDialog(
                mContext,
                mContext.getString(R.string.new_folder_name),
                mContext.getString(R.string.operation_create_folder_message),
                mContext.getString(R.string.new_folder_name),
                new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        return doCreateFolder(text);
                    }
                });
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private boolean doCreateFolder(String text) {
        if (TextUtils.isEmpty(text))
            return false;
        // mCurrentPath = ((FileViewFragment)
        // mFileViewListener).getSDCardPath();
        int errorStrId = 0;
        switch (mFileOperationHelper.CreateFolder(this,mCurrentPath, text)) {
        case -1:
            errorStrId = R.string.fail_to_create_folder_invalid_name;
            break;
        case 0:
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(
                    mCurrentPath, text)));
            mFileListView.setSelection(mFileListView.getCount() - 1);
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
            return true;
        case 1:
            errorStrId = R.string.fail_to_create_folder;
            break;
        case 2:
            errorStrId = R.string.name_too_long;
            break;
        }

        new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(errorStrId))
                .setPositiveButton(R.string.confirm, null).create().show();
        return false;
    }

    public void onOperationSearch() {

    }

    private void onOperationSelectSdcard() {
        final String[] storageItems = {
                mContext.getString(R.string.menu_show_internal_sdcard),
                mContext.getString(R.string.menu_show_external_sdcard),
                mContext.getString(R.string.menu_show_all_sdcard)
        };
        if(CurrentShowListCardIndex == -1)
            CurrentShowListCardIndex = 2;
        new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.menu_select_sdcard))
                // #70059 modify begin by bin.dong
                .setSingleChoiceItems(storageItems, CurrentShowListCardIndex,
                // #70059 modify end by bin.dong
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                showSelectStorage(item);
                                dialog.cancel();
                            }
                        }).show();
    }

    private void onOperationSort() {
        int checkedItem = ((FileManagerMainActivity) mContext).getFileListIndex().ordinal() -1;
        if(checkedItem == -1)
            checkedItem = 0;
       
        final String[] items = { mContext.getString(R.string.menu_sort_name),
                mContext.getString(R.string.menu_sort_size),
                mContext.getString(R.string.menu_sort_time),
                mContext.getString(R.string.menu_sort_type) };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.dialog_sort));
        //#938474 modify begin by bin.dong
        builder.setSingleChoiceItems(items, /*checkedItem*/checkSortItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch(item){
                        case 0 :// MENU_SORT_NAME : GlobalConsts.MENU_SORT
                            ((FileManagerMainActivity) mContext).setFileListIndex(FileListIndex.NoIndex);// added by weihong, #63036 "Name --> NoIndex"
                            checkSortItem = 0;
                            onSortChanged(SortMethod.name);
                            dialog.cancel();
                            break;
                        case 1 : // MENU_SORT_SIZE
                            ((FileManagerMainActivity) mContext).setFileListIndex(FileListIndex.Size);
                            onSortChanged(SortMethod.size);
                            checkSortItem = 1;
                            dialog.cancel();
                            break;
                        case 2 : // MENU_SORT_DATE
                            ((FileManagerMainActivity) mContext).setFileListIndex(FileListIndex.Date);
                            onSortChanged(SortMethod.date);
                            checkSortItem = 2;
                            dialog.cancel();
                            break;
                        case 3 : // MENU_SORT_TYPE
                            ((FileManagerMainActivity) mContext).setFileListIndex(FileListIndex.type);
                            onSortChanged(SortMethod.type);
                            checkSortItem = 3;
                            dialog.cancel();
                            break;
                        }
                        if (((FileManagerMainActivity) mContext)
                                .getCurrentItemIndex() == 0) {// &&!((FileCategoryFragment)mFileViewListener).isHomePage()
                            refreshFileList();
                        }
                    }
                }).show();
      //#938474 modify end by bin.dong
    }

    public void onSortChanged(SortMethod s) {
        if (mFileSortHelper.getSortMethod() != s) {
            mFileSortHelper.setSortMethog(s);
            sortCurrentList();
        }
    }

    public void setSortMethod(SortMethod s) {
        mFileSortHelper.setSortMethog(s);
    }

    public void onOperationCopyPrepare() {
        int fragmentIndex = ((FileManagerMainActivity) mFileViewListener
                .getContext()).getCurrentItemIndex();
        ((FileManagerMainActivity) mContext)
                .setSourceFragmentIndex(fragmentIndex);
        ((FileManagerMainActivity) mContext).getFileOperationHelper().Copy(
                getSelectedFileList());
        clearSelection();
        if (fragmentIndex == Util.CATEGORY_TAB_INDEX) {
            scrollToSDcardTab();
        }
        showConfirmOperationBar(true);
        Button confirmButton = (Button) ((FileManagerMainActivity) mConfirmOperationBar
                .getContext()).findViewById(R.id.toolbar_paste);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void onOperationCopyPath() {
        if (getSelectedFileList().size() == 1) {
            copy(getSelectedFileList().get(0).filePath);
        }
        clearSelection();
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mContext
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);// ((FileManagerMainActivity)mContext).getPasteDestPath()
    }

    private void onOperationPaste() {
        if (((FileManagerMainActivity) mContext).getFileOperationHelper()
                .Paste(this,mCurrentPath, mRoot)) {
            operationHint = mContext.getString(R.string.file_pasted);
            showProgress(mContext.getString(R.string.operation_pasting));// ((FileManagerMainActivity)mContext).
        }
    }

    public void onOperationMovePrepare() {

        int fragmentIndex = ((FileManagerMainActivity) mFileViewListener
                .getContext()).getCurrentItemIndex();
        ((FileManagerMainActivity) mContext)
                .setSourceFragmentIndex(fragmentIndex);
        ((FileManagerMainActivity) mContext).getFileOperationHelper()
                .StartMove(getSelectedFileList());
        clearSelection();
        if (fragmentIndex == Util.CATEGORY_TAB_INDEX) {
            scrollToSDcardTab();
        }
        showConfirmOperationBar(true);
        Button confirmButton = (Button) ((FileManagerMainActivity) mConfirmOperationBar
                .getContext()).findViewById(R.id.toolbar_paste);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void refreshFileList() {
        clearSelection();
        int fragmentIndex = ((FileManagerMainActivity) mFileViewListener
                .getContext()).getCurrentItemIndex();
        String fragmentTag = mFileViewListener.getFragmentTag();
        int storageLocationFilter = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                .getFragment(0)).getStorageLocationFilter();
        if (FileManagerMainActivity.COUNT_TAG.equals(fragmentTag)
                && storageLocationFilter != -1) {
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper,
                    storageLocationFilter);
        } else {
            // onRefreshFileList returns true indicates list has changed
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
        }
        // update move operation button state
        updateConfirmButtons();
    }

    private void updateConfirmButtons() {
        if (mConfirmOperationBar.getVisibility() == View.GONE)
            return;
        Button confirmButton = (Button) ((FileManagerMainActivity) mFileViewListener
                .getContext()).findViewById(R.id.toolbar_paste);
        if (isSelectingFiles()) {
            confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
        } else if (isMoveState()) {
            confirmButton.setEnabled(((FileManagerMainActivity) mContext)
                    .getFileOperationHelper().canMove(mCurrentPath));
        }
    }

    public void onOperationSend() {
        ArrayList<FileInfo> selectedFileList = getSelectedFileList();
        for (FileInfo f : selectedFileList) {
            if (f.IsDir) {
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setMessage(R.string.error_info_cant_send_folder)
                        .setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                return;
            }
        }
        Intent intent = IntentBuilder.buildSendFile(selectedFileList);
        if (intent != null) {
            try {
                mFileViewListener.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
        clearSelection();
    }

    public void onOperationRename() {

        if (getSelectedFileList().size() == 0) {
            return;
        }

        final FileInfo f = getSelectedFileList().get(0);

        TextInputDialog dialog = new TextInputDialog(mContext,
                mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message),
                f.fileName, new OnFinishListener() {
            @Override
            public boolean onFinish(String text) {
                        boolean isDoRenameSuccess = doRename(f, text);
                        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                                .getActionMode();
                        if (null != actionMode) {
                            actionMode.finish();
                        }
                        return isDoRenameSuccess;
            }
        });

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private boolean doRename(final FileInfo f, String text) {
        if (TextUtils.isEmpty(text))
            return false;
        int errorStrId = 0;
        int position = f.filePath.lastIndexOf('/');
        String path = f.filePath.substring(0, position) + '/' + text;
        File file = new File(path);
        if (text.length() >= GlobalConsts.MAX_FILE_NAME_LEN) {
            errorStrId = R.string.name_too_long;
        } else if (file.exists()) {
            errorStrId = R.string.fail_to_rename_file;
        } else if (((FileManagerMainActivity) mContext)
                .getFileOperationHelper().Rename(f, text)) {
            // mFileOperationHelper.Rename(f, text)) {
            f.fileName = text;
            mFileViewListener.onDataChanged();
            return true;
        } else {
            errorStrId = R.string.fail_to_rename;
        }
        new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(errorStrId))
                .setPositiveButton(R.string.confirm, null).create().show();
        return false;
    }

    public void notifyFileSystemChanged(String path) {

        if (path == null||path.length()<=1)
            return;
        final File f = new File(path.toString());
        //#69406 modify begin by bin.dong
        final Intent intent;
        if (f.isDirectory()) {
        /*MediaScannerConnection.scanFile(mContext, new String[] {path}, null, new MediaScannerConnection.OnScanCompletedListener() {
             @Override
             public void onScanCompleted(String path, Uri uri) {
                 Log.e(LOG_TAG, "onScanCompleted path:"+path);
                 Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                 intent.setData(Uri.fromFile(f));
                 mContext.sendBroadcast(intent);
                }
            });*/
        intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
            intent.setClassName("com.android.providers.media",
                    "com.android.providers.media.MediaScannerReceiver");
            intent.setData(Uri.fromFile(new File(mRoot)));           
        } else {
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(f));
        }
        mContext.sendBroadcast(intent);
      //#69406 modify end by bin.dong
    }

    private void notifyFileSystemChangedOnKitkat(String path) {
        if (path == null)
            return;

        final File f = new File(path.toString());

        if (f.isDirectory()) {//Android4.4 change the way to update a folder
            MediaScannerConnection.scanFile(mContext, new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.v("Gracker", "onScanCompleted path = " + path +
                                    " Uri = " + uri);
                        }
                    }
            );
        } else {//a file can be updated by ACTION_MEDIA_SCANNER_SCAN_FILE
            final Intent intent;
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(f));
            mContext.sendBroadcast(intent);
        }
    }

    public void onOperationPictureFilter() {
        float currentValue = FileManagerPreferenceActivity
                .getPicFilter(mContext);

        PictureFilterDialog dialog = new PictureFilterDialog(
                mContext,
                mContext.getString(R.string.filtersize_pic_title),
                mContext.getString(R.string.filtersize_pic_title),
                currentValue,
                new com.lewa.filemanager.PictureFilterDialog.OnFinishListener() {
                    @Override
                    public boolean onFinish(float inputValue) {
                        FileManagerPreferenceActivity.setPicFilter(mContext,
                                inputValue);
                        mFileViewListener.onRefreshFileList(mCurrentPath,
                                mFileSortHelper, CurrentShowListCardIndex);
                        return true;
                    }
                });
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public void onOperationDelete() {
        doOperationDelete(getSelectedFileList());
    }

    /**
     * This function is for future use.
     *
     * @param position
     */
    public void onOperationDelete(int position) {
        FileInfo file = mFileViewListener.getItem(position);
        if (file == null)
            return;

        ArrayList<FileInfo> selectedFileList = new ArrayList<FileInfo>();
        selectedFileList.add(file);
        doOperationDelete(selectedFileList);
    }

    private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
        final ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(
                selectedFileList);
        Dialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(
                        mContext.getString(R.string.operation_delete_confirm_message))
                .setTitle(R.string.operation_delete)
                .setPositiveButton(R.string.confirm,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                if (((FileManagerMainActivity) mContext)
                                        .getFileOperationHelper().Delete(mfileViewInteractionHub,selectedFiles, mRoot)) {
                                    
                                    operationHint = mContext
                                            .getString(R.string.file_deleted);
                                    showProgress(mContext
                                            .getString(R.string.deleting));
                                    mFileViewListener.onRefreshFileList(
                                            mCurrentPath, mFileSortHelper);
                                }
                                clearSelection();
                                ActionMode actionMode = ((FileManagerMainActivity) mContext)
                                        .getActionMode();
                                // added by weihong, #64755
                                actionMode.finish();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
//                                clearSelection();
                            }
                        }).create();
        dialog.show();
    }

    public void onOperationInfo() {
        if (getSelectedFileList().size() == 0)
            return;

        FileInfo file = getSelectedFileList().get(0);
        if (file == null)
            return;
        boolean isApk = false;
        if (getCurrentPath().equals(
                "/" + mContext.getString(R.string.category_apk))) {
            isApk = true;
        }
        InformationDialog dialog = new InformationDialog(mContext, file,
                mFileViewListener.getFileIconHelper(), isApk);

        dialog.show();
    }

    public void onOperationButtonConfirm() {
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(mCheckedFileNameList);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (((FileManagerMainActivity) mContext)
                .getFileOperationHelper().isMoveState()) { // Move(Cut) Paste
            // Status
            if (((FileManagerMainActivity) mContext).getFileOperationHelper()
                    .EndMove(this,mCurrentPath, mRoot)) {
                // mFileOperationHelper.EndMove(mCurrentPath, this.mRoot)) {
                operationHint = mContext.getString(R.string.file_pasted);
                showProgress(mContext.getString(R.string.operation_moving));
            }
        } else { // Copy Paste status
            if (((FileManagerMainActivity) mContext).getCurrentItemIndex() - 1 < 0) {
                progressDialog.dismiss();
            } else {
                onOperationPaste();
            }
        }
        showConfirmOperationBar(false);
    }

    public void onOperationButtonCancel() {
        ((FileManagerMainActivity) mContext).getFileOperationHelper().clear();
        // mFileOperationHelper.clear();
        showConfirmOperationBar(false);
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(null);
            mSelectFilesCallback = null;
            clearSelection();
            setMode(Mode.View);
        } else if (((FileManagerMainActivity) mContext)
                .getFileOperationHelper().isMoveState()) {
            // refresh to show previously selected hidden files
            ((FileManagerMainActivity) mContext).getFileOperationHelper()
                    .EndMove(this,null, mRoot);
            refreshFileList();
        } else {
            refreshFileList();
        }

        ((FileManagerMainActivity) mContext).setSourceFragmentIndex(-1);
    }

    /*add by jwgao
    *
    *正在进行复制粘贴时，滑到第一屏则取消复制粘贴状态
    * */
    public void onOperationScollCancelMove() {
        ((FileManagerMainActivity) mContext).getFileOperationHelper().clear();
        showConfirmOperationBar(false);
        if (((FileManagerMainActivity) mContext)
                .getFileOperationHelper().isMoveState()) {
            ((FileManagerMainActivity) mContext).getFileOperationHelper()
                    .EndMove(this,null, mRoot);
            refreshFileList();
        }
        ((FileManagerMainActivity) mContext).setSourceFragmentIndex(-1);
    }

    // context menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenuInfo menuInfo) {
        }
    };

    // File List view setup
    private ListView mFileListView;

    private int mListViewContextMenuSelectedItem;

    private void setupFileListView() {
        mFileListView = (ListView) mFileViewListener
                .getViewById(R.id.file_path_list);
        mFileListView.setLongClickable(true);
        mFileListView
                .setOnCreateContextMenuListener(mListViewContextMenuListener);
        mFileListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mCurPosition = position;
                onListItemClick(parent, view, position, id);
                ActionMode actionMode = ((FileManagerMainActivity) mContext)
                        .getActionMode();
                if (null != actionMode) {
                    Menu menu = actionMode.getMenu();
//                    MenuItem selectAll = menu.findItem(R.id.action_select_all);
//                    if(isSelectedAll()){
//                        selectAll.setIcon(lewa.R.drawable.ic_menu_clear_select);
//                        // Delete for standalone by Fan.Yang
//                        actionMode.setRightActionButtonResource(lewa.R.drawable.ic_menu_clear_select);
//                    } else {
//                        selectAll.setIcon(lewa.R.drawable.ic_menu_select_all);
//                        actionMode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
//                    }
                }
            }
        });

        // Set listview long click listener.
        mFileListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int pos, long id) {
                // TODO Auto-generated method stub
                onListItemLongClick(parent, view, pos, id);
                return true;
            }
        });
        // this.setMode(Mode.View);
        //add by jjlin for hotknot begin
        mHandler = new Handler(){
            public void handleMessage(Message msg) {
                // modify by jjlin for HotKnot Begin
                switch (msg.what) {
                case MSG_HOTKNOT_COMPLETE:
                    updateMenuItem();
                    break;
                case MSG_HOTKNOT_MODECHANGED:
                	updatehotknotmenu();
                	break;
                default:
                    break;
                }
                // modify by jjlin for HotKnot End
            }
        };
        //add by jjlin for hotknot end
    }

    // menu
    private static final int MENU_SEARCH = 1;

    private static final int MENU_SEND = 7;

    private static final int MENU_DELETE = 9;

    private static final int MENU_INFO = 10;

    private static final int MENU_SORT_NAME = 11;

    private static final int MENU_SORT_SIZE = 12;

    private static final int MENU_SORT_DATE = 13;

    private static final int MENU_SORT_TYPE = 14;

    private static final int MENU_SELECTALL = 16;

    private static final int MENU_SETTING = 17;

    private static final int MENU_EXIT = 18;
    private static final int MENU_CLEAN = 19;

    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                    .getMenuInfo();
            mListViewContextMenuSelectedItem = info != null
                    ? info.position
                    : -1;

            int itemId = item.getItemId();
            if (mFileViewListener.onOperation(itemId)) {
                return true;
            }

            addContextMenuSelectedItem();

            switch (itemId) {
                case MENU_SEARCH:
                    onOperationSearch();
                    break;
                case GlobalConsts.MENU_NEW_FOLDER:
                    onOperationCreateFolder();
                    break;
                case GlobalConsts.MENU_REFRESH:
                    onOperationReferesh();
                    break;
                case MENU_SELECTALL:
                    onOperationSelectAllOrCancel();
                    break;
                case GlobalConsts.MENU_SHOWHIDE:
                    onOperationShowSysFiles();
                    break;
                case GlobalConsts.MENU_SORT:
//                    ((FileManagerMainActivity) mContext)
//                    .showDialog(GlobalConsts.DIALOG_SORT);
                    onOperationSort();
                    break;
                // case GlobalConsts.MENU_FAVORITE:
                // onOperationFavorite();
                // break;
                case MENU_SETTING :
                    onOperationSetting();
                    break;
                case MENU_EXIT :
                    ((FileManagerMainActivity) mContext).finish();
                    break;
                // added by weihong, #63816
                case GlobalConsts.MENU_SELECT_SDCARD:
//                    ((FileManagerMainActivity) mContext)
//                    .showDialog(GlobalConsts.MENU_SELECT_SDCARD);
                    onOperationSelectSdcard();
                    break;
                case GlobalConsts.MENU_COPY_PATH :
                    onOperationCopyPath();
                    break;
                case GlobalConsts.MENU_PASTE :
                    onOperationPaste();
                    break;
                // case GlobalConsts.MENU_MOVE:
                // onOperationMove();
                // break;
                case MENU_SEND :
                    onOperationSend();
                    break;
                case R.id.action_rename : // MENU_RENAME:
                    onOperationRename();
                    break;
                case MENU_DELETE :
                    onOperationDelete();
                    mFileViewListener.onRefreshFileList(mCurrentPath,
                            mFileSortHelper);
                    break;
                case MENU_INFO :
                    onOperationInfo();
                    break;
                case MENU_CLEAN :
                    showProgress(mContext
                            .getString(R.string.operation_cleaning));
                    EmptyDirectoryCleaner.clearCleanData();
                    new HandlerThread("",
                            android.os.Process.THREAD_PRIORITY_BACKGROUND) {
                        @Override
                        public void run() {
                            new File(
                                    mFileViewListener.getClass().toString() == FileViewInteractionHub.class
                                            .toString()
                                            ? ((FileInfo) navTool.navEntity
                                                    .peek().producingSource).path
                                            : mCurrentPath)
                                    // "/mnt/sdcard")
                                    .listFiles(new EmptyDirectoryCleaner(
                                            new Integer[]{TypeFilter.FILTER_BOTH_DIR_FILE},
                                            Constants.HIDDEN_EXCLUDED,
                                            Constants.HIDDEN_INCLUDED));
                            handler.sendEmptyMessageDelayed(Constants.OperationContants.CLEAN_NOTIFY, 200);
                            // mFileViewListener.onRefreshFileList(mCurrentPath,
                            // mFileSortHelper);
                        }
                    }.start();

                    break;
                case GlobalConsts.MENU_PICTURE_FILTER :
                    onOperationPictureFilter();
                    break;
                default :
                    return false;
            }

            mListViewContextMenuSelectedItem = -1;
            return false;
        }

    };

    // added by weihong, #63816
    private void showSelectStorage(int sdcardIndex){
        switch(sdcardIndex){
            case 0: // When sdcardIndex 0 is internal sdcard.
                if (((FileManagerMainActivity) mContext)
                        .getCurrentItemIndex() == 0) {
                    FileCategory fileCategory = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCurCategory();
                    CategoryInfo catInfo = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCategoryInfo(fileCategory);
                    catInfo.setCategorySDCardIndex(Util.internalStorageIndex);
                    mFileViewListener.onRefreshFileList(mCurrentPath,
                            mFileSortHelper, Util.internalStorageIndex);
                }
                CurrentShowListCardIndex = Util.internalStorageIndex;
                break;
            case 1: // When sdcardIndex 1 is external sdcard.
                if (((FileManagerMainActivity) mContext)
                        .getCurrentItemIndex() == 0) {
                    FileCategory fileCategory1 = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCurCategory();
                    CategoryInfo catInfo1 = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCategoryInfo(fileCategory1);
                    catInfo1.setCategorySDCardIndex(Util.sdcardIndex);
                    mFileViewListener.onRefreshFileList(mCurrentPath,
                            mFileSortHelper, Util.sdcardIndex);
                }
                CurrentShowListCardIndex = Util.sdcardIndex;
                break;
            case 2:
                if (((FileManagerMainActivity) mContext)
                        .getCurrentItemIndex() == 0) {
                    FileCategory fileCategory2 = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCurCategory();
                    CategoryInfo catInfo2 = ((FileCategoryFragment) ((FileManagerMainActivity) mContext)
                            .getFragment(0)).getFileCategoryHelper()
                            .getCategoryInfo(fileCategory2);
                    catInfo2.setCategorySDCardIndex(2);
                    mFileViewListener.onRefreshFileList(mCurrentPath,
                            mFileSortHelper, 2);
                }
                CurrentShowListCardIndex = 2;
                break;
        }
    }

    private void scrollToSDcardTab() {
        ((FileManagerMainActivity) mFileViewListener.getContext())
                .setCurrentItemIndex(1); // 1: the first SD card page.
    }

    public Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.OperationContants.CLEAN_NOTIFY :
                    // adapter.notifyDataSetChanged();
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    ((FileManagerMainActivity) mContext)
                            .showDialog(GlobalConsts.DIALOG_CLEAN_DIR);
                    // refresh();
                    mFileViewListener.onRefreshFileList(mCurrentPath,
                            mFileSortHelper);
                    break;

            }
        }
    };
    // ----------above are for clearing empty folders.
    private com.lewa.filemanager.FileViewInteractionHub.Mode mCurrentMode;

    private SelectFilesCallback mSelectFilesCallback;

    public void setMenuItem(Menu menu, int menuItemIdx) {
        MenuItem mItem = menu.getItem(menuItemIdx);
        mItem.setOnMenuItemClickListener(this);
    }

    public void setMenuItemById(Menu menu, int idcount) {
        for (int i = 0; i < idcount; i++) {
            setMenuItem(menu, i);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (getMode() == Mode.Edit)
            return true;
        clearSelection();
        menu.clear();

        addMenuItem(menu, GlobalConsts.MENU_SORT, 1, R.string.menu_item_sort, 0);

        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 2,
                R.string.operation_create_folder, 0);

        addMenuItem(menu, GlobalConsts.MENU_REFRESH, 3,
                R.string.operation_refresh, R.drawable.ic_refresh);
        // // MENU_CLEAN: Delete empty folders.
        addMenuItem(menu, MENU_CLEAN, 4, R.string.menu_onekeyclean,
                R.drawable.clean);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 5,
                R.string.show_hidden_file, 0);

        return true;
    }

    public boolean onCreateCategoryOptionsMenu(Menu menu, Object sdcardIndex) {
        if (getMode() == Mode.Edit)
            return true;
        sdcardIndex = 2;
        clearSelection();
        menu.clear();
        addMenuItem(menu, GlobalConsts.MENU_SORT, 1, R.string.menu_item_sort, 0);

        addMenuItem(menu, GlobalConsts.MENU_REFRESH, 2,
                R.string.operation_refresh, R.drawable.ic_refresh);

        if (((FileManagerMainActivity) mContext).getCurrentItemIndex() == 0) {
            if (!((FileCategoryFragment) mFileViewListener).isHomePage()
                    && ((FileCategoryFragment) mFileViewListener)
                            .getFileCategoryHelper().getCurCategory() == FileCategory.Picture) {
                addMenuItem(menu, GlobalConsts.MENU_PICTURE_FILTER, 3,
                        R.string.filtersize_pic_title, R.drawable.ic_refresh);
            }
        }

        if (Util.isAllSDCardReady() && Util.isSupportDualSDCard() && Util.getSdcardMountedNum() == 2 ) {
            // added by weihong, #63816
            addMenuItem(menu, GlobalConsts.MENU_SELECT_SDCARD, 4,
                    R.string.menu_select_sdcard, 0);
            /*
            SubMenu sdcardMenu = menu.addSubMenu(4,
                    GlobalConsts.MENU_SELECT_SDCARD, 4,
                    R.string.menu_select_sdcard);
            FileCategory fileCategory = ((FileCategoryFragment) mFileViewListener)
                    .getFileCategoryHelper().getCurCategory();
            CategoryInfo catInfo = ((FileCategoryFragment) mFileViewListener)
                    .getFileCategoryHelper().getCategoryInfo(fileCategory);
            // Index:// 0: internal sdcard; 1: external sdcard; other values:
            // all sdcard.
            if (catInfo.getCategorySDCardIndex() == Util.internalStorageIndex) {
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_EXTERNAL_SDCARD,
                        0, R.string.menu_show_external_sdcard);
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_ALL_SDCARD, 1,
                        R.string.menu_show_all_sdcard);
            } else if (catInfo.getCategorySDCardIndex() == Util.sdcardIndex) {
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_INTERNAL_SDCARD,
                        0, R.string.menu_show_internal_sdcard);
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_ALL_SDCARD, 1,
                        R.string.menu_show_all_sdcard);
            } else {
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_INTERNAL_SDCARD,
                        0, R.string.menu_show_internal_sdcard);
                addMenuItem(sdcardMenu, GlobalConsts.MENU_SHOW_EXTERNAL_SDCARD,
                        1, R.string.menu_show_external_sdcard);
            }
            */
        }
        return true;
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string) {
        addMenuItem(menu, itemId, order, string, -1);
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string,
            int iconRes) {
        if (!mFileViewListener.shouldHideMenu(itemId)) {
            MenuItem item = menu.add(0, itemId, order, string)
                    .setOnMenuItemClickListener(menuItemClick);
            if (iconRes > 0) {
                item.setIcon(iconRes);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuItems(menu);
        return true;
    }

    private void updateMenuItems(Menu menu) {
        MenuItem menuItem = menu.findItem(GlobalConsts.MENU_SHOWHIDE);
        if (menuItem != null) {
            menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles()
                    ? R.string.hide_file
                    : R.string.show_hidden_file);
        }
    }

    public boolean isFileSelected(String filePath) {
        return mFileOperationHelper.isFileSelected(filePath);
    }

    public boolean isFileChecked(String filePath) {
        boolean result = false;
        for (FileInfo f : mCheckedFileNameList) {
            if (f.filePath.equalsIgnoreCase(filePath))
                return true;
        }
        return false;
    }

    public void setMode(Mode m) {
        mCurrentMode = m;
        if (Mode.Edit == mCurrentMode){
            mFileListView.setPadding(0, 0, 0, (int)mContext.getResources().getDimension(R.dimen.menu_height));
        } else {
            mFileListView.setPadding(0, 0, 0, 0);
        }
    }

    public Mode getMode() {
        return mCurrentMode;
    }

    public void onListItemClick(AdapterView<?> parent, View view, int position,
            long id) {

        FileInfo lFileInfo = mFileViewListener.getItem(position);

        if (lFileInfo == null) {
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
            refreshFileList();
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }

        if (isInSelection()) {
            boolean selected = lFileInfo.Selected;
            lFileInfo.Selected = !selected;
            ActionMode actionMode = ((FileManagerMainActivity) mContext)
                    .getActionMode();
            CheckBox checkBox = (CheckBox) view
                    .findViewById(R.id.file_checkbox);
            if (selected) {
                mCheckedFileNameList.remove(lFileInfo);
                checkBox.setChecked(false);
            } else {
                mCheckedFileNameList.add(lFileInfo);
                checkBox.setChecked(true);
            }
            checkBox.setTag(lFileInfo);

            Util.updateActionModeMenuTitle(actionMode, mContext,
                    mCheckedFileNameList.size());
            Util.updateActionModeMenuState(actionMode, mContext);
            updateMenuItem();
            return;
        }

        if (!lFileInfo.IsDir) {
            // mfileViewInteractionHub.setMode(Mode.Pick);
            if (mCurrentMode == Mode.Pick) {
                mFileViewListener.onPick(lFileInfo);
            } else {
                viewFile(lFileInfo);
            }
            return;
        }

        mCurrentPath = getAbsoluteName(mCurrentPath, lFileInfo.fileName);
        mFileViewListener.addTab(lFileInfo.fileName); // new Lewa scroll bar.
        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
        if (actionMode != null) {
            actionMode.finish();
        }

        if (mfileViewInteractionHub.getSelectedFileList().size() <= 0
                && mfileViewInteractionHub.getMode() != Mode.Pick) {
            mfileViewInteractionHub.setMode(Mode.View);
            refreshFileList();
        } else if (mfileViewInteractionHub.getSelectedFileList().size() <= 0
                && mfileViewInteractionHub.getMode() == Mode.Pick) {
            refreshFileList();
        } else if (mfileViewInteractionHub.getMode() != Mode.Pick) {
            mfileViewInteractionHub.setMode(Mode.Edit);
            mFileViewListener.onRefreshEditFileList(mCurrentPath,
                    mFileSortHelper);
        }
        mFileViewListener.onDataChanged();
    }

    public void onListItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (mfileViewInteractionHub.getMode() == Mode.Edit
                || mConfirmOperationBar.getVisibility() == View.VISIBLE
                || mfileViewInteractionHub.getMode() == Mode.Pick)
            return;

        FileInfo lFileInfo = mFileViewListener.getItem(position);
        if (lFileInfo == null) {
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }
        boolean selected = lFileInfo.Selected;
        lFileInfo.Selected = !selected;
        mHotknotUris = new Uri[1];//add by jjlin for hotknot 

        CheckBox checkBox = (CheckBox) view.findViewById(R.id.file_checkbox);

        if (!lFileInfo.Selected) {
            mCheckedFileNameList.remove(lFileInfo);
            checkBox.setChecked(false);// .setImageResource(R.drawable.btn_check_off_holo_light);
        } else {
            mCheckedFileNameList.add(lFileInfo);
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(true);// .setImageResource(R.drawable.btn_check_on_holo_light);
           //add by jjlin for hotknot begin
            contentUri =Uri.fromFile(new File(lFileInfo.filePath));
            mHotknotUris[0]=Uri.fromFile(new File(lFileInfo.filePath));
            //add by jjlin for hotknot end
        }
        checkBox.setTag(lFileInfo);

        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
        if (actionMode == null) {
            actionMode = ((FileManagerMainActivity) mContext)
                    .startSupportActionMode(new ModeCallback(mContext,
                            mfileViewInteractionHub));
            ((FileManagerMainActivity) mContext).setActionMode(actionMode);
            actionMode.invalidate();
        } else {
            actionMode.invalidate();
        }

        // Util.updateActionModeTitle(actionMode, mContext,
        // mfileViewInteractionHub.getSelectedFileList().size());
        Util.updateActionModeMenuTitle(actionMode, mContext,
                mfileViewInteractionHub.getSelectedFileList().size());

        if (mfileViewInteractionHub.getSelectedFileList().size() <= 0
                && mfileViewInteractionHub.getMode() != Mode.Pick) {
            mfileViewInteractionHub.setMode(Mode.View);
            refreshFileList();
        } else if (mfileViewInteractionHub.getMode() != Mode.Pick) {
            mfileViewInteractionHub.setMode(Mode.Edit);
            mFileViewListener.onRefreshEditFileList(mCurrentPath,
                    mFileSortHelper,position);
        }
        mFileViewListener.onDataChanged();
    }

    public void setRootPath(String path) {
        mRoot = path;
        mCurrentPath = path;
    }

    public String getRootPath() {
        return mRoot;
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        mCurrentPath = path;
    }

    public int getCurPosition(){
        return this.mCurPosition;
    }

    public void setCurPosition(int position){
        this.mCurPosition = position;
    }

    private String getAbsoluteName(String path, String name) {
        return path.equals(GlobalConsts.ROOT_PATH) ? path + name : path
                + File.separator + name;
    }

    // check or uncheck
    public boolean onCheckItem(FileInfo f, View v) {
        if (isMoveState())
            return false;

        if (isSelectingFiles() && f.IsDir) {
            return false;
        }

        if (f.Selected) {
            mCheckedFileNameList.add(f);
        } else {
            mCheckedFileNameList.remove(f);
        }

        return true;
    }

    private boolean isSelectingFiles() {
        return mSelectFilesCallback != null;
    }

    public boolean isSelectedAll() {

        return mFileViewListener.getItemCount() != 0
                && mCheckedFileNameList.size() == mFileViewListener
                        .getItemCount();
    }

    public boolean isSelected() {
        return mCheckedFileNameList.size() != 0;
    }

    public void clearSelection() {
        if (mCheckedFileNameList.size() > 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f == null) {
                    continue;
                }
                f.Selected = false;
            }
            mCheckedFileNameList.clear();
            mFileViewListener.onDataChanged();
        }
    }

    private void viewFile(FileInfo lFileInfo) {
        try {
            if (fileIsExists(lFileInfo.filePath)) {
                IntentBuilder.viewFile(mContext, lFileInfo.filePath);
            } else {
                Log.e(LOG_TAG, "viewFile -> File does not exist: "
                        + lFileInfo.filePath);
            }
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "fail to view file: " + e.toString());
        }
    }

    public boolean fileIsExists(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                Log.e(LOG_TAG, "fileIsExists -> File does not exist: " + path);
                return false;
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "fileIsExists error " + path);
            // TODO: handle exception
            return false;
        }
        return true;
    }

    public boolean onBackPressed() {
        //#938474 add begin by bin.dong
        checkSortItem = 0;
        //#939674 add begin by bin.dong
        if (true /*FileViewFragment.isShouldRefresh*/) { // #980018 by ningyi
            ((FileCategoryFragment) ((FileManagerMainActivity) mContext).getFragment(0))
                    .refreshCategoryInfo();
            FileViewFragment.isShouldRefresh = false;
        }
        //#939674 add end by bin.dong
        //#938474 add end by bin.dong
        if (isInSelection()) {
            clearSelection();
            setMode(Mode.View);
            refreshFileList();
        } else if (!onOperationUpLevel()) {
            // this.hideBottomBar();
            return false;
        } else if (mCurrentPath == null) {
            return false;
        } else { // Both sdcard and category will go this way:
            if (!"/mnt".equals(mCurrentPath) && mRoot != null) {
                // mCurrentPath = new File(mCurrentPath).getParent();
                String[] result = mRoot.split("/");
                int rootSize = result.length;
                result = mCurrentPath.split("/");

                int id = result.length - rootSize; // Keep it 3 to make sure
                                                   // sdcard can return to the
                                                   // right path.
                mFileViewListener.onRefreshPathBar(mCurrentPath, id);
                // !!Notice: Do not refresh file list here for keep position for
                // Up Level file list.

                // return true;
            }
        }
        return true;
    }

    // public void copyFile(ArrayList<FileInfo> files) {
    // mFileOperationHelper.Copy(files);
    // }

    public void moveFileFrom(ArrayList<FileInfo> files) {
        ((FileManagerMainActivity) mContext).getFileOperationHelper()
                .StartMove(files);
        // mFileOperationHelper.StartMove(files);
        showConfirmOperationBar(true);
        updateConfirmButtons();
        // refresh to hide selected files
        refreshFileList();
    }

    @Override
    public void onFileChanged(String path) {
        if (Build.VERSION.SDK_INT == 19) {
            notifyFileSystemChangedOnKitkat(path);

        } else {
            notifyFileSystemChanged(path);
        }
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        mSelectFilesCallback = callback;
        showConfirmOperationBar(true);
        updateConfirmButtons();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
    }

    public class ModeCallback implements ActionMode.Callback, HotKnotHelper.HotknotListener { // modify by jjlin for HotKnot
        private Menu mMenu;
        private Context mContext;
        private FileViewInteractionHub mFileViewInteractionHub;
        private DropDownMenu mSelectionMenu;
        Button btn;

        private void initMenuItemSelectAllOrCancel() {
            boolean isSelectedAll = mFileViewInteractionHub.isSelectedAll();
            MenuItem item = mSelectionMenu.findItem(R.id.action_top_select_all);
            if (item != null) {
                if (isSelectedAll) {
                    item.setChecked(true);
//                    mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
                    // item.setTitle(R.string.operation_cancel_selectall);//operation_cancel);//operation_cancel_selectall);
                } else {
                    item.setChecked(false);// false
                    // item.setTitle(R.string.operation_selectall);
                }
            }
        }

        private void scrollToSDcardTab() {
            ((FileManagerMainActivity) mContext).getActionBar();
        }

        public ModeCallback(Context context,
                FileViewInteractionHub fileViewInteractionHub) {
            mContext = context;
            mFileViewInteractionHub = fileViewInteractionHub;
        }

        @Override
        public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
            Util.updateActionModeMenuTitle(mode, mContext,
                    mFileViewInteractionHub.getSelectedFileList().size());

            MenuInflater inflater = ((Activity) mContext).getMenuInflater();
            mMenu = menu;
            inflater.inflate(R.menu.operation_menu, mMenu);        
            mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
//            MenuItem selectAll = menu.findItem(R.id.action_select_all);
//            if (mFileViewInteractionHub.isSelectedAll()) {
//                selectAll.setIcon(lewa.R.drawable.ic_menu_clear_select);
//                selectAll.setTitle(R.string.operation_cancel_selectall);
//            } else {
//                selectAll.setIcon(lewa.R.drawable.ic_menu_select_all);
//                selectAll.setTitle(R.string.operation_selectall);
//            }
            // modify by jjlin for HotKnot Begin
            mHotKnotHelper = new HotKnotHelper(mContext);
            if (null != mHotKnotHelper) {
                mHotKnotHelper.initialize();
            }
            // modify by jjlin for HotKnot End
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mFileViewInteractionHub.isSelectedAll();
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mFileViewInteractionHub.addContextMenuSelectedItem();
            switch (item.getItemId()) {
                case R.id.action_delete :
                    mFileViewInteractionHub.onOperationDelete();
                    // added by weihong, #64755
//                    mode.finish();
                    break;
                case R.id.action_copy :
                    mFileViewInteractionHub.onOperationCopyPrepare();
                    mode.finish();
                    break;
                case R.id.action_move :
                    mFileViewInteractionHub.onOperationMovePrepare();
                    mode.finish();
                    break;
                case R.id.action_send :
                    mFileViewInteractionHub.onOperationSend();
                    mode.finish();
                    break;
//                case R.id.action_select_all :
//                    if (mFileViewInteractionHub.isSelectedAll()) {
//                        item.setIcon(lewa.R.drawable.ic_menu_select_all);
//                        item.setTitle(R.string.operation_cancel_selectall);
//                        // Delete for standalone by Fan.Yang
//                        mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
//                        mFileViewInteractionHub.clearSelection();
//                    } else {
//                        item.setIcon(lewa.R.drawable.ic_menu_clear_select);
//                        item.setTitle(R.string.operation_selectall);
//                        // Delete for standalone by Fan.Yang
//                        mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_clear_select);
//                        mFileViewInteractionHub.onOperationSelectAll();
//                        if (mode != null)
//                            mode.invalidate();
//                    }
//                    updateMenuItem();
//                    break;
                    
                case lewa.support.v7.appcompat.R.id.action_mode_right_button :
                    if (mFileViewInteractionHub.isSelectedAll()) {
                        item.setIcon(lewa.R.drawable.ic_menu_select_all);
                        item.setTitle(R.string.operation_cancel_selectall);
                        // Delete for standalone by Fan.Yang
                        mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
                        mFileViewInteractionHub.clearSelection();
                    } else {
                        item.setIcon(lewa.R.drawable.ic_menu_clear_select);
                        item.setTitle(R.string.operation_selectall);
                        // Delete for standalone by Fan.Yang
                        mode.setRightActionButtonResource(lewa.R.drawable.ic_menu_clear_select);
                        mFileViewInteractionHub.onOperationSelectAll();
                        if (mode != null)
                            mode.invalidate();
                    }
                    updateMenuItem();
                    break;
                case R.id.action_rename :
                    mFileViewInteractionHub.onOperationRename();
                    break;
                case R.id.action_fileinfo :
                    mFileViewInteractionHub.onOperationInfo();
                    break;
              /// M: added for HotKnot @{
                case R.id.action_hotknot:
                    // modify by jjlin for HotKnot Begin
//                    onHotknotShare();
                	getFileListUris();
            	    if (null != mHotKnotHelper) {
                  	    if (!mHotKnotHelper.isSending()) {
                  	        mHotKnotHelper.setHotknotListener(this);
                            mHotKnotHelper.startSend(mHotknotUris, (Activity)mContext);
                        } else {
                  	        mHotKnotHelper.stopSend();
                            mHotKnotHelper.setHotknotListener(null);
                  	    }
                    }
                    break;
                    // modify by jjlin for HotKnot End
            /// @}
            }

            Util.updateActionModeMenuTitle(mode, mContext,
                    mFileViewInteractionHub.getSelectedFileList().size());

            return false;
        }
    // modify by jjlin for HotKnot Begin
    public void onHotKnotSendComplete() {
        Log.d(LOG_TAG, "HotKnot: onHotKnotSendComplete");
        Message message = new Message();
        message.what = MSG_HOTKNOT_COMPLETE;
        mHandler.sendMessage(message);
    }
    
    @Override
	public void onHotKnotModeChanged(boolean isInShareMode) {
		// TODO Auto-generated method stub
    	Log.d(LOG_TAG, "HotKnot: onHotKnotModeChanged");
    	Message message = new Message();
        message.what = MSG_HOTKNOT_MODECHANGED;
        mHandler.sendMessage(message);
    }
    // modify by jjlin for HotKnot End
        /*
         * When ActionMode is shown, pressing hard back key, calls
         * onDestroyActionMode().
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // modify by jjlin for HotKnot Begin
        	if (null != mHotKnotHelper) {
            	mHotKnotHelper.finalize();
            }
            // modify by jjlin for HotKnot End
            mFileViewInteractionHub.setMode(Mode.View);
            mFileViewInteractionHub.clearSelection();
            mFileViewListener.onDataChanged();
            ((FileManagerMainActivity) mContext).setActionMode(null);
        }
    }

    public void updateMenuItem() {
        // TODO Auto-generated method stub
        if (getSelectedFileList().size() > 1) {
            // update menu, remove info and rename
            setActionMenuunable(true);
            removeInfoAndRenameMenu();
        } else if (getSelectedFileList().size() == 1) {
            // add info and rename
            setActionMenuunable(true);
            addInfoAndRenameMenu();
        } else if (getSelectedFileList().size() == 0) {
            updatehotknotmenu(); //add by jjlin for PR737086
            setActionMenuunable(false);
        }
    }
    
  //add by jjlin for hotknot begin
    public void updatehotknotmenu() {
        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
	if(actionMode != null){//PR766524 qhwu add
        Menu menu = actionMode.getMenu();
        // modify by jjlin for HotKnot Begin
        if (menu != null) {//PR766524 qhwu modify
        	if (null != mHotKnotHelper && mHotKnotHelper.isSending()) {
                menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot_press);
        	} else {
        		menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot); 
        	}
        }
        // modify by jjlin for HotKnot End
	}//PR766524 qhwu add
    }
    //add by jjlin for hotknot end 
    public void removeInfoAndRenameMenu() {
        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
        if(null != actionMode){
            Menu menu = actionMode.getMenu();
            if (menu.findItem(R.id.action_rename) != null) {
                menu.findItem(R.id.action_rename).setVisible(false);
            }
            if (menu.findItem(R.id.action_fileinfo) != null) {
                menu.findItem(R.id.action_fileinfo).setVisible(false);
            }
            // modify by jjlin for HotKnot Begin
            boolean bFondDir=false;
            for (int i=0;i<getSelectedFileList().size();i++) {
                 if (getSelectedFileList().get(i).IsDir)
	                 bFondDir=true;
            }
            if (bFondDir) {
                if (menu.findItem(R.id.action_hotknot) != null) {
        	        if (null != mHotKnotHelper) {
              	        mHotKnotHelper.stopSend();
              	        mHotKnotHelper.setHotknotListener(null);
                    }
                    menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot);
                    menu.findItem(R.id.action_hotknot).setEnabled(false);
                }
            }
            if (menu.findItem(R.id.action_hotknot) != null) {
                if (null != mHotKnotHelper && !mHotKnotHelper.isSending()) {
              	    menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot);
                }
            }
            // modify by jjlin for HotKnot End
        }
    }

    public void addInfoAndRenameMenu() {
        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
        if(null != actionMode){
            Menu menu = actionMode.getMenu();
            if (menu.findItem(R.id.action_rename) != null) {
                menu.findItem(R.id.action_rename).setVisible(true);
            }
            if (menu.findItem(R.id.action_fileinfo) != null) {
                menu.findItem(R.id.action_fileinfo).setVisible(true);
            }
            // modify by jjlin for HotKnot Begin
            if (getSelectedFileList().get(0).IsDir) {
                if (null != mHotKnotHelper) {
              	    mHotKnotHelper.stopSend();
              	    mHotKnotHelper.setHotknotListener(null);
                }
                if (menu.findItem(R.id.action_hotknot) != null) {
                    menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot);
                    menu.findItem(R.id.action_hotknot).setEnabled(false);
                }
            } else {
        	    if (menu.findItem(R.id.action_hotknot) != null) {
                    menu.findItem(R.id.action_hotknot).setEnabled(true);
        	    }
                if (getSelectedFileList().size() == 1) {
                    contentUri =Uri.fromFile(new File(getSelectedFileList().get(0).filePath));
                }
            }
            if (menu.findItem(R.id.action_hotknot) != null) {
                if (null != mHotKnotHelper && !mHotKnotHelper.isSending()) {
                    menu.findItem(R.id.action_hotknot).setIcon(R.drawable.ic_hotknot);
                }
            }
            // modify by jjlin for HotKnot End
        }
    }

    public void setActionMenuunable(boolean isAble) {
        // TODO Auto-generated method stub
        ActionMode actionMode = ((FileManagerMainActivity) mContext)
                .getActionMode();
        if(null != actionMode){
            Menu menu = actionMode.getMenu();
            menu.setGroupEnabled(0, isAble);
        }
    }
//add by jjlin for hotknot begin
    public void onHotknotShare() {
		ArrayList<FileInfo> selectedFileList = getSelectedFileList();

		  uris = new Uri[selectedFileList.size()];
		 for (int i=0;i<uris.length;i++) {
               uris[i] = Uri.fromFile(new File(selectedFileList.get(i).filePath));
               //Log.d(LOG_TAG, "HotKnot: uris="+uris[i]);
		 }
     }
//add by jjlin for hotknot end
    public void deleteFile(FileInfo f){
        ((FileManagerMainActivity) mContext).getFileOperationHelper().DeleteFile(f);
    }
    
    // add by jjlin for hotknot begin
    private Uri[] getFileListUris() {
    	ArrayList<FileInfo> selectedFileList = getSelectedFileList();

    	mHotknotUris = new Uri[selectedFileList.size()];
		 for (int i=0;i<mHotknotUris.length;i++) {
			 mHotknotUris[i] = Uri.fromFile(new File(selectedFileList.get(i).filePath));
			 if (getSelectedFileList().get(i).IsDir)
				 return null;
             //Log.d(LOG_TAG, "HotKnot: uris="+uris[i]);
		 }	
		 return mHotknotUris;
    }
    // add by jjlin for hotknot end
}
