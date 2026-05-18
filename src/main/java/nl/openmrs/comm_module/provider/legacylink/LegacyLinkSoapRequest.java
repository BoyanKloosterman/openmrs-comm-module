package nl.openmrs.comm_module.provider.legacylink;

public class LegacyLinkSoapRequest {

    private final String phoneNumber;
    private final String messageText;
    private final String senderIdentification;

    public LegacyLinkSoapRequest(String phoneNumber, String messageText, String senderIdentification) {
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.senderIdentification = senderIdentification;
    }

    public String toXml() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <SendSmsRequest xmlns="http://legacylink.fakecomworld.com/v1">
                  <PhoneNumber>%s</PhoneNumber>
                  <MessageText>%s</MessageText>
                  <SenderIdentification>%s</SenderIdentification>
                </SendSmsRequest>
                """.formatted(
                escapeXml(phoneNumber),
                escapeXml(messageText),
                escapeXml(senderIdentification)
        );
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}