# ATLAS — Guida completa al progetto

> Documento in italiano, pensato per essere letto **da zero**: non dà per scontato che tu conosca
> il progetto, il framework o le scelte fatte. Spiega cosa fa ogni pezzo, perché esiste, e cosa
> succederebbe se non ci fosse.
>
> La documentazione in inglese (`README.md`, `docs/ARCHITECTURE.md`) è quella rivolta a chi legge il
> repository da fuori. Questa è la versione lunga, per te.

**Indice**

1. [Cos'è ATLAS in una pagina](#1-cosè-atlas-in-una-pagina)
2. [Il problema che risolve](#2-il-problema-che-risolve)
3. [Come si esegue](#3-come-si-esegue)
4. [Anatomia del repository](#4-anatomia-del-repository)
5. [Il SUT: l'applicazione sotto test](#5-il-sut-lapplicazione-sotto-test)
6. [Il core del framework, componente per componente](#6-il-core-del-framework-componente-per-componente)
7. [Il livello dei test](#7-il-livello-dei-test)
8. [Il livello BDD](#8-il-livello-bdd)
9. [Suite TestNG e profili Maven](#9-suite-testng-e-profili-maven)
10. [Parallelismo e thread-safety](#10-parallelismo-e-thread-safety)
11. [Reporting: come si legge un run](#11-reporting-come-si-legge-un-run)
12. [Selenium Grid e Docker](#12-selenium-grid-e-docker)
13. [Le pipeline: com'è strutturata la CI](#13-le-pipeline-comè-strutturata-la-ci)
14. [Glossario](#14-glossario)
15. [Domande frequenti e problemi noti](#15-domande-frequenti-e-problemi-noti)

---

## 1. Cos'è ATLAS in una pagina

ATLAS (*Automated Test Layer & Assurance Suite*) è un **framework di automazione dei test** scritto
in Java 17, basato su Selenium 4, TestNG e Cucumber.

"Framework" e non "suite di test" significa una cosa precisa: il codice è diviso in due parti che
non si mescolano mai.

| | Cosa contiene | Dove sta | Sa cosa testa? |
|---|---|---|---|
| **Il framework** | driver, locator, attese, report, retry, lettura configurazione, client REST | `src/main/java/io/atlas/qa/core` | **No.** Non conosce nessuna applicazione |
| **Il prodotto** | page object, test, scenari Gherkin, dati di test | `src/test/java` + `src/test/resources` | Sì, conosce "Atlas Outdoor" |

Questa separazione è la differenza tra un framework e una raccolta di script: il core può essere
impacchettato come libreria e riusato su un altro prodotto senza toccarne una riga. Nel repository
c'è la prova: il profilo `e2e` punta lo stesso identico core su un e-commerce di terze parti
(SauceDemo) cambiando solo un file di properties.

La particolarità più insolita: **il framework si porta dietro l'applicazione da testare**. Dentro
`src/test/resources/sut-app` c'è una piccola web app (HTML + JavaScript + API JSON) che viene servita
da un web server avviato dal framework stesso nel processo dei test. Non serve rete, non serve
Docker, non serve un sito demo pubblico.

**Numeri reali** (misurati, non stimati):

| Suite | Test | Tempo | Browser |
|---|---:|---:|---|
| `api` | 8 | ~1 s | nessuno |
| `smoke` | 9 | ~11 s | 1, headless |
| `regression` | 36 | ~37 s | 6 in parallelo |
| `bdd` | 8 scenari | ~9 s | 4 in parallelo |

---

## 2. Il problema che risolve

Un progetto Selenium da portfolio, di solito, è fatto così: si punta un sito demo pubblico
(SauceDemo, The Internet, OrangeHRM), si scrivono venti test con `By.id`, si mette un
`Thread.sleep(3000)` dove le cose non funzionano, e si consegna.

Quel progetto ha quattro problemi che diventano visibili solo dopo:

**Problema 1 — dipende da un server di qualcun altro.**
Il giorno in cui il sito demo è lento, va offline, cambia layout o introduce un rate limit, la suite
diventa rossa. Chi la guarda non sa distinguere "il codice ha un bug" da "internet ha avuto una
brutta giornata". Una suite che mente sul motivo per cui è rossa smette di essere usata.

> **Soluzione in ATLAS:** il SUT è dentro il repository e viene servito in-process
> ([sezione 5](#5-il-sut-lapplicazione-sotto-test)). Se la build è rossa, il difetto è nel codice.

**Problema 2 — i locator marciscono.**
Le applicazioni moderne sono costruite con librerie di componenti che rigenerano id e classi CSS a
ogni build. Un locator scritto su `#submit-button-3f7a` funziona finché il front-end non fa un
deploy. Poi duecento test si rompono insieme, e il team passa un giorno a riscriverli.

> **Soluzione in ATLAS:** ogni locator dichiara una strategia primaria **e dei fallback ordinati**.
> Il resolver le prova tutte dentro un'unica deadline e registra ogni "guarigione" in un file di
> backlog ([sezione 6.3](#63-element--il-cuore-del-framework)).

**Problema 3 — le attese sbagliate.**
`Thread.sleep(3000)` è lento quando l'applicazione risponde in 200 ms, ed è insufficiente quando ne
impiega 3.100. È l'unica riga di codice che riesce a essere contemporaneamente troppo lenta e troppo
veloce.

> **Soluzione in ATLAS:** `Thread.sleep` non compare in nessun punto del progetto. Ogni attesa è una
> condizione con una scadenza ([sezione 6.3.4](#634-waits--sincronizzazione)).

**Problema 4 — i retry che nascondono i bug.**
Riprovare automaticamente ogni test fallito fa diventare verde la pipeline. Fa anche sparire le
regressioni vere che si manifestano una volta su due.

> **Soluzione in ATLAS:** si riprovano **solo** i fallimenti ambientali (sessione persa, rete,
> nodo del Grid caduto). Un'asserzione fallita è un difetto e resta rossa
> ([sezione 6.6](#66-listener--le-politiche-di-esecuzione)).

---

## 3. Come si esegue

### 3.1 Prerequisiti

| | Serve? | Nota |
|---|---|---|
| **JDK 17 o superiore** | **sì** | Deve essere un JDK, non un JRE. Verifica con `java -version`. Il progetto compila a *release 17* per portabilità, ma gira su 21 e 26 senza modifiche |
| **Maven** | no | Il repository include il *wrapper*: `./mvnw` scarica e mette in cache Maven 3.9.9 al primo avvio |
| **Un browser** | solo per i test UI | Chrome, Firefox o Edge. Il profilo `-Papi` non ne usa nessuno |
| **Driver (chromedriver…)** | no | Li risolve **Selenium Manager**, incluso in Selenium 4 |
| **Docker** | opzionale | Solo per Selenium Grid e per la pipeline containerizzata |
| **Rete** | solo al primo build | Serve a scaricare le dipendenze. Dopo, le suite `sandbox` girano completamente offline |

### 3.2 Primo avvio

```bash
git clone https://github.com/ctoscanoEng/atlas-test-framework.git
cd atlas-test-framework
./mvnw test -Psmoke
```

Il primo avvio impiega qualche minuto perché scarica Maven e le dipendenze. Dal secondo in poi,
`-Psmoke` finisce in circa 11 secondi.

Cosa vedi scorrere a schermo, in ordine:

```
INFO  [] Configuration resolved for environment 'sandbox' (22 keys)
INFO  [] Sandbox application under test started on http://127.0.0.1:54321
INFO  [] HTML report will be written to …/target/atlas-report/index.html
INFO  [standardUserSignsIn] ──────── START standardUserSignsIn ────────
INFO  [standardUserSignsIn] Session started | browser=chrome | headless=true | remote=false
WARN  [standardUserSignsIn] [HEALED] Login: sign in button — primary strategy failed …
INFO  [standardUserSignsIn] ──────── PASS  standardUserSignsIn (2841 ms) ────────
…
WARN  [] Locator health: 4 element(s) required a fallback during this run
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Le righe `[HEALED]` **non sono errori**: sono il motore di self-healing che fa il suo lavoro e lo
dichiara. Vedi la [sezione 5.4](#54-i-tre-ostacoli-progettati-apposta).

### 3.3 Tutti i comandi

```bash
# --- suite ---
./mvnw test -Papi              # 8 test di contratto REST, ~1 s, nessun browser
./mvnw test -Psmoke            # 9 test, il gate minimo
./mvnw test -Pregression       # 36 test su 6 thread, UI + API
./mvnw test -Pbdd              # 8 scenari Gherkin
./mvnw test -Pcross-browser    # gli stessi test su Chrome, Firefox ed Edge in parallelo
./mvnw test -Pe2e              # il framework puntato su SauceDemo (richiede rete)

# --- modificatori (si combinano con la virgola) ---
./mvnw test -Psmoke,headed     # browser visibile, un thread solo
./mvnw test -Pregression,grid  # browser sul Selenium Grid invece che locali

# --- override puntuali ---
./mvnw test -Psmoke -Datlas.browser=firefox
./mvnw test -Psmoke -Datlas.headless=false -Datlas.timeout.explicit=30
./mvnw test -Psmoke -Datlas.locator.selfHealing=false   # per vedere la suite rompersi

# --- un solo test ---
./mvnw test -Dtest=CheckoutJourneyTests
./mvnw test -Dtest=CheckoutJourneyTests#customerCompletesAPurchase
```

### 3.4 Dove finiscono i risultati

```
target/atlas-report/
├── index.html              ← il report HTML (aprilo nel browser)
├── locator-backlog.md      ← i locator sopravvissuti grazie a un fallback
├── screenshots/            ← PNG catturati al momento del fallimento
├── page-source/            ← l'HTML della pagina al momento del fallimento
├── logs/atlas.log          ← traccia DEBUG completa, una riga per azione
├── cucumber.html           ← report nativo Cucumber (solo con -Pbdd)
└── cucumber.json           ← output macchina per living documentation
```

### 3.5 Vedere l'applicazione con i tuoi occhi

Il SUT gira su una porta casuale, quindi normalmente non sai dove sia. Puoi fissarla:

```bash
./mvnw test -Psmoke,headed -Datlas.sandbox.port=8420
```

Mentre la suite gira, apri `http://127.0.0.1:8420` nel tuo browser. Le credenziali sono scritte
nella pagina di login.

---

## 4. Anatomia del repository

```
atlas-test-framework
│
├── mvnw, mvnw.cmd, .mvn/         Maven wrapper: nessuna installazione richiesta
├── pom.xml                       dipendenze + 8 profili di esecuzione
├── Jenkinsfile                   pipeline dichiarativa per Jenkins
├── LICENSE                       MIT
│
├── .github/workflows/ci.yml      pipeline GitHub Actions
│
├── docker/
│   ├── docker-compose.grid.yml   Selenium Grid: hub + 2 Chrome + Firefox + Edge
│   ├── docker-compose.ci.yml     Grid + suite: l'intera pipeline in un comando
│   └── Dockerfile                immagine del test runner (senza browser dentro)
│
├── docs/
│   ├── ARCHITECTURE.md           le decisioni di design, in inglese
│   ├── COURSE-ROADMAP.md         i 12 moduli del corso mappati sul codice
│   ├── PORTFOLIO.md              come presentare il progetto
│   └── it/
│       ├── GUIDA-COMPLETA.md     questo documento
│       └── JENKINS.md            guida passo-passo a Jenkins
│
└── src
    ├── main/java/io/atlas/qa/core/      ═══ IL FRAMEWORK ═══
    │   ├── config/      lettura configurazione con catena di precedenza
    │   ├── driver/      ciclo di vita del browser + capabilities per browser
    │   ├── element/     Locator, resolver con fallback, wrapper fluente, attese
    │   ├── page/        contratto comune a tutti i page object
    │   ├── report/      report HTML, screenshot, page source
    │   ├── listener/    retry, reporting, ciclo di vita della suite
    │   ├── data/        lettori JSON/Excel, generatore di dati con seed
    │   ├── api/         client REST + tracciamento delle chiamate
    │   ├── sandbox/     il web server che ospita l'applicazione sotto test
    │   └── exception/   gerarchia di eccezioni del framework
    │
    └── test
        ├── java/io/atlas/qa/            ═══ IL PRODOTTO ═══
        │   ├── base/         BaseWebTest, BaseApiTest
        │   ├── sandbox/
        │   │   ├── pages/    7 page object dell'app locale
        │   │   └── tests/    5 classi di test
        │   ├── e2e/          page object + test di SauceDemo
        │   ├── api/tests/    test di contratto REST
        │   ├── bdd/          runner, hook, step definition
        │   ├── domain/       Customer, LoginAttempt (record)
        │   └── support/      Money, DataProviders
        │
        └── resources/
            ├── config/       atlas.properties + overlay per ambiente
            ├── suites/       6 file suite TestNG
            ├── features/     2 file Gherkin
            ├── testdata/     fixture JSON
            ├── schemas/      JSON Schema per i test di contratto
            ├── sut-app/      ═══ L'APPLICAZIONE SOTTO TEST ═══
            ├── log4j2.xml    configurazione dei log
            └── META-INF/services/org.testng.ITestNGListener
```

---

## 5. Il SUT: l'applicazione sotto test

**SUT** = *System Under Test*, il sistema che stai testando. In questo progetto si chiama
**Atlas Outdoor**: un piccolo e-commerce di attrezzatura da montagna.

### 5.1 Perché il SUT è dentro il repository

Sembra una stranezza. È la decisione più importante del progetto, e ha quattro conseguenze
concrete:

1. **Determinismo.** Nessuna dipendenza esterna significa che un fallimento è sempre un difetto.
2. **Esecuzione offline.** La suite gira su un aereo, in treno, o su un agent CI senza accesso a
   internet.
3. **Nessun conflitto di porta.** Il server chiede al sistema operativo una porta libera
   (*effimera*), quindi puoi lanciare più suite in parallelo sulla stessa macchina.
4. **Un avversario controllato.** Ed è il punto vero: **posso mettere nell'applicazione esattamente
   i comportamenti che rompono le suite reali**, e dimostrare che il framework li regge. Con un sito
   demo di terze parti non puoi farlo: puoi solo sperare che capiti.

### 5.2 Il server: `SandboxServer.java`

Sta in `src/main/java/io/atlas/qa/core/sandbox/SandboxServer.java`. È un singleton avviato
pigramente al primo utilizzo.

**Tecnologia:** `com.sun.net.httpserver.HttpServer`, il web server incluso nel JDK. **Zero
dipendenze aggiuntive**: nessun Jetty, nessun Tomcat, nessun container. Sono circa 200 righe.

```java
this.server = HttpServer.create(new InetSocketAddress(bindAddress, requestedPort), 0);
this.server.createContext("/api/", this::handleApi);     // le rotte JSON
this.server.createContext("/", this::handleStatic);      // i file della web app
this.server.setExecutor(Executors.newFixedThreadPool(8, /* thread daemon */));
```

Dettagli che meritano una spiegazione:

- **`requestedPort` è 0 di default.** Zero significa "sistema operativo, scegli tu una porta
  libera". L'URL reale è noto solo a runtime, ed è per questo che `AtlasConfig.baseUrl()` non legge
  un file di properties quando l'ambiente è `sandbox`, ma chiede l'URL al server.
- **Thread daemon.** Un thread daemon non impedisce alla JVM di terminare. Se il server usasse
  thread normali, la build resterebbe appesa alla fine dei test.
- **`bindAddress` e `advertisedHost` sono due impostazioni separate.** È una distinzione che serve
  solo quando i browser girano in container:

  | Scenario | bind | advertised | Perché |
  |---|---|---|---|
  | Browser locale | `127.0.0.1` | `127.0.0.1` | Il browser è sulla stessa macchina |
  | Browser in Docker | `0.0.0.0` | `host.docker.internal` | Il server deve ascoltare su tutte le interfacce, e il browser dentro il container deve ricevere un nome che *lui* sappia risolvere: `127.0.0.1`, per lui, è sé stesso |

- **Shutdown hook.** Alla chiusura della JVM il server viene fermato: nessun processo zombie.
- **Guardia sul path traversal.** Una richiesta a `/../../etc/passwd` viene respinta con 400. Non è
  un server esposto a internet, ma è un'abitudine che costa tre righe.

### 5.3 Com'è composta l'applicazione

```
src/test/resources/sut-app/
├── index.html               pagina di login
├── inventory.html           catalogo prodotti
├── cart.html                carrello
├── checkout-info.html       checkout step 1 — dati di consegna
├── checkout-review.html     checkout step 2 — riepilogo e totali
├── checkout-complete.html   checkout step 3 — conferma ordine
├── widgets.html             "component playground": dialog, iframe, shadow DOM…
├── frame-content.html       il documento caricato dentro l'iframe
├── assets/
│   ├── atlas.css            foglio di stile (tema scuro)
│   └── atlas.js             tutta la logica applicativa condivisa
└── api/
    └── products.json        il catalogo, servito da GET /api/products
```

#### Il flusso funzionale

```
index.html ──login──▶ inventory.html ──aggiungi──▶ cart.html
                            │                          │
                            └──── continua spesa ◀─────┘
                                                       │ checkout
                                                       ▼
                          checkout-info.html ──▶ checkout-review.html ──▶ checkout-complete.html
                            (validazione)          (totali e tasse)         (numero d'ordine)
```

#### `assets/atlas.js` — la logica

Un unico file, circa 150 righe, che espone l'oggetto globale `Atlas`:

| Sezione | Cosa fa |
|---|---|
| `Atlas.session` | salva/legge l'utente in `sessionStorage`; `requireLogin()` rimanda al login se la sessione manca |
| `Atlas.cart` | carrello in `sessionStorage`: `add`, `remove`, `count`, `subtotal`, `tax` (8%), `total` |
| `Atlas.money` | formattazione degli importi in euro con due decimali |
| `Atlas.renderCartBadge` | aggiorna il contatore nell'header, **nascosto quando è zero** |
| `Atlas.scrambleVolatileIds` | rigenera gli id degli elementi marcati `data-volatile-id` |
| `Atlas.showError` | mostra o nasconde il banner di errore |

Due dettagli tecnici importanti:

**1. Il contatore delle richieste HTTP.** All'avvio, `atlas.js` sostituisce `window.fetch` con una
versione che conta le chiamate in corso:

```javascript
window.__atlasPendingRequests = 0;
const nativeFetch = window.fetch.bind(window);
window.fetch = function (...args) {
    window.__atlasPendingRequests++;
    return nativeFetch(...args).finally(() => { window.__atlasPendingRequests--; });
};
```

Questo permette al framework, dal lato Java, di aspettare che la rete sia ferma invece di indovinare:
`Waits.untilPageIsIdle()` esegue uno script che controlla `document.readyState === 'complete'` **e**
`__atlasPendingRequests === 0`. È il tipo di collaborazione tra front-end e QA che nelle aziende
serie si negozia: qui l'ho scritta da entrambe le parti.

**2. Gli id volatili.**

```javascript
scrambleVolatileIds() {
    const suffix = Math.random().toString(36).slice(2, 8);
    document.querySelectorAll("[data-volatile-id]").forEach(element => {
        if (element.id) { element.id = element.id + "-" + suffix; }
    });
}
```

Gli elementi marcati `data-volatile-id` nell'HTML — il pulsante di login, i pulsanti "aggiungi al
carrello", il pulsante di checkout, "continua", "conferma ordine" — cambiano id a ogni caricamento
di pagina. `id="btn-login"` diventa `id="btn-login-k3f9a2"`, poi `id="btn-login-x71bqe"`, e così via.

#### L'API JSON

Servita dallo stesso server, sotto `/api/`:

| Metodo | Rotta | Risposta |
|---|---|---|
| `GET` | `/api/health` | `200` — `{"status":"UP","component":"atlas-sandbox"}` |
| `GET` | `/api/products` | `200` — catalogo completo: 8 prodotti, `currency`, `taxRate` |
| `GET` | `/api/products/{id}` | `200` con il prodotto, `404` se non esiste |
| `POST` | `/api/auth/login` | `200` + token e ruoli · `401` credenziali errate · **`423` account bloccato** |

Il `423 Locked` non è decorativo: è uno status HTTP semanticamente corretto per un account bloccato,
e permette a un test di distinguere "password sbagliata" da "utente disabilitato" senza leggere il
messaggio di errore. È esattamente il tipo di dettaglio di contratto che un test di API dovrebbe
verificare.

#### Gli utenti (`SandboxUsers.java`)

| Utente | Password | Ruoli | A cosa serve |
|---|---|---|---|
| `standard_user` | `atlas_secret` | `CUSTOMER`, `CHECKOUT` | percorso felice: può comprare |
| `readonly_user` | `atlas_secret` | `CUSTOMER` | può navigare e riempire il carrello, **non può ordinare** |
| `locked_user` | `atlas_secret` | — | account bloccato, respinto prima ancora di controllare la password |

Le tre personae sono definite **in Java**, in una sola classe, e usate sia dal server (per l'API)
sia dai test UI sia dagli scenari Gherkin. Un solo punto di verità: se domani aggiungi un ruolo, non
devi cercarlo in quattro posti.

Il token restituito è `"atlas." + base64url(username)`: deterministico, verificabile in un test, e
onesto — non finge di essere un vero JWT.

#### Il catalogo (`api/products.json`)

Otto prodotti con `id`, `name`, `category`, `price`, `stock`, `rating`, `description`.
Scelte fatte apposta:

- **`ATL-006` (Granite Trekking Poles) ha `stock: 0`** → nell'interfaccia il pulsante è disabilitato
  e l'etichetta è "Out of stock". Serve al test `outOfStockProductCannotBeAdded`.
- **Quattro categorie** con numeri diversi di prodotti → i test sui filtri hanno un'attesa concreta
  (categoria "Equipment" = esattamente 3 prodotti).
- **`taxRate: 0.08`** → i test ricalcolano le tasse e il totale invece di confrontarli con un numero
  scritto a mano.

### 5.4 I tre ostacoli progettati apposta

Questo è il motivo per cui il SUT è fatto in casa. Ogni ostacolo corrisponde a una causa reale di
suite instabili.

#### Ostacolo 1 — gli id che cambiano

**Cosa fa l'app:** rigenera gli id degli elementi interattivi a ogni caricamento.

**Cosa succede a un framework normale:** `By.id("btn-login")` non trova più niente. Duecento test
rossi.

**Cosa fa ATLAS:** il locator dichiara la strategia primaria *e* i fallback.

```java
private static final Locator SUBMIT = Locator.named("Login: sign in button")
        .by(By.id("btn-login"))                               // fallisce sempre, di proposito
        .orBy(By.cssSelector("[data-testid='login-submit']"))  // regge
        .orBy(By.xpath("//form[@id='login-form']//button[@type='submit']"))
        .build();
```

La suite resta verde e alla fine del run trovi:

```
| Login: sign in button | 9 | `By.cssSelector: [data-testid='login-submit']` |
```

**Vuoi vedere il framework senza questa protezione?** Disattivala:

```bash
./mvnw test -Psmoke -Datlas.locator.selfHealing=false
```

I test falliscono con un messaggio che nomina l'elemento. È una dimostrazione, non un'affermazione.

#### Ostacolo 2 — il rendering asincrono con latenza variabile

**Cosa fa l'app:** `inventory.html` chiede il catalogo via `fetch`, poi lo disegna dopo un ritardo
**casuale tra 250 e 900 ms**:

```javascript
const latency = 250 + Math.floor(Math.random() * 650);
setTimeout(() => { /* rimuove lo spinner e disegna la griglia */ }, latency);
```

**Cosa succede a un framework normale:** chi usa `Thread.sleep(500)` passa il 60% delle volte. È il
peggior tipo di test: quello che fallisce a caso.

**Cosa fa ATLAS:** la griglia è `display:none` finché non è pronta, ed è dichiarata come *marker*
della pagina. `waitUntilLoaded()` aspetta che sia **visibile**, non che esista. Non c'è nessun numero
scritto nel codice: aspetta quanto serve, non un millisecondo di più.

#### Ostacolo 3 — le parti di browser che i framework ignorano

`widgets.html` raccoglie tutto ciò che di solito viene evitato:

| Elemento | Perché è difficile | Test che lo copre |
|---|---|---|
| `alert` / `confirm` / `prompt` | sono dialog **del browser**, non elementi del DOM: `findElement` non li vede | `nativeDialogsAreHandled` |
| `<iframe>` | è un documento separato: bisogna entrarci e — soprattutto — ricordarsi di uscirne | `iframeContentIsReachable` |
| **Shadow DOM** | un `<atlas-counter>` con shadow root aperto: **invisibile a XPath e a `querySelector`** | `shadowDomComponentIsDriven` |
| Job differito (2 s) | il risultato compare dopo | `deferredRenderingIsAwaited` |
| Seconda scheda | cambio di *window handle*, e ripulitura | `secondWindowIsReadAndClosed` |
| Tabella ordinabile | ordinamento **numerico**: `1204.90` deve stare dopo `98.00`, non prima | `tableSortsNumerically` |
| `<input type="file">` | l'upload va fatto scrivendo il path, mai aprendo il dialog di sistema | `fileIsUploaded` |

Sul shadow DOM vale la pena soffermarsi, perché è la cosa che distingue un candidato che ha letto un
tutorial da uno che ha lavorato su applicazioni moderne. Un *web component* con shadow root
incapsula il proprio DOM: XPath non ci entra, `document.querySelector` non lo vede. L'unica via è
passare per la shadow root:

```java
protected WebElement inShadowRoot(Locator host, By insideShadow) {
    SearchContext shadowRoot = resolver.resolve(host).getShadowRoot();
    return shadowRoot.findElement(insideShadow);
}
```

### 5.5 Perché non basta SauceDemo

SauceDemo è un ottimo sito per imparare. Non è un buon SUT per un progetto da portfolio:

| | SauceDemo | Atlas Outdoor |
|---|---|---|
| Disponibilità | dipende da terzi | sempre |
| Offline | no | sì |
| Id volatili | no | sì, di proposito |
| Shadow DOM | no | sì |
| Latenza variabile | no | sì, 250–900 ms |
| API da testare | no | sì, con JSON Schema |
| Puoi aggiungere una funzionalità? | no | sì, è tuo |

Nel repository ci sono **entrambi**: il profilo `e2e` gira su SauceDemo per dimostrare che il core
non è cucito addosso all'app locale. Ma non fa parte del gate di merge, perché una pipeline che
diventa rossa a causa del server di qualcun altro insegna al team a ignorare le pipeline rosse.

---

## 6. Il core del framework, componente per componente

### 6.1 `config` — la configurazione

**Il problema:** la stessa suite deve girare sul portatile di uno sviluppatore (browser visibile,
un thread), sulla pipeline (headless, sei thread), e in un container che punta a un Grid. Se per
cambiare comportamento bisogna modificare un file e ricompilare, prima o poi qualcuno committa la
propria configurazione locale e rompe la pipeline.

**La soluzione:** quattro sorgenti, dalla più debole alla più forte.

```
1. config/atlas.properties            i default, versionati nel repository
2. config/atlas-<env>.properties      l'overlay per ambiente (sandbox / staging)
3. variabili d'ambiente ATLAS_*       quello che iniettano CI e Docker
4. system property -Datlas.*          quello che sovrascrive uno sviluppatore per un run
```

Esempio concreto della catena in azione:

```bash
# atlas.properties dice        timeout.explicit=15
# atlas-staging.properties dice timeout.explicit=25
ATLAS_TIMEOUT_EXPLICIT=30 ./mvnw test -Pe2e -Datlas.timeout.explicit=45
# vince 45
```

**`ConfigLoader.java`** costruisce la mappa una sola volta, all'avvio, e la rende **immutabile**
(`Collections.unmodifiableMap`). Immutabile significa che sei thread possono leggerla insieme senza
sincronizzazione e senza rischio: è la scelta più semplice che sia anche corretta.

La conversione dei nomi merita una nota: la chiave `timeout.explicit` diventa la variabile
d'ambiente `ATLAS_TIMEOUT_EXPLICIT`, e la chiave `sandbox.bindAddress` (camelCase) diventa
`ATLAS_SANDBOX_BIND_ADDRESS`. Lo fa questa riga:

```java
key.replace('.', '_').replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
```

**`AtlasConfig.java`** è la facciata tipizzata. I test non toccano mai una stringa:

```java
AtlasConfig.explicitTimeout()   // → Duration, non int
AtlasConfig.browser()           // → BrowserType, non String
AtlasConfig.headless()          // → boolean
```

Il valore di questo strato si vede il giorno in cui rinomini una chiave: la modifichi in un punto
solo, e il compilatore ti dice se hai dimenticato qualcosa.

**Un caso speciale:** `baseUrl()`. Quando l'ambiente è `sandbox` non legge nessun file, perché la
porta è nota solo a runtime:

```java
public static String baseUrl() {
    if (environment() == TargetEnvironment.SANDBOX) {
        return SandboxServer.instance().baseUrl();   // avvia il server se serve
    }
    return ConfigLoader.require("baseUrl");
}
```

**Sui segreti:** nel repository non c'è nessuna credenziale reale. Le password di produzione
arrivano dalle variabili d'ambiente, che è esattamente il meccanismo con cui i *credential store* di
Jenkins e i *secrets* di GitHub Actions le iniettano.

### 6.2 `driver` — il ciclo di vita del browser

#### `DriverManager` — una sessione per thread

```java
private static final ThreadLocal<WebDriver> SESSION = new ThreadLocal<>();
```

**Perché `ThreadLocal` e non un campo statico.** TestNG esegue i metodi su un pool di thread. Con un
solo `static WebDriver driver`, sei thread piloterebbero lo stesso browser: i click di un test
finirebbero nella pagina di un altro. È la causa numero uno delle suite che "funzionano solo quando
il parallelismo è spento". Con `ThreadLocal`, ogni thread ha la sua sessione e il problema non esiste
per costruzione.

**Perché `remove()` è obbligatorio.** `quitSession()` chiama sempre `SESSION.remove()` dentro un
`finally`. Senza, il thread del pool — che viene riusato per il test successivo — si porterebbe
dietro il riferimento a un browser morto.

**Perché un browser per ogni test.** Costa circa un secondo e compra isolamento totale: nessun
cookie, nessuna voce di `sessionStorage`, nessuna navigazione residua può passare da un test al
successivo. Le suite che condividono il browser sono più veloci finché non falliscono in modo
dipendente dall'ordine, e a quel punto costano giorni di debug.

#### `DriverFactory` — dove nasce il browser

Quattro responsabilità, in ordine:

1. chiede le *capabilities* alla strategia del browser richiesto;
2. costruisce un driver locale **oppure** un `RemoteWebDriver` verso il Grid — e il test non può
   accorgersi della differenza;
3. applica la politica dei timeout;
4. avvolge l'istanza nel listener degli eventi.

```java
private static void applyTimeouts(WebDriver driver) {
    driver.manage().timeouts()
            .implicitlyWait(Duration.ZERO)               // ← deliberato
            .pageLoadTimeout(AtlasConfig.pageLoadTimeout())
            .scriptTimeout(AtlasConfig.scriptTimeout());
}
```

**L'implicit wait a zero è la riga più importante della classe.** Mescolare attese implicite ed
esplicite produce timeout che nessuno riesce a prevedere: un `findElements` dentro una
`WebDriverWait` può aspettare l'implicit wait a ogni polling, e un'attesa da 10 secondi diventa da 90.
In ATLAS tutta la sincronizzazione passa da un solo punto.

**I driver binari:** li risolve **Selenium Manager**, incluso in Selenium 4. Niente WebDriverManager
come dipendenza, niente `chromedriver` committato, niente da installare sull'agent.

**Sul Grid:** il `RemoteWebDriver` riceve un `LocalFileDetector`. Senza, l'upload di un file verso
un browser in un altro container fallisce in silenzio, perché il path esiste solo sul client.

#### Le capabilities: pattern Strategy

```
BrowserOptionsProvider  (interfaccia)
├── ChromeOptionsProvider
├── FirefoxOptionsProvider
├── EdgeOptionsProvider
└── SafariOptionsProvider
        ▲
OptionsRegistry  ── EnumMap<BrowserType, BrowserOptionsProvider>, costruita una volta
```

Aggiungere un browser significa **aggiungere una classe e registrarla**: nessuna classe esistente
viene modificata. È il principio *open/closed* applicato alla parte di framework che storicamente
degenera in una catena di `if/else` lunga duecento righe.

Argomenti che meritano una spiegazione:

| Argomento | Perché c'è |
|---|---|
| `--headless=new` | il nuovo headless di Chrome condivide il motore di rendering con la versione con finestra: evita i classici "verde in locale, rosso in CI" da differenze di layout |
| `--no-sandbox` | necessario dentro i container, dove il sandboxing del kernel non è disponibile |
| `--disable-dev-shm-usage` | Docker assegna 64 MB a `/dev/shm`; Chrome li esaurisce e le schede crashano. Sembra un test instabile, è un problema di memoria condivisa |
| `excludeSwitches: enable-automation` | toglie la barra "Chrome è controllato da un software di test", che può rubare il focus |
| `SafariOptions` con headless | **lancia un'eccezione**: safaridriver non supporta l'headless. Fallire subito con un messaggio chiaro batte fallire dopo in modo misterioso |

#### `WebDriverEventLogger`

Agganciato con `EventFiringDecorator` di Selenium 4, traccia ogni interazione (`afterGet`,
`beforeClick`, `beforeSendKeys`, `onError`). È la differenza tra un report che dice
`NoSuchElementException` e uno che mostra le ultime cinque azioni che ci hanno portato lì.
Non lancia mai eccezioni: un problema di osservabilità non deve poter far fallire un test.

### 6.3 `element` — il cuore del framework

#### 6.3.1 `Locator` — un indirizzo con un nome e dei piani B

Un `By` di Selenium è anonimo. Quando smette di funzionare, il report dice
`By.cssSelector: #a > div:nth-child(3)` e nessuno sa cosa dovesse essere.

```java
Locator.named("Checkout: pulsante conferma ordine")   // ← un umano lo capisce
       .by(By.id("btn-place-order"))                  // primaria: veloce, fragile
       .orBy(By.cssSelector("[data-testid='place-order']"))  // patto col front-end
       .orBy(By.xpath("//button[normalize-space()='Place order']"))  // ultima spiaggia
       .build();
```

L'oggetto è **immutabile**, quindi può essere una costante `static final` condivisa da tutti i
thread. Esiste anche `Locator.dynamic(...)` per gli elementi parametrici (la riga della tabella di
un certo prodotto), che formatta descrizione e XPath con gli stessi argomenti.

#### 6.3.2 `ElementResolver` — l'algoritmo

L'implementazione ingenua del self-healing prova la strategia primaria per tutto il timeout (15
secondi), poi la prima alternativa per altri 15, e così via. Con tre strategie e un locator morto,
sono 30 secondi persi **per ogni singola interazione**. Nessuno terrebbe accesa una funzionalità del
genere.

ATLAS interroga tutte le strategie **a rotazione dentro un'unica scadenza**:

```
finché (adesso < scadenza)
    per ogni strategia          // la primaria per prima
        se findElements(strategia) non è vuoto
            → restituisci l'elemento
            → e se non era la primaria, registra un evento di healing
    aspetta l'intervallo di polling (200 ms)
```

**Costo di una ricerca guarita: un `findElements` in più, circa 10 ms.** Non 15 secondi. È questa
differenza che rende la funzionalità economicamente sostenibile e quindi tenibile sempre accesa —
l'unico modo in cui una protezione sopravvive al contatto con una pipeline vera.

Il metodo `candidates()` rispetta l'interruttore di configurazione:

```java
return AtlasConfig.selfHealingEnabled() ? locator.strategies() : List.of(locator.primary());
```

Con `-Datlas.locator.selfHealing=false` il framework si comporta come uno classico. Serve a
dimostrare, non a decorare.

L'enum `Visibility` distingue "esiste nel DOM" da "esiste ed è renderizzato". È una distinzione che
conta: il badge del carrello esiste sempre, ma è nascosto quando è vuoto.

#### 6.3.3 `HealingLedger` — il registro

Il self-healing senza contabilità è pericoloso: la suite resta verde mentre i page object marciscono
in silenzio. Il registro tiene due viste:

- **per thread** — gli eventi del test in corso, allegati al suo report;
- **per run** — tutti gli eventi, riversati a fine suite in `locator-backlog.md`.

```markdown
| Element                    | Times healed | Recovered with                        |
| Login: sign in button      |            9 | `By.cssSelector: [data-testid=…]`     |
| Cart: proceed to checkout  |            2 | `By.cssSelector: [data-testid=…]`     |
```

Questo file è ciò che trasforma il self-healing da trucchetto a **debito tecnico tracciato**.

#### 6.3.4 `UiElement` — il wrapper fluente

Regola fondamentale: **`UiElement` contiene un `Locator`, mai un `WebElement`**. L'elemento viene
risolto di nuovo a ogni singola interazione.

Questo elimina *strutturalmente* la `StaleElementReferenceException` nelle applicazioni a pagina
singola, che ridisegnano il DOM a ogni cambio di stato. Un `WebElement` salvato in un campo è un
puntatore a un nodo che potrebbe non esistere più.

**Il click che scala:**

```
click nativo
   └─ se qualcosa lo copre → scroll al centro della viewport → riprova
        └─ se ancora coperto → click via JavaScript
              └─ e scrive un WARNING nel report
```

Il warning non è pigrizia: **un test che ha bisogno di JavaScript per cliccare sta verificando
qualcosa che un utente non potrebbe fare**. Va visto, non nascosto.

**Il mascheramento dei dati sensibili:** se la descrizione del locator contiene *password*, *secret*,
*token* o *card*, il valore digitato viene sostituito con asterischi prima di finire nei log o nel
report. Un report HTML viene allegato ai ticket e incollato nelle chat: una password che finisce lì
è un incidente vero.

**I retry sulla staleness** sono limitati a tre tentativi e riguardano **solo**
`StaleElementReferenceException`. Qualunque altra eccezione viene propagata subito: inghiottire i
fallimenti veri è il modo in cui un framework comincia a nascondere i bug.

#### 6.3.5 `Waits` — sincronizzazione

`Thread.sleep` non compare nel progetto. Ogni attesa è una condizione con una scadenza:

| Metodo | Cosa aspetta |
|---|---|
| `untilUrlContains` | l'URL contiene un frammento |
| `untilPageIsIdle` | `readyState === 'complete'` **e** jQuery fermo **e** nessuna `fetch` in volo |
| `untilAlertPresent` | il dialog nativo del browser |
| `untilNumberOfWindowsIs` | il numero di finestre/schede aperte |
| `until(supplier, descrizione)` | una condizione di dominio qualsiasi |

L'ultimo è il ponte verso il linguaggio del business:

```java
waitUntil(() -> cartBadgeCount() > before, "il badge del carrello cresca dopo aver aggiunto un prodotto");
```

Se scade, il messaggio d'errore è quella frase, non `TimeoutException: expected condition failed`.

### 6.4 `page` — il contratto dei page object

`BasePage` impone tre regole:

**1. Un page object non possiede un driver.** Lo chiede al `DriverManager` del thread corrente.
La stessa classe è quindi sicura in una suite con otto browser attivi.

**2. Ogni pagina dichiara il proprio marker.**

```java
protected abstract Locator pageMarker();   // l'elemento che dimostra che sei sulla pagina giusta
```

`waitUntilLoaded()` lo usa per rispondere alla domanda "sono davvero dove credo di essere?", e se la
risposta è no produce un errore che contiene URL, titolo e marker atteso. È una domanda che senza
questo meccanismo viene copiata in cinquanta test.

Il metodo restituisce `this` già tipizzato come la pagina concreta, così le factory si leggono
`return new CartPage().waitUntilLoaded();` senza cast.

**3. I page object espongono azioni di business, mai `WebElement`.** Un test non deve poter chiamare
`.click()` su un elemento: deve chiamare `.proceedToCheckout()`. La differenza si vede quando il
front-end cambia: cambi il page object, i test restano.

`BasePage` fornisce anche le operazioni "di frontiera": entrare e uscire da un iframe, entrare in una
shadow root, gestire i dialog nativi, eseguire JavaScript.

### 6.5 `report` — le prove

**`ReportManager`** avvolge ExtentReports con un `ThreadLocal<ExtentTest>`.

L'intestazione del report risponde alla prima domanda che si fa chiunque guardi un fallimento che
non ha lanciato lui: *com'era configurato questo run?* Ambiente, browser, headless, locale o Grid,
self-healing attivo, politica di retry, versione di Java, sistema operativo, URL di base.

**Contratto importante:** se nessun report è agganciato al thread, `ReportManager.step(...)` non fa
niente. I page object restano usabili fuori da un run TestNG. Un page object che funziona solo dentro
un report non è riusabile.

**`ScreenshotService`** cattura tre cose al fallimento:

1. **base64** incorporato nell'HTML → il report è un file unico, si può inviare per email;
2. **PNG su disco** → è ciò che le CI archiviano come artefatto;
3. **il page source** → a volte lo screenshot sembra a posto ed è il DOM a raccontare la verità.

Ogni metodo tollera il fallimento: un browser già morto non può fare screenshot, e quel problema
secondario non deve sostituire l'errore vero nel report.

**I log** portano `%X{testName}` grazie al `ThreadContext` di Log4j2. Un file di log prodotto da
otto browser in parallelo, senza il nome del test su ogni riga, è illeggibile.

### 6.6 `listener` — le politiche di esecuzione

#### `RetryAnalyzer` — l'opinione più forte del progetto

```java
if (cause instanceof AssertionError) return false;   // mai
if (cause instanceof WebDriverException
 || cause instanceof SocketException
 || cause instanceof IOException) return true;       // sì, fino al limite configurato
```

Riprovare tutto è il modo più rapido per smettere di fidarsi della propria suite: una regressione
vera che fallisce una volta su due diventa invisibile. Qui un'asserzione fallita resta rossa, sempre.
I retry sono limitati, scritti nel report, e `@NoRetry` permette di escludere del tutto un test
(usato sul test dei permessi, dove un secondo tentativo potrebbe solo mascherare un difetto).

#### `RetryTransformer` — applicare la politica senza duplicarla

Scrivere `@Test(retryAnalyzer = RetryAnalyzer.class)` su quattrocento metodi è duplicazione che prima
o poi diverge: basta una dimenticanza e una classe si comporta diversamente da tutte le altre. Il
transformer aggancia la politica a runtime, in un punto solo.

#### `TestReportListener`

Trasforma gli eventi grezzi di TestNG in una narrazione: apre il report del test, mette il nome nel
contesto di logging, allega gli eventi di healing, e al fallimento raccoglie le prove.

Dettaglio non ovvio: nei test *data-driven* il nome del metodo è sempre lo stesso. Il listener
aggiunge i parametri al nome, così nel report leggi
`rejectedSignIn [locked account is refused…]` invece di sei righe identiche.

#### `SuiteLifecycleListener`

All'avvio scrive la configurazione effettiva (mascherando le password) e avvia il SUT. Alla fine
scarica il report su disco e genera il backlog dei locator.

#### La registrazione via `META-INF/services`

I tre listener sono dichiarati in
`src/test/resources/META-INF/services/org.testng.ITestNGListener`. TestNG li trova da solo tramite il
`ServiceLoader` di Java.

Due motivi, entrambi concreti:

1. Un `IAnnotationTransformer` **deve** essere registrato prima che TestNG legga le annotazioni:
   `@Listeners` non può farlo.
2. Dichiararli in ogni file suite duplicherebbe la politica e, soprattutto, romperebbe il
   funzionamento dall'IDE.

**Conseguenza pratica:** in Eclipse, tasto destro su una classe → *Run As → TestNG Test* si comporta
esattamente come la pipeline. Un framework il cui comportamento dipende da come lo lanci è un
framework di cui la gente smette di fidarsi.

### 6.7 `data` — i dati di test

**`JsonDataReader`** deserializza le fixture in `record` Java. Il vantaggio non è estetico: la firma
di un test diventa `void checkout(Customer customer)` invece di quattro stringhe posizionali, e una
modifica alla fixture fallisce **a compile time**, non alla riga 37 di un foglio di calcolo.

**`ExcelDataReader`** legge un `.xlsx` con Apache POI in una lista di mappe intestazione→valore.
Excel non è un buon formato per i dati di test; è il formato che i business analyst usano davvero.
Supportarlo è il modo in cui l'automazione riceve i casi di test da chi possiede i requisiti.

**`FakeData`** genera dati sintetici con Datafaker, ma **con un seed**:

```
INFO  Synthetic data seed = 1754257384912 (replay this run with -Datlas.data.seed=1754257384912)
```

I dati casuali trovano bug che le fixture fisse non troveranno mai — e rendono i fallimenti
irriproducibili, che è il motivo per cui quasi tutti i team li abbandonano. Il seed stampato risolve
il compromesso: rilanci con quel valore e ottieni **esattamente** lo stesso set di dati.

Le email generate finiscono sempre su `@atlas.test`: nessun test manda mai una mail a una persona
vera.

### 6.8 `api` — il client REST

**Perché un framework UI ha un livello API.** Due motivi, entrambi sul lato UI:

- **velocità** — fare login via API e iniettare la sessione costa ~50 ms contro i ~3 s di
  compilazione di una form, moltiplicati per ogni test;
- **isolamento** — le precondizioni (un utente, un ordine, un carrello pieno) si creano via API, così
  un test sul checkout non fallisce perché è rotta la pagina di registrazione.

**`ApiClient` è immutabile.** Ogni metodo `withX` restituisce una nuova istanza: condividere una
`RequestSpecification` mutabile tra thread paralleli è una fonte classica di interferenze.

**`ApiTraceFilter`** registra ogni scambio HTTP con metodo, URL, status e durata, e lo allega al
report. Prima di scrivere qualsiasi cosa, **oscura** i campi sensibili:

```java
body.replaceAll("(?i)(\"(?:password|token|authorization|secret)\"\\s*:\\s*\")[^\"]*(\")", "$1********$2");
```

---

## 7. Il livello dei test

### 7.1 Le classi base

**`BaseWebTest`** apre un browser prima di ogni metodo e lo chiude dopo, sempre
(`alwaysRun = true`, così avviene anche se il test è saltato).

```java
@BeforeMethod(alwaysRun = true)
@Parameters("browser")
public void startBrowser(@Optional String browser) { … }
```

Il parametro `browser` è opzionale: quando il file suite lo fornisce (suite cross-browser) vince
quello, altrimenti si usa il default della configurazione. È ciò che permette alla stessa classe di
girare su tre browser in parallelo **e** di essere lanciabile da Eclipse senza alcun setup.

**`BaseApiTest`** non apre nessun browser. Un test di API che paga il costo di una sessione WebDriver
che non userà mai è lo spreco più comune nelle suite ibride.

### 7.2 I page object

| Classe | Pagina | Cosa espone |
|---|---|---|
| `LoginPage` | login | `signInAs`, `signInExpectingRejection`, `errorMessage` |
| `InventoryPage` | catalogo | `filterByCategory`, `sortBy`, `addToCart`, `cartBadgeCount`, `openCart` |
| `CartPage` | carrello | `lineNames`, `quantityOf`, `subtotal`, `tax`, `total`, `removeLine`, `proceedToCheckout` |
| `CheckoutDetailsPage` | checkout 1 | `fill(Customer)`, `fillPartially`, `submit`, `submitExpectingRejection` |
| `CheckoutReviewPage` | checkout 2 | `lineCount`, `subtotal`, `tax`, `total`, `placeOrder` |
| `OrderConfirmationPage` | conferma | `orderReference`, `itemCount`, `amountCharged` |
| `PlaygroundPage` | widgets | dialog, iframe, shadow DOM, job asincrono, seconda scheda, upload |

Nota il metodo `addToCart`: non si limita a cliccare, **verifica l'effetto osservabile**.

```java
public InventoryPage addToCart(String productName) {
    int before = cartBadgeCount();
    $(addToCartButtonFor(productName)).click();
    waitUntil(() -> cartBadgeCount() > before, "il badge cresca dopo aver aggiunto '" + productName + "'");
    return this;
}
```

Asserire sull'effetto e non sul click è ciò che rende un page object affidabile in un'applicazione
asincrona.

### 7.3 Le classi di test

| Classe | Test | Cosa dimostra |
|---|---:|---|
| `AuthenticationTests` | 8 | login valido, 6 scenari negativi data-driven, sessione scaduta |
| `CatalogueTests` | 5 | caricamento asincrono, filtri, ordinamento per prezzo, prodotto esaurito, carrello |
| `CheckoutJourneyTests` | 5 | acquisto completo con verifica dell'aritmetica, validazione, permessi |
| `BrowserCapabilityTests` | 7 | dialog, iframe, shadow DOM, attese, seconda scheda, tabella, upload |
| `LocatorResilienceTests` | 3 | **test del framework stesso** |
| `CatalogueApiTests` | 8 | contratto REST con JSON Schema, autenticazione, 401/404/423 |

`LocatorResilienceTests` merita attenzione: sono test **del framework**, non dell'applicazione.
Verificano che l'app rigeneri davvero gli id, che una strategia primaria morta venga guarita e
registrata, e che un locator senza fallback fallisca con un messaggio che nomina l'elemento. Se il
resolver smettesse di funzionare, tutti gli altri test continuerebbero a passare — fino al giorno in
cui l'applicazione cambia e ne falliscono duecento insieme. Questi tre lo impediscono.

### 7.4 L'aritmetica del checkout

Il test dell'acquisto **ricalcola** i totali invece di confrontarli con numeri scritti a mano:

```java
BigDecimal subtotal = cart.subtotal();
BigDecimal expectedTax = subtotal.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);

assertThat(cart.tax()).isEqualByComparingTo(expectedTax);
assertThat(cart.total()).isEqualByComparingTo(subtotal.add(expectedTax));
// … e alla fine
assertThat(confirmation.amountCharged()).isEqualByComparingTo(amountReviewed);
```

**Mai `double`.** `0.1 + 0.2` non fa `0.3` in virgola mobile binaria. Un test sul checkout che
confronta importi come `double` fallisce una volta al mese per un artefatto di arrotondamento, e una
suite che fallisce per motivi che nessuno sa spiegare smette di essere creduta molto prima di
smettere di essere eseguita. La classe `Money` fa il parsing degli importi renderizzati in
`BigDecimal`, con una regola esplicita per distinguere il separatore decimale da quello delle
migliaia.

---

## 8. Il livello BDD

### 8.1 Cosa c'è

```
src/test/java/io/atlas/qa/bdd/
├── runner/CucumberTestRunner.java     il punto di ingresso
├── support/
│   ├── Hooks.java                     apertura/chiusura browser, prove al fallimento
│   └── ScenarioContext.java           stato condiviso tra le step definition
└── steps/
    ├── AuthenticationSteps.java
    └── CheckoutSteps.java

src/test/resources/features/
├── authentication.feature             1 scenario + 1 outline con 4 esempi
└── checkout.feature                   3 scenari
```

### 8.2 Le regole rispettate

**Regola 1 — le step definition non contengono locator né attese.** Solo delega ai page object:

```java
@When("I add {string} to the cart")
public void iAddToTheCart(String product) {
    context.put("catalogue", context.get("catalogue", InventoryPage.class).addToCart(product));
}
```

Una step definition che contiene un locator è un page object travestito: il giorno in cui una seconda
feature ha bisogno della stessa interazione, viene copiata invece che riusata.

**Regola 2 — nessuno stato statico.** `ScenarioContext` viene creato da **PicoContainer** una volta
per scenario e iniettato nel costruttore delle classi di step. È ciò che permette agli scenari di
girare in parallelo.

**Regola 3 — BDD riusa il prodotto, non è un secondo framework.** Gli stessi page object, lo stesso
driver, lo stesso report, la stessa politica di retry.

### 8.3 Il trucco del reporting

Il runner Cucumber espone **un solo metodo TestNG** (`runScenario`) per tutti gli scenari. Senza
accorgimenti, il report conterrebbe otto voci chiamate tutte "runScenario".

La soluzione è un'interfaccia marcatore, `SelfReporting`:

```java
public class CucumberTestRunner extends AbstractTestNGCucumberTests implements SelfReporting { … }
```

Il listener TestNG vede il marcatore e si fa da parte; sono gli hook Cucumber a registrare ogni
scenario con il suo nome vero, quello di business.

### 8.4 Filtrare per tag

```bash
./mvnw test -Pbdd -Dcucumber.tags="@smoke"
./mvnw test -Pbdd -Dcucumber.tags="@regression and not @authorisation"
./mvnw test -Pbdd -Dcucumber.tags="@checkout or @authentication"
```

---

## 9. Suite TestNG e profili Maven

### 9.1 I file suite

| File | Parallelismo | Contenuto |
|---|---|---|
| `smoke.xml` | 4 thread, per metodo | solo il gruppo `smoke`, UI + API |
| `sandbox-regression.xml` | 6 thread + 4 per i data provider | due blocchi: interfaccia e servizi |
| `cross-browser.xml` | 3 test in parallelo | lo stesso gruppo su Chrome, Firefox, Edge |
| `api.xml` | 8 thread | solo servizi, nessun browser |
| `bdd.xml` | 4 scenari in parallelo | il runner Cucumber |
| `e2e-public.xml` | 2 thread | SauceDemo, escluso dal gate di merge |

`cross-browser.xml` mostra come si inietta il browser:

```xml
<test name="Firefox">
    <parameter name="browser" value="firefox"/>
    <groups><run><include name="smoke"/></run></groups>
    <packages><package name="io.atlas.qa.sandbox.tests"/></packages>
</test>
```

Il parametro arriva in `BaseWebTest.startBrowser(@Optional String browser)`. Nessun codice di test
sa quale browser sta usando: è esattamente lo scopo di avere una factory.

### 9.2 I profili Maven

| Profilo | Effetto |
|---|---|
| `smoke`, `regression`, `api`, `bdd`, `e2e`, `cross-browser` | scelgono il file suite e il numero di thread |
| `grid` | `remote=true`, punta al Grid, 8 thread |
| `headed` | browser visibile, un thread solo (per guardare) |

Si combinano: `-Pregression,grid` esegue la regressione sul Grid.

Il collegamento tra Maven e la configurazione avviene nel `pom.xml`, dove Surefire passa le
proprietà alla JVM dei test:

```xml
<systemPropertyVariables>
    <atlas.browser>${atlas.browser}</atlas.browser>
    <atlas.headless>${atlas.headless}</atlas.headless>
    <atlas.remote>${atlas.remote}</atlas.remote>
    …
</systemPropertyVariables>
```

Sono la sorgente più forte della catena di precedenza vista nella [sezione 6.1](#61-config--la-configurazione).

---

## 10. Parallelismo e thread-safety

Cosa succede realmente con `-Pregression` (6 thread):

```
Thread-1 ──▶ browser 1 ──▶ sessione TestNG ──▶ HealingLedger (lista thread-local)
Thread-2 ──▶ browser 2 ──▶ …                        │
…                                                   ▼
Thread-6 ──▶ browser 6                      coda globale condivisa
                                            (ConcurrentLinkedQueue)
        tutti leggono ──▶ mappa di configurazione IMMUTABILE
        tutti scrivono ─▶ ExtentReports (ThreadLocal<ExtentTest>)
        tutti chiamano ─▶ SandboxServer (uno solo, con pool di 8 thread HTTP)
```

Le difese, una per volta:

| Rischio | Difesa |
|---|---|
| Due test sullo stesso browser | `ThreadLocal<WebDriver>` |
| Thread del pool riusato con sessione morta | `remove()` nel `finally` |
| Configurazione modificata a metà run | mappa immutabile, risolta una volta |
| Voci di report mescolate | `ThreadLocal<ExtentTest>` |
| Log illeggibili | `ThreadContext` con il nome del test su ogni riga |
| Dati di test in collisione | dati generati per test, con seed |
| Stato condiviso tra scenari BDD | contesto iniettato per scenario da PicoContainer |
| Porta occupata | porta effimera scelta dal sistema operativo |

**Quanti thread?** Il collo di bottiglia sono la RAM e la CPU disponibili per i browser, non il
framework. Su un portatile, 4–6 thread è il punto oltre il quale i tempi peggiorano invece di
migliorare. Su un Grid con nodi dedicati si sale.

---

## 11. Reporting: come si legge un run

### 11.1 Il report HTML

`target/atlas-report/index.html`, generato con ExtentReports (tema scuro).

- **Intestazione** — com'era configurato il run.
- **Elenco dei test** — ognuno con durata, gruppi, e i passi in ordine.
- **I passi** — generati automaticamente da `UiElement` (`click su 'Cart: proceed to checkout'`) e
  dai passi espliciti dei page object (`Signing in as 'standard_user'`).
- **I WARNING gialli** — sono le guarigioni dei locator e i click via JavaScript. Vanno letti.
- **Il fallimento** — messaggio, stack trace, screenshot incorporato.

Il report è **pubblicato automaticamente** dalla pipeline: la versione dell'ultimo run su `main` è su
`https://ctoscanoeng.github.io/atlas-test-framework/latest/`.

### 11.2 Il backlog dei locator

`target/atlas-report/locator-backlog.md`. Se è vuoto, il log dice:

```
Locator health: every element was found with its primary strategy
```

Se non lo è, contiene la lista degli elementi da riparare. È l'output che rende il self-healing una
pratica di manutenzione invece che un modo per rimandare i problemi.

### 11.3 I log

`target/atlas-report/logs/atlas.log`, livello DEBUG, con il nome del test su ogni riga:

```
10:31:22.104 DEBUG [TestNG-test=User interface-1] [customerCompletesAPurchase] i.a.q.c.e.UiElement - click on 'Catalogue: add 'Summit Down Jacket' to cart'
```

---

## 12. Selenium Grid e Docker

### 12.1 Le tre topologie

| Topologia | Comando | Quando serve |
|---|---|---|
| **Locale** | `./mvnw test -Pregression` | sviluppo quotidiano |
| **Grid** | `docker compose -f docker/docker-compose.grid.yml up -d` + `-Pregression,grid` | più browser, più parallelismo, versioni fissate |
| **Tutto in container** | `docker compose -f docker/docker-compose.ci.yml up --build --exit-code-from atlas-tests` | riprodurre la CI su una macchina con solo Docker |

### 12.2 Il Grid

`docker-compose.grid.yml` avvia un hub e quattro nodi (2 Chrome, 1 Firefox, 1 Edge). Due dettagli
che sembrano minori e non lo sono:

- **`shm_size: 2gb`** — Chrome scrive la cache di rendering in `/dev/shm`; il default di Docker è
  64 MB e le schede crashano sotto carico. Sembra un test instabile, è memoria condivisa esaurita.
- **`SE_SESSION_REQUEST_TIMEOUT`** — una sessione che non può essere servita deve fallire in fretta
  invece di tenere appesa la suite.

Durante l'esecuzione, `http://localhost:4444/ui` mostra le sessioni vive e la coda.

### 12.3 Il Dockerfile

Immagine multi-stage che contiene **la suite e un JDK, nessun browser**: i browser stanno nei
container del Grid. Questa separazione tiene l'immagine piccola e rende rispondibile la domanda
"quale versione di Chrome ha usato quel run?".

Le dipendenze vengono risolte in un layer separato: finché `pom.xml` non cambia, una modifica al
codice ricostruisce l'immagine in pochi secondi.

### 12.4 Il problema di rete che quasi tutti sbagliano

Il SUT gira **dentro il processo dei test**. Se i browser sono in altri container, `127.0.0.1` per
loro significa "sé stessi": non raggiungono l'applicazione.

```
┌─ container test ─────────┐        ┌─ container chrome ──────┐
│  suite + SandboxServer   │◀───────│  browser                │
│  bind 0.0.0.0:54321      │        │  apre http://atlas-tests:54321
└──────────────────────────┘        └─────────────────────────┘
      advertised host = "atlas-tests"  ← nome risolvibile nella rete Docker
```

Configurazione:

```bash
ATLAS_SANDBOX_BIND_ADDRESS=0.0.0.0          # ascolta su tutte le interfacce
ATLAS_SANDBOX_ADVERTISED_HOST=atlas-tests   # nome che i browser sanno risolvere
```

Su Docker Desktop (macOS/Windows), quando la suite gira **sull'host** e i browser in container, il
nome giusto è `host.docker.internal`.

---

## 13. Le pipeline: com'è strutturata la CI

Nel repository ci sono **due** pipeline che fanno la stessa cosa su due prodotti diversi:
`.github/workflows/ci.yml` per GitHub Actions e `Jenkinsfile` per Jenkins. Non è ridondanza fine a
sé stessa: GitHub Actions è dove il progetto vive e mostra il badge verde, Jenkins è ciò che trovi
installato nella maggior parte delle aziende italiane. Saper leggere e scrivere entrambi è parte del
mestiere.

### 13.1 Il principio: gli stadi in ordine di costo

Il criterio con cui è ordinata la pipeline è uno solo: **fallire il prima possibile e il più a buon
mercato possibile.**

```
   ~15 s          ~1 min              ~3 min                    ~1 min
┌──────────┐   ┌──────────┐   ┌────────────────────┐   ┌──────────────┐
│ Contratti│──▶│  Smoke   │──▶│ Regressione        │──▶│ Pubblicazione│
│   API    │   │ (chrome) │   │ chrome ∥ firefox   │   │  del report  │
│ 0 browser│   │ 1 browser│   │ + scenari BDD      │   │              │
└──────────┘   └──────────┘   └────────────────────┘   └──────────────┘
```

Se il contratto dell'API è rotto, la pipeline muore in quindici secondi senza aver mai avviato un
browser. Se lo smoke fallisce, non si spendono tre minuti di regressione per scoprire che
l'applicazione non parte nemmeno. È l'inverso della pipeline "un job unico che fa tutto", dove per
sapere se hai rotto il login aspetti dodici minuti.

### 13.2 GitHub Actions, job per job

| Job | Quando gira | Cosa fa |
|---|---|---|
| `service-contracts` | sempre | `-Papi`: 8 test, nessun browser |
| `smoke` | dopo il precedente | `-Psmoke` su Chrome headless |
| `regression` | **non** sui push — solo PR, notturno e manuale | matrice Chrome + Firefox, `fail-fast: false` |
| `behaviour` | idem | scenari Gherkin |
| `publish-report` | solo su `main`, solo se la regressione è passata | pubblica il report su GitHub Pages |

**Gli inneschi:**

```yaml
on:
  push:         [main, develop]     # gate veloce: api + smoke
  pull_request: [main]              # gate completo
  schedule:     "0 3 * * *"         # regressione notturna
  workflow_dispatch:                # esecuzione manuale, con scelta del profilo
```

**Le decisioni che meritano una spiegazione:**

`concurrency` con `cancel-in-progress: true` — se pushi tre volte di fila, i run vecchi vengono
annullati. Testare un commit già superato è tempo di CI buttato.

`fail-fast: false` nella matrice — se Firefox fallisce, Chrome deve comunque arrivare in fondo.
Altrimenti scopri un browser rotto alla volta invece che tutti insieme.

**Firefox installato esplicitamente.** Su alcune immagini Ubuntu di GitHub, Firefox è la build
*snap*, che geckodriver non riesce a pilotare. Il passo `browser-actions/setup-firefox` trasforma un
mistero occasionale in una versione fissata.

```yaml
- name: Install Firefox
  if: matrix.browser == 'firefox'
  uses: browser-actions/setup-firefox@v1
```

**La pubblicazione è condizionata:**

```yaml
if: always() && needs.regression.result == 'success' && github.ref == 'refs/heads/main'
```

Senza `needs.regression.result == 'success'`, su un push normale il job partirebbe, cercherebbe un
artefatto che non esiste (la regressione non gira sui push) e fallirebbe: badge rosso su un
repository appena pubblicato, per un motivo che non c'entra con la qualità del codice.

**Nessun passo di installazione del browser per Chrome, nessuno per i driver.** L'immagine del runner
include già Chrome, e Selenium Manager risolve il driver. Meno passi nella pipeline significa meno
cose che possono rompersi.

**Gli artefatti** vengono caricati con `if: always()`, cioè **anche quando i test falliscono** — che
è precisamente il momento in cui il report serve.

### 13.3 Jenkins, stadio per stadio

Il `Jenkinsfile` è **dichiarativo** (`pipeline { … }`), non scripted. La sintassi dichiarativa è più
rigida ma è quella che Jenkins sa disegnare graficamente nella *Stage View*.

```groovy
pipeline {
    agent any

    parameters {
        choice(name: 'SUITE',   choices: ['regression','smoke','api','bdd','cross-browser'])
        choice(name: 'BROWSER', choices: ['chrome','firefox','edge'])
        booleanParam(name: 'USE_GRID', defaultValue: true)
        string(name: 'SANDBOX_HOST', defaultValue: 'host.docker.internal')
    }

    options {
        timestamps()                                   // ogni riga di log con l'orario
        timeout(time: 45, unit: 'MINUTES')             // un job appeso non blocca l'esecutore
        buildDiscarder(logRotator(numToKeepStr: '30')) // 30 build, poi si buttano
        disableConcurrentBuilds()                      // due run insieme si contendono la porta 4444
    }

    stages {
        stage('Checkout')          { … compila senza eseguire i test }
        stage('Service contracts') { … -Papi, il gate più economico }
        stage('Start the Grid')    { … solo se USE_GRID: docker compose up -d --wait }
        stage('Browser suite')     { … la suite scelta dal parametro }
    }

    post { always { … junit + publishHTML + archiveArtifacts + docker compose down } }
}
```

**I parametri sono la ragione per cui vale la pena avere Jenkins.** Un tester manuale può aprire
Jenkins, scegliere "smoke" e "firefox" da due menu a tendina, premere *Build* e leggere il report.
Non deve sapere cosa sia Maven.

**Lo stadio più delicato è `Browser suite`,** perché deve risolvere il problema di rete della
[sezione 12.4](#124-il-problema-di-rete-che-quasi-tutti-sbagliano):

```groovy
def bindAddress    = params.USE_GRID ? '0.0.0.0' : '127.0.0.1'
def advertisedHost = params.USE_GRID ? params.SANDBOX_HOST : '127.0.0.1'

withEnv(["ATLAS_SANDBOX_BIND_ADDRESS=${bindAddress}",
         "ATLAS_SANDBOX_ADVERTISED_HOST=${advertisedHost}"]) {
    sh "./mvnw -B test -P${params.SUITE} ${gridFlags}"
}
```

Con browser locali, il loopback è l'unico indirizzo sicuramente valido. Con browser in container,
serve un nome che *loro* sappiano risolvere. Metterlo in un `withEnv` invece che nel blocco
`environment` globale permette di deciderlo **dopo** aver letto i parametri.

**Il blocco `post { always { … } }`** fa quattro cose, tutte anche quando la build è rossa:

| Passo | Perché |
|---|---|
| `junit 'target/surefire-reports/junitreports/*.xml'` | dà a Jenkins il grafico storico dei test e la lista dei falliti |
| `publishHTML(...)` | pubblica il report ExtentReports come tab nella build |
| `archiveArtifacts 'target/atlas-report/**'` | conserva screenshot, log e **il backlog dei locator** |
| `docker compose down` | spegne il Grid: un esecutore che lascia container accesi prima o poi esaurisce la macchina |

### 13.4 Le differenze tra le due pipeline

| | GitHub Actions | Jenkins |
|---|---|---|
| Parallelismo tra job | nativo (`needs`) | sequenziale in questo file; servirebbe `parallel { }` |
| Matrice di browser | `strategy.matrix` | il parametro `BROWSER`, un browser per build |
| Grid | non usato: i browser sono sul runner | container avviati e spenti dalla pipeline stessa |
| Report | pubblicato su GitHub Pages | tab HTML nella build + artefatti |
| Chi lo lancia | ogni push e ogni PR | un umano dalla UI, o un trigger |

Sono complementari, e la scelta di tenerli entrambi è deliberata: la CI pubblica dimostra che il
progetto è verde, il Jenkinsfile dimostra che sai lavorare con l'orchestratore che troverai in
azienda.

### 13.5 Vedere la pipeline in Jenkins

C'è una guida dedicata, passo per passo, dall'installazione al primo run verde:
**[JENKINS.md](JENKINS.md)**.

---

## 14. Glossario

| Termine | Significato |
|---|---|
| **SUT** | *System Under Test* — l'applicazione che stai testando. Qui: Atlas Outdoor |
| **Page Object** | classe che rappresenta una pagina ed espone azioni di business, non elementi |
| **Locator** | l'indirizzo di un elemento. In ATLAS: descrizione + strategia primaria + fallback |
| **Self-healing** | quando la strategia primaria fallisce, si usa un fallback dichiarato, e lo si registra |
| **Explicit wait** | attesa su una condizione con una scadenza. L'opposto di `Thread.sleep` |
| **Implicit wait** | timeout globale su ogni `findElement`. Qui è **zero**, di proposito |
| **Stale element** | riferimento a un elemento che il DOM ha sostituito. Risolto risolvendo di nuovo |
| **Shadow DOM** | DOM incapsulato dentro un web component, invisibile a XPath |
| **Selenium Grid** | hub + nodi che eseguono i browser su altre macchine o container |
| **ThreadLocal** | variabile con un valore diverso per ogni thread. Base della thread-safety qui |
| **Data provider** | metodo TestNG che alimenta lo stesso test con più set di dati |
| **Retry analyzer** | componente TestNG che decide se rieseguire un test fallito |
| **BDD / Gherkin** | scenari in linguaggio naturale (`Given/When/Then`) eseguibili |
| **Smoke test** | il sottoinsieme minimo che risponde a "vale la pena testare oltre?" |
| **Flaky test** | test che passa e fallisce senza che il codice cambi. Il nemico numero uno |
| **JSON Schema** | descrizione formale della struttura di una risposta JSON, verificabile |

---

## 15. Domande frequenti e problemi noti

**`./mvnw: Permission denied`**
Il bit di esecuzione si perde quando il progetto passa da uno zip: `chmod +x mvnw`.

**`No compiler is provided in this environment`**
Hai un JRE, non un JDK. Installa un JDK 17+ e verifica `java -version`.

**I test partono ma non vedo nessun browser**
È l'headless, il default. Usa `-Pheaded` o `-Datlas.headless=false`.

**Voglio guardare un test al rallentatore**
`./mvnw test -Dtest=CheckoutJourneyTests -Pheaded` — un thread solo, finestra visibile.

**`Unable to obtain a chrome session from the Grid`**
Il Grid non è avviato: `docker compose -f docker/docker-compose.grid.yml up -d`, poi verifica
`http://localhost:4444/ui`. Oppure togli `-Pgrid` e usa i browser locali.

**Sul Grid i browser non raggiungono l'applicazione**
È il problema della [sezione 12.4](#124-il-problema-di-rete-che-quasi-tutti-sbagliano):
`ATLAS_SANDBOX_BIND_ADDRESS=0.0.0.0` e `ATLAS_SANDBOX_ADVERTISED_HOST=host.docker.internal`.

**`Safari does not support headless execution`**
È il framework che rifiuta di fallire in modo misterioso. `-Datlas.headless=false` o un altro
browser.

**Un test fallisce solo in parallelo**
Rilancialo da solo con `-Pheaded` e guardalo. Lo stato condiviso è la causa abituale: questo
framework dà a ogni test il suo browser proprio per escluderla.

**Il report è vuoto**
Viene scritto su disco dal `SuiteLifecycleListener` alla fine della suite. Se hai interrotto il run
con Ctrl-C, il flush non è avvenuto.

**Come aggiungo un test nuovo**
1. Se serve una pagina nuova: crea il page object in `sandbox/pages`, estendi `BasePage`, dichiara
   il `pageMarker()`.
2. Crea il metodo in una classe di test che estende `BaseWebTest`.
3. Mettilo nel gruppo giusto: `@Test(groups = {"regression", "checkout"}, description = "…")`.
   La `description` finisce nel report: scrivila come la leggerebbe un umano.

**Come aggiungo una pagina al SUT**
Il SUT è tuo: aggiungi il file in `src/test/resources/sut-app/`, includi `assets/atlas.js`, e usa
`data-testid` sugli elementi. Se vuoi che un elemento dimostri il self-healing, aggiungi anche
`data-volatile-id`.

**Posso usare questo framework su un'altra applicazione?**
Sì, ed è il punto. Crea `config/atlas-<nome>.properties` con il `baseUrl`, scrivi i page object, e
lancia con `-Datlas.env=<nome>`. Il core non va toccato. Il profilo `e2e` lo dimostra su SauceDemo.

---

*Ultimo aggiornamento: agosto 2026 — versione 1.0.0 del framework.*
