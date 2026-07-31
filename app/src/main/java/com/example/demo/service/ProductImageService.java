package com.example.demo.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductImageService {

    @Value("${supabase.url}")
    private String supabaseBaseUrl;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    public String resolvePublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String cleaned = storedPath.replace('\\', '/').trim();
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned;
        }

        String compactPath = cleaned.replaceAll("^/+", "");
        if (compactPath.isBlank()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(supabaseBaseUrl)
                .append("/storage/v1/object/public/")
                .append(encodeSegment(bucketName));

        for (String segment : compactPath.split("/")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            builder.append("/").append(encodeSegment(segment));
        }

        return builder.toString();
    }

    public List<String> resolvePublicUrls(List<String> storedPaths) {
        List<String> result = new ArrayList<>();
        if (storedPaths == null) {
            return result;
        }

        for (String path : storedPaths) {
            String resolved = resolvePublicUrl(path);
            if (resolved != null) {
                result.add(resolved);
            }
        }

        return result;
    }

    public String resolvePublicUrl(String storedPath, String variant) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String cleaned = storedPath.replace('\\', '/').trim();
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned;
        }

        String normalizedVariant = normalizeVariant(variant);
        if (normalizedVariant != null) {
            String variantPrefix = normalizedVariant + "/";
            if (!cleaned.contains("/")) {
                cleaned = variantPrefix + cleaned;
            } else if (!cleaned.contains("/" + normalizedVariant + "/")) {
                int lastSlash = cleaned.lastIndexOf('/');
                if (lastSlash >= 0) {
                    cleaned = cleaned.substring(0, lastSlash + 1) + variantPrefix + cleaned.substring(lastSlash + 1);
                }
            }
        }

        return resolvePublicUrl(cleaned);
    }

    public String buildObjectPath(String basePath, String variant, String fileName) {
        String normalizedVariant = normalizeVariant(variant);
        String normalizedBasePath = basePath == null ? "" : basePath.replace('\\', '/').trim().replaceAll("^/+", "");
        String normalizedFileName = fileName == null ? "" : fileName.trim();

        if (normalizedBasePath.isBlank()) {
            return normalizedVariant != null ? normalizedVariant + "/" + normalizedFileName : normalizedFileName;
        }

        if (normalizedVariant == null) {
            return normalizedBasePath + "/" + normalizedFileName;
        }

        return normalizedBasePath + "/" + normalizedVariant + "/" + normalizedFileName;
    }

    private String normalizeVariant(String variant) {
        if (variant == null || variant.isBlank()) {
            return null;
        }

        String normalized = variant.trim().toLowerCase();
        if (normalized.equals("large") || normalized.equals("medium") || normalized.equals("compact") || normalized.equals("thumbnail")) {
            return normalized;
        }
        return null;
    }

    private String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
