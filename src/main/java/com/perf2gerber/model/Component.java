package com.perf2gerber.model;

import java.util.UUID;

public abstract class Component {
    private String id;
    private String name;
    private String value;
    private int startX;
    private int startY;
    private double rotation = 0;
    private boolean showName = false;
    private boolean showValue = true;

    public Component() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public int getStartX() { return startX; }
    public void setStartX(int startX) { this.startX = startX; }
    
    public int getStartY() { return startY; }
    public void setStartY(int startY) { this.startY = startY; }
    
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    
    public boolean isShowName() { return showName; }
    public void setShowName(boolean showName) { this.showName = showName; }
    
    public boolean isShowValue() { return showValue; }
    public void setShowValue(boolean showValue) { this.showValue = showValue; }
}
