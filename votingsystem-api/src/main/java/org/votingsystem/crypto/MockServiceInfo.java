package org.votingsystem.crypto;

import eu.europa.esig.dss.tsl.ServiceInfo;

import java.util.Calendar;

public class MockServiceInfo extends ServiceInfo {

    public MockServiceInfo() {
        setTspName("votingsystem, Mock Office DSS-CA");
        setServiceName("votingsystem, Mock Service Name");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -15);
    }

}