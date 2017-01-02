package org.votingsystem.dto.voting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.votingsystem.model.voting.ElectionOption;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JacksonXmlRootElement(localName = "ElectionOption")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElectionOptionDto {

    @JacksonXmlProperty(localName = "Content")
    private String content;
    @JacksonXmlProperty(localName = "NumVotes")
    private Long numVotes;

    public ElectionOptionDto() {}

    public ElectionOptionDto(ElectionOption electionOption) {
        this.content = electionOption.getContent();
    }

    public ElectionOptionDto(String content, Long numVotes) {
        this.content = content;
        this.numVotes = numVotes;
    }

    public String getContent() {
        return content;
    }

    public Long getNumVotes() {
        return numVotes;
    }

    public ElectionOptionDto setContent(String content) {
        this.content = content;
        return this;
    }

    public ElectionOptionDto setNumVotes(Long numVotes) {
        this.numVotes = numVotes;
        return this;
    }
}
