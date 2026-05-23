package nl.openmrs.comm_module.messaging.fhir;

import nl.openmrs.comm_module.messaging.fhir.dto.PatientPollDto;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.StringType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PatientFhirMapper {

    /** Mapt FHIR R5 Patient naar poll-DTO; leeg zonder bruikbare id. */
    public Optional<PatientPollDto> mapPatient(Patient patient) {
        if (patient == null || !patient.hasId()) {
            return Optional.empty();
        }
        String id = patient.getIdElement().getIdPart();
        String displayName = resolveDisplayName(patient);
        String phone = firstPhone(patient);
        return Optional.of(new PatientPollDto(id, displayName, phone));
    }

    private static String resolveDisplayName(Patient patient) {
        if (!patient.hasName()) {
            return null;
        }
        HumanName n = patient.getNameFirstRep();
        if (n.hasText()) {
            return n.getText();
        }
        if (n.hasFamily()) {
            String given = n.getGiven().stream()
                    .map(StringType::getValue)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(" "));
            String family = n.getFamily();
            if (!given.isEmpty()) {
                return given + " " + family;
            }
            return family;
        }
        return null;
    }

    /** Eerste telefoon- of sms-waarde voor notificaties. */
    private static String firstPhone(Patient patient) {
        for (ContactPoint cp : patient.getTelecom()) {
            if (!cp.hasValue()) {
                continue;
            }
            ContactPoint.ContactPointSystem sys = cp.getSystem();
            if (sys == ContactPoint.ContactPointSystem.PHONE
                    || sys == ContactPoint.ContactPointSystem.SMS
                    || sys == ContactPoint.ContactPointSystem.OTHER) {
                return cp.getValue();
            }
        }
        return null;
    }
}
