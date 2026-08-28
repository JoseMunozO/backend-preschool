# Preschool Admin — Backend

Before this project, running the preschool's day-to-day meant payments scribbled in notebooks or
scattered spreadsheets, material inventory nobody could confirm at a glance, and attendance that
lived on paper. This API is the engine that centralizes that daily operation: who's paid and who
owes, what stock remains and when to reorder, who showed up today, and who's responsible for each
child — all queryable instantly instead of reconstructed by hand at month's end.

It's the backend half of a complete application (Spring Boot REST API + React web UI). The UI
lives in the sibling repo [`frontend-preschool`](https://github.com/JoseMunozO/frontend-preschool)
and consumes this API for everything — no business logic is duplicated on the client side. This
document explains what the system solves today; the reasoning behind each decision (what the
client asked for, what was tried and discarded) lives in [`docs/roadmap.md`](docs/roadmap.md).

## Stack

- Java 25, Spring Boot 4
- MySQL 8.4, Flyway (versioned migrations in `src/main/resources/db/migration`)
- Spring Security + JWT
- iText/OpenPDF (PDF receipts and invoices)
- Docker Compose to run everything locally

## Quick start

```bash
docker compose up --build
```

- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Demo credentials: [`docs/demo-credentials.md`](docs/demo-credentials.md)

Full detail (environment variables, volumes, troubleshooting) in
[`docs/docker.md`](docs/docker.md).

## What it actually solves

**A director or administrator** opens the dashboard and sees at a glance what used to take phone
calls and manual digging: how many payments are overdue and how much is owed — with the late fee
already calculated, not something someone has to add up separately — which materials are running
low, and which birthdays are coming up. They can onboard or deactivate staff, assign roles without
inventing a new account type for every combination of responsibilities, and — if something gets
deleted by mistake, a student, a parent, a material — recover it, because every deletion has a real
grace window before it's actually gone for good.

**A teacher** logs in and sees only their own: the students in their assigned group, not the whole
preschool. They mark today's attendance (once midnight passes, that record is archived and can no
longer be altered, so "yesterday's attendance" means what actually happened, not whatever someone
decided to change afterward). They leave notes on a student — behavioral, medical, pedagogical —
knowing only they can edit their own, and that if a substitute covers their class one day, that
substitute can read the full history of every note, who changed what and when, not just the latest
version.

**Finance** sees tuition, meal plan, and any other charge per student, records payments (partial or
full, against one charge or several at once), and generates a PDF receipt on the spot. If one
specific charge needs a discount — a scholarship, a sibling case — it applies to that one invoice,
not to everything that student owes that month; tuition and the meal plan don't get mixed up by
accident.

**A parent or guardian** logs into their own portal and sees only their own children: their
payments, their attendance, nothing about other students.

All of this runs on the same access rules at every layer (not just "this route requires this
role," but "this teacher only sees the groups they're currently assigned to"), and everything
deleted passes through a recoverable trash first before being purged — a deliberate choice so one
wrong click doesn't erase history someone needs later.

## Modules

| Module | What it covers |
| --- | --- |
| Students | Full profile, group, profile photo, allergies/medical notes, teacher notes with an audit trail, image consents, photo albums. |
| Parents / guardians | Onboarding, linking to one or more students, their own portal, extended lifecycle (trash -> archived for 6 years -> purge) designed for families that leave and return. |
| Payments | Configurable charge types, automatic monthly generation with proration, payments allocated across one or several charges, PDF receipts/invoices, per-charge discounts, a late fee computed live. |
| Materials | Inventory with stock in/out/adjustments, low-stock alerts, movement history and audit trail, minimum-quantity suggestions based on consumption. |
| Schedules & attendance | Weekly schedule per group, daily attendance locked after midnight, per-student history. |
| Reports | Six views gated by role: financial, attendance, notes history, material movements, health/allergies, and a unified trash view. |
| Roles & staff | Six roles with rank-based hierarchy, staff onboarding/offboarding that's reactivable without losing history, optional auto-expiring access for time-limited positions (e.g. substitute teachers). |
| Dashboard | A summary tailored to each role (teacher, finance, admin). |

## Tests and verification

```bash
./mvnw test                                    # full suite (unit + integration)
node scripts/api-smoke-test.mjs                # smoke test against a running backend
API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs   # variant that creates/modifies nothing
```

See [`docs/api-smoke-tester.md`](docs/api-smoke-tester.md) for more detail.

## Documentation

- [Functional roadmap](docs/roadmap.md) — full decision history and status per module.
- [Institutional roadmap (future)](docs/institution-roadmap.md)
- [Docker](docs/docker.md)
- [Smoke tester](docs/api-smoke-tester.md)
- [CI (GitHub Actions)](docs/github-actions-ci.md)
- [Cloud deployment](docs/cloud-deployment.md)
- [Operations runbook](docs/operations-runbook.md)
- [Demo credentials](docs/demo-credentials.md)
