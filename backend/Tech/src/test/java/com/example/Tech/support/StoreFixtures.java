package com.example.Tech.support;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Store-side test data for integration tests that run inside the test's transaction (rolled back): a store, a
 * STAFF account assigned to it, and stock. Since Phase 7.6 a STAFF member only reaches the orders of their store and
 * confirming an order needs stock there.
 */
public final class StoreFixtures {

    private StoreFixtures() {
    }

    /** Open store; orders shipped to "…, Quận 5, TP. Hồ Chí Minh" are assigned to a store in that district. */
    public static Store store(EntityManager entityManager, String name, String district) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict(district);
        store.setCity("Hồ Chí Minh");
        store.setActive(true);
        entityManager.persist(store);
        return store;
    }

    /** Employee profile + current assignment of the account to the store. */
    public static void assign(EntityManager entityManager, Long userId, Store store) {
        Employee employee = new Employee();
        employee.setUser(entityManager.getReference(User.class, userId));
        employee.setEmployeeCode("TEST-" + userId);
        entityManager.persist(employee);
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployee(employee);
        assignment.setStore(store);
        assignment.setStartDate(LocalDate.now());
        assignment.setPositionAtStore("Nhân viên");
        entityManager.persist(assignment);
        entityManager.flush();
    }

    /** Works with detached Store / variant objects too (a checkout clears the persistence context). */
    public static void stock(EntityManager entityManager, Store store, ProductVariant variant, int quantity) {
        entityManager.persist(new Inventory(entityManager.getReference(Store.class, store.getId()),
                entityManager.getReference(ProductVariant.class, variant.getId()), quantity));
        entityManager.flush();
    }

    /** Current quantity read from the database (0 when the store never had the variant). */
    public static int quantity(EntityManager entityManager, Store store, ProductVariant variant) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery(
                        "select coalesce(sum(quantity), 0) from inventory where store_id = :store and variant_id = :variant")
                .setParameter("store", store.getId())
                .setParameter("variant", variant.getId())
                .getSingleResult()).intValue();
    }

    /** "TYPE:change" of an order's stock movements, oldest first. */
    public static List<String> movements(EntityManager entityManager, Long orderId) {
        entityManager.flush();
        List<?> rows = entityManager.createNativeQuery(
                        "select movement_type || ':' || quantity_change from stock_movements where order_id = :id "
                                + "order by movement_id")
                .setParameter("id", orderId)
                .getResultList();
        return rows.stream().map(Object::toString).toList();
    }
}
