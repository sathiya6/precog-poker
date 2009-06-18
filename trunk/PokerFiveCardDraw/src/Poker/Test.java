package Poker;

import java.util.Random;


public class Test
{

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
	public static void main(String[] args)
    {
        Random rand = new Random();
        int seed = rand.nextInt();

        Class[] playerClasses =
        {
        	Randall.class,
//        	OpenBook.class,
//        	OpenBook.class,
        	OpenBook.class,
        	Austin.class,
//        	Arthorius.class,
//        	Roboticmayhem_Nick.class,
//        	Roboticmayhem_Nick2.class,
        	Roboticmayhem_Nick.class,
//        	nick.class,
        };
        
        SimplePoker[] tables = new SimplePoker[playerClasses.length];
        for (int i = 0; i < tables.length; i++)
        	tables[i] = playTournament(playerClasses, i, seed);
        
        for (int i = 0; i < tables.length; i++)
        {
        	System.out.println("\nTable " + (i + 1));
        	System.out.println(tables[i].toString());
        }
    }
    
    @SuppressWarnings("unchecked")
	public static SimplePoker playTournament(Class[] players, int rotate, int seed)
    {
    	int startingChips = 100;
        SimplePoker table = new FiveCardDraw();
        for (int i = 0; i < players.length; i++)
        {
        	Class c = players[(i + rotate) % players.length];
        	try
        	{
	        	Player player = (Player)c.newInstance();
	        	table.addPlayer(player, startingChips);
        	}
        	catch (Exception e)
        	{
        		throw new RuntimeException("Cannot instantiate class " + c, e);
        	}
        }

        Random rand = new Random();
        int GAMES = 50;
        
        for (int i = 0; i < GAMES && table.countPlayers() > 1; i++)
        {
        	rand.setSeed(i+seed);
        	table.playHand(table.firstBidder() == 0 ? rand : null);
            System.out.println("\nAfter " + (i+1) + " hands:");
            System.out.println(table);
            System.out.println("");
        }
        if (table.countPlayers() == 1)
            table.endGame();
        else
            System.out.println("Tournament ends without a winner");
        
        return table;
    }

    void oldTest()
    {
//      Deck deck = new Deck();
//      deck.shuffle();
//      Card[] cards = new Card[7];
//      for (int i = 0; i < cards.length; i++)
//      {
//          cards[i] = deck.dealOne();
//      }
//      Hand hand = new Hand(cards);
//      System.out.println("Hand #1:");
//      System.out.println(hand);
//      hand.evaluateAndSortHand();
//      System.out.println(hand);
//
//      for (int i = 0; i < cards.length; i++)
//      {
//          cards[i] = deck.dealOne();
//      }
//      Hand hand2 = new Hand(cards);
//      hand2 = new Hand("5H 2D 3S 4C aH 3H");
//      System.out.println("Hand #2:");
//      System.out.println(hand2);
//      hand2.evaluateAndSortHand();
//      System.out.println(hand2);
//      
//      int d = hand.compareTo(hand2);
//      if (d < 0)
//          System.out.println("Hand #1 loses");
//      else if (d > 0)
//          System.out.println("Hand #1 wins");
//      else
//          System.out.println("Hands tie");
    }
}
