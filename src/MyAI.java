
/*

AUTHOR:      John Lu

DESCRIPTION: This file contains your agent class, which you will
             implement.

NOTES:       - If you are having trouble understanding how the shell
               works, look at the other parts of the code, as well as
               the documentation.

             - You are only allowed to make changes to this portion of
               the code. Any changes to other portions of the code will
               be lost when the tournament runs your code.
*/

package src;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import src.Action.ACTION;
import src.Action;

public class MyAI extends AI {
	// ########################## INSTRUCTIONS ##########################
	// 1) The Minesweeper Shell will pass in the board size, number of mines
	// and first move coordinates to your agent. Create any instance variables
	// necessary to store these variables.
	//
	// 2) You MUST implement the getAction() method which has a single parameter,
	// number. If your most recent move is an Action.UNCOVER action, this value will
	// be the number of the tile just uncovered. If your most recent move is
	// not Action.UNCOVER, then the value will be -1.
	//
	// 3) Feel free to implement any helper functions.
	//
	// ###################### END OF INSTURCTIONS #######################

	// This line is to remove compiler warnings related to using Java generics
	// if you decide to do so in your implementation.
	
	// build using make -f Makefile in openlab
	// make -f Makefile
	// for running -> java -jar bin/mine.jar -f ../WorldGenerator/Problems/ in openlab
	// in eclipse -f "C://My Drive/Masters 2018-2019/Fall 2018/AI/Project/Worlds/Problems"
	@SuppressWarnings("unchecked")

	int remainingMines, totalMines;
	int xDimension, yDimension;
	Stack<Action> actions;
	Cell lastCell;
	WorldState world;
	Set<String> actionSet;

	// constructor
	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		// ################### Implement Constructor (required) ####################
		xDimension = colDimension;
		yDimension = rowDimension;
		world = new WorldState(xDimension, yDimension);

