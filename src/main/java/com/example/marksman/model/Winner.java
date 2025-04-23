package com.example.marksman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "winners")
public class Winner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private LocalDateTime gameDate;

    @Column(nullable = false)
    private int playersCount;

    public Winner() {}

    public Winner(String username, int score, int playersCount) {
        this.username = username;
        this.score = score;
        this.gameDate = LocalDateTime.now();
        this.playersCount = playersCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public LocalDateTime getGameDate() { return gameDate; }
    public void setGameDate(LocalDateTime gameDate) { this.gameDate = gameDate; }
    
    public int getPlayersCount() { return playersCount; }
    public void setPlayersCount(int playersCount) { this.playersCount = playersCount; }
} 