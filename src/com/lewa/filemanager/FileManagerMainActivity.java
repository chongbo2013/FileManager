package com.lewa.filemanager;

//LEWA ADD BEGIN
//import android.app.ActionBar;
//import android.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBarActivity;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar.TabListener;
import lewa.support.v7.app.ActionBar.LayoutParams;
//LEWA ADD END
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
//LEWA ADD BEGIN
//import android.app.ActionBar.LayoutParams;
//LEWA ADD END
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
//LEWA ADD BEGIN
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
//LEWA ADD END
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lewa.search.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.text.DateFormat;

import com.lewa.filemanager.FileCategoryFragment.FileListIndex;
import com.lewa.filemanager.FileOperationHelper.IOperationProgressListener;
import com.lewa.filemanager.FileSortHelper.SortMethod;
import com.lewa.filemanager.FileViewInteractionHub.Mode;
import com.lewa.filemanager.R;
import com.lewa.filemanager.clearFolder.EmptyDirectoryCleaner;
import com.lewa.filemanager.clearFolder.ReportInfo;

public class FileManagerMainActivity extends ActionBarActivity//LEWA ADD BEGIN
        implements
        IOperationProgressListener {

    /* Static data */
    private static final boolean DBG = false;
    private static final String TAG = "FileManagerMainActivity";
    public static final String COUNT_TAG = "COUNT_TAG";
    public static final String SDCARD_TAG = "SDCARD_TAG";
    public static final String SDCARD_TAG2 = "SDCARD_TAG2";
    public static final String SDCARD_TAG3 = "SDCARD_TAG3";
    private final static int SEQ_Search_Code = 123;
    public static final String FILE_SORT_KEY = "file_sort";
    public static final String SHAREDPREFERENCES_FILENAME = "file_sharedpreferences";

    /* View */
    private ActionBar mActionBar = null;
    private ViewPager mViewPager;
    private FileCategoryFragment categoryFragment;
    private FileViewFragment sdcardFragment;
    private FileViewFragment sdcardFragment2;
    private FileViewFragment sdcardFragmentOtg;
    private View mCutsomActionbarView;
    private View mConfirmOperationBar;
    protected View pasteToolbar;
    protected Button pasteBar_paste;
    protected Button pasteBar_cancel;
    private ActionBar.Tab categoryTab;
    private ActionBar.Tab sdcard0Tab;
    private ActionBar.Tab sdcard1Tab;
    private ActionBar.Tab usbTab;

    /* Object */
    private TabsAdapter mTabsAdapter;
    private ActionMode mActionMode;
    private Context mContext;
    private ScannerReceiver mBroadcastReceiver;
    private LayoutParams mLayoutParams;
    private FileOperationHelper mFileOperationHelper;
    private TabListener mTabListener;
    public static DateFormat dateFormat;
    public static DateFormat timeFormat;
    private FragmentManager fragmentManager;
    private Handler handlers;
    private Runnable runnable;

    /* Data */
    private String[] mSDCardList = null;
    private boolean[] mLastVolumeState = new boolean[3];
    private boolean showConfirmOperationBar;
    private boolean isSupportDualCard;
    private int mPasteDestFragmentIndex;
    private int mSourceFragmentIndex = -1;
    private boolean isdoubleSDCard;
    private boolean isFirstLoad = true;
    private String mTimeFormatStr;
    private String mDateFormatStr;
    ArrayList<HashMap<String, String>> list2 = new ArrayList<HashMap<String, String>>();

    long startTime;

    private SharedPreferences mSharedPreferences;
    
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        Util.initOrUpdateStaticValue(this);
        updateVolumeState();
        isdoubleSDCard = Util.sDualSDMounted;
        isSupportDualCard = Util.sSupportDualSD;
        mSDCardList = Util.sSDCardDir;
        mSharedPreferences = getSharedPreferences(SHAREDPREFERENCES_FILENAME,
                Activity.MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pager);

        dateFormat = getDateFormat();
        timeFormat = getTimeFormat();
        mTimeFormatStr = Settings.System.getString(getContentResolver(),
                Settings.System.TIME_12_24);
        mDateFormatStr = Settings.System.getString(getContentResolver(),
                Settings.System.DATE_FORMAT);

        mFileOperationHelper = new FileOperationHelper(this);

        // init view
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mViewPager);

        // init action bar
        //LEWA ADD BEGIN
        mActionBar = getSupportActionBar();
        //LEWA ADD END
        if (null != mActionBar) {
            mActionBar.setCustomView(null);
            mActionBar.setDisplayOptions(0);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            if (mTabListener == null) {
                mTabListener = new TabListener();
            }
            // init action bar
            categoryTab = mActionBar.newTab();
            categoryTab.setText(getString(R.string.tab_category));
            categoryTab.setTabListener(mTabListener);

            sdcard0Tab = mActionBar.newTab();
            sdcard0Tab.setText(getString(R.string.tab_builtInsdcardlist));
            sdcard0Tab.setTabListener(mTabListener);

            sdcard1Tab = mActionBar.newTab();
            sdcard1Tab.setText(getString(R.string.tab_sdcardlist));
            sdcard1Tab.setTabListener(mTabListener);

            usbTab = mActionBar.newTab();
            usbTab.setText(getString(R.string.usbotg_title));
            usbTab.setTabListener(mTabListener);


            mActionBar.addTab(categoryTab);
            if (isdoubleSDCard) {
                mActionBar.addTab(sdcard0Tab);
                mActionBar.addTab(sdcard1Tab);
            } else if (!isdoubleSDCard && isSupportDualCard) {
                mActionBar.addTab(sdcard0Tab);
            } else {
                if (Environment.isExternalStorageRemovable()) {
                    mActionBar.addTab(sdcard1Tab);
                } else {
                    mActionBar.addTab(sdcard0Tab);
                }
            }
            if (Util.hasUsbOtg()) {
                mActionBar.addTab(usbTab);
            }
        }

        setupOperationPane();

