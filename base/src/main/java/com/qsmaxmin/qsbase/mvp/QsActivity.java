package com.qsmaxmin.qsbase.mvp;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ViewAnimator;

import com.qsmaxmin.qsbase.R;
import com.qsmaxmin.qsbase.common.log.L;
import com.qsmaxmin.qsbase.common.utils.PresenterUtils;
import com.qsmaxmin.qsbase.common.utils.QsHelper;
import com.qsmaxmin.qsbase.mvp.model.QsConstants;
import com.qsmaxmin.qsbase.mvp.presenter.QsPresenter;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import butterknife.ButterKnife;

/**
 * @CreateBy qsmaxmin
 * @Date 2017/6/20 16:40
 * @Description
 */
public abstract class QsActivity<P extends QsPresenter> extends FragmentActivity implements QsIActivity {
    private   P              presenter;
    protected ProgressDialog mProgressDialog;
    private   ViewAnimator   mViewAnimator;
    private   boolean        hasInitData;

    @Override public String initTag() {
        return getClass().getSimpleName();
    }


    @Override public int layoutId() {
        return R.layout.qs_framelayout;
    }


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QsHelper.getInstance().getScreenHelper().pushActivity(this);
        QsHelper.getInstance().getApplication().onActivityCreate();
        initView();
        initStatusBar();
        ButterKnife.bind(this);
        if (isOpenEventBus() && !EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
        if (!isDelayData()) {
            hasInitData = true;
            initData(savedInstanceState);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        QsHelper.getInstance().getApplication().onActivityStart();
    }

    @Override protected void onResume() {
        super.onResume();
        QsHelper.getInstance().getApplication().onActivityResume();
    }

    @Override protected void onPause() {
        super.onPause();
        QsHelper.getInstance().getApplication().onActivityPause();
    }

    @Override protected void onStop() {
        super.onStop();
        QsHelper.getInstance().getApplication().onActivityStop();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.setDetach();
        if (isOpenEventBus() && EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        QsHelper.getInstance().getApplication().onActivityDestroy();
        QsHelper.getInstance().getScreenHelper().popActivity(this);
    }


    protected void initView() {
        View rootView;
        if (isOpenViewState() && loadingLayoutId() > 0 && emptyLayoutId() > 0 && errorLayoutId() > 0) {
            rootView = View.inflate(this, R.layout.qs_activity_state, null);
            mViewAnimator = ButterKnife.findById(rootView, android.R.id.home);
            View.inflate(this, loadingLayoutId(), mViewAnimator);
            View.inflate(this, layoutId(), mViewAnimator);
            View.inflate(this, emptyLayoutId(), mViewAnimator);
            View.inflate(this, errorLayoutId(), mViewAnimator);
        } else {
            rootView = View.inflate(this, layoutId(), null);
        }
        setContentView(rootView);
    }

    protected void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) setTranslucentStatus(true);
        if (isStatusBarLowVersionDarkIcon()) setStatusBarIconDarkColor(true);
    }

