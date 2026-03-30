package com.perf2gerber.model;

public class StretchComponent extends Component {
    private int endX;
    private int endY;

    public StretchComponent() {}

    public int getEndX() { return endX; }
    public void setEndX(int endX) { this.endX = endX; }

    public int getEndY() { return endY; }
    public void setEndY(int endY) { this.endY = endY; }
}
