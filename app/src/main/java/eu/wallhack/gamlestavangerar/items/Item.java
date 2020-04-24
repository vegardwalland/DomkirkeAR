package eu.wallhack.gamlestavangerar.items;

import androidx.annotation.NonNull;

public class Item {

    private String _id;
    private String name;
    private String description;
    private Double lat;
    private Double lon;
    private String pictureURI;

    public Item(String id, String name, String description, Double latitude, Double longitude, String pictureURI) {
        this._id = id;
        this.name = name;
        this.description = description;
        this.lat = latitude;
        this.lon = longitude;
        this.pictureURI = pictureURI;
    }

    @NonNull
    @Override
    public String toString() {
        return "Item{" +
                "id='" + _id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", latitude=" + lat +
                ", longitude=" + lon +
                ", imagePath='" + pictureURI + '\'' +
                '}';
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return lat;
    }

    public void setLatitude(Double latitude) {
        this.lat = latitude;
    }

    public Double getLongitude() {
        return lon;
    }

    public void setLongitude(Double longitude) {
        this.lon = longitude;
    }

    public String getPictureURI() {
        return pictureURI;
    }

    public void setPictureURI(String pictureURI) {
        this.pictureURI = pictureURI;
    }
}
