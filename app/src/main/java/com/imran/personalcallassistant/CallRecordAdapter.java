package com.imran.personalcallassistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CallRecordAdapter extends RecyclerView.Adapter<CallRecordAdapter.ViewHolder> {

    private Context context;
    private List<CallRecord> list;
    private OnCallBackClickListener callBackListener;

    public interface OnCallBackClickListener {
        void onCallBack(String phoneNumber);
    }

    public CallRecordAdapter(Context context, List<CallRecord> list, OnCallBackClickListener callBackListener) {
        this.context = context;
        this.list = list;
        this.callBackListener = callBackListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_call_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallRecord record = list.get(position);
        holder.tvCallerName.setText(record.getCallerName());
        holder.tvPhoneNumber.setText(record.getPhoneNumber());
        holder.tvCallReason.setText(record.getReason());
        holder.tvLanguageBadge.setText(record.getLanguage());

        holder.btnCallBack.setOnClickListener(v -> {
            if (callBackListener != null) {
                callBackListener.onCallBack(record.getPhoneNumber());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCallerName, tvPhoneNumber, tvCallReason, tvLanguageBadge;
        MaterialButton btnCallBack;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCallerName = itemView.findViewById(R.id.tvCallerName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvCallReason = itemView.findViewById(R.id.tvCallReason);
            tvLanguageBadge = itemView.findViewById(R.id.tvLanguageBadge);
            btnCallBack = itemView.findViewById(R.id.btnCallBack);
        }
    }
}
