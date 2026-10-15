package com.example.Tech.repository.store;

import com.example.Tech.entity.store.Store;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Store mapping against the real PostgreSQL test database (techshopping_test). Every test
 * is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_setsGeneratedIdAndTimestamps() {
        Store store = storeRepository.save(store("Chi nhánh Test A", true));
        entityManager.flush();
        entityManager.clear();

        Store found = storeRepository.findById(store.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Chi nhánh Test A");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void findAllByActiveTrueOrderByName_excludesInactiveAndSortsByName() {
        storeRepository.save(store("StoreRepoTest Z", true));
        storeRepository.save(store("StoreRepoTest A", true));
        storeRepository.save(store("StoreRepoTest Tạm đóng", false));
        entityManager.flush();
        entityManager.clear();

        List<Store> active = storeRepository.findAllByActiveTrueOrderByName();

        assertThat(active).extracting(Store::getName)
                .filteredOn(name -> name.startsWith("StoreRepoTest"))
                .containsExactly("StoreRepoTest A", "StoreRepoTest Z");
    }

    private static Store store(String name, boolean active) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("123 Đường Test");
        store.setCity("Hồ Chí Minh");
        store.setDistrict("Quận 1");
        store.setActive(active);
        return store;
    }
}
