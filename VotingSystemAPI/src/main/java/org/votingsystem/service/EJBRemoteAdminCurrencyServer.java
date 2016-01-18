package org.votingsystem.service;


import org.votingsystem.model.ResponseVS;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Future;

public interface EJBRemoteAdminCurrencyServer {

    public Future<ResponseVS> initWeekPeriod(Calendar requestDate) throws IOException;

}
