package org.votingsystem.jsf;

import org.votingsystem.util.DateUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@FacesConverter("org.votingsystem.jsf.dateTimeConverter")
public class LocalDateTimeFacesConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String stringValue) {
        if (null == stringValue || stringValue.isEmpty()) {
            return null;
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(stringValue);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object localDateTimeValue) {
        if (null == localDateTimeValue) {
            return "";
        }
        return DateUtils.getISO_OFFSET_DATE_TIME((LocalDateTime) localDateTimeValue);
    }

}