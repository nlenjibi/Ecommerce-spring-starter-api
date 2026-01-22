package com.smart_ecomernce_api.smart_ecomernce_api.common.utils;


import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility service for generating URL-friendly slugs
 * Follows industry standards (Amazon, Shopify, Jumia, Alibaba)
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    /**
     * Generate a URL-friendly slug from a string
     * Examples:
     * - "Samsung Galaxy S24 Ultra" → "samsung-galaxy-s24-ultra"
     * - "Men's Fashion & Accessories" → "mens-fashion-accessories"
     * - "Premium Coffee (250g)" → "premium-coffee-250g"
     *
     * @param input the input string
     * @return URL-friendly slug
     */
    public String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }

        String slug = input.trim().toLowerCase(Locale.ENGLISH);

        // Normalize unicode characters (é → e, ñ → n)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);

        // Replace whitespace with dashes
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Remove non-alphanumeric characters (except dashes)
        slug = NON_LATIN.matcher(slug).replaceAll("");

        // Replace multiple consecutive dashes with single dash
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");

        // Remove dashes from edges
        slug = EDGES_DASHES.matcher(slug).replaceAll("");

        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Generated slug is empty after processing");
        }

        return slug;
    }

    /**
     * Generate a unique slug with incremental suffix if needed
     * Examples:
     * - "samsung-galaxy-s24" → "samsung-galaxy-s24-1" (if exists)
     * - "iphone-15-pro" → "iphone-15-pro-2" (if -1 exists)
     *
     * @param baseSlug the base slug
     * @param counter the counter for uniqueness
     * @return unique slug with suffix
     */
    public String generateUniqueSlug(String baseSlug, int counter) {
        if (counter <= 0) {
            return baseSlug;
        }
        return baseSlug + "-" + counter;
    }

    /**
     * Generate slug from multiple parts
     * Example: generateSlug("Electronics", "Phones", "Samsung") → "electronics-phones-samsung"
     *
     * @param parts the parts to combine
     * @return combined slug
     */
    public String generateSlug(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("At least one part is required");
        }

        StringBuilder combined = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                if (combined.length() > 0) {
                    combined.append(" ");
                }
                combined.append(part.trim());
            }
        }

        return generateSlug(combined.toString());
    }

    /**
     * Validate if a string is a valid slug format
     *
     * @param slug the slug to validate
     * @return true if valid slug format
     */
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return false;
        }

        // Valid slug: lowercase, alphanumeric, dashes only, no leading/trailing dashes
        return slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    }

    /**
     * Truncate slug to maximum length while preserving word boundaries
     *
     * @param slug the slug to truncate
     * @param maxLength maximum length
     * @return truncated slug
     */
    public String truncateSlug(String slug, int maxLength) {
        if (slug == null || slug.length() <= maxLength) {
            return slug;
        }

        String truncated = slug.substring(0, maxLength);

        // Remove trailing incomplete word (everything after last dash)
        int lastDash = truncated.lastIndexOf('-');
        if (lastDash > 0) {
            truncated = truncated.substring(0, lastDash);
        }

        // Remove trailing dash if any
        return EDGES_DASHES.matcher(truncated).replaceAll("");
    }
}

