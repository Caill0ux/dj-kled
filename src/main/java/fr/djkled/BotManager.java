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
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BotManager extends ListenerAdapter {
    static AudioPlayerManager playerManager;
    MessageChannel channel = null;
    VoiceChannel voiceChannel = null;
    static AudioManager audioManager;
    static AudioPlayer audioPlayer;
    PlaylistManager playlistManager;

    public static void main(String[] args) throws LoginException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("src/main/resources/config.properties"));
        String apiKey = prop.getProperty("apiKey");
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
                channel.sendMessage("Mixing! :cd: :notes:").queue();
                if (voiceChannel != null) {
                    Connect(voiceChannel, event.getGuild().getAudioManager());
                }
                AddMusic("https://www.youtube.com/playlist?list=PLI7R7C130Vbo0_v30onp9RE1K2ceyuk9n");
                playlistManager.Shuffle();
            }

        }

        if(message.startsWith("!k ")){
            VoiceChannel voiceChannel = (VoiceChannel) event.getMember().getVoiceState().getChannel();
            if(voiceChannel == null) {
                channel.sendMessage("You need to be in a Voice Channel to let me in!").queue();
                return;
            }
            String search = event.getMessage().getContentRaw().substring(3);
            channel.sendMessage("Roger That!").queue();
            if(voiceChannel != null){
                Connect(voiceChannel, event.getGuild().getAudioManager());
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

    public void AddMusic(String search){
        playerManager.loadItem(search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                playlistManager.Queue(track);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                playlistManager.Clear();
                for(AudioTrack track : audioPlaylist.getTracks()){
                    playlistManager.Queue(track);
                }
                //playlistManager.Shuffle();
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


   /* public String YoutubeSearch(String query, String APIKEY) throws IOException {
        URL url = new URL("https://www.googleapis.com/youtube/v3/search?key="+ APIKEY + "&type=video&part=snippet&maxResults=1&q=" + query);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream response = connection.getInputStream();
        var videoID = ;

        return videoID;
    }*/
}
