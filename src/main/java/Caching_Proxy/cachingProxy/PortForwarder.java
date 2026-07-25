package Caching_Proxy.cachingProxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class PortForwarder {

    public final String origin;
    public final RestClient restClient;
    private final Cache cache;

    public PortForwarder(@Value("${proxy.origin}") String origin, CacheManager cacheManager) {
        this.origin = origin;
        this.restClient = RestClient.create();
        this.cache = cacheManager.getCache("responses");
    }

    // Forwarding the port with the main url
    @RequestMapping("/**")
    public ResponseEntity<byte[]> forward(HttpServletRequest request) {

        String query = request.getQueryString();
        String path = request.getRequestURI();
        String key = (query == null) ? path : path + "?" + query;
        System.out.println("Request: " + key);

        Cache.ValueWrapper cached = cache.get(key);

        if (cached != null) {
            ResponseEntity<byte[]> saved = (ResponseEntity<byte[]>) cached.get();
            return ResponseEntity.status(saved.getStatusCode())
                    .headers(h -> {
                        h.addAll(saved.getHeaders());
                        h.remove(HttpHeaders.TRANSFER_ENCODING);
                    })
                    .header("X-Cache", "HIT")
                    .body(saved.getBody());
        }

        ResponseEntity<byte[]> originResponse = restClient.get()
                .uri(origin + path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (res, req) -> {})
                .toEntity(byte[].class);

        cache.put(key, originResponse);
        return ResponseEntity.status(originResponse.getStatusCode())
                .headers(h -> {
                    h.addAll(originResponse.getHeaders());
                    h.remove(HttpHeaders.TRANSFER_ENCODING);
                })
                .header("X-Cache", "MISS")
                .body(originResponse.getBody());
    }

    @PostMapping("/clear")
    public String clearCache() {
        cache.clear();
        return "Cache cleared";
    }

}
