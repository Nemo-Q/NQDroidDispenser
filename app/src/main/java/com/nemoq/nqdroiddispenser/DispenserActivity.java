package com.nemoq.nqdroiddispenser;

import com.nemoq.nqdroiddispenser.DummyClasses.DummyViewGroup;
import com.nemoq.nqdroiddispenser.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * Since Android without superuser doesn't allow for disabled navigation bar, workarounds is implemented to achieve this.
 * Back button: Simply overriding it in kiosk mode
 * Home Button: Setting this activity as the device Launcher renders the home button us useless
 * Recent button: When the focus of the activity changes the activity is brought to the front.
 *
 * @see SystemUiHider
 */
public class DispenserActivity extends Activity{


    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private boolean KIOSK_MODE;


    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultPreferences();

        setContentView(R.layout.activity_dispenser);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_close_activity));
        intentFilter.addAction(getString(R.string.broadcast_address_changed));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(getResources().getString(R.string.broadcast_close_activity))) {
                    KIOSK_MODE = false;
                    finish();
                }
            }
        }, intentFilter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        KIOSK_MODE = sharedPreferences.getBoolean(this.getString(R.string.pref_key_kiosk),false);

        if (KIOSK_MODE)
            DummyViewGroup.getInstance(this).createDummyOverlay();


        final View contentView = findViewById(R.id.dispenserWebView);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.hide();
        hideSystemUI();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.

                            if (visible && KIOSK_MODE) {
                                // Schedule a hide().
                                hideSystemUI();
                                delayedHide(200);
                            }
                        }
                    }
                });

    }


    private void setDefaultPreferences(){
        PreferenceManager.setDefaultValues(this, R.xml.preference_connection, false);
        PreferenceManager.setDefaultValues(this, R.xml.preference_general, false);
    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
            hideSystemUI();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void showSettings() {

        KIOSK_MODE = false;
        Intent intent  = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, getResources().getInteger(R.integer.settings_request_code));

    }






    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == getResources().getInteger(R.integer.settings_request_code)){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            KIOSK_MODE = sharedPreferences.getBoolean(this.getString(R.string.pref_key_kiosk),false);
            if(KIOSK_MODE)
                DummyViewGroup.getInstance(this).createDummyOverlay();
            else
                DummyViewGroup.getInstance(this).removeDummyOverlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (KIOSK_MODE)
            bringAppToFront();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mSystemUiHider.hide();
        hideSystemUI();
        if (!hasFocus && KIOSK_MODE)
            bringAppToFront();

    }


    private void bringAppToFront(){
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final List<ActivityManager.AppTask> runningProcs = activityManager.getAppTasks();
            for (int i = 0; i < runningProcs.size(); i++) {

                if (runningProcs.get(i).getTaskInfo().baseIntent.getPackage().equals(this.getApplication().getPackageName())) {

                    runningProcs.get(i).moveToFront();
                }
            }
        }
        else {
            final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

            for (int i = 0; i < recentTasks.size(); i++)
                if (recentTasks.get(i).baseActivity.toShortString().contains(this.getApplication().getPackageName()))
                    activityManager.moveTaskToFront(recentTasks.get(i).id, ActivityManager.MOVE_TASK_WITH_HOME);

        }

    }

    private void launcherAppStart(){

        Intent intent = new Intent(getApplicationContext(), DispenserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        hideSystemUI();
        if (!KIOSK_MODE)
            super.onBackPressed();

    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        View dView = findViewById(R.id.dispenserWebView);
        dView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View dView = findViewById(R.id.dispenserWebView);
        dView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }



}
