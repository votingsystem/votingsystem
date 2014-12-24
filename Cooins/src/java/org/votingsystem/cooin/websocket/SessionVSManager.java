package org.votingsystem.cooin.websocket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSManager {

    private static Logger log = Logger.getLogger(SessionVSManager.class);

    private static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    private static final ConcurrentHashMap<String, SessionVS> authenticatedSessionMap = new ConcurrentHashMap<String, SessionVS>();
    private static final ConcurrentHashMap<Long, String> deviceSessionMap = new ConcurrentHashMap<Long, String>();
    private static final SessionVSManager instance = new SessionVSManager();
    private static final ConcurrentHashMap<Long, Set<DeviceVS>> connectedUserVSDeviceMap = new ConcurrentHashMap<Long, Set<DeviceVS>>();

    private SessionVSManager() { }

    public void put(Session session) {
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), session);
        } else log.debug("put - session already in sessionMap");
    }

    public void putAuthenticatedDevice(Session session, UserVS userVS) throws ExceptionVS {
        log.debug("put - authenticatedSessionMap - session id: " + session.getId() + " - user id:" + userVS.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        SessionVS sessionVS = authenticatedSessionMap.get(session.getId());
        if(sessionVS == null) {
            authenticatedSessionMap.put(session.getId(), new SessionVS(session, userVS));
            deviceSessionMap.put(userVS.getDeviceVS().getId(), session.getId());
            if(connectedUserVSDeviceMap.containsKey(userVS.getId())) {
                connectedUserVSDeviceMap.get(userVS.getId()).add(userVS.getDeviceVS());
            } else connectedUserVSDeviceMap.put(userVS.getId(), new HashSet<DeviceVS>(Arrays.asList(userVS.getDeviceVS())));
        } else log.debug("put - session already in authenticatedSessionMap");
    }

    public Collection<Long> getAuthenticatedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getAuthenticatedUsers() {
        return Collections.list(connectedUserVSDeviceMap.keys());
    }

    public Set<Map> connectedDeviceMap(Long userId) {
        if(!connectedUserVSDeviceMap.containsKey(userId)) return new HashSet<>();
        else return connectedUserVSDeviceMap.get(userId).stream().map(d -> {
                Map<String, Object> result = new HashMap<String, Object>();
                result.put("id", d.getId());
                result.put("name", d.getDeviceName());
                return result;
            }).collect(Collectors.toSet());
    }

    public Map<Long, Set> getConnectedUsersDataMap() {
        Map result = new HashMap();
        Map notAuthenticatedDataMap = new HashMap();
        notAuthenticatedDataMap.put("numUsersNotAuthenticated", sessionMap.size());
        List<String> notAuthenticatedSessionList = new ArrayList<>();
        for(Session session : sessionMap.values()) {
            notAuthenticatedSessionList.add(session.getId());
        }
        notAuthenticatedDataMap.put("sessionIdList", notAuthenticatedSessionList);
        Map<Long, Set> authUsersMap = new HashMap();
        for(Long deviceId : deviceSessionMap.keySet()) {
            Map deviceInfoMap = new HashMap();
            deviceInfoMap.put("deviceId", deviceId);
            deviceInfoMap.put("sessionId", deviceSessionMap.get(deviceId));
            Long userVSId = authenticatedSessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId();
            Set userDeviceSet = authUsersMap.get(userVSId);
            if(userDeviceSet == null) userDeviceSet = new HashSet<>();
            userDeviceSet.add(deviceInfoMap);
            authUsersMap.put(userVSId,  userDeviceSet);
        }
        result.put("authenticatedUsers", authUsersMap);
        result.put("notAuthenticatedUsers", notAuthenticatedDataMap);
        return result;
    }

    public void remove(Session session) {
        log.debug("remove - session id: " + session.getId());
        if(sessionMap.containsKey(session.getId())) sessionMap.remove(session.getId());
        if(authenticatedSessionMap.containsKey(session.getId())) {
            SessionVS removedSessionVS = authenticatedSessionMap.remove(session.getId());
            if(deviceSessionMap.containsValue(session.getId())) {
                while (deviceSessionMap.values().remove(session.getId()));
            }
            if(connectedUserVSDeviceMap.containsKey(removedSessionVS.getUserVS().getId())) {
                Set<DeviceVS> userDevices = connectedUserVSDeviceMap.get(removedSessionVS.getUserVS().getId()).stream().
                        filter(d -> { if(removedSessionVS.getUserVS().getDeviceVS().getId() == d.getId()) return false;
                        else return true; }).collect(toSet());
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
        return authenticatedSessionMap.get(sessionId).getSession();
    }

    public Session getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public SessionVS get(Long deviceId) {
        if(!deviceSessionMap.containsKey(deviceId)) {
            log.debug("get - deviceId: '" + deviceId + "' has not active session");
            return null;
        }
        return authenticatedSessionMap.get(deviceSessionMap.get(deviceId));
    }

    public synchronized void broadcast(JSONObject messageJSON) {
        String messageStr = messageJSON.toString();
        log.debug("broadcast - message: " + messageStr + " to '" + sessionMap.size() + "' users NOT authenticated");
        Enumeration<Session> sessions = sessionMap.elements();
        while(sessions.hasMoreElements()) {
            Session session = sessions.nextElement();
            try {
                session.getBasicRemote().sendText(messageJSON.toString());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public synchronized void broadcastToAuthenticatedUsers(JSONObject messageJSON) {
        String messageStr = messageJSON.toString();
        log.debug("broadcastToAuthenticatedUsers - message: " + messageStr + " to '" + authenticatedSessionMap.size() +
                "' users authenticated");
        Enumeration<SessionVS> sessions = authenticatedSessionMap.elements();
        while(sessions.hasMoreElements()) {
            SessionVS sessionVS = sessions.nextElement();
            Session session = sessionVS.getSession();
            try {
                session.getBasicRemote().sendText(messageJSON.toString());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        log.debug("broadcastList");
        String messageStr = new JSONObject(dataMap).toString();
        List<String> errorList = new ArrayList<String>();
        for(String listener : listeners) {
            Session session = sessionMap.get(listener);
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
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

    public ResponseVS broadcastAuthenticatedList(Map dataMap, Set<String> listeners) {
        log.debug("broadcastAuthenticatedList");
        String messageStr = new JSONObject(dataMap).toString();
        List<String> errorList = new ArrayList<String>();
        for(String listener : listeners) {
            SessionVS sessionVS = authenticatedSessionMap.get(listener);
            Session session = sessionVS.getSession();
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
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

    public void sendMessage(List<DeviceVS> userVSDeviceList, String message) throws ExceptionVS {
        for(DeviceVS device : userVSDeviceList) {
            String sessionId = deviceSessionMap.get(device.getId());
            if(sessionId != null) {
                if(!sendMessage(sessionId, message)) deviceSessionMap.remove(device.getId());
            } else log.error("device id '" + device.getId() + "' has no active sessions");
        }
    }

    public boolean sendMessageToDevice(Long deviceId, String message) throws ExceptionVS{
        if(!deviceSessionMap.containsKey(deviceId)) return false;
        else return sendMessage(authenticatedSessionMap.get(deviceSessionMap.get(deviceId)).getSession().getId(), message);
    }

    public boolean sendMessage(String sessionId, String message) throws ExceptionVS {
        if(sessionId == null) throw new ExceptionVS("null sessionId");
        if(authenticatedSessionMap.containsKey(sessionId)) return sendMessageToAuthenticatedUser(sessionId, message);
        if(sessionMap.containsKey(sessionId)) {
            Session session = sessionMap.get(sessionId);
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                    return true;
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
            remove(session);
        }
        log.debug ("sendMessage - lost message for session '" + sessionId + "' - message: " + message);
        return false;
    }

    private boolean sendMessageToAuthenticatedUser(String sessionId, String message) {
        if(!authenticatedSessionMap.containsKey(sessionId)) return false;
        Session session = authenticatedSessionMap.get(sessionId).getSession();
        if(session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                return true;
            } catch (Exception ex) { log.error(ex.getMessage(), ex); }
        }
        log.debug ("sendMessageToAuthenticatedUser - lost message for session '" + sessionId + "' - message: " + message);
        remove(session);
        return false;
    }

}
