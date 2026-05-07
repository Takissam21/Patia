package fr.uga.pddl4j.yasp;

import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.util.BitVector;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import fr.uga.pddl4j.problem.operator.Effect;
/**
 * This class implements a planning problem/domain encoding into DIMACS
 *
 * @author H. Fiorino
 * @version 0.1 - 30.03.2024
 */
public final class SATEncoding {
    /*
     * A SAT problem in dimacs format is a list of int list a.k.a clauses
     */
    private List<List<Integer>> initList = new ArrayList<List<Integer>>();

    /*
     * Goal
     */
    private List<Integer> goalList = new ArrayList<Integer>();

    /*
     * Actions
     */
    private List<List<Integer>> actionPreconditionList = new ArrayList<List<Integer>>();
    private List<List<Integer>> actionEffectList = new ArrayList<List<Integer>>();

    /*
     * State transistions
     */
    private HashMap<Integer, List<Integer>> addList = new HashMap<Integer, List<Integer>>();
    private HashMap<Integer, List<Integer>> delList = new HashMap<Integer, List<Integer>>();
    private List<List<Integer>> stateTransitionList = new ArrayList<List<Integer>>();

    /*
     * Action disjunctions
     */
    private List<List<Integer>> actionDisjunctionList = new ArrayList<List<Integer>>();

    /*
     * Current DIMACS encoding of the planning domain and problem for #steps steps
     * Contains the initial state, actions and action disjunction
     * Goal is no there!
     */
    public List<List<Integer>> currentDimacs = new ArrayList<List<Integer>>();

    /*
     * Current goal encoding
     */
    public List<Integer> currentGoal = new ArrayList<Integer>();

    /*
     * Current number of steps of the SAT encoding
     */
    private int steps;
    private Problem problem;

    public SATEncoding(Problem problem, int steps) {

        this.problem = problem;
        this.steps = steps;

        // Encoding of init
        // Each fact is a unit clause
        // Init state step is 1
        // We get the initial state from the planning problem
        // State is a bit vector where the ith bit at 1 corresponds to the ith fluent being true
        final int nb_fluents = problem.getFluents().size();
        //System.out.println(" fluents = " + nb_fluents );
        final BitVector init = problem.getInitialState().getPositiveFluents();
        
        
        // Initial state encoding.
        // For each fluent, we add one unit clause:
        // true in the initial state  ->  fluent at step 1
        // false in the initial state ->  not fluent at step 1
        for (int f = 0; f < nb_fluents; f++) {
            List<Integer> clause = new ArrayList<Integer>();
            int var = pair(f + 1, 1);

            if (init.get(f)) {
                clause.add(var);
            } else {
                clause.add(-var);
            }

            initList.add(clause);
        }

        // Goal encoding.
        // The exact time of the goal depends on the number of steps.
        // So here we only store fluent numbers, without the time.
        final BitVector goalPositive = problem.getGoal().getPositiveFluents();
        for (int f = goalPositive.nextSetBit(0); f >= 0; f = goalPositive.nextSetBit(f + 1)) {
            goalList.add(f + 1);
            if (f == Integer.MAX_VALUE) {
                break;
            }
        }

        final BitVector goalNegative = problem.getGoal().getNegativeFluents();
        for (int f = goalNegative.nextSetBit(0); f >= 0; f = goalNegative.nextSetBit(f + 1)) {
            goalList.add(-(f + 1));
            if (f == Integer.MAX_VALUE) {
                break;
            }
        }

        // For state transitions, we need to know which actions can add
        // or delete each fluent.
        for (int f = 1; f <= nb_fluents; f++) {
            addList.put(f, new ArrayList<Integer>());
            delList.put(f, new ArrayList<Integer>());
        }

        for (int a = 0; a < problem.getActions().size(); a++) {
            final Action action = problem.getActions().get(a);
            final int actionId = nb_fluents + a + 1;
            final Effect effect = action.getUnconditionalEffect();

            final BitVector positiveEffects = effect.getPositiveFluents();
            for (int f = positiveEffects.nextSetBit(0); f >= 0; f = positiveEffects.nextSetBit(f + 1)) {
                addList.get(f + 1).add(actionId);
                if (f == Integer.MAX_VALUE) {
                    break;
                }
            }

            final BitVector negativeEffects = effect.getNegativeFluents();
            for (int f = negativeEffects.nextSetBit(0); f >= 0; f = negativeEffects.nextSetBit(f + 1)) {
                delList.get(f + 1).add(actionId);
                if (f == Integer.MAX_VALUE) {
                    break;
                }
            }
        }

        // Makes DIMACS encoding from 1 to steps
        encode(1, steps);
    }
    
