package org.votingsystem.util;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.EventVSResponse;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.StatusVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;

import java.io.ByteArrayInputStream;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ResponseVS<T> implements Parcelable {

    private static final long serialVersionUID = 1L;

    public static final String TAG = ResponseVS.class.getSimpleName();

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
    public static final int SC_WS_CONNECTION_INIT_OK    = 800;
    public static final int SC_WS_MESSAGE_SEND_OK       = 801;
    public static final int SC_WS_MESSAGE_ENCRYPTED     = 810;
    public static final int SC_WS_CONNECTION_INIT_ERROR = 840;
    public static final int SC_WS_CONNECTION_NOT_FOUND  = 841;
    public static final int SC_CANCELLED                = 0;
    public static final int SC_INITIALIZED              = 1;
    public static final int SC_PAUSED                   = 10;


    private int statusCode;
    private Integer iconId = null;
    private StatusVS<?> status;
    private OperationVS operation;
    private EventVSResponse eventQueryResponse;
    private String caption;
    private String notificationMessage;
    private String message;
    private String serviceCaller;
    private String url;
    private T data;
    private TypeVS typeVS;
    private SMIMEMessage smimeMessage;
    private byte[] smimeMessageBytes;
    private ContentTypeVS contentType = ContentTypeVS.TEXT;
    private byte[] messageBytes;
    private Uri uri;
    private JSONObject messageJSON;


    public ResponseVS() {  }

    public ResponseVS(Parcel source) {
        // Must read values in the same order as they were placed in
        statusCode = source.readInt();
        iconId = (Integer) source.readValue(Integer.class.getClassLoader());
        serviceCaller = source.readString();
        caption = source.readString();
        notificationMessage = source.readString();
        message = source.readString();
        String messageJSONStr = (String) source.readValue(String.class.getClassLoader());
        try {if(messageJSONStr != null) messageJSON = new JSONObject(messageJSONStr);}
        catch (Exception ex) {ex.printStackTrace();}
        operation = (OperationVS) source.readParcelable(OperationVS.class.getClassLoader());
        eventQueryResponse = (EventVSResponse) source.readSerializable();
        typeVS = (TypeVS) source.readSerializable();
        contentType = (ContentTypeVS) source.readSerializable();
        messageBytes = new byte[source.readInt()];
        source.readByteArray(messageBytes);
        smimeMessageBytes = new byte[source.readInt()];
        source.readByteArray(smimeMessageBytes);
        uri = source.readParcelable(Uri.class.getClassLoader());
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
              TypeVS typeVS, SMIMEMessage smimeMessage) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.smimeMessage = smimeMessage;
    }

    public ResponseVS(int statusCode, String serviceCaller, String caption, String message,
            TypeVS typeVS, SMIMEMessage smimeMessage, Integer iconId) {
        this.statusCode = statusCode;
        this.serviceCaller = serviceCaller;
        this.caption = caption;
        this.message = message;
        this.typeVS = typeVS;
        this.smimeMessage = smimeMessage;
        this.iconId = iconId;
    }

    public ResponseVS(int statusCode, TypeVS typeVS) {
        this.statusCode = statusCode;
        this.typeVS = typeVS;
    }


    public ResponseVS(int statusCode, String message, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.message = message;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, SMIMEMessage smimeMessage) {
        this.statusCode = statusCode;
        this.smimeMessage = smimeMessage;
    }

    public ResponseVS(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public ResponseVS(int statusCode, String message, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.message = message;
        this.contentType = contentType;
    }

    public ResponseVS(int statusCode, byte[] messageBytes) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
    }

    public ResponseVS(int statusCode, byte[] messageBytes, ContentTypeVS contentType) {
        this.statusCode = statusCode;
        this.messageBytes = messageBytes;
        this.contentType = contentType;
    }

    public ResponseVS(int statusCode) {
        this.statusCode = statusCode;
    }
    public ResponseVS(TypeVS typeVS, T data) {
        this.typeVS = typeVS;
        this.data = data;
    }

    public static ResponseVS getResponse(Integer statusCode, String serviceCaller, String caption,
             String notificationMessage, TypeVS typeVS) {
        ResponseVS result = new ResponseVS(statusCode);
        result.setCaption(caption).setNotificationMessage(notificationMessage);
        result.setServiceCaller(serviceCaller);
        result.setTypeVS(typeVS);
        return result;
    }

    public static ResponseVS getExceptionResponse(String caption, String message) {
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_ERROR);
        responseVS.setCaption(caption);
        responseVS.setNotificationMessage(message);
        return responseVS;
    }

    public static ResponseVS getExceptionResponse(Exception ex, Context context) {
        String message = ex.getMessage();
        if(message == null || message.isEmpty()) message = context.getString(R.string.exception_lbl);
        return getExceptionResponse(context.getString(R.string.exception_lbl), message);
    }

    public String getMessage() {
        if(message == null && messageBytes != null) {
            try {
                message = new String(messageBytes, "UTF-8");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return message;
    }

    public JSONObject getMessageJSON() {
        if(messageJSON != null) return messageJSON;
        try {
            String message = getMessage();
            if(message != null) messageJSON = new JSONObject(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return messageJSON;
    }


    public ResponseVS setMessage(String message) {
        this.message = message;
        return this;
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

    public ResponseVS setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public ResponseVS setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
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

    public SMIMEMessage getSMIME() {
        if(smimeMessage == null && smimeMessageBytes != null) {
            try {
                smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return smimeMessage;
    }

	public void setSMIME(SMIMEMessage smimeMessage) {
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

    public ResponseVS setIconId(Integer iconId) {
        this.iconId = iconId;
        return this;
    }

    public String getCaption() {
        if(caption == null && operation != null) return operation.getSignedMessageSubject();
        else return caption;
    }

    public ResponseVS setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getServiceCaller() {
        return serviceCaller;
    }

    public ResponseVS setServiceCaller(String serviceCaller) {
        this.serviceCaller = serviceCaller;
        return this;
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

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(statusCode);
        parcel.writeValue(iconId);
        parcel.writeString(serviceCaller);
        parcel.writeString(caption);
        parcel.writeString(notificationMessage);
        parcel.writeString(message);
        if(messageJSON == null) parcel.writeValue(null);
        else parcel.writeValue(messageJSON.toString());
        parcel.writeParcelable(operation, flags);
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
        if(notificationMessage == null) {
            if (ContentTypeVS.JSON == contentType && getMessageJSON().has("message")) {
                try {
                    notificationMessage = getMessageJSON().getString("message");
                } catch(Exception ex) {
                    Log.e(TAG + ".getMessageJSON() ", ex.getMessage(), ex);
                }
            } else notificationMessage = getMessage();
        }
        return notificationMessage;
    }

    public ResponseVS setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
        return this;
    }

    public OperationVS getOperation() {
        return operation;
    }

    public void setOperation(OperationVS operation) {
        this.operation = operation;
    }

    public void setMessageJSON(JSONObject messageJSON) {
        this.messageJSON = messageJSON;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override public String toString() {
        return this.getClass().getSimpleName() + " - statusCode: " + statusCode +
                " - typeVS:" + typeVS;
    }
}