package s18749.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.awt.Color;

import javax.swing.SwingUtilities;

import s18749.Player.views.BoardView;
import s18749.Player.views.MenuView;
import s18749.Player.views.PlayerView;

public class PassivePlayer implements Runnable, Player {
    private static int PACKET_SIZE = 508;
    private BoardView _board;
    private MenuView _menu;
    private MulticastSocket _socket;
    private String _id = "";
    private Color[] _backgrounds = { new Color(223, 230, 233) };

    private Color[] _foregrounds = { Color.BLACK };

    private volatile boolean _running = true;

    PassivePlayer(int port) throws IOException {
        PlayerView view = new PlayerView(this, selectColor(_backgrounds), selectColor(_foregrounds));
        _board = view.getBoard();
        _menu = _board.getMenu();
        SwingUtilities.invokeLater(view);

        connect(port);
    }

    private Color selectColor(Color[] list) {
        return list[(int) (Math.random() * (list.length - 1))];
    }

    private void connect(int port) throws IOException {
        _socket = new MulticastSocket(port);
        _socket.setBroadcast(true);
    }

    private void waitForMove() {
        boolean wasWatching = false;
        boolean watching = false;

        try {
            _socket.setSoTimeout(10000);
            byte[] _extracted = new byte[PACKET_SIZE];
            DatagramPacket rcv = new DatagramPacket(_extracted, _extracted.length);

            if (_running) {
                if (!wasWatching && watching) {
                    wasWatching = true;
                    _socket.setSoTimeout(30000);
                }

                _socket.receive(rcv);
                String data = new String(rcv.getData(), StandardCharsets.UTF_8);
                String[] args = data.split(" ");
                // id id model winseq|false(unresolved)

                if (args.length > 3) {
                    int[] winseq = new int[3];

                    _board.setModel(args[2]);
                    watching = true;
                    _board.setInfo("player: " + args[0].substring(0, 15) + "...", null);

                    args[3] = args[3].trim();
                    if (!args[3].contains("false")) {
                        if (args[3].contains("unresolved")) {
                            _board.setInfo("unresolved", Color.GRAY);
                        } else {
                            for (int i = 0; i < args[3].length(); i++) {
                                winseq[i] = Integer.parseInt("" + args[3].charAt(i));
                            }

                            _board.markWinSequence(winseq, new Color(85, 239, 196));
                        }
                    }

                    repaintScreen();
                }
            }

        } catch (SocketTimeoutException e) {
            if (wasWatching) {
                _board.setInfo("no move in 30 sec", Color.GRAY);
            } else {
                _board.setInfo("no duel is being transmited", null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                _socket.close();
            }
        });

        _menu.close();
        _menu.hidePlay();
        _board.setInfo("view only mode", null);
        while (_running)
            waitForMove();
    }

    public String getPlayerId() {
        return _id;
    }

    public boolean isCross() {
        return false;
    }

    public boolean isPlaying() {
        return false;
    }

    public boolean isLocked() {
        return true;
    }

    public synchronized void play() {
    }

    public synchronized void exit() {
        System.exit(0);
    }

    public synchronized void onInteraction(String move) {

    }

    public void repaintScreen() {
        _board.repaint();
        if (_menu.isVisible()) {
            _menu.repaint();
        }
    }

    public ArrayList<String> getAllPlayers() {
        return new ArrayList<>();
    }
}