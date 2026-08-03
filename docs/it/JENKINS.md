# Vedere la pipeline in Jenkins — guida passo per passo

> Obiettivo: avere Jenkins in esecuzione sul tuo Mac, un job che legge il `Jenkinsfile` di questo
> repository, e una build verde con il report HTML consultabile dalla sua interfaccia grafica.
>
> Tempo realistico la prima volta: **30–40 minuti**, di cui metà sono attese di download.
>
> La spiegazione di *come è strutturata* la pipeline sta in
> [GUIDA-COMPLETA.md §13](GUIDA-COMPLETA.md#13-le-pipeline-comè-strutturata-la-ci). Qui si esegue.

---

## 0. Perché farlo, visto che GitHub Actions funziona già

Tre motivi concreti:

1. **Jenkins è quello che troverai in azienda.** In Italia, la maggior parte dei team QA che usa
   Selenium lo orchestra con Jenkins. Saper dire "ho scritto un Jenkinsfile dichiarativo, con
   parametri, publishHTML e archiviazione degli artefatti" è diverso da "ho usato la CI di GitHub".
2. **I parametri cambiano chi può lanciare i test.** Con Jenkins, un tester manuale apre una pagina,
   sceglie `smoke` e `firefox` da due tendine e preme un pulsante. Non deve sapere cosa sia Maven.
3. **Vedrai la Stage View**, cioè la pipeline disegnata a blocchi con i tempi di ogni stadio. È
   l'immagine che vale la pena mettere in un portfolio.

---

## 1. Scegliere la strada

| | **A — Jenkins nativo (Homebrew)** | **B — Jenkins in Docker** |
|---|---|---|
| Installazione | `brew install jenkins-lts` | un comando `docker run` |
| Browser per i test UI | usa il **Chrome del tuo Mac**: funziona subito | dentro il container **non c'è nessun browser**: serve il Grid |
| Somiglianza con la produzione | media | alta |
| Cose che possono andare storte | poche | rete tra container, permessi sul socket Docker |
| **Consigliata per iniziare** | ✅ | dopo, quando la prima build è verde |

Questa guida segue la **strada A**, e alla fine ([sezione 9](#9-variante-jenkins-in-docker)) spiega
cosa cambia con la B.

---

## 2. Installare e avviare Jenkins

```bash
brew install jenkins-lts
brew services start jenkins-lts
```

Jenkins si avvia su **http://localhost:8080** e resta attivo anche dopo un riavvio del Mac
(`brew services stop jenkins-lts` per spegnerlo).

Alla prima apertura chiede una password di sblocco:

```bash
cat ~/.jenkins/secrets/initialAdminPassword
```

Copiala nel campo, poi:

1. **Install suggested plugins** — sono circa 80, ci vogliono 3–5 minuti. Includono già Pipeline,
   Git, JUnit e Timestamper, cioè quasi tutto il necessario.
2. Crea il primo utente amministratore (username, password, email). **Non saltare questo passo**:
   se lo salti resti su un account `admin` con una password generata che poi dovrai ripescare.
3. Conferma l'URL `http://localhost:8080/` e vai su *Start using Jenkins*.

---

## 3. Installare i due plugin mancanti

Servono due plugin che **non** sono nel pacchetto consigliato:

| Plugin | A cosa serve |
|---|---|
| **HTML Publisher** | pubblica il report ExtentReports come tab dentro la build |
| **TestNG Results** *(opzionale)* | grafici storici specifici per TestNG (il plugin JUnit già incluso basta) |

Percorso: **Manage Jenkins → Plugins → Available plugins** → cerca `HTML Publisher` → spunta →
**Install** → spunta *Restart Jenkins when installation is complete and no jobs are running*.

---

## 4. Dire a Jenkins dove sono Java e Docker

Questo è il punto in cui si blocca il 90% delle persone la prima volta.

Jenkins avviato da `brew services` gira sotto `launchd` e **non eredita il `PATH` del tuo terminale**.
Quindi non trova `java`, e spesso nemmeno `docker`.

Prima recupera i percorsi reali sul tuo Mac:

```bash
/usr/libexec/java_home        # → es. /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
which docker                  # → es. /usr/local/bin/docker  oppure  /opt/homebrew/bin/docker
```

Poi in Jenkins: **Manage Jenkins → System → Global properties** → spunta **Environment variables** e
aggiungi due voci:

| Name | Value |
|---|---|
| `JAVA_HOME` | l'output di `/usr/libexec/java_home` |
| `PATH` | `/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/homebrew/bin` |

**Save** in fondo alla pagina.

> Il `PATH` qui sopra copre sia i Mac Intel (`/usr/local/bin`) sia gli Apple Silicon
> (`/opt/homebrew/bin`). Se `which docker` restituisce un percorso diverso, aggiungilo.

---

## 5. Creare il job

1. Home di Jenkins → **New Item** (in alto a sinistra).
2. Nome: `atlas-test-framework`.
3. Tipo: **Pipeline**. → **OK**.
4. Nella pagina di configurazione, scendi fino alla sezione **Pipeline** e imposta:

| Campo | Valore |
|---|---|
| **Definition** | `Pipeline script from SCM` |
| **SCM** | `Git` |
| **Repository URL** | `https://github.com/ctoscanoEng/atlas-test-framework.git` |
| **Credentials** | `- none -` (il repository è pubblico) |
| **Branch Specifier** | `*/main` |
| **Script Path** | `Jenkinsfile` |

5. **Save**.

> **Perché "from SCM" e non "Pipeline script"?** Perché così la pipeline è versionata insieme al
> codice: se modifichi il `Jenkinsfile` e fai push, la build successiva usa la versione nuova. Una
> pipeline incollata dentro Jenkins è una pipeline che esiste in un solo posto e sparisce con la
> macchina.

---

## 6. Il primo run

Qui c'è una stranezza di Jenkins che confonde tutti: **al primo avvio non vedi "Build with
Parameters"**, ma solo **Build Now**. È normale: Jenkins non conosce ancora i parametri, perché sono
dichiarati dentro il `Jenkinsfile`, che deve prima essere letto almeno una volta.

Quindi:

1. Premi **Build Now**. La build #1 parte con i valori di default (`SUITE=regression`,
   `BROWSER=chrome`, `USE_GRID=true`).
2. Questa build **fallirà quasi certamente allo stadio del Grid o della suite**, se Docker Desktop
   non è avviato. Va benissimo: serve solo a far leggere i parametri.
3. Ricarica la pagina del job: adesso a sinistra c'è **Build with Parameters**.

Ora il run vero. Premi **Build with Parameters** e imposta:

| Parametro | Valore per il primo run |
|---|---|
| `SUITE` | `smoke` |
| `BROWSER` | `chrome` |
| `USE_GRID` | **deselezionato** |
| `SANDBOX_HOST` | lascia com'è (ignorato quando il Grid è spento) |

**Build**. Con il Grid spento, Jenkins usa il Chrome installato sul tuo Mac e non serve Docker.

Tempo atteso: 1–3 minuti la prima volta (il wrapper scarica Maven e le dipendenze dentro
`~/.jenkins`), poi circa 40 secondi.

---

## 7. Leggere il risultato

### 7.1 La Stage View

Nella pagina del job compare una griglia con una colonna per stadio e una riga per build:

```
                Checkout   Service contracts   Start the Grid   Browser suite
   #2  ✅          14s            9s               (skipped)         38s
   #1  ❌          12s            9s                 4s              ✗
```

È la vista che rende evidente **dove** si rompe la pipeline, non solo *se* si rompe.

### 7.2 I quattro posti da controllare in ogni build

| Dove | Cosa ci trovi |
|---|---|
| **Console Output** | il log completo, con l'orario su ogni riga (plugin Timestamper) |
| **Test Result** | l'elenco dei test, i falliti in cima, e il grafico storico dopo qualche build |
| **ATLAS automation report** | il report ExtentReports, con i passi e gli screenshot |
| **Build Artifacts** | screenshot, page source, log e `locator-backlog.md` scaricabili |

### 7.3 ⚠️ Il report HTML senza stile — il problema più comune

Se apri *ATLAS automation report* e vedi una pagina bianca con testo nero senza formattazione, non è
un errore del report: è la **Content Security Policy** di Jenkins, che blocca CSS e JavaScript nelle
pagine pubblicate.

Rimedio immediato — **Manage Jenkins → Script Console**, incolla ed esegui:

```groovy
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
```

Ricarica il report: adesso è formattato.

**Attenzione:** questa impostazione si perde al riavvio di Jenkins. Per renderla permanente, con
Homebrew:

```bash
brew services stop jenkins-lts
# aggiungi l'opzione alla riga di avvio del servizio
sed -i '' 's|<string>--httpListenAddress=127.0.0.1</string>|<string>--httpListenAddress=127.0.0.1</string>\
        <string>-Dhudson.model.DirectoryBrowserSupport.CSP=</string>|' \
  $(brew --prefix)/opt/jenkins-lts/homebrew.mxcl.jenkins-lts.plist
brew services start jenkins-lts
```

Se preferisci non toccare il plist, rilancia la riga dalla Script Console dopo ogni riavvio: per un
ambiente di studio va benissimo.

> Nota di sicurezza, che vale la pena sapere e saper dire a un colloquio: azzerare la CSP è
> accettabile su un Jenkins locale e personale. Su un Jenkins aziendale condiviso si preferisce una
> CSP ristretta (`sandbox; default-src 'self'; style-src 'self' 'unsafe-inline'`) invece della
> stringa vuota.

---

## 8. Il run con il Selenium Grid

Ora la configurazione più interessante: i browser girano in container, la suite gira su Jenkins.

**Prerequisito:** Docker Desktop avviato.

1. **Build with Parameters**:

| Parametro | Valore |
|---|---|
| `SUITE` | `regression` |
| `BROWSER` | `chrome` |
| `USE_GRID` | **selezionato** |
| `SANDBOX_HOST` | `host.docker.internal` |

2. Mentre gira, apri **http://localhost:4444/ui**: vedi le sessioni attive e la coda del Grid.

**Cosa succede sotto:** lo stadio *Start the Grid* esegue
`docker compose -f docker/docker-compose.grid.yml up -d --wait`; la suite si collega a
`http://localhost:4444/wd/hub`; e — questo è il punto delicato — l'applicazione sotto test, che gira
**dentro il processo Maven su Jenkins**, viene esposta su `0.0.0.0` e annunciata ai browser come
`host.docker.internal`, che su Docker Desktop è il nome con cui un container raggiunge il Mac.

```
┌── il tuo Mac ──────────────────┐        ┌── container chrome ────┐
│  Jenkins → Maven → suite       │        │  browser               │
│  SandboxServer  bind 0.0.0.0   │◀───────│  http://host.docker.internal:PORTA
└────────────────────────────────┘        └────────────────────────┘
```

Se questo passaggio non è configurato, i test falliscono tutti con un timeout sull'apertura della
pagina, e il motivo non è ovvio: il browser sta cercando l'applicazione dentro sé stesso.

3. A fine build, il blocco `post` spegne il Grid da solo (`docker compose down`). Verificalo con
   `docker ps`: non deve restare niente acceso.

---

## 9. Variante: Jenkins in Docker

Se preferisci non installare niente sul Mac:

```bash
docker network create atlas-ci

docker run -d --name jenkins --network atlas-ci \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk21

docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Poi vale tutto quanto sopra, con tre differenze:

1. **Dentro il container non c'è nessun browser.** Devi usare `USE_GRID=true` sempre, oppure limitarti
   al profilo `api`.
2. **Il client Docker non è installato** nell'immagine Jenkins: montare il socket non basta. Serve
   installarlo (`docker exec -u root jenkins apt-get update && apt-get install -y docker.io`) oppure
   avviare il Grid a mano dal Mac prima della build e impostare `USE_GRID=false` con
   `-Datlas.remote=true`.
3. **`SANDBOX_HOST` diventa `jenkins`** (il nome del container sulla rete `atlas-ci`), non
   `host.docker.internal`: sia Jenkins sia i nodi del Grid devono stare sulla stessa rete Docker.

È la configurazione più vicina alla realtà aziendale, ed è anche quella in cui si perde più tempo la
prima volta. Falla dopo aver visto una build verde con la strada A.

---

## 10. Automatizzare l'esecuzione

Nella configurazione del job, sezione **Build Triggers**:

| Opzione | Cosa fa | Quando usarla |
|---|---|---|
| **Build periodically** — `H 3 * * *` | esegue ogni notte verso le 3 | regressione notturna |
| **Poll SCM** — `H/15 * * * *` | ogni 15 minuti controlla se ci sono commit nuovi | se Jenkins non è raggiungibile da internet |
| **GitHub hook trigger** | GitHub avvisa Jenkins a ogni push | richiede un Jenkins pubblico o `ngrok` |

Su un Jenkins locale, **Poll SCM** è l'unica che funziona senza esporre la macchina. La `H` iniziale
non è un errore di battitura: dice a Jenkins di distribuire il carico invece di far partire tutti i
job allo stesso secondo.

---

## 11. Problemi noti e rimedi

| Sintomo | Causa | Rimedio |
|---|---|---|
| `./mvnw: No such file or directory` o `java: command not found` | Jenkins non eredita il `PATH` | [sezione 4](#4-dire-a-jenkins-dove-sono-java-e-docker) |
| `docker: command not found` nello stadio del Grid | idem, `docker` non è nel `PATH` di Jenkins | aggiungi `/usr/local/bin` o `/opt/homebrew/bin` |
| Non vedo *Build with Parameters* | è la prima build | lancia una volta **Build Now**, poi ricarica |
| Il report HTML è senza stile | Content Security Policy | [sezione 7.3](#73-️-il-report-html-senza-stile--il-problema-più-comune) |
| Tutti i test UI in timeout con `USE_GRID=true` | i browser non raggiungono il SUT | `SANDBOX_HOST=host.docker.internal` |
| `Cannot connect to the Docker daemon` | Docker Desktop spento | avvialo e rilancia |
| La build resta in coda per sempre | nessun esecutore libero, o una build precedente appesa | Jenkins → *Build Executor Status*, interrompi quella vecchia |
| `Permission denied` sul socket Docker (variante B) | l'utente `jenkins` non è nel gruppo docker | monta il socket e installa il client, oppure usa la strada A |
| Il Grid resta acceso a fine build | il blocco `post` non è arrivato in fondo | `docker compose -f docker/docker-compose.grid.yml down` |

---

## 12. Cosa vale la pena salvare per il portfolio

Quando hai una build verde:

1. **Screenshot della Stage View** con almeno tre build in colonna, di cui una rossa. Una pipeline
   che è sempre stata verde sembra finta; una che mostra un fallimento e poi il recupero racconta
   che è stata usata davvero.
2. **Screenshot del tab "ATLAS automation report"** dentro Jenkins — dimostra che sai integrare un
   report HTML in un orchestratore, non solo generarlo.
3. **Il grafico "Test Result Trend"** dopo cinque o sei build.

Sono tre immagini che in un colloquio valgono più di dieci minuti di racconto.
