import java.util.*;

public class chordsDetector {

    static String[] notes = {
        "C","C#","D","D#","E","F",
        "F#","G","G#","A","A#","B"
    };

    static String[] tuning = {"E","A","D","G","B","E"};

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

    static List<String> tabToNotes(String tab) {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < tab.length(); i++) {
            int fret = Character.getNumericValue(tab.charAt(i));
            result.add(getNote(tuning[i], fret));
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

        boolean has3 = intervals.contains(4) || intervals.contains(3);
        boolean minor3 = intervals.contains(3);
        boolean major3 = intervals.contains(4);
        boolean has5 = intervals.contains(7);
        boolean hasb5 = intervals.contains(6);
        boolean has7 = intervals.contains(10);
        boolean hasMaj7 = intervals.contains(11);
        boolean has2 = intervals.contains(2);
        boolean has4 = intervals.contains(5);
        boolean has6 = intervals.contains(9);

        String root = notes[rootIndex];
        String name = root;

        // --- chord naming ---
        if (major3 && has5) name += "";
        else if (minor3 && has5) name += "m";
        else if (has4 && !has3) name += "sus4";
        else if (has2 && !has3) name += "sus2";
        else if (hasb5) name += "dim";

        if (hasMaj7) name += "maj7";
        else if (has7 && major3) name += "7";
        else if (has7 && minor3) name += "m7";

        if (has6 && !has7 && !hasMaj7) name += "6";
        if (has2 && has3) name += "add9";

        // slash chord
        if (bassIndex != rootIndex) {
            name += "/" + notes[bassIndex];
        }

        // --- scoring ---
        double score = 0;

        // ROOT SCORE
        if (rootIndex == bassIndex) score += 1.0;
        else if (noteSet.contains(rootIndex)) score += 0.6;
        else score += 0.2;

        // QUALITY SCORE
        if (has3) score += 0.5;
        if (has5 || hasb5) score += 0.2;
        if (has7 || hasMaj7) score += 0.3;

        // COMPLETENESS
        if (!has3) score -= 0.4;
        if (!has5) score -= 0.1;

        // GUITAR BONUS (simple version)
        score += 0.2;

        // COMPLEXITY PENALTY
        int complexity = 0;
        if (has2) complexity++;
        if (has4) complexity++;
        if (has6) complexity++;
        score -= complexity * 0.05;

        // AMBIGUITY
        if (!has3) score -= 0.2;

        return new ChordCandidate(name, score);
    }

    static List<ChordCandidate> detectChord(List<String> played) {

        // convert to pitch classes
        List<Integer> noteList = new ArrayList<>();
        for (String n : played) {
            noteList.add(findNoteIndex(n));
        }

        Set<Integer> noteSet = new HashSet<>(noteList);
        int bassIndex = noteList.get(0); // lowest string

        List<ChordCandidate> candidates = new ArrayList<>();

        for (int root : noteSet) {
            candidates.add(analyzeFromRoot(noteSet, root, bassIndex));
        }

        // sort by score
        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        return candidates;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter tab (e.g. 022100): ");
        String tab = scanner.nextLine();

        List<String> played = tabToNotes(tab);
        System.out.println("Notes: " + played);

        List<ChordCandidate> results = detectChord(played);

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