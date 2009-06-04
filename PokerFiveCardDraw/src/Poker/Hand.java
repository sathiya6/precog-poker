/**
 * Created on Mar 25, 2008
 * @author garberd
 */
package Poker;

import java.util.ArrayList;
import java.util.Comparator;

public class Hand implements Comparable<Hand>
{
    public static int evaluateAndSortCards(Card[] cards)
    {
        Hand hand = new Hand(cards);
        int type = hand.evaluateAndSortHand();
        for (int i = 0; i < cards.length; i++)
            cards[i] = hand.getCard(i);
        return type;
    }

    
    private Card[] cards;
    private int handType = UNKNOWN;
    
    public Hand(Card[] cards)
    {
        // Copy the array, so caller may change it
        this.cards = new Card[cards.length];
        System.arraycopy(cards, 0, this.cards, 0, cards.length);
    }
    
    public Hand(String str)
    {
        // For test purposes, this takes a toString of a hand, and recreates the hand
        ArrayList<Card> list = new ArrayList<Card>();
        while (str.length() >= 2)
        {
            list.add(new Card(str));  // Card constructor ignores chars after the first 2
            // look for next card string
            int i = 2;
            while (i < str.length() && (str.charAt(i) == ',' || Character.isWhitespace(str.charAt(i))))
                i++;  // skip past commas and whitespace
            str = str.substring(i);
        }
        // Convert to array
        cards = new Card[list.size()];
        list.toArray(cards);
    }
    
    public Hand(Hand h)
    {
        this(h.cards);
    }
    
    public Card getCard(int index)
    {
        return cards[index];
    }
    
    public Card[] copyCards(int first, int count)
    {
        Card[] copy = new Card[count];
        System.arraycopy(cards, first, copy, 0, count);
        return copy;
    }
    
    public int numCards()
    {
        return cards.length;
    }

    boolean replaceCard(Card remove, Card add)
    {
    	for (int i = 0; i < cards.length; i++)
    	{
    		if (cards[i].equals(remove))
    		{
    			cards[i] = new Card(add);
    			return true;  // replace succeeded
    		}
    	}
    	return false;  // remove card isn't in hand
    }
    
    public static final int UNKNOWN = 0;
    public static final int CARD_HIGH = 1;
    public static final int PAIR = 2;
    public static final int TWO_PAIR = 3;
    public static final int THREE_OF_A_KIND = 4;
    public static final int STRAIGHT = 5;
    public static final int FLUSH = 6;
    public static final int FULL_HOUSE = 7;
    public static final int FOUR_OF_A_KIND = 8;
    public static final int STRAIGHT_FLUSH = 9;
    public static final int IMPOSSIBLE = 10;
    
    public static final String[] HAND_TYPES =
        { "Unknown", "High card", "Pair", "Two pair", "Three of a kind", "Straight", "Flush", "Full house", "Four of a kind", "Straight flush", "Impossible" };
    
