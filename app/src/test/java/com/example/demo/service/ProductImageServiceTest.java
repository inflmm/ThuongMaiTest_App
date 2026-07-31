package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductImageServiceTest {

    @Test
    void resolvePublicUrlShouldBuildSupabasePublicUrl() {
        ProductImageService service = new ProductImageService();
        ReflectionTestUtils.setField(service, "supabaseBaseUrl", "https://example.supabase.co");
        ReflectionTestUtils.setField(service, "bucketName", "product-images");

        String publicUrl = service.resolvePublicUrl("images/PCGVN/demo.webp");

        assertThat(publicUrl)
                .isEqualTo("https://example.supabase.co/storage/v1/object/public/product-images/images/PCGVN/demo.webp");
    }

    @Test
    void resolvePublicUrlWithVariantShouldInjectVariantSegment() {
        ProductImageService service = new ProductImageService();
        ReflectionTestUtils.setField(service, "supabaseBaseUrl", "https://example.supabase.co");
        ReflectionTestUtils.setField(service, "bucketName", "product-images");

        String publicUrl = service.resolvePublicUrl("images/PCGVN/demo.webp", "large");

        assertThat(publicUrl)
                .isEqualTo("https://example.supabase.co/storage/v1/object/public/product-images/images/PCGVN/large/demo.webp");
    }

    @Test
    void buildObjectPathShouldUseVariantFolder() {
        ProductImageService service = new ProductImageService();

        String objectPath = service.buildObjectPath("images/PCGVN", "thumbnail", "demo.webp");

        assertThat(objectPath).isEqualTo("images/PCGVN/thumbnail/demo.webp");
    }

    @Test
    void resolveUploadSettingsShouldClampQualityAndDefaultFormat() {
        ImageUploadService.UploadSettings settings = ImageUploadService.resolveUploadSettings("webp", 120, "specific", "custom", 1200, 900, "manual", "suiob", "size");

        assertThat(settings.format()).isEqualTo("webp");
        assertThat(settings.quality()).isEqualTo(100);
    }

    @Test
    void buildTargetFileNameShouldInclude32CharacterMiddleSegmentForPrefixedNaming() {
        ImageUploadService service = new ImageUploadService();
        ImageUploadService.UploadSettings settings = ImageUploadService.resolveUploadSettings("webp", 85, "specific", "large", null, null, "manual", "suiob", "size");

        String fileName = (String) ReflectionTestUtils.invokeMethod(service, "buildTargetFileName", "suiob", settings, "large", "webp");

        assertThat(fileName).matches("suiob_[a-z0-9]{32}_large\\.webp");
    }
}
