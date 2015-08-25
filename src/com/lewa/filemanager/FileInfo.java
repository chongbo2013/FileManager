package com.lewa.filemanager;

public class FileInfo {

    public String fileName;
    public String fileSDsortName;//yx: chinese name for SD card tab sorting.

    public String filePath;

    public long fileSize;

    public boolean IsDir;

    public int Count;

    public long ModifiedDate;

    public boolean Selected;

    public boolean canRead;

    public boolean canWrite;

    public boolean isHidden;

    public long dbId; // id in the database, if is from database

    public String path;

    public String artist;

    public String sort_key;

}
