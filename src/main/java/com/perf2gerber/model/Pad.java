package com.perf2gerber.model;

/**
 * Represents a single hole/pad on the perfboard grid.
 * It stores its position on the grid and calculates its physical coordinates when needed.
 */
public class Pad {

    private final int gridX;
    private final int gridY;
    private final double copperDiameter;
    private final double holeDiameter;
    private boolean isUsed;

    /**
     * Constructs a new Pad at the specified grid coordinates.
     *
     * @param gridX          The X index on the board grid.
     * @param gridY          The Y index on the board grid.
     * @param copperDiameter The outer copper diameter in millimeters.
     * @param holeDiameter   The inner drill hole diameter in millimeters.
     */
    public Pad(int gridX, int gridY, double copperDiameter, double holeDiameter) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.copperDiameter = copperDiameter;
        this.holeDiameter = holeDiameter;
        this.isUsed = false; // A pad is always unused by default until a trace connects to it
    }

    /**
     * Calculates the true physical X coordinate based on the grid spacing.
     *
     * @param gridSpacing The physical distance between two pads (e.g., 2.54mm).
     * @return The physical X coordinate in millimeters.
     */
    public double getPhysicalX(double gridSpacing) {
        return this.gridX * gridSpacing;
    }

    /**
     * Calculates the true physical Y coordinate based on the grid spacing.
     *
     * @param gridSpacing The physical distance between two pads (e.g., 2.54mm).
     * @return The physical Y coordinate in millimeters.
     */
    public double getPhysicalY(double gridSpacing) {
        return this.gridY * gridSpacing;
    }

    /**
     * Marks the pad as used, meaning it will be exported to the Gerber files.
     */
    public void activate() {
        this.isUsed = true;
    }

    /**
     * Marks the pad as unused. It will be ignored during Gerber export.
     */
    public void deactivate() {
        this.isUsed = false;
    }

    // --- Standard Getters ---

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public double getCopperDiameter() {
        return copperDiameter;
    }

    public double getHoleDiameter() {
        return holeDiameter;
    }

    public boolean isUsed() {
        return isUsed;
    }
}