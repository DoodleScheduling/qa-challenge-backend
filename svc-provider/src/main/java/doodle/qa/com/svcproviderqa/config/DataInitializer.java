package doodle.qa.com.svcproviderqa.config;

import doodle.qa.com.svcproviderqa.dto.CalendarDto;
import doodle.qa.com.svcproviderqa.dto.EventDto;
import doodle.qa.com.svcproviderqa.service.CalendarService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with sample calendars and events on application startup.
 * This component is active in all profiles except "test" to avoid interfering with tests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Don't run during tests
public class DataInitializer implements CommandLineRunner {

  private final CalendarService calendarService;

  @Override
  public void run(String... args) {
    log.info("Initializing sample calendars and events...");
    
    // Create sample calendars with events
    createWorkCalendar();
    createPersonalCalendar();
    createHolidayCalendar();
    
    log.info("Sample data initialization completed.");
  }
  
  private void createWorkCalendar() {
    LocalDateTime now = LocalDateTime.now();
    List<EventDto> workEvents = new ArrayList<>();
    
    // Add some work events
    workEvents.add(EventDto.builder()
        .title("Team Meeting")
        .description("Weekly team sync-up")
        .startTime(now.plusDays(1).withHour(10).withMinute(0))
        .endTime(now.plusDays(1).withHour(11).withMinute(0))
        .location("Conference Room A")
        .build());
        
    workEvents.add(EventDto.builder()
        .title("Project Deadline")
        .description("Final submission for Q2 project")
        .startTime(now.plusDays(5).withHour(17).withMinute(0))
        .endTime(now.plusDays(5).withHour(18).withMinute(0))
        .location("Office")
        .build());
        
    workEvents.add(EventDto.builder()
        .title("Client Presentation")
        .description("Presenting new features to the client")
        .startTime(now.plusDays(3).withHour(14).withMinute(0))
        .endTime(now.plusDays(3).withHour(15).withMinute(30))
        .location("Client Office")
        .build());
    
    CalendarDto workCalendar = CalendarDto.builder()
        .name("Work Calendar")
        .description("Professional appointments and deadlines")
        .events(workEvents)
        .build();
    
    calendarService.createCalendar(workCalendar);
    log.info("Created work calendar with {} events", workEvents.size());
  }
  
  private void createPersonalCalendar() {
    LocalDateTime now = LocalDateTime.now();
    List<EventDto> personalEvents = new ArrayList<>();
    
    // Add some personal events
    personalEvents.add(EventDto.builder()
        .title("Gym Session")
        .description("Weekly workout")
        .startTime(now.plusDays(2).withHour(18).withMinute(0))
        .endTime(now.plusDays(2).withHour(19).withMinute(30))
        .location("Fitness Center")
        .build());
        
    personalEvents.add(EventDto.builder()
        .title("Dinner with Friends")
        .description("Catching up with old friends")
        .startTime(now.plusDays(4).withHour(19).withMinute(0))
        .endTime(now.plusDays(4).withHour(22).withMinute(0))
        .location("Italian Restaurant")
        .build());
        
    personalEvents.add(EventDto.builder()
        .title("Movie Night")
        .description("Watching the latest blockbuster")
        .startTime(now.plusDays(6).withHour(20).withMinute(0))
        .endTime(now.plusDays(6).withHour(23).withMinute(0))
        .location("Cinema")
        .build());
    
    CalendarDto personalCalendar = CalendarDto.builder()
        .name("Personal Calendar")
        .description("Personal appointments and activities")
        .events(personalEvents)
        .build();
    
    calendarService.createCalendar(personalCalendar);
    log.info("Created personal calendar with {} events", personalEvents.size());
  }
  
  private void createHolidayCalendar() {
    LocalDateTime now = LocalDateTime.now();
    List<EventDto> holidayEvents = new ArrayList<>();
    
    // Add some holiday events
    holidayEvents.add(EventDto.builder()
        .title("New Year's Day")
        .description("First day of the year")
        .startTime(LocalDateTime.of(now.getYear() + 1, 1, 1, 0, 0))
        .endTime(LocalDateTime.of(now.getYear() + 1, 1, 1, 23, 59))
        .location("Worldwide")
        .build());
        
    holidayEvents.add(EventDto.builder()
        .title("Independence Day")
        .description("National holiday")
        .startTime(LocalDateTime.of(now.getYear(), 7, 4, 0, 0))
        .endTime(LocalDateTime.of(now.getYear(), 7, 4, 23, 59))
        .location("United States")
        .build());
        
    holidayEvents.add(EventDto.builder()
        .title("Christmas Day")
        .description("Christmas celebration")
        .startTime(LocalDateTime.of(now.getYear(), 12, 25, 0, 0))
        .endTime(LocalDateTime.of(now.getYear(), 12, 25, 23, 59))
        .location("Worldwide")
        .build());
    
    CalendarDto holidayCalendar = CalendarDto.builder()
        .name("Holiday Calendar")
        .description("Public holidays and celebrations")
        .events(holidayEvents)
        .build();
    
    calendarService.createCalendar(holidayCalendar);
    log.info("Created holiday calendar with {} events", holidayEvents.size());
  }
}