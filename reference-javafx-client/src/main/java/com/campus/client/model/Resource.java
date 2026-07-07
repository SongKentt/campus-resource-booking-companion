package com.campus.client.model;

public class Resource {
    private final String resourceId;
    private final String name;
    private final String building;
    private final int capacity;

    public Resource(String resourceId, String name, String building, int capacity){
        this.resourceId = resourceId;
        this.name = name;
        this.building = building;
        this.capacity = capacity;
    }

    public String getResourceId(){
        return resourceId;
    }

    public String getResourceName(){
        return name;
    }

    public String getBuilding(){
        return building;
    }

    public int getCapacity(){
        return capacity;
    }
}
