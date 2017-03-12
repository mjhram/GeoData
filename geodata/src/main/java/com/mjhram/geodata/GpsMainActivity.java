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
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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
import com.mjhram.geodata.helper.SQLiteHandler;
import com.mjhram.geodata.helper.UploadClass;
import com.mjhram.geodata.views.GenericViewFragment;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
    private Button btnAcceptedTask;

    private int driverState;
    //private CountDownTimer countDownTimer;
    //private TRequestObj selectedTRequest = null;
    private DrawerLayout mDrawerLayout;
    public SQLiteHandler dbhandler;

    public class btnOnClickListener implements View.OnClickListener {
        public void onClick(View v) {//start new trip when start or stop service
            AppSettings.setTripId("-1");
            switch (driverState) {
                case 0://state0 is serviceON, then turn it off
                    EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(false));
                    btnAcceptedTask.setText(getString(R.string.BtnTextTurnOn));

                    driverState = 1;
                    break;
                case 1://state1 is serviceOFF, then turn it on
                    EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
                    btnAcceptedTask.setText(getString(R.string.BtnTextTurnOff));
                    driverState = 0;

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
        dbhandler = new SQLiteHandler(this);
        //Utilities.ConfigureLogbackDirectly(getApplicationContext());
        tracer = LoggerFactory.getLogger(GpsMainActivity.class.getSimpleName());

        loadPresetProperties();

        setContentView(R.layout.activity_gps_main);

        AppSettings.setTripId("-1");// new tripid
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        btnAcceptedTask = (Button) findViewById(R.id.btnAcceptedTask);

        btnAcceptedTask.setOnClickListener(new btnOnClickListener());


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
        if(regId == null || regId.isEmpty()) {
            String token = FirebaseInstanceId.getInstance().getToken();
            AppSettings.setRegId(token);
            regId = token;
        }
        if(AppSettings.shouldUploadRegId && regId != null) {
            AppSettings.shouldUploadRegId = false;
            //UploadClass uc = new UploadClass(null);
            UploadClass.updateRegId(AppSettings.getUid(), regId);
        }
    }

    /*DrawerLayout.DrawerListener myDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerClosed(View drawerView) {
            Toast.makeText(getApplicationContext(), "Drawer Closed", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            Toast.makeText(getApplicationContext(), "Drawer Opened", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            Toast.makeText(getApplicationContext(), "Drawer Slide:"+slideOffset, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            String state;
            switch(newState){
                case DrawerLayout.STATE_IDLE:
                    state = "STATE_IDLE";
                    break;
                case DrawerLayout.STATE_DRAGGING:
                    state = "STATE_DRAGGING";
                    break;
                case DrawerLayout.STATE_SETTLING:
                    state = "STATE_SETTLING";
                    break;
                default:
                    state = "unknown!";
            }
            Toast.makeText(getApplicationContext(), "Drawer Changed:"+state, Toast.LENGTH_LONG).show();
        }
    };*/

    @Override
    public void onMapReady(GoogleMap map) {
        map.setMyLocationEnabled(true);
        //map.setOnMyLocationButtonClickListener(this);
        googleMap = map;

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
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }
    /*private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void UnregisterEventBus(){
        try {
        EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }*/

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

    /*private void WriteToFile(MyInfo info) {
        Session.setAddNewTrackSegment(false);
        try {
            tracer.debug("Calling file writers");
            //FileLoggerFactory.Write(getApplicationContext(), info);
            UploadClass.updateLoc(info);
        }
        catch(Exception e){
            tracer.error(getString(R.string.could_not_write_to_file), e);
        }
    }*/

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

            //Deprecated in Lollipop but required if targeting 4.x
            //SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.gps_main_views, R.layout.spinner_dropdown_item);
            //getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            //getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, this);
            //getSupportActionBar().setSelectedNavigationItem(GetUserSelectedNavigationItem());

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
                /*if(drawerView.equals(relativeLayout_rightDrawer)) {
                    UploadClass uploadClass = new UploadClass(GpsMainActivity.this);
                    uploadClass.updateRequests(-1, GpsMainActivity.this);
                    //to make sure the log service is started
                    EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));
                }*/
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

        /*drawer.addItem(new DrawerItem()
                .setId(14)//logout
                .setImage(ContextCompat.getDrawable(this, R.drawable.logout))
                .setTextPrimary(getString(R.string.menu_logout)));*/

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

        /*rightDrawerButton = (ImageButton) findViewById(R.id.imgRightDrawer);
        rightDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(Gravity.RIGHT);
            }
        });*/

    }

    /*private void logout() {
        EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));
        AppSettings.logout();
        Intent loginActivity = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(loginActivity);
        finish();
    }*/

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
    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            tracer.debug("Disconnected from GPSLoggingService from MainActivity");
            //loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            tracer.debug("Connected to GPSLoggingService from MainActivity");
            //loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
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

    /*@EventBusHook
    public void onEventMainThread(ServiceEvents.forceLogout tmp){
        logout();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenGTS upload){
        tracer.debug("Open GTS Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.opengts_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));

            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.AutoEmail upload){
        tracer.debug("Auto Email Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.autoemail_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.OpenStreetMap upload){
        tracer.debug("OSM Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.osm_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Dropbox upload){
        tracer.debug("Dropbox Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.dropbox_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GDocs upload){
        tracer.debug("GDocs Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.gdocs_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.Ftp upload){
        tracer.debug("FTP Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.autoftp_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));
            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }


    @EventBusHook
    public void onEventMainThread(UploadEvents.OwnCloud upload){
        tracer.debug("OwnCloud Event completed, success: " + upload.success);
        Utilities.HideProgress();

        if(!upload.success){
            tracer.error(getString(R.string.owncloud_setup_title)
                    + "-"
                    + getString(R.string.upload_failure));

            if(userInvokedUpload){
                Utilities.MsgBox(getString(R.string.sorry),getString(R.string.upload_failure), this);
                userInvokedUpload = false;
            }
        }
    }*/

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


    /*
    void setStateToIdle() {
        driverState =0;
        //UploadClass uploadClass = new UploadClass(GpsMainActivity.this);
        //uploadClass.updateRequests(-1, this);
        btnAcceptedTask.setVisibility(View.INVISIBLE);
        relativeLayout_passenger.setVisibility(View.INVISIBLE);
        //btnAccept.setVisibility(View.VISIBLE);//show all task list
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        rightDrawerButton.setEnabled(true);

        reqListView.setVisibility(View.VISIBLE);
        //btnPassangers.setVisibility(View.VISIBLE);

        if(fromMarker != null) {
            fromMarker.remove();
            fromMarker = null;
        }
        if(toMarker != null) {
            toMarker.remove();
            fromMarker = null;
        }
        Session.availabilityState = 0;
    }

    void setStateTo(TRequestObj tRequestObj) {
        selectedTRequest = tRequestObj;
        reqListView.setVisibility(View.INVISIBLE);
        //btnPassangers.setVisibility(View.INVISIBLE);
        //1. driver not assigned yet => neglect it
        if(tRequestObj.status.equalsIgnoreCase("assigned")) {
            btnAcceptedTask.setText(getString(R.string.gpsMainBtnPickedUp));
            //btnAccept.setVisibility(View.INVISIBLE);//hide all tasks list
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            rightDrawerButton.setEnabled(false);

            driverState = 1;
            //onNavigationItemSelected(1, 0);//busy
            //Session.availabilityState = 1;
            EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true)); // just to update notification state
        } else {
            //picked up
            btnAcceptedTask.setText(getString(R.string.gpsMainBtnDone));
            //btnAccept.setVisibility(View.INVISIBLE);//hide all tasks list
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            rightDrawerButton.setEnabled(false);

            driverState = 2;
        }
        Session.availabilityState = 1;
        btnAcceptedTask.setVisibility(View.VISIBLE);
        relativeLayout_passenger.setVisibility(View.VISIBLE);
        textViewPassengerName.setText(selectedTRequest.passangerName);
        textViewPassengerInfo.setText(getString(R.string.uploadDriverInfo) + selectedTRequest.passengerInfo);
        btnPassengerPhone.setText(selectedTRequest.passengerPhone);
        {
            //final String IMAGE_URL = "http://developer.android.com/images/training/system-ui.png";
            ImageLoader mImageLoader = AppSettings.getInstance().getImageLoader();
            networkImageViewPassenger.setImageUrl(tRequestObj.passengerPhotoUrl, mImageLoader);
        }
        //2. driver assigned or passanger picked
        LatLng currentPosition = new LatLng(tRequestObj.fromLat, tRequestObj.fromLong);
        if(fromMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentPosition)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .draggable(true);
            ;
            fromMarker = googleMap.addMarker(markerOptions);
        } else {
            fromMarker.setPosition(currentPosition);
        }

        currentPosition = new LatLng(tRequestObj.toLat, tRequestObj.toLong);
        if (toMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentPosition)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .draggable(false);
            toMarker = googleMap.addMarker(markerOptions);
        } else {
            toMarker.setPosition(currentPosition);
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        {
            builder.include(fromMarker.getPosition());
            builder.include(toMarker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 120; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        googleMap.animateCamera(cu);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.UpdateAnnouncement updateAnnEvent){
        String ver = updateAnnEvent.ver;
        String imageName = updateAnnEvent.annImage;
        String tmpText = updateAnnEvent.annText;
        String countDrv = updateAnnEvent.countOfDrivers;
        String countPas = updateAnnEvent.countOfPassengers;

        if(imageName.isEmpty() && tmpText.isEmpty() && countDrv.isEmpty() && countPas.isEmpty()) {
            relativeLayoutAds.setVisibility(View.GONE);
        } else {
            if(imageName.isEmpty()) {
                networkivAds.setVisibility(View.INVISIBLE);
            } else {
                //networkivAds = (NetworkImageView) findViewById(R.id.networkivAds);
                networkivAds.setVisibility(View.VISIBLE);
                {
                    //final String IMAGE_URL = "http://developer.android.com/images/training/system-ui.png";
                    ImageLoader mImageLoader = AppSettings.getInstance().getImageLoader();
                    String tmp = Constants.URL_ads + imageName;
                    networkivAds.setImageUrl(tmp, mImageLoader);
                }
            }
            if(tmpText.isEmpty() && countDrv.isEmpty() && countPas.isEmpty()) {
                textviewAds.setVisibility(View.GONE);
            } else {
                String s =  tmpText.replaceAll("\\\\n", "\\\n");
                String tmp="";
                if(!(countDrv.isEmpty() && countPas.isEmpty()))  {
                    //tmp = String.format("%s:%s - %s:%s",getResources().getString(R.string.Drivers), countDrv, getResources().getString(R.string.Passengers), countPas);
                    //s += "\n" + tmp;
                    //s += "<br>" + tmp;
                    s="<html>" + s + "</html>";
                    textviewAds.setText(Html.fromHtml(s));
                    textviewAds.setMovementMethod(LinkMovementMethod.getInstance());
                    //Linkify.addLinks(textviewAds,Linkify.ALL);
                }
            }
        }
        //check for version:
        if(ver.equalsIgnoreCase(Constants.ver) == false) {
            // request for update => exit app
            new MaterialDialog.Builder(this)
                    .cancelable(false)
                    .autoDismiss(false)
                    .title(R.string.loginUpdateAppTitle)
                    .content(getString(R.string.loginUpdateApp))
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            dialog.dismiss();
                            EventBus.getDefault().post(new CommandEvents.RequestStartStop(false));

                            String url = "market://details?id=" + getPackageName();//getString(R.string.appGoogleLink);
                            if(AppSettings.getChosenLanguage() == "ar") {
                                url += "&hl=ar";
                            }
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);

                            finish();
                            return;
                        }
                    })
                    .show();
            //finish();
        }
    }
    */

    @EventBusHook
    public void onEventMainThread(ServiceEvents.ErrorConnectionEvent erroConnectionEvent){
        tracer.debug("error getting state");
        btnAcceptedTask.setText(getResources().getString(R.string.gpsMainBtnReconnect));
        //btnAcceptedTask.setVisibility(View.VISIBLE);
        //relativeLayout_passenger.setVisibility(View.INVISIBLE);
        //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        //rightDrawerButton.setEnabled(false);
        //driverState = 20;
    }

    /*@EventBusHook
    public void onEventMainThread(ServiceEvents.GetDriverStateEvent getDrvStateEvent){
        UploadClass uc = new UploadClass(GpsMainActivity.this);
        uc.getDriverState(AppSettings.getUid());
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.UpdateDriverStateEvent updateStateEvent){
        TRequestObj tRequestObj = updateStateEvent.treqObj;
        if(tRequestObj == null) {
            //idle:
            setStateToIdle();
        } else {
            setStateTo(tRequestObj);
        }

    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.RefreshTRequests refreshTRequests){
        //if(AppSettings.firstZooming) {
            int treqid = refreshTRequests.tReqId;
            UploadClass uploadClass = new UploadClass(GpsMainActivity.this);
            uploadClass.updateRequests(-1, this);
        //}
    }*/
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
        addNewLoc(new MyInfo(locationUpdate.location));
        updateLoc();
    }



    /*@EventBusHook
    public void onEventMainThread(ServiceEvents.FileNamed fileNamed){
        //showCurrentFileName(fileNamed.newFileName);
    }*/

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        OnWaitingForLocation(waitingForLocation.waiting);
    }

    /*@EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus){
        if(annotationStatus.annotationWritten){
            //SetAnnotationDone();
        }
        else {
            //SetAnnotationReady();
        }
    }*/

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

    /*private void setActionButtonStart(){
        actionButton.setText(R.string.btn_start_logging);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColor));
        actionButton.setAlpha(0.8f);
    }

    private void setActionButtonStop(){
        actionButton.setText(R.string.btn_stop_logging);
        actionButton.setBackgroundColor(getResources().getColor(R.color.accentColorComplementary));
        actionButton.setAlpha(0.8f);
    }*/

    /*private void showPreferencesSummary() {
        //showCurrentFileName(Session.getCurrentFileName());

        ImageView imgGpx = (ImageView) findViewById(R.id.simpleview_imgGpx);
        ImageView imgKml = (ImageView) findViewById(R.id.simpleview_imgKml);
        ImageView imgCsv = (ImageView) findViewById(R.id.simpleview_imgCsv);
        ImageView imgNmea = (ImageView) findViewById(R.id.simpleview_imgNmea);
        ImageView imgLink = (ImageView) findViewById(R.id.simpleview_imgLink);

        if (AppSettings.shouldLogToGpx()) {

            imgGpx.setVisibility(View.VISIBLE);
        } else {
            imgGpx.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToKml()) {

            imgKml.setVisibility(View.VISIBLE);
        } else {
            imgKml.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToNmea()) {
            imgNmea.setVisibility(View.VISIBLE);
        } else {
            imgNmea.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToPlainText()) {

            imgCsv.setVisibility(View.VISIBLE);
        } else {
            imgCsv.setVisibility(View.GONE);
        }

        if (AppSettings.shouldLogToCustomUrl()) {
            imgLink.setVisibility(View.VISIBLE);
        } else {
            imgLink.setVisibility(View.GONE);
        }

        if (!AppSettings.shouldLogToGpx() && !AppSettings.shouldLogToKml()
                && !AppSettings.shouldLogToPlainText()) {
            showCurrentFileName(null);
        }

    }

    private void showCurrentFileName(String newFileName) {
        TextView txtFilename = (TextView) findViewById(R.id.simpleview_txtfilepath);
        if (newFileName == null || newFileName.length() <= 0) {
            txtFilename.setText("");
            txtFilename.setVisibility(View.INVISIBLE);
            return;
        }

        txtFilename.setVisibility(View.VISIBLE);
        txtFilename.setText(Html.fromHtml("<em>" + AppSettings.getGpsLoggerFolder() + "/<strong><br />" + Session.getCurrentFileName() + "</strong></em>"));

        Utilities.SetFileExplorerLink(txtFilename,
                Html.fromHtml("<em><font color='blue'><u>" + AppSettings.getGpsLoggerFolder() + "</u></font>" + "/<strong><br />" + Session.getCurrentFileName() + "</strong></em>" ),
                AppSettings.getGpsLoggerFolder(),
                this.getApplicationContext());

    }
*/
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

    public void updateLoc() {
        //add markers for new locations
        int newRecords = locationMarkers.size();
        while(newRecords < myinfoList.size()){
            MyInfo info = myinfoList.get(newRecords);
            LatLng driverPosition = new LatLng(info.loc.getLatitude(), info.loc.getLongitude());

            /*fPoint arrowPts[] = {new fPoint(-.90,-1.0), new fPoint(-.55, 0.0),
                    new fPoint(-.90, +1.0), new fPoint(+.90, 0.0)};
            double angle = Math.toRadians(45);//info.loc.getBearing()
            fPoint rotatedPts[] = new fPoint[4];
            for(int k = 0; k < 4; k++) {
                rotatedPts[k] = arrowPts[k].rotate(angle);
            }
            Polygon polygon = googleMap.addPolygon(new PolygonOptions()
                    .add(new LatLng(info.loc.getLatitude()+rotatedPts[0].x, info.loc.getLongitude()+rotatedPts[0].y),
                            new LatLng(info.loc.getLatitude()+rotatedPts[1].x, info.loc.getLongitude()+rotatedPts[1].y),
                            new LatLng(info.loc.getLatitude()+rotatedPts[2].x, info.loc.getLongitude()+rotatedPts[2].y),
                            new LatLng(info.loc.getLatitude()+rotatedPts[3].x, info.loc.getLongitude()+rotatedPts[3].y))
                    .strokeColor(Color.RED)
                    .strokeWidth(1f)
                    .fillColor(Color.BLUE));
            locationMarkers.add(polygon);*/

            BitmapDescriptor bmp;
            float spd = info.loc.getSpeed();
            if(spd <1.0) {//3.6km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_red);
            } else if(spd>=1 && spd<6) {//21km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_orange);
            } else if(spd>=6 && spd<12) {//43km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_cyan);
            } else //if(spd>=12 && spd<20)
            {//72km/h
                bmp = BitmapDescriptorFactory.fromResource(R.drawable.arrowup_green);
            }
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(driverPosition)
                    .icon(bmp)
                    .title("")
                    //.snippet(driverInfo[i].phone)
                    //.anchor(0.5f, 0.5f)
                    //.draggable(true);
                    .rotation(Math.round(info.loc.getBearing()))
                    ;
            Marker aMarker = googleMap.addMarker(markerOptions);
            locationMarkers.add(aMarker);
            newRecords++;
        }

        //check and remove old info
        long now = System.currentTimeMillis();
        for(int k=0; k<myinfoList.size(); k++) {
            MyInfo aInfo = myinfoList.get(k);
            if(aInfo.loc.getTime() < now - 1*3600*1000) { //1hour before now
                myinfoList.remove(k);
                locationMarkers.get(k).remove();
                locationMarkers.remove(k);
                Log.v("geo_tag","location removed");
            } else {
                break;
            }
        }

    }

    static public void addNewLoc(MyInfo info){
        //add new location info
        myinfoList.add(info);
        Log.v("geo_tag","location added");
    }

    /*public void clearMarkers() {
        myinfoList.clear();
        if (locationMarkers != null) {
            for (Marker aMarker : locationMarkers) {
                aMarker.remove();
            }
        }
        locationMarkers.clear();
    }

    public void onGetDataClicked(View v) {
        UploadClass.firstTime = true;
        UploadClass uc = new UploadClass(GpsMainActivity.this);
        long i = 0, step = 100;
        uc.getServerData(i, step, dbhandler);
    }*/

    public void onExportClicked(View v) {
        SQLiteHandler.exportDB(this);
    }

    public void onImportClicked(View v) {
        SQLiteHandler.importDB(this);
    }

    public void onCalculateClicked(View v) {
        pDialog.setMessage("Calc'ing ...");
        showDialog();
        dbhandler.calc();
        hideDialog();
    }
}
