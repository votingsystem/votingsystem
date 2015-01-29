package org.votingsystem.client.util;

import javafx.scene.web.WebView;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.CooinDialog;
import org.votingsystem.client.pane.DocumentVSBrowserStackPane;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.service.WebSocketService;
import org.votingsystem.client.service.WebSocketServiceAuthenticated;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.Wallet;

import java.util.Base64;
import java.util.Date;

import static org.votingsystem.client.VotingSystemApp.showMessage;
/**
 * JavaScript interface object
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSClient {

    private static Logger log = Logger.getLogger(BrowserVSClient.class);

    private WebView webView;

    public BrowserVSClient(WebView webView) {
        this.webView = webView;
    }
    public void setJSONMessageToSignatureClient(String messageToSignatureClient) {
        try {
            String jsonStr =  StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
            String logMsg = jsonStr.length() > 300 ? jsonStr.substring(0, 300) + "..." : jsonStr;
            log.debug("BrowserVSClient.setJSONMessageToSignatureClient: " + logMsg);
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonStr);
            OperationVS operationVS = OperationVS.parse(jsonObject);
            BrowserVS.getInstance().registerCallerCallbackView(operationVS.getCallerCallback(), this.webView);
            switch (operationVS.getType()) {
                case CONNECT:
                    WebSocketServiceAuthenticated.getInstance().setConnectionEnabled(
                            true, operationVS.getDocument());
                    break;
                case DISCONNECT:
                    WebSocketServiceAuthenticated.getInstance().setConnectionEnabled(false, null);
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
                    DocumentVSBrowserStackPane.showDialog(smimeMessageStr, null, operationVS.getDocument());
                    break;
                case OPEN_COOIN:
                    CooinDialog.show((Cooin) ObjectUtils.deSerializeObject((
                            (String) operationVS.getDocument().get("object")).getBytes()));
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
                    BrowserVS.getInstance().processOperationVS(operationVS, ContextVS.getMessage("walletPinMsg"));
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
            log.error(ex.getMessage(), ex);
            showMessage(new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage()));
        }
    }

    public String call(String messageToSignatureClient) {
        String result = null;
        try {
            String jsonStr = StringUtils.decodeB64_TO_UTF8(messageToSignatureClient);
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonStr);
            OperationVS operationVS = OperationVS.parse(jsonObject);
            switch (operationVS.getType()) {
                case FORMAT_DATE:
                    Date dateToFormat = DateUtils.getDateFromString((String) operationVS.getDocument().get("dateStr"),
                            (String) operationVS.getDocument().get("dateFormat"));
                    String stringFormat = null;
                    if (operationVS.getDocument().get("stringFormat") != null && !JSONNull.getInstance().equals(
                            operationVS.getDocument().get("stringFormat"))) {
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
            log.error(ex.getMessage(), ex);
            result = ex.getMessage();
        } finally {
            if(result == null) return result;
            else return Base64.getEncoder().encodeToString(result.getBytes());
        }
    }
}
