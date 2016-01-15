package org.votingsystem.client.webextension.dialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.pane.WalletPane;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.currency.Wallet;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletDialog extends DialogVS {

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

    private void show(Set<Currency> currencySet) {
        if(currencySet.isEmpty()) {
            menuButton.setVisible(false);
        } else menuButton.setVisible(true);
        checkCurrencyMenuItem.setOnAction(event -> {
            ProgressDialog.show(new Task() {
                @Override protected Object call() throws Exception {
                    return Wallet.validateCertVSHashSet(currencySet);
                }
            }, ContextVS.getMessage("walletLbl"));
        });
        walletPane.load(currencySet);
        show();
    }

    public static void showDialog() {
        Platform.runLater(() -> {
            if(INSTANCE == null) INSTANCE = new WalletDialog();
            if(!BrowserHost.getInstance().isWalletLoaded()) {
                PasswordDialog.showWithoutPasswordConfirm(password -> {
                    if(password == null) return;
                    try {
                        Set<Currency> currencySet = BrowserHost.getInstance().loadWallet(password);
                        if(currencySet != null) INSTANCE.show(currencySet);
                    } catch (WalletException wex) {
                        Utils.showWalletNotFoundMessage();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                }, ContextVS.getMessage("walletPinMsg"));
            }
            else INSTANCE.show(BrowserHost.getInstance().getWalletCurrencySet());
        });
    }

}
