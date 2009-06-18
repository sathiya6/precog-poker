package Poker;

public class Arthorius implements Player
{
	 	Card[] cards;
	 	int index;
	    int type = Hand.UNKNOWN;
	    boolean drawYet = false;
	    
	    
	    public Arthorius()
	    {
	    	super(); 
	    	this.index = 1;
	    }
	    
	    public Arthorius(int index)
	    {
	    	super();
	    	this.index = index;
	    }
	    
	    public int getIndex()
	    {
	    	return index;
	    }
	    
	    @Override
	    public void initHand(PlayerStats[] stats, int yourIndex)
	    {
	        drawYet = false;
	    }

	    @Override
	    public void deal(Card[] cards)
	    {
	        this.cards = cards;
	    }
	    
	    @Override
	    public Card[] draw(PlayerStats[] stats)
	    {
	        drawYet = true;
	        int keep = 1;
	        type = Hand.evaluateAndSortCards(cards);
	        switch (type)
	        {
	            case Hand.CARD_HIGH:
	                keep = 1;
	                break;
	            case Hand.PAIR:
	                keep = 2;
	                break;
	            case Hand.THREE_OF_A_KIND:
	                keep = 3;
	                break;
	            case Hand.TWO_PAIR:
	            case Hand.FOUR_OF_A_KIND:  // just to make the opponents think we're fishing
	                keep = 4;
	                break;
	            default:
	                keep = 5;
	        }
	        Card[] toss = new Card[cards.length - keep];
	        System.arraycopy(cards, keep, toss, 0, toss.length);
	        return toss;
	    }
	    

	    @Override
	    public int getBid(PlayerStats[] stats, int callBid)
	    {
	    	boolean oBAd = false;
	    	boolean openBook = false;
	    	int oBRaise = 0;
	    	boolean stopBid = true;
	    	for(int i = 0; i < stats.length; i++)
	    	{
	    		if(!stats[i].folded)
	    			stopBid = false;
	    		if(stopBid)
	    		{
	    			return callBid;
	    		}
	    		if(stats[i].name.equalsIgnoreCase("OpenBook"))
	    		{
	    			openBook = true;
	    			int lastBid = stats[i].totalBid;
	    			if(lastBid > callBid)
	    			{
	    				oBAd = true;
	    				oBRaise = lastBid - callBid;
	    				//if(oBRaise >= 1)
	    					return -1;
	    			}
	    			//if(lastBid > callBid)
	    		}
	    		/*if(stats[i].name.equalsIgnoreCase("OpenBook"))
	    		{
	    			return callBid + 5;
	    		}*/
	    	}
	        type = Hand.evaluateAndSortCards(cards);
	       
	        switch (type)
	        {
	            case Hand.CARD_HIGH:
	            	if (callBid < (drawYet ? 1 : 2))
		                return callBid;
		            return -1;
	            case Hand.PAIR:
	                if(cards[0].getRank() > 5)
	                	return callBid + (drawYet ? 1 : 2);
	                else
	                	return callBid + (drawYet ? 0 : 1);
	            case Hand.TWO_PAIR:
	            	if(cards[0].getRank() > 5 && cards[2].getRank() > 5)
	            	{
	            		if(openBook)
	            			return callBid + 2;
	            		if(oBAd)
	            			return callBid + oBRaise;
	            		else
	            			return callBid + 5;
	            	}
	                else if(cards[0].getRank() > 5 && cards[2].getRank() < 5)
	                {
	                	if(openBook)
	            			return callBid + 2;
	                	if(oBAd)
	                		return callBid + oBRaise;
	            		else
	            			return callBid + 4;
	                }
	                else 
	                {
	                	if(openBook)
	            			return callBid + 2;
	                	if(oBAd)
	                		return callBid + oBRaise;
	            		else
	            			return callBid + 3;
	                }
	            case Hand.THREE_OF_A_KIND:
	            	if(openBook)
            			return callBid + 2;
	            	if(cards[0].getRank() > 5)
	                	return callBid + 4;
	                else
	                	return callBid + 3;
	            case Hand.STRAIGHT:
	            	if(openBook)
            			return callBid + 2;
	            	if(cards[0].getRank() > 5)
	            		return callBid + 5;
	            	else
	            		return callBid + 4;
	            case Hand.FLUSH:
	            case Hand.FULL_HOUSE:
	            case Hand.FOUR_OF_A_KIND: 
	            case Hand.STRAIGHT_FLUSH:
	            	if(openBook)
            			return callBid + 2;
	            	return callBid + 5;
	            default:
	                return callBid;
	        }
	    }

	    @Override
	    public String toString()
	    {
	        return "Arthorius";
	    }

	    @Override
	    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
	    {}

}
