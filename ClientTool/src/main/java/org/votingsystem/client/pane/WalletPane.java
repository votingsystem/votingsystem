package org.votingsystem.client.pane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.votingsystem.client.dialog.CurrencyDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.dialog.UserDeviceSelectorDialog;
import org.votingsystem.client.util.CurrencyCheckResponse;
import org.votingsystem.client.util.CurrencyCheckerTask;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.Wallet;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletPane extends VBox implements UserDeviceSelectorDialog.Listener, CurrencyCheckerTask.Listener {

    private static Logger log = Logger.getLogger(WalletPane.class.getSimpleName());

    private static Stage stage;
    private MenuButton menuButton;
    private MenuItem checkCurrencyMenuItem;
    private Map<String, Set<Currency>> currencyMap;

    public WalletPane(Set<Currency> wallet) {
        getStylesheets().add(Utils.getResource("/css/wallet-pane.css"));
        getStyleClass().add("main-pane");
        VBox.setVgrow(this, Priority.ALWAYS);
        currencyMap = new HashMap<>();
        menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getIcon(FontAwesomeIconName.BARS));
        checkCurrencyMenuItem =  new MenuItem(ContextVS.getMessage("checkCurrencyMenuItemLbl"));
        checkCurrencyMenuItem.setOnAction(actionEvent -> {
            ProgressDialog.showDialog(new CurrencyCheckerTask(wallet, this),
                    ContextVS.getMessage("checkingCurrencyMsg"), stage);
        });
        menuButton.getItems().addAll(checkCurrencyMenuItem);
        MenuItem changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> UserDeviceSelectorDialog.show(ContextVS.getMessage(
                "userVSDeviceConnected"), ContextVS.getMessage("selectDeviceToTransferCurrencyMsg"), WalletPane.this));;

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
                if(tagMap.containsKey(currency.getCertTagVS())) tagMap.get(currency.getCertTagVS()).add(currency);
                else tagMap.put(currency.getCertTagVS(), new HashSet<>(Arrays.asList(currency)));
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
                    currencyHBox.setOnMouseClicked(mouseEvent -> CurrencyDialog.show(currency, getScene().getWindow()));
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

    public MenuButton getMenuButton() {
        return menuButton;
    }

    public static void showDialog(Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    Set<Currency> walletJSON = Wallet.getWallet();
                    if(walletJSON == null) {
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                        String password = passwordDialog.getPassword();
                        if(password != null) {
                            try {
                                walletJSON = Wallet.getWallet(password);
                            } catch (WalletException wex) {
                                Utils.showWalletNotFoundMessage();
                            } catch (Exception ex) {
                                log.log(Level.SEVERE, ex.getMessage(), ex);
                                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                            }
                        } else return;
                    }
                    WalletPane walletPane = new WalletPane(walletJSON);
                    if (stage == null) {
                        stage = new Stage(StageStyle.TRANSPARENT);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
                    }
                    stage.setScene(new Scene(new DecoratedPane(ContextVS.getMessage("walletLbl"),
                            walletPane.getMenuButton(), walletPane, stage)));
                    stage.getScene().setFill(null);
                    Utils.addMouseDragSupport(stage);
                    stage.centerOnScreen();
                    stage.toFront();
                    stage.show();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void setSelectedDevice(Map deviceDataMap) {

    }

    @Override public void processCurrencyStatus(CurrencyCheckResponse response) {
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            showMessage(ResponseVS.SC_OK, ContextVS.getMessage("walletCheckResultOKMsg"));
        } else showMessage(ResponseVS.SC_ERROR, response.getMessage());
    }

}
