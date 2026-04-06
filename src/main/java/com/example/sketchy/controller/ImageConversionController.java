package com.example.sketchy.controller;

import com.example.sketchy.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageConversionController {

    private final GeminiService geminiService;

    @Autowired
    public ImageConversionController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertImage(@RequestParam("image") MultipartFile image) {
        try {
            byte[] conversionResult = geminiService.processImageWithGemini(image);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(conversionResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the image: " + e.getMessage());
        }
    }
}
