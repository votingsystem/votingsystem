package org.votingsystem.client.pane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.dialog.*;
import org.votingsystem.client.util.CurrencyCheckResponse;
import org.votingsystem.client.util.CurrencyCheckerTask;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
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

import static org.votingsystem.client.Browser.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletPane extends VBox implements UserDeviceSelectorDialog.Listener, CurrencyCheckerTask.Listener,
        PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(WalletPane.class.getSimpleName());

    private MenuButton menuButton;
    private MenuItem checkCurrencyMenuItem;
    private Map<String, Set<Currency>> currencyMap;
    private static WalletDialog DIALOG_INSTANCE;

    public WalletPane() {
        super(new VBox(10));
        getStylesheets().add(Utils.getResource("/css/wallet-pane.css"));
        getStyleClass().add("main-pane");
        VBox.setVgrow(this, Priority.ALWAYS);
        currencyMap = new HashMap<>();
        menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getIcon(FontAwesomeIcons.BARS));
        checkCurrencyMenuItem =  new MenuItem(ContextVS.getMessage("checkCurrencyMenuItemLbl"));
        menuButton.getItems().addAll(checkCurrencyMenuItem);
        MenuItem changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> UserDeviceSelectorDialog.show(ContextVS.getMessage(
                "userVSDeviceConnected"), ContextVS.getMessage("selectDeviceToTransferCurrencyMsg"), WalletPane.this));;
        for(String currencyCode : currencyMap.keySet()) {
            Set<Currency> currencySet = currencyMap.get(currencyCode);
            VBox currencyPane = new VBox();
            currencyPane.getStyleClass().add("currency-pane");
            Label currencyLbl = new Label();
            currencyLbl.getStyleClass().add("currency");
            currencyLbl.setText(currencyCode);
            Map<String, Set<Currency>> tagMap = new HashMap<>();
            for(Currency currency : currencySet) {
                if(tagMap.containsKey(currency.getTag().getName())) tagMap.get(currency.getTag().getName()).add(currency);
                else tagMap.put(currency.getTag().getName(), new HashSet<>(Arrays.asList(currency)));
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
                    currencyHBox.setOnMouseClicked(mouseEvent -> CurrencyDialog.show(currency));
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

    private void load(Set<Currency> wallet) {
        checkCurrencyMenuItem.setOnAction(actionEvent -> {
            ProgressDialog.showDialog(new CurrencyCheckerTask(wallet, this));
        });
        for(Currency currency : wallet) {
            if(currencyMap.containsKey(currency.getCurrencyCode())) currencyMap.get(currency.getCurrencyCode()).add(currency);
            else currencyMap.put(currency.getCurrencyCode(), new HashSet<>(Arrays.asList(currency)));
        }
    }

    public MenuButton getMenuButton() {
        return menuButton;
    }

    public static void showDialog() {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                Set<Currency> currencySet = Wallet.getWallet();
                if(currencySet == null) {
                    PasswordDialog.showWithoutPasswordConfirm(TypeVS.CURRENCY_OPEN, passwordListener,
                            ContextVS.getMessage("walletPinMsg"));
                }
            }
        });
    }

    @Override public void setSelectedDevice(DeviceVSDto dto) {

    }

    @Override public void processCurrencyStatus(CurrencyCheckResponse response) {
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            showMessage(ResponseVS.SC_OK, ContextVS.getMessage("walletCheckResultOKMsg"));
        } else showMessage(ResponseVS.SC_ERROR, response.getMessage());
    }

    @Override
    public void setPassword(TypeVS passwordType, String password) {
        switch(passwordType) {
            case CURRENCY_OPEN:
                if(password != null) {
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
                break;
        }

    }

    private static PasswordDialog.Listener passwordListener = new PasswordDialog.Listener(){
        @Override public void setPassword(TypeVS passwordType, String password) {
            if(password != null) {
                try {
                    Set<Currency> currencySet = Wallet.getWallet(password);
                } catch (WalletException wex) {
                    Utils.showWalletNotFoundMessage();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                }
            }
        }
    };


    private static class WalletDialog extends DialogVS {

        private WalletPane walletPane;

        public WalletDialog() {
            super(new WalletPane());
            walletPane = (WalletPane) getContentPane();
        }

        private void show(Set<Currency> currencySet) {
            walletPane.load(currencySet);
            show();
        }
    }
}
