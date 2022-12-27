package fr.djkled;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistManager extends AudioEventAdapter {
    static AudioPlayer audioPlayer;
    static List<AudioTrack> tracks = new ArrayList<AudioTrack>();
    static Integer trackPlaying = 0;
    static boolean isLooping;
    static boolean isPlaying;
    static boolean isMixing;
    static List<AudioTrack> playlistBuffer = new ArrayList<AudioTrack>();

    PlaylistManager(AudioPlayer audioPlayer){
        this.audioPlayer = audioPlayer;
        new AudioPlayerSendHandler(audioPlayer);
    }

    public void Queue(AudioTrack track){
        tracks.add(track);
        if(!isPlaying){
            Play();
        }
    }

    public void Mixing(boolean arg){
        isMixing = arg;
        if (arg && !isPlaying){
            if (playlistBuffer.size() <= 0){ isMixing = false; return;}
            tracks.add(playlistBuffer.get(0));
            playlistBuffer.remove(0);
            Play();
        }
    }
    public boolean GetIsMixing(){
        return isMixing;
    }
    public void ClearPlaylistBuffer(){
        playlistBuffer.clear();
    }
    public void QueuePlaylist(AudioTrack track){
        playlistBuffer.add(track);
    }
    public void ShufflePlaylistBuffer(){
        Collections.shuffle(playlistBuffer);
    }
    public void Play(){
        BotManager.ChangeMusic();
        audioPlayer.playTrack(tracks.get(trackPlaying).makeClone());
        isPlaying = true;
    }

    public void Pause(){
        audioPlayer.setPaused(!audioPlayer.isPaused());
    }
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if(!isLooping){
                trackPlaying++;
            }
            if(trackPlaying < tracks.toArray().length) {
                Play();
                return;
            }
            if(isMixing){
                if (playlistBuffer.size() <= 0){ isMixing = false; return;}
                tracks.add(playlistBuffer.get(0));
                playlistBuffer.remove(0);
                Play();
                return;
            }
            isPlaying = false;
        }
    }

    public void Skip() {
        trackPlaying++;
        if(tracks.toArray().length > trackPlaying){
            Play();
            return;
        }
        if(isMixing){
            if (playlistBuffer.size() <= 0){ isMixing = false; return;}
            tracks.add(playlistBuffer.get(0));
            playlistBuffer.remove(0);
            Play();
            return;
        }
        trackPlaying = tracks.size();
        audioPlayer.stopTrack();
        isPlaying = false;

    }
    public void Loop() {
        isLooping = !isLooping;
    }

    public void Rewind() {
        if(trackPlaying - 1 >= 0) {
            trackPlaying--;
            Play();
        }
    }

    public void Shuffle(){
        trackPlaying = 0;
        Collections.shuffle(tracks);
        Play();
    }

    public void Clear(){
        //var temp = tracks.get(trackPlaying);
        tracks.clear();
        if(isPlaying == true)
            tracks.add(audioPlayer.getPlayingTrack());
        trackPlaying = 0;
    }
}
