package com.driver.repository;

import com.driver.model.Country;
import com.driver.model.CountryName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {

    Country findByCountryName(CountryName countryName);
}
