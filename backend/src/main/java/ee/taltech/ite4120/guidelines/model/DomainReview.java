package ee.taltech.ite4120.guidelines.model;

import java.time.LocalDate;

/**
 * One row of the review report: the state of a single clinical domain.
 *
 * <p>This is the component's reporting shape, and the one query that joins all
 * three tables. A domain with no guidelines still reports zeros rather than
 * vanishing from the report — which is the whole point. In the Estonian
 * register two of the four domains under study have no national guideline at
 * all, and a report that quietly dropped them would hide exactly the finding
 * the coordinator needs to see.
 */
public class DomainReview {

    private String domainCode;
    private String domainName;
    private Integer guidelineCount;
    private Integer overdueCount;
    private Integer recommendationCount;
    private LocalDate oldestPublishedOn;

    public String getDomainCode() { return domainCode; }
    public void setDomainCode(String domainCode) { this.domainCode = domainCode; }
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public Integer getGuidelineCount() { return guidelineCount; }
    public void setGuidelineCount(Integer guidelineCount) { this.guidelineCount = guidelineCount; }
    public Integer getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    public Integer getRecommendationCount() { return recommendationCount; }
    public void setRecommendationCount(Integer recommendationCount) { this.recommendationCount = recommendationCount; }
    public LocalDate getOldestPublishedOn() { return oldestPublishedOn; }
    public void setOldestPublishedOn(LocalDate oldestPublishedOn) { this.oldestPublishedOn = oldestPublishedOn; }
}
