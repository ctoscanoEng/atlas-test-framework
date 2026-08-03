# Presenting this project

Everything below assumes one thing: **the repository is the argument**. A certificate proves
attendance; a repository that a stranger can clone and run in one command proves capability. Publish
them together, in that order of emphasis.

---

## 1. Repository checklist

- [x] Repository name: `atlas-test-framework` — https://github.com/ctoscanoEng/atlas-test-framework
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
>
> The angle below is the honest one: two years on a single stack build a comfort zone, and the joke
> that opens the post (`Sleep 5s`) turns into a real technical claim three paragraphs later
> (`Thread.sleep` appears nowhere in this repository). Humour that pays off in evidence is the only
> kind worth putting on a professional profile.
>
> Practical notes: LinkedIn truncates after roughly two lines, so the opening joke must land
> entirely above the "see more" fold. Put the repository link in the post rather than in the first
> comment — less reach, more credibility. Attach one screenshot, and make it the HTML report or
> `locator-backlog.md`, never the certificate: the certificate is the attachment, not the content.

### Italiano

> **Dopo due anni di Robot Framework e Python, davanti a un test instabile il mio riflesso condizionato era diventato uno solo: `Sleep    5s`.**
>
> Funzionava. Quasi sempre. Ed è esattamente il problema.
>
> Robot Framework è uno strumento serio e per certi contesti resta la scelta giusta — ma dopo due
> anni sullo stesso stack mi ero costruito una comfort zone fatta di keyword già pronte e di
> soluzioni che *sembravano* funzionare. Avevo bisogno di una rinfrescata bella forte, non di un
> ripasso.
>
> Così ho fatto un corso completo su **Selenium 4 + Java**: Java e OOP, WebDriver, TestNG, Page
> Object Model, Selenium Grid, Docker, design di framework, Cucumber BDD e CI/CD. Dodici moduli,
> zero scorciatoie.
>
> E invece di consegnare il solito progetto finale contro un sito demo pubblico, ne ho costruito uno
> mio: **ATLAS — Automated Test Layer & Assurance Suite**.
>
> **Tre problemi che volevo risolvere sul serio, non aggirare:**
>
> **1 · Le suite che diventano rosse per motivi che non sono difetti.**
> Quasi tutti i progetti da portfolio puntano a un sito demo pubblico: il giorno che quel sito
> cambia, salta tutto. ATLAS **si porta dietro la propria applicazione sotto test** — una SPA + API
> JSON servite in-process dall'HttpServer del JDK, su porta effimera. La suite gira offline, in
> aereo, su un agent CI blindato. Se la build è rossa, il problema è mio.
>
> **2 · I locator che marciscono.**
> L'applicazione rigenera *di proposito* gli id degli elementi a ogni caricamento, come fa una
> component library a ogni deploy. Ogni locator dichiara una strategia primaria e dei fallback
> ordinati; il resolver le interroga tutte dentro un'unica deadline — ~10 ms di costo, non un
> timeout da 15 secondi. La suite resta verde **e ogni elemento "guarito" finisce in un file di
> backlog di manutenzione generato a fine run.** Il self-healing senza quel file serve solo a far
> marcire i page object in silenzio.
>
> **3 · I retry che nascondono le regressioni.**
> Riprovare ogni fallimento è il modo più veloce per non fidarsi più della propria suite. ATLAS
> riprova solo i fallimenti ambientali: un'asserzione fallita è un difetto e resta rossa.
>
> **E sì: `Thread.sleep` non compare in nessuna delle 5.000 righe del progetto.** Ogni attesa è una
> condizione con una scadenza. Vecchie abitudini, sepolte come si deve.
>
> **I numeri:** 36 test su 6 thread in 37 secondi · 8 test di contratto REST con validazione JSON
> Schema in ~1 secondo · 8 scenari Gherkin che riusano *esattamente* gli stessi page object ·
> Selenium Grid + Docker Compose · pipeline GitHub Actions a stadi (api → smoke → regressione
> cross-browser) e un Jenkinsfile.
>
> **Scelte tecniche che sono pronto a difendere:** niente PageFactory/@FindBy (i proxy risolvono su
> una sola strategia e non sanno esprimere fallback) · sessione browser legata al thread via
> ThreadLocal · implicit wait a zero di proposito · importi in BigDecimal, mai double · listener
> registrati via META-INF/services, così "Run As → TestNG Test" in Eclipse si comporta identico alla
> pipeline.
>
> Tutto documentato — **compreso quello che ho deciso di NON fare e perché**, e qual è oggi il punto
> più debole del progetto.
>
> 🔗 Repository: github.com/ctoscanoEng/atlas-test-framework · 📜 Certificazione: <link>
>
> Ogni feedback da chi fa questo mestiere è più che benvenuto. Soprattutto quello scomodo.
>
> #TestAutomation #Selenium #Java #TestNG #Cucumber #QAEngineering #SDET #CICD

