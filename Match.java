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
    score = score + Simple.params.get(Simple.uni(seq[src], seq[tar], 0));
    score = score + Simple.params.get(Simple.dif(src, tar));
    if(prev != null){
      score = score + Simple.params.get(Simple.bi(prev.tar, tar));
    }
    Match ptr = prev;
    for(int k = 1; k <= 3; k++){
      if(ptr == null) break;
      score = score + Simple.params.get(Simple.uni(seq[ptr.src], seq[tar], -k));
      score = score + Simple.params.get(Simple.uni(seq[src], seq[ptr.tar], k));
      ptr = ptr.prev;
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
