package com.fastturtle.redbusschemadesign.repositories;

import com.fastturtle.redbusschemadesign.models.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Integer> {

    @Query("SELECT p FROM Travel t JOIN t.passengers p WHERE t.travelDate = :travelDate")
    Optional<List<Passenger>> findPassengersByTravelDate(@Param("travelDate") LocalDate travelDate);
}
