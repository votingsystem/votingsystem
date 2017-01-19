package org.votingsystem.client.dialog;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.Utils;
import org.votingsystem.util.Messages;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MainDialog extends AppDialog {

    private static Logger log = Logger.getLogger(MainDialog.class.getName());


    public MainDialog(Stage primaryStage) throws IOException {
        super(primaryStage);
        FlowPane mainPane = new FlowPane(10, 10);
        setPane(mainPane);
        setCaption(Messages.currentInstance().get("mainDialogCaption"));
        //set Stage boundaries to the top right corner of the visible bounds of the main screen
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 240);
        primaryStage.setY(primaryScreenBounds.getMinY() + 70);
        Button openFileButton = new Button(Messages.currentInstance().get("openFileButtonLbl"),
                Utils.getIcon(FontAwesome.Glyph.FOLDER_OPEN_ALT));
        openFileButton.setOnAction(actionEvent -> DocumentBrowserDialog.showDialog());
        openFileButton.setStyle("-fx-pref-width: 200px;");
        mainPane.getChildren().addAll(openFileButton);
        mainPane.setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-pref-width: 450px;");
    }

}