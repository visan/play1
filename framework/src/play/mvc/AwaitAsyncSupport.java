package play.mvc;

import play.libs.F;

import java.util.concurrent.Future;

/**
 * Created by adg on 11.04.2015.
 */
public interface AwaitAsyncSupport {
    void awaitAsync(int millis, F.Action0 callback);

    <T> T awaitAsync(Future<T> future);

    <T> void awaitAsync(Future<T> future, F.Action<T> callback);

    void awaitAsync(String timeout);

    void awaitAsync(String timeout, F.Action0 callback);

    void awaitAsync(int millis);
}