		this.totalMines = totalMines;
		this.remainingMines = totalMines;
		actions = new Stack<Action>();
		actionSet = new HashSet<String>();
		String currentCellId = String.valueOf(startX) + "+" + String.valueOf(startY);
		lastCell = world.getWorld().get(currentCellId);
		lastCell.setState('U');
	}

	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		if (number == -1) {
			lastCell.setState('F');
			lastCell.setValue(-1);
			world.setItem(lastCell, number, 'F');
		} else {
			lastCell.setState('U');
			lastCell.setValue(number);
			world.setItem(lastCell, number, 'U');
		}
		updateActions(lastCell);
		
		/*if (xDimension * yDimension == (world.openCount + world.flagCount))
			return new Action(ACTION.LEAVE);*/
			if(!actions.isEmpty())
				return runProperAction();
			tune();
			if (!actions.isEmpty())
				return runProperAction();
			findPatterns();
			if (!actions.isEmpty())
				return runProperAction();
			getRandom();
			return runProperAction();
	}

	private void updateActions(Cell cell) {
		for (Cell c : world.getNeighboringCells(cell, true)) {
			if (c.getX() <= 0 || c.getX() > xDimension || c.getY() <= 0 || c.getY() > yDimension)
				continue;
			if (c.getState() == 'U') {
				Set<Cell> covered = new HashSet<Cell>();
				Set<Cell> flagged = new HashSet<Cell>();
				for (Cell c2 : world.getNeighboringCells(c, false)) {
					if (c2.getX() <= 0 || c2.getX() > xDimension || c2.getY() <= 0 || c2.getY() > yDimension)
						continue;
					if (c2.getState() == 'C')
						covered.add(c2);
					else if (c2.getState() == 'F')
						flagged.add(c2);
				}
				if (flagged.size() == c.getValue()) {
					for (Cell c3 : covered) {
						if (!actionSet.contains(c3.getX() + "+" + c3.getY())) {
							actions.push(new Action(ACTION.UNCOVER, c3.getX(), c3.getY()));
							actionSet.add(c3.getX() + "+" + c3.getY());
						}
					}
				} else if (flagged.size() + covered.size() == c.getValue()) {
					for (Cell c3 : covered) {
						if (!actionSet.contains(c3.getX() + "+" + c3.getY())) {
							actions.push(new Action(ACTION.FLAG, c3.getX(), c3.getY()));
							actionSet.add(c3.getX() + "+" + c3.getY());
						}
					}
				}
			}
		}
	}

	private Action runProperAction() {
		Action a = actions.pop();
		this.lastCell = world.getWorld().get(a.x + "+" + a.y);
		if (a.action == ACTION.FLAG) {
			this.remainingMines -= 1;
			return new Action(ACTION.FLAG, a.x, a.y);
		} else if (a.action == ACTION.UNCOVER) {
			return new Action(ACTION.UNCOVER, a.x, a.y);
		}
		return null;
	}

	private void getRandom() {
		Random rand = new Random();
		Cell c = new ArrayList<Cell>(world.remainingCells).get(rand.nextInt(world.remainingCells.size()));

		if (!actionSet.contains(c.getX() + "+" + c.getY())) {
			actions.push(new Action(ACTION.UNCOVER, c.getX(), c.getY()));
			actionSet.add(c.getX() + "+" + c.getY());
		} else {
			getRandom();
		}
		
		
	}

	Map<String, List<Cell>> itoc = null;
	
	private void tune() {
		Map<Cell, Integer> visited = new HashMap<Cell, Integer>();
		itoc = new HashMap<String, List<Cell>>();
		//Map<String, List<Cell>> itoc = new HashMap<String, List<Cell>>();
		Iterator<Cell> it = world.uncompletedUncoveredCells.iterator();
		for (int i = 0; i < world.uncompletedUncoveredCells.size(); i++) {
			Cell cur = it.next();
			if (visited.containsKey(cur))
				continue;
			dfs(visited, itoc, i, cur, null);
		}
		Set<Integer> indices = new TreeSet<Integer>(new Comparator<Integer>() {
			@Override
			public int compare(Integer a1, Integer a2) {
				return itoc.get(String.valueOf(a1) + "C").size() +itoc.get(String.valueOf(a1) + "U").size()  - itoc.get(String.valueOf(a2) + "U").size() - itoc.get(String.valueOf(a2) + "C").size();
			}
		});
		for (String str : itoc.keySet()) {
			int i = Integer.parseInt(str.substring(0, str.length() - 1));
			indices.add(i);
		}
		for (Integer id : indices) {
			int n = itoc.get(String.valueOf(id) + "C").size();
			if(xDimension == yDimension && xDimension <=8) {
				if (n > 26)
					break;
			}
			
			if(xDimension == yDimension && xDimension ==16) {
				if (n > 23)
					break;
			}
			
			if(xDimension != yDimension) {
				if (n > 21)
					break;
			}
			
			int alwaysCovered = -1, alwaysUncovered = -1;
			int max = (int) Math.pow(2, n) - 1;
			for (int i = 0; i < max + 1; i++) {
				if (isValid(i, itoc.get(String.valueOf(id) + "C"), itoc.get(String.valueOf(id) + "U"))) {
					if (alwaysCovered == -1) {
						alwaysCovered = i;
						alwaysUncovered = ((~i)&max);
					} else {
						alwaysCovered &= i;
						alwaysUncovered &= ((~i)&max);
					}
				}
			}

			if (alwaysCovered != 0 && alwaysCovered !=-1) {
				for (int i = 0; i < n; i++) {
					if ((alwaysCovered & (1 << i)) !=0) {
						Cell c = itoc.get(String.valueOf(id) + "C").get(i);
						if (!actionSet.contains(c.getX() + "+" + c.getY())) {
							actions.push(new Action(ACTION.FLAG, c.getX(), c.getY()));
							actionSet.add(c.getX() + "+" + c.getY());
						}
					}
				}
			}
			if (alwaysUncovered != 0 && alwaysUncovered !=-1) {
				for (int i = 0; i < n; i++) {
					if ((alwaysUncovered & (1 << i)) !=0) {
						Cell c = itoc.get(String.valueOf(id) + "C").get(i);
						if (!actionSet.contains(c.getX() + "+" + c.getY())) {
							actions.push(new Action(ACTION.UNCOVER, c.getX(), c.getY()));
							actionSet.add(c.getX() + "+" + c.getY());
						}
					}
				}
			}
			if(alwaysUncovered !=0 || alwaysCovered !=0 ) {
				break;
			}
		}
	}


	private void dfs(Map<Cell, Integer> visited, Map<String, List<Cell>> itoc, int pidx, Cell c,
			Character nextNeighborType) {
		if (nextNeighborType == null) {
			nextNeighborType = new Character('C');
		}
		if (visited.containsKey(c))
			return;
		visited.put(c, pidx);
		String k;
		if (nextNeighborType == 'C')
			k = String.valueOf(pidx) + "U";
		else
			k = String.valueOf(pidx) + "C";
		if (itoc.containsKey(k))
			itoc.get(k).add(c);
		else {
			List<Cell> ll = new ArrayList<Cell>();
			ll.add(c);
			itoc.put(k, ll);
		}
		List<Cell> neighbors = world.getNeighboringCells(c, false);
		for (Cell neighbor : neighbors) {
			if (neighbor.getState() == nextNeighborType) {
				if (nextNeighborType == 'C') {
					dfs(visited, itoc, pidx, neighbor, 'U');
				} else
					dfs(visited, itoc, pidx, neighbor, 'C');
			}
		}
	}

	public void findPatterns() {

		for (int i = 1; i <= xDimension; i++) {
			for (int j = 1; j <= yDimension; j++) {

				if (i != 1) {
					if (j + 1 <= yDimension
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (j + 1 <= yDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1))
										.getValue() == 2) {
							if (j + 2 <= yDimension && i + 1 <= xDimension && world.getWorld()
									.get(String.valueOf(i) + "+" + String.valueOf(j + 2)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 2))
												.getX(),
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 2))
												.getY()));
								actionSet.add((i + 1) + "+" + (j + 2));
							}
						}
					}
				}

				if (i != xDimension) {
					if (j + 1 <= yDimension
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (j + 1 <= yDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1))
										.getValue() == 2) {
							if (j + 2 <= yDimension && i - 1 >= 0 && world.getWorld()
									.get(String.valueOf(i) + "+" + String.valueOf(j + 2)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 2))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 2))
												.getY()));
								actionSet.add((i - 1) + "+" + (j + 2));
							}
						}
					}
				}

				if (i != 1) {
					if (j + 1 <= yDimension
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (j + 1 <= yDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 2
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1))
										.getValue() == 1) {
							if (j - 1 >= 0 && i + 1 <= xDimension && world.getWorld()
									.get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i + 1) + "+" + (j - 1));
							}
						}
					}
				}

				if (i != xDimension) {
					if (j + 1 <= yDimension
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (j + 1 <= yDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 2
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1))
										.getValue() == 1) {
							if (j - 1 >= 0 && i - 1 >= 0 && world.getWorld()
									.get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i - 1) + "+" + (j - 1));
							}
						}
					}
				}

				if (j != 1) {
					if (i + 1 <= xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 2
						if (i + 1 <= xDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1
								&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j))
										.getValue() == 2) {
							if (i + 2 <= xDimension && j + 1 <= yDimension && world.getWorld()
									.get(String.valueOf(i + 2) + "+" + String.valueOf(j)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j + 1))
												.getX(),
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j + 1))
												.getY()));
								actionSet.add((i + 2) + "+" + (j + 1));
							}
						}
					}
				}

				if (j != yDimension) {
					if (i + 1 <= xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (i + 1 <= xDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1
								&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j))
										.getValue() == 2) {
							if (i + 2 <= xDimension && j - 1 >= 0 && world.getWorld()
									.get(String.valueOf(i + 2) + "+" + String.valueOf(j)).getValue() == -1) {

								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i + 2) + "+" + (j - 1));
							}
						}
					}
				}

				if (j != 1) {
					if (i + 1 <= xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 2
						if (i + 1 <= xDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 2
								&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j))
										.getValue() == 1) {
							if (i - 1 >= 0 && j + 1 <= yDimension && world.getWorld()
									.get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
												.getY()));
								actionSet.add((i - 1) + "+" + (j + 1));
							}
						}
					}
				}

				if (j != yDimension) {
					if (i + 1 <= xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 2
						if (i + 1 <= xDimension
								&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 2
								&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j))
										.getValue() == 1) {
							if (i - 1 >= 0 && j - 1 >= 0 && world.getWorld()
									.get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == -1) {
								actions.push(new Action(ACTION.FLAG,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i - 1) + "+" + (j - 1));
							}
						}
					}
				}

				if (i != 1) {
					if (j == 0 && world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 1 at edge
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 1) {
							if (i + 1 <= xDimension && world.getWorld()
									.get(String.valueOf(i) + "+" + String.valueOf(j + 2)).getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 2))
												.getX(),
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 2))
												.getY()));
								actionSet.add((i + 1) + "+" + (j + 2));
							}
						}
					}
				}

				if (i != xDimension) {
					if (j == 0 && world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 1) {
							if (i - 1 >= 0 && world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 2))
									.getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 2))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 2))
												.getY()));
								actionSet.add((i - 1) + "+" + (j + 2));
							}
						}
					}
				}

				if (i != 1) {
					if (j == yDimension
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 1) {
							if (i + 1 <= xDimension && world.getWorld()
									.get(String.valueOf(i + 1) + "+" + String.valueOf(j - 2)).getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 2))
												.getX(),
										world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 2))
												.getY()));
								actionSet.add((i + 1) + "+" + (j - 2));
							}
						}
					}
				}

				if (i != xDimension) {
					if (j == yDimension
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 1) {
							if (i - 1 >= 0 && world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 2))
									.getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 2))
												.getX(),
										world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 2))
												.getY()));
								actionSet.add((i - 1) + "+" + (j - 2));
							}
						}
					}
				}

				if (j != 1) {
					if (i == 0 && world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 1 at edge
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 1) {
							if (j + 1 <= yDimension && world.getWorld()
									.get(String.valueOf(i + 2) + "+" + String.valueOf(j)).getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j + 1))
												.getX(),
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j + 1))
												.getY()));
								actionSet.add((i + 2) + "+" + (j + 1));
							}
						}
					}
				}

				if (j != yDimension) {
					if (i == 0 && world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i + 1) + "+" + String.valueOf(j)).getValue() == 1) {
							if (j - 1 >= 0 && world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j))
									.getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i + 2) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i + 2) + "+" + (j - 1));
							}
						}
					}
				}

				if (j != 1) {
					if (i == xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j - 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j - 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 1) {
							if (j + 1 <= yDimension && world.getWorld()
									.get(String.valueOf(i - 2) + "+" + String.valueOf(j + 1)).getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i - 2) + "+" + String.valueOf(j + 1))
												.getX(),
										world.getWorld().get(String.valueOf(i - 2) + "+" + String.valueOf(j + 1))
												.getY()));
								actionSet.add((i - 2) + "+" + (j + 1));
							}
						}
					}
				}

				if (j != yDimension) {
					if (i == xDimension
							&& world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j + 1)).getValue() == 0
							&& world.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j + 1))
									.getValue() == 0) { // 1 1
						if (world.getWorld().get(String.valueOf(i) + "+" + String.valueOf(j)).getValue() == 1 && world
								.getWorld().get(String.valueOf(i - 1) + "+" + String.valueOf(j)).getValue() == 1) {
							if (j - 1 >= 0 && world.getWorld().get(String.valueOf(i - 2) + "+" + String.valueOf(j - 1))
									.getValue() == -1) {
								actions.push(new Action(ACTION.UNCOVER,
										world.getWorld().get(String.valueOf(i - 2) + "+" + String.valueOf(j - 1))
												.getX(),
										world.getWorld().get(String.valueOf(i - 2) + "+" + String.valueOf(j - 1))
												.getY()));
								actionSet.add((i - 2) + "+" + (j - 1));
							}
						}
					}
				}
			}
		}
	}

	private boolean isValid(int combination, List<Cell> coveredCells, List<Cell> uncoveredCells) {
		Set<Cell> possibleMines = new HashSet<Cell>();
		for (int i = 0; i < coveredCells.size(); i++) {
			if ((combination & (1 << i)) != 0)
				possibleMines.add(coveredCells.get(i));
		}
		for (Cell c : uncoveredCells) {
			int f = world.getStates(c).get(2);
			int count = c.getValue();
			char type = c.getState();
			if (type != 'U')
				continue;
			List<Cell> neighbors = world.getNeighboringCells(c, false);
			for (Cell neighbor : neighbors) {
				if (possibleMines.contains(neighbor))
					f += 1;
			}
			if (f != count)
				return false;
		}
		return true;
	}
}

