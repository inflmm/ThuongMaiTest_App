package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.ProductRepository;

import net.coobird.thumbnailator.Thumbnails;
@Service
public class ImageUploadService {

	private Path baseDir = Paths.get("C:/ecommerce-uploads/images/");
	
	@Autowired
    private ProductRepository productRepository;
	
	// Bộ ký tự phục vụ sinh mã ngẫu nhiên 8 ký tự
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * HÀM TRỢ GIÚP 1: Sinh chuỗi ngẫu nhiên gồm 24 ký tự
     */
    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * HÀM TRỢ GIÚP: Kiểm tra Slug và Tính toán số thứ tự ảnh kế tiếp dựa trên thư mục master
     */
    private int getNextImageIndex(String selectedFolder, String productSlug) throws IOException {
        // 1. KIỂM TRA RÀNG BUỘC: Xác thực slug sản phẩm có tồn tại trong hệ thống hay không
        boolean isProductExist = productRepository.existsBySlug(productSlug);
        if (!isProductExist) {
            throw new IllegalArgumentException("Lỗi: Mã Slug sản phẩm '" + productSlug + "' không tồn tại trong hệ thống!");
        }
        
        // Đoạn code giả định kiểm tra (bạn có thể thay thế bằng logic database thực tế của bạn)
        if (productSlug == null || productSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã Slug sản phẩm không được để trống!");
        }

        // Định tiến tới thư mục master theo cấu trúc đường dẫn mới
        // Đường dẫn: baseDir + selectedFolder (nếu có) + productSlug + master
        Path targetFolder = baseDir;
        if (selectedFolder != null && !selectedFolder.trim().isEmpty()) {
            targetFolder = targetFolder.resolve(selectedFolder);
        }
        Path masterDirPath = targetFolder.resolve(productSlug).resolve("master");
        
        if (!Files.exists(masterDirPath)) {
            return 1; // Thư mục trống hoàn toàn -> Thư tự bắt đầu từ 1
        }

        // Đếm số lượng file hợp lệ nằm trong thư mục master bắt đầu bằng chữ "prod_"
        long existingFilesCount = Files.list(masterDirPath)
                .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().startsWith("prod_"))
                .count();

