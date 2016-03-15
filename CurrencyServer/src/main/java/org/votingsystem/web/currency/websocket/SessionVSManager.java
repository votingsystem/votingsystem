package org.votingsystem.web.currency.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.votingsystem.dto.ConnectedUsersDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;

import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSManager {

    private static Logger log = Logger.getLogger(SessionVSManager.class.getName());

    private ConcurrentHashMap<String, Session> sessionMap = null;
    private ConcurrentHashMap<String, Session> authenticatedSessionMap = null;
    private ConcurrentHashMap<Long, Session> deviceSessionMap = null;
    private ConcurrentHashMap<Long, Set<Device>> userDeviceMap = null;

    private static final SessionVSManager instance = new SessionVSManager();

    public static AtomicLong browserDeviceId = new AtomicLong(1000000000);

    private SessionVSManager() {
        sessionMap = new ConcurrentHashMap<>();
        authenticatedSessionMap = new ConcurrentHashMap<>();
        deviceSessionMap = new ConcurrentHashMap<>();
        userDeviceMap = new ConcurrentHashMap<>();
    }

    public Long getAndIncrementBrowserDeviceId() {
        return browserDeviceId.getAndIncrement();
    }

    public void put(Session session) {
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), session);
        } else log.info("put - session already in sessionMap");
    }

    public void putAuthenticatedDevice(Session session, User user) throws ExceptionVS {
        log.info("putAuthenticatedDevice - session id: " + session.getId() + " - User id:" + user.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        authenticatedSessionMap.put(session.getId(), session);
        deviceSessionMap.put(user.getDevice().getId(), session);
        if(userDeviceMap.containsKey(user.getId())) {
            Set<Device> deviceSet = userDeviceMap.get(user.getId()).stream().filter(device ->
                !device.getDeviceId().equals(user.getDevice().getDeviceId())
            ).collect(Collectors.toSet());
            deviceSet.add(user.getDevice());
            userDeviceMap.put(user.getId(), deviceSet);
        } else userDeviceMap.put(user.getId(), Sets.newHashSet(user.getDevice()));
        session.getUserProperties().put("user", user);
        session.getUserProperties().put("device", user.getDevice());
    }

    public void putBrowserDevice(Session session){
        Device device = (Device) session.getUserProperties().get("device");
        deviceSessionMap.put(device.getId(), session);
    }

    public Collection<Long> getAuthenticatedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getAuthenticatedUsers() {
        return Collections.list(userDeviceMap.keys());
    }

    public Set<DeviceDto> connectedDeviceMap(Long userId) {
        if(!userDeviceMap.containsKey(userId)) return new HashSet<>();
        else {
            Set<DeviceDto> userConnectedDevices = userDeviceMap.get(userId).stream().map(d -> {
                try {
                    return new DeviceDto(d);
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                return null;
            }).collect(Collectors.toSet());
            return userConnectedDevices;
        }
    }

    public ConnectedUsersDto getConnectedUsersDto() {
        ConnectedUsersDto connectedUsersDto = new ConnectedUsersDto();
        List<String> notAuthenticatedSessions = new ArrayList<>();
        for(Session session : sessionMap.values()) {
            notAuthenticatedSessions.add(session.getId());
        }
        connectedUsersDto.setNotAuthenticatedSessions(notAuthenticatedSessions);
        Map<Long, Set<DeviceDto>>  usersAuthenticated = new HashMap();
        for(Long deviceId : deviceSessionMap.keySet()) {
            Session deviceSession = deviceSessionMap.get(deviceId);
            Device device = (Device) deviceSession.getUserProperties().get("device");
            User user = (User) deviceSession.getUserProperties().get("user");
            DeviceDto deviceDto = new DeviceDto(device.getId(), device.getDeviceId(), deviceSession.getId());
            if(usersAuthenticated.containsKey(user.getId())) usersAuthenticated.get(user.getId()).add(deviceDto);
            else usersAuthenticated.put(user.getId(), new HashSet<>(Arrays.asList(deviceDto)));
        }
        connectedUsersDto.setUsersAuthenticated(usersAuthenticated);
        return connectedUsersDto;
    }

    public void remove(Session session) {
        log.info("remove - session id: " + session.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        if(authenticatedSessionMap.containsKey(session.getId())) authenticatedSessionMap.remove(session.getId());
        Device device = null;
        if( (device = (Device) session.getUserProperties().get("device")) != null) deviceSessionMap.remove(device.getId());
        User user = null;
        if( (user = (User) session.getUserProperties().get("user")) != null) {
            Set<Device> deviceSet = userDeviceMap.get(user.getId());
            for(Device userDevice: deviceSet) {
                if(userDevice.getId().longValue() == device.getId().longValue()) deviceSet.remove(userDevice);
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

    public Set<Device> getUserDeviceSet(Long userId) {
        Set<Device> result = userDeviceMap.get(userId);
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

    public void sendMessage(List<Device> userDeviceList, SocketMessageDto messageDto) throws ExceptionVS {
        for(Device device : userDeviceList) {
            Session deviceSession = deviceSessionMap.get(device.getId());
            if(messageDto.getSessionId() != null) {
                if(!sendMessage(messageDto, deviceSession.getId())) deviceSessionMap.remove(device.getId());
            } else log.log(Level.SEVERE, "sendMessage - device id '" + device.getId() + "' has no active sessions");
        }
    }

    public boolean sendMessageByTargetDeviceId(SocketMessageDto messageDto) throws ExceptionVS, JsonProcessingException {
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
