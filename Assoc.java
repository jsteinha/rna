import java.util.*;
public class Assoc<T> {
  HashMap<String, HashSet<T> > map = 
    new HashMap<String, HashSet<T> >();
  public Assoc(){}
  void add(String key, T value){
    if(!map.containsKey(key)) map.put(key, new HashSet<T>());
    map.get(key).add(value);
  }
}
