package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.util.TypeVS;

import java.util.Collection;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultListDto<T> {

    private Collection<T> resultList;
    private Integer offset;
    private Integer statusCode;
    private Integer max;
    private Long totalCount;
    private Object state;
    private String message;
    private TypeVS type;

    public ResultListDto() { }

    public ResultListDto(Collection<T> resultList) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
    }

    public ResultListDto(Collection<T> resultList, TypeVS type) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
        this.type = type;
    }

    public ResultListDto(Collection<T> resultList, Integer offset, Integer max, Long totalCount) {
        this.resultList = resultList;
        this.offset = offset;
        this.max = max;
        this.totalCount = totalCount;
    }

    public ResultListDto(Collection<T> resultList, Integer offset, Integer max, Integer totalCount) {
        this(resultList, offset, max, Long.valueOf(totalCount));
    }

    public static <T> ResultListDto GROUP(Collection<T> groupList, Object state, Integer offset, Integer max, Long totalCount) {
        ResultListDto<T> result = new ResultListDto<T>();
        result.setResultList(groupList);
        result.setState(state);
        result.setOffset(offset);
        result.setMax(max);
        result.setTotalCount(totalCount);
        return result;
    }

    public Collection<T> getResultList() {
        return resultList;
    }

    public void setResultList(Collection<T> resultList) {
        this.resultList = resultList;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Object getState() {
        return state;
    }

    public void setState(Object state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TypeVS getType() {
        return type;
    }

    public void setType(TypeVS type) {
        this.type = type;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
}
