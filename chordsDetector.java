import java.util.*;

public class chordsDetector {

    static String[] notes = {
        "C","C#","D","D#","E","F",
        "F#","G","G#","A","A#","B"
    };

    static String[] defaultTuning = {"E","A","D","G","B","E"};

    static class ChordCandidate {
        String name;
        double score;

        ChordCandidate(String name, double score) {
            this.name = name;
            this.score = score;
        }
    }

    static int findNoteIndex(String note) {
        for (int i = 0; i < notes.length; i++) {
            if (notes[i].equals(note)) return i;
        }
        return -1;
    }

    static String getNote(String openNote, int fret) {
        int index = findNoteIndex(openNote);
        return notes[(index + fret) % 12];
    }

    /**
     * SMART PARSER
     * Supports:
     * - x02220
     * - x 0 2 2 2 0
     * - x,0,2,2,2,0
     * - x,10,12,12,11,10
     */
    static String[] parseTab(String input) {

        input = input.trim();
        String[] parts;

        if (input.contains(" ")) {
            parts = input.split("\\s+");
        } else if (input.contains(",")) {
            parts = input.split(",");
        } else {
            if (input.length() != 6) {
                throw new IllegalArgumentException(
                    "Compact format must be 6 characters (e.g. x02220)"
                );
            }

            parts = new String[6];
            for (int i = 0; i < 6; i++) {
                parts[i] = String.valueOf(input.charAt(i));
            }
        }

        if (parts.length != 6) {
            throw new IllegalArgumentException("Must represent exactly 6 strings.");
        }

        for (int i = 0; i < 6; i++) {
            parts[i] = parts[i].trim().toLowerCase();

            if (parts[i].equals("x")) continue;

            try {
                int fret = Integer.parseInt(parts[i]);

                if (fret < 0 || fret > 24) {
                    throw new IllegalArgumentException("Fret out of range: " + fret);
                }

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid input: " + parts[i]);
            }
        }

        return parts;
    }

    /**
     * Ordered notes (low → high)
     */
    static List<String> tabToOrderedNotes(String tab, String[] tuning) {
        String[] parts = parseTab(tab);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            if (parts[i].equals("x")) continue;

            int fret = Integer.parseInt(parts[i]);
            result.add(getNote(tuning[i], fret));
        }

        return result;
    }

    /**
     * Pitch classes for chord detection
     */
    static List<Integer> tabToPitchClasses(String tab, String[] tuning) {
        String[] parts = parseTab(tab);
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            if (parts[i].equals("x")) continue;

            int fret = Integer.parseInt(parts[i]);
            result.add(findNoteIndex(getNote(tuning[i], fret)));
        }

        return result;
    }

    static int interval(int root, int note) {
        return (note - root + 12) % 12;
    }

    static ChordCandidate analyzeFromRoot(Set<Integer> noteSet, int rootIndex, int bassIndex) {

        Set<Integer> intervals = new HashSet<>();
        for (int n : noteSet) {
            intervals.add(interval(rootIndex, n));
        }

        boolean hasMinor3 = intervals.contains(3);
        boolean hasMajor3 = intervals.contains(4);
        boolean has5 = intervals.contains(7);
        boolean hasb5 = intervals.contains(6);
        boolean hasAug5 = intervals.contains(8);

        String triadType = "";
        double triadScore = 0;

        if (hasMajor3 && has5) {
            triadType = "";
            triadScore = 1.0;
        } else if (hasMinor3 && has5) {
            triadType = "m";
            triadScore = 1.0;
        } else if (hasMinor3 && hasb5) {
            triadType = "dim";
            triadScore = 0.9;
        } else if (hasMajor3 && hasAug5) {
            triadType = "aug";
            triadScore = 0.9;
        } else {
            if (hasMajor3 || hasMinor3) triadScore += 0.5;
            if (has5 || hasb5) triadScore += 0.5;
        }

        boolean has7 = intervals.contains(10);
        boolean hasMaj7 = intervals.contains(11);
        boolean has2 = intervals.contains(2);
        boolean has4 = intervals.contains(5);
        boolean has6 = intervals.contains(9);

        String root = notes[rootIndex];
        String name = root + triadType;

        if (hasMaj7) name += "maj7";
        else if (has7 && triadType.equals("")) name += "7";
        else if (has7 && triadType.equals("m")) name += "7";

        if (!hasMajor3 && !hasMinor3) {
            if (has4) name = root + "sus4";
            else if (has2) name = root + "sus2";
        }

        if (has6 && !has7 && !hasMaj7) name += "6";
        if (has2 && (hasMajor3 || hasMinor3)) name += "add9";

        if (bassIndex != -1 && bassIndex != rootIndex) {
            name += "/" + notes[bassIndex];
        }

        double score = 0;

        if (bassIndex == rootIndex) score += 1.0;
        else if (noteSet.contains(rootIndex)) score += 0.6;
        else score += 0.2;

        score += triadScore;

        if (has7 || hasMaj7) score += 0.3;

        if (triadScore < 1.0) score -= 0.3;

        score += 0.2;

        int complexity = 0;
        if (has2) complexity++;
        if (has4) complexity++;
        if (has6) complexity++;
        score -= complexity * 0.05;

        if (!hasMajor3 && !hasMinor3) score -= 0.2;

        return new ChordCandidate(name, score);
    }

    static List<ChordCandidate> detectChord(String tab, String[] tuning) {

        List<Integer> noteList = tabToPitchClasses(tab, tuning);
        if (noteList.isEmpty()) return new ArrayList<>();

        Set<Integer> noteSet = new HashSet<>(noteList);
        int bassIndex = noteList.get(0);

        List<ChordCandidate> candidates = new ArrayList<>();

        for (int root : noteSet) {
            candidates.add(analyzeFromRoot(noteSet, root, bassIndex));
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        return candidates;
    }

    static String[] getCustomTuning(Scanner scanner) {
        while (true) {
            System.out.print("Enter 6 notes separated by commas (e.g. D,A,D,G,B,E): ");
            String input = scanner.nextLine().trim();

            String[] parts = input.split(",");

            if (parts.length != 6) {
                System.out.println("Invalid: must have exactly 6 notes.");
                continue;
            }

            boolean valid = true;
            for (int i = 0; i < 6; i++) {
                parts[i] = parts[i].trim().toUpperCase();
                if (findNoteIndex(parts[i]) == -1) {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                System.out.println("Invalid note detected.");
                continue;
            }

            return parts;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Use custom tuning? (yes/no): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        String[] tuning = (choice.equals("yes") || choice.equals("y"))
            ? getCustomTuning(scanner)
            : defaultTuning;

        System.out.println("Enter tab (supports: x02220, x 0 2 2 2 0, x,10,12,...): ");
        System.out.print("Tab: ");
        String tab = scanner.nextLine();

        try {
            List<String> orderedNotes = tabToOrderedNotes(tab, tuning);
            System.out.println("Notes (low → high): " + orderedNotes);

            List<ChordCandidate> results = detectChord(tab, tuning);

            System.out.println("Top matches:");
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                System.out.printf("%d. %s (%.2f)\n",
                    i + 1,
                    results.get(i).name,
                    results.get(i).score
                );
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}