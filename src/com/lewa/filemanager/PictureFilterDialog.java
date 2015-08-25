package com.lewa.filemanager;

import com.lewa.filemanager.R;
//import com.lewa.filemanager.TextInputDialog.OnFinishListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PictureFilterDialog extends AlertDialog {
    private Float mInputText = 0F;
    private String mTitle;
    private String mMsg;
    private OnFinishListener mListener;
    private Context mContext;
    private View mView;
    private EditText mFilterSize;    
    
    public interface OnFinishListener {
        // return true to accept and dismiss, false reject
        boolean onFinish(float text);
    }

    public PictureFilterDialog(Context context, String title, String msg, float text, com.lewa.filemanager.PictureFilterDialog.OnFinishListener onFinishListener) {
        super(context,com.lewa.internal.R.style.V5_Theme_Holo_Light_Dialog_Alert);
        mTitle = title;
        mMsg = msg;
        mListener = onFinishListener;
        mInputText = text;
        mContext = context;
    }

	public float getInputText() {
        return mInputText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.picturefilter_dialog, null);

        setTitle(mTitle);
//        setMessage(mMsg);

        mFilterSize = (EditText) mView.findViewById(R.id.set_size);
//        mFilterSize.addTextChangedListener(new TextWatcher() {
//            private CharSequence temp;
//            private int selectionStart;
//            private int selectionEnd;
//
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                 temp = s;
//            }
//
//            @Override       
//            public void afterTextChanged(Editable s) {
//                selectionStart = mFilterSize.getSelectionStart();
//                selectionEnd = mFilterSize.getSelectionEnd();
//                if (temp.length() > GlobalConsts.max_pic_char_num) {
//                    s.delete(selectionStart - 1, selectionEnd);
//                    int tempSelection = selectionEnd;
//                    mFilterSize.setText(s);
//                    mFilterSize.setSelection(tempSelection);//设置光标在最后
////                    Toast.makeText(mContext, mContext.getResources().getString(R.string.pic_exceed_max), Toast.LENGTH_SHORT).show();
//                }
//            }
//   });
        mFilterSize.setText(String.valueOf(mInputText));
        mFilterSize.selectAll();

        setView(mView);
        setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE) {
                        	String sKsize = ((EditText) ((AlertDialog) dialog).findViewById(R.id.set_size)).getText().toString();
//                            mInputText = Float.parseFloat(mFolderName.getText().toString());//Integer.parseInt(mFolderName.getText().toString());
                            Float iKsize = 0F;
                            try {
                                iKsize = Float.parseFloat(sKsize);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (mListener.onFinish(iKsize)) {
                                dismiss();
                            }
                        }
                    }
                });
        setButton(BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener) null);

        super.onCreate(savedInstanceState);
    }
}
