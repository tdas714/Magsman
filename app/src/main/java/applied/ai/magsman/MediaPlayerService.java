package applied.ai.magsman;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.IOException;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    //path to the audio file
    private String mediaFile;
    private int pageNum;
    private Book b;
    //Used to pause/resume MediaPlayer
    private int resumePosition;
    private AudioManager audioManager;
    private Audio audio;
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    public static final String Broadcast_PLAY_NEW_AUDIO = "applied.ai.magsman.PlayNewAudio";

    public static final String ACTION_PLAY = "applied.ai.magsman.ACTION_PLAY";
    public static final String ACTION_PAUSE = "applied.ai.magsman.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "applied.ai.magsman.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "applied.ai.magsman.ACTION_NEXT";
    public static final String ACTION_STOP = "applied.ai.magsman.ACTION_STOP";
    private static final String CHANNEL_ID = "MediaPlayerNotify";
    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    private NotificationManager notificationManager;

    private final IBinder iBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        register_playNewAudio();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
//        //Invoked when playback of a media source has completed.
//        stopMedia();
//        //stop the service
//        stopSelf();
        skipToNext();
        buildNotification(PlaybackStatus.PLAYING);
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }


    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            //An audio file is passed to the service through putExtra();
            mediaFile = intent.getExtras().getString("filename");
            pageNum = intent.getExtras().getInt("pagenum");
            audio = new Audio(mediaFile);
            DBHelper db = new DBHelper(getApplicationContext());
            b = db.findBook(mediaFile);
            db.close();
        } catch (NullPointerException e) {
            stopSelf();
        }
        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
                buildNotification(PlaybackStatus.PLAYING);
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
                buildNotification(PlaybackStatus.NOT_PROCESSED);
            }
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
//        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }



    //=====================================================================================
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(audio.getData(pageNum));
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
            buildNotification(PlaybackStatus.NOT_PROCESSED);
        }
    }
//=====================================================================================
    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }
//=================================================================================
    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }
//===================================================================================
//Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }
//====================================================================================
    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }
//====================================================================================
private void initMediaSession() throws RemoteException {
    if (mediaSessionManager != null) return; //mediaSessionManager exists

    mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
    // Create a new MediaSession
    mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
    //Get MediaSessions transport controls
    transportControls = mediaSession.getController().getTransportControls();
    //set MediaSession -> ready to receive media commands
    mediaSession.setActive(true);
    //indicate that the MediaSession handles transport control commands
    // through its MediaSessionCompat.Callback.
    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

    //Set mediaSession's MetaData
    updateMetaData();

    // Attach Callback to receive MediaSession updates
    mediaSession.setCallback(new MediaSessionCompat.Callback() {
        // Implement callbacks
        @Override
        public void onPlay() {
            super.onPlay();
            resumeMedia();
            buildNotification(PlaybackStatus.PLAYING);
        }

        @Override
        public void onPause() {
            super.onPause();
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            skipToNext();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            skipToPrevious();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }

        @Override
        public void onStop() {
            super.onStop();
            removeNotification();
            //Stop the service
            stopSelf();
        }

        @Override
        public void onSeekTo(long position) {
            super.onSeekTo(position);
        }
    });
}

    private void updateMetaData() {
//        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
//                R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, audio.getCover())
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, audio.getPreview(pageNum))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, audio.getTitle())
                .build());
    }
//======================================================================================
    private void skipToNext() {

        if (pageNum == b.getTotalpage() - 1) {
            //if last in playlist
            pageNum = 0;
    //        activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            pageNum = ++pageNum;
        }

        //Update stored index
    //    new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    private void skipToPrevious() {

        if (pageNum == 0) {
            //if first in playlist
            //set index to the last of audioList
            pageNum = b.getTotalpage() - 1;
//            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            pageNum = --pageNum;
        }

        //Update stored index
//        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }
//=========================================================================================
    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;
        boolean seton = false;
        String contentText = "Page: "+(pageNum+1);
        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
            seton = true;
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        } else if(playbackStatus == PlaybackStatus.NOT_PROCESSED){
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
            contentText += "(This page not processed yet.)";
        }

    //    Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
    //            R.drawable.image); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorAccent))
                // Set the large and small icons
                .setLargeIcon(audio.getCover())
                .setSmallIcon(R.mipmap.ic_notification_foreground)
                // Set Notification content information
                .setContentText(contentText)
                .setContentTitle("Book: "+audio.getTitle()+".pdf")
    //            .setContentInfo(activeAudio.getTitle())
                .setSound(null)

                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));
        if(seton){
            notificationBuilder.setOngoing(true);
        }else{
            notificationBuilder.setOngoing(false);
        }
        createNotificationChannel(getApplicationContext(), notificationManager);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AudioBook";
            String description = "This is audiobook playing";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void removeNotification() {
        Log.e("removeNotification: ", "Called");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
//===========================================================================================
    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }
//============================================================================================
    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }
//==============================================================================================
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            mediaFile = intent.getExtras().getString("filename");
            pageNum = intent.getExtras().getInt("pagenum");
            audio = new Audio(mediaFile);
            DBHelper db = new DBHelper(getApplicationContext());
            b = db.findBook(mediaFile);
            db.close();
            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e("onTaskRemoved: ", "Called");
        onDestroy();
    }
}
