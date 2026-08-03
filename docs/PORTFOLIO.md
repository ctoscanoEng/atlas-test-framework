# Presenting this project

Everything below assumes one thing: **the repository is the argument**. A certificate proves
attendance; a repository that a stranger can clone and run in one command proves capability. Publish
them together, in that order of emphasis.

---

## 1. Repository checklist

- [ ] Repository name: `atlas-test-framework`
- [ ] Description (the one line under the title):
      *Hybrid Selenium 4 · Java 17 · TestNG · Cucumber framework with self-healing locators, parallel
      execution, Grid/Docker support and its own application under test — runs offline, one command.*
- [ ] Topics: `selenium` `selenium-webdriver` `java` `testng` `cucumber` `bdd` `test-automation`
      `page-object-model` `selenium-grid` `docker` `ci-cd` `qa-automation`
- [ ] Pinned on your GitHub profile
- [ ] `README.md` renders correctly (check the tables and the diagram on GitHub, not just locally)
- [ ] CI badge at the top once the first workflow run is green
- [ ] Commit history that tells a story: small, dated, meaningful commits — not one "initial commit"
      containing 60 files
- [ ] A tag `v1.0.0` on the first complete version

**Before pushing:** run `./mvnw test -Pregression` and make sure the report is green on a clean
clone. A recruiter who clones your repository and sees a red build has learned something about you
that no description can undo.

---

## 2. LinkedIn — certification + project

> The post that works is specific, quantified, and admits a trade-off. The one that does not work
> is a list of buzzwords and a certificate image.

### English

> **I finished my Selenium automation certification — and instead of the course project, I built my own framework.**
>
> The course covered Java, Selenium WebDriver, TestNG, Cucumber, Grid, Docker and CI/CD. Rather than
> hand in another suite of tests against a public demo site, I built **ATLAS**: a hybrid automation
> framework that ships with its own application under test.
>
> Three problems I wanted to solve properly:
>
> **1 · Suites that break for reasons that are not defects.** Most portfolio projects target a public
> demo site. The day it changes, everything goes red. ATLAS serves its own SPA + JSON API in-process
> on an ephemeral port — the whole suite runs offline and deterministically, so a red build always
> means a real problem.
>
> **2 · Locators that rot.** The application deliberately regenerates element ids at every load, the
> way a component library does on each deploy. Locators declare a primary strategy and ordered
> fallbacks; the resolver polls all of them inside a single deadline (~10 ms overhead, not a 15-second
> timeout), keeps the suite green, and writes every healed element to a maintenance backlog file.
> Self-healing without that backlog is just a way to let page objects rot silently.
>
> **3 · Retries that hide regressions.** Retrying every failure is how teams stop trusting their
> suite. ATLAS retries only environmental failures — a failed assertion is a defect and stays red.
>
> Also in the box: 36 tests running on 6 threads (~40 s), a REST layer with JSON-schema contract
> validation, Gherkin scenarios sharing the exact same page objects, Selenium Grid + Docker Compose,
> a GitHub Actions pipeline staged api → smoke → cross-browser regression, and a Jenkinsfile.
>
> Everything is documented — including what I deliberately left out and why.
>
> Repository: <link> · Certificate: <link>
>
> Feedback from people who do this for a living is very welcome.
>
> #TestAutomation #Selenium #Java #TestNG #Cucumber #QA #SDET #CICD

### Italiano

> **Ho completato la certificazione in Selenium automation — e invece del progetto del corso ho costruito un framework mio.**
>
> Il corso copriva Java, Selenium WebDriver, TestNG, Cucumber, Grid, Docker e CI/CD. Invece di
> consegnare l'ennesima suite contro un sito demo pubblico, ho costruito **ATLAS**: un framework di
> automazione ibrido che si porta dietro la propria applicazione sotto test.
>
> Tre problemi che volevo risolvere sul serio:
>
> **1 · Suite che diventano rosse per motivi che non sono difetti.** Quasi tutti i progetti da
> portfolio puntano a un sito demo pubblico: il giorno che cambia, salta tutto. ATLAS serve la
> propria SPA + API JSON in-process su porta effimera — la suite gira offline e in modo
> deterministico, quindi una build rossa è sempre un problema vero.
>
> **2 · Locator che marciscono.** L'applicazione rigenera di proposito gli id degli elementi a ogni
> caricamento, come fa una component library a ogni deploy. Ogni locator dichiara una strategia
> primaria e dei fallback ordinati; il resolver le interroga tutte dentro un'unica deadline (~10 ms
> di costo, non un timeout da 15 secondi), tiene verde la suite e scrive ogni elemento "guarito" in
> un file di backlog di manutenzione. Il self-healing senza quel backlog serve solo a far marcire i
> page object in silenzio.
>
> **3 · Retry che nascondono le regressioni.** Riprovare ogni fallimento è il modo migliore per non
> fidarsi più della propria suite. ATLAS riprova solo i fallimenti ambientali: un'asserzione fallita
> è un difetto e resta rossa.
>
> Nel pacchetto anche: 36 test su 6 thread (~40 s), un layer REST con validazione di contratto via
> JSON schema, scenari Gherkin che riusano esattamente gli stessi page object, Selenium Grid +
> Docker Compose, pipeline GitHub Actions a stadi api → smoke → regressione cross-browser, e un
> Jenkinsfile.
>
> Tutto documentato — comprese le cose che ho deciso di non fare, e il perché.
>
> Repository: <link> · Certificato: <link>
>
> Ogni feedback da chi fa questo lavoro è benvenuto.
>
> #TestAutomation #Selenium #Java #TestNG #Cucumber #QA #SDET #CICD

