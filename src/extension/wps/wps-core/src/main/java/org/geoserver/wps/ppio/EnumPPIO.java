package org.geoserver.wps.ppio;

import java.lang.reflect.Method;

public class EnumPPIO extends LiteralPPIO {

    public EnumPPIO(Class type) {
        super(type);
    }

    @Override
    public Object decode(String value) throws Exception {
        if (value == null) {
            throw new IllegalArgumentException("Unable to look up enum value from null");
        }

        Method valueOf = getType().getMethod("valueOf", String.class);
        try {
            return valueOf.invoke(null, value);
        }
        catch(Exception e) {
            //try upper case
            try {
                return valueOf.invoke(null, value.toUpperCase());
            }
            catch(Exception e1) {
                //try lower case
                try {
                    return valueOf.invoke(null, value.toLowerCase());
                }
                catch(Exception e2) {
                    //give up and throw back first exception
                    throw e;
                }
            }
        }
    }
    
    @Override
    public String encode(Object value) throws Exception {
        return ((Enum)value).name();
    }
}
