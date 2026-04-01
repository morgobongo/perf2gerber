package com.perf2gerber.model;

public class FixedComponent extends Component {
    private String pinoutOrCount;

    public FixedComponent() {}

    public String getPinoutOrCount() { return pinoutOrCount; }
    public void setPinoutOrCount(String pinoutOrCount) { this.pinoutOrCount = pinoutOrCount; }

    @Override
    public Component cloneComponent() {
        FixedComponent clone = new FixedComponent();
        clone.setName(this.getName());
        clone.setValue(this.getValue());
        clone.setStartX(this.getStartX());
        clone.setStartY(this.getStartY());
        clone.setRotation(this.getRotation());
        clone.setShowName(this.isShowName());
        clone.setShowValue(this.isShowValue());
        clone.setType(this.getType());
        clone.setColor(this.getColor());
        clone.setPinoutOrCount(this.pinoutOrCount);
        return clone;
    }

    @Override
    public boolean contains(double worldX, double worldY) {
        double dx = worldX - getStartX();
        double dy = worldY - getStartY();
        // Map logical grid delta into visual screen delta (because JavaFX Y goes down, but logical grid Y goes up)
        double vx = dx;
        double vy = -dy;
        
        // Un-rotate the visual delta (JavaFX rotations are clockwise, so un-rotate is -rotation)
        double rad = Math.toRadians(-getRotation());
        double rx = vx * Math.cos(rad) - vy * Math.sin(rad);
        double ry = vx * Math.sin(rad) + vy * Math.cos(rad);
        
        boolean isTransistor = "Transistor".equals(getType())
                || (getName() != null && getName().startsWith("Q") && getType() == null);
        if (isTransistor) {
            return (rx >= -1.4 && rx <= 1.4) && (ry >= -0.4 && ry <= 0.4);
        } else {
            int pinsPerSide = 4;
            try {
                if (pinoutOrCount != null) {
                    pinsPerSide = Integer.parseInt(pinoutOrCount);
                }
            } catch (Exception e) {}
            return (rx >= -3.5 && rx <= 0.5) && (ry >= -0.5 && ry <= pinsPerSide - 0.5);
        }
    }
}
