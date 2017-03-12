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

package com.mjhram.geodata.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mjhram.geodata.R;
import com.mjhram.geodata.common.AppSettings;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class GeneralSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener
{

    //int aboutClickCounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);
        Preference prefListeners = (Preference)findPreference("language");
        prefListeners.setOnPreferenceClickListener(this);

        /*Preference profileListeners = findPreference("user_profile");
        profileListeners.setOnPreferenceClickListener(this);

        Preference aboutInfo = findPreference("about_version_info");
        try {

            aboutInfo.setTitle(R.string.app_name+" version " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            //aboutInfo.setOnPreferenceClickListener(this);
        }
        catch (PackageManager.NameNotFoundException e) {
        }*/
    }

    private int getChosenLangIdx() {
        String lang=AppSettings.getChosenLanguage();
        String[] languageArray = this.getResources().getStringArray(R.array.languages_code);
        for(int k=0; k<languageArray.length;k++){
            if(languageArray[k].equals(lang)){
                return k;
            }
        }
        return -1;
    }

    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equalsIgnoreCase("language")){
            /*ArrayList<Integer> chosenIndices = new ArrayList<Integer>();
            final List<String> defaultListeners = AppSettings.GetDefaultListeners();

            for(String chosenListener : AppSettings.getChosenListeners()){
                chosenIndices.add(defaultListeners.indexOf(chosenListener));
            }*/

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.pref_language_title)
                    .items(R.array.languages)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            //set locale
                            int languageIdx = dialog.getSelectedIndex();
                            String[] languageArray = getResources().getStringArray(R.array.languages_code);
                            String tmp = "Language:" + languageArray[languageIdx];
                            //AppSettings.changeLang(GeneralSettingsFragment.this.getActivity(), languageArray[languageIdx]);
                            Log.d("General-Pref", tmp);
                        }
                    })
                    .itemsCallbackSingleChoice(getChosenLangIdx(), new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        //public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                            String[] languageArray = getResources().getStringArray(R.array.languages_code);
                            AppSettings.changeLang(GeneralSettingsFragment.this.getActivity(), languageArray[which]);
                            AppSettings.setChosenLanguage(languageArray[which]);
                            //currentLanguage = LocaleUtils.updateLocaleIfNeeded(GeneralSettingsFragment.this.getActivity(), false, currentLanguage);
                            return true;
                        }
                    }).show();
            return true;
        } /*else if(preference.getKey().equalsIgnoreCase("user_profile")){
            startActivity(new Intent(GeneralSettingsFragment.this.getActivity(), ProfileActivity.class));
        }*/
        return false;
    }
}
