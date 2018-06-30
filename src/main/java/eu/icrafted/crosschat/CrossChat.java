package eu.icrafted.crosschat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;

import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.game.GameReloadEvent;


import org.slf4j.Logger;

import java.io.File;


import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@SuppressWarnings("unused")
@Plugin(id = "crosschat", name = "iCrafted CrossChat", version = "1.2.0", description = "CrossChat functionality created by iCrafted")
public class CrossChat {
    Socket sock;
    static DataOutputStream dOut;
    static DataInputStream dIn;
    static BufferedReader bIn;
    private Task asyncTask;

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private CommentedConfigurationNode config;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Initialize configuration
        initConfig();

        // Initialize the chat
        initChat();
    }

    @Listener
    public void onReloadPlugins(GameReloadEvent event)
    {
        // Initialize configuration
        initConfig();

        // Initialize the chat
        initChat();
    }

    private void initConfig()
    {
        logger.info("-> Config module");

        try {
            // check if the config exists else create it
            Files.createDirectories(configDir);

            if(!defConfig.exists()) {
                logger.info("Creating config file...");
                defConfig.createNewFile();
            }

            configManager = HoconConfigurationLoader.builder().setFile(defConfig).build();
            config = configManager.load();

            // build config
            config.getNode("server", "hostname").setValue(config.getNode("server", "hostname").getString("localhost"));
            config.getNode("server", "port").setValue(config.getNode("server", "port").getInt(1337));

            configManager.save(config);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private void initChat()
    {
        String hostName = config.getNode("server", "hostname").getString();
        int portNumber = config.getNode("server", "port").getInt();

        logger.info("Trying to connect to " + hostName + ":" + portNumber);

        try {
            this.sock = new Socket(hostName, portNumber);
            this.sock.setKeepAlive(true);

            //this.sock.notifyAll();
            CrossChat.dOut = new DataOutputStream(this.sock.getOutputStream());
            CrossChat.dIn = new DataInputStream(this.sock.getInputStream());
            CrossChat.bIn = new BufferedReader(new InputStreamReader(this.sock.getInputStream(), "UTF-8"));

            Task asyncTask = Task.builder().async().interval(0, TimeUnit.MILLISECONDS).name("CrossChat Waiter").execute(() -> {
                while(true) {
                    try {
                        String sentence = "";// = CrossChat.bIn.readLine();
                        while((sentence = CrossChat.bIn.readLine()) != null) {
                            Text toSend = TextSerializers.FORMATTING_CODE.deserialize(sentence);
                            MessageChannel.TO_ALL.send(toSend);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }).submit(this);


        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        // Close the socket when stopping the server
        try {
            logger.info("Disconnecting from CrossChat service");
            asyncTask.cancel();
            this.sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event) {
        Optional<Player> player = event.getCause().first(Player.class);
        if(player.isPresent()) {
            String input = TextSerializers.FORMATTING_CODE.serialize(event.getMessage().toText());
            CrossChat.writeSock(input);
        }
    }

    public static void writeSock(String send) {
        try {
            String toSend = send.replace("ï¿½", "&");
            CrossChat.dOut.write(toSend.getBytes(StandardCharsets.UTF_8));
            CrossChat.dOut.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}