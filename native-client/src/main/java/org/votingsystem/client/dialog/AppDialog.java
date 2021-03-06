package org.votingsystem.client.dialog;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.votingsystem.client.MainApp;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.Messages;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AppDialog {

    private Stage stage;
    //private DecoratedPane decoratedPane;

    private Pane mainPane;

    public AppDialog(Stage stage) throws IOException {
        /*stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.setAlwaysOnTop(true);*/
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        this.stage = stage;
    }

    public AppDialog(String fxmlFilePath) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
    }

    public AppDialog(String fxmlFilePath, String caption) throws IOException {
        this(fxmlFilePath, StageStyle.TRANSPARENT);
        setCaption(caption);
    }

    public AppDialog(String fxmlFilePath, StageStyle stageStyle) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFilePath));
        fxmlLoader.setController(this);
        stage = new Stage(stageStyle);
        //decoratedPane = new DecoratedPane(null, null, fxmlLoader.load(), stage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(MainApp.instance().getScene().getWindow());
        //stage.setScene(new Scene(decoratedPane));

        mainPane = fxmlLoader.load();
        stage.setScene(new Scene(mainPane));
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        stage.setTitle(Messages.currentInstance().get("mainDialogCaption"));
        //decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public AppDialog initOwner(Window window) {
        stage.initOwner(window);
        return this;
    }

    public void initModality(Modality modality) {
        stage.initModality(modality);
    }

    public void setPane(Pane pane) {
        //decoratedPane = new DecoratedPane(null, null, pane, stage);
        stage.setScene(new Scene(pane));
        //decoratedPane.getScene().setFill(Color.TRANSPARENT);
        Utils.addMouseDragSupport(stage);
    }

    public AppDialog setCaption(String caption) {
        //if(decoratedPane != null) decoratedPane.setCaption(caption);
        //else stage.setTitle(caption);


        stage.setTitle(caption);
        return this;
    }

    public void setCaption(String caption, boolean closeButtonVisible) {
        //if(decoratedPane != null) decoratedPane.setCaption(caption);
        //else stage.setTitle(caption);
        //decoratedPane.setCloseButtonVisible(closeButtonVisible);

        stage.setTitle(caption);
    }

    public AppDialog(Pane pane) {
        this(pane, null);
    }

    public AppDialog(Pane pane, Window parentWindow) {
        stage = new Stage(StageStyle.TRANSPARENT);
        //decoratedPane = new DecoratedPane(null, null, pane, stage);
        //stage.setScene(new Scene(decoratedPane));


        stage.setScene(new Scene(pane));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(Messages.currentInstance().get("mainDialogCaption"));
        if(parentWindow == null) stage.initOwner(MainApp.instance().getScene().getWindow());
        else stage.initOwner(parentWindow);
        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        stage.centerOnScreen();
        //decoratedPane.getScene().setFill(Color.TRANSPARENT);

        pane.getScene().setFill(Color.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        Utils.addMouseDragSupport(stage);
    }

    public AppDialog addCloseListener(EventHandler eventHandler) {
        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, windowEvent -> {
            if(eventHandler != null) eventHandler.handle(windowEvent);
        });
        return this;
    }

    public Parent getParent() {
        return stage.getScene().getRoot();
    }

    public Pane getContentPane() {
        //if(decoratedPane == null) return null;
        //return decoratedPane.getContentPane();

        return mainPane;
    }

    public Stage getStage() {
        return stage;
    }

    public void addMenuButton(MenuButton menuButton) {
        //Platform.runLater(() -> decoratedPane.addMenuButton(menuButton));


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