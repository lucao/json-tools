package com.jsontools.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.jsontools.model.JsonNodeWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * Tree view for hierarchical JSON visualization.
 * Nodes expand lazily, tooltips show rich toString on non-primitives.
 */
public class JsonTreeView extends BorderPane {

    private final TreeView<JsonNodeWrapper> treeView;

    public JsonTreeView() {
        treeView = new TreeView<>();
        treeView.setCellFactory(tv -> new JsonTreeCell());
        setCenter(treeView);
    }

    public void display(JsonNodeWrapper root) {
        TreeItem<JsonNodeWrapper> rootItem = createTreeItem(root);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
    }

    private TreeItem<JsonNodeWrapper> createTreeItem(JsonNodeWrapper wrapper) {
        TreeItem<JsonNodeWrapper> item = new TreeItem<>(wrapper);

        if (!wrapper.isPrimitive()) {
            // Add children lazily
            List<JsonNodeWrapper> children = wrapper.getChildren();
            for (JsonNodeWrapper child : children) {
                item.getChildren().add(createTreeItem(child));
            }
        }
        return item;
    }

    /**
     * Custom tree cell with icons and tooltips.
     */
    private static class JsonTreeCell extends TreeCell<JsonNodeWrapper> {
        @Override
        protected void updateItem(JsonNodeWrapper item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setTooltip(null);
            } else {
                String icon = getIcon(item);
                String displayText = item.getKey() + ": " + item.getValuePreview();
                setText(icon + " " + displayText);

                if (!item.isPrimitive()) {
                    Tooltip tooltip = new Tooltip(item.toString());
                    tooltip.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    tooltip.setMaxWidth(500);
                    tooltip.setWrapText(true);
                    setTooltip(tooltip);
                    setStyle("-fx-text-fill: #2196F3;");
                } else {
                    setTooltip(null);
                    setStyle("");
                }
            }
        }

        private String getIcon(JsonNodeWrapper item) {
            if (item.isObject()) return "{}";
            if (item.isArray()) return "[]";
            JsonNode node = item.getNode();
            if (node.isTextual()) return "\"\"";
            if (node.isNumber()) return "#";
            if (node.isBoolean()) return "✓";
            if (node.isNull()) return "∅";
            return "•";
        }
    }
}
