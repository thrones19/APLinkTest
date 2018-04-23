package stark.a.is.zhang.wifitest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private WifiManager mWifiManager;
    private Handler mMainHandler;
    private boolean mHasPermission;
    private WifiReceiver mWifiReceiver;
    private TextView tvConnectCount, tvConnectSuccessCount, tvConnectFailCount, tvSuccessTime;
    private int connectCount, connectSuccessCount, connectFailCount;
    private ScanResult mScanResult;
//    private  String AP_SSID = "TP-LINK_1504";
//    private  static final String AP_SSID = "OPPLE_AP_LINK";
//    private  static final String AP_SSID = "OppleHuawei";
    private  static final String AP_SSID = "OPWIFIAP_00050062_FD32";
    private long connectTime, connectSuccessTime,totalTime,successOnceTime;
    private boolean connectFlag, checkThreadCanRun;
    private Thread checkThread;
    private List<String> successTimeList = new ArrayList();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(WIFI_SERVICE);
        mMainHandler = new Handler();

        findChildViews();

        configChildViews();

        mHasPermission = checkPermission();
        if (!mHasPermission) {
            requestPermission();
        }

        initWifiReceiver();



    }

    Button mOpenWifiButton;
    Button mGetWifiInfoButton;
    RecyclerView mWifiInfoRecyclerView;

    private void findChildViews() {
        mOpenWifiButton = (Button) findViewById(R.id.open_wifi);
        mGetWifiInfoButton = (Button) findViewById(R.id.get_wifi_info);
        mWifiInfoRecyclerView = (RecyclerView) findViewById(R.id.wifi_info_detail);
        tvConnectCount = (TextView) findViewById(R.id.tv_connect_count);
        tvConnectSuccessCount = (TextView) findViewById(R.id.tv_connect_success_count);
        tvConnectFailCount = (TextView) findViewById(R.id.tv_connect_fail_count);
        tvSuccessTime = (TextView) findViewById(R.id.tv_connect_success_time);

    }

    private Runnable mMainRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWifiManager.isWifiEnabled()) {
                mGetWifiInfoButton.setEnabled(true);
            } else {
                mMainHandler.postDelayed(mMainRunnable, 1000);
            }
        }
    };

    private List<ScanResult> mScanResultList;

    private void configChildViews() {
        mOpenWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                    mMainHandler.post(mMainRunnable);
                }
            }
        });

        mGetWifiInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectCount = 0;
                connectSuccessCount = 0;
                connectFailCount = 0;
                changeCountText();
                if (mWifiManager.isWifiEnabled()) {
                    mScanResultList = mWifiManager.getScanResults();
                    sortList(mScanResultList);
                    mWifiInfoRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });

        mWifiInfoRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mWifiInfoRecyclerView.setAdapter(new ScanResultAdapter());
    }

    private void sortList(List<ScanResult> list) {
        TreeMap<String, ScanResult> map = new TreeMap<>();
        for (ScanResult scanResult : list) {
            map.put(scanResult.SSID, scanResult);
        }
        list.clear();
        list.addAll(map.values());
    }

    private class ScanResultViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private TextView mWifiName;
        private TextView mWifiLevel;

        ScanResultViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mWifiName = (TextView) itemView.findViewById(R.id.ssid);
            mWifiLevel = (TextView) itemView.findViewById(R.id.level);
        }

