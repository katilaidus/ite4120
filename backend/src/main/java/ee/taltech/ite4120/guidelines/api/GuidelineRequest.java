package ee.taltech.ite4120.guidelines.api;

import ee.taltech.ite4120.guidelines.model.Guideline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The create/update request body. Bean Validation handles the SHAPE of the
 * message here at the edge; the state-dependent rules (unique code, publication
 * date vs today, known domain) live in GuidelineService — validation in more
 * than one place, on purpose.
 */
public class GuidelineRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 500)
    private String title;

    @NotBlank
    private String domainCode;

    @NotBlank
    private String documentType;

    @NotNull(message = "publishedOn is required")
    private LocalDate publishedOn;

    /** 11 digits — and a string, never a number. */
    @Pattern(regexp = "\\d{11}", message = "approverIsikukood must be exactly 11 digits")
    private String approverIsikukood;

    @Size(max = 1000)
    private String sourceUrl;

    public Guideline toModel() {
        Guideline g = new Guideline();
        g.setCode(code);
        g.setTitle(title);
        g.setDomainCode(domainCode);
        g.setDocumentType(documentType);
        g.setPublishedOn(publishedOn);
        g.setApproverIsikukood(approverIsikukood);
        g.setSourceUrl(sourceUrl);
        return g;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public LocalDate getPublishedOn() { return publishedOn; }
    public void setPublishedOn(LocalDate publishedOn) { this.publishedOn = publishedOn; }
    public String getApproverIsikukood() { return approverIsikukood; }
    public void setApproverIsikukood(String approverIsikukood) { this.approverIsikukood = approverIsikukood; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
