package Poker;

public class OpenBook implements Player
{
    Card[] cards;
    int type = Hand.UNKNOWN;
    boolean drawYet = false;
    
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
        type = Hand.evaluateAndSortCards(cards);
        if (type < Hand.PAIR)
        {
            if (callBid <= 1)
                return callBid;
            return -1; // fold
        }
        if (type == Hand.PAIR)
        {
            if (callBid <= (drawYet ? 2 : 3))
                return callBid;
            return -1;
        }
        return callBid + (drawYet ? 2 : 3);  // raise
    }

    @Override
    public String toString()
    {
        return "OpenBook";
    }

    @Override
    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
    {}

}
