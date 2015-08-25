package com.lewa.filemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.lewa.filemanager.FileCategoryHelper.CategoryInfo;
import com.lewa.filemanager.FileCategoryHelper.FileCategory;
import com.lewa.filemanager.FileManagerMainActivity.IBackPressedListener;
import com.lewa.filemanager.FileSortHelper.SortMethod;
import com.lewa.filemanager.FileViewInteractionHub.Mode;
import com.lewa.filemanager.Util.SDCardInfo;
import com.lewa.filemanager.R;
import com.lewa.search.LewaSearchActivity;

public class FileCategoryFragment extends Fragment
        implements
            IFileInteractionListener,
            IBackPressedListener,
            OnClickListener {

    public static final String LOG_TAG = "FileCategoryFragment";
    public static final String EXT_FILTER_KEY = "ext_filter";
    public static final String EXT_FILE_FIRST_KEY = "ext_file_first";
    public static final String ROOT_DIRECTORY = "root_directory";
    public static final String PICK_FOLDER = "pick_folder";
    private static HashMap<Integer, FileCategory> button2Category = new HashMap<Integer, FileCategory>();
    private HashMap<FileCategory, Integer> categoryIndex = new HashMap<FileCategory, Integer>();
    private FileListCursorAdapter mAdapter;
    private FileViewInteractionHub mFileViewInteractionHub;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;
    // mCategoryBar: progress bar on bottom shows category information for
    // music, picture, video, etc.
    private CategoryBar mCategoryBar0;
    private CategoryBar mCategoryBar1;
    private CategoryBar mCategoryBarOtg;
    private ScannerReceiver mScannerReceiver;
    private ViewPage curViewPage = ViewPage.Invalid;
    private ViewPage preViewPage = ViewPage.Invalid;
    private Activity mActivity;
    private View mRootView;
    private FileViewFragment mFileViewActivity;
    // //for new scroll path. ---
    private String sdCardPath; // for scroll new path
    private String sdCardName;
    protected static final String SEPARATOR = "/";
    private static final long NAV_BAR_AUTO_SCROLL_DELAY = 100;
    private int mTabsCounter = -1;
    // private Button mBlankTab = null;
    // maximum tab text length is 10
    private static final int TAB_TEXT_LENGTH = 20;
    // private List<String> mTabNameList = null;
    private RelativeLayout mBarLayout;
    private HorizontalScrollView mNavigationBar;
    private LinearLayout mTabsHolder;
    // luoyongxing
    private boolean mDualSD = false;
    public static final int SD0_INDEX = 0;
    public static final int SD1_INDEX = 1;
    public static final int CATEGORY_BAR0_INDEX = 0;
    public static final int CATEGORY_BAR1_INDEX = 1;
    public static final int CATEGORY_BAROTG_INDEX = 2;
    private ProgressBar mCategoryProgressBar = null;
    protected ProgressBar mListProgress = null;
    // //---
    ListView fileListView;
    private View mFootView;
    private boolean mConfigurationChanged = false;
//    private FileListIndex mListIndex;
    public static final int REQ_IMAGE_CROP = 1;

    public static int mStorageLocationFilter = -1;

    public enum FileListIndex {
        NoIndex, Name, Size, Date, type
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        if (curViewPage == ViewPage.Category) {
            if (mFileCagetoryHelper.getCurCategory() == FileCategory.Theme) {
                scanThemeInstallPath();
            }
            //#952620 modify begin by bin.dong
            if (mFileCagetoryHelper.getCurCategory() == FileCategory.Apk&&mFileViewInteractionHub.getMode().toString()!= "Edit") {
            //#952620 modify end by bin.dong
                mFileViewInteractionHub.refreshFileList();
            }
        } else if (curViewPage == ViewPage.Home) {
            // added by weihong, #65370
            if (isAdded()) {
                refreshCategoryInfo();
            }
        }
        super.onResume();
    }

    public void setConfigurationChanged(boolean changed) {
        mConfigurationChanged = changed;
    }

    public ProgressBar getProgressBar() {
        return mCategoryProgressBar;
    }

    public FileCategoryHelper getFileCategoryHelper() {
        return mFileCagetoryHelper;
    }

    static {
        button2Category.put(R.id.category_music, FileCategory.Music);
        button2Category.put(R.id.category_video, FileCategory.Video);
        button2Category.put(R.id.category_picture, FileCategory.Picture);
        button2Category.put(R.id.category_theme, FileCategory.Theme);
        button2Category.put(R.id.category_document, FileCategory.Doc);
        // button2Category.put(R.id.category_zip, FileCategory.Zip);
        button2Category.put(R.id.category_apk, FileCategory.Apk);
        // button2Category.put(R.id.category_favorite, FileCategory.Favorite);
    }

    public void setSDCardPath(String sdcardPath) {
        sdCardPath = sdcardPath;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        mDualSD = Util.isSupportDualSDCard();
        // setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();

        // For refresh both tabs after installing SDcard.SDCARD_TAB_INDEX
        // CATEGORY_TAB_INDEX
        mFileViewActivity = (FileViewFragment) ((FileManagerMainActivity) mActivity)
                .getFragment(Util.SDCARD_TAB_INDEX);

        mRootView = inflater.inflate(R.layout.file_explorer_category,
                container, false);
        mBarLayout = (RelativeLayout) mRootView.findViewById(R.id.path_bar);

        // add search item
        addSearchViewItem(mRootView);

        // For Lewa new scroll path.
        View customBarView = inflater.inflate(R.layout.title_layout, null);

        mNavigationBar = (HorizontalScrollView) customBarView
                .findViewById(R.id.navigation_bar);
        mTabsHolder = (LinearLayout) customBarView
                .findViewById(R.id.tabs_holder);
        mListProgress = (ProgressBar) customBarView
                .findViewById(R.id.refresh_loadingprogressbar);
        mBarLayout.addView(customBarView);
        mBarLayout.invalidate();
        getResources();

        onRefreshPathBar("", 0);
        mNavigationBar.setVerticalScrollBarEnabled(false);
        mNavigationBar.setHorizontalScrollBarEnabled(false);
        mCategoryProgressBar = (ProgressBar) mRootView
                .findViewById(R.id.refresh_category_progress);

        curViewPage = ViewPage.Invalid;
        mFileViewInteractionHub = new FileViewInteractionHub(this);
        Intent intent = mActivity.getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (!TextUtils.isEmpty(action)
                && (action.equals(Intent.ACTION_PICK)
                        || action.equals(Intent.ACTION_GET_CONTENT) || action
                            .equals(Util.FILE_PICK_ACTION))
                || (!TextUtils.isEmpty(type) && type.equals("audio/*"))) {
            mFileViewInteractionHub.setMode(Mode.Pick);

            // bellow let files can be sent by message and browser like UC
            // browser.
            boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
            if (!pickFolder) {
                String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
                if (exts != null) {
                    mFileCagetoryHelper.setCustomCategory(exts);
                }
            }
        } else {
            mFileViewInteractionHub.setMode(Mode.View);
        }
        mFileViewInteractionHub.setRootPath("/");
        mFileIconHelper = new FileIconHelper(mActivity);

//        setFileListIndex(FileListIndex.NoIndex);
        mAdapter = new FileListCursorAdapter(mActivity, null,
                mFileViewInteractionHub, mFileIconHelper);

        mFootView = inflater.inflate(R.layout.list_item_footview, null);
        fileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
//        fileListView.addFooterView(mFootView);
        mFootView.setVisibility(View.INVISIBLE);
        fileListView.setAdapter(mAdapter);
        setupClick();
        setupCategoryInfo(); // show progress bar information.
        getFileCategoryHelper().getCategoryInfo(
                getFileCategoryHelper().getCurCategory())
                .setCategorySDCardIndex(2);
        // updateUI();
        registerScannerReceiver();
        setHasOptionsMenu(true);
        return mRootView;
    }

    private void registerScannerReceiver() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        mActivity.registerReceiver(mScannerReceiver, intentFilter);
    }

    private void setupCategoryInfo() {
        mFileCagetoryHelper = new FileCategoryHelper(mActivity,
                mFileViewInteractionHub);

        mCategoryBar0 = (CategoryBar) mRootView
                .findViewById(R.id.category_bar0);
        mCategoryBar1 = (CategoryBar) mRootView
                .findViewById(R.id.category_bar1);
        mCategoryBarOtg = (CategoryBar) mRootView
                .findViewById(R.id.category_barOtg);
        // this.setMemProgress();

        int[] imgs = new int[]{R.drawable.category_bar_music,
                R.drawable.category_bar_picture, R.drawable.category_bar_video,
                R.drawable.category_bar_document, R.drawable.category_bar_apk,
                R.drawable.category_bar_theme,
                R.drawable.category_bar_other
        };
        for (int i = 0; i < imgs.length; i++) {
            mCategoryBar0.addCategory(R.drawable.category_bar_internal);
            mCategoryBar1.addCategory(R.drawable.category_bar_sdcard);
            mCategoryBarOtg.addCategory(R.drawable.category_bar_usbotg);
        }

        for (int i = 0; i < FileCategoryHelper.sCategories.length; i++) {
            categoryIndex.put(FileCategoryHelper.sCategories[i], i);
        }
    }

    public void refreshCategoryInfo() {

        final SDCardInfo sdCardInfo0;
        final SDCardInfo sdCardInfo1;
        SDCardInfo sdCardInfoOtg = null;

        sdCardInfo0 = Util.getSDCardInfo((FileManagerMainActivity) mActivity,
                CATEGORY_BAR0_INDEX);

        // /LEWA_B2B BEGIN
        if (getContext().getResources()
                .getBoolean(R.bool.config_w11_nosdcard)) {
            sdCardInfo1 = new SDCardInfo();
            sdCardInfo1.total = Util.getTotalInternalMemorySize();
            sdCardInfo1.free = Util.getAvailableInternalMemorySize();
        } else {
            if (Util.sDualSDMounted) {
                sdCardInfo1 = Util.getSDCardInfo(
                        (FileManagerMainActivity) mActivity,
                        CATEGORY_BAR1_INDEX);
            } else {
                sdCardInfo1 = new SDCardInfo();
                sdCardInfo1.total = Util.getTotalInternalMemorySize();
                sdCardInfo1.free = Util.getAvailableInternalMemorySize();
            }
        }

        View otgCategoryContainer = mRootView
                .findViewById(R.id.categoryBarOtgContainer);
        if (Util.hasUsbOtg()) {
            sdCardInfoOtg = Util.getSDCardInfo(mActivity, Util.usbOtgIndex);
            otgCategoryContainer.setVisibility(View.VISIBLE);
        } else {
            otgCategoryContainer.setVisibility(View.GONE);
        }
        // /LEWA_B2B END
        int strId_category_bar0_total = 0;
        int strId_category_bar0_avail = 0;
        int strId_category_bar1_total = 0;
        int strId_category_bar1_avail = 0;

        // /LEWA_B2B BEGIN
        if (getContext().getResources()
                .getBoolean(R.bool.config_w11_nosdcard)) {
            strId_category_bar0_total = R.string.internal_sd_card_size;
            strId_category_bar0_avail = R.string.internal_sd_card_available;
            strId_category_bar1_total = R.string.rom_size;
            strId_category_bar1_avail = R.string.rom_available;
        } else {
            if (Util.sDualSDMounted) {
                strId_category_bar0_total = R.string.internal_sd_card_size;
                strId_category_bar0_avail = R.string.internal_sd_card_available;
                strId_category_bar1_total = R.string.external_sd_card_size;
                strId_category_bar1_avail = R.string.external_sd_card_available;
            } else {
                if (Environment.isExternalStorageRemovable()) {
                    strId_category_bar0_total = R.string.sd_card_size;
                    strId_category_bar0_avail = R.string.sd_card_available;
                } else {
                    strId_category_bar0_total = R.string.internal_sd_card_size;
                    strId_category_bar0_avail = R.string.internal_sd_card_available;
                }
                strId_category_bar1_total = R.string.rom_size;
                strId_category_bar1_avail = R.string.rom_available;
            }
        }
        // /LEWA_B2B END

        if (sdCardInfo0 != null) {
            mCategoryBar0.setFullValue(sdCardInfo0.total);

            if (isAdded()) {
                setTextView(
                        R.id.categoryBar0_capacity,
                        getString(strId_category_bar0_total,
                                Util.convertStorage(sdCardInfo0.total)));
                setTextView(
                        R.id.categoryBar0_available,
                        getString(strId_category_bar0_avail,
                                Util.convertStorage(sdCardInfo0.free)));
            }
        }
        if (sdCardInfo1 != null) {

            // this.setMemProgress();
            mCategoryBar1.setFullValue(sdCardInfo1.total);

            if (isAdded()) {
                setTextView(
                        R.id.categoryBar1_capacity,
                        getString(strId_category_bar1_total,
                                Util.convertStorage(sdCardInfo1.total)));
                setTextView(
                        R.id.categoryBar1_available,
                        getString(strId_category_bar1_avail,
                                Util.convertStorage(sdCardInfo1.free)));
            }

            TextView text1 = (TextView) mRootView
                    .findViewById(R.id.categoryBar1_capacity);
            TextView text2 = (TextView) mRootView
                    .findViewById(R.id.categoryBar1_available);
            if (sdCardInfo1.total <= 0) {
                text1.setTextColor(getResources().getColor(
                        R.drawable.unavailable));
                text2.setTextColor(getResources().getColor(
                        R.drawable.unavailable));
            } else {
                // added by weihong, #60570
//                text1.setTextColor(Color.BLACK);
//                text2.setTextColor(Color.BLACK);
            }
        }

        LinearLayout linearLayout = (LinearLayout)mRootView.findViewById(R.id.categoryinfo_system);
        if(!Util.sDualSDMounted){
            linearLayout.setVisibility(View.GONE);
            mCategoryBar1.setVisibility(View.GONE);
        }else{
            linearLayout.setVisibility(View.VISIBLE);
            mCategoryBar1.setVisibility(View.VISIBLE);
        }


        if (sdCardInfoOtg != null) {
            mCategoryBarOtg.setFullValue(sdCardInfoOtg.total);

            setTextView(
                    R.id.categoryBarOtg_capacity,
                    getString(R.string.usb_otg_card_size,
                            Util.convertStorage(sdCardInfoOtg.total)));
            setTextView(
                    R.id.categoryBarOtg_available,
                    getString(R.string.usb_otg_card_available,
                            Util.convertStorage(sdCardInfoOtg.free)));

            long otherSize = sdCardInfoOtg.total - sdCardInfoOtg.free;
            setCategoryBarValue(CATEGORY_BAROTG_INDEX, FileCategory.Other,
                    otherSize);

        }
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                refreshCategoryUI(sdCardInfo0, sdCardInfo1);
            }

        };
        Thread t = new Thread() {

            @Override
            public void run() {
                mFileCagetoryHelper.refreshCategoryInfo();
                mHandler.post(r);
            }

        };
        t.start();
        // the other category size should include those files didn't get
        // scanned.

    }

    private void refreshCategoryUI(SDCardInfo sdCardInfo0,
            SDCardInfo sdCardInfo1) {
        long size_cb0 = 0, size_cb1 = 0;
        for (FileCategory fc : FileCategoryHelper.sCategories) {
            CategoryInfo categoryInfo = mFileCagetoryHelper.getCategoryInfos()
                    .get(fc);
            if (categoryInfo == null) {
                return;
            }
            setCategoryCount(fc, categoryInfo.count);

            // other category size should be set separately with calibration
            if (fc == FileCategory.Other)
                continue;

            setCategorySize(fc, categoryInfo.size);
            setCategoryBarValue(CATEGORY_BAR0_INDEX, fc,
                    categoryInfo.sdInfo[Util.internalStorageIndex].size_sd);
            size_cb0 += categoryInfo.sdInfo[Util.internalStorageIndex].size_sd;
            // /LEWA_B2B BEGIN
            if (mDualSD
                    && !getContext().getResources()
                            .getBoolean(R.bool.config_w11_nosdcard)) {
                // /LEWA_B2B END
                setCategoryBarValue(CATEGORY_BAR1_INDEX, fc,
                        categoryInfo.sdInfo[Util.sdcardIndex].size_sd);
                size_cb1 += categoryInfo.sdInfo[Util.sdcardIndex].size_sd;
            }
        }

        if (sdCardInfo0 != null) {
            long otherSize = sdCardInfo0.total - sdCardInfo0.free - size_cb0;
            setCategorySize(FileCategory.Other, otherSize);
            setCategoryBarValue(CATEGORY_BAR0_INDEX, FileCategory.Other,
                    otherSize);
        }
        // /LEWA_B2B BEGIN
        if (mDualSD
                && !getContext().getResources()
                        .getBoolean(R.bool.config_w11_nosdcard)) {
            // /LEWA_B2B END
            if (sdCardInfo1 != null) {
                long otherSize = sdCardInfo1.total - sdCardInfo1.free
                        - size_cb1;
                setCategoryBarValue(CATEGORY_BAR1_INDEX, FileCategory.Other,
                        otherSize);
            }
        } else {
            if (sdCardInfo1 != null) {
                long otherSize = sdCardInfo1.total - sdCardInfo1.free;
                setCategoryBarValue(CATEGORY_BAR1_INDEX, FileCategory.Other,
                        otherSize);
            }
        }

        if (mCategoryBar0.getVisibility() == View.VISIBLE) {
            mCategoryBar0.startAnimation(this);
        }
        if (mCategoryBar1.getVisibility() == View.VISIBLE) {
            mCategoryBar1.startAnimation(this);
        }
        if (mCategoryBarOtg.getVisibility() == View.VISIBLE) {
            mCategoryBarOtg.startAnimation(this);
        }
    }

    public enum ViewPage {
        Home, Category, NoSD, Invalid
    }

    private void showPage(ViewPage p) {
        if (curViewPage == p)
            return;
        showEmptyView(false);
        curViewPage = p;
        showView(R.id.path_bar, false); // new Lewa path bar
        showView(R.id.file_path_list, false);
        showView(R.id.navigation_bar, false);
        showView(R.id.category_page, false);
        showView(R.id.sd_not_available_page, false);
        switch (p) {
            case Home :
                showView(R.id.category_page, true);
                if (mConfigurationChanged) {
                    ((FileManagerMainActivity) mActivity)
                            .reInstantiateCategoryTab();
                    mConfigurationChanged = false;
                }
                break;
            case Category :
                setHasOptionsMenu(true);
                showView(R.id.path_bar, true); // new Lewa path bar
                showView(R.id.file_path_list, true);
                showEmptyView(mAdapter.getCount() == 0);
                ((FileManagerMainActivity) mActivity).ReflashMenu();
                break;
            case NoSD :
                showView(R.id.sd_not_available_page, true);
                break;
        }
    }

    private void showEmptyView(boolean show) {
        if (isHomePage() || curViewPage == ViewPage.NoSD) {
            return;
        }
        View emptyView = mActivity.findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            emptyView.invalidate();
        }
    }

    private void showView(int id, boolean show) {
        View view = mRootView.findViewById(id);
        if (view != null) {
            // view.invalidate();
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            view.invalidate();

        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mStorageLocationFilter = -1;
            FileCategory f = button2Category.get(v.getId());
            if (f != null) {
                onCategorySelected(f);
            }
        }
    };

    private void setCategoryCount(FileCategory fc, long count) {
        int id = getCategoryCountId(fc);
        int idString = getCategoryStringId(fc);

        if (id == 0)
            return;
        if(isAdded()){
            setTextView(id, getResources().getString(idString) + " ( " + count
                    + " )");
        }
    }

    private void setTextView(int id, String t) {
        TextView text = (TextView) mRootView.findViewById(id);
        text.setText(t);
    }

    private void setColorfulTextView(int id, String t) {
        TextView text = (TextView) mRootView.findViewById(id);
        text.setText(Html.fromHtml(t));
    }

    private static int getCategoryStringId(FileCategory fc) {
        switch (fc) {
            case Music :
                return R.string.category_music;
            case Video :
                return R.string.category_video;
            case Picture :
                return R.string.category_picture;
            case Theme :
                return R.string.category_theme;
            case Doc :
                return R.string.category_document;
            case Apk :
                return R.string.category_apk;
        }

        return 0;
    }

    private void onCategorySelected(FileCategory f) {

        if (curViewPage != ViewPage.Home)
            return;
        // fist clear listview
        mAdapter.changeCursor(null);
        // if go to the different category with last time:
        if (mFileCagetoryHelper.getCurCategory() != f) {
            mFileCagetoryHelper.setCurCategory(f);
            mFileViewInteractionHub.setCurrentPath(mFileViewInteractionHub
                    .getRootPath()
                    + getString(mFileCagetoryHelper.getCurCategoryNameResId()));
        }

        // curViewPage = ViewPage.Category;
//        setFileListIndex(FileListIndex.NoIndex);
        addTab(getString(getCategoryDisplayNameId(mFileCagetoryHelper
                .getCurCategory())));
        mFileViewInteractionHub.setSortMethod(((FileManagerMainActivity) mActivity).getSortMethod());
        getFileCategoryHelper()
                .getCategoryInfo(getFileCategoryHelper().getCurCategory())
                .setCategorySDCardIndex(2); // default show all sdcard.
        showPage(ViewPage.Category);
        setProgressBarShow(MSG_LIST_PROGRESS_BAR_VISIBLE);
        showEmptyView(false);
        showView(R.id.file_path_list, false);
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_Refresh_ListView), 100);
    }

    private static int getCategoryDisplayNameId(FileCategory fc) {
        switch (fc) {
            case Music :
                return R.string.category_all_music;
            case Video :
                return R.string.category_all_video;
            case Picture :
                return R.string.category_all_picture;
            case Theme :
                return R.string.category_all_theme;
            case Doc :
                return R.string.category_all_document;
            case Apk :
                return R.string.category_all_apk;
        }

        return 0;
    }

    private void setupClick(int id) {
        View button = mRootView.findViewById(id);
        button.setOnClickListener(onClickListener);
    }

    private void setupClick() {
        setupClick(R.id.category_music);
        setupClick(R.id.category_video);
        setupClick(R.id.category_picture);
        setupClick(R.id.category_theme);
        setupClick(R.id.category_document);
        setupClick(R.id.category_apk);
    }

    @Override
    public boolean onBack() {
        if (isHomePage() || curViewPage == ViewPage.NoSD
                || mFileViewInteractionHub == null) {
            return false;
        }

        return mFileViewInteractionHub.onBackPressed();
    }

    public boolean isHomePage() {
        return curViewPage == ViewPage.Home;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (curViewPage != ViewPage.Category || curViewPage == ViewPage.Home) {
            return;
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (curViewPage != ViewPage.Category || curViewPage == ViewPage.Home) {
            return;
        }

        if (!isHomePage() && curViewPage != ViewPage.NoSD) {
            int fragmentIndex = ((FileManagerMainActivity) getActivity())
                    .getCurrentItemIndex();
            if (fragmentIndex == 0) {
                mFileViewInteractionHub.onCreateCategoryOptionsMenu(menu, null);
            }
        } else {
            menu.clear();
        }
    }

    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        if (isHomePage()) {
            return false;
        }
        setProgressBarShow(MSG_LIST_PROGRESS_BAR_VISIBLE);
        FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        Cursor c = mFileCagetoryHelper.query(curCategory, sort.getSortMethod());
        if (c != null) {
            UpdateListViewData(c);
            return true;
        }
        return false;
    }

    public boolean onRefreshEditFileList(String path, FileSortHelper sort,int position) {
        return true;
    }

    void UpdateListViewData(Cursor queryData) {
        Button btn = (Button) getViewById(mTabsCounter);
        FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        if(queryData == null || queryData.getCount() == 0){
            showEmptyView(true);
        } else {
            //#947450 add begin by bin.dong
            showEmptyView(false);
            //#947450 add end by bin.dong
            showView(R.id.file_path_list, true);
        }
        mAdapter.changeCursor(queryData);
        if (mTabsCounter > 0) {
            int id = getCategoryDisplayNameId(curCategory);

            //RC48605-jianwu.gao modify begin
            //back to filemanager from pic preview ,FC
            if (id > 0 && isAdded())
                btn.setText(getString(id));
            //RC48605-jianwu.gao modify end
        }
        setProgressBarShow(MSG_LIST_PROGRESS_BAR_GONE);

        // set scroll postion
        final ListView listview = (ListView) mRootView
                .findViewById(R.id.file_path_list);
        listview.post(new Runnable() {
            @Override
            public void run() {
                if (mFileCagetoryHelper.getCurCategory() == FileCategory.Apk){
                    listview.setSelection(mFileViewInteractionHub.getCurPosition());
                    mFileViewInteractionHub.setCurPosition(0);
                }else {
                    listview.setSelection(0);
                }
            }
        });
    }

    public int getStorageLocationFilter() {
        return mStorageLocationFilter;
    }

    public boolean onRefreshFileList(String path, FileSortHelper sort,
            int sdcardIndex) {
        if (isHomePage()) {
            return false;
        }
        mStorageLocationFilter = sdcardIndex;
        setProgressBarShow(MSG_LIST_PROGRESS_BAR_VISIBLE);

        FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        Button btn = (Button) getViewById(mTabsCounter);// new
                                                             // Button(this.getActivity());

        sdcardIndex = getFileCategoryHelper().getCategoryInfo(
                getFileCategoryHelper().getCurCategory())
                .getCategorySDCardIndex();
        if (sdcardIndex >= 0 && sdcardIndex < 2) {
            Cursor c = mFileCagetoryHelper.query(curCategory,
                    sort.getSortMethod(), sdcardIndex);
            showEmptyView(c == null || c.getCount() == 0);
            mAdapter.changeCursor(c);
            if (mTabsCounter > 0) {
                if (sdcardIndex == Util.internalStorageIndex)
                    btn.setText(getString(R.string.built_in_sdcard)
                            + getString(getCategoryStringId(curCategory)));
                if (sdcardIndex == Util.sdcardIndex)
                    btn.setText(getString(R.string.sdcard)
                            + getString(getCategoryStringId(curCategory)));
            }
        } else {
            Cursor c = mFileCagetoryHelper.query(curCategory,
                    sort.getSortMethod());
            showEmptyView(c == null || c.getCount() == 0);
            mAdapter.changeCursor(c);
            if (mTabsCounter > 0) {
                int id = getCategoryDisplayNameId(curCategory);
                if (id > 0) {
                    String str_text = getString(id);
                    if (str_text != null && str_text != "") {
                        btn.setText(str_text);
                    }
                }
            }
        }
        setProgressBarShow(MSG_LIST_PROGRESS_BAR_GONE);
        return true;
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
                // mFavoriteList.getArrayAdapter().notifyDataSetChanged();
                showEmptyView(mAdapter.getCount() == 0);
            }

        });
    }

    @Override
    public void onPick(FileInfo f) {
        // do nothing
        try {
            Uri uri = Uri.fromFile(new File(f.filePath));
            Intent intent = Intent.parseUri(uri.toString(), 0);
            Intent fromIntent = mActivity.getIntent();
            String type = fromIntent.getType();
            String cropStr = "";
            if (fromIntent.hasExtra("crop")) {
                cropStr = fromIntent.getCharSequenceExtra("crop").toString();
            }
            if (cropStr != null && cropStr.equals("true") && type != null
                    && type.equals("image/*")) {
                Intent newIntent = new Intent("com.android.camera.action.CROP");
                newIntent.setDataAndType(uri, "image/*");
                newIntent.putExtra("scale", true);
                newIntent.putExtra("scaleUpIfNeeded", true);
                newIntent.putExtra("aspectX",
                        fromIntent.getIntExtra("aspectX", 1));
                newIntent.putExtra("aspectY",
                        fromIntent.getIntExtra("aspectY", 1));
                newIntent.putExtra("outputX",
                        fromIntent.getIntExtra("outputX", 1));
                newIntent.putExtra("outputY",
                        fromIntent.getIntExtra("outputY", 1));
                // newIntent.putExtra("return-data",
                // fromIntent.getBooleanExtra("return-data", false));
                if (fromIntent.getBooleanExtra("return-data", false)) {
                    newIntent.putExtra("return-data", true);
                } else if (fromIntent
                        .getParcelableExtra(MediaStore.EXTRA_OUTPUT) != null) {
                    newIntent.putExtra(MediaStore.EXTRA_OUTPUT, fromIntent
                            .getParcelableExtra(MediaStore.EXTRA_OUTPUT));
                }
                mActivity.startActivityForResult(newIntent, REQ_IMAGE_CROP);
            } else {
                mActivity.setResult(Activity.RESULT_OK, intent);
                mActivity.finish();
            }
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
        mFileViewInteractionHub.addContextMenuSelectedItem();
        switch (id) {
        // case R.id.button_operation_copy:
        // case GlobalConsts.MENU_COPY:
        // copyFileInFileView(mFileViewInteractionHub.getSelectedFileList());
        // mFileViewInteractionHub.clearSelection();
        // break;
        // case R.id.button_operation_move:
            case GlobalConsts.MENU_MOVE :
                startMoveToFileView(mFileViewInteractionHub
                        .getSelectedFileList());
                mFileViewInteractionHub.clearSelection();
                break;
            case GlobalConsts.OPERATION_UP_LEVEL : // Notice: Do NOT delete this
                                                   // operation!!
                setHasOptionsMenu(false);
                showPage(ViewPage.Home);
                break;
            default :
                return false;
        }
        return true;
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return (menu == GlobalConsts.MENU_NEW_FOLDER // || menu ==
                                                     // GlobalConsts.MENU_FAVORITE
                || menu == GlobalConsts.MENU_PASTE || menu == GlobalConsts.MENU_SHOWHIDE);
    }

    @Override
    public void addSingleFile(FileInfo file) {
        refreshList();
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mAdapter.getAllFiles();
    }

    @Override
    public FileInfo getItem(int pos) {
        return mAdapter.getFileItem(pos);
    }

    @Override
    public int getItemCount() {
        return mAdapter.getCount();
    }

    @Override
    public void sortCurrentList(FileSortHelper sort) {
        refreshList();
    }

    private void refreshList() {
        mFileViewInteractionHub.refreshFileList();
    }

    // private void copyFileInFileView(ArrayList<FileInfo> files) {
    // if (files.size() == 0)
    // return;
    // mFileViewActivity.copyFile(files);
    // // mActivity.getActionBar().setSelectedNavigationItem(
    // // Util.SDCARD_TAB_INDEX);
    // }

    private void startMoveToFileView(ArrayList<FileInfo> files) {
        if (files.size() == 0)
            return;
        mFileViewActivity.moveToFile(files);
        // mActivity.getActionBar().setSelectedNavigationItem(
        // Util.SDCARD_TAB_INDEX);
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    private static int getCategoryCountId(FileCategory fc) {
        switch (fc) {
            case Music :
                return R.id.category_music_count;
            case Video :
                return R.id.category_video_count;
            case Picture :
                return R.id.category_picture_count;
            case Theme :
                return R.id.category_theme_count;
            case Doc :
                return R.id.category_document_count;
            case Apk :
                return R.id.category_apk_count;
        }
        return 0;
    }

    // This function shows the info for little colorful buttons.
    private void setCategorySize(FileCategory fc, long size) {
        int txtId = 0;
        int resId = 0;
        switch (fc) {
            case Music :
                txtId = R.id.category_music_size;// category_legend_music;
                resId = R.string.category_music;
                break;
            case Video :
                txtId = R.id.category_video_size;
                resId = R.string.category_video;
                break;
            case Picture :
                txtId = R.id.category_picture_size;
                resId = R.string.category_picture;
                break;
            case Theme :
                txtId = R.id.category_theme_size;
                resId = R.string.category_theme;
                break;
            case Doc :
                txtId = R.id.category_document_size;
                resId = R.string.category_document;
                break;
            case Apk :
                txtId = R.id.category_apk_size;
                resId = R.string.category_apk;
                break;
        }

        if (txtId == 0 || resId == 0)
            return;

        setTextView(txtId, Util.convertStorage(size));
    }

    private void setCategoryBarValue(int categoryBarIndex, FileCategory f,
            long size) {
        if (mCategoryBar0 == null) {
            mCategoryBar0 = (CategoryBar) mRootView
                    .findViewById(R.id.category_bar0);
        }
        if (mCategoryBar1 == null) {
            mCategoryBar1 = (CategoryBar) mRootView
                    .findViewById(R.id.category_bar1);
        }
        if (categoryBarIndex == 0) {
            mCategoryBar0.setCategoryValue(categoryIndex.get(f), size);
        } else if (categoryBarIndex == 1) {
            mCategoryBar1.setCategoryValue(categoryIndex.get(f), size);
        } else {
            mCategoryBarOtg.setCategoryValue(categoryIndex.get(f), size);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mActivity != null) {
            mActivity.unregisterReceiver(mScannerReceiver);
        }
        if(mAdapter != null && mAdapter.getCursor() != null){
            mAdapter.getCursor().close();
        }
    }

    private class ScannerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // handle intents related to external storage
            if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)
                    || action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                notifyFileChanged();
            }
        }
    }

    public void updateUI() {
        boolean sdCardReady = Util.hasSDCardReady(getContext());
        if (sdCardReady) {
            if (preViewPage != ViewPage.Invalid) {
                showPage(preViewPage);
                preViewPage = ViewPage.Invalid;
            } else if (curViewPage == ViewPage.Invalid
                    || curViewPage == ViewPage.NoSD) {
                showPage(ViewPage.Home);
                mCategoryProgressBar.setVisibility(View.VISIBLE);
            }
            refreshCategoryInfo();
            setSDCardPath(mFileViewInteractionHub.getCurrentPath());
            mFileViewInteractionHub.refreshFileList();

            // refresh file list view in SDcard tab beside category tab
            mFileViewActivity.refresh();
        } else {
            if (curViewPage != ViewPage.NoSD) {
                preViewPage = curViewPage;
                showPage(ViewPage.NoSD);
            }
        }
    }

    private Timer timer;
    private Timer timer2;

    // process file changed notification, using a timer to avoid frequent
    // refreshing due to batch changing on file system
    synchronized public void notifyFileChanged() {
        if (timer != null) {
            timer.cancel();
        }
        // mCategoryProgressBar.setVisibility(View.VISIBLE);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                timer = null;
                Message message = new Message();
                message.what = MSG_FILE_CHANGED_TIMER;
                mHandler.sendMessage(message);
            }

        }, 500);
    }

    private static final int MSG_FILE_CHANGED_TIMER = 100;
    private static final int MSG_PROGRESS_BAR_GONE = 8;
    private static final int MSG_PROGRESS_BAR_VISIBLE = 0;
    private static final int MSG_LIST_PROGRESS_BAR_GONE = 9;
    private static final int MSG_LIST_PROGRESS_BAR_VISIBLE = 1;
    private static final int MSG_Refresh_ListView = 50;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_CHANGED_TIMER :
                    if (isAdded()) {
                        updateUI();
                    }
                    break;
                case MSG_PROGRESS_BAR_GONE :
                    mCategoryProgressBar.setVisibility(View.GONE);
                    // This line is for show all files including 2 cards.
                    getFileCategoryHelper().getCategoryInfo(
                            getFileCategoryHelper().getCurCategory())
                            .setCategorySDCardIndex(2);
                    mFileViewInteractionHub.refreshFileList();
                    break;
                case MSG_PROGRESS_BAR_VISIBLE :
                    mCategoryProgressBar.setVisibility(View.VISIBLE);
                    break;
                case MSG_LIST_PROGRESS_BAR_GONE :
                    mListProgress.setVisibility(View.GONE);
                    break;
                case MSG_LIST_PROGRESS_BAR_VISIBLE :
                    mListProgress.setVisibility(View.VISIBLE);
                    break;
                case MSG_Refresh_ListView :
                    mFileViewInteractionHub.refreshFileList();
                    break;
            }
            super.handleMessage(msg);
        }

    };

    synchronized public void setProgressBarShow(final int show) {
        if (timer2 != null) {
            timer2.cancel();
        }
        // mCategoryProgressBar.setVisibility(View.VISIBLE);
        timer2 = new Timer();
        timer2.schedule(new TimerTask() {

            public void run() {
                timer2 = null;
                Message message = new Message();
                message.what = show;
                mHandler.sendMessage(message);
            }

        }, 100);
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    @Override
    public void addTab(String text) {
        LinearLayout.LayoutParams mlp;
        ++mTabsCounter;

        // set button style
        if (mTabsCounter == 0) {

            Button btn = new Button(getActivity());
            btn.setId(mTabsCounter);

            Drawable drawable = getResources()
                    .getDrawable(R.drawable.path_tail);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                    drawable.getMinimumHeight());
            Drawable drawable2 = getResources().getDrawable(
                    R.drawable.path_head);
            drawable2.setBounds(0, 0, drawable2.getMinimumWidth(),
                    drawable2.getMinimumHeight());
            btn.setCompoundDrawables(drawable2, null, drawable, null);
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setPadding(0, 0, 0, 2);

            mlp = new LinearLayout.LayoutParams(
                    new ViewGroup.MarginLayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
            mlp.setMargins(0, 0, 0, 0);
            btn.setLayoutParams(mlp);
            // btn.setTextColor(Color.BLACK);
            btn.setText(text);
            // btn.setBackgroundDrawable(mResources
            // .getDrawable(R.drawable.custom_tab));

            // add button to the tab holder
            mTabsHolder.addView(btn);
            btn.setOnClickListener(this);
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
        } else {
            Button btn = new Button(getActivity());
            btn.setId(mTabsCounter);
            // btn.setTextColor(Color.BLACK);
            Drawable drawable = getResources()
                    .getDrawable(R.drawable.path_tail);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                    drawable.getMinimumHeight());
            Drawable drawable2 = getResources().getDrawable(
                    R.drawable.path_head);
            drawable2.setBounds(0, 0, drawable2.getMinimumWidth(),
                    drawable2.getMinimumHeight());
            btn.setCompoundDrawables(drawable2, null, drawable, null);
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setPadding(0, 0, 0, 2);

            // btn.setBackgroundDrawable(mResources
            // .getDrawable(R.drawable.custom_tab));//custom_tab

            if (text.length() <= TAB_TEXT_LENGTH) {
                btn.setText(text);
            } else {
                btn.setText(text.substring(0, TAB_TEXT_LENGTH));
            }

            mlp = new LinearLayout.LayoutParams(
                    new ViewGroup.MarginLayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));
            mlp.setMargins(0, 0, 0, 0);// mTabMarginLeft, 0, 0, 0);
            btn.setLayoutParams(mlp);
            btn.setOnClickListener(this);
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

            // add button to the tab holder
            mTabsHolder.addView(btn);
            sdCardPath = sdCardPath
                    + getString(getCategoryStringId((mFileCagetoryHelper
                            .getCurCategory())));
        }// SEPARATOR +

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
        // mTabsHolder begins from 0, mTabsHolder.getChildCount() == mTabsHolder
        // + 1

        // if (0 == id && 1 == mTabsCounter) { //mTabsHolder.getChildCount()) {
        // mTabsHolder.removeViews(1, 1); //mTabsHolder.getChildCount());
        if (0 == id && 1 <= mTabsHolder.getChildCount()) {
            mTabsHolder.removeViews(1, mTabsHolder.getChildCount() - 1);
            mTabsCounter = 0;
            if (!"/".equals(sdCardPath)) {
                sdCardPath = new File(sdCardPath).getParent();
                showPage(ViewPage.Home);
            }
        }
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        ActionMode actionMode = ((FileManagerMainActivity) getContext())
                .getActionMode();
        if (actionMode != null) {
            actionMode.finish();
        }
        int id = view.getId();
        if (id == 0) {
            onBack();
        } else {
            updateNavigationBar(id);
            mFileViewInteractionHub.setCurrentPath(sdCardPath);
        }
    }

    @Override
    public boolean onRefreshPathBar(String path, int id) {
        // mResources = this.getActivity().getResources(); //set resources

        if (mTabsCounter == -1) // when first time run: /mnt/sdcard path
        { // path != "" && path != "/"
            addTab(sdCardName);
        } else {
            sdCardPath = path;
            updateNavigationBar(0);
        }
        return true;
    }

    public void setSDCardName(String sdcardName) {

        sdCardName = sdcardName;
    }

    @Override
    public boolean onRefreshEditFileList(String path, FileSortHelper sort) {
        // TODO Auto-generated method stub

        final int pos = fileListView.getFirstVisiblePosition();

        if (mFileViewInteractionHub.getSelectedFileList().size() > 0
                && mAdapter.getCount() > 1) {
            FileInfo tmpfileInfo = mFileViewInteractionHub
                    .getSelectedFileList().get(0);
            boolean isResetScrollPos = true;
            if (tmpfileInfo == mAdapter.getFileItem(mAdapter.getCount() - 1)
                    || tmpfileInfo == mAdapter
                            .getFileItem(mAdapter.getCount() - 2)) {
                isResetScrollPos = false;
            }

            if (isResetScrollPos) {
                fileListView.post(new Runnable() {
                    @Override
                    public void run() {
                        fileListView.setSelection(pos);
                        // setProgressBarShow(MSG_LIST_PROGRESS_BAR_GONE);
                    }
                });
            }
        }
        return false;
    }

    public static final String ACTION_MEDIA_SCANNER_SCAN_DIR = "android.intent.action.MEDIA_SCANNER_SCAN_DIR";

    public void scanThemeInstallPath() {
        // Lewa installation path is: /mnt/sdcard/LEWA/theme,
        // /mnt/sdcard2/LEWA/theme
        String defaultInstallRoot = Environment.getExternalStorageDirectory()
                .getPath(); // default install root. like /mnt/sdcard.
        Util.notifyRootScanBroadcast(getContext(), defaultInstallRoot);
        if (defaultInstallRoot.equalsIgnoreCase("/mnt/sdcard")) {
            ((FileViewFragment) ((FileManagerMainActivity) mActivity)
                    .getFragment(1)).refresh();
        } else if (defaultInstallRoot.equalsIgnoreCase("/mnt/sdcard2")
                && ((FileViewFragment) ((FileManagerMainActivity) mActivity)
                        .getFragment(2)) != null) {
            ((FileViewFragment) ((FileManagerMainActivity) mActivity)
                    .getFragment(2)).refresh();
        }
        // ==========
        // String defaultInstallRoot2 =
        // Environment.getExternalStorageDirectory().getPath();
        // String installThemePath = Util.makePath(defaultInstallRoot2,
        // getString(R.string.theme_path));
        // Intent intent;
        // intent = new
        // Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//ACTION_MEDIA_SCANNER_SCAN_DIR);
        // intent.setData(Uri.fromFile(new File(installThemePath.toString())));
        // // File f = new File(installThemePath.toString());
        // this.getActivity().sendBroadcast(intent);
    }

    public FileViewInteractionHub getFileViewInteractionHub() {
        return mFileViewInteractionHub;
    }

    @Override
    public String getFragmentTag() {
        return getTag();
    }

    private View mSearchViewItem;
    private SearchView searchView;
    private void addSearchViewItem(View view) {
        mSearchViewItem = (View) mRootView.findViewById(R.id.search_item);
        searchView = (SearchView) mRootView.findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setIconifiedByDefault(true);
            searchView.setIconified(false);
            searchView.onActionViewExpanded();
            searchView.clearFocus();
            searchView.setEnabled(false);
            searchView.setQueryHint(getActivity().getResources().getString(
                    R.string.hint_findFiles));
            findChildView(searchView);
        }
        mSearchViewItem.setVisibility(View.VISIBLE);
    }

    private void findChildView(View v) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof LinearLayout
                        || child instanceof RelativeLayout) {
                    findChildView(child);
                }

                if (child instanceof TextView) {
                    TextView text = (TextView) child;
                    text.setFocusable(false);
                }
                child.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        Intent intent = new Intent(getActivity(),
                                LewaSearchActivity.class);
                        startActivity(intent);
                    }
                });
            }
        }
    }

}
