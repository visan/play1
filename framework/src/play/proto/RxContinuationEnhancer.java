package play.proto;

import javassist.CtClass;
import org.apache.commons.javaflow.bytecode.transformation.asm.AsmClassTransformer;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.EnhancedForContinuations;
import play.classloading.enhancers.Enhancer;

import java.io.InputStream;

public class RxContinuationEnhancer extends Enhancer {

    public static boolean isEnhanced(String appClassName) {
        ApplicationClass appClass = Play.classes.getApplicationClass(appClassName);
        if (appClass == null) {
            return false;
        }

        // All classes enhanced for Continuations are implementing the interface EnhancedForContinuations
        return EnhancedForContinuations.class.isAssignableFrom(appClass.javaClass);
    }

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
//        if (isScala(applicationClass)) {
//            return;
//        }

        CtClass ctClass = makeClass(applicationClass);

//        if (!ctClass.subtypeOf(classPool.get(ControllersEnhancer.ControllerSupport.class.getName()))) {
//            return ;
//        }


        boolean needsContinuations = shouldEnhance(ctClass);

        if (!needsContinuations) {
            return;
        }


        // To be able to runtime detect if a class is enhanced for Continuations,
        // we add the interface EnhancedForContinuations to the class
        CtClass enhancedForContinuationsInterface;
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("play/classloading/enhancers/EnhancedForContinuations.class");
            try {
                enhancedForContinuationsInterface = classPool.makeClass(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ctClass.addInterface(enhancedForContinuationsInterface);

        // Apply continuations
        applicationClass.enhancedByteCode = new AsmClassTransformer().transform(ctClass.toBytecode());

        ctClass.defrost();
        enhancedForContinuationsInterface.defrost();
    }

    private boolean shouldEnhance(CtClass ctClass) throws Exception {

        if (ctClass == null || ctClass.getPackageName() == null || ctClass.getPackageName().startsWith("play.")|| ctClass.getPackageName().startsWith("ap.")) {
            // If we have not found any await-usage yet, we return false..
            return false;
        }

        boolean needsContinuations = false;
        needsContinuations = ctClass.hasAnnotation(AwaitAsync.class);

        if (!needsContinuations) {
            // Check parent class
            needsContinuations = shouldEnhance(ctClass.getSuperclass());
        }

        return needsContinuations;

    }


}
