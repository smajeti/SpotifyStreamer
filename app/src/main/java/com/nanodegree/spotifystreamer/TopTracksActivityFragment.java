package com.nanodegree.spotifystreamer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksActivityFragment extends Fragment {

    private static String TAG = TopTracksActivity.class.getSimpleName();
    private FetchTopTracksTask fetchTask;
    private String artistName;
    private String artistId;
    private ListView topTracksLstView;
    private Tracks fetchedTracks;

    public TopTracksActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        topTracksLstView = (ListView) rootView.findViewById(R.id.topTracksListViewId);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            artistName = intent.getExtras().getString(getString(R.string.artist_name_key));
            artistId = intent.getExtras().getString(getString(R.string.artist_id_key));
        }

        if (fetchedTracks == null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String countryCode = sharedPref.getString(getActivity().getString(R.string.country_code_key),
                                                getActivity().getString(R.string.default_artist_country_code));
            fetchTopTracks(artistId, countryCode);
        } else {
            processTopTracksData(fetchedTracks);
        }

        return rootView;
    }

    private void fetchTopTracks(String artistId, String countryCode) {
        if (UtilClass.isNetworkAvailable(getActivity())) {
            if ((artistId != null && !artistId.isEmpty()) && (countryCode != null && !countryCode.isEmpty())) {
                fetchTask = new FetchTopTracksTask(getActivity());
                fetchTask.execute(artistId, countryCode);
            }
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates top tracks adapter and sets the adapter to the listview
     * @param topTracks
     */
    private void processTopTracksData(Tracks topTracks) {
        if ((topTracks == null) || (topTracks.tracks.size() == 0)) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.no_top_tracks_found_err_msg) + artistName, Toast.LENGTH_SHORT).show();
            return;
        }

        TopTracksAdapter adapter = new TopTracksAdapter(getActivity(), topTracks.tracks);
        topTracksLstView.setAdapter(adapter);
        fetchedTracks = topTracks;
    }


    /**
     * Async task to fetch data from Spotify Cloud
     */
    class FetchTopTracksTask extends AsyncTask<String, Void, Tracks> {

        private Context context;
        private ProgressDialog progressDialog;

        public FetchTopTracksTask(Context context) {
            this.context = context;
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
        }

        /**
         * Helper function to fetch data from cloud
         *
         * @param artistId
         * @return
         */
        private Tracks getSpotifyData(String artistId, String countryCode) {
            try {
                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                if (spotifyService != null) {
                    Map<String, Object> optionsMap = new HashMap<String, Object>();
                    optionsMap.put("country", countryCode);
                    Tracks topTracks = spotifyService.getArtistTopTrack(artistId, optionsMap);
                    if (isCancelled()) {
                        topTracks = null;
                    }
                    return topTracks;
                }
            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage(context.getString(R.string.progress_dlg_msg_txt));
            progressDialog.show();
        }

        @Override
        protected Tracks doInBackground(String... strings) {
            Tracks topTracks = getSpotifyData(strings[0], strings[1]);
            return topTracks;
        }

        @Override
        protected void onPostExecute(Tracks topTracks) {
            if ((topTracks != null) && (topTracks.tracks.size() > 0)) {
                Log.d(TAG, "Got data, top album " + topTracks.tracks.get(0).album.name);
            } else {
                Log.d(TAG, "Got NULL data");
            }

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            processTopTracksData(topTracks);
        }
    }

}
