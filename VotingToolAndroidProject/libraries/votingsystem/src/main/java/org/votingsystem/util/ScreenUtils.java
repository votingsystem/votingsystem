package org.votingsystem.util;

import android.util.DisplayMetrics;
import android.view.Display;

public class ScreenUtils {

    public static final String TAG = "ScreenUtils";

    //http://stackoverflow.com/questions/15055458/detect-7-inch-and-10-inch-tablet-programmatically
    public static double getDiagonalInches(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        //float scaleFactor = metrics.density;
        //float widthDp = widthPixels / scaleFactor;
        //float heightDp = heightPixels / scaleFactor;
        float widthDpi = metrics.xdpi;
        float heightDpi = metrics.ydpi;
        float widthInches = widthPixels / widthDpi;
        float heightInches = heightPixels / heightDpi;
        double diagonalInches = Math.sqrt((widthInches * widthInches) +
                (heightInches * heightInches));
        return diagonalInches;
    }

}
