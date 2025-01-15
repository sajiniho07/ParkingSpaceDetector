package org.psd.parkingspacedetector.enums;

public enum EnumMediaType {
    VIDEO(1, "video"),
    STREAM(2, "stream"),
    IMAGE(3, "image"),
    MODEL(4, "model");

    private long id;
    private String name;

    EnumMediaType(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
