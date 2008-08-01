/**
 * author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.util.HashMap;
import poker.engine.*;
/**
 *
 * @author kevinl
 */
public class Precog extends Player
{
    private Hand myHand;
    private static HashMap<Long, Integer> bitPosition = new HashMap<Long, Integer>();
    
    private static Hand twoPair = new Hand(
            ((Card.TWO_MASK & Card.SPADES_MASK) |
             (Card.TWO_MASK & Card.CLUBS_MASK) |
             (Card.THREE_MASK & Card.HEARTS_MASK) |
             (Card.THREE_MASK & Card.SPADES_MASK) |
             (Card.FOUR_MASK & Card.SPADES_MASK)), 5,5
            );
    
    public Precog(String _name)
    {
        super(_name);
        initiate();
    }
    
    private void initiate()
    {
        for (int i = 0; i < 52; i++)
        {
            bitPosition.put(1L<<i, i);
        }
    }

    public Action beginTurn(GameInfo gi)
    {
        GameState.State curState = gi.getCurrentState();
        if (curState.equals(GameState.State.FIRSTBET))
        {
            if (gi.isValid(new Check(gi.getId(this))))
            {
                return new Check(gi.getId(this));
            }
            else if (gi.getMinimumCallAmount().getAmount() < 10.)
            {
                return new Call(gi.getId(this));
            }
            else return new Fold(gi.getId(this));
        }
        else
        {
            //int rating = rate(Hand.getHighestHand(myHand, gi.getBoard()));
            if (twoPair.compareTo(myHand) < 0)//(rating > 4000)
            {
                if (gi.getBet(this).getAmount() < 5)
                {
                    return new Raise(gi.getId(this), new Money(10.0, Money.Currency.DOLLARS));
                }
                else //if (gi.getMinimumCallAmount().getAmount() < 5.)
                {
                    return new Call(gi.getId(this));
                }
                //else return new Fold(gi.getId(this));
            }
            else return new Fold(gi.getId(this));
        }
    }

    public void beginRound(GameInfo gi)
    {
        
    }

    public void endRound(GameInfo gi) 
    {
    }

    public void acceptHand(Hand h) 
    {
        myHand = h;
    }
    
    
    
    
    //7462 distinct poker hands
    /**
     * 
     * @param h the hand to rate. for now, 5 card hand
     * @return rating.
     */
    private int rate(Hand h)
    {
        int slh = suitlessHand(h.getBitCards());
        if (Hand.hasFlush(h))
        {
            return 0;
            //return PrecogTables.flushes[slh]; //5 unique, garunteed to be in scope
        }
        //if (PrecogTables.unique5[slh] != 0)
        { //5 unique card values (got example: ace, king, six)
            //return PrecogTables.unique5[slh];
        }
        //int idx = binarySearch(multBits(h), 0, PrecogTables.products.length-1);
        return 0;//PrecogTables.values[idx];
    }
    
    //returns an index to look at for hand rank
   // private static int binarySearch(int target, int left, int right)
    //{
        //int mid = (left+right)/2;
        //if (PrecogTables.products[mid] == target)
            //return mid;
        //if (PrecogTables.products[mid] > target)
            //return binarySearch(target, left, mid);
        //else
            //return binarySearch(target, mid, right);
    //}
    
    /**
     * converts a long in which 52 bits are used to an int in which 13 bits are used.
     * use this only for the special case of flush: each of the 5 cards
     * are unique in value and have the same suit
     * 
     * if passed a 5 card hand, this returns a value between
     * 0x1F00 and 0x001F
     */
    private static int suitlessHand(long h)
    {
        int slh = 0;
        int i = 0;
        while (h != 0L)
        {
            if ((h & 0xFL) != 0) //if right most 4 bits have a 1 set
            {
                slh ^= (1 << i);
            }
            i++;
            h >>>= 4;
        }
        return slh;
    }
    
    //use for a 5 card hand
    private static int multBits(Hand h)
    {
        int result = 1;
        long bits = h.getBitCards();
        while (bits != 0)
        {
            result *= bc_to_prime[bitPosition.get(bits & -bits)];
            bits ^= bits & -bits;
        }
        return result;
    }

    // 2 3 4 5 6  7  8  9   10  j   q   k   a
    // 2 3 5 7 11 13 17 19  23  29  31  37  41
    /* access rate_hc[1] through rate_hc[52]
     * bitcards to prime -> bc to prime
     */
    private static final int[] bc_to_prime =
    {
        1,
        2,2,2,2,//deuce
        3,3,3,3,//3
        5,5,5,5,//4
        7,7,7,7,
        11,11,11,11,
        13,13,13,13,
        17,17,17,17,
        19,19,19,19,
        23,23,23,23,
        29,29,29,29,
        31,31,31,31,
        37,37,37,37,
        41,41,41,41
    };
    


    

}
