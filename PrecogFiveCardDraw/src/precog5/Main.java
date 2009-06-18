package precog5;

import java.util.Random;

import Poker.Arthorius;
import Poker.Austin;
import Poker.Card;
import Poker.Deck;
import Poker.FiveCardDraw;
import Poker.Hand;
import Poker.HumanPlayer;
import Poker.OpenBook;
import Poker.Randall;
import Poker.Roboticmayhem_Nick;
import Poker.SimplePoker;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{			
		/*
		double percentile = 0;
		
		Precog p = new Precog();
		Deck deck = new Deck();
		
		double avg_best_perc = 0;
		
		for (int i = 0; i < 10; i++)
		{			
			deck.shuffle();
			
			double best_perc = -1;
			
			Card[][] cards = new Card[4][5];
			for (int pl = 0; pl < 4; pl++)
			{
				for (int j = 0; j < 5; j++)
				{
					cards[pl][j] = deck.dealOne();
				}
				double perc = Precog.percentile_before_trade(Precog.convert_card_array_to_long(cards[pl]));
				if (perc > best_perc)
					best_perc = perc;
			}
			avg_best_perc += best_perc;
		}
		
		System.out.println(avg_best_perc / 10);
		*/
		/*
		Precog p = new Precog();
		
		Card[] cards = new Card[5];
		
		cards[0] = new Card("AS");
		cards[1] = new Card("4S");
		cards[2] = new Card("5D");
		cards[3] = new Card("6C");
		cards[4] = new Card("9H");
		
		Hand hand = new Hand(cards);
		System.out.println(hand);
		
		
		long cards_long = Precog.convert_card_array_to_long(cards);
		
		long time = System.currentTimeMillis();
		int percentile = Precog.rate(cards_long);
		time = System.currentTimeMillis() - time;
		
		System.out.println("percentile: " + percentile);
		System.out.println("time: " + time);		
		*/
		/*
		time = System.currentTimeMillis();		
		percentile = Precog.percentile_before_trade_multithread(cards_long, 2);
		time = System.currentTimeMillis() - time;
		
		System.out.println("percentile(multithreaded): " + percentile);
		System.out.println("time: " + time);
		
		
		time = System.currentTimeMillis();
		long best_discard = Precog.find_best_discard_option(cards_long);
		time = System.currentTimeMillis() - time;
		
		Hand best_discards = new Hand(Precog.convert_long_to_card_array(best_discard));
		
		System.out.println("Best Discards: " + best_discards);
		System.out.println("time: " + time);
		
		time = System.currentTimeMillis();
		best_discard = Precog.find_best_discard_option_2_threads(cards_long);
		time = System.currentTimeMillis() - time;
		
		best_discards = new Hand(Precog.convert_long_to_card_array(best_discard));
		
		System.out.println("Best Discards(multithreaded): " + best_discards);
		System.out.println("time: " + time);
		
		cards_long ^= best_discard;
		
		Card[] traded = null;
		
		switch (best_discards.numCards())
		{
		case 1:
			traded = new Card[1];
			traded[0] = deck.dealOne();
			break;
		case 2:
			traded = new Card[2];
			traded[0] = deck.dealOne();
			traded[1] = deck.dealOne();			
			break;
		case 3:			
			traded = new Card[3];
			traded[0] = deck.dealOne();
			traded[1] = deck.dealOne();
			traded[2] = deck.dealOne();
			break;
		case 4:
			traded = new Card[4];
			traded[0] = deck.dealOne();
			traded[1] = deck.dealOne();
			traded[2] = deck.dealOne();
			traded[3] = deck.dealOne();
			break;
		}
				
		long traded_long = Precog.convert_card_array_to_long(traded);
		cards_long |= traded_long;
		
		Hand newHand = new Hand(Precog.convert_long_to_card_array(cards_long));
		System.out.println(newHand);		
		
		time = System.currentTimeMillis();
		double perc_new = Precog.percentile_after_trade(cards_long, cards_long | best_discard, best_discards.numCards());
		time = System.currentTimeMillis() - time;
		
		System.out.println("New percentile: " + perc_new);
		System.out.println("Time: " + time);
		
		time = System.currentTimeMillis();
		perc_new = Precog.percentile_after_trade_multithreaded(cards_long, cards_long | best_discard, best_discards.numCards(), 2);
		time = System.currentTimeMillis() - time;
		
		System.out.println("New percentile (multi): " + perc_new);
		System.out.println("Time: " + time);
		*/
		
		
		SimplePoker table = new FiveCardDraw();
        //table.addPlayer(new Randall(), 10);
		table.addPlayer(new Precog(), 20);
        table.addPlayer(new Arthorius(), 20);
        table.addPlayer(new Austin(), 20);
        table.addPlayer(new Roboticmayhem_Nick(), 20);
        
        int GAMES = 500;
        
        for (int i = 0; i < GAMES && table.countPlayers() > 1; i++)
        {
            table.playHand(new Random());
            System.out.println("\nAfter " + (i+1) + " hands:");
            System.out.println(table);
            System.out.println("");
        }
        if (table.countPlayers() == 1)
            table.endGame();
        else
            System.out.println("Tournament ends without a winner");
        
	}

}
