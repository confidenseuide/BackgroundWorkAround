package background.work.around;

import android.os.Handler;
import android.os.HandlerThread;
import android.app.AlarmManager;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.app.Service;
import java.util.Locale;
import android.service.notification.ConditionProviderService;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.content.ContentProviderClient;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Build;
import android.print.PrintManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;

public class NotificationService extends ConditionProviderService {	

	private final Map<String, ServiceConnection> whitelistConnections = new HashMap<>();

    private void WhiteList() {
        new Thread(() -> {
            while (true) {
                try {
                    SharedPreferences prefs = getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("whitelist_prefs", Context.MODE_PRIVATE);
                    java.util.Map<String, ?> allEntries = prefs.getAll();
                    PackageManager pm = getPackageManager();

                    for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        String pkg = entry.getKey();
                        Object val = entry.getValue();
                        
                        if (val instanceof Boolean && (Boolean) val) {
                            try {
                                PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_SERVICES);
                                if (pi.services != null) {
                                    for (ServiceInfo si : pi.services) {
                                        if (si.exported && (si.permission == null || si.permission.isEmpty())) {
                                            String serviceKey = si.packageName + "/" + si.name;

                                            if (!whitelistConnections.containsKey(serviceKey)) {
                                                ServiceConnection conn = new ServiceConnection() {
                                                    @Override public void onServiceConnected(ComponentName name, IBinder service) {}
                                                    @Override public void onServiceDisconnected(ComponentName name) {
                                                        whitelistConnections.remove(serviceKey);
                                                    }
                                                };

                                                Intent bindIntent = new Intent();
                                                bindIntent.setComponent(new ComponentName(si.packageName, si.name));
                                                
                                                if (bindService(bindIntent, conn, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT)) {
                                                    whitelistConnections.put(serviceKey, conn);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable t) {                                
                            }
                        }
                    }
                } catch (Throwable t) { }
                
                android.os.SystemClock.sleep(15_000);
            }
        }).start();
    }
		
	@Override 
	public void onCreate() {
    super.onCreate();
		
	startEnforcedService();
	forceBindAndStart();
	scheduleAlarm();

    HandlerThread handlerThread = new HandlerThread("BackgroundWorker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();

    Handler backgroundHandler = new Handler(handlerThread.getLooper());

    backgroundHandler.postDelayed(() -> {
        try {			
			background.work.around.Start.RunService(this);            
            HelpOthers();
            pi();
            startServiceDiscovery();
            startWatchdog();			
			WhiteList();			
        } catch (Throwable t) {
        }
    }, 3000); 
	}
	


	private final Map<String, ServiceConnection> serviceConnections = new HashMap<>();
    private static final String BIND_ACTION = "background.work.around.bind.me";

    private void startServiceDiscovery() {
    new Thread(() -> {
        while (true) {
            Intent intent = new Intent(BIND_ACTION);
            List<android.content.pm.ResolveInfo> services = getPackageManager().queryIntentServices(intent, 0);

            if (services != null) {
                for (android.content.pm.ResolveInfo info : services) {
                    if (info.serviceInfo == null) continue;

                    String serviceKey = info.serviceInfo.packageName + "/" + info.serviceInfo.name;

                    if (!serviceConnections.containsKey(serviceKey)) {
                        try {
                            ServiceConnection conn = new ServiceConnection() {
                                @Override public void onServiceConnected(ComponentName name, IBinder service) {}
                                @Override public void onServiceDisconnected(ComponentName name) {
                                    serviceConnections.remove(serviceKey);
                                }
                            };

                            Intent bindIntent = new Intent();
                            bindIntent.setComponent(new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name));
                            
                            if (bindService(bindIntent, conn, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT)) {
                                serviceConnections.put(serviceKey, conn);
                            }
                        } catch (Throwable t) {
                            serviceConnections.remove(serviceKey);
                        }
                    }
                    SystemClock.sleep(500);
                }
            }
            SystemClock.sleep(15_000);
        }
    }).start();
	}


	private final Map<String, ContentProviderClient> activeConnections = new HashMap<>();
    private static final String META_TAG = "background.work.around.provider.bind.me";

    private void pi() {
        new Thread(() -> {
            while (true) {
                    List<ProviderInfo> providers = getPackageManager().queryContentProviders(
                        null, 0, PackageManager.GET_META_DATA);
                
                processProviders(providers);
                
                SystemClock.sleep(15_000);
            }
        }).start();
    }     
    

    private void processProviders(List<ProviderInfo> providers) {
    java.util.List<ProviderInfo> allProviders = new java.util.ArrayList<>();
    if (providers != null) {
        allProviders.addAll(providers);
    }

    try {
        java.util.List<android.content.UriPermission> persistedPerms = 
                getContentResolver().getPersistedUriPermissions();
                
        for (android.content.UriPermission perm : persistedPerms) {
            String authority = perm.getUri().getAuthority();
            if (authority != null) {
                boolean exists = false;
                for (ProviderInfo p : allProviders) {
                    if (p != null && authority.equals(p.authority)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ProviderInfo fakePi = new ProviderInfo();
                    fakePi.authority = authority;
                    fakePi.metaData = new android.os.Bundle();
                    fakePi.metaData.putBoolean(META_TAG, true);
                    allProviders.add(fakePi);
                }
            }
        }
    } catch (Throwable t) {}

    for (ProviderInfo pi : allProviders) {
        if (pi != null && pi.metaData != null && pi.metaData.containsKey(META_TAG)) {
            String authority = pi.authority;
                        
            if (activeConnections.containsKey(authority)) {
                continue; 
            }
            
            try {
                ContentProviderClient client = getContentResolver().acquireContentProviderClient(authority);
                if (client != null) {
                    activeConnections.put(authority, client);
                    
                    android.net.Uri providerUri = android.net.Uri.parse("content://" + authority);
                    int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(providerUri, takeFlags);
                }
            } catch (Throwable t) {
                activeConnections.remove(authority);
            }
			
            SystemClock.sleep(500);
        }
    }}

	private void HelpOthers() {
    new Thread(() -> {
		while (true) {
        try {
            String action = "background.work.around.THIRDPARTYAPPS";
            android.content.pm.PackageManager pm = getPackageManager();
            java.util.List<android.content.pm.ResolveInfo> receivers = pm.queryBroadcastReceivers(new android.content.Intent(action), 0);

            if (receivers == null) return;

            for (android.content.pm.ResolveInfo info : receivers) {
				try {
                android.content.Intent intent = new android.content.Intent(action);
                intent.setComponent(new android.content.ComponentName(
                        info.activityInfo.packageName,
                        info.activityInfo.name
                ));
                intent.addFlags(android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

                sendBroadcast(intent);
				} catch (Throwable t) {}
                android.os.SystemClock.sleep(700);
            }
        } catch (Throwable t) {}				
		android.os.SystemClock.sleep(5_000);
		Start.RunService(this);	
		android.os.SystemClock.sleep(15_000);
		Start.RunService2(this);		
		}	
    }).start();
	}
	
	private void forceBindAndStart() {
	try {
	Intent serviceIntent2 = new Intent(this, RiderService.class);		
    bindService(serviceIntent2, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT); 
    startService(serviceIntent2);
	} catch (Throwable t) {}    
    }
	
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public void onServiceDisconnected(ComponentName name) {
          forceBindAndStart();
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {    
	startEnforcedService();
	return START_STICKY;
    }


	private void scheduleAlarm() {
    try {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        
        Intent intent = new Intent(this, MyDeviceAdminReceiver.class);                                            
        PendingIntent piRepeating = PendingIntent.getBroadcast(this, 8881337, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 70000, 70000, piRepeating);
    } catch (Throwable t) {} }



	private void startWatchdog() {
    new Thread(() -> {
        while (true) {
			android.os.SystemClock.sleep(5000);			
			try {										
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);                
                if (nm.areNotificationsEnabled() && nm.getActiveNotifications().length == 0) {
                    startEnforcedService();
                }
				if (Build.VERSION.SDK_INT >= 33 && !((PrintManager) getSystemService(PRINT_SERVICE)).isPrintServiceEnabled(new ComponentName(this, PrintService.class)) && !((DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(this, MyDeviceAdminReceiver.class))) {    
					MyDeviceAdminReceiver.setComponentState(NotificationService.this, PrintService.class, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);                 
					android.os.SystemClock.sleep(7000);			     
					MyDeviceAdminReceiver.setComponentState(NotificationService.this, PrintService.class, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);				
				}
            } catch (Throwable t) {}
        }
    }).start();
	}

	private void startEnforcedService() {		
	try {	
	Context context = this;
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    String pkg = context.getPackageName();    

    List<NotificationChannel> channels = nm.getNotificationChannels();
    String activeId = null;
    boolean needNew = false;

    for (NotificationChannel ch : channels) {
        if (ch.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            nm.deleteNotificationChannel(ch.getId());
            needNew = true;
        } else if (activeId == null) {
            activeId = ch.getId();
        }
    }

    if (needNew || activeId == null) {
        activeId = "background.work.around" + Long.toHexString(new java.security.SecureRandom().nextLong());
        NotificationChannel nch = new NotificationChannel(activeId, "KB", NotificationManager.IMPORTANCE_DEFAULT);
        nch.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
		nch.setSound(null, null);
		nch.enableVibration(false);
		nm.createNotificationChannel(nch);
    }

    Notification notif = new Notification.Builder(context, activeId)
            .setContentTitle("🟢🟢🟢")
            .setContentText("ru".equalsIgnoreCase(Locale.getDefault().getLanguage()) ? "Фоновый сервис запущен" : "Background service is started")
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
		    .setVisibility(Notification.VISIBILITY_SECRET)
            .build();
    
	if (android.os.Build.VERSION.SDK_INT >= 34) {
        startForeground(1, notif, 1024);
    } else {
        startForeground(1, notif);
    } } catch (Throwable isError) {}
	}

	@Override
    public void onConnected() {        
    }

	@Override
    public void onRequestConditions(int relevance) {
    }

    @Override
    public void onSubscribe(Uri conditionId) {
    }

	@Override
    public void onUnsubscribe(Uri conditionId) {
	background.work.around.Start.RunService(this);	
    }
	
    @Override
    public void onDestroy() {        
        background.work.around.Start.RunService(this);
        super.onDestroy();
    }
}
