package background.work.around;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class WelcomeActivity extends Activity {

    private static final String PREFS = "prefs";
    private static final String READ_INTRO = "read_intro";
    private TextView text;
    private LinearLayout buttonBox;

    private boolean isBatteryOptimizationsIgnored() {
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean isAppNotificationsEnabled() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }

    private boolean isDndAccessGranted() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    private boolean isEn() { return !Locale.getDefault().getLanguage().equals("ru"); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);
        text = new TextView(this);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setTextSize(16f);
        text.setTextColor(Color.WHITE);
        buttonBox = new LinearLayout(this);
        buttonBox.setOrientation(LinearLayout.VERTICAL);
        buttonBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 64, 0, 0);
        buttonBox.setLayoutParams(boxParams);
        root.addView(text);
        root.addView(buttonBox);
        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void exportFileToDownloads() {
        try {
            java.io.InputStream in = getResources().openRawResource(R.raw.bwa_example);
            java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            String randomUid = java.util.UUID.randomUUID().toString().substring(0, 8);
            String fileName = "BWA_example_" + randomUid + ".zip";
            java.io.File outFile = new java.io.File(downloadsDir, fileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            in.close();
            out.flush();
            out.close();
            android.widget.Toast.makeText(this, (isEn() ? "File saved to Downloads:\n" : "Файл сохранен в Загрузки:\n") + outFile.getName(), android.widget.Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            android.widget.Toast.makeText(this, (isEn() ? "Save error: " : "Ошибка при сохранении: ") + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        updateUI();
        background.work.around.Start.RunService(this);
    }

    private SharedPreferences getProtectedPrefs() {
        return getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void updateUI() {
        SharedPreferences p = getProtectedPrefs();
        boolean readIntro = p.getBoolean(READ_INTRO, false);
        boolean dndGranted = isDndAccessGranted();
        boolean admin = isAdmin();
        
        if (!readIntro) {
            render(isEn() ? TEXT_INTRO_EN : TEXT_INTRO);
            renderButtons(isEn() ? new String[]{"Continue", "Developer Information"} : new String[]{"Продолжить", "Информация для разработчиков"});
            return;
        }

        if (!isBatteryOptimizationsIgnored()) {
            render(isEn() ? TEXT_BATTERY_EN : TEXT_BATTERY);
            renderButtons(isEn() ? new String[]{"Disable Optimization"} : new String[]{"Отключить оптимизацию"});
            return;
        }
                        
        if (!admin) { 
            render(isEn() ? TEXT_ADMIN_EN : TEXT_ADMIN);
            renderButtons(isEn() ? new String[]{"Grant rights"} : new String[]{"Дать права"});
            return;
        }
                
        if (!dndGranted) {
            render(isEn() ? TEXT_NOTIFICATION_EN : TEXT_NOTIFICATION);
            renderButtons(isEn() ? new String[]{"Grant Permission"} : new String[]{"Дать разрешение"});
            return;
        }
        
        boolean appNotifEnabled = isAppNotificationsEnabled();
        if (!appNotifEnabled) {
           render(isEn() ? TEXT_APP_NOTIF_EN : TEXT_APP_NOTIF);
           renderButtons(isEn() ? new String[]{"Notification Visibility Settings"} : new String[]{"Настройки уведомлений"});
           return;
        }
           
        render(isEn() ? TEXT_FORCED_RESTART_EN : TEXT_FORCED_RESTART);
        renderButtons(isEn() ? new String[]{"Forced Restart Management", "Skip"} : new String[]{"Управление принудительным перезапуском", "Пропустить"});        
    }

    private void render(String textValue) { text.setText(textValue); }

    private void renderButtons(String[] actions) {
        buttonBox.removeAllViews();
        for (String a : actions) {
            Button b = new Button(this);
            b.setText(a);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(Color.parseColor("#34495e"));
            b.setBackground(shape);
            b.setTextColor(Color.WHITE);
            b.setPadding(32, 32, 32, 32);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            b.setLayoutParams(params);
            b.setOnClickListener(v -> handleAction(a));
            buttonBox.addView(b);
        }
    }

    private void handleAction(String action) {
        SharedPreferences p = getProtectedPrefs();
        switch (action) {
            case "Продолжить": case "Continue":
                p.edit().putBoolean(READ_INTRO, true).apply();
                updateUI(); break;
            case "Информация для разработчиков": case "Developer Information":
                render(isEn() ? TEXT_DEV_EN : TEXT_DEV);
                renderButtons(isEn() ? new String[]{"Download Examples", "Continue"} : new String[]{"Скачать примеры", "Продолжить"});
                break;
            case "Скачать примеры": case "Download Examples":
                exportFileToDownloads(); break;
                
            case "Дать разрешение": case "Grant Permission":
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); 
                break;
                
            case "Дать права": case "Grant rights":
                Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, MyDeviceAdminReceiver.class));
                startActivity(adminIntent); break;            
            
           case "Отключить оптимизацию": case "Disable Optimization":
                try {
                    Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    batteryIntent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(batteryIntent);
                } catch (Throwable e) {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
            break;    
                
            case "Настройки уведомлений": case "Notification Visibility Settings":
               Intent notifIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
               notifIntent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
               startActivity(notifIntent); break;         

            case "Управление принудительным перезапуском": case "Forced Restart Management":         
                startActivity(new Intent(this, WhitelistActivity.class));
                break;
                
            case "Пропустить": case "Skip":
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;                            
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private boolean isAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(new ComponentName(this, MyDeviceAdminReceiver.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();     
    }

    private static final String TEXT_FORCED_RESTART = "Также вы можете настроить принудительный перезапуск сторонних приложений, не связанных с данным и не поддерживающих его.";
    private static final String TEXT_FORCED_RESTART_EN = "You can also configure the forced restart of third-party applications that are not related to this one and do not support it.";
        
    private static final String TEXT_INTRO = "Привет это приложение BackgroundWorkAround. Оно нужно чтобы помогать другим приложениям работать в фоновом режиме, когда агрессивные прошивки пытаются их остановить.";
    private static final String TEXT_INTRO_EN = "Hello, this is the BackgroundWorkAround application. It is needed to help other applications work in the background when aggressive firmwares try to stop them.";

    private static final String TEXT_BATTERY = "Отключите оптимизацию батареи для приложения. Это позволит обходить ограничения Doze mode и предотвратит принудительное уничтожение фоновых служб системой.";
    private static final String TEXT_BATTERY_EN = "Please, disable battery optimization for the application. This will allow bypassing Doze mode restrictions and prevent the system from forcefully killing background services.";

    private static final String TEXT_DEV = "Для этого оно использует отправку broadcast, прикрепление к ContentProvider и bind к сервисам в зависимости от того что из этого разрешил разработчик вызываемого приложения. В конечных приложениях логика как правило реализуется пробросом bind из провайдера дальше в сервис. Но может реализоваться и через прямой bind, если разрабочик сделал сервис exported=true. Преимущество provider тут в том, что можно вызвать grant uri permissions для конкретного вызывающего, а не для всех приложений, что дает плюс к безопасности и можно использовать exported=false. Другие компоненты можно только защитить через permission, если разрабочик уверен, что на устройстве установлено именно то приложение которое и должно обьявлять этот permission. К примеру можно включать защищенный компонент только после установки владельца permission и проверки его подписи. Но даже так, grant uri будет надежнее.\n\nПримеры кода для проброса бинда через provider, необходимых метаданных провайдера, queries в манифесте, а также action для broadcast и сервисов вы можете скачать по кнопке ниже (Здесь будет AndroidManifest и Java классы. Обратите внимание, в client_example Activity чисто демонстрационная. Также как и звук в сервисе).\n\nВнимание: Это оригинальное приложение может жить дольше чем ваш клиент, ведь оно имеет больше прав и методов перезапуска. Вы можете использовать оригинальные методы и разрешения, или просто быть клиентом. Или и то и другое.\n\nЕсли вы скачаете:\nBWA_example_***.zip/app/... Это код этого оригинального приложения.\n\nBWA_example_***.zip/client_example/... Пример кода вашего клиента !!!";
    private static final String TEXT_DEV_EN = "For this, it uses sending a broadcast, attaching to a ContentProvider, and binding to services depending on what the developer of the calling application has allowed. In final applications, logic is typically implemented by forwarding the bind from the provider to the service. But it can also be implemented through a direct bind if the developer has made the service exported=true. The advantage of the provider here is that you can call grant uri permissions for a specific caller, and not for all applications, which gives a plus to security and you can use exported=false. Other components can only be protected via permission if the developer is sure that exactly that application which should declare this permission is installed on the device. For example, you can enable a protected component only after installing the owner of the permission and checking its signature. But even so, grant uri will be more reliable.\n\nCode examples for forwarding the bind via provider, necessary provider metadata, queries in the manifest, as well as the action for broadcast and services can be downloaded by the button below (Here there will be AndroidManifest and Java classes. Please note, in client_example the Activity is purely demonstrative. Just like the sound in the service).\n\nAttention: This original app can live longer than your client, as it has more rights and restart methods. You can use original methods and permissions, or just be a client. Or both.\n\nif you download:\nBWA_example_***.zip/app/... This app original code.\n\nBWA_example_***.zip/client_example/... Your client code example !!!";

    private static final String TEXT_NOTIFICATION = "Также просьба дать разрешение на автозапуск чтобы приложение могло работать даже после перезагрузки.\n\nДля этого дайте BackgroundWorkAround разрешение на доступ к режимам. Это нужно для того чтобы система выполняла привязку к сервису обработки режимов тем самым запуская процесс приложения после перезагрузки в обход стандартного BOOT_COMPLETED (он тоже используется).";
    private static final String TEXT_NOTIFICATION_EN = "Also, please give permission for auto-start so that app could work even after a reboot.\n\nTo do this, give BackgroundWorkAround permission to access modes. This is necessary so that the system binds to the modes processing service, thereby launching the application process after reboot, bypassing the standard BOOT_COMPLETED (it is also used).";

    private static final String TEXT_APP_NOTIF = "Теперь включите уведомления для приложения.";
    private static final String TEXT_APP_NOTIF_EN = "Now enable notifications for the application.";

    private static final String TEXT_ADMIN = "Теперь дайте права администратора.\nЭто нужно чтобы приложение не могло быть переведено системой в архив когда оно не используется. А также чтобы избежать случайной остановки.";
    private static final String TEXT_ADMIN_EN = "Now give the administrator rights.\nThis is needed so that the application could not be moved by the system to the archive when it is not used. And also to avoid accidental stopping.";
}