class Cell {
	private int x;
	private int y;
	private char state;
	private int value;

	public Cell(int x, int y, char val) {
		super();
		this.x = x;
		this.y = y;
		this.state = val;
		this.value = -1;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int val) {
		this.x = val;
	}

	public void setY(int val) {
		this.y = val;
	}

	public char getState() {
		return state;
	}

	public void setState(char val) {
		this.state = val;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int val) {
		this.value = val;
	}
}

class WorldState {
	int xPos, yPos;
	Map<String, Cell> world;
	int openCount = 0, flagCount = 0;
	Set<Cell> remainingCells;
	Set<Cell> uncoveredCells, uncompletedUncoveredCells, completedUncoveredCells, flaggedCells;
	public WorldState(int x, int y) {

		xPos = x;
		yPos = y;

		world = new HashMap<String, Cell>();
		for (int i = 1; i <= x; i++)
			for (int j = 1; j <= y; j++)
				world.put((String.valueOf(i) + "+" + String.valueOf(j)), new Cell(i, j, 'C'));
		remainingCells = new HashSet<Cell>(world.values());
		uncoveredCells = new HashSet<Cell>();
		uncompletedUncoveredCells = new HashSet<Cell>();
		completedUncoveredCells = new HashSet<Cell>();
		flaggedCells = new HashSet<Cell>();
	}

