package unipi.exercise.trackmemore.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import unipi.exercise.trackmemore.HomePage;
import unipi.exercise.trackmemore.model.SpeedCondtion;
import unipi.exercise.trackmemore.model.Type;


public class TrackingService extends Service implements SensorEventListener, LocationListener {
    private static final String CHANNEL_ID = "TrackingServiceChannel";
    private static final float SPEED_LIMIT = 100.0f; // km/h
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private final List<Float> speedRecordList = new ArrayList<>();
    private DatabaseReference databaseReference;
    private String userId;
    private Location lastKnownLocation;

    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        handlerThread = new HandlerThread("TrackingServiceHandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        createNotificationChannel();
    }


    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, HomePage.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Service from TrackMeMore")
                .setContentText("Tracking your location and activity.")
                .setContentIntent(pendingIntent)
                .build();
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");

        Notification notification = getNotification();
        startForeground(1, notification);
        handler.post(this::startTracking);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.post(this::stopTracking);
        handlerThread.quitSafely();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTracking() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    private void stopTracking() {
        speedRecordList.clear();
        locationManager.removeUpdates(this);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastKnownLocation = location;
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

    private void savePitToFirebase(double longitude, double latitude, float change) {
        String timestamp = getCurrentTimestamp();
        DatabaseReference newUpsAndDownsRef = databaseReference.child("Pit").child(userId).push();
        newUpsAndDownsRef.setValue(new SpeedCondtion(userId, longitude, latitude, timestamp, change, Type.PIT));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometerData(event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void handleAccelerometerData(float x, float y, float z) {
        float gForceX = Math.abs(x / SensorManager.GRAVITY_EARTH);
        float gForceY = Math.abs(y / SensorManager.GRAVITY_EARTH);
        float gForceZ = Math.abs(z / SensorManager.GRAVITY_EARTH);
        float gForce = (float) Math.sqrt(gForceX * gForceX + gForceY * gForceY + gForceZ * gForceZ);

        float threshold = 1.3f;
        if (gForce > threshold) {
            if (lastKnownLocation != null) {
                savePitToFirebase(lastKnownLocation.getLongitude(), lastKnownLocation.getLatitude(), gForce);
            } else {
                Log.d("SensorData", "No location data available for pothole");
            }
        }
    }

}
