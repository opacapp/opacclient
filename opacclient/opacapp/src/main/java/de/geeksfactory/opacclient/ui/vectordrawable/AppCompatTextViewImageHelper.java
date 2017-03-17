package de.geeksfactory.opacclient.ui.vectordrawable;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import de.geeksfactory.opacclient.R;

/*
 *   Based on AOSP code for AppCompatImageHelper
 */

@SuppressLint("RestrictedApi")
public class AppCompatTextViewImageHelper {
    private final TextView mView;
    private final AppCompatDrawableManager mDrawableManager;

    public AppCompatTextViewImageHelper(TextView view, AppCompatDrawableManager drawableManager) {
        mView = view;
        mDrawableManager = drawableManager;
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(mView.getContext(), attrs,
                R.styleable.VectorDrawableTextView, defStyleAttr, 0);
        try {
            Drawable d =
                    a.getDrawableIfKnown(R.styleable.VectorDrawableTextView_android_drawableLeft);
            if (d != null) {
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                mView.setCompoundDrawables(d, null, null, null);
            }

            final int id =
                    a.getResourceId(R.styleable.VectorDrawableTextView_drawableLeftCompat, -1);
            if (id != -1) {
                d = mDrawableManager.getDrawable(mView.getContext(), id);
                if (d != null) {
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    mView.setCompoundDrawables(d, null, null, null);
                }
            }

            final Drawable drawable = mView.getCompoundDrawables()[0];
            if (drawable != null) {
                DrawableUtils.fixDrawable(drawable);
            }
        } finally {
            a.recycle();
        }
    }
}
