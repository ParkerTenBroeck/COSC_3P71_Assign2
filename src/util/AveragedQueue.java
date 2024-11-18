package util;

import ga.GenerationStat;

import java.util.function.Consumer;

public class AveragedQueue {
        private final ValueProvider[] meows;
        private final Consumer<GenerationStat> consumer;
        private int ready = 0;
        private int done = 0;

        public AveragedQueue(int expected, Consumer<GenerationStat> consumer){
            this.consumer = consumer;
            meows = new ValueProvider[expected];
        }

        public synchronized Consumer<GenerationStat> provider(){
            var meow = new ValueProvider();
            for(int i = 0; i < meows.length; i ++){
                if(meows[i] == null){
                    meows[i] = meow;
                    return meow::push;
                }
            }
            return null;
        }

        public synchronized void check(boolean b){
            if(b)ready += 1;else done+=1;
            if(ready == meows.length-done && done != meows.length){
                var count = 0;
                var rawMin = 0.0;
                var rawMax = 0.0;
                var rawAvg = 0.0;
                var norMin = 0.0;
                var norMax = 0.0;
                var norAvg = 0.0;

                ready = 0;
                for (ValueProvider item : meows) {
                    var result = item.pop();

                    rawMin += result.minRawFit;
                    rawMax += result.maxRawFit;
                    rawAvg += result.averageRawFit;
                    norMin += result.minFit;
                    norMax += result.maxFit;
                    norAvg += result.averageFit;
                    count += 1;
                }
                if(count != 0){
                    consumer.accept(new GenerationStat(norMin/count,norMax/count,norAvg/count,rawMin/count,rawMax/count,rawAvg/count));
                }

            }
        }

        private class ValueProvider {
            private GenerationStat meow;
            private GenerationStat last;
            private boolean done = false;

            private void push(GenerationStat stat){
                if(stat == null){
                    synchronized (this){
                        while(meow != null) {
                            try {
                                this.wait();
                            } catch (InterruptedException ignore) {
                            }
                        }
                        done = true;
                    }
                    check(false);
                }else{
                    synchronized (this){
                        while(meow != null) {
                            try {
                                this.wait();
                            } catch (InterruptedException ignore) {
                            }
                        }
                        meow = stat;
                    }
                    check(true);
                }

            }

            private synchronized GenerationStat pop(){
                if(!done()){
                    last = meow;
                    meow = null;
                    this.notify();
                }
                return last;
            }

            public boolean done() {
                return this.done && meow == null;
            }
        }
    }