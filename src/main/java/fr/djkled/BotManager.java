package fr.djkled;

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
import net.dv8tion.jda.api.entities.Member;
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
import java.util.Objects;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BotManager extends ListenerAdapter {
    static AudioPlayerManager playerManager;
    static MessageChannel channel = null;
    static VoiceChannel voiceChannel = null;
    static PlaylistManager playlistManager;
    static Member tackedMember;
    static AudioManager audioManager;
    static AudioPlayer audioPlayer;
    static String YoutubeAPIKEY;
    public static void main(String[] args) throws LoginException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("src/main/resources/config.properties"));
        String apiKey = prop.getProperty("discordApiKey");
        YoutubeAPIKEY = prop.getProperty("youtubeApiKey");
        playerManager = new DefaultAudioPlayerManager();
        //WebhookServer.StartServer();
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
            case "!skip", "!next", "!s", "!n" -> PlaylistManager.Skip();
            case "!loop", "!l" -> PlaylistManager.Loop();
            case "!rewind", "!r" -> PlaylistManager.Rewind();
            case "!pause", "!p" -> PlaylistManager.Pause();
            case "!disconnect", "!d" -> Disconnect();
            case "!random" -> PlaylistManager.Shuffle();
            case "!clear" -> PlaylistManager.Clear();
            case "!track" -> tackedMember = event.getMember();
            case "!server" -> WebhookServer.StartServer();
            case "!mix" -> {

                VoiceChannel voiceChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
                if (voiceChannel == null) {
                    channel.sendMessage("You need to be in a Voice Channel to let me in!").queue();
                    return;
                }
                Connect(voiceChannel, event.getGuild().getAudioManager());
                if(PlaylistManager.isMixing){
                    PlaylistManager.Mixing();
                    PlaylistManager.isMixing = false;
                    return;
                }
                AddMusic("https://www.youtube.com/playlist?list=PLI7R7C130Vbo0_v30onp9RE1K2ceyuk9n");
            }

        }
        if(message.startsWith("!k ")){
            VoiceChannel voiceChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).getChannel();
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

    public static void Connect(VoiceChannel voiceChannel, AudioManager audioManager){
        audioManager.openAudioConnection(voiceChannel);
        audioPlayer = playerManager.createPlayer();
        playlistManager = new PlaylistManager(audioPlayer);
        audioPlayer.addListener(playlistManager);
        BotManager.voiceChannel = voiceChannel;
        BotManager.audioManager = audioManager;
    }

    public static void Disconnect(){
        if(audioManager != null && voiceChannel != null){
            audioManager.closeAudioConnection();
            voiceChannel = null;
        }
    }

    public static void AddMusic(String search) {

        playerManager.loadItem(search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                PlaylistManager.Queue(track);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                PlaylistManager.ClearPlaylistBuffer();
                for(AudioTrack track : audioPlaylist.getTracks()){
                    PlaylistManager.QueuePlaylist(track);
                }
                PlaylistManager.ShufflePlaylistBuffer();
                PlaylistManager.Mixing();
                PlaylistManager.isMixing = true;
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
        URL url = new URL("https://www.googleapis.com/youtube/v3/search?key="+ YoutubeAPIKEY + "&type=video&part=snippet&maxResults=1&q=" + URLEncoder.encode(query, UTF_8));
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

        return id.getString("videoId");
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

}
