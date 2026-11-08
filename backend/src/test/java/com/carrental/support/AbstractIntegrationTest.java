package com.carrental.support;

import com.carrental.car.Car;
import com.carrental.car.CarCategory;
import com.carrental.car.CarRepository;
import com.carrental.car.FuelType;
import com.carrental.car.Transmission;
import com.carrental.location.Location;
import com.carrental.location.LocationRepository;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application against a real PostgreSQL, because the double-booking guarantee
 * relies on a PostgreSQL exclusion constraint that an in-memory database cannot reproduce.
 * <p>
 * The database is a Testcontainers container when Docker is available. Without Docker, set
 * TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD to an existing database. The schema is wiped
 * and re-migrated on startup. If neither is available these tests are skipped, not failed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.TestConfig.class)
@RequiresDatabase
public abstract class AbstractIntegrationTest {

    protected static final Instant START_TIME = Instant.parse("2026-11-02T08:00:00Z");
    protected static final String PASSWORD = "Passw0rd!";

    private static final String EXTERNAL_DB_URL = System.getenv("TEST_DB_URL");
    private static PostgreSQLContainer<?> container;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected LocationRepository locationRepository;
    @Autowired
    protected CarRepository carRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    public static boolean databaseAvailable() {
        if (EXTERNAL_DB_URL != null) {
            return true;
        }
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (EXTERNAL_DB_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
            return;
        }
        synchronized (AbstractIntegrationTest.class) {
            if (container == null) {
                container = new PostgreSQLContainer<>("postgres:17-alpine");
                container.start();
            }
        }
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @BeforeEach
    void resetClock() {
        clock.set(START_TIME);
    }

    // ------------------------------------------------------------------ fixtures

    protected Location createLocation() {
        return locationRepository.save(new Location("Branch " + UUID.randomUUID(), "Main street 1", "Berlin",
                "10115", "DE", null, "Mon-Sun 08:00-20:00"));
    }

    protected Car createCar(Location location, String pricePerDay) {
        Car.CarDetails details = new Car.CarDetails("Toyota", "Yaris", 2024, CarCategory.ECONOMY,
                Transmission.AUTOMATIC, FuelType.HYBRID, 5, new BigDecimal(pricePerDay), null);
        return carRepository.save(new Car(details, randomVin(), "T-" + randomDigits(8), 10_000, location));
    }

    /** Credentials of a test user, so tests that move the clock past token expiry can log in again. */
    protected record Account(String email, String token) {
    }

    protected Account createStaff(Role role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "Staff", "Member", null, role));
        return new Account(email, login(email, PASSWORD).get("accessToken").asText());
    }

    protected String createStaffAndLogin(Role role) throws Exception {
        return createStaff(role).token();
    }

    protected String relogin(Account account) throws Exception {
        return login(account.email(), PASSWORD).get("accessToken").asText();
    }

    /** Registers a customer and returns the auth response (accessToken, refreshToken, user). */
    protected JsonNode registerCustomer() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", PASSWORD,
                                "firstName", "Ada", "lastName", "Lovelace"))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Registers a customer with a complete driver profile and returns their access token. */
    protected String eligibleCustomerToken() throws Exception {
        return eligibleCustomer().token();
    }

    protected Account eligibleCustomer() throws Exception {
        JsonNode auth = registerCustomer();
        String token = auth.get("accessToken").asText();
        mvc.perform(put("/api/v1/account/driver-profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dateOfBirth", LocalDate.of(1990, 5, 17).toString(),
                                "driverLicenseNumber", "B072RRE2I55",
                                "driverLicenseCountry", "DE",
                                "driverLicenseExpiry", LocalDate.of(2034, 1, 1).toString()))))
                .andExpect(status().isOk());
        return new Account(auth.get("user").get("email").asText(), token);
    }

    protected JsonNode login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String randomVin() {
        String alphabet = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
        StringBuilder vin = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            vin.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return vin.toString();
    }

    private static String randomDigits(int length) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < length; i++) {
            digits.append(ThreadLocalRandom.current().nextInt(10));
        }
        return digits.toString();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START_TIME);
        }

        /** Start every test run from an empty schema, so leftovers from earlier runs cannot interfere. */
        @Bean
        FlywayMigrationStrategy cleanMigrateStrategy() {
            return flyway -> {
                flyway.clean();
                flyway.migrate();
            };
        }
    }
}
