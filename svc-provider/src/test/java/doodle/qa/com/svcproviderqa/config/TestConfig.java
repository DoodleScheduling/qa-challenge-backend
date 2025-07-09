package doodle.qa.com.svcproviderqa.config;

import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Test configuration for database components. This configuration provides beans for testing with an
 * embedded H2 database.
 */
@TestConfiguration
public class TestConfig {

  /**
   * Creates an embedded H2 database for testing.
   *
   * @return A data source configured for testing
   */
  @Bean
  @Primary
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .setName("testdb-" + System.currentTimeMillis())
        .build();
  }
}
