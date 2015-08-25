package com.lewa.filemanager;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;
//LEWA ADD BEGIN
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
//LEWA ADD END
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lewa.filemanager.FileManagerMainActivity.IBackPressedListener;
import com.lewa.filemanager.FileViewInteractionHub.Mode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FileViewFragment extends Fragment
        implements
        IFileInteractionListener,
        IBackPressedListener,
        OnClickListener {
    public static final String EXT_FILTER_KEY = "ext_filter";
    private static final String LOG_TAG = "FileViewFragment";
    public static final String EXT_FILE_FIRST_KEY = "ext_file_first";
    public static final String ROOT_DIRECTORY = "root_directory";
    public static final String PICK_FOLDER = "pick_folder";
    private ListView mFileListView;
    // private TextView mCurrentPathTextView;
    private ArrayAdapter<FileInfo> mAdapter;
    private FileViewInteractionHub mFileViewInteractionHub;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;
    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();
    private Activity mActivity;
    private View mRootView;
    private View mFootView;
    // //for Lewa new scroll path. ---
    private String sdCardPath; // for scroll new path, each fragment maintains
    // one.
    private String sdCardRootPath;
    private String sdCardName;
    protected static final String SEPARATOR = "/";
    private static final long NAV_BAR_AUTO_SCROLL_DELAY = 100;
    // private NavigationHistory mNavHistory = null;
    private int mTabsCounter = -1;
    // private Button mBlankTab = null;
    // maximum tab text length is 10
    private static final int TAB_TEXT_LENGTH = 20;
    private RelativeLayout mBarLayout;
    private HorizontalScrollView mNavigationBar;
    private LinearLayout mTabsHolder;
    protected List<FileInfo> mFileInfoList = null;
    protected List<FileInfo> mPreveFileInfoList = null;
    protected ProgressBar mListProgress = null;
    private ProgressDialog progressDialog;
    // //-------

    // private String sdDir; //= Util.getSdDirectory(); // "/mnt/sdcard"; TODO:
    // need to check.
    // memorize the scroll positions of previous paths
    private ArrayList<PathScrollPositionItem> mScrollPositionList = new ArrayList<PathScrollPositionItem>();
    private String mPreviousPath;
    // add by luoyongxing
    private int mSDIndex;
    
    private String dirName = null;
    private String dirPath = null;
    private FileInfo f  = null;
    private static final int MK_DIR_CODE = 1;
    private static final int COPY_FILE_CODE = 2;
    private static final int MOVE_FILE_CODE = 3; 
    private static final int DELETE_FILE_CODE = 4;
    private static int fileNum = 0;
    public static boolean isShouldRefresh = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String type = intent.getType();
            Log.e(LOG_TAG, "received broadcast:" + intent.toString());
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || (!TextUtils.isEmpty(type) && type.equals("audio/*"))) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateNavigationBar(0);
                        if (sdCardRootPath == "" || sdCardRootPath == null) {
                            setSDCardRootPath(Util.sSDCardDir[0]);
                        }

                        if(2 == Util.getSdcardMountedNum()){
                            if(0 == mSDIndex){
                                mFileViewInteractionHub.setCurrentPath(Util.sSDCardDir[0]);
                            }else if(1 == mSDIndex){
                                mFileViewInteractionHub.setCurrentPath(Util.sSDCardDir[1]);
                            }
                        }else{
                            mFileViewInteractionHub.setCurrentPath(sdCardRootPath);
                        }
                        updateUI();
                    }
                });
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                mFileViewInteractionHub.refreshFileList();
            }
        }
    };

    private boolean mBackspaceExit;

    public ProgressBar getProgressBar() {
        return mListProgress;
    }

    public void setSDCardPath(String sdcardPath) {
        sdCardPath = sdcardPath;
        mSDIndex = Util.getSDIndex(sdcardPath);
    }

    public String getSDCardPath() {
        return sdCardPath;
    }

    public String getSDCardRootPath() {
        return sdCardRootPath;
    }

    public void setSDCardRootPath(String sdcardRootPath) {
        sdCardRootPath = sdcardRootPath;
    }

    public void setSDCardName(String sdcardName) {
        sdCardName = sdcardName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();
        synchronized (mActivity) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(mActivity);
            }
            progressDialog.setMessage(mActivity
                    .getString(R.string.loadingfiles));
            progressDialog.setCancelable(false);
            progressDialog.show();
            mRootView = inflater.inflate(R.layout.file_explorer_list,
                    container, false);
            progressDialog.dismiss();
        }
        // For new scroll path.
        mBarLayout = (RelativeLayout) mRootView.findViewById(R.id.path_bar);
        mNavigationBar = (HorizontalScrollView) getViewById(R.id.navigation_bar);
        mTabsHolder = (LinearLayout) getViewById(R.id.tabs_holder);
        mNavigationBar.setVerticalScrollBarEnabled(false);
        mNavigationBar.setHorizontalScrollBarEnabled(false);
        mListProgress = (ProgressBar) mRootView
                .findViewById(R.id.refresh_loadingprogressbar);

        ActivitiesManager.getInstance().registerActivity(
                ActivitiesManager.ACTIVITY_FILE_VIEW, mActivity);

        mFileViewInteractionHub = new FileViewInteractionHub(this);
        mFileViewInteractionHub.setFileViewFragmentObj(this);
        
        mFileCagetoryHelper = new FileCategoryHelper(mActivity,
                mFileViewInteractionHub);
        Intent intent = mActivity.getIntent();
        String action = intent.getAction();

        if (!TextUtils.isEmpty(action)
                && (action.equals(Intent.ACTION_PICK)
                        || action.equals(Intent.ACTION_GET_CONTENT) || action
                            .equals(Util.FILE_PICK_ACTION))) {
            mFileViewInteractionHub.setMode(Mode.Pick);

            boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
            if (!pickFolder) {
                String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
                if (exts != null) {
                    mFileCagetoryHelper.setCustomCategory(exts);
                }
            } else {
                mFileCagetoryHelper.setCustomCategory(new String[]{} /*
                                                                      * folder
                                                                      * only
                                                                      */);
                mRootView.findViewById(R.id.pick_operation_bar).setVisibility(
                        View.VISIBLE);

                mRootView.findViewById(R.id.button_pick_confirm)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    Intent intent = Intent.parseUri(
                                            mFileViewInteractionHub
                                                    .getCurrentPath(), 0);
                                    mActivity.setResult(Activity.RESULT_OK,
                                            intent);
                                    mActivity.finish();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                mRootView.findViewById(R.id.button_pick_cancel)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                mActivity.finish();
                            }
                        });
            }
        } else {
            mFileViewInteractionHub.setMode(Mode.View);
        }

        mFootView = inflater.inflate(R.layout.list_item_footview, null);
        mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
