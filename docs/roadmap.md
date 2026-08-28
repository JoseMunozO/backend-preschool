# Functional roadmap - Preschool admin app

Living document to keep the backend aligned with the proposal validated with the client. Summarizes the objective, functional scope, current status, pending items and recommended implementation order.

This is the active roadmap for the current preschool. For a future version aimed at large institutions, see `docs/institution-roadmap.md`.

## Main idea

Build a simple, centralized application so the preschool can manage students, monthly payments, school materials and schedules from a single place.

## Final project goal

Build a clear, easy-to-use administrative tool adapted to how the preschool actually operates. The application should help reduce manual work, avoid errors and give a quick view of the center's status.

- Centralize important information in a single system.
- Save time on repetitive administrative tasks.
- Have better control over payments, students, materials and schedules.
- Allow the system to grow in the future with new functions.

## Problems to solve

| Area | What it aims to solve |
| --- | --- |
| Students | Have an organized record for each child, their important data and their guardians. |
| Monthly payments | Track paid, pending and overdue installments without relying on loose notes or scattered files. |
| School materials | Know what materials exist, how much is left and when they need restocking. |
| Schedules | Organize groups, activities, daily routines and staff in charge. |
| Dashboard | Quickly see the most important things for the day or the month. |

## Current overall backend status

- [x] JWT authentication.
- [x] Base users and roles.
- [x] Role-based security.
- [x] Basic student administration.
- [x] Administration of parents, mothers/fathers and guardians.
- [x] Linking between students and parents/guardians.
- [x] Basic parent/guardian portal: `/api/parents/me` and `/api/parents/me/students`.
- [x] Flyway baseline applied on the existing database.
- [x] Versioned role seed.
- [x] Basic context and main service tests.
- [x] Base administration of monthly payments.
- [x] Base administration of school materials.
- [x] Base administration of schedules.
- [x] Main dashboard.

## Current priority: soft-delete and restore (delete with undo)

Requested from the frontend (2026-08-20): the admin app wants to offer "delete with the ability to undo" for destructive actions, starting with deleting a student, with a grace window of about 7 days before permanent deletion. Extended (2026-08-21) to the rest of the entities where it makes sense, same pattern.

Status per entity:

