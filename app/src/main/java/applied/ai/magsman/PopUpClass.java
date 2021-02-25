package applied.ai.magsman;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.common.util.concurrent.ListenableFuture;
import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PopUpClass {

    //PopupWindow display method

    private WorkManager wm;
    ArrayList<String> bbs = new ArrayList<>();
    public void showPopupWindow(final View view, String f, Context context, Uri uri) {

        String filename = f.split(".pdf")[0];
        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        //Specify the length and width through constants
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        //Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        //Create a window with our parameters
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        //Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        //Initialize the elements of our window, install the handler

        TextView test2 = popupView.findViewById(R.id.titleText);
        test2.setText(filename);

        wm = WorkManager.getInstance(context);
//        -----------------------------------------
        try{
            DBHelper db = new DBHelper(context);
            String dbOut = db.loadBooks();

            for(String d: dbOut.split("\n")){
                String[] dm = d.split("<Mags>");
                bbs.add(dm[1]);
            }
            db.close();
        }catch (Exception ex) {
            Log.e("showPopupWindow: ", "Database Error");
        }
        Button buttonEdit = popupView.findViewById(R.id.messageButton);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //As an example, display the message
//                Toast.makeText(view.getContext(), "Wow, popup action button", Toast.LENGTH_SHORT).show();

                String file = test2.getText().toString()+".pdf";
//                if(bbs.contains(file)){
//                    Toast.makeText(context, "Database already has a book named "+file, Toast.LENGTH_LONG).show();
//                }else{
                File desFile = null;
                desFile = Utils.getFile("Books", file);
                try {
                    boolean b = FileUtils.copyFile(uri, desFile, context);
//                    Log.e("onActivityResult: ", String.valueOf(b));
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                -------------------------------------------------------
//                Log.e("onClick: ", file);
                Constraints constraints = new Constraints.Builder()
//                        .setRequiresBatteryNotLow(true) // Uncomment this one for the final
                        .setRequiresStorageNotLow(true)
//                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build();
                int core = 1; //Utils.printNumberOfCores();
//                Log.e("onClick: Cores", String.valueOf(core));
                PdfReader reader = null;
                try {
                    reader = new PdfReader(Utils.getFile("Books", file).getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert reader != null;
                int totalPage = reader.getNumberOfPages();
                int minNum = Math.min(totalPage, core);
                for(int c = 0; c < minNum; c++){
                    WorkRequest processWorkRequest = new OneTimeWorkRequest.Builder(BookProcessWorker.class)
                            .setConstraints(constraints)
                            .addTag("BookProcess")
                            .setInputData(new Data.Builder()
                                    .putString("filename", file)
                                    .putInt("start", c)
                                    .putInt("increment", minNum).build())
                            .build();
                    wm.enqueue(processWorkRequest);
                }
//              -------------------------------------------------
                if(!bbs.contains(file)){
                    Utils.returnObject returnObject = null;
                    try {
                        returnObject = Utils.openRenderer(context.getApplicationContext(), Utils.getFile("Books", file));
                        PdfRenderer.Page currentPage = Utils.saveCover(0, file, returnObject.pdfRenderer);
                        Utils.closeRenderer(currentPage, returnObject.pdfRenderer, returnObject.parcelFileDescriptor);
                        String coverPath = Utils.getFile(file, "cover.png").getPath();
                        Book book = new Book(1, file, coverPath, 1  , 0, totalPage, 0);
                        DBHelper db = new DBHelper(context.getApplicationContext());
                        db.addBook(book);
                        db.close();
                        bbs.add(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//                Toast.makeText(context, "You can cancel at any point from Notification. To resume processing the book, add the same book later on.", Toast.LENGTH_LONG).show();
                popupWindow.dismiss();
            }

        });
        //Handler for clicking on the inactive zone of the window
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //Close the window when clicked
                popupWindow.dismiss();
                return true;
            }
        });
    }
//    -----------------------------------------------------------------------------
    private boolean isWorkScheduled(String tag) {
//        WorkManager instance = WorkManager.getInstance();
        ListenableFuture<List<WorkInfo>> statuses = wm.getWorkInfosByTag(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED;
            }
            return running;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
