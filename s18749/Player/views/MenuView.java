package s18749.Player.views;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import s18749.Player.Player;

public class MenuView extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	private Player _player;
	private JLabel _play;
	private JLabel _exit;
	private JLabel _list;
	private JButton _toggleMenuButton;
	private JPanel _view;
	private boolean _playHidden = false;
	private MouseAdapter _entryClickListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			onEntryClick(((JLabel) e.getSource()));
		}
	};

	MenuView(Player player, Color background, Color foreground) {
		super(null);

		_player = player;

		Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);
		_view = new JPanel(null);

		_view.setBounds(75, 95, 150, 150);
		_view.setBorder(border);
		_view.setBackground(background);
		_view.setForeground(foreground);
		_view.setVisible(true);

		makeEntries();
		makeMenuToggleButton();

		add(_view);
		setBounds(0, 0, 300, 320);
		setVisible(true);
		setOpaque(false);
	}

	private void makeMenuToggleButton() {
		_toggleMenuButton = new JButton("...");
		// _toggleMenuButton.setBounds(205, 95, 20, 20);
		_toggleMenuButton.setBounds(259, 1, 20, 19);
		_toggleMenuButton.setBackground(Color.WHITE);
		_toggleMenuButton.setFocusable(false);
		Border border = BorderFactory.createEmptyBorder();
		_toggleMenuButton.setBorder(border);

		System.out.println("listener");
		_toggleMenuButton.addActionListener(this);

		add(_toggleMenuButton);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// if (_player.isPlaying() || _player.wasPlaying()) {
		if (_view.isVisible()) {
			close();
		} else {
			open();
		}
		// }
	}

	private JLabel makeEntry(String text) {
		JLabel label = new JLabel(text);
		label.setOpaque(true);
		label.setHorizontalAlignment(JLabel.CENTER);

		label.addMouseListener(_entryClickListener);

		return label;
	}

	private void makeEntries() {

		_play = makeEntry("PLAY");
		_exit = makeEntry("EXIT");
		_list = makeEntry("LIST USERS");

		_play.setBounds(25, 42, 100, 20);
		_exit.setBounds(25, 68, 100, 20);
		_list.setBounds(25, 92, 100, 20);
		markEnabled(_exit);
		markEnabled(_list);

		markEntries();

		_view.add(_play);
		_view.add(_exit);
		_view.add(_list);
	}

	private void markDisabled(JLabel label) {
		Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY);
		label.setBackground(Color.BLACK);
		label.setBorder(border);
	}

	private void markEnabled(JLabel label) {
		Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.WHITE);
		label.setBackground(Color.WHITE);
		label.setBorder(border);
	}

	private void markEntries() {
		if (!_player.isPlaying()) {
			if (!_playHidden) markEnabled(_play);
			else markDisabled(_play);
		} else {
			markDisabled(_play);
		}
	}

	private void onEntryClick(JLabel source) {
		if (source == _play && !_player.isPlaying()) {
			_player.play();
		} else if (source == _exit && !_player.isPlaying()) {
			_player.exit();
		} else if (source == _list && !_player.isPlaying()) {
			SwingUtilities.invokeLater(new ListView(_player.getAllPlayers(), new Color(45, 52, 54), new Color(178, 190, 195)));
		}
		markEntries();
	}

	public void hidePlay() {
		markDisabled(_play);
		_playHidden = true;
		_player.repaintScreen();
	}

	public void close() {
		// _toggleMenuButton.setBounds(259, 1, 20, 20);
		// _toggleMenuButton.setText("...");
		_view.setVisible(false);
		_player.repaintScreen();
	}

	public void open() {
		markEntries();
		// _toggleMenuButton.setBounds(230, 60, 20, 20);
		// _toggleMenuButton.setText("X");
		_view.setVisible(true);
		_player.repaintScreen();
	}
}
