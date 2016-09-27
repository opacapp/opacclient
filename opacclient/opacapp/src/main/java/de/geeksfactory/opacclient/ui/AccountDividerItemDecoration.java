package de.geeksfactory.opacclient.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.frontend.adapter.AccountAdapter;

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class AccountDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;

    public AccountDividerItemDecoration(Context context) {
        mDivider = context.getDrawable(R.drawable.list_divider_inset);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if (!isDecorated(child, parent)) continue;

            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin;
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView
            .State state) {
        if (isDecorated(view, parent)) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        }
    }

    private boolean isDecorated(View view, RecyclerView parent) {
        RecyclerView.ViewHolder vh = parent.getChildViewHolder(view);
        View nextView = parent.getChildAt(parent.getChildAdapterPosition(view) + 1);
        if (nextView != null) {
            RecyclerView.ViewHolder vh2 = parent.getChildViewHolder(nextView);
            return vh instanceof AccountAdapter.ViewHolder &&
                    vh2 instanceof AccountAdapter.ViewHolder;
        } else {
            return false;
        }
    }
}