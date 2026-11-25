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

/** Estonian labels for the document kinds the register admits. */
const DOCUMENT_TYPE_LABEL: Record<string, string> = {
  RAVIJUHEND: 'Ravijuhend',
  PATSIENDIJUHEND: 'Patsiendijuhend',
  SELTSI_JUHEND: 'Seltsi juhend',
  TEGEVUSJUHEND: 'Tegevusjuhend',
};

/** GRADE strengths, as the handbook words them. */
const STRENGTH_LABEL: Record<string, string> = {
  STRONG_FOR: 'Tugev poolt',
  WEAK_FOR: 'Nõrk poolt',
  WEAK_AGAINST: 'Nõrk vastu',
  STRONG_AGAINST: 'Tugev vastu',
  PRACTICE: 'Hea tava',
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
        .catch((e) => notify.error('Juhendite laadimine ebaõnnestus', e.message))
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
    { key: 'code', title: 'Kood', dataIndex: 'code', locked: true },
    { key: 'title', title: 'Pealkiri', dataIndex: 'title' },
    {
      key: 'domainCode',
      title: 'Valdkond',
      dataIndex: 'domainCode',
      render: (code: string) => (
        <AppTag>{domains.find((d) => d.code === code)?.name ?? code}</AppTag>
      ),
    },
    {
      key: 'documentType',
      title: 'Liik',
      dataIndex: 'documentType',
      render: (type: string) => <AppTag>{DOCUMENT_TYPE_LABEL[type] ?? type}</AppTag>,
    },
    { key: 'publishedOn', title: 'Avaldatud', dataIndex: 'publishedOn' },
    {
      key: 'overdueForReview',
      title: 'Ülevaatus',
      dataIndex: 'overdueForReview',
      render: (overdue: boolean) =>
        overdue ? <AppTag color="red">Tähtaeg ületatud</AppTag> : <AppTag>Ajakohane</AppTag>,
    },
  ];

  return (
    <ResourceList<Guideline>
      title="Ravijuhendid"
      columns={columns}
      dataSource={guidelines}
      rowKey="id"
      loading={loading}
      search={{ value: search, onChange: setSearch, placeholder: 'Pealkiri või kood…' }}
      filter={{
        content: (
          <div style={{ display: 'grid', gap: 12 }}>
            <Select
              style={{ minWidth: 220 }}
              placeholder="Valdkond"
              allowClear
              value={domainFilter}
              onChange={setDomainFilter}
              options={domains.map((d) => ({ value: d.code, label: d.name }))}
            />
            <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Switch checked={overdueFilter} onChange={setOverdueFilter} />
              Ainult ülevaatuse tähtaja ületanud
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
          Ülevaatuse raport
        </AppButtonPrimary>
      }
      detailView={{
        selectedRecord: selected,
        onSelectedRecordChange: setSelected,
        title: 'Ravijuhend',
        render: (g) => (
          <div style={{ display: 'grid', gap: 10 }}>
            <div>
              <b>{g.title}</b>
            </div>
            <div>{g.code} · {DOCUMENT_TYPE_LABEL[g.documentType] ?? g.documentType}</div>
            <div>Avaldatud: {g.publishedOn}</div>
            <div>
              Ülevaatus:{' '}
              {g.overdueForReview ? (
                <AppTag color="red">tähtaeg ületatud</AppTag>
              ) : (
                <AppTag>ajakohane</AppTag>
              )}
              <div style={{ opacity: 0.65, fontSize: 12 }}>
                arvutatud avaldamiskuupäevast, käsiraamatu nelja-aastase nõude järgi
              </div>
            </div>

            {g.approverIsikukood && (
              <div>
                Kinnitaja: {g.approverIsikukood}
                {approver && (
                  <>
                    {' — '}
                    <b>
                      {approver.firstName} {approver.lastName}
                    </b>
                    <div style={{ opacity: 0.65, fontSize: 12 }}>
                      päritud rahvastikuregistrist adapteri kaudu (mock / X-Road)
                    </div>
                  </>
                )}
                {registryDown && (
                  <div style={{ opacity: 0.65, fontSize: 12 }}>
                    register ei vasta — nimi jääb kuvamata, juhend ise on kasutatav
                  </div>
                )}
              </div>
            )}

            {recommendations.length > 0 && (
              <div>
                <div style={{ marginBottom: 4 }}>
                  <b>Soovitused ({recommendations.length})</b>
                </div>
                <div style={{ display: 'grid', gap: 6 }}>
                  {recommendations.map((r) => (
                    <div key={r.id} style={{ fontSize: 13 }}>
                      <AppTag>{STRENGTH_LABEL[r.strength] ?? r.strength}</AppTag>
                      {r.evidenceLevel && <AppTag>Tõendatus {r.evidenceLevel}</AppTag>}
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
