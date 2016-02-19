package org.votingsystem.client.webextension.service;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import org.votingsystem.client.webextension.dto.BrowserSessionDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.RepresentationState;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.File;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserSessionService {

    private static Logger log = Logger.getLogger(BrowserSessionService.class.getName());

    private ResponseVS currentResponseVS;
    private File sessionFile;
    private File representativeStateFile;
    private RepresentativeDelegationDto anonymousDelegationDto;
    private RepresentationStateDto representativeStateDto;
    private BrowserSessionDto browserSessionDto;
    private static PrivateKey privateKey;
    private static Certificate[] chain;
    private static CountDownLatch countDownLatch;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;
    private static final BrowserSessionService INSTANCE = new BrowserSessionService();

    class EventBusWebSocketListener {
        @Subscribe public void call(SocketMessageDto socketMessage) {
            switch(socketMessage.getOperation()) {
                case DISCONNECT:
                    browserSessionDto.setIsConnected(false);
                    flush();
                    break;
                case CONNECT:
                    browserSessionDto.setIsConnected(true);
                    flush();
                    break;
            }
        }
    }

    private BrowserSessionService() {
        try {
            sessionFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.BROWSER_SESSION_FILE);
            if(sessionFile.createNewFile()) {
                browserSessionDto = new BrowserSessionDto();
                getDevice();
            } else {
                try {
                    browserSessionDto = JSON.getMapper().readValue(sessionFile, BrowserSessionDto.class);
                } catch (Exception ex) {
                    browserSessionDto = new BrowserSessionDto();
                    log.log(Level.SEVERE, "CORRUPTED BROWSER SESSION FILE!!!", ex);
                }
            }
            browserSessionDto.setIsConnected(false);
            flush();
            EventBusService.getInstance().register(new EventBusWebSocketListener());
        } catch (Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    private void loadRepresentationData() throws Exception {
        RepresentationStateDto stateDto = null;
        UserVS userVS = null;
        if(browserSessionDto.getUserVS() != null) userVS = browserSessionDto.getUserVS().getUserVS();
        if(userVS != null) {
            stateDto = HttpHelper.getInstance().getData(RepresentationStateDto.class,
                    ContextVS.getInstance().getAccessControl().getRepresentationStateServiceURL(userVS.getNif()),
                    MediaTypeVS.JSON);
        }
        representativeStateFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.REPRESENTATIVE_STATE_FILE);
        if(representativeStateFile.createNewFile()) {
            representativeStateDto = stateDto;
            flush();
        } else {
            representativeStateDto = JSON.getMapper().readValue(representativeStateFile, RepresentationStateDto.class);
            if(stateDto != null && stateDto.getBase64ContentDigest() != null) {
                if(!stateDto.getBase64ContentDigest().equals(representativeStateDto.getBase64ContentDigest())) {
                    log.info("Base64ContentDigest mismatch - updating local representativeState");
                    representativeStateDto = stateDto;
                    flush();
                }
            } else if(stateDto != null && stateDto.getState() == RepresentationState.WITHOUT_REPRESENTATION) {
                if(representativeStateDto.getState() != RepresentationState.WITHOUT_REPRESENTATION) {
                    representativeStateDto = stateDto;
                    flush();
                }
            }
        }
    }

    public void setAnonymousDelegationDto(RepresentativeDelegationDto delegation) {
        try {
            loadRepresentationData();
            representativeStateDto = new RepresentationStateDto(delegation);
            anonymousDelegationDto = delegation;
            flush();
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    public RepresentationStateDto getRepresentationState() throws Exception {
        loadRepresentationData();
        RepresentationStateDto result = representativeStateDto.clone();
        String stateMsg = null;
        switch (representativeStateDto.getState()) {
            case WITH_ANONYMOUS_REPRESENTATION:
                stateMsg = ContextVS.getMessage("withAnonymousRepresentationMsg");
                break;
            case REPRESENTATIVE:
                stateMsg = ContextVS.getMessage("userRepresentativeMsg");
                break;
            case WITHOUT_REPRESENTATION:
                stateMsg = ContextVS.getMessage("withoutRepresentationMsg");
                break;
        }
        result.setStateMsg(stateMsg);
        result.setLastCheckedDateMsg(ContextVS.getMessage("lastCheckedDateMsg",
                DateUtils.getDayWeekDateStr(result.getLastCheckedDate(), "HH:mm")));
        return result;
    }

    public RepresentativeDelegationDto getAnonymousDelegationDto() {
        if(anonymousDelegationDto != null) return anonymousDelegationDto;
        try {
            loadRepresentationData();
            String serializedDelegation = representativeStateDto.getAnonymousDelegationObject();
            if(serializedDelegation != null) {
                anonymousDelegationDto = (RepresentativeDelegationDto) ObjectUtils.deSerializeObject(
                        serializedDelegation.getBytes());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        } finally {
            return anonymousDelegationDto;
        }
    }

    public static BrowserSessionService getInstance() {
        return INSTANCE;
    }

    public SocketMessageDto initAuthenticatedSession(SocketMessageDto socketMsg) {
        try {
            if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                browserSessionDto.setDevice(socketMsg.getConnectedDevice());
                browserSessionDto.setIsConnected(true);
                ContextVS.getInstance().setConnectedDevice(browserSessionDto.getDevice());
                flush();
            } else {
                showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                log.log(Level.SEVERE,"ERROR - initAuthenticatedSession - statusCode: " + socketMsg.getStatusCode());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
        return socketMsg;
    }

    public static void decryptMessage(SocketMessageDto socketMessage) throws Exception {
        if(privateKey != null) socketMessage.decryptMessage(privateKey);
        else {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("improperTokenErrorMsg"));
            throw new ExceptionVS(ContextVS.getMessage("improperTokenErrorMsg"));
        }
    }

    public DeviceVSDto getDevice() throws Exception {
        DeviceVSDto deviceVSDto = browserSessionDto.getDevice();
        if(deviceVSDto == null) {
            deviceVSDto = new DeviceVSDto();
            deviceVSDto.setDeviceId(HttpHelper.getMAC());
            deviceVSDto.setDeviceType(DeviceVS.Type.PC);
            deviceVSDto.setDeviceName(InetAddress.getLocalHost().getHostName());
            browserSessionDto.setDevice(deviceVSDto);
        }
        return browserSessionDto.getDevice();
    }

    public UserVS getUserVS()  {
        try {
            if(browserSessionDto.getUserVS() != null) return browserSessionDto.getUserVS().getUserVS();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public DeviceVSDto getConnectedDevice()  {
        try {
            if(browserSessionDto.getUserVS() != null) return browserSessionDto.getUserVS().getDeviceVS();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
        browserSessionDto.setUserVS(UserVSDto.COMPLETE(userVS));
        browserSessionDto.setIsConnected(isConnected);
        flush();
    }

    public UserVS getKeyStoreUserVS() {
        return ContextVS.getInstance().getKeyStoreUserVS();
    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
            char[] password, String subject) throws Exception {
        log.info("getSMIME");
        countDownLatch = new CountDownLatch(1);
        SocketMessageDto messageDto = SocketMessageDto.getSignRequest(ContextVS.getInstance().getConnectedDevice().getDeviceVS(),
                toUser, textToSign, subject);
        PlatformImpl.runLater(() -> {//Service must only be used from the FX Application Thread
            try {
                WebSocketService.getInstance().sendMessage(JSON.getMapper().writeValueAsString(messageDto));
            } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
        });
        countDownLatch.await();
        ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        else return responseVS.getData();
    }

    public static void setSignResponse(SocketMessageDto socketMsg) {
        switch(socketMsg.getStatusCode()) {
            case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                break;
            case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("deviceVSTokenNotFoundErrorMsg"));
                countDownLatch.countDown();
                break;
            case ResponseVS.SC_ERROR:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, socketMsg.getMessage());
                countDownLatch.countDown();
                break;
            default:
                try {
                    smimeMessage = socketMsg.getSMIME();
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_OK, null, smimeMessage);
                } catch(Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    private void flush() {
        log.info("flush");
        try {
            JSON.getMapper().writeValue(sessionFile, browserSessionDto);
            if(representativeStateDto != null) JSON.getMapper().writeValue(representativeStateFile, representativeStateDto);
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

}
