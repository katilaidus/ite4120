package ee.taltech.ite4120.guidelines.service;

import ee.taltech.ite4120.guidelines.dto.GuidelineQueryParams;
import ee.taltech.ite4120.guidelines.model.Domain;
import ee.taltech.ite4120.guidelines.model.DomainReview;
import ee.taltech.ite4120.guidelines.model.Guideline;
import ee.taltech.ite4120.guidelines.model.Recommendation;
import ee.taltech.ite4120.guidelines.repository.GuidelineRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for the clinical guideline register. The rules here are
 * exactly the ones written as business tests in
 * docs/src/specifications/GUIDELINES.01 — spec first, tests second,
 * implementation third.
 */
@Service
public class GuidelineService {

    /** GRADE strengths, as the Estonian handbook uses them. */
    private static final Set<String> STRENGTHS =
            Set.of("STRONG_FOR", "WEAK_FOR", "WEAK_AGAINST", "STRONG_AGAINST", "PRACTICE");

    private static final Set<String> EVIDENCE_LEVELS = Set.of("A", "B", "C", "D");

    /**
     * The kinds of document that tell a clinician what to do. A national
     * clinical guideline is only one of them — in two of the four domains
     * under study the specialist society's document is the only one there is.
     */
    private static final Set<String> DOCUMENT_TYPES =
            Set.of("RAVIJUHEND", "PATSIENDIJUHEND", "SELTSI_JUHEND", "TEGEVUSJUHEND");

    private final GuidelineRepository repository;

    public GuidelineService(GuidelineRepository repository) {
        this.repository = repository;
    }

    public QueryResult<Guideline> search(GuidelineQueryParams params) {
        return repository.search(params);
    }

    public Guideline require(Long id) {
        Guideline g = repository.load(id);
        if (g == null) {
            throw new NotFoundException("guideline " + id + " not found");
        }
        return g;
    }

    @Transactional
    public Guideline create(Guideline guideline) {
        validate(guideline);
        // Business rule: the code is unique among ACTIVE guidelines.
        if (repository.loadByCode(guideline.getCode()) != null) {
            throw new ConflictException("guideline",
                    "an active guideline with code " + guideline.getCode() + " already exists");
        }
        Long id = repository.insert(guideline);
        return require(id);
    }

    @Transactional
    public Guideline update(Long id, Guideline guideline) {
        Guideline existing = require(id);
        // The code is immutable after creation — identity does not change.
        guideline.setCode(existing.getCode());
        validate(guideline);
        guideline.setId(id);
        if (!repository.update(guideline)) {
            throw new NotFoundException("guideline " + id + " not found");
        }
        return require(id);
    }

    @Transactional
    public void retire(Long id) {
        if (!repository.retire(id)) {
            throw new NotFoundException("guideline " + id + " not found");
        }
    }

    public List<Domain> domains() {
        return repository.domainList();
    }

    public List<Recommendation> recommendations(Long guidelineId) {
        require(guidelineId);   // 404 for an unknown guideline, rather than an empty list
        return repository.recommendationsOf(guidelineId);
    }

    @Transactional
    public Recommendation addRecommendation(Long guidelineId, Recommendation r) {
        require(guidelineId);
        if (!STRENGTHS.contains(r.getStrength())) {
            throw new ApiClientException("unknown strength: " + r.getStrength()
                    + "; allowed: " + String.join(", ", STRENGTHS.stream().sorted().toList()));
        }
        if (r.getEvidenceLevel() != null && !EVIDENCE_LEVELS.contains(r.getEvidenceLevel())) {
            throw new ApiClientException("evidenceLevel must be A, B, C or D");
        }
        r.setGuidelineId(guidelineId);
        return repository.loadRecommendation(repository.insertRecommendation(r));
    }

    /** The reporting operation — see GUIDELINES.01 and DomainReview. */
    public List<DomainReview> reviewReport() {
        return repository.reviewReport();
    }

    private void validate(Guideline g) {
        // Business rule: a guideline cannot be published in the future.
        if (g.getPublishedOn() != null && g.getPublishedOn().isAfter(LocalDate.now())) {
            throw new ApiClientException("publishedOn must not be in the future");
        }
        // Referential rule surfaced as a clear 400 rather than a raw FK violation.
        if (!repository.domainExists(g.getDomainCode())) {
            throw new ApiClientException("unknown domainCode: " + g.getDomainCode());
        }
        if (!DOCUMENT_TYPES.contains(g.getDocumentType())) {
            throw new ApiClientException("unknown documentType: " + g.getDocumentType()
                    + "; allowed: " + String.join(", ", DOCUMENT_TYPES.stream().sorted().toList()));
        }
    }
}
