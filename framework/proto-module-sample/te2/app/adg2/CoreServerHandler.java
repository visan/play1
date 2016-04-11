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
import org.jboss.netty.channel.*;
import play.Invoker;
import play.Logger;

public class CoreServerHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            System.err.println(e);
        }
        super.handleUpstream(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: begin");
        }
        Core.Msg reqMsg = (Core.Msg) e.getMessage();
        long currentTime = System.currentTimeMillis();
        Core.Msg.Builder resMsg = Core.Msg.newBuilder();


        // Deleguate to Play framework
        Invoker.invoke(new ProtoCoreInvocation(reqMsg, resMsg, ctx, e));


        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: end");
        }


//        resMsg.setId(reqMsg.getId());
//        resMsg.setText("Response from server on: " + reqMsg.getText());

//        e.getChannel().write(resMsg.build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
