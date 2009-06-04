package precog5;

import Poker.Card;
import Poker.Deck;
import Poker.Hand;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		/*
		int[] array = new int[10000];
		long time;
		
		// method 1
		time = System.nanoTime();
		
		for (int i = 0; i < 10000; i++)
		{
			boolean even = i % 2 ==0 ? true: false;
		}
		
		time = System.nanoTime() - time;
		System.out.println(time);

		// method 2
		time = System.nanoTime();
		
		for (int i = 0; i < 10000; i++)
		{
			array[i] = i;
		}
		
		for (int i = 0; i < 10000; i++)
		{
			boolean even = array[i] % 2 == 0? true: false;
		}
		
		time = System.nanoTime() - time;
		System.out.println(time);
		*/
		
		Precog p = new Precog();
		Deck deck = new Deck();
		deck.shuffle();
		Card[] cards = new Card[5];
		cards[0] = deck.dealOne();
		cards[1] = deck.dealOne();
		cards[2] = deck.dealOne();		
		cards[3] = deck.dealOne(); 		
		cards[4] = deck.dealOne();
		
		Hand hand = new Hand(cards);
		System.out.println(hand);
		
		long time = System.currentTimeMillis();
		double percentile = p.percentile_before_trade(p.convert_card_array_to_long(cards));
		time = System.currentTimeMillis() - time;
		
		System.out.println("percentile: " + percentile);
		System.out.println("time: " + time);		
		
		time = System.currentTimeMillis();
		percentile = Precog.percentile_before_trade_multithread(Precog.convert_card_array_to_long(cards), 2);
		time = System.currentTimeMillis() - time;
		
		System.out.println("percentile(multithreaded): " + percentile);
		System.out.println("time: " + time);
		
		time = System.currentTimeMillis();
		Hand best_discards = new Hand(p.convert_long_to_card_array(p.find_best_discard_option(p.convert_card_array_to_long(cards))));
		time = System.currentTimeMillis() - time;
		System.out.println("Best Discards: " + best_discards);
		System.out.println(time);
	}

}
