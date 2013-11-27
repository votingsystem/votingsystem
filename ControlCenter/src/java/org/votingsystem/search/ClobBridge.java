package org.votingsystem.search;

import org.hibernate.search.bridge.StringBridge;
import org.votingsystem.util.StringUtils;

import java.sql.Clob;

public class ClobBridge implements StringBridge {

    @Override
    public String objectToString(Object object){
        return StringUtils.getStringFromClob((Clob) object);
    }

}
