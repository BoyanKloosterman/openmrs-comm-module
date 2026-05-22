package nl.openmrs.comm_module.common.encoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for handling character encoding and timezone support for diverse
 * character sets.
 * 
 * Supports UTF-8 encoding for all message content and validates special
 * characters
 * from non-Latin alphabets (Cyrillic, Arabic, Chinese, etc.).
 * 
 * Implements US-011: Diverse karaktersets ondersteunen
 */
@Service
public class CharacterEncodingService {

  private static final Logger logger = LoggerFactory.getLogger(CharacterEncodingService.class);

  /**
   * Validates that a string contains valid UTF-8 characters and can be properly
   * encoded.
   * 
   * @param text The text to validate
   * @return true if text is valid UTF-8, false otherwise
   */
  public boolean isValidUtf8(String text) {
    if (text == null) {
      return true;
    }

    try {
      // Convert to UTF-8 bytes and back - if it round-trips, it's valid UTF-8
      String encoded = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
      return encoded.equals(text);
    } catch (Exception e) {
      logger.warn("UTF-8 validation failed for text: {}", text, e);
      return false;
    }
  }

  /**
   * Ensures a string is properly encoded as UTF-8.
   * 
   * @param text The text to encode
   * @return UTF-8 encoded string, or empty string if input is null
   */
  public String ensureUtf8Encoding(String text) {
    if (text == null) {
      return "";
    }

    try {
      byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
      return new String(utf8Bytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      logger.error("Failed to ensure UTF-8 encoding for text: {}", text, e);
      return text;
    }
  }

  /**
   * Detects if a string contains non-Latin characters (e.g., Cyrillic, Arabic,
   * CJK, etc.).
   * 
   * @param text The text to check
   * @return true if text contains non-Latin characters, false otherwise
   */
  public boolean containsNonLatinCharacters(String text) {
    if (text == null) {
      return false;
    }

    for (char c : text.toCharArray()) {
      // Check if character is outside Latin-1 Supplement range (0x0000-0x00FF)
      if (c > 0xFF) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates that a string containing special characters can be safely
   * transmitted.
   * This includes checking for valid Unicode sequences and proper UTF-8 encoding.
   * 
   * @param text The text to validate
   * @return true if special characters are valid and transmissible, false
   *         otherwise
   */
  public boolean areSpecialCharactersValid(String text) {
    if (text == null) {
      return true;
    }

    try {
      // Validate UTF-8 encoding
      if (!isValidUtf8(text)) {
        logger.warn("Text contains invalid UTF-8 sequences: {}", text);
        return false;
      }

      // Check for valid Unicode code points
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        // Reject control characters (except common ones like newline, tab)
        if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
          logger.warn("Text contains invalid control character: {}", (int) c);
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      logger.error("Error validating special characters: {}", text, e);
      return false;
    }
  }

  /**
   * Converts a UTC instant to the organization's local timezone.
   * 
   * @param instant      The UTC instant to convert
   * @param timezoneName The IANA timezone name (e.g., "Europe/Amsterdam",
   *                     "Asia/Tokyo")
   * @return Formatted local datetime string, or original instant string if
   *         conversion fails
   */
  public String convertToLocalTimezone(Instant instant, String timezoneName) {
    if (instant == null || timezoneName == null) {
      return instant != null ? instant.toString() : null;
    }

    try {
      ZoneId zoneId = ZoneId.of(timezoneName);
      ZonedDateTime zdt = instant.atZone(zoneId);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss z");
      return formatter.format(zdt);
    } catch (Exception e) {
      logger.warn("Failed to convert instant to timezone {}: {}", timezoneName, e.getMessage());
      return instant.toString();
    }
  }

  /**
   * Adds timezone information to a message.
   * 
   * @param message      The base message content
   * @param timezoneName The IANA timezone name
   * @return Message with timezone information appended
   */
  public String addTimezoneInfo(String message, String timezoneName) {
    if (message == null) {
      return "";
    }

    if (timezoneName == null || timezoneName.isBlank()) {
      timezoneName = "Europe/Amsterdam";
    }

    try {
      return message + " (Timezone: " + timezoneName + ")";
    } catch (Exception e) {
      logger.error("Error adding timezone info: {}", e.getMessage());
      return message;
    }
  }

  /**
   * Validates a timezone name against IANA timezone database.
   * 
   * @param timezoneName The timezone name to validate
   * @return true if valid timezone, false otherwise
   */
  public boolean isValidTimezone(String timezoneName) {
    if (timezoneName == null || timezoneName.isBlank()) {
      return false;
    }

    try {
      ZoneId.of(timezoneName);
      return true;
    } catch (Exception e) {
      logger.warn("Invalid timezone name: {}", timezoneName);
      return false;
    }
  }
}
