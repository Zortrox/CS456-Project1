/*
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;

class Node {
	private ArrayList<Node> adjList = new ArrayList<>();
	private int xGrid;
	private int yGrid;
	private boolean hasPellet = false;
	private boolean visited = false;
	private Node parent = null;

	//A* variables
	private int gCost = 0;
	private int hCost = 0;

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

	//A* functions
	void setGCost(int g) {
		gCost = g;
	}

	int getGCost() {
		return gCost;
	}

	void setHCost(int h) {
		hCost = h;
	}

	int getFCost() {
		return hCost + gCost;
	}
}

public class Hupman extends JFrame{

	private int numRows, numCols, numPellets;
	private int[][] arrMaze;
	private int[][] arrPellets;
	private Node startNode;
	private Map<String, Node> mapNodes = new HashMap<>();
	private Map<Node, Map<Node, ArrayList<Node>>> mapPaths = new HashMap<>();
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
		resetPelletNodes();
		createAllPaths();
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

	private void resetPelletNodes() {
		for (int i = 0; i<numPellets; i++) {
			int x = arrPellets[i][0];
			int y = arrPellets[i][1];
			mapNodes.get(x + "-" + y).setHasPellet(true);
		}
	}

	private void resetVisitedNodes() {
		for (Map.Entry<String, Node> entry : mapNodes.entrySet())
		{
			entry.getValue().setVisited(false);
			entry.getValue().setParent(null);
		}
	}

	//get reverse path from end node to initial node
	private ArrayList<Node> reversePath(Node endNode, Node initNode) {
		ArrayList<Node> path = new ArrayList<>();

		Node tempNode = endNode;
		do {
			tempNode = tempNode.getParent();
			path.add(tempNode);
		}
		while (tempNode != null && tempNode != initNode);

		Collections.reverse(path);
		return path;
	}

	//depth first search iterative
	private ArrayList<Node> searchDepth() {
		ArrayList<Node> path = new ArrayList<>();

		Stack<Node> stack = new  Stack<>();
		stack.add(startNode);
		startNode.setVisited(true);

		int foundPellets = 0;
		boolean bAllFound = false;
		boolean bPelletFound = false;

		while (!bAllFound) {
			ArrayList<Node> tempPath = new ArrayList<>();

			Node initNode = stack.peek();
			Node thisNode = null;
			while (!stack.isEmpty() && !bAllFound && !bPelletFound) {
				thisNode = stack.pop();

				if (thisNode.getHasPellet()) {
					foundPellets++;
					thisNode.setHasPellet(false);
					bPelletFound = true;
					tempPath = reversePath(thisNode, initNode);
				}
				if (foundPellets == numPellets) {
					bAllFound = true;
				}

				ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
				for (int i = 0; i < arrAdj.size(); i++) {
					Node checkNode = arrAdj.get(i);
					if (checkNode != null && !checkNode.getVisited()) {
						stack.add(checkNode);
						checkNode.setParent(thisNode);
						checkNode.setVisited(true);
					}
				}
			}

			//add to current path
			path.addAll(tempPath);

			//reset for next search
			stack.clear();
			resetVisitedNodes();
			bPelletFound = false;

			//add first node again
			stack.push(thisNode);
			thisNode.setVisited(true);
		}

		//add final node
		if (stack.peek() != null) path.add(stack.pop());

		return path;
	}

	//breadth first search iterative
	private ArrayList<Node> searchBreadth() {
		ArrayList<Node> path = new ArrayList<>();

		Queue<Node> queue = new LinkedList<>();
		queue.add(startNode);
		startNode.setVisited(true);

		int foundPellets = 0;
		boolean bAllFound = false;
		boolean bPelletFound = false;

		while (!bAllFound) {
			ArrayList<Node> tempPath = new ArrayList<>();

			Node initNode = queue.peek();
			Node thisNode = null;
			while (!queue.isEmpty() && !bAllFound && !bPelletFound) {
				thisNode = queue.remove();

				if (thisNode.getHasPellet()) {
					foundPellets++;
					thisNode.setHasPellet(false);
					bPelletFound = true;
					tempPath = reversePath(thisNode, initNode);
				}
				if (foundPellets == numPellets) {
					bAllFound = true;
				}

				ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
				for (int i = 0; i < arrAdj.size(); i++) {
					Node checkNode = arrAdj.get(i);
					if (checkNode != null && !checkNode.getVisited()) {
						queue.add(checkNode);
						checkNode.setParent(thisNode);
						checkNode.setVisited(true);
					}
				}
			}

			//add to current path
			path.addAll(tempPath);

			//reset for next search
			queue.clear();
			resetVisitedNodes();
			bPelletFound = false;

			//add first node again
			queue.add(thisNode);
			thisNode.setVisited(true);
		}

		//add final node
		if (queue.peek() != null) path.add(queue.remove());

		return path;
	}

	//create paths and distances between pellets (and start) with A*
	private void createAllPaths() {
		ArrayList<Node> pelletNodes = new ArrayList<Node>();
		pelletNodes.add(startNode);
		for (int i = 0; i < numPellets; i++) {
			String key = arrPellets[i][0] + "-" + arrPellets[i][1];
			pelletNodes.add(mapNodes.get(key));
		}

		ArrayList<Node> path = null;
		for (int loops = 0; loops < numPellets; loops++) {
			Node initNode = pelletNodes.get(loops);
			Node goalNode = null;

			for (int inLoops = loops + 1; inLoops <= numPellets; inLoops++) {
				goalNode = pelletNodes.get(inLoops);

				ArrayList<Node> openList = new ArrayList<Node>();
				ArrayList<Node> closedList = new ArrayList<Node>();
				openList.add(initNode); //add starting node to open list

				Node thisNode = null;

				boolean done = false;
				while (!done) {
					thisNode = null;

					//get node with lowest f cost from openList
					int lowestF = -1;
					for (int i = 0; i < openList.size(); i++) {
						int newF = openList.get(i).getFCost();
						if (lowestF < 0) {
							lowestF = newF;
							thisNode = openList.get(i);
						} else if (newF < lowestF) {
							lowestF = newF;
							openList.get(i);
						}
					}

					closedList.add(thisNode); //add current node to closed list
					openList.remove(thisNode); //delete current node from open list

					//found goal
					if (thisNode == goalNode) {
						//get path to node + ending node
						done = true;
						path = reversePath(thisNode, goalNode);
						path.add(thisNode);
					}

					//for all adjacent nodes:
					ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
					for (int i = 0; i < arrAdj.size(); i++) {
						Node nodeAdj = arrAdj.get(i);

						//if not in the openList, add it
						if (!openList.contains(nodeAdj)) {
							int hCost = Math.abs(nodeAdj.getX() - goalNode.getX()) + Math.abs(nodeAdj.getY() - goalNode.getY());

							nodeAdj.setParent(thisNode);
							nodeAdj.setHCost(hCost);
							nodeAdj.setGCost(thisNode.getGCost() + 1);
							openList.add(nodeAdj);
						}
						//else if costs are cheaper, keep it open and lower g cost
						else {
							if (nodeAdj.getGCost() > thisNode.getGCost() + 1) { // costs from current node are cheaper than previous costs
								nodeAdj.setParent(thisNode); // set current node as previous for this node
								nodeAdj.setGCost(thisNode.getGCost() + 1);
							}
						}
					}

					//no path exists
					if (openList.isEmpty()) {
						done = true;
						path = null;
					}
				}
			}

			//add path to initNode and goalNode (reverse)

		}
	}

	//https://www.seas.gwu.edu/~simhaweb/champalg/tsp/tsp.html

	//go to closest pellet, actual grid cost (from A*)
	private ArrayList<Node> searchNearestNeighbor() {
		ArrayList<Node> path = new ArrayList<>();

		return path;
	}

	//switch 2 edges and get new path cost (from A*)
	private ArrayList<Node> search2Op() {
		ArrayList<Node> path = new ArrayList<>();

		return path;
	}


	private void runAllSearches() {
		ArrayList<Node> path;

		//depth first
		path = searchDepth();
		resetPelletNodes();
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
		System.out.println();
		resetVisitedNodes();
		resetPelletNodes();
		hupmanX = 0;
		hupmanY = 0;
		repaint();

		//breadth first
		path = searchBreadth();
		resetPelletNodes();
		System.out.println("Breadth First: " + path.size() + " moves");
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
		System.out.println();
		resetVisitedNodes();
		resetPelletNodes();
		hupmanX = 0;
		hupmanY = 0;
		repaint();



	}

	public static void main(String[] args) {
		Hupman hup = new Hupman();
	}
}
