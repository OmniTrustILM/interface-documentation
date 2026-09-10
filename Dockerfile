# build environment
FROM maven:3.9.16-eclipse-temurin-21 AS build
COPY ./ /home/app
COPY settings.xml /root/.m2/settings.xml
RUN mvn -f /home/app/pom.xml clean verify -Dmaven.compiler.proc=full

# build documentation
FROM node:alpine AS docs
RUN npm install -g @redocly/cli
COPY --from=build /home/app/openapi /home/app
COPY redocly-theme.yaml /home/app/config/redocly-theme.yaml
COPY images /home/app/images
RUN for file in /home/app/*.yaml; do redocly build-docs "$file" -o "${file/yaml/html}" --config=/home/app/config/redocly-theme.yaml; done

# production environment
FROM nginx:stable-alpine

# Patch base-image packages that the org Trivy gate flags as HIGH:
#   libcrypto3, libssl3 - CVE-2026-14456
#   libuuid             - CVE-2026-53612, CVE-2026-53613, CVE-2026-53614, CVE-2026-76642,
#                         CVE-2026-78408, CVE-2026-78409, CVE-2026-78410.
# The pinned minimums make the build fail fast if a fixed package ever goes missing.
RUN apk add --no-cache --upgrade \
        "libcrypto3>=3.5.8-r0" \
        "libssl3>=3.5.8-r0" \
        "libuuid>=2.42.3-r1"

COPY --from=build /home/app/index.html /usr/share/nginx/html/docs/index.html
COPY --from=build /home/app/style.css /usr/share/nginx/html/docs/style.css
COPY --from=build /home/app/images/ilm-logo.svg /usr/share/nginx/html/docs/images/ilm-logo.svg
COPY --from=docs /home/app /usr/share/nginx/html/docs
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
