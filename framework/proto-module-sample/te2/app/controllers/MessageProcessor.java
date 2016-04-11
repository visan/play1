package controllers;


import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.proto.Rx;

/**
 * Created by adg on 30.01.2015.
 */
public class MessageProcessor {
    public void doWork() {
        System.out.println("STEP 1======================");
        Logger.info("Hi from controller.");
        String url = "http://echo.jsontest.com/some-adg/insert-value-here/key/" + System.nanoTime();
        System.out.println("STEP 2======================");
        F.Promise<WS.HttpResponse> promise = WS.url(url).getAsync();
        System.out.println("STEP 3======================");
        String result = Rx.awaitAsync(promise).getString();
        System.out.println("STEP 4======================");
        Logger.info("Lenta: " + result);

    }
}
