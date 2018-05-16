package org.votingsystem.serviceprovider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.model.voting.Election;

import java.util.ArrayList;
import java.util.Collection;

@JacksonXmlRootElement(localName = "ResultList")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElectionsDTDto {

    @JsonProperty("draw")
    private int draw;
    @JsonProperty("recordsTotal")
    private int recordsTotal;
    @JsonProperty("recordsFiltered")
    private int recordsFiltered;

    @JsonProperty("data")
    private Collection<ElectionDTDto> electionList;

    @JsonProperty("error")
    private String error;

    public ElectionsDTDto(int draw, int recordsTotal, int recordsFiltered, Collection<Election> collections) {
        this.draw = draw;
        this.recordsTotal = recordsTotal;
        this.recordsFiltered = recordsFiltered;
        electionList = new ArrayList<>();
        if(collections != null) {
            for(Election election : collections) {
                electionList.add(new ElectionDTDto(election));
            }
        }
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public int getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(int recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public int getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(int recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public Collection<ElectionDTDto> getElectionList() {
        return electionList;
    }

    public void setElectionList(Collection<ElectionDTDto> electionList) {
        this.electionList = electionList;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
