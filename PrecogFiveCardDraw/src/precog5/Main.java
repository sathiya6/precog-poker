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
		
		long cards_long = Precog.convert_card_array_to_long(cards);
		/*
		long time = System.currentTimeMillis();
		double percentile = Precog.percentile_before_trade(cards_long);
		time = System.currentTimeMillis() - time;
		
		System.out.println("percentile: " + percentile);
		System.out.println("time: " + time);		
		
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
		*/
		Card[] traded = new Card[4];
		traded[0] = deck.dealOne();
		traded[1] = deck.dealOne();
		traded[2] = deck.dealOne();
		traded[3] = deck.dealOne();
		
		long traded_long = Precog.convert_card_array_to_long(traded);
		
		long time = System.currentTimeMillis();
		Precog.enum_pos_opp_hands_4_traded(cards_long | traded_long);
		time = System.currentTimeMillis() - time;
		
		System.out.println("Time: " + time);
		
		time = System.currentTimeMillis();
		Precog.enum_pos_opp_hands_4_traded_2_threads(cards_long | traded_long);
		time = System.currentTimeMillis() - time;
		
		System.out.println("Time (multi): " + time);
	}

}
