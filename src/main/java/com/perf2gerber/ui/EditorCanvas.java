package com.perf2gerber.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perf2gerber.model.Board;
import com.perf2gerber.model.Component;
import com.perf2gerber.model.ComponentAdapter;
import com.perf2gerber.model.FixedComponent;
import com.perf2gerber.model.StretchComponent;
import com.perf2gerber.model.Pad;
import com.perf2gerber.model.Trace;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorCanvas extends Canvas {

    public enum Tool {
        POINTER, PADS, WIRE, ERASE, TEXT
    }

    private Board board;
    private double zoomLevel = 15.0;
    private final double padding = 40.0;

    // Virtual camera variables
    private double panX = 0.0;
    private double panY = 0.0;
    private double scale = 1.0;

    private Trace currentTrace = null;
    private Trace.Layer activeLayer = Trace.Layer.BOTTOM;
    private double currentTraceWidth = 1.0;

    private Tool currentTool = Tool.POINTER;
    private boolean isCommandPressed = false;
    private boolean isViewFlipped = false;

    private Integer hoverGridX = null;
    private Integer hoverGridY = null;

    private java.util.function.BiConsumer<Integer, Integer> onCursorMoved;
    private java.util.function.Consumer<Board> onBoardReplaced;

    // Undo / Redo stacks
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentAdapter())
            .create();

    private boolean isUndoingOrRedoing = false;

    // Variable pour mémoriser le texte qu'on est en train de déplacer avec le
    // pointeur
    private com.perf2gerber.model.TextLabel draggedTextLabel = null;

    public EditorCanvas(Board board) {
        this.board = board;
        updateSize();
        setupMouseListeners();
        saveState(); // Initial state
    }

    public void saveState() {
        if (board != null && !isUndoingOrRedoing) {
            String newState = gson.toJson(board);
            // Don't save duplicate states
            if (undoStack.isEmpty() || !undoStack.peek().equals(newState)) {
                undoStack.push(newState);
                redoStack.clear(); // Clear redo stack on new action
            }
        }
    }

    public void undo() {
        if (undoStack.size() > 1) { // Keep initial state
            isUndoingOrRedoing = true;
            String currentState = undoStack.pop();
            redoStack.push(currentState); // Move current state to redo

            String previousState = undoStack.peek(); // Get previous state
            this.board = gson.fromJson(previousState, Board.class);
            if (onBoardReplaced != null)
                onBoardReplaced.accept(this.board);
            updateSize();
            draw();
            isUndoingOrRedoing = false;
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            isUndoingOrRedoing = true;
            String nextState = redoStack.pop();
            undoStack.push(nextState);
            this.board = gson.fromJson(nextState, Board.class);
            if (onBoardReplaced != null)
                onBoardReplaced.accept(this.board);
            updateSize();
            draw();
            isUndoingOrRedoing = false;
        }
    }

    public void setOnCursorMoved(java.util.function.BiConsumer<Integer, Integer> listener) {
        this.onCursorMoved = listener;
    }

    public void setOnBoardReplaced(java.util.function.Consumer<Board> listener) {
        this.onBoardReplaced = listener;
    }

    public Board getBoard() {
        return this.board;
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
        endCurrentTrace();
        draw();
    }

    public void setCommandPressed(boolean pressed) {
        this.isCommandPressed = pressed;
        draw();
    }

    public void setActiveLayer(Trace.Layer layer) {
        this.activeLayer = layer;
        endCurrentTrace();
        draw();
    }

    public void setViewFlipped(boolean flipped) {
        this.isViewFlipped = flipped;
        draw();
    }

    public void setCurrentTraceWidth(double width) {
        this.currentTraceWidth = width;
    }

    public void zoom(double factor) {
        this.scale *= factor;
        draw();
    }

    private double convertToWorldX(double screenX) {
        return (screenX - panX) / scale;
    }

    private double convertToWorldY(double screenY) {
        return (screenY - panY) / scale;
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

    private double getScreenXFromPhys(double physX) {
        double maxPhysX = (board.getColumns() - 1) * board.getGridSpacing();
        double renderX = isViewFlipped ? (maxPhysX - physX) : physX;
        return padding + physicalToScreen(renderX);
    }

    private double getScreenYFromPhys(double physY) {
        double maxPhysY = (board.getRows() - 1) * board.getGridSpacing();
        return padding + physicalToScreen(maxPhysY - physY);
    }

    private void updateHoverPosition(double mouseX, double mouseY) {
        double rawGridX = (mouseX - padding) / physicalToScreen(board.getGridSpacing());
        double rawGridYScreen = (mouseY - padding) / physicalToScreen(board.getGridSpacing());
        int gridX = (int) Math.round(rawGridX);
        if (isViewFlipped)
            gridX = (board.getColumns() - 1) - gridX;
        int gridY = board.getRows() - 1 - (int) Math.round(rawGridYScreen);

        if (gridX <= 0 || gridX >= board.getColumns() - 1 || gridY <= 0 || gridY >= board.getRows() - 1) {
            hoverGridX = null;
            hoverGridY = null;
        } else {
            hoverGridX = gridX;
            hoverGridY = gridY;
        }
        if (onCursorMoved != null)
            onCursorMoved.accept(hoverGridX, hoverGridY);
    }

    private void setupMouseListeners() {
        this.setOnScroll(event -> {
            panX += event.getDeltaX();
            panY += event.getDeltaY();
            draw();
            event.consume();
        });

        this.setOnMouseMoved(event -> {
            updateHoverPosition(convertToWorldX(event.getX()), convertToWorldY(event.getY()));
            draw();
        });

        this.setOnMouseDragged(event -> {
            double worldX = convertToWorldX(event.getX());
            double worldY = convertToWorldY(event.getY());
            updateHoverPosition(worldX, worldY);

            // Si on est en train de glisser un texte avec le Pointeur
            if (currentTool == Tool.POINTER && draggedTextLabel != null) {
                double newPhysX = (worldX - padding) / zoomLevel;
                if (isViewFlipped)
                    newPhysX = (board.getColumns() - 1) * board.getGridSpacing() - newPhysX;
                double newPhysY = ((board.getRows() - 1) * board.getGridSpacing()) - ((worldY - padding) / zoomLevel);

                draggedTextLabel.setX(newPhysX);
                draggedTextLabel.setY(newPhysY);
            }

            draw();
        });

        this.setOnMouseReleased(event -> {
            if (draggedTextLabel != null) {
                draggedTextLabel = null; // On lâche le texte
                saveState(); // On sauvegarde l'état pour que "Undo" fonctionne !
            }
        });

        this.setOnMouseExited(event -> {
            hoverGridX = null;
            hoverGridY = null;
            if (onCursorMoved != null)
                onCursorMoved.accept(null, null);
            draw();
        });

        this.setOnMousePressed(event -> {
            double worldX = convertToWorldX(event.getX());
            double worldY = convertToWorldY(event.getY());

            // --- 1. LOGIQUE DU POINTEUR (Attraper un texte) ---
            if (currentTool == Tool.POINTER) {
                double physXClick = (worldX - padding) / zoomLevel;
                if (isViewFlipped)
                    physXClick = (board.getColumns() - 1) * board.getGridSpacing() - physXClick;
                double physYClick = ((board.getRows() - 1) * board.getGridSpacing()) - ((worldY - padding) / zoomLevel);

                // Cherche si on a cliqué sur un texte de la couche active
                if (board.getTextLabels() != null) {
                    for (com.perf2gerber.model.TextLabel label : board.getTextLabels()) {
                        if (label.getLayer() == activeLayer) {
                            // Hitbox un peu plus large (3.0mm) pour faciliter la sélection
                            if (Math.abs(label.getX() - physXClick) < 3.0
                                    && Math.abs(label.getY() - physYClick) < 3.0) {
                                draggedTextLabel = label;
                                break;
                            }
                        }
                    }
                }
                return; // En mode pointeur, on ne fait rien d'autre au clic
            }

            // --- 2. LOGIQUE DE L'OUTIL TEXTE (Ajout avec taille et rotation) ---
            if (currentTool == Tool.TEXT) {
                if (hoverGridX != null && hoverGridY != null) {
                    javafx.scene.control.Dialog<com.perf2gerber.model.TextLabel> dialog = new javafx.scene.control.Dialog<>();
                    dialog.setTitle("Ajouter un texte");
                    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
                            javafx.scene.control.ButtonType.CANCEL);

                    javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);

                    javafx.scene.control.TextField textF = new javafx.scene.control.TextField("Texte");
                    javafx.scene.control.TextField sizeF = new javafx.scene.control.TextField("2.0");
                    javafx.scene.control.TextField rotF = new javafx.scene.control.TextField("0");

                    grid.add(new javafx.scene.control.Label("Texte:"), 0, 0);
                    grid.add(textF, 1, 0);
                    grid.add(new javafx.scene.control.Label("Taille (mm):"), 0, 1);
                    grid.add(sizeF, 1, 1);
                    grid.add(new javafx.scene.control.Label("Rotation (°):"), 0, 2);
                    grid.add(rotF, 1, 2);
                    dialog.getDialogPane().setContent(grid);
                    javafx.application.Platform.runLater(textF::requestFocus);

                    dialog.setResultConverter(btn -> {
                        if (btn == javafx.scene.control.ButtonType.OK) {
                            try {
                                double physX = hoverGridX * board.getGridSpacing();
                                double physY = hoverGridY * board.getGridSpacing();
                                return new com.perf2gerber.model.TextLabel(textF.getText(), activeLayer, physX, physY,
                                        Double.parseDouble(sizeF.getText()), Double.parseDouble(rotF.getText()));
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        return null;
                    });

                    dialog.showAndWait().ifPresent(label -> {
                        board.addTextLabel(label);
                        saveState();
                        draw();
                    });
                }
                return;
            }

            // --- 3. LOGIQUE D'EFFACEMENT SÉLECTIVE ---
            if (currentTool == Tool.ERASE) {
                double physXClick = (worldX - padding) / zoomLevel;
                if (isViewFlipped)
                    physXClick = (board.getColumns() - 1) * board.getGridSpacing() - physXClick;
                double physYClick = ((board.getRows() - 1) * board.getGridSpacing()) - ((worldY - padding) / zoomLevel);

                com.perf2gerber.model.TextLabel textToRemove = null;
                if (board.getTextLabels() != null) {
                    for (com.perf2gerber.model.TextLabel label : board.getTextLabels()) {
                        if (label.getLayer() == activeLayer) {
                            if (Math.abs(label.getX() - physXClick) < 2.0
                                    && Math.abs(label.getY() - physYClick) < 2.0) {
                                textToRemove = label;
                                break;
                            }
                        }
                    }
                }
                if (textToRemove != null) {
                    board.getTextLabels().remove(textToRemove);
                    saveState();
                    draw();
                    return;
                }

                boolean onActivePad = false;
                if (hoverGridX != null && hoverGridY != null) {
                    Pad p = board.getPad(hoverGridX, hoverGridY);
                    if (p != null && p.isUsed()) {
                        onActivePad = true;
                        p.deactivate();
                        saveState();
                    }
                }

                if (!onActivePad) {
                    eraseSegmentAt(worldX, worldY);
                }
                draw();
                return;
            }

            // --- 4. DESSIN (Pads & Traces) ---
            if (hoverGridX == null || hoverGridY == null)
                return;
            Pad clickedPad = board.getPad(hoverGridX, hoverGridY);
            if (clickedPad != null) {
                boolean isWiring = (currentTool == Tool.WIRE) || (currentTool == Tool.PADS && isCommandPressed);

                if (currentTool == Tool.PADS && !isCommandPressed) {
                    if (!clickedPad.isUsed()) {
                        clickedPad.activate();
                        saveState();
                    }
                }

                if (isWiring) {
                    if (currentTrace == null) {
                        currentTrace = new Trace(activeLayer, currentTraceWidth);
                        currentTrace.addPoint(hoverGridX, hoverGridY);
                        board.addTrace(currentTrace);
                    } else {
                        currentTrace.addPoint(hoverGridX, hoverGridY);

                        // LA MODIFICATION EST ICI :
                        // Si on N'EST PAS en mode continu (bouton cliqué au lieu du clavier),
                        // on termine la trace dès le 2ème clic !
                        if (!isCommandPressed) {
                            endCurrentTrace();
                        }
                    }
                } else if (currentTrace != null) {
                    endCurrentTrace();
                }
                draw();
            }
        });
    }

    private void eraseSegmentAt(double mouseX, double mouseY) {
        double physX = (mouseX - padding) / zoomLevel;
        if (isViewFlipped)
            physX = (board.getColumns() - 1) * board.getGridSpacing() - physX;
        double physY = ((board.getRows() - 1) * board.getGridSpacing()) - ((mouseY - padding) / zoomLevel);

        Trace traceToSplit = null;
        int splitIndex = -1;
        double threshold = 1.2;

        Trace.Layer deletableLayer = activeLayer;

        for (Trace trace : board.getTraces()) {
            if (trace.getLayer() != deletableLayer) {
                continue;
            }
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
            if (traceToSplit != null)
                break;
        }

        if (traceToSplit != null) {
            List<Trace.GridPoint> allPoints = new ArrayList<>(traceToSplit.getSegments());
            List<Trace.GridPoint> partA = new ArrayList<>(allPoints.subList(0, splitIndex + 1));
            List<Trace.GridPoint> partB = new ArrayList<>(allPoints.subList(splitIndex + 1, allPoints.size()));
            board.removeTrace(traceToSplit);
            if (partA.size() >= 2) {
                Trace nA = new Trace(traceToSplit.getLayer(), traceToSplit.getWidth());
                for (Trace.GridPoint p : partA)
                    nA.addPoint(p.x(), p.y());
                board.addTrace(nA);
            }
            if (partB.size() >= 2) {
                Trace nB = new Trace(traceToSplit.getLayer(), traceToSplit.getWidth());
                for (Trace.GridPoint p : partB)
                    nB.addPoint(p.x(), p.y());
                board.addTrace(nB);
            }
            saveState();
        }
    }

    private double distToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0)
            return Math.hypot(px - x1, py - y1);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    public void endCurrentTrace() {
        if (currentTrace != null) {
            if (currentTrace.getSegments().size() < 2) {
                board.removeTrace(currentTrace);
            } else {
                saveState(); // Save state only if trace actually added points
            }
            currentTrace = null;
            draw();
        }
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.save();
        gc.translate(panX, panY);
        gc.scale(scale, scale);

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // 1. Zone de coupe
        gc.setStroke(Color.web("#FFFFFF", 0.5));
        gc.setLineWidth(1.0);
        gc.setLineDashes(5.0);
        gc.strokeRect(padding, padding, physicalToScreen((board.getColumns() - 1) * board.getGridSpacing()),
                physicalToScreen((board.getRows() - 1) * board.getGridSpacing()));
        gc.setLineDashes((double[]) null);

        // 2. Dessiner les Pads EN PREMIER (100% Opaques, aucun filtre alpha)
        for (int x = 0; x < board.getColumns(); x++) {
            for (int y = 0; y < board.getRows(); y++) {
                if (x == 0 || x == board.getColumns() - 1 || y == 0 || y == board.getRows() - 1)
                    continue;
                Pad pad = board.getPad(x, y);
                if (pad != null)
                    drawPad(gc, pad);
            }
        }

        // 3. Dessiner les Traces PAR-DESSUS les pads avec de la transparence
        for (Trace trace : board.getTraces()) {
            boolean isActiveLayer = (trace.getLayer() == activeLayer);
            // L'astuce est ici : 75% d'opacité pour voir les pads à travers la trace !
            double opacity = isActiveLayer ? 0.65 : 0.30;
            gc.setStroke(trace.getLayer() == Trace.Layer.TOP ? Color.web("#E74C3C", opacity)
                    : Color.web("#3498DB", opacity));
            gc.setLineWidth(physicalToScreen(trace.getWidth()));
            List<Trace.GridPoint> segments = trace.getSegments();
            if (segments.size() > 1) {
                gc.beginPath();
                for (int i = 0; i < segments.size(); i++) {
                    double sx = getScreenX(segments.get(i).x());
                    double sy = getScreenY(segments.get(i).y());
                    if (i == 0)
                        gc.moveTo(sx, sy);
                    else
                        gc.lineTo(sx, sy);
                }
                gc.stroke();
            }
        }

        // 4. Trace en cours de dessin (Wire tool)
        boolean isWiring = (currentTool == Tool.WIRE) || (currentTool == Tool.PADS && isCommandPressed);
        if (isWiring && currentTrace != null && hoverGridX != null && hoverGridY != null) {
            List<Trace.GridPoint> segments = currentTrace.getSegments();
            if (!segments.isEmpty()) {
                Trace.GridPoint lastPt = segments.get(segments.size() - 1);
                gc.setLineWidth(physicalToScreen(currentTraceWidth));
                gc.setLineDashes(physicalToScreen(1.5), physicalToScreen(1.5));
                gc.setStroke(activeLayer == Trace.Layer.TOP ? Color.web("#E74C3C", 0.6) : Color.web("#3498DB", 0.6));
                gc.strokeLine(getScreenX(lastPt.x()), getScreenY(lastPt.y()), getScreenX(hoverGridX),
                        getScreenY(hoverGridY));
                gc.setLineDashes((double[]) null);
            }
        }

        // 4.5 Dessin des Composants par-dessus les traces
        if (board.getComponents() != null) {
            for (Component c : board.getComponents()) {
                drawComponent(gc, c);
            }
        }

        // 5. Dessin des Textes TOUT AU-DESSUS de tout le reste
        if (board.getTextLabels() != null) {
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);

            for (com.perf2gerber.model.TextLabel label : board.getTextLabels()) {
                boolean isActiveLayer = (label.getLayer() == activeLayer);
                gc.setGlobalAlpha(isActiveLayer ? 1.0 : 0.2);
                gc.setFill(Color.WHITE);

                double sx = getScreenXFromPhys(label.getX());
                double sy = getScreenYFromPhys(label.getY());

                // On applique la taille dynamique
                gc.setFont(new javafx.scene.text.Font("Monospaced", physicalToScreen(label.getFontSize())));

                gc.save(); // On sauvegarde l'état du pinceau
                gc.translate(sx, sy); // On se déplace au centre du texte

                // Effet Miroir ultra pro pour le Bottom Layer !
                if (label.getLayer() == Trace.Layer.BOTTOM && !isViewFlipped) {
                    gc.scale(-1, 1);
                } else if (label.getLayer() == Trace.Layer.TOP && isViewFlipped) {
                    gc.scale(-1, 1);
                }

                gc.rotate(label.getRotation()); // On tourne
                gc.fillText(label.getText(), 0, 0); // On dessine
                gc.restore(); // On remet le pinceau normal
            }
            gc.setGlobalAlpha(1.0);
        }

        // 6. HOVER RING INTELLIGENT
        if (hoverGridX != null && hoverGridY != null) {
            Pad p = board.getPad(hoverGridX, hoverGridY);
            if (p != null) {
                boolean showRing = true;
                if (currentTool == Tool.ERASE && !p.isUsed())
                    showRing = false;

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

        gc.restore();
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

    private void drawComponent(GraphicsContext gc, Component c) {
        if (c instanceof FixedComponent) {
            FixedComponent fc = (FixedComponent) c;
            double sy = getScreenY(fc.getStartY());

            // 3 cells wide = 3 * gridSpacing
            double rectW = physicalToScreen(board.getGridSpacing() * 3);
            double rectH = physicalToScreen(board.getGridSpacing() * 1.5);
            double centerX = getScreenX(fc.getStartX() + 1); // Center is on the middle pad (Pad 2)
            double centerY = sy;

            gc.setFill(Color.web("#333333", 0.8));
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.fillRect(centerX - rectW / 2, centerY - rectH / 2, rectW, rectH);
            gc.strokeRect(centerX - rectW / 2, centerY - rectH / 2, rectW, rectH);

            gc.setFill(Color.WHITE);
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            gc.setFont(new javafx.scene.text.Font("Monospaced", physicalToScreen(1.5)));

            if (fc.isShowValue() && fc.getValue() != null) {
                gc.fillText(fc.getValue(), centerX, centerY);
            } else if (fc.isShowName() && fc.getName() != null) {
                gc.fillText(fc.getName(), centerX, centerY);
            }

            if (fc.getPinoutOrCount() != null) {
                String pinout = fc.getPinoutOrCount();
                for (int i = 0; i < pinout.length(); i++) {
                    String letter = String.valueOf(pinout.charAt(i));
                    double padX = getScreenX(fc.getStartX() + i);
                    gc.fillText(letter, padX, sy);
                }
            }
        } else if (c instanceof StretchComponent) {
            StretchComponent sc = (StretchComponent) c;
            double x1 = getScreenX(sc.getStartX());
            double y1 = getScreenY(sc.getStartY());
            double x2 = getScreenX(sc.getEndX());
            double y2 = getScreenY(sc.getEndY());

            double midX = (x1 + x2) / 2.0;
            double midY = (y1 + y2) / 2.0;

            gc.setStroke(Color.web("#CCCCCC"));
            gc.setLineWidth(physicalToScreen(0.5));
            gc.strokeLine(x1, y1, x2, y2); // Leads

            double bodyW = physicalToScreen(board.getGridSpacing() * 1.5);
            double bodyH = physicalToScreen(board.getGridSpacing() * 0.8);

            gc.save();
            gc.translate(midX, midY);
            double angle = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            gc.rotate(angle);

            gc.setFill(Color.web("#8B4513", 0.9)); // Brown resistor body
            gc.fillRect(-bodyW / 2, -bodyH / 2, bodyW, bodyH);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.strokeRect(-bodyW / 2, -bodyH / 2, bodyW, bodyH);

            gc.setFill(Color.WHITE);
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
            // On utilise une police légèrement plus petite pour que ça rentre bien dans une
            // résistance !
            gc.setFont(new javafx.scene.text.Font("Monospaced", physicalToScreen(1.0)));

            if (sc.isShowValue() && sc.getValue() != null) {
                gc.fillText(sc.getValue(), 0, 0);
            } else if (sc.isShowName() && sc.getName() != null) {
                gc.fillText(sc.getName(), 0, 0);
            }

            gc.restore();
        }
    }

    public void setBoard(Board newBoard) {
        this.board = newBoard;
        updateSize();
        draw();
        undoStack.clear();
        redoStack.clear();
        saveState(); // Save initial loaded state
    }
}