package com.lewa.filemanager;

import java.util.ArrayList;

import android.os.Environment;

public class NavigationHistory {
    private static final int MAX_LIST_SIZE = 20;
    private final ArrayList<NavigationRecord> mNavigationList;
    
    /**
     * The constructor to construct a navigation history list
     */ 
    protected NavigationHistory() {
        mNavigationList = new ArrayList<NavigationRecord>();
    }
    
    /**
     * This method gets the previous navigation directory path
     @return        the previous navigation path      
     */ 
    protected NavigationRecord getPrevNavigation() {
        if (mNavigationList.isEmpty()) {
            return null;
        } else {
            NavigationRecord navRecord = mNavigationList.get(mNavigationList.size() -1);
            removeFromNavigationList();
            return navRecord;
        }
    }
    
    /**
     * This method adds a directory path to the navigation history
     @param  path       the directory path     
     */
    protected void addToNavigationList(NavigationRecord navigationRecord) {
        if (mNavigationList.size() <= MAX_LIST_SIZE) {
            mNavigationList.add(navigationRecord);
        } else {
            mNavigationList.remove(0);
            mNavigationList.add(navigationRecord);
        }
    }
    
    /**
     * This method removes a directory path from the navigation history
     */
    protected void removeFromNavigationList() {
        if (!mNavigationList.isEmpty()) {
            mNavigationList.remove(mNavigationList.size() - 1);
        }
    }
    
    /**
     * This method clears the navigation history list. Keep the root path only
     */
    protected void clearNavigationList() {
        mNavigationList.clear();
    }

    static public class NavigationRecord {
        private String mNavigationDirPath = null;
        private String mFocusedFileName = null;
        private int mTop = -1;
        public NavigationRecord(String navigationDirPath, String focusedFileName, int top){
            mNavigationDirPath = navigationDirPath;
            mFocusedFileName = focusedFileName;
            mTop = top;
        }

        public String getNavigationDirPath() {
            return mNavigationDirPath;
        }

        public String getFocusedFileName() {
            return mFocusedFileName;
        }

        public int getTop() {
            return mTop;
        }
    }
}