package de.geeksfactory.opacclient.ui.vectordrawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.AppCompatImageHelper;
import android.util.AttributeSet;

@SuppressLint("RestrictedApi")
public class VectorDrawableFloatingActionButton extends FloatingActionButton {

    private AppCompatImageHelper mImageHelper;

    public VectorDrawableFloatingActionButton(Context context) {
        this(context, null);
    }

    public VectorDrawableFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VectorDrawableFloatingActionButton(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mImageHelper = new AppCompatImageHelper(this);
        if (!isInEditMode()) {
            mImageHelper.loadFromAttributes(attrs, defStyleAttr);
        }
    }

}
