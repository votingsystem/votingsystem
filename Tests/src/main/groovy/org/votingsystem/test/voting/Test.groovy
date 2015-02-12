package org.votingsystem.test.voting

import org.apache.log4j.Logger
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils

import java.text.Normalizer

Logger log = TestUtils.init(Test.class, [:])

String test = "http://éspaña@a"


log.debug(test + ": " + StringUtils.getNormalized(test))