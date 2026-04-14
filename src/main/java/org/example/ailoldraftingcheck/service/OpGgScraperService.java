package org.example.ailoldraftingcheck.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Best-effort scraper for op.gg champion pages.
 *
 * IMPORTANT CAVEAT: op.gg's pages are heavily JavaScript-rendered. Pure HTML
 * scraping with Jsoup will only ever capture the static markup the server
 * sends - the dynamic counter widgets often render in the browser after JS
 * execution. So this scraper is BEST EFFORT: it returns whatever it can find,
 * and the caller (CoachController) treats an empty result as "fall back to
 * LLM-only reasoning". This is documented honestly in the UI via the
 * {@code dataSource} field on CoachResponse.
 *
 * Visual analogy: imagine taking a screenshot of a webpage before it finishes
 * loading - you get the skeleton, but the live numbers may not be there yet.
 *
 * To keep us off op.gg's rate-limit radar we cache results for
 * {@code app.opgg.cache-ttl-hours}.
 */
@Service
public class OpGgScraperService {

    private static final Logger logger = LoggerFactory.getLogger(OpGgScraperService.class);

    @Value("${app.opgg.scrape-enabled}")
    private boolean enabled;

    @Value("${app.opgg.cache-ttl-hours}")
    private int cacheTtlHours;

    private record CacheEntry(List<String> counters, Instant ts) {}

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Returns up to ~5 champion names that op.gg lists as counters to the given
     * champion in the given role. Empty list when scraping is disabled, the
     * page can't be fetched, or the page's structure has changed.
     */
    public List<String> fetchCounters(String championNameOrId, String role) {
        if (!enabled || championNameOrId == null) return List.of();

        String slug = toOpGgSlug(championNameOrId);
        String position = toOpGgPosition(role);
        String key = slug + "/" + position;

        CacheEntry hit = cache.get(key);
        if (hit != null && Duration.between(hit.ts(), Instant.now()).toHours() < cacheTtlHours) {
            return hit.counters();
        }

        String url = "https://op.gg/champions/" + slug + "/counters/" + position;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; AILolDraftingCheck/1.0; school project)")
                    .timeout(5_000)
                    .get();

            // op.gg's HTML structure changes often. We try a few selectors and
            // fall back to scanning every link that points at /champions/<name>.
            Set<String> found = new LinkedHashSet<>();
            for (Element e : doc.select("a[href^=/champions/]")) {
                String href = e.attr("href");
                // /champions/aatrox/counters/top  -> "aatrox"
                String[] parts = href.split("/");
                if (parts.length >= 3 && !parts[2].equalsIgnoreCase(slug)) {
                    found.add(parts[2]);
                    if (found.size() >= 5) break;
                }
            }

            List<String> result = new ArrayList<>(found);
            cache.put(key, new CacheEntry(result, Instant.now()));
            logger.info("op.gg scrape {}/{} -> {} counters", slug, position, result.size());
            return result;
        } catch (Exception e) {
            logger.warn("op.gg scrape failed for {}/{}: {}", slug, position, e.getMessage());
            cache.put(key, new CacheEntry(List.of(), Instant.now())); // negative-cache so we don't hammer
            return List.of();
        }
    }

    private static String toOpGgSlug(String name) {
        // op.gg uses lowercase, no special chars: "Kai'Sa" -> "kaisa", "Wukong" stays "wukong"
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String toOpGgPosition(String role) {
        if (role == null) return "top";
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "TOP" -> "top";
            case "JGL", "JUNGLE" -> "jungle";
            case "MID" -> "mid";
            case "ADC", "BOT", "BOTTOM" -> "adc";
            case "SUPP", "SUPPORT" -> "support";
            default -> "top";
        };
    }
}
