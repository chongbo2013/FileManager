package com.lewa.filemanager;

import java.io.File;
import java.util.ArrayList;

import com.lewa.filemanager.R;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class IntentBuilder {

    public static void viewFile(final Context context, final String filePath) {
        String type = getMimeType(filePath);
       // Log.d("yx", "IntentBuilder -> viewFile mimetype: "+ type);
        if(TextUtils.equals(type, "application/lewa-theme")){
        	Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.lewa.themechooser",    
                    "com.lewa.themechooser.receiver.ThemeInstallService"));
            intent.putExtra("THEME_PACKAGE", filePath);
            //if(filePath.endsWith(".lwt")){
            intent.putExtra("isFromFileManager", true);
            context.startService((intent));
            //}
        }
        else if (!TextUtils.isEmpty(type) && !TextUtils.equals(type, "*/*")) {
            /* 设置intent的file与MimeType */
        	//Log.d("yx", "IntentBuilder -> viewFile mimetype 2: ");
            Intent intent = new Intent();
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filePath)), type);
            //context.startActivity(intent);

            // added by weihong, #65389
            if (type.startsWith("image/")) {
                int sortType = (((FileManagerMainActivity) context)).getSortMethod().ordinal();
                intent.putExtra("file_sort_type", sortType);
            }
            try 
            {
            	//[PR939229] Music can be played in file manager when calling
            	TelephonyManager telephonyManager=(TelephonyManager)context. getSystemService(Context.TELEPHONY_SERVICE);
            	if (telephonyManager.getCallState()!=TelephonyManager.CALL_STATE_IDLE && (type.contains("audio/") || type.contains("video/"))) {
            		Toast.makeText(context, R.string.open_file_ring, Toast.LENGTH_SHORT).show();
            	}else {
            		context.startActivity(intent);
				}
				//[PR939229] end
            } 
            catch (ActivityNotFoundException a) 
            {
            	//Log.d("yx", "找不到关联打开程序");
                Toast.makeText(context, R.string.find_no_associated_app, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            // unknown MimeType
        	//Log.d("yx", "IntentBuilder -> viewFile mimetype: known");
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(R.string.dialog_select_type);

            CharSequence[] menuItemArray = new CharSequence[] {
                    context.getString(R.string.dialog_type_text),
                    context.getString(R.string.dialog_type_audio),
                    context.getString(R.string.dialog_type_video),
                    context.getString(R.string.dialog_type_image) };
            dialogBuilder.setItems(menuItemArray,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectType = "*/*";
                            switch (which) {
                            case 0:
                                selectType = "text/plain";
                                break;
                            case 1:
                                selectType = "audio/*";
                                break;
                            case 2:
                                selectType = "video/*";
                                break;
                            case 3:
                                selectType = "image/*";
                                break;
                            }
                            Intent intent = new Intent();
                            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(filePath)), selectType);
                            try
                            {
                                 context.startActivity(intent);
                            }
                            catch(ActivityNotFoundException ex)
                            {
                            	 Toast.makeText(context, R.string.find_no_associated_app, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            dialogBuilder.show();
        }
    }

    public static Intent buildSendFile(ArrayList<FileInfo> files) {
        ArrayList<Uri> uris = new ArrayList<Uri>();

        String mimeType = "*/*";
        for (FileInfo file : files) {
            if (file.IsDir)
                continue;

            File fileIn = new File(file.filePath);
            mimeType = getMimeType(file.fileName);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }

        if (uris.size() == 0)
            return null;

        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                : android.content.Intent.ACTION_SEND);

        if (multiple) {
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }

        return intent;
    }

    private static String getMimeType(String filePath) {
        int dotPosition = filePath.lastIndexOf('.');
        if (dotPosition == -1)
            return "*/*";

        String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
        String mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
        if (ext.equals("lwt")) {
            mimeType = "application/lewa-theme";
        }

        return mimeType != null ? mimeType : "*/*";
    }
}
