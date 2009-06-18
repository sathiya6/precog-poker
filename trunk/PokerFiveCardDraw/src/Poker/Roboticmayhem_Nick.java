package Poker;

import java.util.ArrayList;

//New Strategy!!!
//
//While calculating the exact percent chance of a win will certainly work mathematically, it is relatively devoid 
//of a sense of strategy. I plan on beating other players by following the following set of rules and making 
//player judgements:
//
//	To Play:
//	1) After  2 raises have been placed, only raise if I am extremely confident in my cards.
//	2) Do not raise if you need to draw to improve your hand.
//	3) If your opponent is tight (only plays with good hands, fold on others):
//		- Do not draw any cards
//		- Lots of high raises
//	If your opponent is loose (goes on to play with strong as well as weak hands - not afraid to match high 
//	raises & bets):
//		- Only play with strong hands
//		- Do not attempt to bluff
//	4) If you have a good hand, but not a sure win, either fold or make a high raise - do not place a high raise.
//	
//	To Start
//	1) If you are in an early position, only bet with good cards:
//		- General rule: if you do not have a pair of Jacks to start with, do not continue.
//		- If you have a good feel for the other players:
//			- Early - pair of aces at least
//			- Middle - pair of Jacks at least
//			- End - can stay with a lower pair
//	2) With more players at the table, only place bets with better hands (also affects the above starting 
//	   strategies)
//	
//	Helpful Facts:
//	- With a pair of some sort - drawing can improve hand in 40% of games
//	- With a three of a kind, drawing 2 cards - can improve hand in 15% of games
//	- With a three of a kind, drawing 1 card - can improve hand in 10% of games
//	- If you are near to completing a series such as a flush, the better the hand, the less likely you'll get the
//	  desired card(s).

public class Roboticmayhem_Nick implements Player
{
    Card[] cards;
    int type = Hand.UNKNOWN;
    int raises = 0;
    int raiseNum = 0;
    boolean drawYet = false;
    boolean justOpenBook = false;
    boolean justOBandRand = false;
    boolean againstOB = false;
    int roundCounter = 0;
    int index;
    
    @Override
    public void initHand(PlayerStats[] stats, int yourIndex)
    {
        drawYet = false;
        index = yourIndex;
    	justOpenBook = false;
    	if (stats.length == 2)
    	{
	    	if(index == 0){
	    		if(stats[1].name.equals("OpenBook")){
	    			justOpenBook = true;
	    			againstOB = true;
	    		}
	    	}
	    	else
	    		if(stats[0].name.equals("OpenBook")){
	    			justOpenBook = true;
	    			againstOB = true;
	    		}
    	}
    	boolean openBook = false;
    	boolean randall = false;
    	if (stats.length == 3)
    		for(PlayerStats i : stats){
    			if(i.name.equals("OpenBook")){
    				openBook = true;
    				againstOB = true;
    			}
    			if(i.name.equals("Randall"))
    				randall = true;
    		}
    	if(randall == true && openBook == true)
    		justOBandRand = true; 
    	for(PlayerStats i : stats){
    		if(i.name.equals("OpenBook"))
    			againstOB = true;
    	}
    }

    @Override
    public void deal(Card[] cards)
    {
        this.cards = cards;
    }

    @Override
    public Card[] draw(PlayerStats[] stats)
    {

        System.out.println("");
        System.out.print("Have:  |");
        for(int i = 0; i < 5; i++){
        	System.out.print(" " + cards[i] + " |");
        }
        System.out.println("");
        System.out.println("drawing______________________________________________________________________________________________________");
        System.out.println("");
        
    	drawYet = true;
        ArrayList<Card> discard = new ArrayList<Card>();
        switch (Hand.evaluateAndSortCards(cards))
        {
	        case Hand.CARD_HIGH:
	        	for (int i = 1; i <= 4; i++)
	        		discard.add(cards[i]);
	        	break;
	        case Hand.PAIR:
	        	for (int i = 2; i <= 4; i++)
	        		discard.add(cards[i]);
	        	break;
	        case Hand.THREE_OF_A_KIND:
	        	discard.add(cards[3]);
	        	discard.add(cards[4]);
	        	break;
	        case Hand.FOUR_OF_A_KIND:
	        case Hand.TWO_PAIR:
	    		discard.add(cards[4]);
        }
        Card[] toss = new Card[discard.size()];
        discard.toArray(toss);
        return toss;
    }

    @Override
    public int getBid(PlayerStats[] stats, int callBid)
    {
    	type = Hand.evaluateAndSortCards(cards);

        System.out.println("");
        System.out.print("Have:  |");
        for(int i = 0; i < 5; i++){
        	System.out.print(" " + cards[i] + " |");
        }
        System.out.println("");
        System.out.println("betting______________________________________________________________________________________________________");
        System.out.println("");
    	
    	if (justOBandRand){
    		if (callBid == 0)
    			return 11;
    		else
    			return -1;
		}    	
    	if (justOpenBook || againstOB){
    		if (callBid == 0)
    			return 4;
    		else
    			return -1;
		}
    	else if(type == Hand.CARD_HIGH){
        	if (callBid < (drawYet ? 1 : 2))
                return callBid;
            return -1;
    	}
        else if(type == Hand.PAIR){
//        	if(cards[0].equals('J') || cards[0].equals('Q') || cards[0].equals('K') || cards[0].equals('A')){
        	if(cards[0].getRank() > 10){
                if (callBid <= (drawYet ? 3 : 4))
                    return callBid;
                return -1;
        	}
        	else
        		return -1;		
        }
        else if(type == Hand.TWO_PAIR){
        	if (callBid <= (drawYet ? 3 : 4))
                return callBid + 1;
            return -1;
        }
        else if(type == Hand.THREE_OF_A_KIND){
        	if(drawYet){
            	if (callBid <= (drawYet ? 4 : 5))
                    return callBid + 1;
            	return -1;
        	}
        	else{
            	if (callBid <= (drawYet ? 5 : 6))
                    return callBid + 1;
            	return -1;
        	}
        }
        else if(type > Hand.THREE_OF_A_KIND){
        	return callBid + stats[index].chips;
        }
        else
        	return callBid;
    }

    @Override
    public String toString()
    {
        return "Roboticmayhem_Nick";
    }

    @Override
    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
    {}

}
