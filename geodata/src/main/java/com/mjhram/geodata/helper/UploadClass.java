package com.mjhram.geodata.helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mjhram.geodata.common.AppSettings;
import com.mjhram.geodata.common.MyInfo;
import com.mjhram.geodata.common.Utilities;
import com.mjhram.geodata.common.events.ServiceEvents;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * Created by mohammad.haider on 10/8/2015.
 */
public class UploadClass {
    private ProgressDialog pDialog;
    private Context cx;
    private static final String URL_acceptTRequest = Constants.SERVER_URL+"/acceptTRequest.php";
    private static final String URL_getDriverState = Constants.SERVER_URL+"/getDriverState.php";
    private static final String URL_updateTaxiLocation = Constants.SERVER_URL+"/addLocation.php";
    private static final String URL_getRequests = Constants.SERVER_URL+"/getRequests.php";
    private static final String URL_verifyPhone = Constants.SERVER_URL+"/verifyPhone.php";
    private static final String URL_getUserProfile = Constants.SERVER_URL + "/getuser.php";
    private static final String URL_forgotPassword = Constants.SERVER_URL+"/forgotpassword.php?action=password";

    private static final String TAG = UploadClass.class.getSimpleName();
    private phpErrorMessages phpErrorMsgs;

    public UploadClass(Context theCx) {
        phpErrorMsgs = AppSettings.getInstance().getPhpErrorMsg();
        cx = theCx;
        pDialog = new ProgressDialog(cx);
        pDialog.setCancelable(false);
    }

    static public double prevLat=0, prevLng=0;
    static public long prevFix=0;
    static public boolean firstTime = true;

    public void checkLogin() {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        pDialog.setMessage("Logging in ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_LOGIN, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // user successfully logged in
                        String uid = jObj.getString("uid");
                        AppSettings.setLogin(true, uid);
                    } else {
                        // Error in login. Get the error message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        String errorMsg = AppSettings.getInstance().getPhpErrorMsg().msgMap.get(errorno);
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "login");
                return params;
            }

        };

        // Adding request to request queue
        AppSettings tmp = AppSettings.getInstance();
        tmp.addToRequestQueue(strReq, tag_string_req);
    }

    static public void updateRegId(final String userId, final String regId) {
        String tag_string_req = "regId_update";

        //pDialog.setMessage(cx.getString(R.string.gpsMainDlgMsgUpdating));
        //showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_UpdateRegId, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(AppSettings.TAG, "update reg id Response: " + response);
                //hideDialog();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(AppSettings.TAG, "Update Error: " + error.getMessage());
                //Toast.makeText(cx,
                //        error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "updateRegId");
                params.put("userId", userId);
                params.put("regId", regId);
                return params;
            }

        };

        // Adding request to request queue
        AppSettings tmp = AppSettings.getInstance();
        tmp.addToRequestQueue(strReq, tag_string_req);
    }

    public static ArrayList<Location> locationsBuffer=null;
    private static final int locationExpiryTime = 30000;//30 sec
    private static final int maxLocBuffer = 10;
    static public void addLoc2buffer(Location loc){
        if(locationsBuffer == null){
            locationsBuffer = new ArrayList<>();
        }
        //remove old locations
        long tmpTime = loc.getTime();
        for (Location tmpLoc:  locationsBuffer) {
            if(tmpTime-tmpLoc.getTime() > locationExpiryTime){
                locationsBuffer.remove(tmpLoc);
            }
        }
        if(locationsBuffer.size()>=maxLocBuffer) {
            locationsBuffer.remove(0);
        }
        locationsBuffer.add(loc);
    }

    public static Location getFinalLocation(Location lastLoc){
        Location loc = new Location(lastLoc);
        //do averaging or clustering:
        int bufSize = locationsBuffer.size();
        float initialBearing=0, initialSpeed=0, initialAccuracy=0;
        int nHasBearing=0;
        for (Location tmpLoc:  locationsBuffer) {
            if(tmpLoc.hasBearing()) {
                initialBearing += tmpLoc.getBearing();
                nHasBearing ++;
            }
            initialSpeed += tmpLoc.getSpeed();
            initialAccuracy += tmpLoc.getAccuracy();
        }
        if(nHasBearing!=0) {
            initialBearing /= nHasBearing;
        }
        initialSpeed /= bufSize;
        initialAccuracy /= bufSize;

        for (Location tmpLoc:  locationsBuffer) {
            //IIR filter y=.3x+.7y
            if(tmpLoc.hasBearing()) {
                initialBearing = .3f * tmpLoc.getBearing() + .7f * initialBearing;
            }
            initialSpeed = .3f*tmpLoc.getSpeed()+.7f*initialSpeed;
            initialAccuracy = .3f*tmpLoc.getAccuracy()+.7f*initialAccuracy;
        }
        if(nHasBearing == 0) {
            loc.removeBearing();
        } else {
            loc.setBearing(initialBearing);
        }

        loc.setSpeed(initialSpeed);
        loc.setAccuracy(initialAccuracy);

        locationsBuffer.clear();
        return loc;
    }

    public static void updateLoc(final MyInfo info) {
        String tag_string_req = "updateLoc";
        final boolean avState = info.updateStateOnly;
        System.out.print(avState);

        EventBus.getDefault().post(new ServiceEvents.LocationUpdate(info.loc));

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_updateTaxiLocation, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "updateLoc Response: " + response);
                //hideDialog();
                //check if uid is returned, then use it.
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String uid = jObj.getString("uid");
                        if(!uid.equals(AppSettings.getUid()) && !uid.contentEquals("-1")) {
                            AppSettings.setLogin(true, uid);
                        }
                        String tripid = jObj.getString("tripid");
                        if(!tripid.contentEquals("-1")){
                            AppSettings.setTripId(tripid);
                        }

                    } else {
                        // Error in login. Get the error message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        //String errorMsg = AppSettings.getInstance().getPhpErrorMsg().msgMap.get(errorno);
                        //Toast.makeText(this.cx,
                        //        errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "updateLoc Error: " + error.getMessage());
                //Toast.makeText(cx,
                        //error.getMessage(), Toast.LENGTH_LONG).show();
                //hideDialog();
                //EventBus.getDefault().post(new ServiceEvents.CancelTRequests(error.getMessage()));
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                String uid = AppSettings.getUid();
                params.put("uid", uid);
                params.put("tripid", AppSettings.getTripId());
                params.put("tag", "addLocation");
                params.put("lat", Double.toString(info.loc.getLatitude()));
                params.put("long", Double.toString(info.loc.getLongitude()));
                params.put("speed", Double.toString(info.loc.getSpeed()));
                params.put("bearing", Double.toString(info.loc.getBearing()));
                params.put("accuracy", Double.toString(info.loc.getAccuracy()));
                params.put("fixtime", Double.toString(info.loc.getTime()));
                String hasInfo ="";
                // accuracy, altitude, bearing, speed
                hasInfo += info.loc.hasAccuracy()?"1":"0";
                hasInfo += info.loc.hasAltitude()?"1":"0";
                hasInfo += info.loc.hasBearing()?"1":"0";
                hasInfo += info.loc.hasSpeed()?"1":"0";
                params.put("hasInfo", hasInfo);
                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }


    private void showDialog() {
        if(Utilities.checkContextIsFinishing(cx)) {
            return;
        }
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog!=null && pDialog.isShowing())
            pDialog.dismiss();
    }
}
