package org.votingsystem.client.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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
        Utils.addMouseDragSupport(stage);
        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
    }

    public DialogVS(Pane pane) {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(new Scene(pane));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, windowEvent -> { });
        stage.centerOnScreen();
        Utils.addMouseDragSupport(stage);
    }

    public Parent getParent() {
        return stage.getScene().getRoot();
    }

    public Stage getStage() {
        return stage;
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

}