- [x] `Student` — implemented and verified (2026-08-20).
- [x] `Material` — implemented and verified (2026-08-21). Only difference: `material_movements` has `ON DELETE CASCADE` (not `RESTRICT` like `student_charges`), so the automatic purge does delete the movement history — acceptable, since it's not financial data.
- [x] `ScheduleSlot` — implemented and verified (2026-08-21). Confirmed: `schedule_slots` has no incoming FKs, so the automatic purge is never blocked (verified with a direct `DELETE` against real MySQL, no constraint error).
- [x] `Parent` (parents/guardians) — implemented and verified (2026-08-21). Confirmed design decision: `deletedAt` is independent of the existing `status` field (ACTIVE/INACTIVE) — `status` remains the operational toggle (activate/deactivate) and `deletedAt` only governs the delete/restore/purge mechanics in the admin listing; the rest of the services (payments, consents, etc.) still use `parentRepository.findById` without filtering by `deletedAt`, same pattern already used for `Student`/`Material` in other services. `student_guardians` has `ON DELETE CASCADE` (the link is lost, acceptable), `payments` has `ON DELETE SET NULL` (payment history is preserved), `student_consents` has no explicit action = `RESTRICT` by default (protects the consent record, same as a student's charges) — verified with a direct `DELETE` against real MySQL in all three cases.

Pattern used for `Student`, `Material`, `ScheduleSlot` and `Parent`:

- [x] Added a `deletedAt` field (nullable timestamp) to the `Student` entity (`V8__add_student_deleted_at.sql`), `Material` (`V9__add_material_deleted_at.sql`), `ScheduleSlot` (`V10__add_schedule_slot_deleted_at.sql`) and `Parent` (`V11__add_parent_deleted_at.sql`).
- [x] `DELETE /api/students/{id}` now soft-deletes: sets `deletedAt = now()` instead of removing the row.
- [x] `GET /api/students` and `GET /api/students/{id}` now exclude records with a non-null `deletedAt` by default.
- [x] New endpoint `POST /api/students/{id}/restore`: clears `deletedAt` if still within the grace window (7 days); responds `404` if the student doesn't exist or isn't deleted, `409` if the window already expired.
- [x] Scheduled job (`StudentPurgeScheduler`, daily at 03:00) that permanently purges records with a `deletedAt` older than 7 days. If the student has payment charges or another record protected by `ON DELETE RESTRICT`, the purge of that row is skipped (and logged) instead of failing the whole job or losing financial history — it stays soft-deleted indefinitely. Verified against real MySQL: trying to delete a student with charges raises `Error 1451` (constraint), one with no charges deletes without issue.
- [x] `GET /api/students?includeDeleted=true` so admin can see recently deleted records (includes `deletedAt` in the response).
- [x] Tests: soft-delete doesn't remove the row, restore inside/outside the window, purge respects FK-protected records, `includeDeleted=true`. Also verified end-to-end against real Docker (create, delete, confirm hidden, restore, confirm visible, force an expired window via SQL and confirm 409).
- [x] Same pattern applied to `Material` (`DELETE/POST restore /api/materials/{id}`, `MaterialPurgeScheduler` daily at 03:15), `ScheduleSlot` (`DELETE/POST restore /api/schedules/{id}`, `ScheduleSlotPurgeScheduler` daily at 03:30) and `Parent` (`DELETE/POST restore /api/parents/{id}`, `ParentPurgeScheduler` daily at 03:45) — staggered times so the jobs don't collide with each other.
- [x] `Parent` extended with a third state, "archived" (2026-08-21): unlike the other 3 entities, `Parent` is not purged after 7 days — it moves to an intermediate long-retention state (6 years) meant for families who might re-enroll. See the "Extended Parent lifecycle" section below for detail.

With this, the 4 agreed entities (Student, Material, ScheduleSlot, Parent) now have complete soft-delete/restore/purge support.

This unblocks the frontend's "confirmations for sensitive actions" feature on the student-delete side (see `frontend-preschool/docs/frontend-roadmap.md`).

### Extended Parent lifecycle (2026-08-21)

Specific client request: if a family leaves the preschool and later comes back (with the same child or another one), it would be better to be able to recover the previous parent/guardian record instead of creating a new one from scratch — that way the history of linked children is kept (useful for future discount or benefit eligibility) and there's no need to recreate the login account.

That's why `Parent` has 4 states instead of the simple 2 (active/deleted) that the other entities use:

1. **Active** — normal.
2. **Trash (0-7 days)** — same as the other 3 entities: `deletedAt` set, full data intact, visible with `includeDeleted=true`, fully undoable with `POST /api/parents/{id}/restore` (no change from the original behavior).
3. **Archived (day 7 onward, new)** — instead of purging after 7 days like the other entities, a job (`ParentPurgeScheduler.archiveExpiredSoftDeletedParents`, daily at 03:45) minimizes the record:
   - Kept: `firstName`, `lastName`, `email` (so it can still be found visually in the archive) and the linked login account (`User`, password intact). The link to the children (`student_guardians`) is also automatically preserved since the parent/guardian row isn't deleted, only some fields are cleared.
   - Cleared: `phone`, `address`, `preferredLanguage`, `notes`.
   - Marked with the new `archivedAt` field.
4. **Permanent purge** — only 6 years after `archivedAt` (not the original soft-delete), a new job (`ParentPurgeScheduler.purgeExpiredArchivedParents`, daily at 03:50) deletes the row for good, respecting the same FK guard that already existed (if there are registered consents, it's skipped and logged instead of failing).

If the family comes back while in the archived state: new endpoint `POST /api/parents/{id}/claim` — takes the full data (name/email were already there, phone/address/etc. need to be filled in), clears `deletedAt` and `archivedAt`, and reactivates the same `parentId` as before (which is why the children's history stays linked). Responds `404` if the parent/guardian isn't archived, `409` if 6 years have already passed since `archivedAt`.

`POST /restore` didn't change: it's still exclusively the "quick undo" for the first 7 days, and already rejects automatically (409) once the record moves to archived, with no need to touch its logic.

Verified end-to-end against real Docker: delete, force the 7-day window to expire via SQL, simulate archiving (fields minimized via SQL, name/email intact), confirm 404 on a direct `GET`, confirm visible with `includeDeleted=true` showing `archivedAt`, claim it with `claim` and confirm it becomes a normal active parent again, force `archivedAt` to 7 years ago and confirm `409` on `claim`, and confirm the final purge (direct `DELETE` in MySQL) hits no FK blocks.

## Recommended initial version

The first version should build a functional base that lets us validate whether the application meets the preschool's real needs. The goal isn't to include everything from day one, but to start with what matters most and expand later.

| Module | Included in first version | Current status |
| --- | --- | --- |
| Students | Create, edit, query and organize students. | Partially implemented. |
| Parents/guardians | Register guardians and link them to each student. | Implemented on the backend. |
| Payments | Monthly tracking with paid, pending and overdue states. | Implemented on the backend. |
| School materials | Basic inventory with low-quantity alerts. | Implemented on the backend. |
| Schedules | Basic organization by group and activities. | Implemented on the backend. |
| Dashboard | General summary of key information. | Implemented on the backend. |

## A. Student administration

### Client criteria

- Record of active, pending or discharged students.
- Individual profile with name, date of birth, group/classroom, contact details and important notes.
- Linking the student with their responsible parents or guardians.
- Space for important information such as allergies, medical notes or emergency contacts.
- Internal notes about each child.
- Photos of the children and an optional photo album per student.
- Reminder of upcoming birthdays.
- Search and filters to quickly find a student.

### Current status

- [x] Create student.
- [x] List students.
- [x] Look up student by id.
- [x] Update student.
- [x] Delete student: soft-delete with restore and a 7-day grace window — see the "Current priority: soft-delete and restore" section above.
- [x] Student status.
- [x] Group/classroom via `groupId`.
- [x] Allergies, medical notes and observations.
- [x] Linking with parents/guardians via `student_guardians`.
- [x] Search by name, code, group or status: `GET /api/students?search=&groupId=&status=`.
- [x] Formal filters by status/group: same `groupId` and `status` parameters.
- [x] Emergency contacts as a field or a dedicated entity: `student_emergency_contacts` entity (name, relationship, phone, alternate phone, notes, primary contact), endpoints `GET/POST/PUT/DELETE /api/students/{id}/emergency-contacts`.
- [x] Full profile response including guardians: `StudentResponse` now includes `guardians` (full list of guardians with contact details) in addition to `primaryGuardianName`, both in `GET /api/students` and `GET /api/students/{id}`.
- [x] Review whether current notes are enough or whether a per-date/per-user note history is needed (2026-08-24): added an audit history (`student_note_audit_log`, same pattern as `material_audit_log`) — every edit of a note (`PUT /api/students/{id}/notes/{noteId}`) records who changed it, when, and the content before/after. New endpoint `GET /api/students/{id}/notes/{noteId}/audit-log`, same access rules as the rest of the notes (admin/director unrestricted, teacher only if the student is in one of their active groups).
- [x] Investigate photo storage (2026-08-22): decided on local filesystem for now — simpler for the current environment (local Docker Compose, no cloud credentials yet), migrating to S3/Cloudinary later is feasible without redoing the data model since `profilePhotoUrl`/`photoUrl` are already URLs. Dedicated Docker volume (`preschool_uploads_data:/app/uploads`) so it survives `docker compose down`.
- [x] Define the photo album model per student (2026-08-22): reuses the existing `PhotoAlbum`/`PhotoAlbumPhoto` model (per student or per group), no new model was needed.
- [x] Endpoint to upload a student photo (2026-08-22): `PUT /api/students/{id}/profile-photo` now accepts `multipart/form-data` (previously just a text URL) — saves the actual file to disk, validates JPEG/PNG/WEBP/GIF, still requires active `IMAGE_PROFILE_PHOTO` consent. `POST /api/photo-albums/{albumId}/photos` also switches to `multipart/form-data`.
- [x] Endpoint to list a student's album (2026-08-22): already existed (`GET /api/photo-albums?studentId=`), no changes.
- [x] Endpoint to delete a student photo (2026-08-22): `DELETE .../profile-photo` and `DELETE /api/photo-albums/{albumId}/photos/{photoId}` now also delete the physical file from disk (previously there was no real file to delete). Verified that purging frees space without breaking the record (it stays soft-deleted in the row).
- [x] Endpoint or dashboard item for upcoming birthdays: already implemented on the dashboard (`upcomingBirthdays` in `teacher-summary` and `admin-summary`), see section F.
- [x] Controller/API tests.

### Expected outcome

Staff will be able to quickly look up each child's information without relying on paper, old messages or scattered files.

## B. Parent/guardian administration

### Client criteria

- Registration of parents, mothers/fathers or legal guardians.
- Contact details: phone, email and relationship to the student.
- Ability to link a guardian with one or several students.
- Identification of the person primarily responsible for payments or communications.

### Current status

- [x] Create parent/mother/father/guardian.
- [x] List parents/guardians.
- [x] Search parents/guardians.
- [x] Look up parent/guardian by id.
- [x] Update parent/guardian.
- [x] Activate/deactivate parent/guardian.
- [x] Create a `User` account with the `PARENT` role.
- [x] View own profile via `/api/parents/me`.
- [x] View own students via `/api/parents/me/students`.
- [x] Link a parent/guardian with one or several students.
- [x] Define relationship: `FATHER`, `MOTHER`, `GUARDIAN`, `RELATIVE`, `OTHER`.
- [x] Mark primary contact.
- [x] Mark responsible for payments.
- [x] Mark authorized for pickup.
- [x] Mark whether they live with the student.
- [x] Core service tests.
- [x] Review final payloads for the frontend (2026-08-24): confirmed by the frontend after reviewing the actual code (`ParentListItem`, `StudentGuardianSummary`, the linking flow in `parents.api.ts`) — already consumed end-to-end and tested against the real backend, no field missing or extra. Non-blocking side note: `GET /api/parents` doesn't support server-side `search` the way `/api/students` does, noted for later if needed.
- [x] Add controller/API tests: `ParentControllerApiTest` (admin filters, a parent's own access, rejection when unauthenticated).
- [x] `TEACHER` access to parents/guardians (2026-08-22), requested so a teacher can contact the family in an emergency: reading a single guardian (`GET /api/parents/{parentId}`, `GET /api/parents/{parentId}/students`), and the general listing (`GET /api/parents`) automatically filtered to the guardians of students whose group the teacher currently has assigned. Managing guardians (create/edit/activate/deactivate/delete) remains exclusive to `SUPER_ADMIN`/`ADMIN`/`DIRECTOR`. Coordinated with the frontend: a student's guardians now automatically show up as the first emergency contacts on the profile (from `guardians[]`, already exposed on `GET /api/students/{id}`), with no need for the teacher to type them in by hand.

### Expected outcome

The center will quickly know who to contact and who is responsible for each student.

## C. Monthly payment administration

### Client criteria

- Record of monthly fee per student.
- Clear states: paid, pending or overdue.
- Payment history per student and per month.
- Filter by month, student or payment status.
- Ability to record payment date, payment method and administrative comment.
- Initial payment methods: cash, card and transfer.
- Optional: generate a simple receipt or PDF proof of payment in a later phase.

### Current status

- [x] The database contains tables related to payments and charges.
- [x] Java models for charge types, student charges, payments, allocations and staff.
- [x] Repositories for payments, charges, charge types, allocations and staff.
- [x] Payment/charge DTOs.
- [x] `PaymentService`.
- [x] `PaymentController`.
- [x] Endpoint to list payments per student: `GET /api/payments/students/{studentId}`.
- [x] Endpoint to filter charges by month: `GET /api/payments/charges?month=YYYY-MM`.
- [x] Endpoint to filter charges by student or status.
- [x] Endpoint to record a payment: `POST /api/payments`.
- [x] Support payment method: `CASH`, `CARD`, `TRANSFER`.
- [x] Calculation of the outstanding balance per charge.
- [x] Automatic update of a charge's status when payments are recorded.
- [x] Parent/guardian access to their own payments: `GET /api/payments/me`.
- [x] Parent/guardian access to their own charges: `GET /api/payments/me/charges`.
- [x] Role-based security for `ADMIN`, `DIRECTOR`, `FINANCE` and `PARENT`.
- [x] Service tests.
- [x] Update `api-test.http`.
- [x] Explicit endpoint to update/cancel a charge's status without recording a payment (2026-08-21): `PUT /api/payments/charges/{studentChargeId}`, reuses the same `StudentChargeRequest` used by `POST /api/payments/charges`. Allows editing any field of the charge (date, amount, billing period, description, student/charge type) and explicitly changing the `status` — especially useful for `CANCELLED`, which until now was only reachable when creating the charge, never afterward. If `status` comes in as `null` in the request, the current status isn't touched (avoids accidentally overwriting a `PAID`/`PARTIALLY_PAID` value computed automatically when another field is edited). Verified: cancelling blocks further payments on that charge (`No se puede pagar un cargo cancelado`, a rule that already existed), reactivating (explicit `status: PENDING`) allows payments again, `404` if the charge doesn't exist.
- [x] Monthly report/summary of pending and overdue payments: `GET /api/payments/reports/monthly?month=YYYY-MM` (month optional, defaults to the current month). Returns the count, balance and detail of that month's pending and overdue charges, plus the total payments received.
- [x] Controller/API tests.
- [ ] Review query optimization if payment volume grows.
- [x] Generation of a simple receipt or PDF proof of payment in a later phase (2026-08-24): confirmed with the client — an informational receipt (not a tax document, no NCF/DGII numbering). Generated on the server automatically when a payment is recorded (`POST /api/payments`, non-blocking if it fails — the payment is still recorded, and the receipt is regenerated on request if missing). New `GET /api/payments/{paymentId}/receipt` returns the PDF directly (`application/pdf`, `Content-Disposition: attachment`); access: staff (admin/director/finance) can see any receipt, a parent/guardian only their own. The PDF is stored in a directory separate from `/uploads` (not served publicly like the photos, since a receipt contains payment data) — dedicated Docker volume `preschool_receipts_data`. Migrating to a tax-compliant document with NCF later is possible without redoing this (same endpoint/storage), but would require auditable sequential numbering and accounting validation — out of scope for now.
- [x] Automatic generation of the monthly fee (2026-08-23): direct client question — charges stopped being generated after May/June because there was never an automated process, every charge was created by hand. New daily job (`MonthlyChargeGenerationScheduler`, 02:00) that generates the month's charge for every active student and every active `ChargeType` with `recurrenceType=MONTHLY`, if one doesn't already exist for that student/type/month (idempotent, safe to run multiple times). If the student enrolls mid-month, the first charge is prorated based on the remaining days of the month (client's decision). In addition to the automatic daily run, `POST /api/payments/generate-monthly-charges?month=YYYY-MM` allows triggering it manually for the current month or another one, in case the job didn't run some day.
- [x] Demo prices updated to real market values in the DR (2026-08-24): monthly fee RD$6,000 (midpoint of the RD$4,500-7,500/month range for mid-tier daycares in the DR, client's decision). Meal plan RD$1,500, field trip RD$500 and materials fee RD$2,000 remain reasonable estimates, not firm market data (no specific figure was found for a "meal plan/field trip fee charged by the center" in Dominican preschools, unlike the monthly fee which does have direct data) — adjust if the client confirms other amounts. Only updates the development seed (`docker/mysql/init/01-base-schema.sql`); see the `ChargeType` without API management item further below in the release checklist.
- [x] ~~Per-student discount system~~ — **fully replaced on 2026-08-25, see the item below.** History (2026-08-23 to 25): three consecutive iterations of a discount as a recurring rule per student were tried (`student_discounts`, percentage/fixed amount, date-based validity), including a fix for instantly recalculating open charges (found by testing live against a real student, Lucas Andersson: the charge's price didn't drop when the discount was created until the next cycle) and then two validity types (`INSTANT`/`SCHEDULED`) plus a cap based on the student's withdrawal date. When testing that last version live, the client found the underlying problem: a "per-student" rule was applied at once to *all* charge types (monthly fee AND meal plan, even overdue charges from other months) — there was no way for the discount to apply to just one specific invoice. Instead of continuing to patch the recurring-rule model, it was dropped entirely in favor of a simpler model, see below.
- [x] Discounts on a specific charge (2026-08-25): requested by the client after finding the problem above — a discount must be applicable to one specific charge (whether already existing or at creation time), never "to all payments." The `student_discounts` entity and its entire system were removed (migration `V21`, no data migration needed: they were same-day test rows). The discount now lives directly on `StudentCharge`: `originalAmount` (the amount before any discount, captured once), `discountType`, `discountValue`, `discountReason`. Three ways to use it: `PUT /api/payments/charges/{id}/discount` to apply/replace the discount on an existing charge (always recalculates from `originalAmount`, never compounds if reapplied); `DELETE /api/payments/charges/{id}/discount` to remove it and go back to the original amount; and `POST /api/payments/charges` accepts the three discount fields optionally to apply it right at creation. Verified live against real Docker with real data: a 20% discount on a single monthly-fee charge without affecting the same student's meal-plan charge for the same month. Integrated into the frontend the same day (a "Discounts" button per row in Payments, plus a checkbox when creating a new charge), verified live against the real backend.
- [x] `hasDiscount` filter on `GET /api/payments/charges` (2026-08-25): requested by the frontend team for a "discount history" view in Reports (every charge that currently has a discount, across all students) — without this, all charges would need to be fetched and most discarded on the client. `hasDiscount=true/false` is added to the filters already on that same endpoint (`studentId`/`status`/`month`), instead of creating a new report, since the response already carries all the discount fields. Important: there's no real audit trail/history of discounts, and there never was (unlike notes or materials, which do have their own audit log) — removing a discount leaves no trace, so this view is a snapshot of the current state, not a true history. Integrated into the frontend the same day: a discounts tab in Reports, plus a read-only "currently active discounts" section on the student profile.
- [x] Paid invoices with PDF download (2026-08-25): requested by the client — a financial history showing already-paid invoices (charges), each with a button to download the PDF. Almost everything already existed: the receipt PDF (`GET /api/payments/{paymentId}/receipt`, see above in this module) already lists the invoices covered by that payment, method, reference, date, parent and who received it — it's a real invoice document, not just a confirmation. The only missing piece was knowing which payment(s) covered a specific charge from the charge listing: `StudentChargeResponse` now includes `paymentIds` (the distinct payment IDs from that charge's allocations, empty if not yet paid). `GET /api/payments/charges?status=PAID` already serves as the list of paid invoices. Verified live against real Docker: a paid charge returns its `paymentId`, and downloading that payment's receipt gives a real PDF. Integrated into the frontend the same day.
- [x] One-off charges (field trip, extra hours, etc.) (2026-08-23): confirmed with the client that this already worked — `POST /api/payments/charges` already allows creating a charge of any type (`ChargeType` with `recurrenceType` `ONE_TIME` or `CUSTOM`) for a specific student, no changes needed.

### Expected outcome

The preschool will be able to quickly see who has paid, who is pending and which payments need follow-up.

## D. School material administration

### Client criteria

- Inventory of the center's materials: stationery, cleaning supplies, toys, food or other categories.
- Available quantity and recommended minimum quantity.
- Alerts when a material is low or needs restocking.
- Recording of material entries and exits.
- Person responsible or comment associated with a material movement.

### Current status

- [x] The database contains tables related to materials and movements.
- [x] Java models for materials and movements.
- [x] Repositories for inventory and movements.
- [x] DTOs.
- [x] `MaterialService`.
- [x] `MaterialController`.
- [x] Endpoint to list inventory: `GET /api/materials`.
- [x] Endpoint to create/edit a material.
- [x] Endpoint to record a material entry.
- [x] Endpoint to record a material exit.
- [x] Endpoint to record an adjustment from a physical count.
- [x] Endpoint to query movements.
- [x] Endpoint and filter for materials below the minimum stock.
- [x] Delete material: soft-delete with restore and a 7-day grace window, same as students (`DELETE /api/materials/{id}`, `POST /api/materials/{id}/restore`, `GET /api/materials?includeDeleted=true`). Unlike students, `material_movements` has `ON DELETE CASCADE` (not `RESTRICT`), so the automatic purge does delete the purged material's movement history — acceptable since it isn't financial information.
- [x] Suggested minimum stock (2026-08-21): `GET /api/materials/{id}/suggested-minimum?window=WEEK|MONTH|THREE_MONTHS|SIX_MONTHS|TWELVE_MONTHS` — calculates actual consumption (`OUT` movements) in the chosen window and normalizes it to a comparable monthly average. It's only a suggestion, it never writes `minimumQuantity` automatically; the admin still decides the final value via `PUT /api/materials/{id}` as before. With no movements in the window, it returns `hasData: false` instead of making up a number.
- [x] Name of who performed the movement (2026-08-21): `MaterialMovementResponse` gains `performedByName`, resolved via `Staff` (if the user who made the movement is registered staff); otherwise it stays `null` and `performedByEmail` is used as before.
- [x] Audit trail for material edits (2026-08-21): every `PUT /api/materials/{id}` saves a before/after snapshot in `material_audit_log` (who, when, previous and new values). New endpoint `GET /api/materials/{id}/audit-log`.
- [x] History retention (2026-08-21): daily job (`MaterialHistoryPurgeScheduler`, 04:00) that permanently deletes `material_movements` and `material_audit_log` older than 3 years — these are operational inventory records, not tax documents, so the DGII's (DR) 10-year legal retention requirement, which would apply to `payments`, doesn't apply here; that's why `payments` is explicitly excluded from any automatic deletion for now.
- [x] Internal role-based security.
- [x] Service tests.
- [x] Update `api-test.http`.
- [x] Controller/API tests.
- [ ] Review final categories with the client.
- [ ] Review whether the person responsible needs to be a specific staff member instead of the authenticated user.

### Expected outcome

The center will be able to prevent material shortages and plan purchases with more control.

## E. Schedule administration

### Client criteria

- Schedules per group or classroom.
- Daily activities: drop-off, meals, nap time, recess, educational activities and pick-up.
- Assignment of staff responsible per activity or group.
- Daily or weekly view to make planning easier.
- Space for special events or one-off changes.

### Current status

- [x] The database contains a table related to schedules.
- [x] Java models for schedules and staff-to-group assignments.
- [x] Repositories.
- [x] DTOs.
- [x] `ScheduleService`.
- [x] `ScheduleController`.
- [x] Endpoint to list schedules: `GET /api/schedules`.
- [x] Endpoint for schedules by group: `GET /api/schedules/groups/{groupId}`.
- [x] Endpoint for schedules by day: `GET /api/schedules/days/{dayOfWeek}`.
- [x] Endpoint for schedules by group and day: `GET /api/schedules/groups/{groupId}/days/{dayOfWeek}`.
- [x] Endpoint to create/edit an activity.
- [x] Endpoint to assign the main person responsible.
- [x] Endpoint to view/assign staff to groups.
- [x] Internal role-based security.
- [x] Service tests.
- [x] Controller/API tests.
- [x] Update `api-test.http`.

### Expected outcome

Staff will have a clear view of the preschool's daily and weekly organization.

## F. Main dashboard

### Client criteria

- Summary of active students.
- Pending or overdue payments for the month.
- Materials with low stock.
- Important schedules or activities for the day.
- Upcoming student birthdays.
- Quick access to the main sections.

### Current status

- [x] Create summary DTO.
- [x] Create `DashboardService`.
- [x] Create `DashboardController`.
- [x] Main endpoint `GET /api/dashboard/summary` for admin/direction.
- [x] Split the dashboard into `teacher-summary`, `admin-summary` and `finance-summary` endpoints.
- [x] Count of active students.
- [x] Count/list of pending or overdue payments for the month.
- [x] Financial dashboard restricted to `SUPER_ADMIN`, `ADMIN`, `DIRECTOR` and `FINANCE`.
- [x] Count/list of materials with low stock.
- [x] Important schedules or activities for the day.
- [x] List of upcoming birthdays.
- [x] Service tests.
- [x] Controller/API tests.
- [x] Update `api-test.http`.
- [x] Teacher dashboard: low-materials card replaced with a summary of the day's attendance (2026-08-22) — `todayAttendanceSummary` in `GET /api/dashboard/teacher-summary` (`presentCount`, `absentCount`, `sickCount`, `lateCount`, `unmarkedCount`, computed over active students). The rest of the teacher dashboard (active students, today's schedule, birthdays) didn't change; the low-materials summary stays unchanged in `admin-summary`.

### Expected outcome

When entering the application, the client will see what matters most without having to check module by module.

## Functions for later phases

- [ ] Parent portal: check payments, schedules or center announcements.
- [ ] Automatic notifications for pending payments or important announcements.
- [x] Daily attendance record (2026-08-22): a specific client request via the frontend team, for an attendance/sick-children widget on the teacher dashboard (replacing the low-materials card there). New `student_attendance` entity (`V14__create_student_attendance.sql`), one record per student per day (`status`: `PRESENT`, `ABSENT`, `SICK`, `LATE`, plus notes and who recorded it). `GET /api/attendance?groupId=&date=` returns the full group roster for that date (including students not yet marked, with `status: null`); `POST /api/attendance` saves or updates several records at once (upsert by student+date). `TEACHER` can only read/save attendance for groups currently assigned to them (same `staff_group_assignments` criteria already used for notes/consents/albums/parents); `SUPER_ADMIN`/`ADMIN`/`DIRECTOR` have no group restriction. Verified end-to-end against real Docker: a teacher marks attendance for their group (200), tries a non-assigned group (403).
- [x] Locking edits after midnight (2026-08-23): a specific client request — during the day attendance can be corrected as many times as needed (e.g. mark a child absent and later change it to late if they arrive afterward), but once midnight passes that day is "archived" and can no longer be modified. `POST /api/attendance` now rejects a `date` other than today: `409` if it's a previous day (already archived), `400` if it's a future day (doesn't make sense to record something that hasn't happened yet). `GET /api/attendance` remains unrestricted — previous days can still be queried, they just stop being editable.
- [x] Per-student attendance history (2026-08-23): requested by the frontend team for a history modal in `AttendancePage.tsx` — `GET /api/attendance?groupId=&date=` only covered a single day for a whole group, there was no way to pull several days for a single student. New `GET /api/attendance/students/{studentId}?from=&to=` (both optional, defaulting to the last 30 days), returns the list sorted by date descending in the same format already used by the rest of the attendance endpoints. Same group-assignment-based access rule for `TEACHER` (based on the student's current group).
- [x] Monthly reports for payments, students or inventory (2026-08-25): a client request — that `TEACHER` and `FINANCE` see different reports (not the same listing), and higher ranks (`SUPER_ADMIN`/`ADMIN`/`DIRECTOR`) see all of them. Coordinated with the frontend team before settling on the endpoints (what today was an empty "Reports" page, `PlaceholderPage`). Six reports in total: financial (`GET /api/payments/reports/monthly`, already existed), attendance aggregated by date range/group (`GET /api/attendance/reports/summary`), note history with nested audit trail per student to cover a substitute/replacement (`GET /api/students/{id}/reports/notes-history`), material movements with an optional running balance (`GET /api/materials/reports/movements`), health/allergy data per student (`GET /api/students/reports/health`), and trash (`GET /api/reports/trash`, admin only) which brings together in a single view everything deleted/archived/deactivated (students, materials, parents — including the 6-year archive stage —, schedules and deactivated staff), each with its calculated purge deadline. Access: `SUPER_ADMIN`/`ADMIN`/`DIRECTOR` see all 6; `TEACHER` sees attendance/notes/materials/health; `FINANCE` sees financial and materials (for expense/purchase control). Integrated into the frontend as the single Reports page (with 6 tabs) and also the only place to navigate/restore trash and archived records — the old per-module trash buttons (students/parents/materials/schedules/staff) were removed and each module's "New X" button was moved to the bottom of the page (Payments was left as-is, at the top). Verified in the browser for all 3 role levels.
- [x] Generation of PDF receipts and documents (2026-08-24): see detail in Module C ("Generation of a simple receipt or PDF proof of payment in a later phase").
- [x] Comment-style notes for students: responsible teachers can create/edit their own comments; direction/admin can review the history and moderate (2026-08-24): most of this already existed (chronological list of notes per student with author/type/date, audit history from PR #64). The real gap: any teacher assigned to the group could edit or delete ANOTHER teacher's note about the same student — only group membership was validated, not authorship. Now `PUT/DELETE /api/students/{id}/notes/{noteId}` additionally requires being the note's author if the role is `TEACHER` ("Solo puedes editar o eliminar las notas que tu creaste"); `SUPER_ADMIN`/`ADMIN`/`DIRECTOR` remain unrestricted, as before. `moderateNote` didn't change (still accessible to any teacher in the group, existing and already-tested behavior, not explicitly requested by the client to change).
- [x] Backend base for a student profile photo via `profilePhotoUrl`.
- [x] Real upload/storage of a student profile photo (2026-08-22): see detail in section A above.
- [x] Photo album with real storage per student or group (2026-08-22): permissions by assigned group/student already existed (`ensureCanAccessAlbum`/`ensureCanWriteAlbum`); what was missing was real file storage, now resolved.
- [x] Privacy/image consents: parents or guardians must accept terms before allowing use of the student's photos. Already resolved on the backend (see "Future module - Notes, photos and consents" below: profile photo and albums already require active `IMAGE_PROFILE_PHOTO`/`PHOTO_ALBUM` consent). Only the frontend UI is missing, see the "Family consent UI before enabling profile photo/albums in production" item below.
- [x] Advanced roles (2026-08-22): the 6 base roles already existed (`SUPER_ADMIN`, `ADMIN`, `DIRECTOR`, `TEACHER`, `FINANCE`, `PARENT`) and the multi-role-per-user mechanism (`user_roles` is many-to-many, `POST/DELETE /api/users/{userId}/roles` already worked). Client request: that a `TEACHER` could also take on finance or administrative tasks (e.g. enrolling students, managing materials) without creating a new role — solved by also assigning them the `FINANCE` or `ADMIN` role via the existing mechanism. A more granular, catalog-style permission system (checking off individual permissions per user) was evaluated against reusing the existing roles as activatable "permission packages" for staff; the client chose the second option as much lower risk/effort for the team's current size.
- [x] Advanced role management rules (2026-08-22): every role now has a numeric `rankLevel` (`SUPER_ADMIN=100`, `ADMIN`/`DIRECTOR=90`, `TEACHER`/`FINANCE=10`, `PARENT=0`, migration `V15`). Whoever grants or removes a role (`POST/DELETE /api/users/{userId}/roles`, or when creating a user/staff) can only do so if that role's rank is less than or equal to their own maximum rank — same rank is allowed (`ADMIN` and `DIRECTOR` can grant each other's role and grant `TEACHER`/`FINANCE`), but nobody below `SUPER_ADMIN` can grant `SUPER_ADMIN`. Also, `SUPER_ADMIN` can't be removed from the last user who holds it (prevents the system from ending up with nobody with full access). New endpoint `POST /api/staff` (with `GET /api/staff` and `GET /api/staff/{id}`) to onboard a new staff position, with an optional login account and initial roles — previously there was no endpoint for this at all, `Staff` only came from seed data.
- [x] Staff offboarding (employee fired/resigned) (2026-08-22): `DELETE /api/staff/{staffId}` deactivates the position (hidden from the listing by default, visible with `GET /api/staff?includeDeleted=true`) and deactivates their login account if they have one (they can no longer log in). Unlike `Student`/`Material`/`Parent`/`ScheduleSlot`, **it's never purged**: there's no time window or scheduled job for permanent deletion — an explicit client decision to avoid risking the loss of schedule/audit history tied to a `staffId`, and because unlike those other cases there was no technical reason (a grace window before purge) to limit when it can be undone. `POST /api/staff/{staffId}/restore` reactivates the position and its login account, with no time limit. Same rank guard as with roles: you can't deactivate a position whose account has a role ranked higher than the requester's, and you can't deactivate the last `SUPER_ADMIN` in the system.
- [x] Auto-expiring staff accounts (2026-08-26): a client request for real substitute-teacher cases with a known end date (e.g. covering a one-month leave). New optional `accessExpiresAt` field when creating staff (`POST /api/staff`, migration `V22`, must be a future date). `StaffAccountExpirationScheduler` runs daily and automatically deactivates the position and its account (reusing the same logic already used by `DELETE /api/staff/{staffId}`) as soon as that date is reached — recoverable at any time via `POST /api/staff/{staffId}/restore`, the same as a manual offboarding. Verified end-to-end against real Docker (creation with a future date, rejection of a past date with 400).
- [ ] Internal messaging system between administration and parents.

Note: part of the parent portal already started with `/api/parents/me`, `/api/parents/me/students`, `/api/payments/me` and `/api/payments/me/charges`. Schedules and announcements for parents remain pending.

### Future module - Notes, photos and consents

This module should be treated as sensitive because it may include personal information about minors.

Current status:

- [x] Backend base for comment-style notes with author, type, date, moderation and soft delete.
- [x] Teachers can manage notes only for students whose group is currently assigned to them.
- [x] Direction/admin can review, moderate, update or delete notes for any student.
- [x] Base profile photo available with a URL on the student.
- [x] Backend base for family consents per student and guardian.
- [x] The profile photo requires active `IMAGE_PROFILE_PHOTO` consent.
- [x] Backend base for URL-based albums/photos, with approval, soft delete and group-based permissions.
- [x] Photos linked to a student require active `PHOTO_ALBUM` consent.
- [x] Detailed edit/advanced audit history for notes (2026-08-24): see Module A, `GET /api/students/{id}/notes/{noteId}/audit-log`.
- [ ] Family consent UI before enabling profile photo/albums in production.
- [x] Real file/image storage for albums (2026-08-22): `FileStorageService` saves to the local filesystem, serves via `/uploads/**` (a public static resource, unauthenticated — the same exposure level the external URLs like Cloudinary/S3 already had when stored before as plain text), and deletes the physical file when a photo is removed or the profile photo is replaced.

Initial desired rules:

- Notes should work as comments with an author, date, type and a possible edit history.
- Teachers can create and modify notes only for students or groups under their responsibility.
- Direction/admin can review, moderate or delete notes when needed.
- The student's profile photo must depend on active family consent.
- Albums can be organized by student, group, date, event or manually.
- Teachers can upload/modify photos only for their groups or assigned students.
- Directors/admin can review, approve, delete or correct photos.
- Parents/guardians must accept a privacy/image consent before photos of the student are enabled.
- There must be a way to revoke consent and define what happens to existing photos.
- Basic audit trail should be in place: who uploaded, modified, deleted or approved content.

Suggested future endpoints:

```text
GET /api/students/{studentId}/notes
POST /api/students/{studentId}/notes
PUT /api/students/{studentId}/notes/{noteId}
PATCH /api/students/{studentId}/notes/{noteId}/moderate
DELETE /api/students/{studentId}/notes/{noteId}

POST /api/students/{studentId}/profile-photo
DELETE /api/students/{studentId}/profile-photo

GET /api/photo-albums
POST /api/photo-albums
GET /api/photo-albums/{albumId}
PUT /api/photo-albums/{albumId}
DELETE /api/photo-albums/{albumId}
POST /api/photo-albums/{albumId}/photos
PATCH /api/photo-albums/{albumId}/photos/{photoId}/approve
DELETE /api/photo-albums/{albumId}/photos/{photoId}

GET /api/students/{studentId}/consents
POST /api/students/{studentId}/consents
PATCH /api/students/{studentId}/consents/{consentId}/revoke
```

## Expected usage flow

1. The administrator logs into the system and sees the main dashboard.
2. They can quickly check pending payments, low materials and the day's activities.
3. From students, they can look up or update each child's information.
4. From payments, they can record monthly fees and review debts.
5. From materials, they can update entries, exits and purchase needs.
6. From schedules, they can organize the preschool's daily or weekly routine.

## Points to validate with the client

- [ ] Exactly what data needs to be stored for each student. (Open: there can always be additional fields the client asks for.)
- [x] What types of comments/notes are needed (2026-08-21): already resolved by `StudentNoteType` — `PEDAGOGICAL`, `BEHAVIOR`, `INCIDENT`, `HEALTH`, `FAMILY_FOLLOW_UP`, `ADMINISTRATIVE` (matches pedagogical/behavior/incidents/health/family follow-up/administrative).
- [ ] How they currently handle payments and whether there are different fee types. (Open: a client process question, not a system one — `ChargeType` already supports flexible fee types, but the actual process still needs confirming.)
- [x] Confirm payment methods (2026-08-21): already resolved by `PaymentMethod` — `CASH`, `CARD`, `TRANSFER`, plus `SWISH` and `OTHER`.
- [x] Confirm whether "transfer" needs a reference number (2026-08-21): already resolved — `Payment.referenceNumber` exists (generic for any method, not exclusive to transfers).
- [x] Confirm the privacy/consent policy for photos (2026-08-21): already resolved by `StudentConsentType` (`IMAGE_PROFILE_PHOTO`, `PHOTO_ALBUM`, `INTERNAL_DOCUMENTATION`, `MARKETING_PUBLICATION`) with `acceptedAt`/`revokedAt` on `StudentConsent`.
- [x] Confirm how photos are organized (2026-08-21): already resolved — `PhotoAlbum` per student/group with an approval flow (`PATCH .../photos/{photoId}/approve`).
- [x] Confirm whether teachers can only modify photos/notes for their own groups or assigned students (2026-08-22): already resolved — `StudentNoteService`, `StudentConsentService` and `PhotoAlbumService` already restrict `TEACHER` to students whose group is currently assigned to them (`staff_group_assignments`, the same criterion reused today for the parent/guardian listing). See lines 356 and 361 above.
- [x] Confirm how many days in advance the system should flag upcoming birthdays (2026-08-24): already resolved before this conversation — `upcomingBirthdays` on the dashboard already uses a 30-day window (`BIRTHDAY_LOOKAHEAD_DAYS` in `DashboardService`), exactly "about a month in advance" as the client requested. No changes needed.
- [x] Whether parents need direct access to the application (2026-08-21): already resolved — implemented from the start, `PARENT` role with login and its own endpoints (`/api/parents/me`, `/api/payments/me`, etc.).
- [x] What types of materials they want to track in inventory (2026-08-21): already resolved — `Material.category` is free text with no restriction, a flexibility decision already made.
- [x] How groups, classrooms and schedules are organized (2026-08-21): already resolved — `ClassGroup` and `ScheduleSlot` already implemented.
- [x] Who will use the system (2026-08-21): already resolved — 6 roles implemented (`SUPER_ADMIN`, `ADMIN`, `DIRECTOR`, `TEACHER`, `FINANCE`, `PARENT`).
- [x] Whether printable documents, receipts or reports are needed from the start (2026-08-24): PDF payment receipt already implemented, see "Generation of a simple receipt or PDF proof of payment in a later phase" in Module C. Other printable documents/reports (beyond the receipt) remain deferred, see `docs/plan-puntos-pendientes-2026-08-24.md`.

## Infrastructure and quality

- [x] Flyway configured.
- [x] Baseline applied on the existing schema.
- [x] Versioned role seed.
- [x] `application-local.properties` kept out of version control.
- [x] Workaround for AppleDouble `._*` files on an exFAT volume.
- [x] `api-test.http` updated with the main flows.
- [x] `api-test.http` updated with the base monthly-payments flow.
- [x] `api-test.http` updated with the base schedules flow.
- [x] Automatic smoke tester for the main endpoints.
- [x] Smoke tester with local logs, retention of the last 4 logs and a read-only mode.
- [x] Mockito configured as a Java agent for tests on Java 25.
- [x] Added future migrations `V3`-`V7` for new schema changes (notes, consents, albums, emergency contacts); `V1` also added as a real baseline for clean databases.
- [x] Improved controller test coverage: added `AuthControllerApiTest`, `RoleControllerApiTest`, `UserControllerApiTest` and `PhotoAlbumControllerApiTest` (previously without their own controller tests).
- [x] Reviewed JPA's `open-in-view`: `false` was tried and broke most endpoints (`LazyInitializationException` on associations like `Parent`); kept explicitly `true` with a comment. See the "Checklist for official release" section.
- [x] Reviewed Mockito/Java-agent warnings on Java 25 (2026-08-21). Mockito's own auto-attach warning was already resolved (see line 445, `-javaagent` configured in `pom.xml`). The warning that persists (`sun.misc.Unsafe::objectFieldOffset ... lombok.permit.Permit`) isn't from Mockito, it's from Lombok: a known open bug, reported against JDK 24 and 25 in Lombok's official repo (issues [#3852](https://github.com/projectlombok/lombok/issues/3852), [#3959](https://github.com/projectlombok/lombok/issues/3959), [#3907](https://github.com/projectlombok/lombok/issues/3907), [#4046](https://github.com/projectlombok/lombok/issues/4046)), persisting even in the most recent version (1.18.46, the one we already use). Java flagged those methods as "deprecated for removal" ([JEP 471](https://openjdk.org/jeps/471)) but hasn't removed them yet — purely cosmetic, nothing to fix on our side until Lombok ships a fix.
- [x] Reviewed the Flyway warning with MySQL (2026-08-21). Outdated compatibility metadata in the Flyway version bundled with Spring Boot 4.0.6 (`11.14.1`, officially verified only up to older MySQL versions than the one we use). Flyway's official documentation already verifies up to MySQL 9.4. Confirmed repeatedly in this session that migrations run fine against real MySQL (docker 8.4) — it's just an informational notice at startup, not a functional blocker. Forcing a different Flyway version than the one Spring Boot manages internally would be more risk (incompatibility) than benefit (silencing a cosmetic warning), so it's left as is.
- [x] Fixed requests denied by role returning `401` instead of `403` in the real app (not caught by `@WebMvcTest` tests): `response.sendError()` triggered an internal forward to `/error` without re-authenticating, overwriting the status. Fix: `/error` marked `permitAll()`. Verified with a real-server integration test (`SecurityErrorDispatchIntegrationTest`) that fails without the fix and passes with it.
- [x] Fixed a missing static file under `/uploads/**` returning `500` instead of `404` (2026-08-22): found while implementing real photo storage — `GlobalExceptionHandler` had a generic `@ExceptionHandler(Exception.class)` that intercepted Spring's `NoResourceFoundException` (which already carries its own 404 status) and turned it into a 500, also logging it as an "Unhandled exception". Fix: a specific handler for `NoResourceFoundException` that responds with 404. Verified against real Docker: requesting an already-deleted photo now returns 404 instead of 500.

## Closing proposal

Build a first version focused on internal administration: students, guardians, payments, materials, schedules and the dashboard. After testing it with the center's real usage, flows can be adjusted and functions added such as a parent portal, notifications, attendance and advanced reports.

## Checklist for official release

Before publishing the application for the client's real use:

- [x] Prepare a clean production database: point Flyway at a real, empty MySQL instance (don't reuse `docker/mysql/init/`, which is only for local development and seeds demo data). Verified that an empty database migrates completely via Flyway with no demo data.
- [ ] If `api-smoke-test.mjs` was ever run in write mode against a shared/staging environment, clean up the `SMOKE-*` residue with `scripts/cleanup-smoke-data.sql` (manually review `staff_group_assignments`, which has no distinctive field to filter by pattern).
- [x] Applied Flyway migrations from scratch and confirmed the schema comes out complete: added `V1__initial_schema.sql`, verified against an empty database (creates all 20 tables) and against the existing docker-compose flow (Flyway baselines instead of re-running V1).
- [x] Kept only the seeds needed for base roles and essential system data: `V2__seed_roles.sql` only seeds the 6 base roles, no demo data inside Flyway.
- [ ] Create the client's initial admin user. (Requires the client's real environment/credentials; pending until there's a deployment target.)
- [x] Endpoint to manage `ChargeType` (2026-08-24): requested so admin/finance can create and adjust prices (e.g. raise the monthly fee) from the application, instead of only being able to view them. New `POST /api/payments/charge-types` and `PUT /api/payments/charge-types/{id}` (unique code, name, recurrence type, default amount, active/inactive) — same access rules as the rest of `/api/payments`. Editing the `defaultAmount` of an existing type (e.g. `MONTHLY_FEE`) is automatically reflected in the next run of the monthly-generation job, with nothing else to touch. Separate, non-blocking pending item: on a clean production database there still wouldn't be any `ChargeType` at startup (the seed for the 4 base types remains development-only) — someone with admin access would need to create them once via this new endpoint before automatic generation has anything to generate.
- [ ] Configure a real, sufficiently long `JWT_SECRET`. (The app now fails to start if it's missing; still needs configuring in the real deployment environment.)
- [ ] Configure real database credentials and don't use development passwords. (Same: depends on the real deployment environment.)
- [x] Confirmed that `application-local.properties`, `.env` and secrets aren't pushed to the repository. Verified in `.gitignore` and with `git check-ignore`.
- [x] Ran `./mvnw test`. 71/71 passing.
- [ ] Run `API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs` against the final or staging environment. (Only run against local/Docker so far; there's no real staging environment yet.)
- [x] Reviewed important runtime warnings before delivery. See the "Infrastructure and quality" section.
- [x] Documented the URL, initial user and basic operating steps for the client: see `docs/operations-runbook.md` (includes the procedure to create the first admin on a clean database, verified end-to-end).

## Recommended next step

Start a pre-release review of the backend: check configuration, security, known warnings, demo data, the smoke tester, operational documentation and the delivery checklist before moving on to the frontend or new features.
