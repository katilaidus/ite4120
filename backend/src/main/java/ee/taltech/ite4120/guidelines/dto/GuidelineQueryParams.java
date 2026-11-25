package ee.taltech.ite4120.guidelines.dto;

import org.helex.commons.model.QueryParams;

/**
 * Query parameters for the guideline list endpoint. Extends the platform
 * {@link QueryParams}, which carries {@code limit}, {@code offset} and
 * {@code sort}.
 */
public class GuidelineQueryParams extends QueryParams {

    /** Case-insensitive substring match on title or code. */
    private String textContains;
    /** Exact domain filter, e.g. {@code CARDIOLOGY}. */
    private String domainCode;
    /** Exact document-type filter, e.g. {@code SELTSI_JUHEND}. */
    private String documentType;
    /** When true, only guidelines past the four-year review deadline. */
    private Boolean overdueOnly;

    public String getTextContains() { return textContains; }
    public void setTextContains(String textContains) { this.textContains = textContains; }
    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public Boolean getOverdueOnly() { return overdueOnly; }
    public void setOverdueOnly(Boolean overdueOnly) { this.overdueOnly = overdueOnly; }
}
