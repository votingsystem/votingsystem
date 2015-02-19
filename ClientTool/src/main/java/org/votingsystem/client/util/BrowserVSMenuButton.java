package org.votingsystem.client.util;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.SignDocumentFormPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.service.WebSocketServiceAuthenticated;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSMenuButton extends MenuButton {

    private static Logger log = Logger.getLogger(BrowserVSMenuButton.class);

    private MenuItem voteMenuItem;
    private MenuItem selectRepresentativeMenuItem;
    private MenuItem cooinUsersProceduresMenuItem;
    private MenuItem walletMenuItem;
    private MenuItem connectMenuItem;

    class EventBusConnectionListener {
        @Subscribe public void responseVSChange(ResponseVS responseVS) {
            log.debug("EventBusConnectionListener - response type: " + responseVS.getType());
            AtomicBoolean isConnected = new AtomicBoolean(false);
            if(TypeVS.INIT_VALIDATED_SESSION == responseVS.getType()) {
                isConnected.set(true);
            } else if(TypeVS.DISCONNECT == responseVS.getType()) { }
            PlatformImpl.runLater(() -> connectMenuItem.setVisible(!isConnected.get()));
        }
    }

    public BrowserVSMenuButton () {
        NotificationService.getInstance().registerToEventBus(new EventBusConnectionListener());
        setGraphic(Utils.getImage(FontAwesome.Glyph.BARS));
        MenuItem openFileMenuItem = new MenuItem(ContextVS.getMessage("openFileButtonLbl"));
        openFileMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.FOLDER_OPEN));
        openFileMenuItem.setOnAction(actionEvent -> {
            DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(null, null, null);
            BrowserVS.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
        });
        MenuItem signDocumentMenuItem = new MenuItem(ContextVS.getMessage("signDocumentButtonLbl"));
        signDocumentMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.CERTIFICATE));
        signDocumentMenuItem.setOnAction(actionEvent -> SignDocumentFormPane.showDialog());

        voteMenuItem = new MenuItem(ContextVS.getMessage("voteButtonLbl"));
        voteMenuItem.setVisible(false);
        voteMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.ENVELOPE));
        voteMenuItem.setOnAction(event -> {
            BrowserVS.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(),
                    ContextVS.getMessage("voteButtonLbl"));
        });
        selectRepresentativeMenuItem = new MenuItem(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeMenuItem.setVisible(false);
        selectRepresentativeMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.HAND_ALT_RIGHT));
        selectRepresentativeMenuItem.setOnAction(event -> {
            BrowserVS.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(),
                    ContextVS.getMessage("selectRepresentativeButtonLbl"));
        });

        cooinUsersProceduresMenuItem = new MenuItem(ContextVS.getMessage("financesLbl"));
        cooinUsersProceduresMenuItem.setVisible(false);
        cooinUsersProceduresMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.BAR_CHART_ALT));
        cooinUsersProceduresMenuItem.setOnAction(event -> {
            BrowserVS.getInstance().openCooinURL(ContextVS.getInstance().getCooinServer().getUserDashBoardURL(),
                    ContextVS.getMessage("cooinUsersLbl"));
        });
        walletMenuItem = new MenuItem(ContextVS.getMessage("walletLbl"));
        walletMenuItem.setVisible(false);
        walletMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.MONEY));
        walletMenuItem.setOnAction(event -> WalletPane.showDialog(BrowserVS.getInstance().getScene().getWindow()));

        connectMenuItem  = new MenuItem(ContextVS.getMessage("connectLbl"));
        connectMenuItem.setVisible(false);
        connectMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.FLASH));
        connectMenuItem.setOnAction(event -> WebSocketServiceAuthenticated.getInstance().setConnectionEnabled(
                true, SessionService.getInstance().getConnectDataMap()));

        MenuItem settingsMenuItem = new MenuItem(ContextVS.getMessage("settingsLbl"));
        settingsMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.COG));
        settingsMenuItem.setOnAction(actionEvent -> SettingsDialog.showDialog());
        MenuItem cooinAdminMenuItem = new MenuItem(ContextVS.getMessage("cooinAdminLbl"));
        cooinAdminMenuItem.setOnAction(actionEvent ->  BrowserVS.getInstance().openCooinURL(ContextVS.getInstance().getCooinServer().getAdminDashBoardURL(),
                ContextVS.getMessage("cooinAdminLbl")));
        MenuItem votingSystemAdminMenuItem = new MenuItem(ContextVS.getMessage("votingSystemProceduresLbl"));
        votingSystemAdminMenuItem.setOnAction(actionEvent -> BrowserVS.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getDashBoardURL(),
                ContextVS.getMessage("votingSystemProceduresLbl")));

        Menu adminsMenu = new Menu(ContextVS.getMessage("adminsMenuLbl"));
        adminsMenu.setGraphic(Utils.getImage(FontAwesome.Glyph.USERS));
        adminsMenu.getItems().addAll(cooinAdminMenuItem, votingSystemAdminMenuItem);

        getItems().addAll(voteMenuItem, selectRepresentativeMenuItem, new SeparatorMenuItem(),
                connectMenuItem, walletMenuItem, cooinUsersProceduresMenuItem, new SeparatorMenuItem(),
                signDocumentMenuItem, openFileMenuItem, new SeparatorMenuItem(),
                settingsMenuItem, new SeparatorMenuItem(), adminsMenu);
    }

    public void setVotingSystemAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            voteMenuItem.setVisible(available);
            selectRepresentativeMenuItem.setVisible(available);
        });
    }

    public void setCooinServerAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            cooinUsersProceduresMenuItem.setVisible(available);
            walletMenuItem.setVisible(available);
            connectMenuItem.setVisible(available);
        });
    }

}
