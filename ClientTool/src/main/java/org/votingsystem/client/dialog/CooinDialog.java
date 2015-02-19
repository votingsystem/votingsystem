package org.votingsystem.client.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.service.NotificationService;
import org.votingsystem.client.util.DocumentVS;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.cooin.model.CooinTransactionBatch;
import org.votingsystem.cooin.model.Payment;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinDialog implements DocumentVS, JSONFormDialog.Listener, UserDeviceSelectorDialog.Listener {

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
    private static Stage stage;

    @FXML private MenuButton menuButton;
    @FXML private Button closeButton;
    @FXML private Label serverLbl;
    @FXML private VBox mainPane;
    @FXML private VBox content;
    @FXML private Label cooinHashLbl;
    @FXML private Label cooinValueLbl;
    @FXML private Label cooinTagLbl;
    @FXML private Label validFromLbl;
    @FXML private Label validToLbl;
    @FXML private Label currencyLbl;
    @FXML private Label cooinStatusLbl;

    private MenuItem sendMenuItem;
    private MenuItem changeWalletMenuItem;

    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(cooin.getCooinServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    cooinServer = (CooinServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            cooinServer.getCooinStateServiceURL(cooin.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendMenuItem.setText(responseVS.getMessage());
                        sendMenuItem.setVisible(true);
                    } else {
                        mainPane.getStyleClass().add("cooin-error");
                        cooinStatusLbl.setText(ContextVS.getMessage("invalidCooin"));
                        sendMenuItem.setVisible(false);
                        showMessage(ResponseVS.SC_ERROR, responseVS.getMessage());
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
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
        NotificationService.getInstance().registerToEventBus(new EventBusDeleteCooinListener());
        closeButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> stage.close());
        sendMenuItem = new MenuItem("");
        sendMenuItem.setOnAction(actionEvent -> JSONFormDialog.show(
                new Cooin.TransactionVSData("", "", "", true).getJSON(), CooinDialog.this));
        MenuItem saveMenuItem = new MenuItem(ContextVS.getMessage("saveLbl"));
        saveMenuItem.setOnAction(actionEvent -> System.out.println("saveMenuItem"));
        changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> UserDeviceSelectorDialog.show(ContextVS.getMessage(
                "userVSDeviceConnected"), ContextVS.getMessage("selectDeviceToTransferCooinMsg"), CooinDialog.this));
        PlatformImpl.runLater(statusChecker);
        serverLbl.setText(cooin.getCooinServerURL().split("//")[1]);
        cooinHashLbl.setText(cooin.getHashCertVS());
        cooinValueLbl.setText(cooin.getAmount().toPlainString());
        currencyLbl.setText(cooin.getCurrencyCode());
        cooinTagLbl.setText(MsgUtils.getTagDescription(cooin.getCertTagVS()));
        menuButton.setGraphic(Utils.getImage(FontAwesome.Glyph.BARS));
        validFromLbl.setText(ContextVS.getMessage("issuedLbl") + ": " +
                DateUtils.getDateStr(cooin.getValidFrom(), "dd MMM yyyy' 'HH:mm"));
        validToLbl.setText(ContextVS.getMessage("expiresLbl") + ": " +
                DateUtils.getDateStr(cooin.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        menuButton.getItems().addAll(sendMenuItem, changeWalletMenuItem);
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
                        Utils.getTagForDescription(cooin.getCertTagVS());
                msg = ContextVS.getMessage("cooinInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDateStr(cooin.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                        DateUtils.getDateStr(cooin.getValidTo()), "dd MMM yyyy' 'HH:mm");
            }
            showMessage(ResponseVS.SC_ERROR, msg);
        }
    }

    public static void show(final Cooin cooin, Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    CooinDialog cooinDialog = new CooinDialog(cooin);
                    if(stage == null) {
                        stage = new Stage(StageStyle.TRANSPARENT);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getImageFromResources(Utils.APPLICATION_ICON));
                    }
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Cooin.fxml"));
                    fxmlLoader.setController(cooinDialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
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

    @Override public byte[] getDocumentBytes() throws Exception {
        return ObjectUtils.serializeObject(cooin);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.COOIN;
    }

    @Override public void processJSONForm(JSONObject jsonForm) {
        log.debug("processJSONForm: " + jsonForm.toString());
        ProgressDialog.showDialog(new ProcessFormTask(new Cooin.TransactionVSData(jsonForm), cooin,cooinServer),
                ContextVS.getMessage("sendingMoneyLbl"), stage);
    }

    public static class ProcessFormTask extends Task<ResponseVS> {

        private Cooin.TransactionVSData transactionData;
        private CooinTransactionBatch transactionBatch;
        private Cooin cooin;
        private CooinServer cooinServer;

        public ProcessFormTask(Cooin.TransactionVSData transactionData, Cooin cooin, CooinServer cooinServer) {
            this.cooin = cooin;
            this.cooinServer = cooinServer;
            this.transactionData = transactionData;
            this.transactionBatch = new CooinTransactionBatch(Arrays.asList(cooin));
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("transactionInProgressMsg"));
            JSONObject requestJSON =  transactionBatch.getTransactionVSRequest(TypeVS.COOIN_SEND,
                    Payment.ANONYMOUS_SIGNED_TRANSACTION, transactionData.getSubject(),
                    transactionData.getToUserIBAN(), cooin.getAmount(), cooin.getCurrencyCode(),
                    cooin.getTag().getName(), false, cooinServer.getTimeStampServiceURL());
            updateProgress(3, 10);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(requestJSON.toString().getBytes(),
                    ContentTypeVS.JSON, cooinServer.getCooinTransactionServiceURL());
            updateProgress(8, 10);
            log.debug("transaction result: " + responseVS.getStatusCode());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                showMessage(responseVS);
            } else {
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(responseVS.getMessage());
                transactionBatch.validateTransactionVSResponse(responseJSON, cooinServer.getTrustAnchors());
                showMessage(ResponseVS.SC_OK, responseJSON.getString("message"));
            }
            return responseVS;
        }
    }
}
