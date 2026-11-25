#!/usr/bin/env bash
# Walks the GUIDELINES.01 API requirements end to end. The backend must be running.
# Usage: bash scripts/verify-guidelines-api.sh [base-url]
B="${1:-http://localhost:18440}"
A="Authorization: Bearer superadmin"
J="Content-Type: application/json"

line() { printf '\n--- %s ---\n' "$1"; }

# A unique suffix, so the script can be run repeatedly: a code is unique among
# active guidelines, so a fixed one would answer 409 the second time.
R="$(date +%H%M%S)"

line "1. LIST (200)"
curl -s -o /dev/null -w 'GET  /api/guidelines                 -> %{http_code}\n' -H "$A" "$B/api/guidelines"

line "2. REPORTING OPERATION (200) - joins all three tables"
curl -s -o /dev/null -w 'GET  /api/guidelines/review-report   -> %{http_code}\n' -H "$A" "$B/api/guidelines/review-report"
curl -s -H "$A" "$B/api/guidelines/review-report" \
  | python3 -c "import sys,json
for r in json.load(sys.stdin):
    print('   %-18s documents=%-3s overdue=%-3s recommendations=%-3s oldest=%s' % (
        r['domainName'], r['guidelineCount'], r['overdueCount'],
        r['recommendationCount'], r.get('oldestPublishedOn') or '-'))" 2>/dev/null

line "3. CREATE (201 + Location)"
curl -s -o /dev/null -w 'POST /api/guidelines                 -> %{http_code}   Location: %header{location}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-A\",\"title\":\"Verification guideline\",\"domainCode\":\"RADIOLOGY\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2019-01-01\",
       \"approverIsikukood\":\"38102130265\"}"

line "4. MISSING ID (404 + problem+json)"
curl -s -o /dev/null -w 'GET  /api/guidelines/999999          -> %{http_code}   %{content_type}\n' -H "$A" "$B/api/guidelines/999999"

line "5. VALIDATION (400) - published in the future"
curl -s -w '\n     -> %{http_code}   %{content_type}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-B\",\"title\":\"From the future\",\"domainCode\":\"CARDIOLOGY\",
       \"documentType\":\"RAVIJUHEND\",\"publishedOn\":\"2099-01-01\"}"

line "6. VALIDATION (400) - unknown domain"
curl -s -w '\n     -> %{http_code}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-C\",\"title\":\"Wrong domain\",\"domainCode\":\"KARDIOLOOGIA\",
       \"documentType\":\"RAVIJUHEND\",\"publishedOn\":\"2020-01-01\"}"

line "7. DUPLICATE CODE (409)"
curl -s -o /dev/null -w 'POST with the same code              -> %{http_code}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-A\",\"title\":\"Duplicate\",\"domainCode\":\"RADIOLOGY\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2019-01-01\"}"

line "8. X-ROAD: approver from the registry (200)"
ID=$(curl -s -H "$A" "$B/api/guidelines?textContains=VERIFY-$R-A" \
     | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d[0]['id'] if d else '')" 2>/dev/null)
if [ -n "$ID" ]; then
  curl -s -w '\n     -> %{http_code}\n' -H "$A" "$B/api/guidelines/$ID/approver"
else
  echo "     (guideline not found)"
fi

line "9. X-ROAD FAILURE (502) - fail open"
BAD=$(curl -s -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-D\",\"title\":\"Registry down\",\"domainCode\":\"CERVIX\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2020-01-01\",
       \"approverIsikukood\":\"00000000000\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
if [ -n "$BAD" ]; then
  curl -s -w '\n     /approver -> %{http_code}   %{content_type}\n' -H "$A" "$B/api/guidelines/$BAD/approver"
  curl -s -o /dev/null -w '     ...but the guideline   -> %{http_code}  (fail open: registry down, guideline alive)\n' \
    -H "$A" "$B/api/guidelines/$BAD"
  curl -s -o /dev/null -w '     ...and the report      -> %{http_code}\n' \
    -H "$A" "$B/api/guidelines/review-report"
fi

line "10. OPENAPI CONTRACT"
curl -s -o /dev/null -w 'GET  /v3/api-docs                    -> %{http_code}\n' -H "$A" "$B/v3/api-docs"
curl -s -H "$A" "$B/v3/api-docs" \
  | python3 -c "import sys,json
p=json.load(sys.stdin)['paths']
g=sorted(k for k in p if 'guideline' in k)
print('   guidelines paths in the contract: %d' % len(g))
for k in g: print('     %s' % k)" 2>/dev/null
echo
