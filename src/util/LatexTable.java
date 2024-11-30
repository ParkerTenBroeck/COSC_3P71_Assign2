package util;

public class LatexTable {
    StringBuilder table;
    boolean first;

    public LatexTable(int len) {
        this.table = new StringBuilder();
        this.table.append("\\begin{tabular}{|");
        this.table.append("c|".repeat(Math.max(0, len)));
        this.table.append("}\n\\hline\n");
        this.first = true;
    }

    public void entry(String value) {
        if (!first) this.table.append("&");
        this.table.append(value);
        this.first = false;
    }

    public void nextRow() {
        this.first = true;
        this.table.append("\\\\\n\\hline\n");
    }


    public void end() {
        this.table.append("\\end{tabular}");
    }

    @Override
    public String toString() {
        return table.toString();
    }
}
