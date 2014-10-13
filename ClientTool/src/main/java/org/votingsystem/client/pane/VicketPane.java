package org.votingsystem.client.pane;

import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
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
public class VicketPane extends GridPane implements DocumentVS, JSONFormDialog.Listener {

    private static Logger log = Logger.getLogger(VicketPane.class);

    private Vicket vicket;
    private VicketServer vicketServer;
    private Label vicketStatusLbl;
    private Button sendVicketButton;
    private MessageDialog messageDialog;
    private HBox progressBox;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(vicket.getVicketServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    vicketServer = (VicketServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            vicketServer.getVicketStatusServiceURL(vicket.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendVicketButton.setText(responseVS.getMessage());
                        sendVicketButton.setVisible(true);
                    } else {
                        vicketStatusLbl.getStyleClass().add("statusLbl");
                        vicketStatusLbl.setText(responseVS.getMessage());
                        sendVicketButton.setVisible(false);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public VicketPane(final Vicket vicket) throws ExceptionVS {
        super();
        this.vicket = vicket;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10, 10, 10, 10));

        vicketStatusLbl = new Label();
        vicketStatusLbl.setWrapText(true);
        setHalignment(vicketStatusLbl, HPos.CENTER);

        Label serverLbl = new Label(vicket.getCertSubject().getVicketServerURL());
        Label hashLbl = new Label(vicket.getHashCertVS());
        serverLbl.getStyleClass().add("server");
        hashLbl.getStyleClass().add("server");

        Label vicketValueLbl = new Label(vicket.getAmount().toString() + " " + vicket.getCurrencyCode());
        vicketValueLbl.getStyleClass().add("vicketValue");
        setHalignment(vicketValueLbl, HPos.CENTER);
        Label vicketTagLbl = new Label(Utils.getTagForDescription(vicket.getTag().getName()));
        vicketTagLbl.getStyleClass().add("tag");
        setHalignment(vicketTagLbl, HPos.CENTER);

        sendVicketButton = new Button();
        sendVicketButton.setGraphic(new ImageView(Utils.getImage(this, "accept")));
        sendVicketButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                log.debug("sendVicketButton");
                showForm(new Vicket.TransactionVSData("", "", "", true).getJSON());
            }
        });
        sendVicketButton.setPrefWidth(200);
        sendVicketButton.setVisible(false);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        ColumnConstraints column2 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2);
        setHalignment(sendVicketButton, HPos.RIGHT);

        Label vicketDateInfoLbl = new Label(ContextVS.getMessage("dateInfoLbl", DateUtils.getDayWeekDateStr(vicket.getValidFrom()),
                DateUtils.getDayWeekDateStr(vicket.getValidTo())));
        vicketDateInfoLbl.getStyleClass().add("dateInfo");

        progressLabel = new Label();
        progressLabel.setStyle("-fx-font-size: 12;-fx-font-weight: bold;");
        progressBar = new ProgressBar(0);

        progressBox = new HBox();
        progressBox.setSpacing(5);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.getChildren().addAll(progressLabel, progressBar);
        progressBox.setVisible(false);

        add(serverLbl, 0, 0);
        add(hashLbl, 1,0);
        add(vicketStatusLbl, 0,1);
        add(vicketValueLbl, 0, 2);
        add(vicketTagLbl, 0, 3);
        add(vicketDateInfoLbl, 0, 5);
        add(progressBox, 0, 6);
        add(sendVicketButton, 1, 6);
        setColumnSpan(vicketDateInfoLbl, 2);
        setColumnSpan(vicketValueLbl, 2);
        setColumnSpan(vicketTagLbl, 2);
        setColumnSpan(vicketStatusLbl, 2);
        setColumnSpan(vicketDateInfoLbl, 2);
        setColumnSpan(progressBox, 2);

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

        PlatformImpl.runLater(statusChecker);
    }

    public void showMessage(final String message, Boolean isHtml) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                if (messageDialog == null) messageDialog = new MessageDialog();
                if(isHtml != null && isHtml == Boolean.TRUE) messageDialog.showHtmlMessage(message);
                else messageDialog.showMessage(message);
            }
        });
    }

    public void showForm(JSONObject formData) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                JSONFormDialog formDialog = new JSONFormDialog();
                formDialog.showMessage(ContextVS.getMessage("enterReceptorMsg"), formData, VicketPane.this);
            }
        });
    }

    public Vicket getVicket () {
        return vicket;
    }

    private void setProgressVisible(final boolean isProgressVisible, final boolean isButtonVisible) {
        PlatformImpl.runLater(new Runnable() { @Override public void run() {
            sendVicketButton.setVisible(isButtonVisible);
            progressBox.setVisible(isProgressVisible);  }
        });
    }

    @Override public byte[] getDocumentBytes() throws Exception {
        return ObjectUtils.serializeObject(vicket);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.VICKET;
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
                    PlatformImpl.runLater(new Runnable() { @Override public void run() { progressLabel.setText(newValue);}});
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