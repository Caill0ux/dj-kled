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

    public void Play(){
        BotManager.ChangeMusic();
        audioPlayer.playTrack(tracks.get(trackPlaying));
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
                BotManager.ChangeMusic();
                audioPlayer.playTrack(tracks.get(trackPlaying).makeClone());
                isPlaying = true;
                return;
            }
            isPlaying = false;
        }
    }

    public void Skip() {
        trackPlaying++;
        if(tracks.toArray().length > trackPlaying){
            BotManager.ChangeMusic();
            audioPlayer.playTrack(tracks.get(trackPlaying).makeClone());
            isPlaying = true;
        }
        else{
            trackPlaying = tracks.size();
            audioPlayer.stopTrack();
            isPlaying = false;
        }
    }
    public void Loop() {
        isLooping = !isLooping;
    }

    public void Rewind() {
        if(trackPlaying - 1 >= 0) {
            trackPlaying--;
            BotManager.ChangeMusic();
            audioPlayer.playTrack(tracks.get(trackPlaying).makeClone());
            isPlaying = true;
        }
    }

    public void Shuffle(){
        trackPlaying = 0;
        Collections.shuffle(tracks);
        BotManager.ChangeMusic();
        audioPlayer.playTrack(tracks.get(trackPlaying).makeClone());
    }

    public void Clear(){
        //var temp = tracks.get(trackPlaying);
        trackPlaying = 0;
        tracks.clear();
    }
}
