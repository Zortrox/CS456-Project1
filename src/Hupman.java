/*
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
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

public class Hupman extends JPanel{

	private int numRows, numCols, numPellets;
	private int[][] arrMaze;
	private int[][] arrPellets;
	private Node startNode;
	private Map<String, Node> mapNodes = new HashMap<>();
	private Map<Node, Map<Node, ArrayList<Node>>> mapPaths = new HashMap<>();
	private boolean bFileRead = false;
	private int windowWidth = 600;
	private int windowHeight = 600;
	private int gridOffset = 100;
	private int gridSize = 20;
	private int pelletRadius = gridSize / 5;
	private int hupmanRadius = gridSize / 3;
	private int hupmanX = 0;
	private int hupmanY = 0;

	Hupman() {
		loadFile();
		createGraph();
		resetPelletNodes();

		setPreferredSize(new Dimension(windowWidth, windowHeight));

		repaint();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point mousePos = e.getPoint();

				int gridX = (mousePos.x - gridOffset) / gridSize;
				int gridY = (mousePos.y - gridOffset) / gridSize;

				if (gridX >= 0 && gridX < numCols && gridY >= 0 && gridY < numRows) {
					startNode = mapNodes.get(gridX + "-" + gridY);

					hupmanX = gridX;
					hupmanY = gridY;

					paintImmediately(0, 0, windowHeight, windowWidth);

					resetPelletNodes();
					resetVisitedNodes();
					createAllPaths();

					runAllSearches();
				}
			}
		});
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		//g2.clearRect(0, 0, windowWidth, windowHeight);

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

				int scaleFactor = Math.max(numRows, numCols);
				int limitingFactor = Math.min(windowHeight, windowWidth);
				gridSize = (limitingFactor - 2 * gridOffset) / scaleFactor;
				pelletRadius = gridSize / 5;
				hupmanRadius = gridSize / 3;

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
			path.add(tempNode);
			tempNode = tempNode.getParent();
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
		path.add(startNode);

		int foundPellets = 0;
		boolean bAllFound = false;
		boolean bPelletFound = false;

		//metrics
		int nodesSearched = 0;
		int nodesFringe = 0;

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
						nodesSearched++;
					}
				}
			}

			//add to current path
			path.addAll(tempPath);

			nodesFringe += stack.size();

			//reset for next search
			stack.clear();
			resetVisitedNodes();
			bPelletFound = false;

			//add first node again
			stack.push(thisNode);
			thisNode.setVisited(true);
		}

		System.out.println("Depth First - Nodes Searched: " + nodesSearched);
		System.out.println("Depth First - Nodes on Fringe: " + nodesFringe);

		return path;
	}

	//breadth first search iterative
	private ArrayList<Node> searchBreadth() {
		ArrayList<Node> path = new ArrayList<>();

		Queue<Node> queue = new LinkedList<>();
		queue.add(startNode);
		startNode.setVisited(true);
		path.add(startNode);

		int foundPellets = 0;
		boolean bAllFound = false;
		boolean bPelletFound = false;

		//metrics
		int nodesSearched = 0;
		int nodesFringe = 0;

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
						nodesSearched++;
					}
				}
			}

			//add to current path
			path.addAll(tempPath);

			nodesFringe += queue.size();

			//reset for next search
			queue.clear();
			resetVisitedNodes();
			bPelletFound = false;

			//add first node again
			queue.add(thisNode);
			thisNode.setVisited(true);
		}

		System.out.println("Breadth First - Nodes Expanded: " + nodesSearched);
		System.out.println("Breadth First - Nodes on Fringe: " + nodesFringe);

		return path;
	}

	private ArrayList<ArrayList<Integer>> permute(int size) {
		ArrayList<ArrayList<Integer>> permutations = new ArrayList<>();
		permutations.add(new ArrayList<>());
		int[] c = new int[size];

		for (int i = 0; i < size; i++) {
			c[i] = 0;
			permutations.get(0).add(i + 1);
		}

		for (int i = 0; i < size;) {
			if (c[i] < i) {
				ArrayList<Integer> tempP = new ArrayList<>(permutations.get(permutations.size() - 1));

				if (i % 2 == 0) {
					Collections.swap(tempP, 0, i);
				} else {
					Collections.swap(tempP, c[i], i);
				}

				permutations.add(tempP);
				c[i]++;
				i = 0;
			}
			else {
				c[i] = 0;
				i++;
			}
		}

		return permutations;
	}

	//create paths and distances between pellets (and start) with A*
	private void createAllPaths() {
		ArrayList<Node> pelletNodes = new ArrayList<Node>();
		pelletNodes.add(startNode);

		mapPaths = new HashMap<>();
		mapPaths.put(startNode, new HashMap<>());
		for (int i = 0; i < numPellets; i++) {
			String key = arrPellets[i][0] + "-" + arrPellets[i][1];
			pelletNodes.add(mapNodes.get(key));
			mapPaths.put(mapNodes.get(key), new HashMap<>());
		}

		//metrics
		int nodesSearched = 0;
		System.out.println("A* Search Metrics");

		for (int loops = 0; loops < numPellets; loops++) {
			Node initNode = pelletNodes.get(loops);
			Node goalNode = null;

			for (int inLoops = loops + 1; inLoops <= numPellets; inLoops++) {
				ArrayList<Node> path = new ArrayList<>();
				goalNode = pelletNodes.get(inLoops);

				ArrayList<Node> openList = new ArrayList<Node>();
				openList.add(initNode); //add starting node to open list

				Node thisNode = null;

				boolean done = false;
				while (!done) {
					thisNode = null;

					nodesSearched++;

					//get node with lowest f cost from openList
					int lowestF = -1;
					for (int i = 0; i < openList.size(); i++) {
						int newF = openList.get(i).getFCost();
						if (lowestF < 0 || newF < lowestF) {
							lowestF = newF;
							thisNode = openList.get(i);
						}
					}

					thisNode.setVisited(true);
					openList.remove(thisNode); //delete current node from open list

					//found goal
					if (thisNode == goalNode) {
						//get path to node + ending node
						done = true;
						path = reversePath(thisNode, initNode);
					}

					//continue if node wasn't the goal node
					if (!done) {
						//for all adjacent nodes:
						ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
						for (int i = 0; i < arrAdj.size(); i++) {
							Node nodeAdj = arrAdj.get(i);

							//if not in the openList, add it
							if (!openList.contains(nodeAdj) && !nodeAdj.getVisited()) {
								int hCost = Math.abs(nodeAdj.getX() - goalNode.getX()) + Math.abs(nodeAdj.getY() - goalNode.getY());

								nodeAdj.setParent(thisNode);
								nodeAdj.setHCost(hCost);
								nodeAdj.setGCost(thisNode.getGCost() + 1);
								openList.add(nodeAdj);


							}
							//else if costs are cheaper, keep it open and lower g cost
							else {
								if (nodeAdj.getGCost() > thisNode.getGCost() + 1) {
									nodeAdj.setVisited(false);
									nodeAdj.setParent(thisNode);
									nodeAdj.setGCost(thisNode.getGCost() + 1);
									openList.add(nodeAdj);
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

				//add path to initNode and goalNode (reverse) if one was found
				if (path != null) {
					//don't include end node (reverse's start node)
					ArrayList<Node> reversePath = new ArrayList<>(path.subList(0, path.size() - 1));
					Collections.reverse(reversePath);
					reversePath.add(initNode);	//add start node as reverse's end node

					mapPaths.get(initNode).put(goalNode, path);
					mapPaths.get(goalNode).put(initNode, reversePath);

					//reset visited nodes
					resetVisitedNodes();

					String initCoords = "(" + initNode.getX() + "," + initNode.getY() + ")";
					String goalCoords = "(" + goalNode.getX() + "," + goalNode.getY() + ")";

					System.out.println(initCoords + "<->" + goalCoords + " Path Length: " + path.size()
						+ ", Nodes Expanded: " + nodesSearched + ", Nodes on Fringe: " + openList.size());
				} else {
					String initCoords = "(" + initNode.getX() + "," + initNode.getY() + ")";
					String goalCoords = "(" + goalNode.getX() + "," + goalNode.getY() + ")";
					System.out.println(initCoords + "<->" + goalCoords + " No path found.");
				}
			}
		}

		System.out.println();
	}

	//go to closest pellet, actual grid cost (from A*)
	private ArrayList<Node> searchNearestNeighbor() {
		ArrayList<Node> path= new ArrayList<Node>();
		path.add(startNode);

		Node thisNode = startNode;
		for (int i = 0; i < numPellets; i++) {
			Map<Node, ArrayList<Node>> tempMap = mapPaths.get(thisNode);

			Node goalNode = null;
			int dist = -1;
			for (Map.Entry<Node, ArrayList<Node>> entry : tempMap.entrySet()) {
				int tempDist = entry.getValue().size();
				Node tempNode = entry.getKey();
				if ((dist < 0 || tempDist < dist) && tempNode.getHasPellet()) {
					dist = tempDist;
					goalNode = tempNode;
				}
			}

			path.addAll(mapPaths.get(thisNode).get(goalNode));
			thisNode = goalNode;
			goalNode.setHasPellet(false);
		}

		return path;
	}

	//switch within all permutations of the paths
	private ArrayList<Node> searchPermute() {
		ArrayList<Node> path = new ArrayList<>();

		ArrayList<Node> pelletNodes = new ArrayList<Node>();
		pelletNodes.add(startNode);
		for (int i = 0; i < numPellets; i++) {
			String key = arrPellets[i][0] + "-" + arrPellets[i][1];
			pelletNodes.add(mapNodes.get(key));
		}

		ArrayList<ArrayList<Integer>> permutations = permute(pelletNodes.size() - 1);

		//create initial path
		path.add(startNode);
		for (int i = 0; i < pelletNodes.size() - 1; i++) {
			Node thisNode = pelletNodes.get(i);
			Node goalNode = pelletNodes.get(i + 1);
			path.addAll(mapPaths.get(thisNode).get(goalNode));
		}

		//set the distance to be better than
		int bestDist = path.size();

		int pathsTotal = permutations.size();
		int pathsBetter = 0;

		boolean noSwap = false;
		for (int i = 0; i < permutations.size(); i++) {
			ArrayList<Node> tempPath = new ArrayList<>();

			//build the new path
			tempPath.add(startNode);
			for (int k = 0; k < pelletNodes.size() - 1; k++) {
				Node thisNode = k == 0 ? pelletNodes.get(0) : pelletNodes.get(permutations.get(i).get(k - 1));
				Node goalNode = pelletNodes.get(permutations.get(i).get(k));

				//make sure there's a path between
				ArrayList<Node> twoNodes = mapPaths.get(thisNode).get(goalNode);
				if (twoNodes != null) {
					tempPath.addAll(twoNodes);
				}
			}

			//check if the new path is less than
			if (tempPath.size() < path.size()) {
				path = tempPath;
				noSwap = false;
				bestDist = path.size();
				pathsBetter++;
			}
		}

		System.out.println("Total Permutations: " + pathsTotal);
		System.out.println("Better Permutations Found: " + pathsBetter);

		return path;
	}

	private void moveHupman(String pathType, ArrayList<Node> path) {
		//so hupman can eat the pellets again
		resetPelletNodes();

		System.out.println(pathType + ": " + path.size() + " moves");
		for (Node p : path) {
			hupmanX = p.getX();
			hupmanY = p.getY();
			p.setHasPellet(false);
			paintImmediately(0, 0, windowHeight, windowWidth);
			System.out.print("(" + p.getX() + "," + p.getY() + ") ");

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\n");

		resetVisitedNodes();
		resetPelletNodes();
		hupmanX = startNode.getX();
		hupmanY = startNode.getY();
		repaint();
	}

	private void runAllSearches() {
		ArrayList<Node> path;

		//depth first
		path = searchDepth();
		moveHupman("Depth First", path);

		//breadth first
		path = searchBreadth();
		moveHupman("Breadth First", path);

		//path = searchNearestNeighbor();
		moveHupman("Nearest Neighbor", path);

		path = searchPermute();
		moveHupman("Permutation", path);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Hupman");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(200, 200);
		frame.setVisible(true);

		Hupman hup = new Hupman();
		frame.getContentPane().add(hup);

		frame.pack();
	}
}
