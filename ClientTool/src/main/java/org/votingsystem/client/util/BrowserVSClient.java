package org.votingsystem.client.util;

import javafx.scene.web.WebView;
import org.votingsystem.client.Browser;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.*;
import org.votingsystem.client.dto.SignalVSDto;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.pane.WalletPane;
import org.votingsystem.client.service.BrowserSessionService;
import org.votingsystem.client.service.InboxService;
import org.votingsystem.client.service.WebSocketAuthenticatedService;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.WalletException;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.Wallet;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * JavaScript interface object
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSClient implements PasswordDialog.Listener {

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
            OperationVS operationVS = JSON.getMapper().readValue(jsonStr, OperationVS.class);
            Browser.getInstance().registerCallerCallbackView(operationVS.getCallerCallback(), this.webView);
            switch (operationVS.getType()) {
                case CONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(true);
                    break;
                case DISCONNECT:
                    WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false);
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
                    DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(smimeMessageStr, null);
                    Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
                    break;
                case OPEN_CURRENCY:
                    CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject((operationVS.getMessage()).getBytes()),
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
                case SEND_ANONYMOUS_DELEGATION:
                    Utils.saveReceiptAnonymousDelegation(operationVS, Browser.getInstance());
                    break;
                case CERT_USER_NEW:
                    Browser.getInstance().processOperationVS(operationVS, ContextVS.getMessage("newCertPasswDialogMsg"));
                    break;
                case WALLET_OPEN:
                    WalletPane.showDialog(Browser.getInstance().getScene().getWindow());
                    break;
                case VOTING_PUBLISHING:
                    ElectionEditorDialog.show(operationVS, Browser.getInstance().getScene().getWindow());
                    break;
                case NEW_REPRESENTATIVE:
                case EDIT_REPRESENTATIVE:
                    RepresentativeEditorDialog.show(operationVS);
                    break;
                case CURRENCY_GROUP_NEW:
                case CURRENCY_GROUP_EDIT:
                    GroupVSEditorDialog.show(operationVS);
                    break;
                case MAIL_TO:
                    VotingSystemApp.getInstance().getHostServices().showDocument(
                            ContextVS.getInstance().getDefaultServer().getServerURL() + "/app/contact.xhtml?openMailClient=true");
                    break;
                case WALLET_SAVE:
                    PasswordDialog.showWithoutPasswordConfirm(TypeVS.WALLET_SAVE, this, ContextVS.getMessage("walletPinMsg"));
                    break;
                case MESSAGEVS:
                    if(operationVS.getDocumentToSign() != null) Browser.getInstance().processOperationVS(
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

    public String call(String msgFromBrowser) {
        String result = null;
        try {
            String jsonStr = StringUtils.decodeB64_TO_UTF8(msgFromBrowser);
            OperationVS operationVS = JSON.getMapper().readValue(jsonStr, OperationVS.class);
            switch (operationVS.getType()) {
                case SIGNAL_VS:
                    Browser.getInstance().processSignalVS(operationVS.getData(SignalVSDto.class));
                    break;
                case REPRESENTATIVE_STATE:
                    result = JSON.getMapper().writeValueAsString(BrowserSessionService.getInstance().getRepresentationState());
                    break;
                case WALLET_STATE:
                    result = JSON.getMapper().writeValueAsString(Wallet.getWalletDto());
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

    @Override public void setPassword(TypeVS passwordType, String password) {
        switch (passwordType) {
            case WALLET_SAVE:
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
        }
    }
}
