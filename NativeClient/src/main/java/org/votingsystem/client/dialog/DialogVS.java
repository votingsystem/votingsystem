package org.votingsystem.client.dialog;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.pane.DecoratedPane;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.ContextVS;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DialogVS {

    private Stage stage;
    private DecoratedPane decoratedPane;

    public DialogVS(Stage stage) throws IOException {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.setAlwaysOnTop(true);
        this.stage = stage;
    }

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
        stage.initOwner(MainApp.getInstance().getScene().getWindow());
        stage.setScene(new Scene(decoratedPane));
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        stage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public DialogVS initOwner(Window window) {
        stage.initOwner(window);
        return this;
    }

    public void initModality(Modality modality) {
        stage.initModality(modality);
    }

    public void setPane(Pane pane) {
        decoratedPane = new DecoratedPane(null, null, pane, stage);
        stage.setScene(new Scene(decoratedPane));
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public DialogVS setCaption(String caption) {
        if(decoratedPane != null) decoratedPane.setCaption(caption);
        else stage.setTitle(caption);
        return this;
    }

    public void setCaption(String caption, boolean closeButtonVisible) {
        if(decoratedPane != null) decoratedPane.setCaption(caption);
        else stage.setTitle(caption);
        decoratedPane.setCloseButtonVisible(closeButtonVisible);
    }

    public DialogVS(Pane pane) {
        this(pane, null);
    }

    public DialogVS(Pane pane, Window parentWindow) {
        stage = new Stage(StageStyle.TRANSPARENT);
        decoratedPane = new DecoratedPane(null, null, pane, stage);
        stage.setScene(new Scene(decoratedPane));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        if(parentWindow == null) stage.initOwner(MainApp.getInstance().getScene().getWindow());
        else stage.initOwner(parentWindow);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        Utils.addMouseDragSupport(stage);
    }

    public DialogVS addCloseListener(EventHandler eventHandler) {
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, windowEvent -> {
            if(eventHandler != null) eventHandler.handle(windowEvent);
        });
        return this;
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
        Platform.runLater(() -> decoratedPane.addMenuButton(menuButton));
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