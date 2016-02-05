package org.votingsystem.test.ui;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.votingsystem.test.util.SimulationData;

import java.util.logging.Logger;


public class TestsApp extends Application {

    Logger log =  Logger.getLogger(TestsApp.class.getName());

    private static final TestsApp INSTANCE = new TestsApp();
    private static SimulationData simulationData;

    private ProgressBar progressBar;
    private Label progressLabel;
    private HBox progressBox;

    public static TestsApp getInstance() {
        return INSTANCE;
    }

    public static void main(String[] args) {
        SimulationData simulationData = new SimulationData();
        TestsApp.getInstance().init(simulationData);
    }

    public void init(SimulationData simulationData) {
        this.simulationData = simulationData;
        launch();
    }

    public void start(Stage myStage) {
        myStage.setTitle("TestsApp");
        GridPane rootNode = new GridPane();
        rootNode.setPrefWidth(500);
        rootNode.setPrefHeight(200);
        rootNode.setHgap(10);
        rootNode.setVgap(10);
        rootNode.setPadding(new Insets(10, 10, 10, 10));
        Scene myScene = new Scene(rootNode);
        myStage.setScene(myScene);
        Label infoLbl = new Label("Num. requests projected: " + simulationData.getNumRequestsProjected());

        progressLabel = new Label("progressLabel");
        progressLabel.setStyle("-fx-font-size: 12;-fx-font-weight: bold;");
        progressBar = new ProgressBar(0);

        progressBox = new HBox();
        progressBox.setSpacing(5);
        progressBox.getChildren().addAll(progressLabel, progressBar);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setStyle("-fx-alignment: center;");

        SimulationDataTask task = new SimulationDataTask(simulationData);
        progressBar.setProgress(0);
        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        new Thread(task).start();
        rootNode.setColumnSpan(infoLbl, 2);
        rootNode.add(infoLbl, 0 , 0);

        rootNode.setHalignment(infoLbl, HPos.CENTER);
        rootNode.setHalignment(progressBox, HPos.CENTER);

        rootNode.add(progressBox, 0 , 1);
        rootNode.setAlignment(Pos.CENTER);
        myStage.show();
    }

}