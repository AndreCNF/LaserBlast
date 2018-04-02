package com.andrecnf.android.laserblast;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by CodingWithMitch
 * https://www.youtube.com/watch?v=Vyqz_-sJGFk
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> mShotPlayers = new ArrayList<>();
    private Context mContext;

    public RecyclerViewAdapter(Context context, ArrayList<String> shotPlayers) {
        mShotPlayers = shotPlayers;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        holder.shotPlayer.setText(mShotPlayers.get(position));

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Log.d(TAG, "onClick: clicked on: " + mShotPlayers.get(position));

                // Toast.makeText(mContext, mShotPlayers.get(position), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mShotPlayers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView shotPlayer;
        RelativeLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            shotPlayer = itemView.findViewById(R.id.shotPlayer);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}