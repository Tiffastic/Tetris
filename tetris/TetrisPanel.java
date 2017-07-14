package tetris;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public class TetrisPanel extends JPanel
{
	//******************************************fields relating to animation***************************************
	int levelSpeed = 180;
	int speedMax = 80;
	Timer tetrisTimer = new Timer(levelSpeed, new TetrisTimer());
	private boolean showNumberMatrix;
	int rowsEliminated;
	boolean hasCollided, gameOver;
	Graphics tetrisG;

	//******************************************fields relating to the game board**********************************
	private int height = 35, width = 24;
	private int gap = 2;
	private int topWall = -2;
	private int bottomWall = -1, sideWall = -1;
	private int matrixRightCol = width-2, matrixLeftCol = 1, matrixBottomRow = height-2;
	private Integer[][] board = new Integer[height][width];
	private final int size = 20;

	//******************************************fields relating to the shapes**************************************
	private int shapeSize = 4;
	private List<Integer[][]> shapes = new ArrayList<>();
	private List<Integer> shapesID = new ArrayList<>();
	private Integer[][] square = new Integer[shapeSize][shapeSize]; // In order to put the shapes in a list, I must use Integer[][] instead of int[][]
	private Integer[][] Lshape = new Integer[shapeSize][shapeSize];
	private Integer[][] Lshape2 = new Integer[shapeSize][shapeSize];
	private Integer[][] Zshape = new Integer[shapeSize][shapeSize];
	private Integer[][] Sshape = new Integer[shapeSize][shapeSize];
	private Integer[][] stick = new Integer[shapeSize][shapeSize];
	private Integer[][] plug = new Integer[shapeSize][shapeSize];
	private Integer[][] millionaireShape, nextShape;
	private List<Point> rightDyDx = new ArrayList<>();
	private List<Point> leftDyDx = new ArrayList<>();
	private List<Point>  bottomDyDx = new ArrayList<>();
	private List<Point> surfaceDyDx = new ArrayList<>();
	private int furthestBottom, startingCol, shapeTopRow, endingCol, shapeLengthDx, shapeHeightDy;
	private Set<Point> boardLocation = new HashSet<>();
	private Color squareColor, LshapeColor, L2shapeColor, ZshapeColor, stickColor, plugColor, SshapeColor;
	private int shapeColor, nextShapeColor, nextShapeLength;
	Random generator = new Random();

	private List<Point> closestRightNeighborBelow = new ArrayList<>();
	private List<Point> closestLeftNeighborBelow = new ArrayList<>();
	//******************************************fields relating to game******************************************
	int rowLimit = 3;
	int fallToRow, fallToCol;
	int score, level = 1, nextLevelRequirement = 10;
	boolean hasTetris, hasIncreasedLevel;


	//******************************************constructor***************************************************
	public TetrisPanel()
	{
		setOpaque(false);

		//printSignature();
		createGameBoard();

		createShapesMatrix();

		assignShapeColors();

		addShapesToList();

		assignNextShape();

		newShapeComesDown();



		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new MovementKeyListener());
		//manager.addKeyEventDispatcher(new DebugKeyListener());


	}


	//*******************************************************************************************************
	//*****************Methods to evaluate the tetris shape's: top, bottom, height, starting col, ending col, length  *********
	//*******************************************************************************************************
	// IMORTANT:  These methods MUST be evaluated first before the methods that calculate the DyDx of the shape - the
	// displacements from the fallToRow and fallToCol for the top, bottom, right and left pieces of the shape.  Make sure
	// to call these methods first in the evaluateShape method.

	private void getShapeTopRow(Integer[][] array)
	{
		// start at row zero and iterate through each of the columns,
		// the first non zero piece we find is where the top row will be:

		for (int row = 0; row < shapeSize; row++)
		{
			for (int col = 0; col < shapeSize; col++)
			{
				if (array[row][col] != 0)
				{
					shapeTopRow = row;
					return;
				}
			}
		}
	}

	private void getShapeBottomRow(Integer[][] array)
	{
		for (int row = array.length-1; row >= 0; row--)
		{
			for (int col = 0; col < array[row].length; col++)
			{
				if (array[row][col] != 0)
				{
					furthestBottom = row;
					return;
				}
			}
		}

	}

	private void getShapeHeightDy()
	{
		shapeHeightDy = furthestBottom - shapeTopRow;
	}

	private int getStartingColumn(Integer[][] array)
	{
		mainLoop:
			for (int col = 0; col < array[0].length; col++)
			{
				for (int row = array.length-1; row >= 0; row--)
				{
					if (array[row][col] != 0)
					{
						startingCol = col;
						break mainLoop;
					}
				}
			}
	return startingCol;

	}

	private int getEndingColumn(Integer[][] array)
	{
		// Iterate through each column starting at the right side and
		// see if there is a non zero piece.  When we find the first
		// non zero piece, then we have our ending column
		mainLoop:
			for (int col = array[0].length-1; col >= 0; col--)
			{
				for (int row = array.length-1; row >= 0; row--)
				{
					if (array[row][col] != 0)
					{
						endingCol = col;
						break mainLoop;
					}
				}
			}
	return endingCol;
	}

	private void getShapeLengthDx()
	{
		shapeLengthDx = endingCol - startingCol;
	}



	//********************************************************************************************************
	//*****************Methods to pin point where the shape's left, right, and bottom rows and columns will be at in the array
	//********************************************************************************************************
	// These methods guide us in detecting collision going down, right, left, and rotating left and right by knowing the
	// dx(where to move right) and dy(where to move up) at the fallToRow and fallToCol (bottom left corner of current shape location).
	// The dx/dy tells us where the shape pieces will be on the array.

	private void getLeftDyDx(Integer[][] array)
	{
		leftDyDx.clear();
		// we want to iterate through all the rows and find the FIRST left piece
		// of the array that's NOT zero.  Then we break right away, ensuring
		// that we only get the first left piece and not other pieces next to it.
		for (int row = furthestBottom; row >=0; row--)
		{
			for (int col = startingCol; col < shapeSize; col++)
			{
				if (array[row][col] != 0)
				{
					int dx = col - startingCol;  // MAKE SURE THAT THIS calculation is positive
					int dy = furthestBottom-row;
					leftDyDx.add(new Point(dx, dy));
					break;
				}
			}
		}
	}

	private void getRightDyDx(Integer[][] array)
	{

		rightDyDx.clear();

		for (int row = furthestBottom; row >= 0; row--)
		{
			for (int col = shapeSize-1; col >= startingCol; col--)
			{
				if (array[row][col] != 0)
				{
					int dx = col - startingCol;  // we take the startingCol as our reference point
					int dy = furthestBottom-row;
					rightDyDx.add(new Point(dx, dy));
					break;
				}
			}
		}

	}

	private void getBottomDyDx(Integer[][] array)
	{
		bottomDyDx.clear();

		// iterate through from the startingCol and furthestBottom
		// find the first non zero piece
		for (int col = startingCol; col <= endingCol; col++)
		{
			for (int row = furthestBottom; row >= 0; row--)
			{
				if (array[row][col] != 0)
				{
					int dx = col - startingCol;
					int dy = furthestBottom - row;
					bottomDyDx.add(new Point(dx, dy));
					break;
				}
			}
		}

	}

	private void getSurfaceDyDx(Integer[][] array)

	{
		surfaceDyDx.clear();
		// starting at the first column and top row, move down each row to find the first non zero
		// that would be a surface piece
		for (int col = 0, width = array[0].length; col < width; col++)
		{
			for (int row = 0; row < array.length; row++)
			{
				if (array[row][col] != 0)
				{
					int dx = col - startingCol;
					int dy = furthestBottom - row;
					surfaceDyDx.add(new Point(dx, dy));
					break;
				}
			}
		}
	}


	//*******************************************************************************************************
	//*****************Methods to prepare shape to be drawn on Panel *********************************************
	//*******************************************************************************************************

	private void fillMatrixWithZeros(Integer[][] ... array)
	{
		for (Integer[][] shape : array)
		{
			for(int row = 0, length = shape.length; row < length; row++)
			{
				for (int col = 0, colLength = shape[row].length; col < colLength; col++)
				{
					shape[row][col] = 0;
				}
			}
		}
	}

	private void createShapesMatrix()
	{

		fillMatrixWithZeros(square, Lshape, Lshape2, Zshape, Sshape, plug, stick);

		square[shapeSize-1][0] = 1;
		square[shapeSize-1][1] = 1;
		square[shapeSize-2][0] = 1;
		square[shapeSize-2][1] = 1;
		shapesID.add(1);

		Lshape[shapeSize-1][0] = 2;
		Lshape[shapeSize-1][1] = 2;
		Lshape[shapeSize-2][0] = 2;
		Lshape[shapeSize-3][0] = 2;
		shapesID.add(2);

		Lshape2[shapeSize-1][0] = 3;
		Lshape2[shapeSize-1][1] = 3;
		Lshape2[shapeSize-2][1] = 3;
		Lshape2[shapeSize-3][1] = 3;
		shapesID.add(3);

		Zshape[shapeSize-1][1] = 4;
		Zshape[shapeSize-1][2] = 4;
		Zshape[shapeSize-2][1] = 4;
		Zshape[shapeSize-2][0] = 4;
		shapesID.add(4);

		stick[shapeSize-1][0] = 5;
		stick[shapeSize-2][0] = 5;
		stick[shapeSize-3][0] = 5;
		stick[shapeSize-4][0] = 5;
		shapesID.add(5);

		plug[shapeSize-1][0] = 6;
		plug[shapeSize-1][1] = 6;
		plug[shapeSize-1][2] = 6;
		plug[shapeSize-2][1] = 6;
		shapesID.add(6);

		Sshape[shapeSize-1][0] = 7;
		Sshape[shapeSize-1][1] = 7;
		Sshape[shapeSize-2][1] = 7;
		Sshape[shapeSize-2][2] = 7;
		shapesID.add(7);
	}

	private void addShapesToList()
	{
		shapes.add(square);
		shapes.add(Lshape);
		shapes.add(Lshape2);
		shapes.add(Zshape);
		shapes.add(stick);
		shapes.add(plug);
		shapes.add(Sshape);
	}

	private void evaluateShape(Integer[][] array)
	{
		// IMPORTANT:  the following 4 methods MUST be implemented first, because the other methods
		// depend on the calculations of these 4 methods:  getFurthestBottom, getShapeTopRow, getStartingColumn, getEndingColumn

		// The furthest bottom (row) of the shape will allow us to place the shape on the row correctly
		// because we'll know where to begin match the row with the shape pieces
		getShapeBottomRow(array);
		getShapeTopRow(array);
		// the starting col will make our piece stay put after rotation instead of jumping right or left
		getStartingColumn(array);
		getEndingColumn(array);
		//************************************************************************************
		//*************************************************************************************

		// IMPORTANT: The following methods can only come AFTER we have the calculations of:
		// the furthestBottom, the startingCol, and endingCol.  These calculations are done via:
		// getFurthestBottom, getShapeTopRow, getStartingColumn, and getEndingColumn.  Do NOT reorder the methods
		// without knowing which methods must come first.


		// These dx and dy calculations tell us where to move from the fallToRow and fallToCol reference (bottom left corner piece)
		// in order to get to all of the shape's top, bottom, right, and left pieces.
		getRightDyDx(array);
		getLeftDyDx(array);
		getBottomDyDx(array);
		getSurfaceDyDx(array);

		// In order to detect rotational collision, we need to know the length and heigh of each shape.
		getShapeLengthDx();
		getShapeHeightDy();

	}

	private void assignNextShape()
	{
		int next = generator.nextInt(shapes.size());
		nextShape = shapes.get(next);
		nextShapeColor = shapesID.get(next);
		nextShapeLength = getEndingColumn(nextShape) - getStartingColumn(nextShape);
	}

	public Integer[][] getNextShape()
	{
		return nextShape;
	}

	private void newShapeComesDown()
	{
		millionaireShape = nextShape;
		shapeColor = nextShapeColor;
		assignNextShape();

		fallToRow = rowLimit+1;
		fallToCol = width/2;

		evaluateShape(millionaireShape);
	}



	//*******************************************************************************************************
	// *****************Methods and private class for the graphics and animation ************************************
	//********************************************************************************************************

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		// find which row and col each shape piece will be at
		getDyDxForBoardLocation(detectCollision());

		// "put shape on" means to turn the zero in the matrix to whatever the shape's id is (1-7).
		putShapeOnRow();

		drawGamePieces(g, board, size, gap, getHeight(), 0, 25);

		if (hasCollided)
		{
			int rowsEliminated = eliminateFilledRows();  // eliminate any filled rows
			// if there is collision, then we start from the row on top of the fallToRow to evaluate
			// if the tower has toppled over.
			drawGamePieces(g, board, size, gap, getHeight(), 0, 16);
			if (towerFellOver(rowsEliminated))
			{

				gameOver();
				return;
			}
			else
			{
				newShapeComesDown();
			}
		}
		else  // do not move from if(hasCollided) because we need to make the board at that row and col zero again if the shape didn't land
		{
			// if there is no collision, then clear the temp values from the row and col, because the shape was only there temporarily,
			// it didn't land there.
			clearTempValueOnBoard();
		}


	}

	private void assignShapeColors()
	{
		squareColor = Color.red;

		LshapeColor = new Color (255, 6, 131);

		L2shapeColor =  new Color(253, 72, 19);

		ZshapeColor =  new Color(255, 45, 237);

		stickColor = Color.green;

		plugColor = Color.yellow;

		SshapeColor = new Color(82, 50, 233);

	}

	public Color getShapeColor(int piece)
	{
		switch(piece)
		{
			case 1:
				return squareColor;       //square red color
			case 2:
				return LshapeColor;    // Lshape pinkish color
			case 3:
				return L2shapeColor;     // L2shapeColor orangey
			case 4:
				return ZshapeColor;         // ZshapeColor purple-ish
			case 5:
				return stickColor;        // stick color green
			case 6:
				return plugColor;      // plug color yellow
			case 7:
				return SshapeColor;     // SshapeColor - indigo
			default:
				return new Color(54, 179, 226);  // border color, blueish
		}
	}

	public void drawGamePieces(Graphics g, Integer[][]gameBoard, int shapeSize, int gapSize, int getHeight, int xCoord, int fontSize)
	{
		// make paint component draw square pieces according to what the number indicates
		// establish the x and y in order to draw the rectangles
		// we want the program to start drawing from the bottom row at first column
		int y = getHeight - shapeSize;   // the bottom row at whatever the frame size will be.  This is the y coordinate for the rectangle
		int x = xCoord;  // xcoordinate for tetris rectangle

		// use a nested for loop to draw the rectangle.
		// if the value of the 2D array at that point is not
		// zero, then draw the rectangle with its assigned color
		// make sure to have a gap between the pieces.
		for (int i = gameBoard.length-1; i >= 0; i--)  // starting from the bottom row
		{
			for (int j = 0; j < gameBoard[i].length; j++)  // starting at the first column
			{
				// we have an option of pressing the "0" key to see the number matrix.
				if (!showNumberMatrix)
				{
					if (gameBoard[i][j] != 0)  // the zeros are the spaces, so fill in the rectangles with values of not zero
					{
						g.setColor(getShapeColor(gameBoard[i][j]));
						g.fillRect(x, y, shapeSize, shapeSize);
					}
				}
				else  // if we want the number matrix to show:
				{
					g.setFont(new Font("Times Roman", Font.BOLD, fontSize));
					if (gameBoard[i][j] != 0)
					{
						g.setColor(getShapeColor(gameBoard[i][j]));
						g.drawString(""+gameBoard[i][j], x, y);
					}
					else
					{
						g.setColor(Color.white);
						g.drawString(0+"", x, y);
					}
				}

				x += shapeSize + gapSize;
			}

			x = xCoord;
			y -= shapeSize+gapSize;

		}
	}

	private void createGameBoard()
	{
		fillMatrixWithZeros(board);
		makeGameBorders();
	}

	private void makeGameBorders()
	{
		// IMPORTANT:  When you want to work with only ONE row or ONE column, USE A SINGLE for loop, not a nested one
		// because a nested for loop when working with just ONE col or row is inefficient and prone to bugs.

		int top = rowLimit-1;
		int top2 = rowLimit-2;
		int bottom = height-1;
		// make top and bottom walls
		for (int col = 0; col < width; col++)
		{

			board[top][col] = topWall; 				 // make the top wall.  We want the wall to be one above the rowLimit because if the shape is on the
			board[top2][col] = topWall;			    // rowLimit then the game will be over.  -2 matches our numberline above, and also this value distinguishes
			// itself as a number that is okay to crash into as the shape is coming down the array;
			board[bottom][col] = bottomWall;       // pad the bottom row with numbers because that game doesn't work right if there is no padding
			// at the bottom.  Meaning when the shape hits the bottom row without padding, it cannot move
			// left or right anymore, and a next shape comes down right away.
		}

		// make a border at the left and right side of the matrix, this is for asthetic purposes.
		// left col = 0, right col = width-1
		int leftCol = 0;
		int rightCol = width-1;
		for (int row = rowLimit; row <= matrixBottomRow ; row++)
		{
			board[row][leftCol] = sideWall;
			board[row][rightCol] = sideWall;
		}
	}

	private void printSignature()
	{
		System.out.println("********************************************************************************************");
		System.out.println("********************************************************************************************");
		System.out.println("************************Tetris!  Made by Thuy Nguyen May 13-17, 2014****************************");
		System.out.println("********************************************************************************************");
		System.out.println("********************************************************************************************");
	}

	private class TetrisTimer implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			repaint();
			fallToRow++;
		}

	}



	//********************************************************************************************************
	// *****************Methods for moving the shapes ************************************************************
	//********************************************************************************************************

	public Integer[][] rotate90Left(Integer[][] array)
	{
		// first row => first col
		Integer[][] rotated90 =new Integer[array[0].length][array.length];

		for (int row = 0, rotatedCol = 0; row < array.length; row++, rotatedCol++)
		{
			for (int col = array[row].length-1, rotatedRow = 0; col >= 0; col--, rotatedRow++)
			{
				rotated90[rotatedRow][rotatedCol] = array[row][col];
			}
		}

		millionaireShape = rotated90;
		return millionaireShape;
	}

	public Integer[][] rotate90Right(Integer[][] array)
	{
		// first row => last col.  So switch the length of the row and column
		Integer[][] rotated90 = new Integer[array[0].length][array.length];

		for (int row = 0, rotatedCol = rotated90[0].length-1; row < array.length; row++, rotatedCol--)
		{
			for (int col = 0, rotatedRow = 0; col < array[0].length; col++, rotatedRow++)
			{
				rotated90[rotatedRow][rotatedCol] = array[row][col];
			}
		}

		millionaireShape = rotated90;
		return millionaireShape;
	}

	private boolean canMoveLeft()
	{
		boolean moveLeft = true;

		try
		{
			for (Point point : leftDyDx)
			{

				int boardAt = board[fallToRow-point.y][fallToCol+point.x-1];
				// the -1 indicates the neighbor to the left
				if (boardAt != 0 && boardAt != topWall)
				{
					return false;
				}
			}
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			return false;
		}

		return moveLeft;

	}

	private boolean canMoveRight()
	{
		boolean moveRight = true;
		try
		{
			for (Point point : rightDyDx)
			{
				int boardAt = board[fallToRow-point.y][fallToCol+point.x+1];
				// the +1 indicates the neighbor to the right
				if (boardAt != 0 && boardAt != topWall)
				{
					return false;
				}
			}
		}

		catch(ArrayIndexOutOfBoundsException e)
		{
			return false;
		}


		return moveRight;
	}

	private boolean canRotateRight(int heightBeforeRotation, int lengthBeforeRotation, int closestAboveLeftNeighborRow, int closestRightNeighborColumn)///, int closestBelowRightNeighborRow)
	{
		if (hasAboveLeftRotationCollision(closestAboveLeftNeighborRow, lengthBeforeRotation) || hasRowBelowRightRotationCollision() || hasRightRotationCollision(closestRightNeighborColumn, heightBeforeRotation))
		{
			return false;
		}

		return true;
	}

	private boolean canRotateLeft(int lengthBeforeRotation, int heightBeforeRotation, int closestAboveRightNeighborRow)//, int closestBelowLeftNeighborRow)  //, int closestLeftNeighborColumn)//, int closestLeftNeighborColumn)//, int lengthBeforeRotation)
	{
		if (isAgainstLeftEdge(heightBeforeRotation) || hasAboveRightRotationCollision(closestAboveRightNeighborRow, lengthBeforeRotation) || hasRowBelowLeftRotationCollision())
		{
			return false;
		}

		return true;
	}

	private class MovementKeyListener implements KeyEventDispatcher
	{
		@Override
		public boolean dispatchKeyEvent(KeyEvent e)
		{
			if (e.getID() == KeyEvent.KEY_PRESSED)
			{
				switch(e.getKeyCode())
				{


					case KeyEvent.VK_LEFT:
						if (canMoveLeft())
						{
							fallToCol--;
						}
						break;


					case KeyEvent.VK_RIGHT:
						if (canMoveRight())
						{
							fallToCol++;
						}
						break;


					case  KeyEvent.VK_DOWN:

						if (fallToRow < height-1)
						{
							fallToRow++;
						}
						break;

					case KeyEvent.VK_0:

						// change the view of the game to the number array or
						// to the game view
						showNumberMatrix = showNumberMatrix? false : true;
						break;

					case  KeyEvent.VK_5:

						// pause/unpause the game
						if (tetrisTimer.isRunning())
						{
							tetrisTimer.stop();
						}
						else
						{
							tetrisTimer.start();
						}
						break;


					case  KeyEvent.VK_3:

						//**********Rotate Right***************

						if (!rotationIsOutOfBounds())
						{
							// evaluate the following parameters to see if we can rotate right
							int lengthBeforeRotation = shapeLengthDx;
							int heightBeforeRotation = shapeHeightDy;
							int closestAboveLeftNeighborRow = findClosestAboveLeftRow();
							findClosestRightRowBelow();
							int closestRightNeighborColumn = findClosestRightNeighborColumn();

							if (canRotateRight(heightBeforeRotation, lengthBeforeRotation, closestAboveLeftNeighborRow, closestRightNeighborColumn))
							{
								// rotate the shape right and evaluate, but we're not putting any values on the matrix, we just want to see if the shape will fit into the space
								rotate90Right(millionaireShape);
								evaluateShape(millionaireShape);
								boolean hasLanded = false;   // we haven't collided yet, so don't put on top of fallToRow, thus passing in false for the parameter of findRowColForShape
								getDyDxForBoardLocation(hasLanded);

								if (!canPutShapeOnRow())
								{
									rotate90Left(millionaireShape);
									evaluateShape(millionaireShape);
								}

							}
						}
						break;

					case KeyEvent.VK_1:

						// ************Rotate Left**************

						if (!rotationIsOutOfBounds())
						{
							// evaluate the following parameters to see if we can rotate left
							int lengthBeforeRotation = shapeLengthDx;
							int heightBeforeRotation = shapeHeightDy;
							int closestAboveRightNeighborRow = findClosestAboveRightRow();
							findClosestLeftRowBelow();

							if (canRotateLeft(lengthBeforeRotation, heightBeforeRotation, closestAboveRightNeighborRow))
							{
								// if we passed the first check, rotate the shape and evaluate it to see if we can put it on the row
								rotate90Left(millionaireShape);
								evaluateShape(millionaireShape);
								boolean hasLanded = false;   // we haven't collided yet, so don't put on top of fallToRow, thus passing in false for the parameter of findRowColForShape
								getDyDxForBoardLocation(hasLanded);
								//							  // we haven't collided yet, so don't put on top of fallToRow, thus passing in false for the parameter of findRowColForShape

								if (!canPutShapeOnRow())
								{
									rotate90Right(millionaireShape);
									evaluateShape(millionaireShape);
								}
							}
						}
						break;

					default:
						break;
				}

				repaint();
			}

			return false;
		}

	}



	//*******************************************************************************************************
	//*****************Methods for collision and eliminating rows *************************************************
	//*******************************************************************************************************
	// We detect collision first, that's how we know how to place the shape or if we can place the shape.
	// The shape is placed on the fallToRow if there is no collision, and above the fallToRow if there is collision.
	// For rotation, we need to know if the piece will collide at the bottom or top after rotation.

	private boolean detectCollision()
	{
		hasCollided = false;
		hasCollided = hasCollisionBelow();

		return hasCollided;

	}

	private boolean hasCollisionBelow()
	{
		// Important, evaluate the shape first to get its bottom dy and dx
		for (Point point : bottomDyDx)
		{
			// we know where from the fallToRow and fallToCol the shape will be at.  If there is no space there, then there is collision.
			int boardAt = board[fallToRow-point.y][fallToCol+point.x];
			if (boardAt != 0)
			{
				return true;
			}
		}

		return false;
	}

	// for testing purposes
	private boolean hasCollisionAbove()
	{
		// this collision detects moving upwards, does not apply to rotation collision.
		// from the surface of the shape, see if the row above it is occupied, if so there will be collision.
		int rowAbove = 1;
		for (Point point : surfaceDyDx)
		{
			if (board[fallToRow-point.y - rowAbove][fallToCol+point.x] != 0)
			{
				return true;
			}
		}
		return false;
	}

	// for rotational collision detection we need the following methods:
	private boolean rotationIsOutOfBounds()
	{
		// if we're at the right edge of the board, check to see if the fallToCol plus the height of the shape
		// is not more than width-1.

		if (fallToCol + shapeHeightDy > width-1)
		{
			System.out.println("Rotation is out of bounds");
			return true;
		}

		return false;
	}

	// ***********************************collision detection for rotating left * **********************************
	private void findClosestLeftRowBelow()
	{
		int closestNeighborBelowRow = height; // IMPORTANT, IF WE SET THIS TO HEIGHT-1, WE'LL NEVER ENTER INTO THE IF STATEMENT
		int closestPieceRow = 0;
		int closestPieceCol = 0;
		for (Point piece : leftDyDx)
		{
			int pieceRow = fallToRow-piece.y;
			int pieceCol = fallToCol+piece.x;
			// for each left piece, iterate down it's row to find its closest neighbor below
			for (int rowBelow = pieceRow+1; rowBelow <= height-1; rowBelow++)
			{
				if (board[rowBelow][pieceCol] != 0)
				{
					if (rowBelow < closestNeighborBelowRow)
					{
						closestNeighborBelowRow = rowBelow;
						closestPieceRow = pieceRow;
						closestPieceCol  = pieceCol;
					}

					break;
				}
			}
		}

		closestLeftNeighborBelow.add(new Point(closestPieceRow, closestPieceCol));
	}

	private boolean hasRowBelowLeftRotationCollision()
	{
		boolean spaceInBetween = false;
		int boardAt = board[closestLeftNeighborBelow.get(0).x+1][closestLeftNeighborBelow.get(0).y];
		int closestNeighborRowBelow = closestLeftNeighborBelow.get(0).x+1;

		closestLeftNeighborBelow.clear();
		if (boardAt <= 0)  // the bottom of the board has negative values so we account of those values, along with zero which is a space
		{
			spaceInBetween = true;
		}

		if ( !spaceInBetween && closestNeighborRowBelow == fallToRow)
		{
			return true;
		}

		return false;
	}

	// when rotating left counter clockwise, account for the stick which will hit it's neighbor at the right side above.
	// so that's why we check to see if the above nearest right neighbor is higher than the fallToRow - shapeLengthDy
	private int findClosestAboveRightRow()
	{
		int closestNeighborAbove = rowLimit-1;
		for (Point piece : rightDyDx)
		{
			// find the right piece, and iterate up the row to find the closest neighbor above
			for (int rowAbove = fallToRow-piece.y-1; rowAbove >= rowLimit-1; rowAbove--)
			{
				if (board[rowAbove][fallToCol+piece.x] != 0)
				{
					if (rowAbove > closestNeighborAbove)
					{
						closestNeighborAbove  = rowAbove;
						break;
					}
				}
			}
		}
		return closestNeighborAbove;
	}

	private boolean hasAboveRightRotationCollision( int closestAboveRightNeighborRow, int lengthBeforeRotation)
	{
		if (closestAboveRightNeighborRow >= fallToRow-lengthBeforeRotation)
		{
			return true;
		}

		return false;
	}

	private boolean isAgainstLeftEdge(int heightBeforeRotation)
	{
		if (fallToCol-heightBeforeRotation < 0)
		{
			return true;
		}

		return false;
	}


	// ***********************************collision detection for rotating right **********************************
	private void findClosestRightRowBelow()
	{
		int closestNeighborBelowRow = height;  // IMPORTANT, IF WE SET THIS TO HEIGHT-1, WE'LL NEVER ENTER INTO THE IF STATEMENT
		int closestRightPieceRow = 0;
		int closestRightPieceCol = 0;
		// at the right piece, iterate down the row until we find the closest neighbor
		for (Point piece : rightDyDx)
		{
			int rightPieceRow = fallToRow-piece.y;
			int rightPieceCol = fallToCol + piece.x;
			for (int rowBelow = rightPieceRow+1; rowBelow <= height-1; rowBelow++)
			{
				if (board[rowBelow][rightPieceCol] != 0)
				{
					if (rowBelow < closestNeighborBelowRow)
					{
						closestNeighborBelowRow = rowBelow;
						closestRightPieceRow = rightPieceRow;
						closestRightPieceCol = rightPieceCol;

					}
					break;
				}
			}
		}
		closestRightNeighborBelow.add(new Point(closestRightPieceRow, closestRightPieceCol));
	}

	private boolean hasRowBelowRightRotationCollision()
	{
		boolean spaceInBetween = false;  // IMPORTANT: +1 to the row to get the location underneath
		int boardAt = board[closestRightNeighborBelow.get(0).x+1][closestRightNeighborBelow.get(0).y];
		int closestNeighborRowBelow = closestRightNeighborBelow.get(0).x+1;
		closestRightNeighborBelow.clear();
		if (boardAt <= 0)  // the bottom of the board is non positives so we account for them too.
		{

			spaceInBetween = true;
		}

		if ( !spaceInBetween && closestNeighborRowBelow == fallToRow)
		{
			return true;
		}


		return false;
	}

	private int findClosestAboveLeftRow()
	{
		int closestNeighborAbove = rowLimit-1;

		for (Point piece : leftDyDx)
		{
			for (int rowAbove = fallToRow-piece.y-1; rowAbove >= rowLimit-1; rowAbove--)
			{
				if (board[rowAbove][fallToCol+piece.x] != 0)
				{
					if (rowAbove > closestNeighborAbove)
					{
						closestNeighborAbove = rowAbove;
						break;
					}
				}
			}
		}
		return closestNeighborAbove;
	}

	private boolean hasAboveLeftRotationCollision( int closestAboveLeftNeighborRow, int lengthBeforeRotation)
	{
		// this is for rotating right, we must account for the above collision at the left and side
		if (closestAboveLeftNeighborRow >= fallToRow-lengthBeforeRotation)
		{
			return true;
		}

		return false;
	}

	private int findClosestRightNeighborColumn()
	{
		int closestNeighbor = width;
		for (Point point : rightDyDx)
		{
			for (int rightNeighbor = fallToCol+point.x+1; rightNeighbor <= width-1; rightNeighbor++)
			{
				if (board[fallToRow-point.y][rightNeighbor] != 0)
				{
					if (rightNeighbor < closestNeighbor)   // smaller column number on the matrix means closer right of the shape
					{
						closestNeighbor = rightNeighbor;
						break;  // IMPORTANT:  BREAK after finding the closest neighbor to the right
					}
				}
			}
		}

		return closestNeighbor;
	}

	private boolean hasRightRotationCollision(int closestRightNeighborColumn, int heightBeforeRotation)
	{
		// after rotation:
		// if the fallToCol plus the shape's length is now greater than the closest right neighbor column before rotation, then there is collision.
		if (fallToCol + heightBeforeRotation >= closestRightNeighborColumn)
		{
			return true;
		}

		return false;
	}



	//********************************************************************************************************
	//*****************Methods for putting the shape on the matrix, eliminating the filled rows, and detecting game over****
	//********************************************************************************************************

	private void getDyDxForBoardLocation(boolean hasCollided)
	{
		// pass the dy dx calculations from every dydx lists of the shape to the establishBoardLocation() method
		// along with where the row for the shape would be: above the fallToRow or on the fallToRow.
		// the establishBoardLocation() will calculate where on the board the shape will be.
		// IMPORTANT:  if there is collision, then put the shape on the row ABOVE.  If there is no collision,
		// put the shape on the fallToRow.
		// IMPORTANT:  Be sure to save the row and col of where the shape ID was assigned in the boardLocation list
		// in order to clear off those values if there is no collision.

		boardLocation.clear();
		int fromRow = hasCollided? fallToRow-1 : fallToRow;

		// put bottom piece on the bottom
		for(Point point : bottomDyDx)
		{
			establishBoardLocation(fromRow, point.x, point.y);
		}

		// put top piece on the top
		for (Point point : surfaceDyDx)
		{
			establishBoardLocation(fromRow, point.x, point.y);
		}

		// put right piece on the right
		for (Point point : rightDyDx)
		{
			establishBoardLocation(fromRow, point.x, point.y);
		}

		// put left piece on the left
		for (Point point : leftDyDx)
		{
			establishBoardLocation(fromRow, point.x, point.y);
		}

	}

	private void establishBoardLocation(int startingAt, int dx, int dy)
	{
		int row = startingAt-dy;
		int col = fallToCol + dx;

		boardLocation.add(new Point(row, col));
	}

	private boolean canPutShapeOnRow()
	{
		for (Point point : boardLocation)
		{
			int boardAt = board[point.x][point.y];
			if (boardAt != 0)
			{
				return false;
			}
		}

		return true;

	}

	private void putShapeOnRow()
	{
		// putting the shape on the matrix means replacing the zeros
		// of the matrix with whatever number the shape is.
		for (Point point : boardLocation)
		{
			if (board[point.x][point.y] == 0)  // only replace the matrix value if its currently value is zero.  This ensures that when the shape is first coming out the top wall (-2) doesn't get replaced.
			{
				board[point.x][point.y]= shapeColor;
			}
		}
	}

	private boolean hasFilledRow(int rowAt)
	{
		for (int col = matrixLeftCol; col <= matrixRightCol; col++)
		{
			if (board[rowAt][col] == 0)
			{
				return false;
			}
		}
		return true;
	}

	private int eliminateFilledRows()
	{
		// starting at the bottom row, see if there is any spaces (zeros), if there is not, then turn all of those
		// numbers in that row to zeros, then go through each column and switch the values of the top and bottom number one by one
		// we're starting at row : array.length -2 because the first row is padded with blue squares
		int tetrisCount = 0;
		for (int row =  matrixBottomRow; row > rowLimit; row--)
		{
			if (hasFilledRow(row))
			{
				for (int col = matrixLeftCol; col <= matrixRightCol; col ++)  // IMPORTANT: make sure you check to ensure that you're iterating correctly from left to right
				{
					board[row][col] = 0;
				}

				moveTowerDown(row);
				rowsEliminated++;
				checkIfLevelHasIncreased();

				tetrisCount++;   // counting the rows eliminated, if we have four rows eliminated at once, then we have tetris.
				if (tetrisCount %4 == 0)
				{
					hasTetris = true;
					score += 50;
				}
				score += 10;
				row++;  // since we have eliminated a row, we need to start at the same row in order to see
				// if there are more eliminations we can make
			}
		}
		return tetrisCount;

	}

	private void checkIfLevelHasIncreased()
	{
		if (rowsEliminated % nextLevelRequirement == 0)
		{
			levelSpeed -= 40;
			level += 1;
			tetrisTimer.setDelay(levelSpeed);
			if(levelSpeed <= speedMax)
			{
				levelSpeed = speedMax;
			}

		}
	}

	private void moveTowerDown(int startingAt)
	{
		// switch the top and bottom value.  Move through each row, and shift each column down
		// we only want to begin moving the tower down from where we've eliminated the row
		for (int col = matrixLeftCol; col  <=  matrixRightCol; col++)
		{
			for (int row = startingAt; row > rowLimit; row--)  // shift the matrix down from where we eliminated the row
			{
				int temp = board[row][col];
				board[row][col] = board[row-1][col];
				board[row-1][col] = temp;
			}
		}


	}

	private boolean towerFellOver(int afterElimination)
	{
		// The tower has fallen over if the height of the tower, minus the fallToRow is equal to or less than
		// the row limit.  First, account for the elimination of rows, then see if the tower has reached
		// its max height.
		int shapeIsOnTop = 1;
		return fallToRow + afterElimination - shapeHeightDy - shapeIsOnTop <= rowLimit;
	}

	private void gameOver()
	{
		tetrisTimer.stop();
		JOptionPane.showMessageDialog(null, "Game over, play again soon!");
	}

	private void clearTempValueOnBoard()
	{
		// IMPORTANT:  row = point.x  and col = point.y.  Do not switch them around!
		// When the shape first comes out, it could collide with the top wall, whose value
		// is -2 on the matrix. So clear off any other number besides the topWall, which is
		// -2.
		for (Point point : boardLocation)
		{
			int row = point.x;
			int col = point.y;
			if (board[row][col] != topWall)
			{
				board[row][col] = 0;
			}

		}

	}

	private class Point
	{
		int x, y;
		public Point(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString()
		{
			return String.format("(x = %d, y = %d)", x, y);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (getClass() != obj.getClass())
			{
				return false;
			}
			Point other = (Point) obj;
			if (!getOuterType().equals(other.getOuterType()))
			{
				return false;
			}
			if (x != other.x)
			{
				return false;
			}
			if (y != other.y)
			{
				return false;
			}
			return true;
		}

		private TetrisPanel getOuterType()
		{
			return TetrisPanel.this;
		}
	}

	// This class shows the next shape that's going to come out
	public class NextShapeClass extends JPanel
	{
		Timer nextShapeTimer = new Timer(100, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				repaint();
			}
		});

		public NextShapeClass()
		{
			setPreferredSize(new Dimension(600, 600));
			setOpaque(false);
			nextShapeTimer.start();

		}

		int nextShapeSize = 50;
		int gap = 4;
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			drawGamePieces(g, nextShape, nextShapeSize, gap, getHeight()/2 + nextShapeSize*2,  getWidth()/2, nextShapeSize);
		}

	}

	// this class notifies the player of 50 extra bonuse points when they have eliminated four rows in a row
	public class HasTetrisClass extends JPanel
	{
		Timer hasTetrisTimer = new Timer(1500, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				repaint();
			}
		});
		public HasTetrisClass()
		{
			setOpaque(false);
			setPreferredSize(new Dimension(400,400));
			hasTetrisTimer.start();
		}
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			g.setColor(new Color(128, 255, 0));
			g.setFont(new Font("Times Roman", Font.BOLD, 80));
			g.drawString("Tetris!", getWidth()/7, getHeight() - getHeight()/3 + 50);

			if (hasTetris)
			{
				g.setFont(new Font("Times Roman", Font.BOLD, 50));
				g.drawString("50 points extra!", 0, getHeight() - getHeight()/3 + 100);
				hasTetris = false;
			}
		}
	}

	// This class shows the score and level of the game
	public class ScoreLevelClass extends JPanel
	{
		int nextShapeSize = 50;
		int adjustment;
		private Timer scoreLevelTimer = new Timer(100, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				repaint();
			}
		});

		public ScoreLevelClass()
		{
			setPreferredSize(new Dimension(400, 600));
			setOpaque(false);
			scoreLevelTimer.start();

		}
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			g.setColor(new Color(36, 122, 218));
			g.setFont(new Font("Times Roman", Font.BOLD, 50));
			g.drawString("Level: " + getLevel(), getWidth()/2 - getAdjustment(nextShapeLength), getHeight()/10);
			g.drawString("Score: " + getScore(), getWidth()/2-getAdjustment(nextShapeLength), getHeight()/3);

		}


		// in order to make the score and level labels line up center with the next shape on the gui
		// we need to adjust where the score and level strings are drawn.
		public int getAdjustment(int shapesLength)
		{
			switch(shapesLength)
			{
				case 0:
					return nextShapeSize + nextShapeSize/2;
				case 1:
					return nextShapeSize - 10;
				case 2:
					return nextShapeSize/3;
				default:
					return 0;
			}
		}
	}

	//*********************************************************************************************************
	// *****************Methods to keep track of the score and level of the game and to increase the speed at next level *******
	//**********************************************************************************************************

	public int getLevel()
	{
		return level;
	}

	public int getScore()
	{
		return score;
	}

	public boolean hasTetris()
	{
		return hasTetris;
	}



	//********************************************************************************************************
	// *****************Methods for testing purposes  *************************************************************
	//********************************************************************************************************
	// players will not be allowed to use these methods during the game.
	private void fillBoardForTesting()
	{
		// fill the board with rows of a numbers for testing
		// fill four columns starting from the bottom : height-2 to height-6
		for(int row = matrixBottomRow; row > height-6; row--)
		{
			for (int col = matrixLeftCol; col <= matrixRightCol; col++)
			{
				board[row][col] = generator.nextInt(shapes.size())+1; // we need to add one in order to make the range from 1-7.  IMPORTANT:  Add the 1 OUTSIDE of the nextInt
			}
		}
	}
	private void makeNumberLines()
	{
		// making number lines in order to debug easily

		// make number line at the left:
		for (int row = board.length-1; row >= 0; row--)
		{
			board[row][0] = row*-1;
		}

		// make number line below and
		// make number line above
		for (int col = 0; col < width; col++)
		{
			board[height-1][col] = col*-1;
			board[0][col] = col*-1;
		}




	}
	private void printArrayInfo(String name, Integer[][] array, int shapeColor)
	{
		System.out.println("Shape name: " + name);
		System.out.println("ShapeColor: " + shapeColor);
		System.out.println();

		printArray(array);
		System.out.println();
		evaluateShape(array);

		System.out.println("Furthest bottom row = " + furthestBottom);
		System.out.println("Top row = " + shapeTopRow);
		System.out.println("HeightDy = " + shapeHeightDy);
		System.out.println();
		System.out.println("Starting column = " + startingCol);
		System.out.println("Ending column = " + endingCol);
		System.out.println("LengthDx = " + shapeLengthDx);
		System.out.println();
		System.out.println();


		System.out.println("Dy and Dx of all the pieces starting at the bottom left corner of shape location:");
		System.out.println("Dy is the displacement of the row");
		System.out.println("Dx is the displacement of the column");
		System.out.println();

		printArray(array);

		System.out.println("Bottom Dy Dx:");
		System.out.println(bottomDyDx);
		System.out.println();

		System.out.println("Surface Dy Dx:");
		System.out.println(surfaceDyDx);
		System.out.println();

		System.out.println("Left Dy Dx:");
		System.out.println(leftDyDx);
		System.out.println();

		System.out.println("Right Dy Dx:");
		System.out.println(rightDyDx);
		System.out.println();










	}
	private void getAllShapesEvaluation()
	{
		printArrayInfo("square", square, 1);
		printArrayInfo("Lshape", Lshape, 2);
		printArrayInfo("Lshape2", Lshape2, 3);
		printArrayInfo("Zshape", Zshape, 4);
		printArrayInfo("stick", stick, 5);
		printArrayInfo("plug", plug, 6);
		printArrayInfo("Sshape", Sshape, 7);
	}
	public void printArray(Integer[][] array)
	{
		for (int row = 0; row < array.length; row++)
		{
			for (int col = 0; col < array[row].length; col++)
			{
				System.out.print(array[row][col] + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
	private void printBoard()
	{
		for (int row = 0; row < height; row++)
		{
			for (int col = 0; col < width; col++)
			{
				System.out.print(board[row][col] + " ");
			}

			System.out.println();
		}
	}
	private void printValueAtDyDx(int dx, int dy)
	{
		System.out.println("********************************************************");
		System.out.println("point.x = " + dx);
		System.out.println("point.y = " +  dy);
		System.out.printf("board[%d][%d] = %d\n\n", fallToRow-dy, fallToCol+dx, board[fallToRow-dy][fallToCol+dx]);
		System.out.println("*********************************************************");
	}
	private void printBoardAt(int row, int col)
	{
		System.out.println();
		System.out.println("*******************************************");
		System.out.printf("board[%d][%d] = %d", row, col, board[row][col]);
		System.out.println("*******************************************");
		System.out.println();
	}
	private class DebugKeyListener implements KeyEventDispatcher
	{
		@Override
		public boolean dispatchKeyEvent(KeyEvent e)
		{
			if (e.getID() == KeyEvent.KEY_PRESSED)
			{
				if (e.getKeyCode() == KeyEvent.VK_UP)
				{	// for debugging purposes only, not allowed during a game
					if (!hasCollisionAbove())
					{
						fallToRow--;
					}

				}

				else if (e.getKeyCode() == KeyEvent.VK_A)
				{
					getAllShapesEvaluation();
				}

				else if (e.getKeyCode() == KeyEvent.VK_B)
				{
					// print the matrix of numbers
					printBoard();
				}
				else if (e.getKeyCode() == KeyEvent.VK_F)
				{
					// print current row and col
					System.out.println("Fall To Row: " + fallToRow);
					System.out.println("Fall To col " + fallToCol);
				}

				else if (e.getKeyCode() == KeyEvent.VK_L)
				{
					// shows the row and column numbers
					makeNumberLines();
				}

				else if (e.getKeyCode() == KeyEvent.VK_P)
				{
					// print the shape's information: array, startingCol,
					// endingCol, length, height, furthestBottomDyDx,
					// surfaceDyDx, furthestRightDyDx, furthestLeftDyDx.
					printArrayInfo("Debug Shape", millionaireShape, shapeColor);
				}
				else if (e.getKeyCode() == KeyEvent.VK_T)
				{
					// T for Tetris!
					// fills the array with numbers at the bottom row so we don't
					// have to play the game to get a tetris
					fillBoardForTesting();
				}

				repaint();
			}

			return false;
		}
	}

}



