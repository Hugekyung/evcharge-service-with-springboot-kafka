\pset pager off
\echo '== tables and columns =='
SELECT table_name,column_name,ordinal_position,data_type,character_maximum_length,numeric_precision,numeric_scale,is_nullable
FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('charging_session','charging_event') ORDER BY table_name,ordinal_position;
\echo '== named unique constraints =='
SELECT tc.table_name,tc.constraint_name,tc.constraint_type,kcu.column_name
FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu USING (constraint_catalog,constraint_schema,constraint_name,table_name)
WHERE tc.table_schema='public' AND tc.constraint_type='UNIQUE' ORDER BY tc.table_name,tc.constraint_name,kcu.ordinal_position;
\echo '== indexes =='
SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND tablename IN ('charging_session','charging_event') ORDER BY tablename,indexname;
\echo '== flyway history =='
SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank;
\echo '== counts =='
SELECT (SELECT count(*) FROM charging_session) AS session_rows,(SELECT count(*) FROM charging_event) AS event_rows,(SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success) AS v1_success_count;
