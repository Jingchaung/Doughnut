package com.doughnut.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.android.jtblk.client.Transaction;
import com.android.jtblk.client.Wallet;
import com.android.jtblk.client.bean.AccountInfo;
import com.android.jtblk.client.bean.AccountOffers;
import com.android.jtblk.client.bean.AccountRelations;
import com.android.jtblk.client.bean.AccountTx;
import com.android.jtblk.client.bean.AmountInfo;
import com.android.jtblk.client.bean.Line;
import com.android.jtblk.client.bean.Marker;
import com.android.jtblk.client.bean.Offer;
import com.android.jtblk.client.bean.TransactionInfo;
import com.android.jtblk.keyStore.KeyStore;
import com.android.jtblk.keyStore.KeyStoreFile;
import com.android.jtblk.qrCode.QRGenerator;
import com.doughnut.net.api.GetAllTokenList;
import com.doughnut.net.load.RequestPresenter;
import com.doughnut.utils.GsonUtil;
import com.jccdex.rpc.api.JccConfig;
import com.jccdex.rpc.api.JccdexInfo;
import com.jccdex.rpc.base.JCallback;
import com.jccdex.rpc.url.JccdexUrl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 钱包管理类
 * <p>
 * 创建钱包、删除钱包、导出二维码图片、导入私钥、导入KeyStore、获取私钥、转账、获取交易记录、获取余额
 */
public class WalletManager implements IWallet {

    private static WalletManager walletManager = null;
    private static Context mContext;
    final private String CONFIG_HOST = "weidex.vip";
    final private String COUNTER = "CNT";

    private WalletManager() {
    }

    public static WalletManager getInstance(Context context) {
        mContext = context;
        if (walletManager == null) {
            synchronized (WalletManager.class) {
                if (walletManager == null) {
                    walletManager = new WalletManager();
                }
            }
        }
        return walletManager;
    }

