package com.example.Tech.entity.employee;

import com.example.Tech.entity.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * One employee's assignment to one store for a period. At most one active (is_active = true,
 * end_date = null) assignment per employee at a time (uq_employee_assignments_active). Closing an
 * assignment sets end_date and is_active = false; a new assignment is then inserted for the new store.
 */
@Entity
@Table(name = "employee_assignments")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "position_at_store", length = 100)
    private String positionAtStore;

    @Column(name = "is_active")
    private Boolean active = true;
}
