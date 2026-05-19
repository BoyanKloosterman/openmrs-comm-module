# Platform 2.5.x: appointmentscheduling 2.0.0 ondersteunt t/m 2.5; op 2.7 worden module-.form URLs niet gemapt (404).
# FHIR R5 voor comm-module blijft op aparte fhir-r5-service (HAPI).
FROM openmrs/openmrs-core:2.5.14-amazoncorretto-11

USER root

# legacyui 2.0.0/2.1.0 op Maven vereisen inmiddels platform 2.7.3; 1.24.0 werkt op 2.5.14
ARG LEGACYUI_VERSION=1.24.0
ARG APPOINTMENTSCHEDULING_VERSION=2.0.0
ARG REPORTING_VERSION=1.28.0
ARG WEBSERVICES_REST_VERSION=2.36.0
ARG CALCULATION_VERSION=1.2
ARG SERIALIZATION_XSTREAM_VERSION=0.2.14
ARG HTMLWIDGETS_VERSION=1.7.2
ARG MAVEN_BASE=https://mavenrepo.openmrs.org/public/org/openmrs/module

# legacyui eerst in de lijst (andere modules gebruiken AdministrationSectionExt uit legacyui)
RUN mkdir -p /openmrs/distribution/openmrs_modules \
  && curl -fL -o /openmrs/distribution/openmrs_modules/legacyui.omod \
       "${MAVEN_BASE}/legacyui-omod/${LEGACYUI_VERSION}/legacyui-omod-${LEGACYUI_VERSION}.jar" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/serialization.xstream.omod \
       "${MAVEN_BASE}/serialization.xstream-omod/${SERIALIZATION_XSTREAM_VERSION}/serialization.xstream-omod-${SERIALIZATION_XSTREAM_VERSION}.omod" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/htmlwidgets.omod \
       "${MAVEN_BASE}/htmlwidgets-omod/${HTMLWIDGETS_VERSION}/htmlwidgets-omod-${HTMLWIDGETS_VERSION}.jar" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/calculation.omod \
       "${MAVEN_BASE}/calculation-omod/${CALCULATION_VERSION}/calculation-omod-${CALCULATION_VERSION}.jar" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/reporting.omod \
       "${MAVEN_BASE}/reporting-omod/${REPORTING_VERSION}/reporting-omod-${REPORTING_VERSION}.jar" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/webservices.rest.omod \
       "${MAVEN_BASE}/webservices.rest-omod/${WEBSERVICES_REST_VERSION}/webservices.rest-omod-${WEBSERVICES_REST_VERSION}.jar" \
  && curl -fL -o /openmrs/distribution/openmrs_modules/appointmentscheduling.omod \
       "${MAVEN_BASE}/appointmentscheduling-omod/${APPOINTMENTSCHEDULING_VERSION}/appointmentscheduling-omod-${APPOINTMENTSCHEDULING_VERSION}.jar" \
  && chmod a+r /openmrs/distribution/openmrs_modules/*.omod

USER 1001
