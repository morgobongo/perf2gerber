package com.perf2gerber.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a copper trace connecting multiple pads on the board.
 * It contains its own definitions for layers and grid points to keep the architecture clean.
 */
public class Trace {

    /**
     * Represents the physical side of the board where the copper is drawn.
     */
    public enum Layer {
        TOP,
        BOTTOM
    }

    /**
     * A lightweight data structure to hold grid coordinates for the trace segments.
     *
     * @param x The X index on the board grid.
     * @param y The Y index on the board grid.
     */
    public record GridPoint(int x, int y) {}

    private final List<GridPoint> segments;
    private final Layer layer;
    private final double width;

    /**
     * Constructs a new empty Trace.
     *
     * @param layer The layer on which this trace is drawn (TOP or BOTTOM).
     * @param width The width of the copper trace in millimeters.
     */
    public Trace(Layer layer, double width) {
        this.layer = layer;
        this.width = width;
        this.segments = new ArrayList<>();
    }

    /**
     * Adds a new point to the trace, extending the line.
     *
     * @param point The grid coordinate to add.
     */
    public void addPoint(GridPoint point) {
        this.segments.add(point);
    }

    /**
     * Convenience method to add a new point using raw x and y coordinates.
     *
     * @param x The grid X coordinate.
     * @param y The grid Y coordinate.
     */
    public void addPoint(int x, int y) {
        this.segments.add(new GridPoint(x, y));
    }

    /**
     * Shifts all points in this trace by a specific amount.
     * Used when the board is resized from the Left or Bottom.
     *
     * @param dx The amount to shift on the X axis.
     * @param dy The amount to shift on the Y axis.
     */
    public void shiftPoints(int dx, int dy) {
        List<GridPoint> newSegments = new ArrayList<>();
        for (GridPoint pt : segments) {
            newSegments.add(new GridPoint(pt.x() + dx, pt.y() + dy));
        }
        segments.clear();
        segments.addAll(newSegments);
    }

    // --- Standard Getters ---

    public Layer getLayer() {
        return layer;
    }

    public double getWidth() {
        return width;
    }

    /**
     * Returns an unmodifiable view of the segments to protect the internal list
     * from being accidentally modified outside of this class.
     *
     * @return The list of points making up this trace.
     */
    public List<GridPoint> getSegments() {
        return Collections.unmodifiableList(segments);
    }
}