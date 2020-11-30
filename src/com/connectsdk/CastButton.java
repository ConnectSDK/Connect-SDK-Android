package com.connectsdk;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class CastButton extends LinearLayout {
    private ImageView mImage;

    public CastButton(Context context) {
        super(context);
    }

    public CastButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.cast_button, this);
        mImage = (ImageView) getChildAt(0);
    }

}
