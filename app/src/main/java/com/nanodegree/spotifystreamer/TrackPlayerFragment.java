package com.nanodegree.spotifystreamer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;


public class TrackPlayerFragment extends DialogFragment implements View.OnClickListener,
                                            MediaPlayer.OnPreparedListener,
                                            MediaPlayer.OnCompletionListener,
                                            MediaPlayer.OnSeekCompleteListener,
                                            SeekBar.OnSeekBarChangeListener {

    public static String TAG = TrackPlayerFragment.class.getSimpleName();

    private ImageButton playBtn;
    private ImageButton pauseBtn;
    private ImageButton previousBtn;
    private ImageButton nextBtn;
    private SeekBar playSeekBar;
    private TopTracksActivityFragment.SongInfo songInfoArray[] = null;
    private int currentPosition = -1;
    private PlayCountdownTimer countdownTimer = null;
    private MediaPlayer mediaPlayer;
    private int seekBarPosition = 0;
    private ImageView albumImg;
    private TextView artistNameTxtView;
    private TextView albumNameTxtView;
    private TextView trackNameTxtView;

    public TrackPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        Bundle arguments = getArguments();

        if (arguments != null) {
             songInfoArray = (TopTracksActivityFragment.SongInfo[])
                            arguments.getParcelableArray(getActivity().getString(R.string.songinfo_object_key));
            currentPosition = arguments.getInt(getActivity().getString(R.string.songinfo_current_pos_key));
        }

        if ((songInfoArray == null) || (currentPosition < 0)) {
            return rootView;
        }

        albumImg = (ImageView) rootView.findViewById(R.id.album_art_img_id);
        artistNameTxtView = (TextView) rootView.findViewById(R.id.artist_name_txt_id);
        albumNameTxtView = (TextView) rootView.findViewById(R.id.album_name_txt_id);
        trackNameTxtView = (TextView) rootView.findViewById(R.id.track_name_txt_id);

        setCurrentSongUi();

        playBtn = (ImageButton) rootView.findViewById(R.id.play_btn_id);
        playBtn.setOnClickListener(this);

        pauseBtn = (ImageButton) rootView.findViewById(R.id.pause_btn_id);
        pauseBtn.setOnClickListener(this);

        previousBtn = (ImageButton) rootView.findViewById(R.id.previous_btn_id);
        previousBtn.setOnClickListener(this);

        nextBtn = (ImageButton) rootView.findViewById(R.id.next_btn_id);
        nextBtn.setOnClickListener(this);

        playSeekBar = (SeekBar) rootView.findViewById(R.id.play_seekbar_id);
        playSeekBar.setOnSeekBarChangeListener(this);
        setNextPrevButtonState();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        releaseResources();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_btn_id:
                handlePlayBtnClick();
                break;
            case R.id.pause_btn_id:
                handlePauseBtnClick();
                break;
            case R.id.previous_btn_id:
                handlePreviousBtnClick();
                break;
            case R.id.next_btn_id:
                handleNextBtnClick();
                break;
        }
    }

    private void handlePlayBtnClick() {
        playBtn.setVisibility(View.INVISIBLE);
        pauseBtn.setVisibility(View.VISIBLE);
        try {
            playCurrentSong();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePauseBtnClick() {
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
        seekBarPosition = playSeekBar.getProgress();
        if ((mediaPlayer != null) && (mediaPlayer.isPlaying())) {
            mediaPlayer.pause();
            if (countdownTimer != null) {
                countdownTimer.cancel();
            }
        }
    }

    private void handlePreviousBtnClick() {
        releaseResources();
        --currentPosition;
        if (currentPosition < 0) {
            currentPosition = 0;
        }
        setNextPrevButtonState();
        setCurrentSongUi();
        resetUiElements();
    }

    private void handleNextBtnClick() {
        releaseResources();
        ++currentPosition;
        if (currentPosition >= songInfoArray.length) {
            currentPosition = songInfoArray.length - 1;
        }
        setNextPrevButtonState();
        setCurrentSongUi();
        resetUiElements();
    }

    private void setCurrentSongUi() {
        if ((songInfoArray[currentPosition].artWorkUrl != null) && (!songInfoArray[currentPosition].artWorkUrl.isEmpty())) {
            Picasso.with(getActivity()).load(songInfoArray[currentPosition].artWorkUrl).into(albumImg);
        }
        artistNameTxtView.setText(songInfoArray[currentPosition].artistName);
        albumNameTxtView.setText(songInfoArray[currentPosition].albumName);
        trackNameTxtView.setText(songInfoArray[currentPosition].trackName);
    }

    void setNextPrevButtonState() {
        if (currentPosition <= 0) {
            previousBtn.setEnabled(false);
        } else {
            previousBtn.setEnabled(true);
        }

        if (currentPosition >= (songInfoArray.length - 1)) {
            nextBtn.setEnabled(false);
        } else {
            nextBtn.setEnabled(true);
        }
    }

    private void releaseResources() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        seekBarPosition = 0;
    }

    private void playCurrentSong() throws IOException {
        if ((songInfoArray == null) || UtilClass.isEmptyOrNull(songInfoArray[currentPosition].previewUrl)) {
            return;
        }
        if (mediaPlayer == null) {
            createMediaPlayer();
        } else if (!mediaPlayer.isPlaying()){
            // media player might have been paused
            seekAndPlay();
        }
    }

    private void createMediaPlayer() throws IOException {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(songInfoArray[currentPosition].previewUrl);
        mediaPlayer.prepareAsync();
    }

    private void seekAndPlay() {
        mediaPlayer.seekTo((int) ((seekBarPosition * mediaPlayer.getDuration()) / 100.0));
    }

    private void resetCountdownTimer() {
        int remainingTime = (int)(mediaPlayer.getDuration() - (seekBarPosition * mediaPlayer.getDuration()/100.0));
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        countdownTimer = new PlayCountdownTimer(remainingTime, 300); // all times in ms
        countdownTimer.start();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        seekAndPlay();
    }

    @Override
    public void onCompletion(MediaPlayer mplayer) {
        seekBarPosition = 0;
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        resetUiElements();
    }

    private void resetUiElements() {
        playSeekBar.setProgress(0);
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        resetCountdownTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            seekBarPosition = progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if ((mediaPlayer != null) && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo((int) ((seekBarPosition * mediaPlayer.getDuration()) / 100.0));
        }
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

            playSeekBar.setProgress((int) (mediaPlayer.getCurrentPosition() * 100.0) / mediaPlayer.getDuration());
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "Countdown timer finished");
        }
    }
}
