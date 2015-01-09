package play.mvc;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.ChannelHandlerContext;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.F;
import play.libs.F.BlockingEventStream;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.EventStream;
import play.libs.Time;
import play.utils.HTTP;
import play.utils.Utils;

/**
 * HTTP interface
 */
public class Http {

    public static final String invocationType = "HttpRequest";

    public static class StatusCode {

        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int PARTIAL_INFO = 203;
        public static final int NO_RESPONSE = 204;
        public static final int MOVED = 301;
        public static final int FOUND = 302;
        public static final int METHOD = 303;
        public static final int NOT_MODIFIED = 304;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int PAYMENT_REQUIRED = 402;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
        public static final int NOT_IMPLEMENTED = 501;
        public static final int OVERLOADED = 502;
        public static final int GATEWAY_TIMEOUT = 503;

        public static boolean success(int code) {
            return code / 100 == 2;
        }

        public static boolean redirect(int code) {
            return code / 100 == 3;
        }

        public static boolean error(int code) {
            return code / 100 == 4 || code / 100 == 5;
        }
    }

    /**
     * An HTTP Header
     */
    public static class Header implements Serializable {

        /**
         * Header name
         */
        public String name;
        /**
         * Header value
         */
        public List<String> values;

        public Header() {
            this.values = new ArrayList<String>(5);
        }

        public Header(String name, String value) {
            this.name = name;
            this.values = new ArrayList<String>(5);
            this.values.add(value);
        }

        public Header(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        /**
         * First value
         * @return The first value
         */
        public String value() {
            return values.get(0);
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    /**
     * An HTTP Cookie
     */
    public static class Cookie implements Serializable {

        /**
         * When creating cookie without specifying domain,
         * this value is used. Can be configured using
         * the property 'application.defaultCookieDomain'
         * in application.conf.
         *
         * This feature can be used to allow sharing
         * session/cookies between multiple sub domains.
         */
        public static String defaultDomain = null;

        /**
         * Cookie name
         */
        public String name;
        /**
         * Cookie domain
         */
        public String domain;
        /**
         * Cookie path
         */
        public String path = Play.ctxPath + "/";
        /**
         * for HTTPS ?
         */
        public boolean secure = false;
        /**
         * Cookie value
         */
        public String value;
        /**
         * Cookie max-age in second
         */
        public Integer maxAge;
        /**
         * Don't use
         */
        public boolean sendOnError = false;
        /**
         * See http://www.owasp.org/index.php/HttpOnly
         */
        public boolean httpOnly = false;
    }

    /**
     * An HTTP Request
     */
    public static class Request implements Serializable {
        /**
         * Full action (ex: Application.index)
         */
        public String action;
        /**
         * ActionInvoker.resolvedRoutes was called?
         */
        boolean resolved;
        /**
         * Body stream
         */
        public transient InputStream body;
        /**
         * Bind to thread
         */
        public static ThreadLocal<Request> current = new ThreadLocal<Request>();
        /**
         * The really invoker Java methid
         */
        public transient Method invokedMethod;
        /**
         * The invoked controller class
         */
        public transient Class<? extends Controller> controllerClass;
        /**
         * Free space to store your request specific data
         */
        public Map<String, Object> args = new HashMap<String, Object>(16);
        /**
         * When the request has been received
         */
        public Date date = new Date();
        /**
         * New request or already submitted
         */
        public boolean isNew = true;


        /**
         * Deprecate the default constructor to encourage the use of createRequest() when creating new
         * requests.
         *
         * Cannot hide it with protected because we have to be backward compatible with modules - ie PlayGrizzlyAdapter.java
         */
        @Deprecated
        public Request() {
        }

        /**
         * All creation / initing of new requests should use this method.
         * The purpose of this is to "show" what is needed when creating new Requests.
         * @return the newly created Request object
         */
        public static Request createRequest(
                String _remoteAddress,
                String _method,
                String _path,
                String _querystring,
                String _contentType,
                InputStream _body,
                String _url,
                String _host,
                boolean _isLoopback,
                int _port,
                String _domain,
                boolean _secure,
                Map<String, Http.Header> _headers,
                Map<String, Http.Cookie> _cookies
        ) {
            Request newRequest = new Request();


            newRequest.body = _body;
            return newRequest;
        }

        /**
         * Retrieve the current request
         * @return the current request
         */
        public static Request current() {
            return current.get();
        }

        /**
         * Useful because we sometime use a lazy request loader
         * @return itself
         */
        public Request get() {
            return this;
        }

    }

    /**
     * An HTTP response
     */
    public static class Response {
        /**
         * Send this file directly
         */
        public Object direct;
        /**
         * Response body stream
         */
        public ByteArrayOutputStream out;
        /**
         * Bind to thread
         */
        public static ThreadLocal<Response> current = new ThreadLocal<Response>();

        /**
         * Retrieve the current response
         * @return the current response
         */
        public static Response current() {
            return current.get();
        }


        public void reset() {
            out.reset();
        }
    }

    /**
     * A Websocket Outbound channel
     */
    public static abstract class Outbound {

        public static ThreadLocal<Outbound> current = new ThreadLocal<Outbound>();

        public static Outbound current() {
            return current.get();
        }

        public abstract void send(String data);

        public abstract void send(byte opcode, byte[] data, int offset, int length);

        public abstract boolean isOpen();

        public abstract void close();

        public void send(byte opcode, byte[] data) {
            send(opcode, data, 0, data.length);
        }

        public void send(String pattern, Object... args) {
            send(String.format(pattern, args));
        }

        public void sendJson(Object o) {
            send(new Gson().toJson(o));
        }
    }
}
