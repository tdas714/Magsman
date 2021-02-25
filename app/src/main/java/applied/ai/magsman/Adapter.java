package applied.ai.magsman;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// The adapter class which
// extends RecyclerView Adapter
public class Adapter extends RecyclerView.Adapter<Adapter.MyView> {

    // List with String type
    private List<String> list;
    private List<Bitmap> coverimg;
    private int lastPosition = 0;
    // View Holder class which
    // extends RecyclerView.ViewHolder
    public static class MyView extends RecyclerView.ViewHolder {

        // Text View
        TextView textView;
        ImageView coverimgView;
        // parameterised constructor for View Holder class
        // which takes the view as a parameter
        public MyView(View view)
        {
            super(view);
            // initialise TextView with id
            textView = (TextView)view.findViewById(R.id.card_title_txt);
            coverimgView = view.findViewById(R.id.card_cover);
        }
    }

    // Constructor for adapter class
    // which takes a list of String type
    public Adapter(List<String> horizontalList, List<Bitmap> coveList)
    {
        this.list = horizontalList;
        this.coverimg = coveList;
    }

    // Override onCreateViewHolder which deals
    // with the inflation of the card layout
    // as an item for the RecyclerView.
    @Override
    public MyView onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // Inflate item.xml using LayoutInflator
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);

        // return itemView
        return new MyView(itemView);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull MyView holder) {
        super.onViewAttachedToWindow(holder);
        Intent intent = new Intent("FromAdapterToActivity");
        intent.putExtra("filename", holder.textView.getText()+".pdf");
        LocalBroadcastManager.getInstance(holder.itemView.getContext()).sendBroadcast(intent);
    }

    // Override onBindViewHolder which deals
    // with the setting of different data
    // and methods related to clicks on
    // particular items of the RecyclerView.
    @Override
    public void onBindViewHolder(final MyView holder,
                                 int position)
    {

        // Set the text of each item of
        // Recycler view with the list items
        position = position % list.size();
        holder.textView.setText(list.get(position).split(".pdf")[0]);
//        holder.coverimg.setImageDrawable(coverimg.get(position));
        holder.coverimgView.setImageBitmap(coverimg.get(position));
//
//        Intent intent = new Intent("FromAdapterToActivity");
//        intent.putExtra("filename", list.get(position));
//        LocalBroadcastManager.getInstance(holder.itemView.getContext()).sendBroadcast(intent);

        Palette.from(coverimg.get(position)).generate(new Palette.PaletteAsyncListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onGenerated(Palette palette) {
                Log.e("onGenerated: Pallate", String.valueOf(palette));
//                //Set normal shade to textview
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
//
                Log.e("onGenerated: ", String.valueOf(vibc));
                //Set darkershade to textview
//                    int vibrantDarkColor = palette
//                            .getDarkVibrantColor(context.getResources().getColor(R.color.colorPrimaryDark));
//                Color c = Color.valueOf(vibc.getRgb());
//                Random r = new Random();
//                final int min = 100;
//                final int max = 255;
//                final int random1 = new Random().nextInt((max - min) + 1) + min;
//                final int random2 = new Random().nextInt((max - min) + 1) + min;
//                final int random3 = new Random().nextInt((max - min) + 1) + min;
//                Log.e("onGenerated: ", String.valueOf(random1));
//                Log.e("onGenerated: ", String.valueOf(random2));
//                Log.e("onGenerated: ", String.valueOf(random3));
//                int textc = Color.rgb(random1-c.red(),random2-c.green(), random3-c.blue());
                try{
                    holder.textView.setTextColor(vibc.getRgb());
                }catch (Exception ex){
                    holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.colorAccent));
                }
            }
        });



//        scaleView(holder.itemView, 0f, 0.6f);
    }


    // Override getItemCount which Returns
    // the length of the RecyclerView.
    @Override
    public int getItemCount()
    {
        return Integer.MAX_VALUE;
    }

}
