package applied.ai.magsman;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.itextpdf.text.pdf.PdfReader;

import org.pytorch.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static applied.ai.magsman.MainActivity.hasPermissions;

public class SplashScreen extends AppCompatActivity {

    private ArrayList<String> bbs = new ArrayList<>();
    private ProgressBar pBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        pBar = findViewById(R.id.pBar);
        pBar.setVisibility(View.VISIBLE);

        int ALL_PERMISSIONS = 101;
        final String[] permissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SET_ALARM};
        if (!hasPermissions(getApplicationContext(), permissions)) {
//            for(String p: permissions){
            ActivityCompat.requestPermissions(SplashScreen.this, permissions, ALL_PERMISSIONS);
//            }
        }else{
            Start();
        }
//        ---------------------------------

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Start();
    }

    private void Start(){
        try{
            DBHelper db = new DBHelper(getApplicationContext());
            String dbOut = db.loadBooks();

            for(String d: dbOut.split("\n")){
                String[] dm = d.split("<Mags>");
                bbs.add(dm[1]);
            }
            db.close();
        }catch (Exception ex) {
            Log.e("showPopupWindow: ", "Database Error");
        }

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Module module = null;
                Module melModule = null;
//                G2P g2P = null;
                while (true) {
                    try {
                        module = Module.load(Utils.assetFilePath(getApplicationContext(), "traced_tts_model_alpha.pt"));
                        melModule = Module.load(Utils.assetFilePath(getApplicationContext(), "traced_melGan_s.pt"));
//                        g2P = new G2P(getApplicationContext());
                        break;
                    } catch (Exception e) {
//                        Thread.sleep(1000);
                        Log.e("doWork: Module Loading", "Failed");
                    }
                }
                module.destroy();
                melModule.destroy();


                File dir = Environment.getExternalStorageDirectory();
                File filedir = new File(dir.getAbsoluteFile() + "/Magsman/");
                Log.e("getFile: ", filedir.toString());
                try {
                    if (!filedir.exists()) {
                        Log.e("run: Exist", "Activated");
                        copyDirorfileFromAssetManager("Magsman", "Magsman");
                    }
                } catch (IOException e) {
                    Log.e("onCreate: Copy Dir Error", e.toString());
                }
                pBar.setIndeterminate(false);
                pBar.setMax(100);
                pBar.setProgress(100, true);
                Intent i = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, 500);
    }

    public String copyDirorfileFromAssetManager(String arg_assetDir, String arg_destinationDir) throws IOException
    {
        File sd_path = Environment.getExternalStorageDirectory();
        String dest_dir_path = sd_path + addLeadingSlash(arg_destinationDir);
        File dest_dir = new File(dest_dir_path);
        Log.e("copyDirorfileFromAssetManager: Dest DIR", dest_dir.getPath());
        createDir(dest_dir);

        AssetManager asset_manager = getApplicationContext().getAssets();
        String[] files = asset_manager.list(arg_assetDir);

        for (int i = 0; i < files.length; i++)
        {

            String abs_asset_file_path = addTrailingSlash(arg_assetDir) + files[i];
            String sub_files[] = asset_manager.list(abs_asset_file_path);

            if (sub_files.length == 0)
            {
                // It is a file
                String dest_file_path = addTrailingSlash(dest_dir_path) + files[i];
                copyAssetFile(abs_asset_file_path, dest_file_path);
            } else
            {
                // It is a sub directory
                copyDirorfileFromAssetManager(abs_asset_file_path, addTrailingSlash(arg_destinationDir) + files[i]);
                Utils.returnObject returnObject = null;
                if(files[i].contains(".pdf")){
                    try {
                        PdfReader reader = null;
                        try {
                            reader = new PdfReader(Utils.getFile("Books", files[i]).getPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        assert reader != null;
                        int totalPage = reader.getNumberOfPages();
                        reader.close();
                        returnObject = Utils.openRenderer(getApplicationContext(), Utils.getFile("Books", files[i]));
                        PdfRenderer.Page currentPage = Utils.saveCover(0, files[i], returnObject.pdfRenderer);
                        Utils.closeRenderer(currentPage, returnObject.pdfRenderer, returnObject.parcelFileDescriptor);
                        String coverPath = Utils.getFile(files[i], "cover.png").getPath();
                        Book book = new Book(1, files[i], coverPath, 1  , 0, totalPage, 0);
                        DBHelper db = new DBHelper(getApplicationContext());
                        db.addBook(book);
                        db.close();
                        bbs.add(files[i]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //                -------------------------------------------------
        }

        return dest_dir_path;
    }


    public void copyAssetFile(String assetFilePath, String destinationFilePath) throws IOException
    {
        InputStream in = getApplicationContext().getAssets().open(assetFilePath);
        OutputStream out = new FileOutputStream(destinationFilePath);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public String addTrailingSlash(String path)
    {
        if (path.charAt(path.length() - 1) != '/')
        {
            path += "/";
        }
        return path;
    }

    public String addLeadingSlash(String path)
    {
        if (path.charAt(0) != '/')
        {
            path = "/" + path;
        }
        return path;
    }

    public void createDir(File dir) throws IOException {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IOException("Can't create directory, a file is in the way");
            }
        } else {
            dir.mkdirs();
            if (!dir.isDirectory()) {
                throw new IOException("Unable to create directory");
            }
        }
    }
}