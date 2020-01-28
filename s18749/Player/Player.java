package s18749.Player;

import java.util.ArrayList;

public interface Player {
    public boolean isCross();
    public boolean isLocked();
    public boolean isPlaying();
    public void onInteraction(String move);
    public void play();
    public void exit();
    public void repaintScreen();
    public ArrayList<String> getAllPlayers();
}