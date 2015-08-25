package com.lewa.filemanager;

public abstract class GlobalConsts {
    public static final String KEY_BASE_SD = "key_base_sd";

    public static final String KEY_SHOW_CATEGORY = "key_show_category";

    public static final String INTENT_EXTRA_TAB = "TAB";

    public static final String ROOT_PATH = "/";
    public static Float image_filter_size = 0F;
    public static int max_pic_char_num = 8;//限制图片筛选输入的最大字数， 32GB = 33554432 KB，输入不能超过8位，
//    public static final String SDCARD_PATH = ROOT_PATH + "sdcard2";

    // Menu id
    public static final int MENU_NEW_FOLDER = 100;
    public static final int MENU_FAVORITE = 101;
    public static final int MENU_COPY = 104;
    public static final int MENU_PASTE = 105;
    public static final int MENU_MOVE = 106;
    public static final int MENU_SHOWHIDE = 117;
    public static final int MENU_COPY_PATH = 118;

    public static final int MENU_SORT = 119;
    public static final int MENU_REFRESH = 120;
    public static final int MENU_PICTURE_FILTER = 121;
//    public static final int MENU_CANCEL = 121;
//    
    
    public static final int OPERATION_UP_LEVEL = 3;
    
    public static final int MAX_SDCARD_NUM = 3;
    
    public static final int DIALOG_SORT = 1;
    public static final int DIALOG_CLEAN_DIR = 6;
    
    public static final int MENU_SELECT_SDCARD = 122;
    public static final int MENU_SHOW_INTERNAL_SDCARD = 123;
    public static final int MENU_SHOW_EXTERNAL_SDCARD = 124;
    public static final int MENU_SHOW_ALL_SDCARD = 125;
    
    public static final int MAX_FILE_NAME_LEN = 255;

    //public static final boolean isDoubleSDCard = (Util.getSdDirectories().length >= 2? true:false);
}
