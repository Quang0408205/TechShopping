/* ================= TRANG KHUYẾN NGHỊ ================= */

/*
 * Bố cục 5 khu vực lấy từ bản frontend mới, nhưng dữ liệu là SẢN PHẨM THẬT
 * từ GET /api/v1/products (không dùng catalogue giả mock-data.js).
 *
 * Chưa có hệ thống khuyến nghị (Phase 8, dịch vụ AI riêng), nên mỗi khu vực
 * dùng một quy tắc đơn giản trên API catalogue:
 *   - Gợi ý dành cho bạn: 8 sản phẩm mới nhất;
 *   - Sản phẩm tương tự:  cùng danh mục + thương hiệu với sản phẩm "neo";
 *   - Phổ biến nhất:      4 sản phẩm đầu tiên (chưa có dữ liệu bán / lượt xem);
 *   - Thường được mua kèm: phụ kiện;
 *   - Có thể nâng cấp lên: cùng danh mục, giá cao hơn sản phẩm neo.
 * Sản phẩm neo = sản phẩm đầu tiên của cửa hàng (giống "Lựa chọn hàng đầu").
 * Khi có API khuyến nghị thật, chỉ cần thay các hàm load* bên dưới.
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
            anchor: fetchProductPage("sort=id,asc&size=1").then(function (page) {
                return page.content[0] || null;
            })
        };


        RECOMMENDATION_SECTIONS.forEach(function (section) {
            renderRecommendationSection(section, context);
        });

    }
);


/* GET /products?isActive=true&<query> (công khai, không gửi token) */

function fetchProductPage(query) {

    return apiRequest("/products?isActive=true&page=0&" + query);

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

        const products = await section.load(context, section.size);

        const categories = await context.categories;


        if (section.sectionId) {

            /* Khu "nâng cấp" chỉ hiện khi thật sự có sản phẩm */

            if (products.length === 0) {
                return;
            }

            document.getElementById(section.sectionId).hidden = false;

        }


        renderProductGrid(grid, products, categories.byId);

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


/* ================= QUY TẮC TỪNG KHU VỰC ================= */

async function loadPersonalProducts(context, size) {

    return (await fetchProductPage("sort=id,desc&size=" + size)).content;

}


async function loadPopularProducts(context, size) {

    return (await fetchProductPage("sort=id,asc&size=" + size)).content;

}


async function loadSimilarProducts(context, size) {

    const anchor = await context.anchor;

    if (!anchor) {
        return [];
    }


    const notAnchor = function (product) {
        return product.id !== anchor.id;
    };


    /* Cùng danh mục + thương hiệu trước, thiếu thì bù bằng cùng danh mục */

    let products = [];

    if (anchor.brandId) {

        products = (await fetchProductPage(
            "categoryId=" + anchor.categoryId + "&brandId=" + anchor.brandId + "&sort=id,asc&size=" + (size + 1)
        )).content.filter(notAnchor);

    }


    if (products.length < size) {

        const ids = products.map(function (product) { return product.id; });

        const sameCategory = (await fetchProductPage(
            "categoryId=" + anchor.categoryId + "&sort=id,asc&size=" + (size * 2)
        )).content.filter(function (product) {
            return notAnchor(product) && ids.indexOf(product.id) === -1;
        });

        products = products.concat(sameCategory);

    }


    return products.slice(0, size);

}


async function loadBundleProducts(context, size) {

    const categories = await context.categories;

    const accessoryId = categories.bySlug[PRODUCT_CATEGORY_FILTERS.accessory];

    if (!accessoryId) {
        return [];
    }

    return (await fetchProductPage("categoryId=" + accessoryId + "&sort=id,asc&size=" + size)).content;

}


async function loadUpgradeProducts(context, size) {

    const anchor = await context.anchor;

    const price = anchor ? getDisplayPrice(anchor) : 0;

    if (!anchor || price <= 0) {
        return [];
    }

    return (await fetchProductPage(
        "categoryId=" + anchor.categoryId + "&minPrice=" + Math.floor(price + 1) + "&sort=basePrice,asc&size=" + size
    )).content;

}
