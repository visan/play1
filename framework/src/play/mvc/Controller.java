package play.mvc;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.MessageLite;
import org.w3c.dom.Document;

import play.Invoker.Suspend;
import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ContinuationEnhancer;
//import play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.exceptions.*;
import play.libs.Time;
import play.mvc.Http.Request;
import play.mvc.results.Result;
import play.utils.Default;
import play.utils.Java;
import play.vfs.VirtualFile;

import java.lang.reflect.Type;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.libs.F;

import javax.management.RuntimeErrorException;

/**
 * Application controller support: The controller receives input and initiates a response by making calls on model objects.
 *
 * This is the class that your controllers should extend.
 * 
 */
public class Controller implements ControllerSupport {

    /**
     * The current HTTP request: the message sent by the client to the server.
     *
     * Note: The ControllersEnhancer makes sure that an appropriate thread local version is applied.
     * ie : controller.request - controller.request.current()
     *
     */
    protected static Http.Request request = null;
    /**
     * The current HTTP response: The message sent back from the server after a request.
     *
     * Note: The ControllersEnhancer makes sure that an appropriate thread local version is applied.
     * ie : controller.response - controller.response.current()
     *
     */
    protected static Http.Response response = null;


    /**
     * Retrieve annotation for the action method
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getActionAnnotation(Class<T> clazz) {
//        Method m = (Method) ActionInvoker.getActionMethod(Http.Request.current().action)[1];
//        if (m.isAnnotationPresent(clazz)) {
//            return m.getAnnotation(clazz);
//        }
        return null;
    }

    /**
     * Retrieve annotation for the controller class
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getControllerAnnotation(Class<T> clazz) {
        if (getControllerClass().isAnnotationPresent(clazz)) {
            return getControllerClass().getAnnotation(clazz);
        }
        return null;
    }

    /**
     * Retrieve annotation for the controller class
     * @param clazz The annotation class
     * @return Annotation object or null if not found
     */
    protected static <T extends Annotation> T getControllerInheritedAnnotation(Class<T> clazz) {
        Class<?> c = getControllerClass();
        while (!c.equals(Object.class)) {
            if (c.isAnnotationPresent(clazz)) {
                return c.getAnnotation(clazz);
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /**
     * Retrieve the controller class
     * @return Annotation object or null if not found
     */
    protected static Class<? extends Controller> getControllerClass() {
        return Http.Request.current().controllerClass;
    }



    /**
     * Suspend the current request for a specified amount of time.
     *
     * <p><b>Important:</b> The method will not resume on the line after you call this. The method will
     * be called again as if there was a new HTTP request.
     *
     * @param timeout Period of time to wait, e.g. "1h" means 1 hour.
     */
    @Deprecated
    protected static void suspend(String timeout) {
        suspend(1000 * Time.parseDuration(timeout));
    }

    /**
     * Suspend the current request for a specified amount of time (in milliseconds).
     *
     * <p><b>Important:</b> The method will not resume on the line after you call this. The method will
     * be called again as if there was a new HTTP request.
     *
     * @param millis Number of milliseconds to wait until trying again.
     */
    @Deprecated
    protected static void suspend(int millis) {
        Request.current().isNew = false;
        throw new Suspend(millis);
    }

    /**
     * Suspend this request and wait for the task completion
     *
     * <p><b>Important:</b> The method will not resume on the line after you call this. The method will
     * be called again as if there was a new HTTP request.
     *
     * @param task
     */
    @Deprecated
    protected static void waitFor(Future<?> task) {
        Request.current().isNew = false;
        throw new Suspend(task);
    }

    protected static void await(String timeout) {
        await(1000 * Time.parseDuration(timeout));
    }

    protected static void await(String timeout, F.Action0 callback) {
        await(1000 * Time.parseDuration(timeout), callback);
    }

    protected static void await(int millis) {
        Request.current().isNew = false;
        verifyContinuationsEnhancement();
        storeOrRestoreDataStateForContinuations(null);
        Continuation.suspend(millis);
    }

    /**
     * Used to store data before Continuation suspend and restore after.
     *
     * If isRestoring == null, the method will try to resolve it.
     *
     * important: when using isRestoring == null you have to KNOW that continuation suspend
     * is going to happen and that this method is called twice for this single
     * continuation suspend operation for this specific request.
     *
     * @param isRestoring true if restoring, false if storing, and null if you don't know
     */
    private static void storeOrRestoreDataStateForContinuations(Boolean isRestoring) {

        if (isRestoring==null) {
            // Sometimes, due to how continuations suspends/restarts the code, we do not
            // know when calling this method if we're suspending or restoring.

            final String continuationStateKey = "__storeOrRestoreDataStateForContinuations_started";
            if ( Http.Request.current().args.remove(continuationStateKey)!=null ) {
                isRestoring = true;
            } else {
                Http.Request.current().args.put(continuationStateKey, true);
                isRestoring = false;
            }
        }

        if (isRestoring) {
            //we are restoring after suspend

//            // localVariablesState
//            Stack<MethodExecution> currentMethodExecutions = (Stack<MethodExecution>) Request.current().args.get(ActionInvoker.CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES);
//            if(currentMethodExecutions != null)
//                LVEnhancer.LVEnhancerRuntime.reinitRuntime(currentMethodExecutions);

//            // renderArgs
//            Scope.RenderArgs renderArgs = (Scope.RenderArgs) Request.current().args.remove(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS);
//            Scope.RenderArgs.current.set( renderArgs);
//
//            // Params
//            // We know that the params are partially reprocessed during awake(Before now), but here we restore the correct values as
//            // they where when we performed the await();
//            Map params = (Map) Request.current().args.remove(ActionInvoker.CONTINUATIONS_STORE_PARAMS);
//            Scope.Params.current().all().clear();
//            Scope.Params.current().all().putAll(params);
//
//            // Validations
//            Validation validation = (Validation) Request.current().args.remove(ActionInvoker.CONTINUATIONS_STORE_VALIDATIONS);
//            Validation.current.set(validation);
//            ValidationPlugin.keys.set( (Map<Object, String>) Request.current().args.remove(ActionInvoker.CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS) );

        } else {
            // we are storing before suspend

//            // localVariablesState
//            Stack<MethodExecution> currentMethodExecutions = new Stack<LVEnhancer.MethodExecution>();
//            currentMethodExecutions.addAll(LVEnhancer.LVEnhancerRuntime.getCurrentMethodParams());
//            Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_LOCAL_VARIABLE_NAMES, currentMethodExecutions);

//            // renderArgs
//            Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS, Scope.RenderArgs.current());
//
//             // Params
//             // Store the actual params values so we can restore the exact same state when awaking.
//             Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_PARAMS, new HashMap(Scope.Params.current().data));
//
//            // Validations
//            Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_VALIDATIONS, Validation.current());
//            Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_VALIDATIONPLUGIN_KEYS, ValidationPlugin.keys.get());


        }
    }

    protected static void await(int millis, F.Action0 callback) {
        Request.current().isNew = false;
        Request.current().args.put(ActionInvoker.A, callback);
//        Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS, Scope.RenderArgs.current());
        throw new Suspend(millis);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T await(Future<T> future) {

        if(future != null) {
            Request.current().args.put(ActionInvoker.F, future);
        } else if(Request.current().args.containsKey(ActionInvoker.F)) {
            // Since the continuation will restart in this code that isn't intstrumented by javaflow,
            // we need to reset the state manually.
            StackRecorder.get().isCapturing = false;
            StackRecorder.get().isRestoring = false;
            StackRecorder.get().value = null;
            future = (Future<T>)Request.current().args.get(ActionInvoker.F);

            // Now reset the Controller invocation context
//            ControllerInstrumentation.stopActionCall();
            storeOrRestoreDataStateForContinuations( true );
        } else {
            throw new UnexpectedException("Lost promise for " + Http.Request.current() + "!");
        }
        
        if(future.isDone()) {
            try {
                return future.get();
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        } else {
            Request.current().isNew = false;
            verifyContinuationsEnhancement();
            storeOrRestoreDataStateForContinuations( false );
            Continuation.suspend(future);
            return null;
        }
    }
    private final static Map<Integer, Future> futureMap = new ConcurrentHashMap<Integer, Future>();

    @SuppressWarnings("unchecked")
    public static <T> T awaitProto(Future<T> future) {
        if(1==1) return null;
        com.google.protobuf.MessageLite reqMsg=Proto.getLocalThreadRequest();

        if(future != null) {
            futureMap.put(reqMsg.hashCode(), future);
        } else if(futureMap.containsKey(reqMsg.hashCode())) {
            // Since the continuation will restart in this code that isn't intstrumented by javaflow,
            // we need to reset the state manually.
            StackRecorder.get().isCapturing = false;
            StackRecorder.get().isRestoring = false;
            StackRecorder.get().value = null;
            future = (Future<T>)futureMap.get(reqMsg.hashCode());

            // Now reset the Controller invocation context
//            ControllerInstrumentation.stopActionCall();
            storeOrRestoreDataStateForContinuations( true );
        } else {
            throw new UnexpectedException("Lost promise for " + reqMsg.toString() + "!");
        }

        if(future.isDone()) {
            try {
                futureMap.remove(reqMsg.hashCode());
                return future.get();
            } catch(Exception e) {
                throw new UnexpectedException(e);
            }
        } else {
//            Request.current().isNew = false;
            verifyContinuationsEnhancement();
//            storeOrRestoreDataStateForContinuations( false );
            Continuation.suspend(future);
            return null;
        }
    }

    /**
     * Verifies that all application-code is properly enhanched.
     * "application code" is the code on the callstack after leaving actionInvoke into the app, and before reentering Controller.await
     */
    private static void verifyContinuationsEnhancement() {
        // only check in dev mode..
        if (Play.mode == Play.Mode.PROD) {
            return;
        }
        
        try {
            throw new Exception();
        } catch (Exception e) {
            boolean haveSeenFirstApplicationClass = false;
            for (StackTraceElement ste : e.getStackTrace() ) {
                String className = ste.getClassName();

                if (!haveSeenFirstApplicationClass) {
                    haveSeenFirstApplicationClass = Play.classes.getApplicationClass(className) != null;
                    // when haveSeenFirstApplicationClass is set to true, we are entering the user application code..
                }

                if (haveSeenFirstApplicationClass) {
                    if (className.startsWith("sun.") || className.startsWith("play.")) {
                        // we're back into the play framework code...
                        return ; // done checking
                    } else {
                        // is this class enhanched?
                        boolean enhanced = ContinuationEnhancer.isEnhanced(className);
                        if (!enhanced) {
                            throw new ContinuationsException("Cannot use await/continuations when not all application classes on the callstack are properly enhanced. The following class is not enhanced: " + className);
                        }
                    }
                }
            }

        }
    }

    protected static <T> void await(Future<T> future, F.Action<T> callback) {
        Request.current().isNew = false;
        Request.current().args.put(ActionInvoker.F, future);
        Request.current().args.put(ActionInvoker.A, callback);
//        Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS, Scope.RenderArgs.current());
        throw new Suspend(future);
    }

//    /**
//     * Don't use this directly if you don't know why
//     */
//    public static ThreadLocal<ActionDefinition> _currentReverse = new ThreadLocal<ActionDefinition>();
//
//    /**
//     * @todo - this "Usage" example below doesn't make sense.
//     *
//     * Usage:
//     *
//     * <code>
//     * ActionDefinition action = reverse(); {
//     *     Application.anyAction(anyParam, "toto");
//     * }
//     * String url = action.url;
//     * </code>
//     */
//    protected static ActionDefinition reverse() {
//        ActionDefinition actionDefinition = new ActionDefinition();
//        _currentReverse.set(actionDefinition);
//        return actionDefinition;
//    }
}
