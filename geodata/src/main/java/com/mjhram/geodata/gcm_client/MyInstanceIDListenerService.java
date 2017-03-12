/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mjhram.geodata.gcm_client;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.mjhram.geodata.common.AppSettings;
import com.mjhram.geodata.helper.UploadClass;

public class MyInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        /**
         * Called if InstanceID token is updated. This may occur if the security of
         * the previous token had been compromised. Note that this is also called
         * when the InstanceID token is initially generated, so this is where
         * you retrieve the token.
         */
        // [START refresh_token]
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);

        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        //Intent intent = new Intent(this, RegistrationIntentService.class);
        //startService(intent);
    }
    // [END refresh_token]

    private void sendRegistrationToServer(String token) {
        //always send to server
        // Add custom implementation, as needed.
        AppSettings.setRegId(token);
        //AppSettings.regId = token;
        if(token != null) {
            AppSettings.online = true;
            if(AppSettings.getUid() != "-1") {
                //UploadClass uc = new UploadClass(null);
                UploadClass.updateRegId(AppSettings.getUid(), AppSettings.getRegId());
                AppSettings.shouldUploadRegId = false;
            } else {
                AppSettings.shouldUploadRegId = true;
            }
        }
    }
}