    /**
     * Evaluates a hand to figure out which kind of poker hand it is.
     * Initial hand can have any number of cards.
     * Afterwards, hand will be sorted, and first 5 cards are the final hand.
     * This method is not built to scale to R ranks.
     * @return hand type
     */
    public int evaluateAndSortHand()
    {
        if (handType > UNKNOWN)
            return handType;
        
        int[] suits = new int[Card.NUM_SUITS];
        int[] ranks = new int[Card.NUM_RANKS];
        int[] sets = new int[Card.NUM_SUITS + 1];  // there can be S of a kind if there are S suits (assuming 1 deck)
        
        for (int i = 0; i < cards.length; i++)
        {
            // Gather flush info
            Card c = cards[i];
            suits[c.getSuit()]++;

            // Gather set info
            int r = c.getRank();
            ranks[r]++;
            sets[ranks[r]]++;
        }
        
        // Look for a flush
        sortSuit(false);
        boolean flush = false;
        int highStraightFlush = -1;
        for (int i = 0; i < cards.length; )
        {
            int s = cards[i].getSuit();
            int cs = suits[s]; 
            int nextSuit = i + cs;
            if (cs >= 5)
            {
                flush = true;
                int high = findStraight(cards, i, nextSuit);
                if (high > highStraightFlush)
                {
                    // A straight flush!
                    highStraightFlush = high;
                    shiftStraightToFront(cards, i, nextSuit, high);
                }
                else if (highStraightFlush < 0)
                {
                    // No straight flushes -- just make sure best flush is first
                    if (compareFlush(cards, i, cards, 0) > 0)
                    {
                        shiftSuitToFront(cards, s);
                    }
                }
            }
            i = nextSuit;  // after loop, i will be at next suit
        }
        if (highStraightFlush >= 0)
            return handType = STRAIGHT_FLUSH;  // Can't beat this one!
        
        if (sets[4] >= 1)
        {
            // fill tie-breaker with best 4-of-a-kind, plus best other card
            sortRank(false);
            for (int r = Card.NUM_RANKS - 1; r >= 0; r--)
            {
                if (ranks[r] >= 4)
                {
                    shiftRankToFront(cards, r);
                    break;
                }
            }
            // remaining best card should now be 5th card
            return handType = FOUR_OF_A_KIND;
        }
        if (sets[3] >= 1 && sets[2] >= 2)
        {
            // It's probably inefficient, but it works...
            // Shift each rank of which there is a pair or better to the front, lowest rank first. 
            for (int r = 0; r < Card.NUM_RANKS; r++)
            {
                if (ranks[r] >= 2)
                    shiftRankToFront(cards, r);
            }
            // Highest rank is now up in front.  
            // Finally, shift best triple to front.
            for (int r = Card.NUM_RANKS - 1; r >= 0; r--)
            {
                if (ranks[r] >= 3)
                {
                    shiftRankToFront(cards, r);
                    break;
                }
            }
            return handType = FULL_HOUSE;
        }
    
        if (flush)
        {
            // Cards are still sorted from earlier
            return handType = FLUSH;
        }
        
        // Anything else ignores suit, and cares only about rank.
        sortRank(false);
        
        int highStraight = findStraight(cards, 0, cards.length);
        if (highStraight >= 0)
        {
            shiftStraightToFront(cards, 0, cards.length, highStraight);
            return handType = STRAIGHT;
        }

        // Shift best 1 or 2 sets to front
        int cSets = sets[2];
        for (int r = 0; r < Card.NUM_RANKS; r++)
        {
            if (ranks[r] >= 2)
            {
                if (cSets-- > 2)  // If there are many pairs, skip the low ones
                    continue;
                shiftRankToFront(cards, r);
            }
        }

        if (sets[3] >= 1)
            return handType = THREE_OF_A_KIND;
        if (sets[2] >= 2)
            return handType = TWO_PAIR;
        if (sets[2] >= 1)
            return handType = PAIR;
        return handType = CARD_HIGH;
    }
    
    private static int findStraight(Card[] hand, int first, int end)
    {
        int lastRank = -1;
        int consecutive = 0;
        int high = -1;
        for (int i = first; i < end; i++)
        {
            if (hand[i].getRank() == lastRank)
                continue;
            if (hand[i].getRank() == lastRank - 1)
            {
                if (++consecutive >= 5)
                    return high;  // found first straight
            }
            else
            {
                high = hand[i].getRank();
                consecutive = 1;
            }
            lastRank = hand[i].getRank();
        }
        // check for low straight
        if (consecutive == 4 && lastRank == 0 && hand[first].getRank() == Card.NUM_RANKS - 1)
            return high;
        // no straights
        return -1;
    }
        
    private static void shiftStraightToFront(Card[] hand, int first, int end, int high)
    {
        int front = 0;
        for (int i = first; i < end; i++)
        {
            if (hand[i].getRank() == high)
            {
                shiftCard(hand, i, front++);
                high--;
            }
        }
        // low straight should be automatic -- ace should be sitting in correct location
    }
    
    private static int compareFlush(Card[] hand1, int first1, Card[] hand2, int first2)
    {
        for (int i = 0; i < 5; i++)
        {
            // If either isn't a flush at all, then that one sorts later
            if (hand1[first1 + i].getSuit() != hand1[first1].getSuit())
                return -1;
            if (hand2[first2 + i].getSuit() != hand2[first2].getSuit())
                return 1;
            if (hand1[first1 + i].getRank() != hand2[first2 + i].getRank())
                return hand1[first1 + i].getRank() - hand2[first2 + i].getRank();
        }
        return 0;
    }

