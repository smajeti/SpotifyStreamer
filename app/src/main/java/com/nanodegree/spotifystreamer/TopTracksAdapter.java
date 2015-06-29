package com.nanodegree.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.List;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by smajeti on 6/28/15.
 */
public class TopTracksAdapter extends ArrayAdapter<Track> {

    private Context context;
    private List<Track> tracksList;

    public TopTracksAdapter(Context context, List<Track> tracksList) {
        super(context, R.layout.toptracks_item_layout, tracksList);
        this.context = context;
        this.tracksList = tracksList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = convertView;
        if (convertView == null) {
            rootView = inflater.inflate(R.layout.toptracks_item_layout, parent, false);
        }

        Track track = tracksList.get(position);
        TextView albumName = (TextView) rootView.findViewById(R.id.albumNameTxtId);
        albumName.setText(track.album.name);

        TextView trackName = (TextView) rootView.findViewById(R.id.trackNameTxtId);
        trackName.setText(track.name);

        ImageView albumImg = (ImageView) rootView.findViewById(R.id.albumImgId);
        if ((track.album.images != null) && (track.album.images.size() > 0)) {
            int imgIndx = getImageSizeIndex(track.album.images);
            Picasso.with(context).load(track.album.images.get(imgIndx).url).into(albumImg);
        }

        return rootView;
    }

    /**
     * Pick last but one image size, based on observation it is roughly 200x200 size,
     * this not scientific but a heuristic
     * @param images
     * @return
     */
    private int getImageSizeIndex(List<Image> images) {
        if (images.size() > 1) {
            return images.size() - 2;
        }
        return 0;
    }
}
