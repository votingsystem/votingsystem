package org.votingsystem.client.pane;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
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
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.CooinDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.util.ResizeHelper;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.Wallet;

import java.io.IOException;
import java.util.*;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletPane extends VBox {

    private static Logger log = Logger.getLogger(WalletPane.class);

    private static Stage stage;
    private Map<String, Set<JSONObject>> currencyMap;

    public WalletPane(JSONArray walletJSON) {
        getStylesheets().add(Utils.getResource("/css/wallet-pane.css"));
        getStyleClass().add("main-pane");
        VBox.setVgrow(this, Priority.ALWAYS);
        currencyMap = new HashMap<>();
        for(int i = 0; i < walletJSON.size(); i++) {
            JSONObject cooin = (JSONObject) walletJSON.get(i);
            String currencyCode = cooin.getString("currencyCode");
            if(currencyMap.containsKey(currencyCode)) currencyMap.get(currencyCode).add(cooin);
            else currencyMap.put(currencyCode, new HashSet<>(Arrays.asList(cooin)));
        }
        for(String currencyCode : currencyMap.keySet()) {
            Set<JSONObject> cooinSet = currencyMap.get(currencyCode);
            VBox currencyPane = new VBox();
            currencyPane.getStyleClass().add("currency-pane");
            Label currencyLbl = new Label();
            currencyLbl.getStyleClass().add("currency");
            currencyLbl.setText(currencyCode);
            Map<String, Set<JSONObject>> tagMap = new HashMap<>();
            for(JSONObject cooin: cooinSet) {
                String tag = cooin.getString("tag");
                if(tagMap.containsKey(tag)) tagMap.get(tag).add(cooin);
                else tagMap.put(tag, new HashSet<>(Arrays.asList(cooin)));
            }
            for(String tag: tagMap.keySet()) {
                Set<JSONObject> cooinTagSet = tagMap.get(tag);
                Integer tagAmount = cooinTagSet.stream().mapToInt(cooin -> cooin.getInt("cooinValue")).sum();
                Label tagLbl = new Label();
                tagLbl.getStyleClass().add("tag");
                tagLbl.setText(tag);
                Label amountLbl = new Label();
                amountLbl.getStyleClass().add("amount");
                amountLbl.setText(tagAmount.toString() + " " + currencyCode);
                HBox tagInfoBox = new HBox();
                tagInfoBox.setAlignment(Pos.CENTER);
                tagInfoBox.setSpacing(15);
                tagInfoBox.getChildren().addAll(tagLbl, amountLbl);
                FlowPane tagFlowPane = new FlowPane();
                tagFlowPane.setHgap(10);
                tagFlowPane.setVgap(10);
                for(JSONObject cooin: cooinTagSet) {
                    HBox cooinHBox = new HBox();
                    cooinHBox.setOnMouseClicked(mouseEvent -> CooinDialog.show((Cooin) ObjectUtils.deSerializeObject((
                            (String) cooin.getString("object")).getBytes()), getScene().getWindow()));
                    cooinHBox.getStyleClass().add("cooinPane");
                    Label cooinValueLbl = new Label();
                    cooinValueLbl.getStyleClass().add("cooinValue");
                    cooinValueLbl.setText(cooin.getString("cooinValue") + " " + currencyCode);
                    cooinHBox.getChildren().add(cooinValueLbl);
                    tagFlowPane.getChildren().add(cooinHBox);
                }
                currencyPane.getChildren().addAll(currencyLbl, tagInfoBox, tagFlowPane);
                getChildren().add(currencyPane);
            }
        }
    }

    public static void showDialog(Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    JSONArray walletJSON = Wallet.getWallet();
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
                        }
                    }
                    WalletPane walletPane = new WalletPane(walletJSON);
                    if (stage == null) {
                        stage = new Stage(StageStyle.TRANSPARENT);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
                    }
                    stage.setScene(new Scene(new DecoratedPane(ContextVS.getMessage("walletLbl"), null, walletPane, stage)));
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

}
