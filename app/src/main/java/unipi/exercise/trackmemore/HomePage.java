package unipi.exercise.trackmemore;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import unipi.exercise.trackmemore.model.SpeedCondtion;
import unipi.exercise.trackmemore.model.Type;

public class HomePage extends AppCompatActivity {

    TextView loadingText;
    ProgressBar progressBar;
    Button btnLocPerm;
    Button btnShowRes;
    boolean tracking = false;
    private static final int LOCATION_CODE = 1717;
    List<Float> speedRecordList = new ArrayList<>();
    private LocationManager locationManager;
    private FirebaseUser user;
    private String userId;
    private String userEmail;
    private DatabaseReference databaseReference;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorEventListener;
    private Location lastKnownLocation;

    private static final float SPEED_LIMIT = 100.0f; // km/h

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
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userId = getIntent().getStringExtra("userId");
        userEmail = getIntent().getStringExtra("userEmail");
        user = (FirebaseUser) getIntent().getSerializableExtra("user");
        initializeSensors();
    }

    private void setStartButtonBehavior() {
        btnLocPerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tracking) {
                    stopTracking();
                    btnLocPerm.setText("Start");
                    progressBar.setVisibility(View.GONE);
                    loadingText.setVisibility(View.GONE);
                } else {
                    startTracking();
                    btnLocPerm.setText("Stop");
                    progressBar.setVisibility(View.VISIBLE);
                    loadingText.setVisibility(View.VISIBLE);
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

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        handleAccelerometerData(event.values[0], event.values[1], event.values[2]);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    // Do nothing for now
                }
            };
        }
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_CODE);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000, 0, locationListener);
        if (sensorManager != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopTracking() {
        if (!speedRecordList.isEmpty()) {
            Log.d("LIST", speedRecordList.get(0).toString());
        }
        speedRecordList.clear();
        btnShowRes.setVisibility(View.VISIBLE);
        locationManager.removeUpdates(locationListener);
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            lastKnownLocation = location;
            Log.d("SPEED ", Float.toString(location.getSpeed()));
            float currentSpeed = location.getSpeed();
            speedRecordList.add(currentSpeed);
            if (currentSpeed > SPEED_LIMIT) {
                saveSpeedViolationToFirebase(location.getLongitude(), location.getLatitude(), currentSpeed);
            }
            if (speedRecordList.size() >= 2) {
                float previousSpeed = speedRecordList.get(speedRecordList.size() - 2);
                float speedChange = currentSpeed - previousSpeed;
                if (speedChange <= -6) {
                    saveBreakToFirebase(location.getLongitude(), location.getLatitude(), speedChange);
                } else if (speedChange > 6) {
                    saveAccelerationToFirebase(location.getLongitude(), location.getLatitude(), speedChange);
                }
            }
        }
    };

    private void saveBreakToFirebase(double longitude, double latitude, float speedChange) {
        String timestamp = getCurrentTimestamp();
        DatabaseReference newBreakRef = databaseReference.child("Breaks").child(userId).push();
        newBreakRef.setValue(new SpeedCondtion(userId, longitude, latitude, timestamp, speedChange, Type.BREAK));
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveAccelerationToFirebase(double longitude, double latitude, float acceleration) {
        String timestamp = getCurrentTimestamp();
        DatabaseReference newAccelerationRef = databaseReference.child("Accelerations").child(userId).push();
        newAccelerationRef.setValue(new SpeedCondtion(userId, longitude, latitude, timestamp, acceleration, Type.ACCELERATION));
    }

    private void saveSpeedViolationToFirebase(double longitude, double latitude, float speed) {
        String timestamp = getCurrentTimestamp();
        DatabaseReference newSpeedViolationRef = databaseReference.child("SpeedViolations").child(userId).push();
        newSpeedViolationRef.setValue(new SpeedCondtion(userId, longitude, latitude, timestamp, speed, Type.SPEED_VIOLATION));
    }

    private void handleAccelerometerData(float x, float y, float z) {
        // Calculate the gForce (acceleration including gravity)
        float gForceX = Math.abs(x / SensorManager.GRAVITY_EARTH);
        float gForceY = Math.abs(y / SensorManager.GRAVITY_EARTH);
        float gForceZ = Math.abs(z / SensorManager.GRAVITY_EARTH);
        float gForce = (float) Math.sqrt(gForceX * gForceX + gForceY * gForceY + gForceZ * gForceZ);

        float threshold = 1.3f; // this threshold is set based on the virtual sensors of the emulator
        if (gForce > threshold) {
            if (lastKnownLocation != null) {
                savePitToFirebase(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude(), gForce);
            } else {
                Log.d("SensorData", "No location data available for pothole");
            }
        }
    }

    private void savePitToFirebase(double longitude, double latitude, float change) {
        String timestamp = getCurrentTimestamp();
        DatabaseReference newUpsAndDownsRef = databaseReference.child("Pit").child(userId).push();
        newUpsAndDownsRef.setValue(new SpeedCondtion(userId, longitude, latitude, timestamp, change, Type.PIT));
    }

    // this is the callback of ActivityCompat.requestPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_CODE) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        5000, 0, locationListener);
            }
        }
    }




}