package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.GroupVS
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

    def get() {
        Map resultMap = [:]
        GroupVS result

        if (params.long('id')) {

            GroupVS.withTransaction {
                result = GroupVS.get(params.long('id'))
            }
            if(result) resultMap = groupVSService.getGroupVSDataMap(result)
        }
        if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
            //render resultMap as JSON
            render result as JSON
        } else {
            render(view:'get', model: [groupvsMap:resultMap])
        }
    }

    def index() {
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
        def resultMap = ["${message(code: 'groupvsRecordsLbl')}":resultList, queryRecordCount: totalGroups,
                         numTotalGroups:totalGroups ]
        render resultMap as JSON
    }

}