//        mCutsomActionbarView = LayoutInflater.from(
//                mActionBar.getThemedContext()).inflate(R.layout.head_layout,
//                null);

        fragmentManager = getFragmentManager();

        // Hide all tabs (the current tab will later be re-shown once a tab is
        // selected)
        final FragmentTransaction transaction = fragmentManager
                .beginTransaction();

        mViewPager.setAdapter(mTabsAdapter);
        mViewPager.setOnPageChangeListener(mTabsAdapter);

        categoryFragment = (FileCategoryFragment) fragmentManager
                .findFragmentByTag(COUNT_TAG);
        sdcardFragment = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG);

        if (isdoubleSDCard) {
            sdcardFragment2 = (FileViewFragment) fragmentManager
                    .findFragmentByTag(SDCARD_TAG2);
        }
        if (Util.hasUsbOtg()) {
            sdcardFragmentOtg = (FileViewFragment) fragmentManager
                    .findFragmentByTag(SDCARD_TAG3);
        }

        if (categoryFragment == null) {
            categoryFragment = new FileCategoryFragment();
            transaction.add(R.id.pager, categoryFragment, COUNT_TAG);
        }

        if(sdcardFragment == null){
            sdcardFragment = new FileViewFragment();
        }

        if (sdcardFragment2 == null && isdoubleSDCard) {
            sdcardFragment2 = new FileViewFragment();
        }

        if (sdcardFragmentOtg == null && Util.usbOtgIndex != -1) {
            sdcardFragmentOtg = new FileViewFragment();
        }

        if (mSDCardList.length > 0) {
            sdcardFragment.setSDCardPath(mSDCardList[0]);
            sdcardFragment.setSDCardRootPath(mSDCardList[0]);
        } else {
            sdcardFragment.setSDCardPath("/mnt/sdcard");
            sdcardFragment.setSDCardRootPath("/mnt/sdcard");
        }
        categoryFragment.setSDCardName(getResources().getString(
                R.string.home));

        if (isdoubleSDCard) // isSupportDualCard &&
        {
            sdcardFragment.setSDCardName(getResources().getString(
                    R.string.built_in_sdcard));
            sdcardFragment2.setSDCardName(getResources().getString(
                    R.string.sdcard));
            sdcardFragment2.setSDCardPath(mSDCardList[1]);
            sdcardFragment2.setSDCardRootPath(mSDCardList[1]);
            mViewPager.setOffscreenPageLimit(2);
        } else if (!isdoubleSDCard && isSupportDualCard) {
            sdcardFragment.setSDCardName(getResources().getString(
                    R.string.built_in_sdcard));
            mViewPager.setOffscreenPageLimit(1);
        } else {
            if (Environment.isExternalStorageRemovable()) {
                sdcardFragment.setSDCardName(getResources().getString(
                        R.string.sdcard));
            } else {
                sdcardFragment.setSDCardName(getResources().getString(
                        R.string.built_in_sdcard));
            }
            mViewPager.setOffscreenPageLimit(1);
        }

        if (Util.hasUsbOtg()) {
            if (DBG)
                Log2File.w("usbotg mounted in onCreate");
            sdcardFragmentOtg.setSDCardName(getString(R.string.usbotg_name));
            sdcardFragmentOtg.setSDCardPath(mSDCardList[Util.usbOtgIndex]);
            sdcardFragmentOtg.setSDCardRootPath(mSDCardList[Util.usbOtgIndex]);
            mViewPager.setOffscreenPageLimit(isdoubleSDCard ? 3 : 2);
        }

        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        Intent inten = getIntent();
        String action = inten.getAction();
        // 判断是否从通话录音进入文件管理器
        if ("view_directory".equals(action)){
            String str = inten.getData().getPath();
            if (str != null && str != "") {
                if (isdoubleSDCard && str.contains(mSDCardList[1])) // isSupportDualCard
                {
                    mViewPager.setCurrentItem(2);
                } else {
                    mViewPager.setCurrentItem(1);
                }
            }
        } else{
            mViewPager.setCurrentItem(0);
        }


        registerScannerReceiver();

        /*
         * // Open search file directory 2013-03-26
         */
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String str = bundle.getString("searchpath");
            if (str != null && str != "") {
                if (isdoubleSDCard && str.contains(mSDCardList[1])
                        && mViewPager.getOffscreenPageLimit() > 1) // isSupportDualCard
                // &&
                {
                    mViewPager.setCurrentItem(2);
                } else {
                    mViewPager.setCurrentItem(1);
                }
            }
        }

        handlers = new Handler();
        runnable = new Runnable() {

            @Override
            public void run() {
                FragmentTransaction transaction = fragmentManager
                        .beginTransaction();

                if (null != sdcardFragment) {
                    if (!sdcardFragment.isAdded()) {
                        transaction.add(R.id.pager, sdcardFragment, SDCARD_TAG);
                    }
                }

                if (sdcardFragment2 != null && isdoubleSDCard) {
                    if (!sdcardFragment2.isAdded()) {
                        transaction.add(R.id.pager, sdcardFragment2,
                                SDCARD_TAG2);
                    }
                }

                if (sdcardFragmentOtg != null && Util.usbOtgIndex != -1) {
                    if (!sdcardFragmentOtg.isAdded()) {
                        transaction.add(R.id.pager, sdcardFragmentOtg,
                                SDCARD_TAG3);
                    }
                }

                if (!isFinishing()) {
                    try {
                        transaction.commitAllowingStateLoss();
                        fragmentManager.executePendingTransactions();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO: handle exception
                    }
                }
            }
        };
        handlers.postDelayed(runnable, 1500);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && isFirstLoad) {
            isFirstLoad = false;
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    categoryFragment.updateUI();
                }
            }, 200);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDateTimeFormat();
    }

    public void showConfirmOperationBar(int show) {
        mConfirmOperationBar.setVisibility(show);
        ((IFileInteractionListener) mTabsAdapter.getFragment(mViewPager
                .getCurrentItem())).getFileViewInteractionHub()
                .getConfirmOperationBar().setVisibility(show);

        if (mTabsAdapter.getCount() >= 2) {
            switch (mTabsAdapter.getCount()) {
                case 2:
                    showConfirmOperationBar(1, show);
                    break;
                case 3:
                    showConfirmOperationBar(1, show);
                    showConfirmOperationBar(2, show);
                    break;
                case 4:
                    showConfirmOperationBar(1, show);
                    showConfirmOperationBar(2, show);
                    showConfirmOperationBar(3, show);
                    break;
            }
        }
    }

    private void showConfirmOperationBar(int position, int show) {
        if (mTabsAdapter.getFragment(position) != null
                && ((IFileInteractionListener) mTabsAdapter.getFragment(position))
                .getFileViewInteractionHub() != null
                && ((IFileInteractionListener) mTabsAdapter.getFragment(position))
                .getFileViewInteractionHub()
                .getConfirmOperationBar() != null) {
            ((IFileInteractionListener) mTabsAdapter.getFragment(position))
                    .getFileViewInteractionHub().getConfirmOperationBar()
                    .setVisibility(show);
        }
    }

    public void cancelMoveState(int position) {
        if (0 == position) {
            ((IFileInteractionListener) mTabsAdapter.getFragment(mViewPager
                    .getCurrentItem())).getFileViewInteractionHub()
                    .onOperationScollCancelMove();
        }
    }

    DateFormat getDateFormat() {
        DateFormat dataFormat;
        try {
            dataFormat = android.text.format.DateFormat.getDateFormat(mContext);
        } catch (Exception e) {
            // TODO: handle exception
            dataFormat = null;
        }
        return dataFormat;
    }

    DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(mContext);
    }

    public boolean getCanShowCheckBox() {
        return showConfirmOperationBar;
    }

    public void setCanShowCheckBox(boolean show) {
        showConfirmOperationBar = show;
    }

    private void setupOperationPane() {
        // Notice: Add "FileManagerMainActivity.this" to prevent finding view
        // id:main_layout_paste null.
        mConfirmOperationBar = FileManagerMainActivity.this
                .findViewById(R.id.main_layout_paste);
        pasteBar_paste = (Button) mConfirmOperationBar
                .findViewById(R.id.toolbar_paste);
        pasteBar_cancel = (Button) mConfirmOperationBar
                .findViewById(R.id.pastebar_cancel);
        setupClick(mConfirmOperationBar, R.id.toolbar_paste);
        setupClick(mConfirmOperationBar, R.id.pastebar_cancel);
    }

    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : findViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.toolbar_paste:// button_moving_confirm:
                    ((IFileInteractionListener) mTabsAdapter
                            .getFragment(mViewPager.getCurrentItem()))
                            .getFileViewInteractionHub()
                            .onOperationButtonConfirm();
                    break;
                case R.id.pastebar_cancel:// button_moving_cancel:
                    ((IFileInteractionListener) mTabsAdapter
                            .getFragment(mViewPager.getCurrentItem()))
                            .getFileViewInteractionHub()
                            .onOperationButtonCancel();
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(this).edit();
        editor.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        // super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileCategoryFragment.REQ_IMAGE_CROP
                || requestCode == SEQ_Search_Code
                || resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // return;
        //LEWA ADD BEGIN
        if (/*getActionBar()*/getSupportActionBar().getSelectedNavigationIndex() == Util.CATEGORY_TAB_INDEX) {
        //LEWA ADD END
            FileCategoryFragment categoryFragement = (FileCategoryFragment) mTabsAdapter
                    .getFragment(Util.CATEGORY_TAB_INDEX);// getItem(Util.CATEGORY_TAB_INDEX);
            // .getItem(Util.CATEGORY_TAB_INDEX);
            // //getFragment(Util.CATEGORY_TAB_INDEX);
            if (categoryFragement.isHomePage()) {
                reInstantiateCategoryTab();
            } else {
                categoryFragement.setConfigurationChanged(true);
            }
        }
        super.onConfigurationChanged(newConfig);
    }

    public void reInstantiateCategoryTab() {
        mTabsAdapter.destroyItem(mViewPager, Util.CATEGORY_TAB_INDEX,
                mTabsAdapter.getFragment(Util.CATEGORY_TAB_INDEX));// /.getItem(Util.CATEGORY_TAB_INDEX));
        // //getFragment(Util.CATEGORY_TAB_INDEX));
        mTabsAdapter.instantiateItem(mViewPager, Util.CATEGORY_TAB_INDEX);
    }

    @Override
    public void onBackPressed() {
        IBackPressedListener backPressedListener = (IBackPressedListener) mTabsAdapter
                .getFragment(mViewPager.getCurrentItem());
        // .getItem(mViewPager.getCurrentItem());
        if (!backPressedListener.onBack()) {
            super.onBackPressed();
        }
    }

    public interface IBackPressedListener {
        /**
         * 处理back事件。
         *
         * @return True: 表示已经处理; False: 没有处理，让基类处理。
         */
        boolean onBack();
    }

    public void setActionMode(ActionMode actionMode) {
        mActionMode = actionMode;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    public Fragment getFragment(int tabIndex) {
        return mTabsAdapter.getFragment(tabIndex);
    }

    public void ReflashMenu() {
        invalidateOptionsMenu();
    }

    /**
     * This is for controlling the switching fragment view by clicking and
     * scrolling.
     *
     * @author Administrator
     */
    class TabsAdapter extends PagerAdapter implements OnPageChangeListener {

        private FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
        private int mCurrentPosition;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(Activity activity, ViewPager pager) {
            mFragmentManager = activity.getFragmentManager();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setCurrentItem(0);
            mViewPager.setOnPageChangeListener(this);
        }

        // @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            if (info.fragment == null) {
                info.fragment = Fragment.instantiate(mContext,
                        info.clss.getName(), info.args);
            }
            return info.fragment;
        }

        @Override
        public int getCount() {
            if (Util.hasUsbOtg()) {
                return Util.sDualSDMounted ? 4 : 3;
            } else if (Util.sDualSDMounted) {
                return 3;
            }
            return 2;
        }

        public Fragment getFragment(int position) {
            if (position == 0) {
                return categoryFragment;
            } else if (position == 1) {
                return sdcardFragment;
            } else if (position == 2) {
                if (Util.sDualSDMounted) {
                    return sdcardFragment2;
                } else {
                    return sdcardFragmentOtg;
                }
            } else if (position == 3) {
                return sdcardFragmentOtg;
            }
            throw new IllegalArgumentException("position: " + position);
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            // TODO Auto-generated method stub
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            // TODO Auto-generated method stub
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            mCurTransaction.show(f);
            return f;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object != null && ((Fragment) object).getView() == view;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // TODO Auto-generated method stub
        }

        @Override
        public Parcelable saveState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startUpdate(View view) {
            // TODO Auto-generated method stub
        }

        // 当滑动状态改变时调用
        public void onPageScrollStateChanged(int position) {
            // TODO Auto-generated method stub
            /*getActionBar()*/getSupportActionBar().setScrollState(position);//LEWA ADD BEGIN
            if(position == 0){
                ReflashMenu();
            }
        }

        // 当当前页面被滑动时调用
        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            // TODO Auto-generated method stub
            if (/*getActionBar()*/getSupportActionBar() == null)//LEWA ADD BEGIN
                return;
            try {
                if (positionOffset > 0.0 && positionOffsetPixels > 0) {
                    /*getActionBar()*/getSupportActionBar().smoothScrollTabIndicator(position,//LEWA ADD BEGIN
                            positionOffset, positionOffsetPixels);
                } else {
                    /*getActionBar()*/getSupportActionBar().smoothScrollTabIndicator(position, 0, 0);//LEWA ADD BEGIN
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                mViewPager.setCurrentItem(position);
            }
            if (getCanShowCheckBox()) {
                if (position == 0) {
                    showConfirmOperationBar(View.GONE);
                } else if (position > 0 && mSourceFragmentIndex != -1) {
                    showConfirmOperationBar(View.VISIBLE);
                }
            }
            ActionMode actionMode = ((FileManagerMainActivity) mContext)
                    .getActionMode();
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        // 当新的页面被选中时调用
        public void onPageSelected(final int position) {
            getFragment(position);
            mCurrentPosition = position;
            ActionBar actionBar = getSupportActionBar();//LEWA ADD BEGIN
            if (null != actionBar) {
                /*getActionBar()*/getSupportActionBar().setSelectedNavigationItem(position);//LEWA ADD BEGIN
                if (Util.hasSDCardReady(mContext)) {
                    handler.sendMessageDelayed(handler.obtainMessage(
                            MSG_Refresh_Sdcard_FileList, position, 0), 500);
                }
            }
            // added by weihong, #58413
//            cancelMoveState(position);

        }

        public int getCurrtenPosition() {
            return mCurrentPosition;
        }

    }

    public int getCurrentItemIndex() {
        if (mViewPager != null) {
            return mViewPager.getCurrentItem();
        }
        return 0;
    }

    public void setCurrentItemIndex(int id) {
        if (null != mViewPager  && null != mTabsAdapter) {
            mTabsAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(id);
        }
    }

    private class ScannerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Util.hasUsbOtg()
                    && action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Toast.makeText(context, getString(R.string.usbotg_remove),
                        Toast.LENGTH_LONG).show();
            }

            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                Util.initOrUpdateStaticValue(mContext);
                isdoubleSDCard = Util.sDualSDMounted;
                updateUI();
            }
        }
    }

    private void registerScannerReceiver() {
        mBroadcastReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        // intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
        Log2File.close();
    }

    private Timer timer;
    private static final int MSG_FILE_CHANGED_TIMER = 100;
    private static final int MSG_Refresh_Sdcard_FileList = 1;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_CHANGED_TIMER:
                    updateUI();
                    break;
                case MSG_Refresh_Sdcard_FileList:
                    try {
                        Fragment f = mTabsAdapter.getFragment(msg.arg1);
                        if (msg.arg1 != 0) {
                            if (Util.isSdCardNeedRefresh() && msg.arg1 == 1) {
                                ((IFileInteractionListener) f)
                                        .getFileViewInteractionHub()
                                        .refreshFileList();
                                Util.setIsSdCardNeedRefresh(false);
                            }
                            if (Util.isMemoryListNeedRefresh() && msg.arg1 == 2) {
                                ((IFileInteractionListener) f)
                                        .getFileViewInteractionHub()
                                        .refreshFileList();
                                Util.setMemoryListNeedRefresh(false);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };
    //after sdcard mount/umount need update otg path
    private void refreshOtg(FileViewFragment usbOtgFragment) {
            if (Util.usbOtgIndex != -1) {
                mSDCardList = Util.sSDCardDir;
                usbOtgFragment.setSDCardName(getResources().getString(
                        R.string.usbotg_name));
                usbOtgFragment.setSDCardPath(mSDCardList[Util.usbOtgIndex]);
                usbOtgFragment.setSDCardRootPath(mSDCardList[Util.usbOtgIndex]);
            }
    }
    /**
     * This function refresh the UI after receive mount/unmount broadcast.
     * Notice!! Do not need to Util.initStaticValue() here because has run it
     * once after received.
     */
    synchronized private void updateUI() {
        mContext = this;

        mSDCardList = Util.getsSDCardDirs(this);
        boolean dualSDCardMounted = Util.sDualSDMounted;//
        boolean hasSDCardReady = Util.hasSDCardReady(this);// There's SD card mounted.

        FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager
                .beginTransaction();

        //处理otg相关的逻辑
        FileViewFragment usbOtgFragment = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG3);
        if (Util.hasUsbOtg()) {
            if (DBG)
                Log2File.w("usbotg mounted");
            int usbOtgIndex = dualSDCardMounted ? 3 : 2;
            if (usbOtgFragment != null) {
                if (usbOtgFragment.isHidden()) {
                    transaction.show(usbOtgFragment);
                    mActionBar.addTab(usbTab, usbOtgIndex);
                }
            } else {
                usbOtgFragment = setupUsbOtgFragment();
                mActionBar.addTab(usbTab, usbOtgIndex);
            }
//            mTabsAdapter.notifyDataSetChanged();
//            if (mViewPager.getChildAt(usbOtgIndex) != null) {
//                mViewPager.getChildAt(usbOtgIndex).setVisibility(View.VISIBLE);
//            }
            usbOtgFragment.refresh();
        } else {
            if (DBG)
                Log2File.w("usbotg unmounted");
            if (mLastVolumeState[2]) {
                int usbOtgIndex = mLastVolumeState[1] ? 3 : 2;
                if (usbOtgFragment != null && usbOtgFragment.isVisible()) {
                    transaction.hide(usbOtgFragment);
                    mTabsAdapter.notifyDataSetChanged();
                    mActionBar.removeTab(usbTab);
//                    mViewPager.getChildAt(usbOtgIndex).setVisibility(View.GONE);
                }
            }
        }
        mTabsAdapter.notifyDataSetChanged();
        //处理外置SD卡相关的逻辑
        FileViewFragment sd2Fragment = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG2);

        if (dualSDCardMounted) {
            if (sd2Fragment != null && !sd2Fragment.getSDCardPath().equals("")) {
                sd2Fragment = setupSDCard2Fragment();
                if (sd2Fragment.isHidden()) {
                    transaction.show(sd2Fragment);
                    if (usbOtgFragment != null && usbOtgFragment.isVisible()) {
                        mActionBar.removeTab(usbTab);
                        mActionBar.addTab(sdcard1Tab, 2);
                        mActionBar.addTab(usbTab, 3);
                        refreshOtg(usbOtgFragment);
                    } else {
                        mActionBar.addTab(sdcard1Tab, 2);
                    }
                }
            } else {
                sd2Fragment = setupSDCard2Fragment();
                if (usbOtgFragment != null && usbOtgFragment.isVisible()) {
                    mActionBar.removeTab(usbTab);
                    mActionBar.addTab(sdcard1Tab, 2);
                    mActionBar.addTab(usbTab, 3);
                    refreshOtg(usbOtgFragment);
                } else {
                    mActionBar.addTab(sdcard1Tab, 2);
                }
            }
//            mTabsAdapter.notifyDataSetChanged();
//            if (mViewPager.getChildAt(2) != null) {
//                mViewPager.getChildAt(2).setVisibility(View.VISIBLE);
//            }
            sd2Fragment.refresh();
        } else if (!dualSDCardMounted && hasSDCardReady) {
            if (sd2Fragment != null) {// this.mViewPager.getChildCount() ==
                if (sd2Fragment.isVisible()) {
                    transaction.hide(sd2Fragment);
                    if (usbOtgFragment != null && usbOtgFragment.isVisible()) {
                        refreshOtg(usbOtgFragment);
                    }
                    mTabsAdapter.notifyDataSetChanged();
//                    mViewPager.getChildAt(2).setVisibility(View.GONE);
                    //#70510 add begin by bin.dong
                    if(mActionBar != null)
                        mActionBar.removeTab(sdcard1Tab);
                    //#70510 add end by bin.dong
                }
            }
        }
        //处理完成之后进行提交
        try {
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        updateVolumeState();
        mTabsAdapter.notifyDataSetChanged();
        if (usbOtgFragment != null && usbOtgFragment.isVisible()) {
            mViewPager.setOffscreenPageLimit(dualSDCardMounted?3:2);
        } else {
            mViewPager.setOffscreenPageLimit(dualSDCardMounted?2:1);
        }

        //最后处理内置SD卡的逻辑
        FileViewFragment sdFragment = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG);

        if ((1 == Util.getSdcardMountedNum() || !Util.sDualSDMounted) && !Util.hasUsbOtg()) {
            if (null != sdFragment) {
                sdFragment.setSDCardRootPath(mSDCardList[0]);
                sdFragment.setSDCardPath(mSDCardList[0]);
            }
        }
        setCurrentItemIndex(0);
    }

    private void updateVolumeState() {
        mLastVolumeState[0] = true;
        mLastVolumeState[1] = Util.sDualSDMounted;
        mLastVolumeState[2] = Util.hasUsbOtg();
    }

    private FileViewFragment setupSDCard2Fragment() {
        setCurrentItemIndex(0);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();

        sdcardFragment2 = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG2);
        mSDCardList = Util.sSDCardDir;

        // getSdDirectories(); // Set the SDCardPath for each FileViewFragment.

        if (sdcardFragment2 == null && isdoubleSDCard) {
            sdcardFragment2 = new FileViewFragment();
            if (!sdcardFragment2.isAdded()) {
                transaction.add(R.id.pager, sdcardFragment2, SDCARD_TAG2);
            }
        }
        sdcardFragment2.setSDCardName(getResources().getString(
                R.string.external_sdcard));
        sdcardFragment2.setSDCardPath(mSDCardList[1]);
        sdcardFragment2.setSDCardRootPath(mSDCardList[1]);
        try {
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return sdcardFragment2;
    }

    private FileViewFragment setupUsbOtgFragment() {
        setCurrentItemIndex(0);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();

        sdcardFragmentOtg = (FileViewFragment) fragmentManager
                .findFragmentByTag(SDCARD_TAG3);
        mSDCardList = Util.sSDCardDir;

        // getSdDirectories(); // Set the SDCardPath for each FileViewFragment.

        if (sdcardFragmentOtg == null) {
            sdcardFragmentOtg = new FileViewFragment();
            transaction.add(R.id.pager, sdcardFragmentOtg, SDCARD_TAG3);
        }
        sdcardFragmentOtg.setSDCardName(getResources().getString(
                R.string.usbotg_name));
        sdcardFragmentOtg.setSDCardPath(mSDCardList[Util.usbOtgIndex]);
        sdcardFragmentOtg.setSDCardRootPath(mSDCardList[Util.usbOtgIndex]);
        try {
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return sdcardFragmentOtg;
    }

    // added by weihong, #65297
    public SortMethod getSortMethod() {
        int sort = mSharedPreferences.getInt(FILE_SORT_KEY, 0);
        SortMethod sortMethod;
        switch (sort) {
            case 1:
                sortMethod = SortMethod.name;
                break;
            case 2:
                sortMethod = SortMethod.size;
                break;
            case 3:
                sortMethod = SortMethod.date;
                break;
            case 4:
                sortMethod = SortMethod.type;
                break;
            default:
                sortMethod = SortMethod.name;
                break;
        }
        return sortMethod;
    }

    // added by weihong, #65297
    public FileListIndex getFileListIndex() {
        FileListIndex listIndex;
        int sort = mSharedPreferences.getInt(FILE_SORT_KEY, 0);
        switch (sort) {
            case 0:
                listIndex = FileListIndex.NoIndex;
                break;
            case 1:
                listIndex = FileListIndex.Name;
                break;
            case 2:
                listIndex = FileListIndex.Size;
                break;
            case 3:
                listIndex = FileListIndex.Date;
                break;
            case 4:
                listIndex = FileListIndex.type;
                break;
            default:
                listIndex = FileListIndex.NoIndex;
                break;
        }
        return listIndex;
    }

    // added by weihong, #65297
    public void setFileListIndex(FileListIndex c) {
        mSharedPreferences.edit().putInt(FILE_SORT_KEY, c.ordinal()).commit();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case GlobalConsts.DIALOG_CLEAN_DIR:
                View cleanView = (ViewGroup) LayoutInflater.from(this).inflate(
                        R.layout.clean_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.cleanfinished))
                        .setView(cleanView)
                        .setCancelable(false)
                        .setNeutralButton(R.string.confirm,
                                new OnClickListener() {

                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        dialog.cancel();
                                        dialog.dismiss();
                                    }
                                }
                        ).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case GlobalConsts.DIALOG_CLEAN_DIR:
                showReport(
                        dialog,
                        getString(R.string.cleancount,
                                EmptyDirectoryCleaner.getCleanedCount()),
                        EmptyDirectoryCleaner.getCleanedCount(),
                        EmptyDirectoryCleaner.cleanedEntities
                );
                break;
        }
    }

    public void showReport(Dialog dialog, String reportMessage,
                           int reportCount, final List<ReportInfo> reportInfos) {
        final Dialog cleandialog = dialog;

        ((TextView) cleandialog.findViewById(R.id.cleancount))
                .setText(reportMessage);
    }

    class ViewHolder {
        TextView mVFileName;
        TextView mVFilePath;
    }

    class ReportDetailAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        List<ReportInfo> mReportDetail;

        public ReportDetailAdapter(Context context, List<ReportInfo> reportInfos) {

            mReportDetail = reportInfos;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mReportDetail.size();
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ViewHolder viewHolder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.cleanitem, null);
                viewHolder.mVFileName = (TextView) convertView
                        .findViewById(R.id.cleanname);
                viewHolder.mVFilePath = (TextView) convertView
                        .findViewById(R.id.cleanpath);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.mVFileName
                    .setText(mReportDetail.get(position).firstline_name);
            viewHolder.mVFilePath
                    .setText(mReportDetail.get(position).secondline_detail);

            return convertView;
        }

    }

    public int getPasteDestFragmentIndex() {
        return mPasteDestFragmentIndex;
    }

    public void setPasteDestFragmentIndex(int destFragmentIndex) {
        mPasteDestFragmentIndex = destFragmentIndex;
    }

    public int getSourceFragmentIndex() {
        return mSourceFragmentIndex;
    }

    public void setSourceFragmentIndex(int sourceFragmentIndex) {
        mSourceFragmentIndex = sourceFragmentIndex;
    }

    public FileOperationHelper getFileOperationHelper() {
        return mFileOperationHelper;
    }

    public TabsAdapter getTabsAdapter() {
        return mTabsAdapter;
    }

    @Override
    public void onFinish() {
        ((IFileInteractionListener) this
                .getFragment(getCurrentItemIndex()))
                .getFileViewInteractionHub().onFinish();
        handlers.removeCallbacks(runnable);
    }

    @Override
    public void onFileChanged(String path) {
        ((IFileInteractionListener) this
                .getFragment(getCurrentItemIndex()))
                .getFileViewInteractionHub().onFileChanged(path);
    }

    public Context getContext() {
        return mContext;
    }

    private void refreshDateTimeFormat() {
        try {
            String timeFormatStr = Settings.System.getString(
                    getContentResolver(), Settings.System.TIME_12_24);
            String dateFormatStr = Settings.System.getString(
                    getContentResolver(), Settings.System.DATE_FORMAT);
            if (timeFormatStr != null && !timeFormatStr.equals(mTimeFormatStr)) {
                timeFormat = getTimeFormat();
            }
            if (dateFormatStr != null && !dateFormatStr.equals(mDateFormatStr)) {
                dateFormat = getDateFormat();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class TabListener implements ActionBar.TabListener {
        private TabListener() {
        }

        public void onTabReselected(ActionBar.Tab tab,
                                    android.support.v4.app.FragmentTransaction paramFragmentTransaction) {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        public void onTabSelected(ActionBar.Tab tab,
                                  android.support.v4.app.FragmentTransaction paramFragmentTransaction) {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        public void onTabUnselected(ActionBar.Tab paramTab,
                                    android.support.v4.app.FragmentTransaction paramFragmentTransaction) {
        }
    }
//LEWA ADD END
}
