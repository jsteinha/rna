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

  static Params G2 = new Params(),
                params = new Params();

  static String uni(char x, char y, int offset){
    if(offset == 0){
      return x + "=" + y;
    } else if(offset > 0){
      return x+" "+y + "+" + offset;
    } else {
      return x+" "+y + "-" + (-offset);
    }
  }

  static String bi(int i, int j){
    if(i == 0 && j == 0) return "00";
    else if(i == 0) return "0x";
    else if(j == 0) return "x0";
    else if(i-j == 1) return "+1";
    else if(i-j == -1) return "-1";
    else return "??";
  }

  static String dif(int i, int j){
    if(i == 0 || j == 0) return "DU";
    if(Math.abs(i-j) <= 5) return "D"+Math.abs(i-j);
    else return "D*";
  }

  static void printFeatures(){
    LogInfo.begin_track("params");
    for(String str : params.keySet()){
      LogInfo.logs("%s: %f", str, params.get(str));
    }
    LogInfo.end_track();
  }

  static boolean adagrad = false, onTest = false;
  static void update(String key, double x){
    if(onTest) return;
    if(adagrad){
      G2.update(key, x * x);
      params.update(key, eta * x / Math.sqrt(1e-4 + G2.get(key)));
    } else {
      params.update(key, eta * x);
    }
  }

  static void featurize(char[] seq, int[] match, int N, double x){
    String index;
    for(int n = 1; n <= N; n++){
      index = dif(n, match[n]);
      update(index, x);
      for(int k = -3; k <= 3; k++){
        if(1 <= n+k && n+k <= N){
          index = uni(seq[n+k], seq[match[n]], k);
          update(index, x);
        }
      }
      if(n < N){
        index=bi(match[n], match[n+1]);
        update(index, x);
      }
    }
  }

  static char F(char x){
    if(x == 0) return '*';
    else return x;
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

    ArrayList<Example> examples = new ArrayList<Example>();
    for(final File file : new File("data").listFiles()){
      examples.add(Reader.read(file));
    }

    Collections.shuffle(examples, new Random(0L));

    // initialize with pseudolikelihood
    for(Example ex : examples){
      LogInfo.logs(ex.name);
      featurize(ex.seq, ex.match, ex.N, 1.0);
    }

    for(String str : params.keySet()){
      params.put(str, Math.log(params.get(str) + eta * 1e-1));
    }

    printFeatures();

    adagrad = true;
    int numTrain = 1500, numTest = examples.size() - numTrain;
    for(int t = 1; t <= 15; t++){
      LogInfo.begin_track("Starting iteration %d", t);
      double tot = 0.0;
      int count = 0;
      for(Example ex : examples){
        onTest = count++ >= numTrain;
        int[] pred = predict(ex.seq, ex.N, ex.match);
        double score = sim(pred, ex.match, ex.N);
        LogInfo.begin_track("score="+String.format("%.3f", score));
        for(int i = 1; i <= ex.N; i++)
          LogInfo.logs("%s %d/%s %d/%s\n", ex.seq[i], ex.match[i], F(ex.seq[ex.match[i]]), pred[i], F(ex.seq[pred[i]]));
        LogInfo.end_track();
        if(onTest) tot += score;
      }
      LogInfo.logs("%.3f: %.1f/%.1f", tot/numTest, tot, 1.0*numTest);
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
      double ll = Math.log(sim(match, match0, N));
      //double ll = sim(match, match0, N);
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
