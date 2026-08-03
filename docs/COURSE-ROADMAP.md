# Course roadmap — the twelve modules, mapped onto this repository

This project is built so that every module of a Selenium/Java automation curriculum has a place to
land in it. For each module: **where it already lives here**, and **what to build next** so the
repository grows with the course instead of being rewritten at the end of it.

Work through it in order. Commit each exercise separately — a commit history that shows a framework
growing is worth more in an interview than a repository that appears fully formed in one push.

---

## Module 1 — Automation & Selenium overview

**In this repository:** [`README.md`](../README.md) and
[`docs/ARCHITECTURE.md`](ARCHITECTURE.md) — the "why" of every component; the comparison table in
the README is the answer to *"why automate this at all?"*.

**Exercise:** write `docs/TEST-STRATEGY.md` — which layer tests what (API vs. UI), what is
deliberately *not* automated, and the cost of a false positive. Two pages, no diagrams needed.

---

## Module 2 — Java for Selenium

**In this repository:**
- collections and streams — `InventoryPage.prices()`, `Waits`, `HealingLedger`
- text blocks and `String.formatted` — every multi-line message
- `Optional` instead of `null` — `ElementResolver.tryResolve`
- try-with-resources — `JsonDataReader`, `ExcelDataReader`, `SandboxServer`
- checked vs. unchecked exceptions — `AtlasException` hierarchy
- functional interfaces — `UiElement.execute(String, Function<WebElement, T>)`

**Exercise:** add `core/util/Retry.java`, a generic `Retry.times(3).ignoring(IOException.class).run(supplier)`
helper, and use it in `ApiClient` for a flaky endpoint. Pure Java, no Selenium — that is the point.

---

## Module 3 — OOP

**In this repository:**
- **Strategy** — `BrowserOptionsProvider` + `OptionsRegistry` (adding a browser modifies no existing class)
- **Factory** — `DriverFactory`
- **Singleton, correctly done** — `SandboxServer` (double-checked locking, `volatile`)
- **Builder** — `Locator.Builder`
- **Template method** — `BasePage.waitUntilLoaded()` calling the abstract `pageMarker()`
- **Marker interface** — `SelfReporting`
- **Records and enums as domain types** — `Customer`, `BrowserType`, `TargetEnvironment`
- **Encapsulation with teeth** — page objects never return `WebElement` to a test

**Exercise:** implement `SafariOptionsProvider` fully, then add a `BrowserStackOptionsProvider` that
reads capabilities from configuration. If you have to modify any existing class to do it, the
registry is not doing its job — fix the registry, not the provider.

---

## Module 4 — Selenium WebDriver

**In this repository:** `core/driver`, `core/element`, and the whole `sandbox/pages` package.
Already covered: relative and absolute locators, dropdowns (`Select`), `Actions`, JavaScript
execution, native dialogs, iframes, **shadow DOM**, multiple windows, file upload,
`getDomAttribute` vs. `getDomProperty`, and the `EventFiringDecorator` listener.

**Exercises, in increasing order of difficulty:**
1. Add drag-and-drop to the playground page and drive it with `Actions`.
2. Add a `core/element/RelativeLocators.java` helper wrapping `RelativeLocator.with(...).below(...)`.
3. Intercept network traffic with **Selenium CDP / BiDi** and fail a test when the page logs a
   JavaScript error — `core/driver/ConsoleErrorCollector.java`.

---

## Module 5 — TestNG

**In this repository:** `base/BaseWebTest`, `support/DataProviders`, the six suite files under
`src/test/resources/suites/`, groups, `@Parameters` for cross-browser runs, parallel data providers,
`IRetryAnalyzer`, `IAnnotationTransformer`, `ITestListener`, `ISuiteListener` and `ServiceLoader`
registration.

**Exercises:**
1. Add `dependsOnMethods` where a real precondition exists, and observe how skips are reported.
2. Add an `IMethodInterceptor` that runs the fastest tests first (read the previous run's durations
   from `target/surefire-reports`). Fast feedback is a feature.
3. Add a `soft assertion` helper so one test can report three failed expectations in one run.

---

## Module 6 — Page Object Model

**In this repository:** `core/page/BasePage` plus seven page objects. Note what is *not* there:
no `PageFactory` / `@FindBy`. That is a decision, not an omission — `PageFactory` proxies resolve
eagerly against a single strategy and cannot express fallbacks. Be ready to defend it; that exact
question comes up in interviews.

