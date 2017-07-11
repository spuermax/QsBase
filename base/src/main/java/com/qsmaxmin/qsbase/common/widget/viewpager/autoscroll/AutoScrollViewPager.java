package com.qsmaxmin.qsbase.common.widget.viewpager.autoscroll;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import java.lang.reflect.Field;

/**
 * @CreateBy qsmaxmin
 * @Date 2017-7-6 12:37:16
 * @Description 自动轮播，支持无限轮播
 */
public final class AutoScrollViewPager extends ViewPager {

    public static final int                    DEFAULT_INTERVAL            = 3000;
    public static final int                    LEFT                        = 0;
    public static final int                    RIGHT                       = 1;
    public static final int                    SLIDE_BORDER_MODE_NONE      = 0;
    public static final int                    SLIDE_BORDER_MODE_CYCLE     = 1;
    public static final int                    SLIDE_BORDER_MODE_TO_PARENT = 2;
    private             long                   interval                    = DEFAULT_INTERVAL;
    private             int                    direction                   = RIGHT;
    private             boolean                isCycle                     = true;
    private             boolean                stopScrollWhenTouch         = true;
    private             int                    slideBorderMode             = SLIDE_BORDER_MODE_NONE;
    private             boolean                isBorderAnimation           = true;
    private             double                 autoScrollFactor            = 1.0;
    private             double                 swipeScrollFactor           = 1.0;
    private             boolean                isAutoScroll                = false;
    private             boolean                isStopByTouch               = false;
    private             float                  downX                       = 0f;
    private             CustomDurationScroller scroller                    = null;
    public static final int                    SCROLL_WHAT                 = 0;
    private             boolean                infinitePagesEnabled        = true;

    private Handler handler;

