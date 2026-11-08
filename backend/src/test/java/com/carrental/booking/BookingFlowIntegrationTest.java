package com.carrental.booking;

import com.carrental.car.Car;
import com.carrental.location.Location;
import com.carrental.support.AbstractIntegrationTest;
import com.carrental.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PICKUP = "2026-11-10T09:00:00Z";
    private static final String DROPOFF = "2026-11-13T09:00:00Z";

    @Test
    void customerSearchesQuotesAndBooksCar_andOverlappingBookingIsRejected() throws Exception {
        Location location = createLocation();
        Car car = createCar(location, "40.00");
        String alice = eligibleCustomerToken();
        String bob = eligibleCustomerToken();

        mvc.perform(get("/api/v1/cars")
                        .param("locationId", location.getId().toString())
                        .param("pickupAt", PICKUP).param("dropoffAt", DROPOFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(car.getId().toString()))
                .andExpect(jsonPath("$.content[0].name").value("Toyota Yaris"))
                .andExpect(jsonPath("$.content[0].vin").doesNotExist());

        mvc.perform(get("/api/v1/cars/{id}/quote", car.getId()).param("pickupAt", PICKUP).param("dropoffAt", DROPOFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.rentalDays").value(3))
                .andExpect(jsonPath("$.totalPrice").value(120.00));

        book(alice, car, PICKUP, DROPOFF)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.reference").value(startsWith("BK-")))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.price.totalPrice").value(120.00))
                .andExpect(jsonPath("$.price.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.pickupLocation.id").value(location.getId().toString()));

        book(bob, car, "2026-11-12T09:00:00Z", "2026-11-15T09:00:00Z")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAR_UNAVAILABLE"));

        mvc.perform(get("/api/v1/cars")
                        .param("locationId", location.getId().toString())
                        .param("pickupAt", "2026-11-12T09:00:00Z").param("dropoffAt", "2026-11-15T09:00:00Z"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // Periods are half-open: a booking may start exactly when the previous one ends.
        book(bob, car, DROPOFF, "2026-11-15T09:00:00Z").andExpect(status().isCreated());
    }

    @Test
    void bookingRequiresCompleteDriverProfile() throws Exception {
        Car car = createCar(createLocation(), "40.00");
        String token = registerCustomer().get("accessToken").asText();

        book(token, car, PICKUP, DROPOFF)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DRIVER_NOT_ELIGIBLE"));
    }

    @Test
    void bookingValidationErrorsAreReportedPerField() throws Exception {
        String token = eligibleCustomerToken();

        mvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupAt\":\"2026-11-13T09:00:00Z\",\"dropoffAt\":\"2026-11-10T09:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'carId')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'periodValid')]").exists());
    }

    @Test
    void customersCannotSeeEachOthersBookings() throws Exception {
        Car car = createCar(createLocation(), "40.00");
        String alice = eligibleCustomerToken();
        String bob = eligibleCustomerToken();
        String bookingId = body(book(alice, car, PICKUP, DROPOFF).andReturn()).get("id").asText();

        mvc.perform(get("/api/v1/bookings/{id}", bookingId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/v1/bookings").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/bookings").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void fullRentalLifecycle_withLateReturnAndPayment() throws Exception {
        Location pickupBranch = createLocation();
        Location returnBranch = createLocation();
        Car car = createCar(pickupBranch, "50.00");
        Account customerAccount = eligibleCustomer();
        Account agentAccount = createStaff(Role.AGENT);
        Account adminAccount = createStaff(Role.ADMIN);
        String customer = customerAccount.token();

        Map<String, Object> request = bookingBody(car, "2026-11-10T09:00:00Z", "2026-11-12T09:00:00Z");
        request.put("dropoffLocationId", returnBranch.getId().toString());
        String bookingId = body(mvc.perform(post("/api/v1/bookings")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price.totalPrice").value(100.00))
                .andReturn()).get("id").asText();

        // Days pass, so earlier access tokens have expired: everyone logs in again.
        clock.set(Instant.parse("2026-11-10T08:30:00Z"));
        customer = relogin(customerAccount);
        String agent = relogin(agentAccount);
        mvc.perform(post("/api/v1/bookings/{id}/pickup", bookingId)
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mileage\":10100}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bookings/{id}/pickup", bookingId)
                        .header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mileage\":10100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.handover.pickupMileage").value(10100));

        mvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", bearer(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleet.rentedNow").value(1));

        // Three hours late: beyond the 59-minute grace period, so one extra day is charged.
        clock.set(Instant.parse("2026-11-12T12:00:00Z"));
        agent = relogin(agentAccount);
        customer = relogin(customerAccount);
        String admin = relogin(adminAccount);
        mvc.perform(post("/api/v1/bookings/{id}/return", bookingId)
                        .header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileage\":10500,\"extraCharges\":15.50,\"notes\":\"Fuel tank half empty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.price.lateFee").value(50.00))
                .andExpect(jsonPath("$.price.extraCharges").value(15.50))
                .andExpect(jsonPath("$.price.totalPrice").value(165.50))
                .andExpect(jsonPath("$.price.paymentStatus").value("UNPAID"));

        mvc.perform(post("/api/v1/bookings/{id}/payments", bookingId)
                        .header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":165.50,\"method\":\"CARD\",\"reference\":\"TERM-0042\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price.amountPaid").value(165.50))
                .andExpect(jsonPath("$.price.balanceDue").value(0.00))
                .andExpect(jsonPath("$.price.paymentStatus").value("PAID"));

        mvc.perform(get("/api/v1/bookings/{id}/payments", bookingId).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(165.50))
                .andExpect(jsonPath("$[0].type").value("CHARGE"));

        // The car now stands at the return branch with the new odometer reading.
        mvc.perform(get("/api/v1/admin/cars/{id}", car.getId()).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.mileage").value(10500))
                .andExpect(jsonPath("$.location.id").value(returnBranch.getId().toString()));

        mvc.perform(get("/api/v1/admin/dashboard/revenue")
                        .header("Authorization", bearer(admin))
                        .param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(165.50));
        mvc.perform(get("/api/v1/admin/dashboard/top-cars")
                        .header("Authorization", bearer(admin))
                        .param("from", "2026-11-01").param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].carId").value(car.getId().toString()))
                .andExpect(jsonPath("$[0].rentals").value(1));
    }

    @Test
    void cancellation_isFreeUntilWindow_thenOnlyStaffCanCancelWithFee() throws Exception {
        Car car = createCar(createLocation(), "45.00");
        Account customerAccount = eligibleCustomer();
        Account agentAccount = createStaff(Role.AGENT);
        String customer = customerAccount.token();

        String early = body(book(customer, car, "2026-11-20T09:00:00Z", "2026-11-22T09:00:00Z").andReturn())
                .get("id").asText();
        mvc.perform(post("/api/v1/bookings/{id}/cancel", early).header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.price.totalPrice").value(0.00));

        String late = body(book(customer, car, PICKUP, DROPOFF).andReturn()).get("id").asText();
        clock.set(Instant.parse("2026-11-09T10:00:00Z")); // 23 hours before pickup
        customer = relogin(customerAccount);
        String agent = relogin(agentAccount);

        mvc.perform(post("/api/v1/bookings/{id}/cancel", late).header("Authorization", bearer(customer)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        mvc.perform(post("/api/v1/bookings/{id}/cancel", late)
                        .header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Customer called the desk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.cancellationFee").value(45.00))
                .andExpect(jsonPath("$.price.totalPrice").value(45.00));

        mvc.perform(post("/api/v1/bookings/{id}/cancel", late).header("Authorization", bearer(agent)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void rescheduleKeepsAgreedRateAndChecksAvailability() throws Exception {
        Car car = createCar(createLocation(), "40.00");
        String alice = eligibleCustomerToken();
        String bob = eligibleCustomerToken();
        String aliceBooking = body(book(alice, car, PICKUP, DROPOFF).andReturn()).get("id").asText();
        book(bob, car, "2026-11-20T09:00:00Z", "2026-11-22T09:00:00Z").andExpect(status().isCreated());

        mvc.perform(post("/api/v1/bookings/{id}/reschedule", aliceBooking)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupAt\":\"2026-11-19T09:00:00Z\",\"dropoffAt\":\"2026-11-21T09:00:00Z\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAR_UNAVAILABLE"));

        mvc.perform(post("/api/v1/bookings/{id}/reschedule", aliceBooking)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupAt\":\"2026-11-10T09:00:00Z\",\"dropoffAt\":\"2026-11-15T09:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.rentalDays").value(5))
                .andExpect(jsonPath("$.price.totalPrice").value(200.00));
    }

    @Test
    void carWithOpenBookingsCannotBeTakenOutOfService() throws Exception {
        Car car = createCar(createLocation(), "40.00");
        String admin = createStaffAndLogin(Role.ADMIN);
        book(eligibleCustomerToken(), car, PICKUP, DROPOFF).andExpect(status().isCreated());

        mvc.perform(patch("/api/v1/admin/cars/{id}/status", car.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void concurrentBookingsOfSameCar_exactlyOneSucceeds() throws Exception {
        Car car = createCar(createLocation(), "40.00");
        int contenders = 6;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            tokens.add(eligibleCustomerToken());
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (String token : tokens) {
                Callable<Integer> attempt = () -> {
                    start.await();
                    return book(token, car, PICKUP, DROPOFF).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(attempt));
            }
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
            assertThat(statuses).filteredOn(s -> s == 409).hasSize(contenders - 1);
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions book(String token, Car car, String pickupAt,
                               String dropoffAt) throws Exception {
        return mvc.perform(post("/api/v1/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bookingBody(car, pickupAt, dropoffAt))));
    }

    private static Map<String, Object> bookingBody(Car car, String pickupAt, String dropoffAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("carId", car.getId().toString());
        body.put("pickupAt", pickupAt);
        body.put("dropoffAt", dropoffAt);
        return body;
    }
}
