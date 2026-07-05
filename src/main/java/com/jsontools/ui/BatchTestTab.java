package com.jsontools.ui;

import com.jsontools.model.BatchTestRequest;
import com.jsontools.model.BatchTestResult;
import com.jsontools.testing.BatchTestRunner;
import com.jsontools.testing.TestStatistics;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Batch URL testing tab: load a file with HTTP requests, execute in batch,
 * display results and performance statistics.
 */
public class BatchTestTab extends BorderPane {

    private final Stage stage;
    private final BatchTestRunner testRunner;

    private final Label fileStatusLabel;
    private final TableView<BatchTestResult> resultsTable;
    private final ObservableList<BatchTestResult> resultItems;
    private final TextArea statsArea;
    private final ProgressBar progressBar;
    private final Label progressLabel;

    private List<BatchTestRequest> loadedRequests;

    public BatchTestTab(Stage stage) {
        this.stage = stage;
        this.testRunner = new BatchTestRunner();

        // --- Top: Controls ---
        Button loadFileBtn = new Button("Load Test File");
        loadFileBtn.setOnAction(e -> loadTestFile());

        fileStatusLabel = new Label("No file loaded");

        Button runSequentialBtn = new Button("Run Sequential");
        runSequentialBtn.getStyleClass().add("primary-button");
        runSequentialBtn.setOnAction(e -> runTests(false));

        Button runParallelBtn = new Button("Run Parallel");
        runParallelBtn.getStyleClass().add("primary-button");
        runParallelBtn.setOnAction(e -> runTests(true));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        progressLabel = new Label("");

        HBox controls = new HBox(10, loadFileBtn, fileStatusLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                runSequentialBtn, runParallelBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                progressBar, progressLabel);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(15));
        setTop(controls);

        // --- Center: Results table ---
        resultItems = FXCollections.observableArrayList();
        resultsTable = new TableView<>();
        resultsTable.setItems(resultItems);
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BatchTestResult, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestName()));
        nameCol.setPrefWidth(150);

        TableColumn<BatchTestResult, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestMethod()));
        methodCol.setPrefWidth(70);

        TableColumn<BatchTestResult, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestUrl()));
        urlCol.setPrefWidth(250);

        TableColumn<BatchTestResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getStatusCode())));
        statusCol.setPrefWidth(70);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int code = Integer.parseInt(item);
                    if (code >= 200 && code < 300) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (code >= 400) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (code < 0) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        setText("ERR");
                    } else {
                        setStyle("-fx-text-fill: orange;");
                    }
                }
            }
        });

        TableColumn<BatchTestResult, Number> timeCol = new TableColumn<>("Time (ms)");
        timeCol.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getResponseTimeMs()));
        timeCol.setPrefWidth(90);

        TableColumn<BatchTestResult, String> resultCol = new TableColumn<>("Result");
        resultCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isSuccess() ? "✓ PASS" : "✗ FAIL"));
        resultCol.setPrefWidth(80);
        resultCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(item.contains("PASS")
                            ? "-fx-text-fill: green; -fx-font-weight: bold;"
                            : "-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<BatchTestResult, String> errorCol = new TableColumn<>("Error");
        errorCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getErrorMessage() != null ? d.getValue().getErrorMessage() : ""));
        errorCol.setPrefWidth(200);

        resultsTable.getColumns().addAll(nameCol, methodCol, urlCol, statusCol, timeCol, resultCol, errorCol);

        // --- Right: Statistics panel ---
        statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefColumnCount(40);
        statsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
        statsArea.setPromptText("Statistics will appear after test run...");

        SplitPane split = new SplitPane();
        split.getItems().addAll(resultsTable, statsArea);
        split.setDividerPositions(0.65);

        setCenter(split);
    }

    private void loadTestFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Batch Test File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                loadedRequests = testRunner.loadRequests(file);
                fileStatusLabel.setText("Loaded: " + file.getName() +
                        " (" + loadedRequests.size() + " requests)");
                fileStatusLabel.setStyle("-fx-text-fill: green;");
                statsArea.setText("File loaded: " + file.getName() + "\n" +
                        loadedRequests.size() + " requests ready to execute.\n\n" +
                        "Click 'Run Sequential' or 'Run Parallel' to start.");
            } catch (Exception e) {
                fileStatusLabel.setText("Load failed");
                fileStatusLabel.setStyle("-fx-text-fill: red;");
                statsArea.setText("Error loading file: " + e.getMessage());
            }
        }
    }

    private void runTests(boolean parallel) {
        if (loadedRequests == null || loadedRequests.isEmpty()) {
            showAlert("No test file loaded. Please load a test file first.");
            return;
        }

        resultItems.clear();
        progressBar.setProgress(0);
        progressLabel.setText("Running...");
        statsArea.setText("Executing " + loadedRequests.size() + " requests " +
                (parallel ? "in parallel" : "sequentially") + "...");

        final int total = loadedRequests.size();

        CompletableFuture.runAsync(() -> {
            final int[] count = {0};
            List<BatchTestResult> results;

            if (parallel) {
                results = testRunner.runParallel(loadedRequests, result -> {
                    count[0]++;
                    final int current = count[0];
                    Platform.runLater(() -> {
                        resultItems.add(result);
                        progressBar.setProgress((double) current / total);
                        progressLabel.setText(current + "/" + total);
                    });
                });
            } else {
                results = testRunner.runSequential(loadedRequests, result -> {
                    count[0]++;
                    final int current = count[0];
                    Platform.runLater(() -> {
                        resultItems.add(result);
                        progressBar.setProgress((double) current / total);
                        progressLabel.setText(current + "/" + total);
                    });
                });
            }

            // Compute and display statistics
            TestStatistics stats = new TestStatistics(results);
            Platform.runLater(() -> {
                progressLabel.setText("Done! " + stats.getSuccessCount() + "/" + total + " passed");
                statsArea.setText(stats.toString());
            });
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