//        mFileListView.addFooterView(mFootView);
        mFootView.setVisibility(View.INVISIBLE);
        //#939674 add begin by bin.dong 
        refreshApkFile();
      //#939674 add end by bin.dong 
        mFileIconHelper = new FileIconHelper(mActivity);
        mAdapter = new FileListAdapter(mActivity, R.layout.file_browser_item,
                mFileNameList, mFileViewInteractionHub, mFileIconHelper);

        Uri uri = intent.getData();
        mBackspaceExit = (uri != null)
                && (TextUtils.isEmpty(action) || (!action
                        .equals(Intent.ACTION_PICK)
                        && !action.equals(Intent.ACTION_GET_CONTENT) || !action
                            .equals(Util.FILE_PICK_ACTION)));

        mFileListView.setAdapter(mAdapter);

        onRefreshPathBar(sdCardPath, 0);
        if ("view_directory".equals(action)) {
            String recordDirectory = intent.getData().getPath();
            if (recordDirectory.startsWith(sdCardRootPath)) {
                File file = new File(recordDirectory);
                if (!file.exists()) {
                    file.mkdirs();
                }
                if (recordDirectory != null && recordDirectory != "") {
                    mFileViewInteractionHub.setCurrentPath(recordDirectory);
                    String[] paths = recordDirectory.replaceAll(sdCardPath, "")
                            .split("/");
                    for (int i = 1; i < paths.length; i++) {
                        if (paths[i] != null && paths[i] != "") {
                            addTab(paths[i]);
                        }
                    }
                }
            } else {
                mFileViewInteractionHub.setRootPath(sdCardPath);
                mFileViewInteractionHub.setCurrentPath(sdCardPath);
            }
        } else {
            mFileViewInteractionHub.setRootPath(sdCardPath);
            mFileViewInteractionHub.setCurrentPath(sdCardPath);
        }

        /*
         * // Open search file directory 2013-03-26
         */

        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            String str = bundle.getString("searchpath");
            //#942076 modify begin by bin.dong
            if (str != null && str != ""&&str.contains(sdCardPath)) {
              //#942076 modify end by bin.dong
                mFileViewInteractionHub.setCurrentPath(str);
                String[] paths = str.replaceAll(sdCardPath, "").split("/");
                for (int i = 1; i < paths.length; i++) {
                    if (paths[i] != null && paths[i] != "") {
                        addTab(paths[i]);
                    }
                }
            }
        }

        mFileViewInteractionHub.refreshFileList(); // Refresh the scroll path

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        mActivity.registerReceiver(mReceiver, intentFilter);

        updateUI();

        setHasOptionsMenu(true);
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mActivity.unregisterReceiver(mReceiver);
    }

    /**
     * Set has optionsMenu or not.
     * 
     * @param optionsMenu
     */
    public void setOptionsMenu(boolean optionsMenu) {
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean sdCardReady = Util.isSDCardReady(mSDIndex);
        if (!sdCardReady) {
            return;
        }
        int fragmentIndex = ((FileManagerMainActivity) getActivity())
                .getCurrentItemIndex();

        if (fragmentIndex == 1 && getTag() == "SDCARD_TAG") {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        } else if (fragmentIndex == 2 && getTag() == "SDCARD_TAG2") {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        } else if (getTag() == "SDCARD_TAG3") {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        }
        // added by weihong, #65379
//        if (fragmentIndex > 0){
//            mFileViewInteractionHub.onCreateOptionsMenu(menu);
//        }
        mFileViewInteractionHub.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean sdCardReady = Util.isSDCardReady(mSDIndex);
        if (!sdCardReady) {
            return;
        }
        int fragmentIndex = ((FileManagerMainActivity) getActivity())
                .getCurrentItemIndex();

        if (fragmentIndex == 1
                && getTag() == FileManagerMainActivity.SDCARD_TAG) {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        } else if (fragmentIndex == 2
                && getTag() == FileManagerMainActivity.SDCARD_TAG2) {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        } else if ((fragmentIndex == 3 && getTag() == FileManagerMainActivity.SDCARD_TAG3)
                || (fragmentIndex == 2 && getTag() == FileManagerMainActivity.SDCARD_TAG3)) {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        }
        // added by weihong, #65379
//        if (fragmentIndex > 0){
//            mFileViewInteractionHub.onCreateOptionsMenu(menu);
//        }

    }

    @Override
    public boolean onBack() {
        if (mActivity == null)
            return false;
        if (mBackspaceExit || !Util.hasSDCardReady(mActivity)
                || mFileViewInteractionHub == null) {
            return false;
        }
        return mFileViewInteractionHub.onBackPressed();
    }

    private class PathScrollPositionItem {
        String path;
        int pos;

        PathScrollPositionItem(String s, int p) {
            path = s;
            pos = p;
        }
    }

    // execute before change, return the memorized scroll position
    private int computeScrollPosition(String path) {
        int pos = 0;
        if (mPreviousPath != null) {
            if (path.startsWith(mPreviousPath)) {
                int firstVisiblePosition = mFileListView
                        .getFirstVisiblePosition();
                if (mScrollPositionList.size() != 0
                        && mPreviousPath.equals(mScrollPositionList
                                .get(mScrollPositionList.size() - 1).path)) {
                    mScrollPositionList.get(mScrollPositionList.size() - 1).pos = firstVisiblePosition;
                    pos = firstVisiblePosition;
                } else {
                    mScrollPositionList.add(new PathScrollPositionItem(
                            mPreviousPath, firstVisiblePosition));
                }
            } else {
                int i;
                for (i = 0; i < mScrollPositionList.size(); i++) {
                    if (!path.startsWith(mScrollPositionList.get(i).path)) {
                        break;
                    }
                }
                // navigate to a totally new branch, not in current stack
                if (i > 0) {
                    pos = mScrollPositionList.get(i - 1).pos;
                }
                for (int j = mScrollPositionList.size() - 1; j >= i - 1
                        && j >= 0; j--) {
                    mScrollPositionList.remove(j);
                }
            }
        }
        // Log.i(LOG_TAG, "computeScrollPosition: result pos: " + path + " " +
        // pos + " stack count:" + mScrollPositionList.size());
        mPreviousPath = path;
        return pos;
    }

    private static final int MSG_LIST_PROGRESS_BAR_GONE = 8;
    private static final int MSG_LIST_PROGRESS_BAR_VISIBLE = 0;
    private Timer timer;
    private Handler handler = new Handler() {
        // private View mCategoryProgressBar;
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LIST_PROGRESS_BAR_GONE :
                    mListProgress.setVisibility(View.GONE);
                    break;
                case MSG_LIST_PROGRESS_BAR_VISIBLE :
                    mListProgress.setVisibility(View.VISIBLE);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void setListProgressBarShow(final int show) {
        if (timer != null) {
            timer.cancel();
        }
        // mCategoryProgressBar.setVisibility(View.VISIBLE);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                timer = null;
                Message message = new Message();
                message.what = show;
                handler.sendMessage(message);
            }

        }, 100);
    }

    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        File file = null;
        try {
            file = new File(path);
        } catch (Exception ex) {
            file = null;
        }
        if (file == null || !file.exists() || !file.isDirectory()) {
            return false;
        }
        final int pos = computeScrollPosition(path);
        ArrayList<FileInfo> fileList = mFileNameList;
        if (fileList != null && fileList.size() > 0) {
            fileList.clear();
        }
        File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
        if (listFiles == null)
            return true;

        boolean showHiddenFiles = Settings.instance()
                .getShowDotAndHiddenFiles();
        for (File child : listFiles) {
            String absolutePath = child.getAbsolutePath();
            if (Util.isNormalFile(absolutePath)
                    && Util.shouldShowFile(absolutePath, sdCardRootPath)) {
                FileInfo lFileInfo = Util.GetFileInfo(child,
                        mFileCagetoryHelper.getFilter(), showHiddenFiles);
                if (lFileInfo != null) {
                    fileList.add(lFileInfo);
                }
            }
        }
        sortCurrentList(sort);
        showEmptyView(fileList.size() == 0);
        mFileListView.post(new Runnable() {
            @Override
            public void run() {
                mFileListView.setSelection(pos);
                setListProgressBarShow(View.GONE);
            }
        });
        return true;
    }
    //#939674 add begin by bin.dong
    public void refreshApkFile(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
        .equals(android.os.Environment.MEDIA_MOUNTED); 
        if (sdCardExist){
            sdDir = Environment.getExternalStorageDirectory();
        }
        String path = sdDir.toString();
        int countApk = 0;
        File file = null;
        try {
            file = new File(path);
        } catch (Exception ex) {
            file = null;
        }
        File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
        Log.e(LOG_TAG, "length:"+listFiles.length);   
        for (File child : listFiles) {
            if(child.getName().endsWith(".apk")){
                countApk ++;
            }
        }        
        Log.e(LOG_TAG, "fileNum:"+fileNum+",countapk:"+countApk); 
        if(fileNum != countApk){
            for (File child : listFiles) {
                if (child.getName().endsWith(".apk")) {
                    mFileViewInteractionHub.notifyFileSystemChanged(child.getAbsolutePath());
                    Log.e(LOG_TAG, "apkname:" + child.getName());
                }
            }
            isShouldRefresh = true;
        }
        fileNum = countApk;
    }
    //#939674 add begin by bin.dong
    public boolean onRefreshEditFileList(String path, FileSortHelper sort) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        final int pos = computeScrollPosition(path);
        ArrayList<FileInfo> fileList = mFileNameList;

        if (mFileViewInteractionHub.getSelectedFileList().size() <= 0
                && mFileViewInteractionHub.getMode() != Mode.Pick) {
            mFileViewInteractionHub.setMode(Mode.View);

        } else if (mFileViewInteractionHub.getMode() != Mode.Pick) {
            mFileViewInteractionHub.setMode(Mode.Edit);
        }
        sortCurrentList(sort);
        showEmptyView(fileList.size() == 0);

        if (mFileViewInteractionHub.getSelectedFileList().size() > 0
                && fileList.size() > 1) {
            FileInfo tmpfileInfo = mFileViewInteractionHub
                    .getSelectedFileList().get(0);
            boolean isResetScrollPos = true;
            if (tmpfileInfo == fileList.get(fileList.size() - 1)
                    || tmpfileInfo == fileList.get(fileList.size() - 2)) {
                isResetScrollPos = false;
            }

            if (isResetScrollPos) {
                mFileListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mFileListView.setSelection(pos);
                        setListProgressBarShow(View.GONE);
                    }
                });
            }
        }
        return true;
    }
    
    public boolean onRefreshEditFileList(String path, FileSortHelper sort,int position) {
    	Log.v("Gracker", "onRefreshEditFileList path = "+ path);
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        
        
        int firstVisiblePosition = mFileListView
                .getFirstVisiblePosition();
        final int pos = position - (position - firstVisiblePosition);
        
        ArrayList<FileInfo> fileList = mFileNameList;

        if (mFileViewInteractionHub.getSelectedFileList().size() <= 0
                && mFileViewInteractionHub.getMode() != Mode.Pick) {
            mFileViewInteractionHub.setMode(Mode.View);

        } else if (mFileViewInteractionHub.getMode() != Mode.Pick) {
            mFileViewInteractionHub.setMode(Mode.Edit);
        }
        sortCurrentList(sort);
        showEmptyView(fileList.size() == 0);

        if (mFileViewInteractionHub.getSelectedFileList().size() > 0
                && fileList.size() > 1) {
            FileInfo tmpfileInfo = mFileViewInteractionHub
                    .getSelectedFileList().get(0);
            boolean isResetScrollPos = true;
            if (tmpfileInfo == fileList.get(fileList.size() - 1)
                    || tmpfileInfo == fileList.get(fileList.size() - 2)) {
                isResetScrollPos = false;
            }

            if (isResetScrollPos) {
                mFileListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mFileListView.setSelection(pos);
                        setListProgressBarShow(View.GONE);
                    }
                });
            }
        }
        return true;
    }

    private void updateUI() {
        mListProgress.setVisibility(View.VISIBLE);
        boolean sdCardReady = Util.isSDCardReady(mSDIndex);
        View noSdView = mRootView.findViewById(R.id.sd_not_available_page);
        noSdView.setVisibility(sdCardReady ? View.GONE : View.VISIBLE);
        if (noSdView.getVisibility() == View.VISIBLE) {
            showEmptyView(false);
        }
        mFileListView.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);
        mBarLayout.setVisibility(sdCardReady ? View.VISIBLE : View.GONE);
        if (sdCardReady) {
            mFileViewInteractionHub.refreshFileList();
        }
        // mListProgress.setVisibility(View.GONE);
    }

    private void showEmptyView(boolean show) {
        View emptyView = mRootView.findViewById(R.id.empty_view);
        if (emptyView != null)
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public View getViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }

        });
    }

    @Override
    public void onPick(FileInfo f) {
        try {
            Intent intent = Intent.parseUri(Uri.fromFile(new File(f.filePath))
                    .toString(), 0);
            mActivity.setResult(Activity.RESULT_OK, intent);
            mActivity.finish();
            // Log.d("yx", "FileView -> onPick filePath: " + f.filePath);
            return;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldShowOperationPane() {
        return true;
    }

    @Override
    public boolean onOperation(int id) {
        return false;
    }

    // // 支持显示真实路径
    // @Override
    // public String getDisplayPath(String path) {
    // // this.sdDir = this.sdCardPath;
    // if (path.startsWith(this.sdCardRootPath)//.sdDir)
    // && !FileManagerPreferenceActivity.showRealPath(mActivity)) {
    // return path;//getString(R.string.sd_folder)
    // // + path.substring(this.sdDir.length());
    // } else {
    // return path;
    // }
    // }
    //
    // @Override
    // public String getRealPath(String displayPath) {
    // final String perfixName =
    // this.sdCardRootPath;//getString(R.string.sd_folder);
    // // this.sdDir = this.sdCardPath;
    // if (displayPath.startsWith(perfixName)) {
    // return this.sdCardRootPath + displayPath.substring(perfixName.length());
    // } else {
    // return displayPath;
    // }
    // }

    // @Override
    // public String getFullPath(int position, Object obj) {
    // String path = Util.getSdDirectory(mSDIndex);// new String("/");
    // for (int i = 1; i <= position; i++) {
    // path = path + ((TextGalleryAdapter) obj).getPath(position);//
    // TextGalleryAdapter.
    // }
    // return path;
    // }

    // @Override
    // public boolean onNavigation(String path) {
    // return false;
    // }

    @Override
    public boolean shouldHideMenu(int menu) {
        return false;
    }

    // public void copyFile(ArrayList<FileInfo> files) {
    // mFileViewInteractionHub.onOperationCopy(files);
    // }

    public void refresh() {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    public void moveToFile(ArrayList<FileInfo> files) {
        mFileViewInteractionHub.moveFileFrom(files);
    }

    public interface SelectFilesCallback {
        // files equals null indicates canceled
        void selected(ArrayList<FileInfo> files);
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        mFileViewInteractionHub.startSelectFiles(callback);
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    public boolean setPath(String location) {
        if (!location.startsWith(mFileViewInteractionHub.getRootPath())) {
            return false;
        }
        mFileViewInteractionHub.setCurrentPath(location);
        mFileViewInteractionHub.refreshFileList();
        return true;
    }

    @Override
    public FileInfo getItem(int pos) {
        if (pos < 0 || pos > mFileNameList.size() - 1)
            return null;

        return mFileNameList.get(pos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sortCurrentList(FileSortHelper sort) {
        Collections.sort(mFileNameList, sort.getComparator());
        onDataChanged();
    }

    @Override
    public ArrayList<FileInfo> getAllFiles() {
        return mFileNameList;
    }

    @Override
    public void addSingleFile(FileInfo file) {
        mFileNameList.add(file);
        onDataChanged();
    }

    @Override
    public int getItemCount() {
        return mFileNameList.size();
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    // --------Lewa new scroll path bar.

    /**
     * This method creates tabs on the navigation bar
     * 
     * @param text
     *            the name of the tab
     */
    public void addTab(String text) {

        if (text == null) {
            return;
        }
        LinearLayout.LayoutParams mlp;
        ++mTabsCounter;

        // set button style
        Button btn = new Button(getActivity());
        btn.setId(mTabsCounter);
        Drawable drawable = getResources().getDrawable(R.drawable.path_tail);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                drawable.getMinimumHeight());
        Drawable drawable2 = getResources().getDrawable(R.drawable.path_head);
        drawable2.setBounds(0, 0, drawable2.getMinimumWidth(),
                drawable2.getMinimumHeight());
        btn.setCompoundDrawables(drawable2, null, drawable, null);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(Color.TRANSPARENT);

        mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mlp.setMargins(0, 0, 0, 0);
        btn.setPadding(0, 0, 0, 2);
        btn.setLayoutParams(mlp);
        // btn.setTextColor(Color.BLACK);
        if (text.length() <= TAB_TEXT_LENGTH) {
            btn.setText(text);
        } else {
            btn.setText(text.substring(0, TAB_TEXT_LENGTH));
        }
        // add button to the tab holder
        mTabsHolder.addView(btn);
        btn.setOnClickListener(this);
        // set the press background for path as belowing:
        btn.setOnTouchListener(new Button.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundResource(lewa.R.drawable.android_list_selected_holo_light);
                } else if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    // v.setBackgroundDrawable(mResources.getDrawable(R.drawable.path));
                    v.setBackgroundColor(Color.TRANSPARENT);
                }
                return false;
            }
        });

        if (mTabsCounter > 0) {
            sdCardPath = sdCardPath + SEPARATOR + text;
        }

        // scroll horizontal view to the right
        mNavigationBar.postDelayed(new Runnable() {
            public void run() {
                mNavigationBar.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, NAV_BAR_AUTO_SCROLL_DELAY);
    }

    /**
     * The method updates the navigation bar
     * 
     * @param id
     *            the tab id that was clicked
     */
    protected void updateNavigationBar(int id) {
        if (id < mTabsCounter) {
            int childCount = mTabsHolder.getChildCount();
            int count = Math.min(mTabsCounter - id, childCount);
            if (id + 1 >= 0 && count >= 0) {
                mTabsHolder.removeViews(id + 1, count);
                mTabsCounter = id;
            } else {
                Log.e(LOG_TAG, "ERROR! updateNavigationBar invalid id:" + id
                        + " mTabsCounter:" + mTabsCounter + " childCount:"
                        + childCount);
            }

            int rootSize = sdCardRootPath.split("/").length;
            String[] path = sdCardPath.split("/");
            String temp = sdCardRootPath;
            // Reset the current path.
            for (int i = rootSize; i < id + rootSize; i++) {
                if (i < path.length) {
                    temp = temp + SEPARATOR + path[i];
                } else {
                    Log.e(LOG_TAG, "ERROR!! updateNavigationBar, path:"
                            + sdCardPath + ", root path:" + sdCardRootPath);
                }
            }
            sdCardPath = temp;

        }

        // updateHomeButton();
    }

    /**
     * A callback method to be invoked when a tab on the navigation bar has been
     * clicked
     * 
     * @param view
     *            the tab view that was clicked
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();
        // FileManagerLog.d(TAG, "onClick: " + id);
        ActionMode actionMode = ((FileManagerMainActivity) getContext())
                .getActionMode();
        if (actionMode != null) {
            actionMode.finish();
        }
        // FIXME: temporarily keep origin version code, delete it in future
        if (id < mTabsCounter) {
            updateNavigationBar(id);
            mFileViewInteractionHub.setCurrentPath(sdCardPath);
            mFileViewInteractionHub.refreshFileList();
        }
    }

    /**
     * This function refresh the new scroll bar path: show current path id:
     * clicking tab id
     */
    @Override
    public boolean onRefreshPathBar(String currentPath, int clickID) {
        assert (currentPath != null);
        // new feature: scroll path.

        getResources();

        if (mTabsCounter < 0) { // when first time run: /mnt/sdcard path
            // path != "" && path != "/"
            addTab(sdCardName);
        } else {
            updateNavigationBar(clickID);
            sdCardPath = currentPath;

        }
        return true;
    }

    public FileViewInteractionHub getFileViewInteractionHub() {
        return mFileViewInteractionHub;
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort,
            int sdcardIndex) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getFragmentTag() {
        return getTag();
    }
    /*
    //剪切文件到SD卡
    public void moveFileToSDCard(FileInfo f){
        this.f = f;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, MOVE_FILE_CODE);
    }
    //复制文件到SD卡
    public void copyFileToSDCard(FileInfo f){
        this.f = f;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, COPY_FILE_CODE);
    }
    //复制文件
    private void copyFile(DocumentFile pickedDir, File file) {
        Log.e(LOG_TAG, "opy file ");
        if (file.isDirectory()) {
            Log.e(LOG_TAG, "file.isDirectory");
            DocumentFile docFile1 = pickedDir.findFile(file.getName());
            String fileName = file.getName();
            if(docFile1 != null){
                fileName+="_1";
            }
            pickedDir.createDirectory(fileName);
            DocumentFile docFile = pickedDir.findFile(fileName);
            Log.e(LOG_TAG, "docfile:"+docFile.getName()+",length:"+docFile.length());
            if (0 == file.list().length) {//if source folder is empty.
            } else {
                FilenameFilter mFilter = null;
                for (File child : file.listFiles(mFilter)) {
                    copyFile(docFile, child);
                }
            }
        } else {
            Log.e(LOG_TAG, "file is not dir");
            InputStream fi = null;
            try {
                fi = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "file not found :" + e.getMessage());
                e.printStackTrace();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(fi));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        //    DocumentFile newFile1 = pickedDir.findFile(file.getName());
            String fileName = file.getName();
           
            DocumentFile newFile = pickedDir.createFile("text/plain", fileName);
            OutputStream outputStream = null;
            try {
                outputStream = this.getActivity().getContentResolver().openOutputStream(newFile.getUri());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    writer.write(stringBuilder.toString());
                    writer.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //从sd卡删除文件
    public void deleteFileFromSDCard(FileInfo f){
        this.f = f;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, DELETE_FILE_CODE);
    }
    //删除文件
    public void deleteFile(DocumentFile pickedDir,FileInfo f){
        String[] source = f.filePath.split("/");
        for(int i=3;i<source.length;i++){
            Log.e(LOG_TAG, "source:"+source[i]);
           pickedDir = pickedDir.findFile(source[i]);
        }
           pickedDir.delete();
    }
    //在sd卡创建文件夹
    public void mkDir(String path,String dirName){
        this.dirName = dirName;
        this.dirPath = path;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, MK_DIR_CODE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == this.getActivity().RESULT_OK) {
            Uri treeUri = resultData.getData();
            if (requestCode == MK_DIR_CODE) {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this.getActivity(), treeUri);
                String[] source = dirPath.split("/");
                for(int i=3;i<source.length;i++){
                   pickedDir = pickedDir.findFile(source[i]);
                }
                pickedDir.createDirectory(dirName);
              //#943494 add begin by bin.dong
                mFileViewInteractionHub.notifyFileSystemChanged(dirPath+"/"+dirName);
              //#943494 add end by bin.dong
            }else if(requestCode == COPY_FILE_CODE){
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this.getActivity(), treeUri);
                File file = new File(f.filePath);
                copyFile(pickedDir, file);
            }else if(requestCode == MOVE_FILE_CODE){
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this.getActivity(), treeUri);
                File file = new File(f.filePath);
                copyFile(pickedDir, file);
                mFileViewInteractionHub.deleteFile(f);
            }else if(requestCode == DELETE_FILE_CODE){
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this.getActivity(), treeUri);
                deleteFile(pickedDir, f);
            }
        }
        mFileViewInteractionHub.refreshFileList();
    }
    */
}
