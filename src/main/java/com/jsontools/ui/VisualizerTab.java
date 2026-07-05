package com.jsontools.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.jsontools.model.JsonNodeWrapper;
import com.jsontools.service.HttpService;
import com.jsontools.service.JsonSchemaService;
import com.networknt.schema.ValidationMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Set;

/**
 * Main visualizer tab: load schema, fetch URL, display as table or tree.
 */
public class VisualizerTab extends BorderPane {

    private final Stage stage;
    private final JsonSchemaService schemaService;
    private final HttpService httpService;

    private final TextField urlField;
    private final Label schemaStatusLabel;
    private final TextArea validationArea;
    private final SplitPane contentSplit;
    private final JsonTableView tableView;
    private final JsonTreeView treeView;

    public VisualizerTab(Stage stage) {
        this.stage = stage;
        this.schemaService = new JsonSchemaService();
        this.httpService = new HttpService();

        // --- Top: URL input and schema controls ---
        urlField = new TextField();
        urlField.setPromptText("Enter JSON API URL...");
        urlField.setPrefWidth(500);

        Button fetchBtn = new Button("Fetch & Visualize");
        fetchBtn.getStyleClass().add("primary-button");
        fetchBtn.setOnAction(e -> fetchAndVisualize());

        Button loadSchemaBtn = new Button("Load Schema");
        loadSchemaBtn.setOnAction(e -> loadSchema());

        schemaStatusLabel = new Label("No schema loaded");
        schemaStatusLabel.getStyleClass().add("status-label");

        HBox urlBar = new HBox(10, new Label("URL:"), urlField, fetchBtn);
        urlBar.setAlignment(Pos.CENTER_LEFT);

        HBox schemaBar = new HBox(10, loadSchemaBtn, schemaStatusLabel);
        schemaBar.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(10, urlBar, schemaBar);
        topBox.setPadding(new Insets(15));
        setTop(topBox);

        // --- Center: Table + Tree split ---
        tableView = new JsonTableView();
        treeView = new JsonTreeView();

        Tab tableTab = new Tab("Table View", tableView);
        tableTab.setClosable(false);
        Tab treeTab = new Tab("Tree View", treeView);
        treeTab.setClosable(false);

        TabPane viewTabs = new TabPane(tableTab, treeTab);

        // --- Bottom: Validation output ---
        validationArea = new TextArea();
        validationArea.setEditable(false);
        validationArea.setPrefRowCount(4);
        validationArea.setPromptText("Schema validation results will appear here...");

        contentSplit = new SplitPane();
        contentSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        contentSplit.getItems().addAll(viewTabs, validationArea);
        contentSplit.setDividerPositions(0.8);

        setCenter(contentSplit);
    }

    private void loadSchema() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open JSON Schema");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Schema", "*.json", "*.schema")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                schemaService.loadSchema(file);
                schemaStatusLabel.setText("Schema: " + file.getName());
                schemaStatusLabel.setStyle("-fx-text-fill: green;");
                validationArea.setText("Schema loaded successfully: " + file.getName());
            } catch (Exception e) {
                schemaStatusLabel.setText("Schema load failed");
                schemaStatusLabel.setStyle("-fx-text-fill: red;");
                validationArea.setText("Error loading schema: " + e.getMessage());
            }
        }
    }

    private void fetchAndVisualize() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Please enter a URL.");
            return;
        }

        try {
            JsonNode json = httpService.fetchJson(url);

            // Validate against schema if loaded
            if (schemaService.hasSchema()) {
                Set<ValidationMessage> errors = schemaService.validate(json);
                if (errors.isEmpty()) {
                    validationArea.setText("✓ JSON is valid against the loaded schema.");
                    validationArea.setStyle("-fx-text-fill: green;");
                } else {
                    StringBuilder sb = new StringBuilder("✗ Validation errors:\n");
                    for (ValidationMessage msg : errors) {
                        sb.append("  • ").append(msg.getMessage()).append("\n");
                    }
                    validationArea.setText(sb.toString());
                    validationArea.setStyle("-fx-text-fill: red;");
                }
            } else {
                validationArea.setText("No schema loaded — showing raw JSON data.");
                validationArea.setStyle("");
            }

            // Populate views
            JsonNodeWrapper root = new JsonNodeWrapper("root", json, "$");
            tableView.display(root);
            treeView.display(root);

        } catch (Exception e) {
            validationArea.setText("Error fetching URL: " + e.getMessage());
            validationArea.setStyle("-fx-text-fill: red;");
            showAlert("Fetch failed: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
