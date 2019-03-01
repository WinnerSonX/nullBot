package winnersonx;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.util.*;

class Bot extends ListenerAdapter {
    String last = "";
    String now = "";
    private static JDA bot;
    static AudioPlayer player;
    private static final String token = "NTQ5MzI3NDY0OTYzMzc1MTQ0.D1Stgg.XrKforbswZ8e5mJZPXFtloGH7r0";
    private static JDABuilder builder;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    GuildMusicManager musicManager;

    private Bot() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        player = playerManager.createPlayer();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
        player.setFrameBufferDuration(2000);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }
        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    private void loadAndPlay(final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());
        musicManager.scheduler.queue(track);

    }

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }

    public static void main(String[] args) throws LoginException, InterruptedException {

        Commands.setupCommands();
        builder = new JDABuilder(AccountType.BOT);
        builder.setToken(token);
        bot = builder.build();
        bot.setAutoReconnect(true);
        bot.addEventListener(new Bot());
        bot.awaitReady();
    }

    //region LISTENER

    private String roll(GuildMessageReceivedEvent e) {
        ArrayList<Member> members = new ArrayList<>(e.getMessage().getGuild().getMembers());
        Member selectedMember = members.get(new Random().nextInt(members.size()));

        return "User " + selectedMember.getAsMention() + " wins the roll!";

    } // Выбрать случайного юзера

    private String get(String name) {
        return get(name, null);
    }

    private String get(String commandItem, ArrayList<String> args) {
        switch (commandItem) {
            case "date":
                return "Today is: " + new Date().toString() + "\nHave a Good Day :)";
            case "":
                return "Type \"get help\"";
        }
        return "Unknown command, please type\"get help\"";
    }

    private void play(String url) {
        // loadAndPlay();
    }

    private String vol(int i) {
        player.setVolume(i);

        return "Volume set to " + player.getVolume();
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        MessageChannel channel = event.getJDA().getTextChannelById("536169707787780099");
        channel.sendMessage("Logged in at:\n" + new Date().toString()).queue();

    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        super.onShutdown(event);
        System.out.println("Shutdown...");
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        super.onGuildMessageReceived(event);
        JDA jda = event.getJDA();
        MessageChannel messageChannel = event.getMessage().getTextChannel();
        Member member = event.getMessage().getMember();

        String commandItem = "";
        String command;
        ArrayList<String> message;
        ArrayList<String> args = new ArrayList<>();
        //messageChannel.sendMessage(String.valueOf(event.getGuild().getMembers().size())).queue();
        message = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().split(" ")));
        command = message.get(0);
        if (!Commands.commandList.contains(command) || event.getMessage().getAuthor().isBot())
            return;
        if (message.size() > 1) {
            commandItem = message.get(1);
        }
        if (message.size() > 2)
            args.addAll(message.subList(2, message.size() - 1));
        switch (command) {
            case "list":
                StringBuilder list=new StringBuilder();
                list.append("Command List:\n");
                for(String s:Commands.commandList){
                   list.append(s).append("\n");
                }
                list.append("End of command list");
                messageChannel.sendMessage(list).queue();
                break;
            case "get":
                if (args.size() != 0)
                    messageChannel.sendMessage(get(commandItem, args)).queue();
                else messageChannel.sendMessage(get(commandItem)).queue();
                break;
            case "roll":
                messageChannel.sendMessage(roll(event)).queue();
                break;
            case "vup":
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null)
                    messageChannel.sendMessage("Already there").queue();
                else
                    event.getGuild().getAudioManager().openAudioConnection(member.getVoiceState().getChannel());
                break;
            case "vout":
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() == null)
                    break;
                event.getGuild().getAudioManager().closeAudioConnection();
                musicManager.scheduler.nextTrack();
                break;
            case "play":
                if (commandItem.equals(""))
                    messageChannel.sendMessage("Usage: play <url>").queue();
                else {
                    if (commandItem.equals("last"))
                        loadAndPlay(event.getChannel(), last);
                    else
                        loadAndPlay(event.getChannel(), commandItem);
                    if (last.equals("")) {
                        last = commandItem;
                        now = commandItem;
                    } else if (commandItem.startsWith("http")) {
                        last = now;
                        now = commandItem;
                    }
                }
                break;
            case "last":
                messageChannel.sendMessage(last).queue();
                break;
            case "next":
                musicManager.scheduler.nextTrack();
                break;
            case "stop":
                for (int i = 0; i < musicManagers.size(); i++)
                    musicManager.scheduler.nextTrack();
                break;
            case "vol":
                if (commandItem.equals(""))
                    messageChannel.sendMessage("Usage: vol <int>").queue();
                else messageChannel.sendMessage(vol(Integer.parseInt(commandItem))).queue();
                break;
            case "sqrt":
                if (commandItem.equals(""))
                    messageChannel.sendMessage("Usage: sqrt <number>").queue();
                else
                    messageChannel.sendMessage((String.valueOf(Math.sqrt(Double.parseDouble(commandItem))))).queue();
                break;
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        super.onPrivateMessageReceived(event);
        switch (event.getMessage().getContentRaw()) {
            case "!off":
                event.getJDA().shutdown();
                break;
            case "afk":
                builder.setStatus(OnlineStatus.IDLE);
                break;
        }

    }
    //endregion
}
