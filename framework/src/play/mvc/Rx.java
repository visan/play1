package play.mvc;

import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.Invoker;
import play.Play;
import play.classloading.enhancers.ContinuationEnhancer;
import play.exceptions.ContinuationsException;
import play.exceptions.UnexpectedException;
import play.libs.F;
import play.libs.Time;

import java.util.concurrent.Future;

/**
 * Created by adg on 25.02.2015.
 */
public class Rx {
     void awaitAsync(int millis, F.Action0 callback) {
        Http.Request.current().isNew = false;
        Http.Request.current().args.put(ActionInvoker.A, callback);
//        Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS, Scope.RenderArgs.current());
        throw new Invoker.Suspend(millis);
    }

    public static <T> T awaitAsync(Future<T> future) {

        if(future != null) {
            Http.Request.current().args.put(ActionInvoker.F, future);
        } else if(Http.Request.current().args.containsKey(ActionInvoker.F)) {
            // Since the continuation will restart in this code that isn't intstrumented by javaflow,
            // we need to reset the state manually.
            StackRecorder.get().isCapturing = false;
            StackRecorder.get().isRestoring = false;
            StackRecorder.get().value = null;
            future = (Future<T>) Http.Request.current().args.get(ActionInvoker.F);

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
            Http.Request.current().isNew = false;
            verifyContinuationsEnhancement();
            storeOrRestoreDataStateForContinuations( false );
            Continuation.suspend(future);
            return null;
        }
    }

     <T> void awaitAsync(Future<T> future, F.Action<T> callback) {
        Http.Request.current().isNew = false;
        Http.Request.current().args.put(ActionInvoker.F, future);
        Http.Request.current().args.put(ActionInvoker.A, callback);
//        Request.current().args.put(ActionInvoker.CONTINUATIONS_STORE_RENDER_ARGS, Scope.RenderArgs.current());
        throw new Invoker.Suspend(future);
    }

     void awaitAsync(String timeout) {
        awaitAsync(1000 * Time.parseDuration(timeout));
    }

     void awaitAsync(String timeout, F.Action0 callback) {
        awaitAsync(1000 * Time.parseDuration(timeout), callback);
    }

     void awaitAsync(int millis) {
        Http.Request.current().isNew = false;
        verifyContinuationsEnhancement();
        storeOrRestoreDataStateForContinuations(null);
        Continuation.suspend(millis);
    }
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
}
