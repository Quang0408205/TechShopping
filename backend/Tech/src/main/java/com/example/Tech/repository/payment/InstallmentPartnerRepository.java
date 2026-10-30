package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.InstallmentPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentPartnerRepository extends JpaRepository<InstallmentPartner, String> {

    List<InstallmentPartner> findAllByOrderByDisplayOrderAscCodeAsc();

    List<InstallmentPartner> findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
}
