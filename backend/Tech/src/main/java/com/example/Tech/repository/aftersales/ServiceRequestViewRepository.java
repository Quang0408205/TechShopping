package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceRequestViewRepository
        extends JpaRepository<ServiceRequestView, String>, JpaSpecificationExecutor<ServiceRequestView> {
}
