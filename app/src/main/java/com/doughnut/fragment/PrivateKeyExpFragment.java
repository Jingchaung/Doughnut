package com.doughnut.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.doughnut.R;
import com.doughnut.utils.ToastUtil;
import com.doughnut.utils.Util;
import com.doughnut.wallet.WalletManager;
import com.doughnut.wallet.WalletSp;


public class PrivateKeyExpFragment extends BaseFragment implements View.OnClickListener {


    private TextView mTvPrivatekey, mTvCopyPrivatekey;

    private Context mContext;


    public static PrivateKeyExpFragment newInstance() {
        Bundle args = new Bundle();
        PrivateKeyExpFragment fragment = new PrivateKeyExpFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_private_key_exp, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity();
        initView(view);
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode == Activity.RESULT_OK && data != null) {
//            String result = data.getStringExtra("scan_result");
//            if (!result.isEmpty()) {
//                mEPrivateKey.setText(result);
//            }
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();
//        mLayoutManageWallet.setClickable(true);
//        mLayoutRecordTransaction.setClickable(true);
//        mLayoutNotification.setClickable(true);
//        mLayoutHelp.setClickable(true);
//        mLayoutAbout.setClickable(true);
//        mLayoutLanguage.setClickable(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_copy_privatekey:
                Util.clipboard(getContext(), "", mTvPrivatekey.getText().toString());
                ToastUtil.toast(getContext(), getContext().getString(R.string.toast_private_key_copied));
                break;
        }
    }


    /**
     * 画面初期化
     *
     * @param view
     */
    private void initView(View view) {

        String currentWallet = WalletSp.getInstance(getContext(), "").getCurrentWallet();
        String privateKey = WalletManager.getInstance(mContext).getPrivateKey("11111111", currentWallet);

        mTvPrivatekey = view.findViewById(R.id.tv_privatekey);

        mTvCopyPrivatekey = view.findViewById(R.id.tv_copy_privatekey);
        mTvCopyPrivatekey.setOnClickListener(this);

        mTvPrivatekey.setText(privateKey);

    }
}




