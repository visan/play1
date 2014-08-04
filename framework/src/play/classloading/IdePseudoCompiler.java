package play.classloading;

import play.Logger;
import play.Play;
import play.libs.IO;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.*;

/**
 * Created by aliev on 03/08/14
 */
public class IdePseudoCompiler implements ApplicationCompiler {

  private final CompilerWorkspace compilerWorkspace;
  Map<String, Boolean> packagesCache = new HashMap<String, Boolean>();
  //  ApplicationClasses applicationClasses;
  Map<String, String> settings;
  private final Properties applicationConfig;
  private final EclipseJdtApplicationCompiler realCompiler;


  public IdePseudoCompiler(CompilerWorkspace compilerWorkspace, Properties applicationConfig) {
    this.compilerWorkspace = compilerWorkspace;
    this.applicationConfig = applicationConfig;
    realCompiler = new EclipseJdtApplicationCompiler(compilerWorkspace);
    init();
  }

  public static List<VirtualFile> ideOutputPaths = new ArrayList(20);

  @Override
  public void compile(String[] classNames) {
    dumpCompilationRequest(classNames);

//    proxyToEclipseJdt(classNames);
    doIdePseudeCompile(classNames);
  }

  private void dumpCompilationRequest(String[] classNames) {
    StringBuilder builder = new StringBuilder();
    for (String className : classNames) {
      builder.append(className).append("\n");
    }
    System.out.println("IdePseudoCompiler compiles:\n" + builder.toString());
  }

  private void doIdePseudeCompile(String[] classNames) {
    for (VirtualFile ideOutput : ideOutputPaths) {
      scanOutput(ideOutput);
    }
  }

  private void scanOutput(VirtualFile ideOutput) {
    List<ApplicationClasses.ApplicationClass> applicationClasses = new ArrayList<ApplicationClasses.ApplicationClass>();
    scanPrecompiled(applicationClasses, "", ideOutput/*Play.getVirtualFile("precompiled/java")*/);
//    Play.classes.clear();
//    for (ApplicationClasses.ApplicationClass applicationClass : applicationClasses) {
//      Play.classes.add(applicationClass);
//      Class clazz = loadApplicationClass(applicationClass.name);
//      applicationClass.javaClass = clazz;
//      applicationClass.compiled = true;
//      allClasses.add(clazz);
//    }

  }

  void scanPrecompiled(List<ApplicationClasses.ApplicationClass> classes, String packageName, VirtualFile current) {
    if (!current.isDirectory()) {
      if (current.getName().endsWith(".class") && !current.getName().startsWith(".")) {
        String classname = packageName.substring(packageName.indexOf(".")+1) + current.getName().substring(0, current.getName().length() - 6);
//        classes.add(new ApplicationClasses.ApplicationClass(classname));
//        byte[] classBytes = loadClassBytes(classname);
        if (compilerWorkspace.containsApplicationClass(classname)) {
          compilerWorkspace.setCompiledBytes(classname, current.content());
        }
      }
    } else {
      for (VirtualFile currentDir : current.list()) {
        String nextPackageName = packageName + current.getName() + ".";
        scanPrecompiled(classes, nextPackageName, currentDir);
      }
    }
  }

//  private byte[] loadClassBytes(VirtualFile classFile) {
//    classFile.content();
//    File file = Play.getFile("precompiled/java/" + name.replace(".", "/") + ".class");
//    if (!file.exists()) {
//      return null;
//    }
//    byte[] code = IO.readContent(file);
//    return code;
//  }
//

  private void proxyToEclipseJdt(String[] classNames) {
    dumpCompilationRequest(classNames);
    realCompiler.compile(classNames);
//    System.out.print(".");
  }

  private void init() {
    String ideOutputs = System.getenv("IDE_COMPILATION_OUTPUTS");
    if (ideOutputs != null && ideOutputs.trim().length() > 0) {
      for (String m : ideOutputs.split(System.getProperty("os.name").startsWith("Windows") ? ";" : ":")) {
        File modulePath = new File(m);
        if (!modulePath.exists() || !modulePath.isDirectory()) {
          Logger.error("Ide output %s will not be loaded because %s does not exist", modulePath.getName(), modulePath.getAbsolutePath());
        } else {
//          final String modulePathName = modulePath.getName();
//          final String moduleName = modulePathName.contains("-") ?
//              modulePathName.substring(0, modulePathName.lastIndexOf("-")) :
//              modulePathName;
          VirtualFile ideOutput = VirtualFile.open(modulePath);
          ideOutputPaths.add(ideOutput);
        }
      }
    }

  }
}
