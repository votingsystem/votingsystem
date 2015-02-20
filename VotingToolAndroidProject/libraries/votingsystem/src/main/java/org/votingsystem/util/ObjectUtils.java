package org.votingsystem.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.bouncycastle2.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

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
            base64EncodedSerializedObject = Base64.encode(baos.toByteArray());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return base64EncodedSerializedObject;
    }

    public static String serializeObjectToString(Serializable serializable) {
        byte[] serializedBytes = serializeObject(serializable);
        return new String(serializedBytes);
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

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

}
