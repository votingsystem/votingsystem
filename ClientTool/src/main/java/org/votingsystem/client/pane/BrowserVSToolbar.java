package org.votingsystem.client.pane;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import java.util.logging.Logger;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.EventBusService;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.util.BrowserVSMenuButton;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSToolbar extends HBox {

    private static Logger log = Logger.getLogger(BrowserVSToolbar.class.getSimpleName());

    private TextField locationField = new TextField("");
    private Button reloadButton;
    private Button prevButton;
    private Button forwardButton;
    private Button connectionButton;
    private BrowserVSMenuButton menuButton;

    class EventBusConnectionListener {
        @Subscribe
        public void responseVSChange(ResponseVS responseVS) {
            log.info("EventBusConnectionListener - response type: " + responseVS.getType());
            AtomicBoolean isConnected = new AtomicBoolean(false);
            if(TypeVS.INIT_VALIDATED_SESSION == responseVS.getType()) {
                isConnected.set(true);
            } else if(TypeVS.DISCONNECT == responseVS.getType()) { }
            PlatformImpl.runLater(() -> connectionButton.setVisible(isConnected.get()));
        }
    }
    public BrowserVSToolbar(Stage stage) {
        EventBusService.getInstance().register(new EventBusConnectionListener());
        setSpacing(10);
        setAlignment(Pos.CENTER);
        getStyleClass().add("browser-toolbar");
        forwardButton = Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.CHEVRON_RIGHT));;
        prevButton =  Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.CHEVRON_LEFT));
        reloadButton = Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.REFRESH));
        connectionButton = Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.FLASH));
        prevButton.setDisable(true);
        forwardButton.setDisable(true);
        Button newTabButton = Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.PLUS));
        newTabButton.getStyleClass().add("toolbar-button");
        newTabButton.setOnAction(event -> BrowserVS.getInstance().newTab(null, null, null));
        connectionButton.setVisible(false);
        connectionButton.setOnAction(event -> {
            Button optionButton = new Button(ContextVS.getMessage("disconnectLbl"));
            optionButton.setOnAction(event1 -> WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false, null));
            showMessage(ContextVS.getMessage("disconnectMsg"), optionButton);
        });

        HBox.setHgrow(locationField, Priority.ALWAYS);
        locationField.getStyleClass().add("location-text");
        Button msgButton = Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.ENVELOPE, Utils.COLOR_RED_DARK));
        InboxService.getInstance().setInboxButton(msgButton);

        menuButton = new BrowserVSMenuButton();
        menuButton.getStyleClass().add("toolbar-button");

        Button closeButton =  Utils.getToolBarButton(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> VotingSystemApp.getInstance().stop());

        HBox navButtonBox = new HBox();
        navButtonBox.getChildren().addAll(prevButton, forwardButton);
        getChildren().addAll(newTabButton, navButtonBox, locationField, reloadButton, Utils.createSpacer(), connectionButton,
                msgButton, menuButton, closeButton);
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

    public void setCurrencyServerAvailable(boolean available) {
        menuButton.setCurrencyServerAvailable(available);
    }

    public void setVotingSystemAvailable(boolean available) {
        menuButton.setVotingSystemAvailable(available);
    }

    static class Delta { double x, y; }
}
