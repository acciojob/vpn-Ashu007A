package com.driver.services.impl;

import com.driver.model.Country;
import com.driver.model.CountryName;
import com.driver.model.ServiceProvider;
import com.driver.model.User;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository3;
    @Autowired
    ServiceProviderRepository serviceProviderRepository3;
    @Autowired
    CountryRepository countryRepository3;

    @Override
    public User register(String username, String password, String countryName) throws Exception{

        CountryName validatedCountryName = validateCountryName(countryName);

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setConnected(false);

        // Create a new country object and set its code and name based on the validated country name
        Country userCountry = new Country();
        userCountry.setCountryName(validatedCountryName);
        userCountry.setCode(validatedCountryName.toCode());
        userCountry.setUser(user);

        // Generate a unique IP address for the user using InetAddress class
        Random random = new Random();
        int part2 = random.nextInt(256);
        int part3 = random.nextInt(256);
        int part4 = random.nextInt(256);
        // Convert the country code from a String to a byte using the getBytes method
        byte countryCodeValue = userCountry.getCode().getBytes()[0];
        byte[] ipBytes = new byte[]{countryCodeValue, (byte)part2, (byte)part3, (byte)part4};
        InetAddress originalIp = InetAddress.getByAddress(ipBytes);
        user.setOriginalIp(originalIp.getHostAddress());

        user.setOriginalCountry(userCountry);

        userRepository3.save(user);
        return user;
    }

    @Override
    public User subscribe(Integer userId, Integer serviceProviderId) throws Exception {

        User user = userRepository3.findById(userId).orElse(null);
        ServiceProvider serviceProvider = serviceProviderRepository3.findById(serviceProviderId).orElse(null);

        if (user != null && serviceProvider != null) {
            // Check if the user has already subscribed to the service provider
            if (user.getServiceProviderList().contains(serviceProvider)) {
                throw new Exception("User already subscribed to the service provider");
            }
            user.getServiceProviderList().add(serviceProvider);
            userRepository3.save(user);
        }

        return user;
    }

    private CountryName validateCountryName(String countryName) throws Exception {
        try {
            return CountryName.valueOf(countryName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new Exception("Country not found");
        }
    }
}
