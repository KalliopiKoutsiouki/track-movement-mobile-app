package unipi.exercise.trackmemore.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import unipi.exercise.trackmemore.R;
import unipi.exercise.trackmemore.model.SpeedCondtion;

public class TrackRecordAdapter extends RecyclerView.Adapter<TrackRecordAdapter.TrackRecordViewHolder> {

    private final Context context;
    private List<SpeedCondtion> trackRecordList;

    public TrackRecordAdapter(Context context, List<SpeedCondtion> trackRecordList) {
        this.context = context;
        this.trackRecordList = trackRecordList;
    }

    @NonNull
    @Override
    public TrackRecordAdapter.TrackRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.track_record_item, parent, false);
        return new TrackRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackRecordAdapter.TrackRecordViewHolder holder, int position) {
        SpeedCondtion record = trackRecordList.get(position);
        holder.typeTextView.setText(record.getType().toString());
        holder.timestampTextView.setText(record.getTimestamp());
        holder.longitudeTextView.setText(String.valueOf(record.getLongitude()));
        holder.latitudeTextView.setText(String.valueOf(record.getLatitude()));
    }

    @Override
    public int getItemCount() {
        return trackRecordList.size();
    }

    public void updateList(List<SpeedCondtion> newList) {
        trackRecordList = newList;
        notifyDataSetChanged();
    }

    class TrackRecordViewHolder extends RecyclerView.ViewHolder {
        TextView typeTextView;
        TextView timestampTextView;
        TextView longitudeTextView;
        TextView latitudeTextView;

        public TrackRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            longitudeTextView = itemView.findViewById(R.id.longitudeTextView);
            latitudeTextView = itemView.findViewById(R.id.latitudeTextView);
        }
    }
}
