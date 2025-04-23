package com.example.marksman.client;

import com.example.marksman.config.AppConfig;
import com.example.marksman.model.CircleInfo;
import com.example.marksman.model.GameState;
import com.example.marksman.model.StateOfRunning;
import com.example.marksman.model.PlayerInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

public class UIController {
    @FXML
    private Pane welcomePane;
    @FXML
    private TextField nicknameField;
    @FXML
    private Label welcomeError;
    @FXML
    private BorderPane mainPane;
    @FXML
    private Pane gamePane;
    @FXML
    private Circle bigTarget;
    @FXML
    private Circle smallTarget;
    @FXML
    private VBox playersArea;
    @FXML
    private VBox playersStats;
    @FXML
    private Button startGame;
    @FXML
    private Button stop;
    @FXML
    private Button attack;
    @FXML
    private Button pause;
    @FXML
    private Label pauseText;

    private Client client;
    private final BooleanProperty showWelcome;
    private final Map<String, PlayerStatsController> statsControllers = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger("UIController");

    private final RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 1, true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.RED),
            new Stop(0.3, Color.WHITE)
    );

    public UIController() {
        this.showWelcome = new SimpleBooleanProperty(true);
    }

    @FXML
    private void initialize() {
        bigTarget.setFill(gradient);
        bigTarget.setRadius(44.0); // Начальный радиус
        smallTarget.setFill(gradient);
        smallTarget.setRadius(22.0); // Начальный радиус

        initializePanes();
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    private void onConnectButtonClick() {
        client.connect(nicknameField.getText());
    }

    @FXML
    private void startGame() {
        client.startGame();
    }

    @FXML
    private void stopGame() {
        client.stopGame();
    }

    @FXML
    private void togglePause() {
        client.togglePause();
    }

    @FXML
    private void shoot() {
        client.shoot();
    }

    @FXML
    private void onLeaderboardButtonClick() {
        client.requestLeaderboard();
    }

    public void showWelcomeError(String error) {
        welcomeError.setText(error);
    }

    private void initializePanes() {
        welcomePane.visibleProperty().bind(showWelcome);
        mainPane.visibleProperty().bind(showWelcome.not());
    }

    public void showGameScreen() {
        showWelcome.set(false);
    }

    public void showWelcomeScreen() {
        showWelcome.set(true);
    }

    public void setState(StateOfRunning state) {
        switch (state) {
            case ON -> {
                startGame.setDisable(true);
                stop.setDisable(false);
                attack.setDisable(false);
                pause.setDisable(false);
                pauseText.setVisible(false);
            }
            case OFF -> {
                startGame.setDisable(false);
                stop.setDisable(true);
                attack.setDisable(true);
                pause.setDisable(true);
                pauseText.setVisible(false);
            }
            case PAUSE -> {
                startGame.setDisable(true);
                stop.setDisable(false);
                attack.setDisable(true);
                pause.setDisable(false);
                pauseText.setVisible(true);
            }
        }
    }

    public void addPlayer(final PlayerInfo p) {
        Platform.runLater(() -> {
            try {
                // Создание треугольника игрока
                Polygon triangle = new Polygon(-57.0, -33.0, -57.0, 22.0, -16.0, -5.0);
                triangle.setId(p.username + "Triangle");
                triangle.setFill(Color.valueOf(p.color));
                if (p.wantToStart) triangle.setStroke(Color.BLACK);
                VBox pane1 = new VBox(triangle);
                pane1.setId(p.username + "Icon");
                pane1.setAlignment(Pos.CENTER);
                pane1.setPadding(new Insets(10));
                playersArea.getChildren().add(pane1);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("player-stats.fxml"));
                VBox playerStats = loader.load();
                PlayerStatsController controller = loader.getController();
                controller.setPlayer(p);
                
                statsControllers.put(p.username, controller);
                playersStats.getChildren().add(playerStats);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Ошибка при загрузке компонента статистики игрока", e);
            }
        });
    }

    public void updateScore(final PlayerInfo p) {
        Platform.runLater(() -> {
            PlayerStatsController controller = statsControllers.get(p.username);
            if (controller != null) {
                controller.updateScore(p.score);
            }
        });
    }

    public void updateShots(final PlayerInfo p) {
        Platform.runLater(() -> {
            PlayerStatsController controller = statsControllers.get(p.username);
            if (controller != null) {
                controller.updateShots(p.shots);
            }
        });
    }

    private Polygon findTriangle(final String nickname) {
        return (Polygon) gamePane.getScene().lookup("#" + nickname + "Triangle");
    }

    public void removePlayer(final String nickname) {
        Platform.runLater(() -> {
            try {
                LOGGER.info("Удаление игрока: " + nickname);
                
                // Удаляем стрелу игрока, если она существует
                Arrow arrow = findArrow(nickname);
                if (arrow != null) {
                    gamePane.getChildren().remove(arrow);
                }
                
                // Находим и удаляем VBox контейнер треугольника из playersArea
                VBox playerIconContainer = (VBox) mainPane.getScene().lookup("#" + nickname + "Icon");
                if (playerIconContainer != null) {
                    playersArea.getChildren().remove(playerIconContainer);
                }
                
                // Удаляем информацию о статистике игрока
                PlayerStatsController controller = statsControllers.remove(nickname);
                if (controller != null) {
                    playersStats.getChildren().remove(controller.getRoot());
                }
                
                LOGGER.info("Игрок успешно удален: " + nickname);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Ошибка при удалении игрока: " + nickname, e);
            }
        });
    }

    private void removeArrow(final Arrow arrow) {
        gamePane.getChildren().remove(arrow);
    }

    private Arrow findArrow(final String nickname) {
        return (Arrow) gamePane.getScene().lookup("#" + nickname + "Arrow");
    }

    public void showWinner(final PlayerInfo p) {
        Platform.runLater(() -> {
            String info = "Congratulations to " + p.username + "!\n" + p.username + " won with " + p.score + " score.";
            Alert alert = new Alert(Alert.AlertType.INFORMATION, info);
            alert.show();
        });
    }

    public void updateGameInfo(final GameState gameState) {
        setState(gameState.state);
        Platform.runLater(() -> {
            bigTarget.setLayoutY(gameState.bigTarget.y);
            smallTarget.setLayoutY(gameState.smallTarget.y);
            for (PlayerInfo p : gameState.playerList) {
                Arrow playerArrow = findArrow(p.username);
                if (p.shooting) {
                    if (playerArrow == null) {
                        playerArrow = createArrow(p);
                        updateShots(p);
                    }
                    playerArrow.setLayoutX(p.arrow.x);
                } else if (playerArrow != null) {
                    removeArrow(playerArrow);
                    updateScore(p);
                }
            }
        });
    }

    private Arrow createArrow(final PlayerInfo p) {
        Arrow arrow = new Arrow(0, 0.0, AppConfig.ARROW_END_X, 0.0, 7.0);
        arrow.setLayoutX(AppConfig.ARROW_START_X);
        arrow.setLayoutY(p.arrow.y);
        arrow.setId(p.username + "Arrow");
        gamePane.getChildren().add(arrow);
        return arrow;
    }

    public void updatePlayerWantToPause(final String playerColor) {
        Polygon playerTriangle = findTriangle(playerColor);
        if (playerTriangle.getStroke() == Color.BLACK) playerTriangle.setStroke(Color.RED);
        else playerTriangle.setStroke(Color.BLACK);
    }

    public void resetGameInfo(final GameState gameState) {
        Platform.runLater(() -> {
            bigTarget.setLayoutY(gameState.bigTarget.y);
            smallTarget.setLayoutY(gameState.smallTarget.y);
            for (PlayerInfo p : gameState.playerList) {
                updateShots(p);
                updateScore(p);
                gamePane.getChildren().remove(findArrow(p.username));
                findTriangle(p.username).setStroke(Color.TRANSPARENT);
            }
        });
    }

    public void setPlayerWantToStart(final String nickname) {
        Polygon playerTriangle = findTriangle(nickname);
        playerTriangle.setStroke(Color.BLACK);
    }

    public void showLeaderboard(List<Object[]> leaderboard) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Таблица лидеров");
            alert.setHeaderText("Топ игроков");

            // Создаем таблицу
            TableView<LeaderboardEntry> table = new TableView<>();
            
            // Колонка с именем
            TableColumn<LeaderboardEntry, String> nameColumn = new TableColumn<>("Имя");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            
            // Колонка с количеством побед
            TableColumn<LeaderboardEntry, Integer> winsColumn = new TableColumn<>("Победы");
            winsColumn.setCellValueFactory(new PropertyValueFactory<>("wins"));
            
            table.getColumns().add(nameColumn);
            table.getColumns().add(winsColumn);

            // Заполняем таблицу данными
            ObservableList<LeaderboardEntry> data = FXCollections.observableArrayList();
            for (Object[] row : leaderboard) {
                Number winsNumber = (Number) row[1];
                data.add(new LeaderboardEntry((String) row[0], winsNumber.intValue()));
            }
            table.setItems(data);

            // Настраиваем диалог
            alert.getDialogPane().setContent(table);
            alert.getDialogPane().setPrefSize(300, 400);
            alert.showAndWait();
        });
    }

    // Вспомогательный класс для отображения данных в таблице
    public static class LeaderboardEntry {
        private final String username;
        private final int wins;

        public LeaderboardEntry(String username, int wins) {
            this.username = username;
            this.wins = wins;
        }

        public String getUsername() { return username; }
        public int getWins() { return wins; }
    }

}
