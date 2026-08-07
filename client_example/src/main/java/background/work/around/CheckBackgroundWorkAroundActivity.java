package background.work.around;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Context;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.NotificationManager;

import java.util.Locale;

public class CheckBackgroundWorkAroundActivity extends Activity {

    private static final String TARGET_PACKAGE =
            "background.work.around";

    private TextView textView;
    private Button button;

    private boolean isBatteryOptimizationsIgnored() {
    android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
    return pm != null && pm.isIgnoringBatteryOptimizations("background.work.around");
    }

    private boolean isTargetPackageNotificationsEnabled() {
    if (Build.VERSION.SDK_INT < 33) return true;
    try {
        PackageManager pm = getPackageManager();
        int result = pm.checkPermission("android.permission.POST_NOTIFICATIONS", TARGET_PACKAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    } catch (Exception e) {
        return false;
    } }

    private boolean isDndAccessGranted() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(64, 64, 64, 64);

        textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(18f);

        button = new Button(this);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        textParams.bottomMargin = 48;

        layout.addView(textView, textParams);
        layout.addView(button);

        setContentView(layout);

        updateState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        background.work.around.Start.RunService(this);
        updateState();
    }

    private void updateState() {

        if (!isPackageInstalled()) {

            textView.setText(t(
                    "The app needs to run in the background without restrictions to function effectively. To ensure this, install BackgroundWorkAround and launch it.",
                    "Приложению нужно работать в фоне без ограничений для эффективной работы. Для этого установите BackgroundWorkAround и запустите его."
            ));

            button.setText(t("Скачать в F-Droid", "Download on F-Droid"));

            button.setOnClickListener(v -> {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://f-droid.org/packages/background.work.around")
                ));
            });

            return;
        }
        

        if (!isPackageEnabled() || !isDndAccessGranted() || !isBatteryOptimizationsIgnored() || !isTargetPackageNotificationsEnabled()) {

            textView.setText(t(
                    "To continue, fully complete BackgroundWorkAround setup",
                    "Чтобы продолжить до конца настройте BackgroundWorkAround"
            ));

            button.setText(t("Open app", "Перейти в приложение"));

            button.setOnClickListener(v -> {

                Intent launchIntent =
                        getPackageManager().getLaunchIntentForPackage(TARGET_PACKAGE);

                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    startActivity(new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + TARGET_PACKAGE)
                    ));
                }
            });

            return;
        }
        
        Intent intent = new Intent(
        CheckBackgroundWorkAroundActivity.this,
        MainActivity.class);
        startActivity(intent);
        finish();
    }

    private String t(String en, String ru) {
        return isRussianSystem() ? ru : en;
    }

    private boolean isRussianSystem() {
        return Locale.getDefault().getLanguage().equals("ru");
    }

    private boolean isPackageInstalled() {
      try {
        PackageInfo pi = getPackageManager().getPackageInfo(TARGET_PACKAGE, PackageManager.GET_PERMISSIONS);
        String[] permissions = pi.requestedPermissions;
        if (permissions != null) {
            for (String permission : permissions) {
                if ("android.permission.INTERNET".equals(permission)) {
                    return false;
                }
            }
        }
        
        return true;
        
      } catch (Throwable e) {return false; } 
    }

    private boolean isPackageEnabled() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(TARGET_PACKAGE, 0);
            return info.enabled;
        } catch (Exception e) {
            return false;
        }
    }
}
