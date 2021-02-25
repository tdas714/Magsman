package applied.ai.magsman;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itextpdf.text.List;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class PlayActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageView coverImg;
    private TextView titletxt;
    private PageAdapter pageAdapter;
    boolean isLoading = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        recyclerView = findViewById(R.id.pagelist_view);
        coverImg = findViewById(R.id.cover_img);
        titletxt = findViewById(R.id.title_play_txt);
//      --------------------------------------------------------------
        String title = getIntent().getStringExtra("title");
        DBHelper db1 = new DBHelper(getApplicationContext());
        Book b = db1.findBook(title);
        db1.close();
//        -------------------------------------------------------------
        titletxt.setText(b.getTitle().split(".pdf")[0]);
        coverImg.setImageBitmap(BitmapFactory.decodeFile(b.getCover()));
//        -------------------------------------------------------------
        Palette.from(BitmapFactory.decodeFile(b.getCover())).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {

                Palette.Swatch vibc = null;
                try{
                    vibc = palette.getVibrantSwatch();
                }catch (Exception ex){
                    try{
                        vibc = palette.getDominantSwatch();
                    }catch (Exception e){
                        vibc = palette.getLightMutedSwatch();
                    }
                }
                assert vibc != null;
                try{
                    titletxt.setTextColor(vibc.getRgb());
                }catch (Exception ex){
                    titletxt.setTextColor(getResources().getColor(R.color.colorAccent));
                }
            }
        });
//      -----------------------------------------------------------------
        ArrayList<Integer> pageList = new ArrayList<Integer>();
        ArrayList<String> sumtextList = new ArrayList<>();
        for(int p=1; p <= Math.min(3, b.getTotalpage()); p++){
            pageList.add(p);
            try {
                String text= "";
                PdfReader reader = new PdfReader(Utils.getFile("Books", b.getTitle()).getPath());
                String[] pageText = PdfTextExtractor.getTextFromPage(reader, p).split("\\.");
                int num = 0;
                if(pageText.length >=5){num=4;}
                else if(pageText.length >= 4){num=3;}
                else if(pageText.length >= 3){num=2;}
                else if(pageText.length >= 2){num=1;}
                else if (pageText.length >= 1){num=0;}
//                Log.e("onCreate: PageText", String.valueOf(num));
                text = pageText[num];
                if(text.split("\\s").length<=5){
                    try{
                        text = pageText[num-1] + text;
                    }catch (Exception ex){
                        text  = "";
                    }
                }
                Log.e("onBindViewHolder: PageText", text);
//            Log.e("onBindViewHolder: Text", pageText);
                sumtextList.add(text);
            } catch (IOException e) {
                Log.e("onBindViewHolder: Error", e.toString());
            }
        }
        pageAdapter = new PageAdapter(pageList, sumtextList, b.getTitle(), getApplicationContext());
        LinearLayoutManager layout = new LinearLayoutManager(PlayActivity.this, LinearLayoutManager.VERTICAL, false);
        layout.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(layout);
        recyclerView.setAdapter(pageAdapter);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == pageList.size() - 1) {
                        //bottom of list!
                        loadMore(pageList, sumtextList, b);
                        isLoading = true;
                    }
                }
            }
        });
    }
    private void loadMore(ArrayList<Integer> pageList, ArrayList<String> sumtextList, Book b) {
        pageList.add(null);
        pageAdapter.notifyItemInserted(pageList.size() - 1);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pageList.remove(pageList.size() - 1);
                int scrollPosition = pageList.size();
                pageAdapter.notifyItemRemoved(scrollPosition);
                int currentSize = scrollPosition;
                int nextLimit = Math.min(currentSize + 5, b.getTotalpage());
                Log.e("run: Total page", String.valueOf(nextLimit));
                while (currentSize < nextLimit) {
                    Log.e("run: ", String.valueOf(currentSize+1));
                    addList(currentSize+1, pageList, sumtextList, b, pageAdapter);
                    currentSize++;
                }
                pageAdapter.notifyDataSetChanged();
                isLoading = false;
            }
        }, 2000);
    }
    public static void addList(int i, ArrayList<Integer> pageList, ArrayList<String> sumtextList, Book b, PageAdapter pageAdapter){
        pageList.add(i);
        try {
            String text = "";
            PdfReader reader = new PdfReader(Utils.getFile("Books", b.getTitle()).getPath());
            String[] pageText = PdfTextExtractor.getTextFromPage(reader, i).split("\n");
            int num = 0;
            if (pageText.length >= 5) {
                num = 4;
            } else if (pageText.length >= 4) {
                num = 3;
            } else if (pageText.length >= 3) {
                num = 2;
            } else if (pageText.length >= 2) {
                num = 1;
            } else if (pageText.length >= 1) {
                num = 0;
            }
            text = pageText[num];
            if (text.split("\\s").length <= 5) {
                try{
                    text = pageText[num - 1] + text;
                }catch (Exception ex){
                    text = "";
                }
            }
            Log.e("onBindViewHolder: PageText", text);
//            Log.e("onBindViewHolder: Text", pageText);
            sumtextList.add(text);
            pageAdapter.notifyItemInserted(i);
        } catch (IOException e) {
            Log.e("onBindViewHolder: Error", e.toString());
        }
    }
}