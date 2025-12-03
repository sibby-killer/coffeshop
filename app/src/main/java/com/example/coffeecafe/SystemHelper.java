package com.example.coffeecafe;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

public class SystemHelper {
    private Activity activity;

    public void SystemBarHelper(Activity activity) {
        this.activity = activity;
    }

    public SystemHelper(Activity activity) {
        this.activity = activity;
    }

    public void setSystemBars(int statusBarColorRes, int navBarColorRes, boolean darkStatusIcons) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(activity, statusBarColorRes));
            window.setNavigationBarColor(ContextCompat.getColor(activity, navBarColorRes));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = activity.getWindow().getDecorView();
            if (darkStatusIcons) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decor.setSystemUiVisibility(0); // default light icons
            }
        }
    }
}
