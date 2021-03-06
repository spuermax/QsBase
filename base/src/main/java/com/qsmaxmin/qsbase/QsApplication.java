package com.qsmaxmin.qsbase;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.LayoutRes;

import com.qsmaxmin.qsbase.common.http.QsHttpCallback;
import com.qsmaxmin.qsbase.common.utils.ImageHelper;
import com.qsmaxmin.qsbase.common.utils.QsHelper;
import com.qsmaxmin.qsbase.common.widget.dialog.QsProgressDialog;

/**
 * @CreateBy qsmaxmin
 * @Date 2017/6/20 16:40
 * @Description
 */

public abstract class QsApplication extends Application implements QsIApplication {

    @Override public void onCreate() {
        super.onCreate();
        QsHelper.init(this);
    }

    @Override public Application getApplication() {
        return this;
    }

    @Override public abstract boolean isLogOpen();

    @Override public void onActivityCreate(Activity activity) {
    }

    @Override public void onActivityStart(Activity activity) {
    }

    @Override public void onActivityResume(Activity activity) {
    }

    @Override public void onActivityPause(Activity activity) {
    }

    @Override public void onActivityStop(Activity activity) {
    }

    @Override public void onActivityDestroy(Activity activity) {
    }

    @Override public @LayoutRes int loadingLayoutId() {
        return 0;
    }

    @Override public @LayoutRes int emptyLayoutId() {
        return 0;
    }

    @Override public @LayoutRes int errorLayoutId() {
        return 0;
    }

    /**
     * 公共progressDialog
     */
    @Override public QsProgressDialog getLoadingDialog() {
        return null;
    }

    /**
     * http请求全局回调
     */
    @Override public QsHttpCallback registerGlobalHttpListener() {
        return null;
    }

    /**
     * 公共图片加载回调
     */
    public void onCommonLoadImage(ImageHelper.Builder builder) {
    }
}