    /*
     * SAT encoding for next step
     */
    public void next() {
        this.steps++;
        encode(1, this.steps);
    }

    public String toString(final List<Integer> clause, final Problem problem) {
        final int nb_fluents = problem.getFluents().size();
        List<Integer> dejavu = new ArrayList<Integer>();
        String t = "[";
        String u = "";
        int tmp = 1;
        int [] couple;
        int bitnum;
        int step;
        for (Integer x : clause) {
            if (x > 0) {
                couple = unpair(x);
                bitnum = couple[0];
                step = couple[1];
            } else {
                couple = unpair(- x);
                bitnum = - couple[0];
                step = couple[1];
            }
            t = t + "(" + bitnum + ", " + step + ")";
            t = (tmp == clause.size()) ? t + "]\n" : t + " + ";
            tmp++;
            final int b = Math.abs(bitnum);
            if (!dejavu.contains(b)) {
                dejavu.add(b);
                u = u + b + " >> ";
                if (nb_fluents >= b) {
                    Fluent fluent = problem.getFluents().get(b - 1);
                    u = u + problem.toString(fluent)  + "\n";
                } else {
                    u = u + problem.toShortString(problem.getActions().get(b - nb_fluents - 1)) + "\n";
                }
            }
        }
        return t + u;
    }

    public Plan extractPlan(final List<Integer> solution, final Problem problem) {
        Plan plan = new SequentialPlan();
        HashMap<Integer, Action> sequence = new HashMap<Integer, Action>();

        final int nb_fluents = problem.getFluents().size();
        final int nb_actions = problem.getActions().size();

        for (Integer x : solution) {
            // We only care about positive variables.
            // A positive action variable means that the action is selected.
            if (x > 0) {
                int[] couple = unpair(x);
                int bitnum = couple[0];
                int step = couple[1];

                // We keep only real action variables.
                if (bitnum > nb_fluents
                        && bitnum <= nb_fluents + nb_actions
                        && step >= 1
                        && step <= this.steps) {

                    final Action action = problem.getActions().get(bitnum - nb_fluents - 1);
                    sequence.put(step, action);
                }
            }
        }

        // Add actions in chronological order.
        List<Integer> orderedSteps = new ArrayList<Integer>(sequence.keySet());
        Collections.sort(orderedSteps);

        int time = 0;
        for (Integer s : orderedSteps) {
            plan.add(time, sequence.get(s));
            time++;
        }

        return plan;
    }
    
    // Cantor paring function generates unique numbers
    private static int pair(int num, int step) {
        return (int) (0.5 * (num + step) * (num + step + 1) + step);
    }

    private static int[] unpair(int z) {
        /*
        Cantor unpair function is the reverse of the pairing function. It takes a single input
        and returns the two corespoding values.
        */
        int t = (int) (Math.floor((Math.sqrt(8 * z + 1) - 1) / 2));
        int bitnum = t * (t + 3) / 2 - z;
        int step = z - t * (t + 1) / 2;
        return new int[]{bitnum, step}; //Returning an array containing the two numbers
    }

