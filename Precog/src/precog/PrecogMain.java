package precog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import poker.engine.*;
import poker.engine.Card.Suit;
import poker.players.*;

/**
 *
 * @author kevinl
 */
public class PrecogMain 
{
        
    public static void main(String... args)
    {     
    	assert false : "assertions on";
    	Precog a = new Precog("precog");
        Player s1 = new Stupid("stupid");

        Player[] c = {a, s1};
        Game g = new Game(c);
        g.begin();        
    }
    
    private static void consolePoker()
    {    	   	
    	System.out.println("Input the hand:");
    	
    	long h1 = getCardFromConsole();
    	long h2 = getCardFromConsole();
    	
    	System.out.println("The Chen Score is : " + Precog.scorePocket(new Hand(h1 | h2, 2, 2)));
    	
    	System.out.println("Input the flop:");
    	
    	long b1 = getCardFromConsole();
    	long b2 = getCardFromConsole();
    	long b3 = getCardFromConsole();
    	    	
    	System.out.println("The Average Percentile is: " + Precog.pf_avg_perc(h1 | h2, b1 | b2 | b3));
    }
    
    private static long getCardFromConsole()
    {
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	
    	System.out.println("What is the suit? s = spades, c = clubs, h = hearts, d = diamonds");
    	
    	String s = null;
        try
        {            
            s = br.readLine();
        } 
        catch (IOException ex)
        {                
        }
        
        Card.Suit suit = null;
        
        if ("h".equals(s))
        {
        	suit = Suit.HEARTS;
        }
        else if ("c".equals(s))
        {
        	suit = Suit.CLUBS;
        }
        else if ("d".equals(s))
        {
        	suit = Suit.DIAMONDS;
        }
        else if ("s".equals(s))
        {
        	suit  = Suit.SPADES;
        }
                        
        System.out.println("What is the number? value must be between 2 - 14");
        
        s = null;
        try
        {            
            s = br.readLine();
        } 
        catch (IOException ex)
        {                
        }
        
        int num = Integer.parseInt(s);
        
        return new Card(suit, (byte) num).getValue();
    }
    
}
