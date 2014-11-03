package org.votingsystem.vicket.websocket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ExceptionVS;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSHelper {

    private static Logger logger = Logger.getLogger(SessionVSHelper.class);

    private static final ConcurrentHashMap<String, SessionVS> sessionMap = new ConcurrentHashMap<String, SessionVS>();
    private static final ConcurrentHashMap<Long, String> deviceSessionMap = new ConcurrentHashMap<Long, String>();

    private static final SessionVSHelper instance = new SessionVSHelper();

    public void put(Session session) {
        logger.debug("put - Session id: " + session.getId());
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), new SessionVS(session));
        } else logger.debug("put - Session already in sessionMap");
    }

    public void put(Session session, UserVS userVS) throws ExceptionVS {
        logger.debug("put - Session id: " + session.getId() + " - User id:" + userVS.getId());
        SessionVS sessionVS = sessionMap.get(session.getId());
        if(sessionVS == null) {
            sessionMap.put(session.getId(), new SessionVS(session, userVS));
            deviceSessionMap.put(userVS.getDeviceVS().getId(), session.getId());
        } else if(!deviceSessionMap.contains(userVS.getDeviceVS().getId())) {
            if(sessionVS.getUserVS() == null) sessionVS.setUserVS(userVS);
            else if(sessionVS.getUserVS().getId() != userVS.getId()){ throw new ExceptionVS("SessionVS with existing user");}
            deviceSessionMap.putIfAbsent(userVS.getDeviceVS().getId(), session.getId());
        } else logger.debug("put - Session already in sessionMap");
    }

    public Collection<Long> getConnectedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getConnectedUsers() {
        Set<Long> result = new HashSet();
        for(Long deviceId : deviceSessionMap.keySet()) {
            result.add(sessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId());
        }
        return result;
    }

    public Map<Long, Set> getConnectedUsersDataMap() {
        Map<Long, Set> result = new HashMap();
        for(Long deviceId : deviceSessionMap.keySet()) {
            Long userVSId = sessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId();
            Set<Long> deviceSet = result.get(userVSId);
            if(deviceSet != null) deviceSet.add(deviceId);
            else result.put(userVSId,  new HashSet<>(Arrays.asList(deviceId)));
        }
        return result;
    }

    public SessionVS remove(Session session) {
        logger.debug("remove - Session id: " + session.getId());
        SessionVS removedSessionVS = sessionMap.remove(session.getId());
        while (deviceSessionMap.values().remove(session.getId()));
        return removedSessionVS;
    }

    public static SessionVSHelper getInstance() {
        return instance;
    }

    public SessionVS get(Session session) {
        return sessionMap.get(session.getId());
    }

    public SessionVS get(Long deviceId) {
        String sessionId = deviceSessionMap.get(deviceId);
        if(sessionId == null) {
            logger.debug("get - deviceId: '" + deviceId + "' has not active session");
            return null;
        }
        return sessionMap.get(sessionId);
    }

    public synchronized void broadcast(JSONObject messageJSON) {
        String messageStr = messageJSON.toString();
        logger.debug("broadcast - message: " + messageStr);
        Enumeration<SessionVS> sessions = sessionMap.elements();
        while(sessions.hasMoreElements()) {
            SessionVS sessionVS = sessions.nextElement();
            Session session = sessionVS.getSession();
            try {
                session.getBasicRemote().sendText(messageJSON.toString());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                try {
                    sessionMap.values().remove(sessionVS);
                    session.close();
                } catch (IOException ex1) {// Ignore
                }
            }
        }
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        logger.debug("broadcastList");
        JSONObject messageJSON = new JSONObject(dataMap);
        String messageStr = messageJSON.toString();
        //log.debug("--- broadcastList - message: " + messageStr)
        List<String> errorList = new ArrayList<String>();
        for(String listener : listeners) {
            if(listener == null) {
                listeners.remove(listener);
                continue;
            }
            SessionVS sessionVS = sessionMap.get(listener);
            Session session = sessionVS.getSession();
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    try {
                        sessionMap.values().remove(sessionVS);
                        session.close();
                    } catch (IOException e1) {// Ignore
                    }
                }
            } else {
                errorList.add(listener);
            }
        }
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        responseVS.setData(errorList);
        return responseVS;
    }

    public void sendMessage(List<DeviceVS> userVSDeviceList, String message) {
        for(DeviceVS device : userVSDeviceList) {
            String sessionId = deviceSessionMap.get(device.getId());
            if(sessionId != null) sendMessage(sessionId, message);
        }
    }

    public void sendMessage(String sessionId, String message) {
        SessionVS sessionVS = sessionMap.get(sessionId);
        if(sessionVS != null) {
            if(sessionVS.getSession().isOpen()) {
                try {
                    sessionVS.getSession().getBasicRemote().sendText(message);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    remove(sessionVS.getSession());
                }
            } else {
                logger.debug (" **** Lost message for session '" + sessionId + "' - message: " + message);
                remove(sessionVS.getSession());
            }
        }
    }
}
