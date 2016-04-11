package play.mvc;

import com.google.protobuf.MessageLite;

/**
 * Created by adg on 23.04.2015.
 */
public class Proto {
    private static ThreadLocal<MessageLite> localThreadRequest = new ThreadLocal();
    public static MessageLite getLocalThreadRequest() {
        return Proto.localThreadRequest.get();
    }

    public static void setLocalThreadRequest(MessageLite localThreadRequest) {
        Proto.localThreadRequest.set(localThreadRequest);
    }
}
