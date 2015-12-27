package org.votingsystem.client.webextension.dialog;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.service.EventBusService;
import org.votingsystem.client.webextension.service.WebSocketAuthenticatedService;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.*;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyDialog extends DialogVS implements UserDeviceSelectorDialog.Listener {

    private static Logger log = Logger.getLogger(CurrencyDialog.class.getSimpleName());

    class EventBusCurrencyListener {
        @Subscribe public void call(SocketMessageDto socketMessage) {
            switch(socketMessage.getOperation()) {
                case CURRENCY_WALLET_CHANGE:
                    if(walletChangeTask != null) walletChangeTask.update(socketMessage);
                    break;
            }
        }
    }

    private Currency currency;
    private CurrencyServer currencyServer;

    @FXML private Label serverLbl;
    @FXML private VBox mainPane;
    @FXML private TextField currencyHashText;
    @FXML private Label currencyValueLbl;
    @FXML private Label currencyTagLbl;
    @FXML private Label validFromLbl;
    @FXML private Label validToLbl;
    @FXML private Label currencyLbl;


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
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                        currency.setState(Currency.State.ERROR);
                        currency.setReason(responseVS.getReason());
                        update();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    };

    public CurrencyDialog() throws IOException {
        super("/fxml/Currency.fxml");
        changeWalletMenuItem =  new MenuItem(ContextVS.getMessage("changeWalletLbl"));
        changeWalletMenuItem.setOnAction(actionEvent -> {
            if(WebSocketAuthenticatedService.getInstance().isConnectedWithAlert()) {
                UserDeviceSelectorDialog.show(ContextVS.getMessage("userVSDeviceConnected"),
                        ContextVS.getMessage("selectDeviceToTransferCurrencyMsg"), CurrencyDialog.this);
            }
        });
        MenuButton menuButton = new MenuButton();
        menuButton.setGraphic(Utils.getIcon(FontAwesome.Glyph.BARS));
        menuButton.getItems().addAll(changeWalletMenuItem);
        addMenuButton(menuButton);
    }

    private void update() {
        if(currency.getState() == Currency.State.ERROR) {
            mainPane.getStyleClass().add("currency-error");
            Label currencyStatusLbl = new Label();
            currencyStatusLbl.setText(ContextVS.getMessage("invalidCurrency"));
            currencyStatusLbl.getStyleClass().add("currency-error-msg");
            mainPane.getChildren().add(1, currencyStatusLbl);
            changeWalletMenuItem.setVisible(false);
            getStage().sizeToScene();
            BrowserHost.showMessage(ResponseVS.SC_ERROR, currency.getReason());
        }
    }

    public void showDialog(Currency currency) {
        this.currency = currency;
        mainPane.getStyleClass().remove("currency-error");
        setCaption(currency.getCurrencyServerURL().split("//")[1]);
        currencyHashText.setText(currency.getHashCertVS());
        currencyValueLbl.setText(currency.getAmount().toPlainString());
        currencyLbl.setText(currency.getCurrencyCode());
        currencyTagLbl.setText(MsgUtils.getTagDescription(currency.getTagVS().getName()));
        validFromLbl.setText(ContextVS.getMessage("issuedLbl") + ": " +
                DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"));
        validToLbl.setText(ContextVS.getMessage("expiresLbl") + ": " +
                DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
        try {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    ContextVS.getInstance().getCurrencyServer().getTrustAnchors(), false, Arrays.asList(
                            currency.getCertificationRequest().getCertificate()));
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            PlatformImpl.runLater(statusChecker);
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
                        Utils.getTagForDescription(currency.getTagVS().getName());
                msg = ContextVS.getMessage("currencyInfoErroMsg", errorMsg, amountStr, x509Cert.getIssuerDN().toString(),
                        DateUtils.getDateStr(currency.getValidFrom(), "dd MMM yyyy' 'HH:mm"),
                        DateUtils.getDateStr(currency.getValidTo(), "dd MMM yyyy' 'HH:mm"));
            }
            BrowserHost.showMessage(msg, ContextVS.getMessage("errorLbl"));
        }
        show();
    }

    public Currency getCurrency() {
        return currency;
    }

    @FXML void initialize() {// This method is called by the FXMLLoader when initialization is complete
        log.info("initialize");
        EventBusService.getInstance().register(new EventBusCurrencyListener());
    }

    public static void show(final Currency currency, final Window parentWindow) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                try {
                    CurrencyDialog currencyDialog = new CurrencyDialog();
                    currencyDialog.initOwner(parentWindow);
                    currencyDialog.showDialog(currency);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });
    }

    @Override public void setSelectedDevice(DeviceVSDto device) {
        log.info("setSelectedDevice - device: " + device.getDeviceName());
        walletChangeTask = new WalletChangeTask(device);
        ProgressDialog.showDialog(walletChangeTask, ContextVS.getMessage("changeWalletLbl"));
    }

    public  class WalletChangeTask extends Task<ResponseVS> {

        private DeviceVSDto device;
        private AtomicBoolean isCancelled = new AtomicBoolean();
        private CountDownLatch countDownLatch;

        public WalletChangeTask(DeviceVSDto device) {
            this.device = device;
        }

        @Override protected ResponseVS call() throws Exception {
            updateMessage(ContextVS.getMessage("sendingDataLbl"));
            updateProgress(1, 10);
            try {
                countDownLatch = new CountDownLatch(1);
                DeviceVS deviceVS = device.getDeviceVS();
                WebSocketAuthenticatedService.getInstance().sendMessage(SocketMessageDto.getCurrencyWalletChangeRequest(
                        deviceVS, Arrays.asList(currency)).toString());
                countDownLatch.await();
                WebSocketSession webSocketSession = ContextVS.getInstance().getWSSession(deviceVS.getId());
            } catch(Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
            return null;
        }

        public void update(SocketMessageDto socketMessage) {
            log.info("WalletChangeTask - update");
            countDownLatch.countDown();
        }
    }

    public static class ProcessFormTask extends Task<ResponseVS> {

        private TransactionVSDto transactionVSDto;
        private CurrencyBatchDto batchDto;
        private Currency currency;
        private CurrencyServer currencyServer;

        public ProcessFormTask(TransactionVSDto transactionVSDto, Currency currency, CurrencyServer currencyServer)
                throws Exception {
            this.currency = currency;
            this.currencyServer = currencyServer;
            this.transactionVSDto = transactionVSDto;
            this.batchDto = CurrencyBatchDto.NEW(transactionVSDto.getSubject(),
                    transactionVSDto.getToUserIBAN().iterator().next(), currency.getAmount(),
                    currency.getCurrencyCode(), currency.getTagVS().getName(), false, Arrays.asList(currency),
                    currencyServer.getServerURL(),  currencyServer.getTimeStampServiceURL());
        }

        @Override protected ResponseVS call() throws Exception {
            updateProgress(1, 10);
            updateMessage(ContextVS.getMessage("transactionInProgressMsg"));
            updateProgress(3, 10);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(JSON.getMapper().writeValueAsString(batchDto).getBytes(),
                    ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
            updateProgress(8, 10);
            log.info("transaction result: " + responseVS.getStatusCode());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
            } else {
                CurrencyBatchResponseDto responseDto = JSON.getMapper().readValue(responseVS.getMessage(),
                        CurrencyBatchResponseDto.class);
                batchDto.validateResponse(responseDto, currencyServer.getTrustAnchors());
                BrowserHost.showMessage(ResponseVS.SC_OK, responseDto.getMessage());
            }
            return responseVS;
        }
    }
}
