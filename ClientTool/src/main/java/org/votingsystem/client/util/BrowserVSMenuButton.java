package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.SettingsDialog;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.SignDocumentFormPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ContextVS;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSMenuButton extends MenuButton {

    private static Logger log = Logger.getLogger(BrowserVSMenuButton.class.getSimpleName());

    private MenuItem voteMenuItem;
    private MenuItem selectRepresentativeMenuItem;
    private MenuItem currencyUserMenuItem;
    private MenuItem walletMenuItem;
    private MenuItem votingSystemAdminMenuItem;
    private MenuItem currencyAdminMenuItem;


    public BrowserVSMenuButton () {
        setGraphic(Utils.getIcon(FontAwesomeIcons.BARS));
        MenuItem openFileMenuItem = new MenuItem(ContextVS.getMessage("openFileButtonLbl"));
        openFileMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.FOLDER_OPEN));
        openFileMenuItem.setOnAction(actionEvent -> {
            DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(null, null);
            Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
        });
        MenuItem signDocumentMenuItem = new MenuItem(ContextVS.getMessage("signDocumentButtonLbl"));
        signDocumentMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.CERTIFICATE));
        signDocumentMenuItem.setOnAction(actionEvent -> SignDocumentFormPane.showDialog());

        voteMenuItem = new MenuItem(ContextVS.getMessage("voteButtonLbl"));
        voteMenuItem.setVisible(false);
        voteMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.ENVELOPE));
        voteMenuItem.setOnAction(event -> {
            Browser.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getVotingPageURL(
                            "user", Locale.getDefault().toString()),
                    ContextVS.getMessage("voteButtonLbl"));
        });
        selectRepresentativeMenuItem = new MenuItem(ContextVS.getMessage("selectRepresentativeButtonLbl"));
        selectRepresentativeMenuItem.setVisible(false);
        selectRepresentativeMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.HAND_ALT_RIGHT));
        selectRepresentativeMenuItem.setOnAction(event -> {
            Browser.getInstance().openVotingSystemURL(ContextVS.getInstance().getAccessControl().getSelectRepresentativePageURL(
                    "user", Locale.getDefault().toString()), ContextVS.getMessage("selectRepresentativeButtonLbl"));
        });

        currencyUserMenuItem = new MenuItem(ContextVS.getMessage("financesLbl"));
        currencyUserMenuItem.setVisible(false);
        currencyUserMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.BAR_CHART));
        currencyUserMenuItem.setOnAction(event -> {
            StringBuilder sb = new StringBuilder();
            if(BrowserSessionService.getInstance().getUserVS() != null) sb.append(ContextVS.getInstance().getCurrencyServer()
                    .getUserVSBalanceURL(BrowserSessionService.getInstance().getUserVS().getNif(), "user",
                    Locale.getDefault().toString()));
            else sb.append(ContextVS.getInstance().getCurrencyServer().getGroupVSListURL("user", Locale.getDefault().toString()));
            Browser.getInstance().openCurrencyURL(sb.toString(), ContextVS.getMessage("currencyUsersLbl"));
        });
        walletMenuItem = new MenuItem(ContextVS.getMessage("walletLbl"));
        walletMenuItem.setVisible(false);
        walletMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.MONEY));
        walletMenuItem.setOnAction(event -> WalletPane.showDialog());

        MenuItem settingsMenuItem = new MenuItem(ContextVS.getMessage("settingsLbl"));
        settingsMenuItem.setGraphic(Utils.getIcon(FontAwesomeIcons.COG));
        settingsMenuItem.setOnAction(actionEvent -> SettingsDialog.showDialog());
        currencyAdminMenuItem = new MenuItem(ContextVS.getMessage("currencyAdminLbl"));
        currencyAdminMenuItem.setOnAction(actionEvent -> Browser.getInstance().openCurrencyURL(
                ContextVS.getInstance().getCurrencyServer().getDashBoardURL("admin", Locale.getDefault().toString()),
                ContextVS.getMessage("currencyAdminLbl")));
        currencyAdminMenuItem.setVisible(false);
        votingSystemAdminMenuItem = new MenuItem(ContextVS.getMessage("votingSystemProceduresLbl"));
        votingSystemAdminMenuItem.setOnAction(actionEvent -> Browser.getInstance().openVotingSystemURL(
                ContextVS.getInstance().getAccessControl().getDashBoardURL("admin", Locale.getDefault().toString()),
                ContextVS.getMessage("votingSystemProceduresLbl")));
        votingSystemAdminMenuItem.setVisible(false);

        Menu adminsMenu = new Menu(ContextVS.getMessage("adminsMenuLbl"));
        adminsMenu.setGraphic(Utils.getIcon(FontAwesomeIcons.USERS));
        adminsMenu.getItems().addAll(currencyAdminMenuItem, votingSystemAdminMenuItem);

        getItems().addAll(voteMenuItem, selectRepresentativeMenuItem, new SeparatorMenuItem(),
                walletMenuItem, currencyUserMenuItem, new SeparatorMenuItem(),
                signDocumentMenuItem, openFileMenuItem, new SeparatorMenuItem(),
                settingsMenuItem, new SeparatorMenuItem(), adminsMenu);
    }

    public void setVotingSystemAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            voteMenuItem.setVisible(available);
            selectRepresentativeMenuItem.setVisible(available);
            votingSystemAdminMenuItem.setVisible(available);
        });
    }

    public void setCurrencyServerAvailable(boolean available) {
        PlatformImpl.runLater(() -> {
            currencyUserMenuItem.setVisible(available);
            walletMenuItem.setVisible(available);
            currencyAdminMenuItem.setVisible(available);
        });
    }

}
