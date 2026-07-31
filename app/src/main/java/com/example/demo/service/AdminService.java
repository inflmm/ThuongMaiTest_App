package com.example.demo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.service.SupabaseStorageService.StorageRoot;

@Service
public class AdminService {

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    public List<String> listFolders(StorageRoot root, String prefix) {
        try {
            return supabaseStorageService.listObjects(root, prefix, true);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<String> listFiles(StorageRoot root, String prefix) {
        try {
            return supabaseStorageService.listObjects(root, prefix, false);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to list files from storage: " + ex.getMessage(), ex);
        }
    }

    public void createFolder(StorageRoot root, String folderPath) throws IOException {
        supabaseStorageService.createFolder(root, folderPath);
    }

    public void deleteFolder(StorageRoot root, String folderPath) throws IOException {
        supabaseStorageService.deleteFolder(root, folderPath);
    }
}
