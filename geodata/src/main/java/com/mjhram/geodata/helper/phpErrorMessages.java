package com.mjhram.geodata.helper;

import android.content.Context;
import com.mjhram.geodata.R;
import java.util.HashMap;

/**
 * Created by mohammad.haider on 3/24/2016.
 */
public class phpErrorMessages {

    private int msgno[] = {
            //addTrequest://10-
            10,11,12,
            //acceptTrequest://20- UC#2
            20,21,22,23,
            //login_register://30- ok
            30,31,32,33,34, 35,36,37,
            //logTaxiLoc
            40,41,
            //updateRegId->GpsMainActivity
            50,51,
            //updateLoc
            60,61,
            //updateTaxiLocation
            70, 71, 72, 73,
            //updateTRequest-
            80,81,82,83,
            //updateUserInfo-UC#1
            90,91,
            //uploadImage
            100,101,
            //verifyPhone
            110,111,112,113,114,
            //verifyPhone-ConfirmCode
            120,121,122,123,124,
            //isPhoneVerified
            130,131,132
            //forgot password
            ,140,141,142,143

    };
    public HashMap<Integer, String> msgMap;

    public phpErrorMessages(Context cx){
        String[] msgs = cx.getResources().getStringArray(R.array.phpErrorMsgs);
        //int[] msgno = cx.getResources().getIntArray(R.array.phpErrorMsgNos);

        msgMap = new HashMap<Integer, String>();
        for (int i = 0; i < msgno.length; i++) {
            msgMap.put(msgno[i], msgs[i]);
        }
    }
}
