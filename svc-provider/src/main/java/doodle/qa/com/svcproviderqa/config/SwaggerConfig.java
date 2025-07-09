package doodle.qa.com.svcproviderqa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI providerServiceOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Calendar Provider Service API")
                .description("API for managing calendars and events")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("Doodle QA Team")
                        .email("qa@doodle.com")
                        .url("https://doodle.com"))
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(List.of(new Server().url("/").description("Default Server URL")));
  }
}
