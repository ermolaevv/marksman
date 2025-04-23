package com.example.marksman.model;

import com.example.marksman.config.AppConfig;
import com.example.marksman.server.PlayerHandler;
import com.example.marksman.service.WinnerService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameManager {
    private static final Logger LOGGER = Logger.getLogger(GameManager.class.getName());

    private final GameState gameState;
    private Thread gameThread;
    private final WinnerService winnerService;
    private final GameEventListener eventListener;

    public interface GameEventListener {
        void onGameStateChanged(StateOfRunning state);

        void onGameUpdate(GameState gameState);

        void onGameOver(PlayerInfo winner);

        void onGameReset(GameState gameState);

        void onGameStop();
    }

    public GameManager(GameState gameState, WinnerService winnerService, GameEventListener eventListener) {
        this.gameState = gameState;
        this.winnerService = winnerService;
        this.eventListener = eventListener;
    }

    public boolean allWantToStart() {
        for (PlayerInfo p : gameState.playerList)
            if (!p.wantToStart) return false;
        return true;
    }

    public void setArrowStartY() {
        final int div = gameState.playerList.size() / 2;
        final int mod = gameState.playerList.size() % 2;
        for (int i = 0; i < gameState.playerList.size(); ++i) {
            gameState.playerList.get(i).arrow.y = 0.5 * AppConfig.GAME_HEIGHT + 50.0 * (i - div) + (1 - mod) * 25.0;
        }
    }

    public void startGame() {
        if (allWantToStart() && !gameState.playerList.isEmpty()) {
            LOGGER.info("Начало новой игры. Количество игроков: " + gameState.playerList.size());
            setArrowStartY();
            gameState.state = StateOfRunning.ON;
            eventListener.onGameStateChanged(gameState.state);

            gameThread = new Thread(() -> {
                try {
                    while (!isGameOver()) {
                        if (gameState.state == StateOfRunning.PAUSE) {
                            LOGGER.info("Игра приостановлена");
                            pause();
                        }
                        next();
                        eventListener.onGameUpdate(gameState);
                        Thread.sleep(AppConfig.GAME_REFRESH_RATE_MS);
                    }
                    LOGGER.info("Игра завершена");
                    PlayerInfo winner = findWinner();
                    saveWinnerToDatabase(winner);
                    eventListener.onGameOver(winner);
                } catch (InterruptedException e) {
                    LOGGER.info("Игра прервана");
                    eventListener.onGameStop();
                } finally {
                    resetInfo();
                    eventListener.onGameReset(gameState);
                    gameState.state = StateOfRunning.OFF;
                    eventListener.onGameStateChanged(gameState.state);
                }
            });
            gameThread.setDaemon(true);
            gameThread.start();
        }
    }

    private void next() {
        nextCirclePos(gameState.bigTarget);
        nextCirclePos(gameState.smallTarget);
        for (PlayerInfo p : gameState.playerList) {
            if (p.shooting) {
                p.arrow.x += p.arrow.moveSpeed;
                if (hit(p.arrow, gameState.bigTarget)) {
                    ++p.score;
                    p.shooting = false;
                    p.arrow.x = AppConfig.ARROW_START_X;
                } else if (hit(p.arrow, gameState.smallTarget)) {
                    p.score += 2;
                    p.shooting = false;
                    p.arrow.x = AppConfig.ARROW_START_X;
                } else if (p.arrow.x + AppConfig.ARROW_END_X >= AppConfig.GAME_WIDTH) {
                    p.shooting = false;
                    p.arrow.x = AppConfig.ARROW_START_X;
                }
            }
        }
    }

    private void nextCirclePos(CircleInfo c) {
        double newY = c.y + c.moveSpeed;
        if (newY - c.radius >= AppConfig.GAME_HEIGHT) {
            newY = -c.radius;
        }
        c.y = newY;
    }

    public boolean hit(ArrowInfo a, CircleInfo c) {
        double dx = (a.x + AppConfig.ARROW_END_X) - c.x;
        double dy = a.y - c.y;
        return dx * dx + dy * dy <= c.radius * c.radius;
    }

    private boolean isGameOver() {
        for (PlayerInfo p : gameState.playerList) {
            if (p.score >= AppConfig.WINNING_SCORE) return true;
        }
        return false;
    }

    public synchronized void resume() {
        gameState.state = StateOfRunning.ON;
        notifyAll();
        eventListener.onGameStateChanged(gameState.state);
    }

    private synchronized void pause() throws InterruptedException {
        gameState.state = StateOfRunning.PAUSE;
        wait();
    }

    public boolean anyWantToPause() {
        return gameState.playerList.stream().anyMatch((PlayerInfo p) -> p.wantToPause);
    }

    public boolean allWantToResume() {
        return gameState.playerList.stream().noneMatch((PlayerInfo p) -> p.wantToPause);
    }

    public void pauseGame() {
        if (anyWantToPause()) {
            LOGGER.info("Игра поставлена на паузу");
            gameState.state = StateOfRunning.PAUSE;
            eventListener.onGameStateChanged(gameState.state);
            return;
        }
        if (allWantToResume()) {
            LOGGER.info("Игра возобновлена");
            resume();
        }
    }

    private PlayerInfo findWinner() {
        PlayerInfo winner = gameState.playerList.get(0);
        for (PlayerInfo p : gameState.playerList) {
            if (p.score > winner.score) winner = p;
        }
        return winner;
    }

    private void saveWinnerToDatabase(PlayerInfo winner) {
        try {
            Winner winnerEntity = new Winner(
                    winner.username,
                    winner.score,
                    gameState.playerList.size()
            );
            winnerService.saveWinner(winnerEntity);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Не удалось сохранить победителя в БД", e);
        }
    }

    private void resetInfo() {
        for (PlayerInfo p : gameState.playerList) {
            p.score = 0;
            p.shots = 0;
            p.shooting = false;
            p.wantToPause = false;
            p.wantToStart = false;
            p.wantToStop = false;
            p.arrow.x = AppConfig.ARROW_START_X;
        }
    }

    public void stopGame(PlayerHandler handler) {
        // Устанавливаем флаг, что игрок хочет остановить игру
        if (handler.getPlayerInfo() != null) {
            gameState.playerList.stream()
                    .filter(p -> p.username.equals(handler.getPlayerInfo().username))
                    .findFirst()
                    .ifPresent(p -> p.wantToStop = true);
        }

        // Если хотя бы один игрок хочет остановить, останавливаем игру
        if (gameState.playerList.stream().anyMatch(p -> p.wantToStop)) {
            LOGGER.info("Игра остановлена по запросу игрока: " +
                    (handler.getPlayerInfo() != null ? handler.getPlayerInfo().username : "неизвестный"));

            // Если игровой поток существует и запущен, прерываем его
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.interrupt();
            }

            // Останавливаем игру
            gameState.state = StateOfRunning.OFF;
            eventListener.onGameStateChanged(gameState.state);

            // Отправляем сообщение о принудительной остановке
            eventListener.onGameStop();

            // Обновляем состояние игры
            resetInfo();
            eventListener.onGameReset(gameState);
            eventListener.onGameStateChanged(gameState.state);
        }
    }

    public void stopGame() {
        if (gameThread != null && gameThread.isAlive()) {
            gameThread.interrupt();
        }
    }

    public boolean isGameStarted() {
        return gameState.state == StateOfRunning.ON || gameState.state == StateOfRunning.PAUSE;
    }
} 