        private void encode(int from, int to) {
        this.currentDimacs.clear();
        this.currentGoal.clear();

        this.actionPreconditionList.clear();
        this.actionEffectList.clear();
        this.stateTransitionList.clear();
        this.actionDisjunctionList.clear();

        final int nb_fluents = this.problem.getFluents().size();
        final int nb_actions = this.problem.getActions().size();

        // 1. Initial state.
        for (List<Integer> clause : initList) {
            this.currentDimacs.add(new ArrayList<Integer>(clause));
        }

        // 2. For each action step.
        for (int step = 1; step <= to; step++) {

            // 2.1 Action encoding:
            // action -> preconditions
            // action -> positive effects
            // action -> negative effects
            for (int a = 0; a < nb_actions; a++) {
                final Action action = this.problem.getActions().get(a);
                final int actionId = nb_fluents + a + 1;
                final int actionVar = pair(actionId, step);

                // Positive preconditions: action -> fluent
                final BitVector positivePreconditions = action.getPrecondition().getPositiveFluents();
                for (int f = positivePreconditions.nextSetBit(0);
                     f >= 0;
                     f = positivePreconditions.nextSetBit(f + 1)) {

                    List<Integer> clause = new ArrayList<Integer>();
                    clause.add(-actionVar);
                    clause.add(pair(f + 1, step));

                    this.actionPreconditionList.add(clause);
                    this.currentDimacs.add(clause);

                    if (f == Integer.MAX_VALUE) {
                        break;
                    }
                }

                // Negative preconditions: action -> not fluent
                final BitVector negativePreconditions = action.getPrecondition().getNegativeFluents();
                for (int f = negativePreconditions.nextSetBit(0);
                     f >= 0;
                     f = negativePreconditions.nextSetBit(f + 1)) {

                    List<Integer> clause = new ArrayList<Integer>();
                    clause.add(-actionVar);
                    clause.add(-pair(f + 1, step));

                    this.actionPreconditionList.add(clause);
                    this.currentDimacs.add(clause);

                    if (f == Integer.MAX_VALUE) {
                        break;
                    }
                }

                final Effect effect = action.getUnconditionalEffect();

                // Positive effects: action -> fluent at next step
                final BitVector positiveEffects = effect.getPositiveFluents();
                for (int f = positiveEffects.nextSetBit(0);
                     f >= 0;
                     f = positiveEffects.nextSetBit(f + 1)) {

                    List<Integer> clause = new ArrayList<Integer>();
                    clause.add(-actionVar);
                    clause.add(pair(f + 1, step + 1));

                    this.actionEffectList.add(clause);
                    this.currentDimacs.add(clause);

                    if (f == Integer.MAX_VALUE) {
                        break;
                    }
                }

                // Negative effects: action -> not fluent at next step
                final BitVector negativeEffects = effect.getNegativeFluents();
                for (int f = negativeEffects.nextSetBit(0);
                     f >= 0;
                     f = negativeEffects.nextSetBit(f + 1)) {

                    List<Integer> clause = new ArrayList<Integer>();
                    clause.add(-actionVar);
                    clause.add(-pair(f + 1, step + 1));

                    this.actionEffectList.add(clause);
                    this.currentDimacs.add(clause);

                    if (f == Integer.MAX_VALUE) {
                        break;
                    }
                }
            }

            // 2.2 State transitions.
            // If a fluent becomes true, an action must have added it.
            // If a fluent becomes false, an action must have deleted it.
            for (int f = 1; f <= nb_fluents; f++) {

                // not f(step) and f(step+1) -> one action adds f
                // CNF: f(step) or not f(step+1) or add_action_1 or ...
                List<Integer> addClause = new ArrayList<Integer>();
                addClause.add(pair(f, step));
                addClause.add(-pair(f, step + 1));

                for (Integer actionId : addList.get(f)) {
                    addClause.add(pair(actionId, step));
                }

                this.stateTransitionList.add(addClause);
                this.currentDimacs.add(addClause);

                // f(step) and not f(step+1) -> one action deletes f
                // CNF: not f(step) or f(step+1) or del_action_1 or ...
                List<Integer> delClause = new ArrayList<Integer>();
                delClause.add(-pair(f, step));
                delClause.add(pair(f, step + 1));

                for (Integer actionId : delList.get(f)) {
                    delClause.add(pair(actionId, step));
                }

                this.stateTransitionList.add(delClause);
                this.currentDimacs.add(delClause);
            }

            // 2.3 Action disjunction.
            // Simple sequential version: at most one action per step.
            for (int a1 = 0; a1 < nb_actions; a1++) {
                for (int a2 = a1 + 1; a2 < nb_actions; a2++) {
                    final int actionId1 = nb_fluents + a1 + 1;
                    final int actionId2 = nb_fluents + a2 + 1;

                    List<Integer> clause = new ArrayList<Integer>();
                    clause.add(-pair(actionId1, step));
                    clause.add(-pair(actionId2, step));

                    this.actionDisjunctionList.add(clause);
                    this.currentDimacs.add(clause);
                }
            }
        }

        // 3. Goal at the final state.
        // If we have "to" action steps, the final state is "to + 1".
        for (Integer g : goalList) {
            if (g > 0) {
                this.currentGoal.add(pair(g, to + 1));
            } else {
                this.currentGoal.add(-pair(-g, to + 1));
            }
        }

        System.out.println("Encoding : successfully done (" + (this.currentDimacs.size()
                + this.currentGoal.size()) + " clauses, " + to + " steps)");
    }

}
