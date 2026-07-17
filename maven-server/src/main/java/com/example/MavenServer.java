package com.example;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;

public class MavenServer {

    public static void main(String[] args) throws Exception {

        var repo = Paths.get(System.getenv().getOrDefault("REPO_PATH", "./repo"));
        Files.createDirectories(repo);
        
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/repo/", exchange -> {

            var path = exchange.getRequestURI().getPath().substring("/repo/".length());
            var file = repo.resolve(path);
            
            switch (exchange.getRequestMethod()) {
                case "PUT" -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.copy(exchange.getRequestBody(), file, StandardCopyOption.REPLACE_EXISTING);
                        exchange.sendResponseHeaders(200, -1);
                    } catch (Exception e) {
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
                case "GET" -> {
                    if (Files.exists(file)) {
                        exchange.sendResponseHeaders(200, Files.size(file));
                        Files.copy(file, exchange.getResponseBody());
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        
        server.setExecutor(null);
        server.start();
        System.out.println("Maven server running on port 8080");
    }
}
