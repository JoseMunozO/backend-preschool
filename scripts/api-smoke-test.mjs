#!/usr/bin/env node

import { mkdir, readdir, stat, unlink, writeFile } from "node:fs/promises";
import { join } from "node:path";

const baseUrl = process.env.API_BASE_URL ?? "http://localhost:8080";
const logsDir = process.env.API_SMOKE_LOG_DIR ?? "logs";
const logsToKeep = Number.parseInt(process.env.API_SMOKE_LOGS_TO_KEEP ?? "4", 10);
const scheduleGroupId = Number.parseInt(process.env.API_SMOKE_GROUP_ID ?? "1", 10);
const scheduleStaffId = Number.parseInt(process.env.API_SMOKE_STAFF_ID ?? "1", 10);
const readOnly = parseBoolean(process.env.API_SMOKE_READ_ONLY);
const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
const logFile = join(logsDir, `api-smoke-test-${timestamp}.log`);
const runId = timestamp.replace(/[^0-9TZ-]/g, "").slice(0, 24);
const smokeAssignmentDate = buildSmokeAssignmentDate(new Date());

if (process.argv.includes("--help") || process.argv.includes("-h")) {
  printHelp();
  process.exit(0);
}

const credentials = {
  admin: {
    email: process.env.API_ADMIN_EMAIL ?? "admin@school.com",
    password: process.env.API_ADMIN_PASSWORD ?? "123456",
  },
  parent: {
    email: process.env.API_PARENT_EMAIL ?? "parent.demo@school.com",
    password: process.env.API_PARENT_PASSWORD ?? "123456",
  },
};

const state = {
  passed: 0,
  failed: 0,
  logLines: [],
  tokens: {},
  refs: {},
  skipped: 0,
};

function now() {
  return new Date().toISOString();
}

function log(message = "") {
  state.logLines.push(message);
}

function print(message = "") {
  console.log(message);
  log(message);
}

function printHelp() {
  console.log(`API smoke tester

Usage:
  node scripts/api-smoke-test.mjs
  API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs

Common commands:
  Full local run:
    node scripts/api-smoke-test.mjs

  Read-only run:
    API_SMOKE_READ_ONLY=true node scripts/api-smoke-test.mjs

  Custom backend URL:
    API_BASE_URL=http://localhost:8081 node scripts/api-smoke-test.mjs

Environment variables:
  API_BASE_URL              Backend URL. Default: http://localhost:8080
  API_ADMIN_EMAIL           Admin login email. Default: admin@school.com
  API_ADMIN_PASSWORD        Admin login password. Default: 123456
  API_PARENT_EMAIL          Parent login email. Default: parent.demo@school.com
  API_PARENT_PASSWORD       Parent login password. Default: 123456
  API_SMOKE_READ_ONLY       true/false. Skip write checks when true.
  API_SMOKE_LOGS_TO_KEEP    Number of local logs to retain. Default: 4
  API_SMOKE_GROUP_ID        Existing group id for schedule checks. Default: 1
  API_SMOKE_STAFF_ID        Existing staff id for schedule checks. Default: 1
  API_SMOKE_LOG_DIR         Local log directory. Default: logs
`);
}

function normalizeUrl(path) {
  return `${baseUrl}${path}`;
}

function parseBoolean(value) {
  return value === "true" || value === "1" || value === "yes";
}

function buildSmokeAssignmentDate(date) {
  const baseDate = Date.UTC(2090, 0, 1);
  const dayOffset = Math.floor(date.getTime() / 1000) % 3000;
  return formatDate(new Date(baseDate + dayOffset * 24 * 60 * 60 * 1000));
}

