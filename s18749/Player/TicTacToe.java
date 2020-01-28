package s18749.Player;

import java.io.IOException;
import java.net.BindException;

public class TicTacToe {

    private static void printHelp() {
        System.out.println(
                "TicTacToe Player\nUsage:\n\n    arg0: host of the tic tac toe server\n    arg1: port\n\nmade by Jan Witkowski 2020");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printHelp();
            return;
        }

        String host;
        int port;

        try {
            port = Integer.parseInt(args[1]);

            if (args[0].equals("-v") || args[0].equals("--view")) {
                PassivePlayer player = new PassivePlayer(port);
                (new Thread(player)).start();
            } else {

                host = args[0];
                InteractivePlayer player = new InteractivePlayer(host, port);
                (new Thread(player)).start();
            }
        } catch (NumberFormatException e) {
            printHelp();
        } catch (BindException e) {
            System.out.println("ERR: Address already in use");
        } catch (IOException e) {
            e.printStackTrace();
            printHelp();
            System.out.println("ERR: could not connect to the server");
        }
    }
}