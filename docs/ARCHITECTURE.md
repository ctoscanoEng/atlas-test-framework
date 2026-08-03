# Architecture

Every section below answers the same two questions: *what does this component do* and *what
would go wrong without it*. A framework is a collection of decisions; undocumented decisions get
reverted by the next person who touches the code.

---

## 1. The boundary: `src/main` is a library, `src/test` is a product

`src/main/java/io/atlas/qa/core` compiles without knowing that "Atlas Outdoor" exists. It has no
page object, no product name, no URL. `src/test/java` holds everything specific to an application:
page objects, tests, Gherkin glue, domain records.

Why it matters: the honest test of "is this a framework or a pile of helpers?" is whether it can
drive a second application without being modified. The `e2e` profile does exactly that against a
third-party store, reusing the same `BasePage`, resolver, reporting and retry policy, with nothing
changed but a properties overlay.

---

## 2. Configuration: one precedence chain, no code changes

```
config/atlas.properties          committed defaults
config/atlas-<env>.properties    per-environment overlay
ATLAS_* environment variables    what CI and Docker inject
-Datlas.* system properties      what a developer overrides for one run
```

`ConfigLoader` merges them once into an immutable map — safe to read from every parallel thread
with no synchronisation — and `AtlasConfig` exposes it as typed accessors (`Duration`,
`BrowserType`, `boolean`). Test code never touches a raw string key, so renaming one is a one-line
change in a single file.

**Trade-off accepted:** the map is resolved at class-load time, so a test cannot mutate the
configuration halfway through a run. That is deliberate — configuration that changes under a
running suite makes failures irreproducible.

---

## 3. The application under test lives inside the framework

`SandboxServer` starts a JDK `HttpServer` (zero dependencies) on an **ephemeral port**, serving a
small SPA and a JSON API from the classpath.

What this buys:

- **Determinism** — a red build is always a defect in this repository, never someone else's outage.
- **Offline execution** — the suite runs on a plane and on a locked-down CI agent.
- **No port collisions** — the OS assigns the port, so several suites run concurrently on one agent.
- **A controlled adversary** — the app *deliberately* regenerates element ids, renders the
  catalogue after a variable delay, and exposes a shadow-DOM component. Those are not obstacles
  invented for a demo: they are the three things that actually break real suites.

The bind address and the advertised host are separate settings, because when the browsers run in
other containers the server must listen on `0.0.0.0` while handing those browsers a hostname they
can resolve.

---

## 4. Driver lifecycle: one browser per thread, one browser per test

```java
private static final ThreadLocal<WebDriver> SESSION = new ThreadLocal<>();
```

A single static `WebDriver` would have eight parallel threads driving one browser — the classic
suite that "only fails when parallel is on". `quitSession()` always calls `remove()` in a `finally`,
so a pooled thread never inherits a dead session.

A fresh browser per test method costs about a second and buys total isolation: no cookie, no
storage entry, no leftover navigation can leak forward. Suites that share a browser between tests
are faster until the day they fail in an order-dependent way nobody can debug.

`DriverFactory` decides local vs. Grid from configuration; the test code cannot tell the difference.
Driver binaries come from **Selenium Manager**, so nothing is installed on the agent and no
`chromedriver` is committed.

**Implicit waits are set to zero on purpose.** Mixing implicit and explicit waits produces timeouts
nobody can predict; all synchronisation goes through `Waits` and the resolver.

---

## 5. Locators: named, multi-strategy, audited

A `By` is anonymous. When it stops matching, the report shows `By.cssSelector: #a > div:nth-child(3)`
and nobody knows what it was meant to be. A `Locator` carries a description and an ordered list of
strategies.

`ElementResolver` polls every strategy round-robin within one deadline, rather than spending the
full timeout on each in turn:

```
while (now < deadline)
    for each strategy      // primary first
        if findElements(strategy) is not empty -> return, and record a healing event if not primary
    sleep(pollingInterval)
```

Cost of a healed lookup: one extra round-trip, ~10 ms. Cost of the naive implementation: one full
timeout per dead strategy. The difference is what makes the feature permanently affordable.

`HealingLedger` keeps a per-thread view (attached to the test in the report) and a per-run view
(written to `locator-backlog.md`). **Self-healing without accounting is a way to let page objects
rot silently**; with the backlog it becomes a maintenance queue.

---

## 6. `UiElement`: lazy, self-defending, honest

