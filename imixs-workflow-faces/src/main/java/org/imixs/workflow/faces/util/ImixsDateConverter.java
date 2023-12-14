package org.imixs.workflow.faces.util;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter("ImixsDateConverter")
public class ImixsDateConverter implements Converter, Serializable {

    private static final String DEFAULT_DATE_FORMAT_PATTERN = "yyyy-mm-dd";
    private static final String DEFAULT_TIME_ZONE = "UTC"; // Set the timezone to CET

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {

        if (value == null || value.trim().isEmpty()) {
            return ""; // Convert null to empty string
        }

        try {
            value = value.trim();
            Map<String, Object> map = component.getAttributes();
            String pattern = DEFAULT_DATE_FORMAT_PATTERN;
            String timezone = DEFAULT_TIME_ZONE;

            if (map != null && map.containsKey("org.imixs.date.pattern")) {
                pattern = map.get("org.imixs.date.pattern").toString();
            }
            if (map != null && map.containsKey("org.imixs.date.timeZone")) {
                timezone = map.get("org.imixs.date.timeZone").toString();
            }

            DateFormat dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
            return dateFormat.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException("Error converting date", e);
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null || "".equals(value)) {
            return ""; // Convert null to empty string
        }

        if (value instanceof Date) {
            Map<String, Object> map = component.getAttributes();
            String pattern = DEFAULT_DATE_FORMAT_PATTERN;
            String timezone = DEFAULT_TIME_ZONE;

            if (map != null && map.containsKey("org.imixs.date.pattern")) {
                pattern = map.get("org.imixs.date.pattern").toString();
            }
            if (map != null && map.containsKey("org.imixs.date.timeZone")) {
                timezone = map.get("org.imixs.date.timeZone").toString();
            }

            DateFormat dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
            return dateFormat.format((Date) value);
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
        }
    }
}