//        void bindScanResult(final ScanResult scanResult) {
//            mWifiName.setText(
//                    getString(R.string.scan_wifi_name, "" + scanResult.SSID));
//            mWifiLevel.setText(
//                    getString(R.string.scan_wifi_level, "" + scanResult.level));
//
//            mView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    connectTime = System.currentTimeMillis();
//                    mScanResult = scanResult;
//                    connectWifi(scanResult);
//                }
//            });
//        }
    }

    private void connectWifi(final ScanResult scanResult) {
        if (scanResult == null) {
            Toast.makeText(this, "未找到指定的WIFI", Toast.LENGTH_SHORT).show();
            return;
        }
        int netId = mWifiManager.addNetwork(createWifiConfig(scanResult.SSID, "", WIFICIPHER_NOPASS));
//        int netId = mWifiManager.addNetwork(createWifiConfig(scanResult.SSID, "opwifi.fzj123", WIFICIPHER_WPA));
        boolean enable = mWifiManager.enableNetwork(netId, true);
        Log.d("ZJTest", "enable: " + enable);
        boolean reconnect = mWifiManager.reconnect();
        Log.d("ZJTest", "reconnect: " + reconnect);
        //连接wifi次数
        connectCount++;
        changeCountText();

        connectFlag = true;
        checkThreadCanRun = true;
        if (checkThread!=null){
            checkThread.interrupt();
            checkThread = null;
        }
        checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (checkThreadCanRun && connectCount < 10){
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (!wifiInfo.getSSID().equalsIgnoreCase("\"" + AP_SSID + "\"")){
                        connectWifi(mScanResult);
                        Log.e("ZJTest","10s无WIFI连接");
                    }
                }
            }
        });

        checkThread.start();
    }

    private void changeCountText(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnectCount.setText("连接次数:" + connectCount);
                tvConnectSuccessCount.setText("成功:" + connectSuccessCount);
                tvConnectFailCount.setText("失败:" + connectFailCount);
            }
        });
    }


    private static final int WIFICIPHER_NOPASS = 0;
    private static final int WIFICIPHER_WEP = 1;
    private static final int WIFICIPHER_WPA = 2;

    private WifiConfiguration createWifiConfig(String ssid, String password, int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        WifiConfiguration tempConfig = isExist(ssid);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (type == WIFICIPHER_NOPASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (type == WIFICIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }

    private class ScanResultAdapter extends RecyclerView.Adapter<ScanResultViewHolder> {
        @Override
        public ScanResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.item_scan_result, parent, false);

            return new ScanResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ScanResultViewHolder holder, int position) {
            if (mScanResultList != null) {
                final ScanResult scanResult = mScanResultList.get(position);
                holder.mWifiName.setText(
                            getString(R.string.scan_wifi_name, "" + scanResult.SSID));
                holder.mWifiLevel.setText(
                            getString(R.string.scan_wifi_level, "" + scanResult.level));

                holder.mView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            connectTime = System.currentTimeMillis();
                            mScanResult = scanResult;
                            connectWifi(mScanResult);
                        }
                    });

            }
        }

        @Override
        public int getItemCount() {
            if (mScanResultList == null) {
                return 0;
            } else {
                return mScanResultList.size();
            }
        }
    }

    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for (String permission : NEEDED_PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static final int PERMISSION_REQUEST_CODE = 0;

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWifiManager.isWifiEnabled() && mHasPermission) {
            mGetWifiInfoButton.setEnabled(true);
        } else {
            mGetWifiInfoButton.setEnabled(false);
            if (mScanResultList != null) {
                mScanResultList.clear();
                mWifiInfoRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean hasAllPermission = true;

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermission = false;
                    break;
                }
            }

            if (hasAllPermission) {
                mHasPermission = true;
            } else {
                mHasPermission = false;
                Toast.makeText(
                        this, "Need More Permission",
                        Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterBroadcastReceiver();
        if (mWifiReceiver != null) {
            unregisterReceiver(mWifiReceiver);
        }
    }


    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub

            String action = intent.getAction();
//            Log.d("ZJTest", action);

            if (action.equalsIgnoreCase(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {//wifi连接上与否

                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d("ZJTest", "NetworkInfo.State = " + info.getState());
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    Log.d("ZJTest", "wifi网络连接断开");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTING)) {
                    Log.d("ZJTest", "wifi网络正在连接中");
                }
                else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    if (!connectFlag) return;
                    connectFlag = false;
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (connectCount == 0) return;
                    if (checkThread != null){
                        checkThreadCanRun = false;
                        checkThread.interrupt();
                    }
                    if (wifiInfo.getSSID().equalsIgnoreCase("\"" + AP_SSID + "\"")) {
                        connectSuccessCount++;
                        connectCount = 0;
                        connectFailCount = 0;
                        changeCountText();
                        connectSuccessTime = System.currentTimeMillis();
                        successOnceTime = connectSuccessTime - connectTime;
//                        totalTime = totalTime + successOnceTime;
                        connectTime = System.currentTimeMillis();
                        successTimeList.add(String.valueOf(successOnceTime));
                        for (int i = 0; i < successTimeList.size(); i++){
                            Log.d("ZJTest", "successTimeList    " + successTimeList.get(i));
                        }
                        Log.d("ZJTest", "wifi网络连接成功**" + "  connectSuccessCount" + connectSuccessCount + "   successOnceTime" + successOnceTime);
                        if (connectSuccessCount < 5) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectWifi(mScanResult);

                                }
                            },5000);
                        }
//                        Toast.makeText(MainActivity.this, "连接到网络" + wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                    } else {
                        connectFailCount++;
                        changeCountText();
                        Log.d("ZJTest", "连接到其它WIFI ："  + wifiInfo.getSSID() + "  connectFailCount" + connectFailCount);
                        if (connectCount < 10) {
                            mWifiManager.disconnect();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectWifi(mScanResult);

                                }
                            },5000);
                        }else {
                            tvSuccessTime.setText("时间:" + successOnceTime);
                        }
                    }

                }
            }

//            if (action.equalsIgnoreCase(WifiManager.WIFI_STATE_CHANGED_ACTION)){
////                Log.d("ZJTest", "WIFI_STATE_CHANGED_ACTION");
//            }
//
//            if (action.equalsIgnoreCase(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)){
////                Log.d("ZJTest", "SUPPLICANT_CONNECTION_CHANGE_ACTION");
//            }
//
//            if (action.equalsIgnoreCase(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
//                SupplicantState state = intent.getParcelableExtra("newState");
//                SupplicantState.isValidState(state);
//                Log.d("ZJTest", "newState = "+SupplicantState.isValidState(state)+"  "+WifiManager.EXTRA_SUPPLICANT_ERROR + intent.getIntExtra("supplicantError",999));
//            }

//            if (intent.getAction().equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION)){
//                ConnectivityManager c = (ConnectivityManager)getSystemService(context.CONNECTIVITY_SERVICE);
//                NetworkInfo in = c.getActiveNetworkInfo();
//                if (in != null && in.isAvailable()){
//                    String name = in.getTypeName();
//                    int type = in.getType();
//                    if (type == ConnectivityManager.TYPE_WIFI){
//                        Log.d("ZJTest", "wifi网络连接" + name);
//                    }
//                }
//            }

        }
    }

    private void initWifiReceiver() {

        mWifiReceiver = new WifiReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        registerReceiver(mWifiReceiver, filter);
    }

}
