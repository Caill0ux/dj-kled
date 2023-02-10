package fr.djkled;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public class WebhookServer extends Thread{
    public void run(){
        System.out.println("Server running...");
        StartServer(BotManager.KeyStorePath, BotManager.KeystorePassword, BotManager.KeyManagerPassword);
    }
    public void StartServer(String KeyStorePath, String KeystorePassword, String KeyManagerPassword){
        final Server server = new Server(8080);
        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(KeyStorePath);
        sslContextFactory.setKeyStorePassword(KeystorePassword);
        sslContextFactory.setKeyManagerPassword(KeyManagerPassword);

        ServerConnector sslConnector = new ServerConnector(server, sslContextFactory);
        sslConnector.setPort(443);

        server.addConnector(sslConnector);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
                System.out.println("Request received ... ");
                JSONObject json = new JSONObject(request.getRequestURI());

                String intentName = json.getJSONObject("intent").getString("name");
                switch (intentName){
                    case "AddMusic"-> BotManager.AddMusic((VoiceChannel) BotManager.tackedMember.getVoiceState(),Objects.requireNonNull(BotManager.tackedMember.getVoiceState()).getGuild().getAudioManager(),json.getJSONObject("intent").getJSONObject("params").getJSONObject("MusicName").getString("original"));
                    case "ClearMusic"-> PlaylistManager.Clear();
                    case "Disconnect"-> BotManager.Disconnect();
                    case "LoopMusic"-> PlaylistManager.Loop();
                    case "MixMusic"-> BotManager.Mixing((VoiceChannel)BotManager.tackedMember.getVoiceState(), BotManager.tackedMember.getGuild().getAudioManager());
                    case "PauseMusic"-> PlaylistManager.Pause();
                    case "ShuffleMusic"-> PlaylistManager.Shuffle();
                    case "SkipMusic"-> PlaylistManager.Skip();
                }
                String sessionID = json.getJSONObject("session").getString("id");
                String sceneName = json.getJSONObject("scene").getString("name");
                json = new JSONObject();
                json.put("session", new JSONObject().put("id", sessionID).put("params", new JSONObject()));
                json.put("prompt", new JSONObject().put("override", false).put("firstSimple", new JSONObject().put("speech", "C'est bon!").put("text", "")));
                json.put("scene", new JSONObject().put("name", sceneName).put("slots", new JSONObject()).put("next", new JSONObject().put("name", "actions.scene.END_CONVERSATION")));
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("application/json");
                httpServletResponse.getWriter().println(json);
                System.out.println("Response sent.");
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