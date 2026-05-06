#!/usr/bin/env node

import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";

const baseUrl = process.env.API_BASE_URL ?? "http://localhost:8080";
const logsDir = process.env.API_SMOKE_LOG_DIR ?? "logs";
const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
const logFile = join(logsDir, `api-smoke-test-${timestamp}.log`);

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

function normalizeUrl(path) {
  return `${baseUrl}${path}`;
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

function assertStatus(result, expectedStatus) {
  if (result.status !== expectedStatus) {
    throw new Error(formatHttpFailure(result, expectedStatus));
  }
}

function assertArrayBody(result) {
  if (!Array.isArray(result.body)) {
    throw new Error(`Expected response body to be an array. Received: ${formatBody(result.body)}`);
  }
}

function assertToken(loginResult) {
  if (!loginResult.body || typeof loginResult.body.token !== "string" || loginResult.body.token.trim() === "") {
    throw new Error(`Login response did not include a token. Body: ${formatBody(loginResult.body)}`);
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

async function main() {
  print(`API smoke test`);
  print(`Base URL: ${baseUrl}`);
  print(`Log file: ${logFile}`);

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

  await expectJsonArray("list students as admin", "/api/students", "admin");
  await expectJsonArray("list parents as admin", "/api/parents", "admin");
  await expectJsonArray("list roles as admin", "/api/roles", "admin");
  await expectJsonArray("list payment charges as admin", "/api/payments/charges", "admin");
  await expectJsonArray("list current parent charges", "/api/payments/me/charges", "parent");
  await expectJsonArray("list materials as admin", "/api/materials", "admin");
  await expectJsonArray("list schedules as admin", "/api/schedules", "admin");
  await expectJsonArray("list schedule staff assignments as admin", "/api/schedules/staff-assignments", "admin");

  await runCheck("reject invalid schedule time", async () => {
    const result = await request("/api/schedules", {
      method: "POST",
      token: state.tokens.admin,
      body: {
        groupId: 1,
        primaryStaffId: 1,
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
  print(`Summary: ${state.passed} passed, ${state.failed} failed`);

  await mkdir(logsDir, { recursive: true });
  await writeFile(logFile, `${state.logLines.join("\n")}\n`, "utf8");

  if (state.failed > 0) {
    print(`Detailed log: ${logFile}`);
    process.exitCode = 1;
  }
}

main().catch(async (error) => {
  state.failed += 1;
  print("FAIL smoke test runner");
  print(`  ${error.message}`);
  log(error.stack ?? error.message);
  await mkdir(logsDir, { recursive: true });
  await writeFile(logFile, `${state.logLines.join("\n")}\n`, "utf8");
  print(`Detailed log: ${logFile}`);
  process.exitCode = 1;
});
