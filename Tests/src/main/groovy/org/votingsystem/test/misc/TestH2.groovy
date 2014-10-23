package org.votingsystem.test.misc

import org.apache.log4j.Logger
import org.votingsystem.test.util.H2Utils
import org.votingsystem.test.util.TestUtils

Logger log = TestUtils.init(TestH2.class, "./H2Databases")
H2Utils.testBlob()
//H2Utils.testPlainText()
TestUtils.finish("OK")
