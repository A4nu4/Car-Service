package com.carrental.customer;

import com.carrental.common.exception.ApiException;
import com.carrental.customer.dto.CustomerDetailResponse;
import com.carrental.customer.dto.DriverProfileRequest;
import com.carrental.customer.dto.DriverProfileResponse;
import com.carrental.user.Role;
import com.carrental.user.User;
import com.carrental.user.UserRepository;
import com.carrental.user.UserSpecifications;
import com.carrental.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerProfileRepository profiles;
    private final UserRepository users;

    public CustomerService(CustomerProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    public CustomerProfile createEmptyProfile(User customer) {
        return profiles.save(new CustomerProfile(customer));
    }

    @Transactional(readOnly = true)
    public DriverProfileResponse getDriverProfile(UUID userId) {
        return DriverProfileResponse.from(findProfile(userId));
    }

    public DriverProfileResponse updateDriverProfile(UUID userId, DriverProfileRequest request) {
        CustomerProfile profile = findProfile(userId);
        profile.update(request.dateOfBirth(), request.driverLicenseNumber().trim(), request.driverLicenseCountry(),
                request.driverLicenseExpiry(), trimToNull(request.addressLine()), trimToNull(request.city()),
                trimToNull(request.postalCode()), request.country());
        return DriverProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public CustomerProfile findProfile(UUID userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Customer profile for user", userId));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listCustomers(String search, Pageable pageable) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.hasRoleIn(List.of(Role.CUSTOMER)),
                UserSpecifications.matchesText(search));
        return users.findAll(spec, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomer(UUID userId) {
        User user = users.findById(userId)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> ApiException.notFound("Customer", userId));
        return new CustomerDetailResponse(UserResponse.from(user), DriverProfileResponse.from(findProfile(userId)));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
