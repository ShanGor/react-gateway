package io.github.shangor.gateway;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

import static io.github.shangor.gateway.MediaTypes.MEDIA_TYPES;

@Slf4j
@RestController
public class SinglePageApplicationController {
    private final String basePath;
    private final Path resourceParent;
    private final String defaultFile;
    private final String spaPath;

    private static final Set<String> NO_CACHE_FILES = Set.of("favicon.ico", "robots.txt", "index.html", "index.htm");

    public SinglePageApplicationController(@Value("${spring.webflux.base-path:}") String base,
                                           @Value("${gateway.default-file:index.html}") String defaultFile,
                                           @Value("${gateway.static-file-path}") String resourcePath,
                                           @Value("${gateway.spa-path}") String spaPath) {
        if (StringUtils.isBlank(base) || "/".equals(base)) {
            basePath = "";
        } else if (!base.startsWith("/")) {
            if (base.endsWith("/")) {
                basePath = "/%s".formatted(base.substring(0, base.length() - 1));
            } else {
                basePath = "/%s".formatted(base);
            }
        } else {
            if (base.endsWith("/")) {
                basePath = base.substring(0, base.length() - 1);
            } else {
                basePath = base;
            }
        }
        resourceParent = Paths.get(resourcePath);
        this.defaultFile = defaultFile;
        this.spaPath = spaPath;
    }

    public Mono<Void> index(ServerHttpResponse response) {
        if (!Files.exists(resourceParent)) {
            return return404(response);
        }
        var resource = resourceParent.resolve(defaultFile);
        if (!Files.exists(resource)) {
            return return404(response);
        }

        return returnFile(response, resource);
    }


    @GetMapping("${gateway.spa-path}/**")
    public Mono<Void> serveSpa(ServerHttpRequest request, ServerHttpResponse response) {
        var prefix = "%s%s".formatted(basePath, spaPath);
        var prefixAsPath = "%s%s/".formatted(basePath, spaPath);

        var requestPath = request.getPath().value();
        // Prevent path traversal
        if (requestPath.contains("..") || requestPath.contains("//") || requestPath.contains("./")|| requestPath.contains("\\")) {
            return return404(response);
        }

        if (prefix.equals(requestPath) || prefixAsPath.equals(requestPath)) return index(response);

        var filePath = requestPath.substring(prefixAsPath.length());

        var resource = resourceParent.resolve(filePath);
        if (!Files.exists(resource)) {
            return index(response);
        }
        if (Files.isDirectory(resource)) {
            var tryResource = resource.resolve(defaultFile);
            if (Files.exists(tryResource)) {
                return returnFile(response, tryResource);
            } else {
                // Maybe you want to make it 404 because it is requesting a directory
                return index(response);
            }
        }

        return returnFile(response, resource);
    }

    public static Mono<Void> return404(ServerHttpResponse response) {
        response.setRawStatusCode(404);
        return response.writeWith(Mono.empty());
    }

    public static Mono<Void> returnFile(ServerHttpResponse response, Path resource) {
        var file = resource.toFile();
        setHeaders(response, file);
        if (response instanceof ZeroCopyHttpOutputMessage zeroCopyResponse) {
            return zeroCopyResponse.writeWith(file, 0, file.length());
        } else {
            return response.writeWith(DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 4096));
        }
    }

    public static void setHeaders(ServerHttpResponse response, File file) {
        var fileName = file.getName().toLowerCase(Locale.ROOT);
        // get suffix of the fileName
        if (fileName.contains(".")) {
            var suffix = fileName.substring(fileName.lastIndexOf("."));
            if (MEDIA_TYPES.containsKey(suffix)) {
                response.getHeaders().set("Content-Type", MEDIA_TYPES.get(suffix));
            }
        }

        if (NO_CACHE_FILES.contains(fileName)) {
            response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            response.getHeaders().set("Pragma", "no-cache");
            response.getHeaders().set("Expires", "0");
        } else {
            // max-age: seconds to expire. 31536000 = 365 days approximately 1 year
            response.getHeaders().set("Cache-Control", "max-age=31536000");
        }
    }
}
