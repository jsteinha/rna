import java.util.*;
import fig.basic.LogInfo;
public class Example {
  int N;
  char[] seq;
  int[] match;
  String name;
  ArrayList[] proposals;
  public Example(char[] seq, int[] match, String name){
    this.seq = seq;
    this.match = match;
    this.name = name;
    N = seq.length-1;
    proposals = new ArrayList[N+1];
  }
  private String reverse(String str){
    return new StringBuilder(str).reverse().toString();
  }
  void generateProposals(){
    Assoc<Integer> groups = new Assoc<Integer>();
    for(int i=2;i<=N-1;i++){
      groups.add(new String(new char[]{seq[i-1],seq[i],seq[i+1]}), i);
    }
    for(int i=1;i<=N;i++) proposals[i] = new ArrayList<Integer>();
    for(int i=1;i<=N;i++){
      if(i>1) proposals[1].add(i);
      if(i<N) proposals[N].add(i);
    }
    for(String key : Simple.assoc.map.keySet()){
      if(groups.map.get(key) == null) continue;
      for(String value : Simple.assoc.map.get(key)){
        if(groups.map.get(reverse(value)) == null) continue;
        for(Integer i : groups.map.get(key)){
          for(Integer j : groups.map.get(reverse(value))){
            proposals[i].add(j);
          }
        }
      }
    }
    double avg_size = 0.0;
    for(int i=2;i<N;i++){
      avg_size += proposals[i].size() / (double)N;
    }
    LogInfo.logs("avg_size: %f (max %f)", avg_size, (double)N);
  }
}
