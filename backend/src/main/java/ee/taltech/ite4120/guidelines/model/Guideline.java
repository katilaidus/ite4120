package ee.taltech.ite4120.guidelines.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One clinical guideline. Maps 1:1 onto {@code guidelines.guideline} — see the
 * Liquibase changesets under {@code resources/guidelines/db/changelog/guidelines/}.
 *
 * <p>The {@code sys*} fields are filled by the shared {@code core.sys_columns()}
 * trigger, never by application code.
 */
public class Guideline {

    /**
     * The Ravijuhendi kasiraamat requires a guideline to be reviewed at least
     * every four years. The interval lives here, as one constant, because both
     * the derived flag below and the review report's cut-off date must mean the
     * same thing.
     */
    public static final int REVIEW_INTERVAL_YEARS = 4;

    /**
     * The review rule itself, as a pure function of two dates — which is what
     * makes it testable without a database or a clock. Exactly four years to
     * the day is NOT yet overdue; the guideline owns that boundary.
     */
    public static boolean isOverdue(LocalDate publishedOn, LocalDate today) {
        return publishedOn != null && publishedOn.isBefore(today.minusYears(REVIEW_INTERVAL_YEARS));
    }

    private Long id;
    /** Registry code, e.g. {@code PJ-I/4.3-2026}. Unique among active rows. */
    private String code;
    private String title;
    /** FK -> guidelines.domain(code). */
    private String domainCode;
    /** RAVIJUHEND, PATSIENDIJUHEND, SELTSI_JUHEND or TEGEVUSJUHEND. */
    private String documentType;
    private LocalDate publishedOn;
    /** Estonian personal code of the approver. A STRING, never a number. */
    private String approverIsikukood;
    private String sourceUrl;

    private String sysStatus;
    private Long sysVersion;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;
    private OffsetDateTime sysModifiedAt;
    private String sysModifiedBy;

    /**
     * Derived on every read, never stored and never accepted from the caller.
     * A stored flag would be wrong the day after it was written.
     */
    public boolean isOverdueForReview() {
        return isOverdue(publishedOn, LocalDate.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getSysStatus() { return sysStatus; }
    public void setSysStatus(String sysStatus) { this.sysStatus = sysStatus; }
    public Long getSysVersion() { return sysVersion; }
    public void setSysVersion(Long sysVersion) { this.sysVersion = sysVersion; }
    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }
    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }
    public OffsetDateTime getSysModifiedAt() { return sysModifiedAt; }
    public void setSysModifiedAt(OffsetDateTime sysModifiedAt) { this.sysModifiedAt = sysModifiedAt; }
    public String getSysModifiedBy() { return sysModifiedBy; }
    public void setSysModifiedBy(String sysModifiedBy) { this.sysModifiedBy = sysModifiedBy; }
}
