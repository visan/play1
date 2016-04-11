package play.proto;

import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.bytecode.StackRecorder;
import play.Invoker;
import play.Play;
import play.classloading.enhancers.ContinuationEnhancer;
import play.exceptions.ContinuationsException;
import play.exceptions.UnexpectedException;
import play.libs.F;
import play.libs.Time;
import play.mvc.Controller;
import play.mvc.Proto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by adg on 25.02.2015.
 */
public class Rx {

    private final static Map<Integer, Future> futureMap = new ConcurrentHashMap<Integer, Future>();

    public static <T> T awaitAsync(Future<T> future) {
//        return Controller.awaitProto(future);

        com.google.protobuf.MessageLite reqMsg= Proto.getLocalThreadRequest();

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
//            storeOrRestoreDataStateForContinuations( true );
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

}
