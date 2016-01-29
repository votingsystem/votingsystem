package org.votingsystem.web.currency.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.votingsystem.dto.ConnectedUsersDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSManager {

    private static Logger log = Logger.getLogger(SessionVSManager.class.getSimpleName());

    private ConcurrentHashMap<String, Session> sessionMap = null;
    private ConcurrentHashMap<String, Session> authenticatedSessionMap = null;
    private ConcurrentHashMap<Long, Session> deviceSessionMap = null;
    private ConcurrentHashMap<Long, Set<DeviceVS>> userVSDeviceMap = null;

    private static final SessionVSManager instance = new SessionVSManager();

    private SessionVSManager() {
        sessionMap = new ConcurrentHashMap<>();
        authenticatedSessionMap = new ConcurrentHashMap<>();
        deviceSessionMap = new ConcurrentHashMap<>();
        userVSDeviceMap = new ConcurrentHashMap<>();
    }

    public void put(Session session) {
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), session);
        } else log.info("put - session already in sessionMap");
    }

    public void putAuthenticatedDevice(Session session, UserVS userVS) throws ExceptionVS {
        log.info("putAuthenticatedDevice - session id: " + session.getId() + " - UserVS id:" + userVS.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        authenticatedSessionMap.put(session.getId(), session);
        deviceSessionMap.put(userVS.getDeviceVS().getId(), session);
        if(userVSDeviceMap.containsKey(userVS.getId())) {
            Set<DeviceVS> deviceSet = userVSDeviceMap.get(userVS.getId()).stream().filter(device ->
                !device.getDeviceId().equals(userVS.getDeviceVS().getDeviceId())
            ).collect(Collectors.toSet());
            deviceSet.add(userVS.getDeviceVS());
            userVSDeviceMap.put(userVS.getId(), deviceSet);
        } else userVSDeviceMap.put(userVS.getId(), Sets.newHashSet(userVS.getDeviceVS()));
        session.getUserProperties().put("userVS", userVS);
        session.getUserProperties().put("deviceVS", userVS.getDeviceVS());
    }

    public Collection<Long> getAuthenticatedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getAuthenticatedUsers() {
        return Collections.list(userVSDeviceMap.keys());
    }

    public Set<DeviceVSDto> connectedDeviceMap(Long userId) {
        if(!userVSDeviceMap.containsKey(userId)) return new HashSet<>();
        else {
            Set<DeviceVSDto> userVSConnectedDevices = userVSDeviceMap.get(userId).stream().map(     d -> {
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
            Session deviceSession = deviceSessionMap.get(deviceId);
            DeviceVS deviceVS = (DeviceVS) deviceSession.getUserProperties().get("deviceVS");
            UserVS userVS = (UserVS) deviceSession.getUserProperties().get("userVS");
            DeviceVSDto deviceVSDto = new DeviceVSDto(deviceVS.getId(), deviceVS.getDeviceId(), deviceSession.getId());
            if(usersAuthenticated.containsKey(userVS.getId())) usersAuthenticated.get(userVS.getId()).add(deviceVSDto);
            else usersAuthenticated.put(userVS.getId(), new HashSet<DeviceVSDto>(Arrays.asList(deviceVSDto)));
        }
        connectedUsersDto.setUsersAuthenticated(usersAuthenticated);
        return connectedUsersDto;
    }

    public void remove(Session session) {
        log.info("remove - session id: " + session.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        if(authenticatedSessionMap.containsKey(session.getId())) authenticatedSessionMap.remove(session.getId());
        DeviceVS deviceVS = null;
        if( (deviceVS = (DeviceVS) session.getUserProperties().get("deviceVS")) != null) deviceSessionMap.remove(deviceVS.getId());
        UserVS userVS = null;
        if( (userVS = (UserVS) session.getUserProperties().get("userVS")) != null) {
            Set<DeviceVS> deviceVSSet = userVSDeviceMap.get(userVS.getId());
            for(DeviceVS userVSDevice:deviceVSSet) {
                if(userVSDevice.getId().longValue() == deviceVS.getId().longValue()) deviceVSSet.remove(userVSDevice);
            }
        }
        try {
            session.close();
        } catch (Exception ex) {
            log.severe(ex.getMessage());
        }
    }

    public String getDeviceSessionId(Long deviceId) {
        if(!deviceSessionMap.containsKey(deviceId)) return null;
        else return deviceSessionMap.get(deviceId).getId();
    }

    public static SessionVSManager getInstance() {
        return instance;
    }

    public Session getAuthenticatedSession(String sessionId) {
        return authenticatedSessionMap.get(sessionId);
    }

    public Session getNotAuthenticatedSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public synchronized void broadcast(String message) {
        log.info("broadcast - message: " + message + " to '" + sessionMap.size() + "' NOT authenticated users");
        for(Session session : sessionMap.values()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public synchronized void broadcastToAuthenticatedUsers(String message) {
        log.info("broadcastToAuthenticatedUsers - message: " + message + " to '" + authenticatedSessionMap.size() +
                "' users authenticated");
        for(Session session : authenticatedSessionMap.values()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public Set<String> broadcastList(String message, Collection<String> listenersSessionIdSet) {
        log.info("broadcastList");
        Set<String> brokenSessionSet = new HashSet<>();
        for(String sessionId : listenersSessionIdSet) {
            Session session = sessionMap.get(sessionId);
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    remove(session);
                }
            } else {
                brokenSessionSet.add(sessionId);
            }
        }
        return brokenSessionSet;
    }

    public Set<DeviceVS> getUserVSDeviceVSSet(Long userId) {
        Set<DeviceVS> result = userVSDeviceMap.get(userId);
        if(result == null) result = new HashSet<>();
        return result;
    }

    public Set<String> broadcastAuthenticatedList(String message, Set<String> listenersSessionIdSet) {
        log.info("broadcastAuthenticatedList");
        Set<String> brokenSessionSet = new HashSet<>();
        for(String listener : listenersSessionIdSet) {
            Session session = authenticatedSessionMap.get(listener);
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                    remove(session);
                }
            } else {
                brokenSessionSet.add(listener);
            }
        }
        return brokenSessionSet;
    }

    public void sendMessage(List<DeviceVS> userVSDeviceList, SocketMessageDto messageDto) throws ExceptionVS {
        for(DeviceVS device : userVSDeviceList) {
            Session deviceSession = deviceSessionMap.get(device.getId());
            if(messageDto.getSessionId() != null) {
                if(!sendMessage(messageDto, deviceSession.getId())) deviceSessionMap.remove(device.getId());
            } else log.log(Level.SEVERE, "sendMessage - device id '" + device.getId() + "' has no active sessions");
        }
    }

    public boolean sendMessageToDevice(SocketMessageDto messageDto) throws ExceptionVS, JsonProcessingException {
        Session deviceSession = deviceSessionMap.get(messageDto.getDeviceToId());
        if(deviceSession == null) return false;
        else return sendMessage(messageDto, deviceSession.getId());
    }

    private boolean sendMessage(SocketMessageDto messageDto, String deviceToSessionId) throws ExceptionVS {
        Session session = null;
        if((session = authenticatedSessionMap.get(deviceToSessionId)) != null) {
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(JSON.getMapper().writeValueAsString(messageDto));
                    return true;
                } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
            }
            log.info ("sendMessage - lost message for session '" + messageDto.getSessionId() + "' - message: " + messageDto);
            remove(session);
            return false;
        } else if((session = sessionMap.get(deviceToSessionId)) != null) {
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


}
