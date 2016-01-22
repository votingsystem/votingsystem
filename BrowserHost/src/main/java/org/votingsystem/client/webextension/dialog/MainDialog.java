package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.InboxService;
import org.votingsystem.client.webextension.service.WebSocketAuthenticatedService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.service.EventBusService;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MainDialog extends DialogVS {

    private static Logger log = Logger.getLogger(MainDialog.class.getSimpleName());

    private Button connectionButton;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    class EventBusConnectionListener {

    @Subscribe public void call(SocketMessageDto socketMessage) {
            log.info("EventBusConnectionListener - response type: " + socketMessage.getOperation());
            boolean uiUpdated = false;
            if(TypeVS.INIT_SIGNED_SESSION == socketMessage.getOperation()) {
                isConnected.set(true);
                uiUpdated = true;
            } else if(TypeVS.DISCONNECT == socketMessage.getOperation()) {
                isConnected.set(false);
                uiUpdated = true;
            }
            if(uiUpdated) {
                PlatformImpl.runLater(() -> {
                    if (isConnected.get()) {
                        connectionButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.FLASH));
                        connectionButton.setTooltip(new Tooltip(ContextVS.getMessage("disconnectLbl")));
                        connectionButton.setText(ContextVS.getMessage("disconnectLbl"));
                    } else {
                        connectionButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CLOUD_UPLOAD));
                        connectionButton.setTooltip(new Tooltip(ContextVS.getMessage("connectLbl")));
                        connectionButton.setText(ContextVS.getMessage("connectLbl"));
                    }
                });
            }
        }
    }

    public MainDialog(Stage primaryStage) throws IOException {
        super(primaryStage);
        FlowPane mainPane = new FlowPane(10, 10);
        setPane(mainPane);
        setCaption(ContextVS.getMessage("mainDialogCaption"));
        //set Stage boundaries to the top right corner of the visible bounds of the main screen
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 240);
        primaryStage.setY(primaryScreenBounds.getMinY() + 70);

        connectionButton = new Button(ContextVS.getMessage("connectLbl"), Utils.getIcon(FontAwesome.Glyph.CLOUD_UPLOAD, Utils.COLOR_RED_DARK));
        connectionButton.setTooltip(new Tooltip(ContextVS.getMessage("connectLbl")));
        connectionButton.setOnAction(event -> {
            if(isConnected.get()) {
                Button optionButton = new Button(ContextVS.getMessage("disconnectLbl"));
                optionButton.setOnAction(event1 -> WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false));
                BrowserHost.showMessage(null, ContextVS.getMessage("disconnectMsg"), optionButton, null);
            } else {
                OperationVS operationVS = new OperationVS();
                try {
                    operationVS.setOperation(TypeVS.CONNECT).initProcess();
                } catch(Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
            }
        });

        Button walletButton = new Button(ContextVS.getMessage("walletLbl"), Utils.getIcon(FontAwesome.Glyph.MONEY));
        walletButton.setOnAction(event -> WalletDialog.showDialog());

        Button qrCodeButton = new Button(ContextVS.getMessage("createQRLbl"), Utils.getIcon(FontAwesome.Glyph.QRCODE));
        qrCodeButton.setOnAction(event -> QRTransactionFormDialog.showDialog());

        Button inboxButton = new Button(ContextVS.getMessage("messageInboxLbl"), Utils.getIcon(FontAwesome.Glyph.ENVELOPE));
        inboxButton.setOnAction(actionEvent -> SettingsDialog.showDialog());
        InboxService.getInstance().setInboxButton(inboxButton);

        Button openFileButton = new Button(ContextVS.getMessage("openFileButtonLbl"), Utils.getIcon(FontAwesome.Glyph.FOLDER_OPEN_ALT));
        openFileButton.setOnAction(actionEvent -> DocumentBrowserDialog.showDialog());

        connectionButton.setStyle("-fx-pref-width: 200px;");
        inboxButton.setStyle("-fx-pref-width: 200px;");
        walletButton.setStyle("-fx-pref-width: 200px;");
        qrCodeButton.setStyle("-fx-pref-width: 200px;");
        openFileButton.setStyle("-fx-pref-width: 200px;");

        MenuItem settingsMenuItem =  new MenuItem(ContextVS.getMessage("settingsLbl"), Utils.getIcon(FontAwesome.Glyph.COG));
        settingsMenuItem.setOnAction(actionEvent -> SettingsDialog.showDialog() );
        MenuButton menuButton = new MenuButton(null, Utils.getIcon(FontAwesome.Glyph.BARS));
        menuButton.getItems().addAll(settingsMenuItem);
        addMenuButton(menuButton);

        mainPane.getChildren().addAll(connectionButton, inboxButton, walletButton, qrCodeButton, openFileButton);
        mainPane.setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-pref-width: 450px;");
        EventBusService.getInstance().register(new EventBusConnectionListener());
    }
    

    public void setVotingSystemAvailable(boolean available) {
        PlatformImpl.runLater(() -> { });
    }

    public void setCurrencyServerAvailable(boolean available) {
        PlatformImpl.runLater(() -> { });
    }
}