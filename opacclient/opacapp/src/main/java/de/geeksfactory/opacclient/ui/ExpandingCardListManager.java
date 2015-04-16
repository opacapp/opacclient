package de.geeksfactory.opacclient.ui;

import android.content.Context;
import android.support.v7.internal.widget.TintManager;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.R;

/**
 * Mananges a list of cards that can be expanded and collapsed
 */
public abstract class ExpandingCardListManager {
    private static final int ANIMATION_DURATION = 500;
    private Context context;
    private LinearLayout layout;
    private int expandedPosition = -1;
    private CardView mainCard;
    private LinearLayout llMain;
    private CardView upperCard;
    private LinearLayout llUpper;
    private CardView lowerCard;
    private LinearLayout llLower;
    private CardView expandedCard;
    private TintManager tint;
    private List<View> views = new ArrayList<>();

    public ExpandingCardListManager (Context context, LinearLayout layout) {
        this.context = context;
        this.layout = layout;
        tint = new TintManager(context);
        initViews();
        addViews();
    }

    private void initViews() {
        layout.removeAllViews();

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int sideMargin = context.getResources().getDimensionPixelSize(
                R.dimen.card_side_margin_default);
        int topbottomMargin = context.getResources().getDimensionPixelSize(R.dimen.card_topbottom_margin_default);
        cardParams.setMargins(sideMargin, topbottomMargin, sideMargin, topbottomMargin);

        LinearLayout.LayoutParams expandedCardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int sideMargin2 = context.getResources().getDimensionPixelSize(R.dimen.card_side_margin_selected);
        int topbottomMargin2 = context.getResources().getDimensionPixelSize(R.dimen.card_topbottom_margin_selected);
        expandedCardParams.setMargins(sideMargin2, topbottomMargin2, sideMargin2, topbottomMargin2);

        CardView.LayoutParams listParams = new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);

        // Upper card
        upperCard = new CardView(context);
        upperCard.setVisibility(View.GONE);
        upperCard.setLayoutParams(cardParams);

        llUpper = new LinearLayout(context);
        llUpper.setLayoutParams(listParams);
        llUpper.setOrientation(LinearLayout.VERTICAL);

        upperCard.addView(llUpper);
        layout.addView(upperCard);

        // Expanded card
        expandedCard = new CardView(context);
        expandedCard.setVisibility(View.GONE);
        expandedCard.setLayoutParams(expandedCardParams);
        layout.addView(expandedCard);

        // Lower card
        lowerCard = new CardView(context);
        lowerCard.setVisibility(View.GONE);
        lowerCard.setLayoutParams(cardParams);

        llLower = new LinearLayout(context);
        llLower.setLayoutParams(listParams);
        llLower.setOrientation(LinearLayout.VERTICAL);

        lowerCard.addView(llLower);
        layout.addView(lowerCard);

        // Main card
        mainCard = new CardView(context);
        mainCard.setLayoutParams(cardParams);

        llMain = new LinearLayout(context);
        llMain.setLayoutParams(listParams);
        llMain.setOrientation(LinearLayout.VERTICAL);

        mainCard.addView(llMain);
        layout.addView(mainCard);
    }

    private void addViews() {
        for (int i = 0; i < getCount(); i++) {
            View view = getView(i, llMain);
            views.add(view);
            llMain.addView(view);
            if (i < getCount() - 1) llMain.addView(createDivider());
        }
    }

    private View createDivider() {
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) context.getResources().getDisplayMetrics().density));
        divider.setBackground(tint.getDrawable(R.drawable.abc_list_divider_mtrl_alpha));
        return divider;
    }

    public void expand(int position) {
        if (isExpanded()) {
            if (expandedPosition == position) return;
            else collapse();
        }

        for (int i = 0; i < position; i++) {
            llUpper.addView(getView(i, llUpper));
            if (i < position - 1) llUpper.addView(createDivider());
        }
        final View expandedView = getView(position, expandedCard);
        expandView(position, expandedView);
        expandedCard.addView(expandedView);
        for (int i = position + 1; i < getCount(); i++) {
            llLower.addView(getView(i, llLower));
            if (i < getCount() - 1) llLower.addView(createDivider());
        }

        final float lowerPos;
        if (position + 1 < getCount()) lowerPos = ViewHelper.getY(views.get(position + 1));
        else lowerPos = -1;

        final float mainPos = ViewHelper.getY(views.get(position));
        final int unexpandedHeight = views.get(position).getHeight();

        upperCard.setVisibility(View.VISIBLE);
        lowerCard.setVisibility(View.VISIBLE);
        expandedCard.setVisibility(View.VISIBLE);
        mainCard.setVisibility(View.GONE);

        layout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                layout.getViewTreeObserver().removeOnPreDrawListener(this);
                if (lowerPos > 0) ViewHelper.setY(lowerCard, lowerPos - lowerCard.getPaddingTop());
                ViewHelper.setY(expandedCard, mainPos - expandedCard.getPaddingTop());

                AnimatorSet set = new AnimatorSet();
                int defaultMargin = context.getResources().getDimensionPixelSize(
                        R.dimen.card_side_margin_default);
                int expandedMargin = context.getResources().getDimensionPixelSize(
                        R.dimen.card_side_margin_selected);
                int marginDifference = expandedMargin - defaultMargin;
                set.playTogether(
                        ObjectAnimator
                                .ofFloat(lowerCard, "translationY", lowerCard.getTranslationY(), 0),
                        ObjectAnimator.ofFloat(expandedCard, "translationY",
                                expandedCard.getTranslationY(), 0),
                        ObjectAnimator.ofFloat(expandedCard, "cardElevation",
                                context.getResources().getDimension(R.dimen.card_elevation_default),
                                context.getResources()
                                       .getDimension(R.dimen.card_elevation_selected)),
                        ObjectAnimator.ofInt(expandedCard, "bottom",
                                expandedCard.getBottom() + unexpandedHeight -
                                        expandedView.getHeight(), expandedCard.getBottom()),
                        ObjectAnimator.ofInt(expandedCard, "left",
                                expandedCard.getLeft() - marginDifference, expandedCard.getLeft()),
                        ObjectAnimator.ofInt(expandedCard, "right",
                                expandedCard.getRight() + marginDifference, expandedCard.getRight())
                );
                set.setDuration(ANIMATION_DURATION).start();
                return false;
            }
        });

        expandedPosition = position;
    }

    public void collapse() {
        mainCard.setVisibility(View.VISIBLE);
        upperCard.setVisibility(View.GONE);
        lowerCard.setVisibility(View.GONE);
        expandedCard.setVisibility(View.GONE);
        llUpper.removeAllViews();
        llLower.removeAllViews();
        expandedCard.removeAllViews();
        expandedPosition = -1;
    }

    public void notifyDataSetChanged() {
        llMain.removeAllViews();
        llUpper.removeAllViews();
        llLower.removeAllViews();
        expandedCard.removeAllViews();
        addViews();
    }

    public boolean isExpanded() {
        return expandedPosition != -1;
    }

    public abstract View getView(int position, ViewGroup container);
    public abstract void expandView(int position, View view);
    public abstract void collapseView(int position, View view);
    public abstract int getCount();
}
