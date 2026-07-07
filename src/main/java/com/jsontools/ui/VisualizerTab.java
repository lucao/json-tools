package com.jsontools.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.jsontools.model.JsonNodeWrapper;
import com.jsontools.service.HttpService;
import com.jsontools.service.JsonSchemaService;
import com.networknt.schema.ValidationMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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

    private final ComboBox<String> authTypeCombo;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final TextField tokenField;
    private final HBox basicAuthBox;
    private final HBox bearerAuthBox;

    private final TextField searchField;
    private final HBox searchBar;
    private final Label searchResultsLabel;

    public VisualizerTab(Stage stage) {
        this.stage = stage;
        this.schemaService = new JsonSchemaService();
        this.httpService = new HttpService();

        // --- Top: URL input, auth, and schema controls ---
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

        // Authentication controls
        authTypeCombo = new ComboBox<>();
        authTypeCombo.getItems().addAll("No Auth", "Basic Auth", "Bearer Token");
        authTypeCombo.setValue("No Auth");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(120);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(120);

        tokenField = new TextField();
        tokenField.setPromptText("Bearer token...");
        tokenField.setPrefWidth(300);

        basicAuthBox = new HBox(8, new Label("User:"), usernameField, new Label("Pass:"), passwordField);
        basicAuthBox.setAlignment(Pos.CENTER_LEFT);
        basicAuthBox.setVisible(false);
        basicAuthBox.setManaged(false);

        bearerAuthBox = new HBox(8, new Label("Token:"), tokenField);
        bearerAuthBox.setAlignment(Pos.CENTER_LEFT);
        bearerAuthBox.setVisible(false);
        bearerAuthBox.setManaged(false);

        authTypeCombo.setOnAction(e -> {
            String selected = authTypeCombo.getValue();
            basicAuthBox.setVisible("Basic Auth".equals(selected));
            basicAuthBox.setManaged("Basic Auth".equals(selected));
            bearerAuthBox.setVisible("Bearer Token".equals(selected));
            bearerAuthBox.setManaged("Bearer Token".equals(selected));
        });

        HBox urlBar = new HBox(10, new Label("URL:"), urlField, fetchBtn);
        urlBar.setAlignment(Pos.CENTER_LEFT);

        HBox authBar = new HBox(10, new Label("Auth:"), authTypeCombo, basicAuthBox, bearerAuthBox);
        authBar.setAlignment(Pos.CENTER_LEFT);

        HBox schemaBar = new HBox(10, loadSchemaBtn, schemaStatusLabel);
        schemaBar.setAlignment(Pos.CENTER_LEFT);

        VBox topBox = new VBox(10, urlBar, authBar, schemaBar);
        topBox.setPadding(new Insets(15));
        setTop(topBox);

        // --- Search bar ---
        searchField = new TextField();
        searchField.setPromptText("Search JSON (keys, values, paths)... Ctrl+F to focus");
        searchField.setPrefWidth(400);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearch(newVal));

        Button clearSearchBtn = new Button("✕");
        clearSearchBtn.setOnAction(e -> {
            searchField.clear();
            searchField.requestFocus();
        });

        searchResultsLabel = new Label("");
        searchResultsLabel.getStyleClass().add("status-label");

        searchBar = new HBox(8, new Label("🔍"), searchField, clearSearchBtn, searchResultsLabel);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(5, 15, 5, 15));
        searchBar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

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

        VBox centerBox = new VBox(searchBar, contentSplit);
        VBox.setVgrow(contentSplit, Priority.ALWAYS);
        setCenter(centerBox);

        // Ctrl+F shortcut to focus search
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                        () -> searchField.requestFocus()
                );
            }
        });
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
            Map<String, String> headers = buildAuthHeaders();
            JsonNode json = httpService.fetchJson(url, headers);

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

    private void applySearch(String searchText) {
        tableView.filter(searchText);
        treeView.filter(searchText);

        if (searchText == null || searchText.isBlank()) {
            searchResultsLabel.setText("");
        } else {
            searchResultsLabel.setText("Filtering by: \"" + searchText + "\"");
            searchResultsLabel.setStyle("-fx-text-fill: #666;");
        }
    }

    private Map<String, String> buildAuthHeaders() {
        String authType = authTypeCombo.getValue();
        if ("Basic Auth".equals(authType)) {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (!username.isEmpty()) {
                String encoded = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes());
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + encoded);
                return headers;
            }
        } else if ("Bearer Token".equals(authType)) {
            String token = tokenField.getText().trim();
            if (!token.isEmpty()) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        }
        return null;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
