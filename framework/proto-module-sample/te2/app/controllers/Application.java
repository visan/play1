package controllers;

import adg.Core;
import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.proto.AwaitAsync;
import play.proto.Rx;

@AwaitAsync
public class Application{
    private static MessageProcessor processor = new MessageProcessor();

    public static void index() {
        System.out.println("STEP 1======================");
        Logger.info("Hi from controller.");
        String url = "http://echo.jsontest.com/some-adg/insert-value-here/key/" + System.nanoTime();
        System.out.println("STEP 2======================");
        F.Promise<WS.HttpResponse> promise = WS.url(url).getAsync();
        System.out.println("STEP 3======================");
//        String result = await(promise).getString();
        System.out.println("STEP 4======================");
//        Logger.info("Lenta: " + result);

//        processor.doWork();


//        System.out.println("STEP 5======================");
//        F.Promise<String> jprom = new Job<String>() {
//            @Override
//            public String doJobWithResult() throws Exception {
//                System.out.println("STEP 6======================");
//                Thread.sleep(3000);
//                System.out.println("STEP 7======================");
//                return "result from job";
//            }
//        }.now();
//        System.out.println("STEP 8======================");
//        String jobRes = await(jprom);
//        System.out.println("STEP 9======================");
//
//        Logger.info("Job result: " + jobRes);
//        System.out.println("10======================");
//        System.out.println(request.action);

    }
    public static void processMessage(Core.Msg reqMsg, Core.Msg.Builder resMsg){
        System.out.println("STEP PROTO 1======================");
        Logger.info("Hi from controller.");
        String url = "http://echo.jsontest.com/some-adg/insert-value-here/key/" + System.nanoTime();
        System.out.println("STEP PROTO 2======================");
        F.Promise<WS.HttpResponse> promise = WS.url(url).getAsync();
        System.out.println("STEP PROTO 3======================");
        String result = Rx.awaitAsync(promise).getString();
        System.out.println("STEP PROTO 4======================");
        Logger.info("Lenta: " + result);
        resMsg.setId(reqMsg.getId() + 1);
        resMsg.setText(result);
    }


}