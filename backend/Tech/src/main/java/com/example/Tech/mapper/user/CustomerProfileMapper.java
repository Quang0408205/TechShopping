package com.example.Tech.mapper.user;

import com.example.Tech.dto.request.user.CustomerProfileUpdateRequest;
import com.example.Tech.dto.response.user.CustomerProfileResponse;
import com.example.Tech.entity.user.CustomerProfile;
import org.springframework.stereotype.Component;

/**
 * Loyalty points and total spent are never set from requests.
 */
@Component
public class CustomerProfileMapper {

    public void updateEntity(CustomerProfile profile, CustomerProfileUpdateRequest request) {
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setAddress(UserMapper.trimToNull(request.address()));
        profile.setCity(UserMapper.trimToNull(request.city()));
        profile.setDistrict(UserMapper.trimToNull(request.district()));
        profile.setWard(UserMapper.trimToNull(request.ward()));
        profile.setPostalCode(UserMapper.trimToNull(request.postalCode()));
        profile.setDefaultShippingAddress(UserMapper.trimToNull(request.defaultShippingAddress()));
    }

    public CustomerProfileResponse toResponse(CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getCustomerId(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getAddress(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getWard(),
                profile.getPostalCode(),
                profile.getDefaultShippingAddress(),
                profile.getLoyaltyPoints(),
                profile.getTotalSpent(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
