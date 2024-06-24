package unipi.exercise.trackmemore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import unipi.exercise.trackmemore.adapter.TrackRecordAdapter;
import unipi.exercise.trackmemore.model.SpeedCondtion;
import unipi.exercise.trackmemore.model.Type;

public class TrackingListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TrackRecordAdapter adapter;
    private List<SpeedCondtion> trackRecordList;

    private List<SpeedCondtion> filteredTrackRecordList;
    private CheckBox chkAcceleration, chkBreaks, chkPit, chkSpeedViolation;
    private Button btnApplyFilters;
    private Button btnShowInMaps;
    private DatabaseReference databaseReferenceAccelerations;
    private DatabaseReference databaseReferenceBreaks;
    private DatabaseReference databaseReferencePit;
    private DatabaseReference databaseReferenceSpeedViolations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tracking_list);
        initializeComponents();
        btnApplyFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilters();
            }
        });
        btnShowInMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInGoogleMaps();
            }
        });
        fetchTrackRecords();
    }

    private void initializeComponents() {
        recyclerView = findViewById(R.id.recyclerView);
        chkAcceleration = findViewById(R.id.chkAcceleration);
        chkBreaks = findViewById(R.id.chkBreaks);
        chkPit = findViewById(R.id.chkPit);
        chkSpeedViolation = findViewById(R.id.chkSpeedViolation);
        btnApplyFilters = findViewById(R.id.btnApplyFilters);
        btnShowInMaps = findViewById(R.id.btnShowInMaps);
        trackRecordList = new ArrayList<>();
        filteredTrackRecordList = new ArrayList<>();
        adapter = new TrackRecordAdapter(this, trackRecordList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        initializeDBReferences();
    }

    private void initializeDBReferences() {
        databaseReferenceAccelerations = FirebaseDatabase.getInstance().getReference("Accelerations");
        databaseReferenceBreaks = FirebaseDatabase.getInstance().getReference("Breaks");
        databaseReferencePit = FirebaseDatabase.getInstance().getReference("Pit");
        databaseReferenceSpeedViolations = FirebaseDatabase.getInstance().getReference("SpeedViolations");
    }

    private void fetchTrackRecords() {
        trackRecordList.clear();
        fetchFromReference(databaseReferenceAccelerations, Type.ACCELERATION);
        fetchFromReference(databaseReferenceBreaks, Type.BREAK);
        fetchFromReference(databaseReferencePit, Type.PIT);
        fetchFromReference(databaseReferenceSpeedViolations, Type.SPEED_VIOLATION);
    }

    private void fetchFromReference(DatabaseReference reference, final Type type) {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        SpeedCondtion trackRecord = childSnapshot.getValue(SpeedCondtion.class);
                        if (trackRecord != null) {
                            trackRecord.setType(type);
                            trackRecordList.add(trackRecord);
                        }
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error fetching data", databaseError.toException());
            }
        });
    }

    private void showInGoogleMaps() {
        if (trackRecordList.isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("track_record_list", new ArrayList<>(filteredTrackRecordList));
        startActivity(intent);
    }

    private void applyFilters() {
        boolean acceleration = chkAcceleration.isChecked();
        boolean breaks = chkBreaks.isChecked();
        boolean pit = chkPit.isChecked();
        boolean speedViolation = chkSpeedViolation.isChecked();

        filteredTrackRecordList.clear();
        for (SpeedCondtion record : trackRecordList) {
            if ((acceleration && record.getType() == Type.ACCELERATION) ||
                    (breaks && record.getType() == Type.BREAK) ||
                    (pit && record.getType() == Type.PIT) ||
                    (speedViolation && record.getType() == Type.SPEED_VIOLATION)) {
                filteredTrackRecordList.add(record);
            }
        }
        adapter.updateList(filteredTrackRecordList);
        if (filteredTrackRecordList.isEmpty()) {
            btnShowInMaps.setVisibility(View.GONE);
        } else {
            btnShowInMaps.setVisibility(View.VISIBLE);
        }
    }
}