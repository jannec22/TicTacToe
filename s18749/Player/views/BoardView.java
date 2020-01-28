package s18749.Player.views;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import s18749.Player.Player;

public class BoardView extends JPanel implements MouseListener {
	private static final long serialVersionUID = 1L;
	private FieldView[] _fields;
	private Player _player;
	private MenuView _menu;
	private JLabel _info;
	private Color _background;
	private Color _foreground;

	BoardView(Player player, Color background, Color foreground) {
		super(null);
		_background = background;
		_foreground = foreground;
		setBounds(0, 0, 300, 320);
		setBackground(new Color(99, 110, 114));

		makeInfoLabel();
		_menu = new MenuView(player, background, foreground);
		add(_menu);

		_fields = makeFields();
		_player = player;
	}

	public void clear() {
		_info.setText("");
		_info.setBackground(_background);
		_info.setForeground(_foreground);

		for (FieldView fieldView : _fields) {
			fieldView.clear();
		}
	}

	public void updateFields(int x, int y) {
		if (_fields == null) {
			makeFields();
		}
		_fields[y * 3 + x].setMarked(!_player.isCross());
	}

	public void markWinSequence(int[] seq, Color color) {
		for (int i : seq) {
			System.out.print(i + "-");
			_fields[i].setForeground(color);
		}
		System.out.println();
	}

	public void setInfo(String info, Color color) {
		_info.setText(info);

		if (color != null) {
			_info.setBackground(color);
		} else {
			_info.setBackground(_background);
		}
		_info.repaint();
	}

	private void makeInfoLabel() {
		_info = new JLabel("");
		_info.setBounds(1, 1, 258, 19);
		_info.setOpaque(true);
		_info.setBackground(_background);
		_info.setForeground(_foreground);
		Border border = BorderFactory.createEmptyBorder(0, 6, 0, 0);
		_info.setBorder(border);

		add(_info);
	}

	private FieldView[] makeFields() {
		FieldView[] fields = new FieldView[9];

		int x = 0;
		int y = 20;

		for (int i = 0; i < 9;) {
			FieldView field = new FieldView(x / 100, y / 100);
			field.addMouseListener(this);
			field.setBackground(_background);
			field.setBounds(x, y, 100, 100);

			this.add(field);
			fields[i] = field;

			x += 100;
			if (++i % 3 == 0) {
				y += 100;
				x = 0;
			}
		}

		return fields;
	}

	@Override
	public void mousePressed(MouseEvent event) {
		FieldView source = ((FieldView) event.getSource());

		if (!_player.isLocked() && !source.isMarked()) {
			source.setMarked(_player.isCross());
			_player.onInteraction(source.position());
		}
	}

	@Override
	public void mouseClicked(MouseEvent event) {
	}

	@Override
	public void mouseEntered(MouseEvent event) {
	}

	@Override
	public void mouseExited(MouseEvent event) {
	}

	@Override
	public void mouseReleased(MouseEvent event) {
	}

	public MenuView getMenu() {
		return _menu;
	}

	public void setModel(String model) {
		System.out.println("setting model: " + model);

		for (int i = 0; i < 9; i++) {
			char c = model.charAt(i);

			if (c == '_') {
				_fields[i].clear();
			} else if (c == 'x') {
				_fields[i].setMarked(true);
			} else {
				_fields[i].setMarked(false);
			}
		}
	}
}
