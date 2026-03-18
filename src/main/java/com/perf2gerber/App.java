package com.perf2gerber;

import com.perf2gerber.model.Board;
import com.perf2gerber.model.Trace;
import com.perf2gerber.ui.EditorCanvas;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Main application class for Perf2Gerber.
 */
public class App extends Application {

    private Board board;
    private EditorCanvas canvas;

    private ToggleButton btnPads;
    private ToggleButton btnWire;
    private ToggleButton btnText;
    private final String drawTextNormal = " Pads (Hold Shift to wire)";
    private final String drawTextContinuous = " Continuous ";

    private Label lblSize;
    private Label lblPhysical;
    private Label lblCoords;

    private java.io.File currentFile = null;

    @Override
    public void start(Stage primaryStage) {
        this.board = showWelcomeScreen();
        if (this.board == null) {
            Platform.exit(); // Si on ferme la fenêtre ou qu'on fait "Exit", on quitte.
            return;
        }

        canvas = new EditorCanvas(this.board);
        canvas.setOnBoardReplaced(newBoard -> {
            this.board = newBoard;
            updateStatusBar();
        });

        MenuBar menuBar = buildMenuBar();
        HBox toolbar = buildToolbar();
        HBox statusBar = buildStatusBar();

        VBox topContainer = new VBox(menuBar, toolbar);

        BorderPane resizeWrapper = new BorderPane();
        resizeWrapper.setStyle("-fx-background-color: #1E1E1E;");

        // Resize buttons
        resizeWrapper.setTop(new StackPane(new HBox(5, createResizeBtn("+", 0, 0, 0, 1), createResizeBtn("-", 0, 0, 0, -1))));
        resizeWrapper.setBottom(new StackPane(new HBox(5, createResizeBtn("+", 0, 0, 1, 0), createResizeBtn("-", 0, 0, -1, 0))));
        resizeWrapper.setLeft(new StackPane(new VBox(5, createResizeBtn("+", 1, 0, 0, 0), createResizeBtn("-", -1, 0, 0, 0))));
        resizeWrapper.setRight(new StackPane(new VBox(5, createResizeBtn("+", 0, 1, 0, 0), createResizeBtn("-", 0, -1, 0, 0))));

        ((HBox)((StackPane)resizeWrapper.getTop()).getChildren().get(0)).setAlignment(Pos.CENTER);
        ((HBox)((StackPane)resizeWrapper.getBottom()).getChildren().get(0)).setAlignment(Pos.CENTER);
        ((VBox)((StackPane)resizeWrapper.getLeft()).getChildren().get(0)).setAlignment(Pos.CENTER);
        ((VBox)((StackPane)resizeWrapper.getRight()).getChildren().get(0)).setAlignment(Pos.CENTER);

        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setStyle("-fx-background-color: #1E1E1E;");

        ScrollPane scrollPane = new ScrollPane(canvasContainer);
        scrollPane.setStyle("-fx-background: #1E1E1E; -fx-border-color: transparent;");
        canvasContainer.minWidthProperty().bind(scrollPane.widthProperty());
        canvasContainer.minHeightProperty().bind(scrollPane.heightProperty());

        resizeWrapper.setCenter(scrollPane);

        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(resizeWrapper);
        root.setBottom(statusBar);

        canvas.setOnCursorMoved((x, y) -> {
            if (x == null || y == null) lblCoords.setText("Pos: - , -");
            else lblCoords.setText(String.format("Pos: X=%d , Y=%d", x, y));
        });

        updateStatusBar();

        Scene scene = new Scene(root, 1200, 800);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SHIFT) {
                canvas.setCommandPressed(true);
                if (btnPads.isSelected()) {
                    btnPads.setText(" Wire ");
                    btnPads.setStyle("-fx-background-color: #5C8A5C; -fx-text-fill: white; -fx-font-weight: bold;");
                } else if (btnWire.isSelected()) {
                    btnWire.setText(drawTextContinuous);
                    btnWire.setStyle("-fx-background-color: #5C8A5C; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                canvas.endCurrentTrace();
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SHIFT) {
                canvas.setCommandPressed(false);
                canvas.endCurrentTrace();
                if (btnPads.isSelected()) {
                    btnPads.setText(drawTextNormal);
                    btnPads.setStyle("");
                } else if (btnWire.isSelected()) {
                    btnWire.setText(" Wire");
                    btnWire.setStyle("");
                }
            }
        });

        primaryStage.setTitle("Perf2Gerber - Alpha");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");

        MenuItem itemNew = new MenuItem("New Project...");
        MenuItem itemOpen = new MenuItem("Open Project...");
        MenuItem itemSave = new MenuItem("Save");
        itemSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        MenuItem itemSaveAs = new MenuItem("Save As...");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem itemExport = new MenuItem("Export to Gerber...");

