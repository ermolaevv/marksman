package com.example.marksman.config;

import com.google.gson.Gson;

public class AppConfig {
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 8080;
    public static final Gson GSON = new Gson();
    
    public static final double GAME_HEIGHT = 436;
    public static final double GAME_WIDTH = 600;
    
    public static final String[] PLAYER_COLORS = {
        "#dc8a78", "#dd7878", "#ea76cb", "#8839ef", 
        "#d20f39", "#d20f39", "#fe640b", "#df8e1d", 
        "#40a02b", "#179299", "#04a5e5", "#209fb5", 
        "#1e66f5", "#7287fd"
    };
    
    public static final int WINNING_SCORE = 5;
    public static final int GAME_REFRESH_RATE_MS = 16;
    
    public static final double ARROW_START_X = 5.0;
    public static final double ARROW_END_X = 45.0;    
    
    public static final String TEXT_COLOR = "#4c4f69";
}