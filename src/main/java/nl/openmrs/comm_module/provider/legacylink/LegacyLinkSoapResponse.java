package nl.openmrs.comm_module.provider.legacylink;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class LegacyLinkSoapResponse {

    private final int statusCode;
    private final String statusMessage;
    private final String messageReference;
    private final String timestamp;

    public LegacyLinkSoapResponse(int statusCode, String statusMessage, String messageReference, String timestamp) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.messageReference = messageReference;
        this.timestamp = timestamp;
    }

    public static LegacyLinkSoapResponse fromXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            int statusCode = Integer.parseInt(getText(document, "StatusCode"));

            return new LegacyLinkSoapResponse(
                    statusCode,
                    getText(document, "StatusMessage"),
                    getText(document, "MessageReference"),
                    getText(document, "Timestamp")
            );
        } catch (Exception exception) {
            throw new LegacyLinkApiException("Could not parse LegacyLink XML response: " + exception.getMessage(), exception);
        }
    }

    private static String getText(Document document, String tagName) {
        var nodes = document.getElementsByTagNameNS("*", tagName);

        if (nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent();
    }

    public boolean isSuccessful() {
        return statusCode == 200;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getMessageReference() {
        return messageReference;
    }

    public String getTimestamp() {
        return timestamp;
    }
}