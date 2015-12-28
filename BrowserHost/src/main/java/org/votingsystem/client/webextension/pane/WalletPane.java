package org.votingsystem.client.webextension.pane;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.dialog.*;
import org.votingsystem.client.webextension.util.CurrencyCheckResponse;
import org.votingsystem.client.webextension.util.CurrencyCheckerTask;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Wallet;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletPane extends VBox implements UserDeviceSelectorDialog.Listener, CurrencyCheckerTask.Listener,
        PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(WalletPane.class.getSimpleName());

    private static WalletDialog DIALOG_INSTANCE;

    public WalletPane() {
        super(new VBox(10));
        getStylesheets().add(Utils.getResource("/css/wallet-pane.css"));
        getStyleClass().add("main-pane");
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    private void load(Set<Currency> wallet) {
        Map<String, Set<Currency>> currencyMap = new HashMap<>();
        getChildren().remove(0, getChildren().size());
        for(Currency currency : wallet) {
            if(currencyMap.containsKey(currency.getCurrencyCode())) currencyMap.get(currency.getCurrencyCode()).add(currency);
            else currencyMap.put(currency.getCurrencyCode(), new HashSet<>(Arrays.asList(currency)));
        }
        for(String currencyCode : currencyMap.keySet()) {
            Set<Currency> currencySet = currencyMap.get(currencyCode);
            VBox currencyPane = new VBox();
            currencyPane.getStyleClass().add("currency-pane");
            Label currencyLbl = new Label();
            currencyLbl.getStyleClass().add("currency");
            currencyLbl.setText(currencyCode);
            Map<String, Set<Currency>> tagMap = new HashMap<>();
            for(Currency currency : currencySet) {
                if(tagMap.containsKey(currency.getTagVS().getName())) tagMap.get(currency.getTagVS().getName()).add(currency);
                else tagMap.put(currency.getTagVS().getName(), new HashSet<>(Arrays.asList(currency)));
            }
            currencyPane.getChildren().add(currencyLbl);
            for(String tag: tagMap.keySet()) {
                Set<Currency> currencyTagSet = tagMap.get(tag);
                Integer tagAmount = currencyTagSet.stream().mapToInt(currency -> currency.getAmount().intValue()).sum();
                Label tagLbl = new Label();
                tagLbl.getStyleClass().add("tag");
                tagLbl.setText(MsgUtils.getTagDescription(tag));
                Label amountLbl = new Label();
                amountLbl.getStyleClass().add("amount");
                amountLbl.setText(tagAmount.toString() + " " + currencyCode);
                HBox tagInfoBox = new HBox();
                tagInfoBox.setAlignment(Pos.CENTER);
                tagInfoBox.setSpacing(15);
                tagInfoBox.getChildren().addAll(tagLbl, amountLbl);
                if(currencyPane.getChildren().size() > 1) VBox.setMargin(tagInfoBox, new Insets(15, 0, 3, 0));
                FlowPane tagFlowPane = new FlowPane();
                tagFlowPane.setHgap(10);
                tagFlowPane.setVgap(10);
                tagFlowPane.setAlignment(Pos.CENTER);
                for(Currency currency : currencyTagSet) {
                    HBox currencyHBox = new HBox();
                    currencyHBox.setOnMouseClicked(mouseEvent -> CurrencyDialog.show(currency, this.getScene().getWindow()));
                    currencyHBox.getStyleClass().add("currencyPane");
                    Label currencyValueLbl = new Label();
                    currencyValueLbl.getStyleClass().add("currencyValue");
                    currencyValueLbl.setText(currency.getAmount().toPlainString() + " " + currencyCode);
                    currencyHBox.getChildren().add(currencyValueLbl);
                    tagFlowPane.getChildren().add(currencyHBox);
                }
                currencyPane.getChildren().addAll(tagInfoBox, tagFlowPane);
            }
            getChildren().add(currencyPane);
        }
    }



    public static void show() {
        Platform.runLater(() -> {
            if(DIALOG_INSTANCE == null) DIALOG_INSTANCE = new WalletDialog();
            Set<Currency> currencySet = Wallet.getWallet();
            if(currencySet == null) {
                PasswordDialog.showWithoutPasswordConfirm(TypeVS.CURRENCY_OPEN, passwordListener,
                        ContextVS.getMessage("walletPinMsg"));
            } else DIALOG_INSTANCE.show(currencySet);
        });
    }


    @Override public void processCurrencyStatus(CurrencyCheckResponse response) {
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            showMessage(ResponseVS.SC_OK, ContextVS.getMessage("walletCheckResultOKMsg"));
        } else showMessage(ResponseVS.SC_ERROR, response.getMessage());
    }

    @Override
    public void processPassword(TypeVS passwordType, char[] password) {
        switch(passwordType) {
            case CURRENCY_OPEN:
                try {
                    Set<Currency> currencySet = Wallet.getWallet(password);
                    if(DIALOG_INSTANCE == null) DIALOG_INSTANCE = new WalletDialog();
                    DIALOG_INSTANCE.show(currencySet);
                } catch (WalletException wex) {
                    Utils.showWalletNotFoundMessage();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
                break;
        }

    }

    @Override public void cancelPassword(TypeVS passwordType) { }

    private static PasswordDialog.Listener passwordListener = new PasswordDialog.Listener(){
        @Override public void processPassword(TypeVS passwordType, char[] password) {
            try {
                Set<Currency> currencySet = Wallet.getWallet(password);
                if(DIALOG_INSTANCE == null) DIALOG_INSTANCE = new WalletDialog();
                DIALOG_INSTANCE.show(currencySet);
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
    public void setSelectedDevice(DeviceVSDto device) {
        log.info("setSelectedDevice");
    }


    private static class WalletDialog extends DialogVS {

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
            if(currencySet.isEmpty()) menuButton.setVisible(false);
            else menuButton.setVisible(true);
            checkCurrencyMenuItem.setOnAction(actionEvent -> {
                ProgressDialog.show(new CurrencyCheckerTask(currencySet, walletPane));
            });
            walletPane.load(currencySet);
            show();
        }
    }
}
