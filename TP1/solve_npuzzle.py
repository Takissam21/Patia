from npuzzle import (Solution,
                     State,
                     Move,
                     UP, 
                     DOWN, 
                     LEFT, 
                     RIGHT,
                     create_goal,
                     get_children,
                     is_goal,
                     is_solution,
                     load_puzzle,
                     to_string)
from node import Node
from typing import Literal, List
import argparse
import math
import time

BFS = 'bfs'
DFS = 'dfs'
ASTAR = 'astar'
IDDFS = 'iddfs'

def solve_bfs(open : List[Node]) -> Solution:
    '''Solve the puzzle using the BFS algorithm'''
    
    goal_state = create_goal(int(math.sqrt(len(open[0].state))))
    moves = [UP, DOWN, LEFT, RIGHT]
    dimension = int(math.sqrt(len(open[0].state)))
    closed = set()
    
    while open:
        node = open.pop(0)
        state_key = tuple(node.state)
        
        if state_key in closed:
            continue
        
        closed.add(state_key)
        
        if is_goal(node.state, goal_state):
            return node.get_path()
        
        open_states = {tuple(n.state) for n in open}
        children = get_children(node.state, moves, dimension)
        
        for child_state, child_move in children:
            child_key = tuple(child_state)
            if child_key not in closed and child_key not in open_states:
                child_node = Node(state=child_state, move=child_move, cost=node.cost + 1, parent=node)
                open.append(child_node)
    
    return None


def solve_dfs(open : List[Node]) -> Solution:
    '''Solve the puzzle using the DFS algorithm'''

    goal_state = create_goal(int(math.sqrt(len(open[0].state))))
    moves = [UP, DOWN, LEFT, RIGHT]
    dimension = int(math.sqrt(len(open[0].state)))
    visited = set()
    visited.add(tuple(open[0].state))
    
    while open:
        node = open.pop()
        
        if is_goal(node.state, goal_state):
            path = []
            current = node
            while current.parent is not None:
                path.append(current.move)
                current = current.parent
            path.reverse()
            return path
        
        children = get_children(node.state, moves, dimension)
        
        for child_state, child_move in reversed(children):
            child_key = tuple(child_state)
            if child_key not in visited:
                visited.add(child_key)
                child_node = Node(state=child_state, move=child_move, cost=node.cost + 1, parent=node)
                open.append(child_node)
    
    return None

def solve_astar(open : List[Node]) -> Solution:
    '''Solve the puzzle using the A* algorithm'''
    
    goal_state = create_goal(int(math.sqrt(len(open[0].state))))
    moves = [UP, DOWN, LEFT, RIGHT]
    dimension = int(math.sqrt(len(open[0].state)))
    closed = set()
    best_cost = {tuple(open[0].state): 0}
    open[0].heuristic = heuristic(open[0].state, goal_state)
    
    while open:
        best_index = 0
        best_value = open[0].cost + open[0].heuristic
        
        for i in range(1, len(open)):
            value = open[i].cost + open[i].heuristic
            if value < best_value:
                best_value = value
                best_index = i
        
        node = open.pop(best_index)
        state_key = tuple(node.state)
        
        if state_key in closed:
            continue
        
        if is_goal(node.state, goal_state):
            return node.get_path()
        
        closed.add(state_key)
        
        children = get_children(node.state, moves, dimension)
        for child_state, child_move in children:
            child_key = tuple(child_state)
            new_cost = node.cost + 1
            
            if child_key not in closed and (child_key not in best_cost or new_cost < best_cost[child_key]):
                best_cost[child_key] = new_cost
                child_node = Node(state=child_state,
                                  move=child_move,
                                  cost=new_cost,
                                  heuristic=heuristic(child_state, goal_state),
                                  parent=node)
                open.append(child_node)
    
    return None

