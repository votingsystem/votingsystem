package org.votingsystem.client.controller;

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
import org.votingsystem.client.dialog.JSONFormDialog;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.Utils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.vicket.model.Vicket;
import org.votingsystem.vicket.model.VicketTransactionBatch;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketPaneController  implements DocumentVS,  JSONFormDialog.Listener {

    private static Logger log = Logger.getLogger(VicketPaneController.class);

    private Vicket vicket;
    private VicketServer vicketServer;
    @FXML private GridPane mainPane;
    @FXML private Label vicketServerLbl;
    @FXML private Label vicketHashLbl;
    @FXML private Label vicketValueLbl;
    @FXML private Label vicketTagLbl;
    @FXML private Label dateInfoLbl;
    @FXML private Label currencyLbl;
    @FXML private Label vicketStatusLbl;
    @FXML private ContextMenu contextMenu;
    @FXML private HBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLbl;
    @FXML private Label operationsLbl;
    private MessageDialog messageDialog;

    private MenuItem sendMenuItem;

    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(vicket.getVicketServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    vicketServer = (VicketServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            vicketServer.getVicketStatusServiceURL(vicket.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendMenuItem.setText(responseVS.getMessage());
                        sendMenuItem.setVisible(true);
                    } else {
                        vicketStatusLbl.getStyleClass().add("statusLbl");
                        vicketStatusLbl.setText(responseVS.getMessage());
                        sendMenuItem.setVisible(false);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public VicketPaneController(Vicket vicket) {
        this.vicket = vicket;
    }

    public Vicket getVicket () {
        return vicket;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.debug("initialize");
        sendMenuItem = new MenuItem("");
        sendMenuItem.setGraphic(Utils.getImage(FontAwesome.Glyph.CHECK));
        sendMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                showForm(new Vicket.TransactionVSData("", "", "", true).getJSON());
            }
        });
        MenuItem saveMenuItem = new MenuItem(ContextVS.getMessage("saveLbl"));
        saveMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("saveMenuItem");
            }
        });
        contextMenu.getItems().addAll(sendMenuItem, saveMenuItem);
        contextMenu.show(vicketValueLbl, Side.BOTTOM, 0, 0);
        setProgressVisible(false, true);
        PlatformImpl.runLater(statusChecker);
        vicketServerLbl.setText(vicket.getVicketServerURL());
        vicketHashLbl.setText(vicket.getHashCertVS());
        vicketValueLbl.setText(vicket.getAmount().toPlainString());
        currencyLbl.setText(vicket.getCurrencyCode());
        vicketTagLbl.setText(vicket.getTag().getName());
        operationsLbl.setText(ContextVS.getMessage("operationsLbl"));
        operationsLbl.setGraphic(Utils.getImage(FontAwesome.Glyph.COGS, Utils.COLOR_RED_DARK));
        String vicketDateInfoLbl = ContextVS.getMessage("dateInfoLbl", DateUtils.getDayWeekDateStr(vicket.getValidFrom()),
                DateUtils.getDayWeekDateStr(vicket.getValidTo()));
        dateInfoLbl.setText(vicketDateInfoLbl);
        operationsLbl.setOnMouseClicked(new EventHandler<MouseEvent>() {
                 @Override public void handle(MouseEvent event) {
                     contextMenu.show(vicketValueLbl, Side.BOTTOM, 0, 0);
                 }
             }
        );
        try {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    ContextVS.getInstance().getVicketServer().getTrustAnchors(), false, Arrays.asList(
                            vicket.getCertificationRequest().getCertificate()));
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            log.debug("VicketPane. Vicket issuer: " + certCaResult.getSubjectDN().toString());
        } catch(Exception ex) {
            log.debug(ex.getMessage(), ex);
            X509Certificate x509Cert = vicket.getX509AnonymousCert();
            String msg = null;
            if(x509Cert == null) msg = ContextVS.getMessage("vicketWithouCertErrorMsg");
            else {
                String errorMsg =  null;
                if(Calendar.getInstance().getTime().after(x509Cert.getNotAfter())) {
                    errorMsg =  ContextVS.getMessage("vicketLapsedErrorLbl");
                } else errorMsg =  ContextVS.getMessage("vicketErrorLbl");
                String amountStr = vicket.getAmount() + " " + vicket.getCurrencyCode() + " " +
                        Utils.getTagForDescription(vicket.getTag().getName());
                msg = ContextVS.getMessage("vicketInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDayWeekDateStr(vicket.getValidFrom()), DateUtils.getDayWeekDateStr(vicket.getValidTo()));
            }
            showMessage(msg, Boolean.TRUE);
        }
    }

    public void showForm(JSONObject formData) {
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                JSONFormDialog formDialog = new JSONFormDialog();
                formDialog.showMessage(ContextVS.getMessage("enterReceptorMsg"), formData, VicketPaneController.this);
            }
        });
    }

    public static void show(final Vicket vicket) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    VicketPaneController vicketPaneController = new VicketPaneController(vicket);
                    Stage stage = new Stage();
                    stage.centerOnScreen();
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Vicket.fxml"));
                    fxmlLoader.setController(vicketPaneController);
                        stage.setScene(new Scene(fxmlLoader.load()));
                    stage.setTitle("Vicket - " + vicket.getAmount().toPlainString() + " " + vicket.getCurrencyCode() +
                            " " + Utils.getTagForDescription(vicket.getTag().getName()));
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
        return ObjectUtils.serializeObject(vicket);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.VICKET;
    }

    public void showMessage(final String message, Boolean isHtml) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                messageDialog = new MessageDialog();
                if(isHtml != null && isHtml == Boolean.TRUE) messageDialog.showHtmlMessage(message);
                else messageDialog.showMessage(message);
            }
        });
    }

    @Override public void processJSONForm(JSONObject jsonForm) {
        log.debug("processJSONForm: " + jsonForm.toString());
        Vicket.TransactionVSData transactionData = new Vicket.TransactionVSData(jsonForm);
        VicketTransactionBatch transactionBatch = new VicketTransactionBatch();
        transactionBatch.addVicket(vicket);
        try {
            setProgressVisible(true, false);
            Task transactionTask =  new Task() {
                @Override protected Object call() throws Exception {
                    updateProgress(1, 10);
                    updateMessage(ContextVS.getMessage("transactionInProgressMsg"));
                    transactionBatch.initTransactionVSRequest(transactionData.getToUserName(), transactionData.getToUserIBAN(),
                            transactionData.getSubject(), false, vicketServer.getTimeStampServiceURL());
                    updateProgress(3, 10);
                    ResponseVS responseVS = HttpHelper.getInstance().sendData(transactionBatch.getTransactionVSRequest().toString().getBytes(),
                            ContentTypeVS.JSON, vicketServer.getVicketTransactionServiceURL());
                    updateProgress(8, 10);
                    log.debug("Vicket Transaction result: " + responseVS.getStatusCode());
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                    JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(responseVS.getMessage());
                    transactionBatch.validateTransactionVSResponse(responseJSON.getJSONArray("receiptList"), vicketServer.getTrustAnchors());
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
