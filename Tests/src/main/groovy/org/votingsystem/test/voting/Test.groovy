package org.votingsystem.test.voting

import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.test.util.TestUtils
import org.votingsystem.test.util.TransactionVSAnalizer
import org.votingsystem.util.FileUtils
import org.votingsystem.util.HttpHelper

Logger log = TestUtils.init(Test.class, [:])

File signedFile = new File("/home/jgzornoza/6b7d033c-d927-431e-ad2b-e15f3c863eca")


SMIMEMessage smimeMessage = new SMIMEMessage(new FileInputStream(signedFile))
smimeMessage.writeTo(System.out)

log.debug("=====" + smimeMessage.getSignedContent())



