import java.util.*;
public class Match implements Comparable {
  Match prev;
  double score;
  int src, tar;
  public Match(){
    prev = null;
    score = 0.0;
  }
  public Match(int n, int i, Match prev, char[] seq){
    this.prev = prev;
    src = n;
    tar = i;
    score = prev.score;
    score = score + Simple.theta[Simple.uni(seq[n], i == 0 ? '0' : seq[i])];
    if(prev != null){
      score = score + Simple.theta[Simple.bi(prev.tar, tar)];
    }
  }
  public Match(int[] match, char[] seq){
    Match m = new Match();
    for(int i = 1; i < match.length; i++){
      m = new Match(i, match[i], m, seq);
    }
    this.prev = m.prev;
    this.score = m.score;
    this.src = m.src;
    this.tar = m.tar;
  }

  int match(int i){
    if(prev == null) return -1;
    else if(tar == i) return src;
    else return prev.match(i);
  }
  void match(HashSet<Integer> matches){
    if(prev != null){
      if(tar != 0) matches.add(tar);
      prev.match(matches);
    }
  }

  int[] toArray(){
    int[] ret = new int[src+1];
    toArray(ret);
    return ret;
  }
  void toArray(int[] ret){
    if(prev != null){
      ret[src] = tar;
      prev.toArray(ret);
    }
  }

  @Override
  public int compareTo(Object other){
    return (int)Math.round(Math.signum(((Match)other).score-score));
  }
}
