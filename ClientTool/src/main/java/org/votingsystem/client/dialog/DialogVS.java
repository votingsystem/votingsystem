package org.votingsystem.client.dialog;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.votingsystem.client.Browser;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogVS {

    private Stage stage;
    private DecoratedPane decoratedPane;

    public DialogVS(String fxmlFilePath) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
    }

    public DialogVS(String fxmlFilePath, String caption) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
        setCaption(caption);
    }

    public DialogVS(String fxmlFilePath, StageStyle stageStyle) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath));
        fxmlLoader.setController(this);
        stage = new Stage(stageStyle);
        decoratedPane = new DecoratedPane(null, null, fxmlLoader.load(), stage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(Browser.getInstance().getScene().getWindow());
        stage.setScene(new Scene(decoratedPane));
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        decoratedPane.getScene().getStylesheets().add(Utils.getResource("/css/dialogvs.css"));
        decoratedPane.getStyleClass().add("glassBox");
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public void setCaption(String caption) {
        if(decoratedPane != null) decoratedPane.setCaption(caption);
        else stage.setTitle(caption);
    }

    public DialogVS(Pane pane) {
        stage = new Stage(StageStyle.TRANSPARENT);
        decoratedPane = new DecoratedPane(null, null, pane, stage);
        stage.setScene(new Scene(decoratedPane));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(Browser.getInstance().getScene().getWindow());
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        decoratedPane.getScene().getStylesheets().add(Utils.getResource("/css/dialogvs.css"));
        decoratedPane.getStyleClass().add("glassBox");
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public void addCloseListener(EventHandler eventHandler) {
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, windowEvent -> {
            eventHandler.handle(windowEvent);
        });
    }

    public Parent getParent() {
        return stage.getScene().getRoot();
    }

    public Pane getContentPane() {
        if(decoratedPane == null) return null;
        return decoratedPane.getContentPane();
    }

    public Stage getStage() {
        return stage;
    }

    public void addMenuButton(MenuButton menuButton) {
        decoratedPane.addMenuButton(menuButton);
    }

    public void show() {
        stage.sizeToScene();
        stage.show();
        stage.toFront();
    }

    public void show(String caption) {
        setCaption(caption);
        show();
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