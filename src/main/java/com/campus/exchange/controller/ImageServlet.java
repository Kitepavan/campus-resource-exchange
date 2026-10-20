package com.campus.exchange.controller;

import com.campus.exchange.util.AppConfig;
import com.campus.exchange.util.UploadUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controlled retrieval of listing images. Only UUID-style filenames generated
 * by UploadUtil are accepted; arbitrary server paths are never served.
 */
@WebServlet("/images/*")
public class ImageServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String filename = request.getPathInfo() == null
                ? null
                : request.getPathInfo().substring(1);
        if (!UploadUtil.isSafeImageName(filename)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path directory = AppConfig.uploadDir();
        Path image = directory.resolve(filename).normalize();
        if (!image.startsWith(directory) || !Files.isRegularFile(image)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(UploadUtil.contentTypeFor(filename));
        response.setContentLengthLong(Files.size(image));
        response.setHeader("Cache-Control", "public, max-age=86400");
        Files.copy(image, response.getOutputStream());
    }
}
