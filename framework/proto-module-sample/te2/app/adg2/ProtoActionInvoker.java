package adg2;

import adg.Core;
import com.jamonapi.Monitor;
import controllers.Application;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.Invoker;
import play.Play;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.Proto;
import play.mvc.results.Result;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by adg on 13.04.2015.
 */
public class ProtoActionInvoker {
    public static void resolve(Core.Msg reqMsg, Core.Msg.Builder resMsg) {

        if (!Play.started) {
            return;
        }
        Proto.setLocalThreadRequest(reqMsg);
//        ControllersEnhancer.currentAction.set(new Stack<String>());

//        if (request.resolved) {
//            return;
//        }

        // Find the action method
//        try {
//            Method actionMethod = null;
////            Object[] ca = getActionMethod(request.action);
//            Object[] ca = getActionMethod("Application.processMessage");
//            actionMethod = (Method) ca[1];
//            request.controller = ((Class) ca[0]).getName().substring(12).replace("$", "");
//            request.controllerClass = ((Class) ca[0]);
//            request.actionMethod = actionMethod.getName();
//            request.action = request.controller + "." + request.actionMethod;
//            request.invokedMethod = actionMethod;
//
//            if (Logger.isTraceEnabled()) {
//                Logger.trace("------- %s", actionMethod);
//            }
//
//            request.resolved = true;
//
//        } catch (ActionNotFoundException e) {
//            Logger.error(e, "%s action not found", e.getAction());
//            throw new NotFound(String.format("%s action not found", e.getAction()));
//        }

    }

    public static void invoke(Core.Msg reqMsg, Core.Msg.Builder resMsg) {
        Monitor monitor = null;

        try {

//            resolve(request, response);
//            Method actionMethod = request.invokedMethod;
            Method[] methods = Application.class.getMethods();
            Method theMethod = null;
            for (Method m : methods) {
//                System.out.println(m.getName());
                if (m.getName().equals("processMessage")) theMethod = m;

            }
            if (theMethod == null) throw new RuntimeException("proto action method not found");
//            Method actionMethod = request.invokedMethod;
            Method actionMethod = theMethod;


//            ControllerInstrumentation.stopActionCall();
//            Play.pluginCollection.beforeActionInvocation(actionMethod);

//            // Monitoring
//            monitor = MonitorFactory.start(request.action + "()");

            invokeWithContinuation(actionMethod, null, reqMsg, resMsg);

//                monitor.stop();
//                monitor = null;

        } catch (Result result) {
//            Play.pluginCollection.onActionInvocationResult(result);

//            Play.pluginCollection.afterActionInvocation();

        } catch (PlayException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }


    static final String C = "__continuation";
    static final String A = "__callback";
    static final String F = "__future";
    static final String CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES = "__CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES";
    static final String CONTINUATIONS_STORE_RENDER_ARGS = "__CONTINUATIONS_STORE_RENDER_ARGS";
    static final String CONTINUATIONS_STORE_PARAMS = "__CONTINUATIONS_STORE_PARAMS";
    public static final String CONTINUATIONS_STORE_VALIDATIONS = "__CONTINUATIONS_STORE_VALIDATIONS";
    static final String CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS = "__CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS";
    private final static Map<Integer, Continuation> contMap = new ConcurrentHashMap<Integer, Continuation>();

    static Object invokeWithContinuation(Method method, Object instance, Core.Msg reqMsg, Core.Msg.Builder resMsg) throws Exception {
        Object[] realArgs = new Object[]{reqMsg, resMsg};

        // Continuations case
//        Continuation continuation = (Continuation) Http.Request.current().args.get(C);
        Continuation continuation = contMap.get(reqMsg.hashCode());
        if (continuation == null) {
            continuation = new Continuation(new StackRecorder((Runnable) null));
        }

        StackRecorder pStackRecorder = new StackRecorder(continuation.stackRecorder);
        Object result = null;

        final StackRecorder old = pStackRecorder.registerThread();
        try {
            pStackRecorder.isRestoring = !pStackRecorder.isEmpty();

            // Execute code
            result = method.invoke(instance, realArgs);

            if (pStackRecorder.isCapturing) {
                if (pStackRecorder.isEmpty()) {
                    throw new IllegalStateException("stack corruption. Is " + method + " instrumented for javaflow?");
                }
                Object trigger = pStackRecorder.value;
                Continuation nextContinuation = new Continuation(pStackRecorder);
//                Http.Request.current().args.put(C, nextContinuation);
                contMap.put(reqMsg.hashCode(), nextContinuation);

                if (trigger instanceof Long) {
                    throw new Invoker.Suspend((Long) trigger);
                }
                if (trigger instanceof Integer) {
                    throw new Invoker.Suspend(((Integer) trigger).longValue());
                }
                if (trigger instanceof Future) {
                    throw new Invoker.Suspend((Future) trigger);
                }

                throw new UnexpectedException("Unexpected continuation trigger -> " + trigger);
            } else {
                //Http.Request.current().args.remove(C);
                contMap.remove(reqMsg.hashCode());
            }
        } finally {
            pStackRecorder.deregisterThread(old);
        }

        return result;
    }
}

