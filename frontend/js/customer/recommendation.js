/* ================= TRANG KHUYẾN NGHỊ ================= */

/*
 * 5 khu vực, dữ liệu từ hệ thống khuyến nghị AI qua POST /api/v1/recommendations:
 *   - Gợi ý dành cho bạn:   PERSONAL (Hybrid CBF + ALS; khách chưa đăng nhập → phổ biến + mới);
 *   - Sản phẩm tương tự:    SIMILAR của sản phẩm "neo";
 *   - Phổ biến nhất:        PERSONAL hỏi như khách (luôn là danh sách phổ biến);
 *   - Thường được mua kèm:  CROSS_SELL của sản phẩm neo;
 *   - Có thể nâng cấp lên:  UPSELL của sản phẩm neo.
 * Sản phẩm neo = sản phẩm khách xem gần nhất (product-detail.js lưu lại), chưa xem gì thì lấy
 * sản phẩm phổ biến nhất. AI_Service ngừng thì backend trả danh sách dự phòng (source = "FALLBACK").
 */

const RECOMMENDATION_SECTIONS = [
    { key: "personal", gridId: "personalGrid", size: 8, load: loadPersonalProducts },
    { key: "similar", gridId: "similarGrid", size: 4, load: loadSimilarProducts },
    { key: "popular", gridId: "popularGrid", size: 4, load: loadPopularProducts },
    { key: "bundle", gridId: "bundleGrid", size: 4, load: loadBundleProducts },
    { key: "upgrade", gridId: "upgradeGrid", size: 4, load: loadUpgradeProducts, sectionId: "upgradeSection" }
];


document.addEventListener(
    "DOMContentLoaded",
    function () {

        const context = {
            categories: fetchCategoryMaps().catch(function () {
                return { byId: {}, bySlug: {} };
            }),
            anchor: findAnchorProductId()
        };


        RECOMMENDATION_SECTIONS.forEach(function (section) {
            renderRecommendationSection(section, context);
        });

    }
);


async function findAnchorProductId() {

    try {

        const saved = Number(localStorage.getItem(LAST_VIEWED_PRODUCT_KEY));

        if (saved > 0) {
            return saved;
        }

    } catch (error) {
        /* bỏ qua */
    }

    const popular = await fetchRecommendations("PERSONAL", null, 1, { guest: true });

    return popular.items.length ? popular.items[0].product.id : null;

}


async function renderRecommendationSection(section, context) {

    const grid = document.getElementById(section.gridId);

    if (!grid) {
        return;
    }


    if (!section.sectionId) {
        grid.innerHTML = skeletonProductGrid(4);
    }


    try {

        const items = await section.load(context, section.size);

        const categories = await context.categories;


        if (section.sectionId) {

            /* Khu "nâng cấp" chỉ hiện khi thật sự có sản phẩm */

            if (items.length === 0) {
                return;
            }

            document.getElementById(section.sectionId).hidden = false;

        }


        if (items.length === 0) {
            grid.innerHTML = emptyStateHtml("Chưa có gợi ý", "Hãy xem thêm vài sản phẩm để hệ thống hiểu bạn hơn.");
            return;
        }

        renderRecommendationGrid(grid, items, categories.byId);

    } catch (error) {

        if (section.sectionId) {
            return;
        }

        const retryId = section.key + "RetryBtn";

        grid.innerHTML = errorStateHtml(getErrorMessage(error), retryId);

        document.getElementById(retryId)
            .addEventListener("click", function () {
                renderRecommendationSection(section, context);
            });

    }

}


/* ================= NGUỒN DỮ LIỆU TỪNG KHU VỰC ================= */

async function loadPersonalProducts(context, size) {

    return (await fetchRecommendations("PERSONAL", null, size)).items;

}


async function loadPopularProducts(context, size) {

    return (await fetchRecommendations("PERSONAL", null, size, { guest: true })).items;

}


async function loadAnchored(type, context, size) {

    const anchor = await context.anchor;

    return anchor ? (await fetchRecommendations(type, anchor, size)).items : [];

}


function loadSimilarProducts(context, size) {

    return loadAnchored("SIMILAR", context, size);

}


function loadBundleProducts(context, size) {

    return loadAnchored("CROSS_SELL", context, size);

}


function loadUpgradeProducts(context, size) {

    return loadAnchored("UPSELL", context, size);

}
