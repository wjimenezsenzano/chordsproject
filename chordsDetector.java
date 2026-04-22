const notes = ["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"];

// Top → bottom UI order (high E → low E)
const tuning = ["E","B","G","D","A","E"];

// State: one value per string
let selected = ["x","x","x","x","x","x"];

// ---------- helpers ----------
function findIndex(n) {
    return notes.indexOf(n);
}

function getNote(open, fret) {
    return notes[(findIndex(open) + fret) % 12];
}

// ---------- build fretboard ----------
function buildFretboard() {
    const board = document.getElementById("board");

    tuning.forEach((openNote, stringIndex) => {

        const row = document.createElement("div");
        row.className = "row";

        const label = document.createElement("div");
        label.className = "label";
        label.innerText = openNote;
        row.appendChild(label);

        const string = document.createElement("div");
        string.className = "string";

        for (let fret = 0; fret <= 12; fret++) {

            const div = document.createElement("div");
            div.className = "fret";

            if (fret === 0) div.classList.add("open");

            div.innerText = getNote(openNote, fret);

            div.addEventListener("click", () => {

                // clear previous selection on this string
                Array.from(string.children).forEach(f => f.classList.remove("mark"));

                // toggle off
                if (selected[stringIndex] == fret.toString()) {
                    selected[stringIndex] = "x";
                    updateOutput();
                    return;
                }

                selected[stringIndex] = String(fret);
                div.classList.add("mark");

                updateOutput();
            });

            string.appendChild(div);
        }

        row.appendChild(string);
        board.appendChild(row);
    });
}

// ---------- build selected notes ----------
function getNotes() {
    let result = [];

    for (let i = 0; i < 6; i++) {
        if (selected[i] === "x") continue;

        let fret = parseInt(selected[i]);
        let note = getNote(tuning[i], fret);
        result.push(note);
    }

    return result;
}

// ---------- chord detection (simplified scoring model) ----------
function guessChords(notesList) {

    let pitch = [...new Set(notesList.map(findIndex))];

    let results = [];

    for (let root of pitch) {

        let intervals = pitch.map(n => (n - root + 12) % 12);

        let has3 = intervals.includes(4);
        let hasm3 = intervals.includes(3);
        let has5 = intervals.includes(7);

        let name = notes[root];

        if (has3 && has5) name += "";
        else if (hasm3 && has5) name += "m";

        let score = 0;
        if (has3 || hasm3) score += 1;
        if (has5) score += 1;

        results.push({ name, score });
    }

    return results
        .sort((a,b) => b.score - a.score)
        .map(r => `${r.name} (${r.score.toFixed(2)})`);
}

// ---------- live update ----------
function updateOutput() {

    const out = document.getElementById("output");
    const notesList = getNotes();

    if (notesList.length < 3) {
        out.innerText = "Select at least 3 notes...";
        return;
    }

    const chords = guessChords(notesList);

    out.innerText =
        "Notes (low → high): " + notesList.join(" → ") + "\n\n" +
        "Chords:\n" + chords.join("\n");
}

// ---------- init ----------
buildFretboard();