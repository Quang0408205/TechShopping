package com.example.Tech.service.order;

import com.example.Tech.entity.store.Store;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NearestStoreResolverTest {

    private final Store q1 = store(1, "Quận 1", "Hồ Chí Minh");
    private final Store q10 = store(2, "Quận 10", "TP. Hồ Chí Minh");
    private final Store binhThanh = store(3, "Quận Bình Thạnh", "Hồ Chí Minh");
    private final Store thuDuc = store(4, "Thủ Đức", "Thành phố Hồ Chí Minh");
    private final Store hoanKiem = store(5, "Hoàn Kiếm", "Hà Nội");
    private final Store cauGiay = store(6, "Cầu Giấy", "Hà Nội");
    private final List<Store> all = List.of(q1, q10, binhThanh, thuDuc, hoanKiem, cauGiay);

    @Test
    void districtAndCity_win() {
        assertThat(resolve("12 Nguyễn Trãi, Phường 3, Quận 10, TP. Hồ Chí Minh")).contains(q10);
        assertThat(resolve("5 Xuân Thuỷ, Cầu Giấy, Hà Nội")).contains(cauGiay);
    }

    @Test
    void quan1_doesNotMatchQuan10_andShortFormsAreExpanded() {
        assertThat(resolve("1 Lê Lợi, P.Bến Nghé, Q.1, TP.HCM")).contains(q1);
        assertThat(resolve("200 Ba Tháng Hai, Q10, tphcm")).contains(q10);
        assertThat(resolve("Phường 1, Quận 10, Sài Gòn")).contains(q10);
    }

    @Test
    void accentsCaseAndAdminWordsDoNotMatter() {
        assertThat(resolve("10 xo viet nghe tinh, binh thanh, ho chi minh")).contains(binhThanh);
        assertThat(resolve("Võ Văn Ngân, TP Thủ Đức, Hồ Chí Minh")).contains(thuDuc);
    }

    @Test
    void cityOnly_goesToTheLowestIdStoreOfThatCity() {
        assertThat(resolve("Số 3, ngõ 12, Hà Nội")).contains(hoanKiem);
        assertThat(resolve("Một nơi nào đó, Hồ Chí Minh")).contains(q1);
    }

    @Test
    void noMatchAtAll_goesToTheLowestIdStore_andNoStoreGivesEmpty() {
        assertThat(resolve("123 Đường ABC, Đà Nẵng")).contains(q1);
        assertThat(NearestStoreResolver.resolve("Quận 1", List.of())).isEmpty();
    }

    @Test
    void normalize_removesAccentsPunctuationAndExpandsShortForms() {
        assertThat(NearestStoreResolver.normalize("  Q.5, TP.HCM ")).isEqualTo("quan 5 thanh pho ho chi minh");
        assertThat(NearestStoreResolver.normalize("Đống Đa - HN")).isEqualTo("dong da ha noi");
        assertThat(NearestStoreResolver.normalize(null)).isEmpty();
    }

    private Optional<Store> resolve(String address) {
        return NearestStoreResolver.resolve(address, all);
    }

    private static Store store(int id, String district, String city) {
        Store store = new Store();
        store.setId(id);
        store.setName("Chi nhánh " + id);
        store.setDistrict(district);
        store.setCity(city);
        return store;
    }
}
