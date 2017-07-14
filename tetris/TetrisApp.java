package tetris;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;

public class TetrisApp extends JFrame
{
	TetrisPanel tetrisPanel = new TetrisPanel();
	JPanel leftPanel = new JPanel();
	JPanel rightPanel = new JPanel();
	private String player;
	Timer scoreLevelTimer = new Timer(100, new ScoreLevelTimer());
	
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				new TetrisApp();
			}
		});
	}
	
	public TetrisApp()
	{
		super("Tetris!");    // made by Thuy Nguyen May 13 - 20, 2014
		
		setVisible(true);
		setExtendedState(MAXIMIZED_BOTH);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.black);
		add(tetrisPanel, BorderLayout.CENTER);
		
		setUpLeftPanel();
		setUpRightPanel();
		padBottomWithSpace();
		setUpMenuBar();
		
		
		askPlayerName();
		giveInstructionMessage();
		
		scoreLevelTimer.start();
		validate();
		
	}
	
	private void padBottomWithSpace()
	{
		JPanel southPanel = new JPanel();
		southPanel.setOpaque(false);
		southPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
		add(southPanel, BorderLayout.SOUTH);
	}
	
	private void setUpLeftPanel()
	{
		leftPanel.setLayout(new GridLayout(2, 1, 10, 10));
		leftPanel.setBackground(Color.black);
		EmptyBorder emptyBorder = new EmptyBorder(30, 10, 15, 10);
		leftPanel.setBorder(emptyBorder);
		
		TetrisPanel.NextShapeClass nextShape = tetrisPanel.new NextShapeClass();
		leftPanel.add(nextShape); 

		TetrisPanel.ScoreLevelClass scoreLevelPanel = tetrisPanel.new ScoreLevelClass();
		leftPanel.add(scoreLevelPanel);
		
		add(leftPanel, BorderLayout.WEST);
	}

	private void setUpRightPanel()
	{
		TetrisPanel.HasTetrisClass hasTetrisPanel = tetrisPanel.new HasTetrisClass();
		rightPanel.add(hasTetrisPanel);
		rightPanel.setBackground(Color.black);
		
		add(rightPanel, BorderLayout.EAST);
	}

	private void setUpMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		JMenu newGameMenu = new JMenu("New Game!");
		JMenuItem newGameItem = new JMenuItem("Start Tetris Game!");
		newGameItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				new TetrisApp();
			}
		});
		newGameMenu.add(newGameItem);
		menuBar.add(newGameMenu);
		
		setJMenuBar(menuBar);
	}

	private void askPlayerName()
	{
		javax.swing.UIManager.put("OptionPane.messageFont", new FontUIResource(new Font("Verdana", Font.BOLD, 15)));
		String name = JOptionPane.showInputDialog(null, "Who is the challenger?", "Tetris!", JOptionPane.QUESTION_MESSAGE);
				if (name != null)
				{
					player = name;
				}
				else
				{
					System.exit(0);
				}
			setTitle(name + "'s game:         Level: 1           Score: 0" );
		
		
	}
	
	private void giveInstructionMessage()
	{
		javax.swing.UIManager.put("OptionPane.messageFont", new FontUIResource(new Font("Verdana", Font.BOLD, 15)));
		StringBuilder sb = new StringBuilder("Welcome to Tetris!\n\n");
		sb.append("Instructions:  \n");
		sb.append("Down, Left, and Right arrow keys move the Tetris shapes\n");
		sb.append("1 = rotate left\n");
		sb.append("3 = rotate right\n");
		sb.append("5 = pause/start\n\n");
		sb.append("Extra: Press the \"0\" key to see the number matrix\n\n"); 
		sb.append("           Press 5 to start and have fun!"); 
		
		JOptionPane.showMessageDialog(null, sb, "Tetris! By Thuy Nguyen", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private class ScoreLevelTimer implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			setTitle(player + "'s game:         Level: " + tetrisPanel.getLevel() + "           Score: " + tetrisPanel.getScore());
		}
	}
}
