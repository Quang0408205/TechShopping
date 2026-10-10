package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServiceRequestImageRepository extends JpaRepository<ServiceRequestImage, Long> {

    List<ServiceRequestImage> findAllByWarrantyRequest_IdInOrderByDisplayOrderAsc(Collection<Long> ids);

    List<ServiceRequestImage> findAllByMaintenanceRequest_IdInOrderByDisplayOrderAsc(Collection<Long> ids);

    List<ServiceRequestImage> findAllByReturnRequest_IdInOrderByDisplayOrderAsc(Collection<Long> ids);
}