**Exercises:**
1. Extract the header into a **component object** (`components/HeaderComponent.java`) shared by
   every page instead of being redeclared.
2. Introduce a `Component` base class scoped to a parent `SearchContext` and use
   `ElementResolver.resolveWithin`.
3. Rewrite one page with `PageFactory` on a branch, then write two paragraphs comparing them.

---

## Module 7 — Selenium Grid

**In this repository:** `docker/docker-compose.grid.yml`, the `grid` profile,
`DriverFactory.createRemote` with `LocalFileDetector`.

**Exercises:**
1. Run `-Pcross-browser,grid` and watch `http://localhost:4444/ui` while the queue drains.
2. Scale to six Chrome nodes and find the thread count where the machine, not the Grid, becomes
   the bottleneck. Write the numbers down — capacity data is what makes a QA engineer credible.
3. Switch the Grid to **dynamic mode** (nodes started per session) and compare startup cost.

---

## Module 8 — Docker

**In this repository:** `docker/Dockerfile` (layered dependency cache, no browser in the image) and
`docker/docker-compose.ci.yml` (grid + suite in one command).

**Exercises:**
1. Add video recording (`selenium/video`) to the compose file and attach the recording to failures.
2. Publish the image to a registry and run the suite from it on a machine with no JDK installed.
3. Measure the image size before and after the multi-stage build, and explain the difference.

---

## Module 9 — Automation frameworks

**In this repository:** this is the whole repository. The hybrid model is explicit: data-driven
(JSON/Excel providers), keyword-ish (`UiElement`'s fluent vocabulary) and BDD, sharing one core.

**Exercise:** write `docs/FRAMEWORK-COMPARISON.md` — linear script vs. modular vs. data-driven vs.
keyword vs. hybrid vs. BDD, with a paragraph on which one this project is and *why the others were
rejected*. Interviewers ask this in the first ten minutes.

---

## Module 10 — Hybrid framework with POM (the project)

**In this repository:** already delivered end to end. This is the artefact to show.

**Exercises to reach production quality:**
1. **Coverage** — add scenarios for stock decrement, quantity edits and an order history page
   (you will have to extend the sandbox app: that is a feature, you own the SUT).
2. **API-accelerated preconditions** — sign in through `ApiClient`, inject the session into the
   browser via `localStorage`, and cut ~2 s from every UI test. Measure before and after.
3. **Flakiness budget** — run the regression suite twenty times in a loop, and treat any test that
   fails once as a defect in the test.

---

## Module 11 — Cucumber BDD

**In this repository:** `bdd/runner`, `bdd/support` (hooks + PicoContainer context), `bdd/steps`,
two feature files, tag filtering through `-Dcucumber.tags`.

**Exercises:**
1. Add a `Scenario Outline` driven by a **data table** and a custom `DataTableType` mapping rows
   straight onto `Customer`.
2. Add a `ParameterType` so `"{customer}"` in a step resolves to a record.
3. Generate the living documentation (`cucumber.json` → HTML) and publish it with the report.

---

## Module 12 — CI/CD (Git, GitHub, Jenkins)

**In this repository:** `.github/workflows/ci.yml` (staged: api → smoke → matrix regression → BDD →
publish) and `Jenkinsfile` (parameterised, HTML publisher, artefact archiving, Grid lifecycle).

**Exercises:**
1. Turn on branch protection so `main` requires the smoke job to pass.
2. Publish the report to GitHub Pages and put the badge in the README.
3. Add a nightly job running `-Pe2e` against the public store, and let it fail *without* blocking
   the merge queue — then explain in the README why those two things are separate.
4. Write `docs/GIT-WORKFLOW.md`: branch naming, commit message convention, what a reviewer looks at.

---

## Suggested order of work

| Week | Focus | Deliverable |
|---|---|---|
| 1 | Modules 1–3 | `TEST-STRATEGY.md`, `Retry.java`, a new options provider |
| 2 | Modules 4–5 | drag & drop, relative locators, method interceptor |
| 3 | Module 6 | header component object, `Component` base class |
| 4 | Modules 7–8 | Grid capacity numbers, video recording |
| 5 | Modules 9–10 | coverage extension, API-accelerated login, flakiness budget |
| 6 | Modules 11–12 | data tables, GitHub Pages report, branch protection |

At the end of six weeks the repository contains roughly eighty tests, three execution topologies and
a documented rationale for every decision. That is what "portfolio project" is supposed to mean.
