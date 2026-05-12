# Platform 2.7.x; FHIR2 voor R4 REST; Legacy UI voorkomt "no user interface module" (login/admin)
FROM openmrs/openmrs-core:2.7.9-amazoncorretto-11

USER root

RUN mkdir -p /openmrs/distribution/openmrs_modules \
  && curl -fL -o /openmrs/distribution/openmrs_modules/fhir2.omod \
    "https://addons.openmrs.org/api/v1/addon/org.openmrs.module.openmrs-fhir2-module/2.8.0/download" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/legacyui.omod \
    "https://mavenrepo.openmrs.org/public/org/openmrs/module/legacyui-omod/2.1.0/legacyui-omod-2.1.0.jar" \
  && chmod a+r /openmrs/distribution/openmrs_modules/*.omod

USER 1001