def heuristic(current_state : State, goal_state : State) -> int:
    '''Calculate the Manhattan distance of the puzzle'''
    
    distance = 0
    dimension = int(math.sqrt(len(current_state)))
    
    for i in range(len(current_state)):
        tile = current_state[i]
        
        if tile != 0:
            goal_index = goal_state.index(tile)
            current_row = i // dimension
            current_col = i % dimension
            goal_row = goal_index // dimension
            goal_col = goal_index % dimension
            
            distance += abs(current_row - goal_row) + abs(current_col - goal_col)
    
    return distance

def depth_limited_search(node: Node, limit: int, goal_state: State, moves: List[Move], dimension: int) -> Solution | None:
    '''Perform a depth-limited search'''
    
    if is_goal(node.state, goal_state):
        return node.get_path()
    
    if limit == 0:
        return None
    
    children = get_children(node.state, moves, dimension)
    
    for child_state, child_move in children:
        if node.parent is not None and child_state == node.parent.state:
            continue
        
        child_node = Node(state=child_state, move=child_move, cost=node.cost + 1, parent=node)
        result = depth_limited_search(child_node, limit - 1, goal_state, moves, dimension)
        
        if result is not None:
            return result
    
    return None

def solve_iddfs(root: Node, max_depth: int) -> Solution:
    '''Solve the puzzle using the Iterative Deepening Depth-First Search algorithm'''
    
    goal_state = create_goal(int(math.sqrt(len(root.state))))
    moves = [UP, DOWN, LEFT, RIGHT]
    dimension = int(math.sqrt(len(root.state)))
    
    for limit in range(max_depth + 1):
        result = depth_limited_search(root, limit, goal_state, moves, dimension)
        if result is not None:
            return result
    
    return None

def main():
    parser = argparse.ArgumentParser(description='Load an n-puzzle and solve it.')
    parser.add_argument('filename', type=str, help='File name of the puzzle')
    parser.add_argument('-a', '--algo', type=str, choices=['bfs', 'dfs', 'astar', 'iddfs'], required=True, help='Algorithm to solve the puzzle')
    parser.add_argument('-v', '--verbose', action='store_true', help='Increase output verbosity')
    parser.add_argument('-d', '--max_depth', type=int, default=100, help='Maximum depth for IDDFS')
    
    args = parser.parse_args()
    
    puzzle = load_puzzle(args.filename)
    
    if args.verbose:
        print('Puzzle:\n')
        print(to_string(puzzle))
    
    if not is_goal(puzzle, create_goal(int(math.sqrt(len(puzzle))))):   
         
        root = Node(state = puzzle, move = None)
        open = [root]
        
        if args.algo == BFS:
            print('BFS\n')
            start_time = time.time()
            solution = solve_bfs(open)
            duration = time.time() - start_time
            if solution:
                print('Solution:', solution)
                print('Valid solution:', is_solution(puzzle, solution))
                print('Duration:', duration)
            else:
                print('No solution')
        elif args.algo == DFS:
            print('DFS\n')
            start_time = time.time()
            solution = solve_dfs(open)
            duration = time.time() - start_time
            if solution:
                print('Solution:', solution)
                print('Valid solution:', is_solution(puzzle, solution))
                print('Duration:', duration)
            else:
                print('No solution')
        elif args.algo == ASTAR:
            print('A*')
            start_time = time.time()
            solution = solve_astar(open)
            duration = time.time() - start_time
            if solution:
                print('Solution:', solution)
                print('Valid solution:', is_solution(puzzle, solution))
                print('Duration:', duration)
        elif args.algo == IDDFS:
            print('IDDFS')
            start_time = time.time()
            solution = solve_iddfs(root, args.max_depth)
            duration = time.time() - start_time
            if solution:
                print('Solution:', solution)
                print('Valid solution:', is_solution(puzzle, solution))
                print('Duration:', duration)        
            else:
                print('No solution')
    else:
        print('Puzzle is already solved')
    
if __name__ == '__main__':
    main()