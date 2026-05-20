package nl.openmrs.comm_module.testgui.dto;

public record CreateTestAppointmentRequest(String phone, String patientName, Boolean runPollAfter) {}
