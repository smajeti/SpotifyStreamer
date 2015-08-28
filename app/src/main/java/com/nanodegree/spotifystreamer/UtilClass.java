package com.nanodegree.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

/**
 * Created by smajeti on 7/2/15.
 */
public class UtilClass {

    public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean isEmptyOrNull(String str) {
        return ((str == null) || (str.isEmpty()));
    }

    public static void launchTrackplayerAcitivity(Context context, TopTracksActivityFragment.SongInfo songInfoArray[], 
                                                  int currentPosition, boolean fromNotification) {
        Intent intent = new Intent(context, TrackPlayerActivity.class);
        intent.putExtra(context.getString(R.string.songinfo_current_pos_key), currentPosition);
        intent.putExtra(context.getString(R.string.songinfo_object_key), songInfoArray);
        intent.putExtra(context.getString(R.string.notification_intent_key), fromNotification);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

}