**Posting notes:** put the repository link in the first comment if you want reach, in the post if you
want credibility — pick credibility. Attach one screenshot: the HTML report, not the certificate.

---

## 3. CV / résumé

**Project entry**

> **ATLAS — Test Automation Framework** · Java 17, Selenium 4, TestNG, Cucumber, Docker
> Hybrid UI + API automation framework with a self-healing locator engine, thread-safe parallel
> execution (6 threads, 36 tests in ~40 s), Selenium Grid and Docker Compose topologies, and a
> self-hosted application under test that makes the suite fully offline and deterministic.
> Staged CI on GitHub Actions and Jenkins; HTML reporting with failure evidence and an automatically
> generated locator maintenance backlog.

**Bullets you can lift into a job description**

- Designed a multi-strategy locator resolver that keeps suites green through front-end id churn
  while recording every healed element as tracked technical debt
- Built a thread-safe driver lifecycle (`ThreadLocal` session per test) enabling parallel execution
  without cross-test interference
- Defined a retry policy that distinguishes environmental failures from assertion failures, so real
  regressions cannot hide behind a green build
- Containerised the full execution topology (Selenium Grid + suite) so a machine with only Docker
  reproduces the pipeline exactly

---

## 4. The interview questions this project invites

Prepare these. They *will* be asked, and the answers are already in the code.

**"Why not `PageFactory` and `@FindBy`?"**
`PageFactory` proxies resolve against a single strategy and cannot express fallbacks, and the lazy
proxy hides *when* the lookup happens. `BasePage.$(Locator)` makes the moment of resolution explicit
and supports the fallback chain. See `docs/ARCHITECTURE.md` §6.

**"Isn't self-healing dangerous? You could be testing the wrong element."**
Yes, if it is silent. That is why every fallback is declared explicitly by a human (never guessed),
every healing event is logged and attached to the test, and the run ends with a backlog file listing
what must be repaired. It buys time to fix locators; it does not replace fixing them.

**"Why one browser per test? Isn't that slow?"**
About a second per test, and it buys complete isolation. The alternative fails in an
order-dependent way that costs days to debug. If the second matters, the answer is more threads or a
Grid, not shared state.

**"How do you keep a parallel suite from becoming flaky?"**
Session bound to the thread, zero implicit waits, no `Thread.sleep` anywhere, every wait is a
condition with a deadline, immutable shared configuration, fresh data per test from a seeded
generator, and assertions on observable side effects rather than on the click.

**"What would you add next, with two more weeks?"**
API-accelerated preconditions (sign in through REST and inject the session — roughly 2 s saved per
UI test), visual regression with a baseline store, and a flakiness budget job that runs the
regression suite twenty times and treats any single failure as a defect in the test.

**"What is the weakest part of this framework?"**
Answer honestly — it is the strongest signal you can send. Candidates: the sandbox application is
simpler than a real product; there is no database layer; the healing backlog is generated but not yet
enforced by the pipeline (nothing fails when it grows). Have the fix in mind for each.

---

## 5. What not to do

- Do not claim the framework "eliminates flaky tests". It reduces specific, named causes.
- Do not pad the numbers. 36 tests that run reliably beat "200+ test cases" nobody can verify.
- Do not post the certificate alone. Certificates are common; a running framework is not.
- Do not describe features. Describe **problems and trade-offs** — that is the difference between an
  automation tester and an automation engineer.
