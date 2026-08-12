SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name IN ('charging_session','charging_event')
ORDER BY table_name, ordinal_position;
SELECT conrelid::regclass AS table_name, conname, contype, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid::regclass::text IN ('charging_session','charging_event')
ORDER BY 1,2;
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname='public' AND tablename IN ('charging_session','charging_event')
ORDER BY tablename,indexname;
SELECT COUNT(*) AS flyway_v1_count
FROM flyway_schema_history
WHERE version='1' AND description='create charging tables' AND success;
