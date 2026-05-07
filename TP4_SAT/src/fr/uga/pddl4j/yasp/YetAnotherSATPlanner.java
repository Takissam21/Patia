package fr.uga.pddl4j.yasp;

import fr.uga.pddl4j.heuristics.state.FastForward;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.ErrorManager;
import fr.uga.pddl4j.parser.Message;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.State;
import fr.uga.pddl4j.planners.statespace.AbstractStateSpacePlanner;
import fr.uga.pddl4j.planners.LogLevel;
import fr.uga.pddl4j.plan.Plan;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.uga.pddl4j.problem.operator.Action;
/**
 * The class shows how to use PDDL4J + SAT4J libraries to create a SAT planner.
 *
 * @version 0.1 - 29.03.2024
 */
public class YetAnotherSATPlanner extends AbstractStateSpacePlanner {

    /**
     * The main method the class. The first argument must be the path to the PDDL domain description and the second
     * argument the path to the PDDL problem description.
     *
     * @param args the command line arguments.
     */

    static final int MAXSTEPS = 50;
    // SAT solver max number of var
    static final int MAXVAR = 1000000;
    // SAT solver max number of clauses
    static final int NBCLAUSES = 500000;
    // SAT solver timeout
    static final int TIMEOUT = 3600;

    static final boolean DEBUG = false;
    static final String PLAN_OUTPUT_FILE = "sat_plan.txt";

    /**
     * Instantiates the planning problem from a parsed problem.
     *
     * @param problem the problem to instantiate.
     * @return the instantiated planning problem or null if the problem cannot be instantiated.
     */
    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }

    /**
     * Solves the planning problem and returns the first solution found.
     *
     * @param problem the problem to be solved.
     * @return a solution search or null if it does not exist.
     */
    @Override
    public Plan solve(final Problem problem) {

        int stepmax = MAXSTEPS;
        Plan plan = null;

        // Compute a heuristic lower bound for plan steps
        final FastForward ff = new FastForward(problem);
        final int hlb = ff.estimate(new State(problem.getInitialState()), problem.getGoal());
        if (hlb > MAXSTEPS) {
            System.out.println("Problem has no solution in " + MAXSTEPS + " steps!");
            System.out.println("At least " + hlb + " steps are necessary.");
            System.exit(0);
        } else {

            // Intial number of steps of the SAT encoding
            int steps = hlb;
            if (steps < 0) {
                steps = 0;
            }

            // Create the SAT encoding
            SATEncoding sat = new SATEncoding(problem, steps);

            // Create the SAT solver
            
            // Seach starts here!
            boolean doSearch = true;

            while (doSearch && !(steps > stepmax)) {
                
                

                System.out.println("Trying with " + steps + " action step(s)");

                // We create a fresh SAT solver for each horizon.
                // This is simpler than trying to remove old clauses.
                final ISolver solver = SolverFactory.newDefault();
                solver.setTimeout(TIMEOUT);

                // Compute the maximum SAT variable used in the current encoding.
                int maxVar = 1;
                int nbClauses = sat.currentDimacs.size() + sat.currentGoal.size();

                for (List<Integer> clause : sat.currentDimacs) {
                    for (Integer literal : clause) {
                        maxVar = Math.max(maxVar, Math.abs(literal));
                    }
                }

                for (Integer literal : sat.currentGoal) {
                    maxVar = Math.max(maxVar, Math.abs(literal));
                }

                solver.newVar(maxVar);
                solver.setExpectedNumberOfClauses(nbClauses);

                try {
                    // Add all clauses except the goal.
                    for (List<Integer> clause : sat.currentDimacs) {
                        int[] literals = new int[clause.size()];

                        for (int i = 0; i < clause.size(); i++) {
                            literals[i] = clause.get(i);
                        }

                        solver.addClause(new VecInt(literals));
                    }

                    // Add the goal as unit clauses.
                    for (Integer goalLiteral : sat.currentGoal) {
                        solver.addClause(new VecInt(new int[] { goalLiteral }));
                    }

                    IProblem ip = solver;

                    if (ip.isSatisfiable()) {
                        System.out.println("SAT solution found with " + steps + " action step(s)");

                        final int[] model = ip.model();
                        final List<Integer> solution = Arrays.stream(model)
                                .boxed()
                                .collect(Collectors.toList());

                        plan = sat.extractPlan(solution, problem);
                        doSearch = false;

                    } else {
                        System.out.println("No plan with " + steps + " action step(s)");

                        steps++;
                        if (steps <= stepmax) {
                            sat.next();
                        }
                    }

                } catch (ContradictionException e) {
                    System.out.println("Contradiction with " + steps + " action step(s)");

                    steps++;
                    if (steps <= stepmax) {
                        sat.next();
                    }

                } catch (TimeoutException e) {
                    System.out.println("SAT solver timeout.");
                    doSearch = false;
                }
            }
        }
        return plan;
    }

    private static void writePlanToFile(final Plan plan, final Problem problem, final String fileName) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(fileName));

            for (Action action : plan.actions()) {
                String actionText = problem.toShortString(action);

                // Clean spaces so that VAL gets a simple PDDL-like plan line.
                actionText = actionText.replaceAll("\\s+", " ");
                actionText = actionText.replace("( ", "(");
                actionText = actionText.trim();

                // VAL expects one PDDL action per line, usually with parentheses.
                if (!actionText.startsWith("(")) {
                    actionText = "(" + actionText + ")";
                }

                out.println(actionText);
            }

            out.close();
            System.out.println("Plan written for VAL in file: " + fileName);

        } catch (IOException e) {
            System.out.println("Could not write plan file: " + e.getMessage());
        }
    }


    public static void main(final String[] args) {

        // Checks the number of arguments from the command line
        if (args.length != 2) {
            System.out.println("Invalid command line");
            return;
        }

        try {
            // Creates an instance of the PDDL parser
            final Parser parser = new Parser();
            parser.setLogLevel(LogLevel.OFF);
            // Parses the domain and the problem files.
            final DefaultParsedProblem parsedProblem = parser.parse(args[0], args[1]);
            // Gets the error manager of the parser
            final ErrorManager errorManager = parser.getErrorManager();
            // Checks if the error manager contains errors
            if (!errorManager.isEmpty()) {
                // Prints the errors
                for (Message m : errorManager.getMessages()) {
                    System.out.println(m.toString());
                }
            } else {
                
                // Creates an instance of the SAT planner
                final YetAnotherSATPlanner planner = new YetAnotherSATPlanner();

                // Prints that the domain and the problem were successfully parsed
                System.out.print("\nparsing domain file \"" + args[0] + "\" done successfully");
                System.out.print("\nparsing problem file \"" + args[1] + "\" done successfully\n\n");
                
                // Create a problem
                final Problem problem = planner.instantiate(parsedProblem);

                // Check if the goal is trivially unsatisfiable
                if (!problem.isSolvable()) {
                    System.out.println("Goal can be simplified to FALSE. No search will solve it");
                    System.exit(0);
                } else {
                    
                    Plan plan = planner.solve(problem);
                        
                        if (plan != null) {
                            System.out.println(problem.toString(plan));
                            writePlanToFile(plan, problem, PLAN_OUTPUT_FILE);
                       }else {
                            System.out.println("No solution found!");
                        }
                }
            }
            // This exception could happen if the domain or the problem does not exist
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}