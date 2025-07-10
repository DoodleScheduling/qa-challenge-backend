package doodle.qa.com.svccalendarqa.repository;

import doodle.qa.com.svccalendarqa.entity.Meeting;
import doodle.qa.com.svccalendarqa.entity.UserCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    /**
     * Find meetings by user calendar and time range.
     *
     * @param userCalendar the user calendar
     * @param from the start time
     * @param to the end time
     * @param pageable the pagination information
     * @return a page of meetings
     */
    Page<Meeting> findByUserCalendarAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            UserCalendar userCalendar, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * Find meetings by user calendar and meeting ID.
     *
     * @param userCalendar the user calendar
     * @param id the meeting ID
     * @return an optional meeting
     */
    Optional<Meeting> findByUserCalendarAndId(UserCalendar userCalendar, UUID id);

    /**
     * Find all meetings that overlap with the given time range for a specific user calendar.
     *
     * @param userCalendar the user calendar
     * @param from the start time
     * @param to the end time
     * @return a list of meetings
     */
    @Query("SELECT m FROM Meeting m WHERE m.userCalendar = :userCalendar AND " +
           "((m.startTime <= :to AND m.endTime >= :from))")
    List<Meeting> findOverlappingMeetingsByUserCalendar(
            @Param("userCalendar") UserCalendar userCalendar,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
