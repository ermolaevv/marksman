package com.example.marksman.client;

import com.example.marksman.model.Action;
import com.example.marksman.model.GameState;
import com.example.marksman.model.PlayerInfo;
import com.example.marksman.model.StateOfRunning;
import com.example.marksman.config.AppConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ServerHandler extends Thread {
    private static final Logger LOGGER = Logger.getLogger("ServerHandler");
    private final Client client;
    private final Socket clientSocket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public ServerHandler(Client client, Socket socket,
                         DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        this.client = client;
        clientSocket = socket;
        in = dataInputStream;
        out = dataOutputStream;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            requestGameInfo();
            handlingMessage();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Соединение с сервером потеряно: " + e.getMessage());
        } finally {
            downHandler();
        }
    }

    private void requestGameInfo() throws IOException {
        String jsonInfo = in.readUTF();
        GameState gameState = AppConfig.GSON.fromJson(jsonInfo, GameState.class);
        client.setGameInfo(gameState);
    }

    private void handlingMessage() throws IOException {
        while (true) {
            String msg = in.readUTF();
            Action action = AppConfig.GSON.fromJson(msg, Action.class);
            switch (action.type()) {
                case NEW -> client.addPlayer(AppConfig.GSON.fromJson(action.info(), PlayerInfo.class));
                case WANT_TO_START -> client.setPlayerWantToStart(action.info());
                case STATE -> client.setGameInfo(new GameState(0) {{ state = AppConfig.GSON.fromJson(action.info(), StateOfRunning.class); }});
                case UPDATE -> client.updateGameInfo(AppConfig.GSON.fromJson(action.info(), GameState.class));
                case WANT_TO_PAUSE -> client.updatePlayerWantToPause(action.info());
                case WANT_TO_STOP -> client.stopGame();
                case WINNER -> client.showWinner(AppConfig.GSON.fromJson(action.info(), PlayerInfo.class));
                case RESET -> client.resetGameInfo(AppConfig.GSON.fromJson(action.info(), GameState.class));
                case REMOVE -> client.removePlayer(action.info());
                case STOP -> client.showStop();
                case LEADERBOARD_RESPONSE -> client.handleLeaderboardResponse(action.info());
            }
        }
    }

    private void downHandler() {
        try {
            LOGGER.info("Закрытие соединения с сервером");
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при закрытии соединения с сервером", e);
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Ошибка при отправке сообщения на сервер: " + e.getMessage());
            downHandler();
        }
    }
}