	public List<Cell> getNeighboringCells(Cell c, boolean includeSelf) {
		List<Cell> neighbourList = new ArrayList<Cell>();
		int xMax = this.xPos;
		int yMax = this.yPos;
		int x = c.getX();
		int y = c.getY();
		int[][] deltas = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
		for (int i = 0; i < deltas.length; i++) {
			if (x + deltas[i][0] > 0 && x + deltas[i][0] <= xMax && y + deltas[i][1] > 0 && y + deltas[i][1] <= yMax)
				neighbourList.add(world.get(String.valueOf(x + deltas[i][0]) + "+" + String.valueOf(y + deltas[i][1])));
		}
		if (includeSelf)
			neighbourList.add(world.get(String.valueOf(x) + "+" + String.valueOf(y)));
		return neighbourList;
	}

	private void completeCells(int xPos, int yPos) {
		List<Cell> neighbourCells = getNeighboringCells(
				this.world.get(String.valueOf(xPos) + "+" + String.valueOf(yPos)), true);
		for (Cell cell : neighbourCells) {
			if (!this.uncompletedUncoveredCells.contains(cell))
				continue;
			int c = 0;
			for (Cell cellLocal : getNeighboringCells(cell, false)) {
				if (cellLocal.getX() <= 0 || cellLocal.getX() > xPos || cellLocal.getY() <= 0
						|| cellLocal.getY() > yPos)
					continue;
				if (cellLocal.getValue() == -1) {
					c += 1;
					break;
				}
			}
			if (c == 0) {
				this.uncompletedUncoveredCells.remove(cell);
				this.completedUncoveredCells.add(cell);
			}
		}
	}

