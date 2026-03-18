package com.perf2gerber.exporter;

import com.perf2gerber.model.Board;
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
     * Génère la couche Silkscreen (Sérigraphie) en convertissant le texte en tracés vectoriels.
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

            if (board.getTextLabels() != null) {
                // Utilisation de AWT pour extraire la géométrie vectorielle du texte
                java.awt.Font baseFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10);
                java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, false, false);

                for (com.perf2gerber.model.TextLabel label : board.getTextLabels()) {
                    if (label.getLayer() == layer) {
                        // NOUVEAU : On utilise la VRAIE taille du texte !
                        java.awt.Font scaledFont = baseFont.deriveFont((float) label.getFontSize());
                        java.awt.font.GlyphVector gv = scaledFont.createGlyphVector(frc, label.getText());

                        java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();
                        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();

                        // Centrage, miroir et ROTATION
                        at.translate(toGerber(label.getX()) / SCALE, toGerber(label.getY()) / SCALE);
                        if (layer == Trace.Layer.BOTTOM) {
                            at.scale(-1, -1); // Miroir pour la face arrière
                        } else {
                            at.scale(1, -1);  // Inversion Y standard Gerber
                        }
                        at.rotate(Math.toRadians(label.getRotation()));
                        at.translate(-bounds.getCenterX(), -bounds.getCenterY());

                        java.awt.Shape shape = at.createTransformedShape(gv.getOutline());
                        // On aplatit les courbes en lignes droites (précision 0.05mm)
                        java.awt.geom.PathIterator pi = shape.getPathIterator(null, 0.05);

                        double[] coords = new double[6];
                        while (!pi.isDone()) {
                            int type = pi.currentSegment(coords);
                            int gx = toGerber(coords[0]);
                            int gy = toGerber(coords[1]);

                            if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                                writer.write(formatCoord(gx, gy) + "D02*\n"); // Lève le pinceau
                            } else if (type == java.awt.geom.PathIterator.SEG_LINETO) {
                                writer.write(formatCoord(gx, gy) + "D01*\n"); // Trace
                            }
                            pi.next();
                        }
                    }
                }
            }
            writer.write("M02*\n");
        }
    }

}