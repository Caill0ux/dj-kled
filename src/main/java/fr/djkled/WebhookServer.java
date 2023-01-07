package fr.djkled;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public class WebhookServer {
    public static void StartServer(){
        final Server server = new Server(8080);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
                System.out.println("Received request: " + request.getRequestURI());
                JSONObject json = new JSONObject(request.getRequestURI());

                String intentName = json.getJSONObject("intent").getString("name");
                switch (intentName){
                    case "AddMusic"-> {
                        BotManager.Connect((VoiceChannel) BotManager.tackedMember.getVoiceState(), Objects.requireNonNull(BotManager.tackedMember.getVoiceState()).getGuild().getAudioManager());
                        BotManager.AddMusic("https://www.youtube.com/watch?v=" + BotManager.YoutubeSearch(json.getJSONObject("intent").getString("query")));
                    }
                    case "ClearMusic"-> PlaylistManager.Clear();
                    case "Disconnect"-> BotManager.Disconnect();
                    case "LoopMusic"-> PlaylistManager.Loop();
                    case "MixMusic"-> {
                        BotManager.Connect((VoiceChannel) BotManager.tackedMember.getVoiceState(), Objects.requireNonNull(BotManager.tackedMember.getVoiceState()).getGuild().getAudioManager());
                        if(PlaylistManager.isMixing){
                            PlaylistManager.Mixing();
                            PlaylistManager.isMixing = false;
                            return;
                        }
                        BotManager.AddMusic("https://www.youtube.com/playlist?list=PLI7R7C130Vbo0_v30onp9RE1K2ceyuk9n");
                    }
                    case "PauseMusic"-> PlaylistManager.Pause();
                    case "ShuffleMusic"-> PlaylistManager.Shuffle();
                    case "SkipMusic"-> PlaylistManager.Skip();
                }
                json = new JSONObject();
                json.put("session", new JSONObject().put("id", "example_session_id").put("params", new JSONObject()));
                json.put("prompt", new JSONObject().put("override", false).put("firstSimple", new JSONObject().put("speech", "Hello World.").put("text", "")));
                json.put("scene", new JSONObject().put("name", "SceneName").put("slots", new JSONObject()).put("next", new JSONObject().put("name", "actions.scene.END_CONVERSATION")));
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.setContentType("application/json");
                httpServletResponse.getWriter().println(json);
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