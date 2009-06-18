package Poker;

import java.util.ArrayList;
import java.util.Random;

public class Austin implements Player
{
    Card[] cards;
    private int minChips;
    private boolean drawn;
    private int bidRound;
    private double aggression;
    private int matches;
    private int losses;
    private boolean bluffing;
    private boolean openBookOnly;
    private int yourIndex;
    
    @Override
    public void deal(Card[] cards)
    {

        this.cards = cards;
        drawn = false;
        bluffing = false;
        bidRound = 0;
    }

    @Override
    public Card[] draw(PlayerStats[] stats)
    {
    	drawn = true;
        ArrayList<Card> toss = new ArrayList<Card>();
        //System.out.println(checkForIncompleteStraight());
        switch (Hand.evaluateAndSortCards(cards))
        {
	        case Hand.CARD_HIGH:
	        	for (int i = 1; i <= 4; i++)
	        		toss.add(cards[i]);
	        	//System.out.println("CH: discarding 4");
	        	break;
	        case Hand.PAIR:
	        	for (int i = 2; i <= 4; i++)
	        		toss.add(cards[i]);
	        	//System.out.println("P: discarding 3");
	        	break;
	        case Hand.THREE_OF_A_KIND:
	        	toss.add(cards[3]);
	        	toss.add(cards[4]);
	        	//System.out.println("3oK: discarding 2");
	        	break;
	        case Hand.FOUR_OF_A_KIND:
	        case Hand.TWO_PAIR:
	    		toss.add(cards[4]);
	        	//System.out.println("4oK/2P: discarding 1");
        }
        Card[] tossArr = new Card[toss.size()];
        toss.toArray(tossArr);
        return tossArr;
    }
    
    private boolean checkForIncompleteStraight()
    {
    	int[] nums = new int[cards.length];
    	for (int i = 0; i < nums.length; i++)
    		nums[i] = cards[i].getRank();
    	for (int i = 0; i < nums.length; i++)
    	{
    		int check = nums[i] + 1;
    		while (check <= nums[i] + 3)
    		{
    			for (int j = 0; j < nums.length; j++)
    			{
    				if (nums[j] == check)
    				{
    					check++;
    					continue;
    				}
    			}
    			break;
    		}
    		if (check >= nums[i] + 5)
    			return true;
    	}
    	return false;
    }

    @Override
    public int getBid(PlayerStats[] stats, int callBid)
    {
    	minChips = 9999;
    	for (int i = 0; i < stats.length; i++)
    		if (i != yourIndex)
    		{
    			PlayerStats ps = stats[i];
    			minChips = Math.min(ps.chips, minChips);
    		}
    	//for (Card c : cards)
    	//	System.out.print(c.toString() + " ");
    	//System.out.println();
    	bidRound++;
    	if (openBookOnly)
		{
    		if (callBid == 0)
    			return 4;
    		else
    			return -1;
		}
    	
    	//if (checkForIncompleteStraight())
    		//System.out.println("Incomplete Straight");
    	Hand.evaluateAndSortCards(cards);
    	double handStrength = getHandStrength(500);
    	handStrength = Math.min(handStrength, 1.0 - 1E-9);
    	//System.out.println("Austin's hand strength: " + Math.round(handStrength * 100) / 100.0);
    	int bid = 0;
    	int guessBid = (int) Math.round(-1.5 * Math.log(1 - handStrength)) - 1;
    	//System.out.println("Austin: Maybe I should bid " + guessBid + "?");
    	if (handStrength < 0.5 - (0.4 * aggression / losses) && callBid > 0)
    		bid = -1;
    	else if (bidRound == 1)
    	{
    		if (callBid == 0 || guessBid + (2.0 * aggression / losses) >= callBid)
    			bid = Math.max(guessBid, callBid);
			else
				bid = -1;
    	}
    	else if (bluffing || (bidRound == 2 && callBid <= 1 && Math.random() > .9))
    	{
    		//System.out.println("Austin attempts to bluff...");
    		bluffing = true;
    		int myChips = 0;
    		for (PlayerStats ps : stats)
    			if (ps.name.equals(this.toString()))
    				myChips = ps.chips;
			bid = Math.max(callBid + 1, Math.min(3 - bidRound, myChips));
    	}
    	else if (callBid == 0 || guessBid + (5.0 * aggression / losses) >= callBid)
    		bid = callBid;
    	else if (true)
    		bid = -1;
    	if (minChips <= (drawn ? 1 : 2) && bid >= minChips + callBid - (drawn ? 1 : 2))
    	{
    		//System.out.println("Austin tries to eliminate oponent with " + minChips + " chips.");
    		return Math.max(callBid, minChips);
    	}
        return bid;
    }

    @Override
    public String toString()
    {
        return "Austin";
    }

    @Override
    public void initHand(PlayerStats[] stats, int yourIndex)
    {
    	this.yourIndex = yourIndex;
    	openBookOnly = false;
    	if (stats.length == 2)
    	{
	    	for (PlayerStats ps : stats)
	    		if (ps.name.equals("OpenBook"))
	    			openBookOnly = true;
    	}
    }

    @Override
    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
    {
    	this.matches++;
    	if (stats[winnerIndex].name.equals(this.toString()))
    		return; // I won!
    	losses++;
    	if (new Hand(cards).compareTo(new Hand(allCards[winnerIndex])) > 0)
    	{
    		//System.out.println("Austin: BUT I COULD HAVE WON!!!");
    		aggression++;
    	}
    	/*if (Hand.evaluateAndSortCards(allCards[winnerIndex]) < Hand.PAIR)
    	{
    		bluffCount++;
    		System.out.println("BLUFF DETECTED!");
    	}*/
    }
    
    private double getHandStrength(int reps)
    {
    	int wins = 0;
    	final int TRIES = reps;
    	Hand myHand = new Hand(this.cards);
    	Card[] cards = new Card[5];
    	Deck deck = new Deck();
    	for (int i = 0; i < TRIES; i++)
    	{
	    	deck.shuffle(new Random());
	    	for (int j = 0; j < cards.length; j++)
	    		cards[j] = deck.dealOne();
	    	Hand otherHand = new Hand(cards);
	    	otherHand.evaluateAndSortHand();
	    	if (myHand.compareTo(otherHand) > 0)
	    		wins++;
    	}
    	return (double)wins / TRIES;
    } 

}
