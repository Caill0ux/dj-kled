package fr.djkled;


import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import java.io.IOException;

public class WebhookServer {
    public static void StartServer(){
        final Server server = new Server(8080);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, jakarta.servlet.http.HttpServletRequest httpServletRequest, jakarta.servlet.http.HttpServletResponse httpServletResponse){
                System.out.println("Received request: " + request.getRequestURI());
                JSONObject json = new JSONObject(request.getRequestURI());

                String intentName = json.getJSONObject("intent").getString("name");
                String query = json.getJSONObject("intent").getString("query");

            }
        });

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}