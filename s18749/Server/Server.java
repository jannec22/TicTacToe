package s18749.Server;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class Server implements Runnable {
    private ServerSocket _socket;
    private static ArrayList<Client> _players;
    private static ArrayList<InetAddress> _broadcastAddresses;
    private static ArrayList<BroadcastSource> _duelsBroadcasts = new ArrayList<>();

    private static class BroadcastSource {
        public DatagramSocket socket;
        public int port;
        public String name = "";

        public BroadcastSource(int _port) throws SocketException {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            port = _port;
        }
    }

    Server(int port, int[] broadcastPorts) throws IOException {
        _socket = new ServerSocket(port);
        _players = new ArrayList<>();
        _broadcastAddresses = getAllBroadcasts();

        if (broadcastPorts.length == 0) {
            System.out.println("broadcast is disabled");
        } else {
            for (int brPort : broadcastPorts) {
                _duelsBroadcasts.add(new BroadcastSource(brPort));
            }
        }
    }

    private ArrayList<InetAddress> getAllBroadcasts() {
        ArrayList<InetAddress> list = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        if (broadcast != null) {
                            list.add(broadcast);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.out.println("closing sockets...");
                    _socket.close();

                    for (BroadcastSource source : _duelsBroadcasts) {
                        source.socket.close();
                    }

                    System.out.println("The server is shut down!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        while (true) {
            Socket client;

            try {
                client = _socket.accept();
                System.out.println("new player accepted");

                Client player = new Client(client);
                (new Thread(player)).start();
                _players.add(player);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printHelp() {
        System.out.println(
                "TicTacToe Server\nUsage:\n\n   arg0: port to listen to incoming player's requests\n   [-b <port> <port> ...]: ports to broadcast the duels\n\nmade by Jan Witkowski 2020");
    }

    public static ArrayList<Client> getAllPlayers() {
        return _players;
    }

    public static Client requestOpponent(Client a) {
        synchronized (_players) {
            for (Client b : _players) {
                if (b.isWaiting() && !a.clientId.equals(b.clientId)) {
                    System.out.println("opponent found" + Thread.currentThread().getId());
                    return b;
                }
            }

            return null;
        }
    }

    public static void logout(Client a) {
        synchronized (_players) {
            System.out.println("client logged out");
            _players.remove(a);
        }
    }

    public static void broadcast(String duel, String message) throws IOException {
        BroadcastSource source = null;

        for (BroadcastSource s : _duelsBroadcasts) {
            if (s.name.equals(duel)) {
                source = s;
                break;
            }
        }

        if (source == null) {
            for (BroadcastSource s : _duelsBroadcasts) {
                if (s.name.isEmpty()) {
                    s.name = duel;
                    source = s;
                    break;
                }
            }
        }

        if (source != null) {
            for (InetAddress inetAddress : _broadcastAddresses) {
                byte[] bytes = message.getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, inetAddress, source.port);

                System.out.println("broadcasting on: " + inetAddress + ":" + source.port);

                // if (source.socket.isConnected()) {
                source.socket.send(packet);
                // } else {
                // System.out.println("broadcast socket is closed");
                // }
            }
        } else {
            System.out.println("no more ports to broadcast");
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            return;
        }

        int port;
        boolean broadcast = false;
        int index = 0;
        int[] broadcastPorts = null;

        for (int i = 1; i < args.length && !broadcast; i++) {
            System.out.println(args[i]);
            if (args[i].contains("-b") || args[i].contains("--broadcasts")) {
                broadcast = true;
                index = i;
            }
        }

        try {
            port = Integer.parseInt(args[0]);

            if (broadcast) {
                ++index;
                broadcastPorts = new int[args.length - index];

                for (int i = 0; i < broadcastPorts.length && index + i < args.length; i++) {
                    if (!args[index + i].isEmpty()) {
                        System.out.println("broadcast port: " + args[index + i]);
                        broadcastPorts[i] = Integer.parseInt(args[index + i]);
                    }
                }
            } else
                broadcastPorts = new int[0];

            Server server = new Server(port, broadcastPorts);
            (new Thread(server)).start();

        } catch (BindException e) {
            System.out.println("ERR: port already in use");
        } catch (NumberFormatException e) {
            e.printStackTrace();
            printHelp();
        } catch (IOException e) {
            e.printStackTrace();
            printHelp();
        }
    }
}