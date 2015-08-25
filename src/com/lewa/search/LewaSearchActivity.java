package com.lewa.search;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.lewa.filemanager.R;
//LEWA ADD BEGIN
//import android.app.ActionBar;
//import android.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar;
import lewa.support.v7.app.ActionBarActivity;
import lewa.support.v7.app.ActionBar.Tab;
import lewa.support.v7.app.ActionBar.TabListener;
import lewa.support.v7.app.ActionBar.LayoutParams;
//LEWA ADD END
//mport android.app.ActionBar;
import android.app.Activity;
//import android.app.ActionBar.LayoutParams;
import android.content.Context;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.lewa.search.adapter.FileInfoAdapter;
import com.lewa.search.bean.FileInfo;
import com.lewa.search.bean.ImageFileInfo;
import com.lewa.search.bean.MusicFileInfo;
import com.lewa.search.bean.NormalFileInfo;
import com.lewa.search.bean.VideoFileInfo;
import com.lewa.search.db.DBUtil;
import com.lewa.search.decorator.Decorator;
import com.lewa.search.decorator.TextHighLightDecorator;
import com.lewa.search.decorator.TextSimplifiedHighLightDecorator;
import com.lewa.search.match.KeyMatcher;
import com.lewa.search.system.Constants;
import com.lewa.search.system.SystemMode;
import com.lewa.search.util.FileInfoUtil;
import com.lewa.search.util.SearchUtil;

/**
 * This class defines the Activity on "search" page.
 *
 * @author wangfan
 * @version 2012.07.04
 */

public class LewaSearchActivity extends ActionBarActivity implements OnQueryTextListener, OnCloseListener {

    public String SEARCH_SCHEME = "com.lewa.search";

    //this list contains the searching results
    public List<FileInfo> resultList = new ArrayList<FileInfo>();

    //this adapter and listView presents the results on the screen
    private FileInfoAdapter fileInfoAdapter;
    private ListView listView;
    private TextView headerText;

    //this editText get the user input
    //private SearchEditText editSearch;

    //this button clear the user input
    //private ImageView deleteButton;

    //this matcher matches the key in decorator
    private KeyMatcher matcher;

    //register a map of decorators for variables showed in the view
    private Map<Integer, Decorator> decorators = new HashMap<Integer, Decorator>();

    //register a thread and a handler for search task to avoid time delay in user input
    private SearchThread searchThread;
    private SearchHandler searchHandler;

    private boolean searchImage;
    private boolean searchMusic;
    private boolean searchFile;
    private boolean sdcardIsEnable = true;
    /// new style
    View mCustomSearchView;
    private ActionBar mActionBar;
    private SearchView mSearchView;
    LayoutParams mLayoutParams;
    private String mSearchKey = null;
//    private Handler mHandler = null;

    public static int MAX_FILENAME_VALUE = 28;
//    private static final int EVENT_SHOW_KEYBOARD = 1;

    boolean IsPick = false;
    Activity mSearchActivity = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //mSearchView = (SearchView) mCustomSearchView.findViewById(R.id.search_view);

