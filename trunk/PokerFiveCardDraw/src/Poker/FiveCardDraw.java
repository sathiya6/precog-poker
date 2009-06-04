package Poker;

public class FiveCardDraw extends SimplePoker
{
    public void playHand()
    {
        beforeHand();
        dealCards();
        bidding();
        if (numPlayersIn > 1)
        {
            drawCards();
            bidding();
        }
        resolveWinner();
        removeBankrupt();
    }
    
    void drawCards()
    {
        for (PlayerData ps : data)
        {
            if (ps.folded)
                continue;
            Card[] cards = ps.player.draw(getPlayerStats());
            if (cards != null)
            {
            	int n = 0;
            	for (Card c : cards)
            	{
            		if (c == null)
            			continue;
            		if (!ps.replaceCard(c, deck.dealOne()))
            			throw new IllegalArgumentException(ps + " tried to discard a card that isn't in their hand!");
            		n++;
            	}
            	System.out.println(ps + " draws " + n);
            }
        	// Communicate the new state via the existing deal API
            Card[] copy = ps.copyHand(5);
            ps.player.deal(copy);
        }
    }

}
