package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Every public endpoint the website uses, against real MySQL data (the 48-product demo catalogue). */
class PublicApiIT extends AbstractApiIT {

    private ResultActions getJson(String url) throws Exception {
        return mvc.perform(get(url));
    }

    private List<String> names(String url, String path) throws Exception {
        String body = getJson(url).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, path);
    }

    @Nested
    @DisplayName("GET /api/public/products – catalogue")
    class Catalogue {

        @Test
        @DisplayName("Default: 48 products, page 0 of 4, 12 per page, frontend JSON shape")
        void defaultPage() throws Exception {
            getJson("/api/public/products")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(48))
                    .andExpect(jsonPath("$.totalPages").value(4))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(12))
                    .andExpect(jsonPath("$.content", hasSize(12)))
                    .andExpect(jsonPath("$.content[0].slug").value("10cm-electric-sparklers"))
                    .andExpect(jsonPath("$.content[0].sku").value("SP-001"))
                    .andExpect(jsonPath("$.content[0].packSize").value("10 Pcs"))
                    .andExpect(jsonPath("$.content[0].unit").value("BOX"))
                    .andExpect(jsonPath("$.content[0].mrp").value(60.0))
                    .andExpect(jsonPath("$.content[0].price").value(45.0))
                    .andExpect(jsonPath("$.content[0].stockStatus").value("IN_STOCK"))
                    .andExpect(jsonPath("$.content[0].createdAt").value("2026-08-01"))
                    .andExpect(jsonPath("$.content[0].images", hasSize(0)))
                    .andExpect(jsonPath("$.content[0].isDemo").value(true))
                    .andExpect(jsonPath("$.content[0].category.slug").value("sparklers"))
                    .andExpect(jsonPath("$.content[0].brand.slug").value("megha-sparkles"));
        }

        @Test
        @DisplayName("Internal fields are never exposed")
        void noInternalFields() throws Exception {
            getJson("/api/public/products")
                    .andExpect(jsonPath("$.content[0].stockQuantity").doesNotExist())
                    .andExpect(jsonPath("$.content[0].reorderLevel").doesNotExist())
                    .andExpect(jsonPath("$.content[0].visibleOnWebsite").doesNotExist())
                    .andExpect(jsonPath("$.content[0].active").doesNotExist())
                    .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist());
        }

        @Test
        @DisplayName("Search by product, brand, category and multiple words")
        void search() throws Exception {
            getJson("/api/public/products?search=rocket").andExpect(jsonPath("$.totalElements").value(4));
            getJson("/api/public/products?search=minnal").andExpect(jsonPath("$.totalElements").value(4));
            getJson("/api/public/products?search=aerial").andExpect(jsonPath("$.totalElements").value(4));
            mvc.perform(get("/api/public/products").param("search", "gold sparklers"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].slug").value("50cm-gold-sparklers"));
            getJson("/api/public/products?search=ROCKET").andExpect(jsonPath("$.totalElements").value(4));
        }

        @Test
        @DisplayName("No match → empty page 1 of 1; LIKE wildcards are matched literally")
        void noMatch() throws Exception {
            getJson("/api/public/products?search=xyzzz")
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.content", hasSize(0)));
            // Unescaped, "%" or "_" would match every product; escaped they match none.
            mvc.perform(get("/api/public/products").param("search", "%")).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/api/public/products").param("search", "_")).andExpect(jsonPath("$.totalElements").value(0));
            mvc.perform(get("/api/public/products").param("search", "100%")).andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Category and brand filters (multi-select) and their combination")
        void filters() throws Exception {
            getJson("/api/public/products?categories=sparklers,rockets").andExpect(jsonPath("$.totalElements").value(8));
            getJson("/api/public/products?categories=sparklers&categories=rockets").andExpect(jsonPath("$.totalElements").value(8));
            getJson("/api/public/products?categories=sparklers,rockets&brands=minnal-pyro")
                    .andExpect(jsonPath("$.totalElements").value(2));
            getJson("/api/public/products?categories=rockets&brands=minnal-pyro&minPrice=200&maxPrice=300")
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].slug").value("colour-rocket"));
            getJson("/api/public/products?brands=aruvi-fireworks").andExpect(jsonPath("$.totalElements").value(7));
        }

        @Test
        @DisplayName("Price bounds are inclusive; inStock hides the 3 out-of-stock products")
        void priceAndStock() throws Exception {
            getJson("/api/public/products?minPrice=1000&size=24")
                    .andExpect(jsonPath("$.totalElements").value(6));
            getJson("/api/public/products?maxPrice=30").andExpect(jsonPath("$.totalElements").value(1));
            getJson("/api/public/products?inStock=true").andExpect(jsonPath("$.totalElements").value(45));
        }

        @Test
        @DisplayName("Sorting: name A–Z/Z–A, price low/high, newest")
        void sorting() throws Exception {
            List<String> az = names("/api/public/products?sort=name,asc&size=48", "$.content[*].name");
            assertThat(az).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
            List<String> za = names("/api/public/products?sort=name,desc&size=48", "$.content[*].name");
            assertThat(za).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER.reversed());
            getJson("/api/public/products?sort=price,asc").andExpect(jsonPath("$.content[0].price").value(30.0));
            getJson("/api/public/products?sort=price,desc").andExpect(jsonPath("$.content[0].price").value(3000.0));
            getJson("/api/public/products?sort=newest").andExpect(jsonPath("$.content[0].slug").value("premium-celebration-box"));
        }

        @Test
        @DisplayName("Pagination: page 3 = items 37–48; page beyond the end is empty")
        void pagination() throws Exception {
            getJson("/api/public/products?page=3").andExpect(jsonPath("$.content", hasSize(12))).andExpect(jsonPath("$.page").value(3));
            getJson("/api/public/products?page=1&size=24").andExpect(jsonPath("$.content", hasSize(24))).andExpect(jsonPath("$.totalPages").value(2));
            getJson("/api/public/products?page=99").andExpect(jsonPath("$.content", hasSize(0))).andExpect(jsonPath("$.totalElements").value(48));
            List<String> page0 = names("/api/public/products?page=0", "$.content[*].slug");
            List<String> page1 = names("/api/public/products?page=1", "$.content[*].slug");
            assertThat(page0).doesNotContainAnyElementsOf(page1);
        }

        @Test
        @DisplayName("Invalid input → 400 with the field name")
        void validation() throws Exception {
            getJson("/api/public/products?size=7").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
            getJson("/api/public/products?sort=bogus").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
            getJson("/api/public/products?page=-1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
            getJson("/api/public/products?minPrice=-5").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
            getJson("/api/public/products?minPrice=500&maxPrice=100").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
            mvc.perform(get("/api/public/products").param("categories", "Bad Slug"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("categories"));
            getJson("/api/public/products?search=" + "a".repeat(101)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("search"));
            getJson("/api/public/products?minPrice=abc").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
        }
    }

    @Nested
    @DisplayName("Featured, new arrivals, detail, related, Estimate List lookup")
    class ProductViews {

        @Test
        @DisplayName("Featured: 8 featured products")
        void featured() throws Exception {
            getJson("/api/public/products/featured")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(8)))
                    .andExpect(jsonPath("$[*].featured", everyItem(is(true))));
            getJson("/api/public/products/featured?limit=0").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("New arrivals: newest first")
        void newArrivals() throws Exception {
            getJson("/api/public/products/new-arrivals?limit=3")
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].slug").value("premium-celebration-box"))
                    .andExpect(jsonPath("$[1].slug").value("kids-delight-box"));
        }

        @Test
        @DisplayName("Detail: category description and safety notes; unknown slug → 404")
        void detail() throws Exception {
            getJson("/api/public/products/colour-rocket")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Colour Rocket"))
                    .andExpect(jsonPath("$.category.slug").value("rockets"))
                    .andExpect(jsonPath("$.category.description").isString())
                    .andExpect(jsonPath("$.safetyNotes", hasSize(3)))
                    .andExpect(jsonPath("$.brand.name").value("Minnal Pyro"));
            getJson("/api/public/products/not-a-real-product")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Product not found"));
        }

        @Test
        @DisplayName("Related: 4 products, never the product itself, same category first")
        void related() throws Exception {
            getJson("/api/public/products/colour-rocket/related")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[*].slug", not(hasItem("colour-rocket"))))
                    .andExpect(jsonPath("$[0].category.slug").value("rockets"))
                    .andExpect(jsonPath("$[2].category.slug").value("rockets"))
                    .andExpect(jsonPath("$[3].brand.slug").value("minnal-pyro"));
        }

        @Test
        @DisplayName("By slugs (Estimate List): order kept, unknown skipped, limit enforced")
        void bySlugs() throws Exception {
            getJson("/api/public/products/by-slugs?slugs=flower-pot-big,ghost-product,15cm-colour-sparklers")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].slug").value("flower-pot-big"))
                    .andExpect(jsonPath("$[1].slug").value("15cm-colour-sparklers"));
            getJson("/api/public/products/by-slugs").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
            String tooMany = String.join(",", java.util.Collections.nCopies(201, "a"));
            getJson("/api/public/products/by-slugs?slugs=" + tooMany).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Hidden product: excluded from lists, detail 404, skipped in by-slugs")
        void hiddenProduct() throws Exception {
            jdbc.update("UPDATE products SET visible_on_website = FALSE WHERE slug = 'colour-rocket'");
            try {
                getJson("/api/public/products").andExpect(jsonPath("$.totalElements").value(47));
                getJson("/api/public/products/colour-rocket").andExpect(status().isNotFound());
                getJson("/api/public/products/by-slugs?slugs=colour-rocket").andExpect(jsonPath("$", hasSize(0)));
                getJson("/api/public/categories/rockets").andExpect(jsonPath("$.productCount").value(3));
            } finally {
                jdbc.update("UPDATE products SET visible_on_website = TRUE WHERE slug = 'colour-rocket'");
            }
        }

        @Test
        @DisplayName("Inactive brand hides its products everywhere")
        void inactiveBrand() throws Exception {
            jdbc.update("UPDATE brands SET active = FALSE WHERE slug = 'minnal-pyro'");
            try {
                getJson("/api/public/products").andExpect(jsonPath("$.totalElements").value(44));
                getJson("/api/public/brands").andExpect(jsonPath("$", hasSize(7)));
                getJson("/api/public/brands/minnal-pyro").andExpect(status().isNotFound());
            } finally {
                jdbc.update("UPDATE brands SET active = TRUE WHERE slug = 'minnal-pyro'");
            }
        }
    }

    @Nested
    @DisplayName("Price list, categories, brands")
    class Lists {

        @Test
        @DisplayName("Price list: 12 categories in display order, 48 products")
        void priceList() throws Exception {
            String body = getJson("/api/public/price-list").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<String> groups = JsonPath.read(body, "$[*].category.slug");
            List<String> slugs = JsonPath.read(body, "$[*].products[*].slug");
            assertThat(groups).hasSize(12).startsWith("sparklers", "flower-pots").endsWith("gift-boxes");
            assertThat(slugs).hasSize(48).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Price range: ₹30 – ₹3,000")
        void priceRange() throws Exception {
            getJson("/api/public/price-range").andExpect(jsonPath("$.min").value(30.0)).andExpect(jsonPath("$.max").value(3000.0));
        }

        @Test
        @DisplayName("Categories: 12 in display order, 4 products each; detail with safety notes; unknown → 404")
        void categories() throws Exception {
            getJson("/api/public/categories")
                    .andExpect(jsonPath("$", hasSize(12)))
                    .andExpect(jsonPath("$[0].slug").value("sparklers"))
                    .andExpect(jsonPath("$[*].productCount", everyItem(is(4))))
                    .andExpect(jsonPath("$[0].isDemo").value(true));
            getJson("/api/public/categories/rockets")
                    .andExpect(jsonPath("$.name").value("Rockets"))
                    .andExpect(jsonPath("$.safetyNotes", hasSize(3)))
                    .andExpect(jsonPath("$.productCount").value(4));
            getJson("/api/public/categories/nope").andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Brands: 8, each once, with counts; brand detail; unknown → 404")
        void brands() throws Exception {
            String body = getJson("/api/public/brands").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<String> slugs = JsonPath.read(body, "$[*].slug");
            assertThat(slugs).hasSize(8).doesNotHaveDuplicates();
            getJson("/api/public/brands/aruvi-fireworks")
                    .andExpect(jsonPath("$.productCount").value(7))
                    .andExpect(jsonPath("$.isDemo").value(true));
            getJson("/api/public/brands/nope").andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Business information")
    class Business {

        @Test
        @DisplayName("Settings: frontend shape; unknown details null; demoMode true with demo data")
        void settings() throws Exception {
            getJson("/api/public/settings")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shopName").value("SIVA Crackers"))
                    .andExpect(jsonPath("$.address.area").value("Eachanari"))
                    .andExpect(jsonPath("$.address.city").value("Coimbatore"))
                    .andExpect(jsonPath("$.address.line1").value(nullValue()))
                    .andExpect(jsonPath("$.phone").value(nullValue()))
                    .andExpect(jsonPath("$.whatsappNumber").value(nullValue()))
                    .andExpect(jsonPath("$.email").value(nullValue()))
                    .andExpect(jsonPath("$.businessHours").value(nullValue()))
                    .andExpect(jsonPath("$.licenceDetails").value(nullValue()))
                    .andExpect(jsonPath("$.whatsappEnquiryEnabled").value(true))
                    .andExpect(jsonPath("$.mapIsApproximate").value(true))
                    .andExpect(jsonPath("$.announcement.active").value(true))
                    .andExpect(jsonPath("$.highlights", hasSize(4)))
                    .andExpect(jsonPath("$.highlights[0].title").value("Family-owned shop"))
                    .andExpect(jsonPath("$.safetyTips", hasSize(7)))
                    .andExpect(jsonPath("$.demoMode").value(true))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.createdAt").doesNotExist());
        }

        @Test
        @DisplayName("demoMode is false when no demo product is visible")
        void demoModeFollowsData() throws Exception {
            jdbc.update("UPDATE products SET active = FALSE WHERE is_demo = TRUE");
            try {
                getJson("/api/public/settings").andExpect(jsonPath("$.demoMode").value(false));
            } finally {
                jdbc.update("UPDATE products SET active = TRUE WHERE is_demo = TRUE");
            }
        }

        @Test
        @DisplayName("Contact details, safety tips and banners")
        void contactTipsBanners() throws Exception {
            getJson("/api/public/settings/contact")
                    .andExpect(jsonPath("$.address.state").value("Tamil Nadu"))
                    .andExpect(jsonPath("$.phone").value(nullValue()))
                    .andExpect(jsonPath("$.whatsappNotice").isString());
            getJson("/api/public/settings/safety-tips").andExpect(jsonPath("$", hasSize(7)));
            getJson("/api/public/banners")
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].title").value("Light up every celebration"))
                    .andExpect(jsonPath("$[0].ctaLink").value("/products"))
                    .andExpect(jsonPath("$[0].theme").value("gold"));
        }
    }

    @Nested
    @DisplayName("Security of the public API")
    class Security {

        @Test
        @DisplayName("Public endpoints are read-only: POST is not allowed without login")
        void readOnly() throws Exception {
            mvc.perform(post("/api/public/products").with(csrf())).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin and account areas still require login")
        void protectedAreas() throws Exception {
            mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        }
    }
}