        initSystems();
        initUtils();
        initViews();
        initDecorators();
        initActionView();
        mSearchActivity = this;
        Intent fromIntent = getIntent();
        IsPick = fromIntent.getBooleanExtra("IsPick", false);

    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        sdcardIsEnable = true;
        super.onResume();

    }


    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

    }

    /**
     * This method initialize system environment and record system state.
     */
    private void initSystems() {
        //initialize a handler to process message
        searchHandler = new SearchHandler();

        //initialize language mode
        String language = Locale.getDefault().getLanguage();
        if (language == null || language.equals("zh")) {
            SystemMode.langeuageMode = Constants.LANGUAGEMODE_CHINESE;
        } else {
            SystemMode.langeuageMode = Constants.LANGUAGEMODE_ENGLISH;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    private void updateDisplayOptions() {
        // All the flags we may change in this method.
//	        final int MASK = ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
//	                | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.NAVIGATION_MODE_STANDARD;
        final int MASK = ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_CUSTOM;
        // The current flags set to the action bar.  (only the ones that we may change here)
        mActionBar.setIcon(null);
        mActionBar.setDisplayOptions(MASK);
        mActionBar.show();
    }
    // TODO: for temporarily resolving: can't auto show IME soft keyboard.
//	    private void openKeyboard() {
//

    @Override
    public Intent getIntent() {
        return super.getIntent();
    }
//	        Timer timer = new Timer();
//	        timer.schedule(new TimerTask() {
//	                public void run() {
//
//	                    mHandler.sendEmptyMessage(EVENT_SHOW_KEYBOARD);
//
//	                }
//	        }, 1000);
//	    }


    /**
     * This method initialize some system utils
     */
    private void initUtils() {
        //set contetx for database search
        DBUtil.context = LewaSearchActivity.this;
        //initialize softCache to cache some datas and structures used frequently
        //SoftCache.init(LewaSearchActivity.this);

        //initialize a fileAdapter
        //each adapter can only have one layout for item
        //the methodArgs helps to assemble the method name in invocation
        fileInfoAdapter = new FileInfoAdapter(getLayoutInflater());
        fileInfoAdapter.fileList = resultList;
        fileInfoAdapter.setMethodArgs("get");
        fileInfoAdapter.setLayoutId(R.layout.listitem);

        //initialize a matcher
        matcher = new KeyMatcher("");
    }

    private void initActionView() {
        mActionBar = getSupportActionBar();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCustomSearchView = inflater.inflate(R.layout.search_bar, null);

        int searchViewWidth = this.getResources().getDimensionPixelSize(
                R.dimen.search_view_width);
        if (searchViewWidth == 0) {
            searchViewWidth = LayoutParams.MATCH_PARENT;
        }
        mSearchView = (SearchView) mCustomSearchView.findViewById(R.id.search_view);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setQueryHint(this.getString(R.string.hint_findFiles));
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);

        Display display = getWindowManager().getDefaultDisplay();
        mLayoutParams = new LayoutParams(display.getWidth()
                - getResources().getDimensionPixelSize(R.dimen.search_view_margin_left),
                LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        mActionBar.setCustomView(mCustomSearchView, mLayoutParams);

        mSearchView.setQuery(mSearchKey, false);
        setFocusOnSearchView();
        updateDisplayOptions();
    }

    private void setFocusOnSearchView() {
        mSearchView.requestFocus();

        mSearchView.setIconified(false); // Workaround for the "IME not popping up" issue.
    }

    /**
     * This method initialize views showed on the screen
     */
    private void initViews() {
        //initialize the listView
        listView = (ListView) findViewById(R.id.ListEntries);
        listView.setAdapter(fileInfoAdapter);
        listView.setOnTouchListener(listTouchListener);
        listView.setOnItemClickListener(fileEnterListener);

        headerText = (TextView) findViewById(R.id.ListHeader);
        headerText.setVisibility(View.GONE);
    }

    @Override
    public boolean onClose() {
        // TODO Auto-generated method stub

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // TODO Auto-generated method stub
        headerText.setVisibility(View.GONE);
        if (!SearchUtil.isSDCardEnable()) {
            sdcardIsEnable = false;
        }

        String key = newText;
        mSearchKey = newText;
        if (key.contains("'")) {
            sdcardIsEnable = true;
        }

        //clear list each time when a new search starts
        cleanList();
        if (!key.equals("")) //user has input,start a new thread
        {
            //updata matchers
            updateMatchers(key);
            //stop current searchThread
            if (searchThread != null) {
                searchThread.stoped = true;
                searchThread.interrupt();
            }

            //check searching scope
            loadSearchInfo();

            //start a new searchThread
            searchThread = new SearchThread(key);
            searchThread.start();
        } else //user clears input,stop searching
        {
            if (searchThread != null) {
                searchThread.stoped = true;
                searchThread.interrupt();
            }
        }
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // TODO Auto-generated method stub
        android.util.Log.e("Search", "onQueryTextSubmit():" + query);
        return false;
    }

    /**
     * This method initialize decorators used for decorating objects showed on the screen
     */
    private void initDecorators() {
        Resources rs = this.getResources();
        //initialize a TextHighLightDecorator.
        TextHighLightDecorator decoratorHighLight = new TextHighLightDecorator(rs.getColor(R.color.highlight_blue));
        decoratorHighLight.setMatcher(matcher);
        decorators.put(Decorator.DECORATOR_HIGHLIGHT, decoratorHighLight);

        //initialize a TextSimplifiedHighLightDecorator without suffix
        TextSimplifiedHighLightDecorator decoratorHighLightSimplifiedNoSuffix = new TextSimplifiedHighLightDecorator(rs.getColor(R.color.highlight_blue), MAX_FILENAME_VALUE, "...", false);
        decoratorHighLightSimplifiedNoSuffix.setMatcher(matcher);
        decorators.put(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED_NO_SUFFIX, decoratorHighLightSimplifiedNoSuffix);

        //initialize a TextSimplifiedHighLightDecorator with suffix
        TextSimplifiedHighLightDecorator decoratorHighLightSimplified = new TextSimplifiedHighLightDecorator(rs.getColor(R.color.highlight_blue), MAX_FILENAME_VALUE, "...", true);
        decoratorHighLightSimplified.setMatcher(matcher);
        decorators.put(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED, decoratorHighLightSimplified);
    }

    //create an object lock for searchThreads visiting database
    static Object searchLock = new Object();

    /**
     * This inner class defines a thread for search files and infomation match the key
     *
     * @author wangfan
     * @version 2012.07.04
     */
    class SearchThread extends Thread {

        private String key;
        //a tag represents whether the thread should be shopped
        //it will be checked before each operation which has the need to visit database
        private boolean stoped = false;

        //a temp list to store search result, each thread has his own temp list
        private List<FileInfo> threadList;

        public SearchThread(String key) {
            this.key = key;
        }

        public void run() {
            synchronized (searchLock) {
                //initialize an empty temp list
                threadList = new ArrayList<FileInfo>();
                if (!sdcardIsEnable) {
                    sdcardIsEnable = true;
                    //search setting information
//                  if(stoped == false && searchSetting == true)
//                  {
//                      List<FileInfo> tempList = searchSettings(key);
//                      if(tempList != null && stoped == false)
//                      {
//                          threadList.addAll(tempList);
//                      }
//                  }

//                  //search contact information
//                  if(stoped == false && searchContact == true)
//                  {
//                      List<FileInfo> tempList = searchContracts(key);
//
//                      if(tempList != null)
//                      {
//                          threadList.addAll(tempList);
//                      }
//                  }

                    //search message information
//                  if(stoped == false && searchMessage == true)
//                  {
//                      List<FileInfo> tempList = searchMessages(key);
//
//                      if(tempList != null  && stoped == false)
//                      {
//                          threadList.addAll(tempList);
//                      }
//                  }

                    //search application information
//                  if(stoped == false && searchApp == true)
//                  {
//                      List<FileInfo> tempList = searchApps(key);
//
//                      if(tempList != null && stoped == false)
//                      {
//                          threadList.addAll(tempList);
//                      }
//                  }

                    //search image information
                    if (stoped == false && searchImage == true) {
                        List<FileInfo> tempList = searchImages(key);

                        if (tempList != null && stoped == false) {
                            threadList.addAll(tempList);
                        }
                    }

                    //search music information
                    if (stoped == false && searchMusic == true) {
                        List<FileInfo> tempList = searchMusics(key);

                        if (tempList != null && stoped == false) {
                            threadList.addAll(tempList);
                        }
                    }

                    //search file information,video files was regarded as normal file information
                    if (searchFile == true) {
                        if (stoped == false) {
                            List<FileInfo> tempList = searchNormals(key);

                            if (tempList != null && stoped == false) {
                                threadList.addAll(tempList);
                            }
                        }

                        if (stoped == false) {
                            List<FileInfo> tempList = searchVideos(key);

                            if (tempList != null && stoped == false) {
                                threadList.addAll(tempList);
                            }
                        }
                    }
                }


//    			if(stoped == false)
//    			{
//    				List<FileInfo> tempList = buildWebSearchButtons(key);
//
//    				 if(tempList != null && stoped == false)
//        			{
//        				threadList.addAll(tempList);
//        			}
//    			}

                //return the final result to resultList owned by this activity
                if (stoped == false) {
                    Message msg = new Message();
                    msg.what = Constants.INFO_DATA_CHANGED;
                    Handler handler = LewaSearchActivity.this.searchHandler;

                    //Remove duplicates
                    // add 2013-03-28
                    //
                    List<FileInfo> templist = new ArrayList<FileInfo>();
                    Map<String, String> map = new HashMap<String, String>();
                    for (FileInfo fi : threadList) {
                        if (map.containsKey(fi.getText())) {
                            continue;
                        }
                        map.put(fi.getText(), fi.getTitle());
                        templist.add(fi);
                    }
                    threadList.clear();

                    //clear resultList before addition
                    resultList.clear();

                    //add result to resultList
                    resultList.addAll(templist);

                    //send message to notify the change
                    handler.sendMessage(msg);
                }
            }
        }

        /**
         * This method was used for searching setting information.
         * @param key    key in search
         */
//    	@SuppressWarnings("unchecked")
//        private List<FileInfo> searchSettings(String key)
//        {
//    		//get search result
//        	List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_SETTING, key, "");
//
//            if(tempList != null)
//        	{
//            	//register a TextHighLightDecorator for the name field "title"
//            	Map<String, Decorator> decoratorsControctInfo = new HashMap<String, Decorator>();
//            	decoratorsControctInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//            	decoratize(tempList, decoratorsControctInfo);
//
//            	 return tempList;
//        	}
//
//            return null;
//        }

        /**
         * This method was used for searching contact information.
         * @param key    key in search
         */
//    	@SuppressWarnings("unchecked")
//    	private List<FileInfo> searchContracts(String key)
//        {
//    		//get search result
//            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_CONTRACT, key, "_id COLLATE NOCASE");
//
//            if(tempList != null)
//        	{
//            	//register a TextHighLightDecorator for the name field "title"
//            	//register a TextHighLightDecorator for the name field "text"
//            	Map<String, Decorator> decoratorsControctInfo = new HashMap<String, Decorator>();
//            	decoratorsControctInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//            	decoratorsControctInfo.put("text", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//            	decoratize(tempList, decoratorsControctInfo);
//
//            	 return tempList;
//        	}
//
//            return null;
//        }

        /**
         * This method was used for searching message information.
         * @param key    key in search
         */
//        @SuppressWarnings("unchecked")
//        private List<FileInfo> searchMessages(String key)
//        {
//        	//get search result
//            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_MESSAGE, key, "_id COLLATE NOCASE");
//
//            if(tempList != null)
//        	{
//            	//register a TextHighLightDecorator for the name field "title"
//            	//register a TextSimplifiedHighLightDecorator without suffix for the name field "text"
//            	Map<String, Decorator> decoratorsMessageInfo = new HashMap<String, Decorator>();
//            	decoratorsMessageInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//            	decoratorsMessageInfo.put("text", decorators.get(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED_NO_SUFFIX));
//            	decoratize(tempList, decoratorsMessageInfo);
//
//            	 return tempList;
//        	}
//
//            return null;
//        }

        /**
         * This method was used for searching message information.
         * @param key    key in search
         */
//        @SuppressWarnings("unchecked")
//    	private List<FileInfo> searchApps(String key)
//        {
//        	//get search result
//            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_APP, key, null);
//
//            if(tempList != null)
//        	{
//            	//register a TextHighLightDecorator for the name field "title"
//            	Map<String, Decorator> decoratorsControctInfo = new HashMap<String, Decorator>();
//            	decoratorsControctInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//            	decoratize(tempList, decoratorsControctInfo);
//
//            	 return tempList;
//        	}
//
//            return null;
//        }

        /**
         * This method was used for searching message information.
         *
         * @param key key in search
         */
        @SuppressWarnings("unchecked")
        private List<FileInfo> searchNormals(String key) {
            //get search result
            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_NORMAL, key, "_id COLLATE NOCASE");

            if (tempList != null) {
                //register a TextSimplifiedHighLightDecorator with suffix for the name field "title"
                Map<String, Decorator> decoratorsNormalInfo = new HashMap<String, Decorator>();
                decoratorsNormalInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED));
                decoratize(tempList, decoratorsNormalInfo);

                return tempList;
            }

            return null;
        }

        /**
         * This method was used for searching image files.
         *
         * @param key key in search
         */
        @SuppressWarnings("unchecked")
        private List<FileInfo> searchImages(String key) {
            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_IMAGE, key, "_id COLLATE NOCASE");

            if (tempList != null) {
                Map<String, Decorator> decoratorsImageInfo = new HashMap<String, Decorator>();
                decoratorsImageInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED));
                decoratize(tempList, decoratorsImageInfo);

                return tempList;
            }

            return null;
        }

        /**
         * This method was used for searching music files.
         *
         * @param key key in search
         */
        @SuppressWarnings("unchecked")
        private List<FileInfo> searchMusics(String key) {
            //get search result
            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_MUSIC, key, "_id COLLATE NOCASE");

            if (tempList != null) {
                //register a TextSimplifiedHighLightDecorator with suffix for the name field "title"
                Map<String, Decorator> decoratorsMusicInfo = new HashMap<String, Decorator>();
                decoratorsMusicInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED));
                decoratize(tempList, decoratorsMusicInfo);

                return tempList;
            }

            return null;
        }

        /**
         * This method was used for searching video files.
         *
         * @param key key in search
         */
        @SuppressWarnings("unchecked")
        private List<FileInfo> searchVideos(String key) {
            //get search result
            List<FileInfo> tempList = (List<FileInfo>) FileInfoUtil.searchContentsByKey(Constants.CLASS_VIDEO, key, "_id COLLATE NOCASE");

            if (tempList != null) {
                //register a TextSimplifiedHighLightDecorator with suffix for the name field "title"
                Map<String, Decorator> decoratorsVideoInfo = new HashMap<String, Decorator>();
                decoratorsVideoInfo.put("title", decorators.get(Decorator.DECORATOR_HIGHLIGHT_AND_SIMPLIFIED));
                decoratize(tempList, decoratorsVideoInfo);

                return tempList;
            }

            return null;
        }

        /**
         * This method was used to pretend to build file items with no need to visit database.
         * These items was regarded as buttons to search web resources.
         * @param key    key in search
         */
