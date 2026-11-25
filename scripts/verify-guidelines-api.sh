#!/usr/bin/env bash
# Kaib labi GUIDELINES.01 API nouded. Server peab jooksma (scripts/run-backend).
# Kasutus: bash scripts/verify-guidelines-api.sh [base-url]
B="${1:-http://localhost:18440}"
A="Authorization: Bearer superadmin"
J="Content-Type: application/json"

line() { printf '\n--- %s ---\n' "$1"; }

# Unikaalne jarelliide, et skript oleks korduvalt jooksutatav: kood on
# aktiivsete seas unikaalne, nii et fikseeritud kood annaks teisel korral 409.
R="$(date +%H%M%S)"

line "1. LOEND (200)"
curl -s -o /dev/null -w 'GET  /api/guidelines                 -> %{http_code}\n' -H "$A" "$B/api/guidelines"

line "2. REPORTING-OPERATSIOON (200) — uhendab kolm tabelit"
curl -s -o /dev/null -w 'GET  /api/guidelines/review-report   -> %{http_code}\n' -H "$A" "$B/api/guidelines/review-report"
curl -s -H "$A" "$B/api/guidelines/review-report" \
  | python3 -c "import sys,json
for r in json.load(sys.stdin):
    print('   %-18s dokumente=%-3s ule_tahtaja=%-3s soovitusi=%-3s vanim=%s' % (
        r['domainName'], r['guidelineCount'], r['overdueCount'],
        r['recommendationCount'], r.get('oldestPublishedOn') or '-'))" 2>/dev/null

line "3. LOOMINE (201 + Location)"
curl -s -o /dev/null -w 'POST /api/guidelines                 -> %{http_code}   Location: %header{location}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-A\",\"title\":\"Kontrolljuhend\",\"domainCode\":\"RADIOLOGY\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2019-01-01\",
       \"approverIsikukood\":\"38102130265\"}"

line "4. PUUDUV ID (404 + problem+json)"
curl -s -o /dev/null -w 'GET  /api/guidelines/999999          -> %{http_code}   %{content_type}\n' -H "$A" "$B/api/guidelines/999999"

line "5. VALIDEERIMINE (400) — tulevikus avaldatud"
curl -s -w '\n     -> %{http_code}   %{content_type}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-B\",\"title\":\"Tulevikust\",\"domainCode\":\"CARDIOLOGY\",
       \"documentType\":\"RAVIJUHEND\",\"publishedOn\":\"2099-01-01\"}"

line "6. VALIDEERIMINE (400) — tundmatu valdkond"
curl -s -w '\n     -> %{http_code}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-C\",\"title\":\"Vale valdkond\",\"domainCode\":\"KARDIOLOOGIA\",
       \"documentType\":\"RAVIJUHEND\",\"publishedOn\":\"2020-01-01\"}"

line "7. DUBLIKAAT (409)"
curl -s -o /dev/null -w 'POST sama koodiga                    -> %{http_code}\n' \
  -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-A\",\"title\":\"Dublikaat\",\"domainCode\":\"RADIOLOGY\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2019-01-01\"}"

line "8. X-ROAD: kinnitaja registrist (200)"
ID=$(curl -s -H "$A" "$B/api/guidelines?textContains=VERIFY-$R-A" \
     | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d[0]['id'] if d else '')" 2>/dev/null)
if [ -n "$ID" ]; then
  curl -s -w '\n     -> %{http_code}\n' -H "$A" "$B/api/guidelines/$ID/approver"
else
  echo "     (juhendit ei leitud)"
fi

line "9. X-ROAD TORGE (502) — fail open"
BAD=$(curl -s -X POST "$B/api/guidelines" -H "$A" -H "$J" \
  -d "{\"code\":\"VERIFY-$R-D\",\"title\":\"Register maas\",\"domainCode\":\"CERVIX\",
       \"documentType\":\"SELTSI_JUHEND\",\"publishedOn\":\"2020-01-01\",
       \"approverIsikukood\":\"00000000000\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
if [ -n "$BAD" ]; then
  curl -s -w '\n     /approver -> %{http_code}   %{content_type}\n' -H "$A" "$B/api/guidelines/$BAD/approver"
  curl -s -o /dev/null -w '     ...aga juhend ise      -> %{http_code}  (fail open: register maas, juhend elus)\n' \
    -H "$A" "$B/api/guidelines/$BAD"
  curl -s -o /dev/null -w '     ...ja raport           -> %{http_code}\n' \
    -H "$A" "$B/api/guidelines/review-report"
fi

line "10. OPENAPI LEPING"
curl -s -o /dev/null -w 'GET  /v3/api-docs                    -> %{http_code}\n' -H "$A" "$B/v3/api-docs"
curl -s -H "$A" "$B/v3/api-docs" \
  | python3 -c "import sys,json
p=json.load(sys.stdin)['paths']
g=sorted(k for k in p if 'guideline' in k)
print('   guidelines teed lepingus: %d' % len(g))
for k in g: print('     %s' % k)" 2>/dev/null
echo
