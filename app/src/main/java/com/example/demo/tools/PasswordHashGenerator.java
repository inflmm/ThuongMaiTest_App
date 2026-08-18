package com.example.demo.tools;

import java.util.Scanner;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * NOT a Spring bean — not annotated, not scanned, not wired into any
 * controller, startup path, or security config. Plain main() so this only
 * ever runs when YOU explicitly execute it in your IDE, on your own
 * machine. Nothing about it is ever deployed or reachable over HTTP.
 *
 * Use for out-of-band admin/employee password resets: run this locally,
 * copy the printed hash, then UPDATE the target user's row directly via
 * Supabase's table editor or SQL editor:
 *
 *   UPDATE users SET password = '<paste hash>' WHERE username = '...';
 *
 * The plaintext password never leaves your machine — it isn't sent to the
 * deployed app, isn't logged, and isn't typed into a chat transcript
 * anywhere. Uses the same BCryptPasswordEncoder (default strength) that
 * SecurityConfig already uses, so the resulting hash is guaranteed
 * compatible with how the app verifies it.
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the new plaintext password: ");
        String plainPassword = scanner.nextLine();
        scanner.close();

        if (plainPassword == null || plainPassword.isBlank()) {
            System.out.println("No password entered — aborting.");
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(plainPassword);

        System.out.println();
        System.out.println("Hash (paste this into the users.password column):");
        System.out.println(hash);
    }
}