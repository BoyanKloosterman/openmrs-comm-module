package nl.openmrs.comm_module.sync;

import nl.openmrs.comm_module.config.OpenmrsSchedulingSyncProperties;
import nl.openmrs.comm_module.messaging.fhir.OpenmrsFhirAppointmentMetadata;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/** Bouwt FHIR R5 Patient/Appointment uit OpenMRS scheduling-rijen. */
@Component
public class OpenmrsFhirResourceFactory {

    public Patient buildPatient(
            OpenmrsSchedulingAppointmentRow row, OpenmrsSchedulingSyncProperties properties) {
        Patient patient = new Patient();
        patient.setId(row.fhirPatientId());

        HumanName name = new HumanName();
        if (row.givenName() != null && !row.givenName().isBlank()) {
            name.addGiven(row.givenName().trim());
        }
        if (row.familyName() != null && !row.familyName().isBlank()) {
            name.setFamily(row.familyName().trim());
        }
        patient.addName(name);

        String phone = resolvePhone(row, properties);
        if (phone != null) {
            patient.addTelecom(
                    new ContactPoint()
                            .setSystem(ContactPoint.ContactPointSystem.PHONE)
                            .setValue(phone));
        }
        return patient;
    }

    public Appointment buildAppointment(
            OpenmrsSchedulingAppointmentRow row, OpenmrsSchedulingSyncProperties properties) {
        ZoneId zone = ZoneId.of(properties.getZoneId());
        Instant start = toInstant(row.startDate(), zone);
        Instant end = toInstant(row.endDate(), zone);

        Appointment appointment = new Appointment();
        appointment.setId(row.fhirAppointmentId());
        appointment.setStatus(OpenmrsSchedulingStatusMapper.toFhir(row.status(), row.voided()));
        appointment.setStart(Date.from(start));
        if (end != null) {
            appointment.setEnd(Date.from(end));
        }

        appointment.setSubject(new Reference("Patient/" + row.fhirPatientId()));

        if (row.appointmentTypeName() != null && !row.appointmentTypeName().isBlank()) {
            appointment.addServiceType(new CodeableReference(new CodeableConcept().setText(row.appointmentTypeName())));
        }
        // Locatie/instructies als metadata (geen Location-resource op HAPI).
        OpenmrsFhirAppointmentMetadata.applyTo(appointment, row.locationName(), row.reason());

        Appointment.AppointmentParticipantComponent patientParticipant =
                new Appointment.AppointmentParticipantComponent();
        patientParticipant.setActor(new Reference("Patient/" + row.fhirPatientId()));
        patientParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        appointment.addParticipant(patientParticipant);

        return appointment;
    }

    private static String resolvePhone(OpenmrsSchedulingAppointmentRow row, OpenmrsSchedulingSyncProperties properties) {
        if (row.phone() != null && !row.phone().isBlank()) {
            return row.phone().trim();
        }
        String fallback = properties.getFallbackPhone();
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private static Instant toInstant(LocalDateTime local, ZoneId zone) {
        if (local == null) {
            throw new IllegalArgumentException("start/end datum ontbreekt");
        }
        return local.atZone(zone).toInstant();
    }
}
