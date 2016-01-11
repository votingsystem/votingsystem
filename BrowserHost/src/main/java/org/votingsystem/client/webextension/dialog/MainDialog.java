package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.pane.DecoratedPane;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.service.WebSocketAuthenticatedService;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MainDialog {

    private static Logger log = Logger.getLogger(MainDialog.class.getSimpleName());

    private VBox mainPane;
    private Button connectionButton;
    private Button walletButton;
    private Button qrCodeButton;
    private Stage primaryStage;
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

    public MainDialog(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        mainPane = new VBox(10);
        DecoratedPane decoratedPane = new DecoratedPane(null, null, mainPane, primaryStage);
        primaryStage.setScene(new Scene(decoratedPane));
        primaryStage.setTitle(ContextVS.getMessage("mainDialogCaption"));
        decoratedPane.setCaption(ContextVS.getMessage("mainDialogCaption"));
        primaryStage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
        decoratedPane.getScene().setFill(Color.TRANSPARENT);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setResizable(false);
        Utils.addMouseDragSupport(primaryStage);
        //set Stage boundaries to the top right corner of the visible bounds of the main screen
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 240);
        primaryStage.setY(primaryScreenBounds.getMinY() + 70);

        HBox firsRowButtonsBox = new HBox(10);
        HBox secondRowButtonsBox = new HBox(10);
        
        connectionButton = new Button(ContextVS.getMessage("connectLbl"));
        connectionButton.setTooltip(new Tooltip(ContextVS.getMessage("connectLbl")));
        connectionButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.CLOUD_UPLOAD, Utils.COLOR_RED_DARK));
        connectionButton.setOnAction(event -> {
            if(isConnected.get()) {
                Button optionButton = new Button(ContextVS.getMessage("disconnectLbl"));
                optionButton.setOnAction(event1 -> WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false));
                BrowserHost.showMessage(ContextVS.getMessage("disconnectMsg"), optionButton);
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

        walletButton = new Button(ContextVS.getMessage("walletLbl"));
        walletButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.MONEY));
        walletButton.setOnAction(event -> WalletDialog.showDialog());
        
        qrCodeButton = new Button(ContextVS.getMessage("createQRLbl"));
        qrCodeButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.QRCODE));
        qrCodeButton.setOnAction(event -> QRTransactionFormDialog.showDialog());

        Button settingsButton = new Button(ContextVS.getMessage("settingsLbl"));
        settingsButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.COG));
        settingsButton.setOnAction(actionEvent -> SettingsDialog.showDialog());


        connectionButton.setStyle("-fx-pref-width: 190px;");
        settingsButton.setStyle("-fx-pref-width: 190px;");
        walletButton.setStyle("-fx-pref-width: 190px;");
        qrCodeButton.setStyle("-fx-pref-width: 190px;");
        firsRowButtonsBox.getChildren().addAll(connectionButton, settingsButton);
        secondRowButtonsBox.getChildren().addAll(walletButton, qrCodeButton);

        mainPane.getChildren().addAll(firsRowButtonsBox, secondRowButtonsBox);
        mainPane.setStyle("-fx-max-width: 600px;-fx-padding: 3 20 20 20;-fx-spacing: 10;-fx-alignment: center;" +
                "-fx-font-size: 16;-fx-font-weight: bold;-fx-pref-width: 430px;");
        EventBusService.getInstance().register(new EventBusConnectionListener());
    }

    public void show() {
        primaryStage.show();
    }

    public void setVotingSystemAvailable(boolean available) {
        PlatformImpl.runLater(() -> { });
    }

    public void setCurrencyServerAvailable(boolean available) {
        PlatformImpl.runLater(() -> { });
    }
}