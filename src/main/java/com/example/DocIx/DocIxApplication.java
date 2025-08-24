package com.example.DocIx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kelas utama aplikasi DocIx - Sistem manajemen dan pencarian dokumen
 * Menggunakan Spring Boot framework untuk menjalankan aplikasi
 */
@SpringBootApplication
public class DocIxApplication {

	/**
	 * Metode utama untuk menjalankan aplikasi Spring Boot
	 * @param args argumen baris perintah yang diteruskan saat aplikasi dijalankan
	 */
	public static void main(String[] args) {
		SpringApplication.run(DocIxApplication.class, args);
	}

}
