package lexing;

import java.util.Objects;

public record Loc(String fileName, int startLine, int startColumn, int endLine, int endColumn) {

    public String toString() {
        return startLine + ":" + startColumn + " in file \"" + fileName + "\"";
    }

    //-1 if a is first, 0 if tie, 1 if b is first
    private static int comparePositions(int aline, int acol, int bline, int bcol) {
        if (aline < bline)
            return -1;
        if (aline > bline)
            return 1;
        return Integer.compare(acol, bcol);
    }

    private boolean contains(Loc other) {
        return
                comparePositions(startLine, startColumn, other.startLine, other.startColumn) < 1 &&
                comparePositions(endLine, endColumn, other.endLine, other.endColumn) > -1;
    }

    //Merge 2 locations, resulting in a location that contains both
    public static Loc merge(Loc a, Loc b) {
        if (!Objects.equals(a.fileName, b.fileName))
            throw new IllegalArgumentException("Attempt to merge 2 locations with different file names; bug in compiler, please report!");
        if (a.contains(b))
            return a;
        if (b.contains(a))
            return b;

        int startLine, startColumn;
        if (comparePositions(a.startLine, a.startColumn, b.startLine, b.startColumn) < 1) {
            //If start of A is before start of B, then start should be A
            startLine = a.startLine;
            startColumn = a.startColumn;
        } else {
            startLine = b.startLine;
            startColumn = b.startColumn;
        }

        int endLine, endColumn;
        if (comparePositions(a.endLine, a.endColumn, b.endLine, b.endColumn) < 1) {
            //If the end of A is before the end of B, then end should be B
            endLine = b.endLine;
            endColumn = b.endColumn;
        } else {
            endLine = a.endLine;
            endColumn = a.endColumn;
        }

        return new Loc(a.fileName, startLine, startColumn, endLine, endColumn);
    }

    public Loc merge(Loc other) {
        return Loc.merge(this, other);
    }

}