It holds a `Locator`, never a `WebElement`, and re-resolves at every interaction — which structurally
removes `StaleElementReferenceException` from single-page applications that re-render on each state
change. Staleness during an interaction is retried three times; anything else propagates
immediately, because swallowing real failures is how a framework starts hiding bugs.

Clicks escalate: native → scroll into view and retry → scripted click. The last step is **reported
as a warning**, since a test that needs JavaScript to click is testing something a user could not do.

Values typed into a field whose description mentions *password*, *secret*, *token* or *card* are
masked before they reach a log or a report.

---

## 7. Retry policy: an opinion, not a switch

`RetryAnalyzer` re-runs a test **only** when the failure is environmental — `WebDriverException`,
`SocketException`, `IOException`. An `AssertionError` is never retried.

Blindly retrying everything is how teams stop trusting their suite: a genuine regression that fails
one run in two becomes invisible. Retries are capped, always written to the report, and
`@NoRetry` opts a test out entirely (used for permission tests, where a second attempt could only
mask a real defect).

`RetryTransformer` attaches the policy to every test at runtime. Writing
`@Test(retryAnalyzer = …)` on four hundred methods is duplication waiting to drift.

---

## 8. Listener registration: `META-INF/services`

TestNG discovers the three listeners through the `ServiceLoader`. Two reasons:

1. `IAnnotationTransformer` must be registered *before* annotations are read, which `@Listeners`
   cannot do.
2. Declaring listeners in every suite file duplicates the policy and silently breaks IDE runs.

Consequence: **Run As → TestNG Test** in Eclipse behaves exactly like the pipeline. A framework whose
behaviour depends on how it was launched is a framework people stop trusting.

---

## 9. Reporting: the report answers the questions people actually ask

The header records how the run was configured (environment, browser, headless, Grid, healing,
retry policy, JVM, OS). Each test carries its steps, its healing events and, on failure, a
screenshot embedded as base64 (single portable file), a PNG on disk and the page source — because
sometimes the screenshot looks fine and the DOM tells the story.

`ReportManager` is a no-op when no report is bound to the thread, so page objects remain usable
outside a TestNG run. A page object that only works inside a report is not reusable.

Log lines carry `%X{testName}` from Log4j2's `ThreadContext`: a log file produced by eight parallel
browsers is unreadable otherwise.

---

## 10. BDD reuses the product, it is not a second framework

The Cucumber runner exposes one TestNG method for all scenarios, which would file a hundred report
entries called `runScenario`. It implements the `SelfReporting` marker, the TestNG listener stands
back, and the hooks report each scenario under its real business name.

Step definitions contain no locators and no waits — only delegation to page objects. A step
definition holding a locator is a page object in disguise, and the day a second feature needs the
same interaction it gets copied instead of reused.

Scenario state is injected by PicoContainer (`ScenarioContext`, one instance per scenario), so
scenarios run in parallel without static state.

---

## 11. Test data: typed, and reproducible by seed

JSON fixtures deserialise into `record` types, so a test signature reads `checkout(Customer)` rather
than four positional strings, and a change in the fixture fails at compile time.

`FakeData` seeds the generator from configuration and prints the seed. Random data finds bugs that
fixed fixtures never will — and makes failures irreproducible, which is why most teams abandon it.
Re-running with `-Datlas.data.seed=<value>` replays the exact data set.

Excel is supported because that is the format business analysts actually send, not because it is a
good format for test data.

---

## 12. Money is never a `double`

`0.1 + 0.2` is not `0.3` in binary floating point. A checkout test comparing totals as `double`
fails once a month on a rounding artefact, and a suite that fails for reasons nobody can explain
stops being trusted long before it stops being run. `Money` parses rendered amounts into
`BigDecimal` with an explicit separator heuristic.

---

## 13. What was deliberately left out

- **A page-object generator.** Generated page objects encode the current DOM, which is the part that
  changes; the value of a page object is the business vocabulary a human chooses for it.
- **A database layer.** Assertions against the database bypass the contract the application
  actually exposes, and they rot the moment the schema changes.
- **A screenshot-diff visual layer.** Worth having, but it needs a baseline store and a review
  workflow; bolting it on without those produces noise, not signal.
- **Retries at the step level.** They hide the fact that a step is not idempotent.

Each of these is a reasonable next increment — see the roadmap — but shipping them half-done would
have cost more than not shipping them.
