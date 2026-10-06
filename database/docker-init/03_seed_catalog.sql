-- =====================================================================
-- LEGACY DUMP: không còn được Docker nạp tự động.
-- Dữ liệu này dùng phiên bản schema cũ, không khớp database/techshopping.sql hiện tại.
-- Tạo database mới bằng schema hiện tại rồi dùng Raw_data/import_catalog.py để nạp CSV.
-- Không chạy file này lên database hiện tại.
-- =====================================================================
--
-- TechShopping: dữ liệu catalogue mẫu (seed)
-- 877 sản phẩm crawl từ Thế Giới Di Động (Raw_data/tgdd_products_cleaned.csv):
-- danh mục, thương hiệu, sản phẩm, phiên bản, ảnh, thuộc tính.
-- KHÔNG chứa user / mật khẩu / token. Đã bỏ dữ liệu thử nghiệm (E2E).
--
-- File được giữ để tham khảo dữ liệu cũ; Docker Compose hiện không mount file này.
-- Đừng dùng lệnh dump bên dưới để nạp vào schema hiện tại.
-- =====================================================================

--
-- PostgreSQL database dump
--

\restrict kFDbZwiYJcdJhcE1piSXDkdOaIDjLzzTwdZx0XLIOHXacLPA56CEWN14eNf58qV

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: attributes; Type: TABLE DATA; Schema: public; Owner: -
--

SET SESSION AUTHORIZATION DEFAULT;

ALTER TABLE public.attributes DISABLE TRIGGER ALL;

COPY public.attributes (attribute_id, name, description, attribute_type) FROM stdin;
1	Màu sắc	Màu sắc của phiên bản	color
2	RAM	Dung lượng RAM	memory
3	Bộ nhớ trong	Dung lượng lưu trữ	storage
\.


ALTER TABLE public.attributes ENABLE TRIGGER ALL;

--
-- Data for Name: attribute_values; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.attribute_values DISABLE TRIGGER ALL;

COPY public.attribute_values (value_id, attribute_id, value) FROM stdin;
1	1	Bạc
2	1	Cam
3	1	Cam nhạt
4	1	Cam Vũ Trụ
5	1	Đen
6	1	Đen bóng
7	1	Đen - Đỏ
8	1	Đen - Tím
9	1	Đen - Vàng
10	1	Đen - Vàng đồng
11	1	Đen - Xám
12	1	Đỏ
13	1	Đỏ Burgundy
14	1	Ghi đen
15	1	Hồng
16	1	Hồng nhạt
17	1	Hồng - Tím
18	1	Kem
19	1	Màu be
20	1	Màu combo
21	1	Màu kết hợp
22	1	Nâu
23	1	Nâu nhạt
24	1	Tím
25	1	Tím bạc
26	1	Tím nhạt
27	1	Titan đen
28	1	Titan Tím
29	1	Titan tự nhiên
30	1	Titan vàng
31	1	Titan xám
32	1	Titan xanh
33	1	Trắng
34	1	Trắng Ánh Sao
35	1	Trắng - Kem
36	1	Trắng Starlight
37	1	Trắng - Vàng
38	1	Trắng - Xanh
39	1	Vàng
40	1	Vàng đồng
41	1	Vàng Hồng
42	1	Vàng nhạt
43	1	Vàng Nhạt
44	1	Xám
45	1	Xám - đồng
46	1	Xám nhạt
47	1	Xanh
48	1	Xanh Bạc Hà
49	1	Xanh Đậm
50	1	Xanh da trời
51	1	Xanh da trời nhạt
52	1	Xanh đen
53	1	Xanh Dương
54	1	Xanh dương đậm
55	1	Xanh dương nhạt
56	1	Xanh lá
57	1	Xanh lá đậm
58	1	Xanh Lam Nhạt
59	1	Xanh Lá Xô Thơm
60	1	Xanh Lưu Ly
61	1	Xanh mint
62	1	Xanh ngọc
63	1	Xanh Nước Biển
64	1	Xanh Oliu
65	1	Xanh rêu
66	1	Xanh tím
67	1	Xanh - Xám
68	2	12 GB
69	2	16 GB
70	2	24 GB
71	2	32 GB
72	2	3 GB
73	2	48 GB
74	2	4 GB
75	2	6 GB
76	2	8 GB
77	3	128 GB
78	3	12 GB
79	3	16 GB
80	3	192 GB
81	3	1 TB
82	3	24 GB
83	3	256 GB
84	3	32 GB
85	3	512 GB
86	3	64 GB
87	3	8 GB
\.


ALTER TABLE public.attribute_values ENABLE TRIGGER ALL;

--
-- Data for Name: brands; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.brands DISABLE TRIGGER ALL;

COPY public.brands (brand_id, name, slug, logo_url, description, website_url, is_active, created_at, updated_at) FROM stdin;
2	iPhone (Apple)	iphone-apple	\N	\N	\N	t	2026-09-24 20:54:07.549865	2026-09-24 20:54:07.549865
3	Xiaomi	xiaomi	\N	\N	\N	t	2026-09-24 20:54:07.709875	2026-09-24 20:54:07.709875
4	realme	realme	\N	\N	\N	t	2026-09-24 20:54:07.779561	2026-09-24 20:54:07.779561
5	HONOR	honor	\N	\N	\N	t	2026-09-24 20:54:07.892141	2026-09-24 20:54:07.892141
6	Motorola	motorola	\N	\N	\N	t	2026-09-24 20:54:07.934375	2026-09-24 20:54:07.934375
7	Samsung	samsung	\N	\N	\N	t	2026-09-24 20:54:08.004988	2026-09-24 20:54:08.004988
8	OPPO	oppo	\N	\N	\N	t	2026-09-24 20:54:08.046604	2026-09-24 20:54:08.046604
9	vivo	vivo	\N	\N	\N	t	2026-09-24 20:54:08.184334	2026-09-24 20:54:08.184334
10	Nothing Phone	nothing-phone	\N	\N	\N	t	2026-09-24 20:54:08.2304	2026-09-24 20:54:08.2304
11	Tecno	tecno	\N	\N	\N	t	2026-09-24 20:54:08.409642	2026-09-24 20:54:08.409642
12	Nokia	nokia	\N	\N	\N	t	2026-09-24 20:54:09.628411	2026-09-24 20:54:09.628411
13	Masstel	masstel	\N	\N	\N	t	2026-09-24 20:54:09.70374	2026-09-24 20:54:09.70374
14	Nubia	nubia	\N	\N	\N	t	2026-09-24 20:54:10.015294	2026-09-24 20:54:10.015294
15	Mobell	mobell	\N	\N	\N	t	2026-09-24 20:54:10.073091	2026-09-24 20:54:10.073091
16	Xphone	xphone	\N	\N	\N	t	2026-09-24 20:54:10.830033	2026-09-24 20:54:10.830033
17	HP	hp	\N	\N	\N	t	2026-09-24 20:54:11.643891	2026-09-24 20:54:11.643891
18	MacBook	macbook	\N	\N	\N	t	2026-09-24 20:54:11.680453	2026-09-24 20:54:11.680453
19	Asus	asus	\N	\N	\N	t	2026-09-24 20:54:11.718444	2026-09-24 20:54:11.718444
20	Dell	dell	\N	\N	\N	t	2026-09-24 20:54:11.752162	2026-09-24 20:54:11.752162
21	Lenovo	lenovo	\N	\N	\N	t	2026-09-24 20:54:11.785721	2026-09-24 20:54:11.785721
22	MSI	msi	\N	\N	\N	t	2026-09-24 20:54:11.883126	2026-09-24 20:54:11.883126
23	Acer	acer	\N	\N	\N	t	2026-09-24 20:54:11.940973	2026-09-24 20:54:11.940973
24	SingPC	singpc	\N	\N	\N	t	2026-09-24 20:54:12.010913	2026-09-24 20:54:12.010913
25	GIGABYTE	gigabyte	\N	\N	\N	t	2026-09-24 20:54:13.476355	2026-09-24 20:54:13.476355
26	iPad (Apple)	ipad-apple	\N	\N	\N	t	2026-09-24 20:54:19.369046	2026-09-24 20:54:19.369046
27	Boox	boox	\N	\N	\N	t	2026-09-24 20:54:19.942779	2026-09-24 20:54:19.942779
28	Kindle	kindle	\N	\N	\N	t	2026-09-24 20:54:20.025036	2026-09-24 20:54:20.025036
29	Kidcare	kidcare	\N	\N	\N	t	2026-09-24 20:54:20.294918	2026-09-24 20:54:20.294918
30	Huawei	huawei	\N	\N	\N	t	2026-09-24 20:54:20.328083	2026-09-24 20:54:20.328083
31	Amazfit	amazfit	\N	\N	\N	t	2026-09-24 20:54:20.409996	2026-09-24 20:54:20.409996
32	Apple	apple	\N	\N	\N	t	2026-09-24 20:54:20.441076	2026-09-24 20:54:20.441076
33	imoo	imoo	\N	\N	\N	t	2026-09-24 20:54:21.111854	2026-09-24 20:54:21.111854
34	Garmin	garmin	\N	\N	\N	t	2026-09-24 20:54:21.295775	2026-09-24 20:54:21.295775
35	Zwatch	zwatch	\N	\N	\N	t	2026-09-24 20:54:22.083859	2026-09-24 20:54:22.083859
36	Mykid	mykid	\N	\N	\N	t	2026-09-24 20:54:22.225375	2026-09-24 20:54:22.225375
37	Tammi	tammi	\N	\N	\N	t	2026-09-24 20:54:22.452035	2026-09-24 20:54:22.452035
38	Zobo	zobo	\N	\N	\N	t	2026-09-24 20:54:22.604261	2026-09-24 20:54:22.604261
39	Suunto	suunto	\N	\N	\N	t	2026-09-24 20:54:23.115753	2026-09-24 20:54:23.115753
40	ORIENT	orient	\N	\N	\N	t	2026-09-24 20:54:23.447355	2026-09-24 20:54:23.447355
41	BABY-G	baby-g	\N	\N	\N	t	2026-09-24 20:54:23.480331	2026-09-24 20:54:23.480331
42	EDIFICE CASIO	edifice-casio	\N	\N	\N	t	2026-09-24 20:54:23.647132	2026-09-24 20:54:23.647132
43	MVW	mvw	\N	\N	\N	t	2026-09-24 20:54:23.679245	2026-09-24 20:54:23.679245
44	TORRAS	torras	\N	\N	\N	t	2026-09-24 20:54:23.861871	2026-09-24 20:54:23.861871
45	Ugreen	ugreen	\N	\N	\N	t	2026-09-24 20:54:23.89367	2026-09-24 20:54:23.89367
46	Imou	imou	\N	\N	\N	t	2026-09-24 20:54:23.941299	2026-09-24 20:54:23.941299
47	AUKEY	aukey	\N	\N	\N	t	2026-09-24 20:54:23.973713	2026-09-24 20:54:23.973713
48	Modi	modi	\N	\N	\N	t	2026-09-24 20:54:24.004591	2026-09-24 20:54:24.004591
49	TOMTOC	tomtoc	\N	\N	\N	t	2026-09-24 20:54:24.035517	2026-09-24 20:54:24.035517
50	AVA+	ava	\N	\N	\N	t	2026-09-24 20:54:24.067037	2026-09-24 20:54:24.067037
51	Golf	golf	\N	\N	\N	t	2026-09-24 20:54:24.162833	2026-09-24 20:54:24.162833
52	WARRIOR	warrior	\N	\N	\N	t	2026-09-24 20:54:24.195312	2026-09-24 20:54:24.195312
53	Innostyle	innostyle	\N	\N	\N	t	2026-09-24 20:54:24.226792	2026-09-24 20:54:24.226792
54	EDRA	edra	\N	\N	\N	t	2026-09-24 20:54:24.259474	2026-09-24 20:54:24.259474
55	Tucano	tucano	\N	\N	\N	t	2026-09-24 20:54:24.344743	2026-09-24 20:54:24.344743
\.


ALTER TABLE public.brands ENABLE TRIGGER ALL;

--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.categories DISABLE TRIGGER ALL;

COPY public.categories (category_id, name, slug, description, parent_id, icon_url, display_order, is_active, created_at, updated_at) FROM stdin;
4	Điện Thoại	dien-thoai	\N	\N	\N	\N	t	2026-09-24 20:54:07.48947	2026-09-24 20:54:07.48947
5	Laptop	laptop	\N	\N	\N	\N	t	2026-09-24 20:54:11.626495	2026-09-24 20:54:11.626495
6	Máy Tính Bảng	may-tinh-bang	\N	\N	\N	\N	t	2026-09-24 20:54:19.352871	2026-09-24 20:54:19.352871
7	Đồng Hồ Thông Minh	dong-ho-thong-minh	\N	\N	\N	\N	t	2026-09-24 20:54:20.278223	2026-09-24 20:54:20.278223
8	Đồng Hồ Thời Trang	dong-ho-thoi-trang	\N	\N	\N	\N	t	2026-09-24 20:54:23.43225	2026-09-24 20:54:23.43225
9	Phụ Kiện	phu-kien	\N	\N	\N	\N	t	2026-09-24 20:54:23.846844	2026-09-24 20:54:23.846844
\.


ALTER TABLE public.categories ENABLE TRIGGER ALL;

--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.products DISABLE TRIGGER ALL;

COPY public.products (product_id, name, slug, description, brand_id, category_id, base_price, discount_price, stock_quantity, sku, weight, warranty_months, rating, total_reviews, view_count, is_active, created_at, updated_at, deleted_at) FROM stdin;
1	Điện thoại iPhone 18 Pro Max 256GB	dien-thoai-iphone-18-pro-max-256gb	\N	2	4	41990000.00	\N	0	TGDD-370982	\N	12	0.00	0	0	t	2026-09-24 20:54:07.605619	2026-09-24 20:54:07.605619	\N
2	Điện thoại iPhone 18 Pro 256GB	dien-thoai-iphone-18-pro-256gb	\N	2	4	38990000.00	\N	0	TGDD-370977	\N	12	0.00	0	0	t	2026-09-24 20:54:07.663478	2026-09-24 20:54:07.663478	\N
3	Điện thoại iPhone Duo 256GB	dien-thoai-iphone-duo-256gb	\N	2	4	64990000.00	\N	0	TGDD-370987	\N	12	0.00	0	0	t	2026-09-24 20:54:07.688399	2026-09-24 20:54:07.688399	\N
4	Điện thoại Xiaomi Redmi Note 17 4G 4GB/128GB	dien-thoai-xiaomi-redmi-note-17-4g-4gb-128gb	\N	3	4	5790000.00	\N	0	TGDD-369617	\N	12	0.00	0	0	t	2026-09-24 20:54:07.733282	2026-09-24 20:54:07.733282	\N
5	Điện thoại iPhone 17 Pro Max 256GB	dien-thoai-iphone-17-pro-max-256gb	\N	2	4	34590000.00	\N	0	TGDD-342679	\N	12	0.00	0	0	t	2026-09-24 20:54:07.757371	2026-09-24 20:54:07.757371	\N
6	Điện thoại realme 16T 5G 8GB/128GB	dien-thoai-realme-16t-5g-8gb-128gb	\N	4	4	7790000.00	\N	0	TGDD-367499	\N	12	0.00	0	0	t	2026-09-24 20:54:07.80319	2026-09-24 20:54:07.80319	\N
7	Điện thoại Xiaomi Redmi Note 17 Pro 5G 12GB/256GB	dien-thoai-xiaomi-redmi-note-17-pro-5g-12gb-256gb	\N	3	4	10790000.00	\N	0	TGDD-369626	\N	12	0.00	0	0	t	2026-09-24 20:54:07.847494	2026-09-24 20:54:07.847494	\N
8	Điện thoại Xiaomi Redmi Note 17 Pro Max 5G 12GB/256GB	dien-thoai-xiaomi-redmi-note-17-pro-max-5g-12gb-256gb	\N	3	4	13990000.00	\N	0	TGDD-369628	\N	12	0.00	0	0	t	2026-09-24 20:54:07.870968	2026-09-24 20:54:07.871495	\N
9	Điện thoại Honor 600 Lite 8GB/256GB	dien-thoai-honor-600-lite-8gb-256gb	\N	5	4	8940000.00	\N	0	TGDD-367011	\N	12	0.00	0	0	t	2026-09-24 20:54:07.914253	2026-09-24 20:54:07.91478	\N
10	Điện thoại Motorola Razr Fold 5G 12GB/256GB	dien-thoai-motorola-razr-fold-5g-12gb-256gb	\N	6	4	39490000.00	\N	0	TGDD-368236	\N	12	0.00	0	0	t	2026-09-24 20:54:07.960386	2026-09-24 20:54:07.960386	\N
11	Điện thoại iPhone 17 256GB	dien-thoai-iphone-17-256gb	\N	2	4	28990000.00	\N	0	TGDD-342667	\N	12	0.00	0	0	t	2026-09-24 20:54:07.984529	2026-09-24 20:54:07.984529	\N
12	Điện thoại Samsung Galaxy S26 FE 5G 8GB/128GB	dien-thoai-samsung-galaxy-s26-fe-5g-8gb-128gb	\N	7	4	15840000.00	\N	0	TGDD-370544	\N	12	0.00	0	0	t	2026-09-24 20:54:08.026758	2026-09-24 20:54:08.026758	\N
13	Điện thoại OPPO Reno16F 5G 8GB/128GB	dien-thoai-oppo-reno16f-5g-8gb-128gb	\N	8	4	13140000.00	\N	0	TGDD-368250	\N	12	0.00	0	0	t	2026-09-24 20:54:08.068634	2026-09-24 20:54:08.068634	\N
14	Điện thoại iPhone 17 Pro 256GB	dien-thoai-iphone-17-pro-256gb	\N	2	4	31990000.00	\N	0	TGDD-342676	\N	12	0.00	0	0	t	2026-09-24 20:54:08.090941	2026-09-24 20:54:08.090941	\N
15	Điện thoại Samsung Galaxy Z Fold8 12GB/256GB	dien-thoai-samsung-galaxy-z-fold8-12gb-256gb	\N	7	4	39090000.00	\N	0	TGDD-368057	\N	12	0.00	0	0	t	2026-09-24 20:54:08.114449	2026-09-24 20:54:08.114449	\N
16	Điện thoại Samsung Galaxy Z Fold8 Ultra 12GB/256GB	dien-thoai-samsung-galaxy-z-fold8-ultra-12gb-256gb	\N	7	4	43790000.00	\N	0	TGDD-368050	\N	12	0.00	0	0	t	2026-09-24 20:54:08.13694	2026-09-24 20:54:08.13694	\N
17	Điện thoại iPhone 16 Plus 128GB	dien-thoai-iphone-16-plus-128gb	\N	2	4	24990000.00	\N	0	TGDD-329138	\N	12	0.00	0	0	t	2026-09-24 20:54:08.159298	2026-09-24 20:54:08.159298	\N
18	Điện thoại Vivo X300 Ultra 16GB/512GB	dien-thoai-vivo-x300-ultra-16gb-512gb	\N	9	4	41840000.00	\N	0	TGDD-363408	\N	12	0.00	0	0	t	2026-09-24 20:54:08.208365	2026-09-24 20:54:08.208365	\N
19	Điện thoại Nothing Phone (4a) 5G 8GB/128GB	dien-thoai-nothing-phone-4a-5g-8gb-128gb	\N	10	4	11140000.00	\N	0	TGDD-368872	\N	12	0.00	0	0	t	2026-09-24 20:54:08.253044	2026-09-24 20:54:08.253044	\N
20	Điện thoại Samsung Galaxy A37 5G 6GB/128GB	dien-thoai-samsung-galaxy-a37-5g-6gb-128gb	\N	7	4	9000000.00	\N	0	TGDD-363401	\N	12	0.00	0	0	t	2026-09-24 20:54:08.276	2026-09-24 20:54:08.276	\N
21	Điện thoại Xiaomi 17T 5G 12GB/256GB	dien-thoai-xiaomi-17t-5g-12gb-256gb	\N	3	4	14890000.00	\N	0	TGDD-367002	\N	12	0.00	0	0	t	2026-09-24 20:54:08.297495	2026-09-24 20:54:08.297495	\N
22	Điện thoại OPPO A6 6GB/128GB	dien-thoai-oppo-a6-6gb-128gb	\N	8	4	8840000.00	\N	0	TGDD-370752	\N	12	0.00	0	0	t	2026-09-24 20:54:08.32041	2026-09-24 20:54:08.32041	\N
23	Điện thoại Vivo V70 FE 5G 8GB/256GB	dien-thoai-vivo-v70-fe-5g-8gb-256gb	\N	9	4	13030000.00	\N	0	TGDD-363466	\N	12	0.00	0	0	t	2026-09-24 20:54:08.343441	2026-09-24 20:54:08.343441	\N
24	Điện thoại realme C100x 4GB/128GB	dien-thoai-realme-c100x-4gb-128gb	\N	4	4	6640000.00	\N	0	TGDD-367427	\N	12	0.00	0	0	t	2026-09-24 20:54:08.366894	2026-09-24 20:54:08.366894	\N
25	Điện thoại HONOR X9d 5G 12GB/256GB	dien-thoai-honor-x9d-5g-12gb-256gb	\N	5	4	10340000.00	\N	0	TGDD-358683	\N	12	0.00	0	0	t	2026-09-24 20:54:08.389306	2026-09-24 20:54:08.389306	\N
26	Điện thoại Tecno Spark 50 4GB/128GB	dien-thoai-tecno-spark-50-4gb-128gb	\N	11	4	4640000.00	\N	0	TGDD-367204	\N	12	0.00	0	0	t	2026-09-24 20:54:08.430225	2026-09-24 20:54:08.430225	\N
27	Điện thoại Motorola Edge 70 Fusion 8GB/128GB	dien-thoai-motorola-edge-70-fusion-8gb-128gb	\N	6	4	8440000.00	\N	0	TGDD-367975	\N	12	0.00	0	0	t	2026-09-24 20:54:08.451561	2026-09-24 20:54:08.451561	\N
28	Điện thoại iPhone 15 128GB	dien-thoai-iphone-15-128gb	\N	2	4	21990000.00	\N	0	TGDD-281570	\N	12	0.00	0	0	t	2026-09-24 20:54:08.473022	2026-09-24 20:54:08.473022	\N
29	Điện thoại iPhone Air 256GB	dien-thoai-iphone-air-256gb	\N	2	4	22990000.00	\N	0	TGDD-342670	\N	12	0.00	0	0	t	2026-09-24 20:54:08.506234	2026-09-24 20:54:08.506234	\N
30	Điện thoại Samsung Galaxy S26 5G 12GB/256GB	dien-thoai-samsung-galaxy-s26-5g-12gb-256gb	\N	7	4	19500000.00	\N	0	TGDD-361947	\N	12	0.00	0	0	t	2026-09-24 20:54:08.528135	2026-09-24 20:54:08.528135	\N
31	Điện thoại OPPO Reno16 5G 8GB/256GB	dien-thoai-oppo-reno16-5g-8gb-256gb	\N	8	4	18340000.00	\N	0	TGDD-368099	\N	12	0.00	0	0	t	2026-09-24 20:54:08.549575	2026-09-24 20:54:08.549575	\N
32	Điện thoại vivo V60 Lite 5G 8GB/256GB	dien-thoai-vivo-v60-lite-5g-8gb-256gb	\N	9	4	9520000.00	\N	0	TGDD-357576	\N	12	0.00	0	0	t	2026-09-24 20:54:08.572594	2026-09-24 20:54:08.572594	\N
33	Điện thoại realme C100 4G 8GB/128GB	dien-thoai-realme-c100-4g-8gb-128gb	\N	4	4	7690000.00	\N	0	TGDD-364187	\N	12	0.00	0	0	t	2026-09-24 20:54:08.595395	2026-09-24 20:54:08.595395	\N
34	Điện thoại Honor X7d 5G 8GB/256GB	dien-thoai-honor-x7d-5g-8gb-256gb	\N	5	4	6540000.00	\N	0	TGDD-358026	\N	12	0.00	0	0	t	2026-09-24 20:54:08.616505	2026-09-24 20:54:08.616505	\N
35	Điện thoại Tecno Spark Go 3 4GB/128GB	dien-thoai-tecno-spark-go-3-4gb-128gb	\N	11	4	3640000.00	\N	0	TGDD-364633	\N	12	0.00	0	0	t	2026-09-24 20:54:08.63944	2026-09-24 20:54:08.63944	\N
36	Điện thoại Motorola G57 Power 5G 8GB/128GB	dien-thoai-motorola-g57-power-5g-8gb-128gb	\N	6	4	6040000.00	\N	0	TGDD-362373	\N	12	0.00	0	0	t	2026-09-24 20:54:08.665133	2026-09-24 20:54:08.665133	\N
37	Điện thoại iPhone 17e 256GB	dien-thoai-iphone-17e-256gb	\N	2	4	21590000.00	\N	0	TGDD-342692	\N	12	0.00	0	0	t	2026-09-24 20:54:08.687671	2026-09-24 20:54:08.687671	\N
38	Điện thoại Xiaomi Redmi Note 15 6GB/128GB	dien-thoai-xiaomi-redmi-note-15-6gb-128gb	\N	3	4	4490000.00	\N	0	TGDD-360302	\N	12	0.00	0	0	t	2026-09-24 20:54:08.708299	2026-09-24 20:54:08.708299	\N
39	Điện thoại OPPO A6x 4GB/64GB	dien-thoai-oppo-a6x-4gb-64gb	\N	8	4	4840000.00	\N	0	TGDD-360244	\N	12	0.00	0	0	t	2026-09-24 20:54:08.729442	2026-09-24 20:54:08.729442	\N
40	Điện thoại OPPO Reno16 Pro 5G 12GB/256GB	dien-thoai-oppo-reno16-pro-5g-12gb-256gb	\N	8	4	23240000.00	\N	0	TGDD-368101	\N	12	0.00	0	0	t	2026-09-24 20:54:08.749853	2026-09-24 20:54:08.749853	\N
41	Điện thoại vivo X300 Pro 5G 16GB/512GB	dien-thoai-vivo-x300-pro-5g-16gb-512gb	\N	9	4	31040000.00	\N	0	TGDD-357862	\N	12	0.00	0	0	t	2026-09-24 20:54:08.769298	2026-09-24 20:54:08.769298	\N
42	Điện thoại realme 16 5G 8GB/256GB	dien-thoai-realme-16-5g-8gb-256gb	\N	4	4	10140000.00	\N	0	TGDD-361703	\N	12	0.00	0	0	t	2026-09-24 20:54:08.789525	2026-09-24 20:54:08.789525	\N
43	Điện thoại HONOR Magic V5 5G 16GB/512GB	dien-thoai-honor-magic-v5-5g-16gb-512gb	\N	5	4	34840000.00	\N	0	TGDD-344641	\N	12	0.00	0	0	t	2026-09-24 20:54:08.810387	2026-09-24 20:54:08.810387	\N
44	Điện thoại Motorola Edge 70 8GB/256GB	dien-thoai-motorola-edge-70-8gb-256gb	\N	6	4	9240000.00	\N	0	TGDD-362374	\N	12	0.00	0	0	t	2026-09-24 20:54:08.831473	2026-09-24 20:54:08.831473	\N
45	Điện thoại iPhone 16 128GB	dien-thoai-iphone-16-128gb	\N	2	4	24990000.00	\N	0	TGDD-329135	\N	12	0.00	0	0	t	2026-09-24 20:54:08.852037	2026-09-24 20:54:08.852037	\N
46	Điện thoại iPhone 16e 128GB	dien-thoai-iphone-16e-128gb	\N	2	4	16990000.00	\N	0	TGDD-334864	\N	12	0.00	0	0	t	2026-09-24 20:54:08.872574	2026-09-24 20:54:08.872574	\N
47	Điện thoại Samsung Galaxy S25 5G 12GB/256GB	dien-thoai-samsung-galaxy-s25-5g-12gb-256gb	\N	7	4	16790000.00	\N	0	TGDD-333363	\N	12	0.00	0	0	t	2026-09-24 20:54:08.892356	2026-09-24 20:54:08.892356	\N
48	Điện thoại Xiaomi Redmi Note 15 5G 6GB/128GB	dien-thoai-xiaomi-redmi-note-15-5g-6gb-128gb	\N	3	4	5490000.00	\N	0	TGDD-360310	\N	12	0.00	0	0	t	2026-09-24 20:54:08.913732	2026-09-24 20:54:08.913732	\N
49	Điện thoại HONOR 400 Lite 12GB/256GB	dien-thoai-honor-400-lite-12gb-256gb	\N	5	4	8180000.00	\N	0	TGDD-339245	\N	12	0.00	0	0	t	2026-09-24 20:54:08.933434	2026-09-24 20:54:08.933434	\N
50	Điện thoại realme C85 8GB/128GB	dien-thoai-realme-c85-8gb-128gb	\N	4	4	5940000.00	\N	0	TGDD-360344	\N	12	0.00	0	0	t	2026-09-24 20:54:08.953384	2026-09-24 20:54:08.953384	\N
51	Điện thoại Motorola G06 POWER 4GB/64GB	dien-thoai-motorola-g06-power-4gb-64gb	\N	6	4	3440000.00	\N	0	TGDD-369546	\N	12	0.00	0	0	t	2026-09-24 20:54:08.974359	2026-09-24 20:54:08.974359	\N
52	Điện thoại Samsung Galaxy A17 5G 8GB/128GB	dien-thoai-samsung-galaxy-a17-5g-8gb-128gb	\N	7	4	6000000.00	\N	0	TGDD-341688	\N	12	0.00	0	0	t	2026-09-24 20:54:08.993907	2026-09-24 20:54:08.993907	\N
53	Điện thoại Xiaomi Redmi Note 15 Pro 5G 8GB/256GB	dien-thoai-xiaomi-redmi-note-15-pro-5g-8gb-256gb	\N	3	4	7990000.00	\N	0	TGDD-360312	\N	12	0.00	0	0	t	2026-09-24 20:54:09.014084	2026-09-24 20:54:09.014084	\N
54	Điện thoại vivo Y39 5G 8GB/128GB	dien-thoai-vivo-y39-5g-8gb-128gb	\N	9	4	6720000.00	\N	0	TGDD-339180	\N	12	0.00	0	0	t	2026-09-24 20:54:09.034914	2026-09-24 20:54:09.034914	\N
55	Điện thoại realme C85 Pro 8GB/128GB	dien-thoai-realme-c85-pro-8gb-128gb	\N	4	4	5840000.00	\N	0	TGDD-357832	\N	12	0.00	0	0	t	2026-09-24 20:54:09.055576	2026-09-24 20:54:09.055576	\N
56	Điện thoại HONOR 400 5G 12GB/256GB Đen	dien-thoai-honor-400-5g-12gb-256gb-den	\N	5	4	11100000.00	\N	0	TGDD-339638	\N	12	0.00	0	0	t	2026-09-24 20:54:09.075642	2026-09-24 20:54:09.075642	\N
57	Điện thoại realme C85 5G 8GB/128GB	dien-thoai-realme-c85-5g-8gb-128gb	\N	4	4	6630000.00	\N	0	TGDD-362760	\N	12	0.00	0	0	t	2026-09-24 20:54:09.095363	2026-09-24 20:54:09.095363	\N
58	Điện thoại Tecno Spark 40 6GB/128GB	dien-thoai-tecno-spark-40-6gb-128gb	\N	11	4	3440000.00	\N	0	TGDD-366660	\N	12	0.00	0	0	t	2026-09-24 20:54:09.115041	2026-09-24 20:54:09.115041	\N
59	Điện thoại Motorola G35 5G 4GB/128GB	dien-thoai-motorola-g35-5g-4gb-128gb	\N	6	4	4070000.00	\N	0	TGDD-358048	\N	12	0.00	0	0	t	2026-09-24 20:54:09.134917	2026-09-24 20:54:09.134917	\N
60	Điện thoại realme C100i 4GB/64GB	dien-thoai-realme-c100i-4gb-64gb	\N	4	4	4940000.00	\N	0	TGDD-366829	\N	12	0.00	0	0	t	2026-09-24 20:54:09.154184	2026-09-24 20:54:09.154184	\N
61	Điện thoại realme P4 Power 5G 12GB/256GB	dien-thoai-realme-p4-power-5g-12gb-256gb	\N	4	4	13690000.00	\N	0	TGDD-363438	\N	12	0.00	0	0	t	2026-09-24 20:54:09.173054	2026-09-24 20:54:09.173054	\N
62	Điện thoại Samsung Galaxy S25 FE 5G 8GB/128GB	dien-thoai-samsung-galaxy-s25-fe-5g-8gb-128gb	\N	7	4	12490000.00	\N	0	TGDD-342560	\N	12	0.00	0	0	t	2026-09-24 20:54:09.192351	2026-09-24 20:54:09.192351	\N
63	Điện thoại OPPO Reno15 F 5G 8GB/256GB	dien-thoai-oppo-reno15-f-5g-8gb-256gb	\N	8	4	11840000.00	\N	0	TGDD-360240	\N	12	0.00	0	0	t	2026-09-24 20:54:09.21234	2026-09-24 20:54:09.21234	\N
64	Điện thoại Xiaomi Redmi Note 15 Pro 8GB/256GB	dien-thoai-xiaomi-redmi-note-15-pro-8gb-256gb	\N	3	4	6690000.00	\N	0	TGDD-360307	\N	12	0.00	0	0	t	2026-09-24 20:54:09.232297	2026-09-24 20:54:09.232297	\N
65	Điện thoại OPPO A6 Pro 8GB/256GB	dien-thoai-oppo-a6-pro-8gb-256gb	\N	8	4	9020000.00	\N	0	TGDD-344650	\N	12	0.00	0	0	t	2026-09-24 20:54:09.25283	2026-09-24 20:54:09.25283	\N
66	Điện thoại realme 15 Pro 5G 12GB/256GB	dien-thoai-realme-15-pro-5g-12gb-256gb	\N	4	4	10240000.00	\N	0	TGDD-343067	\N	12	0.00	0	0	t	2026-09-24 20:54:09.275711	2026-09-24 20:54:09.275711	\N
67	Điện thoại realme 15T 5G 8GB/256GB	dien-thoai-realme-15t-5g-8gb-256gb	\N	4	4	6940000.00	\N	0	TGDD-343063	\N	12	0.00	0	0	t	2026-09-24 20:54:09.296387	2026-09-24 20:54:09.296387	\N
68	Điện thoại Motorola G86 POWER 5G 8GB/128GB	dien-thoai-motorola-g86-power-5g-8gb-128gb	\N	6	4	5840000.00	\N	0	TGDD-358225	\N	12	0.00	0	0	t	2026-09-24 20:54:09.317531	2026-09-24 20:54:09.317531	\N
69	Điện thoại Motorola Edge 60 Fusion 5G 8GB/256GB	dien-thoai-motorola-edge-60-fusion-5g-8gb-256gb	\N	6	4	6840000.00	\N	0	TGDD-358224	\N	12	0.00	0	0	t	2026-09-24 20:54:09.337435	2026-09-24 20:54:09.337435	\N
70	Điện thoại Motorola Razr 60 5G 8GB/256GB	dien-thoai-motorola-razr-60-5g-8gb-256gb	\N	6	4	14040000.00	\N	0	TGDD-358223	\N	12	0.00	0	0	t	2026-09-24 20:54:09.357458	2026-09-24 20:54:09.357458	\N
71	Điện thoại Samsung Galaxy Z Fold7 5G 12GB/256GB	dien-thoai-samsung-galaxy-z-fold7-5g-12gb-256gb	\N	7	4	36090000.00	\N	0	TGDD-338738	\N	12	0.00	0	0	t	2026-09-24 20:54:09.388875	2026-09-24 20:54:09.388875	\N
72	Điện thoại Xiaomi Redmi Note 15 Pro+ 5G 12GB/256GB	dien-thoai-xiaomi-redmi-note-15-pro-5g-12gb-256gb	\N	3	4	9990000.00	\N	0	TGDD-360309	\N	12	0.00	0	0	t	2026-09-24 20:54:09.410461	2026-09-24 20:54:09.410461	\N
73	Điện thoại OPPO A6 Pro 5G 8GB/256GB	dien-thoai-oppo-a6-pro-5g-8gb-256gb	\N	8	4	10520000.00	\N	0	TGDD-343124	\N	12	0.00	0	0	t	2026-09-24 20:54:09.431622	2026-09-24 20:54:09.431622	\N
74	Điện thoại vivo V60 5G 12GB/256GB	dien-thoai-vivo-v60-5g-12gb-256gb	\N	9	4	14920000.00	\N	0	TGDD-341625	\N	12	0.00	0	0	t	2026-09-24 20:54:09.45359	2026-09-24 20:54:09.45359	\N
75	Điện thoại HONOR X8d 8GB/128GB	dien-thoai-honor-x8d-8gb-128gb	\N	5	4	6340000.00	\N	0	TGDD-362919	\N	12	0.00	0	0	t	2026-09-24 20:54:09.476491	2026-09-24 20:54:09.476491	\N
76	Điện thoại OPPO A6t 4GB/64GB	dien-thoai-oppo-a6t-4gb-64gb	\N	8	4	4840000.00	\N	0	TGDD-361191	\N	12	0.00	0	0	t	2026-09-24 20:54:09.499017	2026-09-24 20:54:09.499017	\N
77	Điện thoại vivo V50 Lite 5G 8GB/256GB	dien-thoai-vivo-v50-lite-5g-8gb-256gb	\N	9	4	8660000.00	\N	0	TGDD-336408	\N	12	0.00	0	0	t	2026-09-24 20:54:09.520126	2026-09-24 20:54:09.520126	\N
78	Điện thoại Samsung Galaxy S26 Ultra 5G 12GB/256GB	dien-thoai-samsung-galaxy-s26-ultra-5g-12gb-256gb	\N	7	4	27500000.00	\N	0	TGDD-361951	\N	12	0.00	0	0	t	2026-09-24 20:54:09.543298	2026-09-24 20:54:09.543298	\N
79	Điện thoại Samsung Galaxy A57 5G 8GB/128GB	dien-thoai-samsung-galaxy-a57-5g-8gb-128gb	\N	7	4	10590000.00	\N	0	TGDD-363398	\N	12	0.00	0	0	t	2026-09-24 20:54:09.56585	2026-09-24 20:54:09.56585	\N
80	Điện thoại Samsung Galaxy A07 4GB/64GB	dien-thoai-samsung-galaxy-a07-4gb-64gb	\N	7	4	3140000.00	\N	0	TGDD-341802	\N	12	0.00	0	0	t	2026-09-24 20:54:09.587706	2026-09-24 20:54:09.587706	\N
81	Điện thoại Samsung Galaxy A17 4GB/128GB	dien-thoai-samsung-galaxy-a17-4gb-128gb	\N	7	4	5200000.00	\N	0	TGDD-341797	\N	12	0.00	0	0	t	2026-09-24 20:54:09.609282	2026-09-24 20:54:09.609282	\N
82	Điện thoại Nokia 105 4G Pro	dien-thoai-nokia-105-4g-pro	\N	12	4	780000.00	\N	0	TGDD-311033	\N	12	0.00	0	0	t	2026-09-24 20:54:09.646951	2026-09-24 20:54:09.646951	\N
83	Điện thoại Xiaomi Redmi 17 4G 4GB/128GB	dien-thoai-xiaomi-redmi-17-4g-4gb-128gb	\N	3	4	4790000.00	\N	0	TGDD-369630	\N	12	0.00	0	0	t	2026-09-24 20:54:09.666592	2026-09-24 20:54:09.666592	\N
84	Điện thoại Xiaomi Poco C71 4GB/64GB	dien-thoai-xiaomi-poco-c71-4gb-64gb	\N	3	4	3190000.00	\N	0	TGDD-365289	\N	12	0.00	0	0	t	2026-09-24 20:54:09.686312	2026-09-24 20:54:09.686312	\N
85	Điện thoại Masstel IZI T6 T127	dien-thoai-masstel-izi-t6-t127	\N	13	4	550000.00	\N	0	TGDD-367977	\N	12	0.00	0	0	t	2026-09-24 20:54:09.723301	2026-09-24 20:54:09.723301	\N
86	Điện thoại vivo Y31d 6GB/128GB	dien-thoai-vivo-y31d-6gb-128gb	\N	9	4	7640000.00	\N	0	TGDD-360671	\N	12	0.00	0	0	t	2026-09-24 20:54:09.744294	2026-09-24 20:54:09.744294	\N
87	Điện thoại Masstel Fami 50 4G	dien-thoai-masstel-fami-50-4g	\N	13	4	650000.00	\N	0	TGDD-323546	\N	12	0.00	0	0	t	2026-09-24 20:54:09.765306	2026-09-24 20:54:09.765306	\N
88	Điện thoại vivo Y05 4GB/64GB	dien-thoai-vivo-y05-4gb-64gb	\N	9	4	4740000.00	\N	0	TGDD-365875	\N	12	0.00	0	0	t	2026-09-24 20:54:09.786425	2026-09-24 20:54:09.786425	\N
89	Điện thoại Nokia HMD 105 4G	dien-thoai-nokia-hmd-105-4g	\N	12	4	750000.00	\N	0	TGDD-329676	\N	12	0.00	0	0	t	2026-09-24 20:54:09.806566	2026-09-24 20:54:09.806566	\N
90	Điện thoại Masstel IZI 10 4G Type-C	dien-thoai-masstel-izi-10-4g-type-c	\N	13	4	430000.00	\N	0	TGDD-342939	\N	12	0.00	0	0	t	2026-09-24 20:54:09.827504	2026-09-24 20:54:09.827504	\N
91	Điện thoại Nokia 220 4G	dien-thoai-nokia-220-4g	\N	12	4	1100000.00	\N	0	TGDD-207956	\N	12	0.00	0	0	t	2026-09-24 20:54:09.847255	2026-09-24 20:54:09.847255	\N
92	Điện thoại Masstel IZI 10S	dien-thoai-masstel-izi-10s	\N	13	4	490000.00	\N	0	TGDD-370758	\N	12	0.00	0	0	t	2026-09-24 20:54:09.866919	2026-09-24 20:54:09.866919	\N
93	Điện thoại Xiaomi Redmi A7 Pro 4GB/64GB	dien-thoai-xiaomi-redmi-a7-pro-4gb-64gb	\N	3	4	3590000.00	\N	0	TGDD-364793	\N	12	0.00	0	0	t	2026-09-24 20:54:09.88597	2026-09-24 20:54:09.88597	\N
94	Điện thoại OPPO A6c 4GB/64GB	dien-thoai-oppo-a6c-4gb-64gb	\N	8	4	4540000.00	\N	0	TGDD-367596	\N	12	0.00	0	0	t	2026-09-24 20:54:09.904426	2026-09-24 20:54:09.904426	\N
95	Điện thoại realme Note 80 4GB/64GB	dien-thoai-realme-note-80-4gb-64gb	\N	4	4	4290000.00	\N	0	TGDD-363437	\N	12	0.00	0	0	t	2026-09-24 20:54:09.922479	2026-09-24 20:54:09.922479	\N
96	Điện thoại vivo Y05e 4GB/64GB	dien-thoai-vivo-y05e-4gb-64gb	\N	9	4	4040000.00	\N	0	TGDD-370492	\N	12	0.00	0	0	t	2026-09-24 20:54:09.941306	2026-09-24 20:54:09.941306	\N
97	Điện thoại Nokia 110 4G Pro	dien-thoai-nokia-110-4g-pro	\N	12	4	850000.00	\N	0	TGDD-311034	\N	12	0.00	0	0	t	2026-09-24 20:54:09.960216	2026-09-24 20:54:09.960216	\N
98	Điện thoại Vivo Y11d 4GB/128GB	dien-thoai-vivo-y11d-4gb-128gb	\N	9	4	5940000.00	\N	0	TGDD-365878	\N	12	0.00	0	0	t	2026-09-24 20:54:09.979175	2026-09-24 20:54:09.979175	\N
99	Điện thoại Xiaomi Redmi A7 3GB/64GB	dien-thoai-xiaomi-redmi-a7-3gb-64gb	\N	3	4	3190000.00	\N	0	TGDD-368232	\N	12	0.00	0	0	t	2026-09-24 20:54:09.998497	2026-09-24 20:54:09.998497	\N
100	Điện thoại Nubia V80 Design 4GB/128GB	dien-thoai-nubia-v80-design-4gb-128gb	\N	14	4	3540000.00	\N	0	TGDD-366923	\N	12	0.00	0	0	t	2026-09-24 20:54:10.033712	2026-09-24 20:54:10.033712	\N
101	Điện thoại Samsung Galaxy A07 5G 4GB/128GB	dien-thoai-samsung-galaxy-a07-5g-4gb-128gb	\N	7	4	4800000.00	\N	0	TGDD-361709	\N	12	0.00	0	0	t	2026-09-24 20:54:10.054593	2026-09-24 20:54:10.054593	\N
102	Điện thoại Mobell F209	dien-thoai-mobell-f209	\N	15	4	610000.00	\N	0	TGDD-299998	\N	12	0.00	0	0	t	2026-09-24 20:54:10.091815	2026-09-24 20:54:10.091815	\N
103	Điện thoại HONOR Play 10 3GB/64GB	dien-thoai-honor-play-10-3gb-64gb	\N	5	4	3140000.00	\N	0	TGDD-358698	\N	12	0.00	0	0	t	2026-09-24 20:54:10.111609	2026-09-24 20:54:10.111609	\N
104	Điện thoại Samsung Galaxy S25 Ultra 5G 12GB/256GB	dien-thoai-samsung-galaxy-s25-ultra-5g-12gb-256gb	\N	7	4	24390000.00	\N	0	TGDD-333347	\N	12	0.00	0	0	t	2026-09-24 20:54:10.129875	2026-09-24 20:54:10.129875	\N
105	Điện thoại Xiaomi Redmi Note 17 5G 6GB/128GB	dien-thoai-xiaomi-redmi-note-17-5g-6gb-128gb	\N	3	4	7390000.00	\N	0	TGDD-369620	\N	12	0.00	0	0	t	2026-09-24 20:54:10.15104	2026-09-24 20:54:10.15104	\N
106	Điện thoại Nubia A56 4GB/128GB	dien-thoai-nubia-a56-4gb-128gb	\N	14	4	3240000.00	\N	0	TGDD-366662	\N	12	0.00	0	0	t	2026-09-24 20:54:10.171474	2026-09-24 20:54:10.171474	\N
107	Điện thoại Nubia A36 4GB/64GB	dien-thoai-nubia-a36-4gb-64gb	\N	14	4	2840000.00	\N	0	TGDD-361630	\N	12	0.00	0	0	t	2026-09-24 20:54:10.191016	2026-09-24 20:54:10.191016	\N
108	Điện thoại Xiaomi Redmi 17 5G 4GB/128GB	dien-thoai-xiaomi-redmi-17-5g-4gb-128gb	\N	3	4	4990000.00	\N	0	TGDD-369634	\N	12	0.00	0	0	t	2026-09-24 20:54:10.209627	2026-09-24 20:54:10.209627	\N
109	Điện thoại Mobell F309 4G	dien-thoai-mobell-f309-4g	\N	15	4	750000.00	\N	0	TGDD-304608	\N	12	0.00	0	0	t	2026-09-24 20:54:10.229051	2026-09-24 20:54:10.229051	\N
110	Điện thoại Mobell M239 4G	dien-thoai-mobell-m239-4g	\N	15	4	430000.00	\N	0	TGDD-284122	\N	12	0.00	0	0	t	2026-09-24 20:54:10.249032	2026-09-24 20:54:10.249032	\N
111	Điện thoại Xiaomi 17T Pro 5G 12GB/256GB	dien-thoai-xiaomi-17t-pro-5g-12gb-256gb	\N	3	4	17290000.00	\N	0	TGDD-367004	\N	12	0.00	0	0	t	2026-09-24 20:54:10.270715	2026-09-24 20:54:10.270715	\N
112	Điện thoại Xiaomi Redmi 15 5G 4GB/128GB	dien-thoai-xiaomi-redmi-15-5g-4gb-128gb	\N	3	4	4590000.00	\N	0	TGDD-359776	\N	12	0.00	0	0	t	2026-09-24 20:54:10.293415	2026-09-24 20:54:10.293415	\N
113	Điện thoại Vivo V70 5G 8GB/256GB	dien-thoai-vivo-v70-5g-8gb-256gb	\N	9	4	16820000.00	\N	0	TGDD-363464	\N	12	0.00	0	0	t	2026-09-24 20:54:10.315231	2026-09-24 20:54:10.315231	\N
114	Điện thoại Nubia V80 Max 6GB/128GB	dien-thoai-nubia-v80-max-6gb-128gb	\N	14	4	3740000.00	\N	0	TGDD-367811	\N	12	0.00	0	0	t	2026-09-24 20:54:10.345373	2026-09-24 20:54:10.345373	\N
115	Điện thoại HONOR X5c Plus 4GB/64GB	dien-thoai-honor-x5c-plus-4gb-64gb	\N	5	4	3540000.00	\N	0	TGDD-358691	\N	12	0.00	0	0	t	2026-09-24 20:54:10.367694	2026-09-24 20:54:10.367694	\N
116	Điện thoại Masstel IZI T6 4G	dien-thoai-masstel-izi-t6-4g	\N	13	4	550000.00	\N	0	TGDD-322877	\N	12	0.00	0	0	t	2026-09-24 20:54:10.388137	2026-09-24 20:54:10.388137	\N
117	Điện thoại Masstel Fami 60S 4G	dien-thoai-masstel-fami-60s-4g	\N	13	4	750000.00	\N	0	TGDD-322876	\N	12	0.00	0	0	t	2026-09-24 20:54:10.406993	2026-09-24 20:54:10.406993	\N
118	Điện thoại OPPO Reno15 5G 8GB/256GB	dien-thoai-oppo-reno15-5g-8gb-256gb	\N	8	4	13520000.00	\N	0	TGDD-360238	\N	12	0.00	0	0	t	2026-09-24 20:54:10.460708	2026-09-24 20:54:10.460708	\N
119	Điện thoại Xiaomi Redmi 15C 6GB/128GB	dien-thoai-xiaomi-redmi-15c-6gb-128gb	\N	3	4	4090000.00	\N	0	TGDD-346265	\N	12	0.00	0	0	t	2026-09-24 20:54:10.479209	2026-09-24 20:54:10.479209	\N
120	Điện thoại Samsung Galaxy Z Flip8 5G 12GB/256GB	dien-thoai-samsung-galaxy-z-flip8-5g-12gb-256gb	\N	7	4	25000000.00	\N	0	TGDD-368053	\N	12	0.00	0	0	t	2026-09-24 20:54:10.498742	2026-09-24 20:54:10.498742	\N
121	Điện thoại Xiaomi POCO X8 Pro 5G 8GB/256GB	dien-thoai-xiaomi-poco-x8-pro-5g-8gb-256gb	\N	3	4	10290000.00	\N	0	TGDD-363470	\N	12	0.00	0	0	t	2026-09-24 20:54:10.518148	2026-09-24 20:54:10.518148	\N
122	Điện thoại Xiaomi Redmi 15 6GB/128GB	dien-thoai-xiaomi-redmi-15-6gb-128gb	\N	3	4	4090000.00	\N	0	TGDD-341272	\N	12	0.00	0	0	t	2026-09-24 20:54:10.537104	2026-09-24 20:54:10.537104	\N
123	Điện thoại Honor X7d 8GB/128GB	dien-thoai-honor-x7d-8gb-128gb	\N	5	4	5240000.00	\N	0	TGDD-362971	\N	12	0.00	0	0	t	2026-09-24 20:54:10.558637	2026-09-24 20:54:10.558637	\N
124	Điện thoại Samsung Galaxy A27 6GB/128GB	dien-thoai-samsung-galaxy-a27-6gb-128gb	\N	7	4	7500000.00	\N	0	TGDD-368045	\N	12	0.00	0	0	t	2026-09-24 20:54:10.578396	2026-09-24 20:54:10.578396	\N
125	Điện thoại OPPO Find X9s 12GB/512GB	dien-thoai-oppo-find-x9s-12gb-512gb	\N	8	4	25840000.00	\N	0	TGDD-365402	\N	12	0.00	0	0	t	2026-09-24 20:54:10.59797	2026-09-24 20:54:10.59797	\N
126	Điện thoại Xiaomi 15T 5G 12GB/256GB	dien-thoai-xiaomi-15t-5g-12gb-256gb	\N	3	4	10690000.00	\N	0	TGDD-344644	\N	12	0.00	0	0	t	2026-09-24 20:54:10.6173	2026-09-24 20:54:10.6173	\N
127	Điện thoại Mobell M539	dien-thoai-mobell-m539	\N	15	4	690000.00	\N	0	TGDD-288630	\N	12	0.00	0	0	t	2026-09-24 20:54:10.638133	2026-09-24 20:54:10.638133	\N
128	Điện thoại vivo Y21d 6GB/128GB	dien-thoai-vivo-y21d-6gb-128gb	\N	9	4	5600000.00	\N	0	TGDD-358669	\N	12	0.00	0	0	t	2026-09-24 20:54:10.65918	2026-09-24 20:54:10.659691	\N
129	Điện thoại Samsung Galaxy Z Flip7 FE 5G 8GB/128GB	dien-thoai-samsung-galaxy-z-flip7-fe-5g-8gb-128gb	\N	7	4	15840000.00	\N	0	TGDD-338741	\N	12	0.00	0	0	t	2026-09-24 20:54:10.682634	2026-09-24 20:54:10.682634	\N
130	Điện thoại Motorola Moto G37 5G 4GB/64GB	dien-thoai-motorola-moto-g37-5g-4gb-64gb	\N	6	4	4140000.00	\N	0	TGDD-368234	\N	12	0.00	0	0	t	2026-09-24 20:54:10.701674	2026-09-24 20:54:10.701674	\N
131	Điện thoại Mobell Rock 7	dien-thoai-mobell-rock-7	\N	15	4	890000.00	\N	0	TGDD-338026	\N	12	0.00	0	0	t	2026-09-24 20:54:10.719581	2026-09-24 20:54:10.719581	\N
132	Điện thoại OPPO Reno15 Pro 5G 12GB/256GB	dien-thoai-oppo-reno15-pro-5g-12gb-256gb	\N	8	4	17020000.00	\N	0	TGDD-360236	\N	12	0.00	0	0	t	2026-09-24 20:54:10.737216	2026-09-24 20:54:10.737216	\N
133	Điện thoại Mobell M331 4G	dien-thoai-mobell-m331-4g	\N	15	4	540000.00	\N	0	TGDD-314697	\N	12	0.00	0	0	t	2026-09-24 20:54:10.756273	2026-09-24 20:54:10.756273	\N
134	Điện thoại Xiaomi 17 Ultra 5G 16GB/512GB	dien-thoai-xiaomi-17-ultra-5g-16gb-512gb	\N	3	4	30290000.00	\N	0	TGDD-361270	\N	12	0.00	0	0	t	2026-09-24 20:54:10.775407	2026-09-24 20:54:10.775407	\N
135	Điện thoại Nokia 3210 4G	dien-thoai-nokia-3210-4g	\N	12	4	1490000.00	\N	0	TGDD-326477	\N	12	0.00	0	0	t	2026-09-24 20:54:10.794355	2026-09-24 20:54:10.794355	\N
136	Điện thoại Xiaomi 17 5G 12GB/256GB	dien-thoai-xiaomi-17-5g-12gb-256gb	\N	3	4	19090000.00	\N	0	TGDD-361269	\N	12	0.00	0	0	t	2026-09-24 20:54:10.81259	2026-09-24 20:54:10.81259	\N
137	Điện thoại Xphone Hera S9	dien-thoai-xphone-hera-s9	\N	16	4	590000.00	\N	0	TGDD-370547	\N	12	0.00	0	0	t	2026-09-24 20:54:10.847739	2026-09-24 20:54:10.847739	\N
138	Điện thoại Motorola Signature 12GB/256GB	dien-thoai-motorola-signature-12gb-256gb	\N	6	4	17990000.00	\N	0	TGDD-362375	\N	12	0.00	0	0	t	2026-09-24 20:54:10.867592	2026-09-24 20:54:10.867592	\N
139	Điện thoại HONOR X6c 6GB/128GB	dien-thoai-honor-x6c-6gb-128gb	\N	5	4	4340000.00	\N	0	TGDD-340220	\N	12	0.00	0	0	t	2026-09-24 20:54:10.885796	2026-09-24 20:54:10.885796	\N
140	Điện thoại OPPO Reno14 F 5G 12GB/256GB	dien-thoai-oppo-reno14-f-5g-12gb-256gb	\N	8	4	10620000.00	\N	0	TGDD-339177	\N	12	0.00	0	0	t	2026-09-24 20:54:10.904082	2026-09-24 20:54:10.904082	\N
141	Điện thoại Xiaomi 15T 5G 12GB/512GB	dien-thoai-xiaomi-15t-5g-12gb-512gb	\N	3	4	11590000.00	\N	0	TGDD-344645	\N	12	0.00	0	0	t	2026-09-24 20:54:10.922325	2026-09-24 20:54:10.922325	\N
142	Điện thoại Xiaomi POCO M8 5G 8GB/256GB	dien-thoai-xiaomi-poco-m8-5g-8gb-256gb	\N	3	4	5890000.00	\N	0	TGDD-363468	\N	12	0.00	0	0	t	2026-09-24 20:54:10.942077	2026-09-24 20:54:10.942077	\N
143	Điện thoại Samsung Galaxy S25 Edge 5G 12GB/512GB	dien-thoai-samsung-galaxy-s25-edge-5g-12gb-512gb	\N	7	4	19000000.00	\N	0	TGDD-335955	\N	12	0.00	0	0	t	2026-09-24 20:54:10.960846	2026-09-24 20:54:10.960846	\N
144	Điện thoại realme 16 Pro 5G 12GB/256GB	dien-thoai-realme-16-pro-5g-12gb-256gb	\N	4	4	11940000.00	\N	0	TGDD-361707	\N	12	0.00	0	0	t	2026-09-24 20:54:10.978993	2026-09-24 20:54:10.978993	\N
145	Điện thoại Nothing Phone (4a) Pro 5G 12GB/256GB	dien-thoai-nothing-phone-4a-pro-5g-12gb-256gb	\N	10	4	14740000.00	\N	0	TGDD-368875	\N	12	0.00	0	0	t	2026-09-24 20:54:10.998019	2026-09-24 20:54:10.998019	\N
146	Điện thoại OPPO Reno13 5G 12GB/256GB	dien-thoai-oppo-reno13-5g-12gb-256gb	\N	8	4	11230000.00	\N	0	TGDD-332934	\N	12	0.00	0	0	t	2026-09-24 20:54:11.016579	2026-09-24 20:54:11.016579	\N
147	Điện thoại Xiaomi Redmi Note 14 Pro 5G 12GB/512GB	dien-thoai-xiaomi-redmi-note-14-pro-5g-12gb-512gb	\N	3	4	8900000.00	\N	0	TGDD-337714	\N	12	0.00	0	0	t	2026-09-24 20:54:11.035112	2026-09-24 20:54:11.035112	\N
148	Điện thoại realme 15 5G 12GB/256GB	dien-thoai-realme-15-5g-12gb-256gb	\N	4	4	8440000.00	\N	0	TGDD-343066	\N	12	0.00	0	0	t	2026-09-24 20:54:11.054957	2026-09-24 20:54:11.054957	\N
149	Điện thoại Honor 600 Pro 12GB/256GB	dien-thoai-honor-600-pro-12gb-256gb	\N	5	4	21040000.00	\N	0	TGDD-367007	\N	12	0.00	0	0	t	2026-09-24 20:54:11.073784	2026-09-24 20:54:11.073784	\N
150	Điện thoại Nubia Air 5G 8GB/256GB	dien-thoai-nubia-air-5g-8gb-256gb	\N	14	4	5340000.00	\N	0	TGDD-366919	\N	12	0.00	0	0	t	2026-09-24 20:54:11.092507	2026-09-24 20:54:11.092507	\N
151	Điện thoại Samsung Galaxy A26 5G 6GB/128GB	dien-thoai-samsung-galaxy-a26-5g-6gb-128gb	\N	7	4	5800000.00	\N	0	TGDD-335915	\N	12	0.00	0	0	t	2026-09-24 20:54:11.112173	2026-09-24 20:54:11.112173	\N
152	Điện thoại Honor 600 8GB/256GB	dien-thoai-honor-600-8gb-256gb	\N	5	4	14840000.00	\N	0	TGDD-367010	\N	12	0.00	0	0	t	2026-09-24 20:54:11.130819	2026-09-24 20:54:11.130819	\N
153	Điện thoại Xiaomi 15T Pro 5G 12GB/256GB	dien-thoai-xiaomi-15t-pro-5g-12gb-256gb	\N	3	4	14390000.00	\N	0	TGDD-344646	\N	12	0.00	0	0	t	2026-09-24 20:54:11.149358	2026-09-24 20:54:11.149358	\N
154	Điện thoại OPPO A6t Pro 8GB/128GB	dien-thoai-oppo-a6t-pro-8gb-128gb	\N	8	4	10840000.00	\N	0	TGDD-367598	\N	12	0.00	0	0	t	2026-09-24 20:54:11.167636	2026-09-24 20:54:11.167636	\N
155	Điện thoại Xiaomi POCO F8 Pro 12GB/512GB	dien-thoai-xiaomi-poco-f8-pro-12gb-512gb	\N	3	4	16790000.00	\N	0	TGDD-362273	\N	12	0.00	0	0	t	2026-09-24 20:54:11.187142	2026-09-24 20:54:11.187142	\N
156	Điện thoại Xiaomi POCO X7 Pro 12GB/512GB	dien-thoai-xiaomi-poco-x7-pro-12gb-512gb	\N	3	4	10290000.00	\N	0	TGDD-362274	\N	12	0.00	0	0	t	2026-09-24 20:54:11.205135	2026-09-24 20:54:11.205135	\N
157	Điện thoại Xiaomi POCO F9 Ultra 5G 12GB/256GB	dien-thoai-xiaomi-poco-f9-ultra-5g-12gb-256gb	\N	3	4	24290000.00	\N	0	TGDD-370849	\N	12	0.00	0	0	t	2026-09-24 20:54:11.224459	2026-09-24 20:54:11.224459	\N
158	Điện thoại OPPO Find N3 Flip 5G 12GB/256GB Hồng	dien-thoai-oppo-find-n3-flip-5g-12gb-256gb-hong	\N	8	4	17430000.00	\N	0	TGDD-317981	\N	12	0.00	0	0	t	2026-09-24 20:54:11.242929	2026-09-24 20:54:11.242929	\N
159	Điện thoại OPPO Find X9 Ultra 12GB/512GB	dien-thoai-oppo-find-x9-ultra-12gb-512gb	\N	8	4	44840000.00	\N	0	TGDD-364791	\N	12	0.00	0	0	t	2026-09-24 20:54:11.260785	2026-09-24 20:54:11.260785	\N
160	Điện thoại Tecno Pova Curve 2 5G 8GB/128GB	dien-thoai-tecno-pova-curve-2-5g-8gb-128gb	\N	11	4	8740000.00	\N	0	TGDD-367617	\N	12	0.00	0	0	t	2026-09-24 20:54:11.279585	2026-09-24 20:54:11.279585	\N
161	Điện thoại Masstel IZI 10	dien-thoai-masstel-izi-10	\N	13	4	430000.00	\N	0	TGDD-265311	\N	12	0.00	0	0	t	2026-09-24 20:54:11.297952	2026-09-24 20:54:11.297952	\N
162	Điện thoại OPPO A5 Pro 5G 8GB/256GB	dien-thoai-oppo-a5-pro-5g-8gb-256gb	\N	8	4	7530000.00	\N	0	TGDD-334404	\N	12	0.00	0	0	t	2026-09-24 20:54:11.316953	2026-09-24 20:54:11.316953	\N
163	Điện thoại OPPO Find N6 5G 16GB/512GB	dien-thoai-oppo-find-n6-5g-16gb-512gb	\N	8	4	64840000.00	\N	0	TGDD-363258	\N	12	0.00	0	0	t	2026-09-24 20:54:11.336837	2026-09-24 20:54:11.336837	\N
164	Điện thoại HONOR X5c 4GB/64GB	dien-thoai-honor-x5c-4gb-64gb	\N	5	4	3290000.00	\N	0	TGDD-363410	\N	12	0.00	0	0	t	2026-09-24 20:54:11.356047	2026-09-24 20:54:11.356047	\N
165	Điện thoại Honor 600 Pro Molly 12GB/512GB Vàng Trắng	dien-thoai-honor-600-pro-molly-12gb-512gb-vang-trang	\N	5	4	23540000.00	\N	0	TGDD-368098	\N	12	0.00	0	0	t	2026-09-24 20:54:11.374017	2026-09-24 20:54:11.374017	\N
166	Điện thoại realme 14 5G 12GB/256GB	dien-thoai-realme-14-5g-12gb-256gb	\N	4	4	10050000.00	\N	0	TGDD-336623	\N	12	0.00	0	0	t	2026-09-24 20:54:11.393059	2026-09-24 20:54:11.393059	\N
167	Điện thoại Samsung Galaxy Z Flip7 5G 12GB/256GB	dien-thoai-samsung-galaxy-z-flip7-5g-12gb-256gb	\N	7	4	23010000.00	\N	0	TGDD-338736	\N	12	0.00	0	0	t	2026-09-24 20:54:11.411553	2026-09-24 20:54:11.411553	\N
168	Điện thoại Tecno Spark 50 Pro 4GB/128GB	dien-thoai-tecno-spark-50-pro-4gb-128gb	\N	11	4	6340000.00	\N	0	TGDD-368231	\N	12	0.00	0	0	t	2026-09-24 20:54:11.4373	2026-09-24 20:54:11.4373	\N
169	Điện thoại Nothing Phone (4b) 5G 8GB/128GB	dien-thoai-nothing-phone-4b-5g-8gb-128gb	\N	10	4	10490000.00	\N	0	TGDD-371029	\N	12	0.00	0	0	t	2026-09-24 20:54:11.455316	2026-09-24 20:54:11.455316	\N
170	Điện thoại vivo V80 Lite 5G 6GB/128GB	dien-thoai-vivo-v80-lite-5g-6gb-128gb	\N	9	4	0.00	\N	0	TGDD-371119	\N	12	0.00	0	0	t	2026-09-24 20:54:11.475079	2026-09-24 20:54:11.475079	\N
171	Điện thoại vivo V80 Lite 5G 6GB/256GB	dien-thoai-vivo-v80-lite-5g-6gb-256gb	\N	9	4	0.00	\N	0	TGDD-371120	\N	12	0.00	0	0	t	2026-09-24 20:54:11.493219	2026-09-24 20:54:11.493219	\N
172	Điện thoại OPPO A7 Pro 5G 8GB/128GB	dien-thoai-oppo-a7-pro-5g-8gb-128gb	\N	8	4	0.00	\N	0	TGDD-371195	\N	12	0.00	0	0	t	2026-09-24 20:54:11.512139	2026-09-24 20:54:11.512139	\N
173	Điện thoại OPPO A7 Pro 5G 6GB/256GB	dien-thoai-oppo-a7-pro-5g-6gb-256gb	\N	8	4	0.00	\N	0	TGDD-371196	\N	12	0.00	0	0	t	2026-09-24 20:54:11.531507	2026-09-24 20:54:11.531507	\N
174	Điện thoại OPPO A7 Pro 5G 8GB/256GB	dien-thoai-oppo-a7-pro-5g-8gb-256gb	\N	8	4	0.00	\N	0	TGDD-371197	\N	12	0.00	0	0	t	2026-09-24 20:54:11.550711	2026-09-24 20:54:11.550711	\N
175	Điện thoại OPPO Find X10 Pro Max 5G 12GB/512GB	dien-thoai-oppo-find-x10-pro-max-5g-12gb-512gb	\N	8	4	0.00	\N	0	TGDD-371315	\N	12	0.00	0	0	t	2026-09-24 20:54:11.569219	2026-09-24 20:54:11.569219	\N
176	Điện thoại OPPO Find X10 5G 12GB/512GB	dien-thoai-oppo-find-x10-5g-12gb-512gb	\N	8	4	0.00	\N	0	TGDD-371316	\N	12	0.00	0	0	t	2026-09-24 20:54:11.589295	2026-09-24 20:54:11.589295	\N
177	Điện thoại OPPO Find X10 5G 12GB/256GB	dien-thoai-oppo-find-x10-5g-12gb-256gb	\N	8	4	0.00	\N	0	TGDD-371317	\N	12	0.00	0	0	t	2026-09-24 20:54:11.609147	2026-09-24 20:54:11.609147	\N
178	Laptop HP 15 fc0023AU - D0BH1PA (R5 7520U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fc0023au-d0bh1pa-r5-7520u-16gb-512gb-full-hd-win11	\N	17	5	21290000.00	\N	0	TGDD-361311	\N	12	0.00	0	0	t	2026-09-24 20:54:11.662617	2026-09-24 20:54:11.662617	\N
179	Laptop MacBook Neo 13 inch A18 Pro 8GB/256GB	laptop-macbook-neo-13-inch-a18-pro-8gb-256gb	\N	18	5	18990000.00	\N	0	TGDD-363537	\N	12	0.00	0	0	t	2026-09-24 20:54:11.699482	2026-09-24 20:54:11.699482	\N
180	Laptop Asus Vivobook S14 S3407VA - LY146W (Core 5 210H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-s14-s3407va-ly146w-core-5-210h-16gb-512gb-wuxga-win11	\N	19	5	21490000.00	\N	0	TGDD-358132	\N	12	0.00	0	0	t	2026-09-24 20:54:11.735677	2026-09-24 20:54:11.735677	\N
181	Laptop Dell 15 DC15255 - DC5R5973W1-2Y (R5 7530U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15255-dc5r5973w1-2y-r5-7530u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	23990000.00	\N	0	TGDD-368556	\N	12	0.00	0	0	t	2026-09-24 20:54:11.769387	2026-09-24 20:54:11.769387	\N
182	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR00A4VN (Ultra 5 322, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iph11-83ur00a4vn-ultra-5-322-16gb-512gb-wuxga-win11	\N	21	5	23690000.00	\N	0	TGDD-367016	\N	12	0.00	0	0	t	2026-09-24 20:54:11.802774	2026-09-24 20:54:11.802774	\N
183	Laptop HP 240R G10 - C3RU7AT (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-hp-240r-g10-c3ru7at-core-5-120u-16gb-512gb-full-hd-win11	\N	17	5	22990000.00	\N	0	TGDD-355729	\N	12	0.00	0	0	t	2026-09-24 20:54:11.82076	2026-09-24 20:54:11.82076	\N
184	Laptop Dell 15 DC15250 - DC5I5897W1 (i5 1334U, 16GB, 512GB, Full HD+ 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc5i5897w1-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	22990000.00	\N	0	TGDD-362025	\N	12	0.00	0	0	t	2026-09-24 20:54:11.842824	2026-09-24 20:54:11.842824	\N
185	Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W	laptop-macbook-air-13-inch-m5-16gb-512gb-8gpu-70w	\N	18	5	35290000.00	\N	0	TGDD-363487	\N	12	0.00	0	0	t	2026-09-24 20:54:11.864185	2026-09-24 20:54:11.864185	\N
186	Laptop MSI Modern 15 F1MG-1264VN (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-msi-modern-15-f1mg-1264vn-core-5-120u-16gb-512gb-full-hd-win11	\N	22	5	19990000.00	\N	0	TGDD-369212	\N	12	0.00	0	0	t	2026-09-24 20:54:11.901705	2026-09-24 20:54:11.901705	\N
187	Laptop Asus Vivobook 16 A1607QA - MB067W (X1 26 100, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-16-a1607qa-mb067w-x1-26-100-16gb-512gb-wuxga-win11	\N	19	5	19490000.00	\N	0	TGDD-364397	\N	12	0.00	0	0	t	2026-09-24 20:54:11.923154	2026-09-24 20:54:11.923154	\N
188	Laptop Acer Aspire Lite 14 AL14-44P-R0SP - NX.DMCSV.003 (R7 7730U, 8GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-lite-14-al14-44p-r0sp-nx-dmcsv-003-r7-7730u-8gb-512gb-full-hd-win11	\N	23	5	20490000.00	\N	0	TGDD-367375	\N	12	0.00	0	0	t	2026-09-24 20:54:11.958174	2026-09-24 20:54:11.958174	\N
189	Laptop HP 245 G10 - B8PF9AT (R5 7530U, 8GB, 512GB, Full HD, Win11)	laptop-hp-245-g10-b8pf9at-r5-7530u-8gb-512gb-full-hd-win11	\N	17	5	19290000.00	\N	0	TGDD-337420	\N	12	0.00	0	0	t	2026-09-24 20:54:11.975859	2026-09-24 20:54:11.975859	\N
190	Laptop Dell 15 DC15250 - 71092479 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365B, Win11)	laptop-dell-15-dc15250-71092479-i5-1334u-16gb-512gb-full-hd-120hz-officehs24-365b-win11	\N	20	5	22990000.00	\N	0	TGDD-364859	\N	12	0.00	0	0	t	2026-09-24 20:54:11.993842	2026-09-24 20:54:11.993842	\N
191	Laptop SingPC M16-i382 (i3 1215U, 8GB, 256GB, WUXGA, Win11 Pro)	laptop-singpc-m16-i382-i3-1215u-8gb-256gb-wuxga-win11-pro	\N	24	5	13490000.00	\N	0	TGDD-368189	\N	12	0.00	0	0	t	2026-09-24 20:54:12.028684	2026-09-24 20:54:12.028684	\N
192	Laptop Acer Aspire Go 15 AG15-72P-500W - NX.JRRSV.007 (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-go-15-ag15-72p-500w-nx-jrrsv-007-core-5-120u-16gb-512gb-full-hd-win11	\N	23	5	19990000.00	\N	0	TGDD-363263	\N	12	0.00	0	0	t	2026-09-24 20:54:12.047094	2026-09-24 20:54:12.047094	\N
193	Laptop Lenovo IdeaPad Slim 3 15ARP10 - 83K700YUVN (R5 7535HS, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15arp10-83k700yuvn-r5-7535hs-16gb-512gb-wuxga-win11	\N	21	5	21490000.00	\N	0	TGDD-366746	\N	12	0.00	0	0	t	2026-09-24 20:54:12.06703	2026-09-24 20:54:12.06703	\N
194	Laptop Acer Aspire Lite 15 AL15-49P-R6XX - NX.DRZSV.002 (R5 7430U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-49p-r6xx-nx-drzsv-002-r5-7430u-8gb-512gb-full-hd-win11	\N	23	5	18690000.00	\N	0	TGDD-367376	\N	12	0.00	0	0	t	2026-09-24 20:54:12.085182	2026-09-24 20:54:12.085182	\N
195	Laptop MacBook Air 15 inch M5 16GB/512GB 70W	laptop-macbook-air-15-inch-m5-16gb-512gb-70w	\N	18	5	41290000.00	\N	0	TGDD-363507	\N	12	0.00	0	0	t	2026-09-24 20:54:12.103279	2026-09-24 20:54:12.103279	\N
196	Laptop MSI Modern 15 F13MG - 667VN_16GB (i5 1334U, 16GB, 512GB, Full HD, Win11)	laptop-msi-modern-15-f13mg-667vn-16gb-i5-1334u-16gb-512gb-full-hd-win11	\N	22	5	18190000.00	\N	0	TGDD-342941	\N	12	0.00	0	0	t	2026-09-24 20:54:12.120953	2026-09-24 20:54:12.120953	\N
197	Laptop Asus Vivobook 15 X1504VA - BQ295W (Core 7 150U, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504va-bq295w-core-7-150u-16gb-512gb-full-hd-win11	\N	19	5	22090000.00	\N	0	TGDD-360420	\N	12	0.00	0	0	t	2026-09-24 20:54:12.141129	2026-09-24 20:54:12.141129	\N
198	Laptop HP OmniBook 5 Flip 14 fp0057TU - BZ7Q6PA (Core 5 120U, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-5-flip-14-fp0057tu-bz7q6pa-core-5-120u-16gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	26490000.00	\N	0	TGDD-358078	\N	12	0.00	0	0	t	2026-09-24 20:54:12.159544	2026-09-24 20:54:12.159544	\N
278	Laptop Lenovo IdeaPad Slim 3 14IWC11 - 83RQ002NVN (Core 5 320, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-14iwc11-83rq002nvn-core-5-320-16gb-512gb-wuxga-win11	\N	21	5	22990000.00	\N	0	TGDD-368207	\N	12	0.00	0	0	t	2026-09-24 20:54:13.612086	2026-09-24 20:54:13.612086	\N
199	Laptop HP Gaming VICTUS 15 fa2731TX - B85LNPA (i5 13420H, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	laptop-hp-gaming-victus-15-fa2731tx-b85lnpa-i5-13420h-16gb-512gb-rtx-3050-6gb-full-hd-144hz-win11	\N	17	5	26490000.00	\N	0	TGDD-338204	\N	12	0.00	0	0	t	2026-09-24 20:54:12.178781	2026-09-24 20:54:12.178781	\N
200	Laptop Asus Vivobook Go 15 E1504FA - BQ374W (R5 40, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-go-15-e1504fa-bq374w-r5-40-8gb-512gb-full-hd-win11	\N	19	5	18890000.00	\N	0	TGDD-362620	\N	12	0.00	0	0	t	2026-09-24 20:54:12.197456	2026-09-24 20:54:12.197456	\N
201	Laptop Dell 15 DC15250 - CPH99 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	laptop-dell-15-dc15250-cph99-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365b-win11	\N	20	5	22490000.00	\N	0	TGDD-357990	\N	12	0.00	0	0	t	2026-09-24 20:54:12.215042	2026-09-24 20:54:12.215042	\N
202	Laptop Asus TUF Gaming FA506NCQ - HN005W (R7 170, 16GB, 512GB, RTX 3050 4GB, Full HD 144Hz, Win11)	laptop-asus-tuf-gaming-fa506ncq-hn005w-r7-170-16gb-512gb-rtx-3050-4gb-full-hd-144hz-win11	\N	19	5	27490000.00	\N	0	TGDD-362621	\N	12	0.00	0	0	t	2026-09-24 20:54:12.234468	2026-09-24 20:54:12.234468	\N
203	Laptop Lenovo Gaming LOQ 15ARP10 - 83S000CNVN (R7 170, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15arp10-83s000cnvn-r7-170-16gb-512gb-rtx-3050-6gb-full-hd-144hz-win11	\N	21	5	29490000.00	\N	0	TGDD-363903	\N	12	0.00	0	0	t	2026-09-24 20:54:12.252799	2026-09-24 20:54:12.252799	\N
204	Laptop Acer Aspire Go 15 AG15-52P-52WT - NX.JWKSV.001 (Ultra 5 115U, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-go-15-ag15-52p-52wt-nx-jwksv-001-ultra-5-115u-16gb-512gb-full-hd-win11	\N	23	5	22990000.00	\N	0	TGDD-366090	\N	12	0.00	0	0	t	2026-09-24 20:54:12.271847	2026-09-24 20:54:12.271847	\N
205	Laptop Lenovo Ideapad Slim 3 15IWC11 - 83RR00CVVN (Core 5 320, 8GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iwc11-83rr00cvvn-core-5-320-8gb-512gb-wuxga-win11	\N	21	5	19190000.00	\N	0	TGDD-368211	\N	12	0.00	0	0	t	2026-09-24 20:54:12.289959	2026-09-24 20:54:12.289959	\N
206	Laptop HP OmniBook 5 AI 16 af1048TU - BZ7Q9PA (Ultra 5 225U, 16GB, 512GB, WUXGA, OfficeH24, Win11)	laptop-hp-omnibook-5-ai-16-af1048tu-bz7q9pa-ultra-5-225u-16gb-512gb-wuxga-officeh24-win11	\N	17	5	25990000.00	\N	0	TGDD-341267	\N	12	0.00	0	0	t	2026-09-24 20:54:12.308646	2026-09-24 20:54:12.308646	\N
207	Laptop Dell 14 DC14250 - DC4C5386W (Core 5 120U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-14-dc14250-dc4c5386w-core-5-120u-16gb-512gb-full-hd-officeh24-365-win11	\N	20	5	26990000.00	\N	0	TGDD-360690	\N	12	0.00	0	0	t	2026-09-24 20:54:12.326729	2026-09-24 20:54:12.326729	\N
208	Laptop Acer Gaming Aspire 7 A715-59G-59RD - NH.DXUSV.001 (Core 5 210H, 16GB, 512GB, RTX 3050 4GB, Fulll HD 144Hz, Win11)	laptop-acer-gaming-aspire-7-a715-59g-59rd-nh-dxusv-001-core-5-210h-16gb-512gb-rtx-3050-4gb-fulll-hd-144hz-win11	\N	23	5	26990000.00	\N	0	TGDD-368656	\N	12	0.00	0	0	t	2026-09-24 20:54:12.344341	2026-09-24 20:54:12.344341	\N
209	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU-27 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-dc15250-i5u165w11slu-27-i5-1334u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	22990000.00	\N	0	TGDD-365310	\N	12	0.00	0	0	t	2026-09-24 20:54:12.3623	2026-09-24 20:54:12.3623	\N
210	Laptop HP 15 fd1486TU - D0BH3PA (Ultra 5 125H, 24GB, 512GB, Full HD, Win11)	laptop-hp-15-fd1486tu-d0bh3pa-ultra-5-125h-24gb-512gb-full-hd-win11	\N	17	5	23990000.00	\N	0	TGDD-362398	\N	12	0.00	0	0	t	2026-09-24 20:54:12.380276	2026-09-24 20:54:12.380276	\N
211	Laptop Lenovo IdeaPad Slim 3 15IWC11 - 83RR00AAVN (Core 5 320, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iwc11-83rr00aavn-core-5-320-16gb-512gb-wuxga-win11	\N	21	5	22990000.00	\N	0	TGDD-368210	\N	12	0.00	0	0	t	2026-09-24 20:54:12.399026	2026-09-24 20:54:12.399026	\N
212	Laptop SingPC M16-i595 (i5 1235U, 16GB, 512GB, WUXGA, Win11 Pro)	laptop-singpc-m16-i595-i5-1235u-16gb-512gb-wuxga-win11-pro	\N	24	5	17390000.00	\N	0	TGDD-368192	\N	12	0.00	0	0	t	2026-09-24 20:54:12.417286	2026-09-24 20:54:12.417286	\N
213	Laptop Acer Aspire Lite 15 AL15-21P-R91W - NX.DNRSV.002 (R5 40, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-21p-r91w-nx-dnrsv-002-r5-40-16gb-512gb-full-hd-win11	\N	23	5	19990000.00	\N	0	TGDD-366087	\N	12	0.00	0	0	t	2026-09-24 20:54:12.4365	2026-09-24 20:54:12.4365	\N
214	Laptop Acer Nitro ProPanel ANV15-52-50RB - NH.QUASV.002 (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, Full HD 180Hz, Win11)	laptop-acer-nitro-propanel-anv15-52-50rb-nh-quasv-002-core-5-210h-16gb-512gb-rtx-4050-6gb-full-hd-180hz-win11	\N	23	5	33490000.00	\N	0	TGDD-369645	\N	12	0.00	0	0	t	2026-09-24 20:54:12.454723	2026-09-24 20:54:12.454723	\N
215	Laptop MacBook Pro 16 inch M5 Pro 48GB/1TB	laptop-macbook-pro-16-inch-m5-pro-48gb-1tb	\N	18	5	95990000.00	\N	0	TGDD-363492	\N	12	0.00	0	0	t	2026-09-24 20:54:12.472895	2026-09-24 20:54:12.472895	\N
216	Laptop Acer Gaming Predator Helios Neo 16 AI PHN16 73 757W - NH.QVQSV.001 (Ultra 7 255HX, 32GB, 1TB, RTX 5060 8GB, 2K+ 240Hz, Win11)	laptop-acer-gaming-predator-helios-neo-16-ai-phn16-73-757w-nh-qvqsv-001-ultra-7-255hx-32gb-1tb-rtx-5060-8gb-2k-240hz-win11	\N	23	5	60990000.00	\N	0	TGDD-360295	\N	12	0.00	0	0	t	2026-09-24 20:54:12.490386	2026-09-24 20:54:12.490386	\N
217	Laptop Dell Pro 14 Essential PV14250 - PV14250-120U-16512W-BL (Core 5 120U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-pro-14-essential-pv14250-pv14250-120u-16512w-bl-core-5-120u-16gb-512gb-full-hd-officeh24-365-win11	\N	20	5	27990000.00	\N	0	TGDD-362024	\N	12	0.00	0	0	t	2026-09-24 20:54:12.508152	2026-09-24 20:54:12.508152	\N
218	Laptop Asus Vivobook 14 X1407CA - LY008W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-14-x1407ca-ly008w-ultra-5-225h-16gb-512gb-wuxga-win11	\N	19	5	22190000.00	\N	0	TGDD-362410	\N	12	0.00	0	0	t	2026-09-24 20:54:12.52702	2026-09-24 20:54:12.52702	\N
219	Laptop HP Gaming VICTUS 15 fb3116AX - BX8U4PA (R7 7445HS, 16GB, 512GB, RTX3050 6GB, Full HD 144Hz, Win11)	laptop-hp-gaming-victus-15-fb3116ax-bx8u4pa-r7-7445hs-16gb-512gb-rtx3050-6gb-full-hd-144hz-win11	\N	17	5	26490000.00	\N	0	TGDD-341623	\N	12	0.00	0	0	t	2026-09-24 20:54:12.545855	2026-09-24 20:54:12.545855	\N
220	Laptop Lenovo IdeaPad Slim 3 14IWC11 - 83RQ002PVN (Core 5 320, 8GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-14iwc11-83rq002pvn-core-5-320-8gb-512gb-wuxga-win11	\N	21	5	18990000.00	\N	0	TGDD-368208	\N	12	0.00	0	0	t	2026-09-24 20:54:12.565829	2026-09-24 20:54:12.565829	\N
221	Laptop Asus Vivobook Go 15 E1504FA - BQ350W (R5 40, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-go-15-e1504fa-bq350w-r5-40-16gb-512gb-full-hd-win11	\N	19	5	20790000.00	\N	0	TGDD-362406	\N	12	0.00	0	0	t	2026-09-24 20:54:12.583778	2026-09-24 20:54:12.583778	\N
222	Laptop Dell 15 DC15250 - DC5I7748W1 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc5i7748w1-i7-1355u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	25490000.00	\N	0	TGDD-340562	\N	12	0.00	0	0	t	2026-09-24 20:54:12.601997	2026-09-24 20:54:12.601997	\N
223	Laptop HP Probook 455 G10 - B8PG7AT (R5 7530U, 16GB, 512GB, Full HD, Win11)	laptop-hp-probook-455-g10-b8pg7at-r5-7530u-16gb-512gb-full-hd-win11	\N	17	5	19990000.00	\N	0	TGDD-340484	\N	12	0.00	0	0	t	2026-09-24 20:54:12.620735	2026-09-24 20:54:12.620735	\N
224	Laptop Lenovo IdeaPad Slim 3 14ARP10 - 83K600E6VN (R5 7535HS, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-14arp10-83k600e6vn-r5-7535hs-16gb-512gb-wuxga-win11	\N	21	5	21390000.00	\N	0	TGDD-366745	\N	12	0.00	0	0	t	2026-09-24 20:54:12.639571	2026-09-24 20:54:12.639571	\N
225	Laptop Asus Vivobook S16 S3607VA - RP155WS (Core 5 210H, 16GB, 512GB, WUXGA 144Hz, OfficeH24+365, Win11)	laptop-asus-vivobook-s16-s3607va-rp155ws-core-5-210h-16gb-512gb-wuxga-144hz-officeh24-365-win11	\N	19	5	21990000.00	\N	0	TGDD-342758	\N	12	0.00	0	0	t	2026-09-24 20:54:12.658523	2026-09-24 20:54:12.658523	\N
226	Laptop Acer Gaming Nitro ProPanel ANV16-72-71T9 - NH.QUNSV.001 (Core 7 240H, 16GB, 512GB, RTX 5060 8GB, WUXGA 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv16-72-71t9-nh-qunsv-001-core-7-240h-16gb-512gb-rtx-5060-8gb-wuxga-180hz-win11	\N	23	5	43990000.00	\N	0	TGDD-366823	\N	12	0.00	0	0	t	2026-09-24 20:54:12.677903	2026-09-24 20:54:12.677903	\N
227	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV003CVN (i5 13450HX, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx9-83dv003cvn-i5-13450hx-16gb-512gb-rtx-3050-6gb-full-hd-144hz-win11	\N	21	5	29790000.00	\N	0	TGDD-335554	\N	12	0.00	0	0	t	2026-09-24 20:54:12.697567	2026-09-24 20:54:12.697567	\N
228	Laptop HP Pavilion 16 af0055TU - AY8C4PA (Ultra 5 125U, 16GB, 512GB, WUXGA, OfficeHS+365, Win11)	laptop-hp-pavilion-16-af0055tu-ay8c4pa-ultra-5-125u-16gb-512gb-wuxga-officehs-365-win11	\N	17	5	22390000.00	\N	0	TGDD-337044	\N	12	0.00	0	0	t	2026-09-24 20:54:12.716852	2026-09-24 20:54:12.716852	\N
229	Laptop Acer Aspire Lite 15 AL15-46P-R73C - NX.JXMSV.001 (R3 5400U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-46p-r73c-nx-jxmsv-001-r3-5400u-8gb-512gb-full-hd-win11	\N	23	5	15990000.00	\N	0	TGDD-366086	\N	12	0.00	0	0	t	2026-09-24 20:54:12.73544	2026-09-24 20:54:12.73544	\N
230	Laptop Dell 15 DC15250 - DC5I5357W1 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc5i5357w1-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	22490000.00	\N	0	TGDD-340561	\N	12	0.00	0	0	t	2026-09-24 20:54:12.755635	2026-09-24 20:54:12.755635	\N
231	Laptop Dell Inspiron 14 5441 - 5MNK1 (X1P 64 100, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-inspiron-14-5441-5mnk1-x1p-64-100-16gb-512gb-full-hd-officeh24-365-win11	\N	20	5	25990000.00	\N	0	TGDD-339682	\N	12	0.00	0	0	t	2026-09-24 20:54:12.77408	2026-09-24 20:54:12.77408	\N
232	Laptop HP Gaming VICTUS 15 fb3115AX - BX9C9PA (R7 7445HS, 16GB, 512GB, RTX4050 6GB, Full HD 144Hz, Win11)	laptop-hp-gaming-victus-15-fb3115ax-bx9c9pa-r7-7445hs-16gb-512gb-rtx4050-6gb-full-hd-144hz-win11	\N	17	5	29990000.00	\N	0	TGDD-341624	\N	12	0.00	0	0	t	2026-09-24 20:54:12.79242	2026-09-24 20:54:12.79242	\N
233	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01ANVN (i5 13450HX, 16GB, 1TB, RTX 3050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx9-83dv01anvn-i5-13450hx-16gb-1tb-rtx-3050-6gb-full-hd-144hz-win11	\N	21	5	29290000.00	\N	0	TGDD-342524	\N	12	0.00	0	0	t	2026-09-24 20:54:12.810431	2026-09-24 20:54:12.810431	\N
234	Laptop Acer Gaming Nitro ProPanel ANV15 52 72BM - NH.QZ9SV.004 (i7 13620H, 16GB, 512GB, RTX5050 8GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv15-52-72bm-nh-qz9sv-004-i7-13620h-16gb-512gb-rtx5050-8gb-full-hd-180hz-win11	\N	23	5	36990000.00	\N	0	TGDD-341607	\N	12	0.00	0	0	t	2026-09-24 20:54:12.827817	2026-09-24 20:54:12.827817	\N
235	Laptop Acer Gaming Nitro ProPanel AN16S 61 R193 - NH.QXTSV.001 (R9 AI 365, 16GB, 512GB, RTX 5070 8GB, 2K+ 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-an16s-61-r193-nh-qxtsv-001-r9-ai-365-16gb-512gb-rtx-5070-8gb-2k-180hz-win11	\N	23	5	59990000.00	\N	0	TGDD-360298	\N	12	0.00	0	0	t	2026-09-24 20:54:12.845305	2026-09-24 20:54:12.845305	\N
236	Laptop Acer Aspire Lite 16 AL16-71P-582Q - NX.DRSSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+ 120Hz, Win11)	laptop-acer-aspire-lite-16-al16-71p-582q-nx-drssv-001-ultra-5-125h-16gb-512gb-full-hd-120hz-win11	\N	23	5	24990000.00	\N	0	TGDD-368655	\N	12	0.00	0	0	t	2026-09-24 20:54:12.862633	2026-09-24 20:54:12.862633	\N
237	Laptop Asus VivoBook Go 14 E1404FA - EB945W (R5 40, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-go-14-e1404fa-eb945w-r5-40-16gb-512gb-full-hd-win11	\N	19	5	20690000.00	\N	0	TGDD-364183	\N	12	0.00	0	0	t	2026-09-24 20:54:12.879462	2026-09-24 20:54:12.879976	\N
238	Laptop Dell 14 DC14250 - DC14250-C3U085W11SLU-27 (Core 3 100U, 8GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-14-dc14250-dc14250-c3u085w11slu-27-core-3-100u-8gb-512gb-full-hd-officeh24-365-win11	\N	20	5	18990000.00	\N	0	TGDD-367346	\N	12	0.00	0	0	t	2026-09-24 20:54:12.896989	2026-09-24 20:54:12.896989	\N
239	Laptop HP OmniBook 5 AI 16 af1046TU - BZ7Q8PA (Ultra 5 225U, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-5-ai-16-af1046tu-bz7q8pa-ultra-5-225u-16gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	26990000.00	\N	0	TGDD-341266	\N	12	0.00	0	0	t	2026-09-24 20:54:12.914904	2026-09-24 20:54:12.914904	\N
240	Laptop HP 15 fd1487TU - D0BH4PA (Ultra 5 125H, 24GB, 512GB, Full HD, Win11)	laptop-hp-15-fd1487tu-d0bh4pa-ultra-5-125h-24gb-512gb-full-hd-win11	\N	17	5	23990000.00	\N	0	TGDD-365624	\N	12	0.00	0	0	t	2026-09-24 20:54:12.934534	2026-09-24 20:54:12.934534	\N
241	Laptop Asus Vivobook S14 S3407CA - LY095WS (Ultra 5 225H, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-asus-vivobook-s14-s3407ca-ly095ws-ultra-5-225h-16gb-512gb-wuxga-officeh24-365-win11	\N	19	5	23590000.00	\N	0	TGDD-338320	\N	12	0.00	0	0	t	2026-09-24 20:54:12.951757	2026-09-24 20:54:12.951757	\N
242	Laptop Dell 15 DC15250 - 71084747 (i7 1355U, 16GB, 512GB, Full HD 120 Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-71084747-i7-1355u-16gb-512gb-full-hd-120-hz-officehs24-365-win11	\N	20	5	25490000.00	\N	0	TGDD-360692	\N	12	0.00	0	0	t	2026-09-24 20:54:12.970478	2026-09-24 20:54:12.970478	\N
243	Laptop Acer Aspire Lite 15 AL15-48P-R86Q - NX.DS1SV.002 (R7 5825U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-48p-r86q-nx-ds1sv-002-r7-5825u-8gb-512gb-full-hd-win11	\N	23	5	18990000.00	\N	0	TGDD-366992	\N	12	0.00	0	0	t	2026-09-24 20:54:12.988772	2026-09-24 20:54:12.988772	\N
244	Laptop Lenovo IdeaPad Slim 3 15IRH10 - 83K1000FVN (i7 13620H, 24GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15irh10-83k1000fvn-i7-13620h-24gb-512gb-wuxga-win11	\N	21	5	23390000.00	\N	0	TGDD-334446	\N	12	0.00	0	0	t	2026-09-24 20:54:13.005952	2026-09-24 20:54:13.005952	\N
245	Laptop Acer Aspire Lite 16 G2 AL16 52P 76DU - NX.J2SSV.005 (i7 1355U, 16GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-lite-16-g2-al16-52p-76du-nx-j2ssv-005-i7-1355u-16gb-512gb-full-hd-win11	\N	23	5	19990000.00	\N	0	TGDD-333421	\N	12	0.00	0	0	t	2026-09-24 20:54:13.023205	2026-09-24 20:54:13.023205	\N
246	Laptop Acer Gaming Nitro V 15 ProPanel ANV15 41 R0FE - NH.QPFSV.005 (R7 7735HS, 16GB, 512GB, RTX 3050 6GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-v-15-propanel-anv15-41-r0fe-nh-qpfsv-005-r7-7735hs-16gb-512gb-rtx-3050-6gb-full-hd-180hz-win11	\N	23	5	29990000.00	\N	0	TGDD-341601	\N	12	0.00	0	0	t	2026-09-24 20:54:13.041233	2026-09-24 20:54:13.041762	\N
247	Laptop MSI Modern 14 F1MG - 432VN (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-msi-modern-14-f1mg-432vn-core-5-120u-16gb-512gb-full-hd-win11	\N	22	5	20890000.00	\N	0	TGDD-339954	\N	12	0.00	0	0	t	2026-09-24 20:54:13.058707	2026-09-24 20:54:13.058707	\N
248	Laptop MSI Gaming Cyborg 15 A13VEK - 1423VN (i7 13620H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-a13vek-1423vn-i7-13620h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	22	5	29990000.00	\N	0	TGDD-327707	\N	12	0.00	0	0	t	2026-09-24 20:54:13.07604	2026-09-24 20:54:13.07604	\N
249	Laptop Asus VivoBook 16 X1607CA - MB980W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-16-x1607ca-mb980w-ultra-5-225h-16gb-512gb-wuxga-win11	\N	19	5	22190000.00	\N	0	TGDD-363257	\N	12	0.00	0	0	t	2026-09-24 20:54:13.093322	2026-09-24 20:54:13.093322	\N
250	Laptop Dell 15 DC15255 - X9YM42 (R7 7730U, 16GB, 1TB, Full HD 120Hz, OfficeH24+36, Win11)	laptop-dell-15-dc15255-x9ym42-r7-7730u-16gb-1tb-full-hd-120hz-officeh24-36-win11	\N	20	5	29890000.00	\N	0	TGDD-369911	\N	12	0.00	0	0	t	2026-09-24 20:54:13.110194	2026-09-24 20:54:13.110194	\N
251	Laptop Lenovo IdeaPad Slim 3 14IPH11 - 83UQ003NVN (Ultra 5 322, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-14iph11-83uq003nvn-ultra-5-322-16gb-512gb-wuxga-win11	\N	21	5	23690000.00	\N	0	TGDD-367684	\N	12	0.00	0	0	t	2026-09-24 20:54:13.127156	2026-09-24 20:54:13.127156	\N
252	Laptop Dell 15 DC15250 - DC5I7952W1 (i7 1355U, 16GB, 512GB, Full HD+ 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc5i7952w1-i7-1355u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	25490000.00	\N	0	TGDD-362026	\N	12	0.00	0	0	t	2026-09-24 20:54:13.144795	2026-09-24 20:54:13.144795	\N
253	Laptop MSI Gaming Cyborg 15 A13UC - 2088VN (i5 13420H, 16GB, 512GB, RTX 3050 4GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-a13uc-2088vn-i5-13420h-16gb-512gb-rtx-3050-4gb-full-hd-144hz-win11	\N	22	5	26090000.00	\N	0	TGDD-363690	\N	12	0.00	0	0	t	2026-09-24 20:54:13.161717	2026-09-24 20:54:13.161717	\N
254	Laptop HP 15 fd0015TU - A19C5PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd0015tu-a19c5pa-i7-1355u-16gb-512gb-full-hd-win11	\N	17	5	21990000.00	\N	0	TGDD-325691	\N	12	0.00	0	0	t	2026-09-24 20:54:13.181922	2026-09-24 20:54:13.181922	\N
255	Laptop HP 15 fd1288TU - C2CV7PA (Ultra 7 155H, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd1288tu-c2cv7pa-ultra-7-155h-16gb-512gb-full-hd-win11	\N	17	5	25990000.00	\N	0	TGDD-358797	\N	12	0.00	0	0	t	2026-09-24 20:54:13.199873	2026-09-24 20:54:13.199873	\N
256	Laptop Acer Aspire Lite 16 AI AL16 71P 5674 - NX.D4XSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-lite-16-ai-al16-71p-5674-nx-d4xsv-001-ultra-5-125h-16gb-512gb-full-hd-win11	\N	23	5	19990000.00	\N	0	TGDD-341602	\N	12	0.00	0	0	t	2026-09-24 20:54:13.218164	2026-09-24 20:54:13.218164	\N
257	Laptop Lenovo Ideapad Slim 5 OLED 14AKP10 - 83HX00B2VN (R5 330, 16GB, 1TB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-5-oled-14akp10-83hx00b2vn-r5-330-16gb-1tb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-359486	\N	12	0.00	0	0	t	2026-09-24 20:54:13.236021	2026-09-24 20:54:13.236021	\N
258	Laptop HP 15 fd2127TU - D72CCPA (Ultra 5 225U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd2127tu-d72ccpa-ultra-5-225u-16gb-512gb-full-hd-win11	\N	17	5	24190000.00	\N	0	TGDD-367575	\N	12	0.00	0	0	t	2026-09-24 20:54:13.253636	2026-09-24 20:54:13.253636	\N
259	Laptop Dell 15 DC15250 - 71084746 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-71084746-i5-1334u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	22490000.00	\N	0	TGDD-360691	\N	12	0.00	0	0	t	2026-09-24 20:54:13.271493	2026-09-24 20:54:13.271493	\N
260	Laptop Dell 15 DC15250 - CPH991 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-cph991-i7-1355u-16gb-1tb-full-hd-120hz-officehs24-365-win11	\N	20	5	27490000.00	\N	0	TGDD-362020	\N	12	0.00	0	0	t	2026-09-24 20:54:13.288907	2026-09-24 20:54:13.288907	\N
261	Laptop HP 240R G9 - C40LGAT (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-hp-240r-g9-c40lgat-core-5-120u-16gb-512gb-full-hd-win11	\N	17	5	22990000.00	\N	0	TGDD-365608	\N	12	0.00	0	0	t	2026-09-24 20:54:13.30551	2026-09-24 20:54:13.30551	\N
262	Laptop Lenovo IdeaPad Slim 5 OLED 14IPH11 - 83S5000DVN (Ultra 5 322, 16GB, 512GB, WUXGA OLED, Win11)	laptop-lenovo-ideapad-slim-5-oled-14iph11-83s5000dvn-ultra-5-322-16gb-512gb-wuxga-oled-win11	\N	21	5	29990000.00	\N	0	TGDD-366741	\N	12	0.00	0	0	t	2026-09-24 20:54:13.322486	2026-09-24 20:54:13.322486	\N
263	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01H2VN (i7 13645HX, 16GB, 1TB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx9-83dv01h2vn-i7-13645hx-16gb-1tb-rtx-4050-6gb-full-hd-144hz-win11	\N	21	5	36990000.00	\N	0	TGDD-366749	\N	12	0.00	0	0	t	2026-09-24 20:54:13.339912	2026-09-24 20:54:13.339912	\N
264	Laptop Acer Gaming Nitro ProPanel ANV15-52-73Z8 - NH.QUASV.001 (Core 7 240H, 16GB, 512GB, RTX 4050 6GB, Full HD  180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv15-52-73z8-nh-quasv-001-core-7-240h-16gb-512gb-rtx-4050-6gb-full-hd-180hz-win11	\N	23	5	35990000.00	\N	0	TGDD-369754	\N	12	0.00	0	0	t	2026-09-24 20:54:13.358043	2026-09-24 20:54:13.358043	\N
265	Laptop Acer Gaming Nitro ProPanel ANV15 52 59AA - NH.QZ9SV.002 (i5 13420H, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv15-52-59aa-nh-qz9sv-002-i5-13420h-16gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	34990000.00	\N	0	TGDD-341626	\N	12	0.00	0	0	t	2026-09-24 20:54:13.375	2026-09-24 20:54:13.375	\N
266	Laptop Acer Gaming Nitro V ANV15 51 55CA - NH.QN8SV.004 (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-acer-gaming-nitro-v-anv15-51-55ca-nh-qn8sv-004-i5-13420h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	23	5	26490000.00	\N	0	TGDD-325697	\N	12	0.00	0	0	t	2026-09-24 20:54:13.392996	2026-09-24 20:54:13.392996	\N
267	Laptop Acer Aspire Go 14 AG14-72P-54DF - NX.JSBSV.009 (Core 5 120U, 16GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-go-14-ag14-72p-54df-nx-jsbsv-009-core-5-120u-16gb-512gb-full-hd-win11	\N	23	5	19990000.00	\N	0	TGDD-363261	\N	12	0.00	0	0	t	2026-09-24 20:54:13.409938	2026-09-24 20:54:13.409938	\N
268	Laptop HP OmniBook 5 Flip 14 fp0055TU - BZ7Q4PA (Core 7 150U, 24GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-5-flip-14-fp0055tu-bz7q4pa-core-7-150u-24gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	31490000.00	\N	0	TGDD-358077	\N	12	0.00	0	0	t	2026-09-24 20:54:13.426673	2026-09-24 20:54:13.426673	\N
269	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU-5 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc15250-i7u161w11slu-5-i7-1355u-16gb-1tb-full-hd-120hz-officeh24-365-win11	\N	20	5	27490000.00	\N	0	TGDD-362667	\N	12	0.00	0	0	t	2026-09-24 20:54:13.443639	2026-09-24 20:54:13.443639	\N
270	Laptop Dell 15 DC15250 - 71100520 (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-71100520-core-3-100u-8gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21490000.00	\N	0	TGDD-368558	\N	12	0.00	0	0	t	2026-09-24 20:54:13.460588	2026-09-24 20:54:13.460588	\N
271	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CMHI2VN893SH (i7 13620H, 16GB, 512GB, RTX 4050 6GB, WUXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-gaming-a16-cmhi2vn893sh-i7-13620h-16gb-512gb-rtx-4050-6gb-wuxga-165hz-win11	\N	25	5	31790000.00	\N	0	TGDD-339211	\N	12	0.00	0	0	t	2026-09-24 20:54:13.493539	2026-09-24 20:54:13.493539	\N
272	Laptop HP Gaming VICTUS 15 fa2451TX - D17WPPA (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-hp-gaming-victus-15-fa2451tx-d17wppa-i5-13420h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	17	5	28990000.00	\N	0	TGDD-367576	\N	12	0.00	0	0	t	2026-09-24 20:54:13.510934	2026-09-24 20:54:13.510934	\N
273	Laptop Acer Aspire 5 A515 58M 79R7 - NX.KQ8SV.007 (i7 13620H, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-5-a515-58m-79r7-nx-kq8sv-007-i7-13620h-16gb-512gb-full-hd-win11	\N	23	5	20690000.00	\N	0	TGDD-327507	\N	12	0.00	0	0	t	2026-09-24 20:54:13.52759	2026-09-24 20:54:13.52759	\N
274	Laptop Lenovo IdeaPad Slim 3 15IWC11 - 83RR00A8VN (Core 3 304, 8GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iwc11-83rr00a8vn-core-3-304-8gb-512gb-wuxga-win11	\N	21	5	17490000.00	\N	0	TGDD-369478	\N	12	0.00	0	0	t	2026-09-24 20:54:13.544132	2026-09-24 20:54:13.544132	\N
275	Laptop Dell 14 DC14250 - DC4C5375W1-2Y (Core 5 120U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-14-dc14250-dc4c5375w1-2y-core-5-120u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	28990000.00	\N	0	TGDD-368557	\N	12	0.00	0	0	t	2026-09-24 20:54:13.561361	2026-09-24 20:54:13.561361	\N
276	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc15250-i7u161w11slu-i7-1355u-16gb-1tb-full-hd-120hz-officeh24-365-win11	\N	20	5	27490000.00	\N	0	TGDD-351613	\N	12	0.00	0	0	t	2026-09-24 20:54:13.578466	2026-09-24 20:54:13.578466	\N
277	Laptop Acer Nitro ProPanel ANV15-41-R0Y4 - NH.QPESV.004 (R7 7735HS, 16GB, 512GB, RTX 4050 6GB, Full HD 180Hz, Win11)	laptop-acer-nitro-propanel-anv15-41-r0y4-nh-qpesv-004-r7-7735hs-16gb-512gb-rtx-4050-6gb-full-hd-180hz-win11	\N	23	5	30990000.00	\N	0	TGDD-363260	\N	12	0.00	0	0	t	2026-09-24 20:54:13.594766	2026-09-24 20:54:13.594766	\N
279	Laptop HP 15 fd0235TU - 9Q970PA_120U (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd0235tu-9q970pa-120u-core-5-120u-16gb-512gb-full-hd-win11	\N	17	5	22990000.00	\N	0	TGDD-341618	\N	12	0.00	0	0	t	2026-09-24 20:54:13.62984	2026-09-24 20:54:13.62984	\N
280	Laptop Acer Aspire Lite 15 AL15-36P-30TN - NX.DDASV.001 (Core 3 N350, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-36p-30tn-nx-ddasv-001-core-3-n350-8gb-512gb-full-hd-win11	\N	23	5	16990000.00	\N	0	TGDD-368654	\N	12	0.00	0	0	t	2026-09-24 20:54:13.648038	2026-09-24 20:54:13.648038	\N
281	Laptop HP 15 fd2126TU - D72CBPA (Ultra 5 225U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd2126tu-d72cbpa-ultra-5-225u-16gb-512gb-full-hd-win11	\N	17	5	24190000.00	\N	0	TGDD-367574	\N	12	0.00	0	0	t	2026-09-24 20:54:13.665269	2026-09-24 20:54:13.665269	\N
282	Laptop Asus Gaming V16 V3607VU - RP192W (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, WUXGA 144Hz, Win11)	laptop-asus-gaming-v16-v3607vu-rp192w-core-5-210h-16gb-512gb-rtx-4050-6gb-wuxga-144hz-win11	\N	19	5	25890000.00	\N	0	TGDD-339688	\N	12	0.00	0	0	t	2026-09-24 20:54:13.682122	2026-09-24 20:54:13.682122	\N
283	Laptop Dell 15 DC15250 - 71092480 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365B, Win11)	laptop-dell-15-dc15250-71092480-i7-1355u-16gb-512gb-full-hd-120hz-officehs24-365b-win11	\N	20	5	25490000.00	\N	0	TGDD-364860	\N	12	0.00	0	0	t	2026-09-24 20:54:13.699634	2026-09-24 20:54:13.699634	\N
284	Laptop HP OmniBook X Flip 14 fm0088TU - BZ7Q2PA (Ultra 5 226V, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365B, Win11)	laptop-hp-omnibook-x-flip-14-fm0088tu-bz7q2pa-ultra-5-226v-16gb-512gb-wuxga-cam-ung-officeh24-365b-win11	\N	17	5	31390000.00	\N	0	TGDD-358080	\N	12	0.00	0	0	t	2026-09-24 20:54:13.716657	2026-09-24 20:54:13.716657	\N
285	Laptop Dell 15 DC15255 - X9YM41 (R7 7730U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365B, Win11)	laptop-dell-15-dc15255-x9ym41-r7-7730u-16gb-1tb-full-hd-120hz-officeh24-365b-win11	\N	20	5	22990000.00	\N	0	TGDD-357992	\N	12	0.00	0	0	t	2026-09-24 20:54:13.734799	2026-09-24 20:54:13.734799	\N
286	Laptop MSI Venture A15 AI A2HMG - 003VN (R7 260, 16GB, 512GB, Full HD 144Hz, Win11)	laptop-msi-venture-a15-ai-a2hmg-003vn-r7-260-16gb-512gb-full-hd-144hz-win11	\N	22	5	21390000.00	\N	0	TGDD-340006	\N	12	0.00	0	0	t	2026-09-24 20:54:13.754232	2026-09-24 20:54:13.754232	\N
287	Laptop Acer Gaming Nitro V ANV15 51 57B2 - NH.QN8SV.001 (i5 13420H, 8GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-acer-gaming-nitro-v-anv15-51-57b2-nh-qn8sv-001-i5-13420h-8gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	23	5	25990000.00	\N	0	TGDD-318354	\N	12	0.00	0	0	t	2026-09-24 20:54:13.772733	2026-09-24 20:54:13.772733	\N
288	Laptop Dell 15 DC15250 - CPH992 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365B, Win11)	laptop-dell-15-dc15250-cph992-i5-1334u-16gb-512gb-full-hd-120hz-officehs24-365b-win11	\N	20	5	22990000.00	\N	0	TGDD-365998	\N	12	0.00	0	0	t	2026-09-24 20:54:13.791303	2026-09-24 20:54:13.791303	\N
289	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU-5 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc15250-i5u165w11slu-5-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	22490000.00	\N	0	TGDD-362666	\N	12	0.00	0	0	t	2026-09-24 20:54:13.810204	2026-09-24 20:54:13.810204	\N
290	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CMHH2VN893SH (i5 13420H, 16GB, 512GB, RTX 4050 6GB, WUXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-gaming-a16-cmhh2vn893sh-i5-13420h-16gb-512gb-rtx-4050-6gb-wuxga-165hz-win11	\N	25	5	29090000.00	\N	0	TGDD-337843	\N	12	0.00	0	0	t	2026-09-24 20:54:13.827973	2026-09-24 20:54:13.827973	\N
291	Laptop Dell 15 DC15250 - 71071928 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	laptop-dell-15-dc15250-71071928-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365b-win11	\N	20	5	22490000.00	\N	0	TGDD-358498	\N	12	0.00	0	0	t	2026-09-24 20:54:13.845324	2026-09-24 20:54:13.845324	\N
292	Laptop HP Gaming VICTUS 15 fa2732TX - B85LPPA (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-hp-gaming-victus-15-fa2732tx-b85lppa-i5-13420h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	17	5	29990000.00	\N	0	TGDD-345510	\N	12	0.00	0	0	t	2026-09-24 20:54:13.862971	2026-09-24 20:54:13.862971	\N
293	Laptop HP OmniBook 5 AI 16 af1054TU - C1MN8PA (Ultra 7 255U, 32GB, 512GB, WUXGA, OfficeH24, Win11)	laptop-hp-omnibook-5-ai-16-af1054tu-c1mn8pa-ultra-7-255u-32gb-512gb-wuxga-officeh24-win11	\N	17	5	31690000.00	\N	0	TGDD-358074	\N	12	0.00	0	0	t	2026-09-24 20:54:13.880393	2026-09-24 20:54:13.880393	\N
294	Laptop HP Pavilion 15 eg3093TU - 8C5L4PA (i5 1335U, 16GB, 512GB, Full HD, Win11)	laptop-hp-pavilion-15-eg3093tu-8c5l4pa-i5-1335u-16gb-512gb-full-hd-win11	\N	17	5	19990000.00	\N	0	TGDD-311176	\N	12	0.00	0	0	t	2026-09-24 20:54:13.898331	2026-09-24 20:54:13.898331	\N
295	Laptop MSI Gaming Katana 15 HX B14WFK - 025VN (i7 14650HX, 16GB, 512GB, RTX5060 8GB, QHD 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wfk-025vn-i7-14650hx-16gb-512gb-rtx5060-8gb-qhd-165hz-win11	\N	22	5	44590000.00	\N	0	TGDD-341610	\N	12	0.00	0	0	t	2026-09-24 20:54:13.918224	2026-09-24 20:54:13.918224	\N
296	Laptop Acer Gaming Aspire 5 A515 58GM 598J - NX.KW1SV.002 (i5 13420H, 16GB, 512GB, RTX 2050 4GB, Full HD 144Hz, Win11)	laptop-acer-gaming-aspire-5-a515-58gm-598j-nx-kw1sv-002-i5-13420h-16gb-512gb-rtx-2050-4gb-full-hd-144hz-win11	\N	23	5	20990000.00	\N	0	TGDD-334998	\N	12	0.00	0	0	t	2026-09-24 20:54:13.936982	2026-09-24 20:54:13.936982	\N
297	Laptop Asus ZenBook 14 UX3405CA - ST628W (Ultra 5 225H, 16GB, 512GB, 3K OLED 120Hz, Win11)	laptop-asus-zenbook-14-ux3405ca-st628w-ultra-5-225h-16gb-512gb-3k-oled-120hz-win11	\N	19	5	29590000.00	\N	0	TGDD-364401	\N	12	0.00	0	0	t	2026-09-24 20:54:13.954676	2026-09-24 20:54:13.954676	\N
298	Laptop Lenovo Gaming LOQ Essential 15IRX11 - 83SC003SVN (i5 13450HX, 16GB, 1TB, RTX 5050 8GB, Full HD, 144Hz, Win11)	laptop-lenovo-gaming-loq-essential-15irx11-83sc003svn-i5-13450hx-16gb-1tb-rtx-5050-8gb-full-hd-144hz-win11	\N	21	5	31990000.00	\N	0	TGDD-359477	\N	12	0.00	0	0	t	2026-09-24 20:54:13.973918	2026-09-24 20:54:13.973918	\N
299	Laptop HP Pavilion 16 af0054TU - AY8C3PA (Ultra 5 125U, 16GB, 1TB, WUXGA, OfficeHS+365, Win11)	laptop-hp-pavilion-16-af0054tu-ay8c3pa-ultra-5-125u-16gb-1tb-wuxga-officehs-365-win11	\N	17	5	23390000.00	\N	0	TGDD-337043	\N	12	0.00	0	0	t	2026-09-24 20:54:13.993181	2026-09-24 20:54:13.993181	\N
300	Laptop Dell Inspiron 15 3530 - N5I5530W1 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-n5i5530w1-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21990000.00	\N	0	TGDD-334803	\N	12	0.00	0	0	t	2026-09-24 20:54:14.01179	2026-09-24 20:54:14.01179	\N
301	Laptop MSI Venture A14 AI+ A3HMG - 004VN (R5 AI 340, 16GB, 512GB, 2.8K OLED 120Hz, Win11)	laptop-msi-venture-a14-ai-a3hmg-004vn-r5-ai-340-16gb-512gb-2-8k-oled-120hz-win11	\N	22	5	24990000.00	\N	0	TGDD-339951	\N	12	0.00	0	0	t	2026-09-24 20:54:14.029683	2026-09-24 20:54:14.029683	\N
302	Laptop HP 15 fd0234TU - 9Q969PA-120U (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fd0234tu-9q969pa-120u-core-5-120u-16gb-512gb-full-hd-win11	\N	17	5	22990000.00	\N	0	TGDD-341270	\N	12	0.00	0	0	t	2026-09-24 20:54:14.048676	2026-09-24 20:54:14.048676	\N
303	Laptop Acer Aspire 16 AI A16 71M 59L5 - NX.J4YSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-16-ai-a16-71m-59l5-nx-j4ysv-001-ultra-5-125h-16gb-512gb-full-hd-win11	\N	23	5	21590000.00	\N	0	TGDD-333424	\N	12	0.00	0	0	t	2026-09-24 20:54:14.066807	2026-09-24 20:54:14.066807	\N
304	Laptop MSI Gaming Katana 15 HX B14WEK - 286VN (i5 14450HX, 16GB, 512GB, RTX5050 8GB, QHD 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wek-286vn-i5-14450hx-16gb-512gb-rtx5050-8gb-qhd-165hz-win11	\N	22	5	39990000.00	\N	0	TGDD-341611	\N	12	0.00	0	0	t	2026-09-24 20:54:14.085412	2026-09-24 20:54:14.085412	\N
631	Máy tính bảng Samsung Galaxy Tab S10 FE 5G 8GB/128GB	may-tinh-bang-samsung-galaxy-tab-s10-fe-5g-8gb-128gb	\N	7	6	12610000.00	\N	0	TGDD-336738	\N	12	0.00	0	0	t	2026-09-24 20:54:19.754819	2026-09-24 20:54:19.754819	\N
305	Laptop Asus Vivobook 14 X1404VA - EB260W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-14-x1404va-eb260w-core-5-120u-16gb-512gb-full-hd-win11	\N	19	5	19490000.00	\N	0	TGDD-360417	\N	12	0.00	0	0	t	2026-09-24 20:54:14.103374	2026-09-24 20:54:14.103374	\N
306	Laptop Dell 14 DC14250 - 71100515 (Core 7 150U, 16GB, 512GB, Full HD+ 120Hz, OfficeH24+365, Win11)	laptop-dell-14-dc14250-71100515-core-7-150u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	31990000.00	\N	0	TGDD-368559	\N	12	0.00	0	0	t	2026-09-24 20:54:14.121152	2026-09-24 20:54:14.121152	\N
307	Laptop Asus Zenbook 14 UX3405CA - ST648W (Ultra 9 285H, 32GB, 1TB, 3K OLED 120Hz, Win11)	laptop-asus-zenbook-14-ux3405ca-st648w-ultra-9-285h-32gb-1tb-3k-oled-120hz-win11	\N	19	5	38590000.00	\N	0	TGDD-364405	\N	12	0.00	0	0	t	2026-09-24 20:54:14.138557	2026-09-24 20:54:14.138557	\N
308	Laptop HP OmniBook 5 16 ag1069AU - BZ7T1PA (R5 AI 340, 16GB, 512GB, WUXGA, OfficeH24+365B, Win11)	laptop-hp-omnibook-5-16-ag1069au-bz7t1pa-r5-ai-340-16gb-512gb-wuxga-officeh24-365b-win11	\N	17	5	25990000.00	\N	0	TGDD-358120	\N	12	0.00	0	0	t	2026-09-24 20:54:14.157308	2026-09-24 20:54:14.157308	\N
309	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc15250-i5u165w11slu-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	22490000.00	\N	0	TGDD-342754	\N	12	0.00	0	0	t	2026-09-24 20:54:14.174417	2026-09-24 20:54:14.174417	\N
310	Laptop MacBook Pro 14 inch M5 24GB/1TB	laptop-macbook-pro-14-inch-m5-24gb-1tb	\N	18	5	56990000.00	\N	0	TGDD-358089	\N	12	0.00	0	0	t	2026-09-24 20:54:14.191271	2026-09-24 20:54:14.191271	\N
311	Laptop HP Probook 4 G1ah 16 - C40JPAT (R5 220, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1ah-16-c40jpat-r5-220-16gb-512gb-wuxga-win11	\N	17	5	25490000.00	\N	0	TGDD-366702	\N	12	0.00	0	0	t	2026-09-24 20:54:14.208516	2026-09-24 20:54:14.208516	\N
312	Laptop Lenovo ThinkPad E14 Gen 7 - 21SX00BNVN (Ultra 5 135H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-gen-7-21sx00bnvn-ultra-5-135h-16gb-512gb-wuxga-win11	\N	21	5	33690000.00	\N	0	TGDD-365535	\N	12	0.00	0	0	t	2026-09-24 20:54:14.22536	2026-09-24 20:54:14.22536	\N
313	Laptop MSI Gaming Cyborg 15 B13WFKG - 658VN (i7 13620H, 16GB, 1TB, RTX 5060 8GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-b13wfkg-658vn-i7-13620h-16gb-1tb-rtx-5060-8gb-full-hd-144hz-win11	\N	22	5	39090000.00	\N	0	TGDD-362922	\N	12	0.00	0	0	t	2026-09-24 20:54:14.242798	2026-09-24 20:54:14.242798	\N
314	Laptop Asus Vivobook 15 X1504VA - BQ285W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504va-bq285w-core-5-120u-16gb-512gb-full-hd-win11	\N	19	5	19490000.00	\N	0	TGDD-360419	\N	12	0.00	0	0	t	2026-09-24 20:54:14.260532	2026-09-24 20:54:14.260532	\N
315	Laptop Dell 15 DC15250 - CPH993 (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-cph993-core-3-100u-8gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21490000.00	\N	0	TGDD-369481	\N	12	0.00	0	0	t	2026-09-24 20:54:14.278388	2026-09-24 20:54:14.278388	\N
316	Laptop HP 240R G9 - C40M2AT (Core 5 120U, 8GB, 512GB, Full HD, Win11)	laptop-hp-240r-g9-c40m2at-core-5-120u-8gb-512gb-full-hd-win11	\N	17	5	21490000.00	\N	0	TGDD-365607	\N	12	0.00	0	0	t	2026-09-24 20:54:14.295796	2026-09-24 20:54:14.295796	\N
317	Laptop Dell 14 DC14250 - F0FTK7 (Core 7 150U, 16GB, 512GB, MX570A 2GB, Full HD+, OfficeHS24+365, Win11)	laptop-dell-14-dc14250-f0ftk7-core-7-150u-16gb-512gb-mx570a-2gb-full-hd-officehs24-365-win11	\N	20	5	30990000.00	\N	0	TGDD-364967	\N	12	0.00	0	0	t	2026-09-24 20:54:14.312696	2026-09-24 20:54:14.312696	\N
318	Laptop Acer Nitro Lite NL16-71G-71FN - NH.D5ASV.003 (i7 13620H, 16GB, 512GB, RTX 4050 6GB, FHD+ 180Hz, Win11)	laptop-acer-nitro-lite-nl16-71g-71fn-nh-d5asv-003-i7-13620h-16gb-512gb-rtx-4050-6gb-fhd-180hz-win11	\N	23	5	30990000.00	\N	0	TGDD-363259	\N	12	0.00	0	0	t	2026-09-24 20:54:14.329932	2026-09-24 20:54:14.329932	\N
319	Laptop MSI Gaming Cyborg 15 Black Edition A13VE - A13VE-2410VN (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-black-edition-a13ve-a13ve-2410vn-i5-13420h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	22	5	28090000.00	\N	0	TGDD-366909	\N	12	0.00	0	0	t	2026-09-24 20:54:14.347816	2026-09-24 20:54:14.347816	\N
320	Laptop Asus Zenbook 14 UX3405CA - PZ187WS (Ultra 5 225H, 16GB, 512GB, 2.8K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-asus-zenbook-14-ux3405ca-pz187ws-ultra-5-225h-16gb-512gb-2-8k-oled-120hz-cam-ung-officeh24-365-win11	\N	19	5	30790000.00	\N	0	TGDD-334796	\N	12	0.00	0	0	t	2026-09-24 20:54:14.365585	2026-09-24 20:54:14.365585	\N
321	Laptop HP 14 ep1137TU - C2CY8PA (Ultra 7 155H, 16GB, 512GB, Full HD, Win11)	laptop-hp-14-ep1137tu-c2cy8pa-ultra-7-155h-16gb-512gb-full-hd-win11	\N	17	5	25990000.00	\N	0	TGDD-358796	\N	12	0.00	0	0	t	2026-09-24 20:54:14.381919	2026-09-24 20:54:14.381919	\N
322	Laptop Lenovo Ideapad Slim 5 OLED 14AGP11 - 83S1003FVN (R7 445, 32GB, 512GB, WUXGA OLED, Win11)	laptop-lenovo-ideapad-slim-5-oled-14agp11-83s1003fvn-r7-445-32gb-512gb-wuxga-oled-win11	\N	21	5	36990000.00	\N	0	TGDD-363882	\N	12	0.00	0	0	t	2026-09-24 20:54:14.399562	2026-09-24 20:54:14.399562	\N
323	Laptop HP Probook 450 G10 - 9H1N5PT (i5 1335U, 16GB, 512GB, Full HD, Win11)	laptop-hp-probook-450-g10-9h1n5pt-i5-1335u-16gb-512gb-full-hd-win11	\N	17	5	21690000.00	\N	0	TGDD-327406	\N	12	0.00	0	0	t	2026-09-24 20:54:14.416391	2026-09-24 20:54:14.416391	\N
324	Laptop Acer Aspire Go 15 AG15-72P-76A2 - NX.JRRSV.008 (Core 7 150U, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-go-15-ag15-72p-76a2-nx-jrrsv-008-core-7-150u-16gb-512gb-full-hd-win11	\N	23	5	22990000.00	\N	0	TGDD-363262	\N	12	0.00	0	0	t	2026-09-24 20:54:14.434463	2026-09-24 20:54:14.434463	\N
325	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CTHH3VN893SH (i5 13420H, 16GB, 512GB, RTX5050 8GB, WUXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-gaming-a16-cthh3vn893sh-i5-13420h-16gb-512gb-rtx5050-8gb-wuxga-165hz-win11	\N	25	5	34490000.00	\N	0	TGDD-341751	\N	12	0.00	0	0	t	2026-09-24 20:54:14.45236	2026-09-24 20:54:14.45236	\N
326	Laptop HP 15 fd1490TU - D0BH5PA (Ultra 7 155H, 24GB, 512GB, Full HD, Win11)	laptop-hp-15-fd1490tu-d0bh5pa-ultra-7-155h-24gb-512gb-full-hd-win11	\N	17	5	27990000.00	\N	0	TGDD-362399	\N	12	0.00	0	0	t	2026-09-24 20:54:14.47036	2026-09-24 20:54:14.47036	\N
327	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU-27 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-dc15250-i7u161w11slu-27-i7-1355u-16gb-1tb-full-hd-120hz-officehs24-365-win11	\N	20	5	28490000.00	\N	0	TGDD-365311	\N	12	0.00	0	0	t	2026-09-24 20:54:14.48763	2026-09-24 20:54:14.48763	\N
328	Laptop Dell Inspiron 15 3530 - P16WD22 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-p16wd22-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21990000.00	\N	0	TGDD-339681	\N	12	0.00	0	0	t	2026-09-24 20:54:14.505069	2026-09-24 20:54:14.505069	\N
329	Laptop Lenovo ThinkBook 16 G9 IRL - 21US008FVN (i5 13420H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkbook-16-g9-irl-21us008fvn-i5-13420h-16gb-512gb-wuxga-win11	\N	21	5	26990000.00	\N	0	TGDD-363901	\N	12	0.00	0	0	t	2026-09-24 20:54:14.522898	2026-09-24 20:54:14.522898	\N
330	Laptop Dell Inspiron 15 3530 - 71070372 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-71070372-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21990000.00	\N	0	TGDD-340560	\N	12	0.00	0	0	t	2026-09-24 20:54:14.541012	2026-09-24 20:54:14.541012	\N
331	Laptop HP 15 fc0655AU - C81NGPA (R5 7430U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fc0655au-c81ngpa-r5-7430u-16gb-512gb-full-hd-win11	\N	17	5	21290000.00	\N	0	TGDD-358079	\N	12	0.00	0	0	t	2026-09-24 20:54:14.562792	2026-09-24 20:54:14.562792	\N
596	Laptop HP 15 fd2183TU - DP2C0PA (Ultra 7 255U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-15-fd2183tu-dp2c0pa-ultra-7-255u-16gb-512gb-wuxga-win11	\N	17	5	31590000.00	\N	0	TGDD-371123	\N	12	0.00	0	0	t	2026-09-24 20:54:19.136794	2026-09-24 20:54:19.136794	\N
332	Laptop MSI Gaming Katana 15 HX B14WFK - 267VN (i7 14650HX, 32GB, 512GB, RTX 5060 8GB, QHD 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wfk-267vn-i7-14650hx-32gb-512gb-rtx-5060-8gb-qhd-165hz-win11	\N	22	5	49990000.00	\N	0	TGDD-341609	\N	12	0.00	0	0	t	2026-09-24 20:54:14.584243	2026-09-24 20:54:14.584243	\N
333	Laptop MSI Gaming Katana 15 HX B14WEK - 027VN (i7 14650HX, 32GB, 512GB, RTX5050 8GB, QHD 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wek-027vn-i7-14650hx-32gb-512gb-rtx5050-8gb-qhd-165hz-win11	\N	22	5	46990000.00	\N	0	TGDD-341613	\N	12	0.00	0	0	t	2026-09-24 20:54:14.604042	2026-09-24 20:54:14.604042	\N
334	Laptop Asus Gaming V16 V3607VJ - RP071W (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WUXGA 144Hz, Win11)	laptop-asus-gaming-v16-v3607vj-rp071w-core-5-210h-16gb-512gb-rtx-3050-6gb-wuxga-144hz-win11	\N	19	5	25990000.00	\N	0	TGDD-360421	\N	12	0.00	0	0	t	2026-09-24 20:54:14.622012	2026-09-24 20:54:14.622012	\N
335	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01ALVN (i7 13650HX, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx9-83dv01alvn-i7-13650hx-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	21	5	32990000.00	\N	0	TGDD-344606	\N	12	0.00	0	0	t	2026-09-24 20:54:14.640047	2026-09-24 20:54:14.640047	\N
336	Laptop Dell Gaming Alienware 16 Aurora AC16250 - C7H161W11II4050 (Core 7 240H, 16GB, 1TB, RTX 4050 6GB, WQXGA 120Hz, OfficeH24+365, Win11)	laptop-dell-gaming-alienware-16-aurora-ac16250-c7h161w11ii4050-core-7-240h-16gb-1tb-rtx-4050-6gb-wqxga-120hz-officeh24-365-win11	\N	20	5	37990000.00	\N	0	TGDD-360693	\N	12	0.00	0	0	t	2026-09-24 20:54:14.657198	2026-09-24 20:54:14.657198	\N
337	Laptop Dell 15 DC15250 - 71073959 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	laptop-dell-15-dc15250-71073959-i7-1355u-16gb-512gb-full-hd-120hz-officeh24-365b-win11	\N	20	5	25490000.00	\N	0	TGDD-358499	\N	12	0.00	0	0	t	2026-09-24 20:54:14.674038	2026-09-24 20:54:14.674038	\N
338	Laptop Dell Inspiron 15 3530 - N3530-i5U165W11SLU-BL (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-n3530-i5u165w11slu-bl-i5-1334u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21990000.00	\N	0	TGDD-338316	\N	12	0.00	0	0	t	2026-09-24 20:54:14.690963	2026-09-24 20:54:14.690963	\N
339	Laptop Lenovo ThinkBook 14 - 21SG007VVN (Core 7 240H, 32GB, 1TB, WUXGA, Win11)	laptop-lenovo-thinkbook-14-21sg007vvn-core-7-240h-32gb-1tb-wuxga-win11	\N	21	5	36590000.00	\N	0	TGDD-342513	\N	12	0.00	0	0	t	2026-09-24 20:54:14.708733	2026-09-24 20:54:14.708733	\N
340	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CTHI3VN893SH (i7 13620H, 16GB, 512GB, RTX 5050 8GB, WUXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-gaming-a16-cthi3vn893sh-i7-13620h-16gb-512gb-rtx-5050-8gb-wuxga-165hz-win11	\N	25	5	36490000.00	\N	0	TGDD-359927	\N	12	0.00	0	0	t	2026-09-24 20:54:14.727297	2026-09-24 20:54:14.727297	\N
341	Laptop HP OmniBook X Flip 14 fk0092AU - BZ7P5PA (R5 AI 340, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-x-flip-14-fk0092au-bz7p5pa-r5-ai-340-16gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	31290000.00	\N	0	TGDD-358075	\N	12	0.00	0	0	t	2026-09-24 20:54:14.74491	2026-09-24 20:54:14.74491	\N
342	Laptop Acer Gaming Nitro ProPanel ANV16S 41 R337 - NH.QZZSV.002 (R7 260, 16GB, 1TB, RTX 5050 8GB, FHD+ 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv16s-41-r337-nh-qzzsv-002-r7-260-16gb-1tb-rtx-5050-8gb-fhd-180hz-win11	\N	23	5	42590000.00	\N	0	TGDD-360296	\N	12	0.00	0	0	t	2026-09-24 20:54:14.762529	2026-09-24 20:54:14.762529	\N
343	Laptop MacBook Pro 14 inch M5 16GB/1TB	laptop-macbook-pro-14-inch-m5-16gb-1tb	\N	18	5	52990000.00	\N	0	TGDD-358088	\N	12	0.00	0	0	t	2026-09-24 20:54:14.779634	2026-09-24 20:54:14.779634	\N
344	Laptop Dell Inspiron 15 3530 - N3530-i5U165W11SLU-HS24 (i5 1334U, 16GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-n3530-i5u165w11slu-hs24-i5-1334u-16gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21990000.00	\N	0	TGDD-340558	\N	12	0.00	0	0	t	2026-09-24 20:54:14.797178	2026-09-24 20:54:14.797178	\N
345	Laptop HP 240R G10 - C3SG9AT (Core 5 120U, 8GB, 512GB, Full HD, Win11)	laptop-hp-240r-g10-c3sg9at-core-5-120u-8gb-512gb-full-hd-win11	\N	17	5	21490000.00	\N	0	TGDD-365609	\N	12	0.00	0	0	t	2026-09-24 20:54:14.814648	2026-09-24 20:54:14.814648	\N
346	Laptop Lenovo ThinkPad E14 Gen 7 - 21SX005NVN (Ultra 5 225U, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-gen-7-21sx005nvn-ultra-5-225u-16gb-512gb-wuxga-win11	\N	21	5	36490000.00	\N	0	TGDD-368220	\N	12	0.00	0	0	t	2026-09-24 20:54:14.831981	2026-09-24 20:54:14.831981	\N
347	Laptop Dell Gaming Alienware 16 Aurora AC16250 - C7H161W11II5050 (Core 7 240H, 16GB, 1TB, RTX 5050 8GB, WQXGA 120Hz, OfficeH24+365, Win11)	laptop-dell-gaming-alienware-16-aurora-ac16250-c7h161w11ii5050-core-7-240h-16gb-1tb-rtx-5050-8gb-wqxga-120hz-officeh24-365-win11	\N	20	5	40990000.00	\N	0	TGDD-341564	\N	12	0.00	0	0	t	2026-09-24 20:54:14.849834	2026-09-24 20:54:14.849834	\N
348	Laptop Asus Vivobook S16 M3607GA - SH034W (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s16-m3607ga-sh034w-r7-ai-445-16gb-512gb-wuxga-oled-win11	\N	19	5	27990000.00	\N	0	TGDD-364399	\N	12	0.00	0	0	t	2026-09-24 20:54:14.866583	2026-09-24 20:54:14.866583	\N
349	Laptop MSI Gaming Cyborg 15 AI A1VEK - 053VN (Ultra 7 155H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-ai-a1vek-053vn-ultra-7-155h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	22	5	28990000.00	\N	0	TGDD-322099	\N	12	0.00	0	0	t	2026-09-24 20:54:14.88374	2026-09-24 20:54:14.88374	\N
350	Laptop Acer Swift Go 14 AI SFG14-I71-70RP - NX.JZHSV.003 (Ultra 7 358H, 32GB, 1TB, 2.8K 120Hz, Win11)	laptop-acer-swift-go-14-ai-sfg14-i71-70rp-nx-jzhsv-003-ultra-7-358h-32gb-1tb-2-8k-120hz-win11	\N	23	5	49990000.00	\N	0	TGDD-367378	\N	12	0.00	0	0	t	2026-09-24 20:54:14.900466	2026-09-24 20:54:14.900466	\N
351	Laptop Dell Gaming Alienware 16 Aurora AC16250 - 71072939 (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WQXGA 120Hz, OfficeH24+365, Win11)	laptop-dell-gaming-alienware-16-aurora-ac16250-71072939-core-5-210h-16gb-512gb-rtx-3050-6gb-wqxga-120hz-officeh24-365-win11	\N	20	5	33990000.00	\N	0	TGDD-341563	\N	12	0.00	0	0	t	2026-09-24 20:54:14.91779	2026-09-24 20:54:14.91779	\N
352	Laptop Asus Vivobook S14 S3407CA - LY096WS (Ultra 7 255H, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-asus-vivobook-s14-s3407ca-ly096ws-ultra-7-255h-16gb-512gb-wuxga-officeh24-365-win11	\N	19	5	27490000.00	\N	0	TGDD-338321	\N	12	0.00	0	0	t	2026-09-24 20:54:14.935579	2026-09-24 20:54:14.935579	\N
353	Laptop HP Pavilion 15 eg3091TU - 8C5L2PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	laptop-hp-pavilion-15-eg3091tu-8c5l2pa-i7-1355u-16gb-512gb-full-hd-win11	\N	17	5	24290000.00	\N	0	TGDD-311177	\N	12	0.00	0	0	t	2026-09-24 20:54:14.952787	2026-09-24 20:54:14.952787	\N
354	Laptop Acer Gaming Nitro Lite 16 NL16 71G 71UJ - NH.D59SV.002 (i7 13620H, 16GB, 512GB, RTX 3050 6GB, Full HD+ 165Hz, Win11)	laptop-acer-gaming-nitro-lite-16-nl16-71g-71uj-nh-d59sv-002-i7-13620h-16gb-512gb-rtx-3050-6gb-full-hd-165hz-win11	\N	23	5	27990000.00	\N	0	TGDD-339201	\N	12	0.00	0	0	t	2026-09-24 20:54:14.971426	2026-09-24 20:54:14.971426	\N
355	Laptop HP OmniBook 7 Aero 13 bg1087AU - BZ7S1PA (R5 AI 340, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-hp-omnibook-7-aero-13-bg1087au-bz7s1pa-r5-ai-340-16gb-512gb-wuxga-officeh24-365-win11	\N	17	5	28490000.00	\N	0	TGDD-366703	\N	12	0.00	0	0	t	2026-09-24 20:54:14.991176	2026-09-24 20:54:14.991176	\N
356	Laptop Lenovo V14 G5 - 83HD005JVN (i5 13420H, 16GB, 512GB, Full HD, Win11)	laptop-lenovo-v14-g5-83hd005jvn-i5-13420h-16gb-512gb-full-hd-win11	\N	21	5	23990000.00	\N	0	TGDD-359482	\N	12	0.00	0	0	t	2026-09-24 20:54:15.009731	2026-09-24 20:54:15.009731	\N
357	Laptop Dell 14 DC14250 - 71083580 (Core 7 150U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-14-dc14250-71083580-core-7-150u-16gb-512gb-full-hd-officeh24-365-win11	\N	20	5	29990000.00	\N	0	TGDD-361536	\N	12	0.00	0	0	t	2026-09-24 20:54:15.028132	2026-09-24 20:54:15.028132	\N
358	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CVHI3VN893SH (i7 13620H, 16GB, 512GB, RTX5060 8GB, WUXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-gaming-a16-cvhi3vn893sh-i7-13620h-16gb-512gb-rtx5060-8gb-wuxga-165hz-win11	\N	25	5	37590000.00	\N	0	TGDD-341752	\N	12	0.00	0	0	t	2026-09-24 20:54:15.046348	2026-09-24 20:54:15.046348	\N
359	Laptop Dell 16 DC16250 - C7U161W11BLU (Core 7 150U, 16GB, 1TB, Full HD+, Cảm ứng, OfficeH24+365, Win11)	laptop-dell-16-dc16250-c7u161w11blu-core-7-150u-16gb-1tb-full-hd-cam-ung-officeh24-365-win11	\N	20	5	33990000.00	\N	0	TGDD-360694	\N	12	0.00	0	0	t	2026-09-24 20:54:15.075953	2026-09-24 20:54:15.075953	\N
360	Laptop Dell 15 DC15250 - CPH997 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-cph997-i7-1355u-16gb-1tb-full-hd-120hz-officeh24-365-win11	\N	20	5	27490000.00	\N	0	TGDD-357991	\N	12	0.00	0	0	t	2026-09-24 20:54:15.095778	2026-09-24 20:54:15.095778	\N
361	Laptop HP OmniBook X Flip 14 fm0076TU - BZ7P6PA (Ultra 7 258V, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-x-flip-14-fm0076tu-bz7p6pa-ultra-7-258v-32gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	37990000.00	\N	0	TGDD-358076	\N	12	0.00	0	0	t	2026-09-24 20:54:15.113207	2026-09-24 20:54:15.113207	\N
362	Laptop HP OmniBook 7 14 fs0043TU - C1MN3PA (Core 5 210H, 16GB, 512GB, 2K, OfficeH24+365, Win11)	laptop-hp-omnibook-7-14-fs0043tu-c1mn3pa-core-5-210h-16gb-512gb-2k-officeh24-365-win11	\N	17	5	27190000.00	\N	0	TGDD-361726	\N	12	0.00	0	0	t	2026-09-24 20:54:15.130013	2026-09-24 20:54:15.130013	\N
363	Laptop Asus Zenbook S 14 UX5406SA - PV140WS (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, OfficeHS, Win11)	laptop-asus-zenbook-s-14-ux5406sa-pv140ws-ultra-7-258v-32gb-1tb-2-8k-oled-120hz-officehs-win11	\N	19	5	43990000.00	\N	0	TGDD-330579	\N	12	0.00	0	0	t	2026-09-24 20:54:15.147848	2026-09-24 20:54:15.147848	\N
364	Laptop Lenovo ThinkBook 14 Gen 8 - 21SJ00EAVN (Ultra 5 135H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkbook-14-gen-8-21sj00eavn-ultra-5-135h-16gb-512gb-wuxga-win11	\N	21	5	32290000.00	\N	0	TGDD-363897	\N	12	0.00	0	0	t	2026-09-24 20:54:15.16473	2026-09-24 20:54:15.16473	\N
365	Laptop Dell 15 DC15250 - DC5C3259W1-2Y (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-15-dc15250-dc5c3259w1-2y-core-3-100u-8gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	21490000.00	\N	0	TGDD-368555	\N	12	0.00	0	0	t	2026-09-24 20:54:15.181634	2026-09-24 20:54:15.181634	\N
366	Laptop HP Pavilion 15 eg3112TU - 8U6L9PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	laptop-hp-pavilion-15-eg3112tu-8u6l9pa-i7-1355u-16gb-512gb-full-hd-win11	\N	17	5	24290000.00	\N	0	TGDD-340479	\N	12	0.00	0	0	t	2026-09-24 20:54:15.198681	2026-09-24 20:54:15.198681	\N
367	Laptop Asus Vivobook 14 M1407GA - LY270W (R7 AI 445, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-14-m1407ga-ly270w-r7-ai-445-16gb-512gb-wuxga-win11	\N	19	5	23690000.00	\N	0	TGDD-362408	\N	12	0.00	0	0	t	2026-09-24 20:54:15.22314	2026-09-24 20:54:15.22314	\N
368	Laptop HP ProBook 4 G1iR 14 - C40JKAT (Core 5 120U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1ir-14-c40jkat-core-5-120u-16gb-512gb-wuxga-win11	\N	17	5	26990000.00	\N	0	TGDD-365625	\N	12	0.00	0	0	t	2026-09-24 20:54:15.244474	2026-09-24 20:54:15.244474	\N
369	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US002WVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-16iph11-83us002wvn-ultra-7-355-16gb-512gb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-367683	\N	12	0.00	0	0	t	2026-09-24 20:54:15.263574	2026-09-24 20:54:15.263574	\N
370	Laptop Dell Inspiron 14 5440 - N4I7204W1 (Core 7 150U, 16GB, 512GB, Full HD+, OfficeHS, Win11)	laptop-dell-inspiron-14-5440-n4i7204w1-core-7-150u-16gb-512gb-full-hd-officehs-win11	\N	20	5	27990000.00	\N	0	TGDD-325244	\N	12	0.00	0	0	t	2026-09-24 20:54:15.280553	2026-09-24 20:54:15.280553	\N
371	Laptop HP 240R G10 - CC9B9PT (Core 7 150U, 16GB, 512GB, Full HD, Win11)	laptop-hp-240r-g10-cc9b9pt-core-7-150u-16gb-512gb-full-hd-win11	\N	17	5	25990000.00	\N	0	TGDD-365613	\N	12	0.00	0	0	t	2026-09-24 20:54:15.300459	2026-09-24 20:54:15.300459	\N
372	Laptop Asus Zenbook 14 UM3406GA - QD075WS (R7 AI 445 ,16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	laptop-asus-zenbook-14-um3406ga-qd075ws-r7-ai-445-16gb-512gb-wuxga-oled-officeh24-365-win11	\N	19	5	29490000.00	\N	0	TGDD-362625	\N	12	0.00	0	0	t	2026-09-24 20:54:15.318391	2026-09-24 20:54:15.318391	\N
373	Laptop Lenovo ThinkPad E16 Gen 3 - 22AY003VVN (Ultra 7 258V, 32GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e16-gen-3-22ay003vvn-ultra-7-258v-32gb-512gb-wuxga-win11	\N	21	5	35490000.00	\N	0	TGDD-363891	\N	12	0.00	0	0	t	2026-09-24 20:54:15.335802	2026-09-24 20:54:15.335802	\N
374	Laptop HP OmniBook 7 14 fr0033TU - C1MN2PA (Ultra 5 225U, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-hp-omnibook-7-14-fr0033tu-c1mn2pa-ultra-5-225u-16gb-512gb-wuxga-officeh24-365-win11	\N	17	5	27490000.00	\N	0	TGDD-369845	\N	12	0.00	0	0	t	2026-09-24 20:54:15.353843	2026-09-24 20:54:15.353843	\N
375	Laptop Lenovo V15 G5 IRL - 83HF00BYVN (i5 13420H, 16GB, 512GB, Full HD, Win11)	laptop-lenovo-v15-g5-irl-83hf00byvn-i5-13420h-16gb-512gb-full-hd-win11	\N	21	5	23990000.00	\N	0	TGDD-359471	\N	12	0.00	0	0	t	2026-09-24 20:54:15.370983	2026-09-24 20:54:15.370983	\N
376	Laptop Acer Gaming Aspire 7 A715-59G-79XF - NH.QX6SV.008 (Core 7 240H, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	laptop-acer-gaming-aspire-7-a715-59g-79xf-nh-qx6sv-008-core-7-240h-16gb-512gb-rtx-3050-6gb-full-hd-144hz-win11	\N	23	5	28990000.00	\N	0	TGDD-364979	\N	12	0.00	0	0	t	2026-09-24 20:54:15.389075	2026-09-24 20:54:15.389075	\N
377	Laptop Dell Inspiron 14 5440 - 71053697 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeH24+365B, Win11)	laptop-dell-inspiron-14-5440-71053697-core-5-120u-16gb-1tb-full-hd-officeh24-365b-win11	\N	20	5	26990000.00	\N	0	TGDD-357987	\N	12	0.00	0	0	t	2026-09-24 20:54:15.406099	2026-09-24 20:54:15.406099	\N
378	Laptop Asus Vivobook 14 M1407KA - LY849W (R5 AI 330, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-14-m1407ka-ly849w-r5-ai-330-16gb-512gb-wuxga-win11	\N	19	5	21590000.00	\N	0	TGDD-364414	\N	12	0.00	0	0	t	2026-09-24 20:54:15.423348	2026-09-24 20:54:15.423348	\N
379	Laptop Asus TUF Gaming A16 FA607NUQ - RL007W (R7 170, 16GB, 512GB, RTX 4050 6GB, Full HD+ 144Hz, Win11)	laptop-asus-tuf-gaming-a16-fa607nuq-rl007w-r7-170-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	19	5	35990000.00	\N	0	TGDD-364181	\N	12	0.00	0	0	t	2026-09-24 20:54:15.440706	2026-09-24 20:54:15.440706	\N
380	Laptop Dell Inspiron 15 3530 - 71053721 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-71053721-i7-1355u-16gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	24490000.00	\N	0	TGDD-337472	\N	12	0.00	0	0	t	2026-09-24 20:54:15.457875	2026-09-24 20:54:15.457875	\N
381	Laptop HP 15 fd1037TU - 9Z2W5PA (Core 7 150U, 16GB, 1TB, Full HD, Win11)	laptop-hp-15-fd1037tu-9z2w5pa-core-7-150u-16gb-1tb-full-hd-win11	\N	17	5	23490000.00	\N	0	TGDD-341621	\N	12	0.00	0	0	t	2026-09-24 20:54:15.475238	2026-09-24 20:54:15.475238	\N
382	Laptop Lenovo Gaming LOQ 15IRX10 - 83JE01AGVN (i7 13645HX, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx10-83je01agvn-i7-13645hx-16gb-512gb-rtx-5050-8gb-full-hd-144hz-win11	\N	21	5	37590000.00	\N	0	TGDD-364623	\N	12	0.00	0	0	t	2026-09-24 20:54:15.492302	2026-09-24 20:54:15.492302	\N
383	Laptop Dell 16 Plus DB16250 - X65NW7 (Ultra 7 256V, 16GB, 1TB, QHD+ 120Hz, OfficeH24+365, Win11)	laptop-dell-16-plus-db16250-x65nw7-ultra-7-256v-16gb-1tb-qhd-120hz-officeh24-365-win11	\N	20	5	40990000.00	\N	0	TGDD-361538	\N	12	0.00	0	0	t	2026-09-24 20:54:15.509117	2026-09-24 20:54:15.509117	\N
384	Laptop Dell Inspiron 14 5440 - 71059084 (Core 7 150U, 16GB, 1TB, MX570A 2GB, 2.2K, OfficeH24+365B, Win11)	laptop-dell-inspiron-14-5440-71059084-core-7-150u-16gb-1tb-mx570a-2gb-2-2k-officeh24-365b-win11	\N	20	5	31990000.00	\N	0	TGDD-357988	\N	12	0.00	0	0	t	2026-09-24 20:54:15.526302	2026-09-24 20:54:15.526302	\N
385	Laptop MSI Gaming Katana A15 AI B8VG - 465VN (R7 8845HS, 16GB, 1TB, RTX 4070 8GB, Full HD 144Hz, Win11)	laptop-msi-gaming-katana-a15-ai-b8vg-465vn-r7-8845hs-16gb-1tb-rtx-4070-8gb-full-hd-144hz-win11	\N	22	5	37790000.00	\N	0	TGDD-326280	\N	12	0.00	0	0	t	2026-09-24 20:54:15.543722	2026-09-24 20:54:15.543722	\N
386	Laptop MSI Prestige 14 AI+ Evo C2VMG - 020VN (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Win11)	laptop-msi-prestige-14-ai-evo-c2vmg-020vn-ultra-7-258v-32gb-1tb-2-8k-oled-120hz-win11	\N	22	5	33990000.00	\N	0	TGDD-335020	\N	12	0.00	0	0	t	2026-09-24 20:54:15.561239	2026-09-24 20:54:15.561239	\N
387	Laptop MSI Gaming Vector 16 HX AI A2XWIG - 062VN (Ultra 9 275HX, 16GB, 1TB, RTX 5080 16GB, QHD+ 240Hz, Win11)	laptop-msi-gaming-vector-16-hx-ai-a2xwig-062vn-ultra-9-275hx-16gb-1tb-rtx-5080-16gb-qhd-240hz-win11	\N	22	5	79590000.00	\N	0	TGDD-335015	\N	12	0.00	0	0	t	2026-09-24 20:54:15.578339	2026-09-24 20:54:15.578339	\N
388	Laptop Acer Swift AI SF14 51 53P9 - NX.J2KSV.002 (Ultra 5 226V, 16GB, 1TB, 2.8K OLED 90Hz, Win11)	laptop-acer-swift-ai-sf14-51-53p9-nx-j2ksv-002-ultra-5-226v-16gb-1tb-2-8k-oled-90hz-win11	\N	23	5	29990000.00	\N	0	TGDD-332397	\N	12	0.00	0	0	t	2026-09-24 20:54:15.595764	2026-09-24 20:54:15.595764	\N
389	Laptop Dell 14 DC14250 - 71092478 (Core 7 150U, 16GB, 512GB, Full HD+ 120Hz, OfficeHS24+365, Win11)	laptop-dell-14-dc14250-71092478-core-7-150u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	29990000.00	\N	0	TGDD-365305	\N	12	0.00	0	0	t	2026-09-24 20:54:15.612077	2026-09-24 20:54:15.612077	\N
390	Laptop Acer Gaming Nitro ProPanel ANV16 41 R6ZY - NH.QP2SV.002 (R5 8645HS, 16GB, 512GB, RTX 3050 6GB, WUXGA 165Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv16-41-r6zy-nh-qp2sv-002-r5-8645hs-16gb-512gb-rtx-3050-6gb-wuxga-165hz-win11	\N	23	5	28990000.00	\N	0	TGDD-332579	\N	12	0.00	0	0	t	2026-09-24 20:54:15.628821	2026-09-24 20:54:15.628821	\N
391	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR00A5VN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iph11-83ur00a5vn-ultra-7-355-16gb-512gb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-367017	\N	12	0.00	0	0	t	2026-09-24 20:54:15.64548	2026-09-24 20:54:15.64548	\N
392	Laptop Lenovo Gaming Legion 5 15IRX10 - 83LY00HQVN (i7 13650HX, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, OfficeH24, Win11)	laptop-lenovo-gaming-legion-5-15irx10-83ly00hqvn-i7-13650hx-16gb-512gb-rtx-5060-8gb-wuxga-165hz-officeh24-win11	\N	21	5	51990000.00	\N	0	TGDD-342521	\N	12	0.00	0	0	t	2026-09-24 20:54:15.66176	2026-09-24 20:54:15.66176	\N
393	Laptop HP EliteBook 6 G1a 14 - C0CE1PT (R5 AI 340, 16GB, 512GB, WUXGA, Win11)	laptop-hp-elitebook-6-g1a-14-c0ce1pt-r5-ai-340-16gb-512gb-wuxga-win11	\N	17	5	29990000.00	\N	0	TGDD-365614	\N	12	0.00	0	0	t	2026-09-24 20:54:15.678174	2026-09-24 20:54:15.678174	\N
394	Laptop Asus Vivobook 14 Flip TP3407SA - SG349W (Ultra 5 226V, 16GB, 512GB, WUXGA OLED, Cảm ứng, Win11)	laptop-asus-vivobook-14-flip-tp3407sa-sg349w-ultra-5-226v-16gb-512gb-wuxga-oled-cam-ung-win11	\N	19	5	26590000.00	\N	0	TGDD-362409	\N	12	0.00	0	0	t	2026-09-24 20:54:15.695281	2026-09-24 20:54:15.695281	\N
395	Laptop HP OmniBook 5 AI 16 af1052TU - C1MN6PA (Ultra 7 255U, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-5-ai-16-af1052tu-c1mn6pa-ultra-7-255u-32gb-512gb-wuxga-cam-ung-officeh24-win11	\N	17	5	32490000.00	\N	0	TGDD-358072	\N	12	0.00	0	0	t	2026-09-24 20:54:15.711472	2026-09-24 20:54:15.711472	\N
396	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR0075VN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-15iph11-83ur0075vn-ultra-7-355-16gb-512gb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-366165	\N	12	0.00	0	0	t	2026-09-24 20:54:15.728776	2026-09-24 20:54:15.728776	\N
397	Laptop Dell Inspiron 15 3530 - N5I7421W1 (i7 1355U, 16GB, 512GB,  Full HD 120Hz, OfficeHS24+365, Win11)	laptop-dell-inspiron-15-3530-n5i7421w1-i7-1355u-16gb-512gb-full-hd-120hz-officehs24-365-win11	\N	20	5	24490000.00	\N	0	TGDD-342469	\N	12	0.00	0	0	t	2026-09-24 20:54:15.745777	2026-09-24 20:54:15.745777	\N
398	Laptop Asus Vivobook 16 X1607CA - MB990W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-16-x1607ca-mb990w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	24990000.00	\N	0	TGDD-364942	\N	12	0.00	0	0	t	2026-09-24 20:54:15.762734	2026-09-24 20:54:15.762734	\N
399	Laptop Asus Zenbook A14 UX3407QA - QD299WS (X1 26 100, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	laptop-asus-zenbook-a14-ux3407qa-qd299ws-x1-26-100-16gb-512gb-wuxga-oled-officeh24-365-win11	\N	19	5	28490000.00	\N	0	TGDD-334800	\N	12	0.00	0	0	t	2026-09-24 20:54:15.778686	2026-09-24 20:54:15.778686	\N
400	Laptop HP OmniBook 7 14 fr0024TU - C1MN0PA (Ultra 7 255H, 32GB, 512GB, WUXGA, OfficeH24+365B, Win11)	laptop-hp-omnibook-7-14-fr0024tu-c1mn0pa-ultra-7-255h-32gb-512gb-wuxga-officeh24-365b-win11	\N	17	5	34990000.00	\N	0	TGDD-358119	\N	12	0.00	0	0	t	2026-09-24 20:54:15.796121	2026-09-24 20:54:15.796121	\N
401	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01H3VN (i7 13645HX, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx9-83dv01h3vn-i7-13645hx-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	21	5	34990000.00	\N	0	TGDD-366167	\N	12	0.00	0	0	t	2026-09-24 20:54:15.812467	2026-09-24 20:54:15.812467	\N
402	Laptop Lenovo Gaming LOQ 15ARP10E - 83S0004FVN (R7 7735HS, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15arp10e-83s0004fvn-r7-7735hs-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	21	5	30490000.00	\N	0	TGDD-366166	\N	12	0.00	0	0	t	2026-09-24 20:54:15.83072	2026-09-24 20:54:15.83072	\N
403	Laptop Lenovo Gaming Legion 5 15IRX10 - 83LY00HRVN (i7 13650HX, 16GB, 512GB, RTX 5050 8GB, WUXGA 165Hz, OfficeH24, Win11)	laptop-lenovo-gaming-legion-5-15irx10-83ly00hrvn-i7-13650hx-16gb-512gb-rtx-5050-8gb-wuxga-165hz-officeh24-win11	\N	21	5	44690000.00	\N	0	TGDD-358381	\N	12	0.00	0	0	t	2026-09-24 20:54:15.848397	2026-09-24 20:54:15.848397	\N
404	Laptop HP EliteBook 6 G1a 14 - C0CG2PT (R7 AI 350, 16GB, 512GB, WUXGA, Win11)	laptop-hp-elitebook-6-g1a-14-c0cg2pt-r7-ai-350-16gb-512gb-wuxga-win11	\N	17	5	32990000.00	\N	0	TGDD-365616	\N	12	0.00	0	0	t	2026-09-24 20:54:15.866684	2026-09-24 20:54:15.866684	\N
405	Laptop Dell Gaming Alienware 16 Aurora AC16250 - 71072937 (Core 7 240H, 16GB, 1TB, RTX 5060 8GB, WQXGA 120Hz, OfficeH24+365, Win11)	laptop-dell-gaming-alienware-16-aurora-ac16250-71072937-core-7-240h-16gb-1tb-rtx-5060-8gb-wqxga-120hz-officeh24-365-win11	\N	20	5	44990000.00	\N	0	TGDD-341565	\N	12	0.00	0	0	t	2026-09-24 20:54:15.884478	2026-09-24 20:54:15.884478	\N
406	Laptop Dell 14 DC14250 - F0FTK5 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeH24+365, Win11)	laptop-dell-14-dc14250-f0ftk5-core-5-120u-16gb-1tb-full-hd-officeh24-365-win11	\N	20	5	27990000.00	\N	0	TGDD-362021	\N	12	0.00	0	0	t	2026-09-24 20:54:15.902271	2026-09-24 20:54:15.902271	\N
407	Laptop HP Probook 4 G1i 16 - BQ5E1PT (Ultra 7 255U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1i-16-bq5e1pt-ultra-7-255u-16gb-512gb-wuxga-win11	\N	17	5	29990000.00	\N	0	TGDD-345503	\N	12	0.00	0	0	t	2026-09-24 20:54:15.920177	2026-09-24 20:54:15.920177	\N
408	Laptop MSI Gaming Katana 15 HX B14WGK - 023VN (i7 14650HX, 16GB, 1TB, RTX 5070 8GB, QHD, 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wgk-023vn-i7-14650hx-16gb-1tb-rtx-5070-8gb-qhd-165hz-win11	\N	22	5	48590000.00	\N	0	TGDD-359298	\N	12	0.00	0	0	t	2026-09-24 20:54:15.937176	2026-09-24 20:54:15.937176	\N
409	Laptop Lenovo ThinkPad E14 Gen 7 - 21U2003SVN (Ultra 5 228V, 32GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-gen-7-21u2003svn-ultra-5-228v-32gb-512gb-wuxga-win11	\N	21	5	38390000.00	\N	0	TGDD-363898	\N	12	0.00	0	0	t	2026-09-24 20:54:15.953848	2026-09-24 20:54:15.953848	\N
410	Laptop HP EliteBook 6 G1i 13 - BQ9L8PT (Ultra 5 225U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-elitebook-6-g1i-13-bq9l8pt-ultra-5-225u-16gb-512gb-wuxga-win11	\N	17	5	31990000.00	\N	0	TGDD-345506	\N	12	0.00	0	0	t	2026-09-24 20:54:15.970351	2026-09-24 20:54:15.970351	\N
411	Laptop Dell Pro 13 Plus PB13250 - 71084491 (Ultra 5 235U, 16GB, 512GB, Full HD+, Win11)	laptop-dell-pro-13-plus-pb13250-71084491-ultra-5-235u-16gb-512gb-full-hd-win11	\N	20	5	30490000.00	\N	0	TGDD-363826	\N	12	0.00	0	0	t	2026-09-24 20:54:15.986818	2026-09-24 20:54:15.986818	\N
412	Laptop MSI Gaming Katana 15 HX B14WFK - 294VN (i9 14900HX, 16GB, 512GB, RTX 5060 8GB, QHD, 165Hz, Win11)	laptop-msi-gaming-katana-15-hx-b14wfk-294vn-i9-14900hx-16gb-512gb-rtx-5060-8gb-qhd-165hz-win11	\N	22	5	45590000.00	\N	0	TGDD-359299	\N	12	0.00	0	0	t	2026-09-24 20:54:16.002899	2026-09-24 20:54:16.002899	\N
413	Laptop Acer Aspire Lite 15 AL15-53P-56QH - NX.DG3SV.001 (Core 5 120U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-53p-56qh-nx-dg3sv-001-core-5-120u-8gb-512gb-full-hd-win11	\N	23	5	18990000.00	\N	0	TGDD-367377	\N	12	0.00	0	0	t	2026-09-24 20:54:16.020657	2026-09-24 20:54:16.020657	\N
414	Laptop Asus Vivobook S14 S3407VA - LY053W (i7 13620H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-s14-s3407va-ly053w-i7-13620h-16gb-512gb-wuxga-win11	\N	19	5	23490000.00	\N	0	TGDD-342749	\N	12	0.00	0	0	t	2026-09-24 20:54:16.037364	2026-09-24 20:54:16.037364	\N
415	Laptop Dell Inspiron 15 3530 - P16WD (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS, Win11)	laptop-dell-inspiron-15-3530-p16wd-i7-1355u-16gb-1tb-full-hd-120hz-officehs-win11	\N	20	5	26490000.00	\N	0	TGDD-325247	\N	12	0.00	0	0	t	2026-09-24 20:54:16.054303	2026-09-24 20:54:16.054303	\N
416	Laptop Lenovo ThinkPad E14 Gen 7 - 21U2003JVN (Ultra 7 258V, 32GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-gen-7-21u2003jvn-ultra-7-258v-32gb-512gb-wuxga-win11	\N	21	5	36590000.00	\N	0	TGDD-363900	\N	12	0.00	0	0	t	2026-09-24 20:54:16.072353	2026-09-24 20:54:16.072353	\N
417	Laptop HP OmniBook 5 16 ag1066AU - BZ7S9PA (R7 Al 350, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365B, Win11)	laptop-hp-omnibook-5-16-ag1066au-bz7s9pa-r7-al-350-16gb-512gb-wuxga-cam-ung-officeh24-365b-win11	\N	17	5	30790000.00	\N	0	TGDD-358798	\N	12	0.00	0	0	t	2026-09-24 20:54:16.090157	2026-09-24 20:54:16.090157	\N
418	Laptop HP OmniBook UF 14 fh0097TU - BZ7S3PA (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-uf-14-fh0097tu-bz7s3pa-ultra-7-258v-32gb-1tb-2-8k-oled-120hz-cam-ung-officeh24-win11	\N	17	5	55990000.00	\N	0	TGDD-341269	\N	12	0.00	0	0	t	2026-09-24 20:54:16.108202	2026-09-24 20:54:16.108202	\N
419	Laptop MSI Gaming Stealth A16 AI+ A3XWFG - 018VN (R9 AI HX 370, 32GB, 1TB, RTX 5060 8GB, QHD+ OLED, 240Hz, Win11)	laptop-msi-gaming-stealth-a16-ai-a3xwfg-018vn-r9-ai-hx-370-32gb-1tb-rtx-5060-8gb-qhd-oled-240hz-win11	\N	22	5	63990000.00	\N	0	TGDD-359302	\N	12	0.00	0	0	t	2026-09-24 20:54:16.125038	2026-09-24 20:54:16.125038	\N
420	Laptop Asus Vivobook S14 M3407GA - SF030W (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s14-m3407ga-sf030w-r7-ai-445-16gb-512gb-wuxga-oled-win11	\N	19	5	27990000.00	\N	0	TGDD-362624	\N	12	0.00	0	0	t	2026-09-24 20:54:16.142986	2026-09-24 20:54:16.142986	\N
421	Laptop HP ProBook 4 G1q 14 - C40JQAT (X1 26 100, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1q-14-c40jqat-x1-26-100-16gb-512gb-wuxga-win11	\N	17	5	22590000.00	\N	0	TGDD-361729	\N	12	0.00	0	0	t	2026-09-24 20:54:16.159428	2026-09-24 20:54:16.159428	\N
422	Laptop Lenovo V15 G5 IRL - 83HF00BVVN (i5 13420H, 8GB, 512GB, Full HD, Win11)	laptop-lenovo-v15-g5-irl-83hf00bvvn-i5-13420h-8gb-512gb-full-hd-win11	\N	21	5	20590000.00	\N	0	TGDD-363902	\N	12	0.00	0	0	t	2026-09-24 20:54:16.176069	2026-09-24 20:54:16.176069	\N
423	Laptop Asus Vivobook S14 S3407CA - SF913W (Ultra 5 225H, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s14-s3407ca-sf913w-ultra-5-225h-16gb-512gb-wuxga-oled-win11	\N	19	5	27890000.00	\N	0	TGDD-364415	\N	12	0.00	0	0	t	2026-09-24 20:54:16.193237	2026-09-24 20:54:16.193237	\N
424	Laptop Lenovo ThinkBook 16 G9 - 21UT005MVN (R7 250, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkbook-16-g9-21ut005mvn-r7-250-16gb-512gb-wuxga-win11	\N	21	5	30190000.00	\N	0	TGDD-368209	\N	12	0.00	0	0	t	2026-09-24 20:54:16.209265	2026-09-24 20:54:16.209265	\N
425	Laptop HP 245 G10 - A20TDPT (R5 7530U, 8GB, 512GB, Full HD, Win11)	laptop-hp-245-g10-a20tdpt-r5-7530u-8gb-512gb-full-hd-win11	\N	17	5	19290000.00	\N	0	TGDD-326049	\N	12	0.00	0	0	t	2026-09-24 20:54:16.226181	2026-09-24 20:54:16.226181	\N
426	Laptop HP Pavilion 16 af0052TU - AY8C1PA (Ultra 7 155U, 16GB, 1TB, WUXGA, OfficeHS+365, Win11)	laptop-hp-pavilion-16-af0052tu-ay8c1pa-ultra-7-155u-16gb-1tb-wuxga-officehs-365-win11	\N	17	5	27390000.00	\N	0	TGDD-337041	\N	12	0.00	0	0	t	2026-09-24 20:54:16.242768	2026-09-24 20:54:16.242768	\N
427	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010CVN (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Win11Pro)	laptop-lenovo-thinkpad-x1-carbon-gen-13-21ns010cvn-ultra-7-258v-32gb-1tb-2-8k-oled-120hz-win11pro	\N	21	5	68590000.00	\N	0	TGDD-364622	\N	12	0.00	0	0	t	2026-09-24 20:54:16.259434	2026-09-24 20:54:16.259434	\N
428	Laptop GIGABYTE Gaming A16 GA6H - CWHI3VNC94SH (i7 13620H, 16GB, 1TB, RTX 5070 8GB, WQXGA 165Hz, Win11)	laptop-gigabyte-gaming-a16-ga6h-cwhi3vnc94sh-i7-13620h-16gb-1tb-rtx-5070-8gb-wqxga-165hz-win11	\N	25	5	41490000.00	\N	0	TGDD-359928	\N	12	0.00	0	0	t	2026-09-24 20:54:16.277451	2026-09-24 20:54:16.277451	\N
429	Laptop Asus Zenbook 14 UM3406GA - QD073WS (R7 AI 445, 32GB, 1TB, WUXGA OLED, OfficeH24+365, Win11)	laptop-asus-zenbook-14-um3406ga-qd073ws-r7-ai-445-32gb-1tb-wuxga-oled-officeh24-365-win11	\N	19	5	35990000.00	\N	0	TGDD-362626	\N	12	0.00	0	0	t	2026-09-24 20:54:16.294312	2026-09-24 20:54:16.294312	\N
430	Laptop Lenovo ThinkBook 14 Gen 9 IRL - 21UY008TVN (i5 13420H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkbook-14-gen-9-irl-21uy008tvn-i5-13420h-16gb-512gb-wuxga-win11	\N	21	5	28690000.00	\N	0	TGDD-368221	\N	12	0.00	0	0	t	2026-09-24 20:54:16.311263	2026-09-24 20:54:16.311263	\N
431	Laptop HP Pavilion 16 af0053TU - AY8C2PA (Ultra 7 155U, 16GB, 512GB, WUXGA, OfficeHS+365, Win11)	laptop-hp-pavilion-16-af0053tu-ay8c2pa-ultra-7-155u-16gb-512gb-wuxga-officehs-365-win11	\N	17	5	26390000.00	\N	0	TGDD-337042	\N	12	0.00	0	0	t	2026-09-24 20:54:16.327156	2026-09-24 20:54:16.327156	\N
432	Laptop Dell 16 DC16250 - DC16250-C7U161W11BLU-27 (Core 7 150U, 16GB, 1TB, Full HD+, OfficeHS24+365, Win11)	laptop-dell-16-dc16250-dc16250-c7u161w11blu-27-core-7-150u-16gb-1tb-full-hd-officehs24-365-win11	\N	20	5	33990000.00	\N	0	TGDD-365312	\N	12	0.00	0	0	t	2026-09-24 20:54:16.343999	2026-09-24 20:54:16.343999	\N
433	Laptop Dell Inspiron 14 5441 - N4O10441W1 (X1P 64 100, 16GB, 1TB, Full HD+, OfficeH24+365, Win11)	laptop-dell-inspiron-14-5441-n4o10441w1-x1p-64-100-16gb-1tb-full-hd-officeh24-365-win11	\N	20	5	28490000.00	\N	0	TGDD-340563	\N	12	0.00	0	0	t	2026-09-24 20:54:16.361305	2026-09-24 20:54:16.361305	\N
434	Laptop Dell Inspiron 15 3530 - N3530-i7U161W11SLU (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-inspiron-15-3530-n3530-i7u161w11slu-i7-1355u-16gb-1tb-full-hd-120hz-officeh24-365-win11	\N	20	5	26490000.00	\N	0	TGDD-340559	\N	12	0.00	0	0	t	2026-09-24 20:54:16.378034	2026-09-24 20:54:16.378034	\N
435	Laptop HP OmniBook X Flip 14 fk0082AU - BZ7N8PA (R7 AI 350, 32GB, 1TB, 2K, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-x-flip-14-fk0082au-bz7n8pa-r7-ai-350-32gb-1tb-2k-cam-ung-officeh24-365-win11	\N	17	5	39890000.00	\N	0	TGDD-368223	\N	12	0.00	0	0	t	2026-09-24 20:54:16.394344	2026-09-24 20:54:16.394344	\N
436	Laptop Lenovo IdeaPad 5 2in1 14IPH11 - 83UG0026VN (Ultra 5 322, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	laptop-lenovo-ideapad-5-2in1-14iph11-83ug0026vn-ultra-5-322-16gb-512gb-wuxga-cam-ung-win11	\N	21	5	30590000.00	\N	0	TGDD-367012	\N	12	0.00	0	0	t	2026-09-24 20:54:16.411299	2026-09-24 20:54:16.411299	\N
437	Laptop Dell 16 DC16251 - DC6C7557W1 (Core 7 150U, 16GB, 1TB, Full HD+, OfficeH24+365, Win11)	laptop-dell-16-dc16251-dc6c7557w1-core-7-150u-16gb-1tb-full-hd-officeh24-365-win11	\N	20	5	33990000.00	\N	0	TGDD-362023	\N	12	0.00	0	0	t	2026-09-24 20:54:16.427302	2026-09-24 20:54:16.427302	\N
438	Laptop Dell Inspiron 14 5440 - 7FN5J (Core 7 150U, 16GB, 1TB, Full HD+, OfficeHS, Win11)	laptop-dell-inspiron-14-5440-7fn5j-core-7-150u-16gb-1tb-full-hd-officehs-win11	\N	20	5	28990000.00	\N	0	TGDD-325953	\N	12	0.00	0	0	t	2026-09-24 20:54:16.443674	2026-09-24 20:54:16.443674	\N
439	Laptop HP Gaming OMEN 16 am0176TX - BX9D3PA (Ultra 7 255H, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, Win11)	laptop-hp-gaming-omen-16-am0176tx-bx9d3pa-ultra-7-255h-16gb-512gb-rtx-5060-8gb-wuxga-165hz-win11	\N	17	5	43890000.00	\N	0	TGDD-342719	\N	12	0.00	0	0	t	2026-09-24 20:54:16.460162	2026-09-24 20:54:16.460162	\N
440	Laptop Lenovo Yoga 7 2in1 14IPH11 - 83TC002LVN (Ultra 5 322, 16GB, 512GB, WUXGA OLED, Cảm ứng, OfficeH24+365, Win11)	laptop-lenovo-yoga-7-2in1-14iph11-83tc002lvn-ultra-5-322-16gb-512gb-wuxga-oled-cam-ung-officeh24-365-win11	\N	21	5	38490000.00	\N	0	TGDD-368217	\N	12	0.00	0	0	t	2026-09-24 20:54:16.47818	2026-09-24 20:54:16.47818	\N
441	Laptop HP Probook 4 G1i 16 - BQ5D4PT (Ultra 5 225U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1i-16-bq5d4pt-ultra-5-225u-16gb-512gb-wuxga-win11	\N	17	5	29490000.00	\N	0	TGDD-345500	\N	12	0.00	0	0	t	2026-09-24 20:54:16.494514	2026-09-24 20:54:16.494514	\N
442	Laptop Dell 14 DC14250 - DC14250-C7U161W11SLU-2Y (Core 7 150U, 16GB, 1TB, Full HD+ ,OfficeH24+365, Win11)	laptop-dell-14-dc14250-dc14250-c7u161w11slu-2y-core-7-150u-16gb-1tb-full-hd-officeh24-365-win11	\N	20	5	34990000.00	\N	0	TGDD-369747	\N	12	0.00	0	0	t	2026-09-24 20:54:16.512271	2026-09-24 20:54:16.512271	\N
443	Laptop Lenovo ThinkBook 14 G9 - 21V0005PVN (R7 250, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkbook-14-g9-21v0005pvn-r7-250-16gb-512gb-wuxga-win11	\N	21	5	33390000.00	\N	0	TGDD-368218	\N	12	0.00	0	0	t	2026-09-24 20:54:16.52889	2026-09-24 20:54:16.52889	\N
444	Laptop HP EliteBook 6 G1i 13 - BQ9M6PT (Ultra 7 255U, 16GB, 512GB, WUXGA, Win11)	laptop-hp-elitebook-6-g1i-13-bq9m6pt-ultra-7-255u-16gb-512gb-wuxga-win11	\N	17	5	35990000.00	\N	0	TGDD-345508	\N	12	0.00	0	0	t	2026-09-24 20:54:16.546114	2026-09-24 20:54:16.546114	\N
445	Laptop HP OmniBook 7 16 az0038TU - C2CX1PA (Core 5 210H, 24GB, 512GB, WUXGA, OfficeH24, Win11)	laptop-hp-omnibook-7-16-az0038tu-c2cx1pa-core-5-210h-24gb-512gb-wuxga-officeh24-win11	\N	17	5	27990000.00	\N	0	TGDD-358794	\N	12	0.00	0	0	t	2026-09-24 20:54:16.563716	2026-09-24 20:54:16.563716	\N
446	Laptop Lenovo IdeaPad 5 2in1 14IPH11 - 83UG0027VN (Ultra 7 355, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	laptop-lenovo-ideapad-5-2in1-14iph11-83ug0027vn-ultra-7-355-16gb-512gb-wuxga-cam-ung-win11	\N	21	5	35990000.00	\N	0	TGDD-367013	\N	12	0.00	0	0	t	2026-09-24 20:54:16.581264	2026-09-24 20:54:16.581264	\N
447	Laptop HP ProBook 4 G1q 14 - C40JRAT (X1 26 100, 32GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1q-14-c40jrat-x1-26-100-32gb-512gb-wuxga-win11	\N	17	5	26590000.00	\N	0	TGDD-361730	\N	12	0.00	0	0	t	2026-09-24 20:54:16.597977	2026-09-24 20:54:16.597977	\N
448	Laptop Asus Zenbook 14 UM3406KA - PP113WS (R7 AI 350, 16GB, 512GB, 2.8K 120Hz, OfficeH24+365, Win11)	laptop-asus-zenbook-14-um3406ka-pp113ws-r7-ai-350-16gb-512gb-2-8k-120hz-officeh24-365-win11	\N	19	5	27390000.00	\N	0	TGDD-339235	\N	12	0.00	0	0	t	2026-09-24 20:54:16.615284	2026-09-24 20:54:16.615284	\N
449	Laptop HP Gaming OMEN 16 am0127TX - BX8Y0PA (i9 14900HX, 32GB, 512GB, RTX 5070 8GB, WUXGA 165Hz, Win11)	laptop-hp-gaming-omen-16-am0127tx-bx8y0pa-i9-14900hx-32gb-512gb-rtx-5070-8gb-wuxga-165hz-win11	\N	17	5	56990000.00	\N	0	TGDD-342721	\N	12	0.00	0	0	t	2026-09-24 20:54:16.632245	2026-09-24 20:54:16.632245	\N
450	Laptop Lenovo IdeaPad Slim 5 14AGP11 - 83S1006RVN (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	laptop-lenovo-ideapad-slim-5-14agp11-83s1006rvn-r7-ai-445-16gb-512gb-wuxga-oled-win11	\N	21	5	30990000.00	\N	0	TGDD-367021	\N	12	0.00	0	0	t	2026-09-24 20:54:16.649996	2026-09-24 20:54:16.649996	\N
451	Laptop HP OmniBook UF 14 fh0095TU - BZ7S2PA (Ultra 9 288V, 32GB, 1TB, 2.8K OLED 120Hz, Cảm ứng, OfficeH24, Win11)	laptop-hp-omnibook-uf-14-fh0095tu-bz7s2pa-ultra-9-288v-32gb-1tb-2-8k-oled-120hz-cam-ung-officeh24-win11	\N	17	5	59990000.00	\N	0	TGDD-341268	\N	12	0.00	0	0	t	2026-09-24 20:54:16.666174	2026-09-24 20:54:16.666174	\N
452	Laptop HP OmniBook 7 16 az0043TU - C2CX3PA (Core 7 240H, 32GB, 512GB, WUXGA, OfficeH24, Win11)	laptop-hp-omnibook-7-16-az0043tu-c2cx3pa-core-7-240h-32gb-512gb-wuxga-officeh24-win11	\N	17	5	34990000.00	\N	0	TGDD-358795	\N	12	0.00	0	0	t	2026-09-24 20:54:16.683048	2026-09-24 20:54:16.683048	\N
453	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010JVN (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Win11Pro)	laptop-lenovo-thinkpad-x1-carbon-gen-13-21ns010jvn-ultra-7-258v-32gb-1tb-2-8k-oled-120hz-win11pro	\N	21	5	68790000.00	\N	0	TGDD-364621	\N	12	0.00	0	0	t	2026-09-24 20:54:16.699543	2026-09-24 20:54:16.699543	\N
454	Laptop Lenovo IdeaPad Slim 3 14IPH11 - 83UQ003PVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-14iph11-83uq003pvn-ultra-7-355-16gb-512gb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-367685	\N	12	0.00	0	0	t	2026-09-24 20:54:16.71625	2026-09-24 20:54:16.71625	\N
455	Laptop HP Elitebook Ultra G1q - B4PY1PT (X1E 78 100, 32GB, 1TB, 2.2K, Cảm ứng, Win11 Pro)	laptop-hp-elitebook-ultra-g1q-b4py1pt-x1e-78-100-32gb-1tb-2-2k-cam-ung-win11-pro	\N	17	5	48790000.00	\N	0	TGDD-337040	\N	12	0.00	0	0	t	2026-09-24 20:54:16.733108	2026-09-24 20:54:16.733108	\N
456	Laptop HP Gaming HyperX Omen 15 ga0092TX - D72D8PA (i5 14450HX, 16GB, 512GB, RTX 5050 8GB, WUXGA 165Hz, Win11)	laptop-hp-gaming-hyperx-omen-15-ga0092tx-d72d8pa-i5-14450hx-16gb-512gb-rtx-5050-8gb-wuxga-165hz-win11	\N	17	5	45590000.00	\N	0	TGDD-368224	\N	12	0.00	0	0	t	2026-09-24 20:54:16.750896	2026-09-24 20:54:16.750896	\N
457	Laptop HP 15 fc0085AU - A6VV8PA (R5 7430U, 16GB, 512GB, Full HD, Win11)	laptop-hp-15-fc0085au-a6vv8pa-r5-7430u-16gb-512gb-full-hd-win11	\N	17	5	21290000.00	\N	0	TGDD-327098	\N	12	0.00	0	0	t	2026-09-24 20:54:16.767216	2026-09-24 20:54:16.767216	\N
458	Laptop HP EliteBook X G1a 14 AI - B9FE4PT (R9 AI HX PRO 375, 32GB, 1TB, 2.8K OLED 120Hz, Cảm ứng, Win11 Pro)	laptop-hp-elitebook-x-g1a-14-ai-b9fe4pt-r9-ai-hx-pro-375-32gb-1tb-2-8k-oled-120hz-cam-ung-win11-pro	\N	17	5	59590000.00	\N	0	TGDD-339558	\N	12	0.00	0	0	t	2026-09-24 20:54:16.783658	2026-09-24 20:54:16.783658	\N
459	Laptop HP OmniBook 7 16 az0040TU - C2DR3PA (Core 9 270H, 32GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-hp-omnibook-7-16-az0040tu-c2dr3pa-core-9-270h-32gb-512gb-wuxga-officeh24-365-win11	\N	17	5	39890000.00	\N	0	TGDD-358081	\N	12	0.00	0	0	t	2026-09-24 20:54:16.800102	2026-09-24 20:54:16.800102	\N
460	Laptop Lenovo Yoga Slim 7 OLED 14IPH11 - 83QM002FVN (Ultra 7 355, 32GB, 1TB, WUXGA OLED, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-oled-14iph11-83qm002fvn-ultra-7-355-32gb-1tb-wuxga-oled-officeh24-365-win11	\N	21	5	54490000.00	\N	0	TGDD-367019	\N	12	0.00	0	0	t	2026-09-24 20:54:16.816684	2026-09-24 20:54:16.816684	\N
461	Laptop HP HyperX Omen 15 ga0091TX - D72D7PA (i5 14450HX, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, Win11)	laptop-hp-hyperx-omen-15-ga0091tx-d72d7pa-i5-14450hx-16gb-512gb-rtx-5060-8gb-wuxga-165hz-win11	\N	17	5	48590000.00	\N	0	TGDD-368225	\N	12	0.00	0	0	t	2026-09-24 20:54:16.832823	2026-09-24 20:54:16.832823	\N
462	Laptop Lenovo IdeaPad Slim 5 14IPH11 - 83S5004FVN (Ultra 7 355, 16GB, 512GB, WUXGA OLED, Win11)	laptop-lenovo-ideapad-slim-5-14iph11-83s5004fvn-ultra-7-355-16gb-512gb-wuxga-oled-win11	\N	21	5	34990000.00	\N	0	TGDD-368213	\N	12	0.00	0	0	t	2026-09-24 20:54:16.850268	2026-09-24 20:54:16.850268	\N
463	Laptop HP Elitebook X360 830 G11 - A7RB9PT (Ultra 5 135U, 16GB, 512GB, WUXGA, Cảm ứng, Win11 Pro)	laptop-hp-elitebook-x360-830-g11-a7rb9pt-ultra-5-135u-16gb-512gb-wuxga-cam-ung-win11-pro	\N	17	5	43690000.00	\N	0	TGDD-331522	\N	12	0.00	0	0	t	2026-09-24 20:54:16.868518	2026-09-24 20:54:16.868518	\N
491	Laptop Asus ExpertBook P5 P5405CSA - NZ1368W (Ultra 5 226V, 16GB, 512GB, WQXGA 144Hz, Win11)	laptop-asus-expertbook-p5-p5405csa-nz1368w-ultra-5-226v-16gb-512gb-wqxga-144hz-win11	\N	19	5	30090000.00	\N	0	TGDD-369464	\N	12	0.00	0	0	t	2026-09-24 20:54:17.360546	2026-09-24 20:54:17.360546	\N
464	Laptop Lenovo Yoga Slim 7 Ultra 14IPH11 - 83QK006GVN (Ultra 7 355, 32GB, 1TB, WQXGA+ 120Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-ultra-14iph11-83qk006gvn-ultra-7-355-32gb-1tb-wqxga-120hz-cam-ung-officeh24-365-win11	\N	21	5	64990000.00	\N	0	TGDD-370357	\N	12	0.00	0	0	t	2026-09-24 20:54:16.885212	2026-09-24 20:54:16.885212	\N
465	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010EVN (Ultra 5 226V, 16GB, 1TB, 2.8K OLED 120Hz, Win11Pro)	laptop-lenovo-thinkpad-x1-carbon-gen-13-21ns010evn-ultra-5-226v-16gb-1tb-2-8k-oled-120hz-win11pro	\N	21	5	61390000.00	\N	0	TGDD-363896	\N	12	0.00	0	0	t	2026-09-24 20:54:16.902858	2026-09-24 20:54:16.902858	\N
466	Laptop MSI Gaming Stealth A16 Mercedes AMG AI+ A3XWGG - 032VN (R9 AI HX 370, 32GB, 2TB, RTX 5070 8GB, QHD+ OLED 240Hz, Win11)	laptop-msi-gaming-stealth-a16-mercedes-amg-ai-a3xwgg-032vn-r9-ai-hx-370-32gb-2tb-rtx-5070-8gb-qhd-oled-240hz-win11	\N	22	5	78590000.00	\N	0	TGDD-341573	\N	12	0.00	0	0	t	2026-09-24 20:54:16.919164	2026-09-24 20:54:16.919164	\N
467	Laptop Acer Aspire Lite 14 AL14-45P-R7Z3 - NX.DPESV.002 (R3 5400U, 8GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-lite-14-al14-45p-r7z3-nx-dpesv-002-r3-5400u-8gb-512gb-full-hd-win11	\N	23	5	15990000.00	\N	0	TGDD-367374	\N	12	0.00	0	0	t	2026-09-24 20:54:16.936395	2026-09-24 20:54:16.936395	\N
468	Laptop Asus Vivobook 14 X1407CA - LY203W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-14-x1407ca-ly203w-ultra-5-225h-16gb-512gb-wuxga-win11	\N	19	5	24090000.00	\N	0	TGDD-368301	\N	12	0.00	0	0	t	2026-09-24 20:54:16.952651	2026-09-24 20:54:16.952651	\N
469	Laptop Asus Vivobook 15 X1504VA - BQ185W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504va-bq185w-core-5-120u-16gb-512gb-full-hd-win11	\N	19	5	18990000.00	\N	0	TGDD-360418	\N	12	0.00	0	0	t	2026-09-24 20:54:16.968848	2026-09-24 20:54:16.968848	\N
470	Laptop Asus Vivobook Go 14 E1404FA - EB1832W (R5 40, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-go-14-e1404fa-eb1832w-r5-40-8gb-512gb-full-hd-win11	\N	19	5	18790000.00	\N	0	TGDD-368146	\N	12	0.00	0	0	t	2026-09-24 20:54:16.98695	2026-09-24 20:54:16.98695	\N
471	Laptop Acer Aspire Lite 15 AL15-410P-R1W8 - NX.X0RSV.001 (Ryzen 5 3500U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-410p-r1w8-nx-x0rsv-001-ryzen-5-3500u-8gb-512gb-full-hd-win11	\N	23	5	17990000.00	\N	0	TGDD-370862	\N	12	0.00	0	0	t	2026-09-24 20:54:17.004929	2026-09-24 20:54:17.004929	\N
472	Laptop Asus Gaming V16 V3607VJ Core 5 - TK189W (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WUXGA 144Hz, Win11)	laptop-asus-gaming-v16-v3607vj-core-5-tk189w-core-5-210h-16gb-512gb-rtx-3050-6gb-wuxga-144hz-win11	\N	19	5	25990000.00	\N	0	TGDD-369755	\N	12	0.00	0	0	t	2026-09-24 20:54:17.021141	2026-09-24 20:54:17.021141	\N
473	Laptop Asus Vivobook Go 14 E1404FA - EB012W (R5 40, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-go-14-e1404fa-eb012w-r5-40-8gb-512gb-full-hd-win11	\N	19	5	18790000.00	\N	0	TGDD-368302	\N	12	0.00	0	0	t	2026-09-24 20:54:17.037469	2026-09-24 20:54:17.037469	\N
474	Laptop Asus Vivobook 16 X1607CA - MB387W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-16-x1607ca-mb387w-ultra-5-225h-16gb-512gb-wuxga-win11	\N	19	5	24590000.00	\N	0	TGDD-369450	\N	12	0.00	0	0	t	2026-09-24 20:54:17.054429	2026-09-24 20:54:17.054429	\N
475	Laptop Asus Vivobook 15 X1504MA - BQ893W (Core 3 304, 8GB, 512GB, Full HD, Win 11)	laptop-asus-vivobook-15-x1504ma-bq893w-core-3-304-8gb-512gb-full-hd-win-11	\N	19	5	17990000.00	\N	0	TGDD-369758	\N	12	0.00	0	0	t	2026-09-24 20:54:17.07182	2026-09-24 20:54:17.07182	\N
476	Laptop Dell 14 DC14250 - DC14250-C3U085W11SLU-2Y (Core 3 100U, 8GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-14-dc14250-dc14250-c3u085w11slu-2y-core-3-100u-8gb-512gb-full-hd-officeh24-365-win11	\N	20	5	21490000.00	\N	0	TGDD-369746	\N	12	0.00	0	0	t	2026-09-24 20:54:17.088901	2026-09-24 20:54:17.088901	\N
477	Laptop Asus Vivobook S14 M3407HA - SF480W (R5 220, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s14-m3407ha-sf480w-r5-220-16gb-512gb-wuxga-oled-win11	\N	19	5	22990000.00	\N	0	TGDD-364941	\N	12	0.00	0	0	t	2026-09-24 20:54:17.106731	2026-09-24 20:54:17.106731	\N
478	Laptop Asus Vivobook 14 X1404MA - EB219W (Core 5 320, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-14-x1404ma-eb219w-core-5-320-8gb-512gb-full-hd-win11	\N	19	5	19990000.00	\N	0	TGDD-367892	\N	12	0.00	0	0	t	2026-09-24 20:54:17.124734	2026-09-24 20:54:17.124734	\N
479	Laptop Asus Vivobook 15 X1504MA - BQ385W (Core 5 320, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504ma-bq385w-core-5-320-16gb-512gb-full-hd-win11	\N	19	5	23990000.00	\N	0	TGDD-368147	\N	12	0.00	0	0	t	2026-09-24 20:54:17.142176	2026-09-24 20:54:17.142176	\N
480	Laptop MSI Cyborg 15 Black Edition A13VEO - 2608VN (i7 13620H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-msi-cyborg-15-black-edition-a13veo-2608vn-i7-13620h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	22	5	29890000.00	\N	0	TGDD-369984	\N	12	0.00	0	0	t	2026-09-24 20:54:17.160239	2026-09-24 20:54:17.160239	\N
481	Laptop Asus TUF Gaming F16 FX608JMI - TU241W (i7 14650HX, 16GB, 512GB, RTX 5060 8GB, Full HD+ 144Hz, Win11)	laptop-asus-tuf-gaming-f16-fx608jmi-tu241w-i7-14650hx-16gb-512gb-rtx-5060-8gb-full-hd-144hz-win11	\N	19	5	42490000.00	\N	0	TGDD-364489	\N	12	0.00	0	0	t	2026-09-24 20:54:17.177119	2026-09-24 20:54:17.177702	\N
482	Laptop Asus Zenbook 14 UX3405CA - ST629W (Ultra 7 255H, 32GB, 512GB, 3K OLED 120Hz, Win11)	laptop-asus-zenbook-14-ux3405ca-st629w-ultra-7-255h-32gb-512gb-3k-oled-120hz-win11	\N	19	5	35590000.00	\N	0	TGDD-364403	\N	12	0.00	0	0	t	2026-09-24 20:54:17.196372	2026-09-24 20:54:17.196372	\N
483	Laptop Acer Aspire Lite 15 AL15-44P-R4UH - NX.DJVSV.002 (R7 7730U, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-44p-r4uh-nx-djvsv-002-r7-7730u-16gb-512gb-full-hd-win11	\N	23	5	21990000.00	\N	0	TGDD-368658	\N	12	0.00	0	0	t	2026-09-24 20:54:17.216651	2026-09-24 20:54:17.216651	\N
484	Laptop Asus Vivobook 15 X1504MA - BQ632W (Core 5 320, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504ma-bq632w-core-5-320-8gb-512gb-full-hd-win11	\N	19	5	19990000.00	\N	0	TGDD-367898	\N	12	0.00	0	0	t	2026-09-24 20:54:17.236299	2026-09-24 20:54:17.236299	\N
485	Laptop Macbook Air 13 inch M5 24GB/512GB 35W	laptop-macbook-air-13-inch-m5-24gb-512gb-35w	\N	18	5	39790000.00	\N	0	TGDD-363503	\N	12	0.00	0	0	t	2026-09-24 20:54:17.253608	2026-09-24 20:54:17.253608	\N
486	Laptop Acer Aspire Lite 15 AL15-53P-56EC - NX.DG3SV.002.16G (Core 5 120U, 16GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-15-al15-53p-56ec-nx-dg3sv-002-16g-core-5-120u-16gb-512gb-full-hd-win11	\N	23	5	21990000.00	\N	0	TGDD-368657	\N	12	0.00	0	0	t	2026-09-24 20:54:17.271088	2026-09-24 20:54:17.271088	\N
487	Laptop Dell Pro 15 Essential PV15250 - PV15250-100U-8512W-2Y (Core 3 100U, 8GB, 512GB, Full HD, OfficeH24+365, Win11)	laptop-dell-pro-15-essential-pv15250-pv15250-100u-8512w-2y-core-3-100u-8gb-512gb-full-hd-officeh24-365-win11	\N	20	5	22490000.00	\N	0	TGDD-369749	\N	12	0.00	0	0	t	2026-09-24 20:54:17.289618	2026-09-24 20:54:17.289618	\N
488	Laptop MacBook Air 15 inch M5 24GB/512GB 70W	laptop-macbook-air-15-inch-m5-24gb-512gb-70w	\N	18	5	44990000.00	\N	0	TGDD-363511	\N	12	0.00	0	0	t	2026-09-24 20:54:17.30697	2026-09-24 20:54:17.30697	\N
489	Laptop MSI Cyborg 15 C13WEO - 418VN (i5 13420H, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	laptop-msi-cyborg-15-c13weo-418vn-i5-13420h-16gb-512gb-rtx-5050-8gb-full-hd-144hz-win11	\N	22	5	30990000.00	\N	0	TGDD-369983	\N	12	0.00	0	0	t	2026-09-24 20:54:17.324613	2026-09-24 20:54:17.324613	\N
490	Laptop Asus Gaming ROG Strix G16 G614PH - S5101W (R9 8940HX, 16GB, 512GB, RTX 5050 8GB, WQXGA 240Hz, Win11)	laptop-asus-gaming-rog-strix-g16-g614ph-s5101w-r9-8940hx-16gb-512gb-rtx-5050-8gb-wqxga-240hz-win11	\N	19	5	46590000.00	\N	0	TGDD-364409	\N	12	0.00	0	0	t	2026-09-24 20:54:17.341679	2026-09-24 20:54:17.341679	\N
630	Máy tính bảng Samsung Galaxy Tab S10+ WiFi 12GB/256GB	may-tinh-bang-samsung-galaxy-tab-s10-wifi-12gb-256gb	\N	7	6	19840000.00	\N	0	TGDD-322130	\N	12	0.00	0	0	t	2026-09-24 20:54:19.737809	2026-09-24 20:54:19.737809	\N
492	Laptop Asus Vivobook S 16 S3607VA - RP155W (Core 5 210H, 16GB, 512GB, Full HD+ 144Hz, Win11)	laptop-asus-vivobook-s-16-s3607va-rp155w-core-5-210h-16gb-512gb-full-hd-144hz-win11	\N	19	5	26990000.00	\N	0	TGDD-369469	\N	12	0.00	0	0	t	2026-09-24 20:54:17.378191	2026-09-24 20:54:17.378191	\N
493	Laptop MacBook Pro 14 inch M5 Pro 24GB/1TB 70W	laptop-macbook-pro-14-inch-m5-pro-24gb-1tb-70w	\N	18	5	66990000.00	\N	0	TGDD-363488	\N	12	0.00	0	0	t	2026-09-24 20:54:17.395498	2026-09-24 20:54:17.395498	\N
494	Laptop Asus Vivobook S14 M3407KA - SF034WS (R5 AI 330, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	laptop-asus-vivobook-s14-m3407ka-sf034ws-r5-ai-330-16gb-512gb-wuxga-oled-officeh24-365-win11	\N	19	5	23790000.00	\N	0	TGDD-351616	\N	12	0.00	0	0	t	2026-09-24 20:54:17.412607	2026-09-24 20:54:17.412607	\N
495	Laptop ASUS Vivobook 14 X1407AA - LY360W (Ultra 5 325, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-14-x1407aa-ly360w-ultra-5-325-16gb-512gb-wuxga-win11	\N	19	5	24390000.00	\N	0	TGDD-366826	\N	12	0.00	0	0	t	2026-09-24 20:54:17.42952	2026-09-24 20:54:17.42952	\N
496	Laptop Lenovo Gaming LOQ 15IRX10 - 83JE00PEVN (i7 13650HX, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	laptop-lenovo-gaming-loq-15irx10-83je00pevn-i7-13650hx-16gb-512gb-rtx-5050-8gb-full-hd-144hz-win11	\N	21	5	39990000.00	\N	0	TGDD-358371	\N	12	0.00	0	0	t	2026-09-24 20:54:17.446805	2026-09-24 20:54:17.446805	\N
497	Laptop Asus Vivobook 14 X1404MA - EB028W (Core 5 320, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-14-x1404ma-eb028w-core-5-320-16gb-512gb-full-hd-win11	\N	19	5	23990000.00	\N	0	TGDD-369449	\N	12	0.00	0	0	t	2026-09-24 20:54:17.464725	2026-09-24 20:54:17.464725	\N
498	Laptop Asus Vivobook S14 S3407CA - SF923W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-s14-s3407ca-sf923w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	28890000.00	\N	0	TGDD-364416	\N	12	0.00	0	0	t	2026-09-24 20:54:17.482588	2026-09-24 20:54:17.482588	\N
499	Laptop Asus Vivobook 15 X1504MA - BQ395W (Core 7 350, 16GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504ma-bq395w-core-7-350-16gb-512gb-full-hd-win11	\N	19	5	27990000.00	\N	0	TGDD-368148	\N	12	0.00	0	0	t	2026-09-24 20:54:17.499332	2026-09-24 20:54:17.499332	\N
500	Laptop MSI Cyborg 15 Black Edition A13VEO - 2610VN (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	laptop-msi-cyborg-15-black-edition-a13veo-2610vn-i5-13420h-16gb-512gb-rtx-4050-6gb-full-hd-144hz-win11	\N	22	5	27890000.00	\N	0	TGDD-369985	\N	12	0.00	0	0	t	2026-09-24 20:54:17.515806	2026-09-24 20:54:17.515806	\N
501	Laptop Asus Gaming V16 V3607VU - TK416W (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, WUXGA 144Hz, Win11)	laptop-asus-gaming-v16-v3607vu-tk416w-core-5-210h-16gb-512gb-rtx-4050-6gb-wuxga-144hz-win11	\N	19	5	32990000.00	\N	0	TGDD-371075	\N	12	0.00	0	0	t	2026-09-24 20:54:17.533316	2026-09-24 20:54:17.533316	\N
502	Laptop Asus Vivobook S14 S3407VA - LY256W (Core 7 240H, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s14-s3407va-ly256w-core-7-240h-16gb-512gb-wuxga-oled-win11	\N	19	5	26990000.00	\N	0	TGDD-368300	\N	12	0.00	0	0	t	2026-09-24 20:54:17.550278	2026-09-24 20:54:17.550278	\N
503	Laptop Dell 15 DC15250 - DC5C3851W1-2Y (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	laptop-dell-15-dc15250-dc5c3851w1-2y-core-3-100u-8gb-512gb-full-hd-120hz-officeh24-365-win11	\N	20	5	21490000.00	\N	0	TGDD-369748	\N	12	0.00	0	0	t	2026-09-24 20:54:17.567703	2026-09-24 20:54:17.567703	\N
504	Laptop Asus TUF Gaming A16 FA608UM - RV266W (R7 260, 16GB, 512GB, RTX5060 8GB, Full HD+ 165Hz, Win11)	laptop-asus-tuf-gaming-a16-fa608um-rv266w-r7-260-16gb-512gb-rtx5060-8gb-full-hd-165hz-win11	\N	19	5	43590000.00	\N	0	TGDD-364490	\N	12	0.00	0	0	t	2026-09-24 20:54:17.584635	2026-09-24 20:54:17.584635	\N
505	Laptop Dell 14 DC14250 - DC14250-C7U161W11SLU-27 (Core 7 150U, 16GB, 1TB, Full HD+, OfficeHS24+365, Win11)	laptop-dell-14-dc14250-dc14250-c7u161w11slu-27-core-7-150u-16gb-1tb-full-hd-officehs24-365-win11	\N	20	5	31990000.00	\N	0	TGDD-365309	\N	12	0.00	0	0	t	2026-09-24 20:54:17.601669	2026-09-24 20:54:17.601669	\N
506	Laptop Asus Vivobook 15 X1504MA - BQ633W (Core 7 350, 8GB, 512GB, Full HD, Win11)	laptop-asus-vivobook-15-x1504ma-bq633w-core-7-350-8gb-512gb-full-hd-win11	\N	19	5	23990000.00	\N	0	TGDD-367899	\N	12	0.00	0	0	t	2026-09-24 20:54:17.618643	2026-09-24 20:54:17.618643	\N
507	Laptop Asus ExpertBook P5 P5405CSA - NZ1371W (Ultra 7 258V, 32GB, 512GB, WQXGA 144Hz, Win11)	laptop-asus-expertbook-p5-p5405csa-nz1371w-ultra-7-258v-32gb-512gb-wqxga-144hz-win11	\N	19	5	37490000.00	\N	0	TGDD-369463	\N	12	0.00	0	0	t	2026-09-24 20:54:17.635751	2026-09-24 20:54:17.635751	\N
508	Laptop GIGABYTE Gaming A16 - 3THK3VN893SH (R7 260, 16GB, 512GB, RTX 5050 8GB, Full HD+ 165Hz, Win11)	laptop-gigabyte-gaming-a16-3thk3vn893sh-r7-260-16gb-512gb-rtx-5050-8gb-full-hd-165hz-win11	\N	25	5	35490000.00	\N	0	TGDD-367862	\N	12	0.00	0	0	t	2026-09-24 20:54:17.65297	2026-09-24 20:54:17.65297	\N
509	Laptop GIGABYTE Gaming AERO X16 - 1VH93VNC94AH (R7 AI 350, 16GB, 1TB, RTX 5060 8GB, WQXGA 165Hz, Win11)	laptop-gigabyte-gaming-aero-x16-1vh93vnc94ah-r7-ai-350-16gb-1tb-rtx-5060-8gb-wqxga-165hz-win11	\N	25	5	42590000.00	\N	0	TGDD-367864	\N	12	0.00	0	0	t	2026-09-24 20:54:17.669934	2026-09-24 20:54:17.669934	\N
510	Laptop Dell Pro 14 PC14250 - PC14250-235U-16512WP-2Y (Ultra 5 235U, 16GB, 512GB, Full HD+, Win11Pro)	laptop-dell-pro-14-pc14250-pc14250-235u-16512wp-2y-ultra-5-235u-16gb-512gb-full-hd-win11pro	\N	20	5	37290000.00	\N	0	TGDD-367347	\N	12	0.00	0	0	t	2026-09-24 20:54:17.686211	2026-09-24 20:54:17.686211	\N
511	Laptop Dell 16 Plus DB16250 - DB6U5387W1 (Ultra 5 226V, 16GB, 1TB, WQXGA, OfficeH24+365, Win11)	laptop-dell-16-plus-db16250-db6u5387w1-ultra-5-226v-16gb-1tb-wqxga-officeh24-365-win11	\N	20	5	36290000.00	\N	0	TGDD-362022	\N	12	0.00	0	0	t	2026-09-24 20:54:17.703445	2026-09-24 20:54:17.703445	\N
512	Laptop Dell 16 DC16250 - 71092481 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeHS24+365, Win11)	laptop-dell-16-dc16250-71092481-core-5-120u-16gb-1tb-full-hd-officehs24-365-win11	\N	20	5	27990000.00	\N	0	TGDD-365307	\N	12	0.00	0	0	t	2026-09-24 20:54:17.72073	2026-09-24 20:54:17.72073	\N
513	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US002TVN (Ultra 5 322, 24GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-16iph11-83us002tvn-ultra-5-322-24gb-512gb-wuxga-win11	\N	21	5	28490000.00	\N	0	TGDD-367682	\N	12	0.00	0	0	t	2026-09-24 20:54:17.737064	2026-09-24 20:54:17.737064	\N
514	Laptop Asus ExpertBook P5 P5406CCA - SF0045W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-expertbook-p5-p5406cca-sf0045w-ultra-5-225h-16gb-512gb-wuxga-win11	\N	19	5	30990000.00	\N	0	TGDD-369467	\N	12	0.00	0	0	t	2026-09-24 20:54:17.754182	2026-09-24 20:54:17.754182	\N
515	Laptop Acer Gaming Nitro ProPanel ANV15-52-78MD - NH.QZ9SV.007 (Core 7 240H, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv15-52-78md-nh-qz9sv-007-core-7-240h-16gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	40990000.00	\N	0	TGDD-370280	\N	12	0.00	0	0	t	2026-09-24 20:54:17.770701	2026-09-24 20:54:17.770701	\N
516	Laptop Asus Vivobook 16 X1607CA - MB399W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-vivobook-16-x1607ca-mb399w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	26690000.00	\N	0	TGDD-371299	\N	12	0.00	0	0	t	2026-09-24 20:54:17.786703	2026-09-24 20:54:17.786703	\N
517	Laptop Acer Aspire Lite 14 AL14-47P-R0TR - NX.X0PSV.002 (Ryzen 5 3500U, 8GB, 512GB, Full HD, Win11)	laptop-acer-aspire-lite-14-al14-47p-r0tr-nx-x0psv-002-ryzen-5-3500u-8gb-512gb-full-hd-win11	\N	23	5	17990000.00	\N	0	TGDD-370863	\N	12	0.00	0	0	t	2026-09-24 20:54:17.803208	2026-09-24 20:54:17.803208	\N
518	Laptop HP Probook 4 G1i 16 - BQ5E4PT (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1i-16-bq5e4pt-ultra-7-255h-16gb-512gb-wuxga-win11	\N	17	5	32990000.00	\N	0	TGDD-345504	\N	12	0.00	0	0	t	2026-09-24 20:54:17.820235	2026-09-24 20:54:17.820235	\N
519	Laptop Lenovo ThinkPad E16 Gen 3 - 22AY003UVN (Ultra 7 258V, 32GB, 1TB, WUXGA, Win11)	laptop-lenovo-thinkpad-e16-gen-3-22ay003uvn-ultra-7-258v-32gb-1tb-wuxga-win11	\N	21	5	40090000.00	\N	0	TGDD-363894	\N	12	0.00	0	0	t	2026-09-24 20:54:17.836265	2026-09-24 20:54:17.836265	\N
520	Laptop Asus TUF Gaming FX608JHI - TU209W (i7 14650HX, 16GB, 512GB, RTX5050 8GB, Full HD+ 144Hz, Win11)	laptop-asus-tuf-gaming-fx608jhi-tu209w-i7-14650hx-16gb-512gb-rtx5050-8gb-full-hd-144hz-win11	\N	19	5	42990000.00	\N	0	TGDD-365113	\N	12	0.00	0	0	t	2026-09-24 20:54:17.852607	2026-09-24 20:54:17.852607	\N
521	Laptop MacBook Air 15 inch M5 32GB/512GB	laptop-macbook-air-15-inch-m5-32gb-512gb	\N	18	5	51790000.00	\N	0	TGDD-363513	\N	12	0.00	0	0	t	2026-09-24 20:54:17.869554	2026-09-24 20:54:17.869554	\N
522	Laptop GIGABYTE Gaming A16 - 3VHK3VN893SH (R7 260, 16GB, 512GB, RTX 5060 8GB, Full HD+ 165Hz, Win11)	laptop-gigabyte-gaming-a16-3vhk3vn893sh-r7-260-16gb-512gb-rtx-5060-8gb-full-hd-165hz-win11	\N	25	5	37490000.00	\N	0	TGDD-367863	\N	12	0.00	0	0	t	2026-09-24 20:54:17.88559	2026-09-24 20:54:17.88559	\N
523	Laptop Lenovo V14 G5 - 83HD0035VN (Core7 240H, 16G, 512GB, Full HD, Win11)	laptop-lenovo-v14-g5-83hd0035vn-core7-240h-16g-512gb-full-hd-win11	\N	21	5	31290000.00	\N	0	TGDD-368219	\N	12	0.00	0	0	t	2026-09-24 20:54:17.902457	2026-09-24 20:54:17.902457	\N
524	Laptop MSI Gaming Cyborg 15 C13WEO-417VN (i7 13620H, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	laptop-msi-gaming-cyborg-15-c13weo-417vn-i7-13620h-16gb-512gb-rtx-5050-8gb-full-hd-144hz-win11	\N	22	5	34590000.00	\N	0	TGDD-369213	\N	12	0.00	0	0	t	2026-09-24 20:54:17.919763	2026-09-24 20:54:17.919763	\N
525	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US0040VN (Ultra 5 322, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-16iph11-83us0040vn-ultra-5-322-16gb-512gb-wuxga-win11	\N	21	5	23690000.00	\N	0	TGDD-370349	\N	12	0.00	0	0	t	2026-09-24 20:54:17.937701	2026-09-24 20:54:17.937701	\N
526	Laptop Lenovo Yoga Slim 7 Ultra 14IPH11 - 83QK0015VN (Ultra 7 355, 32GB, 1TB, WQXGA+ 120Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-ultra-14iph11-83qk0015vn-ultra-7-355-32gb-1tb-wqxga-120hz-cam-ung-officeh24-365-win11	\N	21	5	62990000.00	\N	0	TGDD-370356	\N	12	0.00	0	0	t	2026-09-24 20:54:17.955484	2026-09-24 20:54:17.955484	\N
527	Laptop Acer Aspire Go 14 AI AG14-I71M-977M - NX.W26SV.001 (Ultra 9 185H, 16GB, 512GB, Full HD+, Win11)	laptop-acer-aspire-go-14-ai-ag14-i71m-977m-nx-w26sv-001-ultra-9-185h-16gb-512gb-full-hd-win11	\N	23	5	29990000.00	\N	0	TGDD-370866	\N	12	0.00	0	0	t	2026-09-24 20:54:17.975187	2026-09-24 20:54:17.975187	\N
528	Laptop Asus TUF Gaming A14 FA401GM - RG013W (R9 AI 465, 32GB, 1TB, RTX 5060 8GB, WQXGA 165Hz, Win11)	laptop-asus-tuf-gaming-a14-fa401gm-rg013w-r9-ai-465-32gb-1tb-rtx-5060-8gb-wqxga-165hz-win11	\N	19	5	56590000.00	\N	0	TGDD-364408	\N	12	0.00	0	0	t	2026-09-24 20:54:17.992573	2026-09-24 20:54:17.992573	\N
529	Laptop HP EliteBook 6 G1a 14 - C0CE2PT (R7 AI 350, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	laptop-hp-elitebook-6-g1a-14-c0ce2pt-r7-ai-350-16gb-512gb-wuxga-cam-ung-win11	\N	17	5	34990000.00	\N	0	TGDD-365617	\N	12	0.00	0	0	t	2026-09-24 20:54:18.011047	2026-09-24 20:54:18.011047	\N
530	Laptop Dell 16 Plus DB16250 - X65NW9 (Ultra 9 288V, 32GB, 2TB, QHD+ 120Hz, OfficeH24+365, Win11)	laptop-dell-16-plus-db16250-x65nw9-ultra-9-288v-32gb-2tb-qhd-120hz-officeh24-365-win11	\N	20	5	56990000.00	\N	0	TGDD-361539	\N	12	0.00	0	0	t	2026-09-24 20:54:18.027299	2026-09-24 20:54:18.027299	\N
531	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010FVN (Ultra 7 256V, 16GB, 512GB, 2.8K OLED 120Hz, Win11Pro)	laptop-lenovo-thinkpad-x1-carbon-gen-13-21ns010fvn-ultra-7-256v-16gb-512gb-2-8k-oled-120hz-win11pro	\N	21	5	62390000.00	\N	0	TGDD-363899	\N	12	0.00	0	0	t	2026-09-24 20:54:18.043634	2026-09-24 20:54:18.043634	\N
532	Laptop Asus Gaming ROG Flow Z13 GZ302EAC - RU184WS (AI MAX+ 395, 128GB, 1TB, WQXGA 180Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-asus-gaming-rog-flow-z13-gz302eac-ru184ws-ai-max-395-128gb-1tb-wqxga-180hz-cam-ung-officeh24-365-win11	\N	19	5	109990000.00	\N	0	TGDD-364389	\N	12	0.00	0	0	t	2026-09-24 20:54:18.060873	2026-09-24 20:54:18.060873	\N
533	Laptop Asus TUF Gaming A16 FA608PP - RV089W (R9 8940HX, 16GB, 512GB, RTX5070 8GB, WUXGA 165Hz, Win11)	laptop-asus-tuf-gaming-a16-fa608pp-rv089w-r9-8940hx-16gb-512gb-rtx5070-8gb-wuxga-165hz-win11	\N	19	5	57590000.00	\N	0	TGDD-364488	\N	12	0.00	0	0	t	2026-09-24 20:54:18.080373	2026-09-24 20:54:18.080373	\N
534	Laptop Dell 14 DC14255 - 71092477 (R7 350, 16GB, 1TB, Full HD+, OfficeHS24+365B, Win11)	laptop-dell-14-dc14255-71092477-r7-350-16gb-1tb-full-hd-officehs24-365b-win11	\N	20	5	31490000.00	\N	0	TGDD-365302	\N	12	0.00	0	0	t	2026-09-24 20:54:18.096348	2026-09-24 20:54:18.096348	\N
535	Laptop Asus Gaming ROG Zephyrus GA403GM - SY004W (R9 AI 465, 32GB, 1TB, RTX 5060 8GB, 3K OLED 120Hz, Win11)	laptop-asus-gaming-rog-zephyrus-ga403gm-sy004w-r9-ai-465-32gb-1tb-rtx-5060-8gb-3k-oled-120hz-win11	\N	19	5	64590000.00	\N	0	TGDD-366068	\N	12	0.00	0	0	t	2026-09-24 20:54:18.113939	2026-09-24 20:54:18.113939	\N
536	Laptop Asus Vivobook S14 S3407AA - SF945W (Ultra 5 325, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-vivobook-s14-s3407aa-sf945w-ultra-5-325-16gb-512gb-wuxga-oled-win11	\N	19	5	29890000.00	\N	0	TGDD-366827	\N	12	0.00	0	0	t	2026-09-24 20:54:18.130564	2026-09-24 20:54:18.130564	\N
537	Laptop Lenovo Gaming Legion 5 15IPH11 - 83RW0023VN (Ultra 7 356H, 16GB, 512GB, RTX 5060 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	laptop-lenovo-gaming-legion-5-15iph11-83rw0023vn-ultra-7-356h-16gb-512gb-rtx-5060-8gb-wqxga-oled-165hz-officeh24-win11	\N	21	5	60990000.00	\N	0	TGDD-367015	\N	12	0.00	0	0	t	2026-09-24 20:54:18.147207	2026-09-24 20:54:18.147207	\N
538	Laptop Dell Pro 14 PC14250 - PC14250-255U-32512WH-2Y (Ultra 7 255U, 32GB, 512GB, Full HD+, Win11)	laptop-dell-pro-14-pc14250-pc14250-255u-32512wh-2y-ultra-7-255u-32gb-512gb-full-hd-win11	\N	20	5	54290000.00	\N	0	TGDD-367348	\N	12	0.00	0	0	t	2026-09-24 20:54:18.162952	2026-09-24 20:54:18.162952	\N
539	Laptop Asus Gaming ROG Strix G16 G614PM - TS147W (R9 8940HX, 16GB, 512GB, RTX 5060 8GB, WQXGA 300Hz, Win11)	laptop-asus-gaming-rog-strix-g16-g614pm-ts147w-r9-8940hx-16gb-512gb-rtx-5060-8gb-wqxga-300hz-win11	\N	19	5	54990000.00	\N	0	TGDD-368149	\N	12	0.00	0	0	t	2026-09-24 20:54:18.180097	2026-09-24 20:54:18.180097	\N
540	Laptop Asus Gaming ROG Strix G16 G614PR - TS103W (R9 8940HX, 16GB, 512GB, RTX 5070Ti 12GB, WQXGA 300Hz, Win11)	laptop-asus-gaming-rog-strix-g16-g614pr-ts103w-r9-8940hx-16gb-512gb-rtx-5070ti-12gb-wqxga-300hz-win11	\N	19	5	70590000.00	\N	0	TGDD-368150	\N	12	0.00	0	0	t	2026-09-24 20:54:18.197299	2026-09-24 20:54:18.197299	\N
541	Laptop Lenovo ThinkPad X9 14 Gen 1 - 21QA006JVN (Ultra 7 258V, 32GB, 512GB, WUXGA OLED, Win11Pro)	laptop-lenovo-thinkpad-x9-14-gen-1-21qa006jvn-ultra-7-258v-32gb-512gb-wuxga-oled-win11pro	\N	21	5	53790000.00	\N	0	TGDD-368212	\N	12	0.00	0	0	t	2026-09-24 20:54:18.213769	2026-09-24 20:54:18.213769	\N
542	Laptop Lenovo Yoga 7 2in1 14IPH11 - 83TC002MVN (Ultra 7 355, 32GB, 512GB, WUXGA OLED, Cảm ứng, OfficeH24+365, Win11)	laptop-lenovo-yoga-7-2in1-14iph11-83tc002mvn-ultra-7-355-32gb-512gb-wuxga-oled-cam-ung-officeh24-365-win11	\N	21	5	55990000.00	\N	0	TGDD-368216	\N	12	0.00	0	0	t	2026-09-24 20:54:18.230391	2026-09-24 20:54:18.230391	\N
543	Laptop Acer Gaming Nitro ProPanel ANV16S-71-58WQ - NH.QXBSV.001 (Core 5 210H, 16GB, 512GB, RTX 5050 8GB, 2K+ 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv16s-71-58wq-nh-qxbsv-001-core-5-210h-16gb-512gb-rtx-5050-8gb-2k-180hz-win11	\N	23	5	44990000.00	\N	0	TGDD-368651	\N	12	0.00	0	0	t	2026-09-24 20:54:18.245993	2026-09-24 20:54:18.245993	\N
544	Laptop Asus ExpertBook Ultra B9 B9406CAA - X7321TW (Ultra X7 358H, 32GB, 1TB, WQXGA+ OLED 120Hz, Win11)	laptop-asus-expertbook-ultra-b9-b9406caa-x7321tw-ultra-x7-358h-32gb-1tb-wqxga-oled-120hz-win11	\N	19	5	65990000.00	\N	0	TGDD-369461	\N	12	0.00	0	0	t	2026-09-24 20:54:18.261819	2026-09-24 20:54:18.261819	\N
545	Laptop Acer Swift Go AI SFG14-75-5264 - NX.JNBSV.001 (Ultra 5 226V, 16GB, 512GB, Full HD, Win11)	laptop-acer-swift-go-ai-sfg14-75-5264-nx-jnbsv-001-ultra-5-226v-16gb-512gb-full-hd-win11	\N	23	5	29990000.00	\N	0	TGDD-369979	\N	12	0.00	0	0	t	2026-09-24 20:54:18.278304	2026-09-24 20:54:18.278304	\N
546	Laptop Acer Nitro ProPanel ANV16-72-782F - NH.QUPSV.001 (Core 7 240H, 16GB, 512GB, RTX 5050 8GB, Full HD+ 180 Hz, Win11)	laptop-acer-nitro-propanel-anv16-72-782f-nh-qupsv-001-core-7-240h-16gb-512gb-rtx-5050-8gb-full-hd-180-hz-win11	\N	23	5	43990000.00	\N	0	TGDD-369980	\N	12	0.00	0	0	t	2026-09-24 20:54:18.294678	2026-09-24 20:54:18.294678	\N
547	Laptop Lenovo ThinkPad E14 - 21T90025VN (Gen 7 Core 7 240H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-21t90025vn-gen-7-core-7-240h-16gb-512gb-wuxga-win11	\N	21	5	34190000.00	\N	0	TGDD-370352	\N	12	0.00	0	0	t	2026-09-24 20:54:18.312573	2026-09-24 20:54:18.312573	\N
548	Laptop Lenovo Yoga Slim 7 14IPH11 - 83QM0076VN (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-14iph11-83qm0076vn-ultra-7-355-16gb-512gb-wuxga-officeh24-365-win11	\N	21	5	40990000.00	\N	0	TGDD-370355	\N	12	0.00	0	0	t	2026-09-24 20:54:18.328905	2026-09-24 20:54:18.328905	\N
549	Laptop HP Gaming OMEN 14 fb0135TX - AY8V1PA (Ultra 7 155H, 16GB, 1TB, RTX 4060 8GB, 2.8K OLED 120Hz, Win11)	laptop-hp-gaming-omen-14-fb0135tx-ay8v1pa-ultra-7-155h-16gb-1tb-rtx-4060-8gb-2-8k-oled-120hz-win11	\N	17	5	62790000.00	\N	0	TGDD-332274	\N	12	0.00	0	0	t	2026-09-24 20:54:18.345827	2026-09-24 20:54:18.345827	\N
550	Laptop Acer Gaming Predator Helios 18 AI PH18 73 98AQ - NH.QVWSV.001 (Ultra 9 275HX, 192GB, 6TB, RTX 5090 24GB, 4K 120Hz, Win11 Pro)	laptop-acer-gaming-predator-helios-18-ai-ph18-73-98aq-nh-qvwsv-001-ultra-9-275hx-192gb-6tb-rtx-5090-24gb-4k-120hz-win11-pro	\N	23	5	149990000.00	\N	0	TGDD-335963	\N	12	0.00	0	0	t	2026-09-24 20:54:18.362659	2026-09-24 20:54:18.362659	\N
551	Laptop Acer Gaming Predator Helios 18 AI PH18 73 93P0 - NH.QVYSV.001 (Ultra 9 275HX, 64GB, 3TB, RTX 5080 12GB, 2.5K 250Hz, Win11 Pro)	laptop-acer-gaming-predator-helios-18-ai-ph18-73-93p0-nh-qvysv-001-ultra-9-275hx-64gb-3tb-rtx-5080-12gb-2-5k-250hz-win11-pro	\N	23	5	99990000.00	\N	0	TGDD-335964	\N	12	0.00	0	0	t	2026-09-24 20:54:18.379557	2026-09-24 20:54:18.379557	\N
552	Laptop Lenovo Gaming Legion 5 15AHP10 - 83M0002YVN (R7 260, 16GB, 512GB, RTX 5050 8GB, WQXGA OLED 165Hz, Win11)	laptop-lenovo-gaming-legion-5-15ahp10-83m0002yvn-r7-260-16gb-512gb-rtx-5050-8gb-wqxga-oled-165hz-win11	\N	21	5	42990000.00	\N	0	TGDD-342527	\N	12	0.00	0	0	t	2026-09-24 20:54:18.395965	2026-09-24 20:54:18.395965	\N
553	Laptop Dell Inspiron 14 5441 - 71069158 (X1 26 100, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	laptop-dell-inspiron-14-5441-71069158-x1-26-100-16gb-512gb-full-hd-officeh24-365-win11	\N	20	5	0.00	\N	0	TGDD-361537	\N	12	0.00	0	0	t	2026-09-24 20:54:18.411699	2026-09-24 20:54:18.411699	\N
554	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS0107VN (Ultra 5 226V, 16GB, 512GB, 2.8K OLED 120Hz, Win11Pro)	laptop-lenovo-thinkpad-x1-carbon-gen-13-21ns0107vn-ultra-5-226v-16gb-512gb-2-8k-oled-120hz-win11pro	\N	21	5	55290000.00	\N	0	TGDD-363895	\N	12	0.00	0	0	t	2026-09-24 20:54:18.429032	2026-09-24 20:54:18.429032	\N
555	Laptop MSI Prestige 14 Flip AI+ D3MTG - 021VN (U9 386H, 32GB, 1TB, Full HD+ OLED, Cảm ứng, Win11)	laptop-msi-prestige-14-flip-ai-d3mtg-021vn-u9-386h-32gb-1tb-full-hd-oled-cam-ung-win11	\N	22	5	54990000.00	\N	0	TGDD-364868	\N	12	0.00	0	0	t	2026-09-24 20:54:18.445649	2026-09-24 20:54:18.445649	\N
556	Laptop HP EliteBook 6 G1a 14 - C0CF2PT (R5 AI 340, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	laptop-hp-elitebook-6-g1a-14-c0cf2pt-r5-ai-340-16gb-512gb-wuxga-cam-ung-win11	\N	17	5	31990000.00	\N	0	TGDD-365615	\N	12	0.00	0	0	t	2026-09-24 20:54:18.462377	2026-09-24 20:54:18.462377	\N
557	Laptop HP EliteBook 8 G1a 14 - C0CE3PT (R5 AI PRO 340, 32GB, 512GB, WUXGA, Cảm ứng, Win11 Pro)	laptop-hp-elitebook-8-g1a-14-c0ce3pt-r5-ai-pro-340-32gb-512gb-wuxga-cam-ung-win11-pro	\N	17	5	42590000.00	\N	0	TGDD-365619	\N	12	0.00	0	0	t	2026-09-24 20:54:18.478757	2026-09-24 20:54:18.478757	\N
558	Laptop HP EliteBook 8 G1a 14 - C0CE5PT (R7 AI PRO 350, 32GB, 512GB, WUXGA, Cảm ứng, Win11)	laptop-hp-elitebook-8-g1a-14-c0ce5pt-r7-ai-pro-350-32gb-512gb-wuxga-cam-ung-win11	\N	17	5	0.00	\N	0	TGDD-365621	\N	12	0.00	0	0	t	2026-09-24 20:54:18.495184	2026-09-24 20:54:18.495184	\N
559	Laptop Asus TUF Gaming FA401EA - RG034W (RYZEN AI MAX+ 392, 64GB, 1TB, WQXGA 165Hz, Win11)	laptop-asus-tuf-gaming-fa401ea-rg034w-ryzen-ai-max-392-64gb-1tb-wqxga-165hz-win11	\N	19	5	63590000.00	\N	0	TGDD-366066	\N	12	0.00	0	0	t	2026-09-24 20:54:18.511896	2026-09-24 20:54:18.511896	\N
560	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-50H7 - NH.U4SSV.001 (i5 14450HX, 32GB, 512GB, RTX 5050 8GB, Full HD+ 180Hz, Win11)	laptop-acer-gaming-predator-helios-neo-16-phn16-i31-50h7-nh-u4ssv-001-i5-14450hx-32gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	49990000.00	\N	0	TGDD-366091	\N	12	0.00	0	0	t	2026-09-24 20:54:18.527636	2026-09-24 20:54:18.527636	\N
561	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-72XE - NH.U4RSV.001 (i7 14650HX, 32GB, 512GB, RTX5060 8GB, 2K+ 180Hz, Win11)	laptop-acer-gaming-predator-helios-neo-16-phn16-i31-72xe-nh-u4rsv-001-i7-14650hx-32gb-512gb-rtx5060-8gb-2k-180hz-win11	\N	23	5	59990000.00	\N	0	TGDD-366092	\N	12	0.00	0	0	t	2026-09-24 20:54:18.543859	2026-09-24 20:54:18.543859	\N
562	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-74MN - NH.U4SSV.002 (i7 14650HX, 32GB, 512GB, RTX 5050 8GB, Full HD+ 180Hz, Win11)	laptop-acer-gaming-predator-helios-neo-16-phn16-i31-74mn-nh-u4ssv-002-i7-14650hx-32gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	54990000.00	\N	0	TGDD-366093	\N	12	0.00	0	0	t	2026-09-24 20:54:18.560057	2026-09-24 20:54:18.560057	\N
563	Laptop Lenovo Gaming Legion 5 15AHP11 - 83Q7001JVN (R7 250, 16GB, 512GB, RTX 5060 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	laptop-lenovo-gaming-legion-5-15ahp11-83q7001jvn-r7-250-16gb-512gb-rtx-5060-8gb-wqxga-oled-165hz-officeh24-win11	\N	21	5	54990000.00	\N	0	TGDD-366744	\N	12	0.00	0	0	t	2026-09-24 20:54:18.576512	2026-09-24 20:54:18.576512	\N
564	Laptop Lenovo Gaming LOQ 15IPH11 - 83SL000LVN (Ultra 7 356H, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, Win11)	laptop-lenovo-gaming-loq-15iph11-83sl000lvn-ultra-7-356h-16gb-512gb-rtx-5060-8gb-wuxga-165hz-win11	\N	21	5	49990000.00	\N	0	TGDD-367686	\N	12	0.00	0	0	t	2026-09-24 20:54:18.593229	2026-09-24 20:54:18.593229	\N
565	Laptop MSI Gaming Raider 16 MAX HX B2WI - 095VN (Ultra 9 290HX Plus, 64GB, 2TB, RTX 5080 16GB, QHD+ OLED 240Hz, Win11)	laptop-msi-gaming-raider-16-max-hx-b2wi-095vn-ultra-9-290hx-plus-64gb-2tb-rtx-5080-16gb-qhd-oled-240hz-win11	\N	22	5	129590000.00	\N	0	TGDD-367865	\N	12	0.00	0	0	t	2026-09-24 20:54:18.609792	2026-09-24 20:54:18.609792	\N
566	Laptop Asus TUF Gaming F16 FX608JHI - TU210W (i5 14450HX, 16GB, 512GB,RTX 5050 8GB, WUXGA 144Hz, Win11)	laptop-asus-tuf-gaming-f16-fx608jhi-tu210w-i5-14450hx-16gb-512gb-rtx-5050-8gb-wuxga-144hz-win11	\N	19	5	42590000.00	\N	0	TGDD-367887	\N	12	0.00	0	0	t	2026-09-24 20:54:18.626552	2026-09-24 20:54:18.626552	\N
567	Laptop Asus Zenbook S 16 UM5606GA - SS441W (R9 AI 465, 16GB, 512GB, 3K OLED 120Hz, Win11)	laptop-asus-zenbook-s-16-um5606ga-ss441w-r9-ai-465-16gb-512gb-3k-oled-120hz-win11	\N	19	5	45090000.00	\N	0	TGDD-367888	\N	12	0.00	0	0	t	2026-09-24 20:54:18.643277	2026-09-24 20:54:18.643277	\N
568	Laptop Asus Gaming ROG Zephyrus G14 GU405AW - SY029W (Ultra 9 386H, 32GB, 1TB, RTX 5080 16GB, 3K OLED 120Hz, Win11)	laptop-asus-gaming-rog-zephyrus-g14-gu405aw-sy029w-ultra-9-386h-32gb-1tb-rtx-5080-16gb-3k-oled-120hz-win11	\N	19	5	118590000.00	\N	0	TGDD-367889	\N	12	0.00	0	0	t	2026-09-24 20:54:18.660385	2026-09-24 20:54:18.660385	\N
569	Laptop Asus Zenbook A14 UX3407NA - QD132WS (X2E 88 100, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	laptop-asus-zenbook-a14-ux3407na-qd132ws-x2e-88-100-16gb-512gb-wuxga-oled-officeh24-365-win11	\N	19	5	39990000.00	\N	0	TGDD-367890	\N	12	0.00	0	0	t	2026-09-24 20:54:18.676448	2026-09-24 20:54:18.676448	\N
570	Laptop Asus Zenbook A14 UX3407NA - QD254W (X2E 88 100, 32GB, 1TB, WUXGA OLED, Win11)	laptop-asus-zenbook-a14-ux3407na-qd254w-x2e-88-100-32gb-1tb-wuxga-oled-win11	\N	19	5	54990000.00	\N	0	TGDD-367891	\N	12	0.00	0	0	t	2026-09-24 20:54:18.693831	2026-09-24 20:54:18.693831	\N
571	Laptop Asus Zenbook DUO UX8407AA - SN439WS (Ultra X7 358H, 32GB, 512GB, 3K OLED 144Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-asus-zenbook-duo-ux8407aa-sn439ws-ultra-x7-358h-32gb-512gb-3k-oled-144hz-cam-ung-officeh24-365-win11	\N	19	5	78990000.00	\N	0	TGDD-367897	\N	12	0.00	0	0	t	2026-09-24 20:54:18.712374	2026-09-24 20:54:18.712374	\N
572	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US003YVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-ideapad-slim-3-16iph11-83us003yvn-ultra-7-355-16gb-512gb-wuxga-win11	\N	21	5	28990000.00	\N	0	TGDD-368214	\N	12	0.00	0	0	t	2026-09-24 20:54:18.729359	2026-09-24 20:54:18.729359	\N
573	Laptop Acer Gaming Nitro ProPanel ANV16S-61-R0B8 - NH.QXQSV.001 (Ryzen AI 5 340, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv16s-61-r0b8-nh-qxqsv-001-ryzen-ai-5-340-16gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	42990000.00	\N	0	TGDD-368650	\N	12	0.00	0	0	t	2026-09-24 20:54:18.745582	2026-09-24 20:54:18.745582	\N
574	Laptop Acer Gaming Predator Helios Neo 16S PHN16S-I51-97T9 - NH.U3SSV.006 (Ultra 9 386H, 16GB, 512GB, RTX 5070 8GB, 2K+ 165Hz, Win11)	laptop-acer-gaming-predator-helios-neo-16s-phn16s-i51-97t9-nh-u3ssv-006-ultra-9-386h-16gb-512gb-rtx-5070-8gb-2k-165hz-win11	\N	23	5	75990000.00	\N	0	TGDD-368653	\N	12	0.00	0	0	t	2026-09-24 20:54:18.761843	2026-09-24 20:54:18.761843	\N
575	Laptop Acer Gaming Nitro ProPanel AN16S-61-R9CN - NH.QXGSV.001 (Ryzen AI 7 350, 16GB, 512GB, RTX 5070 8GB, 2K+ 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-an16s-61-r9cn-nh-qxgsv-001-ryzen-ai-7-350-16gb-512gb-rtx-5070-8gb-2k-180hz-win11	\N	23	5	57990000.00	\N	0	TGDD-368659	\N	12	0.00	0	0	t	2026-09-24 20:54:18.779402	2026-09-24 20:54:18.779402	\N
576	Laptop Asus Zenbook 14 UM3406GA - QD075W (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	laptop-asus-zenbook-14-um3406ga-qd075w-r7-ai-445-16gb-512gb-wuxga-oled-win11	\N	19	5	34590000.00	\N	0	TGDD-369460	\N	12	0.00	0	0	t	2026-09-24 20:54:18.796201	2026-09-24 20:54:18.796201	\N
577	Laptop Asus ExpertBook Ultra B9 B9406CAA - X7642TW (Ultra X7 358H, 64GB, 2TB, WQXGA+ OLED 120Hz, Win11)	laptop-asus-expertbook-ultra-b9-b9406caa-x7642tw-ultra-x7-358h-64gb-2tb-wqxga-oled-120hz-win11	\N	19	5	97990000.00	\N	0	TGDD-369462	\N	12	0.00	0	0	t	2026-09-24 20:54:18.81215	2026-09-24 20:54:18.81215	\N
578	Laptop Asus ExpertBook P5 P5406CCA - SF0047W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-expertbook-p5-p5406cca-sf0047w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	35990000.00	\N	0	TGDD-369465	\N	12	0.00	0	0	t	2026-09-24 20:54:18.828406	2026-09-24 20:54:18.828406	\N
579	Laptop Asus ExpertBook P5 P5406CCA - SF0241W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-expertbook-p5-p5406cca-sf0241w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	35990000.00	\N	0	TGDD-369466	\N	12	0.00	0	0	t	2026-09-24 20:54:18.84545	2026-09-24 20:54:18.84545	\N
580	Laptop Lenovo Gaming Legion 5 15AHP11 - 83Q7001HVN (R7 250, 16GB, 512GB, RTX 5050 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	laptop-lenovo-gaming-legion-5-15ahp11-83q7001hvn-r7-250-16gb-512gb-rtx-5050-8gb-wqxga-oled-165hz-officeh24-win11	\N	21	5	52990000.00	\N	0	TGDD-369476	\N	12	0.00	0	0	t	2026-09-24 20:54:18.861769	2026-09-24 20:54:18.861769	\N
581	Laptop Dell 16 DC16250 - 71100513 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeH24+365B, Win11)	laptop-dell-16-dc16250-71100513-core-5-120u-16gb-1tb-full-hd-officeh24-365b-win11	\N	20	5	0.00	\N	0	TGDD-369482	\N	12	0.00	0	0	t	2026-09-24 20:54:18.878743	2026-09-24 20:54:18.878743	\N
582	Laptop MSI Modern 14S G3MG-003VN (Core 5 320, 16GB, 1TB, Full HD+ 120Hz, Win11)	laptop-msi-modern-14s-g3mg-003vn-core-5-320-16gb-1tb-full-hd-120hz-win11	\N	22	5	27990000.00	\N	0	TGDD-369751	\N	12	0.00	0	0	t	2026-09-24 20:54:18.895063	2026-09-24 20:54:18.895063	\N
583	Laptop Asus ProArt P1 H7606WP - SR335W (R9 AI HX 370, 64GB, 2TB, RTX 5070 8GB, 3K 120Hz, Cảm ứng, Win11)	laptop-asus-proart-p1-h7606wp-sr335w-r9-ai-hx-370-64gb-2tb-rtx-5070-8gb-3k-120hz-cam-ung-win11	\N	19	5	114890000.00	\N	0	TGDD-369756	\N	12	0.00	0	0	t	2026-09-24 20:54:18.912437	2026-09-24 20:54:18.912437	\N
584	Laptop Acer Nitro ProPanel AN16S-61-R7ZJ - NH.QXFSV.002 (R7 AI 350, 16GB, 512GB, RTX 5060 8GB, 2K, Win11)	laptop-acer-nitro-propanel-an16s-61-r7zj-nh-qxfsv-002-r7-ai-350-16gb-512gb-rtx-5060-8gb-2k-win11	\N	23	5	49990000.00	\N	0	TGDD-369981	\N	12	0.00	0	0	t	2026-09-24 20:54:18.930264	2026-09-24 20:54:18.930264	\N
585	Laptop Acer Nitro ProPanel ANV16S-61-R7KQ - NH.QXPSV.001 (R5 AI 340, 16GB, 512GB, RTX 5060 8GB, Full HD 180Hz, Win11)	laptop-acer-nitro-propanel-anv16s-61-r7kq-nh-qxpsv-001-r5-ai-340-16gb-512gb-rtx-5060-8gb-full-hd-180hz-win11	\N	23	5	46990000.00	\N	0	TGDD-369982	\N	12	0.00	0	0	t	2026-09-24 20:54:18.946576	2026-09-24 20:54:18.946576	\N
586	Laptop Acer Swift Go AI SFG14-75-765M - NX.JNBSV.004 (Ultra 7 258V, 32GB, 512GB, Full HD+, Win11)	laptop-acer-swift-go-ai-sfg14-75-765m-nx-jnbsv-004-ultra-7-258v-32gb-512gb-full-hd-win11	\N	23	5	39990000.00	\N	0	TGDD-370278	\N	12	0.00	0	0	t	2026-09-24 20:54:18.962987	2026-09-24 20:54:18.962987	\N
587	Laptop Acer Gaming Nitro ProPanel ANV15-52-556L - NH.QZ9SV.006 (Core 5 210H, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	laptop-acer-gaming-nitro-propanel-anv15-52-556l-nh-qz9sv-006-core-5-210h-16gb-512gb-rtx-5050-8gb-full-hd-180hz-win11	\N	23	5	38990000.00	\N	0	TGDD-370279	\N	12	0.00	0	0	t	2026-09-24 20:54:18.979828	2026-09-24 20:54:18.979828	\N
588	Laptop Lenovo ThinkPad E14 - 21SX002YVN (Gen 7 Ultra 255H, 16GB, 512GB, WUXGA, Win11)	laptop-lenovo-thinkpad-e14-21sx002yvn-gen-7-ultra-255h-16gb-512gb-wuxga-win11	\N	21	5	39190000.00	\N	0	TGDD-370353	\N	12	0.00	0	0	t	2026-09-24 20:54:18.995968	2026-09-24 20:54:18.995968	\N
589	Laptop Lenovo Yoga Slim 7 14IPH11 - 83QM0075VN (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-14iph11-83qm0075vn-ultra-7-355-16gb-512gb-wuxga-officeh24-365-win11	\N	21	5	40990000.00	\N	0	TGDD-370354	\N	12	0.00	0	0	t	2026-09-24 20:54:19.012211	2026-09-24 20:54:19.012211	\N
590	Laptop Lenovo Yoga Slim 7 Ultra 14IPH11 - 83QK009PVN (Ultra X9 378H, 32GB, 1TB, WQXGA+ 120Hz, OfficeH24+365, Win11)	laptop-lenovo-yoga-slim-7-ultra-14iph11-83qk009pvn-ultra-x9-378h-32gb-1tb-wqxga-120hz-officeh24-365-win11	\N	21	5	68990000.00	\N	0	TGDD-370358	\N	12	0.00	0	0	t	2026-09-24 20:54:19.029216	2026-09-24 20:54:19.029216	\N
591	Laptop MSI Cyborg 15 Max C13WE - 422VN (i7 13620H, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	laptop-msi-cyborg-15-max-c13we-422vn-i7-13620h-16gb-512gb-rtx-5050-8gb-full-hd-144hz-win11	\N	22	5	41590000.00	\N	0	TGDD-370582	\N	12	0.00	0	0	t	2026-09-24 20:54:19.045251	2026-09-24 20:54:19.045251	\N
592	Laptop MSI Cyborg 15 Max C13WF - 421VN (i7 13620H, 16GB, 512GB, RTX 5060 8GB, Full HD 144Hz, Win11)	laptop-msi-cyborg-15-max-c13wf-421vn-i7-13620h-16gb-512gb-rtx-5060-8gb-full-hd-144hz-win11	\N	22	5	43590000.00	\N	0	TGDD-370583	\N	12	0.00	0	0	t	2026-09-24 20:54:19.062032	2026-09-24 20:54:19.062032	\N
593	Laptop Acer Swift Air 14 SFA14-I31-52BV - NX.W49SV.002 (Core 5 320, 12GB, 512GB, Full HD 120Hz, Win11)	laptop-acer-swift-air-14-sfa14-i31-52bv-nx-w49sv-002-core-5-320-12gb-512gb-full-hd-120hz-win11	\N	23	5	24990000.00	\N	0	TGDD-370864	\N	12	0.00	0	0	t	2026-09-24 20:54:19.079155	2026-09-24 20:54:19.079155	\N
594	Laptop Acer Swift Air 14 SFA14-I31-34NF - NX.W49SV.001 (Core 3 304, 12GB, 512GB, WUXGA 120Hz, Win11)	laptop-acer-swift-air-14-sfa14-i31-34nf-nx-w49sv-001-core-3-304-12gb-512gb-wuxga-120hz-win11	\N	23	5	21990000.00	\N	0	TGDD-370865	\N	12	0.00	0	0	t	2026-09-24 20:54:19.095271	2026-09-24 20:54:19.095271	\N
595	Laptop Asus ExpertBook P3 P3406CCAP - U716S8W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	laptop-asus-expertbook-p3-p3406ccap-u716s8w-ultra-7-255h-16gb-512gb-wuxga-win11	\N	19	5	32990000.00	\N	0	TGDD-370910	\N	12	0.00	0	0	t	2026-09-24 20:54:19.120329	2026-09-24 20:54:19.120329	\N
597	Laptop HP ProBook 4 G1a 14 - D9CP7AT (R7 250, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1a-14-d9cp7at-r7-250-16gb-512gb-wuxga-win11	\N	17	5	30590000.00	\N	0	TGDD-371124	\N	12	0.00	0	0	t	2026-09-24 20:54:19.154604	2026-09-24 20:54:19.154604	\N
598	Laptop HP Probook 4 G1ah 16 - D9CP8AT (R7 250, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g1ah-16-d9cp8at-r7-250-16gb-512gb-wuxga-win11	\N	17	5	30590000.00	\N	0	TGDD-371125	\N	12	0.00	0	0	t	2026-09-24 20:54:19.171746	2026-09-24 20:54:19.171746	\N
599	Laptop HP ProBook 4 G2i 16 - DK2Y5AT (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g2i-16-dk2y5at-ultra-7-355-16gb-512gb-wuxga-win11	\N	17	5	39190000.00	\N	0	TGDD-371127	\N	12	0.00	0	0	t	2026-09-24 20:54:19.18831	2026-09-24 20:54:19.18831	\N
600	Laptop HP OmniBook Ultra 14 kd0039TU - D72C7PA (Ultra 7 356H, 32GB, 1TB, 3K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-ultra-14-kd0039tu-d72c7pa-ultra-7-356h-32gb-1tb-3k-oled-120hz-cam-ung-officeh24-365-win11	\N	17	5	59190000.00	\N	0	TGDD-371128	\N	12	0.00	0	0	t	2026-09-24 20:54:19.204535	2026-09-24 20:54:19.204535	\N
601	Laptop HP OmniBook Ultra 14 kd0036TU - D72C6PA (Ultra 9 386H, 32GB, 1TB, 3K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-ultra-14-kd0036tu-d72c6pa-ultra-9-386h-32gb-1tb-3k-oled-120hz-cam-ung-officeh24-365-win11	\N	17	5	65190000.00	\N	0	TGDD-371129	\N	12	0.00	0	0	t	2026-09-24 20:54:19.221928	2026-09-24 20:54:19.221928	\N
602	Laptop HP OmniBook 5 14 hh0075TU - D72CTPA (Ultra 5 322, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-hp-omnibook-5-14-hh0075tu-d72ctpa-ultra-5-322-16gb-512gb-wuxga-officeh24-365-win11	\N	17	5	35990000.00	\N	0	TGDD-371130	\N	12	0.00	0	0	t	2026-09-24 20:54:19.238858	2026-09-24 20:54:19.238858	\N
603	Laptop HP OmniBook 5 14 hh0074TU - D72CSPA (Ultra 5 322, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-5-14-hh0074tu-d72cspa-ultra-5-322-16gb-512gb-wuxga-cam-ung-officeh24-365-win11	\N	17	5	36990000.00	\N	0	TGDD-371132	\N	12	0.00	0	0	t	2026-09-24 20:54:19.255098	2026-09-24 20:54:19.255098	\N
604	Laptop HP OmniBook 5 14 hh0073TU - D72CRPA (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	laptop-hp-omnibook-5-14-hh0073tu-d72crpa-ultra-7-355-16gb-512gb-wuxga-officeh24-365-win11	\N	17	5	38990000.00	\N	0	TGDD-371133	\N	12	0.00	0	0	t	2026-09-24 20:54:19.271659	2026-09-24 20:54:19.271659	\N
605	Laptop HP OmniBook 5 14 hh0072TU - D72CQPA (Ultra 7 355, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-5-14-hh0072tu-d72cqpa-ultra-7-355-16gb-512gb-wuxga-cam-ung-officeh24-365-win11	\N	17	5	39990000.00	\N	0	TGDD-371134	\N	12	0.00	0	0	t	2026-09-24 20:54:19.288274	2026-09-24 20:54:19.288274	\N
606	Laptop HP OmniBook X Flip 14 kb0048TU - D72BYPA (Ultra 7 355, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	laptop-hp-omnibook-x-flip-14-kb0048tu-d72bypa-ultra-7-355-32gb-512gb-wuxga-cam-ung-officeh24-365-win11	\N	17	5	49190000.00	\N	0	TGDD-371135	\N	12	0.00	0	0	t	2026-09-24 20:54:19.305674	2026-09-24 20:54:19.305674	\N
607	Laptop HP ProBook 4 G2i 16 - DK2X9AT (Ultra 5 325, 16GB, 512GB, WUXGA, Win11)	laptop-hp-probook-4-g2i-16-dk2x9at-ultra-5-325-16gb-512gb-wuxga-win11	\N	17	5	35190000.00	\N	0	TGDD-371137	\N	12	0.00	0	0	t	2026-09-24 20:54:19.321801	2026-09-24 20:54:19.321801	\N
608	Laptop Asus TUF Gaming A15 FA506NCG - HN390W (R7 155, 16GB, 512GB, RTX 3050 4GB, Full HD 144Hz, Win11)	laptop-asus-tuf-gaming-a15-fa506ncg-hn390w-r7-155-16gb-512gb-rtx-3050-4gb-full-hd-144hz-win11	\N	19	5	27490000.00	\N	0	TGDD-371296	\N	12	0.00	0	0	t	2026-09-24 20:54:19.338676	2026-09-24 20:54:19.338676	\N
609	Máy tính bảng iPad A16 WiFi 128GB	may-tinh-bang-ipad-a16-wifi-128gb	\N	26	6	12290000.00	\N	0	TGDD-335308	\N	12	0.00	0	0	t	2026-09-24 20:54:19.387846	2026-09-24 20:54:19.387846	\N
610	Máy tính bảng iPad Air M4 11 inch WiFi 128GB	may-tinh-bang-ipad-air-m4-11-inch-wifi-128gb	\N	26	6	20690000.00	\N	0	TGDD-363417	\N	12	0.00	0	0	t	2026-09-24 20:54:19.40449	2026-09-24 20:54:19.40449	\N
611	Máy tính bảng OPPO Pad 5 8GB/256GB	may-tinh-bang-oppo-pad-5-8gb-256gb	\N	8	6	13490000.00	\N	0	TGDD-361231	\N	12	0.00	0	0	t	2026-09-24 20:54:19.420853	2026-09-24 20:54:19.420853	\N
612	Máy tính bảng Xiaomi Pad 8 8GB/128GB	may-tinh-bang-xiaomi-pad-8-8gb-128gb	\N	3	6	9990000.00	\N	0	TGDD-362716	\N	12	0.00	0	0	t	2026-09-24 20:54:19.437366	2026-09-24 20:54:19.437366	\N
613	Máy tính bảng Samsung Galaxy Tab A11+ 5G 6GB/128GB	may-tinh-bang-samsung-galaxy-tab-a11-5g-6gb-128gb	\N	7	6	7840000.00	\N	0	TGDD-359089	\N	12	0.00	0	0	t	2026-09-24 20:54:19.45484	2026-09-24 20:54:19.45484	\N
614	Máy tính bảng Samsung Galaxy Tab A11+ WiFi 6GB/128GB	may-tinh-bang-samsung-galaxy-tab-a11-wifi-6gb-128gb	\N	7	6	6440000.00	\N	0	TGDD-359086	\N	12	0.00	0	0	t	2026-09-24 20:54:19.471493	2026-09-24 20:54:19.471493	\N
615	Máy tính bảng Xiaomi Pad 8 Pro 8GB/128GB	may-tinh-bang-xiaomi-pad-8-pro-8gb-128gb	\N	3	6	14390000.00	\N	0	TGDD-362718	\N	12	0.00	0	0	t	2026-09-24 20:54:19.488693	2026-09-24 20:54:19.488693	\N
616	Máy tính bảng OPPO Pad SE WiFi màn hình nhám 4GB/128GB	may-tinh-bang-oppo-pad-se-wifi-man-hinh-nham-4gb-128gb	\N	8	6	6090000.00	\N	0	TGDD-339287	\N	12	0.00	0	0	t	2026-09-24 20:54:19.506376	2026-09-24 20:54:19.506376	\N
617	Máy tính bảng iPad mini 7 WiFi 128GB	may-tinh-bang-ipad-mini-7-wifi-128gb	\N	26	6	16290000.00	\N	0	TGDD-331229	\N	12	0.00	0	0	t	2026-09-24 20:54:19.523121	2026-09-24 20:54:19.523121	\N
618	Máy tính bảng iPad Pro M5 11 inch WiFi 256GB	may-tinh-bang-ipad-pro-m5-11-inch-wifi-256gb	\N	26	6	34490000.00	\N	0	TGDD-358082	\N	12	0.00	0	0	t	2026-09-24 20:54:19.539921	2026-09-24 20:54:19.539921	\N
619	Máy tính bảng OPPO Pad Neo 4G 8GB/128GB	may-tinh-bang-oppo-pad-neo-4g-8gb-128gb	\N	8	6	6800000.00	\N	0	TGDD-320992	\N	12	0.00	0	0	t	2026-09-24 20:54:19.55635	2026-09-24 20:54:19.55635	\N
620	Máy tính bảng Samsung Galaxy Tab S10 Lite 5G 6GB/128GB	may-tinh-bang-samsung-galaxy-tab-s10-lite-5g-6gb-128gb	\N	7	6	9540000.00	\N	0	TGDD-343061	\N	12	0.00	0	0	t	2026-09-24 20:54:19.572095	2026-09-24 20:54:19.572095	\N
621	Máy tính bảng Samsung Galaxy Tab A11 4G 4GB/64GB	may-tinh-bang-samsung-galaxy-tab-a11-4g-4gb-64gb	\N	7	6	4390000.00	\N	0	TGDD-345548	\N	12	0.00	0	0	t	2026-09-24 20:54:19.588058	2026-09-24 20:54:19.588058	\N
622	Máy tính bảng HONOR Pad X7 WiFi 4GB/64GB	may-tinh-bang-honor-pad-x7-wifi-4gb-64gb	\N	5	6	3140000.00	\N	0	TGDD-345544	\N	12	0.00	0	0	t	2026-09-24 20:54:19.604271	2026-09-24 20:54:19.604271	\N
623	Máy tính bảng Samsung Galaxy Tab S11 5G 12GB/128GB	may-tinh-bang-samsung-galaxy-tab-s11-5g-12gb-128gb	\N	7	6	22710000.00	\N	0	TGDD-344721	\N	12	0.00	0	0	t	2026-09-24 20:54:19.620695	2026-09-24 20:54:19.620695	\N
624	Máy tính bảng Lenovo Idea Tab 5G 8GB/128GB	may-tinh-bang-lenovo-idea-tab-5g-8gb-128gb	\N	21	6	7540000.00	\N	0	TGDD-342548	\N	12	0.00	0	0	t	2026-09-24 20:54:19.638115	2026-09-24 20:54:19.638115	\N
625	Máy tính bảng Xiaomi Redmi Pad 2 WiFi 4GB/128GB	may-tinh-bang-xiaomi-redmi-pad-2-wifi-4gb-128gb	\N	3	6	5980000.00	\N	0	TGDD-339204	\N	12	0.00	0	0	t	2026-09-24 20:54:19.655264	2026-09-24 20:54:19.655264	\N
626	Máy tính bảng iPad A16 5G 128GB	may-tinh-bang-ipad-a16-5g-128gb	\N	26	6	16490000.00	\N	0	TGDD-335311	\N	12	0.00	0	0	t	2026-09-24 20:54:19.671475	2026-09-24 20:54:19.671475	\N
627	Máy tính bảng Xiaomi Redmi Pad 2 Pro WiFi 6GB/128GB	may-tinh-bang-xiaomi-redmi-pad-2-pro-wifi-6gb-128gb	\N	3	6	6590000.00	\N	0	TGDD-356854	\N	12	0.00	0	0	t	2026-09-24 20:54:19.687926	2026-09-24 20:54:19.687926	\N
628	Máy tính bảng OPPO Pad 3 8GB/256GB	may-tinh-bang-oppo-pad-3-8gb-256gb	\N	8	6	10330000.00	\N	0	TGDD-333917	\N	12	0.00	0	0	t	2026-09-24 20:54:19.704189	2026-09-24 20:54:19.704189	\N
632	Máy tính bảng Xiaomi Redmi Pad 2 4G 4GB/128GB	may-tinh-bang-xiaomi-redmi-pad-2-4g-4gb-128gb	\N	3	6	5490000.00	\N	0	TGDD-339207	\N	12	0.00	0	0	t	2026-09-24 20:54:19.773675	2026-09-24 20:54:19.773675	\N
633	Máy tính bảng Xiaomi Redmi Pad 2 Pro 5G 6GB/128GB	may-tinh-bang-xiaomi-redmi-pad-2-pro-5g-6gb-128gb	\N	3	6	7890000.00	\N	0	TGDD-356864	\N	12	0.00	0	0	t	2026-09-24 20:54:19.790768	2026-09-24 20:54:19.790768	\N
634	Máy tính bảng OPPO Pad SE 4G 4GB/128GB	may-tinh-bang-oppo-pad-se-4g-4gb-128gb	\N	8	6	7140000.00	\N	0	TGDD-339286	\N	12	0.00	0	0	t	2026-09-24 20:54:19.807213	2026-09-24 20:54:19.807213	\N
635	Máy tính bảng Samsung Galaxy Tab A11 WiFi 4GB/64GB	may-tinh-bang-samsung-galaxy-tab-a11-wifi-4gb-64gb	\N	7	6	3390000.00	\N	0	TGDD-345546	\N	12	0.00	0	0	t	2026-09-24 20:54:19.823502	2026-09-24 20:54:19.823502	\N
636	Máy tính bảng Samsung Galaxy Tab S10 Lite Wifi 8GB/256GB	may-tinh-bang-samsung-galaxy-tab-s10-lite-wifi-8gb-256gb	\N	7	6	10240000.00	\N	0	TGDD-365596	\N	12	0.00	0	0	t	2026-09-24 20:54:19.839534	2026-09-24 20:54:19.839534	\N
637	Máy tính bảng Xiaomi Redmi Pad 2 Wifi 9.7 4GB/64GB	may-tinh-bang-xiaomi-redmi-pad-2-wifi-9-7-4gb-64gb	\N	3	6	4190000.00	\N	0	TGDD-366579	\N	12	0.00	0	0	t	2026-09-24 20:54:19.859701	2026-09-24 20:54:19.859701	\N
638	Máy tính bảng Xiaomi Redmi Pad 2 4G 9.7 4GB/64GB	may-tinh-bang-xiaomi-redmi-pad-2-4g-9-7-4gb-64gb	\N	3	6	4690000.00	\N	0	TGDD-366598	\N	12	0.00	0	0	t	2026-09-24 20:54:19.87727	2026-09-24 20:54:19.87727	\N
639	Máy tính bảng Samsung Galaxy Tab S11 WiFi 12GB/128GB	may-tinh-bang-samsung-galaxy-tab-s11-wifi-12gb-128gb	\N	7	6	19240000.00	\N	0	TGDD-344723	\N	12	0.00	0	0	t	2026-09-24 20:54:19.895092	2026-09-24 20:54:19.895092	\N
640	Máy tính bảng Samsung Galaxy Tab S11 Ultra 5G 12GB/256GB	may-tinh-bang-samsung-galaxy-tab-s11-ultra-5g-12gb-256gb	\N	7	6	30690000.00	\N	0	TGDD-344725	\N	12	0.00	0	0	t	2026-09-24 20:54:19.911581	2026-09-24 20:54:19.911581	\N
641	Máy tính bảng iPad Air M4 13 inch WiFi 128GB	may-tinh-bang-ipad-air-m4-13-inch-wifi-128gb	\N	26	6	26190000.00	\N	0	TGDD-363422	\N	12	0.00	0	0	t	2026-09-24 20:54:19.927439	2026-09-24 20:54:19.927439	\N
642	Máy đọc sách Boox Savi 6S 6 inch 32GB	may-doc-sach-boox-savi-6s-6-inch-32gb	\N	27	6	3240000.00	\N	0	TGDD-363821	\N	12	0.00	0	0	t	2026-09-24 20:54:19.958856	2026-09-24 20:54:19.958856	\N
643	Máy tính bảng Lenovo Idea Tab Plus Matte Edition WiFi 8GB/256GB	may-tinh-bang-lenovo-idea-tab-plus-matte-edition-wifi-8gb-256gb	\N	21	6	10040000.00	\N	0	TGDD-368575	\N	12	0.00	0	0	t	2026-09-24 20:54:19.975372	2026-09-24 20:54:19.975372	\N
644	Máy tính bảng Lenovo Idea Tab 4GB/128GB	may-tinh-bang-lenovo-idea-tab-4gb-128gb	\N	21	6	5740000.00	\N	0	TGDD-368516	\N	12	0.00	0	0	t	2026-09-24 20:54:19.992559	2026-09-24 20:54:19.992559	\N
645	Máy tính bảng iPad Pro M5 13 inch WiFi 256GB	may-tinh-bang-ipad-pro-m5-13-inch-wifi-256gb	\N	26	6	43790000.00	\N	0	TGDD-358099	\N	12	0.00	0	0	t	2026-09-24 20:54:20.009317	2026-09-24 20:54:20.009317	\N
646	Máy đọc sách New Kindle 2024 6 inch 16 GB	may-doc-sach-new-kindle-2024-6-inch-16-gb	\N	28	6	3940000.00	\N	0	TGDD-363825	\N	12	0.00	0	0	t	2026-09-24 20:54:20.04217	2026-09-24 20:54:20.04217	\N
647	Máy tính bảng iPad Air M4 11 inch 5G 128GB	may-tinh-bang-ipad-air-m4-11-inch-5g-128gb	\N	26	6	24890000.00	\N	0	TGDD-363427	\N	12	0.00	0	0	t	2026-09-24 20:54:20.059912	2026-09-24 20:54:20.059912	\N
648	Máy tính bảng iPad Air M4 13 inch 5G 128GB	may-tinh-bang-ipad-air-m4-13-inch-5g-128gb	\N	26	6	30390000.00	\N	0	TGDD-363432	\N	12	0.00	0	0	t	2026-09-24 20:54:20.078163	2026-09-24 20:54:20.078163	\N
649	Máy tính bảng iPad Pro M5 11 inch 5G 256GB	may-tinh-bang-ipad-pro-m5-11-inch-5g-256gb	\N	26	6	39990000.00	\N	0	TGDD-358105	\N	12	0.00	0	0	t	2026-09-24 20:54:20.09682	2026-09-24 20:54:20.09682	\N
650	Máy tính bảng iPad Pro M5 13 inch 5G 256GB	may-tinh-bang-ipad-pro-m5-13-inch-5g-256gb	\N	26	6	49590000.00	\N	0	TGDD-358111	\N	12	0.00	0	0	t	2026-09-24 20:54:20.115134	2026-09-24 20:54:20.115134	\N
651	Máy tính bảng Lenovo Legion Tab Gen 3 WiFi 12GB/256GB	may-tinh-bang-lenovo-legion-tab-gen-3-wifi-12gb-256gb	\N	21	6	14140000.00	\N	0	TGDD-368580	\N	12	0.00	0	0	t	2026-09-24 20:54:20.132596	2026-09-24 20:54:20.132596	\N
652	Máy tính bảng Lenovo Idea Tab Pro Gen 2 Matte Edition WiFi 12GB/256GB	may-tinh-bang-lenovo-idea-tab-pro-gen-2-matte-edition-wifi-12gb-256gb	\N	21	6	16440000.00	\N	0	TGDD-368577	\N	12	0.00	0	0	t	2026-09-24 20:54:20.149154	2026-09-24 20:54:20.149154	\N
653	Combo máy đọc sách Boox Savi 6 6 inch 32 GB	combo-may-doc-sach-boox-savi-6-6-inch-32-gb	\N	27	6	4840000.00	\N	0	TGDD-363822	\N	12	0.00	0	0	t	2026-09-24 20:54:20.165676	2026-09-24 20:54:20.165676	\N
654	Máy tính bảng Lenovo Legion Tab Gen 5 WiFi 12GB/256GB	may-tinh-bang-lenovo-legion-tab-gen-5-wifi-12gb-256gb	\N	21	6	21340000.00	\N	0	TGDD-368581	\N	12	0.00	0	0	t	2026-09-24 20:54:20.18232	2026-09-24 20:54:20.18232	\N
655	Máy tính bảng HONOR Pad X9a 8GB/256GB	may-tinh-bang-honor-pad-x9a-8gb-256gb	\N	5	6	7890000.00	\N	0	TGDD-339828	\N	12	0.00	0	0	t	2026-09-24 20:54:20.198511	2026-09-24 20:54:20.198511	\N
656	Máy đọc sách Boox Palma2 6.13 inch 128 GB	may-doc-sach-boox-palma2-6-13-inch-128-gb	\N	27	6	7640000.00	\N	0	TGDD-363824	\N	12	0.00	0	0	t	2026-09-24 20:54:20.214594	2026-09-24 20:54:20.214594	\N
657	Máy tính bảng Lenovo Yoga Tab WiFi 8GB/256GB	may-tinh-bang-lenovo-yoga-tab-wifi-8gb-256gb	\N	21	6	17980000.00	\N	0	TGDD-368582	\N	12	0.00	0	0	t	2026-09-24 20:54:20.231612	2026-09-24 20:54:20.231612	\N
658	Máy tính bảng Honor Pad X8b 6GB/128GB	may-tinh-bang-honor-pad-x8b-6gb-128gb	\N	5	6	6640000.00	\N	0	TGDD-367014	\N	12	0.00	0	0	t	2026-09-24 20:54:20.247857	2026-09-24 20:54:20.247857	\N
659	Máy tính bảng Lenovo Idea Tab WiFi 8GB/256GB	may-tinh-bang-lenovo-idea-tab-wifi-8gb-256gb	\N	21	6	8440000.00	\N	0	TGDD-368579	\N	12	0.00	0	0	t	2026-09-24 20:54:20.265043	2026-09-24 20:54:20.265043	\N
660	Đồng hồ định vị trẻ em Kidcare Sight S25	dong-ho-dinh-vi-tre-em-kidcare-sight-s25	\N	29	7	1590000.00	\N	0	TGDD-358003	\N	12	0.00	0	0	t	2026-09-24 20:54:20.311001	2026-09-24 20:54:20.311001	\N
661	Huawei Watch Fit 5 42.9mm dây nylon	huawei-watch-fit-5-42-9mm-day-nylon	\N	30	7	3290000.00	\N	0	TGDD-366954	\N	12	0.00	0	0	t	2026-09-24 20:54:20.344549	2026-09-24 20:54:20.344549	\N
662	Đồng hồ định vị trẻ em Kidcare K25 4G	dong-ho-dinh-vi-tre-em-kidcare-k25-4g	\N	29	7	1790000.00	\N	0	TGDD-354255	\N	12	0.00	0	0	t	2026-09-24 20:54:20.36177	2026-09-24 20:54:20.36177	\N
663	Huawei Watch Fit 5 Pro 44.5mm dây nylon	huawei-watch-fit-5-pro-44-5mm-day-nylon	\N	30	7	4990000.00	\N	0	TGDD-366955	\N	12	0.00	0	0	t	2026-09-24 20:54:20.378471	2026-09-24 20:54:20.378471	\N
664	Huawei Watch GT Runner 2 43.5mm dây Nylon	huawei-watch-gt-runner-2-43-5mm-day-nylon	\N	30	7	5590000.00	\N	0	TGDD-364509	\N	12	0.00	0	0	t	2026-09-24 20:54:20.395458	2026-09-24 20:54:20.395458	\N
665	Amazfit Bip Max 49.5mm dây silicone	amazfit-bip-max-49-5mm-day-silicone	\N	31	7	2390000.00	\N	0	TGDD-367947	\N	12	0.00	0	0	t	2026-09-24 20:54:20.426086	2026-09-24 20:54:20.426086	\N
666	Apple Watch SE 3 GPS 40mm viền nhôm dây thể thao	apple-watch-se-3-gps-40mm-vien-nhom-day-the-thao	\N	32	7	6790000.00	\N	0	TGDD-344767	\N	12	0.00	0	0	t	2026-09-24 20:54:20.456912	2026-09-24 20:54:20.456912	\N
667	Vòng đeo tay thông minh Huawei Band 11 viền nhôm dây Fluor	vong-deo-tay-thong-minh-huawei-band-11-vien-nhom-day-fluor	\N	30	7	990000.00	\N	0	TGDD-362941	\N	12	0.00	0	0	t	2026-09-24 20:54:20.474304	2026-09-24 20:54:20.474304	\N
668	Huawei Watch Fit 4 43mm dây silicone	huawei-watch-fit-4-43mm-day-silicone	\N	30	7	2540000.00	\N	0	TGDD-334436	\N	12	0.00	0	0	t	2026-09-24 20:54:20.490591	2026-09-24 20:54:20.490591	\N
669	Apple Watch Series 11 GPS 42mm viền nhôm dây thể thao	apple-watch-series-11-gps-42mm-vien-nhom-day-the-thao	\N	32	7	11490000.00	\N	0	TGDD-344750	\N	12	0.00	0	0	t	2026-09-24 20:54:20.508438	2026-09-24 20:54:20.508438	\N
670	Vòng đeo tay thông minh Mi Band 10 Pro viền gốm	vong-deo-tay-thong-minh-mi-band-10-pro-vien-gom	\N	3	7	2490000.00	\N	0	TGDD-367410	\N	12	0.00	0	0	t	2026-09-24 20:54:20.526138	2026-09-24 20:54:20.526138	\N
671	Amazfit Active 3 Premium 46mm dây silicone	amazfit-active-3-premium-46mm-day-silicone	\N	31	7	3290000.00	\N	0	TGDD-364290	\N	12	0.00	0	0	t	2026-09-24 20:54:20.54304	2026-09-24 20:54:20.54304	\N
672	Vòng đeo tay thông minh Huawei Band 11 dây Fluor	vong-deo-tay-thong-minh-huawei-band-11-day-fluor	\N	30	7	890000.00	\N	0	TGDD-362942	\N	12	0.00	0	0	t	2026-09-24 20:54:20.559977	2026-09-24 20:54:20.559977	\N
673	Samsung Galaxy Watch Ultra 2 LTE 47mm dây silicone	samsung-galaxy-watch-ultra-2-lte-47mm-day-silicone	\N	7	7	17490000.00	\N	0	TGDD-369324	\N	12	0.00	0	0	t	2026-09-24 20:54:20.575856	2026-09-24 20:54:20.575856	\N
674	Amazfit Active Max 46mm dây silicone	amazfit-active-max-46mm-day-silicone	\N	31	7	3490000.00	\N	0	TGDD-361814	\N	12	0.00	0	0	t	2026-09-24 20:54:20.593153	2026-09-24 20:54:20.593153	\N
675	Vòng đeo tay thông minh Huawei Band 11 Pro dây nylon dệt	vong-deo-tay-thong-minh-huawei-band-11-pro-day-nylon-det	\N	30	7	1590000.00	\N	0	TGDD-362940	\N	12	0.00	0	0	t	2026-09-24 20:54:20.609567	2026-09-24 20:54:20.609567	\N
676	Apple Watch SE 3 GPS + Cellular 40mm viền nhôm dây thể thao	apple-watch-se-3-gps-cellular-40mm-vien-nhom-day-the-thao	\N	32	7	8390000.00	\N	0	TGDD-344769	\N	12	0.00	0	0	t	2026-09-24 20:54:20.625545	2026-09-24 20:54:20.625545	\N
677	Samsung Galaxy Watch9 Bluetooth 40mm dây silicone	samsung-galaxy-watch9-bluetooth-40mm-day-silicone	\N	7	7	9490000.00	\N	0	TGDD-369499	\N	12	0.00	0	0	t	2026-09-24 20:54:20.642605	2026-09-24 20:54:20.642605	\N
678	Huawei Watch 5 46mm viền Titanium dây composite	huawei-watch-5-46mm-vien-titanium-day-composite	\N	30	7	6990000.00	\N	0	TGDD-339492	\N	12	0.00	0	0	t	2026-09-24 20:54:20.6593	2026-09-24 20:54:20.6593	\N
679	Apple Watch Series 11 GPS + Cellular 42mm viền Titanium dây thể thao	apple-watch-series-11-gps-cellular-42mm-vien-titanium-day-the-thao	\N	32	7	20490000.00	\N	0	TGDD-344758	\N	12	0.00	0	0	t	2026-09-24 20:54:20.676036	2026-09-24 20:54:20.676036	\N
680	Huawei Watch 5 46mm viền thép dây cao su	huawei-watch-5-46mm-vien-thep-day-cao-su	\N	30	7	6990000.00	\N	0	TGDD-337872	\N	12	0.00	0	0	t	2026-09-24 20:54:20.692609	2026-09-24 20:54:20.692609	\N
681	Samsung Galaxy Watch9 LTE 40mm dây silicone	samsung-galaxy-watch9-lte-40mm-day-silicone	\N	7	7	10990000.00	\N	0	TGDD-369498	\N	12	0.00	0	0	t	2026-09-24 20:54:20.709602	2026-09-24 20:54:20.709602	\N
682	Samsung Galaxy Watch9 Bluetooth 44mm dây silicone	samsung-galaxy-watch9-bluetooth-44mm-day-silicone	\N	7	7	10490000.00	\N	0	TGDD-369497	\N	12	0.00	0	0	t	2026-09-24 20:54:20.72698	2026-09-24 20:54:20.72698	\N
683	Samsung Galaxy Watch9 LTE 44mm dây silicone	samsung-galaxy-watch9-lte-44mm-day-silicone	\N	7	7	11290000.00	\N	0	TGDD-369321	\N	12	0.00	0	0	t	2026-09-24 20:54:20.743281	2026-09-24 20:54:20.743281	\N
684	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Ocean	apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-ocean	\N	32	7	23490000.00	\N	0	TGDD-344764	\N	12	0.00	0	0	t	2026-09-24 20:54:20.760168	2026-09-24 20:54:20.760168	\N
685	Huawei Watch 5 42mm viền thép dây composite Vàng Hồng	huawei-watch-5-42mm-vien-thep-day-composite-vang-hong	\N	30	7	8990000.00	\N	0	TGDD-336948	\N	12	0.00	0	0	t	2026-09-24 20:54:20.777898	2026-09-24 20:54:20.777898	\N
686	Amazfit T-Rex Ultra 2 51mm dây silicone	amazfit-t-rex-ultra-2-51mm-day-silicone	\N	31	7	12990000.00	\N	0	TGDD-364292	\N	12	0.00	0	0	t	2026-09-24 20:54:20.795329	2026-09-24 20:54:20.795329	\N
687	Vòng đeo tay thông minh Mi Band 10 viền gốm	vong-deo-tay-thong-minh-mi-band-10-vien-gom	\N	3	7	1660000.00	\N	0	TGDD-339531	\N	12	0.00	0	0	t	2026-09-24 20:54:20.811559	2026-09-24 20:54:20.811559	\N
688	Huawei Watch GT 6 Pro 46mm viền Titanium dây cao su	huawei-watch-gt-6-pro-46mm-vien-titanium-day-cao-su	\N	30	7	6590000.00	\N	0	TGDD-341453	\N	12	0.00	0	0	t	2026-09-24 20:54:20.828733	2026-09-24 20:54:20.828733	\N
689	OPPO Watch S 46mm dây cao su	oppo-watch-s-46mm-day-cao-su	\N	8	7	3990000.00	\N	0	TGDD-361330	\N	12	0.00	0	0	t	2026-09-24 20:54:20.845822	2026-09-24 20:54:20.845822	\N
690	Amazfit Bip 6 46.3mm dây silicone	amazfit-bip-6-46-3mm-day-silicone	\N	31	7	1890000.00	\N	0	TGDD-337643	\N	12	0.00	0	0	t	2026-09-24 20:54:20.86232	2026-09-24 20:54:20.86232	\N
691	Huawei Watch GT 6 41mm viền thép dây da phối	huawei-watch-gt-6-41mm-vien-thep-day-da-phoi	\N	30	7	4790000.00	\N	0	TGDD-362002	\N	12	0.00	0	0	t	2026-09-24 20:54:20.87831	2026-09-24 20:54:20.87831	\N
692	Samsung Galaxy Watch7 44mm dây silicone	samsung-galaxy-watch7-44mm-day-silicone	\N	7	7	4840000.00	\N	0	TGDD-327697	\N	12	0.00	0	0	t	2026-09-24 20:54:20.895036	2026-09-24 20:54:20.895036	\N
693	Huawei Watch GT 6 Pro 46mm viền Titanium dây Woven	huawei-watch-gt-6-pro-46mm-vien-titanium-day-woven	\N	30	7	6990000.00	\N	0	TGDD-354257	\N	12	0.00	0	0	t	2026-09-24 20:54:20.911242	2026-09-24 20:54:20.911242	\N
694	Huawei Watch GT 6 46mm viền thép dây Woven	huawei-watch-gt-6-46mm-vien-thep-day-woven	\N	30	7	4790000.00	\N	0	TGDD-354260	\N	12	0.00	0	0	t	2026-09-24 20:54:20.927829	2026-09-24 20:54:20.927829	\N
695	OPPO Watch S 46mm dây nylon	oppo-watch-s-46mm-day-nylon	\N	8	7	3990000.00	\N	0	TGDD-361467	\N	12	0.00	0	0	t	2026-09-24 20:54:20.945258	2026-09-24 20:54:20.945258	\N
696	Samsung Galaxy Watch8 Classic 46mm dây da	samsung-galaxy-watch8-classic-46mm-day-da	\N	7	7	8790000.00	\N	0	TGDD-338266	\N	12	0.00	0	0	t	2026-09-24 20:54:20.961646	2026-09-24 20:54:20.961646	\N
697	Huawei Watch GT 6 Pro 46mm viền Titanium dây Titanium	huawei-watch-gt-6-pro-46mm-vien-titanium-day-titanium	\N	30	7	10490000.00	\N	0	TGDD-354258	\N	12	0.00	0	0	t	2026-09-24 20:54:20.978051	2026-09-24 20:54:20.978051	\N
698	Huawei Watch GT 6 41mm viền thép dây da	huawei-watch-gt-6-41mm-vien-thep-day-da	\N	30	7	4790000.00	\N	0	TGDD-354261	\N	12	0.00	0	0	t	2026-09-24 20:54:20.994356	2026-09-24 20:54:20.994356	\N
699	Huawei Watch GT 6 46mm viền thép dây da	huawei-watch-gt-6-46mm-vien-thep-day-da	\N	30	7	4790000.00	\N	0	TGDD-354259	\N	12	0.00	0	0	t	2026-09-24 20:54:21.010693	2026-09-24 20:54:21.010693	\N
700	Samsung Galaxy Watch8 LTE 40mm dây silicone	samsung-galaxy-watch8-lte-40mm-day-silicone	\N	7	7	6890000.00	\N	0	TGDD-340066	\N	12	0.00	0	0	t	2026-09-24 20:54:21.028353	2026-09-24 20:54:21.028353	\N
701	Huawei Watch GT 6 Pro Honma 46mm viền Titanium dây Fluor	huawei-watch-gt-6-pro-honma-46mm-vien-titanium-day-fluor	\N	30	7	8990000.00	\N	0	TGDD-361516	\N	12	0.00	0	0	t	2026-09-24 20:54:21.04525	2026-09-24 20:54:21.04525	\N
702	Amazfit T-Rex 3 Pro 48mm dây silicone	amazfit-t-rex-3-pro-48mm-day-silicone	\N	31	7	8990000.00	\N	0	TGDD-358004	\N	12	0.00	0	0	t	2026-09-24 20:54:21.062577	2026-09-24 20:54:21.062577	\N
703	Huawei Watch GT 6 41mm viền thép dây Milanese	huawei-watch-gt-6-41mm-vien-thep-day-milanese	\N	30	7	6290000.00	\N	0	TGDD-361518	\N	12	0.00	0	0	t	2026-09-24 20:54:21.079588	2026-09-24 20:54:21.079588	\N
704	Apple Watch Series 11 GPS + Cellular 46mm viền nhôm dây thể thao	apple-watch-series-11-gps-cellular-46mm-vien-nhom-day-the-thao	\N	32	7	14990000.00	\N	0	TGDD-344753	\N	12	0.00	0	0	t	2026-09-24 20:54:21.096592	2026-09-24 20:54:21.096592	\N
705	Đồng hồ định vị trẻ em imoo Z7 Spider Man	dong-ho-dinh-vi-tre-em-imoo-z7-spider-man	\N	33	7	5290000.00	\N	0	TGDD-333919	\N	12	0.00	0	0	t	2026-09-24 20:54:21.128472	2026-09-24 20:54:21.128472	\N
706	Samsung Galaxy Watch8 LTE 44mm dây silicone	samsung-galaxy-watch8-lte-44mm-day-silicone	\N	7	7	10490000.00	\N	0	TGDD-340067	\N	12	0.00	0	0	t	2026-09-24 20:54:21.144752	2026-09-24 20:54:21.144752	\N
707	Samsung Galaxy Watch8 44mm dây silicone	samsung-galaxy-watch8-44mm-day-silicone	\N	7	7	6890000.00	\N	0	TGDD-340068	\N	12	0.00	0	0	t	2026-09-24 20:54:21.161079	2026-09-24 20:54:21.161079	\N
708	Amazfit Active 2 Square Sapphire 43.3mm dây da	amazfit-active-2-square-sapphire-43-3mm-day-da	\N	31	7	2990000.00	\N	0	TGDD-340844	\N	12	0.00	0	0	t	2026-09-24 20:54:21.177702	2026-09-24 20:54:21.177702	\N
709	Samsung Galaxy Watch Ultra LTE 47mm dây silicone	samsung-galaxy-watch-ultra-lte-47mm-day-silicone	\N	7	7	12100000.00	\N	0	TGDD-327693	\N	12	0.00	0	0	t	2026-09-24 20:54:21.196147	2026-09-24 20:54:21.196147	\N
710	Amazfit Balance 2 47.4mm dây silicone	amazfit-balance-2-47-4mm-day-silicone	\N	31	7	6990000.00	\N	0	TGDD-341454	\N	12	0.00	0	0	t	2026-09-24 20:54:21.213289	2026-09-24 20:54:21.213289	\N
711	Apple Watch Series 11 GPS + Cellular 42mm viền Titanium dây Milan	apple-watch-series-11-gps-cellular-42mm-vien-titanium-day-milan	\N	32	7	21790000.00	\N	0	TGDD-344754	\N	12	0.00	0	0	t	2026-09-24 20:54:21.229921	2026-09-24 20:54:21.229921	\N
712	Huawei Watch Ultimate 2 47.8mm dây cao su	huawei-watch-ultimate-2-47-8mm-day-cao-su	\N	30	7	19290000.00	\N	0	TGDD-358006	\N	12	0.00	0	0	t	2026-09-24 20:54:21.246855	2026-09-24 20:54:21.246855	\N
713	Huawei Watch Ultimate 2 48.5mm dây cao su	huawei-watch-ultimate-2-48-5mm-day-cao-su	\N	30	7	15290000.00	\N	0	TGDD-358005	\N	12	0.00	0	0	t	2026-09-24 20:54:21.26336	2026-09-24 20:54:21.26336	\N
714	Nhẫn thông minh Samsung Galaxy Ring Size 9	nhan-thong-minh-samsung-galaxy-ring-size-9	\N	7	7	9810000.00	\N	0	TGDD-337834	\N	12	0.00	0	0	t	2026-09-24 20:54:21.279573	2026-09-24 20:54:21.279573	\N
715	Garmin Fenix 9 Pro Solar Sapphire 47mm viền Titanium dây silicone	garmin-fenix-9-pro-solar-sapphire-47mm-vien-titanium-day-silicone	\N	34	7	28990000.00	\N	0	TGDD-370871	\N	12	0.00	0	0	t	2026-09-24 20:54:21.312613	2026-09-24 20:54:21.312613	\N
716	Garmin Fenix 9 Pro Sapphire 47mm viền Titanium Carbon dây silicone	garmin-fenix-9-pro-sapphire-47mm-vien-titanium-carbon-day-silicone	\N	34	7	30290000.00	\N	0	TGDD-370934	\N	12	0.00	0	0	t	2026-09-24 20:54:21.330213	2026-09-24 20:54:21.330213	\N
717	Garmin Fenix 9 Sapphire 43mm viền Titanium dây silicone	garmin-fenix-9-sapphire-43mm-vien-titanium-day-silicone	\N	34	7	26290000.00	\N	0	TGDD-370869	\N	12	0.00	0	0	t	2026-09-24 20:54:21.346832	2026-09-24 20:54:21.346832	\N
718	Garmin Fenix 9 Sapphire 47mm viền Titanium Carbon dây silicone	garmin-fenix-9-sapphire-47mm-vien-titanium-carbon-day-silicone	\N	34	7	27690000.00	\N	0	TGDD-370930	\N	12	0.00	0	0	t	2026-09-24 20:54:21.363379	2026-09-24 20:54:21.363379	\N
719	Garmin Fenix 9 Pro Sapphire 43mm viền Titanium dây silicone	garmin-fenix-9-pro-sapphire-43mm-vien-titanium-day-silicone	\N	34	7	30290000.00	\N	0	TGDD-370870	\N	12	0.00	0	0	t	2026-09-24 20:54:21.379249	2026-09-24 20:54:21.379249	\N
720	Đồng hồ định vị trẻ em Huawei Watch Kids 4 Pro	dong-ho-dinh-vi-tre-em-huawei-watch-kids-4-pro	\N	30	7	2740000.00	\N	0	TGDD-328694	\N	12	0.00	0	0	t	2026-09-24 20:54:21.395702	2026-09-24 20:54:21.395702	\N
721	Garmin Forerunner 165 43mm dây silicone	garmin-forerunner-165-43mm-day-silicone	\N	34	7	3990000.00	\N	0	TGDD-322848	\N	12	0.00	0	0	t	2026-09-24 20:54:21.412639	2026-09-24 20:54:21.412639	\N
722	Xiaomi Watch S5 46mm dây cao su Fluoro	xiaomi-watch-s5-46mm-day-cao-su-fluoro	\N	3	7	4690000.00	\N	0	TGDD-367407	\N	12	0.00	0	0	t	2026-09-24 20:54:21.428747	2026-09-24 20:54:21.428747	\N
723	Vòng đeo tay thông minh Mi Band 9 Active	vong-deo-tay-thong-minh-mi-band-9-active	\N	3	7	630000.00	\N	0	TGDD-332406	\N	12	0.00	0	0	t	2026-09-24 20:54:21.445803	2026-09-24 20:54:21.445803	\N
724	Vòng đeo tay thông minh Huawei Band 10 viền nhựa	vong-deo-tay-thong-minh-huawei-band-10-vien-nhua	\N	30	7	690000.00	\N	0	TGDD-335762	\N	12	0.00	0	0	t	2026-09-24 20:54:21.461749	2026-09-24 20:54:21.461749	\N
725	Huawei Watch D2 48mm dây cao su	huawei-watch-d2-48mm-day-cao-su	\N	30	7	7840000.00	\N	0	TGDD-331078	\N	12	0.00	0	0	t	2026-09-24 20:54:21.47749	2026-09-24 20:54:21.47749	\N
726	Garmin Forerunner 55 42mm dây silicone	garmin-forerunner-55-42mm-day-silicone	\N	34	7	2590000.00	\N	0	TGDD-244296	\N	12	0.00	0	0	t	2026-09-24 20:54:21.493819	2026-09-24 20:54:21.493819	\N
727	Vòng đeo tay thông minh Mi Band 9 Pro	vong-deo-tay-thong-minh-mi-band-9-pro	\N	3	7	1750000.00	\N	0	TGDD-332404	\N	12	0.00	0	0	t	2026-09-24 20:54:21.510017	2026-09-24 20:54:21.510017	\N
728	Garmin Forerunner 70 42.6mm dây silicone	garmin-forerunner-70-42-6mm-day-silicone	\N	34	7	5750000.00	\N	0	TGDD-365405	\N	12	0.00	0	0	t	2026-09-24 20:54:21.526511	2026-09-24 20:54:21.526511	\N
729	Garmin Venu 4 41mm dây silicone	garmin-venu-4-41mm-day-silicone	\N	34	7	14240000.00	\N	0	TGDD-355663	\N	12	0.00	0	0	t	2026-09-24 20:54:21.543502	2026-09-24 20:54:21.543502	\N
730	Garmin Vivoactive 6 42.2mm dây silicone	garmin-vivoactive-6-42-2mm-day-silicone	\N	34	7	7320000.00	\N	0	TGDD-337137	\N	12	0.00	0	0	t	2026-09-24 20:54:21.561072	2026-09-24 20:54:21.561072	\N
731	Garmin Forerunner 970 47mm dây silicone	garmin-forerunner-970-47mm-day-silicone	\N	34	7	17660000.00	\N	0	TGDD-338698	\N	12	0.00	0	0	t	2026-09-24 20:54:21.577734	2026-09-24 20:54:21.577734	\N
732	Garmin Forerunner 570 47mm dây silicone	garmin-forerunner-570-47mm-day-silicone	\N	34	7	12340000.00	\N	0	TGDD-338701	\N	12	0.00	0	0	t	2026-09-24 20:54:21.593854	2026-09-24 20:54:21.593854	\N
733	Garmin Forerunner 170 42.6mm dây silicone	garmin-forerunner-170-42-6mm-day-silicone	\N	34	7	6950000.00	\N	0	TGDD-365406	\N	12	0.00	0	0	t	2026-09-24 20:54:21.610077	2026-09-24 20:54:21.610077	\N
734	Garmin Instinct 3 50mm dây silicone	garmin-instinct-3-50mm-day-silicone	\N	34	7	10760000.00	\N	0	TGDD-334987	\N	12	0.00	0	0	t	2026-09-24 20:54:21.627797	2026-09-24 20:54:21.627797	\N
735	Huawei Watch GT 5 Pro 42mm viền gốm dây gốm	huawei-watch-gt-5-pro-42mm-vien-gom-day-gom	\N	30	7	11990000.00	\N	0	TGDD-330180	\N	12	0.00	0	0	t	2026-09-24 20:54:21.644976	2026-09-24 20:54:21.644976	\N
736	Garmin Instinct 3 45mm dây silicone	garmin-instinct-3-45mm-day-silicone	\N	34	7	9660000.00	\N	0	TGDD-334986	\N	12	0.00	0	0	t	2026-09-24 20:54:21.662031	2026-09-24 20:54:21.662031	\N
737	Garmin Instinct E 45mm dây silicone	garmin-instinct-e-45mm-day-silicone	\N	34	7	7140000.00	\N	0	TGDD-334989	\N	12	0.00	0	0	t	2026-09-24 20:54:21.678766	2026-09-24 20:54:21.678766	\N
738	Garmin Venu X1 Sapphire 51.2mm dây nylon	garmin-venu-x1-sapphire-51-2mm-day-nylon	\N	34	7	17920000.00	\N	0	TGDD-340846	\N	12	0.00	0	0	t	2026-09-24 20:54:21.695282	2026-09-24 20:54:21.695282	\N
739	Xiaomi Redmi Watch 6	xiaomi-redmi-watch-6	\N	3	7	2590000.00	\N	0	TGDD-365217	\N	12	0.00	0	0	t	2026-09-24 20:54:21.711888	2026-09-24 20:54:21.711888	\N
740	Huawei Watch GT 6 46mm viền thép dây cao su	huawei-watch-gt-6-46mm-vien-thep-day-cao-su	\N	30	7	4390000.00	\N	0	TGDD-341452	\N	12	0.00	0	0	t	2026-09-24 20:54:21.728822	2026-09-24 20:54:21.728822	\N
741	Xiaomi Redmi Watch 5 47.5 mm dây TPU	xiaomi-redmi-watch-5-47-5-mm-day-tpu	\N	3	7	1890000.00	\N	0	TGDD-332069	\N	12	0.00	0	0	t	2026-09-24 20:54:21.74609	2026-09-24 20:54:21.74609	\N
742	Xiaomi Redmi Watch 5 Active 49.1mm dây TPU	xiaomi-redmi-watch-5-active-49-1mm-day-tpu	\N	3	7	880000.00	\N	0	TGDD-329834	\N	12	0.00	0	0	t	2026-09-24 20:54:21.764574	2026-09-24 20:54:21.764574	\N
743	Xiaomi Redmi Watch 5 Lite 48.2mm dây TPU	xiaomi-redmi-watch-5-lite-48-2mm-day-tpu	\N	3	7	1190000.00	\N	0	TGDD-329832	\N	12	0.00	0	0	t	2026-09-24 20:54:21.782232	2026-09-24 20:54:21.782232	\N
744	Huawei Watch GT 6 41mm viền thép dây cao su	huawei-watch-gt-6-41mm-vien-thep-day-cao-su	\N	30	7	4390000.00	\N	0	TGDD-354262	\N	12	0.00	0	0	t	2026-09-24 20:54:21.799677	2026-09-24 20:54:21.799677	\N
745	Xiaomi Watch S4 41mm dây cao su Fluoro	xiaomi-watch-s4-41mm-day-cao-su-fluoro	\N	3	7	3990000.00	\N	0	TGDD-341442	\N	12	0.00	0	0	t	2026-09-24 20:54:21.816216	2026-09-24 20:54:21.816216	\N
746	Samsung Galaxy Watch8 40mm dây silicone	samsung-galaxy-watch8-40mm-day-silicone	\N	7	7	8490000.00	\N	0	TGDD-338265	\N	12	0.00	0	0	t	2026-09-24 20:54:21.833614	2026-09-24 20:54:21.833614	\N
747	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Milan	apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-milan	\N	32	7	26490000.00	\N	0	TGDD-344809	\N	12	0.00	0	0	t	2026-09-24 20:54:21.850499	2026-09-24 20:54:21.850499	\N
748	Xiaomi Watch S4 41mm dây Milanese	xiaomi-watch-s4-41mm-day-milanese	\N	3	7	5690000.00	\N	0	TGDD-354314	\N	12	0.00	0	0	t	2026-09-24 20:54:21.866919	2026-09-24 20:54:21.866919	\N
749	Garmin Fenix 8 Sapphire 47mm viền Titanium dây silicone	garmin-fenix-8-sapphire-47mm-vien-titanium-day-silicone	\N	34	7	25020000.00	\N	0	TGDD-329474	\N	12	0.00	0	0	t	2026-09-24 20:54:21.883633	2026-09-24 20:54:21.883633	\N
750	Xiaomi Watch S4 47mm dây silicone Đen Cầu Vồng	xiaomi-watch-s4-47mm-day-silicone-den-cau-vong	\N	3	7	3510000.00	\N	0	TGDD-335516	\N	12	0.00	0	0	t	2026-09-24 20:54:21.900567	2026-09-24 20:54:21.900567	\N
751	Amazfit T-Rex 3 Pro 44mm dây silicone	amazfit-t-rex-3-pro-44mm-day-silicone	\N	31	7	8690000.00	\N	0	TGDD-360220	\N	12	0.00	0	0	t	2026-09-24 20:54:21.918562	2026-09-24 20:54:21.918562	\N
752	Samsung Galaxy Watch8 Classic LTE 46mm dây da	samsung-galaxy-watch8-classic-lte-46mm-day-da	\N	7	7	13490000.00	\N	0	TGDD-340065	\N	12	0.00	0	0	t	2026-09-24 20:54:21.935371	2026-09-24 20:54:21.935371	\N
753	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Trail	apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-trail	\N	32	7	23490000.00	\N	0	TGDD-344765	\N	12	0.00	0	0	t	2026-09-24 20:54:21.951286	2026-09-24 20:54:21.951286	\N
754	Garmin Fenix 8 Sapphire 43mm viền Titanium dây silicone	garmin-fenix-8-sapphire-43mm-vien-titanium-day-silicone	\N	34	7	25020000.00	\N	0	TGDD-329473	\N	12	0.00	0	0	t	2026-09-24 20:54:21.967686	2026-09-24 20:54:21.967686	\N
755	Garmin Fenix 8 47mm viền thép dây silicone	garmin-fenix-8-47mm-vien-thep-day-silicone	\N	34	7	22520000.00	\N	0	TGDD-329472	\N	12	0.00	0	0	t	2026-09-24 20:54:21.983866	2026-09-24 20:54:21.983866	\N
756	Garmin Fenix E 47mm viền thép dây silicone	garmin-fenix-e-47mm-vien-thep-day-silicone	\N	34	7	18090000.00	\N	0	TGDD-329479	\N	12	0.00	0	0	t	2026-09-24 20:54:21.999956	2026-09-24 20:54:21.999956	\N
757	Garmin Fenix 8 43mm viền thép dây silicone	garmin-fenix-8-43mm-vien-thep-day-silicone	\N	34	7	22520000.00	\N	0	TGDD-329468	\N	12	0.00	0	0	t	2026-09-24 20:54:22.015955	2026-09-24 20:54:22.015955	\N
758	Đồng hồ định vị trẻ em Kidcare Sight S1	dong-ho-dinh-vi-tre-em-kidcare-sight-s1	\N	29	7	1990000.00	\N	0	TGDD-333790	\N	12	0.00	0	0	t	2026-09-24 20:54:22.033124	2026-09-24 20:54:22.033124	\N
759	Đồng hồ định vị trẻ em imoo Z1 Hồng Nhạt	dong-ho-dinh-vi-tre-em-imoo-z1-hong-nhat	\N	33	7	2490000.00	\N	0	TGDD-316991	\N	12	0.00	0	0	t	2026-09-24 20:54:22.050999	2026-09-24 20:54:22.050999	\N
760	Samsung Galaxy Watch Ultra LTE 47mm (2025) dây silicone	samsung-galaxy-watch-ultra-lte-47mm-2025-day-silicone	\N	7	7	12090000.00	\N	0	TGDD-338267	\N	12	0.00	0	0	t	2026-09-24 20:54:22.06787	2026-09-24 20:54:22.06787	\N
761	Đồng hồ thông minh Zwatch Z6 44mm Xanh dương	dong-ho-thong-minh-zwatch-z6-44mm-xanh-duong	\N	35	7	490000.00	\N	0	TGDD-318631	\N	12	0.00	0	0	t	2026-09-24 20:54:22.100763	2026-09-24 20:54:22.100763	\N
762	Garmin Lily 2 Classic 34mm dây vải	garmin-lily-2-classic-34mm-day-vai	\N	34	7	6840000.00	\N	0	TGDD-322845	\N	12	0.00	0	0	t	2026-09-24 20:54:22.117766	2026-09-24 20:54:22.117766	\N
763	Đồng hồ định vị trẻ em Kidcare K1	dong-ho-dinh-vi-tre-em-kidcare-k1	\N	29	7	1890000.00	\N	0	TGDD-329076	\N	12	0.00	0	0	t	2026-09-24 20:54:22.13574	2026-09-24 20:54:22.13574	\N
764	Amazfit Active 2 Sapphire 43.9mm dây da	amazfit-active-2-sapphire-43-9mm-day-da	\N	31	7	2990000.00	\N	0	TGDD-336875	\N	12	0.00	0	0	t	2026-09-24 20:54:22.153353	2026-09-24 20:54:22.153353	\N
765	Amazfit Active 2 43.9mm dây silicone	amazfit-active-2-43-9mm-day-silicone	\N	31	7	2590000.00	\N	0	TGDD-335088	\N	12	0.00	0	0	t	2026-09-24 20:54:22.171834	2026-09-24 20:54:22.171834	\N
766	Huawei Watch GT 5 Pro 46mm viền Titanium dây Titanium	huawei-watch-gt-5-pro-46mm-vien-titanium-day-titanium	\N	30	7	10490000.00	\N	0	TGDD-330159	\N	12	0.00	0	0	t	2026-09-24 20:54:22.190031	2026-09-24 20:54:22.190031	\N
767	Garmin Vivoactive 5 42.2mm dây silicone	garmin-vivoactive-5-42-2mm-day-silicone	\N	34	7	5690000.00	\N	0	TGDD-315897	\N	12	0.00	0	0	t	2026-09-24 20:54:22.208581	2026-09-24 20:54:22.208581	\N
768	Đồng hồ định vị trẻ em MyKid 4G Lite	dong-ho-dinh-vi-tre-em-mykid-4g-lite	\N	36	7	1160000.00	\N	0	TGDD-324886	\N	12	0.00	0	0	t	2026-09-24 20:54:22.242575	2026-09-24 20:54:22.242575	\N
769	Đồng hồ định vị trẻ em Masstel Smart Hero 10	dong-ho-dinh-vi-tre-em-masstel-smart-hero-10	\N	13	7	1260000.00	\N	0	TGDD-288629	\N	12	0.00	0	0	t	2026-09-24 20:54:22.260715	2026-09-24 20:54:22.260715	\N
770	Đồng hồ định vị trẻ em Kidcare S6 4G Xanh Dorablue	dong-ho-dinh-vi-tre-em-kidcare-s6-4g-xanh-dorablue	\N	29	7	1800000.00	\N	0	TGDD-236901	\N	12	0.00	0	0	t	2026-09-24 20:54:22.278957	2026-09-24 20:54:22.278957	\N
771	Đồng hồ định vị trẻ em Kidcare S6 4G	dong-ho-dinh-vi-tre-em-kidcare-s6-4g	\N	29	7	1390000.00	\N	0	TGDD-236904	\N	12	0.00	0	0	t	2026-09-24 20:54:22.297035	2026-09-24 20:54:22.297035	\N
772	Đồng hồ định vị trẻ em Masstel Smart Hero Star	dong-ho-dinh-vi-tre-em-masstel-smart-hero-star	\N	13	7	1040000.00	\N	0	TGDD-339434	\N	12	0.00	0	0	t	2026-09-24 20:54:22.313692	2026-09-24 20:54:22.313692	\N
773	Đồng hồ định vị trẻ em Kidcare Sight S5 4G	dong-ho-dinh-vi-tre-em-kidcare-sight-s5-4g	\N	29	7	1440000.00	\N	0	TGDD-367385	\N	12	0.00	0	0	t	2026-09-24 20:54:22.331789	2026-09-24 20:54:22.331789	\N
774	Đồng hồ định vị trẻ em Kidcare Sight S26 4G	dong-ho-dinh-vi-tre-em-kidcare-sight-s26-4g	\N	29	7	1490000.00	\N	0	TGDD-362629	\N	12	0.00	0	0	t	2026-09-24 20:54:22.349588	2026-09-24 20:54:22.349588	\N
775	Đồng hồ định vị trẻ em Masstel Smart Hero 6	dong-ho-dinh-vi-tre-em-masstel-smart-hero-6	\N	13	7	1190000.00	\N	0	TGDD-326906	\N	12	0.00	0	0	t	2026-09-24 20:54:22.366818	2026-09-24 20:54:22.366818	\N
776	Đồng hồ định vị trẻ em Masstel Smart Hero 30	dong-ho-dinh-vi-tre-em-masstel-smart-hero-30	\N	13	7	1890000.00	\N	0	TGDD-337061	\N	12	0.00	0	0	t	2026-09-24 20:54:22.384715	2026-09-24 20:54:22.384715	\N
777	Xiaomi Redmi Watch 6 Active 47 mm dây TPU	xiaomi-redmi-watch-6-active-47-mm-day-tpu	\N	3	7	1190000.00	\N	0	TGDD-370087	\N	12	0.00	0	0	t	2026-09-24 20:54:22.402216	2026-09-24 20:54:22.402216	\N
778	Xiaomi Redmi Watch 6 Lite 48 mm dây TPU	xiaomi-redmi-watch-6-lite-48-mm-day-tpu	\N	3	7	1690000.00	\N	0	TGDD-369986	\N	12	0.00	0	0	t	2026-09-24 20:54:22.419575	2026-09-24 20:54:22.419575	\N
779	Vòng tay thông minh Samsung Galaxy Fit3 - Hồng	vong-tay-thong-minh-samsung-galaxy-fit3-hong	\N	7	7	970000.00	\N	0	TGDD-370976	\N	12	0.00	0	0	t	2026-09-24 20:54:22.436035	2026-09-24 20:54:22.436035	\N
780	Đồng hồ định vị trẻ em Tammi Watch Kid Plus	dong-ho-dinh-vi-tre-em-tammi-watch-kid-plus	\N	37	7	1190000.00	\N	0	TGDD-368547	\N	12	0.00	0	0	t	2026-09-24 20:54:22.468538	2026-09-24 20:54:22.468538	\N
781	Đồng hồ định vị trẻ em Tammi Watch Kid Max	dong-ho-dinh-vi-tre-em-tammi-watch-kid-max	\N	37	7	1490000.00	\N	0	TGDD-368548	\N	12	0.00	0	0	t	2026-09-24 20:54:22.485574	2026-09-24 20:54:22.485574	\N
782	Đồng hồ định vị trẻ em imoo Z3	dong-ho-dinh-vi-tre-em-imoo-z3	\N	33	7	3840000.00	\N	0	TGDD-358457	\N	12	0.00	0	0	t	2026-09-24 20:54:22.502918	2026-09-24 20:54:22.502918	\N
783	Đồng hồ định vị trẻ em Huawei Kids DRA-L10 X1	dong-ho-dinh-vi-tre-em-huawei-kids-dra-l10-x1	\N	30	7	4990000.00	\N	0	TGDD-370841	\N	12	0.00	0	0	t	2026-09-24 20:54:22.520183	2026-09-24 20:54:22.520183	\N
784	Đồng hồ định vị trẻ em MyKid 4G V2	dong-ho-dinh-vi-tre-em-mykid-4g-v2	\N	36	7	1260000.00	\N	0	TGDD-327123	\N	12	0.00	0	0	t	2026-09-24 20:54:22.536781	2026-09-24 20:54:22.536781	\N
785	realme Watch 5 50mm dây silicone	realme-watch-5-50mm-day-silicone	\N	4	7	1190000.00	\N	0	TGDD-344999	\N	12	0.00	0	0	t	2026-09-24 20:54:22.554442	2026-09-24 20:54:22.554442	\N
786	Vòng đeo tay thông minh Mi Band 11 Active	vong-deo-tay-thong-minh-mi-band-11-active	\N	3	7	890000.00	\N	0	TGDD-370088	\N	12	0.00	0	0	t	2026-09-24 20:54:22.571523	2026-09-24 20:54:22.571523	\N
787	Garmin Venu 3S 41mm dây silicone	garmin-venu-3s-41mm-day-silicone	\N	34	7	9830000.00	\N	0	TGDD-313829	\N	12	0.00	0	0	t	2026-09-24 20:54:22.58857	2026-09-24 20:54:22.58857	\N
788	Zobo G1 42.3mm dây silicone	zobo-g1-42-3mm-day-silicone	\N	38	7	740000.00	\N	0	TGDD-359399	\N	12	0.00	0	0	t	2026-09-24 20:54:22.620247	2026-09-24 20:54:22.620247	\N
789	Zobo Sporty 1 42mm dây silicone	zobo-sporty-1-42mm-day-silicone	\N	38	7	1240000.00	\N	0	TGDD-359401	\N	12	0.00	0	0	t	2026-09-24 20:54:22.63771	2026-09-24 20:54:22.63771	\N
790	Amazfit Active 42.3mm dây silicone	amazfit-active-42-3mm-day-silicone	\N	31	7	1790000.00	\N	0	TGDD-322267	\N	12	0.00	0	0	t	2026-09-24 20:54:22.654103	2026-09-24 20:54:22.654103	\N
791	Xiaomi Watch 5 47 mm dây cao su Fluoro	xiaomi-watch-5-47-mm-day-cao-su-fluoro	\N	3	7	5990000.00	\N	0	TGDD-363022	\N	12	0.00	0	0	t	2026-09-24 20:54:22.671186	2026-09-24 20:54:22.671186	\N
792	Đồng hồ định vị trẻ em imoo Z7	dong-ho-dinh-vi-tre-em-imoo-z7	\N	33	7	4990000.00	\N	0	TGDD-329598	\N	12	0.00	0	0	t	2026-09-24 20:54:22.687863	2026-09-24 20:54:22.687863	\N
793	Đồng hồ định vị trẻ em Zobo K2	dong-ho-dinh-vi-tre-em-zobo-k2	\N	38	7	890000.00	\N	0	TGDD-369501	\N	12	0.00	0	0	t	2026-09-24 20:54:22.705052	2026-09-24 20:54:22.705052	\N
794	Amazfit Balance 3 51.4mm dây silicone	amazfit-balance-3-51-4mm-day-silicone	\N	31	7	8690000.00	\N	0	TGDD-369675	\N	12	0.00	0	0	t	2026-09-24 20:54:22.721574	2026-09-24 20:54:22.721574	\N
795	Garmin Forerunner 265 Music 46.1mm dây silicone	garmin-forerunner-265-music-46-1mm-day-silicone	\N	34	7	9180000.00	\N	0	TGDD-305882	\N	12	0.00	0	0	t	2026-09-24 20:54:22.737757	2026-09-24 20:54:22.737757	\N
796	Amazfit T-Rex 3 47.1mm dây silicone	amazfit-t-rex-3-47-1mm-day-silicone	\N	31	7	5490000.00	\N	0	TGDD-329945	\N	12	0.00	0	0	t	2026-09-24 20:54:22.755783	2026-09-24 20:54:22.755783	\N
797	Zobo Novabiz 3 50.2mm dây silicone	zobo-novabiz-3-50-2mm-day-silicone	\N	38	7	940000.00	\N	0	TGDD-359400	\N	12	0.00	0	0	t	2026-09-24 20:54:22.772477	2026-09-24 20:54:22.772477	\N
798	Đồng hồ định vị trẻ em imoo X10	dong-ho-dinh-vi-tre-em-imoo-x10	\N	33	7	8190000.00	\N	0	TGDD-367419	\N	12	0.00	0	0	t	2026-09-24 20:54:22.789044	2026-09-24 20:54:22.789044	\N
799	Garmin Lily 2 34mm dây silicone	garmin-lily-2-34mm-day-silicone	\N	34	7	6180000.00	\N	0	TGDD-322839	\N	12	0.00	0	0	t	2026-09-24 20:54:22.805726	2026-09-24 20:54:22.805726	\N
800	OPPO Watch X3 47.4mm dây cao su	oppo-watch-x3-47-4mm-day-cao-su	\N	8	7	10990000.00	\N	0	TGDD-366928	\N	12	0.00	0	0	t	2026-09-24 20:54:22.822441	2026-09-24 20:54:22.822441	\N
801	Đồng hồ định vị trẻ em Zobo S8	dong-ho-dinh-vi-tre-em-zobo-s8	\N	38	7	1750000.00	\N	0	TGDD-369504	\N	12	0.00	0	0	t	2026-09-24 20:54:22.839278	2026-09-24 20:54:22.839278	\N
802	Garmin Golf Approach S70 47mm dây silicone	garmin-golf-approach-s70-47mm-day-silicone	\N	34	7	14370000.00	\N	0	TGDD-308291	\N	12	0.00	0	0	t	2026-09-24 20:54:22.858452	2026-09-24 20:54:22.858452	\N
803	Garmin Forerunner 265S 41.7mm dây silicone	garmin-forerunner-265s-41-7mm-day-silicone	\N	34	7	9180000.00	\N	0	TGDD-310710	\N	12	0.00	0	0	t	2026-09-24 20:54:22.875434	2026-09-24 20:54:22.875434	\N
804	Amazfit Balance 46mm dây nylon	amazfit-balance-46mm-day-nylon	\N	31	7	4480000.00	\N	0	TGDD-322375	\N	12	0.00	0	0	t	2026-09-24 20:54:22.892602	2026-09-24 20:54:22.892602	\N
805	Garmin Lily 2 Classic 34mm dây da	garmin-lily-2-classic-34mm-day-da	\N	34	7	7840000.00	\N	0	TGDD-322846	\N	12	0.00	0	0	t	2026-09-24 20:54:22.909411	2026-09-24 20:54:22.909411	\N
806	Garmin Golf Approach S70 42mm dây silicone	garmin-golf-approach-s70-42mm-day-silicone	\N	34	7	13340000.00	\N	0	TGDD-329514	\N	12	0.00	0	0	t	2026-09-24 20:54:22.926019	2026-09-24 20:54:22.926019	\N
807	Garmin Lily 2 Active 38mm dây silicone	garmin-lily-2-active-38mm-day-silicone	\N	34	7	7320000.00	\N	0	TGDD-330793	\N	12	0.00	0	0	t	2026-09-24 20:54:22.942079	2026-09-24 20:54:22.942079	\N
808	Garmin Instinct 3 Solar 45mm dây silicone	garmin-instinct-3-solar-45mm-day-silicone	\N	34	7	8640000.00	\N	0	TGDD-334983	\N	12	0.00	0	0	t	2026-09-24 20:54:22.95847	2026-09-24 20:54:22.95847	\N
809	Garmin Approach S50 43mm dây nylon	garmin-approach-s50-43mm-day-nylon	\N	34	7	9710000.00	\N	0	TGDD-335627	\N	12	0.00	0	0	t	2026-09-24 20:54:22.979482	2026-09-24 20:54:22.979482	\N
810	Bộ kit đo size nhẫn	bo-kit-do-size-nhan	\N	7	7	100000.00	\N	0	TGDD-337773	\N	12	0.00	0	0	t	2026-09-24 20:54:22.999253	2026-09-24 20:54:22.999253	\N
811	OPPO Watch X2 mini 43mm dây da	oppo-watch-x2-mini-43mm-day-da	\N	8	7	6990000.00	\N	0	TGDD-339863	\N	12	0.00	0	0	t	2026-09-24 20:54:23.01714	2026-09-24 20:54:23.01714	\N
812	Garmin Fenix 8 Sapphire 43mm viền thép dây silicone	garmin-fenix-8-sapphire-43mm-vien-thep-day-silicone	\N	34	7	25020000.00	\N	0	TGDD-364650	\N	12	0.00	0	0	t	2026-09-24 20:54:23.034224	2026-09-24 20:54:23.034224	\N
813	Amazfit Cheetah 2 Pro 48mm dây silicone	amazfit-cheetah-2-pro-48mm-day-silicone	\N	31	7	9990000.00	\N	0	TGDD-368303	\N	12	0.00	0	0	t	2026-09-24 20:54:23.050067	2026-09-24 20:54:23.050067	\N
814	Đồng hồ định vị trẻ em Zobo S6	dong-ho-dinh-vi-tre-em-zobo-s6	\N	38	7	1740000.00	\N	0	TGDD-369503	\N	12	0.00	0	0	t	2026-09-24 20:54:23.066953	2026-09-24 20:54:23.066953	\N
815	Nhẫn thông minh Samsung Galaxy Ring Size 6	nhan-thong-minh-samsung-galaxy-ring-size-6	\N	7	7	9810000.00	\N	0	TGDD-337830	\N	12	0.00	0	0	t	2026-09-24 20:54:23.083656	2026-09-24 20:54:23.083656	\N
816	Nhẫn thông minh Samsung Galaxy Ring Size 15	nhan-thong-minh-samsung-galaxy-ring-size-15	\N	7	7	9810000.00	\N	0	TGDD-337840	\N	12	0.00	0	0	t	2026-09-24 20:54:23.100126	2026-09-24 20:54:23.100126	\N
817	Suunto Run 46mm dây nylon	suunto-run-46mm-day-nylon	\N	39	7	4590000.00	\N	0	TGDD-364063	\N	12	0.00	0	0	t	2026-09-24 20:54:23.132453	2026-09-24 20:54:23.132453	\N
818	Suunto Race S 45mm dây silicone	suunto-race-s-45mm-day-silicone	\N	39	7	7390000.00	\N	0	TGDD-364064	\N	12	0.00	0	0	t	2026-09-24 20:54:23.149313	2026-09-24 20:54:23.149313	\N
819	Suunto Race S Titanium 45mm dây silicone	suunto-race-s-titanium-45mm-day-silicone	\N	39	7	9390000.00	\N	0	TGDD-364065	\N	12	0.00	0	0	t	2026-09-24 20:54:23.166621	2026-09-24 20:54:23.166621	\N
820	Apple Watch Series 12 GPS 42mm viền nhôm dây silicone	apple-watch-series-12-gps-42mm-vien-nhom-day-silicone	\N	32	7	11490000.00	\N	0	TGDD-371218	\N	12	0.00	0	0	t	2026-09-24 20:54:23.183288	2026-09-24 20:54:23.183288	\N
821	Apple Watch Series 12 GPS 46mm viền nhôm dây silicone	apple-watch-series-12-gps-46mm-vien-nhom-day-silicone	\N	32	7	12990000.00	\N	0	TGDD-371219	\N	12	0.00	0	0	t	2026-09-24 20:54:23.19919	2026-09-24 20:54:23.19919	\N
822	Apple Watch Series 12 GPS + Cellular 42mm viền nhôm dây silicone	apple-watch-series-12-gps-cellular-42mm-vien-nhom-day-silicone	\N	32	7	14490000.00	\N	0	TGDD-371221	\N	12	0.00	0	0	t	2026-09-24 20:54:23.215844	2026-09-24 20:54:23.215844	\N
823	Apple Watch Series 12 GPS + Cellular 42mm viền Titanium dây silicone	apple-watch-series-12-gps-cellular-42mm-vien-titanium-day-silicone	\N	32	7	20990000.00	\N	0	TGDD-371223	\N	12	0.00	0	0	t	2026-09-24 20:54:23.233385	2026-09-24 20:54:23.233385	\N
824	Apple Watch Series 12 GPS + Cellular 42mm viền Titanium dây Milan	apple-watch-series-12-gps-cellular-42mm-vien-titanium-day-milan	\N	32	7	22490000.00	\N	0	TGDD-371225	\N	12	0.00	0	0	t	2026-09-24 20:54:23.250856	2026-09-24 20:54:23.250856	\N
825	Apple Watch Series 12 GPS + Cellular 46mm viền nhôm dây silicone	apple-watch-series-12-gps-cellular-46mm-vien-nhom-day-silicone	\N	32	7	15990000.00	\N	0	TGDD-371226	\N	12	0.00	0	0	t	2026-09-24 20:54:23.267366	2026-09-24 20:54:23.267366	\N
826	Apple Watch Series 12 GPS + Cellular 46mm viền Titanium dây silicone	apple-watch-series-12-gps-cellular-46mm-vien-titanium-day-silicone	\N	32	7	22490000.00	\N	0	TGDD-371228	\N	12	0.00	0	0	t	2026-09-24 20:54:23.283693	2026-09-24 20:54:23.283693	\N
827	Apple Watch Series 12 GPS + Cellular 46mm viền Titanium dây Milan	apple-watch-series-12-gps-cellular-46mm-vien-titanium-day-milan	\N	32	7	23990000.00	\N	0	TGDD-371229	\N	12	0.00	0	0	t	2026-09-24 20:54:23.301906	2026-09-24 20:54:23.301906	\N
828	Apple Watch Series 12 GPS + Cellular 42mm viền Ceramic dây silicone	apple-watch-series-12-gps-cellular-42mm-vien-ceramic-day-silicone	\N	32	7	26790000.00	\N	0	TGDD-371230	\N	12	0.00	0	0	t	2026-09-24 20:54:23.318164	2026-09-24 20:54:23.318164	\N
829	Apple Watch Series 12 GPS + Cellular 46mm viền Ceramic dây silicone	apple-watch-series-12-gps-cellular-46mm-vien-ceramic-day-silicone	\N	32	7	28290000.00	\N	0	TGDD-371231	\N	12	0.00	0	0	t	2026-09-24 20:54:23.33417	2026-09-24 20:54:23.33417	\N
830	Apple Watch Ultra 4 GPS + Cellular 49mm viền Titanium dây Milan	apple-watch-ultra-4-gps-cellular-49mm-vien-titanium-day-milan	\N	32	7	26990000.00	\N	0	TGDD-371235	\N	12	0.00	0	0	t	2026-09-24 20:54:23.350698	2026-09-24 20:54:23.350698	\N
831	Apple Watch SE 3 GPS 40mm viền nhôm dây thể thao 2026	apple-watch-se-3-gps-40mm-vien-nhom-day-the-thao-2026	\N	32	7	6990000.00	\N	0	TGDD-371332	\N	12	0.00	0	0	t	2026-09-24 20:54:23.36791	2026-09-24 20:54:23.36791	\N
832	Apple Watch SE 3 GPS + Cellular 40mm viền nhôm dây thể thao 2026	apple-watch-se-3-gps-cellular-40mm-vien-nhom-day-the-thao-2026	\N	32	7	8490000.00	\N	0	TGDD-371333	\N	12	0.00	0	0	t	2026-09-24 20:54:23.384543	2026-09-24 20:54:23.384543	\N
833	Apple Watch SE 3 GPS 44mm viền nhôm dây thể thao 2026	apple-watch-se-3-gps-44mm-vien-nhom-day-the-thao-2026	\N	32	7	7850000.00	\N	0	TGDD-371335	\N	12	0.00	0	0	t	2026-09-24 20:54:23.401606	2026-09-24 20:54:23.401606	\N
834	Apple Watch SE 3 GPS + Cellular 44mm viền nhôm dây thể thao 2026	apple-watch-se-3-gps-cellular-44mm-vien-nhom-day-the-thao-2026	\N	32	7	9350000.00	\N	0	TGDD-371336	\N	12	0.00	0	0	t	2026-09-24 20:54:23.418302	2026-09-24 20:54:23.418302	\N
835	Đồng hồ Orient 41.5 mm Nam RA-AK0315L30B	dong-ho-orient-41-5-mm-nam-ra-ak0315l30b	\N	40	8	12625000.00	\N	0	TGDD-364640	\N	12	0.00	0	0	t	2026-09-24 20:54:23.464216	2026-09-24 20:54:23.464216	\N
836	Đồng hồ Baby-G 37.9 mm Nữ BGD-565SJ-9DR	dong-ho-baby-g-37-9-mm-nu-bgd-565sj-9dr	\N	41	8	2075000.00	\N	0	TGDD-326940	\N	12	0.00	0	0	t	2026-09-24 20:54:23.496432	2026-09-24 20:54:23.496432	\N
837	Đồng hồ Orient 41.5 mm Nam RA-AK0316L30B	dong-ho-orient-41-5-mm-nam-ra-ak0316l30b	\N	40	8	11925000.00	\N	0	TGDD-364641	\N	12	0.00	0	0	t	2026-09-24 20:54:23.512429	2026-09-24 20:54:23.512429	\N
838	Đồng hồ Orient 38.5 mm Nam RE-AU0112V00B	dong-ho-orient-38-5-mm-nam-re-au0112v00b	\N	40	8	17815000.00	\N	0	TGDD-364642	\N	12	0.00	0	0	t	2026-09-24 20:54:23.529047	2026-09-24 20:54:23.529047	\N
839	Đồng hồ Orient 38.5 mm Nam RE-AU0114E00B	dong-ho-orient-38-5-mm-nam-re-au0114e00b	\N	40	8	18715000.00	\N	0	TGDD-364643	\N	12	0.00	0	0	t	2026-09-24 20:54:23.545533	2026-09-24 20:54:23.545533	\N
840	Đồng hồ Orient 41 mm Nam RE-AV0138V00B	dong-ho-orient-41-mm-nam-re-av0138v00b	\N	40	8	24655000.00	\N	0	TGDD-364646	\N	12	0.00	0	0	t	2026-09-24 20:54:23.562525	2026-09-24 20:54:23.562525	\N
841	Đồng hồ Orient Sun & Moon 42 mm Nam RA-AS0010S30B	dong-ho-orient-sun-moon-42-mm-nam-ra-as0010s30b	\N	40	8	15555000.00	\N	0	TGDD-330392	\N	12	0.00	0	0	t	2026-09-24 20:54:23.578778	2026-09-24 20:54:23.578778	\N
842	Đồng hồ Orient Contemporary 40 mm Nam RA-TX0306S10B	dong-ho-orient-contemporary-40-mm-nam-ra-tx0306s10b	\N	40	8	7230000.00	\N	0	TGDD-331125	\N	12	0.00	0	0	t	2026-09-24 20:54:23.595175	2026-09-24 20:54:23.595175	\N
843	Đồng hồ Orient 41.5 mm Nam RA-AK0314E30B	dong-ho-orient-41-5-mm-nam-ra-ak0314e30b	\N	40	8	12625000.00	\N	0	TGDD-364639	\N	12	0.00	0	0	t	2026-09-24 20:54:23.613826	2026-09-24 20:54:23.613826	\N
844	Đồng hồ Orient 41.5 mm Nam RA-AK0313Y30B	dong-ho-orient-41-5-mm-nam-ra-ak0313y30b	\N	40	8	13190000.00	\N	0	TGDD-364638	\N	12	0.00	0	0	t	2026-09-24 20:54:23.630604	2026-09-24 20:54:23.630604	\N
845	Đồng hồ EDIFICE 47 mm Nam EQB-1200HG-1ADR	dong-ho-edifice-47-mm-nam-eqb-1200hg-1adr	\N	42	8	11320000.00	\N	0	TGDD-283074	\N	12	0.00	0	0	t	2026-09-24 20:54:23.663845	2026-09-24 20:54:23.663845	\N
846	Đồng hồ MVW Urban 39 mm Nam SS050326-03	dong-ho-mvw-urban-39-mm-nam-ss050326-03	\N	43	8	890000.00	\N	0	TGDD-367832	\N	12	0.00	0	0	t	2026-09-24 20:54:23.694868	2026-09-24 20:54:23.694868	\N
847	Đồng hồ MVW Urban 32.5 mm Nam SS050326-04	dong-ho-mvw-urban-32-5-mm-nam-ss050326-04	\N	43	8	690000.00	\N	0	TGDD-367833	\N	12	0.00	0	0	t	2026-09-24 20:54:23.712867	2026-09-24 20:54:23.712867	\N
848	Đồng hồ MVW Urban 39 mm Nam SS050326-02	dong-ho-mvw-urban-39-mm-nam-ss050326-02	\N	43	8	890000.00	\N	0	TGDD-367831	\N	12	0.00	0	0	t	2026-09-24 20:54:23.729378	2026-09-24 20:54:23.729378	\N
849	Đồng hồ MVW Urban 41 mm Nam SS050326-01	dong-ho-mvw-urban-41-mm-nam-ss050326-01	\N	43	8	890000.00	\N	0	TGDD-367830	\N	12	0.00	0	0	t	2026-09-24 20:54:23.746556	2026-09-24 20:54:23.746556	\N
850	Đồng hồ BABY-G 38.8 mm Nữ BGA-250-7A2DR	dong-ho-baby-g-38-8-mm-nu-bga-250-7a2dr	\N	41	8	2655000.00	\N	0	TGDD-199205	\N	12	0.00	0	0	t	2026-09-24 20:54:23.763802	2026-09-24 20:54:23.763802	\N
851	Đồng hồ MVW Lucky Gold 40 mm Nam SS30725-08	dong-ho-mvw-lucky-gold-40-mm-nam-ss30725-08	\N	43	8	2240000.00	\N	0	TGDD-360575	\N	12	0.00	0	0	t	2026-09-24 20:54:23.779816	2026-09-24 20:54:23.779816	\N
852	Đồng hồ MVW Lucky Gold 43 mm Nam SS30725-09	dong-ho-mvw-lucky-gold-43-mm-nam-ss30725-09	\N	43	8	2115000.00	\N	0	TGDD-360576	\N	12	0.00	0	0	t	2026-09-24 20:54:23.797546	2026-09-24 20:54:23.797546	\N
853	Đồng hồ MVW Lucky Gold 40.2 mm Nam SS30725-16	dong-ho-mvw-lucky-gold-40-2-mm-nam-ss30725-16	\N	43	8	2240000.00	\N	0	TGDD-360581	\N	12	0.00	0	0	t	2026-09-24 20:54:23.814158	2026-09-24 20:54:23.814158	\N
854	Đồng hồ MVW Lucky Gold 40.2 mm Nam SS30725-15	dong-ho-mvw-lucky-gold-40-2-mm-nam-ss30725-15	\N	43	8	2115000.00	\N	0	TGDD-360580	\N	12	0.00	0	0	t	2026-09-24 20:54:23.831838	2026-09-24 20:54:23.831838	\N
855	Ốp lưng Magnetic iPhone 17 Pro Max Nhựa cứng TORRAS C1S	op-lung-magnetic-iphone-17-pro-max-nhua-cung-torras-c1s	\N	44	9	369000.00	\N	0	TGDD-370179	\N	12	0.00	0	0	t	2026-09-24 20:54:23.878118	2026-09-24 20:54:23.878118	\N
856	Sạc nhanh 2 cổng Type-C QC3.0 PD 30W Ugreen X516	sac-nhanh-2-cong-type-c-qc3-0-pd-30w-ugreen-x516	\N	45	9	250000.00	\N	0	TGDD-337969	\N	12	0.00	0	0	t	2026-09-24 20:54:23.909816	2026-09-24 20:54:23.909816	\N
857	Loa Bluetooth Xiaomi Sound Play	loa-bluetooth-xiaomi-sound-play	\N	3	9	1590000.00	\N	0	TGDD-367810	\N	12	0.00	0	0	t	2026-09-24 20:54:23.926241	2026-09-24 20:54:23.926241	\N
858	Camera IP 3MP IMOU Cue 2E C32SP	camera-ip-3mp-imou-cue-2e-c32sp	\N	46	9	370000.00	\N	0	TGDD-369653	\N	12	0.00	0	0	t	2026-09-24 20:54:23.958417	2026-09-24 20:54:23.958417	\N
859	Pin sạc dự phòng Polymer 10000mAh Type C PD QC 3.0 15W Aukey PB-Y46 - Xám Đậm	pin-sac-du-phong-polymer-10000mah-type-c-pd-qc-3-0-15w-aukey-pb-y46-xam-dam	\N	47	9	305000.00	\N	0	TGDD-361088	\N	12	0.00	0	0	t	2026-09-24 20:54:23.989104	2026-09-24 20:54:23.989104	\N
860	Đèn pha năng lượng mặt trời Modi MD-PT58-120-W WH	den-pha-nang-luong-mat-troi-modi-md-pt58-120-w-wh	\N	48	9	760000.00	\N	0	TGDD-368720	\N	12	0.00	0	0	t	2026-09-24 20:54:24.020058	2026-09-24 20:54:24.020058	\N
861	Túi đeo chéo Tomtoc Aviator-T37 Travel Crossbody T37S1D1	tui-deo-cheo-tomtoc-aviator-t37-travel-crossbody-t37s1d1	\N	49	9	918000.00	\N	0	TGDD-364553	\N	12	0.00	0	0	t	2026-09-24 20:54:24.051514	2026-09-24 20:54:24.051514	\N
862	Quạt cầm tay AVA+ Mini JF329	quat-cam-tay-ava-mini-jf329	\N	50	9	150000.00	\N	0	TGDD-366973	\N	12	0.00	0	0	t	2026-09-24 20:54:24.082642	2026-09-24 20:54:24.083164	\N
863	Miếng dán kính cường lực iPhone 16 Pro Max TORRAS TITAN HD Tràn Viền 2 Miếng	mieng-dan-kinh-cuong-luc-iphone-16-pro-max-torras-titan-hd-tran-vien-2-mieng	\N	44	9	279000.00	\N	0	TGDD-370235	\N	12	0.00	0	0	t	2026-09-24 20:54:24.099036	2026-09-24 20:54:24.099036	\N
864	Sạc nhanh Type-C 20W TORRAS ICENANO Kèm Cáp Type C 1.2m	sac-nhanh-type-c-20w-torras-icenano-kem-cap-type-c-1-2m	\N	44	9	530000.00	\N	0	TGDD-370101	\N	12	0.00	0	0	t	2026-09-24 20:54:24.115423	2026-09-24 20:54:24.115423	\N
865	Tai nghe Bluetooth True Wireless Aukey EP-M3A - Xám	tai-nghe-bluetooth-true-wireless-aukey-ep-m3a-xam	\N	47	9	165000.00	\N	0	TGDD-361261	\N	12	0.00	0	0	t	2026-09-24 20:54:24.131528	2026-09-24 20:54:24.131528	\N
866	Camera IP 360 Độ 3MP IMOU Ranger 2 A32P Pro	camera-ip-360-do-3mp-imou-ranger-2-a32p-pro	\N	46	9	460000.00	\N	0	TGDD-369599	\N	12	0.00	0	0	t	2026-09-24 20:54:24.147475	2026-09-24 20:54:24.147475	\N
867	Pin sạc dự phòng Polymer 10000mAh Type C 10.5W Golf G80-C	pin-sac-du-phong-polymer-10000mah-type-c-10-5w-golf-g80-c	\N	51	9	180000.00	\N	0	TGDD-360665	\N	12	0.00	0	0	t	2026-09-24 20:54:24.179384	2026-09-24 20:54:24.179384	\N
868	Ghế Gaming WARRIOR Raider Series WGC206 Hồng Trắng	ghe-gaming-warrior-raider-series-wgc206-hong-trang	\N	52	9	2890000.00	\N	0	TGDD-370281	\N	12	0.00	0	0	t	2026-09-24 20:54:24.210885	2026-09-24 20:54:24.210885	\N
869	Túi chống sốc Laptop 14 inch Innostyle Lux Leather Laptop PLS-10BLK-14	tui-chong-soc-laptop-14-inch-innostyle-lux-leather-laptop-pls-10blk-14	\N	53	9	668000.00	\N	0	TGDD-363760	\N	12	0.00	0	0	t	2026-09-24 20:54:24.243489	2026-09-24 20:54:24.243489	\N
870	Bàn Phím Cơ Bluetooth EDRA ThunderBird TGK375WHE	ban-phim-co-bluetooth-edra-thunderbird-tgk375whe	\N	54	9	895000.00	\N	0	TGDD-368617	\N	12	0.00	0	0	t	2026-09-24 20:54:24.276478	2026-09-24 20:54:24.276478	\N
871	Ốp lưng Magsafe iPhone 16 Pro Max PC TPU Innostyle MPC-A01-16PM	op-lung-magsafe-iphone-16-pro-max-pc-tpu-innostyle-mpc-a01-16pm	\N	53	9	197000.00	\N	0	TGDD-363852	\N	12	0.00	0	0	t	2026-09-24 20:54:24.294449	2026-09-24 20:54:24.294449	\N
872	Sạc nhanh Type-C QC4.0+ PD 30W Ugreen Nexode CD319	sac-nhanh-type-c-qc4-0-pd-30w-ugreen-nexode-cd319	\N	45	9	240000.00	\N	0	TGDD-332248	\N	12	0.00	0	0	t	2026-09-24 20:54:24.312672	2026-09-24 20:54:24.312672	\N
873	Đèn đường liền thể năng lượng mặt trời Modi MD-SLT27-50-T WH	den-duong-lien-the-nang-luong-mat-troi-modi-md-slt27-50-t-wh	\N	48	9	415000.00	\N	0	TGDD-368715	\N	12	0.00	0	0	t	2026-09-24 20:54:24.329843	2026-09-24 20:54:24.329843	\N
874	Balo Laptop 15.6 inch TUCANO Loop Eco	balo-laptop-15-6-inch-tucano-loop-eco	\N	55	9	980000.00	\N	0	TGDD-360829	\N	12	0.00	0	0	t	2026-09-24 20:54:24.360633	2026-09-24 20:54:24.360633	\N
875	Điện thoại Samsung Galaxy S26+ 5G 12GB/256GB	dien-thoai-samsung-galaxy-s26-plus-5g-12gb-256gb	\N	7	4	22000000.00	\N	0	TGDD-361949	\N	12	0.00	0	0	t	2026-09-24 20:55:40.607329	2026-09-24 20:55:40.608339	\N
876	Điện thoại Xiaomi Redmi Note 14 Pro+ 5G 12GB/512GB	dien-thoai-xiaomi-redmi-note-14-pro-plus-5g-12gb-512gb	\N	3	4	9860000.00	\N	0	TGDD-337713	\N	12	0.00	0	0	t	2026-09-24 20:55:40.892792	2026-09-24 20:55:40.892792	\N
629	Máy tính bảng Samsung Galaxy Tab S10 FE+ WiFi 8GB/128GB	may-tinh-bang-samsung-galaxy-tab-s10-fe-plus-wifi-8gb-128gb	\N	7	6	14340000.00	\N	0	TGDD-336740	\N	12	0.00	0	0	t	2026-09-24 20:54:19.72086	2026-09-24 20:55:55.413863	\N
877	Máy tính bảng Samsung Galaxy Tab S10 FE WiFi 8GB/128GB	may-tinh-bang-samsung-galaxy-tab-s10-fe-wifi-8gb-128gb	\N	7	6	10800000.00	\N	0	TGDD-336737	\N	12	0.00	0	0	t	2026-09-24 20:55:58.075554	2026-09-24 20:55:58.075554	\N
\.


ALTER TABLE public.products ENABLE TRIGGER ALL;

--
-- Data for Name: product_images; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.product_images DISABLE TRIGGER ALL;

COPY public.product_images (image_id, product_id, image_url, alt_text, display_order, is_primary, uploaded_at) FROM stdin;
1	1	https://cdn.tgdd.vn/Products/Images/42/370982/iphone-18-pro-max-do-thumb-600x600.jpg	Điện thoại iPhone 18 Pro Max 256GB	0	t	2026-09-24 21:15:44.50129
2	2	https://cdn.tgdd.vn/Products/Images/42/370977/iphone-18-pro-do-thumb-600x600.jpg	Điện thoại iPhone 18 Pro 256GB	0	t	2026-09-24 21:15:44.575485
3	3	https://cdn.tgdd.vn/Products/Images/42/370987/iphone-duo-white-thumb-600x600.jpg	Điện thoại iPhone Duo 256GB	0	t	2026-09-24 21:15:44.620087
4	4	https://cdn.tgdd.vn/Products/Images/42/369617/xiaomi-redmi-note-17-4g-purple-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 17 4G 4GB/128GB	0	t	2026-09-24 21:15:44.668027
5	5	https://cdn.tgdd.vn/Products/Images/42/342679/iphone-17-pro-max-cam-thumb-600x600.jpg	Điện thoại iPhone 17 Pro Max 256GB	0	t	2026-09-24 21:15:44.716144
6	6	https://cdn.tgdd.vn/Products/Images/42/367499/realme-16t-xanh-thumb-600x600.jpg	Điện thoại realme 16T 5G 8GB/128GB	0	t	2026-09-24 21:15:44.765125
7	7	https://cdn.tgdd.vn/Products/Images/42/369626/xiaomi-redmi-note-17-pro-5g-purple-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 17 Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:44.813674
8	8	https://cdn.tgdd.vn/Products/Images/42/369628/xiaomi-redmi-note-17-pro-max-5g-purple-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 17 Pro Max 5G 12GB/256GB	0	t	2026-09-24 21:15:44.862886
9	9	https://cdn.tgdd.vn/Products/Images/42/367011/honor-600-lite-xanh-la-thumb-600x600.jpg	Điện thoại Honor 600 Lite 8GB/256GB	0	t	2026-09-24 21:15:44.913325
10	10	https://cdn.tgdd.vn/Products/Images/42/368236/motorola-razr-fold-trang-thumb-600x600.jpg	Điện thoại Motorola Razr Fold 5G 12GB/256GB	0	t	2026-09-24 21:15:44.95741
11	11	https://cdn.tgdd.vn/Products/Images/42/342667/iphone-17-xanh-thumb-600x600.jpg	Điện thoại iPhone 17 256GB	0	t	2026-09-24 21:15:45.003567
12	12	https://cdn.tgdd.vn/Products/Images/42/370544/samsung-galaxy-s26-fe-5g-blue-thumb-600x600.jpg	Điện thoại Samsung Galaxy S26 FE 5G 8GB/128GB	0	t	2026-09-24 21:15:45.047659
13	13	https://cdn.tgdd.vn/Products/Images/42/368250/oppo-reno16-f-trang-thumb-600x600.jpg	Điện thoại OPPO Reno16F 5G 8GB/128GB	0	t	2026-09-24 21:15:45.090816
14	14	https://cdn.tgdd.vn/Products/Images/42/342676/iphone-17-pro-cam-thumb-600x600.jpg	Điện thoại iPhone 17 Pro 256GB	0	t	2026-09-24 21:15:45.133578
15	15	https://cdn.tgdd.vn/Products/Images/42/368057/samsung-galaxy-z-fold8-kem-thumb-600x600.jpg	Điện thoại Samsung Galaxy Z Fold8 12GB/256GB	0	t	2026-09-24 21:15:45.176237
16	16	https://cdn.tgdd.vn/Products/Images/42/368050/samsung-galaxy-z-fold8-ultra-violet-thumb-600x600.jpg	Điện thoại Samsung Galaxy Z Fold8 Ultra 12GB/256GB	0	t	2026-09-24 21:15:45.219593
17	17	https://cdn.tgdd.vn/Products/Images/42/329138/iphone-16-plus-xanh-thumb-600x600.jpg	Điện thoại iPhone 16 Plus 128GB	0	t	2026-09-24 21:15:45.261796
18	18	https://cdn.tgdd.vn/Products/Images/42/363408/vivo-x300-ultra-xanh-thumb-1-600x600.jpg	Điện thoại Vivo X300 Ultra 16GB/512GB	0	t	2026-09-24 21:15:45.299213
19	19	https://cdn.tgdd.vn/Products/Images/42/368872/nothing-phone-4a-trang-thumb-600x600.jpg	Điện thoại Nothing Phone (4a) 5G 8GB/128GB	0	t	2026-09-24 21:15:45.338247
20	20	https://cdn.tgdd.vn/Products/Images/42/363401/samsung-galaxy-a37-trang-thumb-600x600.jpg	Điện thoại Samsung Galaxy A37 5G 6GB/128GB	0	t	2026-09-24 21:15:45.376605
21	21	https://cdn.tgdd.vn/Products/Images/42/367002/xiaomi-17t-5g-tim-thumb-600x600.jpg	Điện thoại Xiaomi 17T 5G 12GB/256GB	0	t	2026-09-24 21:15:45.414973
22	22	https://cdn.tgdd.vn/Products/Images/42/370752/oppo-a6-gold-thumb-600x600.jpg	Điện thoại OPPO A6 6GB/128GB	0	t	2026-09-24 21:15:45.453445
23	23	https://cdn.tgdd.vn/Products/Images/42/363466/vivo-v70-fe-8gb-256gb-tim-thumb-1-600x600.jpg	Điện thoại Vivo V70 FE 5G 8GB/256GB	0	t	2026-09-24 21:15:45.490147
24	24	https://cdn.tgdd.vn/Products/Images/42/367427/realme-c100x-xanh-thumb-1-600x600.jpg	Điện thoại realme C100x 4GB/128GB	0	t	2026-09-24 21:15:45.52938
25	25	https://cdn.tgdd.vn/Products/Images/42/358683/honor-x9d-5g-12gb-256gb-do-thumb-1-2-600x600.jpg	Điện thoại HONOR X9d 5G 12GB/256GB	0	t	2026-09-24 21:15:45.570283
26	26	https://cdn.tgdd.vn/Products/Images/42/367204/tecno-spark-50-4gb-128gb-xam-thumb-600x600.png	Điện thoại Tecno Spark 50 4GB/128GB	0	t	2026-09-24 21:15:45.611616
27	27	https://cdn.tgdd.vn/Products/Images/42/367975/motorola-edge-70-fusion-xanh-lam-thumb-1-600x600.jpg	Điện thoại Motorola Edge 70 Fusion 8GB/128GB	0	t	2026-09-24 21:15:45.660738
28	28	https://cdn.tgdd.vn/Products/Images/42/281570/iphone-15-xanh-thumb-600x600.jpg	Điện thoại iPhone 15 128GB	0	t	2026-09-24 21:15:45.701687
29	29	https://cdn.tgdd.vn/Products/Images/42/342670/iphone-air-vang-thumb-600x600.jpg	Điện thoại iPhone Air 256GB	0	t	2026-09-24 21:15:45.741233
31	31	https://cdn.tgdd.vn/Products/Images/42/368099/oppo-reno16-5g-8gb-256gb-tim-nhat-thumb-600x600.jpg	Điện thoại OPPO Reno16 5G 8GB/256GB	0	t	2026-09-24 21:15:45.816192
32	32	https://cdn.tgdd.vn/Products/Images/42/357576/vivo-v60-lite-pink-thumbai-600x600.jpg	Điện thoại vivo V60 Lite 5G 8GB/256GB	0	t	2026-09-24 21:15:45.854507
33	33	https://cdn.tgdd.vn/Products/Images/42/364187/realme-c100-tim-thumb-600x600.jpg	Điện thoại realme C100 4G 8GB/128GB	0	t	2026-09-24 21:15:45.889669
34	34	https://cdn.tgdd.vn/Products/Images/42/358026/honor-x7d-5g-gold-thumb-600x600.jpg	Điện thoại Honor X7d 5G 8GB/256GB	0	t	2026-09-24 21:15:45.925226
35	35	https://cdn.tgdd.vn/Products/Images/42/364633/tecno-spark-go-3-xanh-thumb-600x600.jpg	Điện thoại Tecno Spark Go 3 4GB/128GB	0	t	2026-09-24 21:15:45.961197
36	36	https://cdn.tgdd.vn/Products/Images/42/362373/motorola-g57-power-5g-8gb-128gb-xanh-bac-ha-thumb-1-600x600.jpg	Điện thoại Motorola G57 Power 5G 8GB/128GB	0	t	2026-09-24 21:15:45.99839
37	37	https://cdn.tgdd.vn/Products/Images/42/342692/iphone-17e-256gb-hong-thumb-600x600.jpg	Điện thoại iPhone 17e 256GB	0	t	2026-09-24 21:15:46.036453
38	38	https://cdn.tgdd.vn/Products/Images/42/360302/xiaomi-redmi-note-15-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 15 6GB/128GB	0	t	2026-09-24 21:15:46.073599
39	39	https://cdn.tgdd.vn/Products/Images/42/360244/oppo-a6x-titan-tim-thumb-600x600.jpg	Điện thoại OPPO A6x 4GB/64GB	0	t	2026-09-24 21:15:46.108871
40	40	https://cdn.tgdd.vn/Products/Images/42/368101/oppo-reno16-pro-den-thumb-600x600.jpg	Điện thoại OPPO Reno16 Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:46.143252
41	41	https://cdn.tgdd.vn/Products/Images/42/357862/vivo-x300-pro-den-thumb-600x600.jpg	Điện thoại vivo X300 Pro 5G 16GB/512GB	0	t	2026-09-24 21:15:46.179896
42	42	https://cdn.tgdd.vn/Products/Images/42/361703/realme-16-trang-thumb-600x600.jpg	Điện thoại realme 16 5G 8GB/256GB	0	t	2026-09-24 21:15:46.215012
43	43	https://cdn.tgdd.vn/Products/Images/42/344641/honor-magic-v5-black-thumb-600x600.jpg	Điện thoại HONOR Magic V5 5G 16GB/512GB	0	t	2026-09-24 21:15:46.2516
44	44	https://cdn.tgdd.vn/Products/Images/42/362374/motorola-edge-70-8gb-256gb-xam-thumb-1-600x600.jpg	Điện thoại Motorola Edge 70 8GB/256GB	0	t	2026-09-24 21:15:46.287383
45	45	https://cdn.tgdd.vn/Products/Images/42/329135/iphone-16-blue-600x600.png	Điện thoại iPhone 16 128GB	0	t	2026-09-24 21:15:46.32041
46	46	https://cdn.tgdd.vn/Products/Images/42/334864/iphone-16e-white-thumb-600x600.jpg	Điện thoại iPhone 16e 128GB	0	t	2026-09-24 21:15:46.353276
47	47	https://cdn.tgdd.vn/Products/Images/42/333363/samsung-galaxy-s25-green-thumbai-600x600.jpg	Điện thoại Samsung Galaxy S25 5G 12GB/256GB	0	t	2026-09-24 21:15:46.389396
48	48	https://cdn.tgdd.vn/Products/Images/42/360310/xiaomi-redmi-note-15-5g-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 15 5G 6GB/128GB	0	t	2026-09-24 21:15:46.423822
49	49	https://cdn.tgdd.vn/Products/Images/42/339245/honor-400-lite-green-thumb-600x600.jpg	Điện thoại HONOR 400 Lite 12GB/256GB	0	t	2026-09-24 21:15:46.459557
50	50	https://cdn.tgdd.vn/Products/Images/42/360344/realme-c85-8gb-128gb-xanh-thumb-600x600.jpg	Điện thoại realme C85 8GB/128GB	0	t	2026-09-24 21:15:46.495156
51	51	https://cdn.tgdd.vn/Products/Images/42/369546/TimerThumb/369546-600x600.png	Điện thoại Motorola G06 POWER 4GB/64GB	0	t	2026-09-24 21:15:46.532626
52	52	https://cdn.tgdd.vn/Products/Images/42/341688/galaxy-a17-5g-gray-thumbai-600x600.jpg	Điện thoại Samsung Galaxy A17 5G 8GB/128GB	0	t	2026-09-24 21:15:46.57282
53	53	https://cdn.tgdd.vn/Products/Images/42/360312/xiaomi-redmi-note-15-pro-5g-8gb-256gb-titan-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 15 Pro 5G 8GB/256GB	0	t	2026-09-24 21:15:46.611681
54	54	https://cdn.tgdd.vn/Products/Images/42/339180/vivo-y39-5g-tim-thumb-600x600.jpg	Điện thoại vivo Y39 5G 8GB/128GB	0	t	2026-09-24 21:15:46.647529
55	55	https://cdn.tgdd.vn/Products/Images/42/357832/realme-c85-pro-tim-thumb-600x600.jpg	Điện thoại realme C85 Pro 8GB/128GB	0	t	2026-09-24 21:15:46.683968
56	56	https://cdn.tgdd.vn/Products/Images/42/339638/honor-400-5g-12gb-256gb-den-thumb-600x600.jpg	Điện thoại HONOR 400 5G 12GB/256GB Đen	0	t	2026-09-24 21:15:46.718759
57	57	https://cdn.tgdd.vn/Products/Images/42/362760/realme-c85-xanh-thumb-600x600.jpg	Điện thoại realme C85 5G 8GB/128GB	0	t	2026-09-24 21:15:46.760477
58	58	https://cdn.tgdd.vn/Products/Images/42/366660/tecno-spark-40-6gb-128gb-den-thumb-600x600.jpg	Điện thoại Tecno Spark 40 6GB/128GB	0	t	2026-09-24 21:15:46.799555
59	59	https://cdn.tgdd.vn/Products/Images/42/358048/motorola-moto-g35-5g-4gb-128gb-xanh-thumb-1-600x600.jpg	Điện thoại Motorola G35 5G 4GB/128GB	0	t	2026-09-24 21:15:46.833801
60	60	https://cdn.tgdd.vn/Products/Images/42/366829/realme-c100i-xam-thumb-1-600x600.jpg	Điện thoại realme C100i 4GB/64GB	0	t	2026-09-24 21:15:46.868974
61	61	https://cdn.tgdd.vn/Products/Images/42/363438/realme-p4-power-cam-thumb-600x600.jpg	Điện thoại realme P4 Power 5G 12GB/256GB	0	t	2026-09-24 21:15:46.907231
62	62	https://cdn.tgdd.vn/Products/Images/42/342560/samsung-galaxy-s25-fe-blue-thumbai-600x600.jpg	Điện thoại Samsung Galaxy S25 FE 5G 8GB/128GB	0	t	2026-09-24 21:15:46.943234
63	63	https://cdn.tgdd.vn/Products/Images/42/360240/oppo-reno15f-5g-8gb-256gb-hong-thumb-600x600.jpg	Điện thoại OPPO Reno15 F 5G 8GB/256GB	0	t	2026-09-24 21:15:46.980005
64	64	https://cdn.tgdd.vn/Products/Images/42/360307/xiaomi-redmi-note-15-pro-8gb-256gb-den-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 15 Pro 8GB/256GB	0	t	2026-09-24 21:15:47.025714
65	65	https://cdn.tgdd.vn/Products/Images/42/344650/oppo-a6-pro-4g-titan-thumbai-600x600.jpg	Điện thoại OPPO A6 Pro 8GB/256GB	0	t	2026-09-24 21:15:47.068355
66	66	https://cdn.tgdd.vn/Products/Images/42/343067/realme-15-pro-bac-thumb-600x600.jpg	Điện thoại realme 15 Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:47.100771
67	67	https://cdn.tgdd.vn/Products/Images/42/343063/realme-15t-bac-thumb-600x600.jpg	Điện thoại realme 15T 5G 8GB/256GB	0	t	2026-09-24 21:15:47.133314
68	68	https://cdn.tgdd.vn/Products/Images/42/358225/motorola-moto-g86-5g-8gb-128gb-tim-thumb-600x600.jpg	Điện thoại Motorola G86 POWER 5G 8GB/128GB	0	t	2026-09-24 21:15:47.164872
69	69	https://cdn.tgdd.vn/Products/Images/42/358224/motorola-edge-60-fusion-8gb-256gb-xanh-thumb-1-600x600.jpg	Điện thoại Motorola Edge 60 Fusion 5G 8GB/256GB	0	t	2026-09-24 21:15:47.197582
70	70	https://cdn.tgdd.vn/Products/Images/42/358223/motorola-razr-60-8gb-256gb-xanh-thumb-1-600x600.jpg	Điện thoại Motorola Razr 60 5G 8GB/256GB	0	t	2026-09-24 21:15:47.23126
71	71	https://cdn.tgdd.vn/Products/Images/42/338738/samsung-galaxy-z-fold7-black-thumb-1-600x600.jpg	Điện thoại Samsung Galaxy Z Fold7 5G 12GB/256GB	0	t	2026-09-24 21:15:47.265363
72	72	https://cdn.tgdd.vn/Products/Images/42/360309/xiaomi-redmi-note-15-pro-plus-5g-12gb-256gb-den-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 15 Pro+ 5G 12GB/256GB	0	t	2026-09-24 21:15:47.298113
73	73	https://cdn.tgdd.vn/Products/Images/42/343124/oppo-a6-pro-5g-blue-thumbai-600x600.jpg	Điện thoại OPPO A6 Pro 5G 8GB/256GB	0	t	2026-09-24 21:15:47.33267
74	74	https://cdn.tgdd.vn/Products/Images/42/341625/vivo-v60-5g-xam-thumb-600x600.jpg	Điện thoại vivo V60 5G 12GB/256GB	0	t	2026-09-24 21:15:47.366112
75	75	https://cdn.tgdd.vn/Products/Images/42/362919/honor-x8d-8gb-128gb-xanh-thumb-600x600.jpg	Điện thoại HONOR X8d 8GB/128GB	0	t	2026-09-24 21:15:47.398489
76	76	https://cdn.tgdd.vn/Products/Images/42/361191/oppo-a6t-xanh-thumb-600x600.jpg	Điện thoại OPPO A6t 4GB/64GB	0	t	2026-09-24 21:15:47.430538
77	77	https://cdn.tgdd.vn/Products/Images/42/336408/vivo-v50-lite-vang-600x600.jpg	Điện thoại vivo V50 Lite 5G 8GB/256GB	0	t	2026-09-24 21:15:47.463923
78	78	https://cdn.tgdd.vn/Products/Images/42/361951/samsung-galaxy-s26-ultra-12gb-256gb-den-thumb-600x600.jpg	Điện thoại Samsung Galaxy S26 Ultra 5G 12GB/256GB	0	t	2026-09-24 21:15:47.497404
79	79	https://cdn.tgdd.vn/Products/Images/42/363398/samsung-galaxy-a57-tim-thumb-600x600.jpg	Điện thoại Samsung Galaxy A57 5G 8GB/128GB	0	t	2026-09-24 21:15:47.529391
80	80	https://cdn.tgdd.vn/Products/Images/42/341802/samsung-galaxy-a07-black-thumbnew-600x600.jpg	Điện thoại Samsung Galaxy A07 4GB/64GB	0	t	2026-09-24 21:15:47.561396
81	81	https://cdn.tgdd.vn/Products/Images/42/341797/samsung-galaxy-a17-lte-xam-thumb-600x600.jpg	Điện thoại Samsung Galaxy A17 4GB/128GB	0	t	2026-09-24 21:15:47.59623
82	82	https://cdn.tgdd.vn/Products/Images/42/311033/nokia-105-4g-den-thumb-600x600.jpg	Điện thoại Nokia 105 4G Pro	0	t	2026-09-24 21:15:47.630179
83	83	https://cdn.tgdd.vn/Products/Images/42/369630/xiaomi-redmi-17-green-thumb-600x600.jpg	Điện thoại Xiaomi Redmi 17 4G 4GB/128GB	0	t	2026-09-24 21:15:47.664
84	84	https://cdn.tgdd.vn/Products/Images/42/365289/xiaomi-poco-c71-4gb-64gb-vang-thumb-600x600.jpg	Điện thoại Xiaomi Poco C71 4GB/64GB	0	t	2026-09-24 21:15:47.697578
85	85	https://cdn.tgdd.vn/Products/Images/42/367977/masstel-izi-t6-t127-den-thumb-600x600.jpg	Điện thoại Masstel IZI T6 T127	0	t	2026-09-24 21:15:47.735636
86	86	https://cdn.tgdd.vn/Products/Images/42/360671/vivo-31d-den-thumb-600x600.jpg	Điện thoại vivo Y31d 6GB/128GB	0	t	2026-09-24 21:15:47.774391
87	87	https://cdn.tgdd.vn/Products/Images/42/323546/masstel-fami-50-green-thumb-600x600.jpg	Điện thoại Masstel Fami 50 4G	0	t	2026-09-24 21:15:47.811026
88	88	https://cdn.tgdd.vn/Products/Images/42/365875/vivo-y05-xanh-den-thumb-600x600.jpg	Điện thoại vivo Y05 4GB/64GB	0	t	2026-09-24 21:15:47.844677
89	89	https://cdn.tgdd.vn/Products/Images/42/329676/nokia-hmd-105-4g-pink-thumb-600x600.jpg	Điện thoại Nokia HMD 105 4G	0	t	2026-09-24 21:15:47.879799
90	90	https://cdn.tgdd.vn/Products/Images/42/342939/masstel-izi-10-xanh-600x600.jpg	Điện thoại Masstel IZI 10 4G Type-C	0	t	2026-09-24 21:15:47.91315
91	91	https://cdn.tgdd.vn/Products/Images/42/207956/nokia-220-4g-cam-thumb-600x600.jpg	Điện thoại Nokia 220 4G	0	t	2026-09-24 21:15:47.946243
92	92	https://cdn.tgdd.vn/Products/Images/42/370758/masstel-izi-10s-den-thumb-600x600.jpg	Điện thoại Masstel IZI 10S	0	t	2026-09-24 21:15:47.978932
93	93	https://cdn.tgdd.vn/Products/Images/42/364793/xiaomi-redmi-a7-pro-cam-thumb-600x600.jpg	Điện thoại Xiaomi Redmi A7 Pro 4GB/64GB	0	t	2026-09-24 21:15:48.013109
94	94	https://cdn.tgdd.vn/Products/Images/42/367596/oppo-a6c-nau-thumb-1-600x600.jpg	Điện thoại OPPO A6c 4GB/64GB	0	t	2026-09-24 21:15:48.046005
95	95	https://cdn.tgdd.vn/Products/Images/42/363437/realme-note-80-den-thumb-600x600.jpg	Điện thoại realme Note 80 4GB/64GB	0	t	2026-09-24 21:15:48.079159
96	96	https://cdn.tgdd.vn/Products/Images/42/370492/vivo-y05e-xanh-thumb-600x600.jpg	Điện thoại vivo Y05e 4GB/64GB	0	t	2026-09-24 21:15:48.115239
97	97	https://cdn.tgdd.vn/Products/Images/42/311034/nokia-110-4g-xanh-thumb-600x600.jpg	Điện thoại Nokia 110 4G Pro	0	t	2026-09-24 21:15:48.148959
98	98	https://cdn.tgdd.vn/Products/Images/42/365878/vivo-y11d-den-thumb-600x600.jpg	Điện thoại Vivo Y11d 4GB/128GB	0	t	2026-09-24 21:15:48.181348
99	99	https://cdn.tgdd.vn/Products/Images/42/368232/xiaomi-redmi-a7-3gb-64gb-xanh-thumb-600x600.jpg	Điện thoại Xiaomi Redmi A7 3GB/64GB	0	t	2026-09-24 21:15:48.213226
100	100	https://cdn.tgdd.vn/Products/Images/42/366923/nubia-v80-design-4gb-128gb-den-thumb-600x600.jpg	Điện thoại Nubia V80 Design 4GB/128GB	0	t	2026-09-24 21:15:48.245808
101	101	https://cdn.tgdd.vn/Products/Images/42/361709/samsung-galaxy-a07-5g-tim-thumb-1-600x600.jpg	Điện thoại Samsung Galaxy A07 5G 4GB/128GB	0	t	2026-09-24 21:15:48.277134
102	102	https://cdn.tgdd.vn/Products/Images/42/299998/mobell-f209-gold-600x600.jpg	Điện thoại Mobell F209	0	t	2026-09-24 21:15:48.310024
103	103	https://cdn.tgdd.vn/Products/Images/42/358698/honor-play-10-3gb-64gb-xanh-thumb-600x600.jpg	Điện thoại HONOR Play 10 3GB/64GB	0	t	2026-09-24 21:15:48.342319
104	104	https://cdn.tgdd.vn/Products/Images/42/333347/samsung-galaxy-s25-ultra-blue-thumbai-600x600.jpg	Điện thoại Samsung Galaxy S25 Ultra 5G 12GB/256GB	0	t	2026-09-24 21:15:48.374468
105	105	https://cdn.tgdd.vn/Products/Images/42/369620/xiaomi-redmi-note-17-5g-blue-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 17 5G 6GB/128GB	0	t	2026-09-24 21:15:48.408418
106	106	https://cdn.tgdd.vn/Products/Images/42/366662/nubia-a56-4gb-128gb-vang-thumb-600x600.jpg	Điện thoại Nubia A56 4GB/128GB	0	t	2026-09-24 21:15:48.440686
107	107	https://cdn.tgdd.vn/Products/Images/42/361630/nubia-a36-4gb-64gb-vang-thumb-600x600.jpg	Điện thoại Nubia A36 4GB/64GB	0	t	2026-09-24 21:15:48.472466
108	108	https://cdn.tgdd.vn/Products/Images/42/369634/xiaomi-redmi-17-5g-cam-thumb-600x600.jpg	Điện thoại Xiaomi Redmi 17 5G 4GB/128GB	0	t	2026-09-24 21:15:48.503944
109	109	https://cdn.tgdd.vn/Products/Images/42/304608/mobell-f309-trang-thumb-600x600.jpg	Điện thoại Mobell F309 4G	0	t	2026-09-24 21:15:48.53529
110	110	https://cdn.tgdd.vn/Products/Images/42/284122/mobell-m239-xanh-thumb-1-600x600.jpg	Điện thoại Mobell M239 4G	0	t	2026-09-24 21:15:48.566787
111	111	https://cdn.tgdd.vn/Products/Images/42/367004/xiaomi-17t-pro-5g-den-thumb-600x600.jpg	Điện thoại Xiaomi 17T Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:48.598473
112	112	https://cdn.tgdd.vn/Products/Images/42/359776/xiaomi-redmi-15-5g-4gb-128gb-xanh-thumb-600x600.jpg	Điện thoại Xiaomi Redmi 15 5G 4GB/128GB	0	t	2026-09-24 21:15:48.632401
113	113	https://cdn.tgdd.vn/Products/Images/42/363464/vivo-v70-8gb-256gb-vang-thumb-600x600.jpg	Điện thoại Vivo V70 5G 8GB/256GB	0	t	2026-09-24 21:15:48.667963
114	114	https://cdn.tgdd.vn/Products/Images/42/367811/nubia-v80-max-6gb-128gb-bac-thumb-600x600.jpg	Điện thoại Nubia V80 Max 6GB/128GB	0	t	2026-09-24 21:15:48.70379
115	115	https://cdn.tgdd.vn/Products/Images/42/358691/honor-x5c-plus-4gb-64gb-den-thumb-600x600.jpg	Điện thoại HONOR X5c Plus 4GB/64GB	0	t	2026-09-24 21:15:48.737491
116	116	https://cdn.tgdd.vn/Products/Images/42/322877/masstel-izi-t6-green-thumb-600x600.jpg	Điện thoại Masstel IZI T6 4G	0	t	2026-09-24 21:15:48.772948
117	117	https://cdn.tgdd.vn/Products/Images/42/322876/masstel-fami-60s-blue-thumb-600x600.jpg	Điện thoại Masstel Fami 60S 4G	0	t	2026-09-24 21:15:48.808712
118	875	https://cdn.tgdd.vn/Products/Images/42/361949/samsung-galaxy-s26-plus-12gb-256gb-xanh-duong-thumb-600x600.jpg	Điện thoại Samsung Galaxy S26+ 5G 12GB/256GB	0	t	2026-09-24 21:15:48.84326
119	118	https://cdn.tgdd.vn/Products/Images/42/360238/oppo-reno15-5g-8gb-256gb-xanh-thumb-600x600.jpg	Điện thoại OPPO Reno15 5G 8GB/256GB	0	t	2026-09-24 21:15:48.878077
120	119	https://cdn.tgdd.vn/Products/Images/42/346265/xiaomi-redmi-15c-black-thumb-600x600.jpg	Điện thoại Xiaomi Redmi 15C 6GB/128GB	0	t	2026-09-24 21:15:48.913893
121	120	https://cdn.tgdd.vn/Products/Images/42/368053/samsung-galaxy-z-flip8-xam-thumb-600x600.jpg	Điện thoại Samsung Galaxy Z Flip8 5G 12GB/256GB	0	t	2026-09-24 21:15:48.94836
122	121	https://cdn.tgdd.vn/Products/Images/42/363470/xiaomi-poco-x8-pro-8gb-256gb-xanh-thumb-600x600.jpg	Điện thoại Xiaomi POCO X8 Pro 5G 8GB/256GB	0	t	2026-09-24 21:15:48.981764
123	122	https://cdn.tgdd.vn/Products/Images/42/341272/xiaomi-redmi-15-tim-thumbnew-600x600.jpg	Điện thoại Xiaomi Redmi 15 6GB/128GB	0	t	2026-09-24 21:15:49.016871
124	123	https://cdn.tgdd.vn/Products/Images/42/362971/honor-x7d-8gb-128gb-vang-thumb-600x600.jpg	Điện thoại Honor X7d 8GB/128GB	0	t	2026-09-24 21:15:49.050717
125	124	https://cdn.tgdd.vn/Products/Images/42/368045/samsung-galaxy-a27-xanh-thumb-600x600.jpg	Điện thoại Samsung Galaxy A27 6GB/128GB	0	t	2026-09-24 21:15:49.086288
126	125	https://cdn.tgdd.vn/Products/Images/42/365402/oppo-find-x9s-cam-thumb-1-600x600.jpg	Điện thoại OPPO Find X9s 12GB/512GB	0	t	2026-09-24 21:15:49.12091
127	126	https://cdn.tgdd.vn/Products/Images/42/344644/xiaomi-15t-12gb-256gb-vang-thumb-600x600.jpg	Điện thoại Xiaomi 15T 5G 12GB/256GB	0	t	2026-09-24 21:15:49.159104
128	127	https://cdn.tgdd.vn/Products/Images/42/288630/mobell-m539-do-thumb-600x600.jpg	Điện thoại Mobell M539	0	t	2026-09-24 21:15:49.2014
129	128	https://cdn.tgdd.vn/Products/Images/42/358669/vivo-y21d-purple-thumb-600x600.jpg	Điện thoại vivo Y21d 6GB/128GB	0	t	2026-09-24 21:15:49.237279
130	129	https://cdn.tgdd.vn/Products/Images/42/338741/samsung-galaxy-z-flip7-fe-white-thumb-600x600.jpg	Điện thoại Samsung Galaxy Z Flip7 FE 5G 8GB/128GB	0	t	2026-09-24 21:15:49.270672
131	130	https://cdn.tgdd.vn/Products/Images/42/368234/motorola-g37-den-thumb-600x600.jpg	Điện thoại Motorola Moto G37 5G 4GB/64GB	0	t	2026-09-24 21:15:49.3027
132	131	https://cdn.tgdd.vn/Products/Images/42/338026/mobell-rock-7-green-thumb-600x600.jpg	Điện thoại Mobell Rock 7	0	t	2026-09-24 21:15:49.333843
133	132	https://cdn.tgdd.vn/Products/Images/42/360236/oppo-reno15-pro-5g-12gb-256gb-xanh-thumb-600x600.jpg	Điện thoại OPPO Reno15 Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:49.365539
134	133	https://cdn.tgdd.vn/Products/Images/42/314697/mobell-m331-thumb-xanh-1-600x600.jpg	Điện thoại Mobell M331 4G	0	t	2026-09-24 21:15:49.397924
135	134	https://cdn.tgdd.vn/Products/Images/42/361270/xiaomi-17-ultra-den-thumb-600x600.jpg	Điện thoại Xiaomi 17 Ultra 5G 16GB/512GB	0	t	2026-09-24 21:15:49.429753
136	135	https://cdn.tgdd.vn/Products/Images/42/326477/nokia-3210-4g-black-thumb-600x600.jpg	Điện thoại Nokia 3210 4G	0	t	2026-09-24 21:15:49.461047
137	136	https://cdn.tgdd.vn/Products/Images/42/361269/xiaomi-17-xanh-la-thumb-600x600.jpg	Điện thoại Xiaomi 17 5G 12GB/256GB	0	t	2026-09-24 21:15:49.492832
138	137	https://cdn.tgdd.vn/Products/Images/42/370547/xphone-hera-s9-den-thumb-600x600.jpg	Điện thoại Xphone Hera S9	0	t	2026-09-24 21:15:49.524676
139	138	https://cdn.tgdd.vn/Products/Images/42/362375/motorola-signature-12gb-256gb-thumb-600x600.jpg	Điện thoại Motorola Signature 12GB/256GB	0	t	2026-09-24 21:15:49.556294
140	139	https://cdn.tgdd.vn/Products/Images/42/340220/honor-x6c-6gb-128gb-xanh-thumb-600x600.jpg	Điện thoại HONOR X6c 6GB/128GB	0	t	2026-09-24 21:15:49.586974
141	140	https://cdn.tgdd.vn/Products/Images/42/339177/oppo-reno14-f-5g-pink-thumb-600x600.jpg	Điện thoại OPPO Reno14 F 5G 12GB/256GB	0	t	2026-09-24 21:15:49.618507
142	141	https://cdn.tgdd.vn/Products/Images/42/344645/xiaomi-15t-12gb-512gb-xam-thumb-600x600.jpg	Điện thoại Xiaomi 15T 5G 12GB/512GB	0	t	2026-09-24 21:15:49.651126
143	142	https://cdn.tgdd.vn/Products/Images/42/363468/xiaomi-poco-m8-8gb-256gb-xanh-thumb-600x600.jpg	Điện thoại Xiaomi POCO M8 5G 8GB/256GB	0	t	2026-09-24 21:15:49.683564
144	143	https://cdn.tgdd.vn/Products/Images/42/335955/samsung-galaxy-s25-edge-sliver-thumb-600x600.jpg	Điện thoại Samsung Galaxy S25 Edge 5G 12GB/512GB	0	t	2026-09-24 21:15:49.715103
145	144	https://cdn.tgdd.vn/Products/Images/42/361707/realme-16-pro-tim-thumb-600x600.jpg	Điện thoại realme 16 Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:49.746746
146	145	https://cdn.tgdd.vn/Products/Images/42/368875/nothing-phone-4a-pro-hong-thumb-600x600.jpg	Điện thoại Nothing Phone (4a) Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:49.777719
147	146	https://cdn.tgdd.vn/Products/Images/42/332934/oppo-reno13-blue-thumbnew-600x600.jpg	Điện thoại OPPO Reno13 5G 12GB/256GB	0	t	2026-09-24 21:15:49.80844
148	147	https://cdn.tgdd.vn/Products/Images/42/337714/xiaomi-redmi-note-14-pro-5g-tim-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 14 Pro 5G 12GB/512GB	0	t	2026-09-24 21:15:49.842777
149	148	https://cdn.tgdd.vn/Products/Images/42/343066/realme-15-5g-hong-thumb-600x600.jpg	Điện thoại realme 15 5G 12GB/256GB	0	t	2026-09-24 21:15:49.877164
150	149	https://cdn.tgdd.vn/Products/Images/42/367007/honor-600-pro-cam-thumbn-600x600.jpg	Điện thoại Honor 600 Pro 12GB/256GB	0	t	2026-09-24 21:15:49.914301
151	150	https://cdn.tgdd.vn/Products/Images/42/366919/nubia-air-5g-8gb-256gb-den-thumb-600x600.jpg	Điện thoại Nubia Air 5G 8GB/256GB	0	t	2026-09-24 21:15:49.94852
152	151	https://cdn.tgdd.vn/Products/Images/42/335915/samsung-galaxy-a26-5g-hong-thumbn-600x600.jpg	Điện thoại Samsung Galaxy A26 5G 6GB/128GB	0	t	2026-09-24 21:15:49.9819
153	152	https://cdn.tgdd.vn/Products/Images/42/367010/honor-600-cam-thumb-600x600.jpg	Điện thoại Honor 600 8GB/256GB	0	t	2026-09-24 21:15:50.015293
154	153	https://cdn.tgdd.vn/Products/Images/42/344646/xiaomi-15t-pro-12gb-256gb-xam-thumb-600x600.jpg	Điện thoại Xiaomi 15T Pro 5G 12GB/256GB	0	t	2026-09-24 21:15:50.047846
155	154	https://cdn.tgdd.vn/Products/Images/42/367598/oppo-a6t-pro-trang-thumb-1-600x600.jpg	Điện thoại OPPO A6t Pro 8GB/128GB	0	t	2026-09-24 21:15:50.080184
156	155	https://cdn.tgdd.vn/Products/Images/42/362273/xiaomi-poco-f8-pro-12-512gb-xanh-thumb-1-600x600.jpg	Điện thoại Xiaomi POCO F8 Pro 12GB/512GB	0	t	2026-09-24 21:15:50.112703
157	156	https://cdn.tgdd.vn/Products/Images/42/362274/xiaomi-poco-x7-pro-12-256gb-xanh-thumb-600x600.jpg	Điện thoại Xiaomi POCO X7 Pro 12GB/512GB	0	t	2026-09-24 21:15:50.144747
158	157	https://cdn.tgdd.vn/Products/Images/42/370849/xiaomi-poco-f9-ultra-5g-do-thumb-600x600.jpg	Điện thoại Xiaomi POCO F9 Ultra 5G 12GB/256GB	0	t	2026-09-24 21:15:50.176399
159	158	https://cdn.tgdd.vn/Products/Images/42/317981/oppo-find-n3-flip-hong-thumb-1-600x600.jpg	Điện thoại OPPO Find N3 Flip 5G 12GB/256GB Hồng	0	t	2026-09-24 21:15:50.208267
160	159	https://cdn.tgdd.vn/Products/Images/42/364791/oppo-find-x9-ultra-den-thumb-600x600.jpg	Điện thoại OPPO Find X9 Ultra 12GB/512GB	0	t	2026-09-24 21:15:50.242055
161	160	https://cdn.tgdd.vn/Products/Images/42/367617/tecno-pova-curve-2-5g-tim-thumb-600x600.jpg	Điện thoại Tecno Pova Curve 2 5G 8GB/128GB	0	t	2026-09-24 21:15:50.277332
162	161	https://cdn.tgdd.vn/Products/Images/42/265311/masstel-izi-10-4g-thumb-1-600x600.jpg	Điện thoại Masstel IZI 10	0	t	2026-09-24 21:15:50.31197
163	162	https://cdn.tgdd.vn/Products/Images/42/334404/oppo-a5-pro-hong-thumb-1-600x600.jpg	Điện thoại OPPO A5 Pro 5G 8GB/256GB	0	t	2026-09-24 21:15:50.347872
164	163	https://cdn.tgdd.vn/Products/Images/42/363258/oppo-find-n6-xam-thumb-1-2-600x600.jpg	Điện thoại OPPO Find N6 5G 16GB/512GB	0	t	2026-09-24 21:15:50.382532
165	164	https://cdn.tgdd.vn/Products/Images/42/363410/honor-x5c-4gb-64gb-den-thumb-600x600.jpg	Điện thoại HONOR X5c 4GB/64GB	0	t	2026-09-24 21:15:50.418106
166	165	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/368098/honor-600-pro-molly-12gb-512gb-thumb-639165315982574935-600x600.jpg	Điện thoại Honor 600 Pro Molly 12GB/512GB Vàng Trắng	0	t	2026-09-24 21:15:50.453083
167	166	https://cdn.tgdd.vn/Products/Images/42/336623/realme-14-5g-xam-thumb-600x600.jpg	Điện thoại realme 14 5G 12GB/256GB	0	t	2026-09-24 21:15:50.494115
168	167	https://cdn.tgdd.vn/Products/Images/42/338736/samsung-galaxy-z-flip7-black-thumb-600x600.jpg	Điện thoại Samsung Galaxy Z Flip7 5G 12GB/256GB	0	t	2026-09-24 21:15:50.539148
169	876	https://cdn.tgdd.vn/Products/Images/42/337713/xiaomi-redmi-note-14-pro-plus-xanh-thumb-600x600.jpg	Điện thoại Xiaomi Redmi Note 14 Pro+ 5G 12GB/512GB	0	t	2026-09-24 21:15:50.58752
170	168	https://cdn.tgdd.vn/Products/Images/42/368231/tecno-spark-50-pro-xam-thumb-600x600.jpg	Điện thoại Tecno Spark 50 Pro 4GB/128GB	0	t	2026-09-24 21:15:50.634532
171	169	https://cdn.tgdd.vn/Products/Images/42/371029/nothing-phone-4b-blue-thumb-600x600.jpg	Điện thoại Nothing Phone (4b) 5G 8GB/128GB	0	t	2026-09-24 21:15:50.671981
172	170	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371119/vivo-v80-lite-6gb-128gb-080926-103845-290-600x600.jpg	Điện thoại vivo V80 Lite 5G 6GB/128GB	0	t	2026-09-24 21:15:50.706073
173	171	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371120/vivo-v80-lite-6gb-256gb-080926-103930-811-600x600.jpg	Điện thoại vivo V80 Lite 5G 6GB/256GB	0	t	2026-09-24 21:15:50.743077
174	172	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371195/oppo-a7-pro-5g-8gb-128gb-100926-115537-175-600x600.jpg	Điện thoại OPPO A7 Pro 5G 8GB/128GB	0	t	2026-09-24 21:15:50.774699
175	173	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371196/oppo-a7-pro-5g-6gb-256gb-100926-115519-969-600x600.jpg	Điện thoại OPPO A7 Pro 5G 6GB/256GB	0	t	2026-09-24 21:15:50.805076
176	174	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371197/oppo-a7-pro-5g-8gb-256gb-100926-115509-906-600x600.jpg	Điện thoại OPPO A7 Pro 5G 8GB/256GB	0	t	2026-09-24 21:15:50.836746
177	175	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371315/oppo-find-x10-pro-max-12gb-512gb-120926-123718-160-600x600.png	Điện thoại OPPO Find X10 Pro Max 5G 12GB/512GB	0	t	2026-09-24 21:15:50.869148
178	176	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371316/oppo-find-x10-12gb-512gb-120926-123807-395-600x600.png	Điện thoại OPPO Find X10 5G 12GB/512GB	0	t	2026-09-24 21:15:50.903517
179	177	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/42/371317/oppo-find-x10-12gb-256gb-120926-123831-822-600x600.png	Điện thoại OPPO Find X10 5G 12GB/256GB	0	t	2026-09-24 21:15:50.938603
180	178	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361311/hp-15-fc0023au-r5-7520u-d0bh1pa-thumb-639030592238863081-600x600.jpg	Laptop HP 15 fc0023AU - D0BH1PA (R5 7520U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:50.973742
181	179	https://cdn.tgdd.vn/Products/Images/44/363537/macbook-neo-13-inch-a18-pro-8gb-256gb-xanh-duong-600x600.jpg	Laptop MacBook Neo 13 inch A18 Pro 8GB/256GB	0	t	2026-09-24 21:15:51.019164
182	180	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358132/asus-vivobook-s14-s3407va-core-5-210h-ly146w-thumb-638965583427975379-600x600.jpg	Laptop Asus Vivobook S14 S3407VA - LY146W (Core 5 210H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.054216
183	181	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368556/dell-15-dc15255-r5-7530u-dc5r5973w1-2y-thumb-639180726788114595-600x600.jpg	Laptop Dell 15 DC15255 - DC5R5973W1-2Y (R5 7530U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:51.087288
184	182	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367016/lenovo-ideapad-slim-3-15iph11-ultra-5-322-83ur00a4vn-thumb-639144382848821483-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR00A4VN (Ultra 5 322, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.118603
185	183	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/355729/hp-240r-g10-core-5-120u-c3ru7at-thumb01-639144570820332386-600x600.jpg	Laptop HP 240R G10 - C3RU7AT (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.149664
186	184	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362025/dell-15-dc15250-i5-1334u-dc5i5897w1-thumb-639046899814234946-600x600.jpg	Laptop Dell 15 DC15250 - DC5I5897W1 (i5 1334U, 16GB, 512GB, Full HD+ 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:51.180739
187	185	https://cdn.tgdd.vn/Products/Images/44/363487/macbook-air-13-inch-m5-16gb-512gb-70w-xanh-da-troi-1-600x600.jpg	Laptop MacBook Air 13 inch M5 16GB/512GB/8GPU 70W	0	t	2026-09-24 21:15:51.213329
188	186	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369212/msi-modern-15-f1mg-1264vn-core-5-120u-thumb-639192846390358481-600x600.jpg	Laptop MSI Modern 15 F1MG-1264VN (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.244584
189	187	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364397/asus-vivobook-16-a1607qa-x1-26-100-mb067w-thumb-2-639155694743786038-600x600.jpg	Laptop Asus Vivobook 16 A1607QA - MB067W (X1 26 100, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.276615
190	193	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366746/lenovo-ideapad-slim-3-15arp10-r5-7535hs-83k700yuvn-thumb-639135011549694205-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15ARP10 - 83K700YUVN (R5 7535HS, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.311634
191	194	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367376/acer-aspire-lite-15-al15-49p-r6xx-r5-7430u-nx-drzsv-002-thumb-639167109436766259-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-49P-R6XX - NX.DRZSV.002 (R5 7430U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.345625
192	195	https://cdn.tgdd.vn/Products/Images/44/363507/macbook-air-15-inch-m5-16gb-512gb-70w-xanh-den-600x600.jpg	Laptop MacBook Air 15 inch M5 16GB/512GB 70W	0	t	2026-09-24 21:15:51.382506
193	196	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342941/msi-modern-15-f13mg-i5-1334u-667vn-16gb-thumb-2-638981132535627040-600x600.jpg	Laptop MSI Modern 15 F13MG - 667VN_16GB (i5 1334U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.418473
194	197	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360420/asus-vivobook-15-x1504va-core-7-150u-bq295w-thumb-639016922618560744-600x600.jpg	Laptop Asus Vivobook 15 X1504VA - BQ295W (Core 7 150U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.454382
195	198	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358078/hp-omnibook-5-flip-14-fp0057tu-core-5-120u-bz7q6pa-thumb-2-639029106959306955-600x600.jpg	Laptop HP OmniBook 5 Flip 14 fp0057TU - BZ7Q6PA (Core 5 120U, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	0	t	2026-09-24 21:15:51.488357
196	199	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/338204/hp-victus-15-fa2731tx-i5-b85lnpa-thumb-638833321610397298-600x600.jpg	Laptop HP Gaming VICTUS 15 fa2731TX - B85LNPA (i5 13420H, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:51.522723
197	200	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362620/asus-vivobook-go-15-e1504fa-r5-40-bq374w-thumb-2-639058215406164553-600x600.jpg	Laptop Asus Vivobook Go 15 E1504FA - BQ374W (R5 40, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.557211
654	742	https://cdn.tgdd.vn/Products/Images/7077/329834/redmi-watch-5-active-den-thb-600x600.jpg	Xiaomi Redmi Watch 5 Active 49.1mm dây TPU	0	t	2026-09-24 21:16:06.709456
198	201	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/357990/dell-15-dc15250-i5-1334u-cph99-thumb-638967241287160500-600x600.jpg	Laptop Dell 15 DC15250 - CPH99 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:51.590341
199	202	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362621/asus-tuf-gaming-fa506ncq-r7-170-hn005w-thumb-639059676707947017-600x600.jpg	Laptop Asus TUF Gaming FA506NCQ - HN005W (R7 170, 16GB, 512GB, RTX 3050 4GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:51.622217
200	203	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363903/lenovo-gaming-loq-15arp10-r7-170-83s000cnvn-thumb-639088378609730153-600x600.jpg	Laptop Lenovo Gaming LOQ 15ARP10 - 83S000CNVN (R7 170, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:51.65439
201	204	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366090/acer-aspire-go-15-ag15-52p-52wt-ultra-5-115u-nx-jwksv-001-thumb-639124658569510271-600x600.jpg	Laptop Acer Aspire Go 15 AG15-52P-52WT - NX.JWKSV.001 (Ultra 5 115U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.68605
202	205	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368211/lenovo-ideapad-slim-3-15iwc11-core-5-320-83rr00cvvn-thumb-639177374828961305-600x600.jpg	Laptop Lenovo Ideapad Slim 3 15IWC11 - 83RR00CVVN (Core 5 320, 8GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.716825
203	206	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341267/hp-omnibook-5-ai-16-af1048tu-ultra-5-225u-bz7q9pa-thumb-3-638978666563774802-600x600.jpg	Laptop HP OmniBook 5 AI 16 af1048TU - BZ7Q9PA (Ultra 5 225U, 16GB, 512GB, WUXGA, OfficeH24, Win11)	0	t	2026-09-24 21:15:51.749443
204	207	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360690/dell-14-dc14250-core-5-120u-dc4c5386w-thumb-639027406591081421-600x600.jpg	Laptop Dell 14 DC14250 - DC4C5386W (Core 5 120U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:51.78193
205	208	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368656/acer-gaming-aspire-7-a715-59g-59rd-core-5-210h-nh-dxusv-001-thumb-639186880778742734-600x600.jpg	Laptop Acer Gaming Aspire 7 A715-59G-59RD - NH.DXUSV.001 (Core 5 210H, 16GB, 512GB, RTX 3050 4GB, Fulll HD 144Hz, Win11)	0	t	2026-09-24 21:15:51.813945
206	209	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365310/dell-15-dc15250-i5-1334u-dc15250-i5u165w11slu-27-thumb-639116750273813116-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU-27 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:51.846414
207	210	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362398/hp-15-fd1486tu-ultra-5-125h-d0bh3pa-thumb-639053708522941531-600x600.jpg	Laptop HP 15 fd1486TU - D0BH3PA (Ultra 5 125H, 24GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.878596
208	211	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368210/lenovo-ideapad-slim-3-15iwc11-core-5-320-83rr00aavn-thumb-639177342871449623-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15IWC11 - 83RR00AAVN (Core 5 320, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:51.912131
209	212	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368192/singpc-m16-i595-i5-1235u-100826-102009-960-600x600.jpg	Laptop SingPC M16-i595 (i5 1235U, 16GB, 512GB, WUXGA, Win11 Pro)	0	t	2026-09-24 21:15:51.945898
210	213	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366087/acer-aspire-lite-15-al15-21p-r91w-r5-40-nx-dnrsv-002-thumb-639124668970737654-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-21P-R91W - NX.DNRSV.002 (R5 40, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:51.980573
211	214	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369645/acer-nitro-propanel-anv15-52-50rb-core-5-210h-nh-quasv-002-thumb-639200108930160685-600x600.jpg	Laptop Acer Nitro ProPanel ANV15-52-50RB - NH.QUASV.002 (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:15:52.014376
212	215	https://cdn.tgdd.vn/Products/Images/44/363492/macbook-pro-16-inch-m5-pro-48gb-1tb-bac-thumb-600x600.jpg	Laptop MacBook Pro 16 inch M5 Pro 48GB/1TB	0	t	2026-09-24 21:15:52.047607
213	216	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360295/acer-gaming-predator-helios-neo-16-ai-phn16-73-757w-nh-qvqsv-001-thumb-639011583719481626-600x600.jpg	Laptop Acer Gaming Predator Helios Neo 16 AI PHN16 73 757W - NH.QVQSV.001 (Ultra 7 255HX, 32GB, 1TB, RTX 5060 8GB, 2K+ 240Hz, Win11)	0	t	2026-09-24 21:15:52.078412
214	217	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362024/dell-pro-14-essential-pv14250-core-5-120u-pv14250-120u-16512w-bl-thumb-639046727854987975-600x600.jpg	Laptop Dell Pro 14 Essential PV14250 - PV14250-120U-16512W-BL (Core 5 120U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:52.108767
215	218	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362410/asus-vivobook-14-x1407ca-ultra-5-225h-ly008w-thumb-639057861785762557-600x600.jpg	Laptop Asus Vivobook 14 X1407CA - LY008W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:52.140492
216	219	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341623/hp-victus-15-fb3116ax-r7-7445hs-bx8u4pa-thumb01-638899050739008488-600x600.jpg	Laptop HP Gaming VICTUS 15 fb3116AX - BX8U4PA (R7 7445HS, 16GB, 512GB, RTX3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:52.172082
217	220	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368208/lenovo-ideapad-slim-3-14iwc11-core-5-320-83rq002pvn-thumb-639172171948435449-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 14IWC11 - 83RQ002PVN (Core 5 320, 8GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:52.205869
218	221	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362406/asus-vivobook-go-15-e1504fa-r5-40-bq350w-thumb-639057642463867778-600x600.jpg	Laptop Asus Vivobook Go 15 E1504FA - BQ350W (R5 40, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.239177
219	222	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340562/dell-15-dc15250-i7-dc5i7748w1-638900114799182560-600x600.jpg	Laptop Dell 15 DC15250 - DC5I7748W1 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:52.273335
220	223	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340484/hp-probook-455-g10-r5-b8pg7at-638900119673112884-600x600.jpg	Laptop HP Probook 455 G10 - B8PG7AT (R5 7530U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.303748
221	224	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366745/lenovo-ideapad-slim-3-14arp10-r5-7535hs-83k600e6vn-thumb-639140579943537651-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 14ARP10 - 83K600E6VN (R5 7535HS, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:52.334357
222	225	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342758/asus-vivobook-s16-s3607va-core-5-210h-rp155ws-thumb-638919139295786834-600x600.jpg	Laptop Asus Vivobook S16 S3607VA - RP155WS (Core 5 210H, 16GB, 512GB, WUXGA 144Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:52.367698
223	226	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366823/acer-gaming-nitro-propanel-anv16-72-71t9-core-7-240h-nh-qunsv-001-thumb-639141099405095592-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV16-72-71T9 - NH.QUNSV.001 (Core 7 240H, 16GB, 512GB, RTX 5060 8GB, WUXGA 180Hz, Win11)	0	t	2026-09-24 21:15:52.40183
224	227	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/335554/lenovo-loq-15irx9-i5-83dv003cvn-638828191874856424-600x600.jpg	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV003CVN (i5 13450HX, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:52.435552
225	228	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337044/hp-pavilion-16-af0055tu-ultra-5-ay8c4pa-thumb-638828179749624956-600x600.jpg	Laptop HP Pavilion 16 af0055TU - AY8C4PA (Ultra 5 125U, 16GB, 512GB, WUXGA, OfficeHS+365, Win11)	0	t	2026-09-24 21:15:52.469028
226	229	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366086/acer-aspire-lite-15-al15-46p-r73c-r3-5400u-nx-jxmsv-001-thumb-639125386869694783-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-46P-R73C - NX.JXMSV.001 (R3 5400U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.508527
227	230	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340561/dell-15-dc15250-i5-dc5i5357w1-638900114676211077-600x600.jpg	Laptop Dell 15 DC15250 - DC5I5357W1 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:52.545315
228	231	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339682/dell-inspiron-14-5441-x1p-64-100-5mnk1-638895760313087821-600x600.jpg	Laptop Dell Inspiron 14 5441 - 5MNK1 (X1P 64 100, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:52.583324
229	232	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341624/hp-victus-15-fb3115ax-r7-7445hs-bx9c9pa-thumb-638899046284890217-600x600.jpg	Laptop HP Gaming VICTUS 15 fb3115AX - BX9C9PA (R7 7445HS, 16GB, 512GB, RTX4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:52.622597
230	233	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342524/lenovo-gaming-loq-15irx9-i5-83dv01anvn-thumb-638913705000350912-600x600.jpg	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01ANVN (i5 13450HX, 16GB, 1TB, RTX 3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:52.659897
231	234	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341607/acer-nitro-v-15-propanel-anv15-52-72bm-i7-13620h-nh-qz9sv-004-thumb-638899014448776181-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV15 52 72BM - NH.QZ9SV.004 (i7 13620H, 16GB, 512GB, RTX5050 8GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:15:52.696365
232	235	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360298/acer-gaming-nitro-16s-ai-propanel-an16s-61-r193-r9-ai-365-nh-qxtsv-001-thumb-3-639020844558265192-600x600.jpg	Laptop Acer Gaming Nitro ProPanel AN16S 61 R193 - NH.QXTSV.001 (R9 AI 365, 16GB, 512GB, RTX 5070 8GB, 2K+ 180Hz, Win11)	0	t	2026-09-24 21:15:52.73314
233	236	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368655/acer-aspire-lite-16-al16-71p-582q-ultra-5-125h-nx-drssv-001-thumb-639185397036068917-600x600.jpg	Laptop Acer Aspire Lite 16 AL16-71P-582Q - NX.DRSSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+ 120Hz, Win11)	0	t	2026-09-24 21:15:52.768211
234	253	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363690/msi-gaming-cyborg-15-a13uc-i5-13420h-2088vn-thumb-639086610086503435-600x600.jpg	Laptop MSI Gaming Cyborg 15 A13UC - 2088VN (i5 13420H, 16GB, 512GB, RTX 3050 4GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:52.803194
235	254	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/325691/hp-15-fd0015tu-i7-a19c5pa-170225-110311-023-600x600.jpg	Laptop HP 15 fd0015TU - A19C5PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.842726
236	255	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358797/hp-15-fd1288tu-ultra-7-155h-c2cv7pa-thumb-638985447995317468-600x600.jpg	Laptop HP 15 fd1288TU - C2CV7PA (Ultra 7 155H, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.876585
237	256	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341602/acer-aspire-lite-16-ai-al16-71p-5674-ultra-5-125h-nx-d4xsv-001-thumb3-638906173311263825-600x600.jpg	Laptop Acer Aspire Lite 16 AI AL16 71P 5674 - NX.D4XSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+, Win11)	0	t	2026-09-24 21:15:52.907548
238	257	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359486/lenovo-ideapad-slim-5-oled-14akp10-r5-330-83hx00b2vn-thumb-638999407712643452-600x600.jpg	Laptop Lenovo Ideapad Slim 5 OLED 14AKP10 - 83HX00B2VN (R5 330, 16GB, 1TB, WUXGA, Win11)	0	t	2026-09-24 21:15:52.939329
239	258	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367575/hp-15-fd2127tu-ultra-5-225u-d72ccpa-thumb-639155613634793350-600x600.jpg	Laptop HP 15 fd2127TU - D72CCPA (Ultra 5 225U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:52.969593
240	259	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360691/dell-15-dc15250-i5-1334u-71084746-thumb-639027872992844662-600x600.jpg	Laptop Dell 15 DC15250 - 71084746 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:53.001012
241	260	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362020/dell-15-dc15250-i7-1355u-cph991-thumb-639043732789508738-600x600.jpg	Laptop Dell 15 DC15250 - CPH991 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:53.035063
242	261	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365608/hp-240r-g9-core-5-120u-c40lgat-thumb-639120162416102265-600x600.jpg	Laptop HP 240R G9 - C40LGAT (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.069488
243	262	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366741/lenovo-ideapad-slim-5-oled-14iph11-ultra-5-322-83s5000dvn-thumb-639140930500825785-600x600.jpg	Laptop Lenovo IdeaPad Slim 5 OLED 14IPH11 - 83S5000DVN (Ultra 5 322, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:53.106954
244	268	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358077/hp-omnibook-5-flip-14-fp0055tu-core-7-150u-bz7q4pa-thumb-2-639029105575189292-600x600.jpg	Laptop HP OmniBook 5 Flip 14 fp0055TU - BZ7Q4PA (Core 7 150U, 24GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	0	t	2026-09-24 21:15:53.140994
245	269	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362667/dell-15-dc15250-i7-1355u-dc15250-i7u161w11slu-5-thumb-639056494584185320-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU-5 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:53.174354
246	270	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368558/dell-15-dc15250-core-3-100u-71100520-thumb-639180736575634714-600x600.jpg	Laptop Dell 15 DC15250 - 71100520 (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:53.206591
247	271	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339211/gigabyte-a16-ga6h-i7-gaming-a16-cmhi2vn893sh-thumb-638850629550378282-600x600.jpg	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CMHI2VN893SH (i7 13620H, 16GB, 512GB, RTX 4050 6GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:53.237734
248	272	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367576/hp-gaming-victus-15-fa2451tx-i5-13420h-d17wppa-thumb-639155621103122690-600x600.jpg	Laptop HP Gaming VICTUS 15 fa2451TX - D17WPPA (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:53.26845
249	273	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/327507/acer-aspire-5-a515-58m-79r7-i7-nxkq8sv007-thumb-638754974350798562-600x600.jpg	Laptop Acer Aspire 5 A515 58M 79R7 - NX.KQ8SV.007 (i7 13620H, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.29927
250	274	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369478/lenovo-ideapad-slim-3-15iwc11-core-3-304-83rr00a8vn-thumb-639197485486262152-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15IWC11 - 83RR00A8VN (Core 3 304, 8GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:53.329582
251	275	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368557/dell-14-dc14250-core-5-120u-dc4c5375w1-2y-thumb-639179831760851046-600x600.jpg	Laptop Dell 14 DC14250 - DC4C5375W1-2Y (Core 5 120U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:53.36025
252	276	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/351613/dell-15-dc15250-i7-1355u-dc15250-i7u161w11slu-thumb-638938737336707122-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:53.394571
253	277	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363260/acer-nitro-v-15-anv15-41-r0y4-r7-7735hs-nh-qpesv-004-thumb-639088183534262922-600x600.jpg	Laptop Acer Nitro ProPanel ANV15-41-R0Y4 - NH.QPESV.004 (R7 7735HS, 16GB, 512GB, RTX 4050 6GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:15:53.424751
254	278	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368207/lenovo-ideapad-slim-3-14iwc11-core-5-320-83rq002nvn-160626-023051-594-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 14IWC11 - 83RQ002NVN (Core 5 320, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:53.455501
255	279	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341618/hp-15-fd0235tu-core-5-120u-9q970pa-120u-thumb-638899119010495035-600x600.jpg	Laptop HP 15 fd0235TU - 9Q970PA_120U (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.487783
256	280	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368654/acer-aspire-lite-15-al15-36p-30tn-core-3-n350-nx-ddasv-001-thumb-639185374358313282-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-36P-30TN - NX.DDASV.001 (Core 3 N350, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.531159
257	281	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367574/hp-15-fd2126tu-ultra-5-225u-d72cbpa-thumb-639155605117543692-600x600.jpg	Laptop HP 15 fd2126TU - D72CBPA (Ultra 5 225U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.573446
258	282	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339688/asus-gaming-v16-v3607vu-core-5-rp192w-638900134315949894-600x600.jpg	Laptop Asus Gaming V16 V3607VU - RP192W (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, WUXGA 144Hz, Win11)	0	t	2026-09-24 21:15:53.605521
259	283	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364860/dell-15-dc15250-i7-1355u-71092480-thumb-639106635920736946-600x600.jpg	Laptop Dell 15 DC15250 - 71092480 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365B, Win11)	0	t	2026-09-24 21:15:53.635361
260	284	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358080/hp-omnibook-x-flip-14-fm0088tu-ultra-5-226v-bz7q2pa-thumb-2-639029109506758395-600x600.jpg	Laptop HP OmniBook X Flip 14 fm0088TU - BZ7Q2PA (Ultra 5 226V, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:53.665263
261	285	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/357992/dell-15-dc15255-r7-7730u-x9ym41-thumb-638965540892499877-600x600.jpg	Laptop Dell 15 DC15255 - X9YM41 (R7 7730U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:53.695766
262	286	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340006/msi-venture-a15-ai-a2hmg-r7-003vn-638895766508733267-600x600.jpg	Laptop MSI Venture A15 AI A2HMG - 003VN (R7 260, 16GB, 512GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:53.727405
263	287	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/318354/acer-nitro-v-anv15-51-57b2-i5-nhqn8sv001-thumb-638754901224650221-600x600.jpg	Laptop Acer Gaming Nitro V ANV15 51 57B2 - NH.QN8SV.001 (i5 13420H, 8GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:53.759027
264	288	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365998/dell-15-dc15250-i5-1334u-cph992-thumb-639122912864506023-600x600.jpg	Laptop Dell 15 DC15250 - CPH992 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeHS24+365B, Win11)	0	t	2026-09-24 21:15:53.791796
265	289	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362666/dell-15-dc15250-i5-1334u-dc15250-i5u165w11slu-5-thumb-639056487911465667-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU-5 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:53.825202
266	290	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337843/gigabyte-a16-ga6h-i5-gaminga16cmhh2vn893sh-638828331771830623-600x600.jpg	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CMHH2VN893SH (i5 13420H, 16GB, 512GB, RTX 4050 6GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:53.856212
267	291	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358498/dell-15-dc15250-i5-1334u-71071928-thumb-2-638975019260439384-600x600.jpg	Laptop Dell 15 DC15250 - 71071928 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:53.886209
268	292	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/345510/hp-gaming-victus-15-fa2732tx-i5-13420h-b85lppa-thumb-638936205435701330-600x600.jpg	Laptop HP Gaming VICTUS 15 fa2732TX - B85LPPA (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:53.916308
269	293	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358074/hp-omnibook-5-ai-16-af1054tu-ultra-7-255u-c1mn8pa-thumb-638965554761304592-600x600.jpg	Laptop HP OmniBook 5 AI 16 af1054TU - C1MN8PA (Ultra 7 255U, 32GB, 512GB, WUXGA, OfficeH24, Win11)	0	t	2026-09-24 21:15:53.945991
270	294	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/311176/hp-pavilion-15-eg3093tu-i5-8c5l4pa-170225-103330-013-600x600.jpg	Laptop HP Pavilion 15 eg3093TU - 8C5L4PA (i5 1335U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:53.976196
271	295	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341610/msi-katana-15-hx-b14wfk-i7-14650hx-025vn-thumb-638899039749026960-600x600.jpg	Laptop MSI Gaming Katana 15 HX B14WFK - 025VN (i7 14650HX, 16GB, 512GB, RTX5060 8GB, QHD 165Hz, Win11)	0	t	2026-09-24 21:15:54.005775
272	296	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/334998/acer-aspire-5-a515-58gm-598j-i5-nxkw1sv002-638854988674476822-600x600.jpg	Laptop Acer Gaming Aspire 5 A515 58GM 598J - NX.KW1SV.002 (i5 13420H, 16GB, 512GB, RTX 2050 4GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:54.035707
273	297	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364401/asus-zenbook-14-ux3405ca-ultra-5-225h-st628w-thumb-3-639159130132430029-600x600.jpg	Laptop Asus ZenBook 14 UX3405CA - ST628W (Ultra 5 225H, 16GB, 512GB, 3K OLED 120Hz, Win11)	0	t	2026-09-24 21:15:54.067313
274	298	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359477/lenovo-gaming-loq-essential-15irx11-i5-13450hx-83sc003svn-thumb-638995960517722304-600x600.jpg	Laptop Lenovo Gaming LOQ Essential 15IRX11 - 83SC003SVN (i5 13450HX, 16GB, 1TB, RTX 5050 8GB, Full HD, 144Hz, Win11)	0	t	2026-09-24 21:15:54.099648
275	299	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337043/hp-pavilion-16-af0054tu-ultra-5-ay8c3pa-thumb01-639144586572964271-600x600.jpg	Laptop HP Pavilion 16 af0054TU - AY8C3PA (Ultra 5 125U, 16GB, 1TB, WUXGA, OfficeHS+365, Win11)	0	t	2026-09-24 21:15:54.132443
276	300	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/334803/dell-inspiron-15-3530-i5-n5i5530w1-thumb-638762534676491196-600x600.jpg	Laptop Dell Inspiron 15 3530 - N5I5530W1 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:54.163643
277	301	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339951/msi-venture-a14-ai-a3hmg-r5-ai-340-004vn-638895763373785370-600x600.jpg	Laptop MSI Venture A14 AI+ A3HMG - 004VN (R5 AI 340, 16GB, 512GB, 2.8K OLED 120Hz, Win11)	0	t	2026-09-24 21:15:54.19562
278	302	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341270/hp-15-fd0234tu-core-5-120u-9q969pa-120u-230725-094819-122-600x600.jpg	Laptop HP 15 fd0234TU - 9Q969PA-120U (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.226954
279	303	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/333424/acer-aspire-16-ai-a16-71m-59l5-ultra-5-nxj4ysv001-638774738263890291-600x600.jpg	Laptop Acer Aspire 16 AI A16 71M 59L5 - NX.J4YSV.001 (Ultra 5 125H, 16GB, 512GB, Full HD+, Win11)	0	t	2026-09-24 21:15:54.258615
280	304	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341611/msi-katana-15-hx-b14wek-i5-14450hx-286vn-thumb-638899040893897338-600x600.jpg	Laptop MSI Gaming Katana 15 HX B14WEK - 286VN (i5 14450HX, 16GB, 512GB, RTX5050 8GB, QHD 165Hz, Win11)	0	t	2026-09-24 21:15:54.288398
281	305	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360417/asus-vivobook-14-x1404va-core-5-120u-eb260w-thumb-2-639034834675227961-600x600.jpg	Laptop Asus Vivobook 14 X1404VA - EB260W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.319416
282	306	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368559/dell-14-dc14250-core-7-150u-71100515-thumb-639180752442220668-600x600.jpg	Laptop Dell 14 DC14250 - 71100515 (Core 7 150U, 16GB, 512GB, Full HD+ 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:54.35023
283	307	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364405/asus-zenbook-14-ux3405ca-ultra-9-285h-st648w-thumb-2-639159131503564651-600x600.jpg	Laptop Asus Zenbook 14 UX3405CA - ST648W (Ultra 9 285H, 32GB, 1TB, 3K OLED 120Hz, Win11)	0	t	2026-09-24 21:15:54.381043
284	308	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358120/hp-omnibook-5-16-ag1069au-r5-ai-340-bz7t1pa-thumb-638966581010858825-600x600.jpg	Laptop HP OmniBook 5 16 ag1069AU - BZ7T1PA (R5 AI 340, 16GB, 512GB, WUXGA, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:54.411244
285	309	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342754/dell-15-dc15250-i5-1334u-dc15250-i5u165w11slu-thumb-638919949640513466-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i5U165W11SLU (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:54.442429
286	310	https://cdn.tgdd.vn/Products/Images/44/358089/macbook-pro-14-inch-m5-24gb-1tb-den-600x600.jpg	Laptop MacBook Pro 14 inch M5 24GB/1TB	0	t	2026-09-24 21:15:54.472958
287	311	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366702/hp-probook-4-g1ah-16-r5-220-c40jpat-thumb-639137433279860816-600x600.jpg	Laptop HP Probook 4 G1ah 16 - C40JPAT (R5 220, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:54.502763
288	312	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365535/lenovo-thinkpad-e14-gen-7-ultra-5-135h-21sx00bnvn-thumb-639117002918581595-600x600.jpg	Laptop Lenovo ThinkPad E14 Gen 7 - 21SX00BNVN (Ultra 5 135H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:54.535195
289	313	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362922/msi-gaming-cyborg-15-b13wfkg-i7-13620h-658vn-thumb-639062292691735674-600x600.jpg	Laptop MSI Gaming Cyborg 15 B13WFKG - 658VN (i7 13620H, 16GB, 1TB, RTX 5060 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:54.566661
290	314	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360419/asus-vivobook-15-x1504va-core-5-120u-bq285w-thumb-639016919628384365-600x600.jpg	Laptop Asus Vivobook 15 X1504VA - BQ285W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.598473
291	315	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369481/dell-15-dc15250-core-3-100u-cph993-thumb-639198083198949080-600x600.jpg	Laptop Dell 15 DC15250 - CPH993 (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:54.6286
292	316	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365607/hp-240r-g9-core-5-120u-c40m2at-thumb-639120152215605601-600x600.jpg	Laptop HP 240R G9 - C40M2AT (Core 5 120U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.659679
293	317	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364967/dell-14-dc14250-core-7-150u-f0ftk7-thumb-639108994209154096-600x600.jpg	Laptop Dell 14 DC14250 - F0FTK7 (Core 7 150U, 16GB, 512GB, MX570A 2GB, Full HD+, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:54.690992
294	318	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363259/acer-nitro-lite-nl16-71g-71fn-i7-13620h-nh-d5asv-003-thumb-639087644608016515-600x600.jpg	Laptop Acer Nitro Lite NL16-71G-71FN - NH.D5ASV.003 (i7 13620H, 16GB, 512GB, RTX 4050 6GB, FHD+ 180Hz, Win11)	0	t	2026-09-24 21:15:54.72375
295	319	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366909/msi-gaming-cyborg-15-black-edition-a13ve-i5-13420h-a13ve-2410vn-thumb-639136628486020710-600x600.jpg	Laptop MSI Gaming Cyborg 15 Black Edition A13VE - A13VE-2410VN (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:54.757677
296	320	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/334796/asus-zenbook-14-ux3405ca-ultra-5-pz187ws-638774629870118990-600x600.jpg	Laptop Asus Zenbook 14 UX3405CA - PZ187WS (Ultra 5 225H, 16GB, 512GB, 2.8K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:54.791191
297	321	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358796/hp-14-ep1137tu-ultra-7-155h-c2cy8pa-thumb-638984649413954000-600x600.jpg	Laptop HP 14 ep1137TU - C2CY8PA (Ultra 7 155H, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.823124
298	322	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363882/lenovo-ideapad-slim-5-oled-14agp11-r7-445-83s1003fvn-thumb-639095713830474357-600x600.jpg	Laptop Lenovo Ideapad Slim 5 OLED 14AGP11 - 83S1003FVN (R7 445, 32GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:54.85574
299	323	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/327406/hp-probook-450-g10-i5-9h1n5pt-170225-110838-638-600x600.jpg	Laptop HP Probook 450 G10 - 9H1N5PT (i5 1335U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.890711
300	324	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363262/acer-aspire-go-15-ag15-72p-76a2-core-7-150u-nx-jrrsv-008-thumb-639086987700838832-600x600.jpg	Laptop Acer Aspire Go 15 AG15-72P-76A2 - NX.JRRSV.008 (Core 7 150U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.925753
301	325	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341751/gigabyte-a16-ga6h-i5-13420h-gaming-a16-cthh3vn893sh-thumb-638899248610359665-600x600.jpg	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CTHH3VN893SH (i5 13420H, 16GB, 512GB, RTX5050 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:54.958481
302	326	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362399/hp-15-fd1490tu-ultra-7-155h-d0bh5pa-thumb-639053713836400288-600x600.jpg	Laptop HP 15 fd1490TU - D0BH5PA (Ultra 7 155H, 24GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:54.991292
303	327	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365311/dell-15-dc15250-i7-1355u-dc15250-i7u161w11slu-27-thumb-639116760361084649-600x600.jpg	Laptop Dell 15 DC15250 - DC15250-i7U161W11SLU-27 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:55.024329
304	328	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339681/dell-inspiron-15-3530-i5-p16wd22-638895760085626264-600x600.jpg	Laptop Dell Inspiron 15 3530 - P16WD22 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.055715
305	329	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363901/lenovo-thinkbook-16-g9-irl-i5-13420h-21us008fvn-thumb-639092687424410855-600x600.jpg	Laptop Lenovo ThinkBook 16 G9 IRL - 21US008FVN (i5 13420H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:55.086407
306	330	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340560/dell-inspiron-15-3530-i5-71070372-638900114510203065-600x600.jpg	Laptop Dell Inspiron 15 3530 - 71070372 (i5 1334U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.117489
307	331	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358079/hp-15-fc0655au-r5-7430u-c81ngpa-thumb-3-639015630581578905-600x600.jpg	Laptop HP 15 fc0655AU - C81NGPA (R5 7430U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:55.148624
308	332	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341609/msi-katana-15-hx-b14wfk-i7-14650hx-267vn-thumb-638899038470195527-600x600.jpg	Laptop MSI Gaming Katana 15 HX B14WFK - 267VN (i7 14650HX, 32GB, 512GB, RTX 5060 8GB, QHD 165Hz, Win11)	0	t	2026-09-24 21:15:55.179029
309	333	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341613/msi-katana-15-hx-b14wek-i7-14650hx-027vn-thumb02-638897277627603509-600x600.jpg	Laptop MSI Gaming Katana 15 HX B14WEK - 027VN (i7 14650HX, 32GB, 512GB, RTX5050 8GB, QHD 165Hz, Win11)	0	t	2026-09-24 21:15:55.210334
310	334	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360421/asus-gaming-v16-v3607vj-core-5-210h-rp071w-thumb-639017543008290379-600x600.jpg	Laptop Asus Gaming V16 V3607VJ - RP071W (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WUXGA 144Hz, Win11)	0	t	2026-09-24 21:15:55.241088
311	335	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/344606/lenovo-gaming-loq-15irx9-i7-13650hx-83dv01alvn-thumb-638930572989400263-600x600.jpg	Laptop Lenovo Gaming LOQ 15IRX9 - 83DV01ALVN (i7 13650HX, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:55.273516
312	336	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360693/dell-gaming-alienware-16-aurora-ac16250-core-7-240h-c7h161w11ii4050-thumb-639023469880245139-600x600.jpg	Laptop Dell Gaming Alienware 16 Aurora AC16250 - C7H161W11II4050 (Core 7 240H, 16GB, 1TB, RTX 4050 6GB, WQXGA 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.305006
313	337	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358499/dell-15-dc15250-i7-1355u-71073959-thumb-2-638975018038148792-600x600.jpg	Laptop Dell 15 DC15250 - 71073959 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:55.336881
314	348	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364399/asus-vivobook-s16-m3607ga-r7-ai-445-sh034w-thumb-2-639159132817621619-600x600.jpg	Laptop Asus Vivobook S16 M3607GA - SH034W (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:55.369528
315	349	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/322099/msi-gaming-cyborg-15-ai-a1vek-ultra-7-053vn-thumb-638763544027585458-600x600.jpg	Laptop MSI Gaming Cyborg 15 AI A1VEK - 053VN (Ultra 7 155H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:55.402865
316	350	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367378/acer-swift-go-14-ai-sfg14-i71-70rp-ultra-7-358h-nx-jzhsv-003-thumb-639161667227431984-600x600.jpg	Laptop Acer Swift Go 14 AI SFG14-I71-70RP - NX.JZHSV.003 (Ultra 7 358H, 32GB, 1TB, 2.8K 120Hz, Win11)	0	t	2026-09-24 21:15:55.43691
317	351	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341563/dell-alienware-16-aurora-ac16250-core-5-210h-71072939-thumb2-638939737088688848-600x600.jpg	Laptop Dell Gaming Alienware 16 Aurora AC16250 - 71072939 (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WQXGA 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.471686
318	352	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/338321/asus-s3407ca-ultra-7-ly096ws-638900879294934441-600x600.jpg	Laptop Asus Vivobook S14 S3407CA - LY096WS (Ultra 7 255H, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.505578
319	353	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/311177/hp-pavilion-15-eg3091tu-i7-8c5l2pa-170225-103417-620-600x600.jpg	Laptop HP Pavilion 15 eg3091TU - 8C5L2PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:55.539881
320	354	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339201/acer-nitro-lite-16-nl16-71g-71uj-i7-nhd59sv002-638854987194644024-600x600.jpg	Laptop Acer Gaming Nitro Lite 16 NL16 71G 71UJ - NH.D59SV.002 (i7 13620H, 16GB, 512GB, RTX 3050 6GB, Full HD+ 165Hz, Win11)	0	t	2026-09-24 21:15:55.577774
321	355	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366703/hp-omnibook-7-aero-13-bg1087au-r5-ai-340-bz7s1pa-thumb-639148687428971330-600x600.jpg	Laptop HP OmniBook 7 Aero 13 bg1087AU - BZ7S1PA (R5 AI 340, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.611123
322	356	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359482/lenovo-v14-g5-i5-13420h-83hd005jvn-thumb-638997477819926601-600x600.jpg	Laptop Lenovo V14 G5 - 83HD005JVN (i5 13420H, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:55.643463
323	357	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361536/dell-14-dc1425-core-7-150u-71083580-thumb-639036575633348854-600x600.jpg	Laptop Dell 14 DC14250 - 71083580 (Core 7 150U, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.674135
324	358	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341752/gigabyte-a16-ga6h-i7-13620h-gaming-a16-cvhi3vn893sh-thumb-638899249647843295-600x600.jpg	Laptop GIGABYTE Gaming A16 GA6H - GAMING-A16-CVHI3VN893SH (i7 13620H, 16GB, 512GB, RTX5060 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:55.704512
325	359	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360694/dell-16-dc16250-core-7-150u-c7u161w11blu-thumb-639023539526002975-600x600.jpg	Laptop Dell 16 DC16250 - C7U161W11BLU (Core 7 150U, 16GB, 1TB, Full HD+, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.734456
326	360	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/357991/dell-15-dc15250-i7-1355u-cph997-thumb-2-638965530519880244-600x600.jpg	Laptop Dell 15 DC15250 - CPH997 (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.764949
327	361	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358076/hp-omnibook-x-flip-14-fm0076tu-ultra-7-258v-bz7p6pa-thumb-638966518216436696-600x600.jpg	Laptop HP OmniBook X Flip 14 fm0076TU - BZ7P6PA (Ultra 7 258V, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	0	t	2026-09-24 21:15:55.794687
328	362	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361726/hp-omnibook-7-14-fs0043tu-core-5-210h-c1mn3pa-thumb-639047771451788022-600x600.jpg	Laptop HP OmniBook 7 14 fs0043TU - C1MN3PA (Core 5 210H, 16GB, 512GB, 2K, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:55.828058
329	363	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/330579/asus-ux5406sa-ultra-7-pv140ws-638765977226776168-600x600.jpg	Laptop Asus Zenbook S 14 UX5406SA - PV140WS (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, OfficeHS, Win11)	0	t	2026-09-24 21:15:55.867803
330	364	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363897/lenovo-thinkbook-14-gen-8-ultra-5-135h-21sj00eavn-thumb-639093639427166327-600x600.jpg	Laptop Lenovo ThinkBook 14 Gen 8 - 21SJ00EAVN (Ultra 5 135H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:55.903118
331	365	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368555/dell-15-dc15250-core-3-100u-dc5c3259w1-2y-thumb-639179822227361298-600x600.jpg	Laptop Dell 15 DC15250 - DC5C3259W1-2Y (Core 3 100U, 8GB, 512GB, Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:55.936842
332	366	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340479/hp-pavilion-15-eg3112tu-i7-8u6l9pa-638900133723764453-600x600.jpg	Laptop HP Pavilion 15 eg3112TU - 8U6L9PA (i7 1355U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:55.969544
333	367	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362408/asus-vivobook-14-m1407ga-r7-ai-445-ly270w-thumb-639057857058543237-600x600.jpg	Laptop Asus Vivobook 14 M1407GA - LY270W (R7 AI 445, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.001639
334	368	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365625/hp-probook-4-g1ir-14-core-5-120u-c40jkat-thumb-2-639156513208500815-600x600.jpg	Laptop HP ProBook 4 G1iR 14 - C40JKAT (Core 5 120U, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.035193
335	369	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367683/lenovo-ideapad-slim-3-16iph11-ultra-7-355-83us002wvn-thumb-639159949380963741-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US002WVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.066353
336	370	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/325244/dell-inspiron-14-5440-core-7-n4i7204w1-thumb-638754948363791973-600x600.jpg	Laptop Dell Inspiron 14 5440 - N4I7204W1 (Core 7 150U, 16GB, 512GB, Full HD+, OfficeHS, Win11)	0	t	2026-09-24 21:15:56.097717
337	371	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365613/hp-240r-g10-core-7-150u-cc9b9pt-thumb-639120233870097729-600x600.jpg	Laptop HP 240R G10 - CC9B9PT (Core 7 150U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:56.1293
338	372	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362625/asus-zenbook-14-um3406ga-r7-ai-445-qd075ws-thumb-639057626515194446-600x600.jpg	Laptop Asus Zenbook 14 UM3406GA - QD075WS (R7 AI 445 ,16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:56.160759
339	373	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363891/lenovo-thinkpad-e16-gen-3-ultra-7-258v-22ay003vvn-thumb-639094240108310334-600x600.jpg	Laptop Lenovo ThinkPad E16 Gen 3 - 22AY003VVN (Ultra 7 258V, 32GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.193772
340	374	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369845/hp-omnibook-7-14-fr0033tu-ultra-5-225u-c1mn2pa-030926-104427-850-600x600.jpg	Laptop HP OmniBook 7 14 fr0033TU - C1MN2PA (Ultra 5 225U, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:56.227492
341	375	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359471/lenovo-v15-g5-irl-i5-13420h-83hf00byvn-thumb-638995925655693100-600x600.jpg	Laptop Lenovo V15 G5 IRL - 83HF00BYVN (i5 13420H, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:56.262382
342	376	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364979/acer-gaming-aspire-7-a715-59g-79xf-core-7-240h-nh-qx6sv-008-thumb-2-639114408725850717-600x600.jpg	Laptop Acer Gaming Aspire 7 A715-59G-79XF - NH.QX6SV.008 (Core 7 240H, 16GB, 512GB, RTX 3050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:56.298388
343	377	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/357987/dell-inspiron-14-5440-core-5-120u-71053697-thumb-2-638967223573722798-600x600.jpg	Laptop Dell Inspiron 14 5440 - 71053697 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:56.33343
344	378	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364414/asus-vivobook-14-m1407ka-r5-ai-330-ly849w-thumb-639113421525121816-600x600.jpg	Laptop Asus Vivobook 14 M1407KA - LY849W (R5 AI 330, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.368006
345	379	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364181/asus-tuf-gaming-a16-fa607nuq-r7-170-rl007w-thumb-639102024670060442-600x600.jpg	Laptop Asus TUF Gaming A16 FA607NUQ - RL007W (R7 170, 16GB, 512GB, RTX 4050 6GB, Full HD+ 144Hz, Win11)	0	t	2026-09-24 21:15:56.399919
346	380	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337472/dell-inspiron-15-3530-i7-71053721-638900879577580798-600x600.jpg	Laptop Dell Inspiron 15 3530 - 71053721 (i7 1355U, 16GB, 512GB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:56.429908
347	381	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341621/hp-15-fd1037tu-core-7-150u-9z2w5pa-thumb-2-638943948968294518-600x600.jpg	Laptop HP 15 fd1037TU - 9Z2W5PA (Core 7 150U, 16GB, 1TB, Full HD, Win11)	0	t	2026-09-24 21:15:56.459868
348	382	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364623/lenovo-gaming-loq-15irx10-i7-13645hx-83je01agvn-thumb-639108190859916049-600x600.jpg	Laptop Lenovo Gaming LOQ 15IRX10 - 83JE01AGVN (i7 13645HX, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:56.490868
349	383	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361538/dell-16-plus-db16250-ultra-7-256v-x65nw7-thumb-2-639086675136984909-600x600.png	Laptop Dell 16 Plus DB16250 - X65NW7 (Ultra 7 256V, 16GB, 1TB, QHD+ 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:56.521204
350	384	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/357988/dell-inspiron-14-5440-core-7-150u-71059084-thumb-638967227865873337-600x600.jpg	Laptop Dell Inspiron 14 5440 - 71059084 (Core 7 150U, 16GB, 1TB, MX570A 2GB, 2.2K, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:56.551304
351	385	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/326280/msi-katana-a15-ai-b8vg-r7-465vn-638763555401666860-600x600.jpg	Laptop MSI Gaming Katana A15 AI B8VG - 465VN (R7 8845HS, 16GB, 1TB, RTX 4070 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:56.581727
352	386	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/335020/msi-prestige-14-ai-evo-c2vmg-ultra-7-020vn-638900880569143429-600x600.jpg	Laptop MSI Prestige 14 AI+ Evo C2VMG - 020VN (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Win11)	0	t	2026-09-24 21:15:56.611722
353	387	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/335015/msi-vector-16-hx-ai-a2xwig-ultra-9-062vn-638828336640479970-600x600.jpg	Laptop MSI Gaming Vector 16 HX AI A2XWIG - 062VN (Ultra 9 275HX, 16GB, 1TB, RTX 5080 16GB, QHD+ 240Hz, Win11)	0	t	2026-09-24 21:15:56.641589
354	388	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/332397/acer-swift-ai-sf14-51-53p9-ultra-5-nxj2ksv002-638765990119867735-600x600.jpg	Laptop Acer Swift AI SF14 51 53P9 - NX.J2KSV.002 (Ultra 5 226V, 16GB, 1TB, 2.8K OLED 90Hz, Win11)	0	t	2026-09-24 21:15:56.672484
407	477	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364941/asus-vivobook-s14-m3407ha-r5-220-sf480w-thumb-2-639172080601728761-600x600.jpg	Laptop Asus Vivobook S14 M3407HA - SF480W (R5 220, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:58.35723
355	389	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365305/dell-14-dc14250-core-7-150u-71092478-thumb-639117277687385755-600x600.jpg	Laptop Dell 14 DC14250 - 71092478 (Core 7 150U, 16GB, 512GB, Full HD+ 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:56.704721
356	390	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/332579/acer-nitro-v-16-propanel-anv16-41-r6zy-r5-nhqp2sv002-638765990357297678-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV16 41 R6ZY - NH.QP2SV.002 (R5 8645HS, 16GB, 512GB, RTX 3050 6GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:56.738424
357	391	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367017/lenovo-ideapad-slim-3-15iph11-ultra-7-355-83ur00a5vn-thumb-639143554790835163-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR00A5VN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.771695
358	392	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342521/lenovo-gaming-legion-5-15irx10-i7-13650hx-83ly00hqvn-thumb-638913824327783675-600x600.jpg	Laptop Lenovo Gaming Legion 5 15IRX10 - 83LY00HQVN (i7 13650HX, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, OfficeH24, Win11)	0	t	2026-09-24 21:15:56.802729
359	393	https://cdnv2.tgdd.vn/mwg-static/dmx/Products/Images/44/365614/hp-elitebook-6-g1a-14-r5-ai-340-c0ce1pt-thumb-639120726657242385-450x300.jpg	Laptop HP EliteBook 6 G1a 14 - C0CE1PT (R5 AI 340, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.834029
360	394	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362409/asus-vivobook-14-flip-tp3407sa-ultra-5-226v-sg349w-thumb-639057859410721898-600x600.jpg	Laptop Asus Vivobook 14 Flip TP3407SA - SG349W (Ultra 5 226V, 16GB, 512GB, WUXGA OLED, Cảm ứng, Win11)	0	t	2026-09-24 21:15:56.864195
361	395	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358072/hp-omnibook-5-ai-16-af1052tu-ultra-7-255u-c1mn6pa-thumb-638965547577598508-600x600.jpg	Laptop HP OmniBook 5 AI 16 af1052TU - C1MN6PA (Ultra 7 255U, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24, Win11)	0	t	2026-09-24 21:15:56.894269
362	396	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366165/lenovo-ideapad-slim-3-15iph11-ultra-7-355-83ur0075vn-thumb-639134496078325474-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 15IPH11 - 83UR0075VN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.925335
363	397	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342469/dell-inspiron-15-3530-i7-1355u-n5i7421w1-thumb-2-638981083623791430-600x600.jpg	Laptop Dell Inspiron 15 3530 - N5I7421W1 (i7 1355U, 16GB, 512GB,  Full HD 120Hz, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:56.958271
364	398	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364942/asus-vivobook-16-x1607ca-ultra-7-255h-mb990w-100426-021509-872-600x600.jpg	Laptop Asus Vivobook 16 X1607CA - MB990W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:56.989409
365	399	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/334800/asus-zenbook-a14-ux3407qa-x1-26-100-qd299ws-638774630428732695-600x600.jpg	Laptop Asus Zenbook A14 UX3407QA - QD299WS (X1 26 100, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.020675
366	413	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367377/acer-aspire-lite-15-al15-53p-56qh-core-5-120u-nx-dg3sv-001-thumb-639161653711301658-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-53P-56QH - NX.DG3SV.001 (Core 5 120U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:57.051996
367	414	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342749/asus-vivobook-s14-s3407va-i7-13620h-ly053w-thumb-638919138521960274-600x600.jpg	Laptop Asus Vivobook S14 S3407VA - LY053W (i7 13620H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.083522
368	415	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/325247/dell-inspiron-15-3530-i7-p16wd-thumb-638754948496889615-600x600.jpg	Laptop Dell Inspiron 15 3530 - P16WD (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeHS, Win11)	0	t	2026-09-24 21:15:57.115127
369	416	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363900/lenovo-thinkpad-e14-gen-7-ultra-7-258v-21u2003jvn-thumb-639092654229595636-600x600.jpg	Laptop Lenovo ThinkPad E14 Gen 7 - 21U2003JVN (Ultra 7 258V, 32GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.14773
370	417	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358798/hp-omnibook-5-16-ag1066au-r7-al-350-bz7s9pa-thumb-638985450204701016-600x600.jpg	Laptop HP OmniBook 5 16 ag1066AU - BZ7S9PA (R7 Al 350, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365B, Win11)	0	t	2026-09-24 21:15:57.178389
371	418	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/341269/hp-omnibook-uf-14-fh0097tu-ultra-7-258v-bz7s3pa-thumb-2-638978692991283064-600x600.jpg	Laptop HP OmniBook UF 14 fh0097TU - BZ7S3PA (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Cảm ứng, OfficeH24, Win11)	0	t	2026-09-24 21:15:57.209656
372	419	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359302/msi-gaming-stealth-a16-ai-a3xwfg-r9-ai-hx-370-018vn-thumb-638993199359996290-600x600.jpg	Laptop MSI Gaming Stealth A16 AI+ A3XWFG - 018VN (R9 AI HX 370, 32GB, 1TB, RTX 5060 8GB, QHD+ OLED, 240Hz, Win11)	0	t	2026-09-24 21:15:57.240094
373	428	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/359928/gigabyte-gaming-a16-ga6h-i7-13620h-cthi3vn893sh-thumb-639003672081449517-600x600.jpg	Laptop GIGABYTE Gaming A16 GA6H - CWHI3VNC94SH (i7 13620H, 16GB, 1TB, RTX 5070 8GB, WQXGA 165Hz, Win11)	0	t	2026-09-24 21:15:57.272395
374	429	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362626/asus-zenbook-14-um3406ga-r7-ai-445-qd073ws-thumb-639057631525660749-600x600.jpg	Laptop Asus Zenbook 14 UM3406GA - QD073WS (R7 AI 445, 32GB, 1TB, WUXGA OLED, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.305541
375	430	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368221/lenovo-thinkbook-14-gen-9-irl-i5-13420h-21uy008tvn-thumb-639178120070286677-600x600.jpg	Laptop Lenovo ThinkBook 14 Gen 9 IRL - 21UY008TVN (i5 13420H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.342867
376	431	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337042/hp-pavilion-16-af0053tu-ultra-7-ay8c2pa-thumb-638828178123952574-600x600.jpg	Laptop HP Pavilion 16 af0053TU - AY8C2PA (Ultra 7 155U, 16GB, 512GB, WUXGA, OfficeHS+365, Win11)	0	t	2026-09-24 21:15:57.379352
377	432	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365312/dell-16-dc16250-core-7-150u-dc16250-c7u161w11blu-27-thumb-639116780681933451-600x600.jpg	Laptop Dell 16 DC16250 - DC16250-C7U161W11BLU-27 (Core 7 150U, 16GB, 1TB, Full HD+, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:57.414277
378	433	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340563/dell-inspiron-14-5441-x1p-64-n4o10441w1-638900115125403698-600x600.jpg	Laptop Dell Inspiron 14 5441 - N4O10441W1 (X1P 64 100, 16GB, 1TB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.450641
379	434	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/340559/dell-inspiron-15-3530-i7-n3530-i7u161w11slu-638900114347898134-600x600.jpg	Laptop Dell Inspiron 15 3530 - N3530-i7U161W11SLU (i7 1355U, 16GB, 1TB, Full HD 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.484307
380	435	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368223/hp-omnibook-x-flip-14-fk0082au-r7-ai-350-bz7n8pa-thumb-639177265513907656-600x600.jpg	Laptop HP OmniBook X Flip 14 fk0082AU - BZ7N8PA (R7 AI 350, 32GB, 1TB, 2K, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.516161
381	436	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367012/lenovo-ideapad-5-2in1-14iph11-ultra-5-322-83ug0026vn-thumb-639144568090091054-600x600.jpg	Laptop Lenovo IdeaPad 5 2in1 14IPH11 - 83UG0026VN (Ultra 5 322, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	0	t	2026-09-24 21:15:57.548354
382	437	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362023/dell-16-dc16251-core-7-150u-dc6c7557w1-thumb-639046783483771365-600x600.jpg	Laptop Dell 16 DC16251 - DC6C7557W1 (Core 7 150U, 16GB, 1TB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.579281
383	438	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/325953/dell-inspiron-14-5440-core-7-7fn5j-thumb-638754955274250524-600x600.jpg	Laptop Dell Inspiron 14 5440 - 7FN5J (Core 7 150U, 16GB, 1TB, Full HD+, OfficeHS, Win11)	0	t	2026-09-24 21:15:57.609886
384	439	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342719/hp-gaming-omen-16-am0176tx-ultra-7-255h-bx9d3pa-thumb-2-638978728050621532-600x600.jpg	Laptop HP Gaming OMEN 16 am0176TX - BX9D3PA (Ultra 7 255H, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:57.642751
385	440	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368217/lenovo-yoga-7-2in1-14iph11-ultra-5-322-83tc002lvn-thumb-639178080349557801-600x600.jpg	Laptop Lenovo Yoga 7 2in1 14IPH11 - 83TC002LVN (Ultra 5 322, 16GB, 512GB, WUXGA OLED, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.677283
386	441	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/345500/hp-probook-4-g1i-16-ultra-5-225u-bq5d4pt-thumb-638936179391158436-600x600.jpg	Laptop HP Probook 4 G1i 16 - BQ5D4PT (Ultra 5 225U, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.707963
387	442	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369747/dell-14-dc14250-core-7-150u-dc14250-c7u161w11slu-2y-thumb-639204946954551554-600x600.jpg	Laptop Dell 14 DC14250 - DC14250-C7U161W11SLU-2Y (Core 7 150U, 16GB, 1TB, Full HD+ ,OfficeH24+365, Win11)	0	t	2026-09-24 21:15:57.738524
388	443	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368218/lenovo-thinkbook-14-g9-r7-250-21v0005pvn-thumb-639178090263762280-600x600.jpg	Laptop Lenovo ThinkBook 14 G9 - 21V0005PVN (R7 250, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.768702
389	444	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/345508/hp-elitebook-6-g1i-13-ultra-7-255u-bq9m6p-thumb-638936199477439480-600x600.jpg	Laptop HP EliteBook 6 G1i 13 - BQ9M6PT (Ultra 7 255U, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.799261
390	445	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358794/hp-omnibook-7-16-az0038tu-core-5-210h-c2cx1pa-thumb01-638983932700674953-600x600.jpg	Laptop HP OmniBook 7 16 az0038TU - C2CX1PA (Core 5 210H, 24GB, 512GB, WUXGA, OfficeH24, Win11)	0	t	2026-09-24 21:15:57.831105
391	453	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364621/lenovo-thinkpad-x1-carbon-gen-13-ultra-7-258v-21ns010jvn-thumb-2-639107238068016232-600x600.jpg	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010JVN (Ultra 7 258V, 32GB, 1TB, 2.8K OLED 120Hz, Win11Pro)	0	t	2026-09-24 21:15:57.862445
392	454	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367685/lenovo-ideapad-slim-3-14iph11-ultra-7-355-83uq003pvn-thumb-639166826072161878-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 14IPH11 - 83UQ003PVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:57.894525
393	455	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/337040/hp-elitebook-ultra-g1q-x1e-78-100-b4py1pt-638851476833171882-600x600.jpg	Laptop HP Elitebook Ultra G1q - B4PY1PT (X1E 78 100, 32GB, 1TB, 2.2K, Cảm ứng, Win11 Pro)	0	t	2026-09-24 21:15:57.925398
394	456	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368224/hp-gaming-hyperx-omen-15-ga0092tx-i5-14450hx-d72d8pa-thumb-639177437337629252-600x600.jpg	Laptop HP Gaming HyperX Omen 15 ga0092TX - D72D8PA (i5 14450HX, 16GB, 512GB, RTX 5050 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:57.9552
395	457	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/327098/hp-15-fc0085au-r5-a6vv8pa-170225-110652-878-600x600.jpg	Laptop HP 15 fc0085AU - A6VV8PA (R5 7430U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:57.983923
396	458	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/339558/hp-elitebook-x-g1a-14-ai-r9-ai-hx-pro-375-b9fe4pt-638900878615705835-600x600.jpg	Laptop HP EliteBook X G1a 14 AI - B9FE4PT (R9 AI HX PRO 375, 32GB, 1TB, 2.8K OLED 120Hz, Cảm ứng, Win11 Pro)	0	t	2026-09-24 21:15:58.012832
397	459	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358081/hp-omnibook-7-16-az0040tu-core-9-270h-c2dr3pa-thumb-638966564370747518-600x600.jpg	Laptop HP OmniBook 7 16 az0040TU - C2DR3PA (Core 9 270H, 32GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:58.043973
398	468	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368301/asus-vivobook-14-x1407ca-ultra-5-225h-ly203w-thumb-639178911847886946-600x600.jpg	Laptop Asus Vivobook 14 X1407CA - LY203W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:58.074836
399	469	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/360418/asus-vivobook-15-x1504va-core-5-120u-bq185w-thumb-639017420847213436-600x600.jpg	Laptop Asus Vivobook 15 X1504VA - BQ185W (Core 5 120U, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.105778
400	470	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368146/asus-vivobook-go-14-e1404fa-r5-40-eb1832w-030926-101520-473-600x600.jpg	Laptop Asus Vivobook Go 14 E1404FA - EB1832W (R5 40, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.135414
401	471	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370862/acer-aspire-lite-15-al15-410p-r1w8-ryzen-5-3500u-nx-x0rsv-001-thumb-639236342840647503-600x600.jpg	Laptop Acer Aspire Lite 15 AL15-410P-R1W8 - NX.X0RSV.001 (Ryzen 5 3500U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.16686
402	472	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369755/asus-gaming-v16-v3607vj-core-5-core-5-210h-tk189w-thumb-639209519222075024-600x600.jpg	Laptop Asus Gaming V16 V3607VJ Core 5 - TK189W (Core 5 210H, 16GB, 512GB, RTX 3050 6GB, WUXGA 144Hz, Win11)	0	t	2026-09-24 21:15:58.197355
403	473	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368302/asus-vivobook-go-14-e1404fa-r5-40-eb012w-030926-101653-932-600x600.jpg	Laptop Asus Vivobook Go 14 E1404FA - EB012W (R5 40, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.229207
404	474	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369450/asus-vivobook-16-x1607ca-ultra-5-225h-mb387w-030926-102623-046-600x600.jpg	Laptop Asus Vivobook 16 X1607CA - MB387W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:58.26096
405	475	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369758/asus-vivobook-go-15-x1504ma-core-3-304-bq893w-thumb-639210253678678922-600x600.jpg	Laptop Asus Vivobook 15 X1504MA - BQ893W (Core 3 304, 8GB, 512GB, Full HD, Win 11)	0	t	2026-09-24 21:15:58.295219
406	476	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369746/dell-14-dc14250-core-3-100u-dc14250-c3u085w11slu-2y-thumb-639204915247971563-600x600.jpg	Laptop Dell 14 DC14250 - DC14250-C3U085W11SLU-2Y (Core 3 100U, 8GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:58.327863
655	743	https://cdn.tgdd.vn/Products/Images/7077/329832/redmi-watch-5-lite-kem-tb-600x600.jpg	Xiaomi Redmi Watch 5 Lite 48.2mm dây TPU	0	t	2026-09-24 21:16:06.744697
408	478	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367892/asus-vivobook-14-x1404ma-core-5-320-eb219w-thumb-639166979400883797-600x600.jpg	Laptop Asus Vivobook 14 X1404MA - EB219W (Core 5 320, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.388459
409	479	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368147/asus-vivobook-15-x1504ma-core-5-320-bq385w-thumb-639173779246567438-600x600.jpg	Laptop Asus Vivobook 15 X1504MA - BQ385W (Core 5 320, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.420183
410	480	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369984/msi-cyborg-15-black-edition-a13veo-i7-13620h-2608vn-thumb-639217144389270458-600x600.jpg	Laptop MSI Cyborg 15 Black Edition A13VEO - 2608VN (i7 13620H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:58.452793
411	485	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363503/macbook-air-13-inch-m5-24gb-512gb-thumb-2-639086552878395354-600x600.jpg	Laptop Macbook Air 13 inch M5 24GB/512GB 35W	0	t	2026-09-24 21:15:58.487392
412	488	https://cdn.tgdd.vn/Products/Images/44/363511/macbook-air-15-inch-m5-24gb-512gb-70w-xanh-da-troi-1-600x600.jpg	Laptop MacBook Air 15 inch M5 24GB/512GB 70W	0	t	2026-09-24 21:15:58.52507
413	493	https://cdn.tgdd.vn/Products/Images/44/363488/macbook-pro-14-inch-m5-pro-24gb-1tb-den-1-600x600.jpg	Laptop MacBook Pro 14 inch M5 Pro 24GB/1TB 70W	0	t	2026-09-24 21:15:58.559659
414	494	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/351616/asus-vivobook-s14-m3407ka-r5-ai-330-sf034ws-thumb-638938740104463982-600x600.jpg	Laptop Asus Vivobook S14 M3407KA - SF034WS (R5 AI 330, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:58.595879
415	495	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366826/asus-vivobook-14-x1407aa-ultra-5-325-ly360w-thumb-639148670722644209-600x600.jpg	Laptop ASUS Vivobook 14 X1407AA - LY360W (Ultra 5 325, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:58.630812
416	496	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/358371/lenovo-gaming-loq-15irx10-i7-13650hx-83je00pevn-thumb-638972596013558743-600x600.jpg	Laptop Lenovo Gaming LOQ 15IRX10 - 83JE00PEVN (i7 13650HX, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:58.6689
417	497	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369449/asus-vivobook-14-x1404ma-core-5-320-eb028w-thumb-639197144395501727-600x600.jpg	Laptop Asus Vivobook 14 X1404MA - EB028W (Core 5 320, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.703927
418	498	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364416/asus-vivobook-s14-s3407ca-ultra-7-255h-sf923w-thumb-639110740348794020-600x600.jpg	Laptop Asus Vivobook S14 S3407CA - SF923W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:58.737648
419	499	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368148/asus-vivobook-15-x1504ma-core-7-350-bq395w-thumb-639173782611096783-600x600.jpg	Laptop Asus Vivobook 15 X1504MA - BQ395W (Core 7 350, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:58.77157
420	500	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369985/msi-cyborg-15-black-edition-a13veo-i5-13420h-2610vn-thumb-639217145450148653-600x600.jpg	Laptop MSI Cyborg 15 Black Edition A13VEO - 2610VN (i5 13420H, 16GB, 512GB, RTX 4050 6GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:58.8039
421	501	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371075/asus-gaming-v16-v3607vu-core-5-210h-tk416w-thumb-639243191854624363-600x600.jpg	Laptop Asus Gaming V16 V3607VU - TK416W (Core 5 210H, 16GB, 512GB, RTX 4050 6GB, WUXGA 144Hz, Win11)	0	t	2026-09-24 21:15:58.835982
422	502	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368300/asus-vivobook-s14-s3407va-core-7-240h-ly256w-thumb-639178218121924648-600x600.jpg	Laptop Asus Vivobook S14 S3407VA - LY256W (Core 7 240H, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:58.87255
423	508	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367862/gigabyte-gaming-a16-r7-260-a16-3thk3vn893sh-thumb-639160080549917841-600x600.jpg	Laptop GIGABYTE Gaming A16 - 3THK3VN893SH (R7 260, 16GB, 512GB, RTX 5050 8GB, Full HD+ 165Hz, Win11)	0	t	2026-09-24 21:15:58.909881
424	509	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367864/gigabyte-gaming-aero-x16-r7-ai-350-aero-x16-1vh93vnc94ah-thumb-639165359795713576-600x600.jpg	Laptop GIGABYTE Gaming AERO X16 - 1VH93VNC94AH (R7 AI 350, 16GB, 1TB, RTX 5060 8GB, WQXGA 165Hz, Win11)	0	t	2026-09-24 21:15:58.953483
425	510	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367347/dell-pro-14-pc14250-ultra-5-235u-pc14250-235u-16512wp-2y-thumb-639147860116885905-600x600.jpg	Laptop Dell Pro 14 PC14250 - PC14250-235U-16512WP-2Y (Ultra 5 235U, 16GB, 512GB, Full HD+, Win11Pro)	0	t	2026-09-24 21:15:58.995425
426	511	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/362022/dell-16-plus-db16250-ultra-5-226v-db6u5387w1-thumb-639045186164675523-600x600.jpg	Laptop Dell 16 Plus DB16250 - DB6U5387W1 (Ultra 5 226V, 16GB, 1TB, WQXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:59.029787
427	512	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365307/dell-16-dc16250-core-5-120u-71092481-thumb-639117288317958934-600x600.jpg	Laptop Dell 16 DC16250 - 71092481 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeHS24+365, Win11)	0	t	2026-09-24 21:15:59.066869
428	513	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367682/lenovo-ideapad-slim-3-16iph11-ultra-5-322-83us002tvn-thumb-639159938982250065-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US002TVN (Ultra 5 322, 24GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.100976
429	514	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369467/asus-expertbook-ultra-p5-p5406cca-ultra-5-225h-sf0045w-030926-103538-938-600x600.jpg	Laptop Asus ExpertBook P5 P5406CCA - SF0045W (Ultra 5 225H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.134288
430	515	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370280/acer-gaming-nitro-propanel-anv15-52-78md-core-7-240h-nh-qz9sv-007-thumb-639224025258189177-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV15-52-78MD - NH.QZ9SV.007 (Core 7 240H, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:15:59.16635
431	516	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371299/asus-vivobook-16-x1607ca-ultra-7-255h-mb399w-thumb-639251549351398586-600x600.jpg	Laptop Asus Vivobook 16 X1607CA - MB399W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.198954
432	517	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370863/acer-aspire-lite-14-al14-47p-r0tr-ryzen-5-3500u-nx-x0psv-002-thumb-639236366485169379-600x600.jpg	Laptop Acer Aspire Lite 14 AL14-47P-R0TR - NX.X0PSV.002 (Ryzen 5 3500U, 8GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:59.231272
433	518	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/345504/hp-probook-4-g1i-16-ultra-7-255h-bq5e4pt-thumb-638936191690396559-600x600.jpg	Laptop HP Probook 4 G1i 16 - BQ5E4PT (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.263916
434	519	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363894/lenovo-thinkpad-e16-gen-3-ultra-7-258v-22ay003uvn-thumb-639095148567434164-600x600.jpg	Laptop Lenovo ThinkPad E16 Gen 3 - 22AY003UVN (Ultra 7 258V, 32GB, 1TB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.294334
435	520	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365113/asus-tuf-gaming-fx608jhi-i7-14650hx-tu209w-thumb-639115006182978654-600x600.jpg	Laptop Asus TUF Gaming FX608JHI - TU209W (i7 14650HX, 16GB, 512GB, RTX5050 8GB, Full HD+ 144Hz, Win11)	0	t	2026-09-24 21:15:59.326427
436	521	https://cdn.tgdd.vn/Products/Images/44/363513/macbook-air-15-inch-m5-32gb-512gb-vang-thumb-600x600.png	Laptop MacBook Air 15 inch M5 32GB/512GB	0	t	2026-09-24 21:15:59.357392
437	522	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367863/gigabyte-gaming-a16-r7-260-a16-3vhk3vn893sh-thumb-639160088411593924-600x600.jpg	Laptop GIGABYTE Gaming A16 - 3VHK3VN893SH (R7 260, 16GB, 512GB, RTX 5060 8GB, Full HD+ 165Hz, Win11)	0	t	2026-09-24 21:15:59.389654
438	523	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368219/lenovo-v14-g5-core7-240h-83hd0035vn-thumb-639178097344250268-600x600.jpg	Laptop Lenovo V14 G5 - 83HD0035VN (Core7 240H, 16G, 512GB, Full HD, Win11)	0	t	2026-09-24 21:15:59.422332
439	524	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369213/msi-gaming-cyborg-15-c13weo-417vn-i7-13620h-thumb-639192871314062724-600x600.jpg	Laptop MSI Gaming Cyborg 15 C13WEO-417VN (i7 13620H, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:15:59.45538
440	525	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370349/lenovo-ideapad-slim-3-16iph11-ultra-5-322-83us0040vn-030926-104445-371-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US0040VN (Ultra 5 322, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:15:59.488435
441	526	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370356/lenovo-yoga-slim-7-ultra-14iph11-ultra-7-355-83qk0015vn-030926-104545-590-600x600.jpg	Laptop Lenovo Yoga Slim 7 Ultra 14IPH11 - 83QK0015VN (Ultra 7 355, 32GB, 1TB, WQXGA+ 120Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:59.519559
442	527	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370866/acer-aspire-go-14-ai-ag14-i71m-977m-ultra-9-185h-nx-w26sv-001-290826-093906-164-600x600.jpg	Laptop Acer Aspire Go 14 AI AG14-I71M-977M - NX.W26SV.001 (Ultra 9 185H, 16GB, 512GB, Full HD+, Win11)	0	t	2026-09-24 21:15:59.550032
443	528	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364408/asus-tuf-gaming-a14-fa401gm-r9-ai-465-rg013w-thumb-639107350281039453-600x600.jpg	Laptop Asus TUF Gaming A14 FA401GM - RG013W (R9 AI 465, 32GB, 1TB, RTX 5060 8GB, WQXGA 165Hz, Win11)	0	t	2026-09-24 21:15:59.587643
444	529	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365617/hp-elitebook-6-g1a-14-r7-ai-350-c0ce2pt-thumb-639116847125407980-600x600.jpg	Laptop HP EliteBook 6 G1a 14 - C0CE2PT (R7 AI 350, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	0	t	2026-09-24 21:15:59.621475
445	530	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361539/dell-16-plus-db16250-ultra-9-288v-x65nw9-thumb-2-639086675576918437-600x600.png	Laptop Dell 16 Plus DB16250 - X65NW9 (Ultra 9 288V, 32GB, 2TB, QHD+ 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:59.65533
446	531	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363899/lenovo-thinkpad-x1-carbon-gen-13-ultra-7-256v-21ns010fvn-thumb-639092545429611693-600x600.jpg	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS010FVN (Ultra 7 256V, 16GB, 512GB, 2.8K OLED 120Hz, Win11Pro)	0	t	2026-09-24 21:15:59.690656
447	532	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364389/asus-gaming-rog-flow-z13-gz302eac-ai-max-395-ru184ws-thumb-639094294582783186-600x600.jpg	Laptop Asus Gaming ROG Flow Z13 GZ302EAC - RU184WS (AI MAX+ 395, 128GB, 1TB, WQXGA 180Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:15:59.724716
448	533	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364488/asus-tuf-gaming-a16-fa608pp-r9-8940hx-rv089w-thumb-639103019071505954-600x600.jpg	Laptop Asus TUF Gaming A16 FA608PP - RV089W (R9 8940HX, 16GB, 512GB, RTX5070 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:15:59.757915
449	534	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365302/dell-14-dc14255-r7-350-71092477-thumb-639117258643684943-600x600.jpg	Laptop Dell 14 DC14255 - 71092477 (R7 350, 16GB, 1TB, Full HD+, OfficeHS24+365B, Win11)	0	t	2026-09-24 21:15:59.787813
450	535	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366068/asus-gaming-rog-zephyrus-ga403gm-r9-ai-465-sy004w-thumb-639124650601014358-600x600.jpg	Laptop Asus Gaming ROG Zephyrus GA403GM - SY004W (R9 AI 465, 32GB, 1TB, RTX 5060 8GB, 3K OLED 120Hz, Win11)	0	t	2026-09-24 21:15:59.819509
451	536	https://cdnv2.tgdd.vn/mwg-static/dmx/Products/Images/44/366827/asus-vivobook-s14-s3407aa-ultra-5-325-sf945w-thumb-639143211718788407-450x300.jpg	Laptop Asus Vivobook S14 S3407AA - SF945W (Ultra 5 325, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:15:59.850317
452	537	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367015/lenovo-gaming-legion-5-15iph11-ultra-7-356h-83rw0023vn-thumb-639144585213475734-600x600.jpg	Laptop Lenovo Gaming Legion 5 15IPH11 - 83RW0023VN (Ultra 7 356H, 16GB, 512GB, RTX 5060 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	0	t	2026-09-24 21:15:59.881108
453	538	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367348/dell-pro-14-pc14250-ultra-7-255u-pc14250-255u-32512wh-2y-thumb-639147886186830093-600x600.jpg	Laptop Dell Pro 14 PC14250 - PC14250-255U-32512WH-2Y (Ultra 7 255U, 32GB, 512GB, Full HD+, Win11)	0	t	2026-09-24 21:15:59.912344
454	539	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368149/asus-gaming-rog-strix-g16-g614pm-r9-8940hx-ts147w-thumb-639173798467756505-600x600.jpg	Laptop Asus Gaming ROG Strix G16 G614PM - TS147W (R9 8940HX, 16GB, 512GB, RTX 5060 8GB, WQXGA 300Hz, Win11)	0	t	2026-09-24 21:15:59.943707
455	540	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368150/asus-gaming-rog-strix-g16-g614pr-r9-8940hx-ts103w-thumb-639173856980267409-600x600.jpg	Laptop Asus Gaming ROG Strix G16 G614PR - TS103W (R9 8940HX, 16GB, 512GB, RTX 5070Ti 12GB, WQXGA 300Hz, Win11)	0	t	2026-09-24 21:15:59.975283
456	541	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368212/lenovo-thinkpad-x9-14-gen-1-ultra-7-258v-21qa006jvn-thumb-639177400683360702-600x600.jpg	Laptop Lenovo ThinkPad X9 14 Gen 1 - 21QA006JVN (Ultra 7 258V, 32GB, 512GB, WUXGA OLED, Win11Pro)	0	t	2026-09-24 21:16:00.006312
457	542	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368216/lenovo-yoga-7-2in1-14iph11-ultra-7-355-83tc002mvn-thumb-639178069051019274-600x600.jpg	Laptop Lenovo Yoga 7 2in1 14IPH11 - 83TC002MVN (Ultra 7 355, 32GB, 512GB, WUXGA OLED, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:00.039158
458	543	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368651/acer-gaming-nitro-propanel-anv16s-71-58wq-core-5-210h-nh-qxbsv-001-thumb-639185342559794630-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV16S-71-58WQ - NH.QXBSV.001 (Core 5 210H, 16GB, 512GB, RTX 5050 8GB, 2K+ 180Hz, Win11)	0	t	2026-09-24 21:16:00.071126
459	544	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369461/asus-expertbook-ultra-b9-b9406caa-x7321tw-ultra-x7-358h-32gb-1tb-wqxga-oled-120hz-win11-030926-103208-019-600x600.jpg	Laptop Asus ExpertBook Ultra B9 B9406CAA - X7321TW (Ultra X7 358H, 32GB, 1TB, WQXGA+ OLED 120Hz, Win11)	0	t	2026-09-24 21:16:00.105035
541	627	https://cdn.tgdd.vn/Products/Images/522/356854/xiaomi-redmi-pad-2-pro-sliver-thumb-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 Pro WiFi 6GB/128GB	0	t	2026-09-24 21:16:02.678699
460	545	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369979/acer-swift-go-ai-sfg14-75-5264-ultra-5-226v-nxjnbsv001-030926-104407-788-600x600.jpg	Laptop Acer Swift Go AI SFG14-75-5264 - NX.JNBSV.001 (Ultra 5 226V, 16GB, 512GB, Full HD, Win11)	0	t	2026-09-24 21:16:00.139003
461	546	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369980/acer-nitro-propanel-anv16-72-782f-core-7-240h-nhqupsv001-thumb2-639216909787985336-600x600.jpg	Laptop Acer Nitro ProPanel ANV16-72-782F - NH.QUPSV.001 (Core 7 240H, 16GB, 512GB, RTX 5050 8GB, Full HD+ 180 Hz, Win11)	0	t	2026-09-24 21:16:00.173773
462	547	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370352/lenovo-thinkpad-e14-gen-7-core-7-240h-21t90025vn-110826-053255-371-600x600.jpg	Laptop Lenovo ThinkPad E14 - 21T90025VN (Gen 7 Core 7 240H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:00.210074
463	548	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370355/lenovo-yoga-slim-7-14iph11-ultra-7-355-83qm0076vn-030926-104533-752-600x600.jpg	Laptop Lenovo Yoga Slim 7 14IPH11 - 83QM0076VN (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:00.24263
464	549	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/332274/hp-omen-14-fb0135tx-ultra-7-ay8v1pa-638765983673045610-600x600.jpg	Laptop HP Gaming OMEN 14 fb0135TX - AY8V1PA (Ultra 7 155H, 16GB, 1TB, RTX 4060 8GB, 2.8K OLED 120Hz, Win11)	0	t	2026-09-24 21:16:00.274411
465	550	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/335963/acer-predator-helios-18-ai-ph18-73-98aq-ultra-9-nhqvwsv001-thumb-638828182518741548-600x600.jpg	Laptop Acer Gaming Predator Helios 18 AI PH18 73 98AQ - NH.QVWSV.001 (Ultra 9 275HX, 192GB, 6TB, RTX 5090 24GB, 4K 120Hz, Win11 Pro)	0	t	2026-09-24 21:16:00.3066
466	551	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/335964/acer-predator-helios-18-ai-ph18-73-93p0-ultra-9-nhqvysv001-thumb-638828182821796512-600x600.jpg	Laptop Acer Gaming Predator Helios 18 AI PH18 73 93P0 - NH.QVYSV.001 (Ultra 9 275HX, 64GB, 3TB, RTX 5080 12GB, 2.5K 250Hz, Win11 Pro)	0	t	2026-09-24 21:16:00.338257
467	552	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/342527/lenovo-gaming-legion-5-15ahp10-r7-260-83m0002yvn-thumb-638913831113387894-600x600.jpg	Laptop Lenovo Gaming Legion 5 15AHP10 - 83M0002YVN (R7 260, 16GB, 512GB, RTX 5050 8GB, WQXGA OLED 165Hz, Win11)	0	t	2026-09-24 21:16:00.372534
468	553	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/361537/dell-inspiron-14-5441-x1-26-100-71069158-thumb-639036594431939344-600x600.jpg	Laptop Dell Inspiron 14 5441 - 71069158 (X1 26 100, 16GB, 512GB, Full HD+, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:00.40459
469	554	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/363895/lenovo-thinkpad-x1-carbon-gen-13-ultra-5-226v-21ns0107vn-thumb-639095107114164899-600x600.jpg	Laptop Lenovo ThinkPad X1 Carbon Gen 13 - 21NS0107VN (Ultra 5 226V, 16GB, 512GB, 2.8K OLED 120Hz, Win11Pro)	0	t	2026-09-24 21:16:00.436431
470	555	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/364868/msi-prestige-14-flip-ai-d3mtg-u9-386h-021vn-thumb-639110365684023102-600x600.jpg	Laptop MSI Prestige 14 Flip AI+ D3MTG - 021VN (U9 386H, 32GB, 1TB, Full HD+ OLED, Cảm ứng, Win11)	0	t	2026-09-24 21:16:00.465247
471	556	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365615/hp-elitebook-6-g1a-14-r5-ai-340-c0cf2pt-thumb-639120731928950053-600x600.jpg	Laptop HP EliteBook 6 G1a 14 - C0CF2PT (R5 AI 340, 16GB, 512GB, WUXGA, Cảm ứng, Win11)	0	t	2026-09-24 21:16:00.495245
472	557	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365619/hp-elitebook-8-g1a-14-r5-ai-pro-340-c0ce3pt-thumb-639117570823568828-600x600.jpg	Laptop HP EliteBook 8 G1a 14 - C0CE3PT (R5 AI PRO 340, 32GB, 512GB, WUXGA, Cảm ứng, Win11 Pro)	0	t	2026-09-24 21:16:00.52466
473	558	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/365621/hp-elitebook-8-g1a-14-ai-350-c0ce5pt-thumb-639117599283437054-600x600.jpg	Laptop HP EliteBook 8 G1a 14 - C0CE5PT (R7 AI PRO 350, 32GB, 512GB, WUXGA, Cảm ứng, Win11)	0	t	2026-09-24 21:16:00.55513
474	559	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366066/asus-tuf-gaming-fa401ea-ryzen-ai-max-392-rg034w-thumb-639123676222782915-600x600.jpg	Laptop Asus TUF Gaming FA401EA - RG034W (RYZEN AI MAX+ 392, 64GB, 1TB, WQXGA 165Hz, Win11)	0	t	2026-09-24 21:16:00.58423
475	560	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366091/acer-gaming-predator-helios-neo-16-phn16-i31-50h7-i5-14450hx-nh-u4ssv-001-thumb-639125466169027520-600x600.jpg	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-50H7 - NH.U4SSV.001 (i5 14450HX, 32GB, 512GB, RTX 5050 8GB, Full HD+ 180Hz, Win11)	0	t	2026-09-24 21:16:00.613915
476	561	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366092/acer-gaming-predator-helios-neo-16-phn16-i31-72xe-i7-14650hx-nh-u4rsv-001-thumb-639125663668779838-600x600.jpg	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-72XE - NH.U4RSV.001 (i7 14650HX, 32GB, 512GB, RTX5060 8GB, 2K+ 180Hz, Win11)	0	t	2026-09-24 21:16:00.645751
477	562	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366093/acer-gaming-predator-helios-neo-16-phn16-i31-74mn-i7-14650hx-nh-u4ssv-002-thumb-639127708440970941-600x600.jpg	Laptop Acer Gaming Predator Helios Neo 16 PHN16-I31-74MN - NH.U4SSV.002 (i7 14650HX, 32GB, 512GB, RTX 5050 8GB, Full HD+ 180Hz, Win11)	0	t	2026-09-24 21:16:00.678006
478	563	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/366744/lenovo-gaming-legion-5-15ahp11-r7-250-83q7001jvn-thumb-639140577776811530-600x600.jpg	Laptop Lenovo Gaming Legion 5 15AHP11 - 83Q7001JVN (R7 250, 16GB, 512GB, RTX 5060 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	0	t	2026-09-24 21:16:00.71012
479	564	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367686/lenovo-gaming-loq-15iph11-ultra-7-356h-83sl000lvn-thumb-639161854460947071-600x600.jpg	Laptop Lenovo Gaming LOQ 15IPH11 - 83SL000LVN (Ultra 7 356H, 16GB, 512GB, RTX 5060 8GB, WUXGA 165Hz, Win11)	0	t	2026-09-24 21:16:00.740798
480	565	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367865/msi-gaming-raider-16-max-hx-b2wi-ultra-9-290hx-plus-095vn-thumb-639159307452915579-600x600.jpg	Laptop MSI Gaming Raider 16 MAX HX B2WI - 095VN (Ultra 9 290HX Plus, 64GB, 2TB, RTX 5080 16GB, QHD+ OLED 240Hz, Win11)	0	t	2026-09-24 21:16:00.772555
481	566	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367887/asus-tuf-gaming-f16-fx608jhi-i5-14450hx-tu210w-thumb-639169465662640002-600x600.jpg	Laptop Asus TUF Gaming F16 FX608JHI - TU210W (i5 14450HX, 16GB, 512GB,RTX 5050 8GB, WUXGA 144Hz, Win11)	0	t	2026-09-24 21:16:00.804061
482	567	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367888/asus-zenbook-s-16-um5606ga-r9-ai-465-ss441w-thumb-639169478465235209-600x600.jpg	Laptop Asus Zenbook S 16 UM5606GA - SS441W (R9 AI 465, 16GB, 512GB, 3K OLED 120Hz, Win11)	0	t	2026-09-24 21:16:00.838643
483	568	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367889/asus-gaming-rog-zephyrus-g14-gu405aw-ultra-9-386h-sy029w-thumb-639167025738826102-600x600.jpg	Laptop Asus Gaming ROG Zephyrus G14 GU405AW - SY029W (Ultra 9 386H, 32GB, 1TB, RTX 5080 16GB, 3K OLED 120Hz, Win11)	0	t	2026-09-24 21:16:00.873084
542	628	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/333917/oppo-pad-3-bac-thumb-638725440842878647-600x600.jpg	Máy tính bảng OPPO Pad 3 8GB/256GB	0	t	2026-09-24 21:16:02.709351
484	569	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367890/asus-zenbook-a14-ux3407na-x2e-88-100-qd132ws-thumb-639167035516702401-600x600.jpg	Laptop Asus Zenbook A14 UX3407NA - QD132WS (X2E 88 100, 16GB, 512GB, WUXGA OLED, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:00.908493
485	570	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367891/asus-zenbook-a14-ux3407na-x2e-88-100-qd254w-thumb-639167043864878325-600x600.jpg	Laptop Asus Zenbook A14 UX3407NA - QD254W (X2E 88 100, 32GB, 1TB, WUXGA OLED, Win11)	0	t	2026-09-24 21:16:00.94299
486	571	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/367897/asus-zenbook-duo-ux8407aa-ultra-x7-358h-sn439ws-thumb-639161081440362895-600x600.jpg	Laptop Asus Zenbook DUO UX8407AA - SN439WS (Ultra X7 358H, 32GB, 512GB, 3K OLED 144Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:00.976819
487	572	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368214/lenovo-ideapad-slim-3-16iph11-ultra-7-355-83us003yvn-thumb-639177452019843818-600x600.jpg	Laptop Lenovo IdeaPad Slim 3 16IPH11 - 83US003YVN (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.007613
488	573	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368650/acer-gaming-nitro-propanel-anv16s-61-r0b8-ryzen-ai-5-340-nh-qxqsv-001-thumb-639185333295671068-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV16S-61-R0B8 - NH.QXQSV.001 (Ryzen AI 5 340, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:16:01.038934
489	574	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368653/acer-gaming-predator-helios-neo-16s-phn16s-i51-97t9-ultra-9-386h-thumb-639185362228558355-600x600.jpg	Laptop Acer Gaming Predator Helios Neo 16S PHN16S-I51-97T9 - NH.U3SSV.006 (Ultra 9 386H, 16GB, 512GB, RTX 5070 8GB, 2K+ 165Hz, Win11)	0	t	2026-09-24 21:16:01.069255
490	575	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/368659/acer-gaming-nitro-propanel-an16s-61-r9cn-ryzen-ai-7-350-nh-qxgsv-001-thumb-639185140971644552-600x600.jpg	Laptop Acer Gaming Nitro ProPanel AN16S-61-R9CN - NH.QXGSV.001 (Ryzen AI 7 350, 16GB, 512GB, RTX 5070 8GB, 2K+ 180Hz, Win11)	0	t	2026-09-24 21:16:01.098317
491	576	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369460/asus-zenbook-14-um3406ga-r7-ai-445-qd075w-030926-102626-500-600x600.jpg	Laptop Asus Zenbook 14 UM3406GA - QD075W (R7 AI 445, 16GB, 512GB, WUXGA OLED, Win11)	0	t	2026-09-24 21:16:01.12855
492	577	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369462/asus-expertbook-ultra-b9-b9406caa-ultra-x7-358h-x7642tw-030926-102732-335-600x600.jpg	Laptop Asus ExpertBook Ultra B9 B9406CAA - X7642TW (Ultra X7 358H, 64GB, 2TB, WQXGA+ OLED 120Hz, Win11)	0	t	2026-09-24 21:16:01.159968
493	578	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369465/asus-expertbook-ultra-p5-p5406cca-ultra-7-255h-sf0047w-030926-103110-747-600x600.jpg	Laptop Asus ExpertBook P5 P5406CCA - SF0047W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.190852
494	579	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369466/asus-expertbook-ultra-p5-p5406cca-ultra-7-255-sf0241w-030926-103128-096-600x600.jpg	Laptop Asus ExpertBook P5 P5406CCA - SF0241W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.222842
495	580	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369476/lenovo-gaming-legion-5-15ahp11-r7-250-83q7001hvn-030926-103715-852-600x600.jpg	Laptop Lenovo Gaming Legion 5 15AHP11 - 83Q7001HVN (R7 250, 16GB, 512GB, RTX 5050 8GB, WQXGA OLED 165Hz, OfficeH24, Win11)	0	t	2026-09-24 21:16:01.253737
496	581	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369482/dell-16-dc16250-core-5-120u-71100513-thumb-639197553107946973-600x600.jpg	Laptop Dell 16 DC16250 - 71100513 (Core 5 120U, 16GB, 1TB, Full HD+, OfficeH24+365B, Win11)	0	t	2026-09-24 21:16:01.285539
497	582	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369751/msi-modern-14s-g3mg-003vn-core-5-320-thumb-639205050484094383-600x600.jpg	Laptop MSI Modern 14S G3MG-003VN (Core 5 320, 16GB, 1TB, Full HD+ 120Hz, Win11)	0	t	2026-09-24 21:16:01.31683
498	583	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369756/asus-proart-p1-h7606wp-r9-ai-hx-370-sr335w-030926-104651-166-600x600.jpg	Laptop Asus ProArt P1 H7606WP - SR335W (R9 AI HX 370, 64GB, 2TB, RTX 5070 8GB, 3K 120Hz, Cảm ứng, Win11)	0	t	2026-09-24 21:16:01.348262
499	584	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369981/acer-nitro-propanel-an16s-61-r7zj-r7-ai-350-nhqxfsv002-030926-104351-407-600x600.jpg	Laptop Acer Nitro ProPanel AN16S-61-R7ZJ - NH.QXFSV.002 (R7 AI 350, 16GB, 512GB, RTX 5060 8GB, 2K, Win11)	0	t	2026-09-24 21:16:01.380155
500	585	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/369982/acer-nitro-propanel-anv16s-61-r7kq-r5-ai-340-nhqxpsv001-030926-104409-047-600x600.jpg	Laptop Acer Nitro ProPanel ANV16S-61-R7KQ - NH.QXPSV.001 (R5 AI 340, 16GB, 512GB, RTX 5060 8GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:16:01.413533
501	586	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370278/acer-swift-go-ai-sfg14-75-765m-ultra-7-258v-nx-jnbsv-004-030926-104426-684-600x600.jpg	Laptop Acer Swift Go AI SFG14-75-765M - NX.JNBSV.004 (Ultra 7 258V, 32GB, 512GB, Full HD+, Win11)	0	t	2026-09-24 21:16:01.443963
502	587	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370279/acer-gaming-nitro-propanel-anv15-52-556l-core-5-210h-nh-qz9sv-006-thumb-639223047513550140-600x600.jpg	Laptop Acer Gaming Nitro ProPanel ANV15-52-556L - NH.QZ9SV.006 (Core 5 210H, 16GB, 512GB, RTX 5050 8GB, Full HD 180Hz, Win11)	0	t	2026-09-24 21:16:01.473027
503	588	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370353/lenovo-thinkpad-e14-gen-7-ultra-255h-21sx002yvn-030926-104511-385-600x600.jpg	Laptop Lenovo ThinkPad E14 - 21SX002YVN (Gen 7 Ultra 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.503509
504	589	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370354/lenovo-yoga-slim-7-14iph11-ultra-7-355-83qm0075vn-030926-104523-413-600x600.jpg	Laptop Lenovo Yoga Slim 7 14IPH11 - 83QM0075VN (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.534614
505	590	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370358/lenovo-yoga-slim-7-ultra-14iph11-ultra-x9-378h-83qk009pvn-030926-104607-468-600x600.jpg	Laptop Lenovo Yoga Slim 7 Ultra 14IPH11 - 83QK009PVN (Ultra X9 378H, 32GB, 1TB, WQXGA+ 120Hz, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.564722
506	591	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370582/msi-cyborg-15-max-c13we-422vn-i7-13620h-639229017667646454-600x600.jpg	Laptop MSI Cyborg 15 Max C13WE - 422VN (i7 13620H, 16GB, 512GB, RTX 5050 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:16:01.593751
507	592	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370583/msi-cyborg-15-max-c13wf-421vn-i7-13620h-639229016105296809-600x600.jpg	Laptop MSI Cyborg 15 Max C13WF - 421VN (i7 13620H, 16GB, 512GB, RTX 5060 8GB, Full HD 144Hz, Win11)	0	t	2026-09-24 21:16:01.622312
508	593	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370864/acer-swift-air-14-sfa14-i31-52bv-core-5-320-nx-w49sv-002-290826-093305-781-600x600.jpg	Laptop Acer Swift Air 14 SFA14-I31-52BV - NX.W49SV.002 (Core 5 320, 12GB, 512GB, Full HD 120Hz, Win11)	0	t	2026-09-24 21:16:01.651953
509	594	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370865/acer-swift-air-14-sfa14-i31-34nf-core-3-304-nx-w49sv-001-290826-093427-775-600x600.jpg	Laptop Acer Swift Air 14 SFA14-I31-34NF - NX.W49SV.001 (Core 3 304, 12GB, 512GB, WUXGA 120Hz, Win11)	0	t	2026-09-24 21:16:01.682403
510	595	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/370910/asus-expertbook-p3-p3406ccap-ultra-7-255h-u716s8w-030926-104621-864-600x600.jpg	Laptop Asus ExpertBook P3 P3406CCAP - U716S8W (Ultra 7 255H, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.713453
511	596	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371123/hp-15-fd2183tu-ultra-7-255u-dp2c0pa-thumb-639249775236666259-600x600.jpg	Laptop HP 15 fd2183TU - DP2C0PA (Ultra 7 255U, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.74471
512	597	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371124/hp-probook-4-g1a-14-r7-250-d9cp7at-thumb-639249903901745997-600x600.jpg	Laptop HP ProBook 4 G1a 14 - D9CP7AT (R7 250, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.776417
513	598	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371125/hp-probook-4-g1ah-16-r7-250-d9cp8at-thumb-639249830637353357-600x600.jpg	Laptop HP Probook 4 G1ah 16 - D9CP8AT (R7 250, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.810239
514	599	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371127/hp-probook-4-g2i-16-ultra-7-355-dk2y5at-thumb-639250219428167380-600x600.jpg	Laptop HP ProBook 4 G2i 16 - DK2Y5AT (Ultra 7 355, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:01.843333
515	600	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371128/hp-omnibook-ultra-14-kd0039tu-ultra-7-356h-d72c7pa-thumb-639250222051766460-600x600.jpg	Laptop HP OmniBook Ultra 14 kd0039TU - D72C7PA (Ultra 7 356H, 32GB, 1TB, 3K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.875507
516	601	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371129/hp-omnibook-ultra-14-kd0036tu-ultra-9-386h-d72c6pa-thumb-639250224032013655-600x600.jpg	Laptop HP OmniBook Ultra 14 kd0036TU - D72C6PA (Ultra 9 386H, 32GB, 1TB, 3K OLED 120Hz, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.905788
517	602	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371130/hp-omnibook-5-14-hh0075tu-ultra-5-322-d72ctpathumb-639250227082183848-600x600.jpg	Laptop HP OmniBook 5 14 hh0075TU - D72CTPA (Ultra 5 322, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.935991
518	603	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371132/hp-omnibook-5-14-hh0074tu-ultra-5-322-d72cspa-thumb-639250228655192328-600x600.jpg	Laptop HP OmniBook 5 14 hh0074TU - D72CSPA (Ultra 5 322, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.966422
519	604	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371133/hp-omnibook-5-14-hh0073tu-ultra-7-355-d72crpa-thumb-639250230536273413-600x600.jpg	Laptop HP OmniBook 5 14 hh0073TU - D72CRPA (Ultra 7 355, 16GB, 512GB, WUXGA, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:01.995889
520	605	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371134/hp-omnibook-5-14-hh0072tu-ultra-7-355-d72cqpa-thumb-639250232820367104-600x600.jpg	Laptop HP OmniBook 5 14 hh0072TU - D72CQPA (Ultra 7 355, 16GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:02.025919
521	606	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371135/hp-omnibook-x-flip-14-kb0048tu-ultra-7-355-d72bypa-thumb-639250235941671920-600x600.jpg	Laptop HP OmniBook X Flip 14 kb0048TU - D72BYPA (Ultra 7 355, 32GB, 512GB, WUXGA, Cảm ứng, OfficeH24+365, Win11)	0	t	2026-09-24 21:16:02.05735
522	607	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/44/371137/hp-probook-4-g2i-16-ultra-5-325-dk2x9at-thumb-639250216296975811-600x600.jpg	Laptop HP ProBook 4 G2i 16 - DK2X9AT (Ultra 5 325, 16GB, 512GB, WUXGA, Win11)	0	t	2026-09-24 21:16:02.088575
523	609	https://cdn.tgdd.vn/Products/Images/522/335308/ipad-11-wifi-yellow-thumb-600x600.jpg	Máy tính bảng iPad A16 WiFi 128GB	0	t	2026-09-24 21:16:02.121997
524	610	https://cdn.tgdd.vn/Products/Images/522/363417/ipad-air-m4-11-inch-wifi-128gb-xam-thumb-600x600.jpg	Máy tính bảng iPad Air M4 11 inch WiFi 128GB	0	t	2026-09-24 21:16:02.154696
525	611	https://cdn.tgdd.vn/Products/Images/522/361231/oppo-pad-5-8gb-256gb-hong-thumb-600x600.jpg	Máy tính bảng OPPO Pad 5 8GB/256GB	0	t	2026-09-24 21:16:02.187378
526	612	https://cdn.tgdd.vn/Products/Images/522/362716/xiaomi-pad-8-xanh-thumb-600x600.jpg	Máy tính bảng Xiaomi Pad 8 8GB/128GB	0	t	2026-09-24 21:16:02.219331
527	613	https://cdn.tgdd.vn/Products/Images/522/359089/samsung-galaxy-tab-a11-plus-5g-6gb-128gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab A11+ 5G 6GB/128GB	0	t	2026-09-24 21:16:02.252041
528	614	https://cdn.tgdd.vn/Products/Images/522/359086/samsung-galaxy-tab-a11-plus-wifi-6gb-128gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab A11+ WiFi 6GB/128GB	0	t	2026-09-24 21:16:02.282932
529	615	https://cdn.tgdd.vn/Products/Images/522/362718/xiaomi-pad-8-pro-xanh-la-thumb-600x600.jpg	Máy tính bảng Xiaomi Pad 8 Pro 8GB/128GB	0	t	2026-09-24 21:16:02.313564
530	616	https://cdn.tgdd.vn/Products/Images/522/339287/oppo-pad-se-sliver-thumb-600x600.jpg	Máy tính bảng OPPO Pad SE WiFi màn hình nhám 4GB/128GB	0	t	2026-09-24 21:16:02.343686
531	617	https://cdn.tgdd.vn/Products/Images/522/331229/ipad-mini-7-wifi-purple-thumb-600x600.jpg	Máy tính bảng iPad mini 7 WiFi 128GB	0	t	2026-09-24 21:16:02.372359
532	618	https://cdn.tgdd.vn/Products/Images/522/358082/ipad-pro-m5-wifi-11-inch-black-thumb-600x600.jpg	Máy tính bảng iPad Pro M5 11 inch WiFi 256GB	0	t	2026-09-24 21:16:02.400647
533	619	https://cdn.tgdd.vn/Products/Images/522/320992/oppo-pad-neo-grey-thumb-600x600.jpg	Máy tính bảng OPPO Pad Neo 4G 8GB/128GB	0	t	2026-09-24 21:16:02.428596
534	620	https://cdn.tgdd.vn/Products/Images/522/343061/samsung-galaxy-tab-s10-lite-5g-6gb-128gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10 Lite 5G 6GB/128GB	0	t	2026-09-24 21:16:02.4593
535	621	https://cdn.tgdd.vn/Products/Images/522/345548/samsung-galaxy-tab-a11-4g-4gb-64gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab A11 4G 4GB/64GB	0	t	2026-09-24 21:16:02.489965
536	622	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/345544/honor-pad-x7-wifi-4gb-64gb-150925-041634-575-600x600.jpg	Máy tính bảng HONOR Pad X7 WiFi 4GB/64GB	0	t	2026-09-24 21:16:02.523064
537	623	https://cdn.tgdd.vn/Products/Images/522/344721/samsung-galaxy-tab-s11-5g-12gb-128gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S11 5G 12GB/128GB	0	t	2026-09-24 21:16:02.55623
538	624	https://cdn.tgdd.vn/Products/Images/522/342548/lenovo-idea-tab-5g-blue-thumb-600x600.jpg	Máy tính bảng Lenovo Idea Tab 5G 8GB/128GB	0	t	2026-09-24 21:16:02.587751
539	625	https://cdn.tgdd.vn/Products/Images/522/339204/xiaomi-redmi-pad-2-gray-thumb-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 WiFi 4GB/128GB	0	t	2026-09-24 21:16:02.619115
540	626	https://cdn.tgdd.vn/Products/Images/522/335311/ipad-11-5g-sliver-thumb-600x600.jpg	Máy tính bảng iPad A16 5G 128GB	0	t	2026-09-24 21:16:02.649657
543	629	https://cdn.tgdd.vn/Products/Images/522/336740/samsung-galaxy-tab-s10-fe-plus-gray-thumb-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10 FE+ WiFi 8GB/128GB	0	t	2026-09-24 21:16:02.740279
544	630	https://cdn.tgdd.vn/Products/Images/522/322130/samsung-galaxy-tab-s10-plus-gray-thumb-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10+ WiFi 12GB/256GB	0	t	2026-09-24 21:16:02.771503
545	631	https://cdn.tgdd.vn/Products/Images/522/336738/samsung-galaxy-tab-s10-fe-blue-thumb-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10 FE 5G 8GB/128GB	0	t	2026-09-24 21:16:02.802035
546	632	https://cdn.tgdd.vn/Products/Images/522/339207/xiaomi-redmi-pad-2-4g-green-thumb-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 4G 4GB/128GB	0	t	2026-09-24 21:16:02.833002
547	633	https://cdn.tgdd.vn/Products/Images/522/356864/xiaomi-redmi-pad-2-pro-5g-6gb-128gb-bac-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 Pro 5G 6GB/128GB	0	t	2026-09-24 21:16:02.86286
548	634	https://cdn.tgdd.vn/Products/Images/522/339286/oppo-pad-se-sliver-thumb-600x600.jpg	Máy tính bảng OPPO Pad SE 4G 4GB/128GB	0	t	2026-09-24 21:16:02.893235
549	635	https://cdn.tgdd.vn/Products/Images/522/345546/samsung-galaxy-tab-a11-4g-4gb-64gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab A11 WiFi 4GB/64GB	0	t	2026-09-24 21:16:02.924489
550	636	https://cdn.tgdd.vn/Products/Images/522/365596/samsung-galaxy-tab-s10-lite-wifi-8gb-256gb-bac-thumb-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10 Lite Wifi 8GB/256GB	0	t	2026-09-24 21:16:02.957287
551	877	https://cdn.tgdd.vn/Products/Images/522/336737/samsung-galaxy-tab-s10-fe-silver-thumb-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S10 FE WiFi 8GB/128GB	0	t	2026-09-24 21:16:02.989565
552	637	https://cdn.tgdd.vn/Products/Images/522/366579/xiaomi-pad-2-wifi-9-7-bac-thumb-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 Wifi 9.7 4GB/64GB	0	t	2026-09-24 21:16:03.021668
553	638	https://cdn.tgdd.vn/Products/Images/522/366598/xiaomi-pad-2-4g-9-7-bac-thumb-600x600.jpg	Máy tính bảng Xiaomi Redmi Pad 2 4G 9.7 4GB/64GB	0	t	2026-09-24 21:16:03.055146
554	639	https://cdn.tgdd.vn/Products/Images/522/344723/samsung-galaxy-tab-s11-wifi-12gb-128gb-xam-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S11 WiFi 12GB/128GB	0	t	2026-09-24 21:16:03.08817
555	640	https://cdn.tgdd.vn/Products/Images/522/344725/samsung-galaxy-tab-s11-ultra-5g-12gb-256gb-xam-1-600x600.jpg	Máy tính bảng Samsung Galaxy Tab S11 Ultra 5G 12GB/256GB	0	t	2026-09-24 21:16:03.12036
556	641	https://cdn.tgdd.vn/Products/Images/522/363422/ipad-air-m4-13-inch-wifi-128gb-xam-thumb-600x600.jpg	Máy tính bảng iPad Air M4 13 inch WiFi 128GB	0	t	2026-09-24 21:16:03.154908
557	642	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/14139/363821/may-doc-sach-boox-savi-6s-6-inch-32gb-thumb-639093632007069723-600x600.jpg	Máy đọc sách Boox Savi 6S 6 inch 32GB	0	t	2026-09-24 21:16:03.187665
558	643	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/368575/lenovo-idea-tab-plus-8gb-256gb-230626-115451-715-600x600.jpg	Máy tính bảng Lenovo Idea Tab Plus Matte Edition WiFi 8GB/256GB	0	t	2026-09-24 21:16:03.219865
559	644	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/368516/lenovo-idea-tab-4gb-128gb-thumb-639174822505855767-600x600.jpg	Máy tính bảng Lenovo Idea Tab 4GB/128GB	0	t	2026-09-24 21:16:03.265777
560	645	https://cdn.tgdd.vn/Products/Images/522/358099/ipad-pro-m5-wifi-13-inch-sliver-thumb-600x600.jpg	Máy tính bảng iPad Pro M5 13 inch WiFi 256GB	0	t	2026-09-24 21:16:03.300949
561	646	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/14139/363825/may-doc-sach-new-kindle-2024-6-inch-16gb-thumb-639113432766230310-600x600.jpg	Máy đọc sách New Kindle 2024 6 inch 16 GB	0	t	2026-09-24 21:16:03.336468
562	647	https://cdn.tgdd.vn/Products/Images/522/363427/ipad-air-m4-11-inch-5g-128gb-tim-thumb-600x600.jpg	Máy tính bảng iPad Air M4 11 inch 5G 128GB	0	t	2026-09-24 21:16:03.371534
563	648	https://cdn.tgdd.vn/Products/Images/522/363432/ipad-air-m4-13-inch-5g-128gb-tim-thumb-600x600.jpg	Máy tính bảng iPad Air M4 13 inch 5G 128GB	0	t	2026-09-24 21:16:03.406944
564	649	https://cdn.tgdd.vn/Products/Images/522/358105/ipad-pro-m5-cellular-wifi-11-inch-black-thumb-600x600.jpg	Máy tính bảng iPad Pro M5 11 inch 5G 256GB	0	t	2026-09-24 21:16:03.445891
565	650	https://cdn.tgdd.vn/Products/Images/522/358111/ipad-pro-m5-cellular-wifi-13-inch-black-thumb-600x600.jpg	Máy tính bảng iPad Pro M5 13 inch 5G 256GB	0	t	2026-09-24 21:16:03.486069
566	651	https://cdn.tgdd.vn/Products/Images/522/368580/lenovo-legion-tab-gen-3-den-thumb-600x600.jpg	Máy tính bảng Lenovo Legion Tab Gen 3 WiFi 12GB/256GB	0	t	2026-09-24 21:16:03.521513
567	652	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/368577/lenovo-idea-tab-pro-gen-2-12gb-256gb-230626-120007-617-600x600.jpg	Máy tính bảng Lenovo Idea Tab Pro Gen 2 Matte Edition WiFi 12GB/256GB	0	t	2026-09-24 21:16:03.55741
568	653	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/14139/363822/combo-may-doc-sach-boox-savi-6-6-inch-32gb-thumb-639094435966501859-600x600.jpg	Combo máy đọc sách Boox Savi 6 6 inch 32 GB	0	t	2026-09-24 21:16:03.591037
569	654	https://cdn.tgdd.vn/Products/Images/522/368581/lenovo-legion-tab-gen-5-den-thumb-600x600.jpg	Máy tính bảng Lenovo Legion Tab Gen 5 WiFi 12GB/256GB	0	t	2026-09-24 21:16:03.622846
570	655	https://cdn.tgdd.vn/Products/Images/522/339828/honor-pad-x9a-thumb-600x600.jpg	Máy tính bảng HONOR Pad X9a 8GB/256GB	0	t	2026-09-24 21:16:03.653761
571	656	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/14139/363824/may-doc-sach-boox-palma2-6-inch-128gb-thumb-639088392755516852-600x600.jpg	Máy đọc sách Boox Palma2 6.13 inch 128 GB	0	t	2026-09-24 21:16:03.685381
572	657	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/368582/lenovo-yoga-tab-wifi-8gb-256gb-thumb-639178136549773217-600x600.jpg	Máy tính bảng Lenovo Yoga Tab WiFi 8GB/256GB	0	t	2026-09-24 21:16:03.716445
573	658	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/367014/honor-pad-x8b-6gb-128gb-thumb-639141134031302340-600x600.jpg	Máy tính bảng Honor Pad X8b 6GB/128GB	0	t	2026-09-24 21:16:03.750616
574	659	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/522/368579/lenovo-idea-tab-wifi-8gb-256gb-thumb-639178129612110739-600x600.jpg	Máy tính bảng Lenovo Idea Tab WiFi 8GB/256GB	0	t	2026-09-24 21:16:03.782509
575	660	https://cdn.tgdd.vn/Products/Images/7077/358003/dong-ho-dinh-vi-tre-em-kidcare-sight-s25-den-thumb-600x600.jpg	Đồng hồ định vị trẻ em Kidcare Sight S25	0	t	2026-09-24 21:16:03.815313
576	661	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/366954/huawei-watch-fit-5-42-9mm-day-nylon-thumb-3-639144521826494787-600x600.jpg	Huawei Watch Fit 5 42.9mm dây nylon	0	t	2026-09-24 21:16:03.850706
577	662	https://cdn.tgdd.vn/Products/Images/7077/354255/kidcare-k25-hong-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em Kidcare K25 4G	0	t	2026-09-24 21:16:03.88747
578	663	https://cdn.tgdd.vn/Products/Images/7077/366955/huawei-watch-fit-5-pro-44-5mm-day-nylon-thumb-600x600.jpg	Huawei Watch Fit 5 Pro 44.5mm dây nylon	0	t	2026-09-24 21:16:03.921487
579	664	https://cdn.tgdd.vn/Products/Images/7077/364509/huawei-watch-gt-runner-2-43-5mm-day-fluor-cam-thumb-1-600x600.jpg	Huawei Watch GT Runner 2 43.5mm dây Nylon	0	t	2026-09-24 21:16:03.954959
580	666	https://cdn.tgdd.vn/Products/Images/7077/344767/apple-watch-se-3-40mm-vien-nhom-day-the-thao-trang-600x600.jpg	Apple Watch SE 3 GPS 40mm viền nhôm dây thể thao	0	t	2026-09-24 21:16:03.989088
581	669	https://cdn.tgdd.vn/Products/Images/7077/344750/apple-watch-series-11-42mm-vien-nhom-day-the-thao-den-bong-600x600.jpg	Apple Watch Series 11 GPS 42mm viền nhôm dây thể thao	0	t	2026-09-24 21:16:04.021997
582	670	https://cdn.tgdd.vn/Products/Images/7077/367410/vong-deo-tay-thong-minh-mi-band-10-pro-vien-gom-thumb-600x600.jpg	Vòng đeo tay thông minh Mi Band 10 Pro viền gốm	0	t	2026-09-24 21:16:04.053747
583	671	https://cdn.tgdd.vn/Products/Images/7077/364290/amazfit-active-3-premium-46mm-day-silicone-trang-600x600.jpg	Amazfit Active 3 Premium 46mm dây silicone	0	t	2026-09-24 21:16:04.086879
584	672	https://cdn.tgdd.vn/Products/Images/7077/362942/vong-deo-tay-thong-minh-huawei-band-11-day-cao-su-den-thumb-1-600x600.jpg	Vòng đeo tay thông minh Huawei Band 11 dây Fluor	0	t	2026-09-24 21:16:04.119762
585	673	https://cdn.tgdd.vn/Products/Images/7077/369324/samsung-galaxy-watch-ultra-2-xanh-thumb-600x600.jpg	Samsung Galaxy Watch Ultra 2 LTE 47mm dây silicone	0	t	2026-09-24 21:16:04.15466
586	674	https://cdn.tgdd.vn/Products/Images/7077/361814/amazfit-active-max-46mm-day-silicone-thumb-600x600.jpg	Amazfit Active Max 46mm dây silicone	0	t	2026-09-24 21:16:04.190081
587	675	https://cdn.tgdd.vn/Products/Images/7077/362940/vong-deo-tay-thong-minh-huawei-band-11-pro-day-nylon-det-thumb-600x600.jpg	Vòng đeo tay thông minh Huawei Band 11 Pro dây nylon dệt	0	t	2026-09-24 21:16:04.225066
588	676	https://cdn.tgdd.vn/Products/Images/7077/344769/apple-watch-se-3-gps-cellular-40mm-vien-nhom-day-the-thao-trang-600x600.jpg	Apple Watch SE 3 GPS + Cellular 40mm viền nhôm dây thể thao	0	t	2026-09-24 21:16:04.26006
589	677	https://cdn.tgdd.vn/Products/Images/7077/369499/samsung-galaxy-watch9-bluetooth-40mm-day-silicone-thumb-trang-600x600.jpg	Samsung Galaxy Watch9 Bluetooth 40mm dây silicone	0	t	2026-09-24 21:16:04.295389
590	678	https://cdn.tgdd.vn/Products/Images/7077/339492/huawei-watch-5-46mm-day-composite-tb-600x600.jpg	Huawei Watch 5 46mm viền Titanium dây composite	0	t	2026-09-24 21:16:04.327034
591	679	https://cdn.tgdd.vn/Products/Images/7077/344758/apple-watch-series-11-gps-cellular-42m-vien-titanium-day-the-thao-vang-600x600.jpg	Apple Watch Series 11 GPS + Cellular 42mm viền Titanium dây thể thao	0	t	2026-09-24 21:16:04.361262
592	680	https://cdn.tgdd.vn/Products/Images/7077/337872/huawei-watch-5-46mm-den-tb-600x600.jpg	Huawei Watch 5 46mm viền thép dây cao su	0	t	2026-09-24 21:16:04.395269
593	681	https://cdn.tgdd.vn/Products/Images/7077/369498/samsung-galaxy-watch9-lte-40mm-day-silicone-thumb-trang-600x600.jpg	Samsung Galaxy Watch9 LTE 40mm dây silicone	0	t	2026-09-24 21:16:04.429318
594	682	https://cdn.tgdd.vn/Products/Images/7077/369497/samsung-galaxy-watch9-bluetooth-44mm-day-silicone-den-thumb-600x600.jpg	Samsung Galaxy Watch9 Bluetooth 44mm dây silicone	0	t	2026-09-24 21:16:04.463942
595	683	https://cdn.tgdd.vn/Products/Images/7077/369321/samsung-galaxy-watch9-lte-44mm-day-silicone-xanh-thumb-1-600x600.jpg	Samsung Galaxy Watch9 LTE 44mm dây silicone	0	t	2026-09-24 21:16:04.499972
596	684	https://cdn.tgdd.vn/Products/Images/7077/344764/apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-ocean-den-600x600.jpg	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Ocean	0	t	2026-09-24 21:16:04.53799
597	685	https://cdn.tgdd.vn/Products/Images/7077/336948/huawei-watch-5-42mm-hong-tb-600x600.jpg	Huawei Watch 5 42mm viền thép dây composite Vàng Hồng	0	t	2026-09-24 21:16:04.577445
598	686	https://cdn.tgdd.vn/Products/Images/7077/364292/amazfit-t-rex-ultra-2-51mm-day-silicone-thumb-2-600x600.jpg	Amazfit T-Rex Ultra 2 51mm dây silicone	0	t	2026-09-24 21:16:04.615764
599	687	https://cdn.tgdd.vn/Products/Images/7077/339531/mi-band-10-vien-gom-600x600.jpg	Vòng đeo tay thông minh Mi Band 10 viền gốm	0	t	2026-09-24 21:16:04.652222
600	688	https://cdn.tgdd.vn/Products/Images/7077/341453/huawei-watch-gt-6-pro-den-thumb-600x600.jpg	Huawei Watch GT 6 Pro 46mm viền Titanium dây cao su	0	t	2026-09-24 21:16:04.686866
601	689	https://cdn.tgdd.vn/Products/Images/7077/361330/oppo-watch-s-46mm-day-cao-su-den-600x600.jpg	OPPO Watch S 46mm dây cao su	0	t	2026-09-24 21:16:04.719142
602	690	https://cdn.tgdd.vn/Products/Images/7077/337643/amazfit-bip-6-do-tn-600x600.jpg	Amazfit Bip 6 46.3mm dây silicone	0	t	2026-09-24 21:16:04.75291
603	691	https://cdn.tgdd.vn/Products/Images/7077/362002/huawei-watch-gt-6-41mm-vien-thep-day-da-phoi-thumb-600x600.jpg	Huawei Watch GT 6 41mm viền thép dây da phối	0	t	2026-09-24 21:16:04.786716
604	692	https://cdn.tgdd.vn/Products/Images/7077/327697/samsung-galaxy-watch7-44mm-bac-tn2-600x600.jpg	Samsung Galaxy Watch7 44mm dây silicone	0	t	2026-09-24 21:16:04.818441
605	693	https://cdn.tgdd.vn/Products/Images/7077/354257/huawei-watch-gt-6-pro-46mm-thumb-600x600.jpg	Huawei Watch GT 6 Pro 46mm viền Titanium dây Woven	0	t	2026-09-24 21:16:04.851382
606	694	https://cdn.tgdd.vn/Products/Images/7077/354260/huawei-watch-gt-6-46mm-vien-thep-day-woven-thumb-600x600.jpg	Huawei Watch GT 6 46mm viền thép dây Woven	0	t	2026-09-24 21:16:04.883097
607	695	https://cdn.tgdd.vn/Products/Images/7077/361467/oppo-watch-s-46mm-day-nylon-thumb-2-600x600.jpg	OPPO Watch S 46mm dây nylon	0	t	2026-09-24 21:16:04.917125
608	696	https://cdn.tgdd.vn/Products/Images/7077/338266/samsung-galaxy-watch8-classic-trang-tn-600x600.jpg	Samsung Galaxy Watch8 Classic 46mm dây da	0	t	2026-09-24 21:16:04.959866
609	697	https://cdn.tgdd.vn/Products/Images/7077/354258/huawei-watch-gt-6-pro-46mm-vien-titanium-day-titanium-thumb-600x600.jpg	Huawei Watch GT 6 Pro 46mm viền Titanium dây Titanium	0	t	2026-09-24 21:16:05.006216
610	698	https://cdn.tgdd.vn/Products/Images/7077/354261/huawei-watch-gt-6-41mm-vien-thep-day-da-trang-thumb-600x600.jpg	Huawei Watch GT 6 41mm viền thép dây da	0	t	2026-09-24 21:16:05.057257
611	699	https://cdn.tgdd.vn/Products/Images/7077/354259/huawei-watch-gt-6-46mm-vien-thep-day-da-thumb-600x600.jpg	Huawei Watch GT 6 46mm viền thép dây da	0	t	2026-09-24 21:16:05.106648
612	700	https://cdn.tgdd.vn/Products/Images/7077/340066/samsung-galaxy-watch8-lte-40mm-trang-tn-600x600.jpg	Samsung Galaxy Watch8 LTE 40mm dây silicone	0	t	2026-09-24 21:16:05.17779
613	701	https://cdn.tgdd.vn/Products/Images/7077/361516/huawei-watch-gt-6-pro-honma-46mm-vien-titanium-day-cao-su-den-thumb-600x600.jpg	Huawei Watch GT 6 Pro Honma 46mm viền Titanium dây Fluor	0	t	2026-09-24 21:16:05.224339
614	702	https://cdn.tgdd.vn/Products/Images/7077/358004/amazfit-t-rex-3-pro-48mm-day-silicone-den-thumb-600x600.jpg	Amazfit T-Rex 3 Pro 48mm dây silicone	0	t	2026-09-24 21:16:05.26297
653	741	https://cdn.tgdd.vn/Products/Images/7077/332069/xiaomi-redmi-watch-5-bac-tb-1-600x600.jpg	Xiaomi Redmi Watch 5 47.5 mm dây TPU	0	t	2026-09-24 21:16:06.673467
615	703	https://cdn.tgdd.vn/Products/Images/7077/361518/huawei-watch-gt-6-41mm-vien-thep-day-milanese-thumb-600x600.jpg	Huawei Watch GT 6 41mm viền thép dây Milanese	0	t	2026-09-24 21:16:05.301671
616	704	https://cdn.tgdd.vn/Products/Images/7077/344753/apple-watch-series-11-gps-cellular-46mm-vien-nhom-day-the-thao-den-bong-600x600.jpg	Apple Watch Series 11 GPS + Cellular 46mm viền nhôm dây thể thao	0	t	2026-09-24 21:16:05.341885
617	705	https://cdn.tgdd.vn/Products/Images/7077/333919/imoo-z7-spiderman-thumb3-639229927300220715-600x600-600x600.jpg	Đồng hồ định vị trẻ em imoo Z7 Spider Man	0	t	2026-09-24 21:16:05.3816
618	706	https://cdn.tgdd.vn/Products/Images/7077/340067/samsung-galaxy-watch8-lte-44mm-den-tn-600x600.jpg	Samsung Galaxy Watch8 LTE 44mm dây silicone	0	t	2026-09-24 21:16:05.422581
619	707	https://cdn.tgdd.vn/Products/Images/7077/340068/samsung-galaxy-watch8-44mm-den-tb-600x600.jpg	Samsung Galaxy Watch8 44mm dây silicone	0	t	2026-09-24 21:16:05.462605
620	708	https://cdn.tgdd.vn/Products/Images/7077/340844/amazfit-active-2-square-sapphire-tn-600x600.jpg	Amazfit Active 2 Square Sapphire 43.3mm dây da	0	t	2026-09-24 21:16:05.50309
621	709	https://cdn.tgdd.vn/Products/Images/7077/327693/galaxy-watch-ultra-trang-tn-600x600.jpg	Samsung Galaxy Watch Ultra LTE 47mm dây silicone	0	t	2026-09-24 21:16:05.544341
622	710	https://cdn.tgdd.vn/Products/Images/7077/341454/amazfit-balance-2-den-tb-600x600.jpg	Amazfit Balance 2 47.4mm dây silicone	0	t	2026-09-24 21:16:05.596862
623	711	https://cdn.tgdd.vn/Products/Images/7077/344754/apple-watch-series-11-gps-cellular-42mm-vien-titanium-day-milan-xam-600x600.jpg	Apple Watch Series 11 GPS + Cellular 42mm viền Titanium dây Milan	0	t	2026-09-24 21:16:05.639307
624	712	https://cdn.tgdd.vn/Products/Images/7077/358006/huawei-watch-ultimate-2-47-8mm-day-cao-su-thumb-600x600.jpg	Huawei Watch Ultimate 2 47.8mm dây cao su	0	t	2026-09-24 21:16:05.676427
625	713	https://cdn.tgdd.vn/Products/Images/7077/358005/huawei-watch-ultimate-2-48-5mm-day-cao-su-thumb-600x600.jpg	Huawei Watch Ultimate 2 48.5mm dây cao su	0	t	2026-09-24 21:16:05.712779
626	714	https://cdn.tgdd.vn/Products/Images/7077/337834/samsung-galaxy-ring-bac-tb-600x600.jpg	Nhẫn thông minh Samsung Galaxy Ring Size 9	0	t	2026-09-24 21:16:05.746093
627	715	https://cdn.tgdd.vn/Products/Images/7077/370871/dong-ho-thong-min-gps-garmin-fenix-9-pro-solar-thumb-600x600.jpg	Garmin Fenix 9 Pro Solar Sapphire 47mm viền Titanium dây silicone	0	t	2026-09-24 21:16:05.780669
628	716	https://cdn.tgdd.vn/Products/Images/7077/370934/garmin-fenix-9-pro-sapphire-47mm-vien-titanium-carbon-day-silicone-thumb-600x600.jpg	Garmin Fenix 9 Pro Sapphire 47mm viền Titanium Carbon dây silicone	0	t	2026-09-24 21:16:05.814811
629	717	https://cdn.tgdd.vn/Products/Images/7077/370869/garmin-fenix-9-sapphire-43mm-vien-titanium-day-silicone-thumb-600x600.jpg	Garmin Fenix 9 Sapphire 43mm viền Titanium dây silicone	0	t	2026-09-24 21:16:05.847278
630	718	https://cdn.tgdd.vn/Products/Images/7077/370930/garmin-fenix-9-sapphire-47mm-vien-titanium-carbon-day-silicone-thumb-600x600.jpg	Garmin Fenix 9 Sapphire 47mm viền Titanium Carbon dây silicone	0	t	2026-09-24 21:16:05.879489
631	719	https://cdn.tgdd.vn/Products/Images/7077/370870/dong-ho-thong-min-gps-garmin-fenix-9-pro-amoled-thumb-600x600.jpg	Garmin Fenix 9 Pro Sapphire 43mm viền Titanium dây silicone	0	t	2026-09-24 21:16:05.911336
632	720	https://cdn.tgdd.vn/Products/Images/7077/328694/huawei-watch-kids-4-pro-xanh-tn2-600x600.jpg	Đồng hồ định vị trẻ em Huawei Watch Kids 4 Pro	0	t	2026-09-24 21:16:05.945035
633	721	https://cdn.tgdd.vn/Products/Images/7077/322848/garmin-forerunner-165-den-tb-600x600.jpg	Garmin Forerunner 165 43mm dây silicone	0	t	2026-09-24 21:16:05.977779
634	722	https://cdn.tgdd.vn/Products/Images/7077/367407/xiaomi-watch-s5-46mm-day-cao-su-fluoro-đen-thumb-600x600.jpg	Xiaomi Watch S5 46mm dây cao su Fluoro	0	t	2026-09-24 21:16:06.010856
635	723	https://cdn.tgdd.vn/Products/Images/7077/332406/mi-band-9-active-trang-thumb-600x600.jpg	Vòng đeo tay thông minh Mi Band 9 Active	0	t	2026-09-24 21:16:06.044333
636	724	https://cdn.tgdd.vn/Products/Images/7077/335762/huawei-band-10-den-tb-600x600.jpg	Vòng đeo tay thông minh Huawei Band 10 viền nhựa	0	t	2026-09-24 21:16:06.080973
637	725	https://cdn.tgdd.vn/Products/Images/7077/331078/huawei-watch-d2-day-cao-su-600x600.jpg	Huawei Watch D2 48mm dây cao su	0	t	2026-09-24 21:16:06.117328
638	726	https://cdn.tgdd.vn/Products/Images/7077/244296/garmin-forerunner-55-day-silicone-den-tn-1-2-600x600.jpg	Garmin Forerunner 55 42mm dây silicone	0	t	2026-09-24 21:16:06.153279
639	727	https://cdn.tgdd.vn/Products/Images/7077/332404/mi-band-9-pro-hong-tb-600x600.jpg	Vòng đeo tay thông minh Mi Band 9 Pro	0	t	2026-09-24 21:16:06.187573
640	728	https://cdn.tgdd.vn/Products/Images/7077/365405/garmin-forerunner-70-xanh-thumb-600x600.jpg	Garmin Forerunner 70 42.6mm dây silicone	0	t	2026-09-24 21:16:06.219663
641	729	https://cdn.tgdd.vn/Products/Images/7077/355663/garmin-venu-4-41mm-day-silicone-trang-thumb-600x600.jpg	Garmin Venu 4 41mm dây silicone	0	t	2026-09-24 21:16:06.253035
642	730	https://cdn.tgdd.vn/Products/Images/7077/337137/garmin-vivoactive-6-den-tb-600x600.jpg	Garmin Vivoactive 6 42.2mm dây silicone	0	t	2026-09-24 21:16:06.28744
643	731	https://cdn.tgdd.vn/Products/Images/7077/338698/garmin-forerunner-970-xam-600x600.jpg	Garmin Forerunner 970 47mm dây silicone	0	t	2026-09-24 21:16:06.321956
644	732	https://cdn.tgdd.vn/Products/Images/7077/338701/garmin-forerunner-570-47mm-trang-600x600.jpg	Garmin Forerunner 570 47mm dây silicone	0	t	2026-09-24 21:16:06.353217
645	733	https://cdn.tgdd.vn/Products/Images/7077/365406/garmin-forerunner-170-trang-thumb-600x600.jpg	Garmin Forerunner 170 42.6mm dây silicone	0	t	2026-09-24 21:16:06.384319
646	734	https://cdn.tgdd.vn/Products/Images/7077/334987/garmin-instinct-3-50mm-den-thumb-600x600.jpg	Garmin Instinct 3 50mm dây silicone	0	t	2026-09-24 21:16:06.417651
647	735	https://cdn.tgdd.vn/Products/Images/7077/330180/huawei-watch-gt-5-pro-41mm-vien-gom-day-gom-tb-600x600.jpg	Huawei Watch GT 5 Pro 42mm viền gốm dây gốm	0	t	2026-09-24 21:16:06.452421
648	736	https://cdn.tgdd.vn/Products/Images/7077/334986/garmin-instinct-3-45mm-den-thumb-600x600.jpg	Garmin Instinct 3 45mm dây silicone	0	t	2026-09-24 21:16:06.488216
649	737	https://cdn.tgdd.vn/Products/Images/7077/334989/garmin-instinct-e-45mm-den-tb-600x600.jpg	Garmin Instinct E 45mm dây silicone	0	t	2026-09-24 21:16:06.524394
650	738	https://cdn.tgdd.vn/Products/Images/7077/340846/garmin-venu-x1-reu-tb-600x600.jpg	Garmin Venu X1 Sapphire 51.2mm dây nylon	0	t	2026-09-24 21:16:06.563976
651	739	https://cdn.tgdd.vn/Products/Images/7077/365217/xiaomi-redmi-watch-6-den-600x600.jpg	Xiaomi Redmi Watch 6	0	t	2026-09-24 21:16:06.60088
652	740	https://cdn.tgdd.vn/Products/Images/7077/341452/huawei-watch-gt-6-den-thumb-600x600.jpg	Huawei Watch GT 6 46mm viền thép dây cao su	0	t	2026-09-24 21:16:06.63821
656	744	https://cdn.tgdd.vn/Products/Images/7077/354262/huawei-watch-gt-6-41mm-vien-thep-day-cao-su-tim-thumb-600x600.jpg	Huawei Watch GT 6 41mm viền thép dây cao su	0	t	2026-09-24 21:16:06.77645
657	745	https://cdn.tgdd.vn/Products/Images/7077/341442/xiaomi-watch-s4-41mm-xanh-thumb-1-600x600.jpg	Xiaomi Watch S4 41mm dây cao su Fluoro	0	t	2026-09-24 21:16:06.808259
658	746	https://cdn.tgdd.vn/Products/Images/7077/338265/samsung-galaxy-watch8-40mm-trang-tb-600x600.jpg	Samsung Galaxy Watch8 40mm dây silicone	0	t	2026-09-24 21:16:06.840078
659	747	https://cdn.tgdd.vn/Products/Images/7077/344809/apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-milan-den-600x600.jpg	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Milan	0	t	2026-09-24 21:16:06.873252
660	748	https://cdn.tgdd.vn/Products/Images/7077/354314/xiaomi-watch-s4-41mm-day-milanese-thumb-600x600.jpg	Xiaomi Watch S4 41mm dây Milanese	0	t	2026-09-24 21:16:06.908738
661	749	https://cdn.tgdd.vn/Products/Images/7077/329474/garmin-fenix-8-sapphire-47mm-den-tb-600x600.jpg	Garmin Fenix 8 Sapphire 47mm viền Titanium dây silicone	0	t	2026-09-24 21:16:06.943355
662	750	https://cdn.tgdd.vn/Products/Images/7077/335516/xiaomi-watch-s4-den-tn-600x600.jpg	Xiaomi Watch S4 47mm dây silicone Đen Cầu Vồng	0	t	2026-09-24 21:16:06.976547
663	751	https://cdn.tgdd.vn/Products/Images/7077/360220/amazfit-t-rex-3-pro-44mm-day-silicone-den-vang-thumb-1-600x600.jpg	Amazfit T-Rex 3 Pro 44mm dây silicone	0	t	2026-09-24 21:16:07.010083
664	752	https://cdn.tgdd.vn/Products/Images/7077/340065/samsung-galaxy-watch8-classic-lte-46mm-den-tn-600x600.jpg	Samsung Galaxy Watch8 Classic LTE 46mm dây da	0	t	2026-09-24 21:16:07.048233
665	753	https://cdn.tgdd.vn/Products/Images/7077/344765/apple-watch-ultra-3-gps-cellular-49mm-vien-titanium-day-trail-den-600x600.jpg	Apple Watch Ultra 3 GPS + Cellular 49mm viền Titanium dây Trail	0	t	2026-09-24 21:16:07.090099
666	754	https://cdn.tgdd.vn/Products/Images/7077/329473/garmin-fenix-8-sapphire-43mm-thumb-2-600x600.jpg	Garmin Fenix 8 Sapphire 43mm viền Titanium dây silicone	0	t	2026-09-24 21:16:07.12726
667	755	https://cdn.tgdd.vn/Products/Images/7077/329472/garmin-fenix-8-47mm-tb-600x600.jpg	Garmin Fenix 8 47mm viền thép dây silicone	0	t	2026-09-24 21:16:07.163956
668	756	https://cdn.tgdd.vn/Products/Images/7077/329479/garmin-fenix-e-47mm-tb-600x600.jpg	Garmin Fenix E 47mm viền thép dây silicone	0	t	2026-09-24 21:16:07.201121
669	757	https://cdn.tgdd.vn/Products/Images/7077/329468/garmin-fenix-8-tb-600x600.jpg	Garmin Fenix 8 43mm viền thép dây silicone	0	t	2026-09-24 21:16:07.234716
670	758	https://cdn.tgdd.vn/Products/Images/7077/333790/kidcare-sight-s1-xanhla-thumb-600x600.jpg	Đồng hồ định vị trẻ em Kidcare Sight S1	0	t	2026-09-24 21:16:07.269198
671	759	https://cdn.tgdd.vn/Products/Images/7077/316991/dong-ho-dinh-vi-tre-em-imoo-z1-41-mm-hong-nhat-600x600.jpg	Đồng hồ định vị trẻ em imoo Z1 Hồng Nhạt	0	t	2026-09-24 21:16:07.30495
672	760	https://cdn.tgdd.vn/Products/Images/7077/338267/galaxy-watch-ultra-2025-xanh-tn-600x600.jpg	Samsung Galaxy Watch Ultra LTE 47mm (2025) dây silicone	0	t	2026-09-24 21:16:07.343235
673	761	https://cdn.tgdd.vn/Products/Images/7077/318631/zwatch-z6-44mm-day-silicone-xanh-duong-thumb-10-600x600.jpg	Đồng hồ thông minh Zwatch Z6 44mm Xanh dương	0	t	2026-09-24 21:16:07.382632
674	762	https://cdn.tgdd.vn/Products/Images/7077/322845/garmin-lily-2-classic-day-vai-nau-tb-600x600.jpg	Garmin Lily 2 Classic 34mm dây vải	0	t	2026-09-24 21:16:07.41731
675	763	https://cdn.tgdd.vn/Products/Images/7077/329076/dong-ho-dinh-vi-tre-em-kidcare-k1-hong-tn-600x600.jpg	Đồng hồ định vị trẻ em Kidcare K1	0	t	2026-09-24 21:16:07.454002
676	764	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/336875/amazfit-active-2-premium-tb-638799784238179304-600x600.jpg	Amazfit Active 2 Sapphire 43.9mm dây da	0	t	2026-09-24 21:16:07.491181
677	765	https://cdn.tgdd.vn/Products/Images/7077/335088/amazfit-active-2-tb-600x600.jpg	Amazfit Active 2 43.9mm dây silicone	0	t	2026-09-24 21:16:07.532322
678	766	https://cdn.tgdd.vn/Products/Images/7077/330159/huawei-watch-gt-5-pro-46mm-day-titanium-tb-600x600.jpg	Huawei Watch GT 5 Pro 46mm viền Titanium dây Titanium	0	t	2026-09-24 21:16:07.575173
679	767	https://cdn.tgdd.vn/Products/Images/7077/315897/garmin-vivoactive-5-day-silicone-1-1-600x600.jpg	Garmin Vivoactive 5 42.2mm dây silicone	0	t	2026-09-24 21:16:07.620094
680	768	https://cdn.tgdd.vn/Products/Images/7077/324886/mykid-4g-lite-xanh-tn-600x600.jpg	Đồng hồ định vị trẻ em MyKid 4G Lite	0	t	2026-09-24 21:16:07.659799
681	769	https://cdn.tgdd.vn/Products/Images/7077/288629/masstel-smart-hero-10-thumbnew-600x600.jpg	Đồng hồ định vị trẻ em Masstel Smart Hero 10	0	t	2026-09-24 21:16:07.698216
682	770	https://cdn.tgdd.vn/Products/Images/7077/236901/dong-ho-kidcare-s6-xanh-new-tn-600x600.jpg	Đồng hồ định vị trẻ em Kidcare S6 4G Xanh Dorablue	0	t	2026-09-24 21:16:07.734299
683	771	https://cdn.tgdd.vn/Products/Images/7077/236904/dong-ho-kidcare-s6-xanh-tn-600x600.jpg	Đồng hồ định vị trẻ em Kidcare S6 4G	0	t	2026-09-24 21:16:07.770223
684	772	https://cdn.tgdd.vn/Products/Images/7077/339434/masstel-smart-hero-star-hong-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em Masstel Smart Hero Star	0	t	2026-09-24 21:16:07.807752
685	773	https://cdn.tgdd.vn/Products/Images/7077/367385/dong-ho-dinh-vi-tre-em-kidcare-sight-s5-4g-den-thumb-600x600.jpg	Đồng hồ định vị trẻ em Kidcare Sight S5 4G	0	t	2026-09-24 21:16:07.845479
686	774	https://cdn.tgdd.vn/Products/Images/7077/362629/kidcare-sight-s26-4g-44-5mm-day-cao-su-hong-thumb-600x600.jpg	Đồng hồ định vị trẻ em Kidcare Sight S26 4G	0	t	2026-09-24 21:16:07.884825
687	775	https://cdn.tgdd.vn/Products/Images/7077/326906/masstel-smart-hero-6-xanh-tb-1-600x600.jpg	Đồng hồ định vị trẻ em Masstel Smart Hero 6	0	t	2026-09-24 21:16:07.921852
688	776	https://cdn.tgdd.vn/Products/Images/7077/337061/masstel-smart-hero-30-den-tb-600x600.jpg	Đồng hồ định vị trẻ em Masstel Smart Hero 30	0	t	2026-09-24 21:16:07.961043
689	777	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/370087/xiaomi-redmi-watch-6-active-47-mm-day-tpu-2-639216261893156726-600x600.jpg	Xiaomi Redmi Watch 6 Active 47 mm dây TPU	0	t	2026-09-24 21:16:08.000625
690	778	https://cdn.tgdd.vn/Products/Images/7077/369986/xiaomi-redmi-watch-6-lite-48-mm-day-tpu-den-thumb-600x600.jpg	Xiaomi Redmi Watch 6 Lite 48 mm dây TPU	0	t	2026-09-24 21:16:08.039246
691	779	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/370976/vong-tay-thong-minh-samsung-galaxy-fit3-hong-thumb-639240312254439754-600x600.jpg	Vòng tay thông minh Samsung Galaxy Fit3 - Hồng	0	t	2026-09-24 21:16:08.076857
692	780	https://cdn.tgdd.vn/Products/Images/7077/368547/dong-ho-dinh-vi-tre-em-tammi-watch-kid-plus-hong-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em Tammi Watch Kid Plus	0	t	2026-09-24 21:16:08.115299
693	781	https://cdn.tgdd.vn/Products/Images/7077/368548/dong-ho-dinh-vi-tre-em-tammi-watch-kid-max-hong-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em Tammi Watch Kid Max	0	t	2026-09-24 21:16:08.15114
694	782	https://cdn.tgdd.vn/Products/Images/7077/358457/dong-ho-dinh-vi-tre-em-imoo-z3-xanh-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em imoo Z3	0	t	2026-09-24 21:16:08.186377
695	783	https://cdn.tgdd.vn/Products/Images/7077/370841/dong-ho-dinh-vi-tre-em-huawei-dra-l10-kids-x1-thumb-xanh-1-600x600.jpg	Đồng hồ định vị trẻ em Huawei Kids DRA-L10 X1	0	t	2026-09-24 21:16:08.221364
696	784	https://cdn.tgdd.vn/Products/Images/7077/327123/mykid-4g-v2-day-silicone-den-tn-600x600.jpg	Đồng hồ định vị trẻ em MyKid 4G V2	0	t	2026-09-24 21:16:08.256185
697	785	https://cdn.tgdd.vn/Products/Images/7077/344999/realme-watch-5-50mm-day-silicone-bac-thumb-600x600.jpg	realme Watch 5 50mm dây silicone	0	t	2026-09-24 21:16:08.290543
698	786	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/370088/vong-deo-tay-thong-minh-mi-band-11-active-thumb-639220452597724041-600x600.jpg	Vòng đeo tay thông minh Mi Band 11 Active	0	t	2026-09-24 21:16:08.325215
699	787	https://cdn.tgdd.vn/Products/Images/7077/313829/garmin-venu-3s-day-silicone-hong-tn2-600x600.jpg	Garmin Venu 3S 41mm dây silicone	0	t	2026-09-24 21:16:08.361178
700	788	https://cdn.tgdd.vn/Products/Images/7077/359399/zobo-g1-42.3mm-day-silicone-thumb-1-600x600.jpg	Zobo G1 42.3mm dây silicone	0	t	2026-09-24 21:16:08.397487
701	789	https://cdn.tgdd.vn/Products/Images/7077/359401/zobo-sporty-1-42mm-day-silicone-thumb-600x600.png	Zobo Sporty 1 42mm dây silicone	0	t	2026-09-24 21:16:08.432153
702	790	https://cdn.tgdd.vn/Products/Images/7077/322267/amazfit-active-den-tb-1-600x600.jpg	Amazfit Active 42.3mm dây silicone	0	t	2026-09-24 21:16:08.467644
703	791	https://cdn.tgdd.vn/Products/Images/7077/363022/xiaomi-watch-5-47-mm-day-cao-su-fluoro-xanh-600x600.jpg	Xiaomi Watch 5 47 mm dây cao su Fluoro	0	t	2026-09-24 21:16:08.501857
704	792	https://cdn.tgdd.vn/Products/Images/7077/329598/dong-ho-dinh-vi-tre-em-imoo-z7-xanh-600x600.jpg	Đồng hồ định vị trẻ em imoo Z7	0	t	2026-09-24 21:16:08.536217
705	793	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/369501/dong-ho-dinh-vi-tre-em-zobo-k2-thumb-639198203753458889-600x600.jpg	Đồng hồ định vị trẻ em Zobo K2	0	t	2026-09-24 21:16:08.570411
706	794	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/369675/amazfit-bip-balance-3-51-4mm-day-silicone-thumb-639208472873338677-600x600.jpg	Amazfit Balance 3 51.4mm dây silicone	0	t	2026-09-24 21:16:08.603926
707	795	https://cdn.tgdd.vn/Products/Images/7077/305882/garmin-forerunner-265-den-tn-2-600x600.jpg	Garmin Forerunner 265 Music 46.1mm dây silicone	0	t	2026-09-24 21:16:08.636214
708	796	https://cdn.tgdd.vn/Products/Images/7077/329945/amazfit-t-rex-3-cam-tn-600x600.jpg	Amazfit T-Rex 3 47.1mm dây silicone	0	t	2026-09-24 21:16:08.669326
709	797	https://cdn.tgdd.vn/Products/Images/7077/359400/zobo-novabiz-3-50.2mm-day-silicone-thumb-600x600.jpg	Zobo Novabiz 3 50.2mm dây silicone	0	t	2026-09-24 21:16:08.701042
710	798	https://cdn.tgdd.vn/Products/Images/7077/367419/dong-ho-dinh-vi-tre-em-imoo-x10-xam-thumb-1-600x600.jpg	Đồng hồ định vị trẻ em imoo X10	0	t	2026-09-24 21:16:08.731786
711	799	https://cdn.tgdd.vn/Products/Images/7077/322839/garmin-lily-2-tim-tb-600x600.jpg	Garmin Lily 2 34mm dây silicone	0	t	2026-09-24 21:16:08.762842
712	800	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/366928/oppo-watch-x3-47-4mm-day-cao-su-thumb-639143692533934845-600x600.jpg	OPPO Watch X3 47.4mm dây cao su	0	t	2026-09-24 21:16:08.794382
713	801	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/369504/dong-ho-dinh-vi-tre-em-zobo-s8-thumb-639198240874233418-600x600.jpg	Đồng hồ định vị trẻ em Zobo S8	0	t	2026-09-24 21:16:08.830573
714	802	https://cdn.tgdd.vn/Products/Images/7077/308291/garmin-golf-approach-s70-day-silicone-tb-1-600x600.jpg	Garmin Golf Approach S70 47mm dây silicone	0	t	2026-09-24 21:16:08.863966
715	803	https://cdn.tgdd.vn/Products/Images/7077/310710/garmin-forerunner-265s-den-day-silicone-tb-600x600.jpg	Garmin Forerunner 265S 41.7mm dây silicone	0	t	2026-09-24 21:16:08.897442
716	804	https://cdn.tgdd.vn/Products/Images/7077/322375/amazfit-balance-day-vai-tn2-600x600.jpg	Amazfit Balance 46mm dây nylon	0	t	2026-09-24 21:16:08.931874
717	805	https://cdn.tgdd.vn/Products/Images/7077/322846/garmin-lily-2-classic-day-da-den-tim-tb-600x600.jpg	Garmin Lily 2 Classic 34mm dây da	0	t	2026-09-24 21:16:08.965374
718	806	https://cdn.tgdd.vn/Products/Images/7077/329514/garmin-golf-approach-s70-42mm-xam-tb-600x600.jpg	Garmin Golf Approach S70 42mm dây silicone	0	t	2026-09-24 21:16:09.000568
719	807	https://cdn.tgdd.vn/Products/Images/7077/330793/garmin-lily-2-classic-day-vai-tim-tb-600x600.jpg	Garmin Lily 2 Active 38mm dây silicone	0	t	2026-09-24 21:16:09.038431
720	808	https://cdn.tgdd.vn/Products/Images/7077/334983/garmin-instinct-3-solar-45mm-den-tb-600x600.jpg	Garmin Instinct 3 Solar 45mm dây silicone	0	t	2026-09-24 21:16:09.073893
721	809	https://cdn.tgdd.vn/Products/Images/7077/335627/garmin-golf-approach-s50-43mm-trang-tb-600x600.jpg	Garmin Approach S50 43mm dây nylon	0	t	2026-09-24 21:16:09.109216
722	810	https://cdn.tgdd.vn/Products/Images/7077/337773/size-kit-600x600.jpg	Bộ kit đo size nhẫn	0	t	2026-09-24 21:16:09.144465
723	811	https://cdn.tgdd.vn/Products/Images/7077/339863/oppo-watch-x2-mini-day-da-tn-600x600.jpg	OPPO Watch X2 mini 43mm dây da	0	t	2026-09-24 21:16:09.179832
724	812	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/364650/garmin-fenix-8-sapphire-43mm-vien-thep-day-silicone-thumb-639096978552017615-600x600.jpg	Garmin Fenix 8 Sapphire 43mm viền thép dây silicone	0	t	2026-09-24 21:16:09.214981
725	813	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7077/368303/amazfit-cheetah-2-pro-48mm-day-silicone-thumb-639174741652171741-600x600.jpg	Amazfit Cheetah 2 Pro 48mm dây silicone	0	t	2026-09-24 21:16:09.249355
726	814	https://cdn.tgdd.vn/Products/Images/7077/369503/dong-ho-dinh-vi-tre-em-zobo-s6-thumb-den-600x600.jpg	Đồng hồ định vị trẻ em Zobo S6	0	t	2026-09-24 21:16:09.282191
727	815	https://cdn.tgdd.vn/Products/Images/7077/337830/samsung-galaxy-ring-den-tb-600x600.jpg	Nhẫn thông minh Samsung Galaxy Ring Size 6	0	t	2026-09-24 21:16:09.318536
728	816	https://cdn.tgdd.vn/Products/Images/7077/337840/samsung-galaxy-ring-bac-tb-600x600.jpg	Nhẫn thông minh Samsung Galaxy Ring Size 15	0	t	2026-09-24 21:16:09.353566
729	817	https://cdn.tgdd.vn/Products/Images/7077/364063/suunto-run-46mm-day-nylon-vang-thumb-600x600.jpg	Suunto Run 46mm dây nylon	0	t	2026-09-24 21:16:09.38983
730	818	https://cdn.tgdd.vn/Products/Images/7077/364064/suunto-race-s-45mm-day-silicone-cam-thumb-600x600.jpg	Suunto Race S 45mm dây silicone	0	t	2026-09-24 21:16:09.423814
731	819	https://cdn.tgdd.vn/Products/Images/7077/364065/suunto-race-s-titanium-45mm-day-silicone-trang-thumb-1-600x600.jpg	Suunto Race S Titanium 45mm dây silicone	0	t	2026-09-24 21:16:09.459816
732	820	https://cdn.tgdd.vn/Products/Images/7077/371218/apple-watch-series-12-gps-42mm-vien-nhom-day-silicone-dong-thumb-600x600.jpg	Apple Watch Series 12 GPS 42mm viền nhôm dây silicone	0	t	2026-09-24 21:16:09.494306
733	821	https://cdn.tgdd.vn/Products/Images/7077/371219/apple-watch-series-12-gps-46mm-vien-nhom-day-silicone-dong-thumb-600x600.jpg	Apple Watch Series 12 GPS 46mm viền nhôm dây silicone	0	t	2026-09-24 21:16:09.529741
734	822	https://cdn.tgdd.vn/Products/Images/7077/371221/apple-watch-series-12-gps-cellular-42mm-vien-nhom-day-silicone-dong-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 42mm viền nhôm dây silicone	0	t	2026-09-24 21:16:09.564413
735	823	https://cdn.tgdd.vn/Products/Images/7077/371223/apple-watch-series-12-gps-cellular-42mm-vien-titanium-day-silicone-gold-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 42mm viền Titanium dây silicone	0	t	2026-09-24 21:16:09.599551
736	824	https://cdn.tgdd.vn/Products/Images/7077/371225/apple-watch-series-12-gps-cellular-42mm-vien-titanium-day-milan-gold-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 42mm viền Titanium dây Milan	0	t	2026-09-24 21:16:09.634495
737	825	https://cdn.tgdd.vn/Products/Images/7077/371226/apple-watch-series-12-gps-cellular-46mm-vien-nhom-day-silicone-dong-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 46mm viền nhôm dây silicone	0	t	2026-09-24 21:16:09.669669
738	826	https://cdn.tgdd.vn/Products/Images/7077/371228/apple-watch-series-12-gps-cellular-46mm-vien-titanium-day-silicone-gold-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 46mm viền Titanium dây silicone	0	t	2026-09-24 21:16:09.70327
739	827	https://cdn.tgdd.vn/Products/Images/7077/371229/apple-watch-series-12-gps-cellular-46mm-vien-titanium-day-milan-gold-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 46mm viền Titanium dây Milan	0	t	2026-09-24 21:16:09.736983
740	828	https://cdn.tgdd.vn/Products/Images/7077/371230/apple-watch-series-12-gps-cellular-42mm-vien-ceramic-day-silicone-trang-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 42mm viền Ceramic dây silicone	0	t	2026-09-24 21:16:09.770493
741	829	https://cdn.tgdd.vn/Products/Images/7077/371231/apple-watch-series-12-gps-cellular-46mm-vien-ceramic-day-silicone-trang-thumb-600x600.jpg	Apple Watch Series 12 GPS + Cellular 46mm viền Ceramic dây silicone	0	t	2026-09-24 21:16:09.804472
742	830	https://cdn.tgdd.vn/Products/Images/7077/371235/apple-watch-ultra-4-gps-cellular-49mm-vien-titanium-day-milan-titan-tu-nhien-thumb-600x600.jpg	Apple Watch Ultra 4 GPS + Cellular 49mm viền Titanium dây Milan	0	t	2026-09-24 21:16:09.837915
743	831	https://cdn.tgdd.vn/Products/Images/7077/371332/apple-watch-se-3-gps-40mm-vien-nhom-day-the-thao-2026-navy-thumb-600x600.jpg	Apple Watch SE 3 GPS 40mm viền nhôm dây thể thao 2026	0	t	2026-09-24 21:16:09.869055
744	832	https://cdn.tgdd.vn/Products/Images/7077/371333/apple-watch-se-3-gps-cellular-40mm-vien-nhom-day-the-thao-2026-navy-thumb-600x600.jpg	Apple Watch SE 3 GPS + Cellular 40mm viền nhôm dây thể thao 2026	0	t	2026-09-24 21:16:09.902847
745	833	https://cdn.tgdd.vn/Products/Images/7077/371335/apple-watch-se-3-gps-44mm-vien-nhom-day-the-thao-2026-starlight-thumb-600x600.jpg	Apple Watch SE 3 GPS 44mm viền nhôm dây thể thao 2026	0	t	2026-09-24 21:16:09.934723
746	834	https://cdn.tgdd.vn/Products/Images/7077/371336/apple-watch-se-3-gps-cellular-44mm-vien-nhom-day-the-thao-2026-starlight-thumb-600x600.jpg	Apple Watch SE 3 GPS + Cellular 44mm viền nhôm dây thể thao 2026	0	t	2026-09-24 21:16:09.968742
747	835	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364640/orient-ra-ak0315l30b-nam-thumb-639099710537900990-600x600.jpg	Đồng hồ Orient 41.5 mm Nam RA-AK0315L30B	0	t	2026-09-24 21:16:10.002906
748	836	https://cdn.tgdd.vn/Products/Images/7264/326940/baby-g-bgd-565sj-9dr-nu-thumb-600x600.jpg	Đồng hồ Baby-G 37.9 mm Nữ BGD-565SJ-9DR	0	t	2026-09-24 21:16:10.036622
749	837	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364641/orient-ra-ak0316l30b-nam-thumb-639099718185649895-600x600.jpg	Đồng hồ Orient 41.5 mm Nam RA-AK0316L30B	0	t	2026-09-24 21:16:10.068197
750	838	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364642/orient-re-au0112v00b-nam-thumb-639099730417630213-600x600.jpg	Đồng hồ Orient 38.5 mm Nam RE-AU0112V00B	0	t	2026-09-24 21:16:10.101646
751	839	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364643/orient-re-au0114e00b-nam-thumb-639099737647946034-600x600.jpg	Đồng hồ Orient 38.5 mm Nam RE-AU0114E00B	0	t	2026-09-24 21:16:10.135391
752	840	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364646/orient-re-av0138v00b-nam-thumb-639099754090826637-600x600.jpg	Đồng hồ Orient 41 mm Nam RE-AV0138V00B	0	t	2026-09-24 21:16:10.168944
753	841	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/330392/orient-ra-as0010s30b-nam-thumb-638642634684583483-600x600.jpg	Đồng hồ Orient Sun & Moon 42 mm Nam RA-AS0010S30B	0	t	2026-09-24 21:16:10.20292
754	842	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/331125/orient-ra-tx0306s10b-nam-thumb-638654690266131155-600x600.jpg	Đồng hồ Orient Contemporary 40 mm Nam RA-TX0306S10B	0	t	2026-09-24 21:16:10.237276
755	843	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364639/orient-ra-ak0314e30b-nam-thumb-639099693080179572-600x600.jpg	Đồng hồ Orient 41.5 mm Nam RA-AK0314E30B	0	t	2026-09-24 21:16:10.271136
756	844	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/364638/orient-ra-ak0313y30b-nam-thumb-639099675664269061-600x600.jpg	Đồng hồ Orient 41.5 mm Nam RA-AK0313Y30B	0	t	2026-09-24 21:16:10.304843
757	845	https://cdn.tgdd.vn/Products/Images/7264/283074/edifice-eqb-1200hg-1adr-nam-thumb-fix-600x600.jpg	Đồng hồ EDIFICE 47 mm Nam EQB-1200HG-1ADR	0	t	2026-09-24 21:16:10.343059
758	846	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/367832/mvw-ss050326-03-nam-thumb-639159231597135818-600x600.jpg	Đồng hồ MVW Urban 39 mm Nam SS050326-03	0	t	2026-09-24 21:16:10.377466
759	847	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/367833/mvw-ss050326-04-nam-thumb-639159235980668606-600x600.jpg	Đồng hồ MVW Urban 32.5 mm Nam SS050326-04	0	t	2026-09-24 21:16:10.414099
760	848	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/367831/mvw-ss050326-02-nam-thumb-639159221603520480-600x600.jpg	Đồng hồ MVW Urban 39 mm Nam SS050326-02	0	t	2026-09-24 21:16:10.448387
761	849	https://cdnv2.tgdd.vn/mwg-static/tgdd/Products/Images/7264/367830/mvw-ss050326-01-nam-thumb-639159212093282665-600x600.jpg	Đồng hồ MVW Urban 41 mm Nam SS050326-01	0	t	2026-09-24 21:16:10.483026
30	30	https://cdn.tgdd.vn/Products/Images/42/361947/samsung-galaxy-s26-12gb-256gb-xanh-thumb-600x600.jpg	Điện thoại Samsung Galaxy S26 5G 12GB/256GB	0	t	2026-09-24 21:15:45.780087
\.


ALTER TABLE public.product_images ENABLE TRIGGER ALL;

--
-- Data for Name: product_specifications; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.product_specifications DISABLE TRIGGER ALL;

COPY public.product_specifications (specification_id, product_id, spec_name, spec_value, spec_order) FROM stdin;
\.


ALTER TABLE public.product_specifications ENABLE TRIGGER ALL;

--
-- Data for Name: product_variants; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.product_variants DISABLE TRIGGER ALL;

COPY public.product_variants (variant_id, product_id, variant_name, sku_variant, price, discount_price, stock_quantity, color, storage, ram, created_at, updated_at) FROM stdin;
1	1	256 GB - Đỏ Burgundy	TGDD-370982-V1	41990000.00	\N	0	Đỏ Burgundy	256 GB	\N	2026-09-24 21:00:40.398462	2026-09-24 21:00:40.398462
2	2	256 GB - Đỏ Burgundy	TGDD-370977-V1	38990000.00	\N	0	Đỏ Burgundy	256 GB	\N	2026-09-24 21:00:40.458522	2026-09-24 21:00:40.458522
3	3	256 GB - Trắng Ánh Sao	TGDD-370987-V1	64990000.00	\N	0	Trắng Ánh Sao	256 GB	\N	2026-09-24 21:00:40.48106	2026-09-24 21:00:40.48106
4	4	4GB/128GB - Tím	TGDD-369617-V1	5790000.00	\N	0	Tím	128 GB	4 GB	2026-09-24 21:00:40.507031	2026-09-24 21:00:40.507031
5	5	256 GB - Cam Vũ Trụ	TGDD-342679-V1	34590000.00	\N	0	Cam Vũ Trụ	256 GB	\N	2026-09-24 21:00:40.529801	2026-09-24 21:00:40.529801
6	6	8GB/128GB - Xanh	TGDD-367499-V1	7790000.00	\N	0	Xanh	128 GB	8 GB	2026-09-24 21:00:40.554103	2026-09-24 21:00:40.554103
7	7	12GB/256GB - Tím	TGDD-369626-V1	10790000.00	\N	0	Tím	256 GB	12 GB	2026-09-24 21:00:40.577101	2026-09-24 21:00:40.577101
8	8	12GB/256GB - Tím	TGDD-369628-V1	13990000.00	\N	0	Tím	256 GB	12 GB	2026-09-24 21:00:40.604232	2026-09-24 21:00:40.604232
9	9	8GB/256GB - Xanh lá	TGDD-367011-V1	8940000.00	\N	0	Xanh lá	256 GB	8 GB	2026-09-24 21:00:40.634656	2026-09-24 21:00:40.634656
10	10	12GB/256GB - Trắng	TGDD-368236-V1	39490000.00	\N	0	Trắng	256 GB	12 GB	2026-09-24 21:00:40.66291	2026-09-24 21:00:40.66291
11	11	256 GB - Xanh Lá Xô Thơm	TGDD-342667-V1	28990000.00	\N	0	Xanh Lá Xô Thơm	256 GB	\N	2026-09-24 21:00:40.691671	2026-09-24 21:00:40.691671
12	12	8GB/128GB - Xanh Dương	TGDD-370544-V1	15840000.00	\N	0	Xanh Dương	128 GB	8 GB	2026-09-24 21:00:40.724463	2026-09-24 21:00:40.724463
13	13	8GB/128GB - Trắng	TGDD-368250-V1	13140000.00	\N	0	Trắng	128 GB	8 GB	2026-09-24 21:00:40.75297	2026-09-24 21:00:40.75297
14	14	256 GB - Cam Vũ Trụ	TGDD-342676-V1	31990000.00	\N	0	Cam Vũ Trụ	256 GB	\N	2026-09-24 21:00:40.779715	2026-09-24 21:00:40.779715
15	15	12GB/256GB - Trắng	TGDD-368057-V1	39090000.00	\N	0	Trắng	256 GB	12 GB	2026-09-24 21:00:40.802643	2026-09-24 21:00:40.802643
16	16	12GB/256GB - Tím	TGDD-368050-V1	43790000.00	\N	0	Tím	256 GB	12 GB	2026-09-24 21:00:40.825344	2026-09-24 21:00:40.825344
17	17	128 GB - Xanh Lưu Ly	TGDD-329138-V1	24990000.00	\N	0	Xanh Lưu Ly	128 GB	\N	2026-09-24 21:00:40.84851	2026-09-24 21:00:40.84851
18	18	16GB/512GB - Xanh lá	TGDD-363408-V1	41840000.00	\N	0	Xanh lá	512 GB	16 GB	2026-09-24 21:00:40.870096	2026-09-24 21:00:40.870096
19	19	8GB/128GB - Trắng	TGDD-368872-V1	11140000.00	\N	0	Trắng	128 GB	8 GB	2026-09-24 21:00:40.896447	2026-09-24 21:00:40.896447
20	20	6GB/128GB - Trắng	TGDD-363401-V1	9000000.00	\N	0	Trắng	128 GB	6 GB	2026-09-24 21:00:40.918465	2026-09-24 21:00:40.918465
21	21	12GB/256GB - Tím	TGDD-367002-V1	14890000.00	\N	0	Tím	256 GB	12 GB	2026-09-24 21:00:40.939943	2026-09-24 21:00:40.939943
22	22	6GB/128GB - Vàng	TGDD-370752-V1	8840000.00	\N	0	Vàng	128 GB	6 GB	2026-09-24 21:00:40.962268	2026-09-24 21:00:40.962268
23	23	8GB/256GB - Tím	TGDD-363466-V1	13030000.00	\N	0	Tím	256 GB	8 GB	2026-09-24 21:00:40.983242	2026-09-24 21:00:40.983242
24	24	4GB/128GB - Xanh	TGDD-367427-V1	6640000.00	\N	0	Xanh	128 GB	4 GB	2026-09-24 21:00:41.00441	2026-09-24 21:00:41.00441
25	25	12GB/256GB - Đỏ	TGDD-358683-V1	10340000.00	\N	0	Đỏ	256 GB	12 GB	2026-09-24 21:00:41.025547	2026-09-24 21:00:41.025547
26	26	4GB/128GB - Xám	TGDD-367204-V1	4640000.00	\N	0	Xám	128 GB	4 GB	2026-09-24 21:00:41.047864	2026-09-24 21:00:41.047864
27	27	8GB/128GB - Xanh Lam Nhạt	TGDD-367975-V1	8440000.00	\N	0	Xanh Lam Nhạt	128 GB	8 GB	2026-09-24 21:00:41.069944	2026-09-24 21:00:41.069944
28	28	128 GB - Xanh dương nhạt	TGDD-281570-V1	21990000.00	\N	0	Xanh dương nhạt	128 GB	\N	2026-09-24 21:00:41.091168	2026-09-24 21:00:41.091168
29	29	256 GB - Vàng Nhạt	TGDD-342670-V1	22990000.00	\N	0	Vàng Nhạt	256 GB	\N	2026-09-24 21:00:41.114003	2026-09-24 21:00:41.114003
30	30	12GB/256GB - Xanh Dương	TGDD-361947-V1	19500000.00	\N	0	Xanh Dương	256 GB	12 GB	2026-09-24 21:00:41.135495	2026-09-24 21:00:41.135495
31	31	8GB/256GB - Tím nhạt	TGDD-368099-V1	18340000.00	\N	0	Tím nhạt	256 GB	8 GB	2026-09-24 21:00:41.156531	2026-09-24 21:00:41.156531
32	32	8GB/256GB - Hồng	TGDD-357576-V1	9520000.00	\N	0	Hồng	256 GB	8 GB	2026-09-24 21:00:41.178919	2026-09-24 21:00:41.179447
33	33	8GB/128GB - Tím	TGDD-364187-V1	7690000.00	\N	0	Tím	128 GB	8 GB	2026-09-24 21:00:41.201461	2026-09-24 21:00:41.201461
34	34	8GB/256GB - Vàng	TGDD-358026-V1	6540000.00	\N	0	Vàng	256 GB	8 GB	2026-09-24 21:00:41.222796	2026-09-24 21:00:41.222796
35	35	4GB/128GB - Xanh Dương	TGDD-364633-V1	3640000.00	\N	0	Xanh Dương	128 GB	4 GB	2026-09-24 21:00:41.242178	2026-09-24 21:00:41.242178
36	36	8GB/128GB - Xanh Bạc Hà	TGDD-362373-V1	6040000.00	\N	0	Xanh Bạc Hà	128 GB	8 GB	2026-09-24 21:00:41.261849	2026-09-24 21:00:41.261849
37	37	256 GB - Hồng nhạt	TGDD-342692-V1	21590000.00	\N	0	Hồng nhạt	256 GB	\N	2026-09-24 21:00:41.292395	2026-09-24 21:00:41.292395
38	38	6GB/128GB - Tím	TGDD-360302-V1	4490000.00	\N	0	Tím	128 GB	6 GB	2026-09-24 21:00:41.312193	2026-09-24 21:00:41.312193
39	39	4GB/64GB - Titan Tím	TGDD-360244-V1	4840000.00	\N	0	Titan Tím	64 GB	4 GB	2026-09-24 21:00:41.33364	2026-09-24 21:00:41.33364
40	40	12GB/256GB - Đen	TGDD-368101-V1	23240000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:41.352592	2026-09-24 21:00:41.352592
41	41	16GB/512GB - Đen	TGDD-357862-V1	31040000.00	\N	0	Đen	512 GB	16 GB	2026-09-24 21:00:41.371717	2026-09-24 21:00:41.371717
42	42	8GB/256GB - Trắng	TGDD-361703-V1	10140000.00	\N	0	Trắng	256 GB	8 GB	2026-09-24 21:00:41.391683	2026-09-24 21:00:41.391683
43	43	16GB/512GB - Đen	TGDD-344641-V1	34840000.00	\N	0	Đen	512 GB	16 GB	2026-09-24 21:00:41.41298	2026-09-24 21:00:41.41298
44	44	8GB/256GB - Xám	TGDD-362374-V1	9240000.00	\N	0	Xám	256 GB	8 GB	2026-09-24 21:00:41.433449	2026-09-24 21:00:41.433449
45	45	128 GB - Xanh Lưu Ly	TGDD-329135-V1	24990000.00	\N	0	Xanh Lưu Ly	128 GB	\N	2026-09-24 21:00:41.453172	2026-09-24 21:00:41.453172
46	46	128 GB - Trắng	TGDD-334864-V1	16990000.00	\N	0	Trắng	128 GB	\N	2026-09-24 21:00:41.474467	2026-09-24 21:00:41.474467
47	47	12GB/256GB - Xanh lá	TGDD-333363-V1	16790000.00	\N	0	Xanh lá	256 GB	12 GB	2026-09-24 21:00:41.49491	2026-09-24 21:00:41.49491
48	48	6GB/128GB - Xanh Dương	TGDD-360310-V1	5490000.00	\N	0	Xanh Dương	128 GB	6 GB	2026-09-24 21:00:41.51566	2026-09-24 21:00:41.51566
49	49	12GB/256GB - Xanh lá	TGDD-339245-V1	8180000.00	\N	0	Xanh lá	256 GB	12 GB	2026-09-24 21:00:41.536781	2026-09-24 21:00:41.536781
50	50	8GB/128GB - Xanh dương nhạt	TGDD-360344-V1	5940000.00	\N	0	Xanh dương nhạt	128 GB	8 GB	2026-09-24 21:00:41.557036	2026-09-24 21:00:41.557036
51	51	4GB/64GB - Xanh Dương	TGDD-369546-V1	3440000.00	\N	0	Xanh Dương	64 GB	4 GB	2026-09-24 21:00:41.582075	2026-09-24 21:00:41.582075
52	52	8GB/128GB - Xám	TGDD-341688-V1	6000000.00	\N	0	Xám	128 GB	8 GB	2026-09-24 21:00:41.606205	2026-09-24 21:00:41.606205
53	53	8GB/256GB - Titan xám	TGDD-360312-V1	7990000.00	\N	0	Titan xám	256 GB	8 GB	2026-09-24 21:00:41.628435	2026-09-24 21:00:41.628435
54	54	8GB/128GB - Tím	TGDD-339180-V1	6720000.00	\N	0	Tím	128 GB	8 GB	2026-09-24 21:00:41.648128	2026-09-24 21:00:41.648128
55	55	8GB/128GB - Tím	TGDD-357832-V1	5840000.00	\N	0	Tím	128 GB	8 GB	2026-09-24 21:00:41.667059	2026-09-24 21:00:41.667579
56	56	12GB/256GB - Đen	TGDD-339638-V1	11100000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:41.685828	2026-09-24 21:00:41.685828
57	57	8GB/128GB - Xanh	TGDD-362760-V1	6630000.00	\N	0	Xanh	128 GB	8 GB	2026-09-24 21:00:41.704603	2026-09-24 21:00:41.704603
58	58	6GB/128GB - Đen	TGDD-366660-V1	3440000.00	\N	0	Đen	128 GB	6 GB	2026-09-24 21:00:41.724721	2026-09-24 21:00:41.724721
59	59	4GB/128GB - Xanh lá	TGDD-358048-V1	4070000.00	\N	0	Xanh lá	128 GB	4 GB	2026-09-24 21:00:41.74386	2026-09-24 21:00:41.74386
60	60	4GB/64GB - Xám	TGDD-366829-V1	4940000.00	\N	0	Xám	64 GB	4 GB	2026-09-24 21:00:41.763838	2026-09-24 21:00:41.763838
61	61	12GB/256GB - Cam	TGDD-363438-V1	13690000.00	\N	0	Cam	256 GB	12 GB	2026-09-24 21:00:41.784068	2026-09-24 21:00:41.784068
62	62	8GB/128GB - Xanh dương nhạt	TGDD-342560-V1	12490000.00	\N	0	Xanh dương nhạt	128 GB	8 GB	2026-09-24 21:00:41.804549	2026-09-24 21:00:41.804549
63	63	8GB/256GB - Hồng	TGDD-360240-V1	11840000.00	\N	0	Hồng	256 GB	8 GB	2026-09-24 21:00:41.825703	2026-09-24 21:00:41.825703
64	64	8GB/256GB - Đen	TGDD-360307-V1	6690000.00	\N	0	Đen	256 GB	8 GB	2026-09-24 21:00:41.849863	2026-09-24 21:00:41.849863
65	65	8GB/256GB - Titan xanh	TGDD-344650-V1	9020000.00	\N	0	Titan xanh	256 GB	8 GB	2026-09-24 21:00:41.872243	2026-09-24 21:00:41.872243
66	66	12GB/256GB - Bạc	TGDD-343067-V1	10240000.00	\N	0	Bạc	256 GB	12 GB	2026-09-24 21:00:41.894254	2026-09-24 21:00:41.894254
67	67	8GB/256GB - Bạc	TGDD-343063-V1	6940000.00	\N	0	Bạc	256 GB	8 GB	2026-09-24 21:00:41.913875	2026-09-24 21:00:41.913875
68	68	8GB/128GB - Xanh tím	TGDD-358225-V1	5840000.00	\N	0	Xanh tím	128 GB	8 GB	2026-09-24 21:00:41.933839	2026-09-24 21:00:41.933839
69	69	8GB/256GB - Xanh Nước Biển	TGDD-358224-V1	6840000.00	\N	0	Xanh Nước Biển	256 GB	8 GB	2026-09-24 21:00:41.953438	2026-09-24 21:00:41.953438
70	70	8GB/256GB - Xanh Dương	TGDD-358223-V1	14040000.00	\N	0	Xanh Dương	256 GB	8 GB	2026-09-24 21:00:41.972054	2026-09-24 21:00:41.972054
71	71	12GB/256GB - Đen	TGDD-338738-V1	36090000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:41.99125	2026-09-24 21:00:41.99125
72	72	12GB/256GB - Đen	TGDD-360309-V1	9990000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:42.010708	2026-09-24 21:00:42.010708
73	73	8GB/256GB - Xanh Dương	TGDD-343124-V1	10520000.00	\N	0	Xanh Dương	256 GB	8 GB	2026-09-24 21:00:42.0296	2026-09-24 21:00:42.0296
74	74	12GB/256GB - Xám	TGDD-341625-V1	14920000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:42.048168	2026-09-24 21:00:42.048168
75	75	8GB/128GB - Xanh Dương	TGDD-362919-V1	6340000.00	\N	0	Xanh Dương	128 GB	8 GB	2026-09-24 21:00:42.066898	2026-09-24 21:00:42.066898
76	76	4GB/64GB - Xanh	TGDD-361191-V1	4840000.00	\N	0	Xanh	64 GB	4 GB	2026-09-24 21:00:42.086866	2026-09-24 21:00:42.086866
77	77	8GB/256GB - Vàng	TGDD-336408-V1	8660000.00	\N	0	Vàng	256 GB	8 GB	2026-09-24 21:00:42.105284	2026-09-24 21:00:42.105284
78	78	12GB/256GB - Đen	TGDD-361951-V1	27500000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:42.123583	2026-09-24 21:00:42.123583
79	79	8GB/128GB - Tím	TGDD-363398-V1	10590000.00	\N	0	Tím	128 GB	8 GB	2026-09-24 21:00:42.142904	2026-09-24 21:00:42.142904
80	80	4GB/64GB - Đen	TGDD-341802-V1	3140000.00	\N	0	Đen	64 GB	4 GB	2026-09-24 21:00:42.161854	2026-09-24 21:00:42.161854
81	81	4GB/128GB - Xám	TGDD-341797-V1	5200000.00	\N	0	Xám	128 GB	4 GB	2026-09-24 21:00:42.181822	2026-09-24 21:00:42.181822
82	82	Đen	TGDD-311033-V1	780000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:42.202831	2026-09-24 21:00:42.202831
83	83	4GB/128GB - Xanh lá	TGDD-369630-V1	4790000.00	\N	0	Xanh lá	128 GB	4 GB	2026-09-24 21:00:42.221987	2026-09-24 21:00:42.221987
84	84	4GB/64GB - Vàng	TGDD-365289-V1	3190000.00	\N	0	Vàng	64 GB	4 GB	2026-09-24 21:00:42.24343	2026-09-24 21:00:42.24343
85	85	Đen	TGDD-367977-V1	550000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:42.263771	2026-09-24 21:00:42.263771
86	86	6GB/128GB - Đen	TGDD-360671-V1	7640000.00	\N	0	Đen	128 GB	6 GB	2026-09-24 21:00:42.284622	2026-09-24 21:00:42.284622
87	87	Xanh lá	TGDD-323546-V1	650000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:42.303626	2026-09-24 21:00:42.303626
88	88	4GB/64GB - Đen	TGDD-365875-V1	4740000.00	\N	0	Đen	64 GB	4 GB	2026-09-24 21:00:42.320645	2026-09-24 21:00:42.320645
89	89	Hồng	TGDD-329676-V1	750000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:42.338195	2026-09-24 21:00:42.338195
90	90	Xanh Dương	TGDD-342939-V1	430000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:42.355483	2026-09-24 21:00:42.355483
91	91	Cam	TGDD-207956-V1	1100000.00	\N	0	Cam	\N	\N	2026-09-24 21:00:42.372883	2026-09-24 21:00:42.372883
92	92	Đen	TGDD-370758-V1	490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:42.390156	2026-09-24 21:00:42.390156
93	93	4GB/64GB - Cam	TGDD-364793-V1	3590000.00	\N	0	Cam	64 GB	4 GB	2026-09-24 21:00:42.408925	2026-09-24 21:00:42.409443
94	94	4GB/64GB - Nâu	TGDD-367596-V1	4540000.00	\N	0	Nâu	64 GB	4 GB	2026-09-24 21:00:42.428919	2026-09-24 21:00:42.428919
95	95	4GB/64GB - Đen	TGDD-363437-V1	4290000.00	\N	0	Đen	64 GB	4 GB	2026-09-24 21:00:42.449605	2026-09-24 21:00:42.449605
96	96	4GB/64GB - Xanh	TGDD-370492-V1	4040000.00	\N	0	Xanh	64 GB	4 GB	2026-09-24 21:00:42.473123	2026-09-24 21:00:42.473123
97	97	Xanh	TGDD-311034-V1	850000.00	\N	0	Xanh	\N	\N	2026-09-24 21:00:42.495051	2026-09-24 21:00:42.495051
98	98	4GB/128GB - Đen	TGDD-365878-V1	5940000.00	\N	0	Đen	128 GB	4 GB	2026-09-24 21:00:42.517887	2026-09-24 21:00:42.517887
99	99	3GB/64GB - Xanh Dương	TGDD-368232-V1	3190000.00	\N	0	Xanh Dương	64 GB	3 GB	2026-09-24 21:00:42.543843	2026-09-24 21:00:42.543843
100	100	4GB/128GB - Đen	TGDD-366923-V1	3540000.00	\N	0	Đen	128 GB	4 GB	2026-09-24 21:00:42.577462	2026-09-24 21:00:42.577986
101	101	4GB/128GB - Tím	TGDD-361709-V1	4800000.00	\N	0	Tím	128 GB	4 GB	2026-09-24 21:00:42.616826	2026-09-24 21:00:42.616826
102	102	Vàng	TGDD-299998-V1	610000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:42.687853	2026-09-24 21:00:42.687853
103	103	3GB/64GB - Xanh lá	TGDD-358698-V1	3140000.00	\N	0	Xanh lá	64 GB	3 GB	2026-09-24 21:00:42.715528	2026-09-24 21:00:42.715528
104	104	12GB/256GB - Xanh Dương	TGDD-333347-V1	24390000.00	\N	0	Xanh Dương	256 GB	12 GB	2026-09-24 21:00:42.737882	2026-09-24 21:00:42.737882
105	105	6GB/128GB - Xanh ngọc	TGDD-369620-V1	7390000.00	\N	0	Xanh ngọc	128 GB	6 GB	2026-09-24 21:00:42.75972	2026-09-24 21:00:42.75972
106	106	4GB/128GB - Vàng	TGDD-366662-V1	3240000.00	\N	0	Vàng	128 GB	4 GB	2026-09-24 21:00:42.778922	2026-09-24 21:00:42.778922
107	107	4GB/64GB - Vàng nhạt	TGDD-361630-V1	2840000.00	\N	0	Vàng nhạt	64 GB	4 GB	2026-09-24 21:00:42.79943	2026-09-24 21:00:42.79943
108	108	4GB/128GB - Cam	TGDD-369634-V1	4990000.00	\N	0	Cam	128 GB	4 GB	2026-09-24 21:00:42.819709	2026-09-24 21:00:42.819709
109	109	Vàng	TGDD-304608-V1	750000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:42.838858	2026-09-24 21:00:42.838858
110	110	Xanh Dương	TGDD-284122-V1	430000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:42.85883	2026-09-24 21:00:42.85883
111	111	12GB/256GB - Đen	TGDD-367004-V1	17290000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:42.876786	2026-09-24 21:00:42.876786
112	112	4GB/128GB - Xanh lá	TGDD-359776-V1	4590000.00	\N	0	Xanh lá	128 GB	4 GB	2026-09-24 21:00:42.89758	2026-09-24 21:00:42.89758
113	113	8GB/256GB - Vàng Hồng	TGDD-363464-V1	16820000.00	\N	0	Vàng Hồng	256 GB	8 GB	2026-09-24 21:00:42.916369	2026-09-24 21:00:42.916369
114	114	6GB/128GB - Bạc	TGDD-367811-V1	3740000.00	\N	0	Bạc	128 GB	6 GB	2026-09-24 21:00:42.935115	2026-09-24 21:00:42.93565
115	115	4GB/64GB - Đen	TGDD-358691-V1	3540000.00	\N	0	Đen	64 GB	4 GB	2026-09-24 21:00:42.965029	2026-09-24 21:00:42.965029
116	116	Xanh lá	TGDD-322877-V1	550000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:42.983957	2026-09-24 21:00:42.983957
117	117	Xanh Dương	TGDD-322876-V1	750000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:43.003333	2026-09-24 21:00:43.003333
118	875	12GB/256GB - Xanh Dương	TGDD-361949-V1	22000000.00	\N	0	Xanh Dương	256 GB	12 GB	2026-09-24 21:00:43.021994	2026-09-24 21:00:43.021994
119	118	8GB/256GB - Xanh Dương	TGDD-360238-V1	13520000.00	\N	0	Xanh Dương	256 GB	8 GB	2026-09-24 21:00:43.041437	2026-09-24 21:00:43.041437
120	119	6GB/128GB - Đen	TGDD-346265-V1	4090000.00	\N	0	Đen	128 GB	6 GB	2026-09-24 21:00:43.064036	2026-09-24 21:00:43.064036
121	120	12GB/256GB - Đen	TGDD-368053-V1	25000000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:43.08563	2026-09-24 21:00:43.08563
122	121	8GB/256GB - Xanh lá	TGDD-363470-V1	10290000.00	\N	0	Xanh lá	256 GB	8 GB	2026-09-24 21:00:43.107125	2026-09-24 21:00:43.107125
123	122	6GB/128GB - Tím	TGDD-341272-V1	4090000.00	\N	0	Tím	128 GB	6 GB	2026-09-24 21:00:43.126644	2026-09-24 21:00:43.126644
124	123	8GB/128GB - Vàng	TGDD-362971-V1	5240000.00	\N	0	Vàng	128 GB	8 GB	2026-09-24 21:00:43.147417	2026-09-24 21:00:43.147417
125	124	6GB/128GB - Xanh đen	TGDD-368045-V1	7500000.00	\N	0	Xanh đen	128 GB	6 GB	2026-09-24 21:00:43.168266	2026-09-24 21:00:43.168266
126	125	12GB/512GB - Cam nhạt	TGDD-365402-V1	25840000.00	\N	0	Cam nhạt	512 GB	12 GB	2026-09-24 21:00:43.189736	2026-09-24 21:00:43.189736
127	126	12GB/256GB - Vàng	TGDD-344644-V1	10690000.00	\N	0	Vàng	256 GB	12 GB	2026-09-24 21:00:43.214673	2026-09-24 21:00:43.214673
128	127	Đỏ	TGDD-288630-V1	690000.00	\N	0	Đỏ	\N	\N	2026-09-24 21:00:43.24285	2026-09-24 21:00:43.24285
129	128	6GB/128GB - Tím	TGDD-358669-V1	5600000.00	\N	0	Tím	128 GB	6 GB	2026-09-24 21:00:43.279296	2026-09-24 21:00:43.279296
130	129	8GB/128GB - Trắng	TGDD-338741-V1	15840000.00	\N	0	Trắng	128 GB	8 GB	2026-09-24 21:00:43.300734	2026-09-24 21:00:43.300734
131	130	4GB/64GB - Xám	TGDD-368234-V1	4140000.00	\N	0	Xám	64 GB	4 GB	2026-09-24 21:00:43.320199	2026-09-24 21:00:43.320199
132	131	Xanh lá	TGDD-338026-V1	890000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:43.33922	2026-09-24 21:00:43.33922
133	132	12GB/256GB - Xanh dương nhạt	TGDD-360236-V1	17020000.00	\N	0	Xanh dương nhạt	256 GB	12 GB	2026-09-24 21:00:43.357858	2026-09-24 21:00:43.357858
134	133	Xanh	TGDD-314697-V1	540000.00	\N	0	Xanh	\N	\N	2026-09-24 21:00:43.377588	2026-09-24 21:00:43.377588
135	134	16GB/512GB - Đen	TGDD-361270-V1	30290000.00	\N	0	Đen	512 GB	16 GB	2026-09-24 21:00:43.396201	2026-09-24 21:00:43.396201
136	135	Đen	TGDD-326477-V1	1490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:43.415248	2026-09-24 21:00:43.415248
137	136	12GB/256GB - Xanh lá	TGDD-361269-V1	19090000.00	\N	0	Xanh lá	256 GB	12 GB	2026-09-24 21:00:43.434309	2026-09-24 21:00:43.434309
138	137	Xám	TGDD-370547-V1	590000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:43.454118	2026-09-24 21:00:43.454118
139	138	12GB/256GB - Xanh Oliu	TGDD-362375-V1	17990000.00	\N	0	Xanh Oliu	256 GB	12 GB	2026-09-24 21:00:43.47364	2026-09-24 21:00:43.47364
140	139	6GB/128GB - Xanh lá	TGDD-340220-V1	4340000.00	\N	0	Xanh lá	128 GB	6 GB	2026-09-24 21:00:43.492814	2026-09-24 21:00:43.492814
141	140	12GB/256GB - Hồng	TGDD-339177-V1	10620000.00	\N	0	Hồng	256 GB	12 GB	2026-09-24 21:00:43.512487	2026-09-24 21:00:43.512487
142	141	12GB/512GB - Xám	TGDD-344645-V1	11590000.00	\N	0	Xám	512 GB	12 GB	2026-09-24 21:00:43.53237	2026-09-24 21:00:43.53237
143	142	8GB/256GB - Xanh lá	TGDD-363468-V1	5890000.00	\N	0	Xanh lá	256 GB	8 GB	2026-09-24 21:00:43.553704	2026-09-24 21:00:43.553704
144	143	12GB/512GB - Bạc	TGDD-335955-V1	19000000.00	\N	0	Bạc	512 GB	12 GB	2026-09-24 21:00:43.576878	2026-09-24 21:00:43.576878
145	144	12GB/256GB - Tím	TGDD-361707-V1	11940000.00	\N	0	Tím	256 GB	12 GB	2026-09-24 21:00:43.600339	2026-09-24 21:00:43.600339
146	145	12GB/256GB - Hồng	TGDD-368875-V1	14740000.00	\N	0	Hồng	256 GB	12 GB	2026-09-24 21:00:43.6211	2026-09-24 21:00:43.6211
147	146	12GB/256GB - Xanh Dương	TGDD-332934-V1	11230000.00	\N	0	Xanh Dương	256 GB	12 GB	2026-09-24 21:00:43.641934	2026-09-24 21:00:43.641934
148	147	12GB/512GB - Tím	TGDD-337714-V1	8900000.00	\N	0	Tím	512 GB	12 GB	2026-09-24 21:00:43.663585	2026-09-24 21:00:43.663585
149	148	12GB/256GB - Hồng	TGDD-343066-V1	8440000.00	\N	0	Hồng	256 GB	12 GB	2026-09-24 21:00:43.68876	2026-09-24 21:00:43.68876
150	149	12GB/256GB - Cam	TGDD-367007-V1	21040000.00	\N	0	Cam	256 GB	12 GB	2026-09-24 21:00:43.711074	2026-09-24 21:00:43.711074
151	150	8GB/256GB - Đen	TGDD-366919-V1	5340000.00	\N	0	Đen	256 GB	8 GB	2026-09-24 21:00:43.73121	2026-09-24 21:00:43.73121
152	151	6GB/128GB - Hồng	TGDD-335915-V1	5800000.00	\N	0	Hồng	128 GB	6 GB	2026-09-24 21:00:43.75119	2026-09-24 21:00:43.75119
153	152	8GB/256GB - Cam	TGDD-367010-V1	14840000.00	\N	0	Cam	256 GB	8 GB	2026-09-24 21:00:43.770899	2026-09-24 21:00:43.770899
154	153	12GB/256GB - Xám	TGDD-344646-V1	14390000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:43.791129	2026-09-24 21:00:43.791129
155	154	8GB/128GB - Trắng	TGDD-367598-V1	10840000.00	\N	0	Trắng	128 GB	8 GB	2026-09-24 21:00:43.810646	2026-09-24 21:00:43.810646
156	155	12GB/512GB - Xanh Dương	TGDD-362273-V1	16790000.00	\N	0	Xanh Dương	512 GB	12 GB	2026-09-24 21:00:43.831337	2026-09-24 21:00:43.831337
157	156	12GB/512GB - Xanh lá	TGDD-362274-V1	10290000.00	\N	0	Xanh lá	512 GB	12 GB	2026-09-24 21:00:43.851254	2026-09-24 21:00:43.851254
158	157	12GB/256GB - Đỏ	TGDD-370849-V1	24290000.00	\N	0	Đỏ	256 GB	12 GB	2026-09-24 21:00:43.872325	2026-09-24 21:00:43.872325
159	158	12GB/256GB - Hồng	TGDD-317981-V1	17430000.00	\N	0	Hồng	256 GB	12 GB	2026-09-24 21:00:43.891913	2026-09-24 21:00:43.891913
160	159	12GB/512GB - Nâu nhạt	TGDD-364791-V1	44840000.00	\N	0	Nâu nhạt	512 GB	12 GB	2026-09-24 21:00:43.917298	2026-09-24 21:00:43.917298
161	160	8GB/128GB - Tím	TGDD-367617-V1	8740000.00	\N	0	Tím	128 GB	8 GB	2026-09-24 21:00:43.942488	2026-09-24 21:00:43.942488
162	161	Đen - Đỏ	TGDD-265311-V1	430000.00	\N	0	Đen - Đỏ	\N	\N	2026-09-24 21:00:43.96399	2026-09-24 21:00:43.96399
163	162	8GB/256GB - Hồng	TGDD-334404-V1	7530000.00	\N	0	Hồng	256 GB	8 GB	2026-09-24 21:00:43.983985	2026-09-24 21:00:43.983985
164	163	16GB/512GB - Titan xanh	TGDD-363258-V1	64840000.00	\N	0	Titan xanh	512 GB	16 GB	2026-09-24 21:00:44.006665	2026-09-24 21:00:44.006665
165	164	4GB/64GB - Đen	TGDD-363410-V1	3290000.00	\N	0	Đen	64 GB	4 GB	2026-09-24 21:00:44.026114	2026-09-24 21:00:44.026114
166	165	12GB/512GB - Trắng - Vàng	TGDD-368098-V1	23540000.00	\N	0	Trắng - Vàng	512 GB	12 GB	2026-09-24 21:00:44.045431	2026-09-24 21:00:44.045431
167	166	12GB/256GB - Xám	TGDD-336623-V1	10050000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:44.066389	2026-09-24 21:00:44.066389
168	167	12GB/256GB - Đen	TGDD-338736-V1	23010000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:44.087461	2026-09-24 21:00:44.087461
169	876	12GB/512GB - Xanh Dương	TGDD-337713-V1	9860000.00	\N	0	Xanh Dương	512 GB	12 GB	2026-09-24 21:00:44.108476	2026-09-24 21:00:44.108476
170	168	4GB/128GB - Xám	TGDD-368231-V1	6340000.00	\N	0	Xám	128 GB	4 GB	2026-09-24 21:00:44.127378	2026-09-24 21:00:44.127378
171	169	8GB/128GB - Xanh Dương	TGDD-371029-V1	10490000.00	\N	0	Xanh Dương	128 GB	8 GB	2026-09-24 21:00:44.146797	2026-09-24 21:00:44.146797
172	170	6GB/128GB - Xanh	TGDD-371119-V1	0.00	\N	0	Xanh	128 GB	6 GB	2026-09-24 21:00:44.168211	2026-09-24 21:00:44.168211
173	171	6GB/256GB - Hồng - Tím	TGDD-371120-V1	0.00	\N	0	Hồng - Tím	256 GB	6 GB	2026-09-24 21:00:44.190383	2026-09-24 21:00:44.190383
174	172	8GB/128GB	TGDD-371195-V1	0.00	\N	0	\N	128 GB	8 GB	2026-09-24 21:00:44.214037	2026-09-24 21:00:44.214037
175	173	6GB/256GB	TGDD-371196-V1	0.00	\N	0	\N	256 GB	6 GB	2026-09-24 21:00:44.236736	2026-09-24 21:00:44.236736
176	174	8GB/256GB	TGDD-371197-V1	0.00	\N	0	\N	256 GB	8 GB	2026-09-24 21:00:44.257727	2026-09-24 21:00:44.257727
177	175	12GB/512GB	TGDD-371315-V1	0.00	\N	0	\N	512 GB	12 GB	2026-09-24 21:00:44.27746	2026-09-24 21:00:44.27746
178	176	12GB/512GB	TGDD-371316-V1	0.00	\N	0	\N	512 GB	12 GB	2026-09-24 21:00:44.297043	2026-09-24 21:00:44.297043
179	177	12GB/256GB	TGDD-371317-V1	0.00	\N	0	\N	256 GB	12 GB	2026-09-24 21:00:44.317526	2026-09-24 21:00:44.317526
180	178	16 GB - Vàng	TGDD-361311-V1	21290000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:44.337812	2026-09-24 21:00:44.337812
181	179	8GB/256GB - Xanh Dương	TGDD-363537-V1	18990000.00	\N	0	Xanh Dương	256 GB	8 GB	2026-09-24 21:00:44.358815	2026-09-24 21:00:44.358815
182	180	16 GB - Bạc	TGDD-358132-V1	21490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.380075	2026-09-24 21:00:44.380075
183	181	16 GB - Bạc	TGDD-368556-V1	23990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.401166	2026-09-24 21:00:44.401166
184	182	16 GB - Xám	TGDD-367016-V1	23690000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:44.421551	2026-09-24 21:00:44.421551
185	183	16 GB - Bạc	TGDD-355729-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.441683	2026-09-24 21:00:44.441683
186	184	16 GB - Bạc	TGDD-362025-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.460398	2026-09-24 21:00:44.460398
187	185	16GB/512GB - Xanh da trời nhạt	TGDD-363487-V1	35290000.00	\N	0	Xanh da trời nhạt	512 GB	16 GB	2026-09-24 21:00:44.479844	2026-09-24 21:00:44.479844
188	186	16 GB - Xám	TGDD-369212-V1	19990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:44.499144	2026-09-24 21:00:44.499144
189	187	16 GB - Bạc	TGDD-364397-V1	19490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.519043	2026-09-24 21:00:44.519043
190	188	8 GB - Bạc	TGDD-367375-V1	20490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:44.538292	2026-09-24 21:00:44.538292
191	189	8 GB - Bạc	TGDD-337420-V1	19290000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:44.557372	2026-09-24 21:00:44.557372
192	190	16 GB - Bạc	TGDD-364859-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.578319	2026-09-24 21:00:44.578319
193	191	8 GB - Bạc	TGDD-368189-V1	13490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:44.599521	2026-09-24 21:00:44.599521
194	192	16 GB - Bạc	TGDD-363263-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.618241	2026-09-24 21:00:44.618241
195	193	16 GB - Xám	TGDD-366746-V1	21490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:44.63699	2026-09-24 21:00:44.63699
196	194	8 GB - Bạc	TGDD-367376-V1	18690000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:44.655651	2026-09-24 21:00:44.655651
197	195	16GB/512GB - Xanh đen	TGDD-363507-V1	41290000.00	\N	0	Xanh đen	512 GB	16 GB	2026-09-24 21:00:44.675441	2026-09-24 21:00:44.675441
198	196	16 GB - Bạc	TGDD-342941-V1	18190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.693992	2026-09-24 21:00:44.693992
199	197	16 GB - Bạc	TGDD-360420-V1	22090000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.713326	2026-09-24 21:00:44.713326
200	198	16 GB - Bạc	TGDD-358078-V1	26490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.732588	2026-09-24 21:00:44.732588
201	199	16 GB - Đen	TGDD-338204-V1	26490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:44.751872	2026-09-24 21:00:44.751872
202	200	8 GB - Bạc	TGDD-362620-V1	18890000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:44.771748	2026-09-24 21:00:44.771748
203	201	16 GB - Bạc	TGDD-357990-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.791067	2026-09-24 21:00:44.791067
204	202	16 GB - Đen	TGDD-362621-V1	27490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:44.810041	2026-09-24 21:00:44.810041
205	203	16 GB - Xám	TGDD-363903-V1	29490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:44.827652	2026-09-24 21:00:44.827652
206	204	16 GB - Bạc	TGDD-366090-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.84557	2026-09-24 21:00:44.84557
207	205	8 GB - Xám	TGDD-368211-V1	19190000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:44.86438	2026-09-24 21:00:44.86438
208	206	16 GB - Bạc	TGDD-341267-V1	25990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.883693	2026-09-24 21:00:44.883693
209	207	16 GB - Bạc	TGDD-360690-V1	26990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.902538	2026-09-24 21:00:44.902538
210	208	16 GB - Đen	TGDD-368656-V1	26990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:44.920593	2026-09-24 21:00:44.920593
211	209	16 GB - Bạc	TGDD-365310-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:44.940374	2026-09-24 21:00:44.940374
212	210	24 GB - Vàng	TGDD-362398-V1	23990000.00	\N	0	Vàng	24 GB	\N	2026-09-24 21:00:44.960762	2026-09-24 21:00:44.960762
213	211	16 GB - Xám	TGDD-368210-V1	22990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:44.980791	2026-09-24 21:00:44.980791
214	212	16 GB - Vàng Nhạt	TGDD-368192-V1	17390000.00	\N	0	Vàng Nhạt	16 GB	\N	2026-09-24 21:00:45.002718	2026-09-24 21:00:45.002718
215	213	16 GB - Bạc	TGDD-366087-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.023311	2026-09-24 21:00:45.023311
216	214	16 GB - Đen	TGDD-369645-V1	33490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.043855	2026-09-24 21:00:45.043855
217	215	48GB/1TB - Bạc	TGDD-363492-V1	95990000.00	\N	0	Bạc	1 TB	48 GB	2026-09-24 21:00:45.065238	2026-09-24 21:00:45.065238
218	216	32 GB - Đen	TGDD-360295-V1	60990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:45.08639	2026-09-24 21:00:45.08639
219	217	16 GB - Xanh Đậm	TGDD-362024-V1	27990000.00	\N	0	Xanh Đậm	16 GB	\N	2026-09-24 21:00:45.107906	2026-09-24 21:00:45.107906
220	218	16 GB - Bạc	TGDD-362410-V1	22190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.131365	2026-09-24 21:00:45.131365
221	219	16 GB - Đen	TGDD-341623-V1	26490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.154664	2026-09-24 21:00:45.154664
222	220	8 GB - Xám	TGDD-368208-V1	18990000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:45.176098	2026-09-24 21:00:45.176098
223	221	16 GB - Bạc	TGDD-362406-V1	20790000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.19854	2026-09-24 21:00:45.19854
224	222	16 GB - Bạc	TGDD-340562-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.220198	2026-09-24 21:00:45.220198
225	223	16 GB - Bạc	TGDD-340484-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.243545	2026-09-24 21:00:45.243545
226	224	16 GB - Xám	TGDD-366745-V1	21390000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.265399	2026-09-24 21:00:45.265399
227	225	16 GB - Bạc	TGDD-342758-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.287533	2026-09-24 21:00:45.287533
228	226	16 GB - Đen	TGDD-366823-V1	43990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.309204	2026-09-24 21:00:45.309204
229	227	16 GB - Xám	TGDD-335554-V1	29790000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.332367	2026-09-24 21:00:45.332367
230	228	16 GB - Bạc	TGDD-337044-V1	22390000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.354964	2026-09-24 21:00:45.354964
231	229	8 GB - Bạc	TGDD-366086-V1	15990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:45.374688	2026-09-24 21:00:45.374688
232	230	16 GB - Bạc	TGDD-340561-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.396534	2026-09-24 21:00:45.396534
233	231	16 GB - Xám	TGDD-339682-V1	25990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.416921	2026-09-24 21:00:45.416921
234	232	16 GB - Đen	TGDD-341624-V1	29990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.434318	2026-09-24 21:00:45.434318
235	233	16 GB - Xám	TGDD-342524-V1	29290000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.452288	2026-09-24 21:00:45.452288
236	234	16 GB - Đen	TGDD-341607-V1	36990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.470726	2026-09-24 21:00:45.470726
237	235	16 GB - Đen	TGDD-360298-V1	59990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.489258	2026-09-24 21:00:45.489258
238	236	16 GB - Bạc	TGDD-368655-V1	24990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.507614	2026-09-24 21:00:45.507614
239	237	16 GB - Bạc	TGDD-364183-V1	20690000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.525996	2026-09-24 21:00:45.525996
240	238	8 GB - Bạc	TGDD-367346-V1	18990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:45.544837	2026-09-24 21:00:45.544837
241	239	16 GB - Bạc	TGDD-341266-V1	26990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.562715	2026-09-24 21:00:45.562715
242	240	24 GB - Bạc	TGDD-365624-V1	23990000.00	\N	0	Bạc	24 GB	\N	2026-09-24 21:00:45.580439	2026-09-24 21:00:45.580439
243	241	16 GB - Xám	TGDD-338320-V1	23590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.598727	2026-09-24 21:00:45.598727
244	242	16 GB - Bạc	TGDD-360692-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.617543	2026-09-24 21:00:45.617543
245	243	8 GB - Bạc	TGDD-366992-V1	18990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:45.63661	2026-09-24 21:00:45.63661
246	244	24 GB - Xám	TGDD-334446-V1	23390000.00	\N	0	Xám	24 GB	\N	2026-09-24 21:00:45.655452	2026-09-24 21:00:45.655452
247	245	16 GB - Bạc	TGDD-333421-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.673041	2026-09-24 21:00:45.673041
248	246	16 GB - Đen	TGDD-341601-V1	29990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.691757	2026-09-24 21:00:45.691757
249	247	16 GB - Bạc	TGDD-339954-V1	20890000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.71118	2026-09-24 21:00:45.71118
250	248	16 GB - Đen	TGDD-327707-V1	29990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.730429	2026-09-24 21:00:45.730429
251	249	16 GB - Bạc	TGDD-363257-V1	22190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.752188	2026-09-24 21:00:45.752188
252	250	16 GB - Bạc	TGDD-369911-V1	29890000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.774259	2026-09-24 21:00:45.774259
253	251	16 GB - Xám	TGDD-367684-V1	23690000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.795663	2026-09-24 21:00:45.795663
254	252	16 GB - Bạc	TGDD-362026-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.816509	2026-09-24 21:00:45.816509
255	253	16 GB - Đen	TGDD-363690-V1	26090000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.840318	2026-09-24 21:00:45.840318
256	254	16 GB - Vàng	TGDD-325691-V1	21990000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:45.864563	2026-09-24 21:00:45.864563
257	255	16 GB - Vàng	TGDD-358797-V1	25990000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:45.887355	2026-09-24 21:00:45.887355
258	256	16 GB - Bạc	TGDD-341602-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.908153	2026-09-24 21:00:45.908153
259	257	16 GB - Xám	TGDD-359486-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:45.92753	2026-09-24 21:00:45.92753
260	258	16 GB - Bạc	TGDD-367575-V1	24190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.947164	2026-09-24 21:00:45.947164
261	259	16 GB - Bạc	TGDD-360691-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.965272	2026-09-24 21:00:45.965272
262	260	16 GB - Bạc	TGDD-362020-V1	27490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:45.982327	2026-09-24 21:00:45.982327
263	261	16 GB - Đen	TGDD-365608-V1	22990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:45.999684	2026-09-24 21:00:45.999684
264	262	16 GB - Xám	TGDD-366741-V1	29990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.016835	2026-09-24 21:00:46.016835
265	263	16 GB - Xám	TGDD-366749-V1	36990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.033286	2026-09-24 21:00:46.033286
266	264	16 GB - Đen	TGDD-369754-V1	35990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.050137	2026-09-24 21:00:46.050137
267	265	16 GB - Đen	TGDD-341626-V1	34990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.06732	2026-09-24 21:00:46.06732
268	266	16 GB - Đen	TGDD-325697-V1	26490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.084971	2026-09-24 21:00:46.084971
269	267	16 GB - Bạc	TGDD-363261-V1	19990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.102125	2026-09-24 21:00:46.102125
270	268	24 GB - Bạc	TGDD-358077-V1	31490000.00	\N	0	Bạc	24 GB	\N	2026-09-24 21:00:46.120143	2026-09-24 21:00:46.120143
271	269	16 GB - Bạc	TGDD-362667-V1	27490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.137886	2026-09-24 21:00:46.137886
272	270	8 GB - Bạc	TGDD-368558-V1	21490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:46.155011	2026-09-24 21:00:46.155011
273	271	16 GB - Đen	TGDD-339211-V1	31790000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.172148	2026-09-24 21:00:46.172148
274	272	16 GB - Đen	TGDD-367576-V1	28990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.188555	2026-09-24 21:00:46.188555
275	273	16 GB - Xám	TGDD-327507-V1	20690000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.205968	2026-09-24 21:00:46.205968
276	274	8 GB - Xám	TGDD-369478-V1	17490000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:46.223517	2026-09-24 21:00:46.223517
277	275	16 GB - Bạc	TGDD-368557-V1	28990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.240534	2026-09-24 21:00:46.240534
278	276	16 GB - Bạc	TGDD-351613-V1	27490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.258421	2026-09-24 21:00:46.258421
279	277	16 GB - Đen	TGDD-363260-V1	30990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.278335	2026-09-24 21:00:46.278335
280	278	16 GB - Xám	TGDD-368207-V1	22990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.298698	2026-09-24 21:00:46.298698
281	279	16 GB - Bạc	TGDD-341618-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.3178	2026-09-24 21:00:46.3178
282	280	8 GB - Bạc	TGDD-368654-V1	16990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:46.336431	2026-09-24 21:00:46.336431
283	281	16 GB - Vàng	TGDD-367574-V1	24190000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:46.356324	2026-09-24 21:00:46.356324
284	282	16 GB - Đen	TGDD-339688-V1	25890000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.376835	2026-09-24 21:00:46.376835
285	283	16 GB - Bạc	TGDD-364860-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.396396	2026-09-24 21:00:46.396396
286	284	16 GB - Xanh	TGDD-358080-V1	31390000.00	\N	0	Xanh	16 GB	\N	2026-09-24 21:00:46.415281	2026-09-24 21:00:46.415281
287	285	16 GB - Bạc	TGDD-357992-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.433875	2026-09-24 21:00:46.433875
288	286	16 GB - Xám	TGDD-340006-V1	21390000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.453059	2026-09-24 21:00:46.453059
289	287	8 GB - Đen	TGDD-318354-V1	25990000.00	\N	0	Đen	8 GB	\N	2026-09-24 21:00:46.47238	2026-09-24 21:00:46.47238
290	288	16 GB - Bạc	TGDD-365998-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.491478	2026-09-24 21:00:46.491478
291	289	16 GB - Bạc	TGDD-362666-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.509675	2026-09-24 21:00:46.509675
292	290	16 GB - Đen	TGDD-337843-V1	29090000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.528272	2026-09-24 21:00:46.528272
293	291	16 GB - Bạc	TGDD-358498-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.548861	2026-09-24 21:00:46.548861
294	292	16 GB - Đen	TGDD-345510-V1	29990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.56905	2026-09-24 21:00:46.56905
295	293	32 GB - Bạc	TGDD-358074-V1	31690000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:46.59005	2026-09-24 21:00:46.59005
296	294	16 GB - Vàng	TGDD-311176-V1	19990000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:46.61044	2026-09-24 21:00:46.61044
297	295	16 GB - Đen	TGDD-341610-V1	44590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.628946	2026-09-24 21:00:46.628946
298	296	16 GB - Xám	TGDD-334998-V1	20990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.647311	2026-09-24 21:00:46.647311
299	297	16 GB - Xanh Dương	TGDD-364401-V1	29590000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:46.665594	2026-09-24 21:00:46.665594
300	298	16 GB - Xám	TGDD-359477-V1	31990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.684631	2026-09-24 21:00:46.684631
301	299	16 GB - Bạc	TGDD-337043-V1	23390000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.704823	2026-09-24 21:00:46.704823
302	300	16 GB - Bạc	TGDD-334803-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.725081	2026-09-24 21:00:46.725081
303	301	16 GB - Xám	TGDD-339951-V1	24990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.746243	2026-09-24 21:00:46.746243
304	302	16 GB - Vàng	TGDD-341270-V1	22990000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:46.767333	2026-09-24 21:00:46.767333
305	303	16 GB - Xám	TGDD-333424-V1	21590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:46.787241	2026-09-24 21:00:46.787241
306	304	16 GB - Đen	TGDD-341611-V1	39990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.807462	2026-09-24 21:00:46.807462
307	305	16 GB - Bạc	TGDD-360417-V1	19490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.828634	2026-09-24 21:00:46.828634
308	306	16 GB - Bạc	TGDD-368559-V1	31990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.848776	2026-09-24 21:00:46.848776
309	307	32 GB - Xanh Dương	TGDD-364405-V1	38590000.00	\N	0	Xanh Dương	32 GB	\N	2026-09-24 21:00:46.868344	2026-09-24 21:00:46.868344
310	308	16 GB - Bạc	TGDD-358120-V1	25990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.889491	2026-09-24 21:00:46.889491
311	309	16 GB - Bạc	TGDD-342754-V1	22490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.910404	2026-09-24 21:00:46.910404
312	310	24GB/1TB - Đen	TGDD-358089-V1	56990000.00	\N	0	Đen	1 TB	24 GB	2026-09-24 21:00:46.930623	2026-09-24 21:00:46.930623
313	311	16 GB - Bạc	TGDD-366702-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:46.949732	2026-09-24 21:00:46.949732
314	312	16 GB - Đen	TGDD-365535-V1	33690000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.968607	2026-09-24 21:00:46.968607
315	313	16 GB - Đen	TGDD-362922-V1	39090000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:46.987424	2026-09-24 21:00:46.987424
316	314	16 GB - Bạc	TGDD-360419-V1	19490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.005799	2026-09-24 21:00:47.005799
317	315	8 GB - Bạc	TGDD-369481-V1	21490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:47.023672	2026-09-24 21:00:47.023672
318	316	8 GB - Đen	TGDD-365607-V1	21490000.00	\N	0	Đen	8 GB	\N	2026-09-24 21:00:47.041392	2026-09-24 21:00:47.04196
319	317	16 GB - Bạc	TGDD-364967-V1	30990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.059818	2026-09-24 21:00:47.059818
320	318	16 GB - Trắng	TGDD-363259-V1	30990000.00	\N	0	Trắng	16 GB	\N	2026-09-24 21:00:47.07816	2026-09-24 21:00:47.07816
321	319	16 GB - Đen	TGDD-366909-V1	28090000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.097333	2026-09-24 21:00:47.097333
322	320	16 GB - Xanh Dương	TGDD-334796-V1	30790000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:47.11627	2026-09-24 21:00:47.11627
323	321	16 GB - Bạc	TGDD-358796-V1	25990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.133433	2026-09-24 21:00:47.133433
324	322	32 GB - Xám	TGDD-363882-V1	36990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:47.150635	2026-09-24 21:00:47.150635
325	323	16 GB - Bạc	TGDD-327406-V1	21690000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.170716	2026-09-24 21:00:47.170716
326	324	16 GB - Bạc	TGDD-363262-V1	22990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.188467	2026-09-24 21:00:47.188467
327	325	16 GB - Xám	TGDD-341751-V1	34490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.2067	2026-09-24 21:00:47.2067
328	326	24 GB - Vàng	TGDD-362399-V1	27990000.00	\N	0	Vàng	24 GB	\N	2026-09-24 21:00:47.22706	2026-09-24 21:00:47.22706
329	327	16 GB - Bạc	TGDD-365311-V1	28490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.246437	2026-09-24 21:00:47.246437
330	328	16 GB - Bạc	TGDD-339681-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.265398	2026-09-24 21:00:47.265398
331	329	16 GB - Xám	TGDD-363901-V1	26990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.284452	2026-09-24 21:00:47.284452
332	330	16 GB - Bạc	TGDD-340560-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.304361	2026-09-24 21:00:47.304361
333	331	16 GB - Vàng	TGDD-358079-V1	21290000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:47.324154	2026-09-24 21:00:47.324154
334	332	32 GB - Đen	TGDD-341609-V1	49990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:47.344443	2026-09-24 21:00:47.344443
335	333	32 GB - Đen	TGDD-341613-V1	46990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:47.366937	2026-09-24 21:00:47.366937
336	334	16 GB - Đen	TGDD-360421-V1	25990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.387995	2026-09-24 21:00:47.387995
337	335	16 GB - Xám	TGDD-344606-V1	32990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.40738	2026-09-24 21:00:47.40738
338	336	16 GB - Xanh Đậm	TGDD-360693-V1	37990000.00	\N	0	Xanh Đậm	16 GB	\N	2026-09-24 21:00:47.426508	2026-09-24 21:00:47.426508
339	337	16 GB - Bạc	TGDD-358499-V1	25490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.444634	2026-09-24 21:00:47.444634
340	338	16 GB - Bạc	TGDD-338316-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.463132	2026-09-24 21:00:47.463132
341	339	32 GB - Xám	TGDD-342513-V1	36590000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:47.481408	2026-09-24 21:00:47.481408
342	340	16 GB - Đen	TGDD-359927-V1	36490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.498603	2026-09-24 21:00:47.498603
343	341	16 GB - Bạc	TGDD-358075-V1	31290000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.516208	2026-09-24 21:00:47.516208
344	342	16 GB - Đen	TGDD-360296-V1	42590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.535243	2026-09-24 21:00:47.535243
345	343	16GB/1TB - Đen	TGDD-358088-V1	52990000.00	\N	0	Đen	1 TB	16 GB	2026-09-24 21:00:47.553186	2026-09-24 21:00:47.553186
346	344	16 GB - Bạc	TGDD-340558-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.571051	2026-09-24 21:00:47.571051
347	345	8 GB - Bạc	TGDD-365609-V1	21490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:47.588398	2026-09-24 21:00:47.588398
348	346	16 GB - Đen	TGDD-368220-V1	36490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.605819	2026-09-24 21:00:47.605819
349	347	16 GB - Xanh Dương	TGDD-341564-V1	40990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:47.624676	2026-09-24 21:00:47.624676
350	348	16 GB - Xám	TGDD-364399-V1	27990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.641171	2026-09-24 21:00:47.641171
351	349	16 GB - Đen	TGDD-322099-V1	28990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.658601	2026-09-24 21:00:47.658601
352	350	32 GB - Bạc	TGDD-367378-V1	49990000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:47.677481	2026-09-24 21:00:47.677481
353	351	16 GB - Xanh Dương	TGDD-341563-V1	33990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:47.694573	2026-09-24 21:00:47.694573
354	352	16 GB - Xám	TGDD-338321-V1	27490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.711465	2026-09-24 21:00:47.711465
355	353	16 GB - Vàng	TGDD-311177-V1	24290000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:47.728295	2026-09-24 21:00:47.728295
356	354	16 GB - Trắng	TGDD-339201-V1	27990000.00	\N	0	Trắng	16 GB	\N	2026-09-24 21:00:47.745178	2026-09-24 21:00:47.745178
357	355	16 GB - Bạc	TGDD-366703-V1	28490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.762266	2026-09-24 21:00:47.762266
358	356	16 GB - Xám	TGDD-359482-V1	23990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.780867	2026-09-24 21:00:47.780867
359	357	16 GB - Bạc	TGDD-361536-V1	29990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.800593	2026-09-24 21:00:47.800593
360	358	16 GB - Xám	TGDD-341752-V1	37590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.821749	2026-09-24 21:00:47.821749
361	359	16 GB - Đen	TGDD-360694-V1	33990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:47.842069	2026-09-24 21:00:47.842069
362	360	16 GB - Bạc	TGDD-357991-V1	27490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.863619	2026-09-24 21:00:47.863619
363	361	32 GB - Xanh	TGDD-358076-V1	37990000.00	\N	0	Xanh	32 GB	\N	2026-09-24 21:00:47.884095	2026-09-24 21:00:47.884095
364	362	16 GB - Bạc	TGDD-361726-V1	27190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.905957	2026-09-24 21:00:47.905957
365	363	32 GB - Trắng	TGDD-330579-V1	43990000.00	\N	0	Trắng	32 GB	\N	2026-09-24 21:00:47.929422	2026-09-24 21:00:47.929422
366	364	16 GB - Xám	TGDD-363897-V1	32290000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:47.949782	2026-09-24 21:00:47.949782
367	365	8 GB - Bạc	TGDD-368555-V1	21490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:47.9705	2026-09-24 21:00:47.9705
368	366	16 GB - Bạc	TGDD-340479-V1	24290000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:47.990815	2026-09-24 21:00:47.990815
369	367	16 GB - Bạc	TGDD-362408-V1	23690000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.017663	2026-09-24 21:00:48.017663
370	368	16 GB - Bạc	TGDD-365625-V1	26990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.039202	2026-09-24 21:00:48.039202
371	369	16 GB - Xám	TGDD-367683-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.058656	2026-09-24 21:00:48.058656
372	370	16 GB - Xanh Dương	TGDD-325244-V1	27990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:48.077671	2026-09-24 21:00:48.077671
373	371	16 GB - Bạc	TGDD-365613-V1	25990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.097614	2026-09-24 21:00:48.097614
374	372	16 GB - Đen	TGDD-362625-V1	29490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.11835	2026-09-24 21:00:48.11835
375	373	32 GB - Đen	TGDD-363891-V1	35490000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:48.145032	2026-09-24 21:00:48.145032
376	374	16 GB - Bạc	TGDD-369845-V1	27490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.172001	2026-09-24 21:00:48.172001
377	375	16 GB - Xám	TGDD-359471-V1	23990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.19738	2026-09-24 21:00:48.19738
378	376	16 GB - Đen	TGDD-364979-V1	28990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.219322	2026-09-24 21:00:48.219322
379	377	16 GB - Xanh Dương	TGDD-357987-V1	26990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:48.245642	2026-09-24 21:00:48.245642
380	378	16 GB - Bạc	TGDD-364414-V1	21590000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.26821	2026-09-24 21:00:48.26821
381	379	16 GB - Xám	TGDD-364181-V1	35990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.289484	2026-09-24 21:00:48.289484
382	380	16 GB - Bạc	TGDD-337472-V1	24490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.311678	2026-09-24 21:00:48.311678
383	381	16 GB - Bạc	TGDD-341621-V1	23490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.334375	2026-09-24 21:00:48.334375
384	382	16 GB - Xám	TGDD-364623-V1	37590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.357767	2026-09-24 21:00:48.357767
385	383	16 GB - Xanh da trời	TGDD-361538-V1	40990000.00	\N	0	Xanh da trời	16 GB	\N	2026-09-24 21:00:48.379754	2026-09-24 21:00:48.379754
386	384	16 GB - Xanh Dương	TGDD-357988-V1	31990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:48.400427	2026-09-24 21:00:48.400427
387	385	16 GB - Đen	TGDD-326280-V1	37790000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.421821	2026-09-24 21:00:48.421821
388	386	32 GB - Xám	TGDD-335020-V1	33990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:48.444531	2026-09-24 21:00:48.444531
389	387	16 GB - Xám	TGDD-335015-V1	79590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.465183	2026-09-24 21:00:48.465183
390	388	16 GB - Xanh	TGDD-332397-V1	29990000.00	\N	0	Xanh	16 GB	\N	2026-09-24 21:00:48.488831	2026-09-24 21:00:48.488831
391	389	16 GB - Bạc	TGDD-365305-V1	29990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.51294	2026-09-24 21:00:48.51294
392	390	16 GB - Xám	TGDD-332579-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.533995	2026-09-24 21:00:48.533995
393	391	16 GB - Xám	TGDD-367017-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.553957	2026-09-24 21:00:48.553957
394	392	16 GB - Đen	TGDD-342521-V1	51990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.574809	2026-09-24 21:00:48.574809
395	393	16 GB - Bạc	TGDD-365614-V1	29990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.595026	2026-09-24 21:00:48.595026
396	394	16 GB - Xám	TGDD-362409-V1	26590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.617889	2026-09-24 21:00:48.617889
397	395	32 GB - Bạc	TGDD-358072-V1	32490000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:48.644046	2026-09-24 21:00:48.644046
398	396	16 GB - Xám	TGDD-366165-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.668965	2026-09-24 21:00:48.668965
399	397	16 GB - Bạc	TGDD-342469-V1	24490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.697472	2026-09-24 21:00:48.697472
400	398	16 GB - Bạc	TGDD-364942-V1	24990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.728184	2026-09-24 21:00:48.728184
401	399	16 GB - Màu be	TGDD-334800-V1	28490000.00	\N	0	Màu be	16 GB	\N	2026-09-24 21:00:48.754132	2026-09-24 21:00:48.754132
402	400	32 GB - Bạc	TGDD-358119-V1	34990000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:48.777702	2026-09-24 21:00:48.777702
403	401	16 GB - Xám	TGDD-366167-V1	34990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.799627	2026-09-24 21:00:48.799627
404	402	16 GB - Xám	TGDD-366166-V1	30490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:48.820025	2026-09-24 21:00:48.820025
405	403	16 GB - Đen	TGDD-358381-V1	44690000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.840284	2026-09-24 21:00:48.840284
406	404	16 GB - Bạc	TGDD-365616-V1	32990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.859256	2026-09-24 21:00:48.859256
407	405	16 GB - Xanh Dương	TGDD-341565-V1	44990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:48.878406	2026-09-24 21:00:48.878406
408	406	16 GB - Bạc	TGDD-362021-V1	27990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.897393	2026-09-24 21:00:48.897393
409	407	16 GB - Bạc	TGDD-345503-V1	29990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.917468	2026-09-24 21:00:48.917468
410	408	16 GB - Đen	TGDD-359298-V1	48590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:48.936905	2026-09-24 21:00:48.936905
411	409	32 GB - Đen	TGDD-363898-V1	38390000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:48.955968	2026-09-24 21:00:48.955968
412	410	16 GB - Bạc	TGDD-345506-V1	31990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.975668	2026-09-24 21:00:48.975668
413	411	16 GB - Bạc	TGDD-363826-V1	30490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:48.994767	2026-09-24 21:00:48.994767
414	412	16 GB - Đen	TGDD-359299-V1	45590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.014358	2026-09-24 21:00:49.014358
415	413	8 GB - Bạc	TGDD-367377-V1	18990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:49.034402	2026-09-24 21:00:49.034402
416	414	16 GB - Bạc	TGDD-342749-V1	23490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.054672	2026-09-24 21:00:49.054672
417	415	16 GB - Bạc	TGDD-325247-V1	26490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.073929	2026-09-24 21:00:49.073929
418	416	32 GB - Đen	TGDD-363900-V1	36590000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.095528	2026-09-24 21:00:49.095528
419	417	16 GB - Bạc	TGDD-358798-V1	30790000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.117186	2026-09-24 21:00:49.117186
420	418	32 GB - Xanh	TGDD-341269-V1	55990000.00	\N	0	Xanh	32 GB	\N	2026-09-24 21:00:49.141231	2026-09-24 21:00:49.141231
421	419	32 GB - Đen	TGDD-359302-V1	63990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.16834	2026-09-24 21:00:49.16834
422	420	16 GB - Xám	TGDD-362624-V1	27990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.189645	2026-09-24 21:00:49.189645
423	421	16 GB - Bạc	TGDD-361729-V1	22590000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.214079	2026-09-24 21:00:49.214079
424	422	8 GB - Xám	TGDD-363902-V1	20590000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:49.236523	2026-09-24 21:00:49.236523
425	423	16 GB - Xám	TGDD-364415-V1	27890000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.258697	2026-09-24 21:00:49.258697
426	424	16 GB - Xám	TGDD-368209-V1	30190000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.278766	2026-09-24 21:00:49.278766
427	425	8 GB - Bạc	TGDD-326049-V1	19290000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:49.296879	2026-09-24 21:00:49.296879
428	426	16 GB - Bạc	TGDD-337041-V1	27390000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.317394	2026-09-24 21:00:49.317394
429	427	32 GB - Đen	TGDD-364622-V1	68590000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.338555	2026-09-24 21:00:49.338555
430	428	16 GB - Đen	TGDD-359928-V1	41490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.357717	2026-09-24 21:00:49.357717
431	429	32 GB - Đen	TGDD-362626-V1	35990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.375137	2026-09-24 21:00:49.375137
432	430	16 GB - Xám	TGDD-368221-V1	28690000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.392039	2026-09-24 21:00:49.392039
433	431	16 GB - Bạc	TGDD-337042-V1	26390000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.408076	2026-09-24 21:00:49.408076
434	432	16 GB - Đen	TGDD-365312-V1	33990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.424294	2026-09-24 21:00:49.424294
435	433	16 GB - Xám	TGDD-340563-V1	28490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.44127	2026-09-24 21:00:49.44127
436	434	16 GB - Bạc	TGDD-340559-V1	26490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.458293	2026-09-24 21:00:49.458293
437	435	32 GB - Bạc	TGDD-368223-V1	39890000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:49.476323	2026-09-24 21:00:49.476323
438	436	16 GB - Xám	TGDD-367012-V1	30590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.492996	2026-09-24 21:00:49.492996
439	437	16 GB - Bạc	TGDD-362023-V1	33990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.510665	2026-09-24 21:00:49.510665
440	438	16 GB - Xanh Dương	TGDD-325953-V1	28990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:49.528076	2026-09-24 21:00:49.528076
441	439	16 GB - Đen	TGDD-342719-V1	43890000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.546142	2026-09-24 21:00:49.546142
442	440	16 GB - Xám	TGDD-368217-V1	38490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.562796	2026-09-24 21:00:49.562796
443	441	16 GB - Bạc	TGDD-345500-V1	29490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.579603	2026-09-24 21:00:49.579603
444	442	16 GB - Bạc	TGDD-369747-V1	34990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.596104	2026-09-24 21:00:49.596104
445	443	16 GB - Xám	TGDD-368218-V1	33390000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.611925	2026-09-24 21:00:49.611925
446	444	16 GB - Bạc	TGDD-345508-V1	35990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.628703	2026-09-24 21:00:49.628703
447	445	24 GB - Bạc	TGDD-358794-V1	27990000.00	\N	0	Bạc	24 GB	\N	2026-09-24 21:00:49.644345	2026-09-24 21:00:49.644345
448	446	16 GB - Xám	TGDD-367013-V1	35990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.660819	2026-09-24 21:00:49.660819
449	447	32 GB - Bạc	TGDD-361730-V1	26590000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:49.677369	2026-09-24 21:00:49.677369
450	448	16 GB - Đen	TGDD-339235-V1	27390000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.695358	2026-09-24 21:00:49.695358
451	449	32 GB - Đen	TGDD-342721-V1	56990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.713471	2026-09-24 21:00:49.713471
452	450	16 GB - Xám	TGDD-367021-V1	30990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.730379	2026-09-24 21:00:49.730379
453	451	32 GB - Xanh	TGDD-341268-V1	59990000.00	\N	0	Xanh	32 GB	\N	2026-09-24 21:00:49.748723	2026-09-24 21:00:49.748723
454	452	32 GB - Bạc	TGDD-358795-V1	34990000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:49.766897	2026-09-24 21:00:49.766897
455	453	32 GB - Đen	TGDD-364621-V1	68790000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:49.784316	2026-09-24 21:00:49.784316
456	454	16 GB - Xám	TGDD-367685-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.802534	2026-09-24 21:00:49.802534
457	455	32 GB - Xanh	TGDD-337040-V1	48790000.00	\N	0	Xanh	32 GB	\N	2026-09-24 21:00:49.824017	2026-09-24 21:00:49.824017
458	456	16 GB - Đen	TGDD-368224-V1	45590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.842945	2026-09-24 21:00:49.842945
459	457	16 GB - Vàng	TGDD-327098-V1	21290000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:49.86129	2026-09-24 21:00:49.86129
460	458	32 GB - Bạc	TGDD-339558-V1	59590000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:49.879023	2026-09-24 21:00:49.879023
461	459	32 GB - Bạc	TGDD-358081-V1	39890000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:49.898577	2026-09-24 21:00:49.898577
462	460	32 GB - Xám	TGDD-367019-V1	54490000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:49.9198	2026-09-24 21:00:49.9198
463	461	16 GB - Đen	TGDD-368225-V1	48590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:49.94106	2026-09-24 21:00:49.94106
464	462	16 GB - Xám	TGDD-368213-V1	34990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:49.961503	2026-09-24 21:00:49.961503
465	463	16 GB - Bạc	TGDD-331522-V1	43690000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:49.981352	2026-09-24 21:00:49.981352
466	464	32 GB - Xám	TGDD-370357-V1	64990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:50.001968	2026-09-24 21:00:50.001968
467	465	16 GB - Đen	TGDD-363896-V1	61390000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.023152	2026-09-24 21:00:50.023152
468	466	32 GB - Xám	TGDD-341573-V1	78590000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:50.046592	2026-09-24 21:00:50.046592
469	467	8 GB - Bạc	TGDD-367374-V1	15990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.068575	2026-09-24 21:00:50.068575
470	468	16 GB - Bạc	TGDD-368301-V1	24090000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.090133	2026-09-24 21:00:50.090133
471	469	16 GB - Bạc	TGDD-360418-V1	18990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.112334	2026-09-24 21:00:50.112334
472	470	8 GB - Bạc	TGDD-368146-V1	18790000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.134107	2026-09-24 21:00:50.134107
473	471	8 GB - Xám	TGDD-370862-V1	17990000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:50.158537	2026-09-24 21:00:50.158537
474	472	16 GB - Đen	TGDD-369755-V1	25990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.18166	2026-09-24 21:00:50.18166
475	473	8 GB - Bạc	TGDD-368302-V1	18790000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.201421	2026-09-24 21:00:50.201421
476	474	16 GB - Bạc	TGDD-369450-V1	24590000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.22049	2026-09-24 21:00:50.22049
477	475	8 GB - Bạc	TGDD-369758-V1	17990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.241986	2026-09-24 21:00:50.241986
478	476	8 GB - Bạc	TGDD-369746-V1	21490000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.260687	2026-09-24 21:00:50.260687
479	477	16 GB - Xám	TGDD-364941-V1	22990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.278905	2026-09-24 21:00:50.278905
480	478	8 GB - Bạc	TGDD-367892-V1	19990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.295925	2026-09-24 21:00:50.295925
481	479	16 GB - Bạc	TGDD-368147-V1	23990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.313322	2026-09-24 21:00:50.313322
482	480	16 GB - Đen	TGDD-369984-V1	29890000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.330333	2026-09-24 21:00:50.330333
483	481	16 GB - Xám	TGDD-364489-V1	42490000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.34798	2026-09-24 21:00:50.34798
484	482	32 GB - Xanh Dương	TGDD-364403-V1	35590000.00	\N	0	Xanh Dương	32 GB	\N	2026-09-24 21:00:50.364007	2026-09-24 21:00:50.364007
485	483	16 GB - Bạc	TGDD-368658-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.382743	2026-09-24 21:00:50.382743
486	484	8 GB - Bạc	TGDD-367898-V1	19990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.400352	2026-09-24 21:00:50.400352
487	485	24GB/512GB - Xanh da trời nhạt	TGDD-363503-V1	39790000.00	\N	0	Xanh da trời nhạt	512 GB	24 GB	2026-09-24 21:00:50.419013	2026-09-24 21:00:50.419013
488	486	16 GB - Bạc	TGDD-368657-V1	21990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.434761	2026-09-24 21:00:50.434761
489	487	8 GB - Đen	TGDD-369749-V1	22490000.00	\N	0	Đen	8 GB	\N	2026-09-24 21:00:50.450996	2026-09-24 21:00:50.450996
490	488	24GB/512GB - Xanh da trời nhạt	TGDD-363511-V1	44990000.00	\N	0	Xanh da trời nhạt	512 GB	24 GB	2026-09-24 21:00:50.466888	2026-09-24 21:00:50.466888
491	489	16 GB - Xám	TGDD-369983-V1	30990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.482614	2026-09-24 21:00:50.482614
492	490	16 GB - Xám	TGDD-364409-V1	46590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.498482	2026-09-24 21:00:50.498482
493	491	16 GB - Xám	TGDD-369464-V1	30090000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.514524	2026-09-24 21:00:50.514524
494	492	16 GB - Bạc	TGDD-369469-V1	26990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.531339	2026-09-24 21:00:50.531339
495	493	24GB/1TB - Đen	TGDD-363488-V1	66990000.00	\N	0	Đen	1 TB	24 GB	2026-09-24 21:00:50.547492	2026-09-24 21:00:50.547492
496	494	16 GB - Bạc	TGDD-351616-V1	23790000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.563461	2026-09-24 21:00:50.563461
497	495	16 GB - Bạc	TGDD-366826-V1	24390000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.580279	2026-09-24 21:00:50.580279
498	496	16 GB - Xám	TGDD-358371-V1	39990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.59699	2026-09-24 21:00:50.59699
499	497	16 GB - Bạc	TGDD-369449-V1	23990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.614622	2026-09-24 21:00:50.614622
500	498	16 GB - Xám	TGDD-364416-V1	28890000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.634357	2026-09-24 21:00:50.634357
501	499	16 GB - Bạc	TGDD-368148-V1	27990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.653995	2026-09-24 21:00:50.653995
502	500	16 GB - Đen	TGDD-369985-V1	27890000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.673412	2026-09-24 21:00:50.673412
503	501	16 GB - Đen	TGDD-371075-V1	32990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.69307	2026-09-24 21:00:50.69307
504	502	16 GB - Bạc	TGDD-368300-V1	26990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.712617	2026-09-24 21:00:50.712617
505	503	8 GB - Đen	TGDD-369748-V1	21490000.00	\N	0	Đen	8 GB	\N	2026-09-24 21:00:50.734962	2026-09-24 21:00:50.734962
506	504	16 GB - Xám	TGDD-364490-V1	43590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.754301	2026-09-24 21:00:50.754301
507	505	16 GB - Bạc	TGDD-365309-V1	31990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.771799	2026-09-24 21:00:50.771799
508	506	8 GB - Bạc	TGDD-367899-V1	23990000.00	\N	0	Bạc	8 GB	\N	2026-09-24 21:00:50.789017	2026-09-24 21:00:50.789017
509	507	32 GB - Xám	TGDD-369463-V1	37490000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:50.806421	2026-09-24 21:00:50.806421
510	508	16 GB - Đen	TGDD-367862-V1	35490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.823172	2026-09-24 21:00:50.823172
511	509	16 GB - Xám	TGDD-367864-V1	42590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.840608	2026-09-24 21:00:50.840608
512	510	16 GB - Bạc	TGDD-367347-V1	37290000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.860395	2026-09-24 21:00:50.860395
513	511	16 GB - Xanh da trời	TGDD-362022-V1	36290000.00	\N	0	Xanh da trời	16 GB	\N	2026-09-24 21:00:50.880109	2026-09-24 21:00:50.880109
514	512	16 GB - Đen	TGDD-365307-V1	27990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.898235	2026-09-24 21:00:50.898235
515	513	24 GB - Xám	TGDD-367682-V1	28490000.00	\N	0	Xám	24 GB	\N	2026-09-24 21:00:50.913922	2026-09-24 21:00:50.913922
516	514	16 GB - Xám	TGDD-369467-V1	30990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:50.931858	2026-09-24 21:00:50.931858
517	515	16 GB - Đen	TGDD-370280-V1	40990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:50.949353	2026-09-24 21:00:50.949353
518	516	16 GB - Bạc	TGDD-371299-V1	26690000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:50.966596	2026-09-24 21:00:50.966596
519	517	8 GB - Xám	TGDD-370863-V1	17990000.00	\N	0	Xám	8 GB	\N	2026-09-24 21:00:50.984763	2026-09-24 21:00:50.984763
520	518	16 GB - Bạc	TGDD-345504-V1	32990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:51.001809	2026-09-24 21:00:51.001809
521	519	32 GB - Đen	TGDD-363894-V1	40090000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:51.018062	2026-09-24 21:00:51.018062
522	520	16 GB - Xám	TGDD-365113-V1	42990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.034144	2026-09-24 21:00:51.034144
523	521	32GB/512GB - Vàng	TGDD-363513-V1	51790000.00	\N	0	Vàng	512 GB	32 GB	2026-09-24 21:00:51.052333	2026-09-24 21:00:51.052333
524	522	16 GB - Đen	TGDD-367863-V1	37490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.068615	2026-09-24 21:00:51.068615
525	523	512 GB - Xám	TGDD-368219-V1	31290000.00	\N	0	Xám	512 GB	\N	2026-09-24 21:00:51.086042	2026-09-24 21:00:51.086042
526	524	16 GB - Xám	TGDD-369213-V1	34590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.102005	2026-09-24 21:00:51.102005
527	525	16 GB - Xám	TGDD-370349-V1	23690000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.11853	2026-09-24 21:00:51.11853
528	526	32 GB - Trắng - Kem	TGDD-370356-V1	62990000.00	\N	0	Trắng - Kem	32 GB	\N	2026-09-24 21:00:51.135116	2026-09-24 21:00:51.135116
529	527	16 GB - Xám	TGDD-370866-V1	29990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.150817	2026-09-24 21:00:51.150817
530	528	32 GB - Xám	TGDD-364408-V1	56590000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.166823	2026-09-24 21:00:51.166823
531	529	16 GB - Bạc	TGDD-365617-V1	34990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:51.183871	2026-09-24 21:00:51.183871
532	530	32 GB - Xanh da trời	TGDD-361539-V1	56990000.00	\N	0	Xanh da trời	32 GB	\N	2026-09-24 21:00:51.201405	2026-09-24 21:00:51.201405
533	531	16 GB - Đen	TGDD-363899-V1	62390000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.221242	2026-09-24 21:00:51.221242
534	532	128 GB - Vàng	TGDD-364389-V1	109990000.00	\N	0	Vàng	128 GB	\N	2026-09-24 21:00:51.23887	2026-09-24 21:00:51.23887
535	533	16 GB - Xám	TGDD-364488-V1	57590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.256877	2026-09-24 21:00:51.256877
536	534	16 GB - Bạc	TGDD-365302-V1	31490000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:51.273308	2026-09-24 21:00:51.273308
537	535	32 GB - Xám	TGDD-366068-V1	64590000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.290852	2026-09-24 21:00:51.290852
538	536	16 GB - Xám	TGDD-366827-V1	29890000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.308429	2026-09-24 21:00:51.308429
539	537	16 GB - Đen	TGDD-367015-V1	60990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.326858	2026-09-24 21:00:51.326858
540	538	32 GB - Bạc	TGDD-367348-V1	54290000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:51.345649	2026-09-24 21:00:51.345649
541	539	16 GB - Xám	TGDD-368149-V1	54990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.363717	2026-09-24 21:00:51.363717
542	540	16 GB - Xám	TGDD-368150-V1	70590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.382024	2026-09-24 21:00:51.382024
543	541	32 GB - Xám	TGDD-368212-V1	53790000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.400287	2026-09-24 21:00:51.400287
544	542	32 GB - Vàng	TGDD-368216-V1	55990000.00	\N	0	Vàng	32 GB	\N	2026-09-24 21:00:51.418057	2026-09-24 21:00:51.418057
545	543	16 GB - Đen	TGDD-368651-V1	44990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.434919	2026-09-24 21:00:51.434919
546	544	32 GB - Xám	TGDD-369461-V1	65990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.452663	2026-09-24 21:00:51.452663
547	545	16 GB - Xanh Dương	TGDD-369979-V1	29990000.00	\N	0	Xanh Dương	16 GB	\N	2026-09-24 21:00:51.468952	2026-09-24 21:00:51.468952
548	546	16 GB - Đen	TGDD-369980-V1	43990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.485303	2026-09-24 21:00:51.485303
549	547	16 GB - Đen	TGDD-370352-V1	34190000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.501125	2026-09-24 21:00:51.501125
550	548	16 GB - Trắng - Kem	TGDD-370355-V1	40990000.00	\N	0	Trắng - Kem	16 GB	\N	2026-09-24 21:00:51.516882	2026-09-24 21:00:51.516882
551	549	16 GB - Trắng	TGDD-332274-V1	62790000.00	\N	0	Trắng	16 GB	\N	2026-09-24 21:00:51.532467	2026-09-24 21:00:51.532467
552	550	192 GB - Đen	TGDD-335963-V1	149990000.00	\N	0	Đen	192 GB	\N	2026-09-24 21:00:51.549079	2026-09-24 21:00:51.549079
553	551	64 GB - Đen	TGDD-335964-V1	99990000.00	\N	0	Đen	64 GB	\N	2026-09-24 21:00:51.564948	2026-09-24 21:00:51.564948
554	552	16 GB - Đen	TGDD-342527-V1	42990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.581459	2026-09-24 21:00:51.581459
555	553	16 GB - Xám	TGDD-361537-V1	0.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.598109	2026-09-24 21:00:51.598109
556	554	16 GB - Đen	TGDD-363895-V1	55290000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.61546	2026-09-24 21:00:51.61546
557	555	32 GB - Xám	TGDD-364868-V1	54990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.632615	2026-09-24 21:00:51.632615
558	556	16 GB - Bạc	TGDD-365615-V1	31990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:51.649417	2026-09-24 21:00:51.649417
559	557	32 GB - Bạc	TGDD-365619-V1	42590000.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:51.665833	2026-09-24 21:00:51.665833
560	558	32 GB - Bạc	TGDD-365621-V1	0.00	\N	0	Bạc	32 GB	\N	2026-09-24 21:00:51.681919	2026-09-24 21:00:51.681919
561	559	64 GB - Xám	TGDD-366066-V1	63590000.00	\N	0	Xám	64 GB	\N	2026-09-24 21:00:51.697735	2026-09-24 21:00:51.697735
562	560	32 GB - Đen	TGDD-366091-V1	49990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:51.7138	2026-09-24 21:00:51.7138
563	561	32 GB - Đen	TGDD-366092-V1	59990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:51.728982	2026-09-24 21:00:51.728982
564	562	32 GB - Đen	TGDD-366093-V1	54990000.00	\N	0	Đen	32 GB	\N	2026-09-24 21:00:51.744911	2026-09-24 21:00:51.744911
565	563	16 GB - Đen	TGDD-366744-V1	54990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.762974	2026-09-24 21:00:51.762974
566	564	16 GB - Xám	TGDD-367686-V1	49990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.782322	2026-09-24 21:00:51.782322
567	565	64 GB - Đen	TGDD-367865-V1	129590000.00	\N	0	Đen	64 GB	\N	2026-09-24 21:00:51.801703	2026-09-24 21:00:51.801703
568	566	16 GB - Xám	TGDD-367887-V1	42590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.826911	2026-09-24 21:00:51.826911
569	567	16 GB - Trắng	TGDD-367888-V1	45090000.00	\N	0	Trắng	16 GB	\N	2026-09-24 21:00:51.85401	2026-09-24 21:00:51.85401
570	568	32 GB - Xám	TGDD-367889-V1	118590000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.883718	2026-09-24 21:00:51.883718
571	569	16 GB - Màu be	TGDD-367890-V1	39990000.00	\N	0	Màu be	16 GB	\N	2026-09-24 21:00:51.909807	2026-09-24 21:00:51.909807
572	570	32 GB - Xám	TGDD-367891-V1	54990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.934306	2026-09-24 21:00:51.934306
573	571	32 GB - Xám	TGDD-367897-V1	78990000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:51.954141	2026-09-24 21:00:51.954141
574	572	16 GB - Xám	TGDD-368214-V1	28990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:51.975369	2026-09-24 21:00:51.975369
575	573	16 GB - Đen	TGDD-368650-V1	42990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:51.995961	2026-09-24 21:00:51.995961
576	574	16 GB - Đen	TGDD-368653-V1	75990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.015172	2026-09-24 21:00:52.015172
577	575	16 GB - Đen	TGDD-368659-V1	57990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.033503	2026-09-24 21:00:52.033503
578	576	16 GB - Đen	TGDD-369460-V1	34590000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.054074	2026-09-24 21:00:52.054074
579	577	64 GB - Xám	TGDD-369462-V1	97990000.00	\N	0	Xám	64 GB	\N	2026-09-24 21:00:52.072846	2026-09-24 21:00:52.072846
580	578	16 GB - Xám	TGDD-369465-V1	35990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.091067	2026-09-24 21:00:52.091067
581	579	16 GB - Xám	TGDD-369466-V1	35990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.109272	2026-09-24 21:00:52.109272
582	580	16 GB - Đen	TGDD-369476-V1	52990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.127397	2026-09-24 21:00:52.127397
583	581	16 GB - Đen	TGDD-369482-V1	0.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.146599	2026-09-24 21:00:52.146599
584	582	16 GB - Bạc	TGDD-369751-V1	27990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.164692	2026-09-24 21:00:52.164692
585	583	64 GB - Đen	TGDD-369756-V1	114890000.00	\N	0	Đen	64 GB	\N	2026-09-24 21:00:52.182715	2026-09-24 21:00:52.182715
586	584	16 GB - Đen	TGDD-369981-V1	49990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.201551	2026-09-24 21:00:52.201551
587	585	16 GB - Đen	TGDD-369982-V1	46990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.220201	2026-09-24 21:00:52.220201
588	586	32 GB - Xanh Dương	TGDD-370278-V1	39990000.00	\N	0	Xanh Dương	32 GB	\N	2026-09-24 21:00:52.237987	2026-09-24 21:00:52.237987
589	587	16 GB - Đen	TGDD-370279-V1	38990000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.255553	2026-09-24 21:00:52.255553
590	588	16 GB - Đen	TGDD-370353-V1	39190000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.272785	2026-09-24 21:00:52.272785
591	589	16 GB - Xám	TGDD-370354-V1	40990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.291129	2026-09-24 21:00:52.291129
592	590	32 GB - Trắng - Kem	TGDD-370358-V1	68990000.00	\N	0	Trắng - Kem	32 GB	\N	2026-09-24 21:00:52.310063	2026-09-24 21:00:52.310063
593	591	16 GB - Xám	TGDD-370582-V1	41590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.330262	2026-09-24 21:00:52.330262
594	592	16 GB - Xám	TGDD-370583-V1	43590000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.350211	2026-09-24 21:00:52.350211
595	593	12 GB - Xanh lá	TGDD-370864-V1	24990000.00	\N	0	Xanh lá	12 GB	\N	2026-09-24 21:00:52.369238	2026-09-24 21:00:52.369238
596	594	12 GB - Xanh lá	TGDD-370865-V1	21990000.00	\N	0	Xanh lá	12 GB	\N	2026-09-24 21:00:52.388162	2026-09-24 21:00:52.388162
597	595	16 GB - Xám	TGDD-370910-V1	32990000.00	\N	0	Xám	16 GB	\N	2026-09-24 21:00:52.406872	2026-09-24 21:00:52.406872
598	596	16 GB - Vàng	TGDD-371123-V1	31590000.00	\N	0	Vàng	16 GB	\N	2026-09-24 21:00:52.425852	2026-09-24 21:00:52.425852
599	597	16 GB - Bạc	TGDD-371124-V1	30590000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.444924	2026-09-24 21:00:52.444924
600	598	16 GB - Bạc	TGDD-371125-V1	30590000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.46436	2026-09-24 21:00:52.46436
601	599	16 GB - Bạc	TGDD-371127-V1	39190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.483626	2026-09-24 21:00:52.483626
602	600	32 GB - Xám	TGDD-371128-V1	59190000.00	\N	0	Xám	32 GB	\N	2026-09-24 21:00:52.502025	2026-09-24 21:00:52.502025
603	601	32 GB - Vàng	TGDD-371129-V1	65190000.00	\N	0	Vàng	32 GB	\N	2026-09-24 21:00:52.520389	2026-09-24 21:00:52.520389
604	602	16 GB - Bạc	TGDD-371130-V1	35990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.540221	2026-09-24 21:00:52.540221
605	603	16 GB - Bạc	TGDD-371132-V1	36990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.5575	2026-09-24 21:00:52.5575
606	604	16 GB - Bạc	TGDD-371133-V1	38990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.574202	2026-09-24 21:00:52.574202
607	605	16 GB - Bạc	TGDD-371134-V1	39990000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.592226	2026-09-24 21:00:52.592226
608	606	32 GB - Xanh	TGDD-371135-V1	49190000.00	\N	0	Xanh	32 GB	\N	2026-09-24 21:00:52.609842	2026-09-24 21:00:52.609842
609	607	16 GB - Bạc	TGDD-371137-V1	35190000.00	\N	0	Bạc	16 GB	\N	2026-09-24 21:00:52.626982	2026-09-24 21:00:52.626982
610	608	16 GB - Đen	TGDD-371296-V1	27490000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:52.64405	2026-09-24 21:00:52.64405
611	609	128 GB - Vàng	TGDD-335308-V1	12290000.00	\N	0	Vàng	128 GB	\N	2026-09-24 21:00:52.661218	2026-09-24 21:00:52.661218
612	610	128 GB - Đen - Xám	TGDD-363417-V1	20690000.00	\N	0	Đen - Xám	128 GB	\N	2026-09-24 21:00:52.678248	2026-09-24 21:00:52.678248
613	611	8GB/256GB - Hồng	TGDD-361231-V1	13490000.00	\N	0	Hồng	256 GB	8 GB	2026-09-24 21:00:52.695825	2026-09-24 21:00:52.695825
614	612	8GB/128GB - Xanh lá đậm	TGDD-362716-V1	9990000.00	\N	0	Xanh lá đậm	128 GB	8 GB	2026-09-24 21:00:52.713009	2026-09-24 21:00:52.713009
615	613	6GB/128GB - Xám	TGDD-359089-V1	7840000.00	\N	0	Xám	128 GB	6 GB	2026-09-24 21:00:52.730258	2026-09-24 21:00:52.730258
616	614	6GB/128GB - Xám	TGDD-359086-V1	6440000.00	\N	0	Xám	128 GB	6 GB	2026-09-24 21:00:52.747523	2026-09-24 21:00:52.747523
617	615	8GB/128GB - Xanh lá đậm	TGDD-362718-V1	14390000.00	\N	0	Xanh lá đậm	128 GB	8 GB	2026-09-24 21:00:52.765119	2026-09-24 21:00:52.765119
618	616	4GB/128GB - Bạc	TGDD-339287-V1	6090000.00	\N	0	Bạc	128 GB	4 GB	2026-09-24 21:00:52.782107	2026-09-24 21:00:52.782107
619	617	128 GB - Tím	TGDD-331229-V1	16290000.00	\N	0	Tím	128 GB	\N	2026-09-24 21:00:52.799737	2026-09-24 21:00:52.799737
620	618	256 GB - Đen	TGDD-358082-V1	34490000.00	\N	0	Đen	256 GB	\N	2026-09-24 21:00:52.81697	2026-09-24 21:00:52.81697
621	619	8GB/128GB - Xám	TGDD-320992-V1	6800000.00	\N	0	Xám	128 GB	8 GB	2026-09-24 21:00:52.834946	2026-09-24 21:00:52.834946
622	620	6GB/128GB - Xám	TGDD-343061-V1	9540000.00	\N	0	Xám	128 GB	6 GB	2026-09-24 21:00:52.853498	2026-09-24 21:00:52.853498
623	621	4GB/64GB - Xám	TGDD-345548-V1	4390000.00	\N	0	Xám	64 GB	4 GB	2026-09-24 21:00:52.872173	2026-09-24 21:00:52.872173
624	622	4GB/64GB - Xám	TGDD-345544-V1	3140000.00	\N	0	Xám	64 GB	4 GB	2026-09-24 21:00:52.891001	2026-09-24 21:00:52.891001
625	623	12GB/128GB - Xám	TGDD-344721-V1	22710000.00	\N	0	Xám	128 GB	12 GB	2026-09-24 21:00:52.909989	2026-09-24 21:00:52.909989
626	624	8GB/128GB - Xanh Dương	TGDD-342548-V1	7540000.00	\N	0	Xanh Dương	128 GB	8 GB	2026-09-24 21:00:52.930237	2026-09-24 21:00:52.930237
627	625	4GB/128GB - Xám	TGDD-339204-V1	5980000.00	\N	0	Xám	128 GB	4 GB	2026-09-24 21:00:52.957117	2026-09-24 21:00:52.957117
628	626	128 GB - Bạc	TGDD-335311-V1	16490000.00	\N	0	Bạc	128 GB	\N	2026-09-24 21:00:52.978016	2026-09-24 21:00:52.978016
629	627	6GB/128GB - Bạc	TGDD-356854-V1	6590000.00	\N	0	Bạc	128 GB	6 GB	2026-09-24 21:00:53.000791	2026-09-24 21:00:53.000791
630	628	8GB/256GB - Bạc	TGDD-333917-V1	10330000.00	\N	0	Bạc	256 GB	8 GB	2026-09-24 21:00:53.024682	2026-09-24 21:00:53.024682
631	629	8GB/128GB - Xám	TGDD-336740-V1	14340000.00	\N	0	Xám	128 GB	8 GB	2026-09-24 21:00:53.047339	2026-09-24 21:00:53.047339
632	630	12GB/256GB - Xám	TGDD-322130-V1	19840000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:53.067531	2026-09-24 21:00:53.067531
633	631	8GB/128GB - Xanh Dương	TGDD-336738-V1	12610000.00	\N	0	Xanh Dương	128 GB	8 GB	2026-09-24 21:00:53.085981	2026-09-24 21:00:53.085981
634	632	4GB/128GB - Xanh lá	TGDD-339207-V1	5490000.00	\N	0	Xanh lá	128 GB	4 GB	2026-09-24 21:00:53.101745	2026-09-24 21:00:53.101745
635	633	6GB/128GB - Bạc	TGDD-356864-V1	7890000.00	\N	0	Bạc	128 GB	6 GB	2026-09-24 21:00:53.121461	2026-09-24 21:00:53.121461
636	634	4GB/128GB - Bạc	TGDD-339286-V1	7140000.00	\N	0	Bạc	128 GB	4 GB	2026-09-24 21:00:53.139238	2026-09-24 21:00:53.139238
637	635	4GB/64GB - Xám	TGDD-345546-V1	3390000.00	\N	0	Xám	64 GB	4 GB	2026-09-24 21:00:53.158295	2026-09-24 21:00:53.158295
638	636	8GB/256GB - Bạc	TGDD-365596-V1	10240000.00	\N	0	Bạc	256 GB	8 GB	2026-09-24 21:00:53.177285	2026-09-24 21:00:53.177285
639	877	8GB/128GB - Bạc	TGDD-336737-V1	10800000.00	\N	0	Bạc	128 GB	8 GB	2026-09-24 21:00:53.198475	2026-09-24 21:00:53.198475
640	637	4GB/64GB - Bạc	TGDD-366579-V1	4190000.00	\N	0	Bạc	64 GB	4 GB	2026-09-24 21:00:53.237048	2026-09-24 21:00:53.237048
641	638	4GB/64GB - Bạc	TGDD-366598-V1	4690000.00	\N	0	Bạc	64 GB	4 GB	2026-09-24 21:00:53.263367	2026-09-24 21:00:53.263367
642	639	12GB/128GB - Xám	TGDD-344723-V1	19240000.00	\N	0	Xám	128 GB	12 GB	2026-09-24 21:00:53.283321	2026-09-24 21:00:53.283321
643	640	12GB/256GB - Xám	TGDD-344725-V1	30690000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:53.302435	2026-09-24 21:00:53.302435
644	641	128 GB - Đen - Xám	TGDD-363422-V1	26190000.00	\N	0	Đen - Xám	128 GB	\N	2026-09-24 21:00:53.32151	2026-09-24 21:00:53.32151
645	642	32 GB - Trắng	TGDD-363821-V1	3240000.00	\N	0	Trắng	32 GB	\N	2026-09-24 21:00:53.34027	2026-09-24 21:00:53.34027
646	643	8GB/256GB - Xám	TGDD-368575-V1	10040000.00	\N	0	Xám	256 GB	8 GB	2026-09-24 21:00:53.358574	2026-09-24 21:00:53.359104
647	644	4GB/128GB - Xám	TGDD-368516-V1	5740000.00	\N	0	Xám	128 GB	4 GB	2026-09-24 21:00:53.377489	2026-09-24 21:00:53.377489
648	645	256 GB - Bạc	TGDD-358099-V1	43790000.00	\N	0	Bạc	256 GB	\N	2026-09-24 21:00:53.396717	2026-09-24 21:00:53.396717
649	646	16 GB - Đen	TGDD-363825-V1	3940000.00	\N	0	Đen	16 GB	\N	2026-09-24 21:00:53.416442	2026-09-24 21:00:53.416442
650	647	128 GB - Tím	TGDD-363427-V1	24890000.00	\N	0	Tím	128 GB	\N	2026-09-24 21:00:53.434898	2026-09-24 21:00:53.434898
651	648	128 GB - Tím	TGDD-363432-V1	30390000.00	\N	0	Tím	128 GB	\N	2026-09-24 21:00:53.453771	2026-09-24 21:00:53.453771
652	649	256 GB - Đen	TGDD-358105-V1	39990000.00	\N	0	Đen	256 GB	\N	2026-09-24 21:00:53.473215	2026-09-24 21:00:53.473215
653	650	256 GB - Đen	TGDD-358111-V1	49590000.00	\N	0	Đen	256 GB	\N	2026-09-24 21:00:53.492548	2026-09-24 21:00:53.492548
654	651	12GB/256GB - Đen	TGDD-368580-V1	14140000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:53.512017	2026-09-24 21:00:53.512017
655	652	12GB/256GB - Xám	TGDD-368577-V1	16440000.00	\N	0	Xám	256 GB	12 GB	2026-09-24 21:00:53.530853	2026-09-24 21:00:53.530853
656	653	32 GB - Trắng	TGDD-363822-V1	4840000.00	\N	0	Trắng	32 GB	\N	2026-09-24 21:00:53.549229	2026-09-24 21:00:53.549229
657	654	12GB/256GB - Đen	TGDD-368581-V1	21340000.00	\N	0	Đen	256 GB	12 GB	2026-09-24 21:00:53.567093	2026-09-24 21:00:53.567093
658	655	8GB/256GB - Xám	TGDD-339828-V1	7890000.00	\N	0	Xám	256 GB	8 GB	2026-09-24 21:00:53.585574	2026-09-24 21:00:53.585574
659	656	128 GB - Đen	TGDD-363824-V1	7640000.00	\N	0	Đen	128 GB	\N	2026-09-24 21:00:53.604904	2026-09-24 21:00:53.604904
660	657	8GB/256GB - Xám	TGDD-368582-V1	17980000.00	\N	0	Xám	256 GB	8 GB	2026-09-24 21:00:53.624917	2026-09-24 21:00:53.624917
661	658	6GB/128GB - Xám	TGDD-367014-V1	6640000.00	\N	0	Xám	128 GB	6 GB	2026-09-24 21:00:53.644353	2026-09-24 21:00:53.644353
662	659	8GB/256GB - Xám	TGDD-368579-V1	8440000.00	\N	0	Xám	256 GB	8 GB	2026-09-24 21:00:53.663742	2026-09-24 21:00:53.663742
663	660	Đen	TGDD-358003-V1	1590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:53.68415	2026-09-24 21:00:53.68415
664	661	Xanh - Xám	TGDD-366954-V1	3290000.00	\N	0	Xanh - Xám	\N	\N	2026-09-24 21:00:53.704621	2026-09-24 21:00:53.704621
665	662	Hồng	TGDD-354255-V1	1790000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:53.724093	2026-09-24 21:00:53.724093
666	663	Cam	TGDD-366955-V1	4990000.00	\N	0	Cam	\N	\N	2026-09-24 21:00:53.743409	2026-09-24 21:00:53.743409
667	664	Màu combo	TGDD-364509-V1	5590000.00	\N	0	Màu combo	\N	\N	2026-09-24 21:00:53.76484	2026-09-24 21:00:53.76484
668	665	Đen	TGDD-367947-V1	2390000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:53.784227	2026-09-24 21:00:53.784227
669	666	Trắng Starlight	TGDD-344767-V1	6790000.00	\N	0	Trắng Starlight	\N	\N	2026-09-24 21:00:53.803534	2026-09-24 21:00:53.803534
670	667	Xanh lá	TGDD-362941-V1	990000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:53.822171	2026-09-24 21:00:53.822171
671	668	Đen	TGDD-334436-V1	2540000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:53.841207	2026-09-24 21:00:53.841207
672	669	Đen bóng	TGDD-344750-V1	11490000.00	\N	0	Đen bóng	\N	\N	2026-09-24 21:00:53.860177	2026-09-24 21:00:53.860177
673	670	Trắng	TGDD-367410-V1	2490000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:53.879582	2026-09-24 21:00:53.879582
674	671	Trắng	TGDD-364290-V1	3290000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:53.898403	2026-09-24 21:00:53.898403
675	672	Đen	TGDD-362942-V1	890000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:53.917634	2026-09-24 21:00:53.917634
676	673	Xanh rêu	TGDD-369324-V1	17490000.00	\N	0	Xanh rêu	\N	\N	2026-09-24 21:00:53.935884	2026-09-24 21:00:53.935884
677	674	Đen	TGDD-361814-V1	3490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:53.956147	2026-09-24 21:00:53.956147
678	675	Xanh lá	TGDD-362940-V1	1590000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:53.977245	2026-09-24 21:00:53.977245
679	676	Trắng Starlight	TGDD-344769-V1	8390000.00	\N	0	Trắng Starlight	\N	\N	2026-09-24 21:00:53.997958	2026-09-24 21:00:53.997958
680	677	Trắng	TGDD-369499-V1	9490000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.018452	2026-09-24 21:00:54.018452
681	678	Tím bạc	TGDD-339492-V1	6990000.00	\N	0	Tím bạc	\N	\N	2026-09-24 21:00:54.039251	2026-09-24 21:00:54.039251
682	679	Titan vàng	TGDD-344758-V1	20490000.00	\N	0	Titan vàng	\N	\N	2026-09-24 21:00:54.062915	2026-09-24 21:00:54.062915
683	680	Đen	TGDD-337872-V1	6990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.082782	2026-09-24 21:00:54.082782
684	681	Trắng	TGDD-369498-V1	10990000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.103097	2026-09-24 21:00:54.103097
685	682	Đen	TGDD-369497-V1	10490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.122402	2026-09-24 21:00:54.122402
686	683	Xanh lá	TGDD-369321-V1	11290000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:54.142092	2026-09-24 21:00:54.142092
687	684	Titan đen	TGDD-344764-V1	23490000.00	\N	0	Titan đen	\N	\N	2026-09-24 21:00:54.161841	2026-09-24 21:00:54.161841
688	685	Vàng Hồng	TGDD-336948-V1	8990000.00	\N	0	Vàng Hồng	\N	\N	2026-09-24 21:00:54.180904	2026-09-24 21:00:54.180904
689	686	Đen	TGDD-364292-V1	12990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.200076	2026-09-24 21:00:54.200076
690	687	Trắng	TGDD-339531-V1	1660000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.21949	2026-09-24 21:00:54.21949
691	688	Đen	TGDD-341453-V1	6590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.237553	2026-09-24 21:00:54.237553
692	689	Đen	TGDD-361330-V1	3990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.25565	2026-09-24 21:00:54.25565
693	690	Đỏ	TGDD-337643-V1	1890000.00	\N	0	Đỏ	\N	\N	2026-09-24 21:00:54.274401	2026-09-24 21:00:54.274401
694	691	Nâu	TGDD-362002-V1	4790000.00	\N	0	Nâu	\N	\N	2026-09-24 21:00:54.293553	2026-09-24 21:00:54.293553
695	692	Bạc	TGDD-327697-V1	4840000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:54.312062	2026-09-24 21:00:54.312062
696	693	Nâu	TGDD-354257-V1	6990000.00	\N	0	Nâu	\N	\N	2026-09-24 21:00:54.331852	2026-09-24 21:00:54.331852
697	694	Xanh lá	TGDD-354260-V1	4790000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:54.351485	2026-09-24 21:00:54.351485
698	695	Xanh - Xám	TGDD-361467-V1	3990000.00	\N	0	Xanh - Xám	\N	\N	2026-09-24 21:00:54.371192	2026-09-24 21:00:54.371192
699	696	Trắng	TGDD-338266-V1	8790000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.3902	2026-09-24 21:00:54.3902
700	697	Bạc	TGDD-354258-V1	10490000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:54.41094	2026-09-24 21:00:54.41094
701	698	Trắng	TGDD-354261-V1	4790000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.429957	2026-09-24 21:00:54.429957
702	699	Nâu	TGDD-354259-V1	4790000.00	\N	0	Nâu	\N	\N	2026-09-24 21:00:54.447991	2026-09-24 21:00:54.447991
703	700	Trắng	TGDD-340066-V1	6890000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.465589	2026-09-24 21:00:54.465589
704	701	Đen	TGDD-361516-V1	8990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.485893	2026-09-24 21:00:54.485893
705	702	Đen	TGDD-358004-V1	8990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.50488	2026-09-24 21:00:54.50488
706	703	Vàng	TGDD-361518-V1	6290000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:54.523609	2026-09-24 21:00:54.523609
707	704	Đen bóng	TGDD-344753-V1	14990000.00	\N	0	Đen bóng	\N	\N	2026-09-24 21:00:54.541432	2026-09-24 21:00:54.541432
708	705	Đỏ	TGDD-333919-V1	5290000.00	\N	0	Đỏ	\N	\N	2026-09-24 21:00:54.559042	2026-09-24 21:00:54.559042
709	706	Đen	TGDD-340067-V1	10490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.575402	2026-09-24 21:00:54.575402
710	707	Đen	TGDD-340068-V1	6890000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.5934	2026-09-24 21:00:54.5934
711	708	Đen	TGDD-340844-V1	2990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.613747	2026-09-24 21:00:54.613747
712	709	Trắng	TGDD-327693-V1	12100000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.631594	2026-09-24 21:00:54.631594
713	710	Đen	TGDD-341454-V1	6990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.651033	2026-09-24 21:00:54.651033
714	711	Titan xám	TGDD-344754-V1	21790000.00	\N	0	Titan xám	\N	\N	2026-09-24 21:00:54.669042	2026-09-24 21:00:54.669042
715	712	Xanh Dương	TGDD-358006-V1	19290000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:54.686025	2026-09-24 21:00:54.686025
716	713	Đen	TGDD-358005-V1	15290000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.703757	2026-09-24 21:00:54.703757
717	714	Bạc	TGDD-337834-V1	9810000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:54.721811	2026-09-24 21:00:54.721811
718	715	Xám	TGDD-370871-V1	28990000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:54.740895	2026-09-24 21:00:54.740895
719	716	Đen	TGDD-370934-V1	30290000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.758312	2026-09-24 21:00:54.758312
720	717	Xám	TGDD-370869-V1	26290000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:54.777693	2026-09-24 21:00:54.777693
721	718	Đen	TGDD-370930-V1	27690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.795693	2026-09-24 21:00:54.795693
722	719	Kem	TGDD-370870-V1	30290000.00	\N	0	Kem	\N	\N	2026-09-24 21:00:54.813438	2026-09-24 21:00:54.813438
723	720	Xanh Dương	TGDD-328694-V1	2740000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:54.83125	2026-09-24 21:00:54.83125
724	721	Đen	TGDD-322848-V1	3990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.849402	2026-09-24 21:00:54.849402
725	722	Đen	TGDD-367407-V1	4690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.867164	2026-09-24 21:00:54.867164
726	723	Trắng	TGDD-332406-V1	630000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:54.884893	2026-09-24 21:00:54.884893
727	724	Đen	TGDD-335762-V1	690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.902728	2026-09-24 21:00:54.902728
728	725	Đen	TGDD-331078-V1	7840000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.91929	2026-09-24 21:00:54.91929
729	726	Đen	TGDD-244296-V1	2590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:54.935669	2026-09-24 21:00:54.935669
730	727	Hồng	TGDD-332404-V1	1750000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:54.951787	2026-09-24 21:00:54.951787
731	728	Xanh dương đậm	TGDD-365405-V1	5750000.00	\N	0	Xanh dương đậm	\N	\N	2026-09-24 21:00:54.968052	2026-09-24 21:00:54.968052
732	729	Trắng Starlight	TGDD-355663-V1	14240000.00	\N	0	Trắng Starlight	\N	\N	2026-09-24 21:00:54.984146	2026-09-24 21:00:54.984146
733	730	Đen	TGDD-337137-V1	7320000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.000101	2026-09-24 21:00:55.000101
734	731	Xám	TGDD-338698-V1	17660000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:55.01649	2026-09-24 21:00:55.01649
735	732	Trắng - Xanh	TGDD-338701-V1	12340000.00	\N	0	Trắng - Xanh	\N	\N	2026-09-24 21:00:55.032851	2026-09-24 21:00:55.032851
736	733	Trắng	TGDD-365406-V1	6950000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:55.048239	2026-09-24 21:00:55.048239
737	734	Đen	TGDD-334987-V1	10760000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.064954	2026-09-24 21:00:55.064954
738	735	Trắng	TGDD-330180-V1	11990000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:55.082099	2026-09-24 21:00:55.082099
739	736	Đen	TGDD-334986-V1	9660000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.098157	2026-09-24 21:00:55.098157
740	737	Đen	TGDD-334989-V1	7140000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.115759	2026-09-24 21:00:55.115759
741	738	Xanh rêu	TGDD-340846-V1	17920000.00	\N	0	Xanh rêu	\N	\N	2026-09-24 21:00:55.132953	2026-09-24 21:00:55.132953
742	739	Đen	TGDD-365217-V1	2590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.149333	2026-09-24 21:00:55.149333
743	740	Đen	TGDD-341452-V1	4390000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.16496	2026-09-24 21:00:55.16496
744	741	Đen	TGDD-332069-V1	1890000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.181348	2026-09-24 21:00:55.181348
745	742	Đen	TGDD-329834-V1	880000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.197307	2026-09-24 21:00:55.197307
746	743	Vàng	TGDD-329832-V1	1190000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:55.213624	2026-09-24 21:00:55.213624
747	744	Tím	TGDD-354262-V1	4390000.00	\N	0	Tím	\N	\N	2026-09-24 21:00:55.229647	2026-09-24 21:00:55.229647
748	745	Xanh mint	TGDD-341442-V1	3990000.00	\N	0	Xanh mint	\N	\N	2026-09-24 21:00:55.244933	2026-09-24 21:00:55.244933
749	746	Trắng	TGDD-338265-V1	8490000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:55.261347	2026-09-24 21:00:55.261347
750	747	Titan đen	TGDD-344809-V1	26490000.00	\N	0	Titan đen	\N	\N	2026-09-24 21:00:55.278959	2026-09-24 21:00:55.27948
751	748	Vàng đồng	TGDD-354314-V1	5690000.00	\N	0	Vàng đồng	\N	\N	2026-09-24 21:00:55.29673	2026-09-24 21:00:55.29673
752	749	Đen	TGDD-329474-V1	25020000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.313548	2026-09-24 21:00:55.313548
753	750	Đen	TGDD-335516-V1	3510000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.330703	2026-09-24 21:00:55.330703
754	751	Đen - Vàng đồng	TGDD-360220-V1	8690000.00	\N	0	Đen - Vàng đồng	\N	\N	2026-09-24 21:00:55.347336	2026-09-24 21:00:55.347336
755	752	Đen	TGDD-340065-V1	13490000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.364626	2026-09-24 21:00:55.364626
756	753	Titan đen	TGDD-344765-V1	23490000.00	\N	0	Titan đen	\N	\N	2026-09-24 21:00:55.382275	2026-09-24 21:00:55.382275
757	754	Đen	TGDD-329473-V1	25020000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.400242	2026-09-24 21:00:55.400242
758	755	Đen	TGDD-329472-V1	22520000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.417169	2026-09-24 21:00:55.417169
759	756	Đen	TGDD-329479-V1	18090000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.4329	2026-09-24 21:00:55.4329
760	757	Trắng	TGDD-329468-V1	22520000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:55.45081	2026-09-24 21:00:55.45081
761	758	Xanh lá	TGDD-333790-V1	1990000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:55.468292	2026-09-24 21:00:55.468292
762	759	Hồng nhạt	TGDD-316991-V1	2490000.00	\N	0	Hồng nhạt	\N	\N	2026-09-24 21:00:55.485965	2026-09-24 21:00:55.485965
763	760	Xanh dương đậm	TGDD-338267-V1	12090000.00	\N	0	Xanh dương đậm	\N	\N	2026-09-24 21:00:55.504671	2026-09-24 21:00:55.504671
764	761	Xanh Dương	TGDD-318631-V1	490000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.523459	2026-09-24 21:00:55.523459
765	762	Đen - Vàng	TGDD-322845-V1	6840000.00	\N	0	Đen - Vàng	\N	\N	2026-09-24 21:00:55.542041	2026-09-24 21:00:55.542041
766	763	Hồng	TGDD-329076-V1	1890000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.560451	2026-09-24 21:00:55.560451
767	764	Đen	TGDD-336875-V1	2990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.580084	2026-09-24 21:00:55.580084
768	765	Đen	TGDD-335088-V1	2590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.599133	2026-09-24 21:00:55.599133
769	766	Bạc	TGDD-330159-V1	10490000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:55.616136	2026-09-24 21:00:55.616136
770	767	Đen	TGDD-315897-V1	5690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.632402	2026-09-24 21:00:55.632402
771	768	Xanh Dương	TGDD-324886-V1	1160000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.648849	2026-09-24 21:00:55.648849
772	769	Xanh Dương	TGDD-288629-V1	1260000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.665613	2026-09-24 21:00:55.665613
773	770	Xanh dương đậm	TGDD-236901-V1	1800000.00	\N	0	Xanh dương đậm	\N	\N	2026-09-24 21:00:55.681212	2026-09-24 21:00:55.681212
774	771	Xanh Dương	TGDD-236904-V1	1390000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.69772	2026-09-24 21:00:55.69772
775	772	Hồng	TGDD-339434-V1	1040000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.713989	2026-09-24 21:00:55.713989
776	773	Đen	TGDD-367385-V1	1440000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.729448	2026-09-24 21:00:55.729448
777	774	Hồng	TGDD-362629-V1	1490000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.745243	2026-09-24 21:00:55.745243
778	775	Xanh Dương	TGDD-326906-V1	1190000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.760872	2026-09-24 21:00:55.760872
779	776	Đen	TGDD-337061-V1	1890000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.775783	2026-09-24 21:00:55.775783
780	777	Đen	TGDD-370087-V1	1190000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.793675	2026-09-24 21:00:55.793675
781	778	Đen	TGDD-369986-V1	1690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.811491	2026-09-24 21:00:55.811491
782	779	Hồng	TGDD-370976-V1	970000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.829265	2026-09-24 21:00:55.829265
783	780	Hồng	TGDD-368547-V1	1190000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.847335	2026-09-24 21:00:55.847335
784	781	Hồng	TGDD-368548-V1	1490000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.864426	2026-09-24 21:00:55.864426
785	782	Xanh Dương	TGDD-358457-V1	3840000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:55.881569	2026-09-24 21:00:55.881569
786	783	Xanh Nước Biển	TGDD-370841-V1	4990000.00	\N	0	Xanh Nước Biển	\N	\N	2026-09-24 21:00:55.900399	2026-09-24 21:00:55.900399
787	784	Đen	TGDD-327123-V1	1260000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.91704	2026-09-24 21:00:55.91704
788	785	Bạc	TGDD-344999-V1	1190000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:55.93364	2026-09-24 21:00:55.93364
789	786	Đen	TGDD-370088-V1	890000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.949852	2026-09-24 21:00:55.949852
790	787	Hồng	TGDD-313829-V1	9830000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.965577	2026-09-24 21:00:55.965577
791	788	Hồng	TGDD-359399-V1	740000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:55.981083	2026-09-24 21:00:55.981083
792	789	Đen	TGDD-359401-V1	1240000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:55.997279	2026-09-24 21:00:55.997279
793	790	Đen	TGDD-322267-V1	1790000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.013933	2026-09-24 21:00:56.013933
794	791	Xanh lá	TGDD-363022-V1	5990000.00	\N	0	Xanh lá	\N	\N	2026-09-24 21:00:56.029288	2026-09-24 21:00:56.029288
795	792	Xanh Dương	TGDD-329598-V1	4990000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:56.045906	2026-09-24 21:00:56.045906
796	793	Xanh Dương	TGDD-369501-V1	890000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:56.062476	2026-09-24 21:00:56.062476
797	794	Đen	TGDD-369675-V1	8690000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.078129	2026-09-24 21:00:56.078129
798	795	Đen	TGDD-305882-V1	9180000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.093989	2026-09-24 21:00:56.093989
799	796	Cam	TGDD-329945-V1	5490000.00	\N	0	Cam	\N	\N	2026-09-24 21:00:56.110164	2026-09-24 21:00:56.110164
800	797	Đen	TGDD-359400-V1	940000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.126381	2026-09-24 21:00:56.126381
801	798	Xám nhạt	TGDD-367419-V1	8190000.00	\N	0	Xám nhạt	\N	\N	2026-09-24 21:00:56.141807	2026-09-24 21:00:56.141807
802	799	Tím	TGDD-322839-V1	6180000.00	\N	0	Tím	\N	\N	2026-09-24 21:00:56.157345	2026-09-24 21:00:56.157345
803	800	Xám	TGDD-366928-V1	10990000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:56.174224	2026-09-24 21:00:56.174224
804	801	Bạc	TGDD-369504-V1	1750000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.189278	2026-09-24 21:00:56.189278
805	802	Đen	TGDD-308291-V1	14370000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.204598	2026-09-24 21:00:56.204598
806	803	Đen	TGDD-310710-V1	9180000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.220179	2026-09-24 21:00:56.220179
807	804	Xám	TGDD-322375-V1	4480000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:56.235125	2026-09-24 21:00:56.235125
808	805	Đen - Tím	TGDD-322846-V1	7840000.00	\N	0	Đen - Tím	\N	\N	2026-09-24 21:00:56.25014	2026-09-24 21:00:56.25014
809	806	Xám nhạt	TGDD-329514-V1	13340000.00	\N	0	Xám nhạt	\N	\N	2026-09-24 21:00:56.265144	2026-09-24 21:00:56.265144
810	807	Tím	TGDD-330793-V1	7320000.00	\N	0	Tím	\N	\N	2026-09-24 21:00:56.282866	2026-09-24 21:00:56.282866
811	808	Đen	TGDD-334983-V1	8640000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.301204	2026-09-24 21:00:56.301204
812	809	Trắng	TGDD-335627-V1	9710000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:56.317575	2026-09-24 21:00:56.317575
813	810	Đen	TGDD-337773-V1	100000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.334332	2026-09-24 21:00:56.334332
814	811	Trắng - Vàng	TGDD-339863-V1	6990000.00	\N	0	Trắng - Vàng	\N	\N	2026-09-24 21:00:56.351331	2026-09-24 21:00:56.351331
815	812	Vàng nhạt	TGDD-364650-V1	25020000.00	\N	0	Vàng nhạt	\N	\N	2026-09-24 21:00:56.367592	2026-09-24 21:00:56.367592
816	813	Đen	TGDD-368303-V1	9990000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.384339	2026-09-24 21:00:56.384339
817	814	Đen	TGDD-369503-V1	1740000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.401465	2026-09-24 21:00:56.401465
818	815	Đen	TGDD-337830-V1	9810000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.417955	2026-09-24 21:00:56.417955
819	816	Bạc	TGDD-337840-V1	9810000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.434829	2026-09-24 21:00:56.434829
820	817	Vàng	TGDD-364063-V1	4590000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.452335	2026-09-24 21:00:56.452335
821	818	Cam	TGDD-364064-V1	7390000.00	\N	0	Cam	\N	\N	2026-09-24 21:00:56.468495	2026-09-24 21:00:56.468495
822	819	Trắng	TGDD-364065-V1	9390000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:56.48555	2026-09-24 21:00:56.48555
823	820	Xám - đồng	TGDD-371218-V1	11490000.00	\N	0	Xám - đồng	\N	\N	2026-09-24 21:00:56.502115	2026-09-24 21:00:56.502115
824	821	Xám - đồng	TGDD-371219-V1	12990000.00	\N	0	Xám - đồng	\N	\N	2026-09-24 21:00:56.518765	2026-09-24 21:00:56.518765
825	822	Xám - đồng	TGDD-371221-V1	14490000.00	\N	0	Xám - đồng	\N	\N	2026-09-24 21:00:56.53587	2026-09-24 21:00:56.53587
826	823	Vàng	TGDD-371223-V1	20990000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.552906	2026-09-24 21:00:56.552906
827	824	Vàng	TGDD-371225-V1	22490000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.569241	2026-09-24 21:00:56.569241
828	825	Xám - đồng	TGDD-371226-V1	15990000.00	\N	0	Xám - đồng	\N	\N	2026-09-24 21:00:56.585805	2026-09-24 21:00:56.585805
829	826	Vàng	TGDD-371228-V1	22490000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.60192	2026-09-24 21:00:56.60192
830	827	Vàng	TGDD-371229-V1	23990000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.617507	2026-09-24 21:00:56.617507
831	828	Trắng	TGDD-371230-V1	26790000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:56.634647	2026-09-24 21:00:56.634647
832	829	Trắng	TGDD-371231-V1	28290000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:56.652389	2026-09-24 21:00:56.652389
833	830	Titan tự nhiên	TGDD-371235-V1	26990000.00	\N	0	Titan tự nhiên	\N	\N	2026-09-24 21:00:56.669667	2026-09-24 21:00:56.669667
834	831	Ghi đen	TGDD-371332-V1	6990000.00	\N	0	Ghi đen	\N	\N	2026-09-24 21:00:56.686943	2026-09-24 21:00:56.686943
835	832	Ghi đen	TGDD-371333-V1	8490000.00	\N	0	Ghi đen	\N	\N	2026-09-24 21:00:56.703462	2026-09-24 21:00:56.703462
836	833	Vàng đồng	TGDD-371335-V1	7850000.00	\N	0	Vàng đồng	\N	\N	2026-09-24 21:00:56.719333	2026-09-24 21:00:56.719333
837	834	Vàng đồng	TGDD-371336-V1	9350000.00	\N	0	Vàng đồng	\N	\N	2026-09-24 21:00:56.736011	2026-09-24 21:00:56.736011
838	835	Bạc	TGDD-364640-V1	12625000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.752594	2026-09-24 21:00:56.752594
839	836	Vàng	TGDD-326940-V1	2075000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:56.768943	2026-09-24 21:00:56.768943
840	837	Bạc	TGDD-364641-V1	11925000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.78503	2026-09-24 21:00:56.78503
841	838	Bạc	TGDD-364642-V1	17815000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.801553	2026-09-24 21:00:56.801553
842	839	Bạc	TGDD-364643-V1	18715000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.817917	2026-09-24 21:00:56.817917
843	840	Bạc	TGDD-364646-V1	24655000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.833253	2026-09-24 21:00:56.833253
844	841	Nâu	TGDD-330392-V1	15555000.00	\N	0	Nâu	\N	\N	2026-09-24 21:00:56.848908	2026-09-24 21:00:56.848908
845	842	Nâu	TGDD-331125-V1	7230000.00	\N	0	Nâu	\N	\N	2026-09-24 21:00:56.863778	2026-09-24 21:00:56.863778
846	843	Bạc	TGDD-364639-V1	12625000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.878838	2026-09-24 21:00:56.878838
847	844	Bạc	TGDD-364638-V1	13190000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.894607	2026-09-24 21:00:56.894607
848	845	Đen	TGDD-283074-V1	11320000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:56.910495	2026-09-24 21:00:56.910495
849	846	Màu kết hợp	TGDD-367832-V1	890000.00	\N	0	Màu kết hợp	\N	\N	2026-09-24 21:00:56.92548	2026-09-24 21:00:56.92548
850	847	Bạc	TGDD-367833-V1	690000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.941171	2026-09-24 21:00:56.941171
851	848	Bạc	TGDD-367831-V1	890000.00	\N	0	Bạc	\N	\N	2026-09-24 21:00:56.957269	2026-09-24 21:00:56.957269
852	849	Màu kết hợp	TGDD-367830-V1	890000.00	\N	0	Màu kết hợp	\N	\N	2026-09-24 21:00:56.97401	2026-09-24 21:00:56.97401
853	850	Trắng	TGDD-199205-V1	2655000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:56.99063	2026-09-24 21:00:56.99063
854	851	Màu kết hợp	TGDD-360575-V1	2240000.00	\N	0	Màu kết hợp	\N	\N	2026-09-24 21:00:57.007647	2026-09-24 21:00:57.007647
855	852	Màu kết hợp	TGDD-360576-V1	2115000.00	\N	0	Màu kết hợp	\N	\N	2026-09-24 21:00:57.024386	2026-09-24 21:00:57.024386
856	853	Màu kết hợp	TGDD-360581-V1	2240000.00	\N	0	Màu kết hợp	\N	\N	2026-09-24 21:00:57.040566	2026-09-24 21:00:57.040566
857	854	Vàng	TGDD-360580-V1	2115000.00	\N	0	Vàng	\N	\N	2026-09-24 21:00:57.057126	2026-09-24 21:00:57.057126
858	855	Trắng	TGDD-370179-V1	369000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.073397	2026-09-24 21:00:57.073397
859	856	Đen	TGDD-337969-V1	250000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.090546	2026-09-24 21:00:57.091058
860	857	Đen	TGDD-367810-V1	1590000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.106894	2026-09-24 21:00:57.106894
861	858	Trắng	TGDD-369653-V1	370000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.123682	2026-09-24 21:00:57.123682
862	859	Xám	TGDD-361088-V1	305000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:57.139467	2026-09-24 21:00:57.139467
863	860	Đen	TGDD-368720-V1	760000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.154489	2026-09-24 21:00:57.154489
864	861	Đen	TGDD-364553-V1	918000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.169111	2026-09-24 21:00:57.169111
865	862	Màu be	TGDD-366973-V1	150000.00	\N	0	Màu be	\N	\N	2026-09-24 21:00:57.183861	2026-09-24 21:00:57.183861
866	863	Trắng	TGDD-370235-V1	279000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.199003	2026-09-24 21:00:57.199003
867	864	Trắng	TGDD-370101-V1	530000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.214043	2026-09-24 21:00:57.214043
868	865	Xám	TGDD-361261-V1	165000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:57.228851	2026-09-24 21:00:57.228851
869	866	Trắng	TGDD-369599-V1	460000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.245238	2026-09-24 21:00:57.245238
870	867	Đen	TGDD-360665-V1	180000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.260186	2026-09-24 21:00:57.260186
871	868	Hồng	TGDD-370281-V1	2890000.00	\N	0	Hồng	\N	\N	2026-09-24 21:00:57.275528	2026-09-24 21:00:57.275528
872	869	Đen	TGDD-363760-V1	668000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.29077	2026-09-24 21:00:57.29077
873	870	Xanh Dương	TGDD-368617-V1	895000.00	\N	0	Xanh Dương	\N	\N	2026-09-24 21:00:57.306305	2026-09-24 21:00:57.306305
874	871	Trắng	TGDD-363852-V1	197000.00	\N	0	Trắng	\N	\N	2026-09-24 21:00:57.321346	2026-09-24 21:00:57.321346
875	872	Xám	TGDD-332248-V1	240000.00	\N	0	Xám	\N	\N	2026-09-24 21:00:57.336501	2026-09-24 21:00:57.336501
876	873	Đen	TGDD-368715-V1	415000.00	\N	0	Đen	\N	\N	2026-09-24 21:00:57.351737	2026-09-24 21:00:57.351737
877	874	Xanh Đậm	TGDD-360829-V1	980000.00	\N	0	Xanh Đậm	\N	\N	2026-09-24 21:00:57.367342	2026-09-24 21:00:57.367342
\.


ALTER TABLE public.product_variants ENABLE TRIGGER ALL;

--
-- Data for Name: variant_attribute_values; Type: TABLE DATA; Schema: public; Owner: -
--

ALTER TABLE public.variant_attribute_values DISABLE TRIGGER ALL;

COPY public.variant_attribute_values (variant_id, value_id) FROM stdin;
1	13
1	83
2	13
2	83
3	34
3	83
4	24
4	74
4	77
5	4
5	83
6	47
6	76
6	77
7	24
7	68
7	83
8	24
8	68
8	83
9	56
9	76
9	83
10	33
10	68
10	83
11	59
11	83
12	53
12	76
12	77
13	33
13	76
13	77
14	4
14	83
15	33
15	68
15	83
16	24
16	68
16	83
17	60
17	77
18	56
18	69
18	85
19	33
19	76
19	77
20	33
20	75
20	77
21	24
21	68
21	83
22	39
22	75
22	77
23	24
23	76
23	83
24	47
24	74
24	77
25	12
25	68
25	83
26	44
26	74
26	77
27	58
27	76
27	77
28	55
28	77
29	43
29	83
30	53
30	68
30	83
31	26
31	76
31	83
32	15
32	76
32	83
33	24
33	76
33	77
34	39
34	76
34	83
35	53
35	74
35	77
36	48
36	76
36	77
37	16
37	83
38	24
38	75
38	77
39	28
39	74
39	86
40	5
40	68
40	83
41	5
41	69
41	85
42	33
42	76
42	83
43	5
43	69
43	85
44	44
44	76
44	83
45	60
45	77
46	33
46	77
47	56
47	68
47	83
48	53
48	75
48	77
49	56
49	68
49	83
50	55
50	76
50	77
51	53
51	74
51	86
52	44
52	76
52	77
53	31
53	76
53	83
54	24
54	76
54	77
55	24
55	76
55	77
56	5
56	68
56	83
57	47
57	76
57	77
58	5
58	75
58	77
59	56
59	74
59	77
60	44
60	74
60	86
61	2
61	68
61	83
62	55
62	76
62	77
63	15
63	76
63	83
64	5
64	76
64	83
65	32
65	76
65	83
66	1
66	68
66	83
67	1
67	76
67	83
68	66
68	76
68	77
69	63
69	76
69	83
70	53
70	76
70	83
71	5
71	68
71	83
72	5
72	68
72	83
73	53
73	76
73	83
74	44
74	68
74	83
75	53
75	76
75	77
76	47
76	74
76	86
77	39
77	76
77	83
78	5
78	68
78	83
79	24
79	76
79	77
80	5
80	74
80	86
81	44
81	74
81	77
82	5
83	56
83	74
83	77
84	39
84	74
84	86
85	5
86	5
86	75
86	77
87	56
88	5
88	74
88	86
89	15
90	53
91	2
92	5
93	2
93	74
93	86
94	22
94	74
94	86
95	5
95	74
95	86
96	47
96	74
96	86
97	47
98	5
98	74
98	77
99	53
99	72
99	86
100	5
100	74
100	77
101	24
101	74
101	77
102	39
103	56
103	72
103	86
104	53
104	68
104	83
105	62
105	75
105	77
106	39
106	74
106	77
107	74
107	86
108	2
108	74
108	77
109	39
110	53
111	5
111	68
111	83
112	56
112	74
112	77
113	41
113	76
113	83
114	1
114	75
114	77
115	5
115	74
115	86
116	56
117	53
118	53
118	68
118	83
119	53
119	76
119	83
120	5
120	75
120	77
121	5
121	68
121	83
122	56
122	76
122	83
123	24
123	75
123	77
124	39
124	76
124	77
125	52
125	75
125	77
126	3
126	68
126	85
127	39
127	68
127	83
128	12
129	24
129	75
129	77
130	33
130	76
130	77
131	44
131	74
131	86
132	56
133	55
133	68
133	83
134	47
135	5
135	69
135	85
136	5
137	56
137	68
137	83
138	44
139	64
139	68
139	83
140	56
140	75
140	77
141	15
141	68
141	83
142	44
142	68
142	85
143	56
143	76
143	83
144	1
144	68
144	85
145	24
145	68
145	83
146	15
146	68
146	83
147	53
147	68
147	83
148	24
148	68
148	85
149	15
149	68
149	83
150	2
150	68
150	83
151	5
151	76
151	83
152	15
152	75
152	77
153	2
153	76
153	83
154	44
154	68
154	83
155	33
155	76
155	77
156	53
156	68
156	85
157	56
157	68
157	85
158	12
158	68
158	83
159	15
159	68
159	83
160	23
160	68
160	85
161	24
161	76
161	77
162	7
163	15
163	76
163	83
164	32
164	69
164	85
165	5
165	74
165	86
166	37
166	68
166	85
167	44
167	68
167	83
168	5
168	68
168	83
169	53
169	68
169	85
170	44
170	74
170	77
171	53
171	76
171	77
172	47
172	75
172	77
173	17
173	75
173	83
174	76
174	77
175	75
175	83
176	76
176	83
177	68
177	85
178	68
178	85
179	68
179	83
180	39
180	79
181	53
181	76
181	83
182	1
182	79
183	1
183	79
184	44
184	79
185	1
185	79
186	1
186	79
187	51
187	69
187	85
188	44
188	79
189	1
189	79
190	1
190	87
191	1
191	87
192	1
192	79
193	1
193	87
194	1
194	79
195	44
195	79
196	1
196	87
197	52
197	69
197	85
198	1
198	79
199	1
199	79
200	1
200	79
201	5
201	79
202	1
202	87
203	1
203	79
204	5
204	79
205	44
205	79
206	1
206	79
207	44
207	87
208	1
208	79
209	1
209	79
210	5
210	79
211	1
211	79
212	39
212	82
213	44
213	79
214	43
214	79
215	1
215	79
216	5
216	79
217	1
217	73
217	81
218	5
218	84
219	49
219	79
220	1
220	79
221	5
221	79
222	44
222	87
223	1
223	79
224	1
224	79
225	1
225	79
226	44
226	79
227	1
227	79
228	5
228	79
229	44
229	79
230	1
230	79
231	1
231	87
232	1
232	79
233	44
233	79
234	5
234	79
235	44
235	79
236	5
236	79
237	5
237	79
238	1
238	79
239	1
239	79
240	1
240	87
241	1
241	79
242	1
242	82
243	44
243	79
244	1
244	79
245	1
245	87
246	44
246	82
247	1
247	79
248	5
248	79
249	1
249	79
250	5
250	79
251	1
251	79
252	1
252	79
253	44
253	79
254	1
254	79
255	5
255	79
256	39
256	79
257	39
257	79
258	1
258	79
259	44
259	79
260	1
260	79
261	1
261	79
262	1
262	79
263	5
263	79
264	44
264	79
265	44
265	79
266	5
266	79
267	5
267	79
268	5
268	79
269	1
269	79
270	1
270	82
271	1
271	79
272	1
272	87
273	5
273	79
274	5
274	79
275	44
275	79
276	44
276	87
277	1
277	79
278	1
278	79
279	5
279	79
280	44
280	79
281	1
281	79
282	1
282	87
283	39
283	79
284	5
284	79
285	1
285	79
286	47
286	79
287	1
287	79
288	44
288	79
289	5
289	87
290	1
290	79
291	1
291	79
292	5
292	79
293	1
293	79
294	5
294	79
295	1
295	84
296	39
296	79
297	5
297	79
298	44
298	79
299	53
299	79
300	44
300	79
301	1
301	79
302	1
302	79
303	44
303	79
304	39
304	79
305	44
305	79
306	5
306	79
307	1
307	79
308	1
308	79
309	53
309	84
310	1
310	79
311	1
311	79
312	5
312	70
312	81
313	1
313	79
314	5
314	79
315	5
315	79
316	1
316	79
317	1
317	87
318	5
318	87
319	1
319	79
320	33
320	79
321	5
321	79
322	53
322	79
323	1
323	79
324	44
324	84
325	1
325	79
326	1
326	79
327	44
327	79
328	39
328	82
329	1
329	79
330	1
330	79
331	44
331	79
332	1
332	79
333	39
333	79
334	5
334	84
335	5
335	84
336	5
336	79
337	44
337	79
338	49
338	79
339	1
339	79
340	1
340	79
341	44
341	84
342	5
342	79
343	1
343	79
344	5
344	79
345	5
345	69
345	81
346	1
346	79
347	1
347	87
348	5
348	79
349	53
349	79
350	44
350	79
351	5
351	79
352	1
352	84
353	53
353	79
354	44
354	79
355	39
355	79
356	33
356	79
357	1
357	79
358	44
358	79
359	1
359	79
360	44
360	79
361	5
361	79
362	1
362	79
363	47
363	84
364	1
364	79
365	33
365	84
366	44
366	79
367	1
367	87
368	1
368	79
369	1
369	79
370	1
370	79
371	44
371	79
372	53
372	79
373	1
373	79
374	5
374	79
375	5
375	84
376	1
376	79
377	44
377	79
378	5
378	79
379	53
379	79
380	1
380	79
381	44
381	79
382	1
382	79
383	1
383	79
384	44
384	79
385	50
385	79
386	53
386	79
387	5
387	79
388	44
388	84
389	44
389	79
390	47
390	79
391	1
391	79
392	44
392	79
393	44
393	79
394	5
394	79
395	1
395	79
396	44
396	79
397	1
397	84
398	44
398	79
399	1
399	79
400	1
400	79
401	19
401	79
402	1
402	84
403	44
403	79
404	44
404	79
405	5
405	79
406	1
406	79
407	53
407	79
408	1
408	79
409	1
409	79
410	5
410	79
411	5
411	84
412	1
412	79
413	1
413	79
414	5
414	79
415	1
415	87
416	1
416	79
417	1
417	79
418	5
418	84
419	1
419	79
420	47
420	84
421	5
421	84
422	44
422	79
423	1
423	79
424	44
424	87
425	44
425	79
426	44
426	79
427	1
427	87
428	1
428	79
429	5
429	84
430	5
430	79
431	5
431	84
432	44
432	79
433	1
433	79
434	5
434	79
435	44
435	79
436	1
436	79
437	1
437	84
438	44
438	79
439	1
439	79
440	53
440	79
441	5
441	79
442	44
442	79
443	1
443	79
444	1
444	79
445	44
445	79
446	1
446	79
447	1
447	82
448	44
448	79
449	1
449	84
450	5
450	79
451	5
451	84
452	44
452	79
453	47
453	84
454	1
454	84
455	5
455	84
456	44
456	79
457	47
457	84
458	5
458	79
459	39
459	79
460	1
460	84
461	1
461	84
462	44
462	84
463	5
463	79
464	44
464	79
465	1
465	79
466	44
466	84
467	5
467	79
468	44
468	84
469	1
469	87
470	1
470	79
471	1
471	79
472	1
472	87
473	44
473	87
474	5
474	79
475	1
475	87
476	1
476	79
477	1
477	87
478	1
478	87
479	44
479	79
480	1
480	87
481	1
481	79
482	5
482	79
483	44
483	79
484	53
484	84
485	1
485	79
486	1
486	87
487	51
487	70
487	85
488	1
488	79
489	5
489	87
490	51
490	70
490	85
491	44
491	79
492	44
492	79
493	44
493	79
494	1
494	79
495	5
495	70
495	81
496	1
496	79
497	1
497	79
498	44
498	79
499	1
499	79
500	44
500	79
501	1
501	79
502	5
502	79
503	5
503	79
504	1
504	79
505	5
505	87
506	44
506	79
507	1
507	79
508	1
508	87
509	44
509	84
510	5
510	79
511	44
511	79
512	1
512	79
513	50
513	79
514	5
514	79
515	44
515	82
516	44
516	79
517	5
517	79
518	1
518	79
519	44
519	87
520	1
520	79
521	5
521	84
522	44
522	79
523	39
523	71
523	85
524	5
524	79
525	44
525	85
526	44
526	79
527	44
527	79
528	35
528	84
529	44
529	79
530	44
530	84
531	1
531	79
532	50
532	84
533	5
533	79
534	39
534	77
535	44
535	79
536	1
536	79
537	44
537	84
538	44
538	79
539	5
539	79
540	1
540	84
541	44
541	79
542	44
542	79
543	44
543	84
544	39
544	84
545	5
545	79
546	44
546	84
547	53
547	79
548	5
548	79
549	5
549	79
550	35
550	79
551	33
551	79
552	5
552	80
553	5
553	86
554	5
554	79
555	44
555	79
556	5
556	79
557	44
557	84
558	1
558	79
559	1
559	84
560	1
560	84
561	44
561	86
562	5
562	84
563	5
563	84
564	5
564	84
565	5
565	79
566	44
566	79
567	5
567	86
568	44
568	79
569	33
569	79
570	44
570	84
571	19
571	79
572	44
572	84
573	44
573	84
574	44
574	79
575	5
575	79
576	5
576	79
577	5
577	79
578	5
578	79
579	44
579	86
580	44
580	79
581	44
581	79
582	5
582	79
583	5
583	79
584	1
584	79
585	5
585	86
586	5
586	79
587	5
587	79
588	53
588	84
589	5
589	79
590	5
590	79
591	44
591	79
592	35
592	84
593	44
593	79
594	44
594	79
595	56
595	78
596	56
596	78
597	44
597	79
598	39
598	79
599	1
599	79
600	1
600	79
601	1
601	79
602	44
602	84
603	39
603	84
604	1
604	79
605	1
605	79
606	1
606	79
607	1
607	79
608	47
608	84
609	1
609	79
610	5
610	79
611	39
611	77
612	11
612	77
613	15
613	76
613	83
614	57
614	76
614	77
615	44
615	75
615	77
616	44
616	75
616	77
617	57
617	76
617	77
618	1
618	74
618	77
619	24
619	77
620	5
620	83
621	44
621	76
621	77
622	44
622	75
622	77
623	44
623	74
623	86
624	44
624	74
624	86
625	44
625	68
625	77
626	53
626	76
626	77
627	44
627	74
627	77
628	1
628	77
629	1
629	75
629	77
630	1
630	76
630	83
631	44
631	76
631	77
632	44
632	68
632	83
633	53
633	76
633	77
634	56
634	74
634	77
635	1
635	75
635	77
636	1
636	74
636	77
637	44
637	74
637	86
638	1
638	76
638	83
639	1
639	76
639	77
640	1
640	74
640	86
641	1
641	74
641	86
642	44
642	68
642	77
643	44
643	68
643	83
644	11
644	77
645	33
645	84
646	44
646	76
646	83
647	44
647	74
647	77
648	1
648	83
649	5
649	79
650	24
650	77
651	24
651	77
652	5
652	83
653	5
653	83
654	5
654	68
654	83
655	44
655	68
655	83
656	33
656	84
657	5
657	68
657	83
658	44
658	76
658	83
659	5
659	77
660	44
660	76
660	83
661	44
661	75
661	77
662	44
662	76
662	83
663	5
664	67
665	15
666	2
667	20
668	5
669	36
670	56
671	5
672	6
673	33
674	33
675	5
676	65
677	5
678	56
679	36
680	33
681	25
682	30
683	5
684	33
685	5
686	56
687	27
688	41
689	5
690	33
691	5
692	5
693	12
694	22
695	1
696	22
697	56
698	67
699	33
700	1
701	33
702	22
703	33
704	5
705	5
706	39
707	6
708	12
709	5
710	5
711	5
712	33
713	5
714	31
715	53
716	5
717	1
718	44
719	5
720	44
721	5
722	18
723	53
724	5
725	5
726	33
727	5
728	5
729	5
730	15
731	54
732	36
733	5
734	44
735	38
736	33
737	5
738	33
739	5
740	5
741	65
742	5
743	5
744	5
745	5
746	39
747	24
748	61
749	33
750	27
751	40
752	5
753	5
754	10
755	5
756	27
757	5
758	5
759	5
760	33
761	56
762	16
763	54
764	53
765	9
766	15
767	5
768	5
769	1
770	5
771	53
772	53
773	54
774	53
775	15
776	5
777	15
778	53
779	5
780	5
781	5
782	15
783	15
784	15
785	53
786	63
787	5
788	1
789	5
790	15
791	15
792	5
793	5
794	56
795	53
796	53
797	5
798	5
799	2
800	5
801	46
802	24
803	44
804	1
805	5
806	5
807	44
808	8
809	46
810	24
811	5
812	33
813	5
814	37
816	5
817	5
818	5
819	1
820	39
821	2
822	33
823	45
824	45
825	45
826	39
827	39
828	45
829	39
830	39
831	33
832	33
833	29
834	14
835	14
836	40
837	40
838	1
839	39
840	1
841	1
842	1
843	1
844	22
845	22
846	1
847	1
848	5
849	21
850	1
851	1
852	21
853	33
854	21
855	21
856	21
857	39
858	33
859	5
860	5
861	33
862	44
863	5
864	5
865	19
866	33
867	33
868	44
869	33
870	5
871	15
872	5
873	53
874	33
875	44
876	5
877	49
107	42
815	42
\.


ALTER TABLE public.variant_attribute_values ENABLE TRIGGER ALL;

--
-- Name: attribute_values_value_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.attribute_values_value_id_seq', 91, true);


--
-- Name: attributes_attribute_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.attributes_attribute_id_seq', 5, true);


--
-- Name: brands_brand_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.brands_brand_id_seq', 56, true);


--
-- Name: categories_category_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.categories_category_id_seq', 11, true);


--
-- Name: product_images_image_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_images_image_id_seq', 765, true);


--
-- Name: product_specifications_specification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_specifications_specification_id_seq', 5, true);


--
-- Name: product_variants_variant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.product_variants_variant_id_seq', 880, true);


--
-- Name: products_product_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.products_product_id_seq', 879, true);


--
-- PostgreSQL database dump complete
--

\unrestrict kFDbZwiYJcdJhcE1piSXDkdOaIDjLzzTwdZx0XLIOHXacLPA56CEWN14eNf58qV
