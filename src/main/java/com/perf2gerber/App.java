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

    private ToggleButton btnDraw;
    private final String drawTextNormal = " Draw (Hold ⌘)";
    private final String drawTextContinuous = " Continuous ";

    private Label lblSize;
    private Label lblPhysical;
    private Label lblCoords;

    private java.io.File currentFile = null;

    @Override
    public void start(Stage primaryStage) {
        int[] initialSize = showStartupDialog();
        if (initialSize == null) {
            Platform.exit();
            return;
        }

        int totalCols = initialSize[0] + 2;
        int totalRows = initialSize[1] + 2;

        board = new Board(totalCols, totalRows, 2.54, 2.0, 1.0);
        canvas = new EditorCanvas(board);

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
            if (event.getCode().isModifierKey() && event.isShortcutDown()) {
                canvas.setCommandPressed(true);
                if (btnDraw.isSelected()) {
                    btnDraw.setText(drawTextContinuous);
                    btnDraw.setStyle("-fx-background-color: #5C8A5C; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
        });

        scene.setOnKeyReleased(event -> {
            if (!event.isShortcutDown()) {
                canvas.setCommandPressed(false);
                canvas.endCurrentTrace();
                btnDraw.setText(drawTextNormal);
                btnDraw.setStyle("");
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
        MenuItem itemSaveAs = new MenuItem("Save As...");
        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem itemExport = new MenuItem("Export to Gerber...");

        itemNew.setOnAction(e -> startNewProject());
        itemOpen.setOnAction(e -> openProject());
        itemSave.setOnAction(e -> saveProject());
        itemSaveAs.setOnAction(e -> saveProjectAs());

        menuFile.getItems().addAll(itemNew, itemOpen, itemSave, itemSaveAs, sep, itemExport);
        menuBar.getMenus().add(menuFile);
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
        });
        return btn;
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #2B2B2B;");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        btnDraw = new ToggleButton(drawTextNormal);
        btnDraw.setSelected(true);
        ToggleButton btnErase = new ToggleButton(" Erase");

        ToggleGroup toolGroup = new ToggleGroup();
        btnDraw.setToggleGroup(toolGroup);
        btnErase.setToggleGroup(toolGroup);

        btnDraw.setOnAction(e -> canvas.setTool(EditorCanvas.Tool.DRAW));
        btnErase.setOnAction(e -> canvas.setTool(EditorCanvas.Tool.ERASE));

        // --- CORRECTION DE VISIBILITÉ ---
        Label lblView = new Label("View:");
        lblView.setTextFill(Color.WHITE);

        Label lblLayer = new Label(" Draw on:");
        lblLayer.setTextFill(Color.WHITE);

        Label lblWidth = new Label(" Trace:");
        lblWidth.setTextFill(Color.WHITE);
        // --------------------------------

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

        // On utilise les nouveaux labels créés
        toolbar.getChildren().addAll(
                btnDraw, btnErase,
                lblView, viewBox,
                lblLayer, layerBox,
                lblWidth, widthBox
        );

        return toolbar;
    }

    private int[] showStartupDialog() {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Define the USEFUL area of your board.");

        ButtonType createButtonType = new ButtonType("Create Board", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField colsField = new TextField("22");
        TextField rowsField = new TextField("11");

        grid.add(new Label("Width:"), 0, 0);
        grid.add(colsField, 1, 0);
        grid.add(new Label("Height:"), 0, 1);
        grid.add(rowsField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    return new int[]{Integer.parseInt(colsField.getText()), Integer.parseInt(rowsField.getText())};
                } catch (NumberFormatException e) {
                    return new int[]{22, 11};
                }
            }
            return null;
        });

        Optional<int[]> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // --- CLEANED PROJECT MANAGEMENT ---

    private void startNewProject() {
        int[] size = showStartupDialog();
        if (size != null) {
            this.board = new Board(size[0] + 2, size[1] + 2, 2.54, 2.0, 1.0);
            canvas.setBoard(this.board);
            currentFile = null;
            updateStatusBar();
        }
    }

    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
        java.io.File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                Board loadedBoard = ProjectManager.loadBoard(file);
                if (loadedBoard != null) {
                    this.board = loadedBoard;
                    canvas.setBoard(this.board);
                    currentFile = file;
                    updateStatusBar();
                }
            } catch (Exception e) {
                showError("Load Error", "Could not open project: " + e.getMessage());
            }
        }
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

    public static void main(String[] args) {
        launch(args);
    }
}