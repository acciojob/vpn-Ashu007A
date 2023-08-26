package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.CountryRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Autowired
    CountryRepository countryRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{

        User user = userRepository2.findById(userId).orElseThrow(() -> new Exception("User not found"));

        if (user.getConnected()) {
            return user; // Already connected
        }

        CountryName validatedCountryName = validateCountryName(countryName);
        Country userCountry = user.getOriginalCountry();

        if (userCountry.getCountryName() == validatedCountryName) {
            return user; // Already in the requested country
        }

        ServiceProvider suitableServiceProvider = findSuitableServiceProvider(user, validatedCountryName);
        if (suitableServiceProvider == null) {
            return null; // Unable to connect
        }

        // Find the country object that matches the validated country name from the service provider's country list
        Country targetCountry = suitableServiceProvider.getCountryList().stream()
                .filter(c -> c.getCountryName() == validatedCountryName)
                .findFirst()
                .orElseThrow(() -> new Exception("Country not found in the service provider"));

        // Establish connection
        Connection connection = new Connection();
        connection.setUser(user);
        connection.setServiceProvider(suitableServiceProvider);
        connectionRepository2.save(connection);

        // Update user details
        user.setConnected(true);
        // Update the user's masked IP based on the target country name
        user.setMaskedIp(updatedMaskedIP(validatedCountryName, user.getId(), suitableServiceProvider.getId()));
        // Set the user's original country to the target country
        user.setOriginalCountry(targetCountry);
        userRepository2.save(user);

        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {

        User user = userRepository2.findById(userId).orElseThrow(() -> new Exception("User not found"));

        if (!user.getConnected()) {
            throw new Exception("Already disconnected");
        }

        // Disconnect user
        user.setConnected(false);
        user.setMaskedIp(null);
        userRepository2.save(user);

        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {

        User sender = userRepository2.findById(senderId).orElse(null);
        User receiver = userRepository2.findById(receiverId).orElse(null);

        if (sender == null || receiver == null) {
            return null; // Users not found
        }

        // Check if the sender and receiver can communicate
        if (!canCommunicate(sender, receiver.getOriginalCountry().getCountryName(), receiver.getOriginalCountry().getServiceProvider())) {
            throw new Exception("Cannot communicate with the receiver");
        }

        // Find a suitable service provider for sender
        ServiceProvider serviceProvider = findSuitableServiceProvider(sender, CountryName.valueOf(receiver.getOriginalCountry().getCountryName().toString()));
        sender.getServiceProviderList().add(serviceProvider);
        sender.setConnected(true);
        userRepository2.save(sender);
        return sender;
    }

    private CountryName validateCountryName(String countryName) throws Exception {
        try {
            return CountryName.valueOf(countryName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new Exception("Country not found");
        }
    }

    private ServiceProvider findSuitableServiceProvider(User user, CountryName countryName) {
        return user.getServiceProviderList().stream()
                .filter(sp -> sp.getUsers().stream()
                        .anyMatch(u -> u.getOriginalCountry().getCountryName() == countryName))
                .sorted(Comparator.comparing(ServiceProvider::getId))
                .findFirst()
                .orElse(null); // Use the one with the smallest ID or return null if none found
    }

    private String updatedMaskedIP(CountryName countryName, int userId, int serviceProviderId) {
        return countryName.toCode() + "." + serviceProviderId + "." + userId;
    }

    private boolean canCommunicate(User sender, CountryName receiverCountryName, ServiceProvider receiverServiceProvider) {
        CountryName senderCountryName = sender.getOriginalCountry().getCountryName();
        ServiceProvider senderServiceProvider = sender.getOriginalCountry().getServiceProvider();

        return senderCountryName == receiverCountryName ||
                (sender.getConnected() && senderServiceProvider == receiverServiceProvider);
    }
}
