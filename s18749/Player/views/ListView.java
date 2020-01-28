package s18749.Player.views;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

public class ListView implements Runnable, ActionListener {
  private JFrame _window;
  private ArrayList<String> _list;
  private Color _background;
  private Color _foreground;

  ListView(ArrayList<String> list, Color background, Color foreground) {
    _list = list;
    if (_list.size() == 0) {
      _list.add("NO OTHER PLAYERS");
    }
    _background = background;
    _foreground = foreground;
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

    _window.add(close);
  }

  private void makeList() {
    String[] slist = new String[_list.size()];
    int i = 0;

    for (String string : _list) {
      slist[i++] = string;
    }

    JList<String> list = new JList<>(slist);
    list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    list.setLayoutOrientation(JList.VERTICAL_WRAP);
    list.setVisibleRowCount(-1);

    JScrollPane listScroller = new JScrollPane(list);
    listScroller.setBounds(0, 20, 300, 300);

    _window.add(listScroller);
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
    makeList();

    _window.setBackground(_background);
    _window.setForeground(_foreground);

    _window.pack();
    _window.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    _window.dispose();
  }
}