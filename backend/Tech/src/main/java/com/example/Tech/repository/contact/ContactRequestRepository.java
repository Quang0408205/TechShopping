package com.example.Tech.repository.contact;

import com.example.Tech.entity.contact.ContactRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long>,
        JpaSpecificationExecutor<ContactRequest> {

    /** The admin list shows the sender account and the handler's name. */
    @Override
    @EntityGraph(attributePaths = {"user", "handledBy"})
    Page<ContactRequest> findAll(Specification<ContactRequest> spec, Pageable pageable);
}
