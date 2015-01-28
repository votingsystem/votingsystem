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
import org.votingsystem.client.util.Utils;

import java.io.IOException;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogVS {

    private Stage stage;
    private Node rootNode;

    public DialogVS(String fxmlFilePath) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
    }

    public DialogVS(String fxmlFilePath, StageStyle stageStyle) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath));
        stage = new Stage(stageStyle);
        stage.initModality(Modality.APPLICATION_MODAL);
        fxmlLoader.setController(this);
        stage.centerOnScreen();
        stage.setScene(new Scene(fxmlLoader.load()));
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
        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
    }

    public DialogVS(Node rootNode) {
        this.rootNode = rootNode;
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
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
        try {
            stage.close();
            this.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    class Delta { double x, y; }

}


