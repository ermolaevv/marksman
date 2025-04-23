package com.example.marksman.client;

import com.example.marksman.model.Action;
import com.example.marksman.model.GameState;
import com.example.marksman.model.PlayerInfo;
import com.example.marksman.model.StateOfRunning;
import com.example.marksman.config.AppConfig;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Client extends Application {
    private static final Logger LOGGER = Logger.getLogger("Client");

    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private ServerHandler serverHandler;
    private GameState gameState = new GameState(0);
    private UIController uiController;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 881, 513);
        stage.setTitle("Marksman");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        
        uiController = fxmlLoader.getController();
        uiController.setClient(this);
        
        try {
            clientSocket = new Socket(AppConfig.SERVER_HOST, AppConfig.SERVER_PORT);
            LOGGER.info("Подключение к серверу: " + AppConfig.SERVER_HOST + ":" + AppConfig.SERVER_PORT);

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка подключения к серверу", e);
            uiController.showWelcomeError("Не удалось подключиться к серверу");
        }
    }

    public void connect(String nickname) {
        try {
            LOGGER.info("Попытка подключения с никнеймом: " + nickname);
            out.writeUTF(nickname);
            String response = in.readUTF();
            if (response.equals("OK")) {
                LOGGER.info("Успешное подключение к серверу");
                uiController.showGameScreen();
                serverHandler = new ServerHandler(this, clientSocket, in, out);
            } else {
                LOGGER.warning("Ошибка подключения: " + response);
                uiController.showWelcomeError(response);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при подключении к серверу", e);
            uiController.showWelcomeError("Ошибка подключения к серверу");
        }
    }

    public void startGame() {
        if (gameState.state == StateOfRunning.OFF) {
            Action action = new Action(Action.Type.WANT_TO_START, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        }
    }

    public void stopGame() {
        if (gameState.state != StateOfRunning.OFF) {
            Action action = new Action(Action.Type.WANT_TO_STOP, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        }
    }

    public void togglePause() {
        if (gameState.state == StateOfRunning.ON) {
            Action action = new Action(Action.Type.WANT_TO_PAUSE, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        } else if (gameState.state == StateOfRunning.PAUSE) {
            Action action = new Action(Action.Type.WANT_TO_RESUME, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        }
    }

    public void shoot() {
        if (gameState.state == StateOfRunning.ON) {
            Action action = new Action(Action.Type.SHOOT, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        }
    }

    public void setGameInfo(final GameState gameState) {
        this.gameState = gameState;
        for (PlayerInfo p : gameState.playerList) {
            uiController.addPlayer(p);
        }
        uiController.setState(this.gameState.state);
    }

    public void setPlayerWantToStart(final String nickname) {
        uiController.setPlayerWantToStart(nickname);
    }

    public void updateGameInfo(final GameState gameState) {
        uiController.updateGameInfo(gameState);
    }

    public void updatePlayerWantToPause(final String playerColor) {
        uiController.updatePlayerWantToPause(playerColor);
    }

    public void resetGameInfo(final GameState gameState) {
        uiController.resetGameInfo(gameState);
    }

    public void showWinner(final PlayerInfo p) {
        uiController.showWinner(p);
    }

    public void removePlayer(final String nickname) {
        uiController.removePlayer(nickname);
    }

    public void showStop() {
        LOGGER.info("Игра остановлена");
        gameState.state = StateOfRunning.OFF;
        uiController.setState(gameState.state);
    }

    public void addPlayer(PlayerInfo playerInfo) {
        uiController.addPlayer(playerInfo);
    }

    public void handleLeaderboardResponse(String jsonData) {
        Type listType = new TypeToken<List<Object[]>>() {}.getType();
        List<Object[]> leaderboard = AppConfig.GSON.fromJson(jsonData, listType);
        
        uiController.showLeaderboard(leaderboard);
    }

    public void requestLeaderboard() {
        if (serverHandler != null) {
            Action action = new Action(Action.Type.LEADERBOARD_REQUEST, "");
            serverHandler.sendMessage(AppConfig.GSON.toJson(action));
        } else {
            LOGGER.warning("Не удалось отправить запрос таблицы лидеров: серверный обработчик не инициализирован");
        }
    }
}

