package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.util.OperationType;

import java.util.Collection;

@JacksonXmlRootElement(localName = "ResultList")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultListDto<T> {

    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private OperationType type;
    @JacksonXmlProperty(localName = "Offset", isAttribute = true)
    private Integer offset;
    @JacksonXmlProperty(localName = "StatusCode", isAttribute = true)
    private Integer statusCode;
    @JacksonXmlProperty(localName = "Max", isAttribute = true)
    private Integer max;
    @JacksonXmlProperty(localName = "TotalCount", isAttribute = true)
    private Long totalCount;
    @JacksonXmlProperty(localName = "Message")
    private String message;
    @JacksonXmlProperty(localName = "Base64Data")
    private String base64Data;

    @JacksonXmlElementWrapper(useWrapping = true, localName = "ItemList")
    @JacksonXmlProperty(localName = "Item")
    private Collection<T> resultList;

    public ResultListDto() { }

    public ResultListDto(Collection<T> resultList) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
    }

    public ResultListDto(Collection<T> resultList, OperationType type) {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }
}
