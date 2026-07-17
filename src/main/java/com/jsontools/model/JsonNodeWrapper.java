package com.jsontools.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps a Jackson JsonNode for display in both table and tree views.
 * Provides rich toString for tooltip visualization.
 */
public class JsonNodeWrapper {

    private final String key;
    private final JsonNode node;
    private final String path;

    public JsonNodeWrapper(String key, JsonNode node, String path) {
        this.key = key;
        this.node = node;
        this.path = path;
    }

    public String getKey() {
        return key;
    }

    public JsonNode getNode() {
        return node;
    }

    public String getPath() {
        return path;
    }

    public boolean isPrimitive() {
        return node.isValueNode();
    }

    public boolean isObject() {
        return node.isObject();
    }

    public boolean isArray() {
        return node.isArray();
    }

    public String getType() {
        if (node.isTextual()) return "String";
        if (node.isNumber()) return "Number";
        if (node.isBoolean()) return "Boolean";
        if (node.isNull()) return "Null";
        if (node.isObject()) return "Object";
        if (node.isArray()) return "Array[" + node.size() + "]";
        return "Unknown";
    }

    public String getValuePreview() {
        if (node.isTextual()) return "\"" + node.asText() + "\"";
        if (node.isNumber()) return node.asText();
        if (node.isBoolean()) return node.asText();
        if (node.isNull()) return "null";
        if (node.isObject()) return toInlineObject();
        if (node.isArray()) return toInlineArray();
        return node.toString();
    }

    /**
     * Inline representation of an object: {key1: val1, key2: val2, ...}
     */
    private String toInlineObject() {
        StringBuilder sb = new StringBuilder("{");
        var fields = node.fields();
        int count = 0;
        while (fields.hasNext() && count < 5) {
            if (count > 0) sb.append(", ");
            var entry = fields.next();
            sb.append(entry.getKey()).append(": ");
            JsonNode val = entry.getValue();
            if (val.isTextual()) sb.append("\"").append(truncate(val.asText(), 20)).append("\"");
            else if (val.isValueNode()) sb.append(val.asText());
            else if (val.isObject()) sb.append("{..}");
            else if (val.isArray()) sb.append("[").append(val.size()).append("]");
            count++;
        }
        if (node.size() > 5) sb.append(", ...(+").append(node.size() - 5).append(")");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Inline representation of an array: [val1, val2, val3, ...]
     */
    private String toInlineArray() {
        StringBuilder sb = new StringBuilder("[");
        int limit = Math.min(node.size(), 6);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(", ");
            JsonNode item = node.get(i);
            if (item.isTextual()) sb.append("\"").append(truncate(item.asText(), 15)).append("\"");
            else if (item.isValueNode()) sb.append(item.asText());
            else if (item.isObject()) {
                // Show first field as hint
                var fields = item.fields();
                if (fields.hasNext()) {
                    var first = fields.next();
                    sb.append("{").append(first.getKey()).append(": ");
                    if (first.getValue().isValueNode()) sb.append(truncate(first.getValue().asText(), 10));
                    else sb.append("...");
                    sb.append(", ..}");
                } else {
                    sb.append("{}");
                }
            } else if (item.isArray()) {
                sb.append("[").append(item.size()).append("]");
            }
        }
        if (node.size() > limit) sb.append(", ...(+").append(node.size() - limit).append(")");
        sb.append("]");
        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…";
    }

    public List<JsonNodeWrapper> getChildren() {
        List<JsonNodeWrapper> children = new ArrayList<>();
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                children.add(new JsonNodeWrapper(
                        entry.getKey(),
                        entry.getValue(),
                        path + "." + entry.getKey()
                ));
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                children.add(new JsonNodeWrapper(
                        "[" + i + "]",
                        node.get(i),
                        path + "[" + i + "]"
                ));
            }
        }
        return children;
    }

    /**
     * Rich toString for tooltip visualization of non-primitive objects.
     */
    @Override
    public String toString() {
        if (isPrimitive()) {
            return key + ": " + getValuePreview();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(" (").append(getType()).append(")\n");
        sb.append("─────────────────────────────\n");

        if (isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            int count = 0;
            while (fields.hasNext() && count < 15) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode val = entry.getValue();
                sb.append("  ").append(entry.getKey()).append(": ");
                if (val.isValueNode()) {
                    sb.append(val.isTextual() ? "\"" + val.asText() + "\"" : val.asText());
                } else if (val.isObject()) {
                    sb.append("{...} (").append(val.size()).append(" fields)");
                } else if (val.isArray()) {
                    sb.append("[...] (").append(val.size()).append(" items)");
                }
                sb.append("\n");
                count++;
            }
            if (node.size() > 15) {
                sb.append("  ... and ").append(node.size() - 15).append(" more fields");
            }
        } else if (isArray()) {
            int limit = Math.min(node.size(), 10);
            for (int i = 0; i < limit; i++) {
                JsonNode item = node.get(i);
                sb.append("  [").append(i).append("]: ");
                if (item.isValueNode()) {
                    sb.append(item.isTextual() ? "\"" + item.asText() + "\"" : item.asText());
                } else if (item.isObject()) {
                    sb.append("{...} (").append(item.size()).append(" fields)");
                } else if (item.isArray()) {
                    sb.append("[...] (").append(item.size()).append(" items)");
                }
                sb.append("\n");
            }
            if (node.size() > 10) {
                sb.append("  ... and ").append(node.size() - 10).append(" more items");
            }
        }
        return sb.toString();
    }
}
