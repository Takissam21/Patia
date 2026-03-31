import npuzzle as np
import argparse

def main():
    
    parser = argparse.ArgumentParser(description='Generate an n-puzzle and save it to a file.')
    parser.add_argument('-s', '--size', type=int, help='Size of the puzzle (e.g., 3 for a 3x3 puzzle)')
    parser.add_argument('-ml', '--maxlength', type=int, help='Maximum length of the move sequence')
    parser.add_argument('-n', '--number', type=int, help='Number of puzzles of a given length to generate randomly (e.g., 10 for 10 puzzles)')
    parser.add_argument('dirname', default='.', type=str, help='Directory name to save the puzzle')
    parser.add_argument('-v', '--verbose', action='store_true', help='Increase output verbosity')
    
    args = parser.parse_args()
    
    # Generate a puzzle of the given size
    goal_state = np.create_goal(args.size)
    if args.verbose:
        print('Goal state:\n')
        print(np.to_string(goal_state))
    
    # Loop generating puzzles of succesive move lengths
    for length in range(1, args.maxlength + 1):
        
        # Loop generating n puzzles of the given length
        for n in range(0, args.number):
            
            new_state = np.shuffle(goal_state)
            
            for i in range(0, length - 1):
                new_state = np.shuffle(new_state)
                
            if args.verbose:
                print('Puzzle:\n')
                print(np.to_string(new_state))
    
            # Save the puzzle to the specified file
            filename = f"{args.dirname}/npuzzle_{args.size}x{args.size}_len{length}_{n}.txt"
            print(f"Saving puzzle to {filename}")
            np.save_puzzle(new_state, filename)

if __name__ == '__main__':
    main()
