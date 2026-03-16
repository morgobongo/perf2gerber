package com.perf2gerber.ui;

import com.perf2gerber.model.Board;
import com.perf2gerber.model.Pad;
import com.perf2gerber.model.Trace;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;

public class EditorCanvas extends Canvas {

    public enum Tool { DRAW, ERASE }

    private Board board;
    private double zoomLevel = 15.0;
    private final double padding = 40.0;

    private Trace currentTrace = null;
    private Trace.Layer activeLayer = Trace.Layer.BOTTOM;
    private double currentTraceWidth = 1.0;

    private Tool currentTool = Tool.DRAW;
    private boolean isCommandPressed = false;
    private boolean isViewFlipped = false;

    private Integer hoverGridX = null;
    private Integer hoverGridY = null;

    // --- NOUVEAU : Callback pour envoyer les coordonnées à la barre d'état ---
    private java.util.function.BiConsumer<Integer, Integer> onCursorMoved;

    public EditorCanvas(Board board) {
        this.board = board;
        updateSize();
        setupMouseListeners();
    }

    // --- NOUVEAU : Setter pour le Callback ---
    public void setOnCursorMoved(java.util.function.BiConsumer<Integer, Integer> listener) {
        this.onCursorMoved = listener;
    }

    public void updateSize() {
        double gridVisualWidth = physicalToScreen((board.getColumns() - 1) * board.getGridSpacing());
        double gridVisualHeight = physicalToScreen((board.getRows() - 1) * board.getGridSpacing());

        setWidth(gridVisualWidth + (padding * 2));
        setHeight(gridVisualHeight + (padding * 2));
        draw();
    }

    public void setTool(Tool tool) {
        this.currentTool = tool;
        this.currentTrace = null;
        draw();
    }

    public void setCommandPressed(boolean pressed) {
        this.isCommandPressed = pressed;
    }

    public void setActiveLayer(Trace.Layer layer) {
        this.activeLayer = layer;
        this.currentTrace = null;
        draw();
    }

    public void setViewFlipped(boolean flipped) {
        this.isViewFlipped = flipped;
        draw();
    }

    public void setCurrentTraceWidth(double width) {
        this.currentTraceWidth = width;
    }

    private double physicalToScreen(double physicalMm) {
        return physicalMm * zoomLevel;
    }

    private double getScreenX(int gridX) {
        int renderX = isViewFlipped ? (board.getColumns() - 1 - gridX) : gridX;
        return padding + physicalToScreen(renderX * board.getGridSpacing());
    }

    private double getScreenY(int gridY) {
        return padding + physicalToScreen((board.getRows() - 1 - gridY) * board.getGridSpacing());
    }

    private void updateHoverPosition(double mouseX, double mouseY) {
        double rawGridX = (mouseX - padding) / physicalToScreen(board.getGridSpacing());
        double rawGridYScreen = (mouseY - padding) / physicalToScreen(board.getGridSpacing());

        int gridX = (int) Math.round(rawGridX);
        if (isViewFlipped) {
            gridX = (board.getColumns() - 1) - gridX;
        }
        int gridY = board.getRows() - 1 - (int) Math.round(rawGridYScreen);

        if (gridX <= 0 || gridX >= board.getColumns() - 1 || gridY <= 0 || gridY >= board.getRows() - 1) {
            hoverGridX = null;
            hoverGridY = null;
        } else {
            hoverGridX = gridX;
            hoverGridY = gridY;
        }

        // --- NOUVEAU : On avertit l'interface principale (App) ---
        if (onCursorMoved != null) {
            onCursorMoved.accept(hoverGridX, hoverGridY);
        }
    }

    private void setupMouseListeners() {
        this.setOnScroll(event -> {
            if (event.getDeltaY() > 0) zoomLevel += 2.0;
            else zoomLevel -= 2.0;

            if (zoomLevel < 5.0) zoomLevel = 5.0;
            if (zoomLevel > 60.0) zoomLevel = 60.0;

            updateSize();
            event.consume();
        });

        this.setOnMouseMoved(event -> {
            updateHoverPosition(event.getX(), event.getY());
            draw();
        });

        this.setOnMouseDragged(event -> {
            updateHoverPosition(event.getX(), event.getY());
            draw();
        });

        this.setOnMouseExited(event -> {
            hoverGridX = null;
            hoverGridY = null;
            if (onCursorMoved != null) onCursorMoved.accept(null, null); // Efface les coords
            draw();
        });

        this.setOnMousePressed(event -> {
            if (hoverGridX == null || hoverGridY == null) return;

            int gridX = hoverGridX;
            int gridY = hoverGridY;

            Pad clickedPad = board.getPad(gridX, gridY);

            if (clickedPad != null) {
                if (currentTool == Tool.ERASE) {
                    eraseTracesOnPad(gridX, gridY);
                    return;
                }

                clickedPad.activate();

                if (currentTrace == null) {
                    currentTrace = new Trace(activeLayer, currentTraceWidth);
                    currentTrace.addPoint(gridX, gridY);
                    board.addTrace(currentTrace);
                } else {
                    currentTrace.addPoint(gridX, gridY);
                    if (!isCommandPressed) {
                        endCurrentTrace();
                    }
                }
                draw();
            }
        });
    }

    public void endCurrentTrace() {
        if (currentTrace != null) {
            if (currentTrace.getSegments().size() < 2) {
                board.removeTrace(currentTrace);
            }
            currentTrace = null;
            recalculatePadStates();
            draw();
        }
    }

