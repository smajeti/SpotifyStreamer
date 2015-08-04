package com.nanodegree.spotifystreamer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private static String TAG = MainActivityFragment.class.getSimpleName();

    private SearchForArtistTask searchTask = null;
    private SearchView searchEditText;
    private ListView artistListView;
    private String artistSearchStr;
    private int listViewPos = -1;
    private ArtistsPager fetchedArtistData;

    public interface Callback {
        public void onItemSelected(String artistName, String artistId);
    }

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        artistListView = (ListView) rootView.findViewById(R.id.artistsListViewId);
        artistListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Artist artist = (Artist) adapterView.getItemAtPosition(position);
                ((Callback)getActivity()).onItemSelected(artist.name, artist.id);
            }
        });

        searchEditText = (SearchView) rootView.findViewById(R.id.searchEditTxtId);
        searchEditText.setIconifiedByDefault(false);
        searchEditText.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                listViewPos = -1;
                artistSearchStr = searchEditText.getQuery().toString();
                fetchArtistData(artistSearchStr);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        saveArtistSearchParameters();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        artistSearchStr = sharedPref.getString(getActivity().getString(R.string.artist_name_search_str_key), null);
        listViewPos = sharedPref.getInt(getActivity().getString(R.string.artist_list_view_pos_key), -1);
        if (fetchedArtistData == null) {
            fetchArtistData(artistSearchStr);
        } else {
            processArtistSearchData(fetchedArtistData);
        }
    }


    /**
     * Saves current user artist search selection and uses them during
     * device rotation or during new activity creation
     */
    private void saveArtistSearchParameters() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if ((artistSearchStr != null) && (!artistSearchStr.isEmpty())) {
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putString(getActivity().getString(R.string.artist_name_search_str_key), artistSearchStr);
            prefEditor.putInt(getActivity().getString(R.string.artist_list_view_pos_key), artistListView.getFirstVisiblePosition());
            prefEditor.commit();
        }
    }

    private void fetchArtistData(String artistSearchStr) {
        if (UtilClass.isNetworkAvailable(getActivity())) {
            if ((artistSearchStr != null) && (!artistSearchStr.isEmpty())) {
                searchTask = new SearchForArtistTask(getActivity());
                searchTask.execute(artistSearchStr);
            }
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
        }
    }

    private void processArtistSearchData(ArtistsPager artistsPager) {
        if ((artistsPager == null) || (artistsPager.artists.items.size() == 0)) {
            Toast.makeText(getActivity(), R.string.empty_artists_search_msg_txt, Toast.LENGTH_SHORT).show();
            return;
        }

        ArtistsAdapter adapter = new ArtistsAdapter(getActivity(), artistsPager.artists.items);
        artistListView.setAdapter(adapter);
        fetchedArtistData = artistsPager;
        if ((artistSearchStr != null) && (!artistSearchStr.isEmpty())) {
            searchEditText.setQuery(artistSearchStr, false);
        }
        if ((listViewPos != -1) && (listViewPos < artistsPager.artists.items.size())) {
            artistListView.setSelection(listViewPos);
        }
    }

    public static void hideSoftKeyboard(Activity activity){
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0); // hide
    }

    /**
     * Async task to fetch data from Spotify Cloud
     */
    class SearchForArtistTask extends AsyncTask<String, Void, ArtistsPager> {

        private Context context;
        private ProgressDialog progressDialog;

        public SearchForArtistTask(Context context) {
            this.context = context;
            progressDialog = new ProgressDialog(context);
            progressDialog.setIndeterminate(true);
        }

        /**
         * Helper function to fetch data from cloud
         *
         * @param artistName
         * @return
         */
        private ArtistsPager getSpotifyData(String artistName) {
            try {
                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                if (spotifyService != null) {
                    ArtistsPager artistsPager = spotifyService.searchArtists(artistName);
                    if (isCancelled()) {
                        artistsPager = null;
                    }
                    return artistsPager;
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
        protected ArtistsPager doInBackground(String... strings) {
            ArtistsPager artistsPager = getSpotifyData(strings[0]);
            return artistsPager;
        }

        @Override
        protected void onPostExecute(ArtistsPager artistsPager) {
            if ((artistsPager != null) && (artistsPager.artists.items.size() > 0)) {
                Log.d(TAG, "Got data for " + artistsPager.artists.items.get(0).name);
            } else {
                Log.d(TAG, "Got NULL data");
            }

            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            processArtistSearchData(artistsPager);
        }
    }
}
