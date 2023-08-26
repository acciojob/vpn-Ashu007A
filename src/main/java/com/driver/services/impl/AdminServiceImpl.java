package com.driver.services.impl;

import com.driver.model.Admin;
import com.driver.model.Country;
import com.driver.model.CountryName;
import com.driver.model.ServiceProvider;
import com.driver.repository.AdminRepository;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    AdminRepository adminRepository1;

    @Autowired
    ServiceProviderRepository serviceProviderRepository1;

    @Autowired
    CountryRepository countryRepository1;

    @Override
    public Admin register(String username, String password) {

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(password);
        return adminRepository1.save(admin);
    }

    @Override
    public Admin addServiceProvider(int adminId, String providerName) {

        Admin admin = adminRepository1.findById(adminId).orElse(null);
        if (admin != null) {
            ServiceProvider serviceProvider = new ServiceProvider();
            serviceProvider.setName(providerName);
            serviceProvider.setAdmin(admin);
            serviceProviderRepository1.save(serviceProvider);
            admin.getServiceProviders().add(serviceProvider);
            adminRepository1.save(admin);
        }
        return admin;
    }

    @Override
    public ServiceProvider addCountry(int serviceProviderId, String countryName) throws Exception{

        ServiceProvider serviceProvider = serviceProviderRepository1.findById(serviceProviderId)
                .orElseThrow(() -> new Exception("Service Provider not found"));

        CountryName validatedCountryName = validateCountryName(countryName);
        Country country = new Country();
        country.setCountryName(validatedCountryName);
        country.setServiceProvider(serviceProvider);

        serviceProvider.getCountryList().add(country);

        return serviceProviderRepository1.save(serviceProvider);
    }
    private CountryName validateCountryName(String countryName) throws Exception {
        try {
            return CountryName.valueOf(countryName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new Exception("Country not found: " + countryName);
        }
    }
}
