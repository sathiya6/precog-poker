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
    // adapted version for testing
    private static double percentileRank2(Hand _board, Hand myHand)
    {        
        long hand = myHand.getBitCards();
        long board = _board.getBitCards();        
        
        int totalOthers = 0;
        double notbigger = 0.d; //# of hands less than or equal to us       
        int myRating = Precog.rate(Precog.getHighestHand(hand, board));        
                
        
        long a = 0xFFFFFFFFFFFFFL ^ (hand | board);
        long b1, b2, c;            

        int count = Precog.bitCount_dense(a);       
        for (int i = 0; i < count - 1; i++)
        {
            c = a ^= b1 = a & -a; // b1 is the lowest bit                                
            for (int j = i + 1; j < count; j++)
            {
                c ^= b2 = c & -c;                    
                long aHand = b1 | b2;

                totalOthers++;
                if (Precog.rate(Precog.getHighestHand(aHand, board)) >= myRating)
                    notbigger++;
            }
        }   
        
        return (notbigger/totalOthers);
    }
    
    public static void main(String... args)
    {         
        Precog p = new Precog("");
        for (int i = 0; i < 10; i++)
        {
            Deck deck = new Deck();

            Card a = deck.drawCard();
            Card b = deck.drawCard();
            Card c = deck.drawCard();
            Card d = deck.drawCard();
            Card e = deck.drawCard();
            
            Card[] boardCards = {a,b,c};
            Hand flop = new Hand(boardCards);

            Card h1 = deck.drawCard();
            Card h2 = deck.drawCard();
            Card[] handCards = {h1, h2};
            Hand hand = new Hand(handCards);
            
            double percent = percentileRank2(flop, hand);
            
            Hand bigHand = Hand.merge(hand, flop);
            System.out.println(bigHand);
            System.out.println(percent);
        }
        
        
//        Precog a = new Precog("precog");        
//        Player d = new Stupid("stupid");
//        Player[] c = {a, d};
//        Game g = new Game(c);
//        g.begin();
        
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
