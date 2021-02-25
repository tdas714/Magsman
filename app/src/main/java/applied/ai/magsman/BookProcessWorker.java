package applied.ai.magsman;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.icu.text.DateTimePatternGenerator;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.json.JSONException;
import org.pytorch.Module;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static android.content.Context.NOTIFICATION_SERVICE;

public class BookProcessWorker extends Worker {

    private NotificationManager notificationManager;
    private Notification notification;
    private String CHANNEL_ID = "WorkerNotify";
    private int NOTIFICATION_ID = 15;
    public BookProcessWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        String filename = getInputData().getString("filename");
        int start = getInputData().getInt("start", 0);
        int increment = getInputData().getInt("increment", 1);

        Log.e("doWork: INPUT DATA: START", String.valueOf(start));
        Log.e("doWork: INPUT DATA: INCREMENT", String.valueOf(increment));
        Log.e("doWork: FILENAME", filename);

//        Log.e("doWork: INPUT DATA", String.valueOf(start));
        setForegroundAsync(createForegroundInfo(String.valueOf(1), filename, 600, 1));
        Log.e("doWork: ", filename);
        float progress = (float) 1.0;
        try {
            int tp;
            Module module = null;
            Module melModule = null;
            G2P g2P = null;
            while (true) {
                try {
                    module = Module.load(Utils.assetFilePath(getApplicationContext(), "traced_tts_model_alpha.pt"));
                    melModule = Module.load(Utils.assetFilePath(getApplicationContext(), "traced_melGan_s.pt"));
                    g2P = new G2P(getApplicationContext());
                    break;
                } catch (Exception e) {
                    Thread.sleep(1000);
                    Log.e("doWork: Module Loading", "Failed");
                }
            }
//          ----------------------------------------------------------------
            Log.e("doWork: File Path", Utils.getFile("Books", filename).getPath());
            PdfReader reader = new PdfReader(Utils.getFile("Books", filename).getPath());
            int totalPage = reader.getNumberOfPages();
            byte[] audio = new byte[0];
//            float p = (float) 100.0 / ((float) totalPage * (float) 3.0);
            for(tp = start; tp < totalPage; tp += increment){
                File checkdir = Environment.getExternalStorageDirectory();
                File checkfiledir = new File(checkdir.getAbsoluteFile()+"/Magsman/"+filename+"/");
                File checkdesFile = new File(checkfiledir, tp+".wav");
//              --------------------------------------------------------------
                if(checkdesFile.exists()){
                    continue;
                }
                String[] pageText = PdfTextExtractor.getTextFromPage(reader, tp+1).split("\\.");
                float p = (float) 100.0 / ((float) pageText.length);
                setForegroundAsync(createForegroundInfo(String.valueOf(1), filename, totalPage, tp+1));
                setProgressAsync(new Data.Builder().putInt("progress", (int) progress).build());
                ByteArrayOutputStream audio_stream = new ByteArrayOutputStream();
                String pre_p = "";
                for(int t=0; t<pageText.length; t++){
                    Log.e("doWork: PageNum", String.valueOf(tp));
                    Log.e("doWork: Progress", String.valueOf(progress));
                    assert module != null;
                    assert melModule != null;

                    if(pageText[t].split("\\s").length <= 5){
                        pre_p = pageText[t]+",";
                        continue;
                    }
                    Log.e("doWork: Input Text", pre_p+" "+pageText[t]);
                    audio = Utils.ProcessText(pre_p+" "+pageText[t]+".", module, melModule, g2P);
                    setForegroundAsync(createForegroundInfo(String.valueOf((int) progress), filename, totalPage, tp+1));
                    setProgressAsync(new Data.Builder().putInt("progress", (int) progress).build());
                    if(audio==null){continue;}
                    progress += p;
                    audio_stream.write(audio);
                    pre_p = "";
                }

                Log.d("doWork: TextPage", Arrays.toString(pageText));
                Log.e("doWork: ", Arrays.toString(audio));
                byte[] page_audio = audio_stream.toByteArray();
                File byteFile = Utils.getFile(filename, tp+".wav");
                Utils.rawToWave(page_audio, byteFile);
//                Log.e("doWork: ", "Testing");
            }
            setForegroundAsync(createForegroundInfo("100", filename, totalPage, tp-1));
            reader.close();
            module.destroy();
            melModule.destroy();
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
        return Result.success();
    }
//
    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress, String filename, int totalPage, int tp) {
        // Build a notification using bytesRead and contentLength

        Context context = getApplicationContext();

        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context.getApplicationContext())
                .createCancelPendingIntent(getId());
        createNotificationChannel(context, notificationManager);
        if(progress.equals("0")){
            Log.e("createForegroundInfo: ", "0 Activated");
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Processing ⇌ "+filename)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                    .setTicker(progress)
                    .setSmallIcon(R.mipmap.ic_notification_foreground)
                    .setColor(context.getResources().getColor(R.color.colorAccent))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle("Processing ⇌ "+filename)
                            .bigText("You can cancel at any point. To resume processing the book, just add the same book later on."))
//                            .setSummaryText("You can cancel at any point. To resume processing the book, add the same book later on."))
                    // Add the cancel action to the notification which can
                    // be used to cancel the worker
                    .addAction(android.R.drawable.ic_delete, "cancel", intent)
                    .setAutoCancel(true)
                    .setProgress(100, Integer.parseInt(progress), true)
                    .setContentInfo("Please Wait")
                    .setSubText("Please Wait ("+tp+"/"+totalPage+")")
                    .build();
        }else if(progress.equals("100") && (totalPage==tp)){
            Log.e("createForegroundInfo: ", "100 Activated");
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("It's Done | Page: "+tp+" "+filename)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                    .setTicker(progress)
                    .setSmallIcon(R.mipmap.ic_notification_foreground)
                    .setColor(context.getResources().getColor(R.color.colorAccent))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle("It's Done | Page: "+tp+" "+filename))
                    // Add the cancel action to the notification which can
                    // be used to cancel the worker
//                    .addAction(android.R.drawable.ic_delete, "cancel", intent)
                    .setAutoCancel(true)
                    .setProgress(100, Integer.parseInt(progress), false)
                    .setContentInfo("Please Wait")
                    .setSubText("Please Wait ("+tp+"/"+totalPage+")")
                    .build();
//            notificationManager.notify("Worker", NOTIFICATION_ID, notification);
        }else{
            Log.e("createForegroundInfo: ", "Else Activated");
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Processing ⇌ "+filename)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                    .setTicker(progress)
                    .setSmallIcon(R.mipmap.ic_notification_foreground)
                    .setColor(context.getResources().getColor(R.color.colorAccent))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle("Processing ⇌ "+filename)
                            .bigText("You can cancel at any point. To resume processing the book, just add the same book later on."))
                    // Add the cancel action to the notification which can
                    // be used to cancel the worker
                    .addAction(android.R.drawable.ic_delete, "cancel", intent)
                    .setOnlyAlertOnce(true)
                    .setProgress(100, Integer.parseInt(progress), false)
                    .setContentInfo("Please Wait")
                    .setSubText("Please Wait ("+tp+"/"+totalPage+")")
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .build();
        }

//                .setAutoCancel(true);
//        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Book Process";
            String description = "Worker Progress";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.e("onStopped: WORKERSTATE", "Worker ONSTOPPED");
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
