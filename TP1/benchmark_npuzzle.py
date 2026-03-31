import os
import time
import matplotlib.pyplot as plt

from npuzzle import load_puzzle
from node import Node
from solve_npuzzle import solve_bfs, solve_dfs, solve_astar


def run_algo(algo, puzzle):
    root = Node(state=puzzle, move=None)
    open_list = [root]

    start_time = time.perf_counter()
    solution = algo(open_list)
    duration = time.perf_counter() - start_time

    if solution is None:
        return None, duration

    return len(solution), duration


def main():
    files = []

    for filename in os.listdir('.'):
        if filename.startswith('npuzzle_') and filename.endswith('.txt'):
            files.append(filename)

    files.sort()

    results = []

    for filename in files:
        puzzle = load_puzzle(filename)

        bfs_length, bfs_time = run_algo(solve_bfs, puzzle)
        dfs_length, dfs_time = run_algo(solve_dfs, puzzle)
        astar_length, astar_time = run_algo(solve_astar, puzzle)

        results.append({
            'file': filename,
            'bfs_length': bfs_length,
            'bfs_time': bfs_time,
            'dfs_length': dfs_length,
            'dfs_time': dfs_time,
            'astar_length': astar_length,
            'astar_time': astar_time
        })

    results.sort(key=lambda x: (x['bfs_length'], x['bfs_time']))

    print('Results:')
    for result in results:
        print(result['file'])
        print('BFS   :', round(result['bfs_time'], 6), 's', '- length =', result['bfs_length'])
        print('DFS   :', round(result['dfs_time'], 6), 's', '- length =', result['dfs_length'])
        print('A*    :', round(result['astar_time'], 6), 's', '- length =', result['astar_length'])
        print()

    graph_results = []
    for result in results:
        if result['bfs_length'] is not None and result['bfs_length'] > 0:
            graph_results.append(result)

    x = list(range(1, len(graph_results) + 1))
    bfs_times = [result['bfs_time'] for result in graph_results]
    dfs_times = [result['dfs_time'] for result in graph_results]
    astar_times = [result['astar_time'] for result in graph_results]

    tick_step = max(1, len(x) // 12)
    tick_positions = x[::tick_step]
    if x and tick_positions[-1] != x[-1]:
        tick_positions.append(x[-1])

    plt.figure(figsize=(12, 6))
    plt.plot(x, bfs_times, marker='o', label='BFS')
    plt.plot(x, dfs_times, marker='o', label='DFS')
    plt.plot(x, astar_times, marker='o', label='A*')
    plt.xticks(tick_positions)
    plt.xlabel('Instances sorted by BFS difficulty')
    plt.ylabel('Resolution time (s)')
    plt.title('N-Puzzle benchmark - all methods')
    plt.yscale('log')
    plt.legend()
    plt.tight_layout()

    output_path = os.path.join(os.getcwd(), 'benchmark_npuzzle_all.png')
    plt.savefig(output_path, dpi=300)
    print('Graph saved to:', output_path)
    plt.show()


if __name__ == '__main__':
    main()
