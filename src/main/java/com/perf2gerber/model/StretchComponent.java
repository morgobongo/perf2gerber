package com.perf2gerber.model;

public class StretchComponent extends Component {
    private int endX;
    private int endY;

    public StretchComponent() {}

    public int getEndX() { return endX; }
    public void setEndX(int endX) { this.endX = endX; }

    public int getEndY() { return endY; }
    public void setEndY(int endY) { this.endY = endY; }

    @Override
    public Component cloneComponent() {
        StretchComponent clone = new StretchComponent();
        clone.setName(this.getName());
        clone.setValue(this.getValue());
        clone.setStartX(this.getStartX());
        clone.setStartY(this.getStartY());
        clone.setRotation(this.getRotation());
        clone.setShowName(this.isShowName());
        clone.setShowValue(this.isShowValue());
        clone.setType(this.getType());
        clone.setEndX(this.endX);
        clone.setEndY(this.endY);
        return clone;
    }

    @Override
    public boolean contains(double worldX, double worldY) {
        double midX = (getStartX() + endX) / 2.0;
        double midY = (getStartY() + endY) / 2.0;
        
        double ang = Math.atan2(endY - getStartY(), endX - getStartX());
        double dx = worldX - midX;
        double dy = worldY - midY;
        
        // Un-rotate the math
        double rx = dx * Math.cos(-ang) - dy * Math.sin(-ang);
        double ry = dx * Math.sin(-ang) + dy * Math.cos(-ang);
        
        // Stretch bodies are 1.5 grid cells long by 0.8 wide (Resistor)
        // Capacitors are 1.2 by 0.9 cells rounded
        boolean isCapacitor = "Capacitor".equals(getType()) || (getName() != null && getName().startsWith("C") && getType() == null);
        boolean isElectroCap = "Capacitor (Polarized)".equals(getType());
        boolean isDiode = "Diode".equals(getType());
        
        if (isElectroCap) {
            return (rx >= -0.8 && rx <= 0.8) && (ry >= -0.8 && ry <= 0.8);
        } else if (isCapacitor) {
            return (rx >= -0.6 && rx <= 0.6) && (ry >= -0.45 && ry <= 0.45);
        } else {
            return (rx >= -0.75 && rx <= 0.75) && (ry >= -0.4 && ry <= 0.4);
        }
    }
}
