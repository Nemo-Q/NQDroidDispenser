package com.nemoq.nqdroiddispenser;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.nemoq.nqdroiddispenser.DummyClasses.DummyActivity;

import java.io.DataOutputStream;
import java.util.List;


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
public class SettingsActivity extends PreferenceActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }


    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Resources res = preference.getContext().getResources();


            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            else if(preference.getKey().equals(res.getString(R.string.pref_key_kiosk))){
                Log.d("Kiosk mode ", stringValue);
            }
            else if(preference.getKey().equals(res.getString(R.string.pref_key_boot))){
                Log.d("Launch app on Boot ", stringValue);
            }
            else if (preference.getKey().equals(res.getString(R.string.pref_key_web_address)) || preference.getKey().equals(res.getString(R.string.pref_key_web_port))){
                String oldValue = preference.getSharedPreferences().getString(preference.getKey(), "");
                preference.setSummary(stringValue);

                if (!oldValue.equals(stringValue)) {
                    Intent intent = new Intent(preference.getContext().getApplicationContext(), DispenserWebLayout.class);
                    intent.setAction(preference.getContext().getString(R.string.broadcast_address_changed));
                    LocalBroadcastManager.getInstance(preference.getContext()).sendBroadcast(intent);

                }
            }
            else if (preference.getKey().equals(res.getString(R.string.pref_key_scale_multiplier))){
                String oldValue = preference.getSharedPreferences().getString(preference.getKey(), "");
                preference.setSummary(stringValue);

                if (!oldValue.equals(stringValue)) {
                    Intent intent = new Intent(preference.getContext().getApplicationContext(), DispenserWebLayout.class);
                    intent.setAction(preference.getContext().getString(R.string.broadcast_view_settings_changed));
                    LocalBroadcastManager.getInstance(preference.getContext()).sendBroadcast(intent);

                }

            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
             return true;

        }
    };


    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        Resources res = preference.getContext().getResources();
        // Trigger the listener immediately with the preference's
        // current value.
        if(preference.getKey().equals(res.getString(R.string.pref_key_kiosk))){

            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {

                    clearLauncherSetting(preference.getContext());

                    return true;
                }
            });


        }
        else if(preference.getKey().equals(res.getString(R.string.pref_key_boot))){

            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));


        }
        else if(preference.getKey().equals(res.getString(R.string.pref_key_close))) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PreferenceActivity preferenceActivity = (PreferenceActivity) preference.getContext();
                    preferenceActivity.finish();
                    return false;
                }
            });

        }
        else if (preference.getKey().equals(res.getString(R.string.pref_key_quit))){

            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {

                    PreferenceActivity preferenceActivity = (PreferenceActivity)preference.getContext();
                    preferenceActivity.finish();

                    Intent intent=new Intent(preference.getContext().getApplicationContext(),DispenserActivity.class);

                    intent.setAction(preference.getContext().getString(R.string.broadcast_close_activity));
                    LocalBroadcastManager.getInstance(preference.getContext()).sendBroadcast(intent);


                    return false;
                    }
                });


        }
        else if (preference.getKey().equals(res.getString(R.string.pref_key_launcher))){

            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {

                    clearLauncherSetting(preference.getContext());

                    return false;
                }
            });
        }
        else if (preference.getKey().equals(res.getString(R.string.pref_key_scale_multiplier))) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), "1"));

        }
        else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

    }



    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ConnectionPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preference_connection, false);

            addPreferencesFromResource(R.xml.preference_connection);
            Resources res = getResources();

            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_web_address)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_web_port)));

        }

    }



    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preference_general, false);
            addPreferencesFromResource(R.xml.preference_general);
            Resources res = getResources();

            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_boot)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_quit)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_close)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_pin)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_launcher)));


        }

    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ViewPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.preference_view, false);
            addPreferencesFromResource(R.xml.preference_view);
            Resources res = getResources();

            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_kiosk)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.pref_key_scale_multiplier)));


        }

    }


    public static void clearLauncherSetting(Context context) {
        PackageManager packageManager = context.getPackageManager();
        packageManager.clearPackagePreferredActivities(context.getPackageName());

        ComponentName componentName = new ComponentName(context, DummyActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

/*    private static void systemUI(boolean enabled){

        try
        {
            Process su;
            su = Runtime.getRuntime().exec("system/xbin/su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("pm " + (enabled ? "disable":"enable" )
                    + " com.android.systemui\n");
            os.writeBytes("exit\n");
            os.flush();

        } catch (Exception e)
        {
            e.printStackTrace();
            throw new SecurityException();
        }




    }*/



}
