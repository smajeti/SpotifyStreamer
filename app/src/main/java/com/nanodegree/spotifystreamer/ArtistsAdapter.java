package com.nanodegree.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by smajeti on 6/25/15.
 */
public class ArtistsAdapter extends ArrayAdapter<Artist> {
    private List<Artist> artistList;
    private Context context;

    public ArtistsAdapter(Context context, List<Artist> artistList) {
        super(context, R.layout.artist_item_layout, artistList);
        this.artistList = artistList;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = convertView;
        if (convertView == null) {
            rootView = inflater.inflate(R.layout.artist_item_layout, parent, false);
        }

        Artist artist = artistList.get(position);
        TextView artistNameTxtView = (TextView) rootView.findViewById(R.id.nameTxtViewId);
        artistNameTxtView.setText(artist.name);

        ImageView artistImg = (ImageView) rootView.findViewById(R.id.artistImgViewId);
        if ((artist.images != null) && (artist.images.size() > 0)) {
            int imgIndx = getImageSizeIndex(artist.images);
            Picasso.with(context).load(artist.images.get(imgIndx).url).into(artistImg);
        }

        return rootView;
    }



    private int getImageSizeIndex(List<Image> images) {
        if (images.size() > 1) {
            return images.size() - 2;
        }
        return 0;
    }

}
