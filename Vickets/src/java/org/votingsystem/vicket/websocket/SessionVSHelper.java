package org.votingsystem.vicket.websocket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.vicket.util.ApplicationContextHolder;
import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionVSHelper {

    private static Logger logger = Logger.getLogger(SessionVSHelper.class);

    private static final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    private static final ConcurrentHashMap<String, SessionVS> authenticatedSessionMap = new ConcurrentHashMap<String, SessionVS>();
    private static final ConcurrentHashMap<Long, String> deviceSessionMap = new ConcurrentHashMap<Long, String>();
    private PluginAwareResourceBundleMessageSource messageSource;
    private static final SessionVSHelper instance = new SessionVSHelper();

    private SessionVSHelper() {
        messageSource = (PluginAwareResourceBundleMessageSource) ApplicationContextHolder.getBean("messageSource");
    }

    public void put(Session session) {
        logger.debug("put - session id: " + session.getId());
        if(!sessionMap.containsKey(session.getId())) {
            sessionMap.put(session.getId(), session);
        } else logger.debug("put - session already in sessionMap");
    }

    public void put(Session session, UserVS userVS) throws ExceptionVS {
        logger.debug("put - authenticatedSessionMap - session id: " + session.getId() + " - User id:" + userVS.getId());
        if(sessionMap.contains(session.getId())) sessionMap.remove(session.getId());
        SessionVS sessionVS = authenticatedSessionMap.get(session.getId());
        if(sessionVS == null) {
            authenticatedSessionMap.put(session.getId(), new SessionVS(session, userVS));
            deviceSessionMap.put(userVS.getDeviceVS().getId(), session.getId());
        } else if(!deviceSessionMap.contains(userVS.getDeviceVS().getId())) {
            if(sessionVS.getUserVS().getId() != userVS.getId()){ throw new ExceptionVS("SessionVS with existing user");}
            deviceSessionMap.putIfAbsent(userVS.getDeviceVS().getId(), session.getId());
        } else logger.debug("put - Session already in authenticatedSessionMap");
    }

    public Collection<Long> getConnectedDevices() {
        return Collections.list(deviceSessionMap.keys());
    }

    public Collection<Long> getConnectedUsers() {
        Set<Long> result = new HashSet();
        for(Long deviceId : deviceSessionMap.keySet()) {
            result.add(authenticatedSessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId());
        }
        return result;
    }

    public Map<Long, Set> getConnectedUsersDataMap() {
        Map result = new HashMap();
        result.put("numUsersNotAuthenticated", sessionMap.size());
        Map<Long, Set> authUsersMap = new HashMap();
        for(Long deviceId : deviceSessionMap.keySet()) {
            Long userVSId = authenticatedSessionMap.get(deviceSessionMap.get(deviceId)).getUserVS().getId();
            Set<Long> deviceSet = authUsersMap.get(userVSId);
            if(deviceSet != null) deviceSet.add(deviceId);
            else authUsersMap.put(userVSId,  new HashSet<>(Arrays.asList(deviceId)));
        }
        result.put("authenticatedUsers", authUsersMap);
        return result;
    }

    public SessionVS remove(Session session) {
        logger.debug("remove - session id: " + session.getId());
        SessionVS removedSessionVS = authenticatedSessionMap.remove(session.getId());
        while (deviceSessionMap.values().remove(session.getId()));
        while (deviceSessionMap.values().remove(session.getId()));
        return removedSessionVS;
    }

    public static SessionVSHelper getInstance() {
        return instance;
    }

    public SessionVS getAuthenticatedSession(Session session) {
        return authenticatedSessionMap.get(session.getId());
    }

    public Session getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public SessionVS get(Long deviceId) {
        String sessionId = deviceSessionMap.get(deviceId);
        if(sessionId == null) {
            logger.debug("get - deviceId: '" + deviceId + "' has not active session");
            return null;
        }
        return authenticatedSessionMap.get(sessionId);
    }

    public synchronized void broadcast(JSONObject messageJSON) {
        String messageStr = messageJSON.toString();
        logger.debug("broadcast - message: " + messageStr);
        Enumeration<Session> sessions = sessionMap.elements();
        while(sessions.hasMoreElements()) {
            Session session = sessions.nextElement();
            try {
                session.getBasicRemote().sendText(messageJSON.toString());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                try {
                    sessionMap.values().remove(session.getId());
                    session.close();
                } catch (IOException ex1) {// Ignore
                }
            }
        }
    }

    public synchronized void broadcastToAuthenticatedUsers(JSONObject messageJSON) {
        String messageStr = messageJSON.toString();
        logger.debug("broadcastToAuthenticatedUsers - message: " + messageStr);
        Enumeration<SessionVS> sessions = authenticatedSessionMap.elements();
        while(sessions.hasMoreElements()) {
            SessionVS sessionVS = sessions.nextElement();
            Session session = sessionVS.getSession();
            try {
                session.getBasicRemote().sendText(messageJSON.toString());
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                try {
                    authenticatedSessionMap.values().remove(sessionVS);
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
            SessionVS sessionVS = authenticatedSessionMap.get(listener);
            Session session = sessionVS.getSession();
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    try {
                        authenticatedSessionMap.values().remove(sessionVS);
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

    public ResponseVS broadcastAuthenticatedList(Map dataMap, Set<String> listeners) {
        logger.debug("broadcastAuthenticatedList");
        JSONObject messageJSON = new JSONObject(dataMap);
        String messageStr = messageJSON.toString();
        //log.debug("--- broadcastList - message: " + messageStr)
        List<String> errorList = new ArrayList<String>();
        for(String listener : listeners) {
            if(listener == null) {
                listeners.remove(listener);
                continue;
            }
            SessionVS sessionVS = authenticatedSessionMap.get(listener);
            Session session = sessionVS.getSession();
            if(session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(messageStr);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    try {
                        authenticatedSessionMap.values().remove(sessionVS);
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

    public ResponseVS sendMessage(Long deviceId, String deviceName, String message) {
        ResponseVS result = null;
        SessionVS sessionVS = authenticatedSessionMap.get(deviceSessionMap.get(deviceId));
        if(sessionVS != null) {
            if(sessionVS.getSession().isOpen()) {
                try {
                    sessionVS.getSession().getBasicRemote().sendText(message);
                    result = new ResponseVS(ResponseVS.SC_PROCESSING);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    remove(sessionVS.getSession());
                }
            } else {
                logger.debug (" **** Lost message for deviceId '" + deviceId);
                remove(sessionVS.getSession());
            }
        }
        if(result == null) result = new ResponseVS(ResponseVS.SC_ERROR, messageSource.getMessage(
                "webSocketDeviceSessionNotFoundErrorMsg", Arrays.asList(deviceName).toArray(), getLocale()));
        return result;
    }

    public void sendMessage(String sessionId, String message) {
        SessionVS sessionVS = authenticatedSessionMap.get(sessionId);
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
