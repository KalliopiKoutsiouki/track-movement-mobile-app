package unipi.exercise.trackmemore.model;

import java.io.Serializable;

public class SpeedCondtion implements Serializable {
    private String userId;
    private double longitude;
    private double latitude;
    private String timestamp;
    private float speedChange;
    private Enum type;
    public SpeedCondtion() {}

    public SpeedCondtion(String userId, double longitude, double latitude, String timestamp, float speedChange, Enum type) {
        this.userId = userId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
        this.speedChange = speedChange;
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public float getSpeedChange() {
        return speedChange;
    }

    public void setType(Enum type) {
        this.type = type;
    }
}
