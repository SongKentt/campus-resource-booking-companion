package com.campus.client.model;

import java.time.LocalTime;

public class Resource {

    private final String resourceId;
    private final String name;
    private final String building;
    private final int capacity;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final String typeName;

    // Constructor of Resource which initialize the attribute values of the resource instance
    public Resource(String resourceId, String name, String building, int capacity ,LocalTime openTime, LocalTime closeTime, String typeName){
        this.resourceId = resourceId;
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.typeName = typeName;

    }

    //Getters Method for the resource instance
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

    public LocalTime getOpenTime(){
        return openTime;
    }
    public LocalTime getCloseTime(){
        return closeTime;
    }

    // Getter method that return the resource type in a human‑readable type name to display to the user */
    public String getTypeName() {
        switch (typeName) {
            case "discussion_room":
                return "Discussion Room";
            case "group_study_room":
                return "Group Study Room";
            case "computer_lab":
                return "Lab Workstation";
            case "study_pod":
                return "Study Pod";
            case "Basketball court":
                return "Basketball Court";
            default:
                return typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        }
    }

}
