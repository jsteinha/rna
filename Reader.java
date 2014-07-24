import java.io.*;
import java.util.*;

public class Reader {
  static Example read(File file){
    try {
      Scanner s = new Scanner(file);
      String str;
      do str = s.nextLine(); while(str.charAt(0) == '#');
      String[] header = str.trim().split("\\s+");
      int N = Integer.parseInt(header[0]);
      //System.out.println(N);
      char[] seq = new char[N+1];
      int[] match = new int[N+1];
      for(int n=1;n<=N;n++){
        String[] line = s.nextLine().trim().split("\\s+");
        if(Integer.parseInt(line[0]) != n) throw new RuntimeException("invalid input on line " + n + " of file " + file.getName() + ": " + line[0]);
        seq[n] = line[1].charAt(0);
        match[n] = Integer.parseInt(line[4]);
        //System.out.println(seq[n] + " " + match[n]);
      }
      seq[0] = '0';
      return new Example(seq, match, file.getName());
    } catch(Exception e){
      throw new RuntimeException("Exception in file " + file.getName() + ":\n\t" + e.toString());
    }
  }



}
