package com.perf2gerber.exporter;

import com.perf2gerber.model.Board;
import com.perf2gerber.model.Component;
import com.perf2gerber.model.FixedComponent;
import com.perf2gerber.model.StretchComponent;
import com.perf2gerber.model.Pad;
import com.perf2gerber.model.Trace;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Gère l'exportation du modèle Board vers les standards industriels Gerber RS-274X et Excellon.
 */
public class GerberExporter {

    private static final double SCALE = 10000.0;

    // --- LE CORRECTIF MAGIQUE ---
    // On décale toute la carte de 20mm.
    // Empêche la fraiseuse de l'usine de calculer des coordonnées négatives et de planter.
    private static final double OFFSET_MM = 20.0;

    // Helper pour convertir les millimètres en unités Gerber avec décalage
    private static int toGerber(double mm) {
        return (int) Math.round((mm + OFFSET_MM) * SCALE);
    }

    // Helper pour les fichiers de perçage avec décalage
    private static double toDrill(double mm) {
        return mm + OFFSET_MM;
    }

    public static void exportEdgeCuts(Board board, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("G04 Fichier de contour (Edge Cuts) genere par Perf2Gerber*\n");
            writer.write("%TF.GenerationSoftware,Perf2Gerber*%\n");
            writer.write("%FSLAX44Y44*%\n");
            writer.write("%MOMM*%\n");
            writer.write("%LPD*%\n");

            // JLCPCB préfère un trait très fin (0.1mm) pour la ligne de découpe
            writer.write("%ADD10C,0.1000*%\n");
            writer.write("D10*\n");

            double widthMm = (board.getColumns() - 1) * board.getGridSpacing();
            double heightMm = (board.getRows() - 1) * board.getGridSpacing();

            // On trace le rectangle avec l'offset intégré
            writer.write(formatCoord(toGerber(0), toGerber(0)) + "D02*\n");
            writer.write(formatCoord(toGerber(widthMm), toGerber(0)) + "D01*\n");
            writer.write(formatCoord(toGerber(widthMm), toGerber(heightMm)) + "D01*\n");
            writer.write(formatCoord(toGerber(0), toGerber(heightMm)) + "D01*\n");
            writer.write(formatCoord(toGerber(0), toGerber(0)) + "D01*\n");

            writer.write("M02*\n");
        }
    }

    public static void exportDrillFile(Board board, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("M48\n");
            writer.write("METRIC\n"); // En-tête simplifié pour éviter les bugs de lecture

            double holeDiameter = 1.0;
            if (board.getColumns() > 1 && board.getRows() > 1) {
                Pad samplePad = board.getPad(1, 1);
                if (samplePad != null) holeDiameter = samplePad.getHoleDiameter();
            }

            writer.write(String.format(Locale.US, "T1C%.3f\n", holeDiameter));
            writer.write("%\n");
            writer.write("G05\n");
            writer.write("T1\n");

            int holesDrilled = 0;
            for (int x = 0; x < board.getColumns(); x++) {
                for (int y = 0; y < board.getRows(); y++) {
                    Pad pad = board.getPad(x, y);
                    if (pad != null && pad.isUsed()) {
                        double physX = x * board.getGridSpacing();
                        double physY = y * board.getGridSpacing();
                        // Coordonnées avec offset
                        writer.write(String.format(Locale.US, "X%.3fY%.3f\n", toDrill(physX), toDrill(physY)));
                        holesDrilled++;
                    }
                }
            }

            if (holesDrilled == 0) {
                writer.write(String.format(Locale.US, "X%.3fY%.3f\n", toDrill(0), toDrill(0)));
            }
            writer.write("M30\n");
        }
    }

    public static void exportCopperLayer(Board board, Trace.Layer layer, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("G04 Fichier de Cuivre genere par Perf2Gerber*\n");
            writer.write("%TF.GenerationSoftware,Perf2Gerber*%\n");
            writer.write("%FSLAX44Y44*%\n");
            writer.write("%MOMM*%\n");
            writer.write("%LPD*%\n");

            double padDia = 2.0;
            if (board.getPad(1,1) != null) padDia = board.getPad(1,1).getCopperDiameter();
            writer.write(String.format(Locale.US, "%%ADD10C,%.4f*%%\n", padDia));

            java.util.Map<Double, Integer> traceTools = new java.util.HashMap<>();
            int dCodeCounter = 11;
            for (Trace t : board.getTraces()) {
                if (t.getLayer() == layer && !traceTools.containsKey(t.getWidth())) {
                    traceTools.put(t.getWidth(), dCodeCounter);
                    writer.write(String.format(Locale.US, "%%ADD%dC,%.4f*%%\n", dCodeCounter, t.getWidth()));
                    dCodeCounter++;
                }
            }

            writer.write("D10*\n");
            for (int x = 0; x < board.getColumns(); x++) {
                for (int y = 0; y < board.getRows(); y++) {
                    Pad pad = board.getPad(x, y);
                    if (pad != null && pad.isUsed()) {
                        double physX = x * board.getGridSpacing();
                        double physY = y * board.getGridSpacing();
                        // Coordonnées avec offset
                        writer.write(formatCoord(toGerber(physX), toGerber(physY)) + "D03*\n");
                    }
                }
            }

            for (Trace t : board.getTraces()) {
                if (t.getLayer() == layer) {
                    int toolCode = traceTools.get(t.getWidth());
                    writer.write("D" + toolCode + "*\n");

                    java.util.List<Trace.GridPoint> pts = t.getSegments();
                    if (pts.size() > 1) {
                        for (int i = 0; i < pts.size(); i++) {
                            double physX = pts.get(i).x() * board.getGridSpacing();
                            double physY = pts.get(i).y() * board.getGridSpacing();

                            // Coordonnées avec offset
                            if (i == 0) writer.write(formatCoord(toGerber(physX), toGerber(physY)) + "D02*\n");
                            else writer.write(formatCoord(toGerber(physX), toGerber(physY)) + "D01*\n");
                        }
                    }
                }
            }
            writer.write("M02*\n");
        }
    }
    /**
     * Génère le fichier de Solder Mask (Vernis épargne).
     * Indique à l'usine de NE PAS mettre de vernis vert sur nos pastilles pour qu'on puisse les souder.
     */
    public static void exportSolderMask(Board board, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("G04 Fichier Solder Mask genere par Perf2Gerber*\n");
            writer.write("%TF.GenerationSoftware,Perf2Gerber*%\n");
            writer.write("%FSLAX44Y44*%\n");
            writer.write("%MOMM*%\n");
            writer.write("%LPD*%\n");

            // On fait le "trou" dans le vernis vert 0.1mm plus grand que la pastille de cuivre
            // C'est ce qu'on appelle la "Solder Mask Clearance" standard
            double maskDia = 2.0 + 0.1;
            if (board.getPad(1,1) != null) maskDia = board.getPad(1,1).getCopperDiameter() + 0.1;
            writer.write(String.format(Locale.US, "%%ADD10C,%.4f*%%\n", maskDia));

            writer.write("D10*\n");
            for (int x = 0; x < board.getColumns(); x++) {
                for (int y = 0; y < board.getRows(); y++) {
                    Pad pad = board.getPad(x, y);
                    if (pad != null && pad.isUsed()) {
                        double physX = x * board.getGridSpacing();
                        double physY = y * board.getGridSpacing();
                        // On tamponne l'ouverture dans le masque
                        writer.write(formatCoord(toGerber(physX), toGerber(physY)) + "D03*\n");
                    }
                }
            }
            writer.write("M02*\n");
        }
    }

    private static String formatCoord(int x, int y) {
        return String.format(Locale.US, "X%dY%d", x, y);
    }
    /**
     * Génère la couche Silkscreen (Sérigraphie) en convertissant le texte et les composants en tracés vectoriels.
     */
    public static void exportSilkscreenLayer(Board board, Trace.Layer layer, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("G04 Fichier Silkscreen genere par Perf2Gerber*\n");
            writer.write("%TF.GenerationSoftware,Perf2Gerber*%\n");
            writer.write("%FSLAX44Y44*%\n");
            writer.write("%MOMM*%\n");
            writer.write("%LPD*%\n");

            // Épaisseur du pinceau pour dessiner le texte (0.15mm est standard chez JLCPCB)
            writer.write("%ADD10C,0.1500*%\n");
            writer.write("D10*\n");

            // --- 1. TextLabels (unchanged logic) ---
            if (board.getTextLabels() != null) {
                java.awt.Font baseFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10);
                java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);

                for (com.perf2gerber.model.TextLabel label : board.getTextLabels()) {
                    if (label.getLayer() == layer) {
                        java.awt.Font scaledFont = baseFont.deriveFont((float) label.getFontSize());
                        java.awt.font.GlyphVector gv = scaledFont.createGlyphVector(frc, label.getText());

                        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();
                        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();

                        at.translate(label.getX(), label.getY());
                        if (layer == Trace.Layer.BOTTOM) {
                            at.scale(-1, -1);
                        } else {
                            at.scale(1, -1);
                        }
                        at.rotate(Math.toRadians(label.getRotation()));
                        at.translate(-bounds.getCenterX(), -bounds.getCenterY());

                        java.awt.Shape shape = at.createTransformedShape(gv.getOutline());
                        writeShape(writer, shape);
                    }
                }
            }

            // --- 2. Component footprints (TOP layer only for through-hole) ---
            if (layer == Trace.Layer.TOP && board.getComponents() != null) {
                double gs = board.getGridSpacing(); // grid spacing in mm

                for (Component c : board.getComponents()) {
                    if (c instanceof StretchComponent) {
                        exportStretchComponentSilkscreen(writer, (StretchComponent) c, gs);
                    } else if (c instanceof FixedComponent) {
                        exportFixedComponentSilkscreen(writer, (FixedComponent) c, gs);
                    }
                }
            }

            writer.write("M02*\n");
        }
    }

    /**
     * Writes a Java2D Shape to the Gerber file using D02 (move) and D01 (draw) commands.
     */
    private static void writeShape(FileWriter writer, java.awt.Shape shape) throws IOException {
        java.awt.geom.PathIterator pi = shape.getPathIterator(null, 0.05);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            int gx = toGerber(coords[0]);
            int gy = toGerber(coords[1]);

            if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                writer.write(formatCoord(gx, gy) + "D02*\n");
            } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                writer.write(formatCoord(gx, gy) + "D01*\n");
            }
            pi.next();
        }
    }

    /**
     * Writes a Java2D Shape as a SOLID FILLED region using G36/G37 Gerber region fill.
     * Used for elements like the diode cathode band that need to appear filled, not just outlined.
     */
    private static void writeSolidFill(FileWriter writer, java.awt.Shape shape) throws IOException {
        writer.write("G36*\n"); // Begin region fill
        java.awt.geom.PathIterator pi = shape.getPathIterator(null, 0.05);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            int gx = toGerber(coords[0]);
            int gy = toGerber(coords[1]);

            if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                writer.write(formatCoord(gx, gy) + "D02*\n");
            } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                writer.write(formatCoord(gx, gy) + "D01*\n");
            }
            pi.next();
        }
        writer.write("G37*\n"); // End region fill
    }


    /**
     * Exports a StretchComponent (Resistor, Capacitor, Diode, LED, etc.) footprint to silkscreen.
     */
    private static void exportStretchComponentSilkscreen(FileWriter writer, StretchComponent sc, double gs) throws IOException {
        double x1mm = sc.getStartX() * gs;
        double y1mm = sc.getStartY() * gs;
        double x2mm = sc.getEndX() * gs;
        double y2mm = sc.getEndY() * gs;

        double midX = (x1mm + x2mm) / 2.0;
        double midY = (y1mm + y2mm) / 2.0;
        double angle = Math.atan2(y2mm - y1mm, x2mm - x1mm);
        double dist = Math.hypot(x2mm - x1mm, y2mm - y1mm);

        boolean isCapacitor = "Capacitor".equals(sc.getType());
        boolean isElectroCap = "Capacitor (Polarized)".equals(sc.getType());
        boolean isDiode = "Diode".equals(sc.getType());
        boolean isLED = "LED".equals(sc.getType());

        // --- Determine body half-width (same ratios as EditorCanvas) ---
        double halfW;
        if (isCapacitor) {
            halfW = Math.max(gs * 0.6, dist - gs * 0.4) / 2.0;
        } else if (isElectroCap || isLED) {
            halfW = gs * 1.6 / 2.0;
        } else if (isDiode) {
            halfW = gs * 1.5 / 2.0;
        } else {
            // Resistor
            halfW = gs * 1.5 / 2.0;
        }

        // Transform helper: rotate a local-space point by angle around midpoint
        // Local space: X along component axis, Y perpendicular
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(midX, midY);
        at.rotate(angle);

        // --- Draw leads (lines from pin to body edge) ---
        java.awt.geom.Line2D leadLeft = new java.awt.geom.Line2D.Double(-dist / 2.0, 0, -halfW, 0);
        java.awt.geom.Line2D leadRight = new java.awt.geom.Line2D.Double(halfW, 0, dist / 2.0, 0);
        writeShape(writer, at.createTransformedShape(leadLeft));
        writeShape(writer, at.createTransformedShape(leadRight));

        // --- Draw body outline ---
        if (isCapacitor) {
            double capW = halfW * 2.0;
            double capH = gs * 0.9;
            java.awt.geom.Ellipse2D body = new java.awt.geom.Ellipse2D.Double(-capW / 2, -capH / 2, capW, capH);
            writeShape(writer, at.createTransformedShape(body));

        } else if (isElectroCap) {
            double capR = gs * 1.6 / 2.0;
            // Full circle outline
            java.awt.geom.Ellipse2D circle = new java.awt.geom.Ellipse2D.Double(-capR, -capR, capR * 2, capR * 2);
            writeShape(writer, at.createTransformedShape(circle));
            // Dividing line (polarity separator)
            java.awt.geom.Line2D divLine = new java.awt.geom.Line2D.Double(0, -capR, 0, capR);
            writeShape(writer, at.createTransformedShape(divLine));
            // "+" symbol OUTSIDE the circle, rotated 45° from axis so it avoids the pad
            double plusDist = capR + gs * 0.35;
            double plusAngle = Math.toRadians(45); // 45° offset from the lead axis
            double plusLocalX = -plusDist * Math.cos(plusAngle);
            double plusLocalY = -plusDist * Math.sin(plusAngle);
            writeTextAtPosition(writer, "+", midX, midY, angle, plusLocalX, plusLocalY, gs * 0.5);

        } else if (isDiode) {
            double bodyW = gs * 1.5;
            double bodyH = gs * 0.8;
            java.awt.geom.Rectangle2D body = new java.awt.geom.Rectangle2D.Double(-bodyW / 2, -bodyH / 2, bodyW, bodyH);
            writeShape(writer, at.createTransformedShape(body));
            // Cathode band — rendered as SOLID FILL using G36/G37 polygon
            double bandW = bodyW * 0.2;
            double bandX = bodyW / 2 - bandW - gs * 0.1;
            java.awt.geom.Rectangle2D band = new java.awt.geom.Rectangle2D.Double(bandX, -bodyH / 2, bandW, bodyH);
            java.awt.Shape transformedBand = at.createTransformedShape(band);
            writeSolidFill(writer, transformedBand);

        } else if (isLED) {
            double capR = gs * 1.6 / 2.0;
            // Full circle outline
            java.awt.geom.Ellipse2D circle = new java.awt.geom.Ellipse2D.Double(-capR, -capR, capR * 2, capR * 2);
            writeShape(writer, at.createTransformedShape(circle));
            // Cathode flat chord — a line across the circle at the flat-spot position
            double flatX = capR * 0.75;
            // Calculate chord Y extent at flatX: y = sqrt(r^2 - x^2)
            double chordY = Math.sqrt(capR * capR - flatX * flatX);
            java.awt.geom.Line2D chordLine = new java.awt.geom.Line2D.Double(flatX, -chordY, flatX, chordY);
            writeShape(writer, at.createTransformedShape(chordLine));
            // "+" on anode side (left), well clear of center
            writeTextAtPosition(writer, "+", midX, midY, angle, -capR * 0.55, 0, gs * 0.5);

        } else {
            // Resistor body outline
            double bodyW = gs * 1.5;
            double bodyH = gs * 0.8;
            java.awt.geom.Rectangle2D body = new java.awt.geom.Rectangle2D.Double(-bodyW / 2, -bodyH / 2, bodyW, bodyH);
            writeShape(writer, at.createTransformedShape(body));
        }

        // --- Draw component text (name or value) ---
        String textToDraw = null;
        if (sc.isShowValue() && sc.getValue() != null) {
            textToDraw = sc.getValue();
        } else if (sc.isShowName() && sc.getName() != null) {
            textToDraw = sc.getName();
        }

        if (textToDraw != null) {
            writeTextAtPosition(writer, textToDraw, midX, midY, angle, 0, 0, 1.0);
        }
    }

    /**
     * Exports a FixedComponent (Transistor, IC) footprint to silkscreen.
     */
    private static void exportFixedComponentSilkscreen(FileWriter writer, FixedComponent fc, double gs) throws IOException {
        double anchorX = fc.getStartX() * gs;
        double anchorY = fc.getStartY() * gs;

        boolean isTransistor = "Transistor".equals(fc.getType()) || (fc.getName() != null && fc.getName().startsWith("Q") && fc.getType() == null);

        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(anchorX, anchorY);
        // EditorCanvas uses screen-space Y (down), Gerber uses physical Y (up).
        // The rotation in EditorCanvas is clockwise screen-space (JavaFX).
        // In physical mm space, we mirror Y so clockwise screen = counter-clockwise physical.
        at.scale(1, -1);
        at.rotate(Math.toRadians(fc.getRotation()));

        if (isTransistor) {
            // Body: rect with clearance padding around the 3-pin row
            java.awt.geom.Rectangle2D body = new java.awt.geom.Rectangle2D.Double(
                    -gs * 1.6, -gs * 0.6, gs * 3.2, gs * 1.2);
            writeShape(writer, at.createTransformedShape(body));

            // Pinout labels (e.g. "EBC") — placed ABOVE the body rectangle, smaller font
            String pinout = fc.getPinoutOrCount();
            if (pinout != null) {
                for (int i = 0; i < pinout.length(); i++) {
                    writeTextAtTransform(writer, String.valueOf(pinout.charAt(i)), at, gs * (i - 1), -gs * 0.9, 1.0);
                }
            }

            String textToDraw = null;
            if (fc.isShowValue() && fc.getValue() != null) {
                textToDraw = fc.getValue();
            } else if (fc.isShowName() && fc.getName() != null) {
                textToDraw = fc.getName();
            }
            if (textToDraw != null) {
                // Name/value placed above the body, smaller font
                writeTextAtTransform(writer, textToDraw, at, 0, -gs * 1.2, 1.0);
            }

        } else {
            // IC logic
            int pinsPerSide = 4;
            try {
                if (fc.getPinoutOrCount() != null) {
                    pinsPerSide = Integer.parseInt(fc.getPinoutOrCount()) / 2;
                }
            } catch (Exception e) {}

            double bodyW = gs * 4;
            double bodyH = gs * pinsPerSide;
            double rectX = -gs * 3.5;
            double rectY = -gs * 0.5;

            // Body outline
            java.awt.geom.Rectangle2D body = new java.awt.geom.Rectangle2D.Double(rectX, rectY, bodyW, bodyH);
            writeShape(writer, at.createTransformedShape(body));

            // Pin 1 notch (semicircle arc at center-top)
            java.awt.geom.Arc2D notch = new java.awt.geom.Arc2D.Double(
                    -gs * 2.0, rectY - gs * 0.5, gs, gs, 180, 180, java.awt.geom.Arc2D.OPEN);
            writeShape(writer, at.createTransformedShape(notch));

            // IC text (rotated 90° inside the body, same as EditorCanvas)
            String textToDraw = null;
            if (fc.isShowValue() && fc.getValue() != null) {
                textToDraw = fc.getValue();
            } else if (fc.isShowName() && fc.getName() != null) {
                textToDraw = fc.getName();
            }
            if (textToDraw != null) {
                // Text is at center of IC body, rotated 90° clockwise
                double textCX = rectX + bodyW / 2;
                double textCY = bodyH / 2 - gs * 0.5;
                java.awt.geom.AffineTransform textAt = new java.awt.geom.AffineTransform(at);
                textAt.translate(textCX, textCY);
                textAt.rotate(Math.toRadians(90));
                writeTextAtOrigin(writer, textToDraw, textAt, 1.5);
            }
        }
    }

    /**
     * Writes vectorized text at a specific position, rotated along the component axis.
     * Used for StretchComponents where local space is centered at the midpoint.
     */
    private static void writeTextAtPosition(FileWriter writer, String text, double centerXmm, double centerYmm,
                                            double angleRad, double localOffX, double localOffY, double fontSize) throws IOException {
        java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10).deriveFont((float) fontSize);
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);
        java.awt.font.GlyphVector gv = font.createGlyphVector(frc, text);
        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();

        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(centerXmm, centerYmm);
        at.rotate(angleRad);
        at.translate(localOffX, localOffY);
        at.scale(1, -1); // Gerber Y inversion
        at.translate(-bounds.getCenterX(), -bounds.getCenterY());

        writeShape(writer, at.createTransformedShape(gv.getOutline()));
    }

    /**
     * Writes vectorized text at a local offset within an existing AffineTransform.
     * Used for FixedComponents (Transistor/IC).
     */
    private static void writeTextAtTransform(FileWriter writer, String text, java.awt.geom.AffineTransform parentAt,
                                              double localX, double localY, double fontSize) throws IOException {
        java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10).deriveFont((float) fontSize);
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);
        java.awt.font.GlyphVector gv = font.createGlyphVector(frc, text);
        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();

        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform(parentAt);
        at.translate(localX, localY);
        at.translate(-bounds.getCenterX(), -bounds.getCenterY());

        writeShape(writer, at.createTransformedShape(gv.getOutline()));
    }

    /**
     * Writes vectorized text at the origin of the given transform (already pre-positioned and rotated).
     */
    private static void writeTextAtOrigin(FileWriter writer, String text, java.awt.geom.AffineTransform at, double fontSize) throws IOException {
        java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10).deriveFont((float) fontSize);
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);
        java.awt.font.GlyphVector gv = font.createGlyphVector(frc, text);
        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();

        java.awt.geom.AffineTransform textAt = new java.awt.geom.AffineTransform(at);
        textAt.translate(-bounds.getCenterX(), -bounds.getCenterY());

        writeShape(writer, textAt.createTransformedShape(gv.getOutline()));
    }

}