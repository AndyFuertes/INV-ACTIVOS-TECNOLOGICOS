package com.dti.keru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KeruApplication {
    public static void main(String[] args) {
        SpringApplication.run(KeruApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  KERU Sistema - DTI UPCH");
        System.out.println("  Servidor corriendo en puerto 8080");
        System.out.println("  http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}
