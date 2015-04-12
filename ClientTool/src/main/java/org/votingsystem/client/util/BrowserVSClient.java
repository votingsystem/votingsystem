package org.votingsystem.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.web.WebView;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.*;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;

import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * JavaScript interface object
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSClient {

    private static Logger log = Logger.getLogger(BrowserVSClient.class.getSimpleName());

    private WebView webView;

    public BrowserVSClient(WebView webView) {
        this.webView = webView;
    }
    public void setMessage(String messageToSignatureClient) {
        try {
            String jsonStr =  StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
            String logMsg = jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
            log.info("BrowserVSClient.setMessage: " + logMsg);
            OperationVS operationVS = new ObjectMapper().readValue(jsonStr, OperationVS.class);
            Browser.getInstance().registerCallerCallbackView(operationVS.getCallerCallback(), this.webView);
            switch (operationVS.getType()) {
                case CONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(
                            true, operationVS.getDocument());
                    break;
                case DISCONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false, null);
                    break;
                case FILE_FROM_URL:
                    Browser.getInstance().processOperationVS(null, operationVS);
                    break;
                case MESSAGEVS_TO_DEVICE:
                    WebSocketService.getInstance().sendMessage(jsonStr);
                    break;
                case  KEYSTORE_SELECT:
                    Utils.selectKeystoreFile(operationVS, Browser.getInstance());
                    break;
                case SELECT_IMAGE:
                    Utils.selectImage(operationVS, Browser.getInstance());
                    break;
                case OPEN_SMIME:
                    String smimeMessageStr = new String(Base64.getDecoder().decode(
                            operationVS.getMessage().getBytes()), "UTF-8");
                    DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(
                            smimeMessageStr, null, operationVS.getDocument());
                    Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
                    break;
                case OPEN_CURRENCY:
                    CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject((
                                    (String) operationVS.getDocument().get("object")).getBytes()),
                            Browser.getInstance().getScene().getWindow());
                    break;
                case OPEN_SMIME_FROM_URL:
                    Browser.getInstance().processOperationVS(null, operationVS);
                    break;
                case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                    RepresentativeAccreditationsDialog.show(operationVS, Browser.getInstance().getScene().getWindow());
                    break;
                case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                    RepresentativeVotingHistoryDialog.show(operationVS, Browser.getInstance().getScene().getWindow());
                    break;
                case SAVE_SMIME:
                    Utils.saveReceipt(operationVS, Browser.getInstance());
                    break;
                case SAVE_SMIME_ANONYMOUS_DELEGATION:
                    Utils.saveReceiptAnonymousDelegation(operationVS, Browser.getInstance());
                    break;
                case CERT_USER_NEW:
                    Browser.getInstance().processOperationVS(operationVS, ContextVS.getMessage("newCertPasswDialogMsg"));
                    break;
                case WALLET_OPEN:
                    WalletPane.showDialog(Browser.getInstance().getScene().getWindow());
                    break;
                case VOTING_PUBLISHING:
                    PublishElectionDialog.show(operationVS, Browser.getInstance().getScene().getWindow());
                    break;
                case NEW_REPRESENTATIVE:
                case EDIT_REPRESENTATIVE:
                    PublishRepresentativeDialog.show(operationVS, Browser.getInstance().getScene().getWindow());
                    break;
                case WALLET_SAVE:
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                    String password = passwordDialog.getPassword();
                    if(password != null) {
                        try {
                            Wallet.getWallet(password);
                            Browser.getInstance().fireCoreSignal("vs-wallet-save", null, false);
                            InboxService.getInstance().removeMessagesByType(TypeVS.CURRENCY_IMPORT);
                        } catch (WalletException wex) {
                            Utils.showWalletNotFoundMessage();
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                            showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    }
                    break;
                case MESSAGEVS:
                    if(operationVS.getDocumentToSignMap() != null) Browser.getInstance().processOperationVS(
                            operationVS, null);
                    else  Browser.getInstance().processOperationVS(null, operationVS);
                    break;
                default:
                    Browser.getInstance().processOperationVS(operationVS, null);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            showMessage(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        }
    }

    public String call(String messageToSignatureClient) {
        String result = null;
        try {
            String jsonStr = StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
            OperationVS operationVS = new ObjectMapper().readValue(jsonStr, OperationVS.class);
            switch (operationVS.getType()) {
                case FORMAT_DATE:
                    Date dateToFormat = DateUtils.getDateFromString((String) operationVS.getDocument().get("dateStr"),
                            (String) operationVS.getDocument().get("dateFormat"));
                    String stringFormat = null;
                    if (operationVS.getDocument().get("stringFormat") != null) {
                        stringFormat = (String) operationVS.getDocument().get("stringFormat");
                    }
                    if (stringFormat != null) result = DateUtils.getDateStr(dateToFormat,
                            (String) operationVS.getDocument().get("stringFormat"));
                    else result = DateUtils.getDayWeekDateStr(dateToFormat);
                    break;
                case SIGNAL_VS:
                    Browser.getInstance().processSignalVS(operationVS.getDocument());
                    break;
                case REPRESENTATIVE_STATE:
                    result = SessionService.getInstance().getRepresentationState().toString();
                    break;
                case WALLET_STATE:
                    result = Wallet.getWalletState().toString();
                    break;
                default:
                    result = "Unknown operation: '" + operationVS.getType() + "'";
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            result = ex.getMessage();
        } finally {
            if(result == null) return result;
            else return Base64.getEncoder().encodeToString(result.getBytes());
        }
    }
}
