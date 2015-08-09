package com.nanodegree.spotifystreamer;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class TopTracksActivity extends AppCompatActivity implements TopTracksActivityFragment.Callback {

    private String artistName;
    private String artistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        setTitle(R.string.title_activity_top_tracks);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                artistName = intent.getExtras().getString(getString(R.string.artist_name_key), "");
                artistId = intent.getExtras().getString(getString(R.string.artist_id_key), "");
            }

            Bundle arguments = new Bundle();

            arguments.putString(getString(R.string.artist_name_key), artistName);
            arguments.putString(getString(R.string.artist_id_key), artistId);
            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.top_tracks_container, fragment)
                    .commit();


            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setSubtitle(artistName);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(TopTracksActivityFragment.SongInfo songInfoArray[], int currentPosition) {

    }
}
