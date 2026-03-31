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
    private String type;
    private String color = "Red";

    // Phase 6 UI fields (not serialized)
    private transient boolean isHovered = false;

    public Component() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
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

    public boolean isHovered() { return isHovered; }
    public void setHovered(boolean hovered) { this.isHovered = hovered; }

    /**
     * Determine if a grid coordinate falls within the boundary.
     * Note: worldX/Y are purely continuous grid coordinates!
     */
    public abstract boolean contains(double worldX, double worldY);

    /**
     * Creates a deep duplicate of this component (must be implemented by subtypes).
     * The ID generated for the new component is freshly randomized on instantiation.
     */
    public abstract Component cloneComponent();
}
