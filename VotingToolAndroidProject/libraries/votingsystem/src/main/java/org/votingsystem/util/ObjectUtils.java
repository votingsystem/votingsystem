package org.votingsystem.util;

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

    public static byte[] serializeObject(Serializable serializable) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        //return new String(Base64.encode(baos.toByteArray()));
        return baos.toByteArray();
    }

    public static Object deSerializedObject(byte[] serializedObject) throws IOException,
            ClassNotFoundException {
        //byte [] data = Base64.decode(base64SerializedObject);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedObject));
        Object object  = ois.readObject();
        ois.close();
        return object;
    }
}