    protected boolean isStatusBarLowVersionDarkIcon() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT) private void setTranslucentStatus(boolean on) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            } else {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            WindowManager.LayoutParams winParams = window.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            if (on) {
                winParams.flags |= bits;
            } else {
                winParams.flags &= ~bits;
            }
            window.setAttributes(winParams);
        }
    }

    private void setStatusBarIconDarkColor(boolean dark) {
        String manufacturer = Build.MANUFACTURER;
        if (TextUtils.isEmpty(manufacturer)) return;
        switch (manufacturer.toLowerCase()) {
            case "xiaomi":
                try {
                    Class<? extends Window> clazz = getWindow().getClass();
                    int darkModeFlag;
                    Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                    Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                    darkModeFlag = field.getInt(layoutParams);
                    Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                    extraFlagField.invoke(getWindow(), dark ? darkModeFlag : 0, darkModeFlag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case "meizu":
                try {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                    Field meiZuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
                    darkFlag.setAccessible(true);
                    meiZuFlags.setAccessible(true);
                    int bit = darkFlag.getInt(null);
                    int value = meiZuFlags.getInt(lp);
                    if (dark) {
                        value |= bit;
                    } else {
                        value &= ~bit;
                    }
                    meiZuFlags.setInt(lp, value);
                    getWindow().setAttributes(lp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override public P getPresenter() {
        if (presenter == null) {
            synchronized (this) {
                if (presenter == null) {
                    presenter = PresenterUtils.createPresenter(getClass(), this);
                    L.i(initTag(), "Presenter初始化完成...");
                }
            }
        }
        return presenter;
    }

    @Override public void initDataWhenDelay() {
        if (!hasInitData) {
            initData(getIntent().getExtras());
            hasInitData = true;
        }
    }

    @Override public Context getContext() {
        return this;
    }

    @Override public boolean isOpenEventBus() {
        return false;
    }

    @Override public boolean isOpenViewState() {
        return false;
    }

    @Override public boolean isDelayData() {
        return false;
    }

    @Override public void activityFinish() {
        activityFinish(false);
    }

    @Override public void activityFinish(boolean finishAfterTransition) {
        if (finishAfterTransition) ActivityCompat.finishAfterTransition(this);
        else finish();
    }

    @Override public int loadingLayoutId() {
        return QsHelper.getInstance().getApplication().loadingLayoutId();
    }

    @Override public int emptyLayoutId() {
        return QsHelper.getInstance().getApplication().emptyLayoutId();
    }

    @Override public int errorLayoutId() {
        return QsHelper.getInstance().getApplication().errorLayoutId();
    }

    @Override public void loading() {
        loading(true);
    }

    @Override public void loading(boolean cancelAble) {
        loading(getString(R.string.loading), cancelAble);
    }

    @Override public void loading(String message) {
        loading(message, true);
    }

    @Override public void loading(String message, boolean cancelAble) {
        if (mProgressDialog == null) mProgressDialog = QsHelper.getInstance().getApplication().getCommonProgressDialog(getContext());
        mProgressDialog.setMessage(message);
        mProgressDialog.setCancelable(cancelAble);
        mProgressDialog.show();
    }

    @Override public void loadingClose() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) mProgressDialog.cancel();
    }

    @Override public void showLoadingView() {
        setViewState(QsConstants.VIEW_STATE_LOADING);
    }

    @Override public void showContentView() {
        setViewState(QsConstants.VIEW_STATE_CONTENT);
    }

    @Override public void showEmptyView() {
        setViewState(QsConstants.VIEW_STATE_EMPTY);
    }

    @Override public void showErrorView() {
        setViewState(QsConstants.VIEW_STATE_ERROR);
    }

    @Override public int currentViewState() {
        return 0;
    }

    @Override public void intent2Activity(Class clazz) {
        QsHelper.getInstance().intent2Activity(clazz);
    }

    @Override public void intent2Activity(Class clazz, int requestCode) {
        QsHelper.getInstance().intent2Activity(clazz, null, requestCode);
    }

    @Override public void intent2Activity(Class clazz, Bundle bundle) {
        QsHelper.getInstance().intent2Activity(clazz, bundle);
    }

    @Override public void intent2Activity(Class clazz, Bundle bundle, int requestCode) {
        QsHelper.getInstance().intent2Activity(clazz, bundle, requestCode);
    }

    @Override public void commitFragment(Fragment fragment) {
        commitFragment(fragment, fragment.getClass().getSimpleName());
    }

    @Override public void commitFragment(Fragment fragment, String tag) {
        commitFragment(android.R.id.custom, fragment, tag);
    }

    @Override public void commitFragment(int layoutId, Fragment fragment) {
        commitFragment(layoutId, fragment, fragment.getClass().getSimpleName());
    }

    @Override public void commitFragment(int layoutId, Fragment fragment, String tag) {
        if (fragment != null && fragment.isAdded()) {
            return;
        }
        getSupportFragmentManager().beginTransaction().add(layoutId, fragment, tag).setTransition(FragmentTransaction.TRANSIT_NONE).commitAllowingStateLoss();
        if (!isOpenViewState()) getSupportFragmentManager().executePendingTransactions();
    }

    @Override public void commitFragment(Fragment old, Fragment fragment) {
        commitFragment(old, fragment, fragment.getClass().getSimpleName());
    }

    @Override public void commitFragment(Fragment old, Fragment fragment, String tag) {
        commitFragment(old, android.R.id.custom, fragment, tag);
    }

    @Override public void commitFragment(Fragment old, int layoutId, Fragment fragment) {
        commitFragment(old, layoutId, fragment, fragment.getClass().getSimpleName());
    }

    @Override public void commitFragment(Fragment old, int layoutId, Fragment fragment, String tag) {
        if (layoutId == 0) return;
        if (fragment != null && fragment.isAdded()) return;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (old != null) fragmentTransaction.detach(old);
        fragmentTransaction.add(layoutId, fragment, tag).setTransition(FragmentTransaction.TRANSIT_NONE).commitAllowingStateLoss();
        if (!isOpenViewState()) getSupportFragmentManager().executePendingTransactions();
    }

    @Override public void commitBackStackFragment(Fragment fragment) {
        commitBackStackFragment(fragment, fragment.getClass().getSimpleName());
    }

    @Override public void commitBackStackFragment(Fragment fragment, String tag) {
        if (fragment != null && fragment.isAdded()) return;
        getSupportFragmentManager().beginTransaction().add(android.R.id.custom, fragment, tag).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitAllowingStateLoss();
        if (!isOpenViewState()) getSupportFragmentManager().executePendingTransactions();
    }

    @Override public void commitBackStackFragment(int layoutId, Fragment fragment) {
        commitBackStackFragment(layoutId, fragment, fragment.getClass().getSimpleName());

    }

    @Override public void commitBackStackFragment(int layoutId, Fragment fragment, String tag) {
        if (layoutId == 0) return;
        if (fragment != null && fragment.isAdded()) return;
        getSupportFragmentManager().beginTransaction().add(layoutId, fragment, tag).addToBackStack(null).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commitAllowingStateLoss();
        if (!isOpenViewState()) getSupportFragmentManager().executePendingTransactions();
    }

    private void setViewState(int showState) {
        L.i(initTag(), "setViewState() showState=" + showState);
        if (!isOpenViewState()) {
            L.i(initTag(), "当前activity没有打开状态模式! isOpenViewState() = false");
            return;
        }
        if (mViewAnimator == null) {
            return;
        }
        int displayedChild = mViewAnimator.getDisplayedChild();
        if (displayedChild == showState) {
            return;
        }
        mViewAnimator.setDisplayedChild(showState);
        if (showState == QsConstants.VIEW_STATE_ERROR) {
            mViewAnimator.getCurrentView().setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showLoadingView();
                    initData(getIntent().getExtras());
                }
            });
        }
    }
}
