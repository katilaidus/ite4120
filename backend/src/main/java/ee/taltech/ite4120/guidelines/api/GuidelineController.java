package ee.taltech.ite4120.guidelines.api;

import ee.taltech.ite4120.guidelines.approverregistry.ApproverInfo;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryAdapter;
import ee.taltech.ite4120.guidelines.dto.GuidelineQueryParams;
import ee.taltech.ite4120.guidelines.model.Domain;
import ee.taltech.ite4120.guidelines.model.DomainReview;
import ee.taltech.ite4120.guidelines.model.Guideline;
import ee.taltech.ite4120.guidelines.model.Recommendation;
import ee.taltech.ite4120.guidelines.service.GuidelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.helex.commons.exception.NotFoundException;
import org.helex.commons.model.QueryResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The clinical guideline register REST API. Nouns in the path, verbs in the
 * method; 201 + Location on create; the platform QueryResult on lists.
 * HTTP only — every business rule lives in GuidelineService.
 */
@RestController
@RequestMapping("/api/guidelines")
@Tag(name = "Guidelines", description = "Register of Estonian clinical guidelines and their recommendations")
public class GuidelineController {

    private final GuidelineService service;
    private final ApproverRegistryAdapter approverRegistry;

    public GuidelineController(GuidelineService service, ApproverRegistryAdapter approverRegistry) {
        this.service = service;
        this.approverRegistry = approverRegistry;
    }

    @Operation(summary = "Search guidelines — paginated, filterable, sortable",
            description = "Filter by text, by domain, or by overdueOnly to see only the "
                    + "guidelines past the handbook's four-year review deadline.")
    @GetMapping
    public QueryResult<Guideline> search(@ModelAttribute GuidelineQueryParams params) {
        return service.search(params);
    }

    @Operation(summary = "Review report — one row per clinical domain",
            description = "The reporting operation. Joins domains, guidelines and "
                    + "recommendations; a domain with no guidelines reports zeros rather "
                    + "than disappearing, because an empty domain is the finding.")
    @GetMapping("/review-report")
    public List<DomainReview> reviewReport() {
        return service.reviewReport();
    }

    @Operation(summary = "Clinical domain reference list")
    @GetMapping("/domains")
    public List<Domain> domains() {
        return service.domains();
    }

    @Operation(summary = "One guideline by id")
    @ApiResponse(responseCode = "200", description = "The guideline")
    @ApiResponse(responseCode = "404", description = "No active guideline with that id")
    @GetMapping("/{id}")
    public Guideline byId(@PathVariable Long id) {
        return service.require(id);
    }

    @Operation(summary = "Register a new guideline")
    @ApiResponse(responseCode = "201", description = "Created — Location header points at the new guideline")
    @ApiResponse(responseCode = "400", description = "Validation failed (future publication date, unknown domain…)")
    @ApiResponse(responseCode = "409", description = "Code already in use by an active guideline")
    @PostMapping
    public ResponseEntity<Guideline> create(@Valid @RequestBody GuidelineRequest body) {
        Guideline saved = service.create(body.toModel());
        return ResponseEntity.created(URI.create("/api/guidelines/" + saved.getId())).body(saved);
    }

    @Operation(summary = "Update a guideline (the code is immutable)")
    @PutMapping("/{id}")
    public Guideline update(@PathVariable Long id, @Valid @RequestBody GuidelineRequest body) {
        return service.update(id, body.toModel());
    }

    @Operation(summary = "Retire a guideline (soft delete)")
    @ApiResponse(responseCode = "204", description = "Retired")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retire(@PathVariable Long id) {
        service.retire(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "The guideline's approver, looked up from the (mock or X-Road) population registry",
            description = "Demonstrates the adapter pattern: the controller knows only the "
                    + "ApproverRegistryAdapter interface; whether the answer comes from the "
                    + "built-in imitated registry or a real X-Road security server is "
                    + "configuration. FAIL OPEN: a registry outage answers 502 here and "
                    + "nowhere else — the guideline, the search and the review report stay "
                    + "usable, because the approver's name is auxiliary.")
    @ApiResponse(responseCode = "200", description = "The approver")
    @ApiResponse(responseCode = "404", description = "No such guideline, no approver recorded, or the registry does not know them")
    @ApiResponse(responseCode = "502", description = "The registry could not be reached")
    @GetMapping("/{id}/approver")
    public ApproverInfo approver(@PathVariable Long id) {
        Guideline guideline = service.require(id);
        if (guideline.getApproverIsikukood() == null) {
            throw new NotFoundException("guideline " + id + " has no approver recorded");
        }
        ApproverInfo approver = approverRegistry.lookup(guideline.getApproverIsikukood());
        if (approver == null) {
            throw new NotFoundException(
                    "the registry does not know " + guideline.getApproverIsikukood());
        }
        return approver;
    }

    @Operation(summary = "Recommendations carried by a guideline")
    @ApiResponse(responseCode = "404", description = "No active guideline with that id")
    @GetMapping("/{id}/recommendations")
    public List<Recommendation> recommendations(@PathVariable Long id) {
        return service.recommendations(id);
    }

    @Operation(summary = "Add a recommendation to a guideline")
    @ApiResponse(responseCode = "201", description = "Created — Location header points at the guideline's recommendations")
    @ApiResponse(responseCode = "400", description = "Unknown GRADE strength or evidence level")
    @PostMapping("/{id}/recommendations")
    public ResponseEntity<Recommendation> addRecommendation(
            @PathVariable Long id, @Valid @RequestBody RecommendationRequest body) {
        Recommendation saved = service.addRecommendation(id, body.toModel());
        return ResponseEntity
                .created(URI.create("/api/guidelines/" + id + "/recommendations"))
                .body(saved);
    }
}