        itemNew.setOnAction(e -> startNewProject());
        itemOpen.setOnAction(e -> openProject());
        itemSave.setOnAction(e -> saveProject());
        itemSaveAs.setOnAction(e -> saveProjectAs());
        itemExport.setOnAction(e -> exportToGerber());

        menuFile.getItems().addAll(itemNew, itemOpen, itemSave, itemSaveAs, sep, itemExport);
        
        Menu menuEdit = new Menu("Edit");
        MenuItem itemUndo = new MenuItem("Undo");
        itemUndo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        itemUndo.setOnAction(e -> canvas.undo());
        
        MenuItem itemRedo = new MenuItem("Redo");
        itemRedo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        itemRedo.setOnAction(e -> canvas.redo());
        
        menuEdit.getItems().addAll(itemUndo, itemRedo);
        
        menuBar.getMenus().addAll(menuFile, menuEdit);
        return menuBar;
    }

    private HBox buildStatusBar() {
        HBox statusBar = new HBox(30);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: #222222; -fx-border-color: #444; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        lblSize = new Label();
        lblSize.setTextFill(Color.LIGHTGRAY);
        lblPhysical = new Label();
        lblPhysical.setTextFill(Color.LIGHTGRAY);
        lblCoords = new Label("Pos: - , -");
        lblCoords.setTextFill(Color.web("#D4AF37"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(lblSize, lblPhysical, spacer, lblCoords);
        return statusBar;
    }

    private void updateStatusBar() {
        int usefulCols = board.getColumns() - 2;
        int usefulRows = board.getRows() - 2;
        lblSize.setText(String.format("Useful Size: %d x %d holes", usefulCols, usefulRows));

        double widthMm = (board.getColumns() - 1) * board.getGridSpacing();
        double heightMm = (board.getRows() - 1) * board.getGridSpacing();
        lblPhysical.setText(String.format("Physical Size: %.2f mm x %.2f mm", widthMm, heightMm));
    }

    private Button createResizeBtn(String text, int left, int right, int bottom, int top) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #3C3F41; -fx-text-fill: white; -fx-font-weight: bold;");
        btn.setOnAction(e -> {
            board.resizeBoard(left, right, bottom, top);
            canvas.updateSize();
            updateStatusBar();
            canvas.saveState();
        });
        return btn;
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #2B2B2B;");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        btnPads = new ToggleButton(drawTextNormal);
        btnPads.setSelected(true);
        btnWire = new ToggleButton(" Wire");
        ToggleButton btnErase = new ToggleButton(" Erase");
        btnText = new ToggleButton(" Text");

        ToggleGroup toolGroup = new ToggleGroup();
        btnPads.setToggleGroup(toolGroup);
        btnWire.setToggleGroup(toolGroup);
        btnErase.setToggleGroup(toolGroup);
        btnText.setToggleGroup(toolGroup);

        btnPads.setOnAction(e -> {
            canvas.setTool(EditorCanvas.Tool.PADS);
            btnPads.setText(drawTextNormal);
            btnWire.setText(" Wire");
            btnErase.setText(" Erase");
        });
        btnWire.setOnAction(e -> {
            canvas.setTool(EditorCanvas.Tool.WIRE);
            btnWire.setText(" Wire");
            btnPads.setText(" Pads (Hold Shift to wire)");
            btnErase.setText(" Erase");
        });
        btnErase.setOnAction(e -> {
            canvas.setTool(EditorCanvas.Tool.ERASE);
            btnPads.setText(" Pads (Hold Shift to wire)");
            btnWire.setText(" Wire");
        });
        btnText.setOnAction(e -> {
                    canvas.setTool(EditorCanvas.Tool.TEXT);
                    btnPads.setText(" Pads (Hold Shift to wire)");
                    btnWire.setText(" Wire");
                    btnErase.setText(" Erase");
        });


        Label lblView = new Label("View:");
        lblView.setTextFill(Color.WHITE);

        Label lblLayer = new Label(" Draw on:");
        lblLayer.setTextFill(Color.WHITE);

        Label lblWidth = new Label(" Trace:");
        lblWidth.setTextFill(Color.WHITE);

        ComboBox<String> viewBox = new ComboBox<>();
        viewBox.getItems().addAll("Front View", "Back View (Flipped)");
        viewBox.setValue("Front View");
        viewBox.setOnAction(e -> canvas.setViewFlipped(viewBox.getValue().contains("Back")));

        ComboBox<String> layerBox = new ComboBox<>();
        layerBox.getItems().addAll("Bottom Layer (Blue)", "Top Layer (Red)");
        layerBox.setValue("Bottom Layer (Blue)");
        layerBox.setOnAction(e -> {
            if (layerBox.getValue().contains("Top")) canvas.setActiveLayer(Trace.Layer.TOP);
            else canvas.setActiveLayer(Trace.Layer.BOTTOM);
        });

        ComboBox<Double> widthBox = new ComboBox<>();
        widthBox.getItems().addAll(0.5, 0.8, 1.0, 1.5, 2.0);
        widthBox.setValue(1.0);
        widthBox.setOnAction(e -> canvas.setCurrentTraceWidth(widthBox.getValue()));

        Label lblPadSize = new Label(" Pad Size:");
        lblPadSize.setTextFill(Color.WHITE);

        ComboBox<Double> padBox = new ComboBox<>();
        padBox.getItems().addAll(1.5, 1.8, 2.0, 2.2, 2.5);
        padBox.setValue(2.0);
        padBox.setOnAction(e -> {
            board.setGlobalPadCopperDiameter(padBox.getValue());
            canvas.draw();
            canvas.saveState();
        });

        toolbar.getChildren().addAll(
                btnPads, btnWire, btnErase, btnText,
                lblView, viewBox,
                lblLayer, layerBox,
                lblWidth, widthBox,
                lblPadSize, padBox
        );

        return toolbar;
    }

    // --- MISE À JOUR : LA FENÊTRE DEMANDE MAINTENANT LE NOM DU PROJET ---
    private Object[] showStartupDialog() {
        Dialog<Object[]> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Define your project name and board size.");

        ButtonType createButtonType = new ButtonType("Create Board", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField("MyAwesomeProject");
        TextField colsField = new TextField("22");
        TextField rowsField = new TextField("11");

        grid.add(new Label("Project Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Width (holes):"), 0, 1);
        grid.add(colsField, 1, 1);
        grid.add(new Label("Height (holes):"), 0, 2);
        grid.add(rowsField, 1, 2);

        // Place le curseur directement dans la case du nom !
        Platform.runLater(nameField::requestFocus);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    String projName = nameField.getText().trim();
                    if (projName.isEmpty()) projName = "Untitled_Project";
                    return new Object[]{projName, Integer.parseInt(colsField.getText()), Integer.parseInt(rowsField.getText())};
                } catch (NumberFormatException e) {
                    return new Object[]{nameField.getText().trim(), 22, 11};
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    // --- NOUVEAU : CRÉATION DU DOSSIER SPÉCIFIQUE AU PROJET ---
    private Board handleNewProjectSetup(Object[] setupData) {
        if (setupData == null) return null;

        String projName = (String) setupData[0];
        int cols = (Integer) setupData[1];
        int rows = (Integer) setupData[2];

        // 1. On crée le sous-dossier (ex: Documents/Perf2Gerber_Projects/MyAwesomeProject/)
        java.io.File projDir = new java.io.File(getDefaultDirectory(), projName);
        if (!projDir.exists()) {
            projDir.mkdirs();
        }

        // 2. On pré-configure le fichier de sauvegarde (MyAwesomeProject.json)
        this.currentFile = new java.io.File(projDir, projName + ".json");

        return new Board(cols + 2, rows + 2, 2.54, 2.0, 1.0);
    }

    private void startNewProject() {
        Object[] setupData = showStartupDialog();
        if (setupData != null) {
            this.board = handleNewProjectSetup(setupData);
            canvas.setBoard(this.board);
            updateStatusBar();
            // On force une première sauvegarde silencieuse pour créer le fichier physique
            saveProject();
        }
    }

    // --- MISE À JOUR DE LA FENÊTRE D'ACCUEIL ---
    private Board showWelcomeScreen() {
        Alert welcome = new Alert(Alert.AlertType.CONFIRMATION);
        welcome.setTitle("Perf2Gerber - Welcome");
        welcome.setHeaderText("Let's build some circuits!");
        welcome.setContentText("What would you like to do?");

        ButtonType btnNew = new ButtonType("New Project");
        ButtonType btnOpen = new ButtonType("Open Project");
        ButtonType btnExit = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        welcome.getButtonTypes().setAll(btnNew, btnOpen, btnExit);

        Optional<ButtonType> result = welcome.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnNew) {
                Object[] setupData = showStartupDialog();
                return handleNewProjectSetup(setupData);
            } else if (result.get() == btnOpen) {
                return promptLoadBoard();
            }
        }
        return null;
    }

    private void openProject() {
        Board loaded = promptLoadBoard();
        if (loaded != null) {
            this.board = loaded;
            canvas.setBoard(this.board);
            updateStatusBar();
        }
    }

    private Board promptLoadBoard() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");
        fileChooser.setInitialDirectory(getDefaultDirectory());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));

        java.io.File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                Board loadedBoard = ProjectManager.loadBoard(file);
                if (loadedBoard != null) {
                    currentFile = file;
                    return loadedBoard;
                }
            } catch (Exception e) {
                showError("Load Error", "Could not open project: " + e.getMessage());
            }
        }
        return null;
    }

    private void saveProject() {
        if (currentFile == null) {
            saveProjectAs();
        } else {
            try {
                ProjectManager.saveBoard(board, currentFile);
            } catch (Exception e) {
                showError("Save Error", e.getMessage());
            }
        }
    }

    private void saveProjectAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project As");

        // Si on est déjà dans un dossier de projet, on l'ouvre par défaut
        if (currentFile != null && currentFile.getParentFile() != null) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
        } else {
            fileChooser.setInitialDirectory(getDefaultDirectory());
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                ProjectManager.saveBoard(board, file);
                currentFile = file;
            } catch (Exception e) {
                showError("Save Error", e.getMessage());
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void exportToGerber() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save Gerber ZIP for JLCPCB");

        // --- MISE À JOUR : EXPORTATION DIRECTEMENT DANS LE DOSSIER DU PROJET ---
        if (currentFile != null && currentFile.getParentFile() != null) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
            // On pré-remplit intelligemment le nom du ZIP !
            String projName = currentFile.getName().replace(".json", "");
            fileChooser.setInitialFileName(projName + "_Gerber.zip");
        } else {
            fileChooser.setInitialDirectory(getDefaultDirectory());
            fileChooser.setInitialFileName("Perf2Gerber_Project.zip");
        }

        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ZIP Archive (*.zip)", "*.zip"));

        java.io.File zipFile = fileChooser.showSaveDialog(null);

        if (zipFile != null) {
            try {
                java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("gerber_export");

                java.io.File edgeFile = new java.io.File(tempDir.toFile(), "board.GML");
                com.perf2gerber.exporter.GerberExporter.exportEdgeCuts(board, edgeFile);

                java.io.File drillFile = new java.io.File(tempDir.toFile(), "board.DRL");
                com.perf2gerber.exporter.GerberExporter.exportDrillFile(board, drillFile);

                java.io.File topFile = new java.io.File(tempDir.toFile(), "board.GTL");
                com.perf2gerber.exporter.GerberExporter.exportCopperLayer(board, Trace.Layer.TOP, topFile);

                java.io.File bottomFile = new java.io.File(tempDir.toFile(), "board.GBL");
                com.perf2gerber.exporter.GerberExporter.exportCopperLayer(board, Trace.Layer.BOTTOM, bottomFile);

                java.io.File topMaskFile = new java.io.File(tempDir.toFile(), "board.GTS");
                com.perf2gerber.exporter.GerberExporter.exportSolderMask(board, topMaskFile);

                java.io.File bottomMaskFile = new java.io.File(tempDir.toFile(), "board.GBS");
                com.perf2gerber.exporter.GerberExporter.exportSolderMask(board, bottomMaskFile);

                // --- EXPORT DU SILKSCREEN (TEXTE) ---
                java.io.File topSilkFile = new java.io.File(tempDir.toFile(), "board.GTO");
                com.perf2gerber.exporter.GerberExporter.exportSilkscreenLayer(board, Trace.Layer.TOP, topSilkFile);

                java.io.File bottomSilkFile = new java.io.File(tempDir.toFile(), "board.GBO");
                com.perf2gerber.exporter.GerberExporter.exportSilkscreenLayer(board, Trace.Layer.BOTTOM, bottomSilkFile);
                // ----------------------------------------------

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                     java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

                    java.io.File[] filesToZip = tempDir.toFile().listFiles();
                    if (filesToZip != null) {
                        for (java.io.File f : filesToZip) {
                            zos.putNextEntry(new java.util.zip.ZipEntry(f.getName()));
                            java.nio.file.Files.copy(f.toPath(), zos);
                            zos.closeEntry();
                        }
                    }
                }

                java.io.File[] tempFiles = tempDir.toFile().listFiles();
                if (tempFiles != null) {
                    for (java.io.File f : tempFiles) f.delete();
                }
                tempDir.toFile().delete();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Gerber ZIP generated!");
                alert.setContentText("Your file is ready for JLCPCB:\n" + zipFile.getAbsolutePath());
                alert.showAndWait();

            } catch (Exception e) {
                showError("Export Error", "Could not export ZIP: " + e.getMessage());
            }
        }
    }

    private java.io.File getDefaultDirectory() {
        java.io.File dir = new java.io.File(System.getProperty("user.home"), "Documents/Perf2Gerber_Projects");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void main(String[] args) {
        launch(args);
    }
}