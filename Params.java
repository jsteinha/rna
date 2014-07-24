import java.util.*;
public class Params extends HashMap<String, Double> {
  @Override
  public Double get(Object key){
    Double ret = super.get(key);
    if(ret == null) return 0.0;
    else return ret;
  }

  public void update(String key, double x){
    put(key, get(key) + x);
  }

}