    @Override public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof InfinitePagerAdapter)) {
            throw new RuntimeException("setAdapter(..) this adapter must be instance of InfinitePagerAdapter");
        }
        InfinitePagerAdapter infinitePagerAdapter = (InfinitePagerAdapter) adapter;
        infinitePagerAdapter.enableInfinite(infinitePagesEnabled);
        super.setAdapter(adapter);
        setCurrentItem(0);
    }


    /**
     * 计算位置
     */
    @Override public void setCurrentItem(int item) {
        if (infinitePagesEnabled) {
            item = getOffsetAmount() + (item % getAdapter().getCount());
        }
        super.setCurrentItem(item);

    }

    public void setEnableInfinite(boolean enable) {
        infinitePagesEnabled = enable;
        if (getAdapter() instanceof InfinitePagerAdapter) {
            InfinitePagerAdapter infAdapter = (InfinitePagerAdapter) getAdapter();
            infAdapter.enableInfinite(infinitePagesEnabled);
        }
    }

    private int getOffsetAmount() {
        if (getAdapter() instanceof InfinitePagerAdapter) {
            InfinitePagerAdapter adapter = (InfinitePagerAdapter) getAdapter();
            return adapter.getRealCount() * 100;
        } else {
            return 0;
        }
    }

    public AutoScrollViewPager(Context paramContext) {
        this(paramContext, null);
    }

    public AutoScrollViewPager(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    private void init() {
        handler = new MyHandler();
        setViewPagerScroller();
    }

    /**
     * 执行滚动
     */
    public void startAutoScroll() {
        startAutoScroll((int) (interval + scroller.getDuration() / autoScrollFactor * swipeScrollFactor));
    }

    /**
     * 执行滚动
     *
     * @param delayTimeInMills 间隔时间
     */
    public void startAutoScroll(int delayTimeInMills) {
        isAutoScroll = true;
        sendScrollMessage(delayTimeInMills);
    }

    /**
     * 停止滚动
     */
    public void stopAutoScroll() {
        isAutoScroll = false;
        handler.removeMessages(SCROLL_WHAT);
    }

    /**
     * 设定因子的滑动动画持续时间会改变
     */
    public void setSwipeScrollDurationFactor(double scrollFactor) {
        swipeScrollFactor = scrollFactor;
    }

    /**
     * 设定因子的滑动动画持续时间会改变 在自动滚动
     */
    public void setAutoScrollDurationFactor(double scrollFactor) {
        autoScrollFactor = scrollFactor;
    }

    private void sendScrollMessage(long delayTimeInMills) {
        handler.removeMessages(SCROLL_WHAT);
        handler.sendEmptyMessageDelayed(SCROLL_WHAT, delayTimeInMills);
    }

    private void setViewPagerScroller() {
        try {
            Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Field interpolatorField = ViewPager.class.getDeclaredField("sInterpolator");
            interpolatorField.setAccessible(true);
            interpolatorField.get(null);
            scroller = new CustomDurationScroller(getContext(), (Interpolator) interpolatorField.get(null));
            scrollerField.set(this, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setScrollerInterpolator(Interpolator interpolator) {
        try {
            Field mInterpolator = scroller.getClass().getSuperclass().getDeclaredField("mInterpolator");
            mInterpolator.setAccessible(true);
            mInterpolator.set(scroller, interpolator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scrollOnce() {
        PagerAdapter adapter = getAdapter();
        int currentItem = getCurrentItem();
        int totalCount;
        if (adapter == null || (totalCount = adapter.getCount()) <= 1) {
            return;
        }
        int nextItem = (direction == LEFT) ? --currentItem : ++currentItem;
        if (nextItem < 0) {
            if (isCycle) {
                setCurrentItem(totalCount - 1, isBorderAnimation);
            }
        } else if (nextItem == totalCount) {
            if (isCycle) {
                setCurrentItem(0, isBorderAnimation);
            }
        } else {
            setCurrentItem(nextItem, true);
        }
    }

    /**
     * <ul>
     * if stopScrollWhenTouch is true
     * <li>if event is down, stop auto scroll.</li>
     * <li>if event is up, start auto scroll again.</li>
     */
    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (stopScrollWhenTouch) {
            if ((ev.getAction() == MotionEvent.ACTION_DOWN) && isAutoScroll) {
                isStopByTouch = true;
                stopAutoScroll();
            } else if ((ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) && isStopByTouch) {
                startAutoScroll();
            }
        }
        if (dispatchTouchEventByScrollMode(ev)) return super.dispatchTouchEvent(ev);
//        dispatchTouchEventByGestures(ev);
        return super.dispatchTouchEvent(ev);
    }

    private boolean dispatchTouchEventByScrollMode(MotionEvent ev) {
        if (slideBorderMode == SLIDE_BORDER_MODE_TO_PARENT || slideBorderMode == SLIDE_BORDER_MODE_CYCLE) {
            float touchX = ev.getX();
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                downX = touchX;
            }
            int currentItem = getCurrentItem();
            PagerAdapter adapter = getAdapter();
            int pageCount = adapter == null ? 0 : adapter.getCount();
            if ((currentItem == 0 && downX <= touchX) || (currentItem == pageCount - 1 && downX >= touchX)) {
                if (slideBorderMode == SLIDE_BORDER_MODE_TO_PARENT) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    if (pageCount > 1) {
                        setCurrentItem(pageCount - currentItem - 1, isBorderAnimation);
                    }
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
        }
        return false;
    }

    private float   lastMoveX;
    private float   lastMoveY;
    private boolean isSetTouchGesture;

    private void dispatchTouchEventByGestures(MotionEvent ev) {
        float newTouchX = ev.getX();
        float newTouchY = ev.getY();
        if (lastMoveY > 0 || lastMoveX > 0) {
            if (Math.abs(newTouchY - lastMoveY) >= Math.abs(newTouchX - lastMoveX) * 1.7) {
                if (!isSetTouchGesture) {
                    isSetTouchGesture = true;
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            } else {
                if (!isSetTouchGesture) {
                    isSetTouchGesture = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } else {
            lastMoveY = newTouchY;
            lastMoveX = newTouchX;
        }
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            lastMoveX = 0;
            lastMoveY = 0;
            isSetTouchGesture = false;
        }
    }

    private class MyHandler extends Handler {
        @Override public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SCROLL_WHAT:
                    scroller.setScrollDurationFactor(autoScrollFactor);
                    scrollOnce();
                    scroller.setScrollDurationFactor(swipeScrollFactor);
                    sendScrollMessage(interval + scroller.getDuration());
                default:
                    break;
            }
        }
    }

    /**
     * 间隔时间
     *
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * set auto scroll time in milliseconds, default is
     * {@link #DEFAULT_INTERVAL}
     *
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * get auto scroll direction
     *
     * @return {@link #LEFT} or {@link #RIGHT}, default is {@link #RIGHT}
     */
    public int getDirection() {
        return (direction == LEFT) ? LEFT : RIGHT;
    }

    /**
     * set auto scroll direction
     *
     * @param direction {@link #LEFT} or {@link #RIGHT}, default is {@link #RIGHT}
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

    /**
     * whether automatic cycle when auto scroll reaching the last or first item,
     * default is true
     *
     * @return the isCycle
     */
    public boolean isCycle() {
        return isCycle;
    }

    /**
     * set whether automatic cycle when auto scroll reaching the last or first
     * item, default is true
     *
     * @param isCycle the isCycle to set
     */
    public void setCycle(boolean isCycle) {
        this.isCycle = isCycle;
    }

    /**
     * whether stop auto scroll when touching, default is true
     *
     * @return the stopScrollWhenTouch
     */
    public boolean isStopScrollWhenTouch() {
        return stopScrollWhenTouch;
    }

    /**
     * set whether stop auto scroll when touching, default is true
     */
    public void setStopScrollWhenTouch(boolean stopScrollWhenTouch) {
        this.stopScrollWhenTouch = stopScrollWhenTouch;
    }

    /**
     * get how to process when sliding at the last or first item
     *
     * @return the slideBorderMode {@link #SLIDE_BORDER_MODE_NONE},
     * {@link #SLIDE_BORDER_MODE_TO_PARENT},
     * {@link #SLIDE_BORDER_MODE_CYCLE}, default is
     * {@link #SLIDE_BORDER_MODE_NONE}
     */
    public int getSlideBorderMode() {
        return slideBorderMode;
    }

    /**
     * set how to process when sliding at the last or first item
     *
     * @param slideBorderMode {@link #SLIDE_BORDER_MODE_NONE},
     *                        {@link #SLIDE_BORDER_MODE_TO_PARENT},
     *                        {@link #SLIDE_BORDER_MODE_CYCLE}, default is
     *                        {@link #SLIDE_BORDER_MODE_NONE}
     */
    public void setSlideBorderMode(int slideBorderMode) {
        this.slideBorderMode = slideBorderMode;
    }

    /**
     * whether animating when auto scroll at the last or first item, default is
     * true
     */
    public boolean isBorderAnimation() {
        return isBorderAnimation;
    }

    /**
     * set whether animating when auto scroll at the last or first item, default
     * is true
     */
    public void setBorderAnimation(boolean isBorderAnimation) {
        this.isBorderAnimation = isBorderAnimation;
    }
}