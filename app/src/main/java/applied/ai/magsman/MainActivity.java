package applied.ai.magsman;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

//import com.github.steveash.jg2p.SimpleEncoder;
//import com.github.steveash.jg2p.model.CmuEncoderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import static android.os.Build.VERSION.SDK_INT;
import static applied.ai.magsman.FileUtils.copyFile;
//import static applied.ai.magsman.Utils.startAlarm;
//import static applied.ai.magsman.Utils.startAlarm;

public class MainActivity extends AppCompatActivity {

    private Button btn_addBook;
    private SwipeButton btn_swipeRight;
    private Handler hand1 = new Handler();
    private static final long GET_DATA_INTERVAL = 1;
    private String[] textfirst = {"Swipe Right to Play ❯❯❯        ", "Swipe Right to Play     ❯❯❯    ", "Swipe Right to Play         ❯❯❯"};
    private int index = 0;
    private static final int FILE_SELECT_CODE = 56;
    // Recycler View object
    RecyclerView recyclerView;
    // Array list for recycler view data source
    ArrayList<String> source;
    ArrayList<Bitmap> coverimag;
    // Layout Manager
    RecyclerView.LayoutManager RecyclerViewLayoutManager;
    // adapter class object
    Adapter adapter;
    // Linear Layout Manager
    LinearLayoutManager HorizontalLayout;
    View ChildView;
    int RecyclerViewItemPosition;
    private boolean loading = true;
    int pastVisiblesItems, visibleItemCount, totalItemCount;
    PopupWindow popupWindow;
    LinearLayout linearLayout1;
    View popView;
    private String filename;

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 10;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//------------------------------------------------------

        startAlarm(MainActivity.this);
//------------------------------------------------------

        btn_addBook = findViewById(R.id.btn_addBook);

//----------------------------------------------------
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("FromAdapterToActivity"));
//-----------------------------------------------------
//        btn_swipeRight.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
//            public void onSwipeRight() {
//                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
//                intent.putExtra("title", filename);
//                startActivity(intent);
////                Toast.makeText(MainActivity.this, filename, Toast.LENGTH_SHORT).show();
//            }
//        });
//------------------------------------------------------
        // initialisation with id's
        recyclerView = (RecyclerView) findViewById(R.id.recycleView_Item);
        RecyclerViewLayoutManager = new LinearLayoutManager(getApplicationContext());
        // Set LayoutManager on Recycler View
        recyclerView.setLayoutManager(RecyclerViewLayoutManager);
        // Adding items to RecyclerView.
        try {
            AddItemsToRecyclerViewArrayList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // calling constructor of adapter
        // with source list as a parameter
        adapter = new Adapter(source, coverimag);
        // Set Horizontal Layout Manager
        // for Recycler view
        HorizontalLayout = new ZoomCenterCardLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);

        HorizontalLayout.scrollToPosition(Integer.MAX_VALUE / 3);
        recyclerView.setLayoutManager(HorizontalLayout);

        LinearSnapHelper linearSnapHelper = new SnapHelperOnebyOne();
        linearSnapHelper.attachToRecyclerView(recyclerView);


        // Set adapter on recycler view
        recyclerView.setAdapter(adapter);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
//---------------------------------------------------------
        btn_addBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showFileChooser();
                popView = view;
//                try {
//                    G2P g2P = new G2P(MainActivity.this);
//                    Log.e("onClick: SEQ", String.valueOf(g2P.getSequence("Nootropics and smart drugs are natural or synthetic substances that can be taken to improve mental performance in healthy people." +
//                            "They have gained popularity in today’s highly competitive society and are most often used to boost memory, " +
//                            "focus, creativity, intelligence and motivation.")));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

//                try {
//                    G2P g2P = new G2P(MainActivity.this);
//                    Log.e("onClick: G2P Testing",g2P.getSequence("terrorists in the region The recoveries of weapons. and hideouts busted by security forces. Unnerved them and their handlers across the border.").toString());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        btn_swipeRight = new SwipeButton(MainActivity.this);
        btn_swipeRight = findViewById(R.id.btn_swipeRight);
//        btn_swipeRight.collapseButton();
        hand1.postDelayed(run1, GET_DATA_INTERVAL);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
