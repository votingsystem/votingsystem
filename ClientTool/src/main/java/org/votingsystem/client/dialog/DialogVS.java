package org.votingsystem.client.dialog;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogVS {

    private Stage stage;
    private Node rootNode;

    public DialogVS(FXMLLoader fxmlLoader) throws IOException {
        loadFXML(fxmlLoader);
    }
    public DialogVS(String fxmlFilePath) throws IOException {
        loadFXML(new FXMLLoader(getClass().getResource(fxmlFilePath)));
    }

    private void loadFXML(FXMLLoader fxmlLoader) throws IOException {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        fxmlLoader.setController(this);
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.centerOnScreen();
        // allow the dialog to be dragged around.
        final Node root = stage.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = getStage().getX() - mouseEvent.getScreenX();
                dragDelta.y = getStage().getY() - mouseEvent.getScreenY();
            }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                getStage().setX(mouseEvent.getScreenX() + dragDelta.x);
                getStage().setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }

    public DialogVS(Node rootNode) {
        this.rootNode = rootNode;
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent window) {      }
        });
        stage.centerOnScreen();
        // allow the dialog to be dragged around.
        final Delta dragDelta = new Delta();
        rootNode.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = getStage().getX() - mouseEvent.getScreenX();
                dragDelta.y = getStage().getY() - mouseEvent.getScreenY();
            }
        });
        rootNode.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                getStage().setX(mouseEvent.getScreenX() + dragDelta.x);
                getStage().setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }


    public Stage getStage() {
        return stage;
    }

    public Node getRootNode() {
        if(rootNode != null) return rootNode;
        return stage.getScene().getRoot();
    }

    public void show() {
        stage.show();
    }

    public void hide() {
        stage.getScene().getWindow().hide();
    }

    class Delta { double x, y; }

}


