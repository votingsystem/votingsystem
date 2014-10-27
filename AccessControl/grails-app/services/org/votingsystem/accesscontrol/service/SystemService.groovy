package org.votingsystem.accesscontrol.service

import grails.transaction.Transactional
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ExceptionVS

@Transactional
class SystemService {

    def grailsApplication
    private ControlCenterVS controlCenter
    private Locale defaultLocale
    def subscriptionVSService

    public ControlCenterVS getControlCenter() throws Exception {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        if(!controlCenter) {
            List<ControlCenterVS> controlCenters = ControlCenterVS.createCriteria().list (offset: 0, order:'desc') {
                eq("state", ActorVS.State.OK)
            }
            if(!controlCenters.isEmpty()) {
                controlCenter = controlCenters.iterator().next()
                ResponseVS responseVS = subscriptionVSService.checkControlCenter(controlCenter.serverURL)
                if(ResponseVS.SC_OK == responseVS.statusCode) {
                    log.debug("There are '${controlCenters.getTotalCount()}' with state 'OK' - fetching Control Center " +
                            "with url: '${controlCenter.serverURL}' ")
                } else {
                    log.error("$methodName - ${responseVS.getMessage()}")
                    controlCenter = null
                    throw new ExceptionVS(responseVS.getMessage())
                }
            } else log.error("Missing Control Center!!!!")
        }
        return controlCenter;
    }

    public Locale getLocale() {
        if(!defaultLocale) defaultLocale = new Locale(grailsApplication.config.vs.defaultLocale)
        return defaultLocale
    }

}