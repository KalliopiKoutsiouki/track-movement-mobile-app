package unipi.exercise.trackmemore;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import unipi.exercise.trackmemore.service.TrackingService;

public class HomePage extends AppCompatActivity {
    private static final int LOCATION_CODE = 1717;
    TextView loadingText;
    ProgressBar progressBar;
    Button btnLocPerm;
    Button btnShowRes;
    boolean tracking = false;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.instantiateComponents();
        this.setStartButtonBehavior();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_CODE);
        }
        userId = getIntent().getStringExtra("userId");
    }

    private void setStartButtonBehavior() {
        btnLocPerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tracking) {
                    stopTrackingService();
                    btnLocPerm.setText("Start");
                    progressBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                    btnShowRes.setVisibility(View.VISIBLE);
                } else {
                    if (ContextCompat.checkSelfPermission(HomePage.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startTrackingService();
                        btnLocPerm.setText("Stop");
                        progressBar.setVisibility(View.VISIBLE);
                        loadingText.setVisibility(View.VISIBLE);
                        btnShowRes.setVisibility(View.GONE);
                    } else {
                        ActivityCompat.requestPermissions(HomePage.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_CODE);
                    }
                }
                tracking = !tracking;
            }
        });
    }

    private void instantiateComponents() {
        loadingText = findViewById(R.id.loadingText);
        progressBar = findViewById(R.id.progressBar);
        btnLocPerm = findViewById(R.id.btnLocPerm);
        btnShowRes = findViewById(R.id.btnShowRes);
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra("userId", userId);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        stopService(serviceIntent);
    }

    // this is the callback of ActivityCompat.requestPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
            }
        }
    }

    public void openResultList(View view) {
        startActivity(new Intent(this, TrackingListActivity.class));
    }

}