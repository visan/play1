package adg2;

import adg.Core;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import play.Invoker;
import play.Logger;
import play.Play;
import play.mvc.Http;
import play.mvc.results.RenderStatic;

import java.lang.annotation.Annotation;

/**
 * Created by adg on 13.04.2015.
 */
public class ProtoCoreInvocation extends Invoker.Invocation {
    private final Core.Msg reqMsg;
    private final Core.Msg.Builder resMsg;
    private final MessageEvent messageEvent;

    public ProtoCoreInvocation(Core.Msg reqMsg, Core.Msg.Builder resMsg, ChannelHandlerContext ctx, MessageEvent e) {
        this.reqMsg=reqMsg;
        this.resMsg = resMsg;
        this.messageEvent=e;
    }

    @Override
    public void execute() throws Exception {
        ProtoActionInvoker.resolve(reqMsg, resMsg);
        ProtoActionInvoker.invoke(reqMsg, resMsg);
    }

    @Override
    public Invoker.InvocationContext getInvocationContext() {
//        ActionInvoker.resolve(request, response);
        return new Invoker.InvocationContext("Proto", new Annotation[]{}, new Annotation[]{});
//                Http.invocationType,
//                request.invokedMethod.getAnnotations(),
//                request.invokedMethod.getDeclaringClass().getAnnotations()
//        );
    }
    @Override
    public boolean init() {
        Thread.currentThread().setContextClassLoader(Play.classloader);
        if (Logger.isTraceEnabled()) {
            Logger.trace("init: begin");
        }


//            CachedBoundActionMethodArgs.init();

        try {
            if (Play.mode == Play.Mode.DEV) {
//                    Router.detectChanges(Play.ctxPath);
            }
            super.init();
//            } catch (NotFound nf) {
//                if (Logger.isTraceEnabled()) {
//                    Logger.trace("init: end false");
//                }
//                return false;
        } catch (RenderStatic rs) {
        }

        if (Logger.isTraceEnabled()) {
            Logger.trace("init: end true");
        }
        return true;
    }
    @Override
    public void run() {
        try {
            if (Logger.isTraceEnabled()) {
                Logger.trace("run: begin");
            }
            super.run();
        } catch (Exception e) {
//                serve500(e, ctx, nettyRequest);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("run: end");
        }
    }
    @Override
    public void onSuccess() throws Exception {
        super.onSuccess();
//        copyResponse(ctx, request, response, nettyRequest);

        resMsg.setId(reqMsg.getId());
        resMsg.setText("Response from server on: " + reqMsg.getText());
        messageEvent.getChannel().write(resMsg.build());

        Logger.trace("send actual response!!!");

        if (Logger.isTraceEnabled()) {
            Logger.trace("execute: end");
        }
    }
}
