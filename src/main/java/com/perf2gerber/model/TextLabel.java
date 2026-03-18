package com.perf2gerber.model;

public class TextLabel {
    private String text;
    private Trace.Layer layer;
    private double x;
    private double y;
    private double fontSize; // NOUVEAU : Taille en mm
    private double rotation; // NOUVEAU : Angle en degrés

    public TextLabel(String text, Trace.Layer layer, double x, double y, double fontSize, double rotation) {
        this.text = text;
        this.layer = layer;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
        this.rotation = rotation;
    }

    public String getText() { return text; }
    public Trace.Layer getLayer() { return layer; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; } // Pour le déplacement futur

    public double getY() { return y; }
    public void setY(double y) { this.y = y; } // Pour le déplacement futur

    public double getFontSize() { return fontSize; }
    public double getRotation() { return rotation; }
}