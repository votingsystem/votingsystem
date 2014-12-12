package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.cooin.model.CooinTransactionBatch;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinDialog implements DocumentVS,  JSONFormDialog.Listener, UserDeviceSelectorDialog.Listener {

    private static Logger log = Logger.getLogger(CooinDialog.class);

    @Override public void setSelectedDevice(JSONObject deviceDataJSON) {
        log.debug("setSelectedDevice - deviceDataJSON: " + deviceDataJSON.toString());
    }

    class EventBusDeleteCooinListener {
        @Subscribe public void responseVSChange(ResponseVS responseVS) {
            if(TypeVS.COOIN_DELETE == responseVS.getType()) {
                log.debug("EventBusDeleteCooinListener - COOIN_DELETE");
            }
        }
    }

    private Cooin cooin;
    private CooinServer cooinServer;
    @FXML private GridPane mainPane;
    @FXML private Label cooinServerLbl;
    @FXML private Label cooinHashLbl;
    @FXML private Label cooinValueLbl;
    @FXML private Label cooinTagLbl;
    @FXML private Label dateInfoLbl;
    @FXML private Label currencyLbl;
    @FXML private Label cooinStatusLbl;
    @FXML private ContextMenu contextMenu;
    @FXML private HBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLbl;
    @FXML private Label operationsLbl;
    private MessageDialog messageDialog;

    private MenuItem sendMenuItem;
    private MenuItem changeWalletMenuItem;
    private MenuItem deleteMenuItem;

    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(cooin.getCooinServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    cooinServer = (CooinServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            cooinServer.getCooinStatusServiceURL(cooin.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendMenuItem.setText(responseVS.getMessage());
                        sendMenuItem.setVisible(true);
                        contextMenu.getItems().removeAll(deleteMenuItem);
                    } else {
                        mainPane.getStyleClass().add("cooin-error");
                        cooinStatusLbl.setText(ContextVS.getMessage("invalidCooin"));
                        sendMenuItem.setVisible(false);
                        showMessage(responseVS.getMessage(), Boolean.FALSE);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public CooinDialog(Cooin cooin) throws IOException {
        this.cooin = cooin;
    }

    public Cooin getCooin() {
        return cooin;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        BrowserVS.getInstance().registerToEventBus(new EventBusDeleteCooinListener());
        sendMenuItem = new MenuItem("");
        sendMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        sendMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                showForm(new Cooin.TransactionVSData("", "", "", true).getJSON());
            }
        });
        deleteMenuItem = new MenuItem(ContextVS.getMessage("deleteLbl"));
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("deleteMenuItem");
                OperationVS operationVS = new OperationVS(TypeVS.COOIN_DELETE);
                operationVS.setMessage(cooin.getHashCertVS());
                BrowserVS.getInstance().processOperationVS(operationVS);
            }
        });
        MenuItem saveMenuItem = new MenuItem(ContextVS.getMessage("saveLbl"));
        saveMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("saveMenuItem");
            }
        });
        changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                UserDeviceSelectorDialog.show(ContextVS.getMessage("userVSDeviceConnected"),
                        ContextVS.getMessage("selectDeviceToTransferCooinMsg"), CooinDialog.this);
            }
        });

        contextMenu.getItems().addAll(sendMenuItem, changeWalletMenuItem, deleteMenuItem, saveMenuItem);
        contextMenu.show(cooinValueLbl, Side.BOTTOM, 0, 0);
        setProgressVisible(false, true);
        PlatformImpl.runLater(statusChecker);
        cooinServerLbl.setText(cooin.getCooinServerURL());
        cooinHashLbl.setText(cooin.getHashCertVS());
        cooinValueLbl.setText(cooin.getAmount().toPlainString());
        currencyLbl.setText(cooin.getCurrencyCode());
        cooinTagLbl.setText(Utils.getTagDescription(cooin.getTag().getName()));
        operationsLbl.setText(ContextVS.getMessage("operationsLbl"));
        operationsLbl.setGraphic(Utils.getImage(FontAwesome.Glyph.COGS, Utils.COLOR_RED));
        String cooinDateInfoLbl = ContextVS.getMessage("dateInfoLbl",
                DateUtils.getDateStr(cooin.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                DateUtils.getDateStr(cooin.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        dateInfoLbl.setText(cooinDateInfoLbl);
        operationsLbl.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    contextMenu.show(cooinValueLbl, Side.BOTTOM, 0, 0);
                }
            });
        try {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    ContextVS.getInstance().getCooinServer().getTrustAnchors(), false, Arrays.asList(
                            cooin.getCertificationRequest().getCertificate()));
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            log.debug("cooin issuer: " + certCaResult.getSubjectDN().toString());
        } catch(Exception ex) {
            log.debug(ex.getMessage(), ex);
            X509Certificate x509Cert = cooin.getX509AnonymousCert();
            String msg = null;
            if(x509Cert == null) msg = ContextVS.getMessage("cooinWithoutCertErrorMsg");
            else {
                String errorMsg =  null;
                if(Calendar.getInstance().getTime().after(x509Cert.getNotAfter())) {
                    errorMsg =  ContextVS.getMessage("cooinLapsedErrorLbl");
                } else errorMsg =  ContextVS.getMessage("cooinErrorLbl");
                String amountStr = cooin.getAmount() + " " + cooin.getCurrencyCode() + " " +
                        Utils.getTagForDescription(cooin.getTag().getName());
                msg = ContextVS.getMessage("cooinInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDateStr(cooin.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                        DateUtils.getDateStr(cooin.getValidTo()), "dd MMM yyyy' 'HH:mm");
            }
            showMessage(msg, Boolean.TRUE);
        }
    }

    public void showForm(JSONObject formData) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                JSONFormDialog formDialog = new JSONFormDialog();
                formDialog.showMessage(ContextVS.getMessage("enterReceptorMsg"), formData, CooinDialog.this);
            }
        });
    }

    public static void show(final Cooin cooin) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    CooinDialog cooinDialog = new CooinDialog(cooin);
                    Stage stage = new Stage();
                    stage.centerOnScreen();
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Cooin.fxml"));
                    fxmlLoader.setController(cooinDialog);
                        stage.setScene(new Scene(fxmlLoader.load()));
                    stage.setTitle("Cooin - " + cooin.getAmount().toPlainString() + " " + cooin.getCurrencyCode() +
                            " " + Utils.getTagForDescription(cooin.getTag().getName()));
                    stage.initModality(Modality.WINDOW_MODAL);
                    //stage.initOwner(((Node)event.getSource()).getScene().getWindow() );
                    stage.show();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void setProgressVisible(final boolean isProgressVisible, final boolean isSendItemVisible) {
        PlatformImpl.runLater(new Runnable() { @Override public void run() {
            progressBox.setVisible(isProgressVisible);
            sendMenuItem.setVisible(isSendItemVisible);
        }
        });
    }

    @Override public byte[] getDocumentBytes() throws Exception {
        return ObjectUtils.serializeObject(cooin);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.COOIN;
    }

    public void showMessage(final String message, Boolean isHtml) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                messageDialog = new MessageDialog();
                if(isHtml != null && isHtml == Boolean.TRUE) messageDialog.showHtmlMessage(message);
                else messageDialog.showMessage(null, message);
            }
        });
    }

    @Override public void processJSONForm(JSONObject jsonForm) {
        log.debug("processJSONForm: " + jsonForm.toString());
        Cooin.TransactionVSData transactionData = new Cooin.TransactionVSData(jsonForm);
        CooinTransactionBatch transactionBatch = new CooinTransactionBatch();
        transactionBatch.addCooin(cooin);
        try {
            setProgressVisible(true, false);
            Task transactionTask =  new Task() {
                @Override protected Object call() throws Exception {
                    updateProgress(1, 10);
                    updateMessage(ContextVS.getMessage("transactionInProgressMsg"));
                    transactionBatch.initTransactionVSRequest(transactionData.getToUserName(), transactionData.getToUserIBAN(),
                            transactionData.getSubject(), false, cooinServer.getTimeStampServiceURL());
                    updateProgress(3, 10);
                    ResponseVS responseVS = HttpHelper.getInstance().sendData(
                            transactionBatch.getTransactionVSRequest().toString().getBytes(),
                            ContentTypeVS.JSON, cooinServer.getCooinTransactionServiceURL());
                    updateProgress(8, 10);
                    log.debug("Cooin Transaction result: " + responseVS.getStatusCode());
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                    JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(responseVS.getMessage());
                    transactionBatch.validateTransactionVSResponse(responseJSON.getJSONArray("receiptList"),
                            cooinServer.getTrustAnchors());
                    Thread.sleep(3000);
                    setProgressVisible(false, false);
                    showMessage(responseJSON.getString("message"), Boolean.FALSE);
                    return true;
                }
            };
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(transactionTask.progressProperty());
            transactionTask.messageProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    PlatformImpl.runLater(new Runnable() { @Override public void run() { progressLbl.setText(newValue);}});
                }
            });
            new Thread(transactionTask).start();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            showMessage(ex.getMessage(), Boolean.FALSE);
            setProgressVisible(false, true);
        }
    }
}
