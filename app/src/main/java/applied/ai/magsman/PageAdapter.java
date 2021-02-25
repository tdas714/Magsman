package applied.ai.magsman;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static applied.ai.magsman.MediaPlayerService.Broadcast_PLAY_NEW_AUDIO;

//import static applied.ai.magsman.MediaPlayerService.Broadcast_PLAY_NEW_AUDIO;

public class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;

    public List<Integer> mItemList;
    public List<String> mSumList;
    public String mfilename;
    private Context mcontext;
//    private MediaPlayerService player;
//    boolean serviceBound = false;
    private MediaPlayerService player;
    boolean serviceBound = false;

    public PageAdapter(List<Integer> itemList, List<String> sumList, String filename, Context context) {
        mItemList = itemList;
        mSumList = sumList;
        mfilename = filename;
        mcontext = context;

    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_item, parent, false);
            return new ItemViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ItemViewHolder) {
            populateItemRows((ItemViewHolder) viewHolder, position);
        } else if (viewHolder instanceof LoadingViewHolder) {
            showLoadingView((LoadingViewHolder) viewHolder, position);
        }
    }
    @Override
    public int getItemCount() {
        return mItemList == null ? 0 : mItemList.size();
    }
    /**
     * The following method decides the type of ViewHolder to display in the RecyclerView
     *
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        return mItemList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }
    private class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem, sumItem;
        ImageView playItem;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.page_num_txt);
            sumItem = itemView.findViewById(R.id.page_summary_txt);
            playItem = itemView.findViewById(R.id.page_play_btn);
        }
    }
    private class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
    private void showLoadingView(LoadingViewHolder viewHolder, int position) {
        //ProgressBar would be displayed
    }
    private void populateItemRows(ItemViewHolder viewHolder, int position) {

        int item = mItemList.get(position);
        viewHolder.tvItem.setText("Page "+item);
        viewHolder.sumItem.setText(mSumList.get(position));
        viewHolder.playItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("onClick: ", "Clicked");
                File data = Utils.getFile(mfilename, (item-1)+".wav");
                if(data.exists()){
                    if(!serviceBound){
                        Intent playerIntent = new Intent(mcontext.getApplicationContext(), MediaPlayerService.class);
                        playerIntent.putExtra("filename", mfilename);
                        playerIntent.putExtra("pagenum", mItemList.get(position)-1);
                        mcontext.getApplicationContext().startService(playerIntent);
                        mcontext.getApplicationContext().bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                    }else{
                        Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                        broadcastIntent.putExtra("filename", mfilename);
                        broadcastIntent.putExtra("pagenum", mItemList.get(position)-1);
                        mcontext.getApplicationContext().sendBroadcast(broadcastIntent);
                    }
                }else{
                    Toast.makeText(mcontext, "Current page not yet processed. Add the book from Magsman folder in your Internal Storage.", Toast.LENGTH_LONG).show();

                }
            }
        });

    }
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

//            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    
}
