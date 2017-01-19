package org.votingsystem.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum CountryEurope {

    @JsonProperty("BE")
    BE("Belgique/België", "Belgium", 0),
    @JsonProperty("BG")
    BG("България", "Bulgaria", 1),
    @JsonProperty("CZ")
    CZ("Česká republika", "Czech Republic", 2),
    @JsonProperty("DK")
    DK("Danmark", "Denmark", 3),
    @JsonProperty("DE")
    DE("Deutschland", "Germany", 4),
    @JsonProperty("EE")
    EE("Eesti", "Estonia", 5),
    @JsonProperty("IE")
    IE("Éire/Ireland", "Ireland", 6),
    @JsonProperty("EL")
    EL("Ελλάδα", "Greece", 7),
    @JsonProperty("ES")
    ES("España", "Spain", 8),
    @JsonProperty("FR")
    FR("France", "France", 9),
    @JsonProperty("HR")
    HR("Hrvatska", "Croatia", 10),
    @JsonProperty("IT")
    IT("Italia", "Italy", 11),
    @JsonProperty("CY")
    CY("Κύπρος", "Cyprus", 12),
    @JsonProperty("LV")
    LV("Latvija", "Latvia", 13),
    @JsonProperty("LT")
    LT("Lietuva", "Lithuania", 14),
    @JsonProperty("LU")
    LU("Luxembourg", "Luxembourg", 15),
    @JsonProperty("HU")
    HU("Magyarország", "Hungary", 16),
    @JsonProperty("MT")
    MT("Malta", "Malta", 17),
    @JsonProperty("NL")
    NL("Nederland", "Netherlands", 17),
    @JsonProperty("AT")
    AT("Österreich", "Austria", 18),
    @JsonProperty("PL")
    PL("Polska", "Poland", 19),
    @JsonProperty("PT")
    PT("Portugal", "Portugal", 20),
    @JsonProperty("RO")
    RO("România", "Romania", 21),
    @JsonProperty("SI")
    SI("Slovenija", "Slovenia", 22),
    @JsonProperty("SK")
    SK("Slovensko", "Slovakia", 23),
    @JsonProperty("FI")
    FI("Suomi/Finland", "Finland", 24),
    @JsonProperty("SE")
    SE("Sverige", "Sweden", 25),
    @JsonProperty("UK")
    UK("United Kingdom", "United Kingdom", 26) ;


    private String name;
    private String englishName;
    private int position;

    private CountryEurope(String name, String englishName, int position) {
        this.setName(name);
        this.setEnglishName(englishName);
        this.setPosition(position);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

}
