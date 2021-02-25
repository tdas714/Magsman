package applied.ai.magsman;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static java.util.Calendar.getInstance;
//import static applied.ai.magsman.Utils.startAlarm;

public class AlarmNotificationReceiver extends Worker {
    private String CHANNEL_ID = "Alarm001";
    private Random randomGenerator;
    private Context context;
    public AlarmNotificationReceiver(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("onReceive: AlarmNotification", "Received");
//        Toast.makeText(context, "Received AlarmREceiver", Toast.LENGTH_SHORT).show();
        NotificationCompat.Builder builder;// = new NotificationCompat.Builder(context, CHANNEL_ID);
        ArrayList<String> q = Utils.readFromFile(context);
        randomGenerator = new Random();
        int index = randomGenerator.nextInt(q.size());
        Log.d("onReceive: ", q.get(index));
        String[] notificationText = q.get(index).split("–");
//        String[] notificationText = new String[]{"This is a test", "Android"};
        Intent myIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                myIntent,
                FLAG_ONE_SHOT );
        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_notification_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                .setColor(context.getResources().getColor(R.color.colorAccent))
                .setContentTitle(notificationText[1]+" – ")
                .setContentText(notificationText[0])
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText[0]))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setSubText("Daily Quotes");
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(context, notificationManager);
        notificationManager.notify("Alarm",123,builder.build());
        PeriodicWorkRequest oneTimeWorkRequest = new PeriodicWorkRequest.Builder(AlarmNotificationReceiver.class, 12, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork("AlarmWorker", ExistingPeriodicWorkPolicy.KEEP, oneTimeWorkRequest);
//        startAlarm(context);
    return Result.success();
    }

    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DailyWroker";
            String description = "This is a alarm";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
