package s18749.Player.views;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.border.Border;

import s18749.Player.Player;

public class PlayerView implements Runnable, ActionListener {
    private JFrame _window;
    private BoardView _board;
    private Player _model;

    public PlayerView(Player model, Color background, Color foreground) {
        _model = model;
        _board = new BoardView(model, background, foreground);
    }

    private void makeCloseButton() {
        JButton close = new JButton("X");
        // _close.setBounds(205, 95, 20, 20);
        close.setBounds(279, 1, 20, 20);
        close.setBackground(new Color(255, 118, 117));
        close.setForeground(Color.WHITE);
        close.setFocusable(false);
        Border border = BorderFactory.createEmptyBorder();
        close.setBorder(border);

        close.addActionListener(this);

        _board.add(close);
    }

    @Override
    public void run() {
        _window = new JFrame();
        _window.setUndecorated(true);
        _window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _window.setPreferredSize(new Dimension(300, 320));
        _window.setLocationRelativeTo(null);
        _window.setLayout(null);

        makeCloseButton();

        _window.setBackground(new Color(99, 110, 114));

        _window.add(_board);
        _window.pack();
        _window.setVisible(true);
    }

    public BoardView getBoard() {
        return _board;
    }

    public void repaint() {
        _board.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        _model.exit();
        System.exit(0);
    }
}