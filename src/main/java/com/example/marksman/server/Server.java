package com.example.marksman.server;

import com.example.marksman.config.AppConfig;
import com.example.marksman.model.*;
import com.example.marksman.service.WinnerService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Server implements GameManager.GameEventListener {
    private static final Logger LOGGER = Logger.getLogger("Server");
    private static final Random rand = new Random();
    private final GameState gameState = new GameState(AppConfig.GAME_HEIGHT);
    private final List<PlayerHandler> handlerList = new ArrayList<>();
    private final WinnerService winnerService;
    private final GameManager gameManager;

    public Server() {
        this.winnerService = new WinnerService();
        this.gameManager = new GameManager(gameState, winnerService, this);
    }

    public static void main(String[] args) {       
        LOGGER.info("Запуск сервера");
        Server server = new Server();
        try {
            server.start(AppConfig.SERVER_PORT);
        } finally {
            WinnerService.shutdown();
        }
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Сервер запущен на порту " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Новое подключение: " + clientSocket.getInetAddress());
                new PlayerHandler(this, clientSocket);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при работе сервера", e);
            throw new RuntimeException(e);
        }
    }

    public void removePlayer(PlayerHandler handler) {
        handlerList.remove(handler);
        if (handler.getPlayerInfo() != null) {
            LOGGER.info("Игрок отключился: " + handler.getPlayerInfo().username);
            gameState.playerList.remove(handler.getPlayerInfo());
            sendRemove(handler.getPlayerInfo());
            if (gameManager.isGameStarted()) {
                gameManager.stopGame();
            } else {
                gameManager.startGame();
            }
        }
    }

    private void sendRemove(PlayerInfo p) {
        Action action = new Action(Action.Type.REMOVE, p.username);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }

    public boolean containsNickname(String nickname) {
        for (PlayerInfo p : gameState.playerList) {
            if (p.username.equals(nickname)) return true;
        }
        return false;
    }

    public void addPlayer(String nickname, PlayerHandler handler) throws IOException {
        // Мобильные клиенты не добавляются в список игроков
        if (handler.isMobileClient()) {
            LOGGER.info("Мобильный клиент подключен. Не добавляем в список игроков.");
            handlerList.add(handler);
            return;
        }
        
        String color = AppConfig.PLAYER_COLORS[rand.nextInt(AppConfig.PLAYER_COLORS.length)];
        while (containsColor(color)) color = AppConfig.PLAYER_COLORS[rand.nextInt(AppConfig.PLAYER_COLORS.length)];

        PlayerInfo newPlayer = new PlayerInfo(nickname, color);
        gameState.playerList.add(newPlayer);
        handler.setPlayerInfo(newPlayer);

        LOGGER.info("Новый игрок присоединился: " + nickname + " (цвет: " + color + ")");
        sendNewPlayer(newPlayer);
        handlerList.add(handler);

        String jsonInfo = AppConfig.GSON.toJson(gameState);
        handler.sendMessage(jsonInfo);
    }

    private boolean containsColor(String color) {
        for (PlayerInfo p : gameState.playerList) {
            if (p.color.equals(color)) return true;
        }
        return false;
    }

    private void sendNewPlayer(PlayerInfo p) throws IOException {
        String jsonPlayer = AppConfig.GSON.toJson(p);
        Action action = new Action(Action.Type.NEW, jsonPlayer);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }

    public void sendWantToStart(PlayerHandler handler) {
        Action wantToStart = new Action(Action.Type.WANT_TO_START, handler.getPlayerInfo().username);
        String json = AppConfig.GSON.toJson(wantToStart);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }

    public void startGame() {
        gameManager.startGame();
    }

    public void stopGame(PlayerHandler handler) {
        gameManager.stopGame(handler);
    }

    public void stopGame() {
        gameManager.stopGame();
    }

    public void pauseGame() {
        gameManager.pauseGame();
    }

    public boolean isGameStarted() {
        return gameManager.isGameStarted();
    }

    public void sendLeaderboard(PlayerHandler handler) {
        List<Object[]> leaderboard = winnerService.getLeaderboard();
        String jsonLeaderboard = AppConfig.GSON.toJson(leaderboard);
        Action response = new Action(Action.Type.LEADERBOARD_RESPONSE, jsonLeaderboard);
        handler.sendMessage(AppConfig.GSON.toJson(response));
    }
    
    // Реализация интерфейса GameEventListener
    @Override
    public void onGameStateChanged(StateOfRunning state) {
        sendState();
    }
    
    @Override
    public void onGameUpdate(GameState gameState) {
        sendGameInfo(Action.Type.UPDATE);
    }
    
    @Override
    public void onGameOver(PlayerInfo winner) {
        String jsonWinner = AppConfig.GSON.toJson(winner);
        Action action = new Action(Action.Type.WINNER, jsonWinner);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler p : handlerList) {
            if (!p.isMobileClient()) {
                p.sendMessage(json);
            }
        }
    }
    
    @Override
    public void onGameReset(GameState gameState) {
        sendGameInfo(Action.Type.RESET);
    }
    
    @Override
    public void onGameStop() {
        sendStop();
    }
    
    private void sendStop() {
        Action action = new Action(Action.Type.STOP, null);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }
    
    private void sendState() {
        String jsonState = AppConfig.GSON.toJson(gameState.state);
        Action action = new Action(Action.Type.STATE, jsonState);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }
    
    private void sendGameInfo(Action.Type type) {
        String jsonInfo = AppConfig.GSON.toJson(gameState);
        Action action = new Action(type, jsonInfo);
        String json = AppConfig.GSON.toJson(action);
        for (PlayerHandler h : handlerList) {
            if (!h.isMobileClient()) {
                h.sendMessage(json);
            }
        }
    }
}