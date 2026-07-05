package com.jsontools;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import com.jsontools.ui.VisualizerTab;
import com.jsontools.ui.BatchTestTab;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();

        Tab visualizerTab = new Tab("JSON Visualizer");
        visualizerTab.setClosable(false);
        visualizerTab.setContent(new VisualizerTab(primaryStage));

        Tab batchTestTab = new Tab("Batch URL Tester");
        batchTestTab.setClosable(false);
        batchTestTab.setContent(new BatchTestTab(primaryStage));

        tabPane.getTabs().addAll(visualizerTab, batchTestTab);

        Scene scene = new Scene(tabPane, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("JSON Tools - Visualizer & Tester");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
