# GitHub Actions CI

This project uses GitHub Actions to validate backend changes automatically on pushes and pull requests.

## Workflow

File:

```text
.github/workflows/backend-ci.yml
```

## When It Runs

The workflow runs on:

- Pushes to `main`
- Pushes to `develop`
- Pushes to `feature/**`
- Pushes to `chore/**`
- Pushes to `fix/**`
- Pull requests targeting `main`
- Pull requests targeting `develop`

## What It Checks

### Test and package

Uses Java 25 with Temurin and Maven cache.

Before Maven runs, the workflow starts the MySQL service from Docker Compose:

```bash
docker compose up -d --wait mysql
```

Maven then connects to Docker MySQL on `localhost:3307` using the Docker seed schema.

Command:

```bash
./mvnw --batch-mode verify
```

This compiles the project, runs the automated tests, and verifies that the Spring Boot package can be built.

### Docker setup

Validates the Docker Compose file:

```bash
docker compose config
```

Builds the backend Docker image:

```bash
docker build --pull --tag backend-preschool:ci .
```

## How To Read The Result

In GitHub, open the repository and go to:

```text
Actions > Backend CI
```

Green means the backend passed the checks. Red means the branch should not be merged until the failing step is fixed.

## Future Improvements

- Add smoke tester execution in CI with Docker Compose.
- Upload smoke test logs as GitHub Actions artifacts when a smoke test fails.
- Add branch protection so `main` only accepts merges after CI passes.
- Add dependency/security scanning.
