/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package adg2;

import adg.Core;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends a list of continent/city pairs to a {@link LocalTimeServer} to
 * get the local times of the specified cities.
 */
public final class CoreClient implements Runnable {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "localhost");
    static final int PORT = Integer.parseInt(System.getProperty("port", "9001"));
    private static AtomicInteger along = new AtomicInteger();
    ChannelFuture connectFuture;
    Channel channel;
    CoreClientHandler handler;

    public CoreClient() {
        final SslContext sslCtx;
        ClientBootstrap bootstrap = null;
        try {
            // Configure SSL.
            if (SSL) {
                sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            } else {
                sslCtx = null;
            }
            bootstrap = new ClientBootstrap(
                    new NioClientSocketChannelFactory(
                            Executors.newCachedThreadPool(),
                            Executors.newCachedThreadPool()));
            // Configure the event pipeline factory.
            bootstrap.setPipelineFactory(new CoreClientPipelineFactory(sslCtx));

            // Make a new connection.
            connectFuture = bootstrap.connect(new InetSocketAddress(HOST, PORT));

            // Wait until the connection is made successfully.
            channel = connectFuture.sync().getChannel();

            // Get the handler instance to initiate the request.
            handler = channel.getPipeline().get(CoreClientHandler.class);


//            // Close the connection.
//            channel.close().sync();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Shut down all thread pools to exit.
//            if (bootstrap != null) bootstrap.releaseExternalResources();
        }

    }

    public static void main(String[] args) throws Exception {
        Thread thread = new Thread(new CoreClient());
        thread.start();
        Thread.currentThread().setDaemon(true);

    }

    @Override
    public void run() {
//        while (true)
        {
            try {
                // Request and get the response.
                Core.Msg.Builder msgReqBldr = Core.Msg.newBuilder();
                msgReqBldr.setId(along.incrementAndGet());
                msgReqBldr.setText("message: " + System.currentTimeMillis());
                Core.Msg msgReq = msgReqBldr.build();

                Core.Msg resMsg = handler.send(msgReq);
                // Print the response at last but not least.
                System.out.printf("\nResponse: id:%s, text:%s", resMsg.getId(), resMsg.getText());

                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
