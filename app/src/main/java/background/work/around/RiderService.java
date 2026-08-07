package background.work.around;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import java.util.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.*;
import android.os.*;
import android.provider.*;
import android.os.storage.*;

public class RiderService extends JobService {  
	private android.media.MediaPlayer player;

	private void startProcessMonitor() {
    new Thread(() -> {
        while (true) {
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();

                if (list != null) {
                    if (list.stream().noneMatch(p -> p.processName.equals(getPackageName() + ":cross1337backgroundworkaround"))) {
                        Start.RunService(this);
                    }

                    if (list.stream().noneMatch(p -> p.processName.equals(getPackageName() + ":x1337backgroundworkaround"))) {
                        Start.RunService2(this);
                    }
                }
            } catch (Throwable t) {}
            SystemClock.sleep(1000);
        }
    }).start();
	}

	private final void DontOverrideMeServiceMainVoid() {
	if (player == null) {
		    player = android.media.MediaPlayer.create(this, R.raw.silence);
            if (player != null) {
                player.setLooping(true);
                player.setVolume(1.0f, 1.0f);
                player.start();
            }
        }				
	}

	private final void DontOverrideMeDestroyCleaner() {
	if (player != null) {
            player.stop();
            player.release();
			player = null;
        }					
	}


	private static final int PERIODIC_JOB_ID = 1001;
    private static final int DELAYED_JOB_ID = 1002;

    @Override
    public boolean onStartJob(JobParameters params) {
        scheduleJobs(getApplicationContext());        
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {        
        scheduleJobs(getApplicationContext());        
		return true;
    }

    public static void scheduleJobs(Context context) {
		try {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) return;

        ComponentName componentName = new ComponentName(context, RiderService.class);

        boolean isPeriodicScheduled = false;
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == PERIODIC_JOB_ID) {
                isPeriodicScheduled = true;
                break;
            }
        }

        if (!isPeriodicScheduled) {
            JobInfo.Builder periodicBuilder = new JobInfo.Builder(PERIODIC_JOB_ID, componentName)
                    .setPeriodic(JobInfo.getMinPeriodMillis())
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                periodicBuilder.setRequiresBatteryNotLow(false);
                periodicBuilder.setRequiresStorageNotLow(false);
            }

            jobScheduler.schedule(periodicBuilder.build());
        }

        JobInfo.Builder delayedBuilder = new JobInfo.Builder(DELAYED_JOB_ID, componentName)
                .setMinimumLatency(30 * 1000L)                
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            delayedBuilder.setRequiresBatteryNotLow(false);
            delayedBuilder.setRequiresStorageNotLow(false);
        }

        jobScheduler.schedule(delayedBuilder.build());
		} catch (Throwable t) {}	
    }
	
		
	private final void EndLessWL() {	
	new Thread(() -> {
	android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
	android.os.PowerManager.WakeLock[] wl = new android.os.PowerManager.WakeLock[10]; 
	int i = 0;
	while (true) {
	try {
	if (i<0) i=10;
	if (i<10) wl[i%10] = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "BackgroundWorkAround"+String.valueOf(i%10)+"::WakeLock"+String.valueOf(i%10));
	wl[i%10].acquire(9000); 
	i++;
	} catch (Throwable t) {}
	android.os.SystemClock.sleep(3000); }
	}).start(); }


		@Override
    public final void onCreate() {
        super.onCreate();
		TryStartEnforcedService();
		ScoreSpammer.startSpamming(this);
		scheduleJobs(this);
		forceBindAndStart();				
		startWatchdogThread();			   			
		EndLessWL();
		DontOverrideMeServiceMainVoid();
		startProcessMonitor();
	}		
		

	private final void startWatchdogThread() {
    new Thread(() -> {
        Context ctx = getApplicationContext();
		int i=0;

        while (true) {
            try {
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                
                Intent intent = new Intent("background.work.around.ALARM");
                intent.setPackage(ctx.getPackageName());

                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, 
                        777+i, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

               if (am != null) {
				  if (i==0) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
				    i=1; 
				  } else {
				    am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
				  }
               }
            } catch (Throwable t) {
              
            } 
            android.os.SystemClock.sleep(30000);
        }
    }).start();
	}	

		
    private void startEnforcedService() {
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
        NotificationChannel nch = new NotificationChannel(activeId, "BackgroundWorkAround", NotificationManager.IMPORTANCE_DEFAULT);
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
    }
	}

	private final void TryStartEnforcedService() {		
		try {startEnforcedService();} 
        catch (Throwable t) {}
	}    

	private final void forceBindAndStart() {
    Intent intent = new Intent(this, NotificationService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    try {startService(intent);} 
    catch (Throwable t) {}
    }
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public final void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public final void onServiceDisconnected(ComponentName name) {
            forceBindAndStart();
        }
    };


    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {    
	TryStartEnforcedService();
    return START_STICKY;
    }

    @Override
    public final void onDestroy() {		
        background.work.around.Start.RunService(this);
		DontOverrideMeDestroyCleaner();
        super.onDestroy();
    }
}
