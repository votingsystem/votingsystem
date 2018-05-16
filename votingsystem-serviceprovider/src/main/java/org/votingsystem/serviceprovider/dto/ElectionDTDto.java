package org.votingsystem.serviceprovider.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.ElectionOption;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;

@JacksonXmlRootElement(localName = "ResultList")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElectionDTDto {

    @JsonProperty("UUID")
    private String UUID;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("date")
    private String date;
    @JsonProperty("state")
    private String state;
    @JsonProperty("content")
    private String content;

    @JsonProperty("options")
    private Collection<String> optionList;

    public ElectionDTDto(Election election) {
        this.subject = election.getSubject();
        this.publisher = election.getPublisher().getFullName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss zzz");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(election.getDateBegin(), ZoneId.systemDefault());
        this.date = formatter.format(zonedDateTime);
        this.state = election.getState().name();
        this.content = election.getContent();
        this.UUID = election.getUUID();
        optionList = new ArrayList<>();
        for(ElectionOption electionOption : election.getElectionOptions()) {
            optionList.add(electionOption.getContent());
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Collection<String> getOptionList() {
        return optionList;
    }

    public void setOptionList(Collection<String> optionList) {
        this.optionList = optionList;
    }

    @JsonIgnore
    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
