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
        Country userCountry = user.getOriginalCountry();

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
        user.setMaskedIp(updatedMaskedIP(validatedCountryName, user.getId(), suitableServiceProvider.getId()));
        userCountry.setServiceProvider(suitableServiceProvider); // Update userCountry instead of user.getOriginalCountry()
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

//        User sender = userRepository2.findById(senderId).orElseThrow(() -> new Exception("Sender not found"));
//        User receiver = userRepository2.findById(receiverId).orElseThrow(() -> new Exception("Receiver not found"));
//
//        CountryName receiverCountryName = receiver.getOriginalCountry().getCountryName();
//        ServiceProvider receiverServiceProvider = receiver.getOriginalCountry().getServiceProvider();
//
//        if (canCommunicate(sender, receiverCountryName, receiverServiceProvider)) {
//            return sender; // Already in a suitable state for communication
//        }
//
//        ServiceProvider suitableServiceProvider = findSuitableServiceProvider(sender, receiverCountryName);
//        if (suitableServiceProvider == null) {
//            throw new Exception("");
////            throw new Exception("Cannot establish communication");
//        }
//
//        // Update sender details
//        sender.setConnected(true);
//        sender.setMaskedIp(updatedMaskedIP(receiverCountryName, sender.getId(), suitableServiceProvider.getId()));
//        sender.getOriginalCountry().setServiceProvider(suitableServiceProvider);
//        userRepository2.save(sender);
//
//        return sender;

        User sender = userRepository2.findById(senderId).orElse(null);
        User receiver = userRepository2.findById(receiverId).orElse(null);

        if (sender == null || receiver == null) {
            throw new Exception("Users not found");
        }

        if (receiver.getOriginalCountry().getCountryName() == sender.getOriginalCountry().getCountryName()) {
            // Users are in the same country, they can communicate
            return sender;
        } else {
            // Find a suitable service provider for sender
            ServiceProvider serviceProvider = findSuitableServiceProvider(sender, CountryName.valueOf(receiver.getOriginalCountry().getCountryName().toString()));
            sender.getServiceProviderList().add(serviceProvider);
            sender.setConnected(true);
            userRepository2.save(sender);
            return sender;
        }
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
                if (serviceProviderUser.getOriginalCountry().getCountryName() == countryName) {
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
        CountryName senderCountryName = sender.getOriginalCountry().getCountryName();
        ServiceProvider senderServiceProvider = sender.getOriginalCountry().getServiceProvider();

        return senderCountryName == receiverCountryName ||
                (sender.getConnected() && senderServiceProvider == receiverServiceProvider);
    }
}