//                    Log.e("Acitvity Result", "File Uri: " + uri.toString());
                    // Get the path
                    String filename = getFileName(uri);
//                    Log.e("Acitvity Result", "File Path: " + filename);

                    PopUpClass popUpClass = new PopUpClass();
                    popUpClass.showPopupWindow(popView, filename, getApplicationContext(), uri);
//             =====================================================================================

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //---------------------------------------------------------
    private boolean checkAndRequestPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            int permissionReadPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
            int permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int permissionMedia = ContextCompat.checkSelfPermission(this, Manifest.permission.MEDIA_CONTENT_CONTROL);
            List<String> listPermissionsNeeded = new ArrayList<>();

            if (permissionReadPhoneState != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
            }

            if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if (permissionMedia != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.MEDIA_CONTENT_CONTROL);
            }

            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    //---------------------------------------------------------
    private String getFileName(Uri uri) throws IllegalArgumentException {
        // Obtain a cursor with information regarding this uri
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor.getCount() <= 0) {
            cursor.close();
            throw new IllegalArgumentException("Can't obtain file name, cursor is empty");
        }

        cursor.moveToFirst();
//    Log.e("getFileName: ", Arrays.toString(cursor.getColumnNames()));
        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

        cursor.close();

        return fileName;
    }

    //---------------------------------------------------------
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //---------------------------------------------------------
    Runnable run1 = new Runnable() {
        @Override
        public void run() {
            if(btn_swipeRight.active){
                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                intent.putExtra("title", filename);
                startActivity(intent);
                hand1.removeCallbacks(run1);
            }else{
                hand1.postDelayed(run1, GET_DATA_INTERVAL);
            }
        }
    };

    // Function to add items in RecyclerView.
    public void AddItemsToRecyclerViewArrayList() throws IOException {
        // Adding items to ArrayList
        DBHelper db1 = new DBHelper(getApplicationContext());
        String re = db1.loadBooks();
        Log.e("Books", re);
        db1.close();


        source = new ArrayList<>();
        coverimag = new ArrayList<>();
        try {
            String[] books = re.split(Objects.requireNonNull(System.getProperty("line.separator")));
            for (String b : books) {
                Log.e("AddItemsToRecyclerViewArrayList: Title", b.split("<Mags>")[1]);
                Log.e("AddItemsToRecyclerViewArrayList: Cover", b.split("<Mags>")[2]);
                source.add(b.split("<Mags>")[1]);
                coverimag.add(BitmapFactory.decodeFile(b.split("<Mags>")[2]));
            }
        } catch (Exception ex) {
            source.add("gfg");
            source.add("is");
            source.add("best");
            source.add("site");
            source.add("for");
            source.add("interview");
            source.add("preparation");

            coverimag.add(getBitmapFromAssets("p.jpeg"));
            coverimag.add(getBitmapFromAssets("b.jpg"));
            coverimag.add(getBitmapFromAssets("p.jpeg"));
            coverimag.add(getBitmapFromAssets("a.jpg"));
            coverimag.add(getBitmapFromAssets("p.jpeg"));
            coverimag.add(getBitmapFromAssets("b.jpg"));
            coverimag.add(getBitmapFromAssets("a.jpg"));
        }

    }

    public Bitmap getBitmapFromAssets(String fileName) throws IOException {
        AssetManager assetManager = MainActivity.this.getAssets();
        InputStream istr = assetManager.open(fileName);
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        istr.close();
        return bitmap;
    }

    //    --------------------------------------------------------------
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    //    ----------------------------------------------------------------
    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            filename = intent.getStringExtra("filename");
            //        Toast.makeText(MainActivity.this,filename+"REceiverd" ,Toast.LENGTH_SHORT).show();
        }
    };

    private void startAlarm(Context context) {

        PeriodicWorkRequest oneTimeWorkRequest = new PeriodicWorkRequest.Builder(AlarmNotificationReceiver.class, 12, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork("AlarmWorker", ExistingPeriodicWorkPolicy.KEEP, oneTimeWorkRequest);
        Log.e("Alarm", "Alarms set for everyday "+"Activated");
    }

}

