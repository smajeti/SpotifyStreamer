package com.nanodegree.spotifystreamer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import kaaes.spotify.webapi.android.models.Track;


public class TrackPlayerFragment extends DialogFragment implements View.OnClickListener,
                                            MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private ImageButton playBtn;
    private ImageButton pauseBtn;
    private ImageButton previousBtn;
    private ImageButton nextBtn;
    private SeekBar playSeekBar;
    private TopTracksActivityFragment.SongInfo songInfo = null;
    private PlayCountdownTimer countdownTimer = null;
    private MediaPlayer mediaPlayer;

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
             songInfo = (TopTracksActivityFragment.SongInfo)
                            arguments.get(getActivity().getString(R.string.songinfo_object_key));
        }

        if (songInfo == null) {
            return rootView;
        }

        ImageView albumImg = (ImageView) rootView.findViewById(R.id.album_art_img_id);
        if ((songInfo.artWorkUrl != null) && (!songInfo.artWorkUrl.isEmpty())) {
            Picasso.with(getActivity()).load(songInfo.artWorkUrl).into(albumImg);
        }

        TextView artistNameTxtView = (TextView) rootView.findViewById(R.id.artist_name_txt_id);
        artistNameTxtView.setText(songInfo.artistName);

        TextView albumNameTxtView = (TextView) rootView.findViewById(R.id.album_name_txt_id);
        albumNameTxtView.setText(songInfo.albumName);

        TextView trackNameTxtView = (TextView) rootView.findViewById(R.id.track_name_txt_id);
        trackNameTxtView.setText(songInfo.trackName);

        playBtn = (ImageButton) rootView.findViewById(R.id.play_btn_id);
        playBtn.setOnClickListener(this);

        pauseBtn = (ImageButton) rootView.findViewById(R.id.pause_btn_id);
        pauseBtn.setOnClickListener(this);

        previousBtn = (ImageButton) rootView.findViewById(R.id.previous_btn_id);
        previousBtn.setOnClickListener(this);

        nextBtn = (ImageButton) rootView.findViewById(R.id.next_btn_id);
        nextBtn.setOnClickListener(this);

        playSeekBar = (SeekBar) rootView.findViewById(R.id.play_seekbar_id);

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
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

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        countdownTimer = new PlayCountdownTimer(mediaPlayer.getDuration(), 200);
        countdownTimer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mplayer) {
        playSeekBar.setProgress(0);
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
        this.mediaPlayer.release();
        this.mediaPlayer = null;
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

    private void playCurrentSong() throws IOException {
        if ((songInfo == null) || UtilClass.isEmptyOrNull(songInfo.previewUrl)) {
            return;
        }
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(songInfo.previewUrl);
            mediaPlayer.prepareAsync();
        } else if (!mediaPlayer.isPlaying()){
            // media player might have been paused
            mediaPlayer.start();
            int remainingTime = mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition();
            countdownTimer = new PlayCountdownTimer(remainingTime, 200); // all time in ms
            countdownTimer.start();
        }
    }

    private void handlePauseBtnClick() {
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
        if ((mediaPlayer != null) && (mediaPlayer.isPlaying())) {
            mediaPlayer.pause();
            if (countdownTimer != null) {
                countdownTimer.cancel();
            }
        }
    }

    private void handlePreviousBtnClick() {

    }

    private void handleNextBtnClick() {

    }

    private class PlayCountdownTimer extends CountDownTimer {

        public PlayCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            if (mediaPlayer == null) {
                return;
            }

            playSeekBar.setProgress((int)(mediaPlayer.getCurrentPosition() * 100.0)/mediaPlayer.getDuration());

        }

        @Override
        public void onFinish() {
        }
    }
}
