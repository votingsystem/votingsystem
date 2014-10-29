package org.votingsystem.vicket.service

import grails.transaction.Transactional
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS

@Transactional
class DashBoardService {


    @Transactional public Map getUserVSInfo(DateUtils.TimePeriod timePeriod) {
        Map result = [timePeriod:timePeriod.getMap()]
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
        result.numTransVicketInitPeriod = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.VICKET_INIT_PERIOD, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransVicketInitPeriodTimeLimited = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.VICKET_INIT_PERIOD_TIME_LIMITED, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransVicketRequest = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.VICKET_REQUEST, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransVicketSend = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.VICKET_SEND, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransVicketCancellation = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.VICKET_CANCELLATION, timePeriod.getDateFrom(), timePeriod.getDateTo())
        result.numTransCancellation = TransactionVS.countByTypeAndDateCreatedBetween(
                TransactionVS.Type.CANCELLATION, timePeriod.getDateFrom(), timePeriod.getDateTo())
        return result
    }

}
