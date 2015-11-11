package com.nemoq.nqdroiddispenser.DummyClasses;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by Martin Backudd on 2015-11-09. Puts the viewgroup as a dummy overlay over the top of the screen, intercepting the touchevents thus blocking the notification bar.
 */
public class DummyViewGroup extends ViewGroup{

    private static DummyViewGroup dummyViewGroup;
    private static Context ctx;

    public static synchronized DummyViewGroup getInstance(Context context){


        if (dummyViewGroup == null){

            dummyViewGroup = new DummyViewGroup(context);
        }

        return dummyViewGroup;


    }



    public DummyViewGroup(Context context) {
        super(context);
        ctx = context;
    }

    public DummyViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ctx = context;
    }

    public DummyViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;
    }

    public void removeDummyOverlay(){

        WindowManager manager = ((WindowManager) ctx.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));

        if (dummyViewGroup.getParent() != null) {
            manager.removeView(dummyViewGroup);
        }

    }

    public void createDummyOverlay(){



            if (dummyViewGroup.getParent() == null) {
                WindowManager manager = ((WindowManager) ctx.getApplicationContext()
                        .getSystemService(Context.WINDOW_SERVICE));

                WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
                localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                localLayoutParams.gravity = Gravity.TOP;
                localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                localLayoutParams.height = (int) (35 * ctx.getResources()
                        .getDisplayMetrics().scaledDensity);
                localLayoutParams.format = PixelFormat.TRANSPARENT;
                manager.addView(dummyViewGroup, localLayoutParams);
            }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
    }
}
