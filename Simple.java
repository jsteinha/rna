import java.io.*;
import java.util.*;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.OptionsParser;
import fig.basic.StatFig;
import fig.exec.Execution;
import fig.record.Record;
import java.util.concurrent.*;

public class Simple implements Runnable {
  @Option(required=true)
  public static String experimentName;
  @Option(required=true)
  public static int numThreads;
  @Option(required=true)
  public static int B;
  @Option(required=true)
  public static double eta;

  public static double[] theta = new double[8*9 + 6],
                         G2    = new double[8*9 + 6];
  static char[] i2c = new char[]{'a','u','g','c','A','U','G','C','0'};
  static int[] c2i = new int[256];

  static int uni(char x, char y){
    return 9*c2i[x] + c2i[y];
  }

  static int bi(int i, int j){
    int offset;
    if(i == 0 && j == 0) offset = 0;
    else if(i == 0) offset = 1;
    else if(j == 0) offset = 2;
    else if(i-j == 1) offset = 3;
    else if(i-j == -1) offset = 4;
    else offset = 5;
    return 8*9 + offset;
  }

  static void printFeatures(){
    for(int i=0;i<8;i++){
      for(int j=0;j<9;j++){
        LogInfo.logs(i2c[i] + "-"+i2c[j]+": " + theta[9*i+j]);
      }
    }
    LogInfo.logs(" 00: " + theta[8*9+0]);
    LogInfo.logs(" 0x: " + theta[8*9+1]);
    LogInfo.logs(" x0: " + theta[8*9+2]);
    LogInfo.logs(" +1: " + theta[8*9+3]);
    LogInfo.logs(" -1: " + theta[8*9+4]);
    LogInfo.logs(" ??: " + theta[8*9+5]);
  }

  static boolean adagrad = false;
  static void update(int index, double x){
    if(adagrad){
      G2[index] += x * x;
      theta[index] += eta * x / Math.sqrt(1e-4 + G2[index]);
    } else {
      theta[index] += eta * x;
    }
  }

  static void featurize(char[] seq, int[] match, int N, double x){
    int index;
    for(int n = 1; n <= N; n++){
      index = uni(seq[n], match[n] == 0 ? '0' : seq[match[n]]);
      update(index, x);
      if(n < N){
        index=bi(match[n], match[n+1]);
        update(index, x);
      }
    }
  }

  public void run() {
    try {
      runWithException();
    } catch(Exception e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  void runWithException() throws Exception {
    for(int i=0;i<i2c.length;i++) c2i[(int)i2c[i]] = i;

    ArrayList<Example> examples = new ArrayList<Example>();
    for(final File file : new File("data").listFiles()){
      examples.add(Reader.read(file));
    }

    // initialize with pseudolikelihood
    for(Example ex : examples){
      LogInfo.logs(ex.name);
      featurize(ex.seq, ex.match, ex.N, 1.0);
    }

    for(int i = 0; i < theta.length; i++){
      theta[i] = Math.log(theta[i] + eta * 1e-1);
    }

    printFeatures();

    adagrad = true;
    for(int t = 1; t <= 15; t++){
      LogInfo.begin_track("Starting iteration %d", t);
      double tot = 0.0;
      for(Example ex : examples){
        int[] pred = predict(ex.seq, ex.N, ex.match);
        double score = sim(pred, ex.match, ex.N);
        LogInfo.logs("score="+String.format("%.3f", score));
        tot += score;
      }
      LogInfo.logs("%.3f: %.1f/%.1f", tot/examples.size(), tot, 1.0*examples.size());
      printFeatures();
      LogInfo.end_track();
    }

  }

  static double sim(int[] pred, int[] match, int N){
    double ret = 0.0;
    for(int n = 1; n <= N; n++)
      if(pred[n] == match[n]) ret += 1.0 / N;
    return ret;
  }

  static int[] predict(final char[] seq, final int N, final int[] match0) throws Exception {
    if(B == -1) return new int[N+1];
    List<Match> beam = Collections.synchronizedList(new ArrayList<Match>());
    beam.add(new Match());
    for(int n0 = 1; n0 <= N; n0++){
      final int n = n0;
      List<Match> oldBeam = beam;
      final List<Match> tmpBeam = Collections.synchronizedList(new ArrayList<Match>());
      final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
      try {
        for(final Match m : oldBeam){
          threadPool.submit(new Runnable(){
            @Override
            public void run(){
              int i = m.match(n);
              if(i > 0){
                tmpBeam.add(new Match(n, i, m, seq));
              } else {
                HashSet<Integer> matches = new HashSet<Integer>();
                m.match(matches);
                tmpBeam.add(new Match(n, 0, m, seq));
                for(i = n+1; i <= N; i++){
                  if(!matches.contains(i)){
                    tmpBeam.add(new Match(n, i, m, seq));
                  }
                }
              }
            }
          });
        }
      } finally {
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      }
      beam = tmpBeam;
      if(n == N){
        beam.add(new Match(match0, seq));
      }
      Collections.sort(beam);
      if(beam.size() > B){
        beam = new ArrayList<Match>(beam.subList(0, B));
      }
    }
    // compute gradient based on final beam
    double[] source = new double[beam.size()],
             target = new double[beam.size()];
    for(int i = 0; i < beam.size(); i++){
      Match m = beam.get(i);
      int[] match = m.toArray();
      source[i] = m.score;
      //double ll = Math.log(sim(match, match0, N));
      double ll = sim(match, match0, N);
      target[i] = source[i] + ll;
    }
    Util.logNormalize(source);
    Util.logNormalize(target);
    for(int i = 0; i < beam.size(); i++){
      int[] match = beam.get(i).toArray();
      featurize(seq, match, N, eta * (Math.exp(target[i]) - Math.exp(source[i])));
    }
    return beam.get(0).toArray();
  }

  public static void main(String[] args){
    Execution.run(args, new Simple());
  }

}
