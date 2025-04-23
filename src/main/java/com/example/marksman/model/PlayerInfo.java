package com.example.marksman.model;

public class PlayerInfo {
    public String username;
    public String color;
    public int score;
    public int shots;
    public ArrowInfo arrow = new ArrowInfo();
    public boolean wantToPause = false;
    public boolean wantToStart = false;
    public boolean wantToStop = false;
    public boolean shooting = false;

    public PlayerInfo(String name, String color) {
        this.username = name;
        this.color = color;
    }
}