        return (int) (existingFilesCount + 1);
    }

    /**
     * PHƯƠNG ÁN 1: Xử lý xuất chuỗi ảnh định dạng JPG (Thumbnailator)
     */
    public List<String> uploadAndProcessProductImagesJpg(MultipartFile[] files, String selectedFolder, String productSlug) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) return generatedFileNames;

        int currentImageIndex = getNextImageIndex(selectedFolder, productSlug);
        String[] subFolders = {"compact", "grande", "master"};

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String randomStr = generateRandomString();
            String savedMasterName = ""; // Dùng để lưu lại tên trả về danh sách

            for (String sub : subFolders) {
                // Định tuyến đường dẫn động: baseDir + selectedFolder + productSlug + subFolder
                Path targetPath = baseDir;
                if (selectedFolder != null && !selectedFolder.trim().isEmpty()) {
                    targetPath = targetPath.resolve(selectedFolder);
                }
                targetPath = targetPath.resolve(productSlug).resolve(sub);

                if (!Files.exists(targetPath)) {
                    Files.createDirectories(targetPath);
                }

                // Đặt tên theo chuẩn: prod_<thutu>_<8ky_tu>_<compact/grande/master>.jpg
                String targetFileName = String.format("prod_%d_%s_%s.jpg", currentImageIndex, randomStr, sub);
                File destinationFile = targetPath.resolve(targetFileName).toFile();

                // Thiết lập lại chuẩn kích thước theo yêu cầu mới
                int size = 2048; // master
                if (sub.equals("compact")) size = 160;
                else if (sub.equals("grande")) size = 600;

                Thumbnails.of(file.getInputStream())
                        .size(size, size)
                        .outputFormat("jpg")
                        .outputQuality(0.75)
                        .toFile(destinationFile);

                if (sub.equals("master")) {
                    savedMasterName = targetFileName;
                }
            }

            generatedFileNames.add(savedMasterName);
            currentImageIndex++;
        }

        return generatedFileNames;
    }

    /**
     * PHƯƠNG ÁN 2: Xử lý xuất chuỗi ảnh định dạng WEBP (CLI Tool Google cwebp)
     */
    public List<String> uploadAndProcessProductImagesWebp(MultipartFile[] files, String selectedFolder, String productSlug) throws IOException {
        List<String> generatedFileNames = new ArrayList<>();
        if (files == null || files.length == 0) return generatedFileNames;

        int currentImageIndex = getNextImageIndex(selectedFolder, productSlug);
        String[] subFolders = {"compact", "grande", "master"};

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String randomStr = generateRandomString();
            String savedMasterName = "";

            // Tạo file nhị phân thô tạm thời làm đầu vào cho lệnh CLI
            File tempOriginalFile = File.createTempFile("raw_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempOriginalFile);

            try {
                for (String sub : subFolders) {
                    Path targetPath = baseDir;
                    if (selectedFolder != null && !selectedFolder.trim().isEmpty()) {
                        targetPath = targetPath.resolve(selectedFolder);
                    }
                    targetPath = targetPath.resolve(productSlug).resolve(sub);

                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }

                    // Tên file chuẩn: prod_<thutu>_<8ky_tu>_<compact/grande/master>.webp
                    String targetFileName = String.format("prod_%d_%s_%s.webp", currentImageIndex, randomStr, sub);
                    File webpFile = targetPath.resolve(targetFileName).toFile();

                    int size = 2048; // master
                    if (sub.equals("compact")) size = 160;
                    else if (sub.equals("grande")) size = 600;

                    // Thực thi câu lệnh nén vuông (giữ tỷ lệ, resize khống chế chiều rộng tối đa)
                    String command = String.format("cwebp -q 85 -resize %d 0 \"%s\" -o \"%s\"", 
                            size, 
                            tempOriginalFile.getAbsolutePath(), 
                            webpFile.getAbsolutePath()
                    );

                    Process process = Runtime.getRuntime().exec(command);
                    int exitCode = process.waitFor();
                    
                    if (exitCode != 0) {
                        throw new IOException("LỖI HỆ THỐNG: Ứng dụng cwebp trả về lỗi hệ điều hành (Mã lỗi: " + exitCode + "). " +
                                "CẢNH BÁO: Vui lòng chắc chắn rằng bạn đã tải bộ công cụ 'libwebp' từ Google, giải nén và đưa đường dẫn chứa file cwebp.exe vào biến môi trường 'Path' của Windows!");
                    }

                    if (sub.equals("master")) {
                        savedMasterName = targetFileName;
                    }
                }

                generatedFileNames.add(savedMasterName);
                currentImageIndex++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Tiến trình CLI WebP bị ngắt quãng bất thường: " + e.getMessage());
            } finally {
                // Luôn luôn dọn dẹp file nháp tạm thời
                if (tempOriginalFile.exists()) {
                    tempOriginalFile.delete();
                }
            }
        }

        return generatedFileNames;
    }

    /**
     * Hàm upload một hoặc nhiều ảnh giữ nguyên dung lượng, kích thước và tên file gốc
     * @param files Mảng các file ảnh từ Client gửi lên
     * @param selectedFolder Đường dẫn thư mục được chọn (ví dụ: "banners" hoặc "blogs/tin-tuc")
     * @return Danh sách tên các file đã lưu thành công
     */
    public List<String> uploadRawImages(MultipartFile[] files, String selectedFolder) throws IOException {
        List<String> uploadedFileNames = new ArrayList<>();

        if (files == null || files.length == 0) {
            return uploadedFileNames;
        }

        // 1. Xác định chính xác đường dẫn thư mục đích dựa trên cấu trúc được chọn
        String targetDirPath = baseDir + "/" + selectedFolder + "/";
        File targetDir = new File(targetDirPath);

        if (!targetDir.exists()) {
        	throw new IOException("Thư mục lưu trữ không tồn tại. Vui lòng tạo thư mục trước.");
        }

        // 2. Duyệt và lưu từng file
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
				continue;
			}

            // Làm sạch tên file để tránh các ký tự đặc biệt nguy hiểm (Path Traversal)
            String rawFileName = StringUtils.cleanPath(file.getOriginalFilename());

            // Nếu muốn chống trùng file tuyệt đối khi lưu chung thư mục, bạn có thể cân nhắc nối chuỗi UUID:
            // String finalFileName = System.currentTimeMillis() + "_" + rawFileName;
            String finalFileName = rawFileName;

            Path targetPath = Paths.get(targetDirPath + finalFileName);

            // Ghi file trực tiếp từ InputStream của Request xuống ổ đĩa, ghi đè nếu file trùng tên
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            uploadedFileNames.add(finalFileName);
        }

        return uploadedFileNames;
    }

}