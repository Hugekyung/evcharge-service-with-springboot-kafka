\set ON_ERROR_STOP on
DO $$
DECLARE n integer;
BEGIN
SELECT count(*) INTO n FROM information_schema.columns WHERE table_schema='public' AND table_name='charging_session'; IF n <> 11 THEN RAISE EXCEPTION 'session column count %',n; END IF;
SELECT count(*) INTO n FROM information_schema.columns WHERE table_schema='public' AND table_name='charging_event'; IF n <> 10 THEN RAISE EXCEPTION 'event column count %',n; END IF;
SELECT count(*) INTO n FROM information_schema.table_constraints WHERE table_schema='public' AND constraint_name IN ('uk_charging_session_session_id','uk_charging_event_event_id') AND constraint_type='UNIQUE'; IF n <> 2 THEN RAISE EXCEPTION 'named unique count %',n; END IF;
SELECT count(*) INTO n FROM pg_indexes WHERE schemaname='public' AND indexname='idx_charging_event_session_sequence' AND indexdef LIKE '%(session_id, sequence)%'; IF n <> 1 THEN RAISE EXCEPTION 'composite index missing'; END IF;
SELECT count(*) INTO n FROM pg_indexes WHERE schemaname='public' AND tablename IN ('charging_session','charging_event') AND indexname NOT IN ('charging_session_pkey','charging_event_pkey','uk_charging_session_session_id','uk_charging_event_event_id','idx_charging_event_session_sequence'); IF n <> 0 THEN RAISE EXCEPTION 'extra app indexes %',n; END IF;
SELECT count(*) INTO n FROM flyway_schema_history WHERE version='1' AND success; IF n <> 1 THEN RAISE EXCEPTION 'V1 success count %',n; END IF;
RAISE NOTICE 'CATALOG_ASSERTIONS_PASS tables=2 columns=21 named_uniques=2 composite_index=1 extra_app_indexes=0 v1_success_count=1';
END$$;
