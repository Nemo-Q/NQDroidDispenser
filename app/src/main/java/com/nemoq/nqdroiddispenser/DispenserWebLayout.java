package com.nemoq.nqdroiddispenser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;


/**
 * Created by Martin Backudd on 2015-10-30. Logic for interacting with the Javascripts on the dispenser through WebView. Swiping top right of the screen pulls out settings menu, 2244 to enter pin for accessing the settings.
 */
public class DispenserWebLayout extends RelativeLayout implements View.OnClickListener, WebView.OnTouchListener{


    private WebView webView;

    private ImageButton settingsButton;

    private boolean initialPageLoad = true;

    private static float SCALE_MULTIPLIER = 1;

    public DispenserWebLayout(Context context) {
        super(context);
        init(context);

    }

    public DispenserWebLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DispenserWebLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
       inflate(context, R.layout.dispenser_ui, this);
    }


    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void showToast(String toast) {
            Log.d("javascript:", toast);
        }

    }


    private class DispenserWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override //TODO STOP SPINNER
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (initialPageLoad) {
                final ImageView splashScreen = (ImageView) findViewById(R.id.splashImageView);
                Animation fadeOutSplash = AnimationUtils.loadAnimation(getContext(), R.anim.abc_fade_out);

                fadeOutSplash.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        splashScreen.setVisibility(INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                splashScreen.setAnimation(fadeOutSplash);
                splashScreen.startAnimation(fadeOutSplash);
                initialPageLoad = false;

            }

        }


        @Override //TODO LOADING SPINNER OR LIKEWISE
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

    }

    private class ChromeWebClient extends WebChromeClient{

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
        }
    }



    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        webView = (WebView)findViewById(R.id.dispenserWebView);


        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new DispenserWebViewClient());

        webView.setOnTouchListener(this);
        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("Chromium Msg", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });


        loadWebFromPreferences();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.broadcast_address_changed));
        intentFilter.addAction(getResources().getString(R.string.broadcast_view_settings_changed));

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(getResources().getString(R.string.broadcast_address_changed))) {

                    loadWebFromPreferences();
                }
                else if (intent.getAction().equals(getResources().getString(R.string.broadcast_view_settings_changed))){
                    reloadCurrentUrl();
                }
            }
        }, intentFilter);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View settingsButtonView) {

                showPinDialog();
                settingsButtonView.setEnabled(false);

            }
        });

    }


    public void loadWebFromPreferences(){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String address = sharedPreferences.getString(getResources().getString(R.string.pref_key_web_address), "");

        webView.loadUrl(address);

    }

    private void reloadCurrentUrl(){

        webView.setInitialScale(getInitialScale());
        webView.loadUrl(webView.getUrl());

    }


    private int getInitialScale(){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SCALE_MULTIPLIER = Float.parseFloat(sharedPreferences.getString(getResources().getString(R.string.pref_key_scale_multiplier), "1"));

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                return (int)(200 * SCALE_MULTIPLIER);
            case DisplayMetrics.DENSITY_MEDIUM:
                return (int)(100 * SCALE_MULTIPLIER);
            case DisplayMetrics.DENSITY_LOW:
                return (int)(50 * SCALE_MULTIPLIER);
            default:
                return (int)(100 * SCALE_MULTIPLIER);
        }
    }



    float historicalX;
    float historicalY;
    boolean slidingButton = false;


    @Override
    public boolean onTouch(View v, MotionEvent event) {


    //Intercept touch events
        if (event.getY() < this.getMeasuredHeight()/8) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                historicalX = event.getX();

            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                historicalX = 0;


            } else if (event.getAction() == MotionEvent.ACTION_MOVE){
                //Settings button logic
                if (!slidingButton) {


                    if ((historicalX >= settingsButton.getX()) && (historicalX - event.getRawX() > 20)) {




                        Animation slideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in);

                        slideIn.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                slidingButton = true;
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                settingsButton.setVisibility(View.INVISIBLE);
                                slidingButton = false;
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        settingsButton.startAnimation(slideIn);
                        settingsButton.setVisibility(View.VISIBLE);

                    }
                }
            }
        }



        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            historicalX = event.getX();
            historicalY = event.getY();

            View ripple = findViewById(R.id.rippleView);
            ripple.setX(event.getRawX()-ripple.getMeasuredWidth()/2 + 2 );
            ripple.setY(event.getRawY()-ripple.getMeasuredHeight()/2 - 4);
        }
        else if (event.getAction() == MotionEvent.ACTION_UP){



            ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());

            if ((viewConfiguration.getScaledTouchSlop() > Math.abs(event.getX() - historicalX)) && viewConfiguration.getScaledTouchSlop() > (Math.abs(event.getY()-historicalY) ))
            {
                View ripple = findViewById(R.id.rippleView);

                ripple.setAlpha(0.7f);
                Animation rippleAnim = AnimationUtils.loadAnimation(getContext(), R.anim.circle_ripple);
                ripple.setAnimation(rippleAnim);
                ripple.startAnimation(rippleAnim);
            }

        }




        return super.onTouchEvent(event);
    }


    private void showPinDialog(){


        View.inflate(getContext(), R.layout.pin_dialog, this);


        EditText pinField = (EditText) findViewById(R.id.pinTextField);
        ViewGroup pinFieldParentView = (ViewGroup)pinField.getParent();
        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.abc_fade_in);
        pinFieldParentView.startAnimation(fadeIn);

        for (int i = 0;i<10;i++){

            int buttonId = getResources().getIdentifier("button" + String.valueOf(i), "id", getContext().getPackageName());

            final Button button = (Button) findViewById(buttonId);
            button.setOnClickListener(this);

        }


        final ImageButton dismissButton = (ImageButton) findViewById(R.id.buttonDismissPin);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogFinish(false);
            }
        });

        final ImageButton eraseButton = (ImageButton) findViewById(R.id.eraseButton);
        eraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText pinField = (EditText) findViewById(R.id.pinTextField);
                pinField.getText().delete(pinField.length()-1,pinField.length());
            }
        });





    }
    private void dialogFinish(final boolean pinOk){

        final EditText pinField = (EditText) findViewById(R.id.pinTextField);

        Animation fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.abc_fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                ViewGroup viewGroupDialog = (ViewGroup) pinField.getParent();
                viewGroupDialog.removeAllViews();

                if (pinOk)
                    showSettings();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        View parent = (ViewGroup)pinField.getParent();

        parent.startAnimation(fadeOut);
        ImageButton settingsButton = (ImageButton)findViewById(R.id.settingsButton);
        settingsButton.setEnabled(true);
        pinTries = 0;


    }

    int pinTries = 0;

    @Override
    public void onClick(View v) {


        EditText pinField = (EditText) findViewById(R.id.pinTextField);
        pinField.append(v.getTag().toString());

        if (pinField.getText().toString().length() > 4){
            pinField.setText(v.getTag().toString());



        }
        else if(pinField.getText().toString().length() == 4) {
            pinTries ++;
            if (PinCodeManager.getInstance(getContext()).checkPin(pinField.getText().toString())) {

                dialogFinish(true);

            }
            else if(pinTries == getContext().getResources().getInteger(R.integer.number_pin_tries)){
                dialogFinish(false);
            }
            else {

                Animation pinShake = AnimationUtils.loadAnimation(getContext(), R.anim.pin_shake);
                pinField.startAnimation(pinShake);


            }

        }



    }



    //Start the settings activity in the DispenserActivity
    private void showSettings(){
        DispenserActivity dispenserActivity = (DispenserActivity)getContext();
        dispenserActivity.showSettings();
    }

}
