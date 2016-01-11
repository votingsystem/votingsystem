package org.votingsystem.client.webextension.dialog;

import javafx.application.Platform;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.pane.WalletPane;
import org.votingsystem.client.webextension.task.CurrencyValidatorTask;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletDialog extends DialogVS implements PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(WalletDialog.class.getSimpleName());

    private static WalletDialog INSTANCE;
    private WalletPane walletPane;
    private MenuButton menuButton;
    private MenuItem checkCurrencyMenuItem;

    public WalletDialog() {
        super(new WalletPane());
        walletPane = (WalletPane) getContentPane();
        checkCurrencyMenuItem =  new MenuItem(ContextVS.getMessage("checkCurrencyMenuItemLbl"));
        menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.BARS));
        menuButton.getItems().addAll(checkCurrencyMenuItem);
        addMenuButton(menuButton);
        setCaption(ContextVS.getMessage("walletLbl"));
    }

    private static PasswordDialog.Listener passwordListener = new PasswordDialog.Listener(){
        @Override public void processPassword(TypeVS passwordType, char[] password) {
            try {
                Set<Currency> currencySet = Wallet.getWallet(password);
                if(INSTANCE == null) INSTANCE = new WalletDialog();
                INSTANCE.show(currencySet);
            } catch (WalletException wex) {
                Utils.showWalletNotFoundMessage();
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        @Override public void cancelPassword(TypeVS passwordType) { }
    };

    @Override
    public void processPassword(TypeVS passwordType, char[] password) {
        switch(passwordType) {
            case CURRENCY_OPEN:
                try {
                    Set<Currency> currencySet = Wallet.getWallet(password);
                    if(INSTANCE == null) INSTANCE = new WalletDialog();
                    INSTANCE.show(currencySet);
                } catch (WalletException wex) {
                    Utils.showWalletNotFoundMessage();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
                break;
        }

    }

    @Override
    public void cancelPassword(TypeVS passwordType) {

    }

    private void show(Set<Currency> currencySet) {
        if(currencySet.isEmpty()) menuButton.setVisible(false);
        else menuButton.setVisible(true);
        checkCurrencyMenuItem.setOnAction(event -> {
            ProgressDialog.show(new CurrencyValidatorTask(currencySet, walletPane), ContextVS.getMessage("walletLbl"));
        });
        walletPane.load(currencySet);
        show();
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            if(INSTANCE == null) INSTANCE = new WalletDialog();
            Set<Currency> currencySet = Wallet.getWallet();
            if(currencySet == null) {
                PasswordDialog.showWithoutPasswordConfirm(TypeVS.CURRENCY_OPEN, passwordListener,
                        ContextVS.getMessage("walletPinMsg"));
            } else INSTANCE.show(currencySet);
        });
    }

}
