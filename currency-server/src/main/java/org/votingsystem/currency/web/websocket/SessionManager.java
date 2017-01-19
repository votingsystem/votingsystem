package org.votingsystem.currency.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.votingsystem.dto.ConnectedUsersDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
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
public class SessionManager {

    private static Logger log = Logger.getLogger(SessionManager.class.getName());

    private static String USER_KEY = "user";
    private static String DEVICE_KEY = "device";

    private ConcurrentHashMap<String, Session> sessionMap = null;
    private ConcurrentHashMap<String, Session> deviceSessionMap = null;
    private ConcurrentHashMap<Long, Set<Device>> userDeviceMap = null;

    private static final SessionManager instance = new SessionManager();

    private SessionManager() {
        sessionMap = new ConcurrentHashMap<>();
        deviceSessionMap = new ConcurrentHashMap<>();
        userDeviceMap = new ConcurrentHashMap<>();
    }

    public void put(Session session) {
        sessionMap.put(session.getId(), session);
    }

    public boolean hasSession(String deviceUUID) {
        return sessionMap.containsKey(deviceUUID);
    }

    public void putAuthenticatedDevice(Session session, User user, Device device) throws Exception {
        log.info("session id: " + session.getId() + " - User id:" + user.getId() + " - device UUID: " + device.getUUID());
        deviceSessionMap.put(device.getUUID(), session);
        if(userDeviceMap.containsKey(user.getId())) {
            Set<Device> deviceSet = userDeviceMap.get(user.getId()).stream().filter(dev ->
                !dev.getUUID().equals(device.getUUID())
            ).collect(Collectors.toSet());
            deviceSet.add(device);
            userDeviceMap.put(user.getId(), deviceSet);
        } else userDeviceMap.put(user.getId(), Sets.newHashSet(device));
        session.getUserProperties().put(USER_KEY, user);
        session.getUserProperties().put(DEVICE_KEY, device);
    }

    public Collection<String> getAuthenticatedDevices() {
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
        for(String deviceUUID : deviceSessionMap.keySet()) {
            Session deviceSession = deviceSessionMap.get(deviceUUID);
            Device device = (Device) deviceSession.getUserProperties().get(DEVICE_KEY);
            User user = (User) deviceSession.getUserProperties().get(USER_KEY);
            DeviceDto deviceDto = new DeviceDto().setUUID(device.getUUID());
            if(usersAuthenticated.containsKey(user.getId()))
                usersAuthenticated.get(user.getId()).add(deviceDto);
            else
                usersAuthenticated.put(user.getId(), new HashSet<>(Arrays.asList(deviceDto)));
        }
        connectedUsersDto.setUsersAuthenticated(usersAuthenticated);
        return connectedUsersDto;
    }

    public void remove(Session session) {
        log.info("remove - session id: " + session.getId());
        Device device = null;
        if( (device = (Device) session.getUserProperties().get(DEVICE_KEY)) != null)
            deviceSessionMap.remove(device.getId());
        User user = null;
        if( (user = (User) session.getUserProperties().get(USER_KEY)) != null) {
            Set<Device> deviceSet = userDeviceMap.get(user.getId());
            for(Device userDevice: deviceSet) {
                if(userDevice.getId().longValue() == device.getId().longValue()) deviceSet.remove(userDevice);
            }
        }
        sessionMap.remove(session.getId());
        try {
            session.close();
        } catch (Exception ex) {
            log.severe(ex.getMessage());
        }
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public Session getSession(String sessionId) {
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

    public Set<Device> getUserDeviceSet(Long userId) {
        Set<Device> result = userDeviceMap.get(userId);
        if(result == null) result = new HashSet<>();
        return result;
    }

    public Set<String> broadcast(String message, Set<String> uuidSet) {
        log.info(" sending broadcast to " + uuidSet.size() + " users");
        Set<String> brokenSessionSet = new HashSet<>();
        for(String listener : uuidSet) {
            Session session = deviceSessionMap.get(listener);
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

    public boolean sendMessageByTargetDeviceId(MessageDto messageDto) throws JsonProcessingException {
        Session deviceSession = deviceSessionMap.get(messageDto.getDeviceToUUID());
        if(deviceSession == null) return false;
        else return sendMessage(JSON.getMapper().writeValueAsString(messageDto), deviceSession.getId());
    }

    public Session getDeviceSession(String deviceUUID) throws JsonProcessingException {
        return deviceSessionMap.get(deviceUUID);
    }

    public boolean sendMessage(String message, String deviceUUID) {
        Session session = null;
        if((session = sessionMap.get(deviceUUID)) != null) {
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                    return true;
                } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex); }
            }
            log.info ("sendMessage - lost message to device '" + deviceUUID + "' - message: " + message);
            remove(session);
            return false;
        } else if((session = sessionMap.get(deviceUUID)) != null) {
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                    return true;
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            remove(session);
        }
        log.info("sendMessage - lost message to session '" + deviceUUID + "' - message: " + message);
        return false;
    }

}