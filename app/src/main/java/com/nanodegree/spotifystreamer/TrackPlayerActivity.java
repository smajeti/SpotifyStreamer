package com.nanodegree.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by smajeti on 8/9/15.
 */
public class TrackPlayerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_player);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            Bundle arguments = null;
            if (intent != null) {
                int position = intent.getIntExtra(getString(R.string.songinfo_current_pos_key), -1);
                Parcelable songInfoArray[] = null;
                if (intent.hasExtra(getString(R.string.songinfo_object_key))) {
                    songInfoArray = intent.getParcelableArrayExtra(getString(R.string.songinfo_object_key));
                }
                boolean fromNotification = intent.getBooleanExtra(getString(R.string.notification_intent_key), false);
                arguments = new Bundle();
                arguments.putInt(getString(R.string.songinfo_current_pos_key), position);
                arguments.putParcelableArray(getString(R.string.songinfo_object_key), songInfoArray);
                arguments.putBoolean(getString(R.string.notification_intent_key), fromNotification);
            }
            TrackPlayerFragment fragment = new TrackPlayerFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.track_player_container, fragment)
                    .commit();
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // @todo for this version do not show settings on this activity we may enable it for next
        // getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
