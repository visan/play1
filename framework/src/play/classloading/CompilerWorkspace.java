package play.classloading;

/**
 * Created by aliev on 03/08/14
 */
public interface CompilerWorkspace {
  String getJavaSource(String clazzName);

  boolean containsApplicationClass(String name);

  byte[] getJavaByteCode(String name);

  void setCompiledBytes(String name, byte[] bytes);
}
