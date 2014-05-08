package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class GroupVSController {

	def grailsApplication;
    def groupVSService
    def userVSService

    def index() {
        if (params.long('id')) {
            def result
            Map resultMap = [:]
            GroupVS.withTransaction {
                result = GroupVS.get(params.long('id'))
            }
            if(result) resultMap = groupVSService.getGroupVSDataMap(result)
            if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
                render resultMap as JSON
            } else {
                render(view:'group', model: [groupvsMap:resultMap])
            }
        } else if(request.contentType?.contains("json")) {
            Map resultMap = [:]
            def result
            List<GroupVS> groupList = null
            int totalGroups = 0;
            GroupVS.withTransaction {
                if(params.searchText || params.searchFrom || params.searchTo || params.state) {
                    GroupVS.State state = null
                    Date dateFrom = null
                    Date dateTo = null
                    try {state = GroupVS.State.valueOf(params.state)} catch(Exception ex) {}
                    //searchFrom:2014/04/14 00:00:00, max:100, searchTo
                    if(params.searchFrom) try {dateFrom = DateUtils.getDateFromString(params.searchFrom)} catch(Exception ex) {}
                    if(params.searchTo) try {dateTo = DateUtils.getDateFromString(params.searchTo)} catch(Exception ex) {}

                    groupList = GroupVS.createCriteria().list(max: params.max, offset: params.offset) {
                        or {
                            if(state) eq("state", state)
                            ilike('name', "%${params.searchText}%")
                            ilike('description', "%${params.searchText}%")
                        }
                        and {
                            if(dateFrom && dateTo) {between("dateCreated", dateFrom, dateTo)}
                            else if(dateFrom) {ge("dateCreated", dateFrom)}
                            else if(dateTo) {le("dateCreated", dateTo)}
                        }
                    }
                    totalGroups = groupList.totalCount
                } else {
                    groupList = GroupVS.createCriteria().list(max: params.max, offset: params.offset){ };
                    totalGroups = groupList.totalCount
                }
            }
            def resultList = []
            groupList.each {groupItem ->
                resultList.add(groupVSService.getGroupVSDataMap(groupItem))
            }
            resultMap = ["${message(code: 'groupvsRecordsLbl')}":resultList, queryRecordCount: totalGroups,
                         numTotalGroups:totalGroups ]
            render resultMap as JSON
        }
    }

    def admin() {}

    def newGroup (){
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = null
            try {
                responseVS = groupVSService.saveGroup(messageSMIMEReq, request.getLocale())
            } catch(Exception ex) {
                log.error (ex.getMessage(), ex)
                String msg = message(code:'publishGroupVSErrorMessage')
                responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR, message: msg, reason:msg, type:TypeVS.VICKET_ERROR)
            }
            return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getUserVS()?.getCertificate()]
        } else {

        }
    }

    def subscribe() {
        if(params.long('id')) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = null
            try {
                responseVS = groupVSService.subscribe(messageSMIMEReq, request.getLocale())
            } catch(Exception ex) {
                log.error (ex.getMessage(), ex)
                String msg = message(code:'publishGroupVSErrorMessage')
                responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR, message: msg, reason:msg, type:TypeVS.VICKET_ERROR)
            }
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }


    def users() {
        if(params.long('id')) {
            def result
            Map resultMap = [:]
            SubscriptionVS.withTransaction {
                result = SubscriptionVS.createCriteria().list(max: params.max, offset: params.offset) {
                    eq("groupVS.id", params.long('id'))
                }
            }
            def resultList = []
            result.each {item ->
                resultList.add(userVSService.getUserVSDataMap(item.userVS))
            }
            int totalCount = result.totalCount
            resultMap = ["${message(code: 'uservsRecordsLbl')}":resultList, queryRecordCount: totalCount,
                         numTotalUsers:totalCount ]
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }


    def test() {

    }

}