    /**
     * 判断app当前是否有钱包
     *
     * @return
     */
    public boolean hasWallet() {
        try {
            return !TextUtils.isEmpty(WalletSp.getInstance(mContext, "").getCurrentWallet());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建钱包
     *
     * @param password
     * @param name
     * @return
     */
    @Override
    public String createWallet(String password, String name) {
        try {
            Wallet wallet = Wallet.generate();
            KeyStoreFile keyStoreFile = KeyStore.createLight(password, wallet);
            String address = keyStoreFile.getAddress();
            WalletSp.getInstance(mContext, address).createWallet(name, keyStoreFile.toString());
            return address;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 删除钱包
     */
    @Override
    public void deleteWallet(String address) {
        WalletSp.getInstance(mContext, address).delete();
    }

    /**
     * 导出二维码图片
     *
     * @param address
     * @param widthAndHeight
     * @param color
     * @return
     */
    @Override
    public Bitmap exportWalletWithQR(String address, int widthAndHeight, int color) {
        try {
            String keyStore = WalletSp.getInstance(mContext, address).getKeyStore();
            Bitmap bitmap = QRGenerator.getQrCodeImage(keyStore, widthAndHeight, color);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 导入私钥
     *
     * @param password
     * @param privateKey
     * @param name
     * @return
     */
    @Override
    public boolean importWalletWithKey(String password, String privateKey, String name) {
        try {
            if (Wallet.isValidSecret(privateKey)) {
                Wallet wallet = Wallet.fromSecret(privateKey);
                KeyStoreFile keyStoreFile = KeyStore.createLight(password, wallet);
                String address = keyStoreFile.getAddress();
                if (Wallet.isValidAddress(address)) {
                    WalletSp.getInstance(mContext, address).createWallet(name, keyStoreFile.toString());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 导入KeyStore
     *
     * @param keyStore
     * @param password
     * @param name
     * @return
     */
    @Override
    public boolean importKeysStore(String keyStore, String password, String name) {
        try {
            KeyStoreFile keyStoreFile = KeyStoreFile.parse(keyStore);
            String address = keyStoreFile.getAddress();
            if (Wallet.isValidAddress(address)) {
                String privateKey = getPrivateKey(password, address);
                if (Wallet.isValidSecret(privateKey)) {
                    WalletSp.getInstance(mContext, address).createWallet(name, keyStoreFile.toString());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取私钥
     *
     * @param password
     * @param address
     * @return
     */
    @Override
    public String getPrivateKey(String password, String address) {
        try {
            KeyStoreFile keyStoreFile = KeyStoreFile.parse(WalletSp.getInstance(mContext, address).getKeyStore());
            Wallet jtKeyPair = KeyStore.decrypt(password, keyStoreFile);
            return jtKeyPair.getSecret();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 转账
     *
     * @param privateKey
     * @param from
     * @param to
     * @param token
     * @param issuer
     * @param value
     * @param fee
     * @param memo
     * @return
     */
    @Override
    public boolean transfer(String privateKey, String from, String to, String token, String issuer, String value, String fee, String memo) {
        try {
            AmountInfo amount;
            amount = new AmountInfo();
            amount.setCurrency(token);
            amount.setIssuer(issuer);
            amount.setValue(value);
            Transaction tx = JtServer.getInstance(mContext).getRemote().buildPaymentTx(from, to, amount);
            tx.setSecret(privateKey);
            List<String> memos = new ArrayList<String>();
            memos.add(memo);
            tx.addMemo(memos);
            if (!TextUtils.isEmpty(fee)) {
                tx.setFee(fee);
            }
            TransactionInfo bean = tx.submit();
            if (WConstant.RESULT_OK.equals(bean.getEngineResultCode())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取交易记录
     *
     * @param limit
     * @return
     */
    @Override
    public AccountTx getTransferHistory(String address, Integer limit, Marker marker) {
        try {
            AccountTx bean = JtServer.getInstance(mContext).getRemote().requestAccountTx(address, limit, marker);
            return bean;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取SWT余额
     *
     * @return
     */
    @Override
    public String getSWTBalance(String address) {
        try {
            // 获取账户信息
            AccountInfo info = JtServer.getInstance(mContext).getRemote().requestAccountInfo(address, null, null);
            if (info != null && info.getAccountData() != null) {
                return info.getAccountData().getBalance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.00";
    }

    /**
     * 获取全部余额
     *
     * @return
     */
    @Override
    public AccountRelations getBalance(String address) {
        try {
            // 获取账户信息
            AccountInfo info = JtServer.getInstance(mContext).getRemote().requestAccountInfo(address, null, null);
            // 获取账户挂单信息
            AccountOffers accountOffers = JtServer.getInstance(mContext).getRemote().requestAccountOffers(address, null);
            // 获取账户其它token信息
            AccountRelations relationsTrust = JtServer.getInstance(mContext).getRemote().requestAccountRelations(address, null, WConstant.RELATION_TRUST);
            AccountRelations relationsFreeze = JtServer.getInstance(mContext).getRemote().requestAccountRelations(address, null, WConstant.RELATION_FREEZE);

            // 计算其它token冻结数量
            List<Offer> offers = accountOffers.getOffers();
            List<Line> linesT = relationsTrust.getLines();

            // 计算swt冻结数量
            Integer freezed = (relationsTrust.getLines().size() + accountOffers.getOffers().size()) * WConstant.FREEZED + WConstant.RESERVED;
            Line swtLine = new Line();
            BigDecimal valid = new BigDecimal(info.getAccountData().getBalance()).subtract(BigDecimal.valueOf(freezed));
            swtLine.setBalance(valid.stripTrailingZeros().toPlainString());
            swtLine.setCurrency(WConstant.CURRENCY_SWT);
            swtLine.setLimit(String.valueOf(freezed));
            relationsTrust.getLines().add(swtLine);

            // trust limit 置零
            for (int j = 0; j < linesT.size(); j++) {
                String currency = linesT.get(j).getCurrency();
                if (!TextUtils.equals(currency, WConstant.CURRENCY_SWT)) {
                    linesT.get(j).setLimit("0");
                }
            }

            // 根据挂单计算冻结数量
            for (int i = 0; i < offers.size(); i++) {
                String getsCurrency = offers.get(i).getTakerGets().getCurrency();
                for (int j = 0; j < linesT.size(); j++) {
                    Line line = linesT.get(j);
                    String currency = line.getCurrency();
                    if (TextUtils.equals(getsCurrency, currency)) {
                        BigDecimal tokenFreeze = new BigDecimal(line.getLimit()).add(new BigDecimal(offers.get(i).getTakerGets().getValue()));
                        BigDecimal balance = new BigDecimal(line.getBalance()).subtract(tokenFreeze);
                        line.setBalance(balance.stripTrailingZeros().toPlainString());
                        line.setLimit(tokenFreeze.stripTrailingZeros().toPlainString());
                    }
                }
            }

            // 根据冻结关系类型计算冻结数量
            List<Line> linesF = relationsFreeze.getLines();
            for (int i = 0; i < linesF.size(); i++) {
                String FCurrency = linesF.get(i).getCurrency();
                for (int j = 0; j < linesT.size(); j++) {
                    Line line = linesT.get(j);
                    String currency = line.getCurrency();
                    if (TextUtils.equals(FCurrency, currency)) {
                        BigDecimal tokenFreeze = new BigDecimal(line.getLimit()).add(new BigDecimal(linesF.get(i).getLimit()));
                        BigDecimal balance = new BigDecimal(line.getBalance()).subtract(tokenFreeze);
                        line.setBalance(balance.stripTrailingZeros().toPlainString());
                        line.setLimit(tokenFreeze.stripTrailingZeros().toPlainString());
                    }
                }
            }

            return relationsTrust;
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取token时价
     *
     * @param base
     * @param callback
     */
    @Override
    public void getTokenPrice(String base, JCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取infoHosts
                JccConfig jccConfig = JccConfig.getInstance();
                JccdexUrl jccdexUrl = new JccdexUrl(CONFIG_HOST, true);
                jccConfig.setmBaseUrl(jccdexUrl);
                jccConfig.requestConfig(new JCallback() {
                    @Override
                    public void onResponse(String code, String response) {
                        if (!TextUtils.isEmpty(response)) {
                            GsonUtil res = new GsonUtil(response);
                            GsonUtil infoHosts = res.getArray("infoHosts");
                            int index = (int) (Math.random() * infoHosts.getLength());
                            final JccdexUrl jccUrl = new JccdexUrl(infoHosts.getString(index, ""), true);
                            JccdexInfo jccdexInfo = JccdexInfo.getInstance();
                            jccdexInfo.setmBaseUrl(jccUrl);
                            // 获取时价
                            jccdexInfo.requestTicker(base, COUNTER, callback);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();
    }

    /**
     * 获取所有的token时价
     *
     * @param jCallback
     */
    @Override
    public void getAllTokenPrice(JCallback jCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取infoHosts
                JccConfig jccConfig = JccConfig.getInstance();
                JccdexUrl jccdexUrl = new JccdexUrl(CONFIG_HOST, true);
                jccConfig.setmBaseUrl(jccdexUrl);
                jccConfig.requestConfig(new JCallback() {
                    @Override
                    public void onResponse(String code, String response) {
                        if (!TextUtils.isEmpty(response)) {
                            GsonUtil res = new GsonUtil(response);
                            GsonUtil infoHosts = res.getArray("infoHosts");
                            int index = (int) (Math.random() * infoHosts.getLength());
                            final JccdexUrl jccUrl = new JccdexUrl(infoHosts.getString(index, ""), true);
                            JccdexInfo jccdexInfo = JccdexInfo.getInstance();
                            jccdexInfo.setmBaseUrl(jccUrl);
                            // 获取时价
                            jccdexInfo.requestAllTickers(jCallback);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();
    }

    /**
     * 获取所有tokens
     */
    @Override
    public void getAllTokens() {
        GetAllTokenList request = new GetAllTokenList();
        new RequestPresenter().loadData(request, false, new RequestPresenter.RequestCallback() {
            @Override
            public void onRequesResult(int ret, GsonUtil json) {
                String code = json.getString("code", "");
                if (TextUtils.equals(code, "0")) {
                    GsonUtil data = json.getArray("data");
                    Map<String, String> tokenMap = new TreeMap<>();
                    for (int i = 0; i < data.getLength(); i++) {
                        GsonUtil tokenPair = data.getObject(i);
                        List<String> keys = tokenPair.getKey();
                        GsonUtil tokens = tokenPair.getArray(keys.get(0));
                        for (int j = 0; j < tokens.getLength(); j++) {
                            String[] token = tokens.getString(j, "").split("_");
                            if (TextUtils.equals(token[0], "CNY")) {
                                tokenMap.put(token[0], "jGa9J9TkqtBcUoHe2zqhVFFbgUVED6o9or");
                                break;
                            }
                            if (token.length == 2) {
                                tokenMap.put(token[0], token[1]);
                            } else if (token.length == 1) {
                                tokenMap.put(token[0], "");
                            }
                        }
                    }
                    String JStr = JSON.toJSONString(tokenMap);
                    // 本地保存tokens
                    String fileName = mContext.getPackageName() + "_tokens";
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences(fileName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("tokens", JStr);
                    editor.apply();
                }
            }
        });
    }
}
