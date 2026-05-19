package nl.openmrs.comm_module.messaging.fhir;

import nl.openmrs.comm_module.messaging.fhir.dto.AppointmentPollDto;
import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.CodeableReference;
import org.hl7.fhir.r5.model.Reference;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Mapt FHIR R5 {@link Appointment} naar {@link AppointmentPollDto}.
 */
@Component
public class AppointmentFhirMapper {

    private static final String PATIENT_TYPE = "Patient";
    private static final String LOCATION_TYPE = "Location";

    // Statussen die we niet meer willen meenemen in herinneringen.
    private static final Set<String> VOIDED_STATUSES = Set.of(
            "cancelled",
            "noshow",
            "entered-in-error");

    public Optional<AppointmentPollDto> map(Appointment appointment) {
        if (appointment == null || !appointment.hasId()) {
            return Optional.empty();
        }
        String appointmentId = appointment.getIdElement().getIdPart();
        String patientId = resolvePatientId(appointment).orElse(null);
        if (patientId == null || patientId.isBlank()) {
            return Optional.empty();
        }
        Instant start = resolveStart(appointment);
        if (start == null) {
            return Optional.empty();
        }
        String locationLabel = resolveLocationLabel(appointment);
        String typeLabel = resolveTypeLabel(appointment);
        boolean voided = isVoidedStatus(appointment.getStatus());
        return Optional.of(new AppointmentPollDto(
                appointmentId,
                appointmentId,
                patientId,
                start,
                locationLabel,
                typeLabel,
                voided));
    }

    private static Optional<String> resolvePatientId(Appointment appointment) {
        if (appointment.hasSubject()) {
            Optional<String> fromSubject = referenceId(appointment.getSubject(), PATIENT_TYPE);
            if (fromSubject.isPresent()) {
                return fromSubject;
            }
        }
        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            if (!participant.hasActor()) {
                continue;
            }
            Optional<String> id = referenceId(participant.getActor(), PATIENT_TYPE);
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> referenceId(Reference reference, String resourceType) {
        if (reference == null || !reference.hasReference()) {
            return Optional.empty();
        }
        String type = reference.getReferenceElement().getResourceType();
        if (!resourceType.equals(type)) {
            return Optional.empty();
        }
        String idPart = reference.getReferenceElement().getIdPart();
        return idPart == null || idPart.isBlank() ? Optional.empty() : Optional.of(idPart);
    }

    private static Instant resolveStart(Appointment appointment) {
        if (!appointment.hasStart()) {
            return null;
        }
        Date start = appointment.getStart();
        return start == null ? null : start.toInstant();
    }

    private static String resolveLocationLabel(Appointment appointment) {
        String fromOpenmrs = OpenmrsFhirAppointmentMetadata.readLocationDisplay(appointment);
        if (fromOpenmrs != null) {
            return fromOpenmrs;
        }
        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            if (!participant.hasActor()) {
                continue;
            }
            Reference actor = participant.getActor();
            if (actor.hasDisplay() && !actor.getDisplay().isBlank()) {
                return actor.getDisplay().trim();
            }
            Optional<String> id = referenceId(actor, LOCATION_TYPE);
            if (id.isPresent()) {
                return id.get();
            }
        }
        return null;
    }

    private static String resolveTypeLabel(Appointment appointment) {
        if (appointment.hasServiceType()) {
            for (CodeableReference serviceType : appointment.getServiceType()) {
                String text = codeableText(serviceType.getConcept());
                if (text != null) {
                    return text;
                }
            }
        }
        return codeableText(appointment.getAppointmentType());
    }

    private static String codeableText(CodeableConcept concept) {
        if (concept == null) {
            return null;
        }
        if (concept.hasText()) {
            return concept.getText();
        }
        if (concept.hasCoding() && concept.getCodingFirstRep().hasDisplay()) {
            return concept.getCodingFirstRep().getDisplay();
        }
        return null;
    }

    private static boolean isVoidedStatus(Appointment.AppointmentStatus status) {
        if (status == null) {
            return false;
        }
        return VOIDED_STATUSES.contains(status.toCode().toLowerCase(Locale.ROOT));
    }
}
