package org.votingsystem.model;https://www.google.es/webhp?sourceid=chrome-instant&ion=1&espv=2&ie=UTF-8#q=...%20la%20raz%C3%B3n%20del%20rollo%20descerebrado%20es%20que%20a%20poco%20que%20te%20sientes%20un%20minuto%20a%20pensar%2C%20te%20dar%C3%A1s%20cuenta%20de%20que%20ya%20no%20hay%20posibilidad%20de%20burbujas

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum Country {

    BE("Belgique/België", "Belgium", 0),
    BG("България", "Bulgaria", 1),
    CZ("Česká republika", "Czech Republic", 2),
    DK("Danmark", "Denmark", 3),
    DE("Deutschland", "Germany", 4),
    EE("Eesti", "Estonia", 5),
    IE("Éire/Ireland", "Ireland", 6),
    EL("Ελλάδα", "Greece", 7),
    ES("España", "Spain", 8),
    FR("France", "France", 9),
    HR("Hrvatska", "Croatia", 10),
    IT("Italia", "Italy", 11),
    CY("Κύπρος", "Cyprus", 12),
    LV("Latvija", "Latvia", 13),
    LT("Lietuva", "Lithuania", 14),
    LU("Luxembourg", "Luxembourg", 15),
    HU("Magyarország", "Hungary", 16),
    MT("Malta", "Malta", 17),
    NL("Nederland", "Netherlands", 17),
    AT("Österreich", "Austria", 18),
    PL("Polska", "Poland", 19),
    PT("Portugal", "Portugal", 20),
    RO("România", "Romania", 21),
    SI("Slovenija", "Slovenia", 22),
    SK("Slovensko", "Slovakia", 23),
    FI("Suomi/Finland", "Finland", 24),
    SE("Sverige", "Sweden", 25),
    UK("United Kingdom", "United Kingdom", 26) ;


    private String name;
    private String englishName;
    private int position;

    private Country(String name, String englishName, int position) {
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
