package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.EncryptedBundle;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGN_MECHANISM;

public class CertRequestTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(CertRequestTask.class.getName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public CertRequestTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected ResponseVS call() {
        ResponseVS responseVS = null;
        try {
            updateMessage(message);
            CertExtensionDto certExtensionDto = operationVS.getData(CertExtensionDto.class);
            DeviceVSDto device = BrowserSessionService.getInstance().getDevice();
            certExtensionDto.setDeviceId(device.getDeviceId());
            certExtensionDto.setDeviceType(device.getDeviceType());
            certExtensionDto.setDeviceName(device.getDeviceName());
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    SIGN_MECHANISM, PROVIDER, certExtensionDto);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            responseVS = HttpHelper.getInstance().sendData(csrBytes, null,
                    ((AccessControlVS) operationVS.getTargetServer()).getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
                EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, serializedCertificationRequest);
                BrowserSessionService.getInstance().setCSRRequest(requestId, bundle);
            }
            operationVS.processResult(responseVS);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        }
        return responseVS;
    }

}
