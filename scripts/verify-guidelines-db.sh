#!/usr/bin/env bash
# Shows the data layer as the assessment asks for it: three tables, foreign
# keys, and the proof that every one of them came from a version-controlled
# migration rather than from someone typing into a console.
#
# Usage: bash scripts/verify-guidelines-db.sh
C="${1:-ite4120-postgres-1}"
U="${2:-animals}"

psql() { docker exec "$C" psql -U "$U" -d "$U" "$@"; }

echo
echo "=============== 1. THE TABLES ==============="
psql -c "\dt guidelines.*"

echo "=============== 2. FOREIGN KEYS ==============="
psql -c "
select conname                as constraint_name,
       conrelid::regclass     as child_table,
       confrelid::regclass    as parent_table
  from pg_constraint
 where contype = 'f'
   and connamespace = 'guidelines'::regnamespace
 order by conname;"

echo "=============== 3. BUILT BY MIGRATIONS, NOT BY HAND ==============="
echo "Liquibase records every changeset it runs. This is that record:"
psql -c "
select orderexecuted as n,
       id            as changeset,
       author,
       dateexecuted::date as applied
  from public.databasechangelog
 where filename like '%guidelines%'
 order by orderexecuted;"

echo "=============== 4. THE JOIN THE REPORT IS BUILT ON ==============="
psql -c "
select d.name                                              as domain,
       count(distinct g.id)                                as documents,
       count(distinct g.id) filter
            (where g.published_on < current_date - interval '4 years') as overdue,
       count(r.id)                                         as recommendations
  from guidelines.domain d
  left join guidelines.guideline g
         on g.domain_code = d.code and g.sys_status = 'A'
  left join guidelines.recommendation r
         on r.guideline_id = g.id and r.sys_status = 'A'
 group by d.code, d.name
 order by d.name;"
echo
