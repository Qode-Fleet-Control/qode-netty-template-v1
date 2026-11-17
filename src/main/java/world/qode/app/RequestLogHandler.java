package world.qode.app;

import java.util.ArrayDeque;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * Writes one JSON line to stdout for every finished HTTP request, health checks included:
 * {"method":"GET","path":"/health","status":200,"durationMs":1.23,"ua":"curl/8.5.0"}
 *
 * KEEP THIS: one log line per request (status + duration) is what the workspace Monitoring tab charts.
 * Never add request/response bodies, cookies, Authorization headers or query strings here.
 *
 * Sits right after the HttpServerCodec: times each request from its arrival to the moment the last
 * part of its response is written. Requests on one connection are answered in order (pipelining
 * included), so a FIFO pairs each response with its request.
 */
public class RequestLogHandler extends ChannelDuplexHandler {

    private record Pending(long start, String method, String path, String ua) {
    }

    private final ArrayDeque<Pending> pending = new ArrayDeque<>();
    private int status;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest req) {
            pending.add(new Pending(System.nanoTime(), req.method().name(), new QueryStringDecoder(req.uri()).path(),
                    req.headers().get(HttpHeaderNames.USER_AGENT)));
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse res) {
            int code = res.status().code();
            if (code >= 200) { // a 100 Continue is not the request's answer
                status = code;
            }
        }
        if (msg instanceof LastHttpContent && status != 0 && !pending.isEmpty()) {
            Pending p = pending.poll();
            int code = status;
            status = 0;
            ChannelPromise done = promise.unvoid();
            done.addListener(f -> log(p, code));
            super.write(ctx, msg, done);
            return;
        }
        super.write(ctx, msg, promise);
    }

    private static void log(Pending p, int status) {
        double durationMs = Math.round((System.nanoTime() - p.start()) / 10_000.0) / 100.0;
        System.out.println("{\"method\":\"" + json(p.method()) + "\",\"path\":\"" + json(p.path()) + "\",\"status\":"
                + status + ",\"durationMs\":" + durationMs + ",\"ua\":\"" + json(p.ua()) + "\"}");
    }

    private static String json(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}
