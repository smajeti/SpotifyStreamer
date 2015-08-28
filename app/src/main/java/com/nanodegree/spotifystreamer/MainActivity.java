package com.nanodegree.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements MainActivityFragment.Callback, TopTracksActivityFragment.Callback {

    private static final String TOP_TACKS_FRAGMENT_TAG = "TOPTRACKSFRAGMENT_TAG";
    private boolean twoPaneMode;
    private boolean launchedFromNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.top_tracks_container) != null) {
            twoPaneMode = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.top_tracks_container, new TopTracksActivityFragment(), TOP_TACKS_FRAGMENT_TAG)
                        .commit();
            }
        } else {
            twoPaneMode = false;
        }

        Intent intent = getIntent();
        launchedFromNotification = false;
        if ((intent != null) && (intent.hasExtra(getString(R.string.notification_intent_key)))) {
            launchedFromNotification = true;
        }

        if (launchedFromNotification) {
            if (twoPaneMode) {
                Bundle arguments = new Bundle();
                arguments.putBoolean(getString(R.string.notification_intent_key), true);
                launchTrackPlayer(arguments);
            } else {
                UtilClass.launchTrackplayerAcitivity(this, null, -1, true);
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        launchedFromNotification = false;
        if ((intent != null) && (intent.hasExtra(getString(R.string.notification_intent_key)))) {
            launchedFromNotification = true;
        }

        if (launchedFromNotification) {
            if (twoPaneMode) {
                Bundle arguments = new Bundle();
                arguments.putBoolean(getString(R.string.notification_intent_key), true);
                launchTrackPlayer(arguments);
            } else {
                UtilClass.launchTrackplayerAcitivity(this, null, -1, true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String artistName, String artistId) {
        if (twoPaneMode) {
            Bundle arguments = new Bundle();

            arguments.putString(getString(R.string.artist_name_key), artistName);
            arguments.putString(getString(R.string.artist_id_key), artistId);
            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.top_tracks_container, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, TopTracksActivity.class);
            intent.putExtra(getString(R.string.artist_name_key), artistName);
            intent.putExtra(getString(R.string.artist_id_key), artistId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    public void onItemSelected(TopTracksActivityFragment.SongInfo songInfoArray[], int currentPosition) {
        // we shouldn't be here in single pane mode, no need to check if we are in two pane mode
        Bundle arguments = new Bundle();
        arguments.putParcelableArray(getString(R.string.songinfo_object_key), songInfoArray);
        arguments.putInt(getString(R.string.songinfo_current_pos_key), currentPosition);
        launchTrackPlayer(arguments);
    }

    private void launchTrackPlayer(Bundle arguments) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        TrackPlayerFragment fragment = new TrackPlayerFragment();
        if (arguments != null) {
            fragment.setArguments(arguments);
        }
        fragment.show(fragmentManager, getString(R.string.track_player_dialog_str));
    }
}
