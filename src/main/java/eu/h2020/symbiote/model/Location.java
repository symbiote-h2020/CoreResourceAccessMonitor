package eu.h2020.symbiote.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Created by jawora on 22.09.16.
 */
public class Location {

    @Id
    private String id;
    private String name;
    private String description;
    private GeoJsonPoint point;
    private Double altitude;

    public Location() {
    }

    public Location(String name, String description,GeoJsonPoint point, Double altitude) {
        this.name = name;
        this.description = description;
        this.altitude = altitude;
        this.point = point;
    }

    public Location(String id, String name, String description,  GeoJsonPoint point, Double altitude) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.altitude = altitude;
        this.point = point;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public GeoJsonPoint getPoint() {
        return point;
    }

    public void setPoint(GeoJsonPoint point) {
        this.point = point;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }
}
