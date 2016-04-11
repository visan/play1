package play.proto;

import play.libs.F;

import java.util.concurrent.Future;

/**
 * Created by adg on 11.04.2015.
 */
public interface AwaitAsyncSupport {
    <T> T awaitAsync(Future<T> future);
}
