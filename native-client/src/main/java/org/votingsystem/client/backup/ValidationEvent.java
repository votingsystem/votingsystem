package org.votingsystem.client.backup;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationEvent<T> {

    public enum Type {
        ACCESS_REQUEST, ACCESS_REQUEST_FINISH, VOTE, VOTE_FINISH;
    }

    private Integer statusCode;
    private Type type;
    private String message;
    private List<String> errorList;
    private T data;

    public ValidationEvent() {}

    public ValidationEvent(Integer statusCode, Type type) {
        this.statusCode = statusCode;
        this.type = type;
    }

    public ValidationEvent(Integer statusCode, Type type, String message) {
        this.statusCode = statusCode;
        this.type = type;
        this.message = message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public ValidationEvent setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Type getType() {
        return type;
    }

    public ValidationEvent setType(Type type) {
        this.type = type;
        return this;
    }

    public T getData() {
        return data;
    }

    public ValidationEvent setData(T data) {
        this.data = data;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ValidationEvent setMessage(String message) {
        this.message = message;
        return this;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public ValidationEvent setErrorList(List<String> errorList) {
        this.errorList = errorList;
        return this;
    }

}