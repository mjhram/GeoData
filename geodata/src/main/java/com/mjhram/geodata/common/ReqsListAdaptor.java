package com.mjhram.geodata.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mjhram.geodata.R;

import java.util.ArrayList;

/**
 * Created by mohammad.haider on 8/16/2016.
 */
public class ReqsListAdaptor extends ArrayAdapter<TRequestObj> {
    public ReqsListAdaptor(Context context, ArrayList<TRequestObj> requests) {
        super(context, 0, requests);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        TRequestObj user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_requests, parent, false);
        }
        // Lookup view for data population
        NetworkImageView networkImageViewRequester = (NetworkImageView) convertView.findViewById(R.id.requesterIm);
        TextView tvRequesterName = (TextView) convertView.findViewById(R.id.tvRequesterName);
        TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);
        TextView fromDesc = (TextView) convertView.findViewById(R.id.tvFromDesc);
        TextView toDesc = (TextView) convertView.findViewById(R.id.tvToDesc);

        TextView tvNoPassengers = (TextView) convertView.findViewById(R.id.tvNoPassengers);
        TextView tvFee = (TextView) convertView.findViewById(R.id.tvFee);
        TextView tvNotes = (TextView) convertView.findViewById(R.id.tvRequsterNotes);
        // Populate the data into the template view using the data object
        tvRequesterName.setText(convertView.getResources().getString(R.string.infoName) + user.passangerName);
        if(user.passengerPhotoUrl.isEmpty()) {
            networkImageViewRequester.setVisibility(View.GONE);
        } else {
            networkImageViewRequester.setVisibility(View.VISIBLE);
            //final String IMAGE_URL = "http://developer.android.com/images/training/system-ui.png";
            ImageLoader mImageLoader = AppSettings.getInstance().getImageLoader();
            networkImageViewRequester.setImageUrl(user.passengerPhotoUrl, mImageLoader);
        }
        if(user.time.isEmpty()) {
            tvTime.setVisibility(View.GONE);
        } else {
            tvTime.setVisibility(View.VISIBLE);
            tvTime.setText(convertView.getResources().getString(R.string.infoTime)+ user.time);
        }
        if(user.fromDesc.isEmpty()) {
            fromDesc.setVisibility(View.GONE);
        } else {
            fromDesc.setVisibility(View.VISIBLE);
            fromDesc.setText(convertView.getResources().getString(R.string.infoFromDesc)+ user.fromDesc);
        }
        if(user.toDesc.isEmpty()) {
            toDesc.setVisibility(View.GONE);
        } else {
            toDesc.setVisibility(View.VISIBLE);
            toDesc.setText(convertView.getResources().getString(R.string.infoToDesc)+ user.toDesc);
        }
        if(user.noOfPassangers.isEmpty()) {
            tvNoPassengers.setVisibility(View.GONE);
        } else {
            tvNoPassengers.setVisibility(View.VISIBLE);
            tvNoPassengers.setText(convertView.getResources().getString(R.string.infoNoPassengers)+ user.noOfPassangers);
        }
        if(user.suggestedFee.isEmpty()) {
            tvFee.setVisibility(View.GONE);
        } else {
            tvFee.setVisibility(View.VISIBLE);
            tvFee.setText(convertView.getResources().getString(R.string.infoFee)+ user.suggestedFee);
        }
        if(user.additionalNotes.isEmpty()) {
            tvNotes.setVisibility(View.GONE);
        } else {
            tvNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(convertView.getResources().getString(R.string.infoNotes)+ user.additionalNotes);
        }
        // Return the completed view to render on screen
        return convertView;
    }
}

