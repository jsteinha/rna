public class Example {
  int N;
  char[] seq;
  int[] match;
  String name;
  public Example(char[] seq, int[] match, String name){
    this.seq = seq;
    this.match = match;
    this.name = name;
    N = seq.length-1;
  }
}
