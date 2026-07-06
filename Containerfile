# Builds a Keycloak image with the Lightbridge provider jars baked in.
# Assemble the jars first:  ./gradlew :dist:collectProviders
# Then:                     buildah build -f Containerfile -t lightbridge-keycloak .

ARG KEYCLOAK_VERSION=26.6.4

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} AS builder
COPY dist/build/providers/*.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}
COPY --from=builder /opt/keycloak/ /opt/keycloak/

LABEL org.opencontainers.image.source="https://github.com/adorsys-gis/lightbridge-keycloak-spi"
LABEL org.opencontainers.image.description="Keycloak with the Lightbridge token-orchestration SPI baked in"
LABEL org.opencontainers.image.licenses="MIT"

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