	public List<Integer> getStates(Cell c) {
		List<Integer> retList = new ArrayList<Integer>();
		List<Cell> neighbourCells = getNeighboringCells(c, false);
		int covered = 0, unCovered = 0, flagged = 0;
		for (Cell nc : neighbourCells) {
			switch (nc.getState()) {
			case 'U': {
				unCovered++;
				break;
			}
			case 'C': {
				covered++;
				break;
			}
			case 'F': {
				flagged++;
				break;
			}
			default:
				break;
			}
		}
		retList.add(covered);
		retList.add(unCovered);
		retList.add(flagged);
		return retList;
	}

	public Map<String, Cell> getWorld() {
		return world;
	}

	public void setItem(Cell c, int val, char s) {
		if (s == 'F') {
			this.flagCount++;
			c.setValue(-2);
			this.flaggedCells.add(c);
		} else {
			this.openCount++;
			c.setValue(val);
			this.uncoveredCells.add(c);
			this.uncompletedUncoveredCells.add(c);
		}
		remainingCells.remove(c);
		completeCells(c.getX(), c.getY());
	}

	public int getItem(int x, int y) {
		Cell c = getWorld().get(x + "+" + y);
		if (c.getValue() == -2)
			return -1;
		if (c.getValue() == -1)
			return -1;
		return c.getValue();
	}

}
