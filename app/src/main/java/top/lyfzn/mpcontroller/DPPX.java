package top.lyfzn.mpcontroller;

import android.app.Activity;
import android.util.DisplayMetrics;

public class DPPX {
    public static int dp2px(int dp,Activity activity){
        DisplayMetrics displayMetrics=new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (dp*displayMetrics.densityDpi)/displayMetrics.DENSITY_DEFAULT;
    }
    public static int px2dp(int px,Activity activity){
        DisplayMetrics displayMetrics=new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return px*displayMetrics.DENSITY_DEFAULT/displayMetrics.densityDpi;
    }
}
