package agency.tango.materialintroscreen.widgets;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;

public abstract class OverscrollContainer<T extends View> extends RelativeLayout {
    private T mOverscrollView = null;
    private boolean mIsBeingDragged = false;
    private float mMotionBeginX = 0;
    private int mTouchSlop;

    abstract protected boolean canOverscrollAtEnd();

    abstract protected T createOverscrollView();

    public OverscrollContainer(Context context) {
        this(context, null);
    }

    public OverscrollContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverscrollContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mOverscrollView = createOverscrollView();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        addView(mOverscrollView, layoutParams);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public T getOverscrollView() {
        return mOverscrollView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            mMotionBeginX = ev.getX();
            mIsBeingDragged = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!mIsBeingDragged) {
                float scrollDirectionDiff = ev.getX() - mMotionBeginX;
                float absScrollDirectionDiff = Math.abs(scrollDirectionDiff);

                if (absScrollDirectionDiff > mTouchSlop) {
                    if (canOverscrollAtEnd() && scrollDirectionDiff < 0f) {
                        mIsBeingDragged = true;
                    }
                }
            }
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        float moveOffset = event.getX() - mMotionBeginX;

        if (action == MotionEvent.ACTION_MOVE) {
            moveOverscrollView(moveOffset);

        } else if (action == MotionEvent.ACTION_UP) {
            resetOverscrollViewWithAnimation(moveOffset);
            mIsBeingDragged = false;
        }

        return true;
    }

    private void moveOverscrollView(float currentX) {
        scrollTo(-(int) currentX, 0);
    }

    private void resetOverscrollViewWithAnimation(float currentX) {
        Interpolator scrollAnimationInterpolator = new DecelerateInterpolator();
        SmoothScrollRunnable smoothScrollRunnable = new SmoothScrollRunnable((int) currentX, 0, 300, scrollAnimationInterpolator);
        post(smoothScrollRunnable);
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToPosition;
        private final int mScrollFromPosition;
        private final long mDuration;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentPosition = -1;

        SmoothScrollRunnable(int fromPosition, int toPosition, long duration, Interpolator scrollAnimationInterpolator) {
            mScrollFromPosition = fromPosition;
            mScrollToPosition = toPosition;
            mInterpolator = scrollAnimationInterpolator;
            mDuration = duration;
        }

        @Override
        public void run() {
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromPosition - mScrollToPosition)
                        * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentPosition = mScrollFromPosition - deltaY;

                moveOverscrollView(mCurrentPosition);
            }

            if (mContinueRunning && mScrollToPosition != mCurrentPosition) {
                ViewCompat.postOnAnimation(OverscrollContainer.this, this);
            }
        }
    }
}