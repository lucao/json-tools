module com.jsontools {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.networknt.schema;
    requires java.net.http;

    opens com.jsontools to javafx.fxml;
    opens com.jsontools.model to com.fasterxml.jackson.databind, javafx.base;
    opens com.jsontools.ui to javafx.fxml;

    exports com.jsontools;
    exports com.jsontools.model;
    exports com.jsontools.service;
    exports com.jsontools.testing;
    exports com.jsontools.ui;
}
