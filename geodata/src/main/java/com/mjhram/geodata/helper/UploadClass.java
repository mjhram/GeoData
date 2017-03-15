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

    /*public void getServerData(final long initial, final long limit, final SQLiteHandler db) {
        // Tag used to cancel the request
        String tag_string_req = "calc";

        pDialog.setMessage("Calc'ing ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_CALC+"?limit="+limit+"&initial="+initial, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        JSONArray data = jObj.getJSONArray("data");
                        int     dataCount = data.length();
                        for(int i=0;i<dataCount;i++){
                            JSONObject c = data.getJSONObject(i);
                            long id = c.getLong("id");
                            String time = c.getString("time");
                            int uid = c.getInt("userid");
                            String lat = c.getString("lat");
                            String lng = c.getString("long");
                            double speed = c.getDouble("speed");
                            double bearing = c.getDouble("bearing");
                            double acc = c.getDouble("accuracy");
                            long fixtime = c.getLong("fixtime");
                            String info = c.getString("hasInfo");

                            db.addData(id, time, uid, lat, lng, speed, bearing, acc, fixtime, info);
                        }
                        Log.d(TAG, "data inserted into sqlite: #" + dataCount);
                        // user successfully logged in
                        if (dataCount < limit) {
                            SQLiteHandler.exportDB(cx);
                        } else {
                            getServerData(initial+limit, limit, db);
                        }

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
                params.put("tag", "calc");
                return params;
            }

        };

        // Adding request to request queue
        AppSettings tmp = AppSettings.getInstance();
        tmp.addToRequestQueue(strReq, tag_string_req);
    }
*/
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
    /*
    public void getUserProfile(final String userId) {
        // Tag used to cancel the request
        String tag_string_req = "updatePassangerState";

        pDialog.setMessage(cx.getString(R.string.uploadDlgMsgUpdatingInfo));
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_getUserProfile, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "getUser Profile Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String tmp;
                        tmp = jObj.getString("requests");
                        if(tmp.equalsIgnoreCase("{}")) {
                            //idle state: no requests
                            //EventBus.getDefault().post(new ServiceEvents.UpdateStateEvent(null));
                        } else {
                            JSONObject res = new JSONObject(tmp);
                            //in a task state
                            // show info: from/to/driver
                            UserInfo user = new UserInfo();
                            user.name = res.getString(Constants.ProfileName);
                            user.email = res.getString(Constants.ProfileEmail);
                            user.phone = res.getString(Constants.ProfilePhone);
                            user.image_id = res.getString(Constants.ProfileImageId);
                            user.licenseState = res.getString(Constants.ProfileLicenseState);

                            user.brand = res.getString(Constants.ProfileCarBrand);
                            user.model = res.getString(Constants.ProfileCarModel);
                            user.make = res.getString(Constants.ProfileCarMake);
                            user.color = res.getString(Constants.ProfileCarColor);
                            user.plate_type = res.getString(Constants.ProfileCarPlateType);
                            user.plateno = res.getString(Constants.ProfileCarPlateNo);
                            user.other = res.getString(Constants.ProfileCarOther);

                            EventBus.getDefault().post(new ServiceEvents.UpdateProfile(user));


                        }
                    } else {
                        //AppSettings.requestId = -1;
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");//Error always false
                        Toast.makeText(cx,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                Log.e(TAG, "getUserProfile Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                //EventBus.getDefault().post(new ServiceEvents.ErrorConnectionEvent());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "getUserProfile");
                params.put("userId", userId);
                params.put("type", "DRV");
                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }

    public void forgotPassword(final String email) {
        // Tag used to cancel the request
        String tag_string_req = "forgotPass";

        pDialog.setMessage("Please wait...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_forgotPassword, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Reset Password response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        //reset linke was sent to your
                        new MaterialDialog.Builder(cx)
                                .title(R.string.app_name)
                                .content(cx.getString(R.string.forgotPasswordReset))
                                .positiveText(R.string.ok)
                                .show();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        String errorMsg = phpErrorMsgs.msgMap.get(errorno);
                        if(errorMsg != null) {
                            Toast.makeText(cx, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Resetting password Failed " + error.getMessage());
                Toast.makeText(cx,
                        "Resseting Password Failed. Try again later", Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "forgotpass");
                params.put("email", email);

                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }

    public void updateUserPhoto(final Bitmap bitmap, final NetworkImageView networkImageViewUser) {
        // Tag used to cancel the request
        final String tag_string = "modifyUserPhoto";
        final String TAG = tag_string;
        final ProgressDialog pDialog = new ProgressDialog(cx);
        pDialog.setCancelable(false);

        pDialog.setMessage(cx.getString(R.string.uploadDlgMsgUpdatingInfo));
        showDialog();

        final String uploadImage = Utilities.getStringImage(bitmap);
        AppSettings.setPhoto(uploadImage);
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_uploadImage, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "updatePhoto Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String newId = jObj.getString("newid");
                        AppSettings.setPhotoId(newId);
                        {
                            //final String IMAGE_URL = "http://developer.android.com/images/training/system-ui.png";
                            ImageLoader mImageLoader = AppSettings.getInstance().getImageLoader();
                            networkImageViewUser.setImageUrl(Constants.URL_downloadUserPhoto+newId, mImageLoader);
                        }
                        //photoImageView.setImageBitmap(bitmap);
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        int errorno = jObj.getInt("error_no");
                        phpErrorMessages phpErrorMsgs = AppSettings.getInstance().getPhpErrorMsg();
                        String errorMsg = phpErrorMsgs.msgMap.get(errorno);
                        Toast.makeText(cx,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "update User Info Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", tag_string);
                params.put("image", uploadImage);
                params.put("uid", AppSettings.getUid());
                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string);
    }

    public void verifyPhoneConfirmCode(final String phone, final String code) {
        // Tag used to cancel the request
        String tag_string_req = "phoneCodeConfirm";

        pDialog.setMessage("Please wait...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_verifyPhone, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Code Confirmation Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        //Confirmation successful
                        AppSettings.setIsPhoneVerified(true);
                        //phone is verified now
                        new MaterialDialog.Builder(cx)
                                .title(R.string.app_name)
                                .content(cx.getString(R.string.profilePhoneVerCodeConfirmed))
                                .positiveText(R.string.ok)
                                .show();

                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        if(errorno == 122) {
                            AppSettings.setIsPhoneVerified(true);
                            new MaterialDialog.Builder(cx)
                                    .title(R.string.app_name)
                                    .content(cx.getString(R.string.profilePhoneVerAlready))
                                    .positiveText(R.string.ok)
                                    .show();
                        } else {
                            String errorMsg = phpErrorMsgs.msgMap.get(errorno);
                            Toast.makeText(cx, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Code Confirmation Error: " + error.getMessage());
                Toast.makeText(cx,
                        "Error in code confirmation. Try again later", Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "verifyPhoneConfirmCode");
                params.put("uid", AppSettings.getUid());
                params.put("verCode", code);
                params.put("phone", phone);

                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }

    public void verifyPhone(final String phone) {
        // Tag used to cancel the request
        String tag_string_req = "phoneVerify";

        pDialog.setMessage("Please wait...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_verifyPhone, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Verify Phone Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        //you will receive SMS with verification code
                        new MaterialDialog.Builder(cx)
                                .title(R.string.app_name)
                                .content(cx.getString(R.string.profilePhoneVerSMS))
                                .positiveText(R.string.ok)
                                .show();

                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        if(errorno == 111) {
                            new MaterialDialog.Builder(cx)
                                    .title(R.string.app_name)
                                    .content(cx.getString(R.string.profilePhoneVerAlready))
                                    .positiveText(R.string.ok)
                                    .show();
                        } else {
                            String errorMsg = phpErrorMsgs.msgMap.get(errorno);

                            Toast.makeText(cx,
                                    errorMsg, Toast.LENGTH_LONG).show();
                            //EventBus.getDefault().post(new ServiceEvents.UpdateDriverStateEvent(null));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Verify Phone Error: " + error.getMessage());
                Toast.makeText(cx,
                        "Error Verifying Phone. Try again later", Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "verifyPhone");
                params.put("uid", AppSettings.getUid());
                params.put("phone", phone);

                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }

    public void updateUserInfo(final String username, final String useremail, final String userphone,
                               final String carbrand, final String carmodel, final String carmake,
                               final String carcolor, final String carplateno, final String carother) {
        // Tag used to cancel the request
        String tag_string = "modifyUserInfo";

        pDialog.setMessage(cx.getString(R.string.uploadDlgMsgUpdatingInfo));
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_updateUserInfo, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "updateTReq Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        AppSettings.setPhone(userphone);
                        AppSettings.setEmail(useremail);
                        AppSettings.setName(username);

                        AppSettings.setCarBrand(carbrand);
                        AppSettings.setCarModel(carmodel);
                        AppSettings.setCarMake(carmake);
                        AppSettings.setCarColor(carcolor);
                        AppSettings.setCarPlateNo(carplateno);
                        AppSettings.setCarOther(carother);
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        String errorMsg = phpErrorMsgs.msgMap.get(errorno);
                        Toast.makeText(cx,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "update User Info Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "updateUserInfo");
                params.put("username", username);
                params.put("useremail", useremail);
                params.put("userphone", userphone);
                params.put("uid", AppSettings.getUid());

                params.put("carbrand", carbrand);
                params.put("carmodel", carmodel);
                params.put("carmake", carmake);
                params.put("carcolor", carcolor);
                params.put("carplateno", carplateno);
                params.put("carother", carother);
                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string);
    }

    public void updateRequests(final int reqId, final GpsMainActivity mainActivity) {
        pDialog.setMessage(mainActivity.getString(R.string.uploadDlgMsgUpdatingRqsts));
        showDialog();
        if(mainActivity.fromMarker != null) {
            mainActivity.fromMarker.remove();
            mainActivity.fromMarker = null;
        }
        if(mainActivity.toMarker != null) {
            mainActivity.toMarker.remove();
            mainActivity.toMarker = null;
        }
        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_getRequests, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(AppSettings.TAG, "getRequests Response: " + response);
                hideDialog();
                //myJson = response;
                mainActivity.showList(response, reqId);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(AppSettings.TAG, "Login Error: " + error.getMessage());
                Toast.makeText(mainActivity.getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
                EventBus.getDefault().post(new ServiceEvents.ErrorConnectionEvent());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "login");
                String uid = AppSettings.getUid();
                params.put("drvId", uid);
                return params;
            }

        };
        AppSettings tmp = AppSettings.getInstance();
        tmp.addToRequestQueue(strReq, "getRequestsTag");
        //1.

    }
    */
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


