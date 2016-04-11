package adg2;

import play.proto.RxContinuationEnhancer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.LoggerFactory;
import play.Logger;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.Enhancer;
import play.exceptions.UnexpectedException;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Created by adg on 12.04.2015.
 */
public class CoreTransportPlugin extends PlayPlugin {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "9001"));
    private static org.slf4j.Logger log = LoggerFactory.getLogger(CoreTransportPlugin.class);

    public static void main(String[] args) throws Exception {
    }

    @Override
    public void onApplicationStart() {
    }

    @Override
    public void afterApplicationStart() {
        try {
            // Configure SSL.
            final SslContext sslCtx;
            if (SSL) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } else {
                sslCtx = null;
            }

            // Configure the server.
            ServerBootstrap bootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool()));

            // Set up the event pipeline factory.
            bootstrap.setPipelineFactory(new CoreServerPipelineFactory(sslCtx));

            // Bind and start to accept incoming connections.
            bootstrap.bind(new InetSocketAddress(PORT));
            Logger.info("Listening for Proto on port %s ...", PORT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onInvocationException(Throwable e) {
        log.info("onInvocationException", e);
    }
}
