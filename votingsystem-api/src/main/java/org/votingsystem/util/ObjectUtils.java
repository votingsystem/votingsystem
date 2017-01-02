package org.votingsystem.util;


import java.io.*;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ObjectUtils {

    private static final Logger log = Logger.getLogger(ObjectUtils.class.getName());

    public static byte[] serializeObject(Serializable serializable) {
        byte[] base64EncodedSerializedObject = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(serializable);
            oos.close();
            base64EncodedSerializedObject = Base64.getEncoder().encode(baos.toByteArray());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return base64EncodedSerializedObject;
    }

    public static String serializeObjectToString(Serializable serializable) {
        String base64EncodedSerializedObject = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(serializable);
            oos.close();
            base64EncodedSerializedObject = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return base64EncodedSerializedObject;
    }

    public static Serializable deSerializeObject(byte[] base64SerializedObject) {
        Serializable deserializedObject = null;
        try {
            byte [] data = Base64.getDecoder().decode(base64SerializedObject);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            deserializedObject  = (Serializable) ois.readObject();
            ois.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return deserializedObject;
    }

    public static void copyFields(Object source, Object target) {
        Field[] fieldsSource = source.getClass().getFields();
        Field[] fieldsTarget = target.getClass().getFields();
        for (Field fieldTarget : fieldsTarget) {
            for (Field fieldSource : fieldsSource) {
                if (fieldTarget.getName().equals(fieldSource.getName())) {
                    try {
                        fieldTarget.set(target, fieldSource.get(source));
                    }
                    catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                    break;
                }
            }
        }
    }

}