/*
    public void getDriverState(final String driverId) {
        // Tag used to cancel the request
        String tag_string_req = "updatePassangerState";

        pDialog.setMessage("Updating State ...");
        showDialog();
        //to make sure the log service is started
        EventBus.getDefault().postSticky(new CommandEvents.RequestStartStop(true));

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_getDriverState, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "getDriverState Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String tmp = jObj.getString("stats");
                        String countDrv="";
                        String countPas="";
                        if(!tmp.equalsIgnoreCase("{}")) {
                            JSONObject stats = new JSONObject(tmp);
                            countDrv = stats.getString("Drv");
                            countPas = stats.getString("Pas");
                        }

                        String tmpAnn = jObj.getString("announcements");
                        String annImage = "", annText="", ver="";
                        if(!tmpAnn.equalsIgnoreCase("{}")){
                            JSONObject anns = new JSONObject(tmpAnn);
                            annImage = anns.getString("annimage");
                            annText = anns.getString("anntext");
                            ver = anns.getString("ver");
                        }
                        EventBus.getDefault().post(new ServiceEvents.UpdateAnnouncement(ver, annImage, annText, countDrv, countPas));

                        tmp = jObj.getString("requests");
                        if(tmp.equalsIgnoreCase("{}")) {
                            EventBus.getDefault().post(new ServiceEvents.UpdateDriverStateEvent(null));
                        }

                        else {
                            JSONObject requests = new JSONObject(tmp);
                            //in a task state
                            // show info: from/to/driver
                            JSONObject c = requests;//.getJSONObject(0);
                            TRequestObj treq = new TRequestObj();
                            treq.idx = c.getInt(Constants.RequestsIdx);
                            treq.time = c.getString(Constants.RequestsTime);
                            treq.passangerName = c.getString(Constants.RequestsPassangerName);
                            treq.passengerInfo = c.getString(Constants.RequestsPassengerEmail);
                            treq.passengerPhone = c.getString(Constants.RequestsPassengerPhone);
                            treq.passengerPhotoUrl = Constants.URL_downloadUserPhoto + c.getString(Constants.RequestsPassengerPhotoId);
                            treq.passanger_id = c.getString(Constants.RequestsPassangerId);
                            treq.fromLat = c.getDouble(Constants.RequestsFromLat);
                            treq.fromLong = c.getDouble(Constants.RequestsFromLong);
                            treq.toLat = c.getDouble(Constants.RequestsToLat);
                            treq.toLong = c.getDouble(Constants.RequestsToLong);
                            treq.fromDesc = c.getString(Constants.RequestsFromDesc);
                            treq.toDesc = c.getString(Constants.RequestsToDesc);

                            treq.driverId = driverId;
                            treq.status = c.getString(Constants.RequestsStatus);

                            EventBus.getDefault().post(new ServiceEvents.UpdateDriverStateEvent(treq));
                        }
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        int errorNo = jObj.getInt("error_no");
                        if(errorNo == 1001) {//invalid user
                            EventBus.getDefault().post(new ServiceEvents.forceLogout());
                        }
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(cx,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "getPassangerState Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
                EventBus.getDefault().post(new ServiceEvents.ErrorConnectionEvent());
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "getDriverState");
                params.put("driverId", driverId);
                String tmpLang = cx.getResources().getString(R.string.applanguage);
                params.put("lang", tmpLang);
                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }

    public void acceptTRequest(final TRequestObj treq) {
        // Tag used to cancel the request
        String tag_string_req = "acceptTRequest";

        pDialog.setMessage("Updating Request State...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_acceptTRequest, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "acceptTReq Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        treq.driverId = AppSettings.getUid();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        //String errorMsg = jObj.getString("error_msg");
                        int errorno = jObj.getInt("error_no");
                        if(errorno == 21) {
                            EventBus.getDefault().post(new ServiceEvents.GetDriverStateEvent());
                        } else if(errorno == 131) {
                                new MaterialDialog.Builder(cx)
                                        .title(R.string.app_name)
                                        .content(cx.getString(R.string.profilePhoneIsNotVerified))
                                        .positiveText(R.string.ok)
                                        .show();
                            EventBus.getDefault().post(new ServiceEvents.UpdateDriverStateEvent(null));
                        } else {
                            String errorMsg = phpErrorMsgs.msgMap.get(errorno);

                            Toast.makeText(cx,
                                    errorMsg, Toast.LENGTH_LONG).show();
                            EventBus.getDefault().post(new ServiceEvents.UpdateDriverStateEvent(null));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "accept TRequest Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "acceptTRequest");
                params.put("treqId", Integer.toString(treq.idx));
                params.put("passangerId", treq.passanger_id);
                params.put("drvId", AppSettings.getUid());

                return params;
            }
        };
        // Adding request to request queue
        AppSettings ac = AppSettings.getInstance();
        ac.addToRequestQueue(strReq, tag_string_req);
    }


    public void setTRequestState(final TRequestObj treqObj, final String reqState) {
        String tag_string_req = "treq_update";

        pDialog.setMessage("Updating ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_UpdateTReq, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(AppSettings.TAG, "update TReq Response: " + response);
                hideDialog();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(AppSettings.TAG, "Update Error: " + error.getMessage());
                Toast.makeText(cx,
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "updateTRequestByDriver");
                params.put("state", reqState);
                params.put("passangerId", treqObj.passanger_id);
                params.put("drvId", treqObj.driverId);
                params.put("requestId", Integer.toString(treqObj.idx));
                params.put("requestId", Integer.toString(treqObj.idx));
                return params;
            }

        };

        // Adding request to request queue
        AppSettings tmp = AppSettings.getInstance();
        tmp.addToRequestQueue(strReq, tag_string_req);
    }
    */
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
