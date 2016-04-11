package play.server;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.stream.ChunkedInput;
import org.jboss.netty.handler.stream.ChunkedStream;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.RenderStatic;
import play.utils.HTTP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;

public class PlayHandler extends SimpleChannelUpstreamHandler {

    /**
     * If true (the default), Play will send the HTTP header "Server: Play! Framework; ....".
     * This could be a security problem (old versions having publicly known security bugs), so you can
     * disable the header in application.conf: <code>http.exposePlayServer = false</code>
     */
    private final static String signature = "Play! Framework;" + Play.version + ";" + Play.mode.name().toLowerCase();
    private final static boolean exposePlayServer;

    private static final String ACCEPT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Charset ASCII = Charset.forName("ASCII");
    private static final MessageDigest SHA_1;
    
    /** 
     * The Pipeline is given for a PlayHandler 
     */
    public Map<String, ChannelHandler> pipelines = new HashMap<String, ChannelHandler>();

    private WebSocketServerHandshaker handshaker;

    static {
        try {
            SHA_1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("SHA-1 not supported on this platform");
        }
    } 
    
    static {
        exposePlayServer = !"false".equals(Play.configuration.getProperty("http.exposePlayServer"));
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent messageEvent) throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: begin");
        }

        final Object msg = messageEvent.getMessage();

        // Http request
        if (msg instanceof HttpRequest) {

            final HttpRequest nettyRequest = (HttpRequest) msg;



            // Plain old HttpRequest
            try {
                final Request request = parseRequest(ctx, nettyRequest, messageEvent);

                final Response response = new Response();
                Http.Response.current.set(response);

                // Buffered in memory output
                response.out = new ByteArrayOutputStream();


                // Raw invocation
                boolean raw = Play.pluginCollection.rawInvocation(request, response);
                if (raw) {
                    copyResponse(ctx, request, response, nettyRequest);
                } else {

                    // Deleguate to Play framework
                    Invoker.invoke(new NettyInvocation(request, response, ctx, nettyRequest, messageEvent));

                }

            } catch (Exception ex) {
            	Logger.warn(ex, "Exception on request. serving 500 back");

            }
        }



        if (Logger.isTraceEnabled()) {
            Logger.trace("messageReceived: end");
        }
    }

    private static final Map<String, RenderStatic> staticPathsCache = new HashMap<String, RenderStatic>();

    public class NettyInvocation extends Invoker.Invocation {

        private final ChannelHandlerContext ctx;
        private final Request request;
        private final Response response;
        private final HttpRequest nettyRequest;
        private final MessageEvent event;

        public NettyInvocation(Request request, Response response, ChannelHandlerContext ctx, HttpRequest nettyRequest, MessageEvent e) {
            this.ctx = ctx;
            this.request = request;
            this.response = response;
            this.nettyRequest = nettyRequest;
            this.event = e;
        }

        @Override
        public boolean init() {
            Thread.currentThread().setContextClassLoader(Play.classloader);
            if (Logger.isTraceEnabled()) {
                Logger.trace("init: begin");
            }

            Request.current.set(request);
            Response.current.set(response);
            

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
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request, response);
            return new InvocationContext(Http.invocationType,
                    request.invokedMethod.getAnnotations(),
                    request.invokedMethod.getDeclaringClass().getAnnotations());
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
        public void execute() throws Exception {
            if (!ctx.getChannel().isConnected()) {
                try {
                    ctx.getChannel().close();
                } catch (Throwable e) {
                    // Ignore
                }
                return;
            }

            // Check the exceeded size before re rendering so we can render the error if the size is exceeded
            saveExceededSizeError(nettyRequest, request, response);
            ActionInvoker.invoke(request, response);
        }

        @Override
        public void onSuccess() throws Exception {
            super.onSuccess();
                   copyResponse(ctx, request, response, nettyRequest);

            if (Logger.isTraceEnabled()) {
                Logger.trace("execute: end");
            }
        }
    }

    void saveExceededSizeError(HttpRequest nettyRequest, Request request, Response response) {
    }

    protected static void writeResponse(ChannelHandlerContext ctx, Response response, HttpResponse nettyResponse, HttpRequest nettyRequest) {
        if (Logger.isTraceEnabled()) {
            Logger.trace("writeResponse: begin");
        }

        byte[] content = null;

        final boolean keepAlive =false;// isKeepAlive(nettyRequest);
        if (nettyRequest.getMethod().equals(HttpMethod.HEAD)) {
            content = new byte[0];
        } else {
            content = response.out.toByteArray();
        }

        ChannelBuffer buf = ChannelBuffers.copiedBuffer(content);
        nettyResponse.setContent(buf);

        if (!nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
            if (Logger.isTraceEnabled()) {
                Logger.trace("writeResponse: content length [" + response.out.size() + "]");
            }
//            setContentLength(nettyResponse, response.out.size());
        }

        ChannelFuture f = ctx.getChannel().write(nettyResponse);

        // Decide whether to close the connection or not.
        if (!keepAlive) {
            // Close the connection when the whole content is written out.
            f.addListener(ChannelFutureListener.CLOSE);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("writeResponse: end");
        }
    }

    public void copyResponse(ChannelHandlerContext ctx, Request request, Response response, HttpRequest nettyRequest) throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("copyResponse: begin");
        }

        // Decide whether to close the connection or not.

        HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(200));

        final Object obj = response.direct;
        File file = null;
        ChunkedInput stream = null;
        InputStream is = null;
        if (obj instanceof File) {
            file = (File) obj;
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        } else if (obj instanceof ChunkedInput) {
            // Streaming we don't know the content length
            stream = (ChunkedInput) obj;
        }

        final boolean keepAlive = false;//isKeepAlive(nettyRequest);
        if (file != null && file.isFile()) {
            try {
                if (nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {

                    Channel ch = ctx.getChannel();

                    // Write the initial line and the header.
                    ChannelFuture writeFuture = ch.write(nettyResponse);

                    if (!keepAlive) {
                        // Close the connection when the whole content is written out.
                        writeFuture.addListener(ChannelFutureListener.CLOSE);
                    }
                } else {
                    try {
//                        FileService.serve(file, nettyRequest, nettyResponse, ctx, request, response, ctx.getChannel());
                    } catch (Throwable exx) {
                        
                        try {
                            ctx.getChannel().close();
                        } catch (Throwable ex) { /* Left empty */ }
                    }

                }
            } catch (Exception e) {
                throw e;
            }
        } else if (is != null) {
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD) && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.getChannel().write(new ChunkedStream(is));
            } else {
                is.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else if (stream != null) {
            ChannelFuture writeFuture = ctx.getChannel().write(nettyResponse);
            if (!nettyRequest.getMethod().equals(HttpMethod.HEAD) && !nettyResponse.getStatus().equals(HttpResponseStatus.NOT_MODIFIED)) {
                writeFuture = ctx.getChannel().write(stream);
            } else {
                stream.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            writeResponse(ctx, response, nettyResponse, nettyRequest);
        }
        if (Logger.isTraceEnabled()) {
            Logger.trace("copyResponse: end");
        }
    }


    static String getRemoteIPAddress(MessageEvent e) {
        String fullAddress = ((InetSocketAddress) e.getRemoteAddress()).getAddress().getHostAddress();
        if (fullAddress.matches("/[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+[:][0-9]+")) {
            fullAddress = fullAddress.substring(1);
            fullAddress = fullAddress.substring(0, fullAddress.indexOf(":"));
        } else if (fullAddress.matches(".*[%].*")) {
            fullAddress = fullAddress.substring(0, fullAddress.indexOf("%"));
        }
        return fullAddress;
    }

    public Request parseRequest(ChannelHandlerContext ctx, HttpRequest nettyRequest, MessageEvent messageEvent) throws Exception {
        if (Logger.isTraceEnabled()) {
            Logger.trace("parseRequest: begin");
            Logger.trace("parseRequest: URI = " + nettyRequest.getUri());
        }
        String uri = nettyRequest.getUri();
        // Remove domain and port from URI if it's present.
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Begins searching / after 9th character (last / of https://)
            int index = uri.indexOf("/", 9);
            // prevent the IndexOutOfBoundsException that was occurring
            if (index >= 0){
              uri = uri.substring(index);
            } else {
              uri = "/";
            }
        }

        String contentType = nettyRequest.getHeader(CONTENT_TYPE);

        // need to get the encoding now - before the Http.Request is created
        String encoding = Play.defaultWebEncoding;
        if (contentType != null) {
            HTTP.ContentTypeWithEncoding contentTypeEncoding = HTTP.parseContentType(contentType);
            if (contentTypeEncoding.encoding != null) {
                encoding = contentTypeEncoding.encoding;
            }
        }

        final int i = uri.indexOf("?");
        String querystring = "";
        String path = uri;
        if (i != -1) {
            path = uri.substring(0, i);
            querystring = uri.substring(i + 1);
        }

        String remoteAddress = getRemoteIPAddress(messageEvent);
        String method = nettyRequest.getMethod().getName();

        if (nettyRequest.getHeader("X-HTTP-Method-Override") != null) {
            method = nettyRequest.getHeader("X-HTTP-Method-Override").intern();
        }

        InputStream body = null;
        ChannelBuffer b = nettyRequest.getContent();
        if (b instanceof FileChannelBuffer) {
            FileChannelBuffer buffer = (FileChannelBuffer) b;
            // An error occurred
            Integer max = Integer.valueOf(Play.configuration.getProperty("play.netty.maxContentLength", "-1"));

            body = buffer.getInputStream();
            if (!(max == -1 || body.available() < max)) {
                body = new ByteArrayInputStream(new byte[0]);
            }

        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(new ChannelBufferInputStream(b), out);
            byte[] n = out.toByteArray();
            body = new ByteArrayInputStream(n);
        }

        String host = nettyRequest.getHeader(HOST);
        boolean isLoopback = false;
        try {
            isLoopback = ((InetSocketAddress) messageEvent.getRemoteAddress()).getAddress().isLoopbackAddress() && host.matches("^127\\.0\\.0\\.1:?[0-9]*$");
        } catch (Exception e) {
            // ignore it
        }

        int port = 0;
        String domain = null;
        if (host == null) {
            host = "";
            port = 80;
            domain = "";
        }
        // Check for IPv6 address
        else if (host.startsWith("[")) {
            // There is no port
            if (host.endsWith("]")) {
                domain = host;
                port = 80;
            }
            else {
                // There is a port so take from the last colon
                int portStart = host.lastIndexOf(':');
                if (portStart > 0 && (portStart + 1) < host.length()) {
                    domain = host.substring(0, portStart);
                    port = Integer.parseInt(host.substring(portStart + 1));
                }
            }
        }
        // Non IPv6 but has port
        else if (host.contains(":")) {
            final String[] hosts = host.split(":");
            port = Integer.parseInt(hosts[1]);
            domain = hosts[0];
        } else {
            port = 80;
            domain = host;
        }

        boolean secure = false;

        final Request request = Request.createRequest(
                remoteAddress,
                method,
                path,
                querystring,
                contentType,
                body,
                uri,
                host,
                isLoopback,
                port,
                domain,
                secure,
                getHeaders(nettyRequest),
                getCookies(nettyRequest));


        if (Logger.isTraceEnabled()) {
            Logger.trace("parseRequest: end");
        }
        return request;
    }

    protected static Map<String, Http.Header> getHeaders(HttpRequest nettyRequest) {
        Map<String, Http.Header> headers = new HashMap<String, Http.Header>(16);

        for (String key : nettyRequest.getHeaderNames()) {
            Http.Header hd = new Http.Header();
            hd.name = key.toLowerCase();
            hd.values = new ArrayList<String>();
            for (String next : nettyRequest.getHeaders(key)) {
                hd.values.add(next);
            }
            headers.put(hd.name, hd);
        }

        return headers;
    }

    protected static Map<String, Http.Cookie> getCookies(HttpRequest nettyRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<String, Http.Cookie>(16);
        String value = nettyRequest.getHeader(COOKIE);
        if (value != null) {
            Set<Cookie> cookieSet = new CookieDecoder().decode(value);
            if (cookieSet != null) {
                for (Cookie cookie : cookieSet) {
                    Http.Cookie playCookie = new Http.Cookie();
                    playCookie.name = cookie.getName();
                    playCookie.path = cookie.getPath();
                    playCookie.domain = cookie.getDomain();
                    playCookie.secure = cookie.isSecure();
                    playCookie.value = cookie.getValue();
                    playCookie.httpOnly = cookie.isHttpOnly();
                    cookies.put(playCookie.name, playCookie);
                }
            }
        }
        return cookies;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        try {
            // If we get a TooLongFrameException, we got a request exceeding 8k.
            // Log this, we can't call serve500()
            Throwable t = e.getCause();
            if (t instanceof TooLongFrameException) {
                Logger.error("Request exceeds 8192 bytes");
            }
            e.getChannel().close();
        } catch (Exception ex) {
        }
    }



    protected static Map<String, Object> getBindingForErrors(Exception e, boolean isError) {

        Map<String, Object> binding = new HashMap<String, Object>();
        if (!isError) {
            binding.put("result", e);
        } else {
            binding.put("exception", e);
        }
//        binding.put("session", Scope.Session.current());
        binding.put("request", Http.Request.current());
//        binding.put("flash", Scope.Flash.current());
//        binding.put("params", Scope.Params.current());
        binding.put("play", new Play());
        try {
//            binding.put("errors", Validation.errors());
        } catch (Exception ex) {
            //Logger.error(ex, "Error when getting Validation errors");
        }

        return binding;
    }
}
