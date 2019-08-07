package de.geeksfactory.opacclient.ui.vectordrawable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.AppCompatTextView;

@SuppressLint("RestrictedApi")
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
