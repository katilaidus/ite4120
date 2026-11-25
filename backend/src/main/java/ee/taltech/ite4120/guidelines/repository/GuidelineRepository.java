package ee.taltech.ite4120.guidelines.repository;

import ee.taltech.ite4120.guidelines.dto.GuidelineQueryParams;
import ee.taltech.ite4120.guidelines.model.Domain;
import ee.taltech.ite4120.guidelines.model.DomainReview;
import ee.taltech.ite4120.guidelines.model.Guideline;
import ee.taltech.ite4120.guidelines.model.Recommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.helex.commons.db.bean.PgBeanProcessor;
import org.helex.commons.db.repo.BaseRepository;
import org.helex.commons.db.sql.SqlBuilder;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Repository;

/**
 * Data access for the guideline register — the canonical Helex repository
 * shape: extend {@link BaseRepository}, map rows with {@link PgBeanProcessor},
 * build dynamic filters with {@link SqlBuilder}.
 */
@Repository
public class GuidelineRepository extends BaseRepository {

    private static final String SELECT = "select g.* from guidelines.guideline g ";

    /** Sort keys the list endpoint accepts (`sort=title`, `sort=-publishedOn`). */
    private static final Map<String, String> SORT = Map.of(
            "title", "g.title",
            "code", "g.code",
            "publishedOn", "g.published_on",
            "domainCode", "g.domain_code");

    private final PgBeanProcessor bp = new PgBeanProcessor(Guideline.class);
    private final PgBeanProcessor domainBp = new PgBeanProcessor(Domain.class);
    private final PgBeanProcessor recommendationBp = new PgBeanProcessor(Recommendation.class);
    private final PgBeanProcessor reviewBp = new PgBeanProcessor(DomainReview.class);

    public Guideline load(Long id) {
        return getBean(SELECT + "where g.id = ? and g.sys_status = 'A'", bp, id);
    }

    public Guideline loadByCode(String code) {
        return getBean(SELECT + "where g.code = ? and g.sys_status = 'A'", bp, code);
    }

    public QueryResult<Guideline> search(GuidelineQueryParams p) {
        return query(p, this::count, this::list);
    }

    private Integer count(GuidelineQueryParams p) {
        SqlBuilder sb = new SqlBuilder(
                "select count(1) from guidelines.guideline g where g.sys_status = 'A'");
        sb.append(filter(p));
        return queryForObject(sb.getSql(), Integer.class, sb.getParams());
    }

    private List<Guideline> list(GuidelineQueryParams p) {
        SqlBuilder sb = new SqlBuilder(SELECT + "where g.sys_status = 'A'");
        sb.append(filter(p));
        sb.append(order(p));
        sb.append(limit(p));
        return getBeans(sb.getSql(), bp, sb.getParams());
    }

    /**
     * The platform's {@code order} throws IllegalArgumentException for a sort key
     * it cannot map, which would surface as a 500 — but a mistyped {@code ?sort=}
     * is the caller's fault, so it becomes a 400 naming the allowed keys.
     */
    private SqlBuilder order(GuidelineQueryParams p) {
        try {
            return order(p, SORT);
        } catch (IllegalArgumentException e) {
            throw new ApiClientException("unknown sort key; sortable: "
                    + String.join(", ", SORT.keySet().stream().sorted().toList()));
        }
    }

    private SqlBuilder filter(GuidelineQueryParams p) {
        SqlBuilder sb = new SqlBuilder();
        if (p.getTextContains() != null && !p.getTextContains().isBlank()) {
            String like = "%" + p.getTextContains().toLowerCase() + "%";
            sb.and("(lower(g.title) like ? or lower(g.code) like ?)", like, like);
        }
        sb.appendIfNotNull("and g.domain_code = ?", p.getDomainCode());
        sb.appendIfNotNull("and g.document_type = ?", p.getDocumentType());
        if (Boolean.TRUE.equals(p.getOverdueOnly())) {
            // The cut-off is computed in Java from the single constant on the
            // model, so the filter and the derived flag can never disagree.
            sb.and("g.published_on < ?", reviewCutoff());
        }
        return sb;
    }

