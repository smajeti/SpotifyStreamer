package com.nanodegree.spotifystreamer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
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

    public static class SongInfo implements Parcelable {

        public String artistName;
        public String albumName;
        public String artWorkUrl;
        public String previewUrl;
        public String trackName;
        public long trackDurationMs;

        public SongInfo() {}

        public static final Parcelable.Creator<SongInfo> CREATOR
                = new Parcelable.Creator<SongInfo>() {
            public SongInfo createFromParcel(Parcel in) {
                return new SongInfo(in);
            }

            public SongInfo[] newArray(int size) {
                return new SongInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(artistName);
            parcel.writeString(albumName);
            parcel.writeString(artWorkUrl);
            parcel.writeString(previewUrl);
            parcel.writeString(trackName);
            parcel.writeLong(trackDurationMs);
        }

        private SongInfo(Parcel in) {
            artistName = in.readString();
            albumName = in.readString();
            artWorkUrl = in.readString();
            previewUrl = in.readString();
            trackName = in.readString();
            trackDurationMs = in.readLong();
        }

    }

    public interface Callback {
        public void onItemSelected(SongInfo songInfo[], int currentPosition);
    }

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

        topTracksLstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int numTracks = adapterView.getAdapter().getCount();
                SongInfo songInfoArray[] = SongInfo.CREATOR.newArray(numTracks);
                for (int indx = 0; indx < numTracks; ++indx) {
                    Track track = (Track) adapterView.getItemAtPosition(indx);
                    songInfoArray[indx] = createSongInfo(track);
                }
                ((Callback) getActivity()).onItemSelected(songInfoArray, position);
            }
        });

        Bundle arguments = getArguments();
        if ((arguments != null)) {
            artistName = arguments.getString(getString(R.string.artist_name_key), "");
            artistId = arguments.getString(getString(R.string.artist_id_key), "");
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

    private SongInfo createSongInfo(Track track) {
        SongInfo songInfo = new SongInfo();
        songInfo.artistName = artistName;
        songInfo.trackName = track.name;
        songInfo.albumName = track.album.name;
        songInfo.artWorkUrl = ((track.album.images != null) &&
                (track.album.images.size() > 0)) ?
                track.album.images.get(0).url : null;
        songInfo.previewUrl = track.preview_url;
        songInfo.trackDurationMs = track.duration_ms;
        return songInfo;
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
