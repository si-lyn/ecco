package at.jku.isse.ecco.adapter.rust.antlr;
import java.util.ArrayList;
import java.util.List;

public class Permutations {
    public static List<List<String>> permutations(List<String> items, int y) {
        List<List<String>> result = new ArrayList<>();
        if (y < 0 || y > items.size()) return result;
        boolean[] used = new boolean[items.size()];
        backtrack(items, y, new ArrayList<>(), used, result);
        return result;
    }

    private static void backtrack(List<String> items, int y,
                                  List<String> current, boolean[] used,
                                  List<List<String>> result) {
        if (current.size() == y) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (used[i]) continue;
            used[i] = true;
            current.add(items.get(i));
            backtrack(items, y, current, used, result);
            current.remove(current.size() - 1);
            used[i] = false;
        }
    }
}