    /**
     * Insertion sort, with larger items first.  
     * If hand.length weren't just 5, I'd do something clever like a ShellSort.
     */
    private static void sort(Card[] hand, Comparator<Card> comp)
    {
        for (int i = 1; i < hand.length; i++)
        {
            Card t = hand[i];
            int j = i - 1;
            for ( ; j >= 0; j--)
            {
                if (comp.compare(hand[j], t) < 0)
                    hand[j + 1] = hand[j];
                else
                    break;
            }
            hand[j + 1] = t;
        }
    }
    
    /**
     * Moves the cards starting at first (inclusive), and ending an end (exclusive)
     * to the front of the array of cards.
     */
    private static void shiftToFront(Card[] hand, int first, int end)
    {
        Card[] hand2 = new Card[end - first];
        System.arraycopy(hand, first, hand2, 0, end - first);  // Copy our suit to temp array
        System.arraycopy(hand, 0, hand, end - first, first);   // Shift start of array to middle
        System.arraycopy(hand2, 0, hand, 0, end - first);      // Copy temp array back into start of hand 
    }

    /**
     * Moves the cards starting at first (inclusive), and ending an end (exclusive)
     * to the front of the array of cards.
     */
    private static void shiftCard(Card[] hand, int from, int to)
    {
        Card temp = hand[from];
        if (to < from)
            System.arraycopy(hand, to, hand, to + 1, from - to);
        else
            System.arraycopy(hand, from + 1, hand, from, to - from);
        hand[to] = temp; 
    }

    /**
     * Sort the hand by suit, and by rank within suit.
     */
    public void sortSuit(boolean lowAce)
    {
        Comparator<Card> comp = new Card.SuitComparator(lowAce);
        sort(cards, comp);
    }
    
    /**
     * Insert all cards that match that suit to the front of the array.
     * Presumes that hand is already sorted by suit, so they're all together.
     */
    private static int shiftSuitToFront(Card[] hand, int suit)
    {
        // find the specified suit
        int first = 0;
        for ( ; first < hand.length && hand[first].getSuit() != suit; first++)
            ;
        if (first >= hand.length)
            return 0;  // that suit wasn't found
        // find the end of that suit
        int end = first + 1;
        for ( ; end < hand.length && hand[end].getSuit() == suit; end++)
            ;
        
        shiftToFront(hand, first, end);
        return end - first;
    }
    
    /**
     * Sort the hand by rank.
     */
    public void sortRank(boolean lowAce)
    {
        Comparator<Card> comp = new Card.RankComparator(lowAce);
        sort(cards, comp);
    }

    /**
     * Insert all cards that match that suit to the front of the array.
     * Presumes that hand is already sorted by suit, so they're all together.
     */
    private static int shiftRankToFront(Card[] hand, int rank)
    {
        // find the specified rank
        int first = 0;
        for ( ; first < hand.length && hand[first].getRank() != rank; first++)
            ;
        if (first >= hand.length)
            return 0;  // that rank wasn't found
        // find the end of that rank
        int end = first + 1;
        for ( ; end < hand.length && hand[end].getRank() == rank; end++)
            ;
        
        shiftToFront(hand, first, end);
        return end - first;
    }

    @Override
    public int compareTo(Hand h)
    {
        int type = this.evaluateAndSortHand();
        int hType = h.evaluateAndSortHand();
        if (type != hType)
            return type - hType;
        for (int i = 0; i < 5; i++)
        {
            // Neither hand should be short of cards, but if one is, that hand loses
            if (i >= cards.length)
                return -1;
            if (i >= h.cards.length)
                return 1;
            // Since both hands are of the same type, only the ranks count.
            // The cards are already sorted in an order appropriate for the hand type.
            // So first non-matching card determines the winner.
            if (this.cards[i].getRank() != h.cards[i].getRank())
                return this.cards[i].getRank() - h.cards[i].getRank();
        }
        // The first 5 cards are all the same!
        return 0;
    }
    
    public String toString()
    {
        if (handType == UNKNOWN)
        {
            String str = cards.length + " cards: ";
            for (Card c : cards)
                str += c + ", ";
            return str;
        }
        else
        {
            String str = HAND_TYPES[handType] + ": ";
            for (int i = 0; i < cards.length; i++)
            {
                if (i >= 5)
                    break;
                if (i > 0)
                    str += ", ";
                str += cards[i];
            }
            return str;
        }
    }
}
