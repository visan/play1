package play.classloading;

/**
 * Created by aliev on 03/08/14
 */
public interface ApplicationCompiler {
  @SuppressWarnings("deprecation")
  void compile(String[] classNames);
}
