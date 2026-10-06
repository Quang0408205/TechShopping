package com.example.Tech.repository.store;

import com.example.Tech.entity.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Integer>, JpaSpecificationExecutor<Store> {

    List<Store> findAllByActiveTrueOrderByName();
}
