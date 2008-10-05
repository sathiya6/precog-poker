/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
//    	Integer i = new Integer(5);
//    	Object o = (Object)i;
//    	if (o instanceof Integer)
//    		System.out.println("true");
    	
    	Precog p = new Precog("");
    	/*Precog a = new Precog("");
    	for (int i = -1; i< 21; i++)
    	{
    		System.out.println(i + ": " + a.get_cs_tol(i));
    	}*/
    	//consolePoker();
    	/*
    	Precog p = new Precog("");
    	Deck deck = new Deck();
    	
    	double cs_total = 0;
    	double count = 0;
    	for (int i = 0; i < 100; i++)
    	{
    		deck.reset();    		
    		Card h1 = deck.drawCard();
    		Card h2 = deck.drawCard();    		
    		Hand h = new Hand(new Card[]{h1,h2});
    		deck.drawCard();
    		Card b1 = deck.drawCard();    		
    		Card b2 = deck.drawCard();    		
    		Card b3 = deck.drawCard();
    		//deck.drawCard();
    		//Card b4 = deck.drawCard();
    		//deck.drawCard();
    		//Card b5 = deck.drawCard();
    		Hand b = new Hand(new Card[]{b1,b2,b3});
    		cs_total += Precog.pf_avg_perc_multithread(h.getBitCards(), b.getBitCards(), 2);
    		count++;
    	}
    	System.out.println(cs_total / count);
    	*/
    	/*
        Precog p = new Precog("");
        long totalMil = 0;
        double avg = 0.d;
        double avg2 = 0.d;
        
        Deck deck = new Deck();

        
        Card h1 = deck.drawCard();
        Card h2 = deck.drawCard();
        Card[] handCards = {h1, h2};            
        
        Hand hand = new Hand(handCards);
        
        System.out.println(hand);
        System.out.println("Chen Score: " + Precog.scorePocket(hand));        
        
        Card a = deck.drawCard();
        Card b = deck.drawCard();
        Card c = deck.drawCard();
        Card d = deck.drawCard();
        Card e = deck.drawCard();
        
        Card[] boardCards = {a,b,c};
        Hand flop = new Hand(boardCards);

        System.out.println(flop);                      
        
        avg = Precog.pf_avg_perc_multithread(hand.getBitCards(), flop.getBitCards(), 2);
        System.out.println("PF average: " + avg);
                
        Card[] boardCards2 = {a,b,c,d};         
        Hand turn = new Hand(boardCards2);
        System.out.println(turn);
        
        avg = Precog.pt_avg_perc_multithread(hand.getBitCards(), turn.getBitCards(), 2);                                        
        
        System.out.println("PT average: " + avg);
        
        Card[] boardCards3 = {a,b,c,d, e};         
        Hand river = new Hand(boardCards3);
        System.out.println(river);
        
        avg = Precog.pr_perc_calc(hand.getBitCards(), river.getBitCards());                                        
        
        System.out.println("PR average: " + avg);
        */
    	
    	Deck deck = new Deck();
    	Card h1 = deck.drawCard();
        Card h2 = deck.drawCard();
        Card[] handCards = {h1, h2};            
        
        Hand hand = new Hand(handCards);
    	Card a = deck.drawCard();
        Card b = deck.drawCard();
        Card c = deck.drawCard();
        Card d = deck.drawCard();
        Card e = deck.drawCard();
        
        Card[] boardCards = {a,b,c};
        Hand flop = new Hand(boardCards);
        
    	double avg;
        long time = System.currentTimeMillis();        
        avg = Precog.pf_avg_perc(hand.getBitCards(), flop.getBitCards());
        //avg2 = percentileRank2(flop, hand);
        time = System.currentTimeMillis() - time;                        
        System.out.println(time);
        System.out.println(avg);
        
        /*time = System.currentTimeMillis();        
        avg = Precog.pf_avg_perc_multithread(hand.getBitCards(), flop.getBitCards(), 4);
        //avg2 = percentileRank2(flop, hand);
        time = System.currentTimeMillis() - time;                        
        System.out.println(time);
        System.out.println(avg);*/
        
        //System.out.println(Precog.scorePocket(hand));
        //System.out.println(avg2);
        //System.out.println(avg);
        /*  
        Precog a = new Precog("precog");
        Player s1 = new Stupid("stupid");
        Player s2 = new Stupid("stupid");
        Player s3 = new Stupid("stupid");
        Player s4 = new Stupid("stupid");
        Player s5 = new Stupid("stupid");
        Player s6 = new Stupid("stupid");
        Player s7 = new Stupid("stupid");
        Player s8 = new Stupid("stupid");
        Player s9 = new Stupid("stupid");
        Player s0 = new Stupid("stupid");
        Player[] c = {a,s1,s2,s3,s4,s5,s6,s7,s8,s9,s0};
        Game g = new Game(c);
        g.begin();
        */
        //assert false : "assertions on";
        
        /*Card a = new Card(Card.Suit.CLUBS, Card.TWO);
        Card b = new Card(Card.Suit.SPADES, Card.THREE);
        Card c = new Card(Card.Suit.HEARTS, Card.FOUR);
        Card d = new Card(Card.Suit.CLUBS, Card.FIVE);
        Card e = new Card(Card.Suit.CLUBS, Card.SIX);
        Card[] cards = {a,b,c,d,e};
        Hand z = new Hand(cards);
        System.out.println(Precog.suitlessHand(z.getBitCards()));*/            
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
