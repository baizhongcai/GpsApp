package com.bzc.gpsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int LOCATION_CODE = 301;
    private LocationManager mLocationManager;
    private String mLocationProvider = null;
    private EditText mLongitude;
    private EditText mLatitude;
    private EditText mAddress;
    private Button mStartBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    private void initEvent() {
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation();
            }
        });
    }

    private void initView() {
        mLongitude = findViewById(R.id.longitude);
        mLatitude = findViewById(R.id.latitude);
        mAddress = findViewById(R.id.address);
        mStartBtn = findViewById(R.id.startgps);
    }

    @SuppressLint("WrongConstant")
    private void getLocation () {

        if(isOPen(this)){
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //获取所有可用的位置提供器
            List<String> providers = mLocationManager.getProviders(true);

            Criteria criteria = new Criteria();
            // 查询精度：高，Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精确
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            // 是否查询海拨：否
            criteria.setAltitudeRequired(true);
            // 是否查询方位角 : 否
            criteria.setBearingRequired(false);
            // 设置是否要求速度
            criteria.setSpeedRequired(false);
            // 电量要求：低
            criteria.setPowerRequirement(Criteria.ACCURACY_LOW);

            mLocationProvider = mLocationManager.getBestProvider(criteria, false);  //获取最佳定位

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //获取权限（如果没有开启权限，会弹出对话框，询问是否开启权限）
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //请求权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_CODE);
                } else {
                    //监视地理位置变化
                    mLocationManager.requestLocationUpdates(mLocationProvider, 3000, 1, locationListener);
                    Location location = mLocationManager.getLastKnownLocation(mLocationProvider);
                    if (location != null) {
                        //输入经纬度
                        getCityInfo(MainActivity.this, location.getLongitude(), location.getLatitude());
                    }
                }
            } else {
                //监视地理位置变化
                mLocationManager.requestLocationUpdates(mLocationProvider, 3000, 1, locationListener);
                Location location = mLocationManager.getLastKnownLocation(mLocationProvider);
                if (location != null) {
                    //不为空,显示地理位置经纬度
                    getCityInfo(MainActivity.this, location.getLongitude(), location.getLatitude());
                }
            }
        }else{
            Toast.makeText(this, "请开启GPS", Toast.LENGTH_LONG).show();
        }
    }

    public LocationListener locationListener = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, "GPS正在打开", Toast.LENGTH_LONG);
        }
        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "GPS正在关闭", Toast.LENGTH_LONG);
        }
        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                //不为空,显示地理位置经纬度
                getCityInfo(MainActivity.this, location.getLongitude(), location.getLatitude());
            }
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_CODE:
                if(grantResults.length > 0 && grantResults[0] == getPackageManager().PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "申请权限", Toast.LENGTH_LONG).show();
                    try {
                        List<String> providers = mLocationManager.getProviders(true);
                        if (providers.contains(LocationManager.GPS_PROVIDER)) {
                            //如果是GPS
                            mLocationProvider = LocationManager.GPS_PROVIDER;
                        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                            //如果是Network
                            mLocationProvider = LocationManager.NETWORK_PROVIDER;
                        } else {
                            Intent i = new Intent();
                            i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(i);
                        }
                        //监视地理位置变化
                        mLocationManager.requestLocationUpdates(mLocationProvider, 3000, 1, locationListener);
                        Location location = mLocationManager.getLastKnownLocation(mLocationProvider);
                        if (location != null) {
                            //不为空,显示地理位置经纬度
                            getCityInfo(MainActivity.this, location.getLongitude(), location.getLatitude());
                        }
                    }catch (SecurityException e){
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "缺少权限", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(locationListener);
    }

    private void  getCityInfo(Context context,double getLongitude, double getLatitude) {
        List<Address> addresses = new ArrayList<>();
        //经纬度转城市
        Geocoder geocoder = new Geocoder(context);
        try {
            addresses = geocoder.getFromLocation(getLatitude, getLongitude, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (Address address : addresses) {
            //国家
            stringBuffer.append(address.getCountryName());
            //省，市，地址
            stringBuffer.append(address.getAdminArea());
            stringBuffer.append(address.getLocality());
            stringBuffer.append(address.getFeatureName());
        }
        mLongitude.setText(String.valueOf(getLongitude));
        mLatitude.setText(String.valueOf(getLatitude));
        mAddress.setText(stringBuffer.toString());
    }

    /**
     * 用来判断有没有打开GPS
     */
    public static final boolean isOPen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }
}