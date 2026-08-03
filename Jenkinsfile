/*
 * =============================================================================
 *  ATLAS — declarative pipeline
 *
 *  The same three stages as the GitHub workflow, expressed for a Jenkins
 *  controller: the framework never assumes a particular CI product, it only
 *  assumes environment variables and exit codes.
 *
 *  Requires the plugins: Pipeline, HTML Publisher, TestNG Results, Timestamper.
 * =============================================================================
 */
pipeline {

    agent any

    parameters {
        choice(name: 'SUITE', choices: ['regression', 'smoke', 'api', 'bdd', 'cross-browser'],
               description: 'Maven profile to execute')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'], description: 'Target browser')
        booleanParam(name: 'USE_GRID', defaultValue: true,
                     description: 'Run the browsers on the dockerised Selenium Grid')
        string(name: 'SANDBOX_HOST', defaultValue: 'host.docker.internal',
               description: '''Hostname the containerised browsers use to reach the application \
under test, which is served by the build itself. Docker Desktop (macOS/Windows): host.docker.internal. \
Linux: the IP of the docker0 bridge, usually 172.17.0.1. Ignored when USE_GRID is off.''')
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
    }

    environment {
        ATLAS_HEADLESS = 'true'
        ATLAS_BROWSER  = "${params.BROWSER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh './mvnw -B -q -DskipTests test-compile'
            }
        }

        stage('Service contracts') {
            steps {
                // Seconds, no browser: the cheapest possible way to fail a bad build.
                sh './mvnw -B test -Papi'
            }
        }

        stage('Start the Grid') {
            when { expression { params.USE_GRID } }
            steps {
                sh 'docker compose -f docker/docker-compose.grid.yml up -d --wait'
            }
        }

        stage('Browser suite') {
            steps {
                script {
                    // The application under test runs inside this build. When the
                    // browsers are containers they cannot reach 127.0.0.1 of the
                    // agent, so the server must listen on every interface and
                    // advertise a hostname those containers can resolve. With local
                    // browsers the opposite is true: the loopback is the only
                    // address that certainly works.
                    def bindAddress    = params.USE_GRID ? '0.0.0.0' : '127.0.0.1'
                    def advertisedHost = params.USE_GRID ? params.SANDBOX_HOST : '127.0.0.1'
                    def gridFlags      = params.USE_GRID
                            ? '-Pgrid -Datlas.gridUrl=http://localhost:4444/wd/hub'
                            : ''

                    withEnv(["ATLAS_SANDBOX_BIND_ADDRESS=${bindAddress}",
                             "ATLAS_SANDBOX_ADVERTISED_HOST=${advertisedHost}"]) {
                        sh "./mvnw -B test -P${params.SUITE} ${gridFlags}"
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/junitreports/*.xml'

            publishHTML(target: [
                    reportDir           : 'target/atlas-report',
                    reportFiles         : 'index.html',
                    reportName          : 'ATLAS automation report',
                    keepAll             : true,
                    alwaysLinkToLastBuild: true,
                    allowMissing        : true
            ])

            // The locator backlog is the artefact that keeps self-healing honest:
            // every element that survived on a fallback is technical debt.
            archiveArtifacts artifacts: 'target/atlas-report/**', allowEmptyArchive: true, fingerprint: true

            script {
                if (params.USE_GRID) {
                    sh 'docker compose -f docker/docker-compose.grid.yml down --remove-orphans || true'
                }
            }
        }
        unstable {
            echo 'Tests failed — see the ATLAS report attached to this build.'
        }
    }
}