### English

> **After two years of Robot Framework and Python, my conditioned reflex in front of a flaky test had become exactly one line: `Sleep    5s`.**
>
> It worked. Most of the time. Which is precisely the problem.
>
> Robot Framework is a serious tool and still the right call in the right context — but after two
> years on the same stack I had built a comfort zone out of ready-made keywords and fixes that only
> *looked* like fixes. I needed a hard reset, not a refresher.
>
> So I took a full **Selenium 4 + Java** course: Java and OOP, WebDriver, TestNG, Page Object Model,
> Selenium Grid, Docker, framework design, Cucumber BDD and CI/CD. Twelve modules, no shortcuts.
>
> And instead of handing in the usual final project against a public demo site, I built my own:
> **ATLAS — Automated Test Layer & Assurance Suite**.
>
> **Three problems I wanted to actually solve, not work around:**
>
> **1 · Suites that go red for reasons that aren't defects.** Most portfolio projects target a
> public demo site; the day it changes, everything breaks. ATLAS **ships its own application under
> test** — an SPA + JSON API served in-process by the JDK HTTP server on an ephemeral port. The
> suite runs offline, on a plane, on a locked-down CI agent. A red build is always my problem.
>
> **2 · Locators that rot.** The app deliberately regenerates element ids on every load, the way a
> component library does on every deploy. Each locator declares a primary strategy and ordered
> fallbacks; the resolver polls all of them inside a single deadline — ~10 ms of overhead, not a
> 15-second timeout. The suite stays green **and every healed element lands in a maintenance backlog
> file generated at the end of the run.** Self-healing without that file is just a way to let page
> objects rot silently.
>
> **3 · Retries that hide regressions.** Retrying every failure is the fastest way to stop trusting
> your own suite. ATLAS retries environmental failures only: a failed assertion is a defect and
> stays red.
>
> **And yes — `Thread.sleep` appears nowhere in the 5,000 lines of this project.** Every wait is a
> condition with a deadline. Old habits, properly buried.
>
> **The numbers:** 36 tests on 6 threads in 37 seconds · 8 REST contract tests with JSON Schema
> validation in ~1 second · 8 Gherkin scenarios reusing *exactly* the same page objects · Selenium
> Grid + Docker Compose · a staged GitHub Actions pipeline (api → smoke → cross-browser regression)
> and a Jenkinsfile.
>
> **Technical decisions I'm ready to defend:** no PageFactory/@FindBy (proxies resolve against a
> single strategy and can't express fallbacks) · browser session bound to the thread via
> ThreadLocal · implicit waits set to zero on purpose · money in BigDecimal, never double ·
> listeners registered through META-INF/services, so "Run As → TestNG Test" in Eclipse behaves
> identically to the pipeline.
>
> All documented — **including what I deliberately left out and why**, and what the weakest part of
> the project is today.
>
> 🔗 Repository: github.com/ctoscanoEng/atlas-test-framework · 📜 Certificate: <link>
>
> Feedback from people who do this for a living is very welcome. Especially the uncomfortable kind.
>
> #TestAutomation #Selenium #Java #TestNG #Cucumber #QAEngineering #SDET #CICD

### Short variant — when sharing the certificate on its own

> Dopo due anni di Robot Framework, la mia soluzione universale ai test instabili era `Sleep 5s`.
> Ho appena finito una certificazione in Selenium 4 + Java e ho costruito un framework in cui
> `Thread.sleep` non compare nemmeno una volta.
> 36 test, 6 thread, 37 secondi, applicazione sotto test inclusa nel repository.
> 👇 github.com/ctoscanoEng/atlas-test-framework

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
