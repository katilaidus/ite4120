package ee.taltech.ite4120.guidelines.api;

import ee.taltech.ite4120.guidelines.model.Recommendation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The body for adding a recommendation to a guideline. */
public class RecommendationRequest {

    @NotBlank
    @Size(max = 2000)
    private String statement;

    /** GRADE strength; the allowed set is checked in the service. */
    @NotBlank
    private String strength;

    /** A, B, C or D — absent for a good-practice statement. */
    private String evidenceLevel;

    public Recommendation toModel() {
        Recommendation r = new Recommendation();
        r.setStatement(statement);
        r.setStrength(strength);
        r.setEvidenceLevel(evidenceLevel);
        return r;
    }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public void setEvidenceLevel(String evidenceLevel) { this.evidenceLevel = evidenceLevel; }
}
