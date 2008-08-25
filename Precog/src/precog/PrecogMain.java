/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package precog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import poker.engine.*;
import poker.players.*;

/**
 *
 * @author kevinl
 */
public class PrecogMain 
{
    public static void main(String... args)
    {         
        Precog a = new Precog("precog");
        //boolean j = a.verifyScorePocket();        
//        Player b = new ConsoleHuman("Shawn");
        Player d = new Stupid("stupid");
        Player[] c = {a, d};
        Game g = new Game(c);
        g.begin();
        
        //assert false : "assertions on";
        
        /*Card a = new Card(Card.Suit.CLUBS, Card.TWO);
        Card b = new Card(Card.Suit.SPADES, Card.THREE);
        Card c = new Card(Card.Suit.HEARTS, Card.FOUR);
        Card d = new Card(Card.Suit.CLUBS, Card.FIVE);
        Card e = new Card(Card.Suit.CLUBS, Card.SIX);
        Card[] cards = {a,b,c,d,e};
        Hand z = new Hand(cards);
        System.out.println(Precog.suitlessHand(z.getBitCards()));*/
        
        //this stuff i'm just using so i can use the chen formula 
        //while playing facebook poker
        /*BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String s = null;
        try
        {            
            s = br.readLine();
        } 
        catch (IOException ex)
        {                
            ex.printStackTrace();
        }

        if (s.equals("stash"))
        {
            System.out.println("Stashes:");
            for (Integer p : info.getActivePlayerIds())   
            {                
                System.out.println(info.getPlayerName(p) + " " + p + ":  $" + info.getStash(p).getAmount());                
            }
            waitForInput();
        }
        else if (s.equals("bets"))
        {
            System.out.println("Bets:");
            for (Integer p : info.getActivePlayerIds())                
                System.out.println(info.getPlayerName(p) + " " + p + ":  $" + info.getBet(p).getAmount());
            waitForInput();
        }
        else if (s.equals("potValue"))
        {
            System.out.println("PotValue:");
            System.out.println("$" + info.getPotValue().getAmount());
            waitForInput();
        }
        else if (s.equals("minimumBid"))
        {
            System.out.println("Minimum Bid:");
            System.out.println("$" + info.getMinimumCallAmount().getAmount());
            waitForInput();
        }
        else if (s.equals("players"))
        {
            System.out.println("Active players:");
            for (Integer p : info.getActivePlayerIds())
            {
                System.out.println(info.getPlayerName(p));
            }
        }*/
    
    }
}
