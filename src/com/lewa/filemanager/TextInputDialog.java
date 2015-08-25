package com.lewa.filemanager;

import com.lewa.filemanager.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TextInputDialog extends AlertDialog {
    /* Data */
    private String mInputText;
    private String mTitle;
    private String mMsg;

    /* Object */
    private OnFinishListener mListener;
    private Context mContext;

    /* View */
    private View mView;
    private TextView mMessage;
    private EditText mFolderName;

    public interface OnFinishListener {
        // return true to accept and dismiss, false reject
        boolean onFinish(String text);
    }

    public TextInputDialog(Context context, String title, String msg,
            String text, OnFinishListener listener) {
        super(context,
                com.lewa.internal.R.style.V5_Theme_Holo_Light_Dialog_Alert);
        mTitle = title;
        mMsg = msg;
        mListener = listener;
        mInputText = text;
        mContext = context;
    }

    public String getInputText() {
        return mInputText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.textinput_dialog, null);

        setTitle(mTitle);

        mFolderName = (EditText) mView.findViewById(R.id.text);
        mFolderName.setText(mInputText);
        mFolderName
                .setFilters(new InputFilter[] { new InputFilter.LengthFilter(
                        GlobalConsts.MAX_FILE_NAME_LEN) });
        mMessage = (TextView) mView.findViewById(R.id.dialog_message);
        mMessage.setText(mMsg);

        mFolderName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                // TODO Auto-generated method stub
                if (mFolderName.length() >= GlobalConsts.MAX_FILE_NAME_LEN) {
                    Toast.makeText(
                            mContext,
                            mContext.getResources().getString(
                                    R.string.name_too_long), Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setView(mView);
        setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE) {
                            mInputText = mFolderName.getText().toString();
                            if (mListener.onFinish(mInputText)) {
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
