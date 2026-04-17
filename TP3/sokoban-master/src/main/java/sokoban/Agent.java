package sokoban;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.planners.LogLevel;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Agent {
    public static void main(String[] args) {
        ClassLoader cl = Agent.class.getClassLoader();
        if (cl instanceof javassist.Loader) {
            javassist.Loader jcl = (javassist.Loader) cl;
            for (String pkg : new String[] {
                    "java.", "javax.", "sun.", "com.sun.",
                    "org.w3c.", "org.xml.", "jdk." }) {
                jcl.delegateLoadingOf(pkg);
            }
        }
        Thread.currentThread().setContextClassLoader(cl);

        Scanner in = new Scanner(System.in);

        boolean firstTurn = true;
        int width = 0;
        int height = 0;
        int boxCount = 0;
        String[] map = null;
        List<String> plan = new ArrayList<>();
        int planIndex = 0;

        while (true) {
            if (firstTurn) {
                if (!in.hasNextLine()) break;

                String[] first = in.nextLine().trim().split(" ");
                width = Integer.parseInt(first[0]);
                height = Integer.parseInt(first[1]);
                boxCount = Integer.parseInt(first[2]);

                map = new String[height];
                for (int i = 0; i < height; i++) {
                    map[i] = in.nextLine();
                }
            }

            if (!in.hasNextLine()) break;
            String playerPos = in.nextLine();

            String[] boxPos = new String[boxCount];
            for (int i = 0; i < boxCount; i++) {
                if (!in.hasNextLine()) break;
                boxPos[i] = in.nextLine();
            }

            if (firstTurn) {
                try {
                    MakePddl.make(map, playerPos, boxPos, "pddl/sokoban/from_agent.pddl");
                    plan = solvePddl();
                } catch (Exception e) {
                    System.err.println("PDDL error: " + e.getMessage());
                }
                firstTurn = false;
            }

            if (planIndex < plan.size()) {
                System.out.println(plan.get(planIndex));
                planIndex++;
            } else {
                System.out.println("D");
            }
        }

        in.close();
    }

    private static List<String> solvePddl() throws Exception {
        HSP planner = new HSP();
        planner.setHeuristic(StateHeuristic.Name.FAST_FORWARD);
        planner.setTimeout(24);
        planner.setLogLevel(LogLevel.OFF);

        DefaultParsedProblem parsedProblem = planner.parse(
            "pddl/sokoban/domain.pddl",
            "pddl/sokoban/from_agent.pddl"
        );
        Problem problem = planner.instantiate(parsedProblem);
        Plan plan = planner.solve(problem);

        List<String> moves = new ArrayList<>();
        if (plan == null) {
            System.err.println("No plan found.");
            return moves;
        }

        for (Action action : plan.actions()) {
            String name = action.getName().toLowerCase();
            if (name.contains("up"))         moves.add("U");
            else if (name.contains("down"))  moves.add("D");
            else if (name.contains("left"))  moves.add("L");
            else if (name.contains("right")) moves.add("R");
        }

        System.err.println("Plan found: " + moves.size() + " moves -> " + moves);
        return moves;
    }
}
