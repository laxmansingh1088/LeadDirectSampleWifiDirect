package com.example.leaddirectsamplewifidirect.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.leaddirectsamplewifidirect.OnRecyclerViewItemClick;
import com.example.leaddirectsamplewifidirect.R;
import com.example.leadp2p.p2p.PeerDevice;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    List<PeerDevice> deviceNamesArray;
    OnRecyclerViewItemClick onRecyclerViewItemClick;

    // RecyclerView recyclerView;
    public DeviceListAdapter(List<PeerDevice> deviceNamesArray, OnRecyclerViewItemClick onRecyclerViewItemClick) {
        this.deviceNamesArray = deviceNamesArray;
        this.onRecyclerViewItemClick = onRecyclerViewItemClick;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.deviceName.setText(deviceNamesArray.get(position).getDeviceName() + " / " + deviceNamesArray.get(position).getDeviceId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecyclerViewItemClick.onItemClick(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return deviceNamesArray.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;

        public ViewHolder(View itemView) {
            super(itemView);
            this.deviceName = itemView.findViewById(R.id.deviceName);
        }
    }
}
