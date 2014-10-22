package org.votingsystem.test.vicket

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSAnalizer
import org.votingsystem.util.HttpHelper

Logger log = TestUtils.init(Test.class, [:])
String serviceURL = "http://vickets:8086/Vickets/balance/weekdb/2014/10/20"

ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.JSON);
TransactionVSAnalizer transactionVSPlan = TransactionVSAnalizer.parse(JSONSerializer.toJSON(responseVS.getMessage()))
log.debug("transactionVSPlan: ${transactionVSPlan.getTimePeriod()}")
Map userReports = transactionVSPlan.getReport(UserVS.Type.USER)
log.debug("userReports: $userReports")

Map groupReports = transactionVSPlan.getReport(UserVS.Type.GROUP)
log.debug("groupReports: $groupReports")
