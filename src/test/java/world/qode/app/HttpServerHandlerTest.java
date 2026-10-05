package world.qode.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HttpServerHandlerTest {

    private FullHttpResponse get(String uri) {
        EmbeddedChannel ch = new EmbeddedChannel(new HttpServerHandler());
        ch.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));
        return ch.readOutbound();
    }

    @Test
    void healthIsOk() {
        FullHttpResponse r = get("/health");
        assertEquals(HttpResponseStatus.OK, r.status());
        ByteBuf body = r.content();
        assertEquals("{\"status\":\"ok\"}", body.toString(StandardCharsets.UTF_8));
        r.release();
    }

    @Test
    void unknownPathIs404() {
        FullHttpResponse r = get("/nope");
        assertEquals(HttpResponseStatus.NOT_FOUND, r.status());
        r.release();
    }
}
