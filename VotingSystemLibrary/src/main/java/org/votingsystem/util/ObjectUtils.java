package org.votingsystem.util;


import java.io.*;
import java.util.Base64;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ObjectUtils {

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


}
