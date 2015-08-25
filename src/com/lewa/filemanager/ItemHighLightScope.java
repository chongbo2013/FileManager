package com.lewa.filemanager;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.SelectionBoundsAdjuster;

public class ItemHighLightScope extends RelativeLayout implements SelectionBoundsAdjuster {

    Context mContext;
    TextView sortIndex;
    private static int PADDING_TOP = 0;

    public ItemHighLightScope(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        try {
            super.onFinishInflate();
            sortIndex = (TextView) findViewById(R.id.date_header_text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect arg0) {
        // TODO Auto-generated method stub
        if (sortIndex.getVisibility() == View.VISIBLE) {
            arg0.top += (sortIndex.getHeight() + PADDING_TOP);
        }
    }

}