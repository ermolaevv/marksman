package com.example.marksman.model;

public record Action(Type type, String info) {
    public enum Type {
        WANT_TO_START,
        WANT_TO_PAUSE,
        WANT_TO_RESUME,
        WANT_TO_STOP,
        NEW,
        STATE,
        UPDATE,
        SHOOT,
        WINNER,
        RESET,
        REMOVE,
        STOP,
        LEADERBOARD_REQUEST,
        LEADERBOARD_RESPONSE,
    }
}
