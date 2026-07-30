package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.demo.service.SupabaseStorageService.StorageRoot;

class AdminServiceTest {

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listFoldersReturnsEmptyListWhenStorageFails() throws IOException {
        when(supabaseStorageService.listObjects(StorageRoot.ARTICLES, "", true))
            .thenThrow(new IOException("Supabase unavailable"));

        List<String> folders = adminService.listFolders(StorageRoot.ARTICLES, "");

        assertTrue(folders.isEmpty());
    }
}
