package com.xuxian.marketpro.activity.store;

import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ab.http.AbHttpUtil;
import com.ab.http.IHttpResponseCallBack;
import com.ab.util.AbPreferenceUtils;
import com.ab.util.AbStrUtil;
import com.ab.util.AbToastUtil;
import com.amap.api.location.AMapLocation;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.Projection;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.xuxian.marketpro.R;
import com.xuxian.marketpro.activity.supers.SuperFragment;
import com.xuxian.marketpro.libraries.gaodemap.GaoDeLocationLibraries;
import com.xuxian.marketpro.libraries.util.ActivityUtil;
import com.xuxian.marketpro.libraries.util.monitor.CityMonitor;
import com.xuxian.marketpro.libraries.util.monitor.GaoDeLocationMonitor;
import com.xuxian.marketpro.libraries.util.monitor.GoodsMonitor;
import com.xuxian.marketpro.libraries.util.monitor.monitor;
import com.xuxian.marketpro.libraries.util.monitor.monitor.GaoDeLocationEnum;
import com.xuxian.marketpro.net.NewIssRequest;
import com.xuxian.marketpro.net.RequestParamsNet;
import com.xuxian.marketpro.presentation.View.adapter.AreaAdapter;
import com.xuxian.marketpro.presentation.View.adapter.StoreAdapter;
import com.xuxian.marketpro.presentation.View.widght.ActivityStateView;
import com.xuxian.marketpro.presentation.db.ShoppingCartGoodsDb;
import com.xuxian.marketpro.presentation.db.StoreDb;
import com.xuxian.marketpro.presentation.entity.CityEntity;
import com.xuxian.marketpro.presentation.entity.GetStoreEntity;
import com.xuxian.marketpro.presentation.entity.ShoppingCartGoodsEntity;
import com.xuxian.marketpro.presentation.entity.StoreEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by youarenotin on 16/7/27.
 */
public class StoreFragment extends SuperFragment implements LocationSource {
    private AMap aMap;
    private AreaAdapter areaAdapter;
    private LatLng cenpt;
    private CityEntity.DataEntity.CityInfoEntity cityEntity;
    private String city_area;
    private String city_id;
    private ActivityStateView emptyview_state;
    private GetStoreEntity getStoreEntity;
    private boolean isExpress;
    boolean isFirstLoc;
    private boolean isShowPop;
    private Handler mHandler = new Handler();
    private boolean isSwitchCity;
    private LinearLayout ll_storeFragment_map;
    private ListView lv_shop_site_area;
    private MapView mapView;
    private LatLng mylatLng;
    public TextView shop_overlay_name;
    private ListView shop_site_list;
    private ShoppingCartGoodsDb shoppingCartGoodsDb;
    private StoreAdapter storeAdapter;
    private StoreDb storeDb;
    private StoreEntity storeEntity;
    private List<GetStoreEntity.DataBean.StoreInfoBean> storeInfoEntities;
    private List<StoreEntity> storeList;
    public View view_layout;
    private List<StoreEntity> store_list;

    @Override
    protected void init() {
        if (this.cityEntity != null) {
            this.city_id = cityEntity.getCity_id();
        } else {
            this.city_id = AbPreferenceUtils.loadPrefString(getActivity(), "city_id", "");
        }
        this.storeDb = new StoreDb(getActivity());
        this.shoppingCartGoodsDb = new ShoppingCartGoodsDb(getActivity());
        this.storeAdapter = new StoreAdapter(getActivity());
        shop_site_list.setAdapter(storeAdapter);
        this.areaAdapter = new AreaAdapter(getActivity());
        lv_shop_site_area.setAdapter(areaAdapter);
        this.storeAdapter.setOnShopItemClickListener(new StoreAdapter.OnShopItemListener() {//进入店面
            @Override
            public void showOverLayPop(StoreEntity storeEntity) {
                LatLng latLng = new LatLng(storeEntity.getLat().doubleValue(), storeEntity.getLng().doubleValue());
                Marker marker = aMap.addMarker(new MarkerOptions()
                        .title(storeEntity.getTitle())
                        .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_gcoding_overlay)))
                        .draggable(true)
                        .position(latLng)
                        .period(50));
                marker.setObject(storeEntity);
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));//将地图移动到指定latlng 和zoomlevel的视窗
                marker.showInfoWindow();//将marker显示到camera中
                replacestore(storeEntity);//切换商店
            }
        });
        shop_site_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
        lv_shop_site_area.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                areaAdapter.setInitPosition(i);
                if (storeInfoEntities != null && !storeInfoEntities.isEmpty()) {
                    List<StoreEntity> store_list = storeInfoEntities.get(i).getStore_list();
                    storeList = store_list;
                    storeAdapter.setData(store_list);
                }
                initMapOverLay(true);
            }
        });
        this.city_area = AbPreferenceUtils.loadPrefString(getActivity(), "city_area");
        gaoDeLocation();
    }

    private void replacestore(StoreEntity entity) {
        if (entity != null) {
            int id = entity.getId();//店面site_id
            int store_id = AbPreferenceUtils.loadPrefInt(getActivity(), "site_id", 0);
            if (0 == 0) {//第一次進入店面
                saveData(entity);
                AbPreferenceUtils.savePrefInt(getActivity(), "start_id", 1);//非第一次启动店面  是否进入引导页的标志
                ActivityUtil.startTabMainActivity(getActivity());//启动店面activity
                CityMonitor.getInstance().IssueMonitors(monitor.CityEnum.CLOSE_PAGE, null);
            } else if (store_id == id) {//切换到与原先一样的店面
                saveData(entity);
                //TODO 刷新商品列表
                CityMonitor.getInstance().IssueMonitors(monitor.CityEnum.CLOSE_PAGE, null);
            } else {// 切换到新的店面
                List<ShoppingCartGoodsEntity> list = this.shoppingCartGoodsDb.queryAllData(AbPreferenceUtils.loadPrefString(getActivity(), "USER_ID", "0"));
                if (list == null || list.size() == 0) {
                    saveData(entity);
                    GoodsMonitor.issueGoodsMonitorCallback(monitor.GoodsEnum.REFRESH_GOODS); //刷新店面商品监视器
                    CityMonitor.getInstance().IssueMonitors(monitor.CityEnum.CLOSE_PAGE, null);
                    return;
                }
                //// TODO: 8/1 0001 原店面购物车有商品给予dialog提示清空
            }
        }
    }

    private void saveData(StoreEntity entity) {
        AbPreferenceUtils.savePrefInt(getActivity(), StoreFragmentActivity.ADDRESS_ID, 0);
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.SITE_NAME, entity.getTitle());
        AbPreferenceUtils.savePrefInt(getActivity(), StoreFragmentActivity.SITE_ID, entity.getId());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.STORE_ADDRESS, entity.getArea());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.CITY_ID, entity.getCity_id());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.CITY_NAME, entity.getCity_name());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.CITY_AREA, entity.getCity_area());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.AREA_ID, entity.getArea_id());
        AbPreferenceUtils.savePrefString(getActivity(), StoreFragmentActivity.STORE_TYPE, "自提");
        if (storeDb.isExists(entity.getId())){
            storeDb.update(entity);
        }else {
            storeDb.saveData(entity);
        }
    }


    private void gaoDeLocation() {
        this.emptyview_state.setVisibility(View.VISIBLE);
        this.emptyview_state.setState(ActivityStateView.ACTIVITY_STATE_LOADING);
        GaoDeLocationLibraries.getInstance(getActivity()).startLocation(false, GaoDeLocationEnum.LOCATION_LATITUDE_AND_LONGITUDE);
    }

    @Override
    protected void initTitleBar() {

    }

    @Override
    protected void initFindViewById(View view) {
        this.lv_shop_site_area = (ListView) view.findViewById(R.id.lv_shop_site_area);
        this.shop_site_list = (ListView) view.findViewById(R.id.shop_site_list);
        this.ll_storeFragment_map = (LinearLayout) view.findViewById(R.id.ll_storeFragment_map);
        this.emptyview_state = (ActivityStateView) view.findViewById(R.id.emptyview_state);
    }

    @Override
    protected void setListener() {
        GaoDeLocationMonitor.getInstance().register(StoreFragment.class.getSimpleName(), new GaoDeLocationMonitor.GaoDeLocationMonitorCallback() {
            @Override
            public void appOperation(GaoDeLocationEnum gaoDeLocationEnum, AMapLocation aMapLocation) {
                switch (gaoDeLocationEnum) {
                    case LOCATION_LATITUDE_AND_LONGITUDE:
                        if (aMapLocation == null) {
                            emptyview_state.setVisibility(View.GONE);
                            return;
                        } else if (aMapLocation.getLongitude() <= 0 || aMapLocation.getLatitude() <= 0) {
                            emptyview_state.setVisibility(View.GONE);
                            AbToastUtil.showToast(getActivity(), "定位失败");
                        } else {
                            emptyview_state.setVisibility(View.GONE);
                            cenpt = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                            mylatLng = cenpt;
                            initStore(Double.valueOf(aMapLocation.getLatitude()), Double.valueOf(aMapLocation.getLongitude()));
                        }
                }
            }


        });

        CityMonitor.getInstance().register(StoreFragment.class.getSimpleName(), new CityMonitor.CityMomitorCallback() {
            @Override
            public void appOpration(monitor.CityEnum cityEnum, CityEntity.DataEntity.CityInfoEntity cityEntity) {

            }
        });


    }

    private void initStore(Double lat, Double lng) {
        AbHttpUtil.getInstance(getActivity()).postAndParse(NewIssRequest.GETSTORE,
                RequestParamsNet.getInstance(getActivity()).getStoreInfo("" + lat, "" + lng, this.city_id),
                GetStoreEntity.class,
                new IHttpResponseCallBack<GetStoreEntity>() {

                    @Override
                    public void EndToParse() {

                    }

                    @Override
                    public void FailedParseBean(String str) {
                        emptyview_state.setVisibility(View.VISIBLE);
                        emptyview_state.setState(ActivityStateView.ACTIVITY_STATE_NETWORK_ERROR);
                    }

                    @Override
                    public void StartToParse() {
                        emptyview_state.setVisibility(View.VISIBLE);
                        emptyview_state.setState(ActivityStateView.ACTIVITY_STATE_LOADING);
                    }

                    @Override
                    public void SucceedParseBean(GetStoreEntity content) {
                        emptyview_state.setVisibility(View.GONE);
                        getStoreEntity = content;
                        if (getStoreEntity != null) {
                            if (getStoreEntity == null) {
                                ((StoreFragmentActivity) getActivity()).showStoreMessge(getStoreEntity.getStatus().getMessage());
                                ll_storeFragment_map.setVisibility(View.INVISIBLE);

                            }
                            ll_storeFragment_map.setVisibility(View.VISIBLE);
                            storeInfoEntities = getStoreEntity.getData().getStore_info();
                            if (storeInfoEntities != null && !storeInfoEntities.isEmpty()) {
                                areaAdapter.setData(storeInfoEntities);
                                if (AbStrUtil.isEmpty(city_area)) {
                                    isShowPop = false;
                                    areaAdapter.setInitPosition(0);
                                    store_list = storeInfoEntities.get(0).getStore_list();

                                } else {
                                    isShowPop = true;
                                    for (int i = 0; i < storeInfoEntities.size(); i++) {
                                        if (storeInfoEntities.get(i).getArea_name().equals(city_area)) {
                                            store_list = storeInfoEntities.get(i).getStore_list();
                                            areaAdapter.setInitPosition(i);
                                            break;
                                        }
                                    }
                                }
                                storeAdapter.setData(store_list);
                                initMapOverLay(false);
                            }
                        }
                    }

                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.shop_location_layout, null);
        initTitleBar();
        initFindViewById(view);
        setListener();
        init();
        initMap(view, savedInstanceState);
        return view;
    }

    private void initMap(View view, Bundle savedInstanceState) {
        this.mapView = (MapView) view.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        if (this.aMap == null) {
            aMap = mapView.getMap();
//            aMap.setTrafficEnabled(true);//显示交通状况
//            aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
            aMap.setLocationSource(this);
            aMap.setMyLocationEnabled(true);
            UiSettings uiSettings = aMap.getUiSettings();
            uiSettings.setZoomGesturesEnabled(true);
            uiSettings.setScaleControlsEnabled(true);
            uiSettings.setZoomControlsEnabled(false);
            uiSettings.setMyLocationButtonEnabled(true);
            initMapOverLay(false);

            aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {

                }
            });

            aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
                @Override
                public void onMapLoaded() {

                }
            });

            aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    StoreEntity obj = (StoreEntity) marker.getObject();//获取自定义marker对象
                    LatLng latLng = new LatLng(obj.getLat(), obj.getLng());//获取marker的latlng对象
                    Projection projection = aMap.getProjection();
                    long startTime = SystemClock.uptimeMillis();
                    Point startPoint = projection.toScreenLocation(latLng);
                    startPoint.offset(0, -100);
                    mHandler.post(new HanderlRunnable(latLng, aMap.getProjection().fromScreenLocation(startPoint), mHandler, new BounceInterpolator(), marker, startTime));
                    return false;
                }
            });

            aMap.setOnInfoWindowClickListener(new AMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {

                }
            });

            aMap.setInfoWindowAdapter(new AMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    View view = View.inflate(getActivity(), R.layout.pop_shop_overlay_layout, null);
                    renderMarkerPopOverLay(view, marker);
                    return view;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    return null;
                }
            });
        }
    }

    private void renderMarkerPopOverLay(View view, Marker marker) {
        StoreEntity obj = (StoreEntity) marker.getObject();
        ((TextView) view.findViewById(R.id.shop_overlay_name)).setText(obj.getTitle());
        LinearLayout ll_enter_shop = (LinearLayout) view.findViewById(R.id.ll_enter_shop);
        ll_enter_shop.setOnClickListener(new EnterShopOnClickListener(obj));//marker中进入店面按钮
        view.findViewById(R.id.ll_enter_shop_detail).setOnClickListener(new EnterShopDetailOnClickListener(obj));//marker中店面详细按钮
    }

    private void initMapOverLay(boolean isSwitchArea) {//默认为false 点击区域选择为true
        String title = "";
        if (this.storeList != null && !this.storeList.isEmpty()) {
            this.aMap.clear();
            int store_id = AbPreferenceUtils.loadPrefInt(getActivity(), "site_id", 0);
            if (store_id > 0 && !isSwitchArea && !isSwitchCity) {
                //TODO 如果有site_id将camera移动到该视窗

            } else {//没有site_i 第一次进入
                this.cenpt = new LatLng(storeList.get(0).getLat().doubleValue(), storeList.get(0).getLng().doubleValue());//显示店面列表position为0的在地图中的位置
                this.isSwitchCity = false;
            }
            changeCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(cenpt, 15.0f, 0, 90.0f)), null);

            if (this.mylatLng != null) {//添加自己位置marker
                ArrayList<BitmapDescriptor> gifList = new ArrayList<>();
                gifList.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                gifList.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                gifList.add(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                aMap.addMarker(new MarkerOptions().title("我的位置").icons(gifList).period(50).position(mylatLng).anchor(0.5f, 1f).draggable(true));
            }


            for (StoreEntity mStoreEntity2 : this.storeList) {//添加该area所有店面marker
                Marker marker = this.aMap.addMarker(new MarkerOptions().title(mStoreEntity2.getTitle()).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_gcoding_overlay))).draggable(true).position(new LatLng(mStoreEntity2.getLat().doubleValue(), mStoreEntity2.getLng().doubleValue())).period(50));
                marker.setObject(mStoreEntity2);
                if (title.equals(mStoreEntity2.getTitle())) {
                    marker.showInfoWindow();
                }
            }
        }

    }

    private void changeCamera(CameraUpdate cameraUpdate, AMap.CancelableCallback callback) {
        this.aMap.animateCamera(cameraUpdate, 1000, callback);
    }

    public void setCityEntity(CityEntity.DataEntity.CityInfoEntity cityInfoEntity) {
        this.cityEntity = cityInfoEntity;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null)
            mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

    public static class HanderlRunnable implements Runnable {
        private LatLng startLatLng;
        private LatLng endLatlng;
        private Handler handler;
        private Interpolator interpolator;
        private Marker marker;
        private long startTime;


        public HanderlRunnable(LatLng startLatLng, LatLng endLatlng, Handler handler, Interpolator interpolator, Marker marker, long startTime) {
            this.startLatLng = startLatLng;
            this.endLatlng = endLatlng;
            this.handler = handler;
            this.interpolator = interpolator;
            this.marker = marker;
            this.startTime = startTime;
        }

        @Override
        public void run() {
            float t = this.interpolator.getInterpolation(((float) (SystemClock.uptimeMillis() - this.startTime)) / 1000.0f);
            double lat = (((double) t) * this.startLatLng.latitude) + (((double) (1 - t)) * this.endLatlng.latitude);
            this.marker.setPosition(new LatLng(lat, (((double) t) * this.startLatLng.longitude) + (((double) (1 - t)) * this.startLatLng.longitude)));
            if (((double) t) < 1.0d) {
                this.handler.postDelayed(this, 16);
            }
        }
    }

    private class EnterShopOnClickListener implements View.OnClickListener {
        private StoreEntity entity;

        public EnterShopOnClickListener(StoreEntity obj) {
            this.entity = obj;
        }

        @Override
        public void onClick(View view) {
            replacestore(entity);
        }
    }

    private class EnterShopDetailOnClickListener implements View.OnClickListener {
        private StoreEntity entity;

        public EnterShopDetailOnClickListener(StoreEntity obj) {
            this.entity = obj;
        }

        @Override
        public void onClick(View view) {
            ActivityUtil.startStoreDetailsActivity(getActivity(), entity);
        }
    }
}
