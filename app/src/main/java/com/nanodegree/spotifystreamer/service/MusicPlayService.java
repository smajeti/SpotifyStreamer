package com.nanodegree.spotifystreamer.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nanodegree.spotifystreamer.TopTracksActivityFragment;
import com.nanodegree.spotifystreamer.UtilClass;

import java.io.IOException;

public class MusicPlayService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {

    public static String TAG = MusicPlayService.class.getSimpleName();

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
        void onProgressUpdate(int progress);
        void onPreparingSongPlay(int position);
        void onPlayStarted(int position);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
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
            callback.onPlayStarted(currentPosition);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        prepareForNextSongPlay();
        ++currentPosition;
        if (currentPosition >= songInfoArray.length) {
            callback.onDonePlay();
            return;
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

    private class PlayCountdownTimer extends CountDownTimer {

        public PlayCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            Log.d(TAG, "Countdown timer ontick ");
            if (mediaPlayer == null) {
                return;
            }

            if (callback != null) {
                callback.onProgressUpdate((int) (mediaPlayer.getCurrentPosition() * 100.0) / mediaPlayer.getDuration());
            }
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "Countdown timer finished");
        }
    }

}
