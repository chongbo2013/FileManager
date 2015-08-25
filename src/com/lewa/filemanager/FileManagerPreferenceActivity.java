package com.lewa.filemanager;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 *
 * @author
 */
public class FileManagerPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private static final String PRIMARY_FOLDER = "pref_key_primary_folder";
    private static final String READ_ROOT = "pref_key_read_root";
    private static final String SHOW_REAL_PATH = "pref_key_show_real_path";
    private static final String PIC_SIZE_FILTER = "key_pic_size_filter";
    private static final String SYSTEM_SEPARATOR = File.separator;

    private EditTextPreference mEditTextPreference;
    private EditTextPreference mPicFilterPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mEditTextPreference = (EditTextPreference) findPreference(PRIMARY_FOLDER);
        mPicFilterPreference = (EditTextPreference) findPreference(PIC_SIZE_FILTER);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Setup the initial values
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        mEditTextPreference.setSummary(this.getString(
                R.string.pref_primary_folder_summary,
                sharedPreferences.getString(PRIMARY_FOLDER, GlobalConsts.ROOT_PATH)));

        mPicFilterPreference.setSummary(this.getString(
                R.string.filtersize_pic_summary,
                sharedPreferences.getString(PIC_SIZE_FILTER, "")));
        // Set up a listener whenever a key changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedpreferences, String key) {
        if (PRIMARY_FOLDER.equals(key)) {
            mEditTextPreference.setSummary(this.getString(
                    R.string.pref_primary_folder_summary,
                    sharedpreferences.getString(PRIMARY_FOLDER, GlobalConsts.ROOT_PATH)));
        }
        if (PIC_SIZE_FILTER.equals(key)) {
        	mPicFilterPreference.setSummary(this.getString(
                    R.string.filtersize_pic_summary,
                    sharedpreferences.getString(PIC_SIZE_FILTER, "")));
        }
    }

    public static String getPrimaryFolder(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String primaryFolder = settings.getString(PRIMARY_FOLDER, context.getString(R.string.default_primary_folder, GlobalConsts.ROOT_PATH));

        if (TextUtils.isEmpty(primaryFolder)) { // setting primary folder = empty("")
            primaryFolder = GlobalConsts.ROOT_PATH;
        }

        // it's remove the end char of the home folder setting when it with the '/' at the end.
        // if has the backslash at end of the home folder, it's has minor bug at "UpLevel" function.
        int length = primaryFolder.length();
        if (length > 1 && SYSTEM_SEPARATOR.equals(primaryFolder.substring(length - 1))) { // length = 1, ROOT_PATH
            return primaryFolder.substring(0, length - 1);
        } else {
            return primaryFolder;
        }
    }

    public static float getPicFilter(Context context) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);//getFloat(key, -1)
    	Float picSize = (Float)settings.getFloat(PIC_SIZE_FILTER, Float.parseFloat(context.getString(R.string.filtersize_pic_preference)));
    			//Integer.parseInt(context.getString(R.string.filtersize_pic_preference)));
    	if(picSize.isInfinite()){
    		picSize = GlobalConsts.image_filter_size;
    	}
    	return picSize.floatValue();
    }

    public static void setPicFilter(Context context, Float size) {
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putFloat(PIC_SIZE_FILTER, size.floatValue());
    	editor.commit();
    }

    public static boolean isReadRoot(Context context, String sdpath) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isReadRootFromSetting = settings.getBoolean(READ_ROOT, false);
        boolean isReadRootWhenSettingPrimaryFolderWithoutSdCardPrefix = !getPrimaryFolder(context).startsWith(sdpath);//Util.getSdDirectory());

        return isReadRootFromSetting || isReadRootWhenSettingPrimaryFolderWithoutSdCardPrefix;
    }
}
