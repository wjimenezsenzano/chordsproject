const notes = [
    "C","C#","D","D#","E","F",
    "F#","G","G#","A","A#","B"
];

// Default tuning (low → high)
const defaultTuning = ["E","A","D","G","B","E"];

// -------------------- NOTE HELPERS --------------------
function findNoteIndex(note) {
    return notes.indexOf(note);
}

function getNote(openNote, fret) {
    const index = findNoteIndex(openNote);
    return notes[(index + fret) % 12];
}

// -------------------- SMART PARSER --------------------
function parseTab(input) {
    input = input.trim();
    let parts;

    if (input.includes(" ")) {
        parts = input.split(/\s+/);
    } else if (input.includes(",")) {
        parts = input.split(",");
    } else {
        if (input.length !== 6) {
            throw new Error("Must be 6 strings (e.g. x02220)");
        }
        parts = input.split("");
    }

    if (parts.length !== 6) {
        throw new Error("Must represent exactly 6 strings.");
    }

    return parts.map(p => {
        p = p.trim().toLowerCase();
        if (p === "x") return "x";

        let fret = parseInt(p);
        if (isNaN(fret) || fret < 0 || fret > 24) {
            throw new Error("Invalid fret: " + p);
        }

        return fret;
    });
}

// -------------------- NOTES (LOW → HIGH) --------------------
function tabToOrderedNotes(tab, tuning = defaultTuning) {
    const parts = parseTab(tab);
    const result = [];

    for (let i = 0; i < 6; i++) {
        if (parts[i] === "x") continue;

        const fret = parts[i];
        result.push(getNote(tuning[i], fret));
    }

    return result;
}

// -------------------- INTERVAL --------------------
function interval(root, note) {
    return (note - root + 12) % 12;
}

// -------------------- CORE ANALYSIS --------------------
function analyzeFromRoot(noteSet, rootIndex, bassIndex) {

    const intervals = new Set(
        [...noteSet].map(n => interval(rootIndex, n))
    );

    const hasMinor3 = intervals.has(3);
    const hasMajor3 = intervals.has(4);
    const has5 = intervals.has(7);
    const hasb5 = intervals.has(6);
    const hasAug5 = intervals.has(8);

    let triadType = "";
    let triadScore = 0;

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

    const has7 = intervals.has(10);
    const hasMaj7 = intervals.has(11);
    const has2 = intervals.has(2);
    const has4 = intervals.has(5);
    const has6 = intervals.has(9);

    const root = notes[rootIndex];
    let name = root + triadType;

    if (hasMaj7) name += "maj7";
    else if (has7 && triadType === "") name += "7";
    else if (has7 && triadType === "m") name += "7";

    if (!hasMajor3 && !hasMinor3) {
        if (has4) name = root + "sus4";
        else if (has2) name = root + "sus2";
    }

    if (has6 && !has7 && !hasMaj7) name += "6";
    if (has2 && (hasMajor3 || hasMinor3)) name += "add9";

    if (bassIndex !== -1 && bassIndex !== rootIndex) {
        name += "/" + notes[bassIndex];
    }

    // -------------------- SCORING --------------------
    let score = 0;

    if (bassIndex === rootIndex) score += 1.0;
    else if (noteSet.has(rootIndex)) score += 0.6;
    else score += 0.2;

    score += triadScore;

    if (has7 || hasMaj7) score += 0.3;
    if (triadScore < 1.0) score -= 0.3;

    let complexity = 0;
    if (has2) complexity++;
    if (has4) complexity++;
    if (has6) complexity++;

    score -= complexity * 0.05;

    if (!hasMajor3 && !hasMinor3) score -= 0.2;

    return { name, score };
}

// -------------------- MAIN DETECTOR --------------------
function detectChord(tab, tuning = defaultTuning) {

    const parts = parseTab(tab);

    const notesList = [];
    for (let i = 0; i < 6; i++) {
        if (parts[i] === "x") continue;

        notesList.push(findNoteIndex(getNote(tuning[i], parts[i])));
    }

    if (notesList.length === 0) return [];

    const noteSet = new Set(notesList);
    const bassIndex = notesList[0];

    const candidates = [];

    for (let root of noteSet) {
        candidates.push(analyzeFromRoot(noteSet, root, bassIndex));
    }

    candidates.sort((a, b) => b.score - a.score);

    return candidates;
}

// -------------------- EXPORT TO UI --------------------
window.detectChord = detectChord;
window.tabToOrderedNotes = tabToOrderedNotes;