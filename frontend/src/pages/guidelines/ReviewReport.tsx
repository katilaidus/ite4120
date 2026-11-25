import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { AppButton, AppCard, AppTag, useAppNotification } from '@helex/ui';
import { guidelinesApi, type DomainReview } from './api';

/** English names for the domains; the database keeps the Estonian ones. */
const DOMAIN_LABEL: Record<string, string> = {
  CARDIOLOGY: 'Cardiology',
  PROSTATE: 'Prostate disease',
  RADIOLOGY: 'Radiology',
  CERVIX: 'Cervical cancer',
};

/**
 * The review report — the component's reporting view, backed by the one query
 * that joins all three tables.
 *
 * <p>Domains with no guidelines stay on the screen showing zero. That is
 * deliberate: in the Estonian register two of the four domains under study
 * have no national guideline at all, and a report that quietly dropped them
 * would hide exactly the fact a coordinator needs to act on.
 */
export const ReviewReport = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [rows, setRows] = useState<DomainReview[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    guidelinesApi
      .reviewReport()
      .then(setRows)
      .catch((e) => notify.error('Could not load the report', e.message))
      .finally(() => setLoading(false));
  }, [notify]);

  const columns = [
    {
      key: 'domainName',
      title: 'Domain',
      dataIndex: 'domainName',
      render: (name: string, row: DomainReview) => DOMAIN_LABEL[row.domainCode] ?? name,
    },
    {
      key: 'guidelineCount',
      title: 'Documents',
      dataIndex: 'guidelineCount',
      render: (n: number) => (n === 0 ? <AppTag color="red">none</AppTag> : n),
    },
    {
      key: 'overdueCount',
      title: 'Overdue for review',
      dataIndex: 'overdueCount',
      render: (n: number) => (n > 0 ? <AppTag color="red">{n}</AppTag> : n),
    },
    { key: 'recommendationCount', title: 'Recommendations', dataIndex: 'recommendationCount' },
    {
      key: 'oldestPublishedOn',
      title: 'Oldest document',
      dataIndex: 'oldestPublishedOn',
      render: (d?: string) => d ?? '—',
    },
  ];

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <AppButton icon={<ArrowLeftOutlined />} onClick={() => navigate('/guidelines')}>
        Back to the list
      </AppButton>

      <AppCard title="Review report by clinical domain">
        <Table
          rowKey="domainCode"
          loading={loading}
          dataSource={rows}
          columns={columns}
          pagination={false}
        />
        <div style={{ marginTop: 12, opacity: 0.7, fontSize: 13 }}>
          The review deadline is derived from the publication date, against the
          four-year rule in the Estonian guideline handbook. A domain with no
          documents stays in this report showing zero — the emptiness is the
          answer, not a missing row.
        </div>
      </AppCard>
    </div>
  );
};
