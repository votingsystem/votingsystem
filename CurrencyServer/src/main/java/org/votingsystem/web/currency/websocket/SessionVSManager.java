package org.votingsystem.web.currency.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.ConnectedUsersDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.SessionVS;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSManager {

    private static Logger log = Logger.getLogger(SessionVSManager.class.getSimpleName());

    private static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    private static final ConcurrentHashMap<String, SessionVS> authenticatedSessionMap = new ConcurrentHashMap<String, SessionVS>();
    private static final ConcurrentHashMap<Long, String> deviceSessionMap = new ConcurrentHashMap<Long, String>();
    private static final SessionVSManager instance = new SessionVSManager();
    private static final ConcurrentHashMap<Long, Set<DeviceVS>> connectedUserVSDeviceMap = new ConcurrentHashMap<Long, Set<DeviceVS>>();

    private SessionVSManager() { }

    public void put(Session session) {
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), session);
        } else log.info("put - session already in sessionMap");
    }

    public void putAuthenticatedDevice(Session session, UserVS userVS) throws ExceptionVS {
        log.info("put - authenticatedSessionMap - session id: " + session.getId() + " - user id:" + userVS.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        SessionVS sessionVS = authenticatedSessionMap.get(session.getId());
        if(sessionVS == null) {
            authenticatedSessionMap.put(session.getId(), new SessionVS(session, userVS));
            deviceSessionMap.put(userVS.getDeviceVS().getId(), session.getId());
            if(connectedUserVSDeviceMap.containsKey(userVS.getId())) {
                connectedUserVSDeviceMap.get(userVS.getId()).add(userVS.getDeviceVS());
            } else connectedUserVSDeviceMap.put(userVS.getId(), new HashSet<DeviceVS>(Arrays.asList(userVS.getDeviceVS())));
        } else log.info("put - session already in authenticatedSessionMap");
    }

    public Collection<Long> getAuthenticatedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getAuthenticatedUsers() {
        return Collections.list(connectedUserVSDeviceMap.keys());
    }

    public Set<DeviceVSDto> connectedDeviceMap(Long userId) {
        if(!connectedUserVSDeviceMap.containsKey(userId)) return new HashSet<>();
        else {
            Set<DeviceVSDto> userVSConnectedDevices = connectedUserVSDeviceMap.get(userId).stream().map(d -> {
                try {
                    return new DeviceVSDto(d);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                return null;
            }).collect(Collectors.toSet());
            return userVSConnectedDevices;
        }
    }

    public ConnectedUsersDto getConnectedUsersDto() {
        ConnectedUsersDto connectedUsersDto = new ConnectedUsersDto();
        List<String> notAuthenticatedSessions = new ArrayList<>();
        for(Session session : sessionMap.values()) {
            notAuthenticatedSessions.add(session.getId());
        }
        connectedUsersDto.setNotAuthenticatedSessions(notAuthenticatedSessions);
        Map<Long, Set<DeviceVSDto>>  usersAuthenticated = new HashMap();
        for(Long deviceId : deviceSessionMap.keySet()) {
            DeviceVSDto deviceVSDto = new DeviceVSDto(deviceId, deviceSessionMap.get(deviceId));
            Long userVSId = authenticatedSessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId();
            Set<DeviceVSDto> userDeviceSet = usersAuthenticated.get(userVSId);
            if(userDeviceSet == null) userDeviceSet = new HashSet<>();
            userDeviceSet.add(deviceVSDto);
            usersAuthenticated.put(userVSId,  userDeviceSet);
        }
        connectedUsersDto.setUsersAuthenticated(usersAuthenticated);
        return connectedUsersDto;
    }

    public void remove(Session session) {
        log.info("remove - session id: " + session.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        if(authenticatedSessionMap.containsKey(session.getId())) {
            SessionVS removedSessionVS = authenticatedSessionMap.remove(session.getId());
            if(deviceSessionMap.containsValue(session.getId())) {
                while (deviceSessionMap.values().remove(session.getId()));
            }
            if(connectedUserVSDeviceMap.containsKey(removedSessionVS.getUserVS().getId())) {
                Set<DeviceVS> userDevices = connectedUserVSDeviceMap.get(removedSessionVS.getUserVS().getId()).stream().
                        filter(d -> { return removedSessionVS.getUserVS().getDeviceVS().getId() != d.getId();}).collect(toSet());
                connectedUserVSDeviceMap.replace(removedSessionVS.getUserVS().getId(), userDevices);
            }
        }
        try {session.close();} catch (Exception ex) {}
    }

    public static SessionVSManager getInstance() {
        return instance;
    }

    public SessionVS getAuthenticatedSession(Session session) {
        return authenticatedSessionMap.get(session.getId());
    }

    public Session getAuthenticatedSession(String sessionId) {
        SessionVS sessionVS = authenticatedSessionMap.get(sessionId);
        return sessionVS != null? sessionVS.getSession() : null;
    }

    public Session getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public SessionVS get(Long deviceId) {
        if(!deviceSessionMap.containsKey(deviceId)) {
            log.info("get - deviceId: '" + deviceId + "' has not active session");
            return null;
        }
        return authenticatedSessionMap.get(deviceSessionMap.get(deviceId));
    }

    public synchronized void broadcast(String messageStr) {
        log.info("broadcast - message: " + messageStr + " to '" + sessionMap.size() + "' users NOT authenticated");
        Enumeration<Session> sessions = sessionMap.elements();
        while(sessions.hasMoreElements()) {
            Session session = sessions.nextElement();
            try {
                session.getBasicRemote().sendText(messageStr);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public synchronized void broadcastToAuthenticatedUsers(String messageStr) {
        log.info("broadcastToAuthenticatedUsers - message: " + messageStr + " to '" + authenticatedSessionMap.size() +
                "' users authenticated");
        Enumeration<SessionVS> sessions = authenticatedSessionMap.elements();
        while(sessions.hasMoreElements()) {
            SessionVS sessionVS = sessions.nextElement();
            Session session = sessionVS.getSession();
            try {
                session.getBasicRemote().sendText(messageStr);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public ResponseVS broadcastList(String messageStr, Set<String> listeners) {
        log.info("broadcastList");
        List<String> errorList = new ArrayList<>();
        for(String listener : listeners) {
            Session session = sessionMap.get(listener);
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    remove(session);
                }
            } else {
                errorList.add(listener);
            }
        }
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        responseVS.setData(errorList);
        return responseVS;
    }

    public ResponseVS broadcastAuthenticatedList(String messageStr, Set<String> listeners) {
        log.info("broadcastAuthenticatedList");
        List<String> errorList = new ArrayList<String>();
        for(String listener : listeners) {
            SessionVS sessionVS = authenticatedSessionMap.get(listener);
            Session session = sessionVS.getSession();
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    remove(session);
                }
            } else {
                errorList.add(listener);
            }
        }
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        responseVS.setData(errorList);
        return responseVS;
    }

    public void sendMessage(List<DeviceVS> userVSDeviceList, SocketMessageDto messageDto) throws ExceptionVS {
        for(DeviceVS device : userVSDeviceList) {
            messageDto.setSessionId(deviceSessionMap.get(device.getId()));
            if(messageDto.getSessionId() != null) {
                if(!sendMessage(messageDto)) deviceSessionMap.remove(device.getId());
            } else log.log(Level.SEVERE, "device id '" + device.getId() + "' has no active sessions");
        }
    }

    public boolean sendMessageToDevice(SocketMessageDto messageDto) throws ExceptionVS, JsonProcessingException {
        if(!deviceSessionMap.containsKey(messageDto.getDeviceToId())) return false;
        messageDto.setSessionId(authenticatedSessionMap.get(deviceSessionMap.get(messageDto.getDeviceToId())).getSession().getId());
        return sendMessage(messageDto);
    }

    public boolean sendMessage(SocketMessageDto messageDto) throws ExceptionVS {
        if(messageDto.getSessionId() == null) throw new ExceptionVS("null sessionId");
        if(authenticatedSessionMap.containsKey(messageDto.getSessionId())) return sendMessageToAuthenticatedUser(messageDto);
        if(sessionMap.containsKey(messageDto.getSessionId())) {
            Session session = sessionMap.get(messageDto.getSessionId());
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                    return true;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            remove(session);
        }
        log.info("sendMessage - lost message for session '" + messageDto.getSessionId() + "' - message: " + messageDto);
        return false;
    }

    private boolean sendMessageToAuthenticatedUser(SocketMessageDto messageDto) {
        if(!authenticatedSessionMap.containsKey(messageDto.getSessionId())) return false;
        Session session = authenticatedSessionMap.get(messageDto.getSessionId()).getSession();
        if(session.isOpen()) {
            try {
                session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                return true;
            } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
        }
        log.info ("sendMessageToAuthenticatedUser - lost message for session '" + messageDto.getSessionId() + "' - message: " + messageDto);
        remove(session);
        return false;
    }

}
