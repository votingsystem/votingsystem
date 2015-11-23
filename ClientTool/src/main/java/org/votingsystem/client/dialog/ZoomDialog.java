package org.votingsystem.client.dialog;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import org.votingsystem.util.ContextVS;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ZoomDialog extends DialogVS {

    private static Logger log = Logger.getLogger(ZoomDialog.class.getSimpleName());

    private VBox dialogVBox;
    private Text messageText;
    private Task cancelTask;
    private DoubleProperty zoomProperty;
    private static ZoomDialog INSTANCE = null;

    public ZoomDialog() {
        super(new VBox(10));
        getStage().initModality(Modality.NONE);
        getStage().setAlwaysOnTop(true);
        dialogVBox = (VBox) getContentPane();
        dialogVBox.setStyle("-fx-pref-width: 400px;-fx-padding: 0 20 10 20;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-color: #f9f9f9;");

        messageText = new Text();
        messageText.setWrappingWidth(320);
        messageText.setStyle("-fx-font-size: 22;-fx-font-weight: bold;-fx-fill: #6c0404;-fx-end-margin: 5;");
        messageText.setTextAlignment(TextAlignment.CENTER);

        addCloseListener(event -> hide());
        final Button resetZoomButton = new Button(ContextVS.getMessage("resetZoomLbl"));
        resetZoomButton.setStyle("-fx-font-size: 12;-fx-alignment: center;");
        resetZoomButton.setOnAction(event -> {
            zoomProperty.setValue(1);
        });

        HBox footerButtonsBox = new HBox();
        footerButtonsBox.getChildren().addAll(resetZoomButton);
        footerButtonsBox.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setMargin(footerButtonsBox, new javafx.geometry.Insets(20, 20, 20, 5));
        dialogVBox.getChildren().addAll(messageText, footerButtonsBox);
    }

    public static void show(DoubleProperty zoomProperty) {
        if(INSTANCE == null) INSTANCE = new ZoomDialog();
        Platform.runLater(() -> {
            INSTANCE.setZoomProperty(zoomProperty);
            if(!INSTANCE.getStage().isShowing()) INSTANCE.show();
            INSTANCE.getStage().setY(50);
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
        messageText.setText(ContextVS.getMessage("zoomLbl", zoomProperty.getValue() * 100));
    }

}