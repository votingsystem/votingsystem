package org.votingsystem.applet.validationtool.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.dialog.FXMessageDialog;
import org.votingsystem.applet.validationtool.dialog.FXProgressDialog;
import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class BrowserVS extends Region {

    private static Logger logger = Logger.getLogger(BrowserVS.class);

    private HBox toolBar;
    private FXMessageDialog messageDialog;
    private FXProgressDialog progressDialog;
    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final WebView smallView = new WebView();
    final ComboBox comboBox = new ComboBox();
    private WebbAppListener webbAppListener;

    public BrowserVS() {
        comboBox.setPrefWidth(400);
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().add(comboBox);
        toolBar.getChildren().add(createSpacer());
        smallView.setPrefSize(120, 80);

        //handle popup windows
        webEngine.setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
                    @Override public WebEngine call(PopupFeatures config) {
                        smallView.setFontScale(0.8);
                        if (!toolBar.getChildren().contains(smallView)) {
                            toolBar.getChildren().add(smallView);
                        }
                        return smallView.getEngine();
                    }
                }
        );

        //process history
        final WebHistory history = webEngine.getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>(){
             @Override
             public void onChanged(Change<? extends WebHistory.Entry> c) {
                 c.next();
                 for (WebHistory.Entry e : c.getRemoved()) {
                     comboBox.getItems().remove(e.getUrl());
                 }
                 for (WebHistory.Entry e : c.getAddedSubList()) {
                     comboBox.getItems().add(e.getUrl());
                 }
             }
         });

        //set the behavior for the history combobox
        comboBox.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent ev) {
            int offset = comboBox.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();
            history.go(offset);
            }
        });

        // process page loading
        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> ov,
                                        Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            JSObject win = (JSObject) webEngine.executeScript("window");
                            win.setMember("javafxClient", new JavafxClient());
                        }
                    }
                }
        );
        getChildren().add(toolBar);
        getChildren().add(browser);
        getChildren().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> c) {}
        });
        browser.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override public void changed(ObservableValue<? extends Worker.State> observableValue,
                                          Worker.State state, Worker.State newState) {
                logger.debug(" --- newState: " + newState);
                if (newState.equals(Worker.State.RUNNING)) showProgressDialog(true);
                if (newState.equals(Worker.State.SUCCEEDED)) showProgressDialog(false);
                if (newState.equals(Worker.State.FAILED)) {
                    showProgressDialog(false);
                    showMessage(ContextVS.getMessage("conectionErrorMsg"));
                }
            }
        });
    }

    public void showMessage(String message) {
        if(messageDialog == null) messageDialog = new FXMessageDialog(this.getScene().getWindow());
        messageDialog.showMessage(message);
    }

    public void showProgressDialog(boolean isVisible) {
        if(this.getScene() == null) {
            logger.debug("showProgressDialog scene null");
            return;
        }
        if(progressDialog == null) progressDialog = new FXProgressDialog(this.getScene().getWindow());
        progressDialog.show(isVisible);
    }

    public void loadURL(String urlToLoad) {
        webEngine.load(urlToLoad);
    }

    public WebbAppListener getWebbAppListener() {
        return webbAppListener;
    }

    public void setWebbAppListener(WebbAppListener webbAppListener) {
        this.webbAppListener = webbAppListener;
    }

    // JavaScript interface object
    public class JavafxClient {
        //public void exit() {Platform.exit();}
        public void setMessageToSignatureClient(String messageToSignatureClient) {
            if(webbAppListener != null) webbAppListener.setMessageToSignatureClient(messageToSignatureClient);
            else logger.debug("webbAppListener null");
        }
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser,0,0,w,h-tbHeight,0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar,0,h-tbHeight,w,tbHeight,0,HPos.CENTER,VPos.CENTER);
    }

    @Override protected double computePrefWidth(double height) {
        return 750;
    }

    @Override protected double computePrefHeight(double width) {
        return 600;
    }

    public void reload() {
        webEngine.reload();
    }

}