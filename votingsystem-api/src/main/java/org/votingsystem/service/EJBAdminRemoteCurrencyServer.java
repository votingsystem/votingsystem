package org.votingsystem.service;

import org.votingsystem.dto.ResponseDto;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.Future;

public interface EJBAdminRemoteCurrencyServer {

    public Future<ResponseDto> initWeekPeriod(ZonedDateTime requestDate) throws IOException;

}
