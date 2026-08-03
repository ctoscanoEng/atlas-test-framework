# ATLAS — Automated Test Layer & Assurance Suite

[![CI](https://github.com/ctoscanoEng/atlas-test-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/ctoscanoEng/atlas-test-framework/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/)
[![Selenium](https://img.shields.io/badge/Selenium-4.33-brightgreen)](https://www.selenium.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.11-blue)](https://testng.org/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7.22-green)](https://cucumber.io/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)
[![Live report](https://img.shields.io/badge/live%20report-latest%20CI%20run-8a2be2)](https://ctoscanoeng.github.io/atlas-test-framework/latest/)

A hybrid test automation framework built on **Java 17 · Selenium 4 · TestNG · Cucumber**, with a
**self-healing locator engine**, thread-safe parallel execution, Selenium Grid / Docker support
and — unusually — **its own application under test**, served in-process by the framework.

The whole suite runs **offline, deterministically, on any machine with a JDK and a browser**:

```bash
git clone https://github.com/ctoscanoEng/atlas-test-framework.git
cd atlas-test-framework
./mvnw test -Psmoke
```

No Maven installation, no driver binaries, no demo website, no network. That is the point.

**Prefer to look before you clone?** Every run on `main` publishes its report:
**[the latest execution, live](https://ctoscanoeng.github.io/atlas-test-framework/latest/)** — steps,
timings, run configuration and, when something fails, the screenshot and the page source.

---

## Why this project is not another Selenium demo

| Most portfolio projects | ATLAS |
|---|---|
| Test a public demo site that breaks the suite when it changes | Ships **its own application under test** (static SPA + JSON API) served by the JDK HTTP server on an ephemeral port |
| `By.id("submit")` everywhere; one redesign kills 200 tests | **Multi-strategy locators** with fallbacks, plus an audit trail of every element that had to be healed |
| `Thread.sleep(3000)` disguised as "waiting" | **Zero sleeps.** Every wait is a condition with a deadline |
| Retries everything, so real regressions hide behind a green build | Retries **only environmental failures**; an `AssertionError` always stays red |
| `static WebDriver driver;` — parallel execution corrupts state | Session bound to the thread via `ThreadLocal`, released in a `finally` |
| Screenshot folder nobody opens | HTML report with steps, evidence, run configuration and a **locator maintenance backlog** |

The application under test deliberately **regenerates element ids at every page load**, exactly like a
component library rebuilt on each deploy. Locators written the naive way break on the second run;
the resolver keeps the suite green and files the debt. It is a demonstration, not a claim.

---

## Quick start

```bash
./mvnw test -Psmoke                     # ~1 min, 9 tests, headless Chrome
./mvnw test -Pregression                # 36 tests, 6 threads, UI + API
./mvnw test -Papi                       # 8 contract tests, ~1 s, no browser
./mvnw test -Pbdd                       # 8 Gherkin scenarios
./mvnw test -Pheaded                    # watch it drive a real window
./mvnw test -Pcross-browser             # Chrome + Firefox + Edge in parallel
```

The report lands in `target/atlas-report/index.html` — the same artefact
[published by CI](https://ctoscanoeng.github.io/atlas-test-framework/latest/).

### Against a Selenium Grid

```bash
docker compose -f docker/docker-compose.grid.yml up -d
./mvnw test -Pregression,grid
open http://localhost:4444/ui           # live sessions
```

### The whole pipeline, containerised

```bash
docker compose -f docker/docker-compose.ci.yml up --build --exit-code-from atlas-tests
```

---

## Architecture

```
                        ┌──────────────────────────────────────────────┐
   TestNG suites  ──┐   │            src/main/java  (the framework)    │
   Cucumber runner ─┼──▶│                                              │
                    │   │  config/    precedence chain, typed access   │
   Page objects  ───┘   │  driver/    factory · ThreadLocal · options  │
        │               │  element/   Locator · Resolver · UiElement   │
        │               │  page/      BasePage contract               │
        ▼               │  report/    Extent · screenshots · steps     │
   ┌─────────────┐      │  listener/  retry · reporting · lifecycle    │
   │ UiElement   │      │  data/      JSON · Excel · seeded fake data  │
   └──────┬──────┘      │  api/       REST Assured client · tracing    │
          │             │  sandbox/   the application under test       │
          ▼             └──────────────────────────────────────────────┘
   ┌──────────────┐                          │
   │ElementResolver│  round-robin over        │ serves
   │  + fallbacks  │  every strategy          ▼
   └──────┬────────┘              http://127.0.0.1:<ephemeral>
          │                    ┌───────────────────────────────┐
          ▼                    │  Atlas Outdoor  (SPA + API)   │
    Selenium WebDriver ───────▶│  volatile ids · async render  │
    local  ·  Grid  ·  Docker  │  shadow DOM · iframe · alerts │
                               └───────────────────────────────┘
```

**The rule the layout enforces:** `src/main/java` is a reusable library that knows nothing about
any particular application. `src/test/java` contains the page objects, the tests and the Gherkin
glue. Pointing the framework at a different product means adding page objects and an environment
overlay — never touching the core. The `e2e` profile proves it by driving a third-party store with
the same engine.

Full design rationale, decision by decision: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## Layout

```
atlas-test-framework
├── mvnw · mvnw.cmd                 Maven wrapper (script only, no jar committed)
├── pom.xml                         one file, seven execution profiles
├── Jenkinsfile                     declarative pipeline
├── .github/workflows/ci.yml        api → smoke → matrix regression → report
├── docker/
│   ├── docker-compose.grid.yml     hub + chrome ×2 + firefox + edge
│   ├── docker-compose.ci.yml       grid + suite, one command
│   └── Dockerfile                  test runner image (no browser inside)
├── docs/
│   ├── ARCHITECTURE.md             why every component exists
│   ├── COURSE-ROADMAP.md           the 12 modules mapped onto this repository
│   └── PORTFOLIO.md                how to present the project
└── src
    ├── main/java/io/atlas/qa/core   ← the framework
    └── test
        ├── java/io/atlas/qa
        │   ├── base/                BaseWebTest · BaseApiTest
        │   ├── sandbox/pages|tests  page objects and tests of the local app
        │   ├── e2e/                 the same engine on a third-party store
        │   ├── api/tests/           REST contract tests
        │   ├── bdd/                 runner · hooks · steps
        │   ├── domain/              Customer, LoginAttempt (records)
        │   └── support/             Money, data providers
        └── resources
            ├── config/              atlas.properties + per-environment overlays
            ├── suites/              six TestNG suite files
            ├── features/            Gherkin
            ├── testdata/            JSON fixtures
            ├── schemas/             JSON schema for contract validation
            └── sut-app/             the application under test
```

---

## Configuration

Four sources, weakest to strongest — so the same build runs anywhere without an edit:

```
config/atlas.properties  →  config/atlas-<env>.properties  →  ATLAS_* env vars  →  -Datlas.* flags
```

```bash
./mvnw test -Pregression -Datlas.browser=firefox -Datlas.headless=false
ATLAS_TIMEOUT_EXPLICIT=30 ATLAS_ENV=staging ./mvnw test -Pe2e
```

Secrets never live in the repository: they arrive as `ATLAS_*` environment variables, which is
exactly what Jenkins credentials and GitHub Actions secrets inject.

| Key | Default | Meaning |
|---|---|---|
| `env` | `sandbox` | `sandbox` (in-process app) or `staging` (public sites) |
| `browser` | `chrome` | chrome · firefox · edge · safari |
| `headless` | `true` | |
| `remote` / `gridUrl` | `false` | run the browsers on a Grid |
| `timeout.explicit` | `15` s | deadline for every element lookup |
| `locator.selfHealing` | `true` | try the declared fallbacks |
| `retry.count` | `1` | retries, environmental failures only |
| `data.seed` | random | fix it to replay a run with identical data |

---

## The locator engine

```java
private static final Locator SUBMIT = Locator.named("Login: sign in button")
        .by(By.id("btn-login"))                               // fastest, most brittle
        .orBy(By.cssSelector("[data-testid='login-submit']"))  // contract with the front-end team
        .orBy(By.xpath("//form[@id='login-form']//button[@type='submit']"))
        .build();
```

The resolver polls **all** strategies round-robin inside a single deadline, instead of burning the
full timeout on each one in turn. A healed lookup costs one extra `findElements` round-trip
(~10 ms) rather than 15 seconds — cheap enough to leave permanently on, which is the only way a
resilience feature survives contact with a real pipeline.

Every healing event is logged, attached to the test in the report, and summarised at the end of the
run in `target/atlas-report/locator-backlog.md`:

```
| Element                        | Times healed | Recovered with                        |
|--------------------------------|-------------:|---------------------------------------|
| Login: sign in button          |           17 | `By.cssSelector: [data-testid=…]`     |
| Cart: proceed to checkout      |            5 | `By.cssSelector: [data-testid=…]`     |
```

Self-healing without that file is a trick that lets page objects rot silently. With it, it is a
maintenance backlog.

---

## What is actually covered

| Suite | Tests | Runtime | What it proves |
|---|---:|---:|---|
| `api` | 8 | ~1 s | catalogue contract against a JSON schema, authentication, 401 / 404 / 423 |
| `smoke` | 9 | ~25 s | sign-in, catalogue, cart, purchase, framework self-checks |
| `regression` | 36 | ~40 s | the above plus validation rules, permissions, dialogs, iframe, shadow DOM, deferred rendering, second window, table sorting, file upload |
| `bdd` | 8 scenarios | ~10 s | the same journeys expressed in Gherkin, same page objects |
| `e2e` | 1 | network | the identical framework driving a third-party store |

Three of those tests are **tests of the framework itself** — they pin down the fact that the
application really does regenerate ids, that a broken primary strategy is healed and recorded, and
that a locator without fallbacks fails with a message naming the element.

---

## Running it in Eclipse

1. **File → Import → Existing Maven Projects**, select the folder, Finish.
2. Right-click any test class → **Run As → TestNG Test**. It works with no extra setup: the
   listeners are registered through `META-INF/services/org.testng.ITestNGListener`, so a test
   launched from the IDE behaves exactly like the same test launched by the pipeline.
3. To run a whole suite: right-click `src/test/resources/suites/smoke.xml` → **Run As → TestNG Suite**.
4. Override anything from **Run Configurations → Arguments → VM arguments**, e.g.
   `-Datlas.headless=false -Datlas.browser=firefox`.

Requirements: JDK 17 or newer and one of Chrome / Firefox / Edge. Drivers are resolved automatically
by Selenium Manager.

---

## Requirements

| | Needed | Notes |
|---|---|---|
| **JDK** | 17 or newer | The project compiles to release 17 for portability; it builds and runs on 21 and 26 as well. `java -version` must resolve to a JDK, not a JRE. |
| **Maven** | not required | `./mvnw` downloads and caches Apache Maven 3.9.9 on first run (`mvnw.cmd` on Windows). |
| **Browser** | one of Chrome, Firefox, Edge | Only for the UI suites. `-Papi` needs no browser at all. |
| **Driver binaries** | not required | Resolved automatically by Selenium Manager, bundled with Selenium 4. |
| **Docker** | optional | Only for the Grid and containerised pipeline topologies. |
| **Network** | optional after the first build | Once dependencies are cached, the sandbox suites run entirely offline. |

## Troubleshooting

**`./mvnw: Permission denied`** — the executable bit was lost (common when the project travels
through a zip file): `chmod +x mvnw`.

**`Unable to obtain a chrome session from the Grid at …`** — the Grid is not running:
`docker compose -f docker/docker-compose.grid.yml up -d`, then check `http://localhost:4444/ui`.
Drop `-Pgrid` to fall back to local browsers.

**The browsers on the Grid cannot reach the application under test** — they run in their own
containers, so the sandbox must listen on every interface and advertise a hostname those containers
can resolve:
`ATLAS_SANDBOX_BIND_ADDRESS=0.0.0.0 ATLAS_SANDBOX_ADVERTISED_HOST=<your-host>`.
`docker/docker-compose.ci.yml` already does this.

**`Safari does not support headless execution`** — that is the framework refusing to fail
mysteriously. Run `-Datlas.headless=false` or pick another browser.

**A test fails only in parallel** — start by running it alone with `-Datlas.threads=1
-Datlas.headless=false` and watch it. Shared state between tests is the usual cause; this framework
gives each test its own browser precisely to rule that out.

**I want to see the application myself** — pin the port and open it:
`./mvnw test -Psmoke,headed -Datlas.sandbox.port=8420`, then browse `http://127.0.0.1:8420`
while the suite runs. Credentials are listed on the sign-in page.

**Where are the results?** — `target/atlas-report/index.html` (report),
`target/atlas-report/locator-backlog.md` (locators that survived on a fallback),
`target/atlas-report/screenshots/` and `page-source/` (failure evidence),
`target/atlas-report/logs/atlas.log` (full DEBUG trace, one line per action, tagged with the test name).

## Documentation

- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — every design decision and the trade-off behind it
- **[docs/COURSE-ROADMAP.md](docs/COURSE-ROADMAP.md)** — the twelve training modules mapped onto this code, with the exercises that extend it
- **[docs/PORTFOLIO.md](docs/PORTFOLIO.md)** — how to present the project: repository, description, interview answers

---

## Licence

MIT.
