package org.votingsystem.jsf;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@FacesConverter("org.votingsystem.jsf.dateConverter")
public class LocalDateFacesConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String stringValue) {
        if (null == stringValue || stringValue.isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return formatter.parse(stringValue);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object localDateTimeValue) {
        if (null == localDateTimeValue) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return formatter.format((LocalDateTime) localDateTimeValue);
    }

}