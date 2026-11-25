package ee.taltech.ite4120.guidelines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.taltech.ite4120.guidelines.dto.GuidelineQueryParams;
import ee.taltech.ite4120.guidelines.model.DomainReview;
import ee.taltech.ite4120.guidelines.model.Guideline;
import ee.taltech.ite4120.guidelines.service.GuidelineService;
import java.time.LocalDate;
import java.util.List;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * THE BUSINESS TESTS — the executable form of the plain-language tests in
 * docs/src/specifications/GUIDELINES.01 § Business tests. Each test quotes the
 * specification; if a rule here looks arbitrary, the specification is where it
 * is justified.
 *
 * <p>Runs against a real PostgreSQL via Testcontainers, so these tests also
 * prove the Liquibase changelog applies clean to an empty database.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class GuidelineBusinessRulesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    GuidelineService service;

    /**
     * Business test 1 — "A guideline is overdue for review four years after
     * publication."
     *
     * <p>The rule is a pure function of two dates, so it is tested as one: no
     * database, no clock, and the boundary is stated rather than hoped for.
     */
    @Test
    void aGuidelineIsOverdueFourYearsAfterPublication() {
        LocalDate today = LocalDate.of(2026, 11, 25);

        // Given a guideline published in 2020, when read today, then it is overdue
        assertThat(Guideline.isOverdue(LocalDate.of(2020, 1, 1), today))
                .as("published six years ago")
                .isTrue();

        // Given one published two years ago, then it is not
        assertThat(Guideline.isOverdue(LocalDate.of(2024, 6, 1), today))
                .as("published two years ago")
                .isFalse();

        // The boundary belongs to the guideline: exactly four years is NOT yet overdue
        assertThat(Guideline.isOverdue(today.minusYears(4), today))
                .as("exactly four years to the day")
                .isFalse();
        assertThat(Guideline.isOverdue(today.minusYears(4).minusDays(1), today))
                .as("one day past four years")
                .isTrue();
    }

    /**
     * And the same rule as the API reports it — derived on read, never stored,
     * so a row written years ago answers correctly today.
     */
    @Test
    void theOverdueFlagIsDerivedOnRead() {
        Guideline old = service.create(guideline("IT-OLD-001", LocalDate.now().minusYears(6)));
        Guideline fresh = service.create(guideline("IT-NEW-001", LocalDate.now().minusMonths(6)));

        assertThat(service.require(old.getId()).isOverdueForReview()).isTrue();
        assertThat(service.require(fresh.getId()).isOverdueForReview()).isFalse();

        // And the list endpoint can filter on it
        GuidelineQueryParams params = new GuidelineQueryParams();
        params.setOverdueOnly(true);
        List<Guideline> overdue = service.search(params).getData();
        assertThat(overdue).extracting(Guideline::getCode).contains("IT-OLD-001");
        assertThat(overdue).extracting(Guideline::getCode).doesNotContain("IT-NEW-001");
    }

    /**
     * Business test 2 — "A guideline code is unique among active guidelines."
     * Retirement is a soft delete, so the code becomes free again.
     */
    @Test
    void aCodeIsUniqueAmongActiveGuidelines() {
        // Given an active guideline with a code
        Guideline first = service.create(guideline("IT-DUP-001", LocalDate.now().minusYears(1)));
        assertThat(first.getId()).isNotNull();

        // When a second guideline claims the same code, then it conflicts
        assertThatThrownBy(() -> service.create(guideline("IT-DUP-001", LocalDate.now().minusYears(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        // And when the first is retired, the code may be used again
        service.retire(first.getId());
        Guideline reused = service.create(guideline("IT-DUP-001", LocalDate.now().minusYears(1)));
        assertThat(reused.getId()).isNotEqualTo(first.getId());
    }

    /** A guideline cannot be published in the future — validated in the service. */
    @Test
    void aGuidelineCannotBePublishedInTheFuture() {
        Guideline g = guideline("IT-FUT-001", LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.create(g))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("publishedOn must not be in the future");
    }

    /** An unknown domain is the caller's mistake, not a raw foreign-key error. */
    @Test
    void anUnknownDomainIsRefusedWithAClearMessage() {
        Guideline g = guideline("IT-DOM-001", LocalDate.now().minusYears(1));
        g.setDomainCode("KARDIOLOOGIA");

        assertThatThrownBy(() -> service.create(g))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("unknown domainCode");
    }

    /**
     * The reporting operation must keep empty domains visible. In the Estonian
     * register two of the four domains under study have no national guideline
     * at all, and a report that dropped them would hide the finding.
     */
    @Test
    void theReviewReportKeepsEmptyDomainsVisible() {
        List<DomainReview> report = service.reviewReport();

        assertThat(report)
                .as("every reference domain appears, with or without guidelines")
                .extracting(DomainReview::getDomainCode)
                .contains("CARDIOLOGY", "PROSTATE", "RADIOLOGY", "CERVIX");

        assertThat(report)
                .allSatisfy(row -> {
                    assertThat(row.getGuidelineCount()).isNotNull().isGreaterThanOrEqualTo(0);
                    assertThat(row.getOverdueCount()).isNotNull().isGreaterThanOrEqualTo(0);
                    assertThat(row.getRecommendationCount()).isNotNull().isGreaterThanOrEqualTo(0);
                });
    }

    private Guideline guideline(String code, LocalDate publishedOn) {
        Guideline g = new Guideline();
        g.setCode(code);
        g.setTitle("Test guideline " + code);
        g.setDomainCode("CARDIOLOGY");
        g.setDocumentType("RAVIJUHEND");
        g.setPublishedOn(publishedOn);
        return g;
    }
}
