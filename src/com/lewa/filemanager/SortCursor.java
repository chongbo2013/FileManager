package com.lewa.filemanager;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

public class SortCursor extends CursorWrapper {
	Cursor mCursor;
	ArrayList<SortEntry> sortList = new ArrayList<SortEntry>();
	int mPos = 0;

	public class SortEntry {
		public String key;
		public int order;
	}

	// 直接初始化,加快比较速度,在G3上从3s->0.2s
	@SuppressWarnings("rawtypes")
	private Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);

	@SuppressWarnings("unchecked")
	public Comparator<SortEntry> comparator = new Comparator<SortEntry>() {
		@Override
		public int compare(SortEntry entry1, SortEntry entry2) {
			// return cmp.compare(entry1.key.toUpperCase(),
			// entry2.key.toUpperCase());
			// Log.d("yx", "entry1.key: " + entry1.key + "; entry2.key: " +
			// entry2.key);
			// Log.d("yx", "entry1.key.toUpperCase(): " +
			// entry1.key.toUpperCase() + "; entry2.key.toUpperCase(): " +
			// entry2.key.toUpperCase());
			// Log.d("yx",
			// "entry1.key.toUpperCase().compareToIgnoreCase(entry2.key.toUpperCase()): "
			// +
			// entry1.key.toUpperCase().compareToIgnoreCase(entry2.key.toUpperCase()));
			return entry1.key.toUpperCase().compareToIgnoreCase(
					entry2.key.toUpperCase());
		}
	};

	public SortCursor(Cursor cursor, String columnName, boolean mixEngCh) {
		super(cursor);
		// MusicLog.i("MediaScanner", "start-----------");
		// TODO Auto-generated constructor stub
		mCursor = cursor;
		if (mCursor != null && mCursor.getCount() > 0) {
			int i = 0;
			int column = cursor.getColumnIndexOrThrow(columnName);
			for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor
					.moveToNext(), i++) {
				SortEntry sortKey = new SortEntry();
				sortKey.key = cursor.getString(column);
				sortKey.key = Util.getPinyinFromCH(sortKey.key, mixEngCh);
				// Log.d("yx", "sortKey.key: " + sortKey.key);
				sortKey.order = i;
				sortList.add(sortKey);
			}
		}
		// MusicLog.i("MediaScanner", "start1-----------");
		// 排序
		Collections.sort(sortList, comparator);
		// Collections.sort(mFileNameList, sort.getComparator());
		// FileSortHelper mFileSortHelper = new FileSortHelper();
		// Collections.sort(sortList, mFileSortHelper.getComparator());
		// MusicLog.i("MediaScanner", "start2-----------");
	}

	public boolean moveToPosition(int position) {
		if (position >= 0 && position < sortList.size()) {
			mPos = position;
			int order = sortList.get(position).order;
			return mCursor.moveToPosition(order);
		}
		if (position < 0) {
			mPos = -1;
		}
		if (position >= sortList.size()) {
			mPos = sortList.size();
		}
		return mCursor.moveToPosition(position);
	}

	public String getKey() {
		// Log.d("yx", "getKey: " + sortList.get(mPos).key);
		return sortList.get(mPos).key;
	}

	public boolean moveToFirst() {
		return moveToPosition(0);
	}

	public boolean moveToLast() {
		return moveToPosition(getCount() - 1);
	}

	public boolean moveToNext() {
		return moveToPosition(mPos + 1);
	}

	public boolean moveToPrevious() {
		return moveToPosition(mPos - 1);
	}

	public boolean move(int offset) {
		return moveToPosition(mPos + offset);
	}

	public int getPosition() {
		return mPos;
	}

	public boolean CursorIsNULL() {
		return mCursor == null ? true : false;
	}
}