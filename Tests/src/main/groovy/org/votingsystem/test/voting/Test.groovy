package org.votingsystem.test.voting

import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.HttpHelper

Logger log = TestUtils.init(Test.class, [:])

ResponseVS responseVS = HttpHelper.getInstance().getData("https://www.sistemavotacion.org/AccessControl/serverInfo", null)
log.debug("responseVS: " + responseVS.getMessage())
