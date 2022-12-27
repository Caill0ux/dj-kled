package fr.djkled;

import com.fasterxml.jackson.core.JsonParser;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

public class BotManager extends ListenerAdapter {
    static AudioPlayerManager playerManager;
    MessageChannel channel = null;
    VoiceChannel voiceChannel = null;
    static AudioManager audioManager;
    static AudioPlayer audioPlayer;
    PlaylistManager playlistManager;
    static String YoutubeAPIKEY;

    public static void main(String[] args) throws LoginException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("src/main/resources/config.properties"));
        String apiKey = prop.getProperty("discordApiKey");
        YoutubeAPIKEY = prop.getProperty("youtubeApiKey");
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        JDA bot = JDABuilder.createDefault(apiKey)
                .setActivity(Activity.listening("No"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOJI, CacheFlag.MEMBER_OVERRIDES, CacheFlag.CLIENT_STATUS, CacheFlag.STICKER)
                .disableIntents(GatewayIntent.GUILD_PRESENCES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_INVITES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS,GatewayIntent.DIRECT_MESSAGE_TYPING)
                .addEventListeners(new BotManager())
                .build();
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        channel = event.getChannel();
        String message = event.getMessage().getContentRaw().toLowerCase();
        switch (message) {
            case "!skip", "!next", "!s", "!n" -> playlistManager.Skip();
            case "!loop", "!l" -> playlistManager.Loop();
            case "!rewind", "!r" -> playlistManager.Rewind();
            case "!pause", "!p" -> playlistManager.Pause();
            case "!disconnect", "!d" -> Disconnect();
            case "!random" -> playlistManager.Shuffle();
            case "!clear" -> playlistManager.Clear();
            case "!mix" -> {

                VoiceChannel voiceChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
                if (voiceChannel == null) {
                    channel.sendMessage("You need to be in a Voice Channel to let me in!").queue();
                    return;
                }
                Connect(voiceChannel, event.getGuild().getAudioManager());
                if(playlistManager.GetIsMixing()){
                    playlistManager.Mixing(false);
                    return;
                }
                AddMusic("https://www.youtube.com/playlist?list=PLI7R7C130Vbo0_v30onp9RE1K2ceyuk9n");
            }

        }
        if(message.startsWith("!k ")){
            VoiceChannel voiceChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
            if(voiceChannel == null) {
                channel.sendMessage("You need to be in a Voice Channel to let me in!").queue();
                return;
            }
            String search = event.getMessage().getContentRaw().substring(3);
            Connect(voiceChannel, event.getGuild().getAudioManager());
            if (!search.startsWith("https://www.yout")){
                try {
                    search = "https://www.youtube.com/watch?v=" + YoutubeSearch(search);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            AddMusic(search);
        }

    }

    public void Connect(VoiceChannel voiceChannel, AudioManager audioManager){
        audioManager.openAudioConnection(voiceChannel);
        audioPlayer = playerManager.createPlayer();
        playlistManager = new PlaylistManager(audioPlayer);
        audioPlayer.addListener(playlistManager);
        this.voiceChannel = voiceChannel;
        this.audioManager = audioManager;
    }

    public void Disconnect(){
        if(audioManager != null && voiceChannel != null){
            audioManager.closeAudioConnection();
            this.voiceChannel = null;
        }
    }

    public void AddMusic(String search) {

        playerManager.loadItem(search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                playlistManager.Queue(track);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                playlistManager.ClearPlaylistBuffer();
                for(AudioTrack track : audioPlaylist.getTracks()){
                    playlistManager.QueuePlaylist(track);
                }
                playlistManager.ShufflePlaylistBuffer();
                playlistManager.Mixing(true);
            }

            @Override
            public void noMatches() {
                channel.sendMessage(search + " Not find.").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                channel.sendMessage("Couldn't load " + search).queue();
            }
        });
    }
    static public void ChangeMusic(){
        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
    }


    static public String YoutubeSearch(String query) throws IOException {
        URL url = new URL("https://www.googleapis.com/youtube/v3/search?key="+ YoutubeAPIKEY + "&type=video&part=snippet&maxResults=1&q=" + URLEncoder.encode(query, "UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream response = connection.getInputStream();
        String jsonString = readInputStream(response);

        // Parse the JSON string into a JSONObject
        JSONObject json = new JSONObject(jsonString);

        // Get the items array
        JSONArray items = json.getJSONArray("items");

        // Get the first item in the array
        JSONObject item = items.getJSONObject(0);

        // Get the id object
        JSONObject id = item.getJSONObject("id");

        // Get the videoId
        String videoId = id.getString("videoId");

        return videoId;
    }

    private static String readInputStream(InputStream inputStream) {
    StringBuilder result = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
    } catch (IOException e) {
        // Handle exception
    }
    return result.toString();
    }


    static public void onWebHookReceived(){ /*
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(, "UTF-8"));
        ((JSONObject)jsonObject.get("intents")).get("query");

        YoutubeSearch(query)
    */}

}
