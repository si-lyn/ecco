package at.jku.isse.ecco.adapter.rust;

import java.util.*;

public class Picks {
    // Generate all combinations (unordered) of size n from list 'items'
    private static void combinations(List<String> items, int n, int start,
                                     List<String> curr, List<List<String>> result) {
        if (curr.size() == n) {
            result.add(new ArrayList<>(curr));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            curr.add(items.get(i));
            combinations(items, n, i + 1, curr, result);
            curr.remove(curr.size() - 1);
        }
    }

    // For each combination, choose y from remaining items (not in the combination)
    public static List<List<String>> generatePicks(List<String> items, int n) {
        List<List<String>> picks = new ArrayList<>();
        List<List<String>> combs = new ArrayList<>();
        combinations(items, n, 0, new ArrayList<>(), combs);

        for (List<String> comb : combs) {
            // create a set for fast membership test
            Set<String> combSet = new HashSet<>(comb);
            for (String candidate : items) {
                if (!combSet.contains(candidate)) {
                    // store a copy of combination (as unordered list) and y
                    List<String> pick = new ArrayList<>(comb);
                    pick.add(candidate);
                    picks.add(pick);
                }
            }
        }
        return picks;
    }

    // Demo
//    public static void main(String[] args) {
//        List<String> items = Arrays.asList("1", "2", "3", "4", "5");
//        int n = 4;
//        List<List<String>> picks = generatePicks(items, n);
//        for (List<String> pick : picks) {
//            System.out.println(pick);
//        }
//    }
}
