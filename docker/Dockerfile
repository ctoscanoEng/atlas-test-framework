# =============================================================================
#  Test runner image
#
#  Contains the suite and a JDK — and no browser at all: browsers live in the
#  Grid containers. That separation is what keeps this image small and makes
#  "which Chrome version did that run use?" an answerable question.
#
#     docker build -f docker/Dockerfile -t atlas-tests .
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS dependencies

WORKDIR /atlas

# Dependencies are resolved in their own layer: as long as pom.xml does not
# change, a code edit rebuilds in seconds instead of re-downloading the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21

WORKDIR /atlas
COPY --from=dependencies /root/.m2 /root/.m2
COPY pom.xml .
COPY src ./src

# The suite runs against the Grid, and the application under test is served by
# the framework itself from inside this very container — hence 0.0.0.0.
ENV ATLAS_REMOTE=true \
    ATLAS_GRID_URL=http://selenium-hub:4444/wd/hub \
    ATLAS_HEADLESS=true \
    ATLAS_SANDBOX_BIND_ADDRESS=0.0.0.0 \
    ATLAS_SANDBOX_ADVERTISED_HOST=atlas-tests

ENTRYPOINT ["mvn", "-B", "test"]
CMD ["-Pregression"]
