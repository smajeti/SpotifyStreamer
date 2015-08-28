package com.nanodegree.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nanodegree.spotifystreamer.MainActivity;
import com.nanodegree.spotifystreamer.R;
import com.nanodegree.spotifystreamer.TopTracksActivityFragment;
import com.nanodegree.spotifystreamer.UtilClass;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class MusicPlayService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {

    public static String TAG = MusicPlayService.class.getSimpleName();
    public static int NOTIFICATION_ID = 1717;

    private final IBinder localBinder = new LocalBinder();
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private PlayCountdownTimer countdownTimer = null;
    private Callback callback = null;
    private Parcelable songInfoArray[] = null;
    private int currentPosition = -1;
    private int seekPosition = 0;
    private boolean isPlayPaused = false;


    public class LocalBinder extends Binder {
        public MusicPlayService getService() {
            return MusicPlayService.this;
        }
    }

    public interface Callback {
        void onProgressUpdate(int progress, int elapsedTime);
        void onPreparingSongPlay(int position);
        void onPlayStarted(int position, long duration);
        void onDonePlay();
        void onError(Exception ex);
    }

    @Override
    public void onCreate() {
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        releaseResources();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        seekAndPlay();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        resetCountdownTimer();
        if (callback != null) {
            callback.onPlayStarted(currentPosition, mediaPlayer.getDuration());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        prepareForNextSongPlay();
        ++currentPosition;
        if (currentPosition >= songInfoArray.length) {
            startOrStopForeground();
            if (callback != null) {
                callback.onDonePlay();
            }
            return;
        } else {
            updateNotification();
        }
        playCurrentSong();
    }

    public void setSongInfo(Parcelable songInfoArray[], int currentPosition) {
        this.songInfoArray = songInfoArray;
        this.currentPosition = currentPosition;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setSeekPosition(int seekPosition) {
        this.seekPosition = seekPosition;
        if (mediaPlayer.isPlaying()) {
            seekAndPlay();
        }
    }

    public void playCurrentSong() {
        if (songInfoArray == null) {
            return;
        }

        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        if  (UtilClass.isEmptyOrNull(songInfo.previewUrl)) {
            return;
        }

        try {
            if (isPlayPaused && !mediaPlayer.isPlaying()) {
                seekAndPlay();
            } else {
                resetMediaPlayer();
            }
            isPlayPaused = false;
        } catch (IOException e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public void playSong(int position) {
        currentPosition = position;
        try {
            resetMediaPlayer();
        } catch (IOException e) {
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public void pausePlayback() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (countdownTimer != null) {
                countdownTimer.cancel();
                countdownTimer = null;
            }
            isPlayPaused = true;
        }
    }

    public void startOrStopForeground() {
        if ((!mediaPlayer.isPlaying()) ) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            stopForeground(true);
            return;
        }

        startForegroundService();
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public Parcelable[] getSongInfoArray() {
        return songInfoArray;
    }

    private void resetMediaPlayer() throws IOException {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        if (mediaPlayer.isPlaying()) {
            if (countdownTimer != null) {
                countdownTimer.cancel();
                countdownTimer = null;
            }
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        mediaPlayer.setDataSource(songInfo.previewUrl);
        mediaPlayer.prepareAsync();
        if (callback != null) {
            callback.onPreparingSongPlay(currentPosition);
        }
    }

    private void seekAndPlay() {
        mediaPlayer.seekTo((int) ((seekPosition * mediaPlayer.getDuration()) / 100.0));
    }

    private void releaseResources() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        songInfoArray = null;
        currentPosition = -1;
        seekPosition = 0;
        callback = null;
    }

    private void prepareForNextSongPlay() {
        seekPosition = 0;
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private void resetCountdownTimer() {
        int remainingTime = (int)(mediaPlayer.getDuration() - (seekPosition * mediaPlayer.getDuration()/100.0));
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        countdownTimer = new PlayCountdownTimer(remainingTime, 300); // all times in ms
        countdownTimer.start();
    }

    private void startForegroundService() {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        new BitmapLoader(false).execute(songInfo.artWorkUrlSmall);
    }

    private void updateNotification() {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        new BitmapLoader(true).execute(songInfo.artWorkUrlSmall);
    }

    private void updateNotification(Bitmap smallImgBitmap) {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
    }

    private class PlayCountdownTimer extends CountDownTimer {

        public PlayCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
//            Log.d(TAG, "Countdown timer ontick ");
            if (mediaPlayer == null) {
                return;
            }

            if (callback != null) {
                int elapsedTime = mediaPlayer.getCurrentPosition();
                callback.onProgressUpdate(((int) (mediaPlayer.getCurrentPosition() * 100.0) / mediaPlayer.getDuration()), elapsedTime);
            }
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "Countdown timer finished");
        }
    }

    private class BitmapLoader extends AsyncTask<String, Void, Bitmap> {
        private boolean updateNotification;

        public BitmapLoader(boolean updateNotification) {
            this.updateNotification = updateNotification;
        }

        @Override
        protected Bitmap doInBackground(String... artWorkUrls) {
            String imageUrl = artWorkUrls[0];
            Bitmap smallImgBitmap = null;
            if (!UtilClass.isEmptyOrNull(imageUrl)) {
                try {
                    smallImgBitmap = Picasso.with(MusicPlayService.this).load(imageUrl).get();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to fetch Bitmap", e);
                }
            }

            return smallImgBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap smallImgBitmap) {
            createNotification(smallImgBitmap);
        }

        private void createNotification(Bitmap smallImgBitmap) {
            TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
            Notification.Builder builder = new Notification.Builder(MusicPlayService.this);
            builder.setContentTitle(getString(R.string.now_playing_str));
            builder.setSmallIcon(android.R.drawable.ic_media_play);
            builder.setLargeIcon(smallImgBitmap);
            builder.setContentText(songInfo.trackName);
            builder.setAutoCancel(true);
            Intent notificationIntent = new Intent(MusicPlayService.this, MainActivity.class);
            notificationIntent.putExtra(getString(R.string.notification_intent_key), true);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(MusicPlayService.this, 0, notificationIntent, 0);
            builder.setContentIntent(pendingIntent);
            Notification notification = builder.build();
            if (updateNotification) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, notification);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.d(TAG, "created notification");
        }
    }

}
