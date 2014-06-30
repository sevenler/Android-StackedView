package com.edisonwang.stackedview.view;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class StackedView extends RelativeLayout {

	public static final boolean DEBUG = true;

	private static final String TAG = "StackedViews";

	private double threshold = 0.1;//手势滑动到height * threshold 后，自动滚动页面

	private int duration = 250;

	private float mLastMotionY;

	private int mActivePointerId;

	private View[] views;

	private int size;//子view数量

	private int current;

	private RelativeLayout root;

	private boolean isScrolling;

	private boolean isScrollingBottom;

	private boolean isPrepared;

	private ScrollerRunner scroller;

	private int topPage;

	private OnPageChangeListener onPageChangeListener;

	private boolean scrollingByTouch = true;//是否根据手势进行滚动
	private static int INTERVAL_BOTTOM_HEIGHT = 100;//卡片层叠的底部间隔高度
	private static int INTERVAL_TOP_HEIGHT = 0;//卡片层叠的顶部间隔高度

	public StackedView(Context context) {
		super(context);
		initStackedViews(context, this, 0);
	}

	public StackedView(Context context, int initialPage) {
		super(context);
		initStackedViews(context, this, initialPage);
	}

	public StackedView(Context context, RelativeLayout root) {
		super(context);
		initStackedViews(context, root, 0);
		if (root != this) {
			addView(root);
		}
	}

	public StackedView(Context context, RelativeLayout root, int initialPage) {
		super(context);
		initStackedViews(context, root, initialPage);
		if (root != this) {
			addView(root);
		}
	}

	public StackedView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initStackedViews(context, this, 0);
	}

	public StackedView(Context context, AttributeSet attrs, int initialPage) {
		super(context);
		initStackedViews(context, this, initialPage);
	}

	public StackedView(Context context, AttributeSet attrs, RelativeLayout root) {
		super(context);
		initStackedViews(context, root, 0);
		if (root != this) {
			addView(root);
		}
	}

	public StackedView(Context context, AttributeSet attrs,
			RelativeLayout root, int initialPage) {
		super(context);
		initStackedViews(context, root, initialPage);
		if (root != this) {
			addView(root);
		}
	}

	public void setRoot(RelativeLayout r) {
		root = r;
		initStackedViews(getContext(), root, 0);
	}

	public RelativeLayout getRoot() {
		return root;
	}

	public StackedView setTopPage(int topPage) {
		this.topPage = topPage;
		return this;
	}

	public int getTopPage() {
		return topPage;
	}

	public void setScrollingByTouch(boolean enabled) {
		this.scrollingByTouch = enabled;
	}

	/**
	 * Currently only onPageSelected is implemented.
	 * 
	 * @param onPageChangeListener
	 */
	public void setOnPageChangedListener(
			OnPageChangeListener onPageChangeListener) {
		this.onPageChangeListener = onPageChangeListener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			mLastMotionY = ev.getY();
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			break;
		}
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			final int index = MotionEventCompat.getActionIndex(ev);
			final float y = MotionEventCompat.getY(ev, index);
			mLastMotionY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			break;
		}
		}
		boolean intercept = super.onInterceptTouchEvent(ev);
		debug("onInterceptTouchEvent: " + intercept);
		this.onTouchEvent(ev);
		return false;
	}

	public void setCurrent(int current) {
		this.current = current;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		boolean callSuper = false;
		if (size == 0) {
			debug("Size == 0");
			return super.onTouchEvent(ev);
		}
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			debug("Down detected");
			mLastMotionY = ev.getY();
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
		}
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			debug("Pointer Down detected");
			callSuper = true;
			final int index = MotionEventCompat.getActionIndex(ev);
			final float y = MotionEventCompat.getY(ev, index);
			mLastMotionY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			debug("Pointer Move detected.");
			if (!scrollingByTouch) {
				break;
			}
			
			//如果滚动动画没有在执行，则根据手势滚动
			if ((!isScrolling) && mActivePointerId != -1) {
				// Scroll to follow the motion event
				final int activePointerIndex = MotionEventCompat
						.findPointerIndex(ev, mActivePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				fixZzIndex(mLastMotionY - y);
				mLastMotionY = y;
			} else {
				debug("Did not perform move because scrolling :" + isScrolling
						+ " and pointerId: " + mActivePointerId);
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP: {
			debug("Pointer Up or Cancel detected");
			mActivePointerId = -1;
			scrollToInternal();
			break;
		}
		case MotionEvent.ACTION_POINTER_UP: {
			final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = MotionEventCompat.getPointerId(ev,
					pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastMotionY = ev.getY(newPointerIndex);
				mActivePointerId = MotionEventCompat.getPointerId(ev,
						newPointerIndex);
				callSuper = true;
			}
			break;
		}
		}
		if (callSuper) {
			// TODO
		}
		return true;
	}

	@Override
	public void invalidate() {
		initStackedViews(getContext(), root, current);
		super.invalidate();
	}

	@Override
	public void addView(View child) {
		if (root == this) {
			addStackedView(child);
		} else {
			super.addView(child);
		}
	}

	/**
	 * Add view to root and reinitialize the view.
	 * 
	 * @param child
	 */
	public void addStackedView(View child) {
		if (root == this) {
			super.addView(child);
		} else {
			root.addView(child);
		}
		initStackedViews(getContext(), getRoot(), current);
	}

	// INTERNAL *************************************

	private void initStackedViews(Context context,
			RelativeLayout relativeLayout, int initialIndex) {
		if (relativeLayout == null) {
			return;
		}
		int cCount = relativeLayout.getChildCount();
		this.views = new View[cCount];
		for (int i = 0; i < cCount; i++) {
			views[i] = relativeLayout.getChildAt(i);
		}
		size = views.length;
		setInitialViewIndex(initialIndex);
		setIsScrolling(false);
		isPrepared = false;
		root = relativeLayout;
		topPage = -1;
	}

	private void setInitialViewIndex(int n) {
		if (n != 0 && n >= size) {
			throw new IllegalArgumentException("N is greater than the number of views");
		}
		for (int i = 0; i < size; i++) {
		}
		current = n;
	}

	private void fixZzIndexTop(float deltaX) {
		debug("Fix to top, current: " + current);
		RelativeLayout.LayoutParams params = (LayoutParams) views[current].getLayoutParams();
		params.topMargin -= deltaX;
		params.bottomMargin += deltaX;
		views[current].setLayoutParams(params);
		
		if (params.topMargin < INTERVAL_TOP_HEIGHT) {
			debug("Change to scrolling to bottom.");
			isScrollingBottom = true;
			prepareScrollingToBottom();
		}
	}
	
	private void prepareScrollingToBottom() {
		if (topPage == current - 1) {
			// TODO
		}
		debug("Prepared Scrolling To bottom");
		
		RelativeLayout.LayoutParams params = (LayoutParams) views[current].getLayoutParams();
		params.topMargin = INTERVAL_TOP_HEIGHT;
		params.bottomMargin = -INTERVAL_TOP_HEIGHT;
		views[current].setLayoutParams(params);
		if (current < size - 1) {
			params = (LayoutParams) views[current + 1].getLayoutParams();
			
			params.topMargin = views[current].getWidth() - (int)(((size - current - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT);
			params.bottomMargin = -params.topMargin;
			views[current + 1].setLayoutParams(params);
		}
		this.isPrepared = true;
	}

	private void fixZzIndexBottom(float deltaX) {
		debug("Fix to bottom, current: " + current);
		// Move
		if (current < size - 1) {
			RelativeLayout.LayoutParams params = (LayoutParams) views[current + 1].getLayoutParams();
			params.topMargin -= deltaX;
			params.bottomMargin += deltaX;
			views[current + 1].setLayoutParams(params);
			// Test
			if (params.topMargin > views[current].getHeight() - (int)(((size - current - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT)) {
				isScrollingBottom = false;
				prepareScrollingToTop();
			}
		} else {
			isScrollingBottom = false;
			prepareScrollingToTop();
		}
	}

	private void prepareScrollingToTop() {
		debug("Prepared Scrolling To top");
		this.isPrepared = true;
	}

	//根据deltaX实时滚动
	private void fixZzIndex(float deltaX) {
		debug("Moving and Fixing Index.");
		
		boolean toTop = deltaX < 0;
		boolean toBottom = deltaX > 0;
		if (toBottom) {
			if (current >= size - 1) {
				debug("Return because current>size-1");
				return;
			}
		} else {
			if (toTop && current == 0) {
				debug("Return because current==0");
				return;
			}
		}
		if (!isPrepared) {
			if (toBottom) {
				isScrollingBottom = true;
				prepareScrollingToBottom();
			} else {
				if (toTop) {
					isScrollingBottom = false;
					prepareScrollingToTop();
				}
			}
		}
		if (isPrepared && isScrollingBottom) {
			fixZzIndexBottom(deltaX);
		} else {//TODO isPrepared判断
			fixZzIndexTop(deltaX);
		}

	}

	/**
	 * 
	 * @return index if needs to snap, else return -1.返回可以滚动到的位置
	 */
	private int scrollTestInternal() {
		final int height = views[current].getHeight();
		if (isScrollingBottom) {
			if (current >= size - 1) {
				return -1;
			}
			RelativeLayout.LayoutParams params = (LayoutParams) views[current + 1].getLayoutParams();
			if (Math.abs(params.topMargin) <= height * (1 - threshold)  - INTERVAL_BOTTOM_HEIGHT) {
				return current < size - 1 ? current + 1 : current;
			}
		} else {
			RelativeLayout.LayoutParams params = (LayoutParams) views[current].getLayoutParams();
			if (Math.abs(params.topMargin) > height * threshold + INTERVAL_TOP_HEIGHT) {
				return current > 0 ? current - 1 : current;
			}
		}
		return -1;
	}

	private void setIsScrolling(boolean isScrolling) {
		debug("setIsScrolling to " + isScrolling);
		this.isScrolling = isScrolling;
	}

	private synchronized void scrollTo(int index) {
		if (isScrolling) {
			return;
		}
		debug("Scrolling to from current: " + current + " to " + index + " ("
				+ (isScrollingBottom ? "bottom" : "top") + ")");
		
		new Thread(getScroller(index)).start();
	}

	public synchronized ScrollerRunner getScroller(int index) {
		if (scroller == null) {
			scroller = new ScrollerRunner();
		}
		scroller.index = index;
		return scroller;
	}

	//canceled = true 回滚到当前index
	//canceled = false 滚动到下一个或者上一个位置或者是不滚动。下一个还是上一个位置，取决与滚动的方向 isScrollingbottom
	private void scrollToInternal() {
		int nextIndex = scrollTestInternal();
		if (nextIndex >= 0 && nextIndex != current) {
			scrollTo(nextIndex);
		}else{
//			scrollTo(current);
		}
	}

	private static void debug(String msg) {
		Log.i(TAG, msg);
	}
	
	private void onScrollBottom(int distance, int step, int total, int index){
//		System.out.println(String.format("onScrollBottom distance:%s total:%s  index:%s", distance, total, index));
		int height = views[index].getHeight();
		for(int i = index + 1; i <= size - 1; i++){
			final RelativeLayout.LayoutParams params = (LayoutParams) views[i].getLayoutParams();
			int targetTopMargin = (int)(((size - i - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT);
			int move = (height - targetTopMargin) + distance * targetTopMargin / total ;
			final int in = i;
			params.topMargin = move;
			params.bottomMargin = -params.topMargin;
			views[in].post(new Runnable() {
				@Override
				public void run() {
					views[in].setLayoutParams(params);
				}
			});
		}
	}
	
	private void onScrollTop(int distance, int step, int total, int index){
//		System.out.println(String.format("onScrollBottom distance:%s total:%s  index:%s", distance, total, index));
		int height = views[index].getHeight();
		for(int i = index + 1; i <= size - 1; i++){
			final RelativeLayout.LayoutParams params = (LayoutParams) views[i].getLayoutParams();
			int targetTopMargin = (int)(((size - i - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT);

			if(total == distance) continue;//total == distance 导致move计算的除数为0
			float move = targetTopMargin * ((1 - (float)distance / total) / ((float)total / distance - 1)) + height - targetTopMargin;
			params.topMargin = (int)move;
			params.bottomMargin = -params.topMargin;
			final int in = i;
			views[in].post(new Runnable() {
				@Override
				public void run() {
					views[in].setLayoutParams(params);
				}
			});
		}
	}

	public class ScrollerRunner implements Runnable {

		public int index;//这是目标index

		@Override
		public synchronized void run() {
			setIsScrolling(true);
			if (isScrollingBottom) {
				if (current + 1 > size - 1) {
					isPrepared = false;
					setIsScrolling(false);
					return;
				}
				debug((" Scrolling ") + ("top") + " Currnet: " + current);

				final RelativeLayout.LayoutParams params = (LayoutParams) views[current + 1].getLayoutParams();

				int totalDistance = params.topMargin -  INTERVAL_TOP_HEIGHT; // >0
				int distance = Math.abs(totalDistance / duration);

				if (distance == 0) {
					distance = 1;
				}
				boolean needsMore;
				needsMore = params.topMargin -  INTERVAL_TOP_HEIGHT > 0;
				int begin = params.topMargin;
				while (needsMore) {
					params.topMargin += -distance;
					params.bottomMargin = -params.topMargin;
					needsMore = params.topMargin > INTERVAL_TOP_HEIGHT;
					views[current + 1].post(new Runnable() {
						@Override
						public void run() {
							views[current + 1].setLayoutParams(params);
						}
					});
					onScrollBottom(begin - params.topMargin, distance, totalDistance, current + 1);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				views[current + 1].post(new Runnable() {
					@Override
					public void run() {
						debug("Set Current Index to " + index);
						current = index;
						if (onPageChangeListener != null) {
							onPageChangeListener.onPageSelected(index);
						}
						RelativeLayout.LayoutParams params = (LayoutParams) views[current]
								.getLayoutParams();
						params.topMargin = INTERVAL_TOP_HEIGHT;
						params.bottomMargin = -INTERVAL_TOP_HEIGHT;
						views[current].setLayoutParams(params);
						isPrepared = false;
						setIsScrolling(false);
					}

				});
			} else {
				if (current < 1) {
					isPrepared = false;
					setIsScrolling(false);
					return;
				}

				final RelativeLayout.LayoutParams params = (LayoutParams) views[current].getLayoutParams();
				debug(("  Scrolling ") + ("bottom"));

				int totalDistance = (views[current - 1].getHeight() - params.topMargin - (int)(((size - current - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT));
				int distance = Math.abs(totalDistance / duration);
				if (distance == 0) {
					distance = 1;
				}
				boolean needsMore;
				needsMore = params.topMargin < views[current - 1].getHeight() - (int)(((size - current - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT);
				int begin = params.topMargin;
				while (needsMore) {
					params.topMargin += distance;
					params.bottomMargin = -params.topMargin;
					needsMore = Math.abs(params.topMargin) < views[current - 1].getHeight() - (int)(((size - current - 1) * 1 + 1) * INTERVAL_BOTTOM_HEIGHT);
					views[current].post(new Runnable() {
						@Override
						public void run() {
							views[current].setLayoutParams(params);
						}
					});
					onScrollTop(totalDistance - (params.topMargin - begin), distance, totalDistance, current);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				debug("Finished scrolling.");
				views[current].post(new Runnable() {
					@Override
					public void run() {
						debug("Set Current Index to " + index);
						current = index;
						if (onPageChangeListener != null) {
							onPageChangeListener.onPageSelected(index);
						}
						isPrepared = false;
						setIsScrolling(false);
					}

				});
			}
		}
	}
}