function addDays(dateString, days) {
  const date = new Date(`${dateString}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return formatDate(date);
}

function formatDate(date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

async function request(path, options = {}) {
  const url = normalizeUrl(path);
  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
    ...(options.headers ?? {}),
  };

  let response;
  try {
    response = await fetch(url, {
      method: options.method ?? "GET",
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
  } catch (error) {
    const cause = error.cause?.message ? ` Cause: ${error.cause.message}` : "";
    throw new Error(`Could not connect to ${url}.${cause}`);
  }

  const rawBody = await response.text();
  let body = rawBody;
  const contentType = response.headers.get("content-type") ?? "";

  if (rawBody && contentType.includes("application/json")) {
    try {
      body = JSON.parse(rawBody);
    } catch {
      body = rawBody;
    }
  }

  return {
    url,
    method: options.method ?? "GET",
    status: response.status,
    body,
    rawBody,
  };
}

async function runCheck(name, check) {
  log("");
  log(`[${now()}] START ${name}`);

  try {
    await check();
    state.passed += 1;
    print(`PASS ${name}`);
    log(`[${now()}] PASS ${name}`);
    return true;
  } catch (error) {
    state.failed += 1;
    print(`FAIL ${name}`);
    print(`  ${error.message}`);
    log(`[${now()}] FAIL ${name}`);
    log(error.stack ?? error.message);
    return false;
  }
}

async function runWriteCheck(name, check) {
  if (readOnly) {
    state.skipped += 1;
    print(`SKIP ${name}`);
    log(`[${now()}] SKIP ${name} because API_SMOKE_READ_ONLY=true`);
    return true;
  }

  return runCheck(name, check);
}

function assertStatus(result, expectedStatus) {
  if (result.status !== expectedStatus) {
    throw new Error(formatHttpFailure(result, expectedStatus));
  }
}

function assertStatusIn(result, expectedStatuses) {
  if (!expectedStatuses.includes(result.status)) {
    throw new Error(formatHttpFailure(result, expectedStatuses.join(" or ")));
  }
}

function assertArrayBody(result) {
  if (!Array.isArray(result.body)) {
    throw new Error(`Expected response body to be an array. Received: ${formatBody(result.body)}`);
  }
}

function assertObjectBody(result) {
  if (!result.body || typeof result.body !== "object" || Array.isArray(result.body)) {
    throw new Error(`Expected response body to be an object. Received: ${formatBody(result.body)}`);
  }
}

function assertToken(loginResult) {
  if (!loginResult.body || typeof loginResult.body.token !== "string" || loginResult.body.token.trim() === "") {
    throw new Error(`Login response did not include a token. Body: ${formatBody(loginResult.body)}`);
  }
}

function assertNonEmptyArray(result) {
  assertArrayBody(result);
  if (result.body.length === 0) {
    throw new Error(`Expected response body array to contain at least one item. Received: ${formatBody(result.body)}`);
  }
}

function assertField(value, fieldName) {
  if (value === undefined || value === null || value === "") {
    throw new Error(`Expected field ${fieldName} to be present`);
  }
}

function formatHttpFailure(result, expectedStatus) {
  return [
    `${result.method} ${result.url}`,
    `Expected status: ${expectedStatus}`,
    `Received status: ${result.status}`,
    `Response body: ${formatBody(result.body)}`,
  ].join("\n  ");
}

function formatBody(body) {
  if (body === undefined || body === null || body === "") {
    return "<empty>";
  }
  if (typeof body === "string") {
    return body;
  }
  return JSON.stringify(body, null, 2);
}

async function login(role) {
  const result = await request("/api/auth/login", {
    method: "POST",
    body: credentials[role],
  });

  assertStatus(result, 200);
  assertToken(result);
  state.tokens[role] = result.body.token;
}

async function expectJsonArray(name, path, tokenRole, expectedStatus = 200) {
  await runCheck(name, async () => {
    const result = await request(path, { token: state.tokens[tokenRole] });
    assertStatus(result, expectedStatus);
    if (expectedStatus === 200) {
      assertArrayBody(result);
    }
  });
}

async function expectJsonObject(name, path, tokenRole, expectedStatus = 200) {
  await runCheck(name, async () => {
    const result = await request(path, { token: state.tokens[tokenRole] });
    assertStatus(result, expectedStatus);
    if (expectedStatus === 200) {
      assertObjectBody(result);
    }
  });
}

async function main() {
  print(`API smoke test`);
  print(`Base URL: ${baseUrl}`);
  print(`Log file: ${logFile}`);
  print(`Read-only mode: ${readOnly ? "enabled" : "disabled"}`);

  const backendReachable = await runCheck("backend is reachable", async () => {
    const result = await request("/api/students");
    assertStatus(result, 401);
  });

  if (!backendReachable) {
    print("");
    print("Backend is not reachable. Start it with:");
    print("./mvnw spring-boot:run -Dspring-boot.run.profiles=local");
    await finish();
    return;
  }

  const adminLoggedIn = await runCheck("login admin", () => login("admin"));
  const parentLoggedIn = await runCheck("login parent", () => login("parent"));

  if (!adminLoggedIn || !parentLoggedIn) {
    print("");
    print("One or more logins failed. Skipping authenticated endpoint checks.");
    await finish();
    return;
  }

  await runCheck("list students as admin", async () => {
    const result = await request("/api/students", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertNonEmptyArray(result);
    assertField(result.body[0].studentId, "studentId");
    state.refs.studentId = result.body[0].studentId;
  });
  await expectJsonObject("get student by id as admin", `/api/students/${state.refs.studentId}`, "admin");
  await expectJsonArray("get student guardians as admin", `/api/students/${state.refs.studentId}/guardians`, "admin");
  await runCheck("reject missing student lookup", async () => {
    const result = await request("/api/students/999999999", { token: state.tokens.admin });
    assertStatus(result, 404);
  });
  await runCheck("list parents as admin", async () => {
    const result = await request("/api/parents", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertNonEmptyArray(result);
    assertField(result.body[0].parentId, "parentId");
    state.refs.parentId = result.body[0].parentId;
  });
  await expectJsonArray("list active parents as admin", "/api/parents?status=ACTIVE", "admin");
  await expectJsonArray("search parents as admin", "/api/parents?search=a", "admin");
  await expectJsonObject("get parent by id as admin", `/api/parents/${state.refs.parentId}`, "admin");
  await expectJsonArray("get linked students by parent as admin", `/api/parents/${state.refs.parentId}/students`, "admin");
  await expectJsonObject("get current parent profile", "/api/parents/me", "parent");
  await expectJsonArray("get current parent students", "/api/parents/me/students", "parent");
  await runCheck("reject missing parent lookup", async () => {
    const result = await request("/api/parents/999999999", { token: state.tokens.admin });
    assertStatus(result, 404);
  });
  await runCheck("list users as admin", async () => {
    const result = await request("/api/users", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertNonEmptyArray(result);
    assertField(result.body[0].userId, "userId");
    state.refs.userId = result.body[0].userId;
  });
  await expectJsonArray("list active users as admin", "/api/users?status=ACTIVE", "admin");
  await expectJsonArray("search users as admin", "/api/users?search=admin", "admin");
  await expectJsonObject("get user by id as admin", `/api/users/${state.refs.userId}`, "admin");
  await expectJsonArray("list roles as admin", "/api/roles", "admin");
  await expectJsonObject("get ADMIN role by code", "/api/roles/ADMIN", "admin");
  await runCheck("reject invalid login", async () => {
    const result = await request("/api/auth/login", {
      method: "POST",
      body: {
        email: "missing-user@example.test",
        password: "wrong-password",
      },
    });
    assertStatus(result, 404);
  });
  await runCheck("parent cannot list users", async () => {
    const result = await request("/api/users", { token: state.tokens.parent });
    assertStatusIn(result, [401, 403]);
  });
  await runCheck("list charge types as admin", async () => {
    const result = await request("/api/payments/charge-types", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertNonEmptyArray(result);
    assertField(result.body[0].chargeTypeId, "chargeTypeId");
    state.refs.chargeTypeId = result.body[0].chargeTypeId;
  });
  await expectJsonArray("list active charge types as admin", "/api/payments/charge-types?activeOnly=true", "admin");
  await runCheck("list payment charges as admin", async () => {
    const result = await request("/api/payments/charges", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertNonEmptyArray(result);
    assertField(result.body[0].studentChargeId, "studentChargeId");
    state.refs.studentChargeId = result.body[0].studentChargeId;
  });
  await expectJsonArray("list payment charges by student", `/api/payments/charges?studentId=${state.refs.studentId}`, "admin");
  await expectJsonArray("list pending payment charges", "/api/payments/charges?status=PENDING", "admin");
  await expectJsonObject("get payment charge by id", `/api/payments/charges/${state.refs.studentChargeId}`, "admin");
  await runCheck("reject missing payment charge lookup", async () => {
    const result = await request("/api/payments/charges/999999999", { token: state.tokens.admin });
    assertStatus(result, 404);
  });
  await expectJsonArray("list payments as admin", "/api/payments", "admin");
  await expectJsonArray("list payments by parent as admin", `/api/payments?parentId=${state.refs.parentId}`, "admin");
  await expectJsonArray("list payments by student as admin", `/api/payments/students/${state.refs.studentId}`, "admin");
  await runCheck("reject payments by missing student", async () => {
    const result = await request("/api/payments/students/999999999", { token: state.tokens.admin });
    assertStatus(result, 404);
  });
  await expectJsonArray("list current parent charges", "/api/payments/me/charges", "parent");
  await expectJsonArray("list current parent payments", "/api/payments/me", "parent");
  await runCheck("list materials as admin", async () => {
    const result = await request("/api/materials", { token: state.tokens.admin });
    assertStatus(result, 200);
    assertArrayBody(result);
    if (result.body.length > 0) {
      state.refs.existingMaterialId = result.body[0].materialId;
    }
  });
  await expectJsonArray("search materials as admin", "/api/materials?search=a", "admin");
  await expectJsonArray("list low stock materials as admin", "/api/materials/low-stock", "admin");
  await runWriteCheck("create smoke material as admin", async () => {
    const result = await request("/api/materials", {
      method: "POST",
      token: state.tokens.admin,
      body: {
        sku: `SMOKE-MAT-${runId}`,
        name: `Smoke material ${runId}`,
        category: "smoke-test",
        unit: "unit",
        quantityOnHand: 10,
        minimumQuantity: 3,
        status: "ACTIVE",
        notes: "Created by api-smoke-test.mjs",
      },
    });
    assertStatus(result, 201);
    assertObjectBody(result);
    assertField(result.body.materialId, "materialId");
    state.refs.materialId = result.body.materialId;
  });
  if (!readOnly) {
    await expectJsonObject("get smoke material by id", `/api/materials/${state.refs.materialId}`, "admin");
    await expectJsonArray("search smoke material by sku", `/api/materials?search=SMOKE-MAT-${runId}`, "admin");
    await runWriteCheck("update smoke material as admin", async () => {
      const result = await request(`/api/materials/${state.refs.materialId}`, {
        method: "PUT",
        token: state.tokens.admin,
        body: {
          sku: `SMOKE-MAT-${runId}`,
          name: `Smoke material updated ${runId}`,
          category: "smoke-test",
          unit: "unit",
          quantityOnHand: 10,
          minimumQuantity: 4,
          status: "ACTIVE",
          notes: "Updated by api-smoke-test.mjs",
        },
      });
      assertStatus(result, 200);
      assertObjectBody(result);
    });
    await runWriteCheck("register material stock entry", async () => {
      const result = await request(`/api/materials/${state.refs.materialId}/movements`, {
        method: "POST",
        token: state.tokens.admin,
        body: {
          movementType: "IN",
          quantity: 5,
          notes: "Smoke stock entry",
        },
      });
      assertStatus(result, 201);
      assertObjectBody(result);
      assertField(result.body.materialMovementId, "materialMovementId");
    });
    await runWriteCheck("register material stock output", async () => {
      const result = await request(`/api/materials/${state.refs.materialId}/movements`, {
        method: "POST",
        token: state.tokens.admin,
        body: {
          movementType: "OUT",
          quantity: 2,
          notes: "Smoke stock output",
        },
      });
      assertStatus(result, 201);
      assertObjectBody(result);
    });
    await runWriteCheck("register material stock adjustment", async () => {
      const result = await request(`/api/materials/${state.refs.materialId}/movements`, {
        method: "POST",
        token: state.tokens.admin,
        body: {
          movementType: "ADJUSTMENT",
          quantity: 8,
          notes: "Smoke stock adjustment",
        },
      });
      assertStatus(result, 201);
      assertObjectBody(result);
    });
  }
  await expectJsonArray("list material movements as admin", "/api/materials/movements", "admin");
  if (!readOnly) {
    await expectJsonArray("list smoke material movements", `/api/materials/movements?materialId=${state.refs.materialId}`, "admin");
    await runWriteCheck("reject material output above stock", async () => {
      const result = await request(`/api/materials/${state.refs.materialId}/movements`, {
        method: "POST",
        token: state.tokens.admin,
        body: {
          movementType: "OUT",
          quantity: 99999,
          notes: "Smoke invalid output",
        },
      });
      assertStatus(result, 400);
    });
  }
  await runCheck("reject missing material lookup", async () => {
    const result = await request("/api/materials/999999999", { token: state.tokens.admin });
    assertStatus(result, 404);
  });
  await expectJsonArray("list schedules as admin", "/api/schedules", "admin");
  await expectJsonArray("list schedules by group", `/api/schedules/groups/${scheduleGroupId}`, "admin");
  await expectJsonArray("list schedules by day", "/api/schedules/days/MONDAY", "admin");
  await expectJsonArray("list schedules by group and day", `/api/schedules/groups/${scheduleGroupId}/days/MONDAY`, "admin");
  await runWriteCheck("create smoke schedule slot", async () => {
    const result = await request("/api/schedules", {
      method: "POST",
      token: state.tokens.admin,
      body: {
        groupId: scheduleGroupId,
        primaryStaffId: scheduleStaffId,
        dayOfWeek: "FRIDAY",
        startTime: "13:00:00",
        endTime: "13:30:00",
        activityTitle: `Smoke schedule ${runId}`,
        roomName: "Smoke room",
        notes: "Created by api-smoke-test.mjs",
      },
    });
    assertStatus(result, 201);
    assertObjectBody(result);
    assertField(result.body.scheduleSlotId, "scheduleSlotId");
    state.refs.scheduleSlotId = result.body.scheduleSlotId;
  });
  if (!readOnly) {
    await expectJsonObject("get smoke schedule slot by id", `/api/schedules/${state.refs.scheduleSlotId}`, "admin");
    await runWriteCheck("update smoke schedule slot", async () => {
      const result = await request(`/api/schedules/${state.refs.scheduleSlotId}`, {
        method: "PUT",
        token: state.tokens.admin,
        body: {
          groupId: scheduleGroupId,
          primaryStaffId: scheduleStaffId,
          dayOfWeek: "FRIDAY",
          startTime: "14:00:00",
          endTime: "14:30:00",
          activityTitle: `Smoke schedule updated ${runId}`,
          roomName: "Smoke room updated",
          notes: "Updated by api-smoke-test.mjs",
        },
      });
      assertStatus(result, 200);
      assertObjectBody(result);
    });
    await runWriteCheck("assign primary staff to smoke schedule slot", async () => {
      const result = await request(`/api/schedules/${state.refs.scheduleSlotId}/primary-staff/${scheduleStaffId}`, {
        method: "PUT",
        token: state.tokens.admin,
      });
      assertStatus(result, 200);
      assertObjectBody(result);
    });
  }
  await expectJsonArray("list schedule staff assignments as admin", "/api/schedules/staff-assignments", "admin");
  await expectJsonArray("list schedule staff assignments by group", `/api/schedules/staff-assignments?groupId=${scheduleGroupId}`, "admin");
  await expectJsonArray("list schedule staff assignments by staff", `/api/schedules/staff-assignments?staffId=${scheduleStaffId}`, "admin");
  await runWriteCheck("create smoke staff group assignment", async () => {
    let lastResult;
    for (let attempt = 0; attempt < 10; attempt += 1) {
      const result = await request("/api/schedules/staff-assignments", {
        method: "POST",
        token: state.tokens.admin,
        body: {
          staffId: scheduleStaffId,
          groupId: scheduleGroupId,
          roleInGroup: "TEACHER",
          primary: false,
          startDate: addDays(smokeAssignmentDate, attempt),
          endDate: null,
        },
      });

      if (result.status === 201) {
        assertObjectBody(result);
        assertField(result.body.staffGroupAssignmentId, "staffGroupAssignmentId");
        return;
      }

      lastResult = result;
    }

    assertStatus(lastResult, 201);
  });

  await runWriteCheck("reject invalid schedule time", async () => {
    const result = await request("/api/schedules", {
      method: "POST",
      token: state.tokens.admin,
      body: {
        groupId: scheduleGroupId,
        primaryStaffId: scheduleStaffId,
        dayOfWeek: "MONDAY",
        startTime: "11:00:00",
        endTime: "10:00:00",
        activityTitle: "Invalid smoke test schedule",
        roomName: "Smoke test",
        notes: "This request should fail validation",
      },
    });

    assertStatus(result, 400);
  });

  await finish();
}

async function finish() {
  print("");
  print(`Summary: ${state.passed} passed, ${state.failed} failed, ${state.skipped} skipped`);

  await mkdir(logsDir, { recursive: true });
  await writeFile(logFile, `${state.logLines.join("\n")}\n`, "utf8");
  await pruneOldLogs();

  if (state.failed > 0) {
    print(`Detailed log: ${logFile}`);
    process.exitCode = 1;
  }
}

async function pruneOldLogs() {
  if (!Number.isInteger(logsToKeep) || logsToKeep < 1) {
    return;
  }

  const entries = await readdir(logsDir);
  const appleDoubleLogs = entries
    .filter((entry) => /^\._api-smoke-test-.*\.log$/.test(entry))
    .map((entry) => unlink(join(logsDir, entry)));

  await Promise.all(appleDoubleLogs);

  const smokeLogs = await Promise.all(
    entries
      .filter((entry) => /^api-smoke-test-.*\.log$/.test(entry))
      .map(async (entry) => {
        const path = join(logsDir, entry);
        const metadata = await stat(path);
        return { path, mtimeMs: metadata.mtimeMs };
      }),
  );

  const logsToDelete = smokeLogs
    .sort((left, right) => right.mtimeMs - left.mtimeMs)
    .slice(logsToKeep);

  await Promise.all(logsToDelete.map((entry) => unlink(entry.path)));
}

main().catch(async (error) => {
  state.failed += 1;
  print("FAIL smoke test runner");
  print(`  ${error.message}`);
  log(error.stack ?? error.message);
  await mkdir(logsDir, { recursive: true });
  await writeFile(logFile, `${state.logLines.join("\n")}\n`, "utf8");
  await pruneOldLogs();
  print(`Detailed log: ${logFile}`);
  process.exitCode = 1;
});
