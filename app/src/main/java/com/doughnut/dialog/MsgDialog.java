package com.doughnut.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.doughnut.R;
import com.doughnut.config.AppConfig;
import com.doughnut.utils.ViewUtil;
import com.doughnut.view.CrossIcon;
import com.doughnut.view.HookIcon;


public class MsgDialog extends BaseDialog {

    private TextView mTvMsg;
    private HookIcon mVHook;
    private CrossIcon mVCross;
    private String mMsg;

    private boolean isHook = true;

    public MsgDialog(@NonNull Context context, String msg) {
        super(context, R.style.DialogStyle);
        mMsg = msg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.layout_msg_dialog);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.y = ViewUtil.dip2px(getContext(), 70);
        lp.gravity = Gravity.TOP;
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(R.style.Dialog_anim);
        getWindow().setBackgroundDrawableResource(R.color.transparent);
        getWindow().setDimAmount(0f);
        initView();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void show() {
        super.show();
        if (isHook) {
            mVHook.startAnim();
        } else {
            mVCross.startAnim();
        }
    }

    private void initView() {
        mTvMsg = (TextView) findViewById(R.id.tv_content);
        mTvMsg.setText(mMsg);
        mVHook = findViewById(R.id.v_hook);
        mVCross = findViewById(R.id.v_cross);
        if (!isHook) {
            mVCross.setVisibility(View.VISIBLE);
            mVHook.setVisibility(View.GONE);
        }

        //5秒后关闭
        AppConfig.postDelayOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 5000);
    }

    /**
     * 是否需要验证密码，默认true
     *
     * @param isVerifyPwd
     * @return
     */
    public MsgDialog setIsHook(boolean isVerifyPwd) {
        this.isHook = isVerifyPwd;
        return this;
    }
}