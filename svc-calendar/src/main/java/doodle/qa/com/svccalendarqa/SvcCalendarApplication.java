package doodle.qa.com.svccalendarqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableTransactionManagement
public class SvcCalendarApplication {

  public static void main(String[] args) {
    SpringApplication.run(SvcCalendarApplication.class, args);
  }
}
