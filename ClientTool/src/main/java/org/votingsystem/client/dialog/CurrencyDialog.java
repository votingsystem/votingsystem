package org.votingsystem.client.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.service.EventBusService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.util.*;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyTransactionBatch;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.*;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyDialog implements DocumentVS, JSONFormDialog.Listener, UserDeviceSelectorDialog.Listener {

    private static Logger log = Logger.getLogger(CurrencyDialog.class.getSimpleName());

    class EventBusCurrencyListener {
        @Subscribe public void responseVSChange(ResponseVS responseVS) {
            switch(responseVS.getType()) {
                case CURRENCY_WALLET_CHANGE:
                case MESSAGEVS_FROM_VS:
                    if(walletChangeTask != null) walletChangeTask.update(responseVS);
                    break;
            }
        }
    }

    private Currency currency;
    private CurrencyServer currencyServer;
    private static Stage stage;

    @FXML private MenuButton menuButton;
    @FXML private Button closeButton;
    @FXML private Label serverLbl;
    @FXML private VBox mainPane;
    @FXML private VBox content;
    @FXML private TextField currencyHashText;
    @FXML private Label currencyValueLbl;
    @FXML private Label currencyTagLbl;
    @FXML private Label validFromLbl;
    @FXML private Label validToLbl;
    @FXML private Label currencyLbl;
    @FXML private Label currencyStatusLbl;

    private MenuItem sendMenuItem;
    private MenuItem changeWalletMenuItem;
    private WalletChangeTask walletChangeTask;

    private Runnable statusChecker = new Runnable() {
        @Override public void run() {
            try {
                ResponseVS responseVS = Utils.checkServer(currency.getCurrencyServerURL());
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    currencyServer = (CurrencyServer) responseVS.getData();
                    responseVS = HttpHelper.getInstance().getData(
                            currencyServer.getCurrencyStateServiceURL(currency.getHashCertVS()), null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        sendMenuItem.setText(responseVS.getMessage());
                        sendMenuItem.setVisible(true);
                    } else {
                        mainPane.getStyleClass().add("currency-error");
                        currencyStatusLbl.setText(ContextVS.getMessage("invalidCurrency"));
                        sendMenuItem.setVisible(false);
                        showMessage(ResponseVS.SC_ERROR, responseVS.getMessage());
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    };

    public CurrencyDialog(Currency currency) throws IOException {
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.info("initialize");
        EventBusService.getInstance().register(new EventBusCurrencyListener());
        closeButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
        closeButton.setOnAction(actionEvent -> stage.close());
        sendMenuItem = new MenuItem("");
        sendMenuItem.setOnAction(actionEvent -> JSONFormDialog.show(
                new Currency.TransactionVSData("", "", "", true).getJSON(), CurrencyDialog.this));
        MenuItem saveMenuItem = new MenuItem(ContextVS.getMessage("saveLbl"));
        saveMenuItem.setOnAction(actionEvent -> System.out.println("saveMenuItem"));
        changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> {
            if(WebSocketAuthenticatedService.getInstance().isConnectedWithAlert()) {
                UserDeviceSelectorDialog.show(ContextVS.getMessage("userVSDeviceConnected"),
                        ContextVS.getMessage("selectDeviceToTransferCurrencyMsg"), CurrencyDialog.this);
            }
        });
        PlatformImpl.runLater(statusChecker);
        serverLbl.setText(currency.getCurrencyServerURL().split("//")[1]);
        currencyHashText.setText(currency.getHashCertVS());
        currencyValueLbl.setText(currency.getAmount().toPlainString());
        currencyLbl.setText(currency.getCurrencyCode());
        currencyTagLbl.setText(MsgUtils.getTagDescription(currency.getCertTagVS()));
        menuButton.setGraphic(Utils.getIcon(FontAwesomeIconName.BARS));
        validFromLbl.setText(ContextVS.getMessage("issuedLbl") + ": " +
                DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"));
        validToLbl.setText(ContextVS.getMessage("expiresLbl") + ": " +
                DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        menuButton.getItems().addAll(sendMenuItem, changeWalletMenuItem);
        try {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    ContextVS.getInstance().getCurrencyServer().getTrustAnchors(), false, Arrays.asList(
                            currency.getCertificationRequest().getCertificate()));
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            log.info("currency issuer: " + certCaResult.getSubjectDN().toString());
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            X509Certificate x509Cert = currency.getX509AnonymousCert();
            String msg = null;
            if(x509Cert == null) msg = ContextVS.getMessage("currencyWithoutCertErrorMsg");
            else {
                String errorMsg =  null;
                if(new Date().after(x509Cert.getNotAfter())) {
                    errorMsg =  ContextVS.getMessage("currencyLapsedErrorLbl");
                } else errorMsg =  ContextVS.getMessage("currencyErrorLbl");
                String amountStr = currency.getAmount() + " " + currency.getCurrencyCode() + " " +
                        Utils.getTagForDescription(currency.getCertTagVS());
                msg = ContextVS.getMessage("currencyInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                        DateUtils.getDateStr(currency.getValidTo()), "dd MMM yyyy' 'HH:mm");
            }
            showMessage(msg, ContextVS.getMessage("errorLbl"));
        }
    }

    public static void show(final Currency currency, Window owner) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    CurrencyDialog currencyDialog = new CurrencyDialog(currency);
                    if(stage == null) {
                        stage = new Stage(StageStyle.TRANSPARENT);
                        stage.initOwner(owner);
                        stage.getIcons().add(Utils.getIconFromResources(Utils.APPLICATION_ICON));
                    }
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Currency.fxml"));
                    fxmlLoader.setController(currencyDialog);
                    stage.setScene(new Scene(fxmlLoader.load()));
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

    @Override public byte[] getDocumentBytes() throws Exception {
        return ObjectUtils.serializeObject(currency);
    }

    @Override public ContentTypeVS getContentTypeVS() {
        return ContentTypeVS.CURRENCY;
    }

    @Override public void processJSONForm(Map dataMap) {
        log.info("processJSONForm: " + dataMap.toString());
        ProgressDialog.showDialog(new ProcessFormTask(new Currency.TransactionVSData(dataMap), currency, currencyServer),
                ContextVS.getMessage("sendingMoneyLbl"), stage);
    }

    @Override public void setSelectedDevice(Map dataMap) {
        log.info("setSelectedDevice - deviceDataJSON: " + dataMap.toString());
        walletChangeTask = new WalletChangeTask(dataMap);
        ProgressDialog.showDialog(walletChangeTask, ContextVS.getMessage("changeWalletLbl"), stage);
    }

    public  class WalletChangeTask extends Task<ResponseVS> {

        private Map dataMap;
        private AtomicBoolean isCancelled = new AtomicBoolean();
        private CountDownLatch countDownLatch;

        public WalletChangeTask(Map dataMap) {
            this.dataMap = dataMap;
        }

        @Override protected ResponseVS call() throws Exception {
            updateMessage(ContextVS.getMessage("sendingDataLbl"));
            updateProgress(1, 10);
            try {
                countDownLatch = new CountDownLatch(1);
                DeviceVS deviceVS = DeviceVS.parse(dataMap);
                WebSocketAuthenticatedService.getInstance().sendMessage(WebSocketMessage.getCurrencyWalletChangeRequest(
                        deviceVS, Arrays.asList(currency)).toString());
                countDownLatch.await();
                WebSocketSession webSocketSession = VotingSystemApp.getInstance().getWSSession(deviceVS.getId());
            } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return null;
        }

        public void update(ResponseVS responseVS) {
            log.info("WalletChangeTask - update");
            countDownLatch.countDown();
        }
    }

    public static class ProcessFormTask extends Task<ResponseVS> {

        private Currency.TransactionVSData transactionData;
        private CurrencyTransactionBatch transactionBatch;
        private Currency currency;
        private CurrencyServer currencyServer;

        public ProcessFormTask(Currency.TransactionVSData transactionData, Currency currency, CurrencyServer currencyServer) {
            this.currency = currency;
            this.currencyServer = currencyServer;
            this.transactionData = transactionData;
            this.transactionBatch = new CurrencyTransactionBatch(Arrays.asList(currency));
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("transactionInProgressMsg"));
            Map dataMap =  transactionBatch.getTransactionVSRequest(TypeVS.CURRENCY_SEND,
                    Payment.ANONYMOUS_SIGNED_TRANSACTION, transactionData.getSubject(),
                    transactionData.getToUserIBAN(), currency.getAmount(), currency.getCurrencyCode(),
                    currency.getTag().getName(), false, currencyServer.getTimeStampServiceURL());
            updateProgress(3, 10);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(new ObjectMapper().writeValueAsString(dataMap).getBytes(),
                    ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
            updateProgress(8, 10);
            log.info("transaction result: " + responseVS.getStatusCode());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                showMessage(responseVS);
            } else {
                Map<String, Object> responseDataMap = new ObjectMapper().readValue(responseVS.getMessage(),
                        new TypeReference<HashMap<String, Object>>() {});
                transactionBatch.validateTransactionVSResponse(responseDataMap, currencyServer.getTrustAnchors());
                showMessage(ResponseVS.SC_OK, (String) responseDataMap.get("message"));
            }
            return responseVS;
        }
    }
}
