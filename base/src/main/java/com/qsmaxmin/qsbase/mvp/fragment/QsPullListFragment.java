package com.qsmaxmin.qsbase.mvp.fragment;

import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;

import com.qsmaxmin.qsbase.R;
import com.qsmaxmin.qsbase.common.log.L;
import com.qsmaxmin.qsbase.common.utils.QsHelper;
import com.qsmaxmin.qsbase.common.widget.listview.LoadingFooter;
import com.qsmaxmin.qsbase.common.widget.ptr.PtrDefaultHandler;
import com.qsmaxmin.qsbase.common.widget.ptr.PtrFrameLayout;
import com.qsmaxmin.qsbase.common.widget.ptr.PtrHandler;
import com.qsmaxmin.qsbase.mvp.presenter.QsPresenter;

import java.util.List;

public abstract class QsPullListFragment<T extends QsPresenter, D> extends QsListFragment<T, D> implements QsIPullListFragment<D> {

    private PtrFrameLayout mPtrFrameLayout;
    private boolean canLoadingMore = true;

    @Override public int layoutId() {
        return R.layout.qs_fragment_pull_listview;
    }

    @Override public int getFooterLayout() {
        return QsHelper.getInstance().getApplication().listFooterLayoutId();
    }

    @Override protected View initView(LayoutInflater inflater) {
        View view = super.initView(inflater);
        initPtrFrameLayout(view);
        return view;
    }

    private void initPtrFrameLayout(View view) {
        if (view instanceof PtrFrameLayout) {
            mPtrFrameLayout = (PtrFrameLayout) view;
        } else {
            mPtrFrameLayout = (PtrFrameLayout) view.findViewById(R.id.swipe_container);
        }
        if (mPtrFrameLayout == null) throw new RuntimeException("PtrFrameLayout is not exit or its id not 'R.id.swipe_container' in current layout!!");
        mPtrFrameLayout.setHeaderView((View) getPtrUIHandlerView());
        mPtrFrameLayout.addPtrUIHandler(getPtrUIHandlerView());
        mPtrFrameLayout.setPtrHandler(new PtrHandler() {
            @Override public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }

            @Override public void onRefreshBegin(PtrFrameLayout frame) {
                onRefresh();
            }
        });
    }

    /**
     * 获取下拉刷新控件
     */
    @Override public PtrFrameLayout getPtrFrameLayout() {
        return mPtrFrameLayout;
    }

    @Override public void requestRefresh() {
        mPtrFrameLayout.autoRefresh();
    }

    @Override public void closeRefresh() {
        mPtrFrameLayout.refreshComplete();
    }

    @Override public void setLoadingState(LoadingFooter.State state) {
        if (mLoadingFooter != null) {
            L.i(initTag(), "设置刷新尾部状态：" + state);
            mLoadingFooter.setState(state);
        }
    }

    @Override public void openPullRefreshing() {
        mPtrFrameLayout.setEnabled(true);
    }

    @Override public void closePullRefreshing() {
        mPtrFrameLayout.setEnabled(false);
    }

    @Override public void openPullLoading() {
        canLoadingMore = true;
    }

    @Override public void closePullLoading() {
        canLoadingMore = false;
    }

    @Override public void setData(List<D> list) {
        mPtrFrameLayout.refreshComplete();
        super.setData(list);
    }

    private void loadingMoreData() {
        if (mLoadingFooter != null) {
            LoadingFooter.State state = mLoadingFooter.getState();
            if (!canLoadingMore) {
                return;
            } else if (state == LoadingFooter.State.Loading) {
                L.i(initTag(), "正在加载中,无需再次加载..........");
                return;
            } else if (state == LoadingFooter.State.TheEnd) {
                L.i(initTag(), "加载完了,没有更多数据了...........");
                return;
            }
            setLoadingState(LoadingFooter.State.Loading);
            onLoad();
        }
    }

    @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
        if (!canChildScrollDown() && scrollState == SCROLL_STATE_IDLE) {
            loadingMoreData();
        }
    }

    /**
     * listView是否滑动到底部
     */
    protected boolean canChildScrollDown() {
        return ViewCompat.canScrollVertically(mListView, 1);
    }
}
