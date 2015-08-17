package com.nanodegree.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nanodegree.spotifystreamer.service.MusicPlayService;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.IOException;


public class TrackPlayerFragment extends DialogFragment implements View.OnClickListener,
                                            SeekBar.OnSeekBarChangeListener,
                                            ServiceConnection,
                                            MusicPlayService.Callback {

    public static String TAG = TrackPlayerFragment.class.getSimpleName();

    private ImageButton playBtn;
    private ImageButton pauseBtn;
    private ImageButton previousBtn;
    private ImageButton nextBtn;
    private SeekBar playSeekBar;
    private ImageView albumImg;
    private TextView artistNameTxtView;
    private TextView albumNameTxtView;
    private TextView trackNameTxtView;
    private ProgressBar waitProgressBar;
    private TextView durationRightTxtView;
    private MusicPlayService playService;

    private Parcelable songInfoArray[] = null;
    private int currentPosition = -1;

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
            currentPosition = arguments.getInt(getActivity().getString(R.string.songinfo_current_pos_key));
            songInfoArray = (Parcelable[])
                    arguments.getParcelableArray(getActivity().getString(R.string.songinfo_object_key));
        }

        if ((songInfoArray == null) || (currentPosition < 0)) {
            return rootView;
        }

        albumImg = (ImageView) rootView.findViewById(R.id.album_art_img_id);
        artistNameTxtView = (TextView) rootView.findViewById(R.id.artist_name_txt_id);
        albumNameTxtView = (TextView) rootView.findViewById(R.id.album_name_txt_id);
        trackNameTxtView = (TextView) rootView.findViewById(R.id.track_name_txt_id);

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

        waitProgressBar = (ProgressBar) rootView.findViewById(R.id.wait_progress_bar_id);

        durationRightTxtView = (TextView) rootView.findViewById(R.id.duration_right_id);

        setCurrentSongUi();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(getActivity(), MusicPlayService.class);
        getActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (playService != null) {
            getActivity().unbindService(this);
            playService = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        playService = ((MusicPlayService.LocalBinder)iBinder).getService();
        playService.setCallback(this);
        playService.setSeekPosition(0);
        handlePlayBtnClick();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        playService = null;
    }

    @Override
    public void onProgressUpdate(int progress) {
        playSeekBar.setProgress(progress);
    }

    @Override
    public void onPreparingSongPlay(int position) {
        this.currentPosition = position;
        waitProgressBar.setVisibility(View.VISIBLE);
        setCurrentSongUi();
        enableUiElements(false);
    }

    @Override
    public void onPlayStarted(int position, long duration) {
        waitProgressBar.setVisibility(View.INVISIBLE);
        enableUiElements(true);
        this.currentPosition = position;
        playBtn.setVisibility(View.INVISIBLE);
        pauseBtn.setVisibility(View.VISIBLE);
        durationRightTxtView.setText(String.format("%.2f", duration / 1000.0)); // in seconds
        setNextPrevButtonState();
    }

    @Override
    public void onDonePlay() {
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onError(Exception ex) {
        Toast.makeText(getActivity(), getResources().getString(R.string.error_playing_song), Toast.LENGTH_SHORT).show();
        enableUiElements(true);
    }

    private boolean raiseToastIfNetworkNotAvailable() {
        if (!UtilClass.isNetworkAvailable(getActivity())) {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && (playService != null)) {
            playService.setSeekPosition(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void handlePlayBtnClick() {

        if (raiseToastIfNetworkNotAvailable()) {
            return;
        }

        if (playService == null) {
            return;
        }

        playService.setSongInfo(songInfoArray, currentPosition);
        playService.playCurrentSong();
    }

    private void handlePauseBtnClick() {
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);

        if (playService != null) {
            playService.pausePlayback();
            int seekBarPosition = playSeekBar.getProgress();
            playService.setSeekPosition(seekBarPosition);
        }
    }

    private void handlePreviousBtnClick() {
        --currentPosition;
        if (currentPosition < 0) {
            currentPosition = 0;
        } else {
            if (raiseToastIfNetworkNotAvailable()) {
                return;
            }
            if (playService != null) {
                playService.setSeekPosition(0);
                playService.playSong(currentPosition);
            }
        }
    }

    private void handleNextBtnClick() {
        ++currentPosition;
        if (currentPosition >= songInfoArray.length) {
            currentPosition = songInfoArray.length - 1;
        } else {
            if (raiseToastIfNetworkNotAvailable()) {
                return;
            }
            if (playService != null) {
                playService.setSeekPosition(0);
                playService.playSong(currentPosition);
            }
        }
    }

    private void setCurrentSongUi() {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        if ((songInfo.artWorkUrl != null) && (!songInfo.artWorkUrl.isEmpty())) {
            Picasso.with(getActivity()).load(songInfo.artWorkUrl).into(albumImg);
        }
        artistNameTxtView.setText(songInfo.artistName);
        albumNameTxtView.setText(songInfo.albumName);
        trackNameTxtView.setText(songInfo.trackName);
        setNextPrevButtonState();
    }

    private void setNextPrevButtonState() {
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

    private void enableUiElements(boolean enable) {
        playSeekBar.setEnabled(enable);
        playBtn.setEnabled(enable);
        pauseBtn.setEnabled(enable);
        nextBtn.setEnabled(enable);
        previousBtn.setEnabled(enable);
    }
}
