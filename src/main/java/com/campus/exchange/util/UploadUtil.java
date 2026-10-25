package com.campus.exchange.util;

import com.campus.exchange.exception.ValidationException;
import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Secure image upload handling. Browser filenames are never trusted.
 */
public final class UploadUtil {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    /** Guards against decompression bombs: a 5 MB PNG can decode to gigabytes. */
    public static final long MAX_IMAGE_PIXELS = 40_000_000L;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif");

    private static final Set<String> SAFE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Pattern SAFE_NAME =
            Pattern.compile("^[a-f0-9-]{36}\\.(jpg|jpeg|png|gif)$");

    private UploadUtil() {
    }

    /**
     * Saves an uploaded image and returns the generated filename, or null when
     * no file was supplied.
     */
    public static String saveImage(Part part, Path uploadDir) throws IOException {
        if (part == null || part.getSize() == 0) {
            return null;
        }
        if (part.getSize() > MAX_IMAGE_BYTES) {
            throw new ValidationException("Image must be 5 MB or smaller.");
        }

        String contentType = part.getContentType() == null
                ? ""
                : part.getContentType().toLowerCase(Locale.ROOT);
        String extension = CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new ValidationException("Only JPEG, PNG, and GIF images are accepted.");
        }

        String submitted = part.getSubmittedFileName();
        if (submitted != null && submitted.contains(".")) {
            String submittedExtension = submitted.substring(submitted.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
            if (!SAFE_EXTENSIONS.contains(submittedExtension)) {
                throw new ValidationException("Unsupported image extension.");
            }
        }

        try (InputStream input = part.getInputStream()) {
            var image = ImageIO.read(input);
            if (image == null) {
                throw new ValidationException("The uploaded file is not a valid image.");
            }
            if ((long) image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS) {
                throw new ValidationException("Image dimensions are too large.");
            }
        }

        Files.createDirectories(uploadDir);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ValidationException("Invalid upload path.");
        }
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    public static void deleteQuietly(Path uploadDir, String filename) {
        if (filename == null || !isSafeImageName(filename)) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(filename).normalize());
        } catch (IOException ignored) {
            // A stale file is not a reason to fail the user's request.
        }
    }

    public static boolean isSafeImageName(String filename) {
        return filename != null && SAFE_NAME.matcher(filename).matches();
    }

    public static String contentTypeFor(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