//        private List<FileInfo> buildWebSearchButtons(String key)
//        {
//        	//build search buttons
//        	List<FileInfo> tempList = (List<FileInfo>) WebSearchUtil.buildWebSearchButtons(LewaSearchActivity.this, key);
//
//        	if(tempList != null)
//        	{
//        		//register a TextHighLightDecorator for the name field "text"
//        		Map<String, Decorator> decoratorsVideoInfo = new HashMap<String, Decorator>();
//                decoratorsVideoInfo.put("text", decorators.get(Decorator.DECORATOR_HIGHLIGHT));
//                decoratize(tempList, decoratorsVideoInfo);
//
//                return tempList;
//        	}
//
//        	return null;
//        }

        /**
         * This method set each item a map decorator in the fileList.
         * Each kind of fileList can only register one map of decorators.
         *
         * @param fileList   the list to be decorated
         * @param decorators this map should be deliver to each item in this list
         */
        private void decoratize(List<FileInfo> fileList, Map<String, Decorator> decorators) {
            FileInfo fileInfo;
            for (int i = 0; i < fileList.size(); i++) {
                fileInfo = fileList.get(i);
                fileInfo.decorators = decorators;
            }
        }
    }

    /**
     * This inner class defines a handler to handle messages sent by searchThread.
     *
     * @author wangfan
     * @version 2012.07.04
     */
    class SearchHandler extends Handler {
        public SearchHandler() {

        }

        public SearchHandler(Looper L) {
            super(L);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case Constants.INFO_DATA_CHANGED: {
                    if (resultList.size() > 0) {
                        //update views on the screen
                        fileInfoAdapter.notifyDataSetChanged();
                    } else {
                        headerText.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    /**
     * This method was used for loading search scope from shared preference.
     * This preference can be modified in LewaSearchInfoSettingActivity.
     */
    private void loadSearchInfo() {
        //String STORE_NAME = "com.lewa.search_preferences";
        SharedPreferences settings = getSharedPreferences(Constants.STORE_NAME, MODE_PRIVATE);

//		searchMessage = settings.getBoolean("info_setting_message", false);
//		searchApp = settings.getBoolean("info_setting_app", false);
//		searchContact = settings.getBoolean("info_setting_contact", false);
        searchImage = settings.getBoolean("info_setting_image", true);
        searchMusic = settings.getBoolean("info_setting_music", true);
//		searchSetting = settings.getBoolean("info_setting_setting", false);
        searchFile = settings.getBoolean("info_setting_file", true);
    }

    /**
     * This method was used for updating matcher.
     */
    private void updateMatchers(String key) {
        matcher.updateMatcher(key);
    }

    /**
     * This method was used to clear resultList.
     */
    private void cleanList() {
        if (resultList.size() > 0) {
            resultList.clear();
        }
        //resultList has changed,notify to update screen
        fileInfoAdapter.notifyDataSetChanged();
    }

    //refer to PIM ~~~ begin ~~~

    /**
     * Dismisses the search UI along with the keyboard if the filter text is empty.
     */
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && (mSearchKey == null || mSearchKey.equals(""))) {
//            hideSoftKeyboard();
            onBackPressed();

            return true;
        }
        return false;
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(listView.getWindowToken(), 0);
    }

    private void showSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        //inputMethodManager.showSoftInputFromInputMethod(listView.getWindowToken(), 0);
        // inputMethodManager.showSoftInputFromInputMethod(mSearchView.getWindowToken(), 0);
        // inputMethodManager.showSoftInput(mSearchView, InputMethodManager.SHOW_FORCED);
        //inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_FORCED);
    }
    //~~~ end ~~~

    /**
     * Register the click listener.
     */
    /*@Override
    public void onClick(View view) {
		// TODO Auto-generated method stub
		//clear user input when deleteButton was pressed
		if(view == deleteButton)
		{
			editSearch.setText("");
		}

		//clear focus on editSearch when listView was touched
		else if(view == listView)
		{
			editSearch.clearFocus();
		}

	}*/


    public static final int REQ_IMAGE_CROP = 101;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMAGE_CROP || resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }


    /**
     * Register the itemClick listener.
     */
    private OnItemClickListener fileEnterListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

            //get fileItem clicked and open the file
            FileInfo fileItem = resultList.get(arg2);
            if (IsPick) {
                Log.d("fm", "ispick:");
                onPick(fileItem);
            } else {
                if (fileItem.getText().endsWith(".lwt")) {
                    openTheme(LewaSearchActivity.this, fileItem);
                } else {
                    enterFile(LewaSearchActivity.this, fileItem);
                }
            }
        }

        public void onPick(FileInfo f) {
            // do nothing
            try {
                Uri uri = Uri.fromFile(new File(f.getText()));
                Intent intent = Intent.parseUri(uri.toString(), 0);
                Intent fromIntent = mSearchActivity.getIntent();
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
                    mSearchActivity.startActivityForResult(newIntent, REQ_IMAGE_CROP);
                } else {
                    mSearchActivity.setResult(Activity.RESULT_OK, intent);
                    mSearchActivity.finish();
                }
                return;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        private void openTheme(Context context, FileInfo fileItem) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.lewa.themechooser",
                    "com.lewa.themechooser.receiver.ThemeInstallService"));
            intent.putExtra("THEME_PACKAGE", fileItem.getText());
            //if(filePath.endsWith(".lwt")){
            intent.putExtra("isFromFileManager", true);
            context.startService((intent));
        }

        private void enterFile(Context context, FileInfo fileItem) {
            switch (fileItem.getFileType()) {
//				case Constants.CLASS_APP:
//				{
//					if(!FileInfoUtil.openApp(LewaSearchActivity.this, ((AppFileInfo) fileItem).getPackageName()))
//					{
//						//assemble error text and display it
//						Resources re = context.getResources();
//						String errorTextPre = re.getString(R.string.enter_app_error_prefix);
//						String errorTextSuf = re.getString(R.string.enter_app_error_suffix);
//						String errorText;
//
//						int appType = ((AppFileInfo) fileItem).getAppType();
//
//						if(appType == Constants.APP_SYSTEM_TYPE)
//						{
//							errorText = errorTextPre + re.getString(R.string.system_app) + errorTextSuf;
//
//						}
//						else
//						{
//							errorText = errorTextPre + re.getString(R.string.user_app) + errorTextSuf;
//						}
//
//						Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show();
//					}
//
//					break;
//				}

//				case Constants.CLASS_CONTRACT:
//				{
//					//open contact detail
//					FileInfoUtil.openContract(context, Long.valueOf(((ContactFileInfo) fileItem).getContactId()));
//
//					break;
//				}

                case Constants.CLASS_IMAGE: {
                    //open image file
                    FileInfoUtil.openFile(((ImageFileInfo) fileItem).getFilePath(), context);
                    break;
                }

//				case Constants.CLASS_MESSAGE:
//				{
//					//open message and locate
//					FileInfoUtil.openMessages(context, ((MessageFileInfo) fileItem).getMessageId(),
//							((MessageFileInfo) fileItem).getThreadId(), ((MessageFileInfo) fileItem).getNumber());
//
//					break;
//				}

                case Constants.CLASS_MUSIC: {
                    //open music file
                    FileInfoUtil.openFile(((MusicFileInfo) fileItem).getFilePath(), context);
                    break;
                }

                case Constants.CLASS_NORMAL: {
                    //open normal file
                    FileInfoUtil.openFile(((NormalFileInfo) fileItem).getFilePath(), context);
                    break;
                }

//				case Constants.CLASS_SETTING:
//				{
//					//open system setting
//					FileInfoUtil.openSetting(context, ((SettingFileInfo) fileItem).getActionName(),
//							((SettingFileInfo) fileItem).getPackageName());
//
//					break;
//				}

                case Constants.CLASS_VIDEO: {
                    //open video file
                    FileInfoUtil.openFile(((VideoFileInfo) fileItem).getFilePath(), context);
                    break;
                }

//				case Constants.CLASS_WEB:
//				{
//					//open browser to search resources from web
//					int webSearchType = ((WebSearchFileInfo) fileItem).getWebSearchType();
//					String key = ((WebSearchFileInfo) fileItem).getKey();
//					if(webSearchType == Constants.WEB_SEARCH)
//					{
//						//search information from web
//						WebSearchUtil.webSearch(context, Constants.WEB_SEARCH, key);
//					}
//					else
//					{
//						//search application from web
//						WebSearchUtil.webSearch(context, Constants.APP_SEARCH, key);
//					}
//				}
            }
        }
    };

    /**
     * Register the touch listener.
     */
    private OnTouchListener listTouchListener = new OnTouchListener() {

        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub

            //hide keyboard when list was touched
            hideSoftKeyboard();
            return false;
        }
    };


    /**
     * Create option menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //register a menu for search setting and load a icon for this menu.
//		menu.add(0, Constants.MENU_SEARCH_SETTING, Menu.FIRST, R.string.app_setting).setIcon(R.drawable.ic_menu_setting);
        return true;
    }


    /**
     * Define operation when option item was selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//		case Constants.MENU_SEARCH_SETTING:
//			// start a LewaSearchSettingActivity
//			startActivity(new Intent(LewaSearchActivity.this,
//					LewaSearchSettingActivity.class));
//		    break;

            case android.R.id.home:
                // back icon in action bar clicked; go home
                onBackPressed();
                return true;

        }

        return super.onContextItemSelected(item);
    }
}
