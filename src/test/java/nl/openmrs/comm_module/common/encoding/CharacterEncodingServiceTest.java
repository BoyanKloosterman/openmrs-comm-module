package nl.openmrs.comm_module.common.encoding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CharacterEncodingService - US-011: Diverse karaktersets
 * ondersteunen
 */
class CharacterEncodingServiceTest {

  private CharacterEncodingService encodingService;

  @BeforeEach
  void setUp() {
    encodingService = new CharacterEncodingService();
  }

  // ==================== UTF-8 Validation Tests ====================

  @Test
  void isValidUtf8_withNullInput_shouldReturnTrue() {
    assertTrue(encodingService.isValidUtf8(null));
  }

  @Test
  void isValidUtf8_withLatinText_shouldReturnTrue() {
    assertTrue(encodingService.isValidUtf8("Hello World"));
  }

  @Test
  void isValidUtf8_withCyrillicText_shouldReturnTrue() {
    // Russian text: "Привет мир" (Hello world)
    assertTrue(encodingService.isValidUtf8("Привет мир"));
  }

  @Test
  void isValidUtf8_withArabicText_shouldReturnTrue() {
    // Arabic text: "مرحبا العالم" (Hello world)
    assertTrue(encodingService.isValidUtf8("مرحبا العالم"));
  }

  @Test
  void isValidUtf8_withChineseText_shouldReturnTrue() {
    // Chinese text: "你好世界" (Hello world)
    assertTrue(encodingService.isValidUtf8("你好世界"));
  }

  @Test
  void isValidUtf8_withJapaneseText_shouldReturnTrue() {
    // Japanese text: "こんにちは世界" (Hello world)
    assertTrue(encodingService.isValidUtf8("こんにちは世界"));
  }

  @Test
  void isValidUtf8_withKoreanText_shouldReturnTrue() {
    // Korean text: "안녕하세요 세계" (Hello world)
    assertTrue(encodingService.isValidUtf8("안녕하세요 세계"));
  }

  @Test
  void isValidUtf8_withMixedText_shouldReturnTrue() {
    // Mixed: English, Russian, Arabic
    assertTrue(encodingService.isValidUtf8("Hello Привет مرحبا"));
  }

  @Test
  void isValidUtf8_withEmptyString_shouldReturnTrue() {
    assertTrue(encodingService.isValidUtf8(""));
  }

  @Test
  void isValidUtf8_withSpecialCharacters_shouldReturnTrue() {
    assertTrue(encodingService.isValidUtf8("©®™€¥£¢"));
  }

  // ==================== UTF-8 Encoding Tests ====================

  @Test
  void ensureUtf8Encoding_withNullInput_shouldReturnEmptyString() {
    assertEquals("", encodingService.ensureUtf8Encoding(null));
  }

  @Test
  void ensureUtf8Encoding_withLatinText_shouldMaintainText() {
    String input = "Hello World";
    assertEquals(input, encodingService.ensureUtf8Encoding(input));
  }

  @Test
  void ensureUtf8Encoding_withCyrillicText_shouldMaintainText() {
    String input = "Привет мир";
    assertEquals(input, encodingService.ensureUtf8Encoding(input));
  }

  @Test
  void ensureUtf8Encoding_withArabicText_shouldMaintainText() {
    String input = "مرحبا العالم";
    assertEquals(input, encodingService.ensureUtf8Encoding(input));
  }

  @Test
  void ensureUtf8Encoding_withChineseText_shouldMaintainText() {
    String input = "你好世界";
    assertEquals(input, encodingService.ensureUtf8Encoding(input));
  }

  // ==================== Non-Latin Characters Detection Tests
  // ====================

  @Test
  void containsNonLatinCharacters_withNullInput_shouldReturnFalse() {
    assertFalse(encodingService.containsNonLatinCharacters(null));
  }

  @Test
  void containsNonLatinCharacters_withLatinOnly_shouldReturnFalse() {
    assertFalse(encodingService.containsNonLatinCharacters("Hello World 123"));
  }

  @Test
  void containsNonLatinCharacters_withCyrillic_shouldReturnTrue() {
    assertTrue(encodingService.containsNonLatinCharacters("Привет"));
  }

  @Test
  void containsNonLatinCharacters_withArabic_shouldReturnTrue() {
    assertTrue(encodingService.containsNonLatinCharacters("مرحبا"));
  }

  @Test
  void containsNonLatinCharacters_withChinese_shouldReturnTrue() {
    assertTrue(encodingService.containsNonLatinCharacters("你好"));
  }

  @Test
  void containsNonLatinCharacters_withJapanese_shouldReturnTrue() {
    assertTrue(encodingService.containsNonLatinCharacters("こんにちは"));
  }

  @Test
  void containsNonLatinCharacters_withMixed_shouldReturnTrue() {
    assertTrue(encodingService.containsNonLatinCharacters("Hello 你好 Привет"));
  }

  @Test
  void containsNonLatinCharacters_withEmptyString_shouldReturnFalse() {
    assertFalse(encodingService.containsNonLatinCharacters(""));
  }

  // ==================== Special Characters Validation Tests ====================

  @Test
  void areSpecialCharactersValid_withNullInput_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid(null));
  }

  @Test
  void areSpecialCharactersValid_withPlainText_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Hello World"));
  }

  @Test
  void areSpecialCharactersValid_withCyrillic_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Привет мир"));
  }

  @Test
  void areSpecialCharactersValid_withArabic_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("مرحبا العالم"));
  }

  @Test
  void areSpecialCharactersValid_withChinese_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("你好世界"));
  }

  @Test
  void areSpecialCharactersValid_withSymbols_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Test@#$%^&*()_+-=[]{}|;:',.<>?"));
  }

  @Test
  void areSpecialCharactersValid_withNewlines_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Line 1\nLine 2"));
  }

  @Test
  void areSpecialCharactersValid_withTabs_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Column1\tColumn2"));
  }

  @Test
  void areSpecialCharactersValid_withMixedSpecialChars_shouldReturnTrue() {
    assertTrue(encodingService.areSpecialCharactersValid("Price: €100, Russian: Привет"));
  }

  // ==================== Timezone Validation Tests ====================

  @Test
  void isValidTimezone_withNullInput_shouldReturnFalse() {
    assertFalse(encodingService.isValidTimezone(null));
  }

  @Test
  void isValidTimezone_withEmptyInput_shouldReturnFalse() {
    assertFalse(encodingService.isValidTimezone(""));
  }

  @Test
  void isValidTimezone_withValidTimezone_shouldReturnTrue() {
    assertTrue(encodingService.isValidTimezone("Europe/Amsterdam"));
  }

  @Test
  void isValidTimezone_withAnotherValidTimezone_shouldReturnTrue() {
    assertTrue(encodingService.isValidTimezone("America/New_York"));
  }

  @Test
  void isValidTimezone_withAsiaTimezone_shouldReturnTrue() {
    assertTrue(encodingService.isValidTimezone("Asia/Tokyo"));
  }

  @Test
  void isValidTimezone_withUTCTimezone_shouldReturnTrue() {
    assertTrue(encodingService.isValidTimezone("UTC"));
  }

  @Test
  void isValidTimezone_withInvalidTimezone_shouldReturnFalse() {
    assertFalse(encodingService.isValidTimezone("Invalid/Timezone"));
  }

  // ==================== Timezone Conversion Tests ====================

  @Test
  void convertToLocalTimezone_withNullInstant_shouldReturnNull() {
    assertNull(encodingService.convertToLocalTimezone(null, "Europe/Amsterdam"));
  }

  @Test
  void convertToLocalTimezone_withNullTimezone_shouldReturnInstantString() {
    var instant = java.time.Instant.now();
    String result = encodingService.convertToLocalTimezone(instant, null);
    assertNotNull(result);
    assertEquals(instant.toString(), result);
  }

  @Test
  void convertToLocalTimezone_withValidInputs_shouldReturnFormattedString() {
    var instant = java.time.Instant.parse("2026-05-22T12:00:00Z");
    String result = encodingService.convertToLocalTimezone(instant, "Europe/Amsterdam");
    assertNotNull(result);
    assertTrue(result.contains("2026-05-22"));
    assertTrue(result.contains("14:00")); // UTC+2 in summer
  }

  @Test
  void convertToLocalTimezone_withDifferentTimezone_shouldShowCorrectOffset() {
    var instant = java.time.Instant.parse("2026-05-22T12:00:00Z");
    String result = encodingService.convertToLocalTimezone(instant, "America/New_York");
    assertNotNull(result);
    assertTrue(result.contains("2026-05-22"));
    assertTrue(result.contains("08:00")); // UTC-4 in summer
  }

  @Test
  void convertToLocalTimezone_withInvalidTimezone_shouldFallbackToInstantString() {
    var instant = java.time.Instant.now();
    String result = encodingService.convertToLocalTimezone(instant, "Invalid/Timezone");
    assertEquals(instant.toString(), result);
  }

  // ==================== Add Timezone Info Tests ====================

  @Test
  void addTimezoneInfo_withNullMessage_shouldReturnEmptyString() {
    assertEquals("", encodingService.addTimezoneInfo(null, "Europe/Amsterdam"));
  }

  @Test
  void addTimezoneInfo_withMessage_shouldAppendTimezone() {
    String result = encodingService.addTimezoneInfo("Appointment reminder", "Europe/Amsterdam");
    assertEquals("Appointment reminder (Timezone: Europe/Amsterdam)", result);
  }

  @Test
  void addTimezoneInfo_withNullTimezone_shouldUseDefault() {
    String result = encodingService.addTimezoneInfo("Message", null);
    assertEquals("Message (Timezone: Europe/Amsterdam)", result);
  }

  @Test
  void addTimezoneInfo_withBlankTimezone_shouldUseDefault() {
    String result = encodingService.addTimezoneInfo("Message", "   ");
    assertEquals("Message (Timezone: Europe/Amsterdam)", result);
  }

  @Test
  void addTimezoneInfo_withDifferentTimezone_shouldAppendIt() {
    String result = encodingService.addTimezoneInfo("Meeting", "Asia/Tokyo");
    assertEquals("Meeting (Timezone: Asia/Tokyo)", result);
  }

  // ==================== Integration Tests ====================

  @Test
  void integration_multipleLanguagesInMessage_shouldValidateCorrectly() {
    String message = "English | Русский | العربية | 中文 | 日本語 | 한국어";

    assertTrue(encodingService.isValidUtf8(message));
    assertTrue(encodingService.containsNonLatinCharacters(message));
    assertTrue(encodingService.areSpecialCharactersValid(message));

    String encoded = encodingService.ensureUtf8Encoding(message);
    assertEquals(message, encoded);
  }

  @Test
  void integration_messageWithTimezoneInfo_shouldHandleCorrectly() {
    String message = "Afspraak: 14:00 CET (Привет)";
    String timezone = "Europe/Amsterdam";

    assertTrue(encodingService.isValidUtf8(message));
    assertTrue(encodingService.isValidTimezone(timezone));

    String messageWithTZ = encodingService.addTimezoneInfo(message, timezone);
    assertTrue(messageWithTZ.contains("Привет"));
    assertTrue(messageWithTZ.contains("Europe/Amsterdam"));
  }
}
