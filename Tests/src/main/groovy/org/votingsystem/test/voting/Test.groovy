package org.votingsystem.test.voting

import net.sf.json.JSONObject
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.HttpHelper

import java.text.DateFormat
import java.text.SimpleDateFormat

Logger log = TestUtils.init(Test.class, [:])

String dateStr = "http://cooins/test"
log.debug("======= " + dateStr.split("//")[1])
