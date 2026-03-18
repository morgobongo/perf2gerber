package com.perf2gerber.model;

public class TextLabel {
    private String text;
    private Trace.Layer layer;
    private double x;
    private double y;

    public TextLabel(String text, Trace.Layer layer, double x, double y) {
        this.text = text;
        this.layer = layer;
        this.x = x;
        this.y = y;
    }

    public String getText() { return text; }
    public Trace.Layer getLayer() { return layer; }
    public double getX() { return x; }
    public double getY() { return y; }
}