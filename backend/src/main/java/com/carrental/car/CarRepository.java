package com.carrental.car;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarRepository extends JpaRepository<Car, UUID>, JpaSpecificationExecutor<Car> {

    /** Loads the location in the same query, avoiding one extra query per car in a page. */
    @Override
    @EntityGraph(attributePaths = "location")
    Page<Car> findAll(Specification<Car> spec, Pageable pageable);

    @EntityGraph(attributePaths = "location")
    Optional<Car> findWithLocationById(UUID id);

    /**
     * SELECT ... FOR UPDATE: concurrent bookings of the same car wait for each other, so the
     * "is it free?" check and the insert happen as one step.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Car c join fetch c.location where c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByVin(String vin);

    boolean existsByVinAndIdNot(String vin, UUID id);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

    @Query("select c.status as status, count(c) as count from Car c group by c.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        CarStatus getStatus();

        long getCount();
    }
}
