package com.jsontools.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.jsontools.model.JsonNodeWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree view for hierarchical JSON visualization.
 * Nodes expand lazily, tooltips show rich toString on non-primitives.
 * Supports search/filter that expands and highlights matching nodes.
 */
public class JsonTreeView extends BorderPane {

    private final TreeView<JsonNodeWrapper> treeView;
    private String currentSearch = "";

    public JsonTreeView() {
        treeView = new TreeView<>();
        treeView.setCellFactory(tv -> new JsonTreeCell());
        setCenter(treeView);
    }

    public void display(JsonNodeWrapper root) {
        currentSearch = "";
        TreeItem<JsonNodeWrapper> rootItem = createTreeItem(root);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
    }

    public void filter(String searchText) {
        currentSearch = (searchText == null) ? "" : searchText.toLowerCase();
        treeView.setCellFactory(tv -> new JsonTreeCell());  // Force cell refresh

        if (currentSearch.isBlank()) {
            collapseAll(treeView.getRoot());
            if (treeView.getRoot() != null) {
                treeView.getRoot().setExpanded(true);
            }
        } else {
            expandMatching(treeView.getRoot());
        }
    }

    private boolean expandMatching(TreeItem<JsonNodeWrapper> item) {
        if (item == null) return false;

        JsonNodeWrapper wrapper = item.getValue();
        boolean thisMatches = matchesSearch(wrapper);
        boolean childMatches = false;

        for (TreeItem<JsonNodeWrapper> child : item.getChildren()) {
            if (expandMatching(child)) {
                childMatches = true;
            }
        }

        if (thisMatches || childMatches) {
            item.setExpanded(true);
            return true;
        } else {
            item.setExpanded(false);
            return false;
        }
    }

    private boolean matchesSearch(JsonNodeWrapper wrapper) {
        if (currentSearch.isBlank()) return false;
        if (wrapper.getKey().toLowerCase().contains(currentSearch)) return true;
        if (wrapper.getValuePreview().toLowerCase().contains(currentSearch)) return true;
        return false;
    }

    private void collapseAll(TreeItem<JsonNodeWrapper> item) {
        if (item == null) return;
        item.setExpanded(false);
        for (TreeItem<JsonNodeWrapper> child : item.getChildren()) {
            collapseAll(child);
        }
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
     * Custom tree cell with icons, tooltips, and search highlighting.
     */
    private class JsonTreeCell extends TreeCell<JsonNodeWrapper> {
        @Override
        protected void updateItem(JsonNodeWrapper item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setTooltip(null);
                setStyle("");
            } else {
                String icon = getIcon(item);
                String displayText = item.getKey() + ": " + item.getValuePreview();
                setText(icon + " " + displayText);

                boolean highlighted = !currentSearch.isBlank() && matchesSearch(item);

                if (highlighted) {
                    setStyle("-fx-background-color: #FFEB3B; -fx-text-fill: #333;");
                } else if (!item.isPrimitive()) {
                    setStyle("-fx-text-fill: #2196F3;");
                } else {
                    setStyle("");
                }

                if (!item.isPrimitive()) {
                    Tooltip tooltip = new Tooltip(item.toString());
                    tooltip.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
                    tooltip.setShowDelay(javafx.util.Duration.millis(300));
                    tooltip.setMaxWidth(500);
                    tooltip.setWrapText(true);
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
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
