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
    public void setText(String text) { this.text = text; }
    public Trace.Layer getLayer() { return layer; }
    public void setLayer(Trace.Layer layer) { this.layer = layer; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; } // Pour le déplacement futur

    public double getY() { return y; }
    public void setY(double y) { this.y = y; } // Pour le déplacement futur

    public double getFontSize() { return fontSize; }
    public void setFontSize(double fontSize) { this.fontSize = fontSize; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
}