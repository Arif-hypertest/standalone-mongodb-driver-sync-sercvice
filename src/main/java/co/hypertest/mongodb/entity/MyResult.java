package co.hypertest.mongodb.entity;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class MyResult {
    private String id; // Renamed from _id for Java convention
    private double value;

    @BsonProperty("_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public MyResult() {
        // Required no-argument constructor for POJO codec
    }
}
