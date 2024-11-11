package util;

import ga.GA;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class GAPopulationGraph extends JFrame {

    private final JTabbedPane tabbedPane = new JTabbedPane();

    public GAPopulationGraph() {
        setTitle("GA");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        add(tabbedPane);
    }

    public GraphPanel graph(String title){
        var graph = new GraphPanel(title);
        SwingUtilities.invokeLater(() -> {
            tabbedPane.addTab("run " + (tabbedPane.getTabCount() + 1), graph);
            if(tabbedPane.getTabCount()-2==tabbedPane.getSelectedIndex())
                tabbedPane.setSelectedComponent(graph);
        });
        return graph;
    }

    public static class GraphPanel extends JPanel {
        private final CombinedGraph lhs = new CombinedGraph(Color.BLUE, Color.RED, Color.GREEN, false);
        private final CombinedGraph rhs = new CombinedGraph(Color.BLUE, Color.RED, Color.GREEN, true);
        private final ArrayList<Util.Tuple<String, Integer>> labels = new ArrayList<>();

        private final Font font = new Font("Arial", Font.PLAIN, 14);
        private final Font fontBold = new Font("Arial", Font.BOLD, 14);
        private final String title;
        private String results = "";


        public GraphPanel(String title) {
            this.title = title;
        }

        public void addDataPoint(GA.GenerationStat result) {
            lhs.add(result.minRawFit, result.maxRawFit, result.averageRawFit);
            rhs.add(result.minFit, result.maxFit, result.averageFit);
            SwingUtilities.invokeLater(this::repaint);
        }

        public void addVerticalLabel(int index, String label){
            synchronized (labels){
                labels.add(new Util.Tuple<>(label, index));
                labels.sort(Comparator.comparing(o -> o.t2));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            int width = getWidth();
            int height = getHeight();
            int margin = 40;

            g.setColor(new Color(0xa9a9a9));
            g.setFont(font);
            g.fillRect(0, 0, width, height);

            g2.setStroke(new BasicStroke(2.5f));
            drawVerticalAxis(g2, margin, margin*3, width-margin, height-margin);
            drawHorizontalAxis(g2, margin, margin*3, width-margin, height-margin);

            g2.setStroke(new BasicStroke(2.5f));

            drawBoundingBox(g2,  margin, margin*3, width-margin, height-margin);

            g2.setClip(margin, margin*3, width-margin*2, height-margin*4);
            lhs.draw(g2, margin, margin*3, width-margin, height-margin);
            rhs.draw(g2, margin, margin*3, width-margin, height-margin);
            drawLabels(g2, margin, margin*3, width-margin, height-margin);
        }

        private void drawBoundingBox(Graphics2D g2, int x1, int y1, int x2, int y2){
            g2.setColor(Color.BLACK);
            g2.drawLine(x1, y1, x2, y1); // top
            g2.drawLine(x1, y1, x1, y2); // left
            g2.drawLine(x1, y2, x2, y2); // bottom
            g2.drawLine(x2, y1, x2, y2); // right
        }

        private void rightAlignedCenteredStr(Graphics2D g2, String s, int x, int y){
            var met = g2.getFontMetrics();
            g2.drawString(s, x-met.stringWidth(s), y + met.getHeight()/3);
        }

        private void centerAlignedStr(Graphics2D g2, String s, int x, int y){
            var met = g2.getFontMetrics();
            g2.drawString(s, x-met.stringWidth(s)/2, y + met.getHeight()/3);
        }

        private void leftAlignedCenteredStr(Graphics2D g2, String s, int x, int y){
            var met = g2.getFontMetrics();
            g2.drawString(s, x, y + met.getHeight()/3);
        }

        private void rightAlignedStr(Graphics2D g2, String s, int x, int y){
            var met = g2.getFontMetrics();
            g2.drawString(s, x-met.stringWidth(s), y);
        }

        private void leftAlignedStr(Graphics2D g2, String s, int x, int y){
            g2.drawString(s, x, y);
        }

        private void drawVerticalAxis(Graphics2D g2, int x1, int y1, int x2, int y2){
            var divisions = 11;
            double lhsMin = lhs.lower();
            double lhsMax = lhs.upper();

            double rhsMin = rhs.lower();
            double rhsMax = rhs.upper();
            g2.setFont(font);
            for (int i = 0; i < divisions; i++) {
                int y = y2 - (i * (y2-y1) / (divisions-1));

                g2.setColor(Color.BLACK);
                rightAlignedCenteredStr(g2, Math.round(i*(lhsMax-lhsMin)/(divisions-1) + lhsMin)+"", x1-10, y);
                leftAlignedCenteredStr(g2, Math.round(i*(rhsMax-rhsMin)/(divisions-1) + rhsMin)+"", x2+10, y);

                g2.setColor(Color.LIGHT_GRAY);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(x1, y, x2, y);
            }

            g2.setColor(Color.BLACK);
            leftAlignedStr(g2, "Raw", x1, y1-10-2*g2.getFontMetrics().getHeight());
            leftAlignedStr(g2, "Solid", x1, y1-10-g2.getFontMetrics().getHeight());

            rightAlignedStr(g2, "Normalized", x2, y1-10-2*g2.getFontMetrics().getHeight());
            rightAlignedStr(g2, "Dotted", x2, y1-10-g2.getFontMetrics().getHeight());


            g2.setColor(Color.BLACK);
            int lineN = 3;
            for(var line : title.lines().toArray()){
                centerAlignedStr(g2, line.toString(), (x2-x1)/2+x1, y1-20-g2.getFontMetrics().getHeight()*lineN);
                lineN += 1;
            }

            lineN = 0;
            for(var line : results.lines().toArray()){
                leftAlignedStr(g2, line.toString(), (x2-x1)/5+x1, y1-10-g2.getFontMetrics().getHeight()*lineN);
                lineN += 1;
            }
            g2.setColor(Color.RED);
            centerAlignedStr(g2, "Max", (x2-x1)/2+x1, y1-10-g2.getFontMetrics().getHeight()*2);
            g2.setColor(Color.BLUE);
            centerAlignedStr(g2, "Min", (x2-x1)/2+x1, y1-10-g2.getFontMetrics().getHeight());
            g2.setColor(Color.GREEN);
            centerAlignedStr(g2, "Average", (x2-x1)/2+x1, y1-10);
        }

        private void drawHorizontalAxis(Graphics2D g2, int x1, int y1, int x2, int y2){
            g2.setColor(Color.BLACK);
            centerAlignedStr(g2, "Generations", (x2-x1)/2+x1, y2+g2.getFontMetrics().getHeight()+10);
            int maxPoints = Math.max(lhs.points(), rhs.points())-1;
            if(maxPoints < 2) return;
            double xStep = (double) (x2-x1) / maxPoints;
            for (int i = 0; i <= maxPoints; i += maxPoints/Math.min(15, maxPoints)) {
                double x = x1 + i * xStep;
                centerAlignedStr(g2, ""+i, (int) x, y2+10);

                g2.setColor(Color.LIGHT_GRAY);
                g2.drawLine((int) x, y1, (int) x, y2);
                g2.setColor(Color.BLACK);
            }
        }

        private void drawLabels(Graphics2D g2, int x1, int y1, int x2, int y2){
            g2.setStroke(new BasicStroke(2));
            g2.setFont(fontBold);
            int maxPoints = Math.max(lhs.points(), rhs.points())-1;
            if(maxPoints < 2) return;
            double xStep = (double) (x2-x1) / maxPoints;
            synchronized (labels){
                double index = 0;
                for(var label : labels){
                    double x = x1 + label.t2 * xStep;
                    g2.setColor(Color.BLACK);
                    var lines = label.t1.split("\n");
                    var fntHeight = g2.getFontMetrics().getHeight();
                    if(index*fntHeight+lines.length*fntHeight>(y2-y1)){
                        index = 0;
                    }
                    for(var line : lines){
                        var y = y1+(index*fntHeight) + fntHeight;
                        if(x1>x-g2.getFontMetrics().stringWidth(line+"->")) {
                                leftAlignedStr(g2, "<-" + line, (int) x, (int) y);
                        }else{
                            rightAlignedStr(g2, line + "->", (int) x, (int) y);
                        }
                        index += 1;
                    }
                    index += 0.25;

                    g2.setColor(new Color(0x797979));
                    g2.drawLine((int) x, y1, (int) x, y2);
                }
            }
        }

        public void showResults(String results) {
            this.results = results;
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    private static class Graph{
        private final Color color;
        private final ArrayList<Double> points = new ArrayList<>();
        private Optional<Double> lower = Optional.empty();
        private Optional<Double> upper = Optional.empty();
        private final boolean dotted;

        private Graph(Color color, boolean dotted) {
            this.color = color;
            this.dotted = dotted;
        }

        private double lower(){
            synchronized (points){
                return this.lower.orElseGet(() -> points.stream().min(Comparator.naturalOrder()).orElse(0.0));
            }
        }

        private double upper(){
            synchronized (points){
                return this.upper.orElseGet(() -> points.stream().max(Comparator.naturalOrder()).orElse(1.0));
            }
        }

        private void draw(Graphics2D g2, int x1, int y1, int x2, int y2){
            g2.setColor(color);
            int size;
            double lower;
            double upper;
            synchronized (points){
                size = points.size();
                if(points.isEmpty()) return;
                lower = lower();
                upper = upper();
            }
            double xStep = (double) (x2-x1) / (size-1);
            double previousX = x1;

            double value = (Math.max(Math.min(points.get(0), upper), lower)-lower)/(upper-lower);
            int previousY = (int) (y2 - (value * (y2-y1)));


            float distance = 0;
            for (int i = 1; i < size; i++) {
                double x = x1 + i * xStep;
                value = (Math.max(Math.min(points.get(i), upper), lower)-lower)/(upper-lower);
                int y = (int) (y2 - (value * (y2-y1)));

                if(dotted){
                    float[] dashPattern = {4, 4};
                    BasicStroke dottedStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, dashPattern, distance);
                    g2.setStroke(dottedStroke);
                }else{
                    g2.setStroke(new BasicStroke(2));
                }
                distance += (float) Math.sqrt(Math.pow(previousX-x, 2) + Math.pow(previousY-y, 2));

                g2.drawLine((int) previousX, previousY, (int) x, y);

                previousX = x;
                previousY = y;
            }
        }

        public void add(double lower) {
            synchronized (this.points){
                this.points.add(lower);
            }
        }
    }

    private static class CombinedGraph{
        Graph lower;
        Graph upper;
        Graph average;

        private CombinedGraph(Color lower, Color upper, Color average, boolean dotted){
            this.lower = new Graph(lower, dotted);
            this.upper = new Graph(upper, dotted);
            this.average = new Graph(average, dotted);
        }

        private double lower(){
            return lower.lower();
        }

        private double upper(){
            return upper.upper();
        }

        private void draw(Graphics2D g2, int x1, int y1, int x2, int y2){
            lower.draw(g2, x1, y1, x2, y2);
            upper.draw(g2, x1, y1, x2, y2);
            average.draw(g2, x1, y1, x2, y2);
        }

        public int points() {
            return average.points.size();
        }

        public void add(double lower, double upper, double average) {
            this.lower.add(lower);
            this.upper.add(upper);
            this.average.add(average);
            this.average.lower = Optional.of(this.lower.lower());
            this.average.upper = Optional.of(this.upper.upper());
            this.lower.upper = this.average.upper;
            this.upper.lower = this.average.lower;
        }
    }
}
