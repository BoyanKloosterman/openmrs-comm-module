# Platform 2.7.x; Legacy UI voor login/admin. FHIR R5 loopt via aparte fhir-r5-service (HAPI).
FROM openmrs/openmrs-core:2.7.9-amazoncorretto-11

USER root

RUN mkdir -p /openmrs/distribution/openmrs_modules \
  && curl -fL -o /openmrs/distribution/openmrs_modules/legacyui.omod \
    "https://mavenrepo.openmrs.org/public/org/openmrs/module/legacyui-omod/2.1.0/legacyui-omod-2.1.0.jar" \
  && chmod a+r /openmrs/distribution/openmrs_modules/*.omod

USER 1001
