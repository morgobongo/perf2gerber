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

    private java.util.function.BiConsumer<Integer, Integer> onCursorMoved;

    public EditorCanvas(Board board) {
        this.board = board;
        updateSize();
        setupMouseListeners();
    }

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
        if (isViewFlipped) gridX = (board.getColumns() - 1) - gridX;
        int gridY = board.getRows() - 1 - (int) Math.round(rawGridYScreen);

        if (gridX <= 0 || gridX >= board.getColumns() - 1 || gridY <= 0 || gridY >= board.getRows() - 1) {
            hoverGridX = null; hoverGridY = null;
        } else {
            hoverGridX = gridX; hoverGridY = gridY;
        }
        if (onCursorMoved != null) onCursorMoved.accept(hoverGridX, hoverGridY);
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
            hoverGridX = null; hoverGridY = null;
            if (onCursorMoved != null) onCursorMoved.accept(null, null);
            draw();
        });

        this.setOnMousePressed(event -> {
            // --- LOGIQUE D'EFFACEMENT SÉLECTIVE ---
            if (currentTool == Tool.ERASE) {
                // Est-on sur un pad actif ?
                boolean onActivePad = false;
                if (hoverGridX != null && hoverGridY != null) {
                    Pad p = board.getPad(hoverGridX, hoverGridY);
                    if (p != null && p.isUsed()) {
                        onActivePad = true;
                        p.deactivate(); // On delete le pad
                    }
                }

                // Si on n'était pas sur un pad actif, on cherche à effacer la trace
                if (!onActivePad) {
                    eraseSegmentAt(event.getX(), event.getY());
                }
                draw();
                return;
            }

            // --- DESSIN ---
            if (hoverGridX == null || hoverGridY == null) return;
            Pad clickedPad = board.getPad(hoverGridX, hoverGridY);
            if (clickedPad != null) {
                clickedPad.activate();
                if (currentTrace == null) {
                    currentTrace = new Trace(activeLayer, currentTraceWidth);
                    currentTrace.addPoint(hoverGridX, hoverGridY);
                    board.addTrace(currentTrace);
                } else {
                    currentTrace.addPoint(hoverGridX, hoverGridY);
                    if (!isCommandPressed) endCurrentTrace();
                }
                draw();
            }
        });
    }

    private void eraseSegmentAt(double mouseX, double mouseY) {
        double physX = (mouseX - padding) / zoomLevel;
        if (isViewFlipped) physX = (board.getColumns() - 1) * board.getGridSpacing() - physX;
        double physY = ((board.getRows() - 1) * board.getGridSpacing()) - ((mouseY - padding) / zoomLevel);

        Trace traceToSplit = null;
        int splitIndex = -1;
        double threshold = 1.2;

        for (Trace trace : board.getTraces()) {
            List<Trace.GridPoint> pts = trace.getSegments();
            for (int i = 0; i < pts.size() - 1; i++) {
                double x1 = pts.get(i).x() * board.getGridSpacing();
                double y1 = pts.get(i).y() * board.getGridSpacing();
                double x2 = pts.get(i + 1).x() * board.getGridSpacing();
                double y2 = pts.get(i + 1).y() * board.getGridSpacing();
                if (distToSegment(physX, physY, x1, y1, x2, y2) < threshold) {
                    traceToSplit = trace;
                    splitIndex = i;
                    break;
                }
            }
            if (traceToSplit != null) break;
        }

        if (traceToSplit != null) {
            List<Trace.GridPoint> allPoints = new ArrayList<>(traceToSplit.getSegments());
            List<Trace.GridPoint> partA = new ArrayList<>(allPoints.subList(0, splitIndex + 1));
            List<Trace.GridPoint> partB = new ArrayList<>(allPoints.subList(splitIndex + 1, allPoints.size()));
            board.removeTrace(traceToSplit);
            if (partA.size() >= 2) {
                Trace nA = new Trace(traceToSplit.getLayer(), traceToSplit.getWidth());
                for (Trace.GridPoint p : partA) nA.addPoint(p.x(), p.y());
                board.addTrace(nA);
            }
            if (partB.size() >= 2) {
                Trace nB = new Trace(traceToSplit.getLayer(), traceToSplit.getWidth());
                for (Trace.GridPoint p : partB) nB.addPoint(p.x(), p.y());
                board.addTrace(nB);
            }
        }
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    public void endCurrentTrace() {
        if (currentTrace != null) {
            if (currentTrace.getSegments().size() < 2) board.removeTrace(currentTrace);
            currentTrace = null;
            draw();
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
            gc.setStroke(trace.getLayer() == Trace.Layer.TOP ? Color.web("#E74C3C", opacity) : Color.web("#3498DB", opacity));
            gc.setLineWidth(physicalToScreen(trace.getWidth()));
            List<Trace.GridPoint> segments = trace.getSegments();
            if (segments.size() > 1) {
                gc.beginPath();
                for (int i = 0; i < segments.size(); i++) {
                    double sx = getScreenX(segments.get(i).x());
                    double sy = getScreenY(segments.get(i).y());
                    if (i == 0) gc.moveTo(sx, sy); else gc.lineTo(sx, sy);
                }
                gc.stroke();
            }
        }

        if (currentTool == Tool.DRAW && currentTrace != null && hoverGridX != null && hoverGridY != null) {
            List<Trace.GridPoint> segments = currentTrace.getSegments();
            if (!segments.isEmpty()) {
                Trace.GridPoint lastPt = segments.get(segments.size() - 1);
                gc.setLineWidth(physicalToScreen(currentTraceWidth));
                gc.setLineDashes(physicalToScreen(1.5), physicalToScreen(1.5));
                gc.setStroke(activeLayer == Trace.Layer.TOP ? Color.web("#E74C3C", 0.6) : Color.web("#3498DB", 0.6));
                gc.strokeLine(getScreenX(lastPt.x()), getScreenY(lastPt.y()), getScreenX(hoverGridX), getScreenY(hoverGridY));
                gc.setLineDashes(null);
            }
        }

        // Zone de coupe
        gc.setStroke(Color.web("#FFFFFF", 0.5));
        gc.setLineWidth(1.0);
        gc.setLineDashes(5.0);
        gc.strokeRect(padding, padding, physicalToScreen((board.getColumns() - 1) * board.getGridSpacing()), physicalToScreen((board.getRows() - 1) * board.getGridSpacing()));
        gc.setLineDashes(null);

        for (int x = 0; x < board.getColumns(); x++) {
            for (int y = 0; y < board.getRows(); y++) {
                if (x == 0 || x == board.getColumns() - 1 || y == 0 || y == board.getRows() - 1) continue;
                Pad pad = board.getPad(x, y);
                if (pad != null) drawPad(gc, pad);
            }
        }

        // --- HOVER RING INTELLIGENT ---
        if (hoverGridX != null && hoverGridY != null) {
            Pad p = board.getPad(hoverGridX, hoverGridY);
            if (p != null) {
                boolean showRing = true;

                // Si on est en mode gomme, on ne montre le rond rouge QUE si le pad est actif
                if (currentTool == Tool.ERASE && !p.isUsed()) {
                    showRing = false;
                }

                if (showRing) {
                    double sx = getScreenX(hoverGridX);
                    double sy = getScreenY(hoverGridY);
                    double radius = physicalToScreen(p.getCopperDiameter()) / 2.0 + physicalToScreen(0.4);
                    gc.setStroke(currentTool == Tool.ERASE ? Color.web("#E74C3C", 0.8) : Color.web("#FFFFFF", 0.7));
                    gc.setLineWidth(2.0);
                    gc.strokeOval(sx - radius, sy - radius, radius * 2, radius * 2);
                }
            }
        }
    }

    private void drawPad(GraphicsContext gc, Pad pad) {
        double screenX = getScreenX(pad.getGridX());
        double screenY = getScreenY(pad.getGridY());
        double copperRadiusPx = physicalToScreen(pad.getCopperDiameter()) / 2.0;
        double holeRadiusPx = physicalToScreen(pad.getHoleDiameter()) / 2.0;
        gc.setFill(pad.isUsed() ? Color.web("#D4AF37") : Color.web("#4A3C13"));
        gc.fillOval(screenX - copperRadiusPx, screenY - copperRadiusPx, copperRadiusPx * 2, copperRadiusPx * 2);
        gc.setFill(pad.isUsed() ? Color.web("#000000") : Color.web("#222222"));
        gc.fillOval(screenX - holeRadiusPx, screenY - holeRadiusPx, holeRadiusPx * 2, holeRadiusPx * 2);
    }

    public void setBoard(Board newBoard) {
        this.board = newBoard;
        updateSize();
        draw();
    }
}