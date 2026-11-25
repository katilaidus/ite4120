import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { AppButton, AppCard, AppTag, useAppNotification } from '@helex/ui';
import { guidelinesApi, type DomainReview } from './api';

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
      .catch((e) => notify.error('Raporti laadimine ebaõnnestus', e.message))
      .finally(() => setLoading(false));
  }, [notify]);

  const columns = [
    { key: 'domainName', title: 'Valdkond', dataIndex: 'domainName' },
    {
      key: 'guidelineCount',
      title: 'Dokumente',
      dataIndex: 'guidelineCount',
      render: (n: number) => (n === 0 ? <AppTag color="red">puudub</AppTag> : n),
    },
    {
      key: 'overdueCount',
      title: 'Ülevaatuse tähtaeg ületatud',
      dataIndex: 'overdueCount',
      render: (n: number) => (n > 0 ? <AppTag color="red">{n}</AppTag> : n),
    },
    { key: 'recommendationCount', title: 'Soovitusi', dataIndex: 'recommendationCount' },
    {
      key: 'oldestPublishedOn',
      title: 'Vanim dokument',
      dataIndex: 'oldestPublishedOn',
      render: (d?: string) => d ?? '—',
    },
  ];

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <AppButton icon={<ArrowLeftOutlined />} onClick={() => navigate('/guidelines')}>
        Tagasi nimekirja
      </AppButton>

      <AppCard title="Ülevaatuse raport valdkondade kaupa">
        <Table
          rowKey="domainCode"
          loading={loading}
          dataSource={rows}
          columns={columns}
          pagination={false}
        />
        <div style={{ marginTop: 12, opacity: 0.7, fontSize: 13 }}>
          Ülevaatuse tähtaeg arvutatakse avaldamiskuupäevast ravijuhendi käsiraamatu
          nelja-aastase nõude järgi. Valdkond, kus dokumente ei ole, jääb raportisse
          nulliga — tühjus on siin vastus, mitte puuduv rida.
        </div>
      </AppCard>
    </div>
  );
};
