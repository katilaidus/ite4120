// The guideline register API client — thin fetch wrappers over the backend
// this component owns. The Vite dev proxy forwards /api to the backend, so
// paths are same-origin. The Authorization header carries the mock session.

export interface Guideline {
  id: number;
  code: string;
  title: string;
  domainCode: string;
  documentType: string;
  publishedOn: string;
  approverIsikukood?: string;
  sourceUrl?: string;
  /** Derived by the backend from publishedOn — never stored, never sent. */
  overdueForReview?: boolean;
  /** Platform columns, filled by the database trigger; read-only. */
  sysStatus?: string;
  sysVersion?: number;
  sysCreatedAt?: string;
  sysCreatedBy?: string;
  sysModifiedAt?: string;
  sysModifiedBy?: string;
}

export interface Recommendation {
  id: number;
  guidelineId: number;
  statement: string;
  strength: string;
  evidenceLevel?: string;
}

export interface DomainReview {
  domainCode: string;
  domainName: string;
  guidelineCount: number;
  overdueCount: number;
  recommendationCount: number;
  oldestPublishedOn?: string;
}

export interface ApproverInfo {
  isikukood: string;
  firstName: string;
  lastName: string;
}

export interface QueryResult<T> {
  data: T[];
  meta?: { total?: number };
}

function authHeaders(): Record<string, string> {
  const user = localStorage.getItem('helex_mock_user') || 'superadmin';
  return { Authorization: `Bearer ${user}` };
}

async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // The backend answers application/problem+json — surface its human text.
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.detail || `HTTP ${response.status}`);
  }
  return response.json();
}

export const guidelinesApi = {
  list: (
    textContains?: string,
    domainCode?: string,
    overdueOnly?: boolean,
  ): Promise<QueryResult<Guideline>> =>
    fetch(
      '/api/guidelines?limit=100&sort=publishedOn' +
        (textContains ? `&textContains=${encodeURIComponent(textContains)}` : '') +
        (domainCode ? `&domainCode=${encodeURIComponent(domainCode)}` : '') +
        (overdueOnly ? '&overdueOnly=true' : ''),
      { headers: authHeaders() },
    ).then((r) => handle<QueryResult<Guideline>>(r)),

  get: (id: number): Promise<Guideline> =>
    fetch(`/api/guidelines/${id}`, { headers: authHeaders() }).then((r) => handle<Guideline>(r)),

  create: (body: Omit<Guideline, 'id'>): Promise<Guideline> =>
    fetch('/api/guidelines', {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then((r) => handle<Guideline>(r)),

  /** The reporting operation — one row per clinical domain. */
  reviewReport: (): Promise<DomainReview[]> =>
    fetch('/api/guidelines/review-report', { headers: authHeaders() }).then((r) =>
      handle<DomainReview[]>(r),
    ),

  domains: (): Promise<{ code: string; name: string }[]> =>
    fetch('/api/guidelines/domains', { headers: authHeaders() }).then((r) =>
      handle<{ code: string; name: string }[]>(r),
    ),

  recommendations: (id: number): Promise<Recommendation[]> =>
    fetch(`/api/guidelines/${id}/recommendations`, { headers: authHeaders() }).then((r) =>
      handle<Recommendation[]>(r),
    ),

  /** Looked up live through the registry adapter; 502 when the registry is down. */
  approver: (id: number): Promise<ApproverInfo> =>
    fetch(`/api/guidelines/${id}/approver`, { headers: authHeaders() }).then((r) =>
      handle<ApproverInfo>(r),
    ),
};