    private void eraseTracesOnPad(int gridX, int gridY) {
        Trace.GridPoint targetPoint = new Trace.GridPoint(gridX, gridY);
        List<Trace> tracesToRemove = new ArrayList<>();

        for (Trace trace : board.getTraces()) {
            if (trace.getSegments().contains(targetPoint)) {
                tracesToRemove.add(trace);
            }
        }
        for (Trace trace : tracesToRemove) {
            board.removeTrace(trace);
        }
        recalculatePadStates();
        draw();
    }

    private void recalculatePadStates() {
        for (int x = 0; x < board.getColumns(); x++) {
            for (int y = 0; y < board.getRows(); y++) {
                if (board.getPad(x, y) != null) board.getPad(x, y).deactivate();
            }
        }
        for (Trace trace : board.getTraces()) {
            for (Trace.GridPoint pt : trace.getSegments()) {
                if (board.getPad(pt.x(), pt.y()) != null) board.getPad(pt.x(), pt.y()).activate();
            }
        }
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        for (Trace trace : board.getTraces()) {
            boolean isActiveLayer = (trace.getLayer() == activeLayer);
            double opacity = isActiveLayer ? 1.0 : 0.2;

            if (trace.getLayer() == Trace.Layer.TOP) gc.setStroke(Color.web("#E74C3C", opacity));
            else gc.setStroke(Color.web("#3498DB", opacity));

            gc.setLineWidth(physicalToScreen(trace.getWidth()));
            List<Trace.GridPoint> segments = trace.getSegments();

            if (segments.size() > 1) {
                gc.beginPath();
                for (int i = 0; i < segments.size(); i++) {
                    Trace.GridPoint pt = segments.get(i);
                    Pad p = board.getPad(pt.x(), pt.y());
                    if (p == null) continue;

                    double sx = getScreenX(p.getGridX());
                    double sy = getScreenY(p.getGridY());

                    if (i == 0) gc.moveTo(sx, sy);
                    else gc.lineTo(sx, sy);
                }
                gc.stroke();
            }
        }

        if (currentTool == Tool.DRAW && currentTrace != null && hoverGridX != null && hoverGridY != null) {
            List<Trace.GridPoint> segments = currentTrace.getSegments();
            if (!segments.isEmpty()) {
                Trace.GridPoint lastPt = segments.get(segments.size() - 1);

                double startX = getScreenX(lastPt.x());
                double startY = getScreenY(lastPt.y());
                double endX = getScreenX(hoverGridX);
                double endY = getScreenY(hoverGridY);

                gc.setLineWidth(physicalToScreen(currentTraceWidth));
                gc.setLineDashes(physicalToScreen(1.5), physicalToScreen(1.5));

                if (activeLayer == Trace.Layer.TOP) gc.setStroke(Color.web("#E74C3C", 0.6));
                else gc.setStroke(Color.web("#3498DB", 0.6));

                gc.strokeLine(startX, startY, endX, endY);
                gc.setLineDashes(null);
            }
        }

        gc.setStroke(Color.web("#FFFFFF", 0.5));
        gc.setLineWidth(1.0);
        gc.setLineDashes(5.0);

        double cutWidth = physicalToScreen((board.getColumns() - 1) * board.getGridSpacing());
        double cutHeight = physicalToScreen((board.getRows() - 1) * board.getGridSpacing());

        gc.strokeRect(padding, padding, cutWidth, cutHeight);
        gc.setLineDashes(null);

        for (int x = 0; x < board.getColumns(); x++) {
            for (int y = 0; y < board.getRows(); y++) {
                if (x == 0 || x == board.getColumns() - 1 || y == 0 || y == board.getRows() - 1) continue;
                Pad pad = board.getPad(x, y);
                if (pad != null) drawPad(gc, pad);
            }
        }

        if (hoverGridX != null && hoverGridY != null) {
            double sx = getScreenX(hoverGridX);
            double sy = getScreenY(hoverGridY);
            Pad p = board.getPad(hoverGridX, hoverGridY);
            if (p != null) {
                double radius = physicalToScreen(p.getCopperDiameter()) / 2.0 + physicalToScreen(0.4);
                if (currentTool == Tool.ERASE) gc.setStroke(Color.web("#E74C3C", 0.8));
                else gc.setStroke(Color.web("#FFFFFF", 0.7));

                gc.setLineWidth(2.0);
                gc.strokeOval(sx - radius, sy - radius, radius * 2, radius * 2);
            }
        }
    }

    private void drawPad(GraphicsContext gc, Pad pad) {
        double screenX = getScreenX(pad.getGridX());
        double screenY = getScreenY(pad.getGridY());

        double copperRadiusPx = physicalToScreen(pad.getCopperDiameter()) / 2.0;
        double holeRadiusPx = physicalToScreen(pad.getHoleDiameter()) / 2.0;

        if (pad.isUsed()) gc.setFill(Color.web("#D4AF37"));
        else gc.setFill(Color.web("#4A3C13"));

        gc.fillOval(screenX - copperRadiusPx, screenY - copperRadiusPx, copperRadiusPx * 2, copperRadiusPx * 2);

        if (pad.isUsed()) gc.setFill(Color.web("#000000"));
        else gc.setFill(Color.web("#222222"));

        gc.fillOval(screenX - holeRadiusPx, screenY - holeRadiusPx, holeRadiusPx * 2, holeRadiusPx * 2);
    }

    public void setBoard(Board newBoard) {
        this.board = newBoard;
        updateSize(); // Redimensionne la grille visuelle
        draw();       // Redessine tout avec les nouvelles pistes
    }

}