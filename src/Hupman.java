/**
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

class Node {
	private Node parent;
	private ArrayList<Node> adjList;
	private int xGrid;
	private int yGrid;
	private int wallType;
	private boolean hasPellet = false;

	Node(Node newParent, int x, int y, int type) {
		parent = newParent;
		xGrid = x;
		yGrid = y;
		wallType = type;
	}

	void addNode(Node newNode, int x, int y, int type) {
		adjList.add(new Node(this, x, y, type));
	}

	int getWallType() {
		return wallType;
	}
}

public class Hupman extends JFrame{

	private int numRows, numCols, numPellets;
	private int[][] arrMaze;
	private int[][] arrPellets;
	private Node startNode;
	private boolean bFileRead = false;
	private int gridOffset = 100;
	private int gridSize = 50;
	private int pelletRadius = 5;
	private int hupmanX = 0;
	private int hupmanY = 0;
	private int hupmanRadius = 20;

	Hupman() {
		setTitle("Hupman");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		loadFile();
		createGraph();
		repaint();
		setSize(numCols * gridSize + 2 * gridOffset, numRows * gridSize + 2 * gridOffset);
		setVisible(true);
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.drawRect(gridOffset, gridOffset, gridSize * arrMaze[0].length, gridSize * arrMaze.length);
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				int xPos = j * gridSize + gridOffset;
				int yPos = i * gridSize + gridOffset;
				if (arrMaze[i][j] == 1 || arrMaze[i][j] == 3) {
					g.drawLine(xPos, yPos, xPos + gridSize, yPos);
				}
				if (arrMaze[i][j] == 2 || arrMaze[i][j] == 3) {
					g.drawLine(xPos + gridSize, yPos, xPos + gridSize, yPos + gridSize);
				}
			}
		}

		g.setColor(Color.red);
		for (int i = 0; i < numPellets; i++) {
			int xPos = arrPellets[i][1] * gridSize + gridSize/2 - pelletRadius + gridOffset;
			int yPos = arrPellets[i][0] * gridSize + gridSize/2 - pelletRadius + gridOffset;
			g.fillOval(xPos, yPos, pelletRadius * 2, pelletRadius * 2);
		}

		g.setColor(Color.orange);
		int xPos = hupmanX * gridSize + gridSize/2 - hupmanRadius + gridOffset;
		int yPos = hupmanY * gridSize + gridSize/2 - hupmanRadius + gridOffset;
		g.fillOval(xPos, yPos, hupmanRadius * 2, hupmanRadius * 2);
	}

	private void loadFile() {
		String filename;
		BufferedReader reader = null;
		do {
			filename = JOptionPane.showInputDialog("Enter maze filename: ");

			try {
				String[] comp;
				reader = new BufferedReader(new FileReader(filename));

				//create the maze array
				comp = reader.readLine().split("\\s+");
				numRows = Integer.parseInt(comp[0]);
				numCols = Integer.parseInt(comp[1]);
				arrMaze = new int[numRows][numCols];

				//wall types at grid positions
				for (int i = 0; i < arrMaze.length; i++) {
					comp = reader.readLine().split("\\s+");
					for (int j = 0; j < arrMaze[i].length; j++) {
						arrMaze[i][j] = Integer.parseInt(comp[j]);
					}
				}

				//remove empty lines
				while ((comp = reader.readLine().split("\\s+")).length == 0);

				numPellets = Integer.parseInt(comp[0]);
				arrPellets = new int[numPellets][2];
				for (int i = 0; i < numPellets; i++) {
					comp = reader.readLine().split("\\s+");
					arrPellets[i][0] = Integer.parseInt(comp[0]);
					arrPellets[i][1] = Integer.parseInt(comp[1]);
				}

				bFileRead = true;
			} catch (IOException ex) {
				System.out.println("This file doesn't exist.  Choose a different file.");
				//ex.printStackTrace();
			} finally {
				try {
					if (reader != null) reader.close();
				} catch (IOException ex) {
					//ex.printStackTrace();
				}
			}
		}
		while (!bFileRead);
	}

	private void createGraph() {
		startNode = new Node(null, hupmanX, hupmanY, arrMaze[hupmanY][hupmanX]);

		int childrenLeft = 0;
		Node tempNode = startNode;
		do {
			switch (tempNode.getWallType()) {
				case 0:

			}
		}
		while (childrenLeft > 0);
	}

	public static void main(String[] args) {
		Hupman hup = new Hupman();
	}
}
