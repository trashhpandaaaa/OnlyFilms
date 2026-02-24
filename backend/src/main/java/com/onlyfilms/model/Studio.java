package com.onlyfilms.model;

public class Studio {
    private int studioId;
    private String studioName;

    public Studio() {}

    public Studio(int studioId, String studioName) {
        this.studioId = studioId;
        this.studioName = studioName;
    }

    public int getStudioId() { return studioId; }
    public void setStudioId(int studioId) { this.studioId = studioId; }

    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }
}
