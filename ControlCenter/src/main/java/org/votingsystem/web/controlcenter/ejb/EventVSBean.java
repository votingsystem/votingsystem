package org.votingsystem.web.controlcenter.ejb;

import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Logger;

@Stateless
public class EventVSBean {

    private static Logger log = Logger.getLogger(EventVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;





}
