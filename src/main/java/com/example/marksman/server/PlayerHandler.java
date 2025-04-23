package com.example.marksman.server;

import com.example.marksman.model.Action;
import com.example.marksman.model.PlayerInfo;
import com.example.marksman.config.AppConfig;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class PlayerHandler extends Thread {
    private final Server server;
    private final Socket clientSocket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private PlayerInfo playerInfo;
    private static final Logger LOGGER = Logger.getLogger(PlayerHandler.class.getName());

    public PlayerHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        clientSocket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        try {
            checkingPlayers();
            handlingMessage();
        } catch (IOException e) {
            stopConnection();
        }
    }

    private void checkingPlayers() throws IOException {
        String nickname = in.readUTF();
        while (server.containsNickname(nickname)) {
            out.writeUTF(nickname + " is already in use.");
            nickname = in.readUTF();
        }
        while (server.isGameStarted()) {
            out.writeUTF("The game has already started");
            nickname = in.readUTF();
        }
        out.writeUTF("OK");
        server.addPlayer(nickname, this);
    }

    private void handlingMessage() throws IOException {
        while (true) {
            String msg = in.readUTF();
            Action action = AppConfig.GSON.fromJson(msg, Action.class);
            switch (action.type()) {
                case WANT_TO_START:
                    playerInfo.wantToStart = true;
                    server.sendWantToStart(this);
                    server.startGame();
                    break;
                case SHOOT:
                    playerInfo.shooting = true;
                    ++playerInfo.shots;
                    break;
                case WANT_TO_PAUSE:
                    playerInfo.wantToPause = true;
                    server.pauseGame();
                    break;
                case WANT_TO_RESUME:
                    playerInfo.wantToPause = false;
                    server.pauseGame();
                    break;
                case LEADERBOARD_REQUEST:
                    server.sendLeaderboard(this);
                    break;
                case WANT_TO_STOP:
                    server.stopGame(this);
                    break;
                case STOP:
                    server.stopGame();
                    break;
            }
        }
    }

    private void stopConnection() {
        try {
            LOGGER.info("Закрытие соединения с игроком: " + 
                        (playerInfo != null ? playerInfo.username : "неизвестный"));
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.warning("Ошибка при закрытии соединения: " + e.getMessage());
        } finally {
            server.removePlayer(this);
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            stopConnection();
        }
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public void setPlayerInfo(PlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }
}
