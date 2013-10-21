// Copyright (C) 2012 LMIT Limited
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.markupartist.android.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.OnLoadMoreDataListener;
import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.logger.Logger;

public class PullToRefreshListView extends ListView implements
    OnScrollListener, OnIsBottomOverScrollListener {

  private static final int TAP_TO_REFRESH = 1;
  private static final int PULL_TO_REFRESH = 2;
  private static final int RELEASE_TO_REFRESH = 3;
  private static final int REFRESHING = 4;

  private static final String TAG = "PullToRefreshListView";

  private OnRefreshListener mOnRefreshListener;

  private OnLoadMoreDataListener mOnLoadMoreDataListener;
  private EndlessScrollListener mEndlessScrollListener;
  private boolean isBottomOverscroll = false;
  private boolean canDoLoadMoreData = false;
  private boolean refreshing = false;

  /**
   * Listener that will receive notifications every time the list scrolls.
   */
  private OnScrollListener mOnScrollListener;
  private LayoutInflater mInflater;

  private LinearLayout mRefreshView;
  private TextView mRefreshViewText;
  private ImageView mRefreshViewImage;
  private ProgressBar mRefreshViewProgress;
  private TextView mRefreshViewLastUpdated;

  private int mCurrentScrollState;
  private int mRefreshState;

  private RotateAnimation mFlipAnimation;
  private RotateAnimation mReverseFlipAnimation;

  private int mRefreshViewHeight;
  private int mRefreshOriginalTopPadding;
  private int mLastMotionY;
  private View lastFooterPad;

  private static final int MAX_Y_OVERSCROLL_DISTANCE = 200;
  private int mMaxYOverscrollDistance;

  public PullToRefreshListView(Context context) {
    super(context);
    init(context);
  }

  public PullToRefreshListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  @Override
  public boolean performItemClick(View view, int position, long id) {

    return super.performItemClick(view, position, id);
  }

  private void init(Context context) {
    // Load all of the animations we need in code rather than through XML
    mFlipAnimation =
        new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
    mFlipAnimation.setInterpolator(new LinearInterpolator());
    mFlipAnimation.setDuration(250);
    mFlipAnimation.setFillAfter(true);
    mReverseFlipAnimation =
        new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
    mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
    mReverseFlipAnimation.setDuration(250);
    mReverseFlipAnimation.setFillAfter(true);

    mInflater =
        (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    mRefreshView =
        (LinearLayout) mInflater.inflate(R.layout.pull_to_refresh_header, null);

    mRefreshViewText =
        (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
    mRefreshViewImage =
        (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
    mRefreshViewProgress =
        (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
    mRefreshViewLastUpdated =
        (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_updated_at);

    mRefreshViewImage.setMinimumHeight(50);
    mRefreshView.setOnClickListener(new OnClickRefreshListener());
    mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();

    mRefreshState = TAP_TO_REFRESH;

    addHeaderView(mRefreshView);

    super.setOnScrollListener(this);

    measureView(mRefreshView);
    mRefreshViewHeight = mRefreshView.getMeasuredHeight();

    final DisplayMetrics metrics = getResources().getDisplayMetrics();
    final float density = metrics.density;

    mMaxYOverscrollDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
  }

  public void forcePullToRefreshViewHidden() {

    invalidateViews();
    setSelectionFromTop(1, 0);
  }

  public void forcePullToRefreshViewHidden(int size) {

    addFooterView(size);
    forcePullToRefreshViewHidden();
  }

  @Override
  public void setAdapter(ListAdapter adapter) {
    super.setAdapter(adapter);
    setSelectionFromTop(1, 0);
  }

  /**
   * Set the listener that will receive notifications every time the list
   * scrolls.
   * 
   * @param l The scroll listener.
   */
  @Override
  public void setOnScrollListener(AbsListView.OnScrollListener l) {
    mOnScrollListener = l;
  }

  /**
   * Register a callback to be invoked when this list should be refreshed.
   * 
   * @param onRefreshListener The callback to run.
   */
  public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
    mOnRefreshListener = onRefreshListener;
  }

  /**
   * Set a text to represent when the list was last updated.
   * 
   * @param lastUpdated Last updated at.
   */
  public void setLastUpdated(CharSequence lastUpdated) {
    if (lastUpdated != null) {
      mRefreshViewLastUpdated.setVisibility(View.VISIBLE);
      mRefreshViewLastUpdated.setText(lastUpdated);
    } else {
      mRefreshViewLastUpdated.setVisibility(View.GONE);
    }
  }

  /**
   * Smoothly scroll by distance pixels over duration milliseconds.
   * 
   * <p>
   * Using reflection internally to call smoothScrollBy for API Level 8
   * otherwise scrollBy is called.
   * 
   * @param distance Distance to scroll in pixels.
   * @param duration Duration of the scroll animation in milliseconds.
   */
  private void scrollListBy(int distance, int duration) {
    try {
      Method method =
          ListView.class
              .getMethod("smoothScrollBy", Integer.TYPE, Integer.TYPE);
      method.invoke(this, distance + 1, duration);
    } catch (NoSuchMethodException e) {
      // If smoothScrollBy is not available (< 2.2)
      setSelectionFromTop(1, 0);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      System.err.println("unexpected " + e);
    } catch (InvocationTargetException e) {
      System.err.println("unexpected " + e);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final int y = (int) event.getY();

    switch (event.getAction()) {
      case MotionEvent.ACTION_UP:
        if (!refreshing) {
          if (canDoLoadMoreData) {
            if (mOnLoadMoreDataListener != null) {
              canDoLoadMoreData = false;
              refreshing = true;
              Logger.getInstance().debug("Calling laod more data");
              mOnLoadMoreDataListener.onLoadMore(3000);
            }
          }
        }

        if (!isVerticalScrollBarEnabled()) {
          setVerticalScrollBarEnabled(true);
        }

        if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
          if (mRefreshView.getBottom() > mRefreshViewHeight
              || mRefreshView.getTop() >= 0
              && mRefreshState == RELEASE_TO_REFRESH) {
            // Initiate the refresh
            mRefreshState = REFRESHING;
            prepareForRefresh();
            onRefresh();
          } else if (mRefreshView.getBottom() < mRefreshViewHeight) {
            // Abort refresh and scroll down below the refresh view
            resetHeader();
            setSelectionFromTop(1, 0);
          }
        }

        break;
      case MotionEvent.ACTION_DOWN:
        mLastMotionY = y;
        break;
      case MotionEvent.ACTION_MOVE:
        applyHeaderPadding(event);
        break;
    }
    return super.onTouchEvent(event);
  }

  private void applyHeaderPadding(MotionEvent ev) {
    final int historySize = ev.getHistorySize();

    // Workaround for getPointerCount() which is unavailable in 1.5
    // (it's always 1 in 1.5)
    int pointerCount = 1;
    try {
      Method method = MotionEvent.class.getMethod("getPointerCount");
      pointerCount = (Integer) method.invoke(ev);
    } catch (NoSuchMethodException e) {
      pointerCount = 1;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (IllegalAccessException e) {
      System.err.println("unexpected " + e);
    } catch (InvocationTargetException e) {
      System.err.println("unexpected " + e);
    }

    for (int h = 0; h < historySize; h++) {
      for (int p = 0; p < pointerCount; p++) {
        if (mRefreshState == RELEASE_TO_REFRESH) {
          if (isVerticalFadingEdgeEnabled()) {
            setVerticalScrollBarEnabled(false);
          }

          int historicalY = 0;
          try {
            // For Android > 2.0
            Method method =
                MotionEvent.class.getMethod("getHistoricalY", Integer.TYPE,
                    Integer.TYPE);
            historicalY = ((Float) method.invoke(ev, p, h)).intValue();
          } catch (NoSuchMethodException e) {
            // For Android < 2.0
            historicalY = (int) (ev.getHistoricalY(h));
          } catch (IllegalArgumentException e) {
            throw e;
          } catch (IllegalAccessException e) {
            System.err.println("unexpected " + e);
          } catch (InvocationTargetException e) {
            System.err.println("unexpected " + e);
          }

          // Calculate the padding to apply, we divide by 1.7 to
          // simulate a more resistant effect during pull.
          int topPadding =
              (int) (((historicalY - mLastMotionY) - mRefreshViewHeight) / 1.7);

          mRefreshView.setPadding(mRefreshView.getPaddingLeft(), topPadding,
              mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
        }
      }
    }
  }

  /**
   * Sets the header padding back to original size.
   */
  private void resetHeaderPadding() {
    mRefreshView.setPadding(mRefreshView.getPaddingLeft(),
        mRefreshOriginalTopPadding, mRefreshView.getPaddingRight(),
        mRefreshView.getPaddingBottom());
  }

  /**
   * Resets the header to the original state.
   */
  private void resetHeader() {
    if (mRefreshState != TAP_TO_REFRESH) {
      mRefreshState = TAP_TO_REFRESH;

      resetHeaderPadding();

      // Set refresh view text to the pull label
      mRefreshViewText.setText(R.string.pull_to_refresh_tap_label);
      // Replace refresh drawable with arrow drawable
      mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
      // Clear the full rotation animation
      mRefreshViewImage.clearAnimation();
      // Hide progress bar and arrow.
      mRefreshViewImage.setVisibility(View.GONE);
      mRefreshViewProgress.setVisibility(View.GONE);
    }
  }

  private void measureView(View child) {
    ViewGroup.LayoutParams p = child.getLayoutParams();
    if (p == null) {
      p =
          new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
    int lpHeight = p.height;
    int childHeightSpec;
    if (lpHeight > 0) {
      childHeightSpec =
          MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
    } else {
      childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    }
    child.measure(childWidthSpec, childHeightSpec);
  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem,
      int visibleItemCount, int totalItemCount) {

    // When the refresh view is completely visible, change the text to say
    // "Release to refresh..." and flip the arrow drawable.
    if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
        && mRefreshState != REFRESHING) {
      if (firstVisibleItem == 0) {
        mRefreshViewImage.setVisibility(View.VISIBLE);
        if ((mRefreshView.getBottom() > mRefreshViewHeight + 20 || mRefreshView
            .getTop() >= 0) && mRefreshState != RELEASE_TO_REFRESH) {
          mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
          mRefreshViewImage.clearAnimation();
          mRefreshViewImage.startAnimation(mFlipAnimation);
          mRefreshState = RELEASE_TO_REFRESH;
        } else if (mRefreshView.getBottom() < mRefreshViewHeight + 20
            && mRefreshState != PULL_TO_REFRESH) {
          mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
          if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshViewImage.clearAnimation();
            mRefreshViewImage.startAnimation(mReverseFlipAnimation);
          }
          mRefreshState = PULL_TO_REFRESH;
        }
      } else {
        mRefreshViewImage.setVisibility(View.GONE);
        resetHeader();
      }
    } else if (mCurrentScrollState == SCROLL_STATE_FLING
        && firstVisibleItem == 0 && mRefreshState != REFRESHING) {
      setSelectionFromTop(1, 0);
    }

    if (mOnScrollListener != null) {
      mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
          totalItemCount);
    }


    if (mEndlessScrollListener != null) {
      mEndlessScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
          totalItemCount);
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    mCurrentScrollState = scrollState;

    if (mOnScrollListener != null) {
      mOnScrollListener.onScrollStateChanged(view, scrollState);
    }

    if (mEndlessScrollListener != null) {
      mEndlessScrollListener.onScrollStateChanged(view, scrollState);
    }
  }

  public void prepareForRefresh() {
    resetHeaderPadding();

    mRefreshViewImage.setVisibility(View.GONE);
    // We need this hack, otherwise it will keep the previous drawable.
    mRefreshViewImage.setImageDrawable(null);
    mRefreshViewProgress.setVisibility(View.VISIBLE);

    // Set refresh view text to the refreshing label
    mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);

    mRefreshState = REFRESHING;
  }

  public void onRefresh() {
    Log.d(TAG, "onRefresh");
    if (!refreshing) {
      if (mOnRefreshListener != null) {
        refreshing = true;
        mOnRefreshListener.onRefresh();
      }
    }
  }

  /**
   * Resets the list to a normal state after a refresh.
   * 
   * @param lastUpdated Last updated at.
   */
  public void onRefreshComplete(CharSequence lastUpdated) {
    setLastUpdated(lastUpdated);
    onRefreshComplete();
  }

  /**
   * Resets the list to a normal state after a refresh.
   */
  public void onRefreshComplete() {
    Log.d(TAG, "onRefreshComplete");

    refreshing = false;

    resetHeader();

    // If refresh view is visible when loading completes, scroll down to
    // the next item.
    if (mRefreshView.getBottom() > 0) {
      invalidateViews();
      setSelectionFromTop(1, 0);
    }
  }

  /**
   * Invoked when the refresh view is clicked on. This is mainly used when
   * there's only a few items in the list and it's not possible to drag the
   * list.
   */
  private class OnClickRefreshListener implements OnClickListener {

    @Override
    public void onClick(View v) {
      if (mRefreshState != REFRESHING) {
        prepareForRefresh();
        onRefresh();
      }
    }

  }

  /**
   * Interface definition for a callback to be invoked when list should be
   * refreshed.
   */
  public interface OnRefreshListener {
    /**
     * Called when the list should be refreshed.
     * <p>
     * A call to {@link PullToRefreshListView #onRefreshComplete()} is expected
     * to indicate that the refresh has completed.
     */
    public void onRefresh();
  }

  @Override
  protected void onSizeChanged(int w, final int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    addFooterView(getCount());
  }

  private void addFooterView(int count) {
    Resources r = getResources();

    Display display =
        ((Activity) getContext()).getWindowManager().getDefaultDisplay();
    int height = display.getHeight();

    float size =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, 60,
            r.getDisplayMetrics());

    int childViewHeight = (int) (count * size);

    if (lastFooterPad != null) {

      this.removeFooterView(lastFooterPad);
    }

    if (childViewHeight < height) {
      View footerPad = new View(getContext());

      lastFooterPad = footerPad;

      footerPad.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
          height - childViewHeight));
      this.setFooterDividersEnabled(false);
      this.addFooterView(footerPad, null, false);
    }
  }

  @Override
  protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
      int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX,
      int maxOverScrollY, boolean isTouchEvent) {
    // Logger.getInstance().debug("Called overScrollBy");
    // Logger.getInstance().debug("scrollY=" + scrollY);
    // Logger.getInstance().debug("deltaY=" + deltaY);
    // Logger.getInstance().debug("scrollRangeY=" + scrollRangeY);
    // Logger.getInstance().debug("maxOverScrollY=" + maxOverScrollY);
    // Logger.getInstance().debug("isTouchEvent=" + isTouchEvent);

    int __mMaxYOverscrollDistance = mMaxYOverscrollDistance;
    canDoLoadMoreData = false;

    if (mOnLoadMoreDataListener != null) {
      if ((deltaY >= 1 || scrollY >= 1) && mOnLoadMoreDataListener != null) {
//        Logger.getInstance().debug(
//            "Enough overscroll detected: checking isBottonOverScroll='"
//                + isBottomOverscroll + "'");
        if (isBottomOverscroll && !refreshing) {
          //Logger.getInstance().debug("Setting canDoLoadMoreData=true");
          canDoLoadMoreData = true;
        }
      } else {
        if (!isBottomOverscroll) {
          //Logger.getInstance().debug("Not bottom overscroll: deny overscroll");
          __mMaxYOverscrollDistance = 0;
        }
      }
    } else {
      if (!isBottomOverscroll) {
        //Logger.getInstance().debug("Not bottom overscroll: deny overscroll");
        __mMaxYOverscrollDistance = 0;
      }
    }

    return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
        scrollRangeY, maxOverScrollX, __mMaxYOverscrollDistance, isTouchEvent);
  }

  @Override
  protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
      boolean clampedY) {

    // Logger.getInstance().debug("Called overScrolled");
    // Logger.getInstance().debug("scrollY=" + scrollY);
    // Logger.getInstance().debug("clampedY=" + clampedY);

    super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
  }

  public void setEndlessScrollListener(
      EndlessScrollListener endlessScrollListener) {
    this.mEndlessScrollListener = endlessScrollListener;
  }

  public void setOnLoadMoreDataListener(
      OnLoadMoreDataListener mOnLoadMoreDataListener) {
    this.mOnLoadMoreDataListener = mOnLoadMoreDataListener;
  }

  public static class EndlessScrollListener implements OnScrollListener {

    private int visibleThreshold;
    private OnIsBottomOverScrollListener listener;

    public EndlessScrollListener(int visibleThreshold,
        OnIsBottomOverScrollListener listener) {
      this.visibleThreshold = visibleThreshold;
      this.listener = listener;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
        int visibleItemCount, int totalItemCount) {

      // Logger.getInstance().debug("totalItemCount=" + totalItemCount);
      // Logger.getInstance().debug("visibleItemCount=" + visibleItemCount);
      // Logger.getInstance().debug("firstVisibleItem=" + firstVisibleItem);
      listener.onIsBottonOverscroll(false);
      if (listener != null) {
        if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
          listener.onIsBottonOverscroll(true);
        } else {
          listener.onIsBottonOverscroll(false);
        }
      }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
      //Logger.getInstance().debug(
        //  "onScrollStateChanged: scrollState=" + scrollState);
    }
  }

  @Override
  public void onIsBottonOverscroll(boolean isBottomOverscroll) {
    //Logger.getInstance().debug("isBottomOverscroll" + isBottomOverscroll);
    this.isBottomOverscroll = isBottomOverscroll;
  }
}
