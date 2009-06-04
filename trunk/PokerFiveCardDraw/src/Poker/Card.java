/**
 * Created on Mar 25, 2008
 * @author garberd
 */
package Poker;

import java.util.Comparator;

public class Card implements Comparable<Card>
{
    public static final String[] SUITS =
        { "clubs", "diamonds", "hearts", "spades" };
    public static final String SUIT_CHARS = "CDHS";
    public static final String RANK_CHARS = "23456789TJQKA";

    public static final int NUM_SUITS = SUIT_CHARS.length();
    public static final int NUM_RANKS = RANK_CHARS.length();
    
    // Immutable cards
    private int suit;  // 0-3
    private int rank;
    
    public Card(int suit, int rank)
    {
        this.suit = suit;
        this.rank = rank;
    }
    
    public Card(Card c)
    {
        this(c.suit, c.rank);
    }
    
    public Card(String str)
    {
        rank = RANK_CHARS.indexOf(Character.toUpperCase(str.charAt(0)));
        suit = SUIT_CHARS.indexOf(Character.toUpperCase(str.charAt(1)));
    }
    
    public int getSuit()
    {
        return suit;
    }
    
    public int getRank()
    {
        return rank;
    }
    
    public String toString()
    {
        return "" + RANK_CHARS.charAt(rank)
            + SUIT_CHARS.charAt(suit);
    }
    
    public boolean equals(Object o)
    {
        // Stricter than compareTo, since we'll use this to communicate cards with the player
        return o instanceof Card && ((Card)o).rank == this.rank && ((Card)o).suit == this.suit;
    }

    public int compareTo(Card c)
    {
        // Suit doesn't matter
        // Higher rank means bigger
        return this.rank - c.rank;
    }
    
    public static class SuitComparator implements Comparator<Card>
    {
        private boolean lowAce;
        public SuitComparator(boolean lowAce)
        {
            this.lowAce = lowAce;
        }
        
        @Override
        public int compare(Card c1, Card c2)
        {
            // Sort by suit.  Within a suit, sort by rank.
            if (c1.suit != c2.suit)
                return c1.suit - c2.suit;
            if (lowAce && c1.rank != c2.rank)
            {
                // if either is an ace, treat that as the smallest within the suit
                if (c1.rank == NUM_RANKS - 1)
                    return -1;  // c2 is always bigger 
                if (c2.rank == NUM_RANKS - 1)
                    return 1;   // c1 is always bigger
            }
            return c1.rank - c2.rank;
        }
        
    }

    public static class RankComparator implements Comparator<Card>
    {
        private boolean lowAce;
        public RankComparator(boolean lowAce)
        {
            this.lowAce = lowAce;
        }
        
        @Override
        public int compare(Card c1, Card c2)
        {
            if (lowAce && c1.rank != c2.rank)
            {
                // if either is an ace, treat that as the smallest
                if (c1.rank == NUM_RANKS - 1)
                    return -1;  // c2 is always bigger 
                if (c2.rank == NUM_RANKS - 1)
                    return 1;   // c1 is always bigger
            }
            // Sort by rank.  Ties are fine.
            return c1.rank - c2.rank;
        }
        
    }
}
