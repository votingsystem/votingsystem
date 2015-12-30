package org.votingsystem.client.webextension.task;

import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.EncryptedBundle;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.net.InetAddress;
import java.util.logging.Logger;

import static org.votingsystem.util.ContextVS.PROVIDER;
import static org.votingsystem.util.ContextVS.SIGN_MECHANISM;

public class CertRequestTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(CertRequestTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;
    private String message;

    public CertRequestTask(OperationVS operationVS, String message, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
        this.message = message;
    }

    @Override protected ResponseVS call() throws Exception {
        updateMessage(message);
        CertExtensionDto certExtensionDto = operationVS.getData(CertExtensionDto.class);
        certExtensionDto.setDeviceId(HttpHelper.getMAC());
        certExtensionDto.setDeviceType(DeviceVS.Type.PC);
        certExtensionDto.setDeviceName(InetAddress.getLocalHost().getHostName());
        CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                SIGN_MECHANISM, PROVIDER, certExtensionDto);
        byte[] csrBytes = certificationRequest.getCsrPEM();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(csrBytes, null,
                ((AccessControlVS) operationVS.getTargetServer()).getUserCSRServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            Long requestId = Long.valueOf(responseVS.getMessage());
            byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
            EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, serializedCertificationRequest);
            BrowserSessionService.getInstance().setCSRRequest(requestId, bundle);
        }
        operationVS.processResult(responseVS);
        return responseVS;
    }
}
