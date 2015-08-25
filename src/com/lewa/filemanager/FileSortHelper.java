package com.lewa.filemanager;

import java.util.Comparator;
import java.util.HashMap;

import android.util.Log;

public class FileSortHelper {

    public enum SortMethod {
        name, size, date, type
    }

    private SortMethod mSort;

    private boolean mFileFirst;

    private HashMap<SortMethod, Comparator> mComparatorList = new HashMap<SortMethod, Comparator>();

    public FileSortHelper() {
        mSort = SortMethod.name;
        mComparatorList.put(SortMethod.name, cmpName);
        mComparatorList.put(SortMethod.size, cmpSize);
        mComparatorList.put(SortMethod.date, cmpDate);
        mComparatorList.put(SortMethod.type, cmpType);
    }

    public void setSortMethog(SortMethod s) {
        mSort = s;
    }

    public SortMethod getSortMethod() {
        return mSort;
    }

    public void setFileFirst(boolean f) {
        mFileFirst = f;
    }

    public Comparator getComparator() {
        return mComparatorList.get(mSort);
    }

    private abstract class FileComparator implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo object1, FileInfo object2) {
            if (object1.IsDir == object2.IsDir) {
                return doCompare(object1, object2);
            }

            if (mFileFirst) {
                // the files are listed before the dirs
                return (object1.IsDir ? 1 : -1);
            } else {
                // the dir-s are listed before the files
                return object1.IsDir ? -1 : 1;
            }
        }

        protected abstract int doCompare(FileInfo object1, FileInfo object2);
    }

    private Comparator cmpName = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {

//        	String fileNameTemp1 = object1.fileName;
//        	String fileNameTemp2 = object2.fileName;
//        	if(object1.fileCHName != null && object1.fileCHName.trim() != "" ) {
//        		Log.d("yx", "object1.fileCHName.trim() = " + object1.fileCHName.trim());
//        		fileNameTemp1 = object1.fileCHName;        		
//            }
//        	if(object2.fileCHName != null && object2.fileCHName.trim() != "" ) fileNameTemp2 = object2.fileCHName;	
//        	return fileNameTemp1.compareToIgnoreCase(fileNameTemp2);
        	//object1.fileName.compareToIgnoreCase(object2.fileName);
        	return object1.fileSDsortName.compareToIgnoreCase(object2.fileSDsortName);
        			//object1.fileName.toLowerCase().compareToIgnoreCase(object2.fileName.toLowerCase());
        }
    };

    private Comparator cmpSize = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return longToCompareInt(object1.fileSize - object2.fileSize);
        }
    };

    private Comparator cmpDate = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return longToCompareInt(object2.ModifiedDate - object1.ModifiedDate);
        }
    };

    private int longToCompareInt(long result) {
        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    private Comparator cmpType = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            int result = Util.getExtFromFilename(object1.fileName).compareToIgnoreCase(
                    Util.getExtFromFilename(object2.fileName));
            if (result != 0)
                return result;

            return Util.getNameFromFilename(object1.fileName).compareToIgnoreCase(
                    Util.getNameFromFilename(object2.fileName));
        }
    };
}
