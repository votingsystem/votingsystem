package org.votingsystem.client.pane;

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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.dialog.CooinDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.dialog.UserDeviceSelectorDialog;
import org.votingsystem.client.util.CooinCheckResponse;
import org.votingsystem.client.util.CooinCheckerTask;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.Wallet;

import java.util.*;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletPane extends VBox implements UserDeviceSelectorDialog.Listener, CooinCheckerTask.Listener {

    private static Logger log = Logger.getLogger(WalletPane.class);

    private static Stage stage;
    private MenuButton menuButton;
    private MenuItem checkCooinsMenuItem;
    private Map<String, Set<Cooin>> currencyMap;

    public WalletPane(Set<Cooin> wallet) {
        getStylesheets().add(Utils.getResource("/css/wallet-pane.css"));
        getStyleClass().add("main-pane");
        VBox.setVgrow(this, Priority.ALWAYS);
        currencyMap = new HashMap<>();
        menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BARS));
        checkCooinsMenuItem =  new MenuItem(ContextVS.getMessage("checkCooinsMenuItemLbl"));
        checkCooinsMenuItem.setOnAction(actionEvent -> {
            ProgressDialog.showDialog(new CooinCheckerTask(wallet, this),
                    ContextVS.getMessage("checkingCooinsMsg"), stage);
        });
        menuButton.getItems().addAll(checkCooinsMenuItem);
        MenuItem changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> UserDeviceSelectorDialog.show(ContextVS.getMessage(
                "userVSDeviceConnected"), ContextVS.getMessage("selectDeviceToTransferCooinMsg"), WalletPane.this));;

        for(Cooin cooin : wallet) {
            if(currencyMap.containsKey(cooin.getCurrencyCode())) currencyMap.get(cooin.getCurrencyCode()).add(cooin);
            else currencyMap.put(cooin.getCurrencyCode(), new HashSet<>(Arrays.asList(cooin)));
        }
        for(String currencyCode : currencyMap.keySet()) {
            Set<Cooin> cooinSet = currencyMap.get(currencyCode);
            VBox currencyPane = new VBox();
            currencyPane.getStyleClass().add("currency-pane");
            Label currencyLbl = new Label();
            currencyLbl.getStyleClass().add("currency");
            currencyLbl.setText(currencyCode);
            Map<String, Set<Cooin>> tagMap = new HashMap<>();
            for(Cooin cooin: cooinSet) {
                if(tagMap.containsKey(cooin.getCertTagVS())) tagMap.get(cooin.getCertTagVS()).add(cooin);
                else tagMap.put(cooin.getCertTagVS(), new HashSet<>(Arrays.asList(cooin)));
            }
            currencyPane.getChildren().add(currencyLbl);
            for(String tag: tagMap.keySet()) {
                Set<Cooin> cooinTagSet = tagMap.get(tag);
                Integer tagAmount = cooinTagSet.stream().mapToInt(cooin -> cooin.getAmount().intValue()).sum();
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
                for(Cooin cooin: cooinTagSet) {
                    HBox cooinHBox = new HBox();
                    cooinHBox.setOnMouseClicked(mouseEvent -> CooinDialog.show(cooin, getScene().getWindow()));
                    cooinHBox.getStyleClass().add("cooinPane");
                    Label cooinValueLbl = new Label();
                    cooinValueLbl.getStyleClass().add("cooinValue");
                    cooinValueLbl.setText(cooin.getAmount().toPlainString() + " " + currencyCode);
                    cooinHBox.getChildren().add(cooinValueLbl);
                    tagFlowPane.getChildren().add(cooinHBox);
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
                    Set<Cooin> walletJSON = Wallet.getWallet();
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
                                log.error(ex.getMessage(), ex);
                                showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                            }
                        } else return;
                    }
                    WalletPane walletPane = new WalletPane(walletJSON);
                    if (stage == null) {
                        stage = new Stage(StageStyle.TRANSPARENT);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
                    }
                    stage.setScene(new Scene(new DecoratedPane(ContextVS.getMessage("walletLbl"),
                            walletPane.getMenuButton(), walletPane, stage)));
                    stage.getScene().setFill(null);
                    Utils.addMouseDragSupport(stage);
                    stage.centerOnScreen();
                    stage.toFront();
                    stage.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void setSelectedDevice(JSONObject deviceDataJSON) {

    }

    @Override public void processCooinStatus(CooinCheckResponse response) {
        if(ResponseVS.SC_OK == response.getStatusCode()) {
            showMessage(ResponseVS.SC_OK, ContextVS.getMessage("walletCheckResultOKMsg"));
        } else showMessage(ResponseVS.SC_ERROR, response.getMessage());
    }

}
