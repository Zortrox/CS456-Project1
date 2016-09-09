/*
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

class Node {
	private ArrayList<Node> adjList = new ArrayList<>();
	private int xGrid;
	private int yGrid;
	private boolean hasPellet = false;
	private boolean visited = false;
	private Node parent = null;

	Node(int x, int y) {
		xGrid = x;
		yGrid = y;
	}

	void addNode(Node node) {
		adjList.add(node);
	}

	void setHasPellet(boolean pellet) {
		hasPellet = pellet;
	}

	boolean getHasPellet() {
		return hasPellet;
	}

	void setVisited(boolean v) {
		visited = v;
	}

	boolean getVisited() {
		return visited;
	}

	void setParent(Node p) {
		parent = p;
	}

	Node getParent() {
		return parent;
	}

	ArrayList<Node> getAdjacentNodes() {
		return adjList;
	}

	int getX() {
		return xGrid;
	}

	int getY() {
		return yGrid;
	}
}

public class Hupman extends JFrame{

	private int numRows, numCols, numPellets;
	private int[][] arrMaze;
	private int[][] arrPellets;
	private Node startNode;
	private Map<String, Node> mapNodes = new HashMap<>();
	private boolean bFileRead = false;
	private int gridOffset = 100;
	private int gridSize = 50;
	private int pelletRadius = gridSize / 10;
	private int hupmanX = 0;
	private int hupmanY = 0;
	private int hupmanRadius = gridSize / 3;

	Hupman() {
		setTitle("Hupman");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		loadFile();
		createGraph();
		repaint();
		setSize(numCols * gridSize + 2 * gridOffset, numRows * gridSize + 2 * gridOffset);
		setVisible(true);
		runAllSearches();
	}

	public void paint(Graphics g) {
		g.clearRect(0, 0, numCols * gridSize + 2 * gridOffset, numRows * gridSize + 2 * gridOffset);

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
		for (Map.Entry<String, Node> entry : mapNodes.entrySet())
		{
			if (entry.getValue().getHasPellet()) {
				int xPos = entry.getValue().getX() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				int yPos = entry.getValue().getY() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				g.fillOval(xPos, yPos, pelletRadius * 2, pelletRadius * 2);
			}
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
				for (int i = 0; i < numRows; i++) {
					comp = reader.readLine().split("\\s+");
					for (int j = 0; j < numCols; j++) {
						arrMaze[i][j] = Integer.parseInt(comp[j]);
					}
				}

				//remove empty lines
				while ((comp = reader.readLine().split("\\s+")).length == 0);

				numPellets = Integer.parseInt(comp[0]);
				arrPellets = new int[numPellets][2];
				for (int i = 0; i < numPellets; i++) {
					comp = reader.readLine().split("\\s+");
					arrPellets[i][0] = Integer.parseInt(comp[1]);	//col as x
					arrPellets[i][1] = Integer.parseInt(comp[0]);	//row as y
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
		startNode = new Node(hupmanX, hupmanY);
		addNode(startNode, hupmanX, hupmanY);
	}

	private void addNode(Node thisNode, int currX, int currY) {
		mapNodes.put(currX + "-" + currY, thisNode);
		int wallType = arrMaze[currY][currX];

		//ADD CHILDREN
		//left
		if (currX > 0 && arrMaze[currY][currX-1] != 2 && arrMaze[currY][currX-1] != 3) {
			String sKey = (currX-1) + "-" + currY;
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX - 1, currY);
				addNode(tempNode, currX - 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//right
		if (currX < numCols - 1 && wallType != 2 && wallType != 3) {
			String sKey = (currX+1) + "-" + currY;
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX + 1, currY);
				addNode(tempNode, currX + 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//up
		if (currY > 0 && wallType != 1 && wallType != 3) {
			String sKey = currX + "-" + (currY-1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX, currY - 1);
				addNode(tempNode, currX, currY - 1);
			}

			thisNode.addNode(tempNode);
		}
		//down
		if (currY < numRows - 1 && arrMaze[currY+1][currX] != 1 && arrMaze[currY+1][currX] != 3) {
			String sKey = currX + "-" + (currY+1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX, currY + 1);
				addNode(tempNode, currX, currY + 1);
			}

			thisNode.addNode(tempNode);
		}
	}

	private void resetNodes() {
		for (int i = 0; i<numPellets; i++) {
			int x = arrPellets[i][0];
			int y = arrPellets[i][1];
			mapNodes.get(x + "-" + y).setHasPellet(true);
		}

		for (Map.Entry<String, Node> entry : mapNodes.entrySet())
		{
			entry.getValue().setVisited(false);
			entry.getValue().setParent(null);
		}
	}

	private ArrayList<Node> searchDepth() {
		Stack<Node> stack = new  Stack<>();
		stack.add(startNode);
		startNode.setVisited(true);

		int foundPellets = 0;
		boolean bAllFound = false;
		ArrayList<Node> path = new ArrayList<>();

		while (!stack.isEmpty() && !bAllFound)
		{
			Node thisNode = stack.peek();

			path.add(thisNode);
			if (thisNode.getHasPellet()) {
				foundPellets++;
				thisNode.setHasPellet(false);
			}
			if (foundPellets == numPellets) bAllFound = true;

			int goodNodes = 0;
			ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
			for (int i = 0; i < arrAdj.size(); i++) {
				Node checkNode = arrAdj.get(i);
				if (checkNode != null && !checkNode.getVisited())
				{
					stack.add(checkNode);
					checkNode.setParent(thisNode);
					checkNode.setVisited(true);
					goodNodes++;

					//break the loop and go further down the tree
					//add sibling nodes later
					i = arrAdj.size();
				}
			}

			//if all children have been checked, move back up the graph
			if (goodNodes == 0) stack.pop();
		}

		return path;
	}

	//go to closest pellet, direct lines
	private ArrayList<Node> searchAStar() {
		ArrayList<Node> path = new ArrayList<>();

		return path;
	}

	//https://www.seas.gwu.edu/~simhaweb/champalg/tsp/tsp.html

	//go to closest pellet, actual grid cost
	private ArrayList<Node> searchNearestNeighbor() {
		ArrayList<Node> path = new ArrayList<>();

		return path;
	}

	//switch 2 edges and get new cost
	private ArrayList<Node> search2Op() {
		ArrayList<Node> path = new ArrayList<>();

		return path;
	}


	private void runAllSearches() {
		ArrayList<Node> path;
		resetNodes();

		//depth first
		path = searchDepth();
		resetNodes();
		System.out.println("Depth First: " + path.size() + " moves");
		for (Node p : path) {
			hupmanX = p.getX();
			hupmanY = p.getY();
			p.setHasPellet(false);
			repaint(10);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.print("(" + p.getX() + "," + p.getY() + ") ");
		}
		resetNodes();
		hupmanX = 0;
		hupmanY = 0;
		repaint();

		//A* (direct line heuristic)
		resetNodes();
		path = searchAStar();

	}

	public static void main(String[] args) {
		Hupman hup = new Hupman();
	}
}
