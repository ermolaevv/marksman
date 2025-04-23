package com.example.marksman.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public final CircleInfo bigTarget;
    public final CircleInfo smallTarget;
    public final List<PlayerInfo> playerList = new ArrayList<>();
    public StateOfRunning state;

    public GameState(final double height) {
        bigTarget = new CircleInfo(406.0, 0.5 * height, 44.0, 1);
        smallTarget = new CircleInfo(506.0, 0.5 * height, 22.0, 2);
        state = StateOfRunning.OFF;
    }
}
