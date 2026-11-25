package ee.taltech.ite4120.guidelines.model;

/** Reference row from {@code guidelines.domain} — the lookup table of 01-domain.sql. */
public class Domain {

    private String code;
    private String name;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
