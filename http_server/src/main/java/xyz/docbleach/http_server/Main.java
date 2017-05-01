package xyz.docbleach.http_server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.docbleach.api.BleachSession;
import xyz.docbleach.api.bleach.DefaultBleach;
import xyz.docbleach.api.exception.BleachException;

import java.io.*;
import java.util.Set;

public class Main extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final long BODY_LIMIT = 1024 * 1024 * 150; // Size limit: 150MB

    private static int getPortNumber() {
        int port = 8080;
        String PORT = System.getenv("PORT");
        if (PORT != null && !PORT.isEmpty()) {
            try {
                port = Integer.valueOf(PORT);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid PORT defined in environment, falling back to 8080.");
            }
        }
        return port;
    }

    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create().setBodyLimit(BODY_LIMIT));

        router.post("/sanitize").handler(routingContext -> {
            Set<FileUpload> uploads = routingContext.fileUploads();
            if (uploads.isEmpty()) {
                routingContext.fail(404);
                return;
            }

            for (FileUpload upload : uploads) {
                LOGGER.info("FileName: {}", upload.fileName());
                if (!"file".equals(upload.name())) {
                    removeFiles(new File(upload.uploadedFileName()));
                    continue;
                }
                // @TODO: split into multiple methods

                LOGGER.info("UploadedFileName: {}", upload.fileName());

                vertx.executeBlocking((Handler<Future<File>>) future -> {
                    try {
                        future.complete(sanitize(upload.uploadedFileName()));
                    } catch (IOException | BleachException e) {
                        LOGGER.error("Error", e);
                        future.fail(e);
                    }
                }, res -> {
                    if (!res.succeeded()) {
                        routingContext.fail(res.cause());
                        return;
                    }

                    final File saneFile = res.result();
                    sendFile(routingContext, upload.fileName(), saneFile);
                    removeFiles(new File(upload.uploadedFileName()), saneFile);
                });

                return;
            }
            // No "file" was found, we abort.
            routingContext.fail(404);
        });

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello from the light DocBleach Server!");
        });

        server.requestHandler(router::accept).listen(getPortNumber());
    }

    private void sendFile(RoutingContext routingContext, String fileName, File saneFile) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Description", "File Transfer");
        response.putHeader("Content-Type", "application/octet-stream");
        response.putHeader("Content-Disposition", "attachment; filename=" + fileName); // @TODO: don't trust this name?
        response.putHeader("Content-Transfer-Encoding", "binary");
        response.putHeader("Expires", "0");
        response.putHeader("Pragma", "Public");
        response.putHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.putHeader("Content-Length", "" + saneFile.length());

        response.sendFile(saneFile.getAbsolutePath());
    }

    private void removeFiles(File... files) {
        vertx.executeBlocking(future -> {
            for (File f : files) {
                if (!f.delete()) {
                    LOGGER.warn("Could not delete file{} ", f.getAbsolutePath());
                }
            }
        }, __ -> {
        });
    }

    private File sanitize(String uploadedFileName) throws IOException, BleachException {
        BleachSession session = new BleachSession(new DefaultBleach());

        File file = new File(uploadedFileName);
        file.deleteOnExit();

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            File fstream = File.createTempFile("docbleach_", "");
            fstream.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(fstream)) {
                session.sanitize(is, os);
                LOGGER.info("Sanitation for '{}': {} potential threats removed", uploadedFileName, session.threatCount());
            }
            return fstream;
        }
    }
}
