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

package com.mjhram.geodata.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mjhram.geodata.R;
import com.mjhram.geodata.common.AppSettings;
import com.mjhram.geodata.common.EventBusHook;
import com.mjhram.geodata.common.Session;
import com.mjhram.geodata.common.Utilities;
import com.mjhram.geodata.common.events.CommandEvents;
import com.mjhram.geodata.common.events.ServiceEvents;

import de.greenrobot.event.EventBus;


/**
 * Common class for communicating with the parent for the
 * GpsViewCallbacks
 */
public abstract class GenericViewFragment extends AppCompatActivity/*ActionBarActivity*/ {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void RegisterEventBus() {
        EventBus.getDefault().register(this);
    }

    private void UnregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RegisterEventBus();
    }

    @Override
    protected void onPause() {
        UnregisterEventBus();
        super.onPause();
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationServicesUnavailable locationServicesUnavailable){
        new MaterialDialog.Builder(this)
                //.title("Location services unavailable")
                .content(R.string.gpsprovider_unavailable)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(settingsIntent);
                    }
                })
                .show();
    }


    public void RequestToggleLogging(){

        if(Session.isStarted()){
            ToggleLogging();
            return;
        }

        if(AppSettings.shouldCreateCustomFile()  && AppSettings.shouldAskCustomFileNameEachTime()){

            MaterialDialog alertDialog = new MaterialDialog.Builder(this)
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.alertview, true)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String chosenFileName = AppSettings.getCustomFileName();
                            EditText userInput = (EditText) dialog.getCustomView().findViewById(R.id.alert_user_input);

                            if (!Utilities.IsNullOrEmpty(userInput.getText().toString()) && !userInput.getText().toString().equalsIgnoreCase(chosenFileName)) {
                                AppSettings.setCustomFileName(userInput.getText().toString());
                            }
                            ToggleLogging();
                        }
                    })
                    .keyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                ToggleLogging();
                                dialog.dismiss();
                            }
                            return true;
                        }
                    })
                    .build();

            EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
            userInput.setText(AppSettings.getCustomFileName());
            TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
            tvMessage.setText(R.string.new_file_custom_message);
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();

        }
        else {
            ToggleLogging();
        }
    }

    public void ToggleLogging(){
        EventBus.getDefault().post(new CommandEvents.RequestToggle());
    }


}
