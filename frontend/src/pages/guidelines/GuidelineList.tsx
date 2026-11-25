import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Select, Switch } from 'antd';
import { BarChartOutlined } from '@ant-design/icons';
import {
  AppButtonPrimary,
  AppTag,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import { guidelinesApi, type ApproverInfo, type Guideline, type Recommendation } from './api';

/**
 * The document kinds the register admits. The Estonian term is kept in
 * brackets because it is what the register itself prints on the document —
 * translating it away would make the register harder to check against the
 * source, not easier.
 */
const DOCUMENT_TYPE_LABEL: Record<string, string> = {
  RAVIJUHEND: 'Clinical guideline',
  PATSIENDIJUHEND: 'Patient guideline',
  SELTSI_JUHEND: 'Society guideline',
  TEGEVUSJUHEND: 'Operating procedure',
};

/**
 * English names for the clinical domains. The database keeps the Estonian
 * name, because that is what the Guidelines Council calls them and what a
 * reader would have to match against the source; the code is the stable
 * identifier, so the interface translates from that.
 */
const DOMAIN_LABEL: Record<string, string> = {
  CARDIOLOGY: 'Cardiology',
  PROSTATE: 'Prostate disease',
  RADIOLOGY: 'Radiology',
  CERVIX: 'Cervical cancer',
};

/** GRADE strengths, as the Estonian handbook defines them. */
const STRENGTH_LABEL: Record<string, string> = {
  STRONG_FOR: 'Strong for',
  WEAK_FOR: 'Weak for',
  WEAK_AGAINST: 'Weak against',
  STRONG_AGAINST: 'Strong against',
  PRACTICE: 'Good practice',
};

/**
 * The guideline list — built on @helex/ui's ResourceList, the same component
 * the production applications use for every list page.
 *
 * <p>The column that matters is "Review": it is derived by the backend from
 * the publication date against the handbook's four-year rule, so it is never
 * editable here and cannot go stale.
 */
export const GuidelineList = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [guidelines, setGuidelines] = useState<Guideline[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Guideline | null>(null);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [approver, setApprover] = useState<ApproverInfo | null>(null);
  const [registryDown, setRegistryDown] = useState(false);

  const [domains, setDomains] = useState<{ code: string; name: string }[]>([]);
  const [domainFilter, setDomainFilter] = useState<string | undefined>();
  const [overdueFilter, setOverdueFilter] = useState(false);
  const [appliedDomain, setAppliedDomain] = useState<string | undefined>();
  const [appliedOverdue, setAppliedOverdue] = useState(false);

  useEffect(() => {
    guidelinesApi.domains().then(setDomains).catch(() => setDomains([]));
  }, []);

  const load = useCallback(
    (text: string, domainCode?: string, overdueOnly?: boolean) => {
      setLoading(true);
      guidelinesApi
        .list(text || undefined, domainCode, overdueOnly)
        .then((result) => setGuidelines(result.data))
        .catch((e) => notify.error('Could not load guidelines', e.message))
        .finally(() => setLoading(false));
    },
    [notify],
  );

  useEffect(() => {
    const t = setTimeout(() => load(search, appliedDomain, appliedOverdue), 250);
    return () => clearTimeout(t);
  }, [search, appliedDomain, appliedOverdue, load]);

  // The selected guideline's recommendations, and its approver via the registry.
  useEffect(() => {
    setRecommendations([]);
    setApprover(null);
    setRegistryDown(false);
    if (!selected?.id) return;

    guidelinesApi.recommendations(selected.id).then(setRecommendations).catch(() => setRecommendations([]));

    if (selected.approverIsikukood) {
      guidelinesApi
        .approver(selected.id)
        .then(setApprover)
        // FAIL OPEN: a registry outage is noted quietly beside the approver's
        // code. The guideline itself stays fully readable — the page never
        // depends on an external system to show what the register owns.
        .catch(() => setRegistryDown(true));
    }
  }, [selected]);

  const columns: ResourceListColumn<Guideline>[] = [
    { key: 'code', title: 'Code', dataIndex: 'code', locked: true },
    { key: 'title', title: 'Title', dataIndex: 'title' },
    {
      key: 'domainCode',
      title: 'Domain',
      dataIndex: 'domainCode',
      render: (code: string) => (
        <AppTag>{DOMAIN_LABEL[code] ?? domains.find((d) => d.code === code)?.name ?? code}</AppTag>
      ),
    },
    {
      key: 'documentType',
      title: 'Type',
      dataIndex: 'documentType',
      render: (type: string) => <AppTag>{DOCUMENT_TYPE_LABEL[type] ?? type}</AppTag>,
    },
    { key: 'publishedOn', title: 'Published', dataIndex: 'publishedOn' },
    {
      key: 'overdueForReview',
      title: 'Review',
      dataIndex: 'overdueForReview',
      render: (overdue: boolean) =>
        overdue ? <AppTag color="red">Overdue</AppTag> : <AppTag>Current</AppTag>,
    },
  ];

  return (
    <ResourceList<Guideline>
      title="Clinical guidelines"
      columns={columns}
      dataSource={guidelines}
      rowKey="id"
      loading={loading}
      search={{ value: search, onChange: setSearch, placeholder: 'Title or code…' }}
      filter={{
        content: (
          <div style={{ display: 'grid', gap: 12 }}>
            <Select
              style={{ minWidth: 220 }}
              placeholder="Domain"
              allowClear
              value={domainFilter}
              onChange={setDomainFilter}
              options={domains.map((d) => ({
                value: d.code,
                label: DOMAIN_LABEL[d.code] ?? d.name,
              }))}
            />
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Switch checked={overdueFilter} onChange={setOverdueFilter} />
              Only those overdue for review
            </label>
          </div>
        ),
        onApply: () => {
          setAppliedDomain(domainFilter);
          setAppliedOverdue(overdueFilter);
        },
        onClear: () => {
          setDomainFilter(undefined);
          setOverdueFilter(false);
          setAppliedDomain(undefined);
          setAppliedOverdue(false);
        },
      }}
      actions={
        <AppButtonPrimary
          icon={<BarChartOutlined />}
          onClick={() => navigate('/guidelines/review')}
        >
          Review report
        </AppButtonPrimary>
      }
      detailView={{
        selectedRecord: selected,
        onSelectedRecordChange: setSelected,
        title: 'Guideline',
        render: (g) => (
          <div style={{ display: 'grid', gap: 10 }}>
            <div>
              <b>{g.title}</b>
            </div>
            <div>{g.code} · {DOCUMENT_TYPE_LABEL[g.documentType] ?? g.documentType}</div>
            <div>Published: {g.publishedOn}</div>
            <div>
              Review:{' '}
              {g.overdueForReview ? (
                <AppTag color="red">overdue</AppTag>
              ) : (
                <AppTag>current</AppTag>
              )}
              <div style={{ opacity: 0.65, fontSize: 12 }}>
derived from the publication date, against the handbook's four-year rule
              </div>
            </div>

            {g.approverIsikukood && (
              <div>
                Approved by: {g.approverIsikukood}
                {approver && (
                  <>
                    {' — '}
                    <b>
                      {approver.firstName} {approver.lastName}
                    </b>
                    <div style={{ opacity: 0.65, fontSize: 12 }}>
fetched live from the population registry through the adapter (mock / X-Road)
                    </div>
                  </>
                )}
                {registryDown && (
                  <div style={{ opacity: 0.65, fontSize: 12 }}>
registry unreachable — the name is omitted; the guideline itself stays usable
                  </div>
                )}
              </div>
            )}

            {recommendations.length > 0 && (
              <div>
                <div style={{ marginBottom: 4 }}>
                  <b>Recommendations ({recommendations.length})</b>
                </div>
                <div style={{ display: 'grid', gap: 6 }}>
                  {recommendations.map((r) => (
                    <div key={r.id} style={{ fontSize: 13 }}>
                      <AppTag>{STRENGTH_LABEL[r.strength] ?? r.strength}</AppTag>
                      {r.evidenceLevel && <AppTag>Evidence {r.evidenceLevel}</AppTag>}
                      <div>{r.statement}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ),
      }}
    />
  );
};
