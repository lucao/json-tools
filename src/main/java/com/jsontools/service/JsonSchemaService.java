package com.jsontools.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Service for loading JSON schemas and validating JSON data against them.
 */
public class JsonSchemaService {

    private final ObjectMapper objectMapper;
    private JsonSchema currentSchema;
    private JsonNode schemaNode;

    public JsonSchemaService() {
        this.objectMapper = new ObjectMapper();
    }

    public void loadSchema(File schemaFile) throws IOException {
        schemaNode = objectMapper.readTree(schemaFile);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        currentSchema = factory.getSchema(schemaNode);
    }

    public void loadSchema(String schemaJson) throws IOException {
        schemaNode = objectMapper.readTree(schemaJson);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        currentSchema = factory.getSchema(schemaNode);
    }

    public Set<ValidationMessage> validate(JsonNode jsonData) {
        if (currentSchema == null) {
            throw new IllegalStateException("No schema loaded. Load a schema first.");
        }
        return currentSchema.validate(jsonData);
    }

    public Set<ValidationMessage> validate(String jsonString) throws IOException {
        JsonNode jsonData = objectMapper.readTree(jsonString);
        return validate(jsonData);
    }

    public boolean isValid(JsonNode jsonData) {
        return validate(jsonData).isEmpty();
    }

    public boolean hasSchema() {
        return currentSchema != null;
    }

    public JsonNode getSchemaNode() {
        return schemaNode;
    }

    public JsonNode parseJson(String json) throws IOException {
        return objectMapper.readTree(json);
    }

    public JsonNode parseJson(File file) throws IOException {
        return objectMapper.readTree(file);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
