package sokoban;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MakePddl {

    public static void make(String[] map, String playerPos, String[] boxPos, String fileName) throws IOException {
        int height = map.length;
        int width = map[0].length();

        ArrayList<String> cells = new ArrayList<>();
        ArrayList<String> goals = new ArrayList<>();
        ArrayList<String> deadlocks = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = map[y].charAt(x);
                if (c != '#') {
                    cells.add(cellName(x, y));
                    if (c == '*') {
                        goals.add(cellName(x, y));
                    }
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = map[y].charAt(x);
                if (c == '#') {
                    continue;
                }
                String cell = cellName(x, y);
                if (!goals.contains(cell) && isStaticCorner(map, x, y)) {
                    deadlocks.add(cell);
                }
            }
        }

        PrintWriter out = new PrintWriter(new FileWriter(fileName));

        out.println("(define (problem sokoban-level)");
        out.println("  (:domain sokoban)");
        out.println();

        out.println("  (:objects");
        out.print("    ");
        for (String cell : cells) {
            out.print(cell + " ");
        }
        out.println("- cell");

        out.print("    ");
        for (int i = 0; i < boxPos.length; i++) {
            out.print("b" + (i + 1) + " ");
        }
        out.println("- box");
        out.println("  )");
        out.println();

        out.println("  (:init");

        int[] player = readPos(playerPos);
        out.println("    (player-at " + cellName(player[0], player[1]) + ")");

        for (int i = 0; i < boxPos.length; i++) {
            int[] box = readPos(boxPos[i]);
            String boxCell = cellName(box[0], box[1]);
            out.println("    (box-at b" + (i + 1) + " " + boxCell + ")");
            if (goals.contains(boxCell)) {
                out.println("    (filled " + boxCell + ")");
            }
        }

        for (int i = 0; i < goals.size(); i++) {
            out.println("    (goal " + goals.get(i) + ")");
        }

        for (int i = 0; i < deadlocks.size(); i++) {
            out.println("    (deadlock " + deadlocks.get(i) + ")");
        }

        for (String cell : cells) {
            if (!isPlayerCell(cell, playerPos) && !isBoxCell(cell, boxPos)) {
                out.println("    (clear " + cell + ")");
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (map[y].charAt(x) == '#') {
                    continue;
                }

                if (y > 0 && map[y - 1].charAt(x) != '#') {
                    out.println("    (up " + cellName(x, y) + " " + cellName(x, y - 1) + ")");
                }
                if (y < height - 1 && map[y + 1].charAt(x) != '#') {
                    out.println("    (down " + cellName(x, y) + " " + cellName(x, y + 1) + ")");
                }
                if (x > 0 && map[y].charAt(x - 1) != '#') {
                    out.println("    (left " + cellName(x, y) + " " + cellName(x - 1, y) + ")");
                }
                if (x < width - 1 && map[y].charAt(x + 1) != '#') {
                    out.println("    (right " + cellName(x, y) + " " + cellName(x + 1, y) + ")");
                }
            }
        }

        out.println("  )");
        out.println();

        out.println("  (:goal");
        out.println("    (and");
        for (int i = 0; i < goals.size(); i++) {
            out.println("      (filled " + goals.get(i) + ")");
        }
        out.println("    )");
        out.println("  )");
        out.println(")");

        out.close();
    }

    private static String cellName(int x, int y) {
        return "c" + x + "_" + y;
    }

    private static int[] readPos(String s) {
        String[] parts = s.trim().split(" ");
        int[] pos = new int[2];
        pos[0] = Integer.parseInt(parts[0]);
        pos[1] = Integer.parseInt(parts[1]);
        return pos;
    }

    private static boolean isPlayerCell(String cell, String playerPos) {
        int[] p = readPos(playerPos);
        return cell.equals(cellName(p[0], p[1]));
    }

    private static boolean isBoxCell(String cell, String[] boxPos) {
        for (int i = 0; i < boxPos.length; i++) {
            int[] b = readPos(boxPos[i]);
            if (cell.equals(cellName(b[0], b[1]))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStaticCorner(String[] map, int x, int y) {
        boolean wallUp = isWall(map, x, y - 1);
        boolean wallDown = isWall(map, x, y + 1);
        boolean wallLeft = isWall(map, x - 1, y);
        boolean wallRight = isWall(map, x + 1, y);

        return (wallUp && wallLeft)
            || (wallUp && wallRight)
            || (wallDown && wallLeft)
            || (wallDown && wallRight);
    }

    private static boolean isWall(String[] map, int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map[y].length()) {
            return true;
        }
        return map[y].charAt(x) == '#';
    }
}
