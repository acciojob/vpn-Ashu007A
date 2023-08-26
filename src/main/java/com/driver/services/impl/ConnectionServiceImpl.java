package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{

        User user = userRepository2.findById(userId).orElseThrow(() -> new Exception("User not found"));

        if (user.getConnected()) {
            throw new Exception("Already connected");
        }

        CountryName validatedCountryName = validateCountryName(countryName);
        Country userCountry = user.getCountry();

        if (userCountry.getCountryName() == validatedCountryName) {
            return user; // Already in the requested country
        }

        ServiceProvider suitableServiceProvider = findSuitableServiceProvider(user, validatedCountryName);
        if (suitableServiceProvider == null) {
            throw new Exception("Unable to connect");
        }

        // Establish connection
        Connection connection = new Connection();
        connection.setUser(user);
        connection.setServiceProvider(suitableServiceProvider);
        connectionRepository2.save(connection);

        // Update user details
        user.setConnected(true);
        user.setMaskedIP(updatedMaskedIP(validatedCountryName, user.getId(), suitableServiceProvider.getId()));
        user.getCountry().setServiceProvider(suitableServiceProvider);
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
        user.setMaskedIP(null);
        userRepository2.save(user);

        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {

        User sender = userRepository2.findById(senderId).orElseThrow(() -> new Exception("Sender not found"));
        User receiver = userRepository2.findById(receiverId).orElseThrow(() -> new Exception("Receiver not found"));

        CountryName receiverCountryName = receiver.getCountry().getCountryName();
        ServiceProvider receiverServiceProvider = receiver.getCountry().getServiceProvider();

        if (canCommunicate(sender, receiverCountryName, receiverServiceProvider)) {
            return sender; // Already in a suitable state for communication
        }

        ServiceProvider suitableServiceProvider = findSuitableServiceProvider(sender, receiverCountryName);
        if (suitableServiceProvider == null) {
            throw new Exception("Cannot establish communication");
        }

        // Update sender details
        sender.setConnected(true);
        sender.setMaskedIP(updatedMaskedIP(receiverCountryName, sender.getId(), suitableServiceProvider.getId()));
        sender.getCountry().setServiceProvider(suitableServiceProvider);
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
        List<ServiceProvider> suitableProviders = new ArrayList<>();
        for (ServiceProvider serviceProvider : user.getServiceProviderList()) {
            for (User serviceProviderUser : serviceProvider.getUsers()) {
                if (serviceProviderUser.getCountry().getCountryName() == countryName) {
                    suitableProviders.add(serviceProvider);
                    break;
                }
            }
        }

        if (!suitableProviders.isEmpty()) {
            suitableProviders.sort(Comparator.comparing(ServiceProvider::getId));
            return suitableProviders.get(0); // Use the one with the smallest ID
        }

        return null;
    }

    private String updatedMaskedIP(CountryName countryName, int userId, int serviceProviderId) {
        return countryName.toCode() + "." + serviceProviderId + "." + userId;
    }

    private boolean canCommunicate(User sender, CountryName receiverCountryName, ServiceProvider receiverServiceProvider) {
        CountryName senderCountryName = sender.getCountry().getCountryName();
        ServiceProvider senderServiceProvider = sender.getCountry().getServiceProvider();

        return senderCountryName == receiverCountryName ||
                (sender.getConnected() && senderServiceProvider == receiverServiceProvider);
    }
}