package com.jsontools.ui;

import com.jsontools.model.JsonNodeWrapper;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Table view for JSON data with:
 * - Inline array/object previews
 * - Click to drill into objects (with slide animation)
 * - Right-click to open object in a new side-by-side panel
 * - Rich tooltips on hover for non-primitives
 */
public class JsonTableView extends BorderPane {

    private final TableView<JsonNodeWrapper> table;
    private final ObservableList<JsonNodeWrapper> items;
    private final Deque<JsonNodeWrapper> navigationStack;
    private final Label breadcrumb;
    private final Button backButton;
    private final HBox navBar;
    private final SplitPane splitPane;
    private final List<TableView<JsonNodeWrapper>> sidePanels;

    private List<JsonNodeWrapper> allChildren;
    private JsonNodeWrapper currentRoot;

    public JsonTableView() {
        this.items = FXCollections.observableArrayList();
        this.navigationStack = new ArrayDeque<>();
        this.allChildren = new ArrayList<>();
        this.sidePanels = new ArrayList<>();

        // Navigation bar
        backButton = new Button("← Back");
        backButton.setDisable(true);
        backButton.setOnAction(e -> navigateBack());

        breadcrumb = new Label("$");
        breadcrumb.getStyleClass().add("breadcrumb");
        breadcrumb.setStyle("-fx-font-family: monospace; -fx-font-size: 13; -fx-text-fill: #555;");

        Button closePanelsBtn = new Button("Close Panels");
        closePanelsBtn.setOnAction(e -> closeSidePanels());
        closePanelsBtn.setStyle("-fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navBar = new HBox(10, backButton, breadcrumb, spacer, closePanelsBtn);
        navBar.setPadding(new Insets(5, 10, 5, 10));
        navBar.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        setTop(navBar);

        // Main table
        table = createTable();
        table.setItems(items);

        // SplitPane to hold main table and side panels
        splitPane = new SplitPane();
        splitPane.getItems().add(table);
        setCenter(splitPane);
    }

    private TableView<JsonNodeWrapper> createTable() {
        TableView<JsonNodeWrapper> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<JsonNodeWrapper, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        keyCol.setPrefWidth(180);

        TableColumn<JsonNodeWrapper, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(90);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("Array") || item.equals("Object")) {
                        setStyle("-fx-text-fill: #7B1FA2; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #666;");
                    }
                }
            }
        });

        TableColumn<JsonNodeWrapper, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValuePreview()));
        valueCol.setPrefWidth(450);

        // Custom cell: clickable links for complex objects + tooltips
        valueCol.setCellFactory(col -> createValueCell(tv));

        tv.getColumns().addAll(keyCol, typeCol, valueCol);

        // Right-click context menu
        tv.setRowFactory(tableView -> {
            TableRow<JsonNodeWrapper> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem openSide = new MenuItem("Open in Side Panel");
            openSide.setOnAction(e -> {
                JsonNodeWrapper rowData = row.getItem();
                if (rowData != null && !rowData.isPrimitive()) {
                    openInSidePanel(rowData);
                }
            });

            MenuItem copyValue = new MenuItem("Copy Value");
            copyValue.setOnAction(e -> {
                JsonNodeWrapper rowData = row.getItem();
                if (rowData != null) {
                    javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(rowData.getNode().toString());
                    clipboard.setContent(content);
                }
            });

            MenuItem copyPath = new MenuItem("Copy Path");
            copyPath.setOnAction(e -> {
                JsonNodeWrapper rowData = row.getItem();
                if (rowData != null) {
                    javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(rowData.getPath());
                    clipboard.setContent(content);
                }
            });

            contextMenu.getItems().addAll(openSide, new SeparatorMenuItem(), copyValue, copyPath);

            row.setContextMenu(contextMenu);
            row.setOnContextMenuRequested(event -> {
                JsonNodeWrapper rowData = row.getItem();
                openSide.setDisable(rowData == null || rowData.isPrimitive());
            });

            return row;
        });

        return tv;
    }

    private TableCell<JsonNodeWrapper, String> createValueCell(TableView<JsonNodeWrapper> ownerTable) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    setStyle("");
                } else {
                    int idx = getIndex();
                    ObservableList<JsonNodeWrapper> tableItems = ownerTable.getItems();
                    if (idx >= 0 && idx < tableItems.size()) {
                        JsonNodeWrapper wrapper = tableItems.get(idx);

                        if (!wrapper.isPrimitive()) {
                            Hyperlink link = new Hyperlink(item);
                            link.setStyle("-fx-font-size: 12;");
                            link.setOnAction(e -> {
                                if (ownerTable == table) {
                                    navigateInto(wrapper);
                                } else {
                                    // Side panel: update in place
                                    ownerTable.getItems().setAll(wrapper.getChildren());
                                }
                            });
                            setGraphic(link);
                            setText(null);

                            Tooltip tooltip = new Tooltip(wrapper.toString());
                            tooltip.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
                            tooltip.setShowDelay(Duration.millis(300));
                            tooltip.setMaxWidth(600);
                            tooltip.setWrapText(true);
                            setTooltip(tooltip);
                        } else {
                            setText(item);
                            setGraphic(null);
                            setStyle("-fx-font-family: monospace;");

                            Tooltip tooltip = new Tooltip(wrapper.getKey() + " = " + item);
                            setTooltip(tooltip);
                        }
                    }
                }
            }
        };
    }

    public void display(JsonNodeWrapper root) {
        navigationStack.clear();
        currentRoot = root;
        closeSidePanels();
        showNode(root);
    }

    public void filter(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            items.setAll(allChildren);
        } else {
            String lower = searchText.toLowerCase();
            List<JsonNodeWrapper> filtered = allChildren.stream()
                    .filter(w -> matchesSearch(w, lower))
                    .toList();
            items.setAll(filtered);
        }
    }

    private boolean matchesSearch(JsonNodeWrapper wrapper, String searchLower) {
        if (wrapper.getKey().toLowerCase().contains(searchLower)) return true;
        if (wrapper.getValuePreview().toLowerCase().contains(searchLower)) return true;
        if (wrapper.getPath().toLowerCase().contains(searchLower)) return true;
        if (!wrapper.isPrimitive()) {
            return matchesDeep(wrapper, searchLower);
        }
        return false;
    }

    private boolean matchesDeep(JsonNodeWrapper wrapper, String searchLower) {
        for (JsonNodeWrapper child : wrapper.getChildren()) {
            if (child.getKey().toLowerCase().contains(searchLower)) return true;
            if (child.getValuePreview().toLowerCase().contains(searchLower)) return true;
            if (!child.isPrimitive() && matchesDeep(child, searchLower)) return true;
        }
        return false;
    }

    private void showNode(JsonNodeWrapper node) {
        allChildren = node.getChildren();

        // Animate the transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), table);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.3);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(120), table);
        slideOut.setFromX(0);
        slideOut.setToX(-30);

        fadeOut.setOnFinished(e -> {
            items.setAll(allChildren);
            breadcrumb.setText(node.getPath());
            backButton.setDisable(navigationStack.size() <= 1);

            // Animate in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), table);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), table);
            slideIn.setFromX(30);
            slideIn.setToX(0);

            fadeIn.play();
            slideIn.play();
        });

        fadeOut.play();
        slideOut.play();
    }

    private void navigateInto(JsonNodeWrapper node) {
        if (navigationStack.isEmpty()) {
            navigationStack.push(currentRoot);
        }
        navigationStack.push(node);
        showNode(node);
    }

    private void navigateBack() {
        if (navigationStack.size() > 1) {
            navigationStack.pop(); // Remove current
            JsonNodeWrapper parent = navigationStack.peek();

            // Reverse animation direction
            FadeTransition fadeOut = new FadeTransition(Duration.millis(120), table);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.3);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(120), table);
            slideOut.setFromX(0);
            slideOut.setToX(30);

            fadeOut.setOnFinished(e -> {
                allChildren = parent.getChildren();
                items.setAll(allChildren);
                breadcrumb.setText(parent.getPath());
                backButton.setDisable(navigationStack.size() <= 1);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), table);
                fadeIn.setFromValue(0.3);
                fadeIn.setToValue(1.0);

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), table);
                slideIn.setFromX(-30);
                slideIn.setToX(0);

                fadeIn.play();
                slideIn.play();
            });

            fadeOut.play();
            slideOut.play();
        }
    }

    private void openInSidePanel(JsonNodeWrapper node) {
        // Create a new table for the side panel
        TableView<JsonNodeWrapper> sideTable = createTable();
        ObservableList<JsonNodeWrapper> sideItems = FXCollections.observableArrayList(node.getChildren());
        sideTable.setItems(sideItems);

        // Wrap in a BorderPane with header and close button
        Label header = new Label(node.getPath());
        header.setStyle("-fx-font-family: monospace; -fx-font-size: 12; -fx-text-fill: #333; -fx-font-weight: bold;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-font-size: 10; -fx-padding: 2 6;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox sideHeader = new HBox(8, header, hSpacer, closeBtn);
        sideHeader.setPadding(new Insets(4, 8, 4, 8));
        sideHeader.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #90caf9; -fx-border-width: 0 0 1 0;");

        BorderPane sidePane = new BorderPane();
        sidePane.setTop(sideHeader);
        sidePane.setCenter(sideTable);

        closeBtn.setOnAction(e -> {
            splitPane.getItems().remove(sidePane);
            sidePanels.remove(sideTable);
            rebalanceDividers();
        });

        splitPane.getItems().add(sidePane);
        sidePanels.add(sideTable);
        rebalanceDividers();

        // Animate the new panel in
        sidePane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), sidePane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void closeSidePanels() {
        while (splitPane.getItems().size() > 1) {
            splitPane.getItems().removeLast();
        }
        sidePanels.clear();
    }

    private void rebalanceDividers() {
        int count = splitPane.getItems().size();
        if (count > 1) {
            double[] positions = new double[count - 1];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = (double) (i + 1) / count;
            }
            splitPane.setDividerPositions(positions);
        }
    }
}
