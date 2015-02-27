package org.votingsystem.test.cooin

import org.apache.log4j.Logger
import org.votingsystem.test.util.TestUtils
import org.votingsystem.util.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat

Logger log = TestUtils.init(Test.class)

Calendar calendar = Calendar.getInstance();
DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

Date date = calendar.getTime()
log.debug "date: ${date.toString()}"
String dateStr = dateFormat.format(date)
log.debug "ISO formatted date: ${dateStr}"

Date parsedDate = DateUtils.getDateFromString(dateStr)
log.debug "parsed date: ${parsedDate.toString()}"

Calendar parsedCalendar = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr)
log.debug "parsedCalendar: ${parsedCalendar.getTime()}"

System.exit(0)