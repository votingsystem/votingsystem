package org.votingsystem.currency.web.ejb;

import org.votingsystem.dto.ResponseDto;
import org.votingsystem.service.EJBAdminRemoteCurrencyServer;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Stateless
@Remote(EJBAdminRemoteCurrencyServer.class)
public class AdminRemoteEJB implements EJBAdminRemoteCurrencyServer {

    private static final Logger log = Logger.getLogger(AdminRemoteEJB.class.getName());

    @Inject private AuditEJB auditBean;
    @Inject private ConfigCurrencyServer config;

    @Asynchronous
    @Override
    public Future<ResponseDto> initWeekPeriod(ZonedDateTime requestDate) throws IOException {
        log.info("requestDate: " + requestDate);
        try {
            auditBean.initWeekPeriod(requestDate.toLocalDateTime());
            return new AsyncResult<>(ResponseDto.OK());
        } catch (Exception e) {
            return new AsyncResult<>(ResponseDto.ERROR(e.getMessage()));
        }
    }

}