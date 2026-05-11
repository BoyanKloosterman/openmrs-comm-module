# Platform 2.7.x; FHIR2-omod nodig want alleen core-WAR heeft geen FHIR REST
FROM openmrs/openmrs-core:2.7.9-amazoncorretto-11

USER root

RUN mkdir -p /openmrs/distribution/openmrs_modules \
  && curl -fL -o /openmrs/distribution/openmrs_modules/fhir2.omod \
    "https://addons.openmrs.org/api/v1/addon/org.openmrs.module.openmrs-fhir2-module/2.8.0/download" \
  && chmod a+r /openmrs/distribution/openmrs_modules/fhir2.omod

USER 1001
