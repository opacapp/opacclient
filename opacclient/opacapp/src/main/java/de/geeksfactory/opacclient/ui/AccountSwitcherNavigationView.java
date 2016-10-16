/**
 * Copyright (C) 2016 by Johan von Forstner under the MIT license
 * <p/>
 * Inspired by GitLabNavigationView.java, Copyright 2016 Commit 451, licensed under under the Apache
 * License, Version 2.0 Source: https://gitlab
 * .com/Commit451/LabCoat/blob/master/app/src/main/java/com/commit451/gitlab/view
 * /GitLabNavigationView.java
        *<p/>
        *Permission is hereby granted,free of charge,to any person obtaining a copy of this
 * software and
        *associated documentation files(the"Software"),to deal in the Software without restriction,
        *including without limitation the rights to use,copy,modify,merge,publish,distribute,
        *sublicense,and/or sell copies of the Software,and to permit persons to whom the Software is
        *furnished to do so,subject to the following conditions:
        *<p/>
        *The above copyright notice and this permission notice shall be included in all copies or
        *substantial portions of the Software.
        *<p/>
        *THE SOFTWARE IS PROVIDED"AS IS",WITHOUT WARRANTY OF ANY KIND,EXPRESS OR IMPLIED,
 * INCLUDING BUT
        *NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,FITNESS FOR A PARTICULAR PURPOSE AND
        *NONINFRINGEMENT.IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
        *DAMAGES OR OTHER LIABILITY,WHETHER IN AN ACTION OF CONTRACT,TORT OR OTHERWISE,ARISING FROM,
        *OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
        */

        package de.geeksfactory.opacclient.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import de.geeksfactory.opacclient.R;

public class AccountSwitcherNavigationView extends NavigationView {
    private RecyclerView accountsList;
    private RecyclerView.Adapter accountsAdapter;
    private boolean accountsVisible;

    public AccountSwitcherNavigationView(Context context) {
        super(context);
        init();
    }

    public AccountSwitcherNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccountSwitcherNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        accountsList = new RecyclerView(getContext());
        accountsList.setLayoutManager(new LinearLayoutManager(getContext()));
        addView(accountsList);
        LayoutParams params = (FrameLayout.LayoutParams) accountsList.getLayoutParams();
        params.setMargins(0,
                getResources().getDimensionPixelSize(R.dimen.navigation_drawer_header_height), 0,
                0);
        params.gravity = Gravity.BOTTOM; // https://code.google.com/p/android/issues/detail?id=28057
        TypedArray a = getContext().obtainStyledAttributes(
                new int[]{android.R.attr.windowBackground});
        accountsList.setBackgroundResource(a.getResourceId(0, 0));
        a.recycle();
        accountsList.setPadding(0,
                getResources().getDimensionPixelSize(R.dimen.list_top_padding), 0, 0);
        accountsList.setClipToPadding(false);
        accountsList.setVisibility(View.GONE);
    }

    public void setAccountsAdapter(RecyclerView.Adapter adapter) {
        this.accountsAdapter = adapter;
        accountsList.setAdapter(accountsAdapter);
    }

    @TargetApi(12)
    public void setAccountsVisible(boolean visible) {
        if (visible == accountsVisible) return;

        accountsVisible = visible;

        // We don't use Animations on API < 12 because they don't seem to work correctly
        // using NineOldAndroids
        if (accountsVisible) {
            accountsList.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 12) {
                accountsList.setAlpha(0);
                accountsList.animate().alpha(1.0f).setListener(null);
                // setListener(null) is needed for removing the listener added below
            }
        } else {
            if (Build.VERSION.SDK_INT >= 12) {
                accountsList.animate().alpha(0.0f).setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (accountsList != null) {
                                    accountsList.clearAnimation();
                                    accountsList.setVisibility(View.GONE);
                                }
                            }
                        });
            } else {
                accountsList.setVisibility(View.GONE);
            }
        }
    }
}
