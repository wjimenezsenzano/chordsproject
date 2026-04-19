import java.util.*;

public class chordsDetector {

    // Chromatic scale
    static String[] notes = {
        "C","C#","D","D#","E","F",
        "F#","G","G#","A","A#","B"
    };

    // Default tuning (used if user says "no")
    static String[] defaultTuning = {"E","A","D","G","B","E"};

    static class ChordCandidate {
        String name;
        double score;

        ChordCandidate(String name, double score) {
            this.name = name;
            this.score = score;
        }
    }

    // Find index of note
    static int findNoteIndex(String note) {
        for (int i = 0; i < notes.length; i++) {
            if (notes[i].equals(note)) return i;
        }
        return -1;
    }

    // Compute note from string + fret
    static String getNote(String openNote, int fret) {
        int index = findNoteIndex(openNote);
        return notes[(index + fret) % 12];
    }

    /**
     * Parse tab → pitch classes
     * Supports 'x' for muted strings
     */
    static List<Integer> tabToPitchClasses(String tab, String[] tuning) {
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < tab.length(); i++) {
            char c = tab.charAt(i);

            if (c == 'x' || c == 'X') continue;

            int fret = Character.getNumericValue(c);
            String note = getNote(tuning[i], fret);

            result.add(findNoteIndex(note));
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

        // TRIAD DETECTION
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

        // EXTENSIONS
        boolean has7 = intervals.contains(10);
        boolean hasMaj7 = intervals.contains(11);
        boolean has2 = intervals.contains(2);
        boolean has4 = intervals.contains(5);
        boolean has6 = intervals.contains(9);

        String root = notes[rootIndex];
        String name = root + triadType;

        // Build chord name
        if (hasMaj7) name += "maj7";
        else if (has7 && triadType.equals("")) name += "7";
        else if (has7 && triadType.equals("m")) name += "7";

        // Suspensions
        if (!hasMajor3 && !hasMinor3) {
            if (has4) name = root + "sus4";
            else if (has2) name = root + "sus2";
        }

        // Add tones
        if (has6 && !has7 && !hasMaj7) name += "6";
        if (has2 && (hasMajor3 || hasMinor3)) name += "add9";

        // Slash chord
        if (bassIndex != rootIndex) {
            name += "/" + notes[bassIndex];
        }

        // SCORING
        double score = 0;

        if (rootIndex == bassIndex) score += 1.0;
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

        Set<Integer> noteSet = new HashSet<>(noteList);

        int bassIndex = noteList.get(0);

        List<ChordCandidate> candidates = new ArrayList<>();

        for (int root : noteSet) {
            candidates.add(analyzeFromRoot(noteSet, root, bassIndex));
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        return candidates;
    }

    /**
     * NEW: Parse custom tuning input
     * Example input: D,A,D,G,B,E
     */
    static String[] getCustomTuning(Scanner scanner) {

        while (true) {
            System.out.print("Enter 6 notes separated by commas (e.g. D,A,D,G,B,E): ");
            String input = scanner.nextLine().trim();

            String[] parts = input.split(",");

            // Validate length
            if (parts.length != 6) {
                System.out.println("Invalid: must have exactly 6 notes.");
                continue;
            }

            // Normalize + validate notes
            boolean valid = true;
            for (int i = 0; i < 6; i++) {
                parts[i] = parts[i].trim().toUpperCase();
                if (findNoteIndex(parts[i]) == -1) {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                System.out.println("Invalid note detected. Use notes like C, C#, D, etc.");
                continue;
            }

            return parts;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // --- NEW: Ask for tuning ---
        System.out.print("Use custom tuning? (yes/no): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        String[] tuning;

        if (choice.equals("yes") || choice.equals("y")) {
            tuning = getCustomTuning(scanner);
        } else {
            tuning = defaultTuning;
        }

        // --- Input tab ---
        System.out.print("Enter tab (e.g. x02220): ");
        String tab = scanner.nextLine();

        List<ChordCandidate> results = detectChord(tab, tuning);

        System.out.println("Top matches:");
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            System.out.printf("%d. %s (%.2f)\n",
                i + 1,
                results.get(i).name,
                results.get(i).score
            );
        }
    }
}