/*******************************************************************************
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.mjhram.geodata;


import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.heinrichreimersoftware.materialdrawer.DrawerView;
import com.heinrichreimersoftware.materialdrawer.structure.DrawerItem;
import com.mjhram.geodata.Faq.Faqtivity;
import com.mjhram.geodata.common.AppSettings;
import com.mjhram.geodata.common.EventBusHook;
import com.mjhram.geodata.common.MyInfo;
import com.mjhram.geodata.common.Session;
import com.mjhram.geodata.common.Utilities;
import com.mjhram.geodata.common.events.CommandEvents;
import com.mjhram.geodata.common.events.ServiceEvents;
import com.mjhram.geodata.common.slf4j.SessionLogcatAppender;
import com.mjhram.geodata.helper.UploadClass;
import com.mjhram.geodata.views.GenericViewFragment;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

public class GpsMainActivity extends GenericViewFragment
    implements
    //Toolbar.OnMenuItemClickListener,
    //ActionBar.OnNavigationListener,
    GoogleApiClient.ConnectionCallbacks,
    LocationListener,
    OnMapReadyCallback {

    public static AtomicInteger activitiesLaunched = new AtomicInteger(0);
    static public ArrayList<MyInfo> myinfoList = new ArrayList<>();
    static private ArrayList<Marker> locationMarkers = new ArrayList<>();
    //static private ArrayList<Polygon> locationMarkers = new ArrayList<>();

    private static boolean userInvokedUpload;
    private static Intent serviceIntent;
    private ActionBarDrawerToggle drawerToggle;
    private org.slf4j.Logger tracer;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    //public Marker fromMarker, toMarker;
    //private Button btnAccept;
    //private Button btnAcceptedTask;
    private FloatingActionButton btnOnOff;

    private int driverState;
    //private CountDownTimer countDownTimer;
    //private TRequestObj selectedTRequest = null;
    private DrawerLayout mDrawerLayout;
    //public SQLiteHandler dbhandler;
    //private MarkerBuilderManagerV2 markerBuilderManager;

    public class btnOnClickListener implements View.OnClickListener {
        public void onClick(View v) {//start new trip when start or stop service
            AppSettings.setTripId("-1");
            switch (driverState) {
                case 0://state0 is serviceON, then turn it off
                    EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(false));
                    //btnAcceptedTask.setText(getString(R.string.BtnTextTurnOn));
                    btnOnOff.setBackgroundTintList(ColorStateList.valueOf(0xffcc334a));
                    driverState = 1;
                    break;
                case 1://state1 is serviceOFF, then turn it on
                    EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
                    //btnAcceptedTask.setText(getString(R.string.BtnTextTurnOff));
                    btnOnOff.setBackgroundTintList(ColorStateList.valueOf(0xff4caf50));
                    driverState = 0;
                    firstMarkerTime=-1;
                    lastMarkerTime=-1;
                    break;

            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (activitiesLaunched.incrementAndGet() > 1) {
            finish();
        }
        super.onCreate(savedInstanceState);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        if (!AppSettings.isLoggedIn()) {
            UploadClass uc = new UploadClass(this);
            uc.checkLogin();
        }
        //dbhandler = new SQLiteHandler(this);
        //Utilities.ConfigureLogbackDirectly(getApplicationContext());
        tracer = LoggerFactory.getLogger(GpsMainActivity.class.getSimpleName());

        loadPresetProperties();

        setContentView(R.layout.activity_gps_main);

        //AppSettings.setTripId("-1");// new tripid
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        btnOnOff = (FloatingActionButton) findViewById(R.id.fab_onoff);
        btnOnOff.setOnClickListener(new btnOnClickListener());
        /*btnAcceptedTask = (Button) findViewById(R.id.btnAcceptedTask);
        btnAcceptedTask.setOnClickListener(new btnOnClickListener());*/


        buildGoogleApiClient();


        SetUpToolbar();
        SetUpNavigationDrawer();
        //LoadDefaultFragmentView();
        StartAndBindService();
        //RegisterEventBus();

        if(AppSettings.shouldStartLoggingOnAppLaunch()){
            tracer.debug("Start logging on app launch");
            EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
        }
        String regId = AppSettings.getRegId();
        String token = FirebaseInstanceId.getInstance().getToken();

        if(regId == null || regId.isEmpty() || !regId.equals(token)) {
            AppSettings.setRegId(token);
            regId = token;
            AppSettings.shouldUploadRegId = true;
        }
        if(AppSettings.shouldUploadRegId && regId != null) {
            AppSettings.shouldUploadRegId = false;
            //UploadClass uc = new UploadClass(null);
            UploadClass.updateRegId(AppSettings.getUid(), regId);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.setMyLocationEnabled(true);
        //map.setOnMyLocationButtonClickListener(this);
        googleMap = map;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                googleMap.setPadding(0,0,0,btnOnOff.getHeight());
            }
        });
        //setUpMap(googleMap);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (location != null && AppSettings.firstZooming) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 13));
            AppSettings.firstZooming = false;
        } else {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }
    protected synchronized void buildGoogleApiClient() {
        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) fm.findFragmentById(R.id.location_map);
        mapFragment.setRetainInstance(true);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        //setActionButtonStop();
        super.onStart();
        mGoogleApiClient.connect();
        StartAndBindService();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
        //        new IntentFilter(Constants.UPDATE_REQ));
        mGoogleApiClient.connect();
        StartAndBindService();

        if (Session.hasDescription()) {
            //SetAnnotationReady();
        }

        //enableDisableMenuItems();
    }

    @Override
    protected void onPause() {
        StopAndUnbindServiceIfRequired();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        activitiesLaunched.getAndDecrement();
        StopAndUnbindServiceIfRequired();
        //UnregisterEventBus();
        super.onDestroy();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            ToggleDrawer();
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService()) {
            StopAndUnbindServiceIfRequired();
        }

        if(keyCode == KeyEvent.KEYCODE_BACK){
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
                ToggleDrawer();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void loadPresetProperties() {

        //Either look for /<appfolder>/geodata.properties or /sdcard/geodata.properties
        File file =  new File(Utilities.GetDefaultStorageFolder(getApplicationContext()) + "/geodata.properties");
        if(!file.exists()){
            file = new File(Environment.getExternalStorageDirectory() + "/geodata.properties");
            if(!file.exists()){
                return;
            }
        }

        try {
            Properties props = new Properties();
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            props.load(reader);

            AppSettings.SetPreferenceFromProperties(props);

        } catch (Exception e) {
            tracer.error("Could not load preset properties", e);
        }
    }


    /**
     * Helper method, launches activity in a delayed handler, less stutter
     */
    private void LaunchPreferenceScreen(final String whichFragment) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent targetActivity = new Intent(getApplicationContext(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", whichFragment);
                startActivity(targetActivity);
            }
        }, 250);
    }



    public Toolbar GetToolbar(){
        return (Toolbar)findViewById(R.id.toolbar);
    }

    public void SetUpToolbar(){
        try{
            Toolbar toolbar = GetToolbar();
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
        }
        catch(Exception ex){
            //http://stackoverflow.com/questions/26657348/appcompat-v7-v21-0-0-causing-crash-on-samsung-devices-with-android-v4-2-2
            tracer.error("Thanks for this, Samsung", ex);
        }

    }

    public void SetUpNavigationDrawer() {
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final DrawerView drawer = (DrawerView) findViewById(R.id.drawer);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                GetToolbar(),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ){

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primaryColorDark));
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.closeDrawer(drawer);

        //drawer.addDivider();
        drawer.addItem(new DrawerItem()
                        .setId(1000)
                        .setImage(ContextCompat.getDrawable(this, R.drawable.settings))
                        .setTextPrimary(getString(R.string.pref_general_title))
                        .setTextSecondary(getString(R.string.pref_general_summary))
        );

        drawer.addItem(new DrawerItem()
                        .setId(2)
                        .setImage(ContextCompat.getDrawable(this, R.drawable.performance))
                        .setTextPrimary(getString(R.string.pref_performance_title))
                        .setTextSecondary(getString(R.string.pref_performance_summary))
        );

        drawer.addItem(new DrawerItem()
                        .setId(11)
                        .setImage(ContextCompat.getDrawable(this,R.drawable.helpfaq))
                        .setTextPrimary(getString(R.string.menu_faq))
        );

        drawer.addDivider();
        drawer.addItem(new DrawerItem()
                .setId(13)
                .setImage(ContextCompat.getDrawable(this, R.drawable.about))
                .setTextPrimary(getString(R.string.menu_about)));

        drawer.addDivider();

        drawer.addItem(new DrawerItem()
                        .setId(12)//exit
                        .setImage(ContextCompat.getDrawable(this, R.drawable.exit))
                        .setTextPrimary(getString(R.string.menu_exit)));

        drawer.setOnItemClickListener(new DrawerItem.OnItemClickListener() {
            @Override
            public void onClick(DrawerItem drawerItem, long id, int position) {
                //drawer.selectItem(3);
                drawerLayout.closeDrawer(drawer);

                switch((int)id){
                    case 1000:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.GENERAL);
                        break;
                    case 2:
                        LaunchPreferenceScreen(MainPreferenceActivity.PreferenceConstants.PERFORMANCE);
                        break;
                    case 11:
                        Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                        startActivity(faqtivity);
                        break;
                    case 13://about
                        new MaterialDialog.Builder(GpsMainActivity.this)
                                .title(R.string.menu_about)
                                .content(getString(R.string.appAbout)+BuildConfig.VERSION_NAME)
                                .positiveText(R.string.ok)
                                .show();
                        break;
                    /*case 14://logout
                        logout();
                        break;*/
                    case 12: //exit
                        EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
                        finish();
                        break;
                }
            }
        });

        ImageButton helpButton = (ImageButton) findViewById(R.id.imgHelp);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                startActivity(faqtivity);
            }
        });
    }

    public void ToggleDrawer(){
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawerLayout.isDrawerOpen(Gravity.LEFT)){
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
        else {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    /**
     * Provides a connection to the GPS Logging Service
     */
    GpsLoggingService loggingService;
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            tracer.debug("Disconnected from GPSLoggingService from MainActivity");
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            tracer.debug("Connected to GPSLoggingService from MainActivity");
            loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
            updateLoc(null);
        }
    };


    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService() {
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        //serviceIntent.putExtra("availabilityState",GetUserSelectedNavigationItem());
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void StopAndUnbindServiceIfRequired() {
        if (Session.isBoundToService()) {

            try {
                unbindService(gpsServiceConnection);
                Session.setBoundToService(false);
            } catch (Exception e) {
                tracer.warn(SessionLogcatAppender.MARKER_INTERNAL, "Could not unbind service", e);
            }
        }

        if (!Session.isStarted()) {
            tracer.debug("Stopping the service");
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                tracer.error("Could not stop the service", e);
            }
        }
    }

    public void OnWaitingForLocation(boolean inProgress) {
        ProgressBar fixBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);

        tracer.debug(inProgress + "");
    }



    @Override
    public void onLocationChanged(Location location) {
        if(AppSettings.firstZooming) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 13));
            AppSettings.firstZooming = false;
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate locationUpdate){
        //DisplayLocationInfo(locationUpdate.location);
        if(AppSettings.firstZooming) {
            Location location = locationUpdate.location;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 13));
            AppSettings.firstZooming = false;
        }

        //add info to the list
        //addNewLoc(new MyInfo(locationUpdate.location));
        Log.d("updateLoc :",locationUpdate.location.toString());
        updateLoc(locationUpdate.location);
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        OnWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        /*if(loggingStatus.loggingStarted){
            showPreferencesSummary();
            setActionButtonStop();
        }
        else {
            setActionButtonStart();
        }*/
        //enableDisableMenuItems();
    }

    private enum IconColorIndicator {
        Good,
        Warning,
        Bad,
        Inactive
    }

    private Toast getToast(String message) {
        return Toast.makeText(this, message, Toast.LENGTH_SHORT);
    }

    /*public void DisplayLocationInfo(Location locationInfo){
        showPreferencesSummary();
    }*/
    private ProgressDialog pDialog;
    private void showDialog() {
        if(Utilities.checkContextIsFinishing(this)) {
            return;
        }
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog!=null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    public class fPoint {
        public double x, y;
        fPoint(double xx, double yy) {
            x=xx;
            y=yy;
        }

        fPoint rotate(double angle){//angle in radian
            double r = Math.sqrt(x*x+y*y);
            double theta = Math.atan2(y,x);
            double xx = x + r*Math.cos(angle+theta);
            double yy = y + r*Math.sin(angle+theta);
            return new fPoint(xx, yy);
        }
    }

    long firstMarkerTime=-1, lastMarkerTime=-1;
    public void updateLoc(Location aLoc){
        if(loggingService == null) return;
        if(aLoc == null){//add all locations
            //add markers for new locations
            int s = loggingService.newLocationsList.size();
            if(s>0) {
                firstMarkerTime = loggingService.newLocationsList.get(0).getTime();
                lastMarkerTime = loggingService.newLocationsList.get(s - 1).getTime();
            }
            for(Location loc: loggingService.newLocationsList){
                addLocationMarker(loc);
            }
        } else {
            addLocationMarker(aLoc);
            if(firstMarkerTime==-1) {
                firstMarkerTime = aLoc.getTime();
            }
            lastMarkerTime = aLoc.getTime();
        }
    }

    private void addLocationMarker(Location loc) {
        LatLng driverPosition = new LatLng(loc.getLatitude(), loc.getLongitude());

        BitmapDescriptor bmp;
        float spd = loc.getSpeed();
        if(loc.hasBearing()) {
            if (spd < 1.0) {//3.6km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_red);
            } else if (spd >= 1 && spd < 6) {//21km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_orange);
            } else if (spd >= 6 && spd < 12) {//43km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_cyan);
            } else //if(spd>=12 && spd<20)
            {//72km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_green);
            }
        }else{
            if (spd < 1.0) {//3.6km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.circle_red);
            } else if (spd >= 1 && spd < 6) {//21km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.circle_orange);
            } else if (spd >= 6 && spd < 12) {//43km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.circle_cyan);
            } else //if(spd>=12 && spd<20)
            {//72km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.circle_grn);
            }
        }
        MarkerOptions markerOptions = new MarkerOptions()
                .position(driverPosition)
                .icon(bmp)
                .title("")
                .flat(true)
                //.snippet(driverInfo[i].phone)
                //.anchor(0.5f, 0.5f)
                //.draggable(true);
                .rotation(Math.round(loc.getBearing()))
                ;
        Marker aMarker = googleMap.addMarker(markerOptions);
        locationMarkers.add(aMarker);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.action_menu, menu);
        // Locate MenuItem with ShareActionProvider
        //MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch and store ShareActionProvider
        //mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        // Return true to display menu
        return true;
    }

    public Bitmap drawMultilineTextToBitmap(Context gContext, Bitmap bitmap, String gText) {
        // prepare canvas
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        //Bitmap bitmap = BitmapFactory.decodeResource(resources, gResId);

        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);

        // new antialiased Paint
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(61, 61, 61));
        // text size in pixels
        paint.setTextSize((int) (20 * scale));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        //canvas.drawText("This is", 100, 100, paint);
        //canvas.drawText("multi-line", 100, 150, paint);
        //canvas.drawText("text", 100, 200, paint);

        // set text width to canvas width minus 16dp padding
        int textWidth = canvas.getWidth() - (int) (16 * scale);

        // init StaticLayout for text
        StaticLayout textLayout = new StaticLayout(
                gText, paint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        // get height of multiline text
        int textHeight = textLayout.getHeight();

        // get position of text's top left corner
        float x = (bitmap.getWidth() - textWidth)/2;
        float y = bitmap.getHeight() - textHeight;

        // draw text to the Canvas center
        canvas.save();
        canvas.translate(x, y);
        textLayout.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    public void captureAndShareGMap()
    {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback()
        {
            @Override
            public void onSnapshotReady(Bitmap snapshot1)
            {
                // TODO Auto-generated method stub
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastMarkerTime);
                String eTime = sdf.format(calendar.getTime());
                calendar.setTimeInMillis(firstMarkerTime);
                String fTime = sdf.format(calendar.getTime());
                sdf.applyPattern("dd MMM,yyyy");
                String fDate = sdf.format(calendar.getTime());
                String dateString = fDate+" "+fTime+"-"+eTime;

                Bitmap snapshot = drawMultilineTextToBitmap(GpsMainActivity.this, snapshot1, dateString);
                //bitmap = snapshot;
                String filePath = System.currentTimeMillis() + ".jpeg";
                filePath = Environment.getExternalStorageDirectory().toString() + "/" + filePath;
                try
                {
                    File imageFile = new File(filePath);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    // Write the string to the file
                    snapshot.compress(Bitmap.CompressFormat.JPEG, 90, fout);
                    fout.flush();
                    fout.close();
                    //Toast.makeText(GpsMainActivity.this, "Stored in: " + filePath, Toast.LENGTH_LONG).show();
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));
                    shareIntent.setType("image/jpeg");
                    startActivity(Intent.createChooser(shareIntent, "Sahre..."));

                }
                catch (FileNotFoundException e)
                {
                    // TODO Auto-generated catch block
                    Log.d("ImageCapture", "FileNotFoundException");
                    Log.d("ImageCapture", e.getMessage());
                    filePath = "";
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    Log.d("ImageCapture", "IOException");
                    Log.d("ImageCapture", e.getMessage());
                    filePath = "";
                }

                //openShareImageDialog(filePath);
            }
        };

        googleMap.snapshot(callback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                captureAndShareGMap();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
