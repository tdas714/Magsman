package applied.ai.magsman;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

//import static applied.ai.magsman.Utils.startAlarm;

public class MyService extends JobIntentService {
    // Service unique ID
    static final int SERVICE_JOB_ID = 50;

    // Enqueuing work in to this service.
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MyService.class, SERVICE_JOB_ID, work);
    }
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        PeriodicWorkRequest oneTimeWorkRequest = new PeriodicWorkRequest.Builder(AlarmNotificationReceiver.class, 12, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("AlarmWorker", ExistingPeriodicWorkPolicy.KEEP, oneTimeWorkRequest);
        Log.e("ALARM", "Alarms set for everyday "+"Activated");
    }

}
