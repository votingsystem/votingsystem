package org.votingsystem.client.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.web.WebView;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.CurrencyDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.PublishElectionDialog;
import org.votingsystem.client.dialog.PublishRepresentativeDialog;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * JavaScript interface object
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSClient {

    private static Logger log = Logger.getLogger(BrowserVSClient.class.getSimpleName());

    private WebView webView;

    public BrowserVSClient(WebView webView) {
        this.webView = webView;
    }
    public void setJSONMessageToSignatureClient(String messageToSignatureClient) {
        try {
            String jsonStr =  StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
            String logMsg = jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
            log.info("BrowserVSClient.setJSONMessageToSignatureClient: " + logMsg);
            OperationVS operationVS = new ObjectMapper().readValue(jsonStr, OperationVS.class);
            BrowserVS.getInstance().registerCallerCallbackView(operationVS.getCallerCallback(), this.webView);
            switch (operationVS.getType()) {
                case CONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(
                            true, operationVS.getDocument());
                    break;
                case DISCONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false, null);
                    break;
                case FILE_FROM_URL:
                    BrowserVS.getInstance().processOperationVS(null, operationVS);
                    break;
                case MESSAGEVS_TO_DEVICE:
                    WebSocketService.getInstance().sendMessage(jsonStr);
                    break;
                case  KEYSTORE_SELECT:
                    Utils.selectKeystoreFile(operationVS, BrowserVS.getInstance());
                    break;
                case SELECT_IMAGE:
                    Utils.selectImage(operationVS, BrowserVS.getInstance());
                    break;
                case OPEN_SMIME:
                    String smimeMessageStr = new String(Base64.getDecoder().decode(
                            operationVS.getMessage().getBytes()), "UTF-8");
                    DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(
                            smimeMessageStr, null, operationVS.getDocument());
                    BrowserVS.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
                    break;
                case OPEN_CURRENCY:
                    CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject((
                                    (String) operationVS.getDocument().get("object")).getBytes()),
                            BrowserVS.getInstance().getScene().getWindow());
                    break;
                case OPEN_SMIME_FROM_URL:
                    BrowserVS.getInstance().processOperationVS(null, operationVS);
                    break;
                case SAVE_SMIME:
                    Utils.saveReceipt(operationVS, BrowserVS.getInstance());
                    break;
                case SAVE_SMIME_ANONYMOUS_DELEGATION:
                    Utils.saveReceiptAnonymousDelegation(operationVS, BrowserVS.getInstance());
                    break;
                case CERT_USER_NEW:
                    BrowserVS.getInstance().processOperationVS(operationVS, ContextVS.getMessage("newCertPasswDialogMsg"));
                    break;
                case WALLET_OPEN:
                    WalletPane.showDialog(BrowserVS.getInstance().getScene().getWindow());
                    break;
                case VOTING_PUBLISHING:
                    PublishElectionDialog.show(operationVS, BrowserVS.getInstance().getScene().getWindow());
                    break;
                case NEW_REPRESENTATIVE:
                    PublishRepresentativeDialog.show(operationVS, BrowserVS.getInstance().getScene().getWindow());
                    break;
                case WALLET_SAVE:
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.showWithoutPasswordConfirm(ContextVS.getMessage("walletPinMsg"));
                    String password = passwordDialog.getPassword();
                    if(password != null) {
                        try {
                            Wallet.getWallet(password);
                            BrowserVS.getInstance().fireCoreSignal("vs-wallet-save", null, false);
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
                    if(operationVS.getDocumentToSignMap() != null) BrowserVS.getInstance().processOperationVS(
                            operationVS, null);
                    else  BrowserVS.getInstance().processOperationVS(null, operationVS);
                    break;
                default:
                    BrowserVS.getInstance().processOperationVS(operationVS, null);
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
                    BrowserVS.getInstance().processSignalVS(operationVS.getDocument());
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
