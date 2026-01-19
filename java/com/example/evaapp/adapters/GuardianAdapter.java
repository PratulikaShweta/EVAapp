package com.example.evaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.evaapp.R;
import com.example.evaapp.models.Guardian;
import java.util.List;

public class GuardianAdapter extends RecyclerView.Adapter<GuardianAdapter.GuardianViewHolder> {

    private List<Guardian> guardianList;

    public GuardianAdapter(List<Guardian> guardianList) {
        this.guardianList = guardianList;
    }

    @NonNull
    @Override
    public GuardianViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.guardian_item, parent, false);
        return new GuardianViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuardianViewHolder holder, int position) {
        Guardian guardian = guardianList.get(position);
        holder.tvName.setText(guardian.getName());
        holder.tvPhone.setText(guardian.getPhone());
    }

    @Override
    public int getItemCount() {
        return guardianList.size();
    }

    static class GuardianViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone;

        GuardianViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGuardianName);
            tvPhone = itemView.findViewById(R.id.tvGuardianPhone);
        }
    }
}
