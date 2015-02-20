package org.votingsystem.client.pane;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.util.FullScreenHelper;
import org.votingsystem.client.util.ResizeHelper;
import org.votingsystem.client.util.Utils;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DecoratedPane extends VBox {

    private static Logger log = Logger.getLogger(DecoratedPane.class);

    private FullScreenHelper fullScreenHelper;
    private Label captionLbl;
    private VBox mainDialog;
    private HBox toolBar;
    private Button closeButton;
    private Stage stage;

    public DecoratedPane(String caption, MenuButton menuButton, Pane contentPane, Stage stage) {
        this.stage = stage;
        fullScreenHelper = new FullScreenHelper(stage);
        setStyle("-fx-background-insets: 3;" +
                "-fx-effect: dropshadow(three-pass-box, #888, 5, 0, 0, 0);" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 5, 5;");
        mainDialog = new VBox();
        mainDialog.setStyle("-fx-background-radius: 4;");
        VBox.setVgrow(mainDialog, Priority.ALWAYS);
        toolBar = new HBox();
        toolBar.setSpacing(10);
        toolBar.setStyle("-fx-padding: 3, 20;");
        toolBar.setAlignment(Pos.TOP_RIGHT);
        if(caption != null) {
            HBox captionBox = new HBox();
            captionBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(captionBox, Priority.ALWAYS);
            captionLbl = new Label(caption);
            captionLbl.setStyle("-fx-font-size: 1.1em; -fx-font-weight: bold; -fx-text-fill: #888;");
            captionBox.getChildren().add(captionLbl);
            toolBar.getChildren().add(captionBox);
        }
        closeButton = new Button();
        closeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> stage.close());
        if(menuButton != null) {
            menuButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BARS));
            toolBar.getChildren().add(menuButton);
        }
        toolBar.getChildren().add(closeButton);
        getChildren().add(mainDialog);
        mainDialog.getChildren().addAll(toolBar, contentPane);
        final Delta dragDelta = new Delta();
        toolBar.setOnMousePressed(mouseEvent -> {  // record a delta distance for the drag and drop operation.
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        toolBar.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
        toolBar.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    fullScreenHelper.toggleFullScreen();
                }
            }
        });
    }

    public void addResizeListener() {//must be called after Scene has been set
        ResizeHelper.addResizeListener(stage);
    }

    static class Delta { double x, y; }
}
