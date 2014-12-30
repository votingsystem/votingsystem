package org.votingsystem.test.cooin

import org.apache.log4j.Logger
import org.votingsystem.signature.util.AESParams
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils

Logger log = TestUtils.init(AESParams.class)

Date date = DateUtils.getDateFromString("2014/12/30 00:00:00")
Calendar result = Calendar.getInstance();
result.setTime(date)
log.debug("result: " + result.getTime() + " - DAY_OF_YEAR: " + result.get(Calendar.DAY_OF_YEAR))
result.set(Calendar.DAY_OF_YEAR, (result.get(Calendar.DAY_OF_YEAR) -7));
log.debug("result: " + result.getTime() + " - DAY_OF_YEAR: " + result.get(Calendar.DAY_OF_YEAR))

System.exit(0)