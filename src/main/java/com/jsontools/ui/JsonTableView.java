package com.jsontools.ui;

import com.jsontools.model.JsonNodeWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Table view for JSON data. Complex objects appear as clickable links
 * that drill down into the object. Tooltips show rich toString() on hover.
 */
public class JsonTableView extends BorderPane {

    private final TableView<JsonNodeWrapper> table;
    private final ObservableList<JsonNodeWrapper> items;
    private final Deque<JsonNodeWrapper> navigationStack;
    private final Label breadcrumb;
    private final Button backButton;

    public JsonTableView() {
        this.items = FXCollections.observableArrayList();
        this.navigationStack = new ArrayDeque<>();

        // Navigation bar
        backButton = new Button("← Back");
        backButton.setDisable(true);
        backButton.setOnAction(e -> navigateBack());

        breadcrumb = new Label("$");
        breadcrumb.getStyleClass().add("breadcrumb");

        HBox navBar = new HBox(10, backButton, breadcrumb);
        navBar.setPadding(new Insets(5, 10, 5, 10));
        setTop(navBar);

        // Table setup
        table = new TableView<>();
        table.setItems(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<JsonNodeWrapper, String> keyCol = new TableColumn<>("Key");
        keyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        keyCol.setPrefWidth(200);

        TableColumn<JsonNodeWrapper, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(100);

        TableColumn<JsonNodeWrapper, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValuePreview()));
        valueCol.setPrefWidth(400);

        // Custom cell factory for clickable complex objects and tooltips
        valueCol.setCellFactory(col -> new TableCell<>() {
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
                    if (idx >= 0 && idx < items.size()) {
                        JsonNodeWrapper wrapper = items.get(idx);

                        if (!wrapper.isPrimitive()) {
                            // Clickable link style for complex objects
                            Hyperlink link = new Hyperlink(item);
                            link.setOnAction(e -> navigateInto(wrapper));
                            setGraphic(link);
                            setText(null);

                            // Rich tooltip for non-primitives
                            Tooltip tooltip = new Tooltip(wrapper.toString());
                            tooltip.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
                            tooltip.setShowDelay(javafx.util.Duration.millis(300));
                            tooltip.setMaxWidth(500);
                            tooltip.setWrapText(true);
                            setTooltip(tooltip);
                        } else {
                            setText(item);
                            setGraphic(null);

                            Tooltip tooltip = new Tooltip(wrapper.getKey() + " = " + item);
                            setTooltip(tooltip);
                        }
                    }
                }
            }
        });

        table.getColumns().addAll(keyCol, typeCol, valueCol);
        setCenter(table);
    }

    public void display(JsonNodeWrapper root) {
        navigationStack.clear();
        showNode(root);
    }

    private void showNode(JsonNodeWrapper node) {
        items.clear();
        List<JsonNodeWrapper> children = node.getChildren();
        items.addAll(children);
        breadcrumb.setText(node.getPath());
        backButton.setDisable(navigationStack.isEmpty());
    }

    private void navigateInto(JsonNodeWrapper node) {
        navigationStack.push(node);
        showNode(node);
    }

    private void navigateBack() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop(); // Remove current
            if (!navigationStack.isEmpty()) {
                JsonNodeWrapper parent = navigationStack.peek();
                showNode(parent);
            }
        }
    }
}
