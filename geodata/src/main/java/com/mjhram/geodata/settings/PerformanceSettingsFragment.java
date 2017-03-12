package com.mjhram.geodata.settings;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mjhram.geodata.R;
import com.mjhram.geodata.common.AppSettings;

import java.util.*;

public class PerformanceSettingsFragment  extends PreferenceFragment implements Preference.OnPreferenceClickListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_performance);

        Preference prefListeners = (Preference)findPreference("listeners");
        prefListeners.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equalsIgnoreCase("listeners")){

            ArrayList<Integer> chosenIndices = new ArrayList<Integer>();
            final List<String> defaultListeners = AppSettings.GetDefaultListeners();

            for(String chosenListener : AppSettings.getChosenListeners()){
                chosenIndices.add(defaultListeners.indexOf(chosenListener));
            }

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.listeners_title)
                    .items(R.array.listeners)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .itemsCallbackMultiChoice(chosenIndices.toArray(new Integer[0]), new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            AppSettings.setChosenListeners(integers);

                            return true;
                        }
                    }).show();

            return true;
        }

        return false;
    }
}
