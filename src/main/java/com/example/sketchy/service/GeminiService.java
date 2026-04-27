package com.example.sketchy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;

@Service
public class GeminiService {

    @Value("${spring.ai.google.genai.api-key}")
    private String googleApiKey;

    private static final long FILE_TTL_MINUTES = 5;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final String PROMPT = "You are a master sketch artist in domain of textile that specialises in converting design of a traditional indian ladies suit from an image to a sketch template"
            + " You identify different components of the suit from the image, stating with border, then you identify the design of the neck and at last you see if there is any block pattern on the suit. The border is a repetitive pattern that marks the exterior boundary of the sketch."
            + " The neck is a vertically symmetrical design at the top of the sketch within the border that indicates the style of the suit."
            + " The block pattern is a repetitive pattern that is present on the suit."
            // + "Use the sample named Sample.png provided to understand the style of the sketch template. Do not incorporate the given sketch in the design "
            + " Don't over do the sketch, just give the basic outline of the suit"
            + " I will be giving you an image. You have to identify the textile suit in the image, extract it and convert it to a single symmeterical stencil template of aspect ratio 3:5 following the same style as I describe."
            + "The border acts as the frame of the image. While top left and right border are always uniform, the bottom border may not mandatory be slightly be different"
            +"The neck is at the top of the image. The block pattern, if present, is spread throughout the middle of the image.";

    /**
     * Tries to delete a file immediately. If deletion fails, schedules
     * a fallback deletion after FILE_TTL_MINUTES (5 minutes).
     */
    private void deleteWithTtlFallback(Path filePath) {
        if (filePath == null) return;
        try {
            if (Files.deleteIfExists(filePath)) {
                System.out.println("Deleted file: " + filePath);
                return;
            }
        } catch (IOException e) {
            System.err.println("Immediate delete failed for: " + filePath + ", scheduling TTL fallback.");
        }
        // Fallback: schedule deletion after 5 minutes
        scheduler.schedule(() -> {
            try {
                Files.deleteIfExists(filePath);
                System.out.println("TTL fallback — deleted file: " + filePath);
            } catch (IOException e) {
                System.err.println("TTL fallback delete also failed: " + filePath);
                e.printStackTrace();
            }
        }, FILE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public byte[] processImageWithGemini(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty or missing");
        }

        String uploadDir = "src/main/resources/static";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path tempFilePath = Paths.get(uploadDir, fileName);
        Files.copy(image.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        // byte[] sampleImageBytes = Files.readAllBytes(Paths.get("src/main/resources/static/sample.png"));
        byte[] imageBytes = image.getBytes();
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/png";
        System.out.println("<<<<<<<<<>>>>>>>>>>>>>>");
        // System.out.println(PROMPT);
        try (Client client = Client.builder().apiKey(googleApiKey).build()) {
            System.out.println("111111111111111");
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .build();
            System.out.println("222222222222222");
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.1-flash-image-preview",
                    Content.fromParts(
                            Part.fromText(PROMPT),
                            // Part.fromBytes(sampleImageBytes, mimeType),
                            Part.fromBytes(imageBytes, mimeType)),
                    config);
            System.out.println("333333333333333");
            // Extract the generated image from response parts
            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] resultBytes = blob.data().get();

                        // Save the result image to static directory
                        String resultFileName = "result_" + UUID.randomUUID().toString() + ".png";
                        Path resultFilePath = Paths.get(uploadDir, resultFileName);
                        Files.write(resultFilePath, resultBytes);
                        System.out.println("Result saved to: " + resultFilePath);

                        // Delete result file immediately, TTL fallback if it fails
                        deleteWithTtlFallback(resultFilePath);

                        return resultBytes;
                    }
                }
            }

            // If no image was returned, fall back to original
            System.out.println("No image generated by Gemini. Text response: " + response.text());
            return imageBytes;
        } finally {
            // Delete uploaded file immediately, TTL fallback if it fails
            deleteWithTtlFallback(tempFilePath);
        }
    }
}
