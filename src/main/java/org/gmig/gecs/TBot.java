package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyKeyboardRemove;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import okhttp3.OkHttpClient;
import org.apache.log4j.Logger;
import org.gmig.gecs.command.ListenableCommand;

import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.pengrad.telegrambot.UpdatesListener.CONFIRMED_UPDATES_ALL;

public class TBot {
    private static final Logger logger = Logger.getLogger(TBot.class);

    private String name;

    public String getName() {
        return name;
    }

    private HashSet<Long> allowedChats;
    private HashMap<String,Consumer<Update>> responses = new HashMap<>();
    private TelegramBot bot;
    static private OkHttpClient client;

    public final ConcurrentLinkedQueue<Consumer<?>> onRestartedByTBot= new ConcurrentLinkedQueue<>();


    public void stop(){
        bot.removeGetUpdatesListener();
    }

    TBot(JsonNode root)throws UnknownHostException{
        this(root.get("token").asText(),root.get("name").asText(),new HashSet<>());
        JsonNode chats = root.get("allowedChats");
        allowedChats = new HashSet<>();
        chats.forEach((c)->allowedChats.add(c.asLong()));
    }

    TBot(String token, String name, HashSet<Long> allowedChats) throws UnknownHostException {
        this.name = name;
        this.allowedChats = allowedChats;
        if (client == null) {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(
                    "145.239.94.208", 2280));
            java.net.Authenticator.setDefault( new java.net.Authenticator() //Because okhttp does not seem to provide socks to set the Authenticator username and password interface, so set a global Authenticator
            {
                private PasswordAuthentication authentication = new PasswordAuthentication("gmig", "EYC34347356734dhdfUYfglju675867KLJGH57q3IT2H".toCharArray());
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return authentication;
                }
            });
            client = (new OkHttpClient.Builder())
                    .proxy(proxy)
                    .build();
            /*client = (new OkHttpClient.Builder())
                    .proxy(new Proxy(Proxy.Type.SOCKS,
                            new InetSocketAddress(java.net.InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 9050)))
                    .build();*/
        }
        bot = new TelegramBot.Builder(token)
                .okHttpClient(client)
                .updateListenerSleep(100)
                .build();
        bot.setUpdatesListener(updates -> {
            try {
                for (Update update : updates) {
                    logger.debug(update);
                    if(this.allowedChats.contains(update.message().chat().id())){
                        Message msg = update.message();
                        if(responses.containsKey(msg.text()))
                            responses.get(msg.text()).accept(update);
                    }
                    else
                        sendMessage(update,"Извините, я не работаю в этом чате");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return CONFIRMED_UPDATES_ALL;
        });
    }

    public void addTextResponce(String request, String text) {
        Consumer<Update> consumer = (update)->{
            sendMessage(update,"@"+update.message().from().username()+" "+ text);
        };
        responses.put(request, consumer);
    }

    public void addChoiceResponce(String request, String header,ArrayList<String> choice){
        String[][] choices = choice.stream().map((c)->new String[]{c}).toArray(String[][]::new);
        Keyboard keyboard = new ReplyKeyboardMarkup(choices)
                .oneTimeKeyboard(true)   // optional
                .resizeKeyboard(true)
                .selective(true);  // optional
        Consumer<Update> consumer = (update)->{
                SendMessage msg =
                        new SendMessage(update
                                .message()
                                .chat().id(),
                                "@"+update.message().from().username()+" "+ header)
                        .replyMarkup(keyboard);
                SendResponse sendResponse = bot.execute(msg);
            };
        responses.put(request, consumer);
    }

    public void addActionResponce(String request, ListenableCommand<?> action) {
        responses.put(request,(update)->{
            if(update.message().replyToMessage()!=null)
                if(update.message().replyToMessage().from().username().equals(name)) {
                    sendMessage(update, "@" + update.message().from().username() + " " + request + ". Пожалуйста подождите. И если есть возможность, напишите пожалуйста в двух словах что пошло не так.");
                    action.exec().whenComplete((o,t)->{
                        if(t!=null)
                            sendMessage(update, "@" + update.message().from().username() + " похоже что с просьбой " + request + " ничего не вышло. Придется подождать. Но если вышло, то это замечательно.");
                        else
                            sendMessage(update, "@" + update.message().from().username() + " похоже что получилось " + request + ". Если нет, то придется подождать.");
                    });
                    onRestartedByTBot.forEach((c)->c.accept(null));
                }
        });
    }

    private boolean sendMessage(Update update,String text){
        SendMessage msg =
                new SendMessage(update
                        .message()
                        .chat().id(), text)
                        .replyMarkup(new ReplyKeyboardRemove());
        SendResponse sendResponse = bot.execute(msg);
        return sendResponse.isOk();
    }


    public void sendMessageToAllChats(String text){
        allowedChats.forEach((chatID)->{
            SendMessage request = new SendMessage(chatID,text);
            SendResponse sendResponse = bot.execute(request);
        });
    }



}
