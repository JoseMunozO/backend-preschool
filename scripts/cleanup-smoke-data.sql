-- Manual cleanup for leftover data from api-smoke-test.mjs running in write
-- mode (API_SMOKE_READ_ONLY=false) against a shared/staging database.
--
-- Read-only mode (the recommended mode before releases and on staging, see
-- docs/api-smoke-tester.md) never writes anything and needs no cleanup.
--
-- Student notes and photo albums created by the smoke test are already
-- deleted by the script itself at the end of each run. Materials and
-- schedule slots have no DELETE endpoint in the API, so they can be left
-- behind; this script removes them by the naming patterns the script uses.
--
-- Review the SELECTs before running the DELETEs. Run against the target
-- database explicitly; this script does not select one for you.

-- Materials created via `sku: SMOKE-MAT-<runId>`, `category: smoke-test`.
SELECT * FROM material_movements
WHERE material_id IN (SELECT material_id FROM materials WHERE sku LIKE 'SMOKE-MAT-%');
SELECT * FROM materials WHERE sku LIKE 'SMOKE-MAT-%' OR category = 'smoke-test';

-- DELETE FROM material_movements
-- WHERE material_id IN (SELECT material_id FROM materials WHERE sku LIKE 'SMOKE-MAT-%');
-- DELETE FROM materials WHERE sku LIKE 'SMOKE-MAT-%' OR category = 'smoke-test';

-- Schedule slots created with `activityTitle: Smoke schedule <runId>` /
-- `Smoke schedule updated <runId>`, `roomName: Smoke room` / 'Smoke room
-- updated', or the invalid-time negative check ('Invalid smoke test
-- schedule', roomName 'Smoke test').
SELECT * FROM schedule_slots
WHERE activity_title LIKE 'Smoke schedule%'
   OR activity_title = 'Invalid smoke test schedule'
   OR room_name IN ('Smoke room', 'Smoke room updated', 'Smoke test');

-- DELETE FROM schedule_slots
-- WHERE activity_title LIKE 'Smoke schedule%'
--    OR activity_title = 'Invalid smoke test schedule'
--    OR room_name IN ('Smoke room', 'Smoke room updated', 'Smoke test');

-- NOTE: "create smoke staff group assignment" writes a row to
-- staff_group_assignments (role_in_group='TEACHER', primary=false) with no
-- distinguishing text field, so it cannot be safely matched by pattern here
-- without risking real assignments. If write-mode smoke tests ever ran
-- against a shared database, review staff_group_assignments manually for
-- rows dated around when the script ran (see the smoke test's log file
-- timestamp under logs/) instead of deleting by pattern.
