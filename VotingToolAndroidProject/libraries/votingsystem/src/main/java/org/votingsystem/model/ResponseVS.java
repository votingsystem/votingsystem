package org.votingsystem.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.ByteArrayInputStream;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ResponseVS<T> implements Parcelable {

    private static final long serialVersionUID = 1L;

    public static final int SC_OK                       = 200;
    public static final int SC_OK_WITHOUT_BODY          = 204;
    public static final int SC_OK_CANCEL_ACCESS_REQUEST = 270;
    public static final int SC_REQUEST_TIMEOUT          = 408;
    public static final int SC_ERROR_REQUEST            = 400;
    public static final int SC_NOT_FOUND                = 404;
    public static final int SC_ERROR_REQUEST_REPEATED   = 409;
    public static final int SC_EXCEPTION                = 490;
    public static final int SC_NULL_REQUEST             = 472;
    public static final int SC_ERROR                    = 500;
    public static final int SC_CONNECTION_TIMEOUT       = 522;
    public static final int SC_ERROR_TIMESTAMP          = 570;
    public static final int SC_PROCESSING               = 700;
    public static final int SC_TERMINATED               = 710;
    public static final int SC_CANCELLED                = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;


    private int statusCode;
    private Integer iconId = -1;
    private StatusVS<?> status;
    private EventVSResponse eventQueryResponse;
    private String caption;
    private String notificationMessage;
    private String message;
    private String serviceCaller;
    private T data;
    private TypeVS typeVS;
    private SMIMEMessageWrapper smimeMessage;
    private byte[] smimeMessageBytes;
    private ContentTypeVS contentType = ContentTypeVS.TEXT;
    private byte[] messageBytes;
    private Uri uri;


    public ResponseVS() {  }

    public ResponseVS(Parcel parcel) {
        readFromParcel(parcel);
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
                      TypeVS typeVS) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
              TypeVS typeVS, Integer iconId) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.iconId = iconId;
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
              TypeVS typeVS, SMIMEMessageWrapper smimeMessage) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.smimeMessage = smimeMessage;
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
            TypeVS typeVS, SMIMEMessageWrapper smimeMessage, Integer iconId) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.smimeMessage = smimeMessage;
        this.iconId = iconId;
    }

    public ResponseVS(int statusCode, String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
    
    public ResponseVS(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }
    
    public ResponseVS(int statusCode) {
        this.statusCode = statusCode;
    }
    public ResponseVS(TypeVS typeVS, T data) {
        this.typeVS = typeVS;
        this.data = data;
    }


    public static ResponseVS getExceptionResponse(String caption, String message) {
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        responseVS.setCaption(caption);
        responseVS.setNotificationMessage(message);
        return responseVS;
    }

    public String getMessage() {
        String result = null;
        try {
            if(message == null && messageBytes != null) result = new String(messageBytes,
                    ContextVS.UTF_8);
            else result = message;
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            return result;
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EventVSResponse getEventQueryResponse() {
        return eventQueryResponse;
    }

    public void setEventQueryResponse(EventVSResponse eventQueryResponse) {
        this.eventQueryResponse = eventQueryResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}

    public SMIMEMessageWrapper getSmimeMessage() {
        if(smimeMessage == null && smimeMessageBytes != null) {
            try {
                smimeMessage = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(smimeMessageBytes), null);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return smimeMessage;
    }

	public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
		this.smimeMessage = smimeMessage;
	}

    public <E> StatusVS<E> getStatus() {
        return (StatusVS<E>)status;
    }

    public <E> void setStatus(StatusVS<E> status) {
        this.status = status;
    }

    public ContentTypeVS getContentType() {
        return contentType;
    }

    public void setContentType(ContentTypeVS contentType) {
        this.contentType = contentType;
    }

    public Integer getIconId() {
        return iconId;
    }

    public void setIconId(Integer iconId) {
        this.iconId = iconId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getServiceCaller() {
        return serviceCaller;
    }

    public void setServiceCaller(String serviceCaller) {
        this.serviceCaller = serviceCaller;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getLogStr() {
        return "statusCode: " + getStatusCode() + " - serviceCaller: " + getServiceCaller() +
                " - caption: " + getCaption() + " - message:" + getNotificationMessage();

    }

    public static final Parcelable.Creator<ResponseVS> CREATOR =
            new Parcelable.Creator<ResponseVS>() {

        @Override public ResponseVS createFromParcel(Parcel source) {
            return new ResponseVS(source);
        }

        @Override public ResponseVS[] newArray(int size) {
            return new ResponseVS[size];
        }

    };

    @Override public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel source) {
        // Must read values in the same order as they were placed in
        statusCode = source.readInt();
        iconId = source.readInt();
        serviceCaller = source.readString();
        caption = source.readString();
        notificationMessage = source.readString();
        message = source.readString();
        eventQueryResponse = (EventVSResponse) source.readSerializable();
        typeVS = (TypeVS) source.readSerializable();
        contentType = (ContentTypeVS) source.readSerializable();
        messageBytes = new byte[source.readInt()];
        source.readByteArray(messageBytes);
        smimeMessageBytes = new byte[source.readInt()];
        source.readByteArray(smimeMessageBytes);
        uri = source.readParcelable(Uri.class.getClassLoader());
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(statusCode);
        parcel.writeInt(iconId);
        parcel.writeString(serviceCaller);
        parcel.writeString(caption);
        parcel.writeString(notificationMessage);
        parcel.writeString(message);
        parcel.writeSerializable(eventQueryResponse);
        parcel.writeSerializable(typeVS);
        parcel.writeSerializable(contentType);
        if(messageBytes != null) {
            parcel.writeInt(messageBytes.length);
            parcel.writeByteArray(messageBytes);
        } else {
            parcel.writeInt(0);
            parcel.writeByteArray(new byte[0]);
        }
        if(smimeMessage != null) {
            try {
                byte[] smimeMessageBytes = smimeMessage.getBytes();
                parcel.writeInt(smimeMessageBytes.length);
                parcel.writeByteArray(smimeMessageBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        } else {
            parcel.writeInt(0);
            parcel.writeByteArray(new byte[0]);
        }
        parcel.writeParcelable(uri, flags);
    }

    public String getNotificationMessage() {
        if(notificationMessage == null) return getMessage();
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

}