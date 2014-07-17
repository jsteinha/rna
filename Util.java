import java.util.*;
import fig.basic.LogInfo;
public class Util {

  public static <T> Map<T, Double> toMap(List<T> list){
    HashMap<T, Double> ret = new HashMap<T, Double>();
    update(ret, list, 1.0);
    return ret;
  }

  public static <T> double getSafe(Map<T, Double> map, T key){
    Double ret = map.get(key);
    if(ret == null) return 0.0;
    else return ret;
  }

  public static <T> void printMap(Map<T, Double> map){
    if(map.size() == 0) return;
    ArrayList<Map.Entry<T, Double> > list = new ArrayList<Map.Entry<T, Double> >(map.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<T, Double> >(){
      @Override
      public int compare(Map.Entry<T, Double> x, Map.Entry<T, Double> y){
        return -Double.compare(Math.abs(x.getValue()), Math.abs(y.getValue()));
      }
    });
    double largest = Math.min(1.0, Math.abs(list.get(0).getValue()));
    for(Map.Entry<T, Double> entry : list){
      if(Math.abs(entry.getValue()) >= 5e-2 * largest){
        LogInfo.logs("%s:\t%s", entry.getKey(), entry.getValue());
      }
    }
  }

  public static <T> void update(Map<T, Double> map, Map<T, Double> delta){
    update(map, delta, 1.0);
  }

  public static <T> void update(Map<T, Double> map, 
                                Map<T, Double> delta, double coef){
    for(Map.Entry<T, Double> entry : delta.entrySet()){
      update(map, entry.getKey(), coef*entry.getValue());
    }
  }

  public static <T> void update(Map<T, Double> map,
                                List<T> list, double coef){
    for(T item : list){
      update(map, item, coef);
    }
  }

  public static <T> void update(Map<T, Double> map, T key){
    update(map, key, 1.0);
  }

  public static <T> void update(Map<T, Double> map, T key, double x){
    Double cur = map.get(key);
    if(cur == null){
      cur = 0.0;
    }
    cur += x;
    map.put(key, cur);
  }

  public static double lse(double x, double y){
    if(x == Double.NEGATIVE_INFINITY) return y;
    else if(y == Double.NEGATIVE_INFINITY) return x;
    else if(x < y) return y + Math.log(1 + Math.exp(x-y));
    else return x + Math.log(1 + Math.exp(y-x));
  }

  public static int sample(double[] logprobs){
    double max = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < logprobs.length; i++){
      max = Math.max(max, logprobs[i]);
    }
    double Z = 0.0;
    for(int i = 0; i < logprobs.length; i++){
      Z += Math.exp(logprobs[i] - max);
    }
    double logZ = Math.log(Z) + max;
    double u = Math.random(), v = 0.0;
    for(int i = 0; i < logprobs.length; i++){
      v += Math.exp(logprobs[i] - logZ);
      if(v > u) return i;
    }
    throw new RuntimeException("this should never happen");
  }

  public static <T> T sample(Map<T, Double> logprobs){
    double max = Double.NEGATIVE_INFINITY;
    for(T key : logprobs.keySet()){
      max = Math.max(max, logprobs.get(key));
    }
    double Z = 0.0;
    for(T key : logprobs.keySet()){
      Z += Math.exp(logprobs.get(key) - max);
    }
    double logZ = Math.log(Z) + max;
    double u = Math.random(), v = 0.0;
    for(T key : logprobs.keySet()){
      v += Math.exp(logprobs.get(key) - logZ);
      if(v > u) return key;
    }
    throw new RuntimeException("this should never happen");
  }

  public static <T> void logNormalize(Map<T, Double> x){
    double lse = Double.NEGATIVE_INFINITY;
    for(Double xi : x.values()){
      if(lse == Double.NEGATIVE_INFINITY) lse = xi;
      else if(xi > lse) lse = xi + Math.log(1 + Math.exp(lse - xi));
      else lse = lse + Math.log(1 + Math.exp(xi - lse));
    }
    if(lse == Double.NEGATIVE_INFINITY) return;
    for(T key : x.keySet()){
      x.put(key, x.get(key) - lse);
    }
  }

  public static void logNormalize(ArrayList<Double> x){
    double lse = Double.NEGATIVE_INFINITY;
    for(Double xi : x){
      if(lse == Double.NEGATIVE_INFINITY) lse = xi;
      else if(xi > lse) lse = xi + Math.log(1 + Math.exp(lse - xi));
      else lse = lse + Math.log(1 + Math.exp(xi - lse));
    }
    if(lse == Double.NEGATIVE_INFINITY) return;
    for(int i = 0; i < x.size(); i++){
      x.set(i, x.get(i) - lse);
    }
    //for(Double xi : x){
    //  xi -= lse;
    //}
  }

  public static void logNormalize(double[] x){
    double lse = Double.NEGATIVE_INFINITY;
    for(int i = 0; i < x.length; i++){
      if(lse == Double.NEGATIVE_INFINITY) lse = x[i];
      else if(x[i] > lse) lse = x[i] + Math.log(1 + Math.exp(lse - x[i]));
      else lse = lse + Math.log(1 + Math.exp(x[i] - lse));
    }
    if(lse == Double.NEGATIVE_INFINITY) return;
    for(int i = 0; i < x.length; i++){
      x[i] -= lse;
    }
  }
}
