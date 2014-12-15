package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.model.TransactionVS

@Transactional
class DashBoardService {


    @Transactional public Map getUserVSInfo(DateUtils.TimePeriod timePeriod) {
        Map result = [timePeriod:timePeriod.getMap("yyyy/MM/dd' 'HH:mm:ss")]
        result.numTransFromBankVS = TransactionVS.countByToUserVSIsNotNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_BANKVS, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransFromUserVS = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_USERVS, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransFromUserVSToUserVS = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_USERVS_TO_USERVS, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransFromGroupVSToMember = TransactionVS.countByToUserVSIsNotNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_MEMBER, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.transFromGroupVSToMemberGroup = [numTrans:TransactionVS.countByToUserVSIsNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP, timePeriod.getDateFrom(), timePeriod.getDateTo()),
                numUsers:TransactionVS.countByToUserVSIsNotNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP, timePeriod.getDateFrom(), timePeriod.getDateTo())]
        result.numTransFromGroupVSToMemberGroup = TransactionVS.countByToUserVSIsNotNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_MEMBER_GROUP, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransFromGroupVSToAllMembers = [numTrans:TransactionVS.countByToUserVSIsNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS, timePeriod.getDateFrom(), timePeriod.getDateTo()),
                numUsers:TransactionVS.countByToUserVSIsNotNullAndTypeAndDateCreatedBetween(
                TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS, timePeriod.getDateFrom(), timePeriod.getDateTo())]
        result.numTransCooinInitPeriod = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.COOIN_INIT_PERIOD, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCooinInitPeriodTimeLimited = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.COOIN_INIT_PERIOD_TIME_LIMITED, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCooinRequest = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.COOIN_REQUEST, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCooinSend = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.COOIN_SEND, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCooinCancellation = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.COOIN_CANCELLATION, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCancellation = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.CANCELLATION, timePeriod.getDateFrom(), timePeriod.getDateTo())
        return result
    }

}
