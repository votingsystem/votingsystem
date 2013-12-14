package org.votingsystem.util;

import org.hibernate.search.bridge.StringBridge;

import java.sql.Clob;

public class ClobBridge implements StringBridge {

    @Override
    public String objectToString(Object object){
        return StringUtils.getStringFromClob((Clob) object);
    }

}
