package com.example.demo.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SupabaseStorageService;
import com.example.demo.service.SupabaseStorageService.StorageRoot;

@RestController
public class StaticAssetController {
    // This controller is a fallback for serving static assets (images, articles) from Supabase storage.
    // It shouldn't be used for general file serving, but rather for specific cases where files are stored in Supabase and need to be accessed directly.
    // For example, if you have images stored in Supabase and want to serve them directly to the client, you can use this controller.
    // But the cost of serving files directly from Supabase might be higher than serving them from your own server or a CDN, so use this approach judiciously.
    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @GetMapping("/images/{path:.+}")
    public ResponseEntity<byte[]> serveImage(@PathVariable("path") String path) throws IOException {
        return serveFromSupabase(StorageRoot.IMAGES, path);
    }

    @GetMapping("/articles/{path:.+}")
    public ResponseEntity<byte[]> serveArticle(@PathVariable("path") String path) throws IOException {
        return serveFromSupabase(StorageRoot.ARTICLES, path);
    }

    private ResponseEntity<byte[]> serveFromSupabase(StorageRoot root, String path) throws IOException {
        byte[] content = supabaseStorageService.downloadBytes(root, path);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (path.toLowerCase().endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (path.toLowerCase().endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        } else if (path.toLowerCase().endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if (path.toLowerCase().endsWith(".svg")) {
            mediaType = MediaType.parseMediaType("image/svg+xml");
        } else if (path.toLowerCase().endsWith(".html") || path.toLowerCase().endsWith(".htm")) {
            mediaType = MediaType.TEXT_HTML;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(mediaType)
                .body(content);
    }
}
