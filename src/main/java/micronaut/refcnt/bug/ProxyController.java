package micronaut.refcnt.bug;

import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@Controller
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private static final Set<String> IGNORED_HEADERS = Set.of("host", "connection");

    @Inject @Client("http://localhost:8080") StreamingHttpClient client;

    @Get("/")
    public Publisher<HttpResponse<ByteBuffer<?>>> root(HttpRequest<?> request) {
        return requestOrigin(request);
    }

    @Get("/{+path}")
    public Publisher<HttpResponse<ByteBuffer<?>>> path(HttpRequest<?> request, String path) {
        return requestOrigin(request);
    }

    private Publisher<HttpResponse<ByteBuffer<?>>> requestOrigin(HttpRequest<?> originalRequest) {
        String uri = originalRequest.getUri().getPath();

        MutableHttpRequest<?> request = HttpRequest.create(originalRequest.getMethod(), uri);

        originalRequest.getHeaders().forEach((it) -> {
            String header = it.getKey();

            if (!IGNORED_HEADERS.contains(header.toLowerCase().trim().strip())) {
                List<String> values = it.getValue();

                for (String value : values) {
                    request.header(header, value);
                }
            }
        });

        log.debug("{} {}", request.getMethodName(), request.getUri().toString());

        return client.exchangeStream(request);
    }

}
