package ee.taltech.ite4120.guidelines.model;

import java.time.OffsetDateTime;

/**
 * One recommendation carried by a guideline — the child side of the
 * one-to-many. Maps onto {@code guidelines.recommendation}.
 *
 * <p>{@code strength} follows GRADE, the grading system the Estonian handbook
 * adopts: direction and confidence are one decision, so they are one value.
 */
public class Recommendation {

    private Long id;
    /** FK -> guidelines.guideline(id). */
    private Long guidelineId;
    private String statement;
    /** STRONG_FOR, WEAK_FOR, WEAK_AGAINST, STRONG_AGAINST or PRACTICE. */
    private String strength;
    /** A, B, C or D — absent for a good-practice statement. */
    private String evidenceLevel;

    private String sysStatus;
    private Long sysVersion;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;
    private OffsetDateTime sysModifiedAt;
    private String sysModifiedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGuidelineId() { return guidelineId; }
    public void setGuidelineId(Long guidelineId) { this.guidelineId = guidelineId; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
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
