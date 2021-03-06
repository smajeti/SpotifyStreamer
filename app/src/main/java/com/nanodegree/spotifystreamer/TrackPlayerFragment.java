package com.nanodegree.spotifystreamer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
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
    private TextView durationLeftTxtView;
    private TextView durationRightTxtView;
    private MusicPlayService playService;
    private long sampleDuration;
    private boolean launchedFromNotification;

    private Parcelable songInfoArray[] = null;
    private int currentPosition = -1;


    public TrackPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent startIntent = new Intent(getActivity(), MusicPlayService.class);
        getActivity().startService(startIntent);
        Log.d(TAG, "Starting service");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_track_player, container, false);

        Bundle arguments = getArguments();

        launchedFromNotification = false;
        if (arguments != null) {
            currentPosition = arguments.getInt(getActivity().getString(R.string.songinfo_current_pos_key), -1);
            if (arguments.containsKey(getActivity().getString(R.string.songinfo_object_key))) {
                songInfoArray = (Parcelable[])
                        arguments.getParcelableArray(getActivity().getString(R.string.songinfo_object_key));
            }
            launchedFromNotification = arguments.getBoolean(getActivity().getString(R.string.notification_intent_key), false);
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
        if (!launchedFromNotification) {
            setNextPrevButtonState();
        }

        waitProgressBar = (ProgressBar) rootView.findViewById(R.id.wait_progress_bar_id);

        durationLeftTxtView = (TextView) rootView.findViewById(R.id.duration_left_id);
        durationRightTxtView = (TextView) rootView.findViewById(R.id.duration_right_id);

        if (!launchedFromNotification) {
            setCurrentSongUi();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent bindIntent = new Intent(getActivity(), MusicPlayService.class);
        getActivity().bindService(bindIntent, this, 0);
        Log.d(TAG, "Binding to service");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (playService != null) {
            playService.startOrStopForeground();
            getActivity().unbindService(this);
            playService.setCallback(null);
            Log.d(TAG, "unbound service");
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
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected");
        playService = ((MusicPlayService.LocalBinder)iBinder).getService();
        if (launchedFromNotification) {
            songInfoArray = playService.getSongInfoArray();
            currentPosition = playService.getCurrentPosition();
            setNextPrevButtonState();
            setCurrentSongUi();
            if (playService.isPlaying()) {
                playBtn.setVisibility(View.INVISIBLE);
                pauseBtn.setVisibility(View.VISIBLE);
            }
        } else {
            playService.setSeekPosition(0);
            handlePlayBtnClick();
        }
        playService.setCallback(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected");
        playService = null;
    }

    @Override
    public void onProgressUpdate(int progress, int elapsedTime) {
        playSeekBar.setProgress(progress);
        durationLeftTxtView.setText(String.format("%.2f", elapsedTime / 1000.0)); // in seconds
        durationRightTxtView.setText(String.format("%.2f", (sampleDuration - elapsedTime) / 1000.0)); // in seconds
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
        sampleDuration = duration;
        waitProgressBar.setVisibility(View.INVISIBLE);
        enableUiElements(true);
        this.currentPosition = position;
        playBtn.setVisibility(View.INVISIBLE);
        pauseBtn.setVisibility(View.VISIBLE);
        durationRightTxtView.setText(String.format("%.2f", sampleDuration / 1000.0)); // in seconds
        setNextPrevButtonState();
    }

    @Override
    public void onDonePlay() {
        playBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.INVISIBLE);
        durationLeftTxtView.setText(String.format("%.2f", sampleDuration / 1000.0)); // in seconds
        durationRightTxtView.setText("0.00");
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
            playCurrentPosSong();
        }
    }

    private void handleNextBtnClick() {
        ++currentPosition;
        if (currentPosition >= songInfoArray.length) {
            currentPosition = songInfoArray.length - 1;
        } else {
            playCurrentPosSong();
        }
    }

    private void setCurrentSongUi() {
        TopTracksActivityFragment.SongInfo songInfo = (TopTracksActivityFragment.SongInfo) songInfoArray[currentPosition];
        if ((songInfo.artWorkUrlBig != null) && (!songInfo.artWorkUrlBig.isEmpty())) {
            Picasso.with(getActivity()).load(songInfo.artWorkUrlBig).into(albumImg);
        }
        artistNameTxtView.setText(songInfo.artistName);
        albumNameTxtView.setText(songInfo.albumName);
        trackNameTxtView.setText(songInfo.trackName);
        durationLeftTxtView.setText("0.00");
        durationRightTxtView.setText("0.00");
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

    private void playCurrentPosSong() {
        if (raiseToastIfNetworkNotAvailable()) {
            return;
        }
        if (playService != null) {
            playService.setSeekPosition(0);
            playService.playSong(currentPosition);
        }
    }
}
