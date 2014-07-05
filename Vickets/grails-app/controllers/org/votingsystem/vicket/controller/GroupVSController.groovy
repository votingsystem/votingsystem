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
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class GroupVSController {

	def grailsApplication;
    def groupVSService
    def userVSService
    def subscriptionVSService

    def index() {
        if (params.long('id')) {
            def result
            Map resultMap = [:]
            GroupVS.withTransaction {
                result = GroupVS.get(params.long('id'))
            }
            if(result) resultMap = groupVSService.getGroupVSDetailedDataMap(result, DateUtils.getCurrentWeekPeriod())
            else {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_NOT_FOUND,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            if(request.contentType?.contains("json")) {
                render resultMap as JSON
            } else render(view:'group', model: [groupvsMap:resultMap])
        } else if(request.contentType?.contains("json")) {
            Map resultMap = [:]
            def result
            List<GroupVS> groupList = null
            GroupVS.withTransaction {
                UserVS.State state = null
                Date dateFrom = null
                Date dateTo = null
                try {state = UserVS.State.valueOf(params.state)} catch(Exception ex) {state = UserVS.State.ACTIVE}
                //searchFrom:2014/04/14 00:00:00, max:100, searchTo
                if(params.searchFrom) try {dateFrom = DateUtils.getDateFromString(params.searchFrom)} catch(Exception ex) {}
                if(params.searchTo) try {dateTo = DateUtils.getDateFromString(params.searchTo)} catch(Exception ex) {}
                groupList = GroupVS.createCriteria().list(max: params.max, offset: params.offset) {
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
            groupList.each {groupItem ->
                resultList.add(groupVSService.getGroupVSDataMap(groupItem))
            }
            resultMap = [groupvsList:resultList, queryRecordCount: groupList.totalCount,
                         numTotalGroups:groupList.totalCount ]
            render resultMap as JSON
        }
    }

    def newGroup (){
        if("POST".equals(request.method)) {
            MessageSMIME messageSMIMEReq = request.messageSMIMEReq
            if(!messageSMIMEReq) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
            }
            ResponseVS responseVS = groupVSService.saveGroup(messageSMIMEReq, request.getLocale())
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                GroupVS newGroupVS = responseVS.data
                String URL = "${createLink(controller: 'groupVS', absolute:true)}/${newGroupVS.id}"
                responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'newVicketGroupOKMsg',
                        args:[newGroupVS.name]), URL:URL]
                responseVS.setContentType(ContentTypeVS.JSON)
            }
            return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getUserVS()?.getCertificate()]
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
                ResponseVS responseVS = groupVSService.editGroup(groupVS, messageSMIMEReq, request.getLocale())
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
            ResponseVS responseVS = groupVSService.cancelGroup(groupVS, messageSMIMEReq, request.getLocale())
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
            ResponseVS responseVS = groupVSService.subscribe(messageSMIMEReq, request.getLocale())
            return [responseVS:responseVS]
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }


    def user() {
        GroupVS groupVS = null
        UserVS userVS = null
        grails.orm.PagedResultList subscriptionVSList = null
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

    def listUsers() {
        if(params.long('id')) {
            GroupVS groupVS = null
            Map resultMap = [:]
            GroupVS.withTransaction { groupVS = GroupVS.get(params.long('id')) }
            if(!groupVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            resultMap = [groupName: groupVS.name, id:groupVS.id]
            UserVS.State state = UserVS.State.ACTIVE
            try {state = UserVS.State.valueOf(params.state)} catch(Exception ex) {}
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
                        }
                    }
                }
                def resultList = []
                userList.each {userItem ->
                    resultList.add(userVSService.getSubscriptionVSDataMap(userItem))
                }
                resultMap = [userVSList:resultList, queryRecordCount: userList.totalCount,
                             numTotalUsers:userList.totalCount ]
                render resultMap as JSON
            } else {
                render(view:'listUsers', model: [subscriptionMap:resultMap])
            }
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    def activateUser () {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = subscriptionVSService.activateUser(messageSMIMEReq, request.getLocale())
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SubscriptionVS subscription = responseVS.data
            responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'vicketGroupUserActivatedMsg',
                    args:[subscription.userVS.nif, subscription.groupVS.name])]
            responseVS.setContentType(ContentTypeVS.JSON)
        }
        return [responseVS:responseVS]
    }

    def deActivateUser () {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = subscriptionVSService.deActivateUser(messageSMIMEReq, request.getLocale())
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            SubscriptionVS subscription = responseVS.data
            responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'vicketGroupUserdeActivatedMsg',
                    args:[subscription.userVS.nif, subscription.groupVS.name])]
            responseVS.setContentType(ContentTypeVS.JSON)
        }
        return [responseVS:responseVS]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${rootCause.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: metaInf,
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: message(code:'paramsErrorMsg'),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }
}