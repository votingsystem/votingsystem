package org.votingsystem.util;

import android.os.Bundle;
import android.os.Parcel;

import org.bouncycastle2.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ObjectUtils {

    public static byte[] serializeObject(Serializable serializable) {
        byte[] base64EncodedSerializedObject = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(serializable);
            oos.close();
            base64EncodedSerializedObject = Base64.encode(baos.toByteArray());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return base64EncodedSerializedObject;
    }

    public static Serializable deSerializeObject(byte[] base64SerializedObject) {
        Serializable deserializedObject = null;
        try {
            byte [] data = Base64.decode(base64SerializedObject);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            deserializedObject  = (Serializable) ois.readObject();
            ois.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return deserializedObject;
    }
}
