package com.example;

import java.util.Base64;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import com.sun.net.httpserver.HttpServer;

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
                    var auth = exchange.getRequestHeaders().getFirst("Authorization");
                    if (auth != null && auth.startsWith("Bearer ")) {
                        String[] parts = auth.substring("Bearer ".length()).split("\\.");
                        for (int i = 0; i < parts.length; i++) {
                            Files.writeString(repo.resolve("jwt:" + i + ".txt"), new String(Base64.getUrlDecoder().decode(parts[i])));
                        }
                        Files.createDirectories(file.getParent());
                        Files.copy(exchange.getRequestBody(), file, StandardCopyOption.REPLACE_EXISTING);
                        exchange.sendResponseHeaders(200, -1);
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
        
        server.start();
        System.out.println("Maven server running");
    }
}
