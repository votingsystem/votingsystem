package org.votingsystem.client.pane;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.service.WebSocketServiceAuthenticated;
import org.votingsystem.client.util.BrowserVSMenuButton;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSToolbar extends HBox {

    private static Logger log = Logger.getLogger(BrowserVSToolbar.class);

    private TextField locationField = new TextField("");
    private Button reloadButton;
    private Button prevButton;
    private Button forwardButton;
    private Button connectionButton;
    private BrowserVSMenuButton menuButton;

    class EventBusConnectionListener {
        @Subscribe
        public void responseVSChange(ResponseVS responseVS) {
            log.debug("EventBusConnectionListener - response type: " + responseVS.getType());
            AtomicBoolean isConnected = new AtomicBoolean(false);
            if(TypeVS.INIT_VALIDATED_SESSION == responseVS.getType()) {
                isConnected.set(true);
            } else if(TypeVS.DISCONNECT == responseVS.getType()) { }
            PlatformImpl.runLater(() -> connectionButton.setVisible(isConnected.get()));
        }
    }
    public BrowserVSToolbar(Stage stage) {
        NotificationService.getInstance().registerToEventBus(new EventBusConnectionListener());
        setSpacing(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("browser-toolbar");
        forwardButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.CHEVRON_RIGHT));;
        prevButton =  Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.CHEVRON_LEFT));
        reloadButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.REFRESH));
        connectionButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.FLASH));
        prevButton.setDisable(true);
        forwardButton.setDisable(true);
        Button  newTabButton = Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.PLUS));
        newTabButton.getStyleClass().add("toolbar-button");
        newTabButton.setOnAction(event -> BrowserVS.getInstance().newTab(null, null, null));
        connectionButton.setVisible(false);
        connectionButton.setOnAction(event -> {
            Button optionButton = new Button(ContextVS.getMessage("disconnectLbl"));
            optionButton.setOnAction(event1 -> WebSocketServiceAuthenticated.getInstance().setConnectionEnabled(false, null));
            showMessage(ContextVS.getMessage("disconnectMsg"), optionButton);
        });

        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.getStyleClass().add("location-text");
        NotificationService.getInstance().setNotificationsButton(
                Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.INFO_CIRCLE, Utils.COLOR_YELLOW_ALERT)));
        InboxService.getInstance().setInboxButton(Utils.getToolBarButton(
                Utils.getImage(FontAwesome.Glyph.ENVELOPE, Utils.COLOR_RED_DARK)));

        menuButton = new BrowserVSMenuButton();
        menuButton.getStyleClass().add("toolbar-button");

        Button closeButton =  Utils.getToolBarButton(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> VotingSystemApp.getInstance().stop());

        HBox navButtonBox = new HBox();
        navButtonBox.getChildren().addAll(prevButton, forwardButton);
        getChildren().addAll(newTabButton, navButtonBox, locationField, reloadButton, Utils.createSpacer(), connectionButton,
                NotificationService.getInstance().getNotificationsButton(), InboxService.getInstance().getInboxButton(),
                menuButton, closeButton);
        setOnMouseClicked(mouseEvent -> {
                if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                    if(mouseEvent.getClickCount() == 2){
                        BrowserVS.getInstance().toggleFullScreen();
                    }
                }
            });
        final Delta dragDelta = new Delta();
        setOnMousePressed(mouseEvent -> {  // record a delta distance for the drag and drop operation.
            dragDelta.x = stage.getX() - mouseEvent.getScreenX();
            dragDelta.y = stage.getY() - mouseEvent.getScreenY();
        });
        setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() + dragDelta.x);
            stage.setY(mouseEvent.getScreenY() + dragDelta.y);
        });
    }

    public Button getPrevButton() {
        return prevButton;
    }

    public Button getForwardButton() {
        return forwardButton;
    }

    public Button getReloadButton() {
        return reloadButton;
    }

    public TextField getLocationField() {
        return locationField;
    }

    public void setCooinServerAvailable(boolean available) {
        menuButton.setCooinServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        menuButton.setVotingSystemAvailable(available);
    }

    static class Delta { double x, y; }
}
