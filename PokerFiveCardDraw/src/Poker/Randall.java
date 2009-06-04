package Poker;

public class Randall implements Player
{
    Card[] cards;
    @Override
    public void deal(Card[] cards)
    {
        this.cards = cards;
    }

    @Override
    public Card[] draw(PlayerStats[] stats)
    {
        Hand.evaluateAndSortCards(cards);
        // Always draw one card (hope we didn't have a straight/flush!)
        Card[] toss = new Card[1];
        toss[0] = cards[cards.length - 1];
        return toss;
    }

    @Override
    public int getBid(PlayerStats[] stats, int callBid)
    {
        // The bigger the call bid, the more likely he is to fold
        // If he doesn't fold, he may raise
        if (Math.random() * 10 < callBid)
            return -1;
        int bid = (int)(Math.random() * 10) - 5;
        return Math.max(callBid, bid);
    }

    @Override
    public String toString()
    {
        return "Randall";
    }

    @Override
    public void initHand(PlayerStats[] stats, int yourIndex)
    {}

    @Override
    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
    {}

}
