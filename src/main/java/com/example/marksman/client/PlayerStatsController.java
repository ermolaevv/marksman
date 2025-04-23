package com.example.marksman.client;

import com.example.marksman.config.AppConfig;
import com.example.marksman.model.PlayerInfo;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;

public class PlayerStatsController {
    @FXML
    private VBox playerStatsBox;
    @FXML
    private Label score;
    @FXML
    private Label shots;
    @FXML
    private Label nickname;

    @FXML
    private void initialize() {
        playerStatsBox.setBorder(Border.stroke(Color.valueOf(AppConfig.TEXT_COLOR)));
        
        score.setTextFill(Color.valueOf(AppConfig.TEXT_COLOR));
        shots.setTextFill(Color.valueOf(AppConfig.TEXT_COLOR));
        nickname.setTextFill(Color.valueOf(AppConfig.TEXT_COLOR));
    }

    public void setPlayer(PlayerInfo player) {
        nickname.setText(nickname.getText() + player.username);

        score.setText(score.getText() + player.score);
        shots.setText(shots.getText() + player.shots);
        
        playerStatsBox.setId(player.username + "VBox");
    }

    public void updateScore(int newScore) {
        String prefix = score.getText().split(" ")[0];
        score.setText(prefix + " " + newScore);
    }

    public void updateShots(int newShots) {
        String prefix = shots.getText().split(" ")[0];
        shots.setText(prefix + " " + newShots);
    }

    public VBox getRoot() {
        return playerStatsBox;
    }
} 