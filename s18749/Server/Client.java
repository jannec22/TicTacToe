package s18749.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class Client implements Runnable {
    private Socket _socket;
    private volatile boolean _running = true;
    private volatile Client _opponent = null;
    private volatile boolean _playing = false;
    private volatile boolean _moving = false;
    private boolean _isCross;
    private String _duelName;
    private char[] _model = { '_', '_', '_', '_', '_', '_', '_', '_', '_' };
    private int _moves = 0;

    public String clientId = getNextId();

    private InputStreamReader _reader;
    private BufferedReader _bufferedReader;
    private PrintWriter _printWriter;

    Client(Socket socket) {
        _socket = socket;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.out.println("closing socket...");
                    _socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void run() {
        System.out.println("listening for player commands");
        try {
            _reader = new InputStreamReader(_socket.getInputStream());
            _bufferedReader = new BufferedReader(_reader);
            _printWriter = new PrintWriter(_socket.getOutputStream());
            String line;

            while ((line = _bufferedReader.readLine()) != null && _running) {
                System.out.println("COMMAND: '" + line + "'");
                if (line.contains("LOGOUT")) {
                    if (_opponent != null) {
                        _opponent.abort();
                    }

                    abort();

                } else if (line.contains("LIST")) {
                    list();

                } else if (line.contains("PLAY")) {
                    System.out.println("requested new game");
                    _printWriter.println("ID " + clientId + " RSP");
                    _printWriter.flush();
                    _playing = true;

                    int timeout = 0;
                    Client found = Server.requestOpponent(this);

                    while ((found = Server.requestOpponent(this)) == null && timeout < 10 && _opponent == null) {
                        System.out.println((found == null) + " " + (_opponent == null) + " " + timeout);
                        try {
                            Thread.sleep(1000);

                            if (_opponent == null) {
                                System.out.println("NO_OPPONENTS " + (10 - timeout));
                                _printWriter.println("NO_OPPONENTS " + (10 - timeout++) + " RSP");
                                _printWriter.flush();
                            }
                        } catch (InterruptedException e) {
                        }
                    }

                    if (found != null) {
                        boolean start = Math.random() > 0.5;
                        _isCross = Math.random() > 0.5;
                        String duelName = UUID.randomUUID().toString();

                        onMatch(found, start, _isCross, duelName);
                        found.onMatch(this, !start, !_isCross, duelName);
                    } else if (_opponent == null) {
                        _printWriter.println("NO_OPPONENTS RSP");
                        _printWriter.flush();
                    }

                } else if (line.contains("MOVE")) {
                    String[] args = line.split(" ");

                    if (args.length < 4 || _opponent == null) {
                        System.out.println("client sent invalid request");
                    } else {
                        try {
                            int x = Integer.parseInt(args[1]);
                            int y = Integer.parseInt(args[2]);

                            update(x, y, clientId);

                            if (_opponent != null) {
                                _opponent.update(x, y, clientId);
                            } else {
                                System.out.println("NO OPPONENT");
                                _printWriter.println("ABORTED RSP");
                                _printWriter.flush();
                                _running = false;
                            }

                        } catch (NumberFormatException e) {
                            _printWriter.println("ABORTED RSP");
                            _printWriter.flush();
                            _running = false;
                        }
                    }
                }
            }

            _bufferedReader.close();
            _reader.close();
            _socket.close();
            System.out.println("client exited");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getNextId() {
        return UUID.randomUUID().toString();
    }

    private synchronized void list() {
        for (Client client : Server.getAllPlayers()) {
            if (!client.clientId.equals(clientId)) {
                _printWriter.println(client);
                System.out.println(client);
            }
        }
        _printWriter.println("END");
        _printWriter.flush();
    }

    public synchronized void onMatch(Client opponent, boolean start, boolean isCross, String duelName) {
        _opponent = opponent;
        _moving = start;
        _isCross = isCross;
        _duelName = duelName;
        Thread.currentThread().interrupt();

        _printWriter.print("START " + (start ? "1" : "0"));
        _printWriter.println(" CROSS " + (isCross ? "1" : "0") + " RSP");
        _printWriter.flush();
        System.out.println("matched users: " + clientId + " and " + opponent.clientId);
    }

    public boolean isWaiting() {
        return _opponent == null && _playing;
    }

    public synchronized void update(int x, int y, String requestor) {
        boolean self = requestor.equals(clientId);
        char s = self ? _isCross ? 'x' : 'o' : _isCross ? 'o' : 'x';
        int[] winSeq = { 0, 0, 0 };
        boolean end = false;
        _moving = !_moving;

        _model[y * 3 + x] = s;

        for (int i = 0; i < 3; i++) {
            if ((winSeq = checkRow(i)) != null || (winSeq = checkColumn(i)) != null) {
                end = true;
                break;
            }
        }

        if (_model[4] != '_' && !end) {
            if (_model[0] == _model[4] && _model[4] == _model[8]) {
                winSeq = new int[] { 0, 4, 8 };
                end = true;
            } else if (_model[2] == _model[4] && _model[4] == _model[6]) {
                winSeq = new int[] { 2, 4, 6 };
                end = true;
            }
        }

        if (++_moves == 9 && !end) {
            _opponent.endGame('u', x, y, winSeq);
            _printWriter.println("UNRESOLVED RSP"); // we win
            _printWriter.flush();

            try {
                Server.broadcast(_duelName, clientId + " " + (_opponent != null ? _opponent.clientId : "") + " "
                        + new String(_opponent._model) + " unresolved");
            } catch (IOException e) {
                e.printStackTrace();
            }
            reset();

        } else if (end) {
            _opponent.endGame('l', x, y, winSeq);
            String seq = "";

            for (int i : winSeq) {
                seq += i;
            }
            _printWriter.println("WIN " + seq + " RSP"); // we win
            _printWriter.flush();
            reset();
        } else {
            if (!self) {
                System.out.println("UPDATING OPPONENT");
                _printWriter.println("STATE " + x + " " + y + " " + !_moving + " RSP");
                _printWriter.flush();

                try {
                    Server.broadcast(_duelName, clientId + " " + (_opponent != null ? _opponent.clientId : "") + " "
                            + new String(_opponent._model) + " false");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("NOT UPDATING ITSELF");
            }
        }
    }

    private int[] checkRow(int row) {
        if (_model[row] == '_') {
            return null;
        }
        if (_model[row] == _model[row + 1] && _model[row + 1] == _model[row + 2]) {
            return new int[] { row, row + 1, row + 2 };
        }
        return null;
    }

    private int[] checkColumn(int column) {
        if (_model[column + 3] == '_') {
            return null;
        }
        if (_model[column] == _model[3 + column] && _model[3 + column] == _model[6 + column]) {
            return new int[] { column, column + 3, column + 6 };
        }
        return null;
    }

    public void abort() {
        _running = false;
        Server.logout(this);

        _printWriter.println("ABORTED RSP");
        _printWriter.flush();
    }

    public void reset() {
        _moves = 0;
        _opponent = null;
        _playing = false;
        _moving = false;
        _isCross = false;

        for (int i = 0; i < _model.length; i++) {
            _model[i] = '_';
        }
    }

    public void endGame(char result, int lastMoveX, int lastMoveY, int[] winSeq) {
        reset();
        String seq = "";

        for (int i : winSeq) {
            seq += i;
        }

        if (result == 'w') {
            _printWriter.println("WIN " + seq + " " + lastMoveX + " " + lastMoveY + " RSP");
            _printWriter.flush();
        } else if (result == 'l') {
            _printWriter.println("LOSE " + seq + " " + lastMoveX + " " + lastMoveY + " RSP");
            _printWriter.flush();
        } else {
            _printWriter.println("UNRESOLVED " + lastMoveX + " " + lastMoveY + " RSP");
            _printWriter.flush();
        }

        try {
            Server.broadcast(_duelName, clientId + " " + (_opponent != null ? _opponent.clientId : "") + " "
                    + new String(_model) + " " + (result == 'u' ? "false" : seq));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return clientId + " " + _socket.getLocalAddress() + ":" + _socket.getLocalPort();
    }
}