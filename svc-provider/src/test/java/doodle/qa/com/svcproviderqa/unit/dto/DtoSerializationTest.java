package doodle.qa.com.svcproviderqa.unit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.util.TestDataFactory;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

/** Tests for DTO serialization to ensure that certain fields are excluded from JSON responses. */
@JsonTest
class DtoSerializationTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should exclude version field from CalendarDto JSON serialization")
  void testCalendarDtoVersionFieldExclusion() throws Exception {
    // Given
    UUID id = UUID.randomUUID();
    CalendarDto calendarDto =
        TestDataFactory.createCalendarDto(id, "Test Calendar", "Test Description", 123L);

    // When
    String json = objectMapper.writeValueAsString(calendarDto);

    // Then
    assertThat(json).contains("\"id\":\"" + id + "\"");
    assertThat(json).contains("\"name\":\"Test Calendar\"");
    assertThat(json).contains("\"description\":\"Test Description\"");
    assertThat(json).doesNotContain("\"version\":123");
  }

  @Test
  @DisplayName("Should exclude version field from EventDto JSON serialization")
  void testEventDtoVersionFieldExclusion() throws Exception {
    // Given
    UUID id = UUID.randomUUID();
    UUID calendarId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    EventDto eventDto =
        TestDataFactory.createEventDto(
            id,
            "Test Event",
            "Test Description",
            now,
            now.plusHours(1),
            "Test Location",
            calendarId,
            456L);

    // When
    String json = objectMapper.writeValueAsString(eventDto);

    // Then
    assertThat(json).contains("\"id\":\"" + id + "\"");
    assertThat(json).contains("\"title\":\"Test Event\"");
    assertThat(json).contains("\"description\":\"Test Description\"");
    assertThat(json).contains("\"calendarId\":\"" + calendarId + "\"");
    assertThat(json).doesNotContain("\"version\":456");
  }
}
