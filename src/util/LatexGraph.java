package util;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LatexGraph {


    interface Blah<T, R>{
        R apply(int i, T v);
    }
    static <T, R> Stream<R> enumerate(Stream<T> in, Blah<T,R> meow){
        return in.map(new Function<T, R>() {
            int count = 0;
            @Override
            public R apply(T t) {
                return meow.apply(count++, t);
            }
        });
    }

    public static String graph(GARuns item) {

        int labelsPerWrap = 35;

        var minRaw = 0;//item.averagedStats.stream().mapToDouble(v -> v.minRawFit).min().orElse(0.0);
        var maxRaw = item.averagedStats.stream().mapToDouble(v -> v.maxRawFit).max().orElse(0.0);

        var minNorm = 0;//item.averagedStats.stream().mapToDouble(v -> v.minFit).min().orElse(0.0);
        var maxNorm = item.averagedStats.stream().mapToDouble(v -> v.maxFit).max().orElse(0.0);
        var str = """
\\begin{tikzpicture}
\\pgfplotsset{
    xmin=0, xmax="""+(item.averagedStats.size()-1)+"""
,
}

\\begin{axis}[
    axis y line*=left,
    ymin="""+minRaw+"""
    , ymax="""+maxRaw+"""
    ,
    xlabel=generations,
    ylabel=raw fitness,
    xmajorgrids=true,
    grid style=dashed,
    legend style={at={(0,1.03)}, anchor=south west},
    legend cell align={left}
    ]
    \\addlegendimage{/pgfplots/refstyle=min-raw}\\addlegendentry{min raw}
    \\addlegendimage{/pgfplots/refstyle=mean-raw}\\addlegendentry{mean raw}
    \\addlegendimage{/pgfplots/refstyle=max-raw}\\addlegendentry{max raw}

    \\addplot[very thick,smooth,blue]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.minRawFit+")").collect(Collectors.joining())+"""
    }; \\label{min-raw}
    
    \\addplot[very thick,smooth,green]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.averageRawFit+")").collect(Collectors.joining())+"""
    }; \\label{mean-raw}
    
    \\addplot[very thick,smooth,red]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.maxRawFit+")").collect(Collectors.joining())+"""
    }; \\label{max-raw}
\\end{axis}

\\begin{axis}[
axis y line*=right,
axis x line=none,
ymin="""+minNorm+"""
        , ymax="""+maxNorm+"""
        ,
    ylabel=normalized fitness,
    legend style={at={(1,1.03)}, anchor=south east},
    legend cell align={left}
    ]

    \\addlegendimage{/pgfplots/refstyle=min-norm}\\addlegendentry{min norm}
    \\addlegendimage{/pgfplots/refstyle=mean-norm}\\addlegendentry{mean norm}
    \\addlegendimage{/pgfplots/refstyle=max-norm}\\addlegendentry{max norm}

    \\addplot[smooth,blue,dashed,very thick]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.minFit+")").collect(Collectors.joining())+"""
    }; \\label{min-norm}

    \\addplot[smooth,green,dashed,very thick]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.averageFit+")").collect(Collectors.joining())+"""
    };\\label{mean-norm}

    \\addplot[smooth,red,dashed,very thick]
    coordinates{"""+enumerate(item.averagedStats.stream(), (i, v) -> "("+i+","+v.maxFit+")").collect(Collectors.joining())+"""
    };\\label{max-norm}
\\end{axis}

\\begin{axis}[
    axis y line=none,
    axis x line=none,
    ymin=0, ymax="""+labelsPerWrap+"""
    ]
""" +
                enumerate(item.runs.stream().sorted(Comparator.comparingInt(c -> c.generationStats.size())), (i, run) -> {
                    var finished = run.generationStats.size()-1;
                    var pos = (labelsPerWrap-i%labelsPerWrap)/(float)labelsPerWrap-1/(float)labelsPerWrap/2.0;
                    return "\\addplot[thick,smooth,gray] coordinates {("
                            +finished+",0)("
                            +finished+","+labelsPerWrap+")}  node[black,pos="
                            +pos+","+(finished<item.averagedStats.size()/2?"right":"left")+"]{\\tiny\\textbf{"
                            +"G"+(run.generationStats.size()-1)+" S"+run.seed+" F"+(int)run.best.fitness+"}};\n";
                }).collect(Collectors.joining())
                +
"""

\\end{axis}


\\end{tikzpicture}
""";
        return str;
    }
}