    /** Guidelines published before this date are past their review deadline. */
    private LocalDate reviewCutoff() {
        return LocalDate.now().minusYears(Guideline.REVIEW_INTERVAL_YEARS);
    }

    public Long insert(Guideline g) {
        String sql = """
                insert into guidelines.guideline (code, title, domain_code, document_type,
                                                  published_on, approver_isikukood, source_url)
                values (?, ?, ?, ?, ?, ?, ?)
                returning id""";
        return queryForObject(sql, Long.class,
                g.getCode(), g.getTitle(), g.getDomainCode(), g.getDocumentType(),
                g.getPublishedOn(), g.getApproverIsikukood(), g.getSourceUrl());
    }

    public boolean update(Guideline g) {
        String sql = """
                update guidelines.guideline
                   set title = ?, domain_code = ?, document_type = ?, published_on = ?,
                       approver_isikukood = ?, source_url = ?
                 where id = ? and sys_status = 'A'""";
        return jdbcTemplate.update(sql, g.getTitle(), g.getDomainCode(), g.getDocumentType(),
                g.getPublishedOn(), g.getApproverIsikukood(), g.getSourceUrl(), g.getId()) == 1;
    }

    /** Soft delete — the platform never hard-deletes registry rows. */
    public boolean retire(Long id) {
        return jdbcTemplate.update(
                "update guidelines.guideline set sys_status = 'C' where id = ? and sys_status = 'A'",
                id) == 1;
    }

    public List<Domain> domainList() {
        return getBeans("select d.* from guidelines.domain d order by d.name", domainBp);
    }

    public boolean domainExists(String code) {
        Integer n = queryForObject(
                "select count(1) from guidelines.domain where code = ?", Integer.class, code);
        return n != null && n > 0;
    }

    public List<Recommendation> recommendationsOf(Long guidelineId) {
        return getBeans("""
                select r.* from guidelines.recommendation r
                 where r.guideline_id = ? and r.sys_status = 'A'
                 order by r.id""", recommendationBp, guidelineId);
    }

    public Long insertRecommendation(Recommendation r) {
        String sql = """
                insert into guidelines.recommendation (guideline_id, statement, strength, evidence_level)
                values (?, ?, ?, ?)
                returning id""";
        return queryForObject(sql, Long.class,
                r.getGuidelineId(), r.getStatement(), r.getStrength(), r.getEvidenceLevel());
    }

    public Recommendation loadRecommendation(Long id) {
        return getBean("select r.* from guidelines.recommendation r "
                + "where r.id = ? and r.sys_status = 'A'", recommendationBp, id);
    }

    /**
     * The reporting query: one row per domain, joining all three tables.
     *
     * <p>Both joins are LEFT and both are filtered in the ON clause rather than
     * the WHERE clause — a WHERE on the right-hand table would silently turn
     * this back into an inner join and drop the empty domains, which are the
     * rows the coordinator most needs to see.
     *
     * <p>{@code count(distinct g.id)} rather than {@code count(*)}: the
     * recommendation join multiplies guideline rows, and counting the product
     * would report a guideline once per recommendation it carries.
     */
    public List<DomainReview> reviewReport() {
        String sql = """
                select d.code                                             as domain_code,
                       d.name                                             as domain_name,
                       count(distinct g.id)                               as guideline_count,
                       count(distinct g.id) filter (where g.published_on < ?) as overdue_count,
                       count(r.id)                                        as recommendation_count,
                       min(g.published_on)                                as oldest_published_on
                  from guidelines.domain d
                  left join guidelines.guideline g
                         on g.domain_code = d.code and g.sys_status = 'A'
                  left join guidelines.recommendation r
                         on r.guideline_id = g.id and r.sys_status = 'A'
                 group by d.code, d.name
                 order by d.name""";
        return getBeans(sql, reviewBp, reviewCutoff());
    }
}
