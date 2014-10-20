package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils


/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class GroupVSController {

	def grailsApplication;
    def groupVSService
    def userVSService
    def subscriptionVSService

    def index() {
        UserVS.State state = null
        if (params.long('id')) {
            def result
            Map resultMap = [:]
            GroupVS.withTransaction {
                result = GroupVS.get(params.long('id'))
            }
            if(result) resultMap = groupVSService.getGroupVSDataMap(result)
            else {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_NOT_FOUND,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            if(request.contentType?.contains("json")) {
                render resultMap as JSON
            } else render(view:'groupvs', model: [groupvsMap:resultMap])
        }
        List<GroupVS> groupVSList = null
        GroupVS.withTransaction {
            Date dateFrom = null
            Date dateTo = null
            try {state = UserVS.State.valueOf(params.state)} catch(Exception ex) {state = UserVS.State.ACTIVE}
            //searchFrom:2014/04/14 00:00:00, max:100, searchTo
            if(params.searchFrom) try {dateFrom = DateUtils.getDateFromString(params.searchFrom)} catch(Exception ex) {}
            if(params.searchTo) try {dateTo = DateUtils.getDateFromString(params.searchTo)} catch(Exception ex) {}
            groupVSList = GroupVS.createCriteria().list(max: params.max, offset: params.offset) {
                or {
                    eq("state", state)
                    ilike('name', "%${params.searchText}%")
                    ilike('description', "%${params.searchText}%")
                }
                and {
                    if(dateFrom && dateTo) {between("dateCreated", dateFrom, dateTo)}
                    else if(dateFrom) {ge("dateCreated", dateFrom)}
                    else if(dateTo) {le("dateCreated", dateTo)}
                }
            }
        }
        def resultList = []
        groupVSList.each {groupItem ->
            resultList.add(groupVSService.getGroupVSDataMap(groupItem))
        }
        Map resultMap = [groupvsList:resultList, offset:params.offset, max: params.max,
                 totalCount:groupVSList.totalCount, state:state.toString()]
        if(request.contentType?.contains("json")) render resultMap as JSON
        else render(view: "index", model: [groupVSList:resultMap])
    }

    def balance() {
        GroupVS groupVS
        GroupVS.withTransaction { groupVS = GroupVS.get(params.long('id')) }
        if(!groupVS) return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_NOT_FOUND,
                    message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
        Map resultMap = groupVSService.getDataWithBalancesMap(groupVS, DateUtils.getCurrentWeekPeriod())

        if(request.contentType?.contains("json")) render resultMap as JSON
        else render(view:'groupvs', model: [groupvsMap:resultMap])
    }

    def newGroup (){
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIME = request.messageSMIMEReq
            if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
            return [responseVS:groupVSService.saveGroup(messageSMIME)]
        }
    }

    def edit() {
        if(params.long('id')) {
            GroupVS groupVS
            Map resultMap = [:]
            GroupVS.withTransaction {
                groupVS = GroupVS.get(params.long('id'))
            }
            if("POST".equals(request.method)) {
                MessageSMIME messageSMIMEReq = request.messageSMIMEReq
                if(!messageSMIMEReq) {
                    return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
                }
                ResponseVS responseVS = groupVSService.editGroup(groupVS, messageSMIMEReq)
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    String URL = "${createLink(controller: 'groupVS', absolute:true)}/${groupVS.id}"
                    responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'vicketGroupEditedOKMsg',
                            args:[groupVS.name]), URL:URL]
                    responseVS.setContentType(ContentTypeVS.JSON)
                }
                return [responseVS:responseVS]
            } else {
                if(groupVS) resultMap = groupVSService.getGroupVSDataMap(groupVS)
                render(view:'edit', model: [groupvsMap:resultMap])
            }
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    def cancel() {
        if(params.long('id')) {
            GroupVS groupVS
            Map resultMap = [:]
            GroupVS.withTransaction {
                groupVS = GroupVS.get(params.long('id'))
            }
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = groupVSService.cancelGroup(groupVS, messageSMIMEReq)
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String URL = "${createLink(controller: 'groupVS', absolute:true)}/${groupVS.id}"
                responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'vicketGroupCancelledOKMsg',
                        args:[groupVS.name]), URL:URL]
                responseVS.setContentType(ContentTypeVS.JSON)
            }
            return [responseVS:responseVS]
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    def subscribe() {
        if(params.long('id')) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            return [responseVS:groupVSService.subscribe(messageSMIMEReq)]
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }


    def user() {
        List subscriptionVSList = null
        SubscriptionVS.withTransaction {
            subscriptionVSList = SubscriptionVS.createCriteria().list(max: params.max, offset: params.offset) {
                eq("groupVS.id", params.long('id'))
                eq("userVS.id", params.long('userId'))
            }
        }
        if(subscriptionVSList.isEmpty()) {
            return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                    message: message(code: 'groupUserNotFoundMsg', args:[params.id, params.userId]))]
        } else {
            Map subscriptionMap = userVSService.getSubscriptionVSDetailedDataMap(subscriptionVSList.iterator().next())
            if(request.contentType?.contains("json")) {
                render subscriptionMap as JSON
            } else render(view:'user', model: [subscriptionMap:subscriptionMap])
        }
    }

    def users() {
        if(params.long('id')) {
            def result
            SubscriptionVS.State state
            if(params.state) try {state = SubscriptionVS.State.valueOf(params.state)} catch(Exception ex) {}
            SubscriptionVS.withTransaction {
                result = SubscriptionVS.createCriteria().list(max: params.max, offset: params.offset) {
                    eq("groupVS.id", params.long('id'))
                    if(state)eq("state", state)
                }
            }
            def resultList = []
            result.each {item ->
                resultList.add(userVSService.getUserVSDataMap(item.userVS, false))
            }
            Map resultMap = ["${message(code: 'uservsRecordsLbl')}":resultList, offset:params.offset, max:params.max,
                    totalCount:result.totalCount ]
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    def listUsers() {
        if(params.long('id')) {
            GroupVS groupVS = null
            GroupVS.withTransaction { groupVS = GroupVS.get(params.long('id')) }
            if(!groupVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            SubscriptionVS.State subscriptionState
            if(params.subscriptionState) try {subscriptionState = SubscriptionVS.State.valueOf(
                    params.subscriptionState)} catch(Exception ex) {}

            UserVS.State state = UserVS.State.ACTIVE
            try {state = UserVS.State.valueOf(params.userVSState)} catch(Exception ex) {}
            if(request.contentType?.contains("json")) {
                def userList
                SubscriptionVS.withTransaction {
                    userList = SubscriptionVS.createCriteria().list(max: params.max, offset: params.offset) {
                        eq("groupVS", groupVS)
                        if(params.searchText) {
                            userVS {
                                or {
                                    ilike('name', "%${params.searchText}%")
                                    ilike('firstName', "%${params.searchText}%")
                                    ilike('lastName', "%${params.searchText}%")
                                    ilike('nif', "%${params.searchText}%")
                                }
                                and { eq("state", state) }
                            }
                        } else {
                            userVS { eq("state", state) }
                            if(subscriptionState) eq("state", subscriptionState)
                        }
                    }
                }
                def resultList = []
                userList.each {userItem ->
                    resultList.add(userVSService.getSubscriptionVSDataMap(userItem))
                }
                Map resultMap = [userVSList:resultList, offset:params.offset, max:params.max,
                                 totalCount:userList.totalCount]
                render resultMap as JSON
            } else {
                render(view:'listUsers', model: [subscriptionMap:[:]])
            }
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    def activateUser () {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS: ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:subscriptionVSService.activateUser(messageSMIME)]
    }

    def deActivateUser () {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS: ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:subscriptionVSService.deActivateUser(messageSMIME)]
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}