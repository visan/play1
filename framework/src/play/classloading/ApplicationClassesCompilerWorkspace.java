package play.classloading;

/**
 * Created by aliev on 03/08/14
 */
public class ApplicationClassesCompilerWorkspace implements CompilerWorkspace {
  private ApplicationClasses applicationClasses;

  public ApplicationClassesCompilerWorkspace() {
  }

  public ApplicationClassesCompilerWorkspace(ApplicationClasses applicationClasses) {
    this.applicationClasses = applicationClasses;
  }

  public void setClasses(ApplicationClasses classes) {
    this.applicationClasses = classes;
  }

  @Override
  public String getJavaSource(String clazzName) {
    return applicationClasses.getApplicationClass(clazzName).javaSource;
  }

  @Override
  public boolean containsApplicationClass(String name) {
    return applicationClasses.getApplicationClass(name) != null;
  }

  @Override
  public byte[] getJavaByteCode(String name) {
    System.out.println("gjbc: " + name + " " + (applicationClasses.getApplicationClass(name).javaByteCode == null ? "null" : "[bytes]"));
    return applicationClasses.getApplicationClass(name).javaByteCode;
  }

  @Override
  public void setCompiledBytes(String name, byte[] bytes) {
    System.out.println("scby: " + name);
    applicationClasses.getApplicationClass(name).compiled(bytes);
  }
}
