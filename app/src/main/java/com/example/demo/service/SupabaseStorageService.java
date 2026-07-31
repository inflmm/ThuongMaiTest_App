package com.example.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Định nghĩa các root folder trong Supabase Storage để phân loại các loại dữ liệu khác nhau (ví dụ: bài viết, hình ảnh)
    public enum StorageRoot {
        ARTICLES("articles"),
        IMAGES("images");

        private final String folder;

        StorageRoot(String folder) {
            this.folder = folder;
        }

        public String getFolder() {
            return folder;
        }
    }
    // Hàm này sẽ upload một file MultipartFile lên Supabase Storage trong thư mục ARTICLES, với đường dẫn tương đối được chỉ định
    public String uploadToArticles(MultipartFile file, String relativePath) throws IOException {
        return uploadFile(StorageRoot.ARTICLES, file, relativePath);
    }
    // Hàm này sẽ upload một file MultipartFile lên Supabase Storage trong thư mục IMAGES, với đường dẫn tương đối được chỉ định
    public String uploadToImages(MultipartFile file, String relativePath) throws IOException {
        return uploadFile(StorageRoot.IMAGES, file, relativePath);
    }
    // Hàm này sẽ upload một mảng byte lên Supabase Storage trong thư mục ARTICLES, với đường dẫn tương đối được chỉ định và loại nội dung (content type) được cung cấp
    public String uploadBytes(StorageRoot root, String relativePath, byte[] bytes, String contentType) throws IOException {
        String objectPath = buildObjectPath(root, relativePath, null);
        String uploadApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);
        headers.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : "application/octet-stream"
        ));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(bytes, headers);
        ResponseEntity<String> response = restTemplate.exchange(uploadApiUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return buildPublicUrl(objectPath);
        }
        throw new IOException("Lỗi upload bytes lên Supabase: " + response.getBody());
    }
    // Hàm này sẽ tải nội dung của một file từ Supabase Storage dựa trên root folder và đường dẫn tương đối được cung cấp. Nội dung được trả về dưới dạng String.
    public String downloadFile(StorageRoot root, String relativePath) throws IOException {
        return new String(downloadBytes(root, relativePath), StandardCharsets.UTF_8);
    }

    public byte[] downloadBytes(StorageRoot root, String relativePath) throws IOException {
        String objectPath = buildObjectPath(root, relativePath, null);
        String downloadApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(downloadApiUrl, HttpMethod.GET, requestEntity, byte[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new IOException("Lỗi tải file từ Supabase: " + response.getStatusCode());
    }
    // Hàm này sẽ xóa một file hoặc thư mục từ Supabase Storage dựa trên root folder và đường dẫn tương đối được cung cấp. Nếu xóa thành công, trả về true; nếu không, ném ra IOException.
    public boolean deleteObject(StorageRoot root, String relativePath) throws IOException {
        String objectPath = buildObjectPath(root, relativePath, null);
        String deleteApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(deleteApiUrl, HttpMethod.DELETE, requestEntity, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }
    // Hàm này sẽ liệt kê các đối tượng (file hoặc thư mục) trong Supabase Storage dựa trên root folder và đường dẫn tương đối được cung cấp. Nếu foldersOnly là true, chỉ trả về các thư mục; nếu false, trả về cả file và thư mục.
    // Hàm đã được tối ưu sửa lỗi lấy thiếu thư mục và lọc sai ảnh/folder
    public List<String> listObjects(StorageRoot root, String prefix, boolean foldersOnly) throws IOException {
        String normalizedPrefix = normalizePath(prefix);
        String storagePrefix = root.getFolder();
        if (!normalizedPrefix.isEmpty()) {
            storagePrefix += "/" + normalizedPrefix;
        }

        String listApiUrl = String.format("%s/storage/v1/object/list/%s", supabaseUrl, bucketName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prefix", storagePrefix);
        body.put("limit", 1000); // Lưu ý: Nếu > 1000 items cần làm vòng lặp offset
        body.put("sortBy", Map.of("column", "name", "order", "asc"));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(listApiUrl, HttpMethod.POST, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Supabase list API failed: " + response.getBody());
        }

        List<Map<String, Object>> objects = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
        Set<String> result = new LinkedHashSet<>();

        for (Map<String, Object> object : objects) {
            String name = (String) object.get("name");
            if (name == null || name.isBlank()) continue;

            // Kiểm tra xem đối tượng này là Folder hay File dựa trên dữ liệu của Supabase:
            // 1. Supabase trả về id = null cho Virtual Folder
            // 2. Hoặc metadata = null cho Folder
            boolean isFolder = (object.get("id") == null) || (object.get("metadata") == null);

            // Xử lý trường hợp Placeholder cho thư mục rỗng
            if (name.endsWith(".emptyFolderPlaceholder")) {
                // Nếu là folder rỗng, tên folder chính là phần prefix cha của placeholder này
                continue; 
            }

            // Lọc theo yêu cầu người dùng
            if (foldersOnly) {
                if (isFolder) {
                    result.add(name);
                }
            } else {
                // Lấy File (Không lấy Folder)
                if (!isFolder) {
                    result.add(name);
                }
            }
        }

        return new ArrayList<>(result);
    }
    // Hàm này sẽ tạo một thư mục trong Supabase Storage dựa trên root folder và đường dẫn tương đối được cung cấp. Nếu tạo thành công, không trả về gì; nếu không, ném ra IOException.
    public void createFolder(StorageRoot root, String folderPath) throws IOException {
        String normalizedPath = normalizePath(folderPath);
        if (normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("Folder path cannot be empty");
        }

        String objectPath = String.format("%s/%s/", root.getFolder(), normalizedPath);
        String uploadApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(new byte[0], headers);
        ResponseEntity<String> response = restTemplate.exchange(uploadApiUrl, HttpMethod.POST, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Supabase create-folder failed: " + response.getBody());
        }
    }
    // Hàm này sẽ xóa một thư mục trong Supabase Storage dựa trên root folder và đường dẫn tương đối được cung cấp. Nếu thư mục không rỗng, ném ra IllegalStateException; nếu xóa thành công, không trả về gì; nếu không, ném ra IOException.
    public void deleteFolder(StorageRoot root, String folderPath) throws IOException {
        String normalizedPath = normalizePath(folderPath);
        if (normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("Folder path cannot be empty");
        }

        String prefix = String.format("%s/%s/", root.getFolder(), normalizedPath);
        List<String> objects = listObjects(root, normalizedPath, false);
        if (!objects.isEmpty()) {
            throw new IllegalStateException("Folder is not empty");
        }

        String objectPath = prefix;
        String deleteApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(deleteApiUrl, HttpMethod.DELETE, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Supabase delete-folder failed: " + response.getBody());
        }
    }
    // Hàm này sẽ upload một file MultipartFile lên Supabase Storage trong thư mục được chỉ định bởi root, với đường dẫn tương đối được chỉ định. Trả về URL công khai của file đã upload.
    public String uploadFile(StorageRoot root, MultipartFile file, String relativePath) throws IOException {
        String fileName = getFileName(file, relativePath);
        String objectPath = buildObjectPath(root, relativePath, fileName);

        String uploadApiUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, objectPath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);
        headers.setContentType(MediaType.parseMediaType(
            file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
            uploadApiUrl,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return buildPublicUrl(objectPath);
        } else {
            throw new RuntimeException("Lỗi upload file lên Supabase: " + response.getBody());
        }
    }
    // Hàm này sẽ lấy tên file từ MultipartFile và đường dẫn tương đối. Nếu đường dẫn tương đối không rỗng, nó sẽ sử dụng phần cuối của đường dẫn làm tên file; nếu không, nó sẽ tạo một UUID mới làm tên file, giữ nguyên phần mở rộng của file gốc.
    private String getFileName(MultipartFile file, String relativePath) {
        if (relativePath != null && !relativePath.isBlank()) {
            String normalized = normalizePath(relativePath);
            if (!normalized.endsWith("/")) {
                return normalized.substring(normalized.lastIndexOf('/') + 1);
            }
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString() + extension;
    }
    // Hàm này sẽ xây dựng đường dẫn đầy đủ cho một đối tượng trong Supabase Storage dựa trên root folder, đường dẫn tương đối và tên file. Nếu đường dẫn tương đối rỗng hoặc kết thúc bằng "/", nó sẽ thêm tên file vào cuối; nếu không, nó sẽ chỉ sử dụng đường dẫn tương đối. Kết quả là một đường dẫn chuẩn hóa, loại bỏ các dấu "/" thừa.
    private String buildObjectPath(StorageRoot root, String relativePath, String fileName) {
        String cleanedPath = normalizePath(relativePath);
        if (cleanedPath.isEmpty() || cleanedPath.endsWith("/")) {
            return String.join("/", root.getFolder(), cleanedPath, fileName).replaceAll("//+", "/");
        }
        return String.join("/", root.getFolder(), cleanedPath).replaceAll("//+", "/");
    }
    // Hàm này sẽ chuẩn hóa đường dẫn bằng cách loại bỏ các dấu "/" thừa, thay thế "\" bằng "/", và loại bỏ dấu "/" ở đầu. Nếu đường dẫn là null, trả về chuỗi rỗng.
    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String cleaned = path.replace('\\', '/').trim();
        while (cleaned.contains("//")) {
            cleaned = cleaned.replace("//", "/");
        }
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }
    // Hàm này sẽ xây dựng URL công khai cho một đối tượng trong Supabase Storage dựa trên đường dẫn đối tượng. Nó sử dụng URL Supabase, tên bucket và đường dẫn đối tượng để tạo ra URL đầy đủ.
    private String buildPublicUrl(String objectPath) {
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, objectPath);
    }
}