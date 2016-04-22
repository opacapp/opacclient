package de.geeksfactory.opacclient.ui.vectordrawable;

import android.content.Context;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class VectorDrawableTextView extends AppCompatTextView {

    private AppCompatTextViewImageHelper mImageHelper;

    public VectorDrawableTextView(Context context) {
        this(context, null);
    }

    public VectorDrawableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VectorDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();
        mImageHelper = new AppCompatTextViewImageHelper(this, drawableManager);
        if (!isInEditMode()) {
            mImageHelper.loadFromAttributes(attrs, defStyleAttr);
        }
    }

}
