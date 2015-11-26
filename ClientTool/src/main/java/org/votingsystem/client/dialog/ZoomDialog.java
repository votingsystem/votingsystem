package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.votingsystem.client.Browser;
import org.votingsystem.util.ContextVS;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ZoomDialog extends DialogVS {

    private static Logger log = Logger.getLogger(ZoomDialog.class.getSimpleName());

    private VBox dialogVBox;
    private Task cancelTask;
    private DoubleProperty zoomProperty;
    private static ZoomDialog INSTANCE = null;

    public ZoomDialog() {
        super(new VBox(10));
        getStage().initModality(Modality.NONE);
        getStage().setAlwaysOnTop(true);
        dialogVBox = (VBox) getContentPane();
        dialogVBox.setStyle("-fx-pref-width: 400px;-fx-padding: 0 20 0 20;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-color: #f9f9f9;");

        addCloseListener(event -> hide());
        final Button resetZoomButton = new Button(ContextVS.getMessage("resetZoomLbl"));
        resetZoomButton.setStyle("-fx-font-size: 12;-fx-alignment: center;");
        resetZoomButton.setOnAction(event -> {
            zoomProperty.setValue(1);
        });

        HBox footerButtonsBox = new HBox();
        footerButtonsBox.getChildren().addAll(resetZoomButton);
        footerButtonsBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(0, 10, 10, 10));
        dialogVBox.getChildren().addAll(footerButtonsBox);
    }

    public static void show(DoubleProperty zoomProperty) {
        if(INSTANCE == null) INSTANCE = new ZoomDialog();
        Platform.runLater(() -> {
            INSTANCE.setZoomProperty(zoomProperty);
            if(!INSTANCE.getStage().isShowing()) INSTANCE.show();
            INSTANCE.getStage().setY(Browser.getInstance().getScene().getWindow().getY());
            INSTANCE.getStage().setX(Browser.getInstance().getScene().getWindow().getX() +
                    (Browser.getInstance().getScene().getWindow().getWidth()/2 - INSTANCE.getStage().getWidth()/2));
            if(INSTANCE.cancelTask != null) INSTANCE.cancelTask.cancel();
            INSTANCE.cancelTask = new Task() {
                @Override protected Object call() throws Exception {
                    Thread.sleep(2000);
                    PlatformImpl.runLater(() -> {if(INSTANCE.getStage().isShowing()) INSTANCE.getStage().close();});
                    return null;
                }
            };
            new Thread(INSTANCE.cancelTask).start();
        });
    }

    public void setZoomProperty(DoubleProperty zoomProperty) {
        this.zoomProperty = zoomProperty;
        setCaption(ContextVS.getMessage("zoomLbl", zoomProperty.getValue() * 100), false);
    }

}