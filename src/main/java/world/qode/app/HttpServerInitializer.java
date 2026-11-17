package world.qode.app;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        // KEEP THIS: one log line per request (status + duration) is what the workspace Monitoring tab charts.
        p.addLast(new RequestLogHandler());
        p.addLast(new HttpObjectAggregator(1 << 20));
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new HttpServerHandler());
    }
}
