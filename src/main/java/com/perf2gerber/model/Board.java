package com.perf2gerber.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

    private int columns;
    private int rows;
    private double gridSpacing;
    private double defaultCopperDiameter;
    private double defaultHoleDiameter;

    private Pad[][] grid;
    private final List<Trace> traces;

    public Board(int columns, int rows, double gridSpacing, double copperDiameter, double holeDiameter) {
        this.columns = columns;
        this.rows = rows;
        this.gridSpacing = gridSpacing;
        this.defaultCopperDiameter = copperDiameter;
        this.defaultHoleDiameter = holeDiameter;
        this.traces = new ArrayList<>();

        initializeGrid();
    }

    private void initializeGrid() {
        this.grid = new Pad[columns][rows];
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                grid[x][y] = new Pad(x, y, defaultCopperDiameter, defaultHoleDiameter);
            }
        }
    }

    /**
     * Resizes the board dynamically by adding or removing columns/rows from specific sides.
     * It handles the shifting of all existing pads and traces so the design doesn't break.
     *
     * @param addLeft   Number of columns to add to the Left (negative to remove).
     * @param addRight  Number of columns to add to the Right.
     * @param addBottom Number of rows to add to the Bottom.
     * @param addTop    Number of rows to add to the Top.
     */
    public void resizeBoard(int addLeft, int addRight, int addBottom, int addTop) {
        int newColumns = this.columns + addLeft + addRight;
        int newRows = this.rows + addBottom + addTop;

        // Prevent shrinking below a minimum size (e.g., 3x3)
        if (newColumns < 3 || newRows < 3) return;

        Pad[][] newGrid = new Pad[newColumns][newRows];

        // 1. Recreate grid and copy old pad states with the correct offset
        for (int x = 0; x < newColumns; x++) {
            for (int y = 0; y < newRows; y++) {
                // On utilise le defaultCopperDiameter qui est mis à jour dynamiquement
                newGrid[x][y] = new Pad(x, y, defaultCopperDiameter, defaultHoleDiameter);

                // Map to the old grid to see if it was used
                int oldX = x - addLeft;
                int oldY = y - addBottom;

                if (oldX >= 0 && oldX < this.columns && oldY >= 0 && oldY < this.rows) {
                    if (this.grid[oldX][oldY].isUsed()) {
                        newGrid[x][y].activate();
                    }
                }
            }
        }

        // 2. Shift all existing traces
        // If we added columns to the left, everything shifts right (+addLeft).
        // If we added rows to the bottom, everything shifts up (+addBottom).
        for (Trace trace : traces) {
            trace.shiftPoints(addLeft, addBottom);
        }

        // 3. Apply changes
        this.columns = newColumns;
        this.rows = newRows;
        this.grid = newGrid;
    }

    public Pad getPad(int x, int y) {
        if (x >= 0 && x < columns && y >= 0 && y < rows) {
            return grid[x][y];
        }
        return null;
    }

    // --- NOUVELLE FONCTIONNALITÉ : REDIMENSIONNEMENT GLOBAL DES PADS ---

    /**
     * Modifie le diamètre de cuivre de TOUTES les pastilles (Pads) du circuit.
     * Utile pour agrandir la surface de soudure globale.
     * @param newCopperDiameterMm Le nouveau diamètre en millimètres (ex: 2.5)
     */
    public void setGlobalPadCopperDiameter(double newCopperDiameterMm) {
        this.defaultCopperDiameter = newCopperDiameterMm; // <-- Correction pour les futurs agrandissements du board !

        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Pad pad = getPad(x, y);
                if (pad != null) {
                    pad.setCopperDiameter(newCopperDiameterMm);
                }
            }
        }
    }

    public void addTrace(Trace trace) {
        if (trace != null) this.traces.add(trace);
    }

    public void removeTrace(Trace trace) {
        this.traces.remove(trace);
    }

    public double getPhysicalWidth() {
        return columns * gridSpacing;
    }

    public double getPhysicalHeight() {
        return rows * gridSpacing;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public double getGridSpacing() {
        return gridSpacing;
    }

    public List<Trace> getTraces() {
        return Collections.unmodifiableList(traces);
    }
}