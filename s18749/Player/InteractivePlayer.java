package s18749.Player;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import s18749.Player.views.BoardView;
import s18749.Player.views.MenuView;
import s18749.Player.views.PlayerView;

public class InteractivePlayer implements Runnable, Player {
    private PlayerView _view;
    private BoardView _board;
    private MenuView _menu;
    private Socket _socket;
    private String _id = "";
    private boolean _cross;

    private volatile boolean _running = true;

    private boolean _playing = false;
    private boolean _wasPlaying = false;

    private boolean _locked = true;
    private boolean _synchronize = false;
    private String _move = "";
    private boolean _waitForMatch = false;

    private InputStreamReader _reader;
    private BufferedReader _bufferedReader;
    private PrintWriter _printWriter;
    private Color[] _backgrounds = { new Color(45, 52, 54) };

    private Color[] _foregrounds = { new Color(178, 190, 195) };

    InteractivePlayer(String host, int port) throws IOException {
        super();
        PlayerView view = new PlayerView(this, selectColor(_backgrounds), selectColor(_foregrounds));
        _view = view;
        _board = view.getBoard();
        _menu = _board.getMenu();
        SwingUtilities.invokeLater(view);

        connect(host, port);
    }

    private void connect(String host, int port) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        _socket = new Socket(address, port);
    }

    private Color selectColor(Color[] list) {
        return list[(int) (Math.random() * (list.length - 1))];
    }

    private void waitForMove() {
        _locked = true;
        _board.setInfo("waiting for opponent's move", null);

        try {
            System.out.println(Thread.currentThread().getId() + "waiting for move... (" + _id + ")");
            String line;

            while ((line = _bufferedReader.readLine()) != null) {
                String[] args = line.split(" ");
                System.out.println(Thread.currentThread().getId() + "opponent moved: " + line);

                if (line.contains("UNRESOLVED")) {

                    if (args.length > 3) {
                        drawMove(args[1], args[2]);
                    }
                    unresolved();

                    return;
                } else if (line.contains("WIN")) {

                    if (args.length > 2) {

                        int[] seq = new int[3];
                        for (int i = 0; i < args[1].length(); i++) {
                            seq[i] = Integer.parseInt("" + args[1].charAt(i));
                        }
                        _board.markWinSequence(seq, new Color(85, 239, 196));

                        if (args.length > 3) {
                            drawMove(args[2], args[3]);
                        }
                    }
                    win();

                    return;
                } else if (line.contains("LOSE")) {
                    if (args.length > 4) {

                        int[] seq = new int[3];
                        for (int i = 0; i < args[1].length(); i++) {
                            seq[i] = Integer.parseInt("" + args[1].charAt(i));
                        }
                        _board.markWinSequence(seq, new Color(255, 118, 117));

                        if (args.length > 3) {
                            drawMove(args[2], args[3]);
                        }
                    }
                    lose();

                    return;
                } else if (line.startsWith("STATE")) {

                    if (args.length < 3) {
                        System.out.println(
                                Thread.currentThread().getId() + "Server responded with the invalid command: " + line);
                    } else {
                        drawMove(args[1], args[2]);
                    }

                    return;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawMove(String sx, String sy) {
        try {
            int x = Integer.parseInt(sx);
            int y = Integer.parseInt(sy);

            System.out.println(Thread.currentThread().getId() + "updating model for: " + _id);
            _board.updateFields(x, y);
            repaintScreen();

            _board.setInfo("your turn", null);
            _locked = false;

        } catch (NumberFormatException e) {
            _printWriter.println("LOGOUT REQ");
            _printWriter.flush();
            _running = false;
        }
    }

    @Override
    public synchronized void run() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    _socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            _reader = new InputStreamReader(_socket.getInputStream());
            _bufferedReader = new BufferedReader(_reader);
            _printWriter = new PrintWriter(_socket.getOutputStream());

            while (_running) {
                wait();

                if (!_wasPlaying && _playing) {
                    initialize();
                } else if (_wasPlaying && !_playing) {
                    close();
                }

                if (_waitForMatch) {
                    _waitForMatch = false;

                    waitForMatch();
                } else if (_synchronize) {
                    _synchronize = false;

                    _printWriter.println("MOVE " + _move + " REQ");
                    _printWriter.flush();

                    _locked = true;
                    waitForMove();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void initialize() throws IOException {
        System.out.println(Thread.currentThread().getId() + "requesting new game...");
        _printWriter.println("PLAY REQ");
        _printWriter.flush();

        String response;

        while ((response = _bufferedReader.readLine()) != null && _id.isEmpty()) {
            if (response.startsWith("ID")) {
                setPlayerId(response.split(" ")[1]);

                _wasPlaying = true;
                _menu.close();
                System.out.println(Thread.currentThread().getId() + "ID " + _id);

                _waitForMatch = true;
                System.out.println(Thread.currentThread().getId() + "waiting for opponent");
                notifyAll();
                return;

            } else {
                System.out.println(Thread.currentThread().getId() + "ERR: server did not respond with the valid ID");
            }
        }
    }

    private synchronized void close() {
        if (_socket == null || _socket.isClosed()) {
            System.exit(-1);
        }

        _printWriter.println("LOGOUT REQ");
        _printWriter.flush();

        try {
            _socket.setSoTimeout(3000);

            String response = _bufferedReader.readLine();
            if (response == null || response.contains("ABORTED")) {
                _bufferedReader.close();
                _reader.close();
                _socket.close();
                System.out.println(Thread.currentThread().getId() + "exiting...");
            } else {
                System.out.println(Thread.currentThread().getId() + "ERR: server respond with incorrect command");
            }

        } catch (SocketTimeoutException e) {
            System.out.println(Thread.currentThread().getId() + "ERR: server did not respond");

            try {
                _bufferedReader.close();
                _reader.close();
                _socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void waitForMatch() throws IOException {
        _board.setInfo("waiting for opponent...", null);

        String line;
        while ((line = _bufferedReader.readLine()) != null) {
            if (line.contains("NO_OPPONENTS")) {
                String[] args = line.split(" ");

                if (args.length > 2) {
                    _board.setInfo("waiting for opponent... " + args[1], null);
                } else {
                    _board.setInfo("could not find any match", null);
                    _waitForMatch = false;
                    _playing = false;
                    _wasPlaying = false;

                    _printWriter.println("LOGOUT REQ");
                    _printWriter.flush();

                    _menu.open();
                    return;
                }
            } else {
                System.out.println(Thread.currentThread().getId() + "found match");
                startGame(line);
                return;
            }
        }
    }

    private void startGame(String line) {
        System.out.println(Thread.currentThread().getId() + line);
        String[] args = line.split(" ");

        if (args.length < 4 || !line.contains("START") || !line.contains("CROSS")) {
            System.out.println(
                    Thread.currentThread().getId() + "ERR: server responded with the invalid command: " + line);
        } else {
            _cross = args[3].equals("1");
            boolean start = args[1].equals("1");

            if (start) {
                _board.setInfo("your turn", null);
                _locked = false;
            } else {
                waitForMove();
            }
        }
    }

    private void win() {
        reset();
        _board.setInfo("YOU WON", new Color(85, 239, 196));
    }

    private void lose() {
        reset();
        _board.setInfo("YOU LOST", new Color(255, 118, 117));
    }

    private void unresolved() {
        reset();
        _board.setInfo("UNRESOLVED", Color.GRAY);
    }

    private void setPlayerId(String id) {
        _id = id;
    }

    public String getPlayerId() {
        return _id;
    }

    public boolean isCross() {
        return _cross;
    }

    public boolean isPlaying() {
        return _playing;
    }

    public boolean wasPlaying() {
        return _wasPlaying;
    }

    public boolean isLocked() {
        return _locked || _waitForMatch;
    }

    public ArrayList<String> getAllPlayers() {
        ArrayList<String> list = new ArrayList<>();

        _printWriter.println("LIST");
        _printWriter.flush();

        String line;
        try {
            _socket.setSoTimeout(3000);
            while ((line = _bufferedReader.readLine()) != null) {
                if (line.contains("END")) {
                    return list;
                } else {
                    list.add(line);
                }
            }
            _socket.setSoTimeout(0); // reset
        } catch (SocketTimeoutException e) {
            System.out.println("ERR: list command timed out");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public synchronized void play() {
        _board.clear();
        repaintScreen();
        _playing = true;
        notifyAll();
    }

    public void exit() {
        close();
    }

    public synchronized void reset() {
        _locked = true;
        _synchronize = false;
        _move = "";
        _id = "";
        _cross = false;
        _playing = false;
        _wasPlaying = false;
        _waitForMatch = false;
        notifyAll();
    }

    public synchronized void onInteraction(String move) {
        if (!_locked) {
            _locked = true;
            _synchronize = true;
            _move = move;
            notifyAll();
        }
    }

    public void repaintScreen() {
        _view.repaint();
    }

}