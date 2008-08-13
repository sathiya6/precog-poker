/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import poker.engine.*;

public class Precog extends Player
{
    private Hand myHand;
    private static HashMap<Long, Integer> bitPosition = new HashMap<Long, Integer>();
    
    private static short[] flushes = new short[7937];
    private static short[] unique5 = new short[7937];
    private static int[] products = new int[4888];
    private static short[] values = new short[4888];
    
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
        try
        {
            initiate();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.err.println(".pct file(s) not found");
            System.exit(-1);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }    
    private void initiate() throws FileNotFoundException, IOException
    {
        for (int i = 0; i < 52; i++)
        {
            bitPosition.put(1L<<i, i);
        }
        

        BufferedReader f = new BufferedReader(new FileReader("flushes.pct"));
        String curLine;
        int index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())
            {
                flushes[index++] = Short.parseShort(st.nextToken());
            }
        }
        
        f = new BufferedReader(new FileReader("unique5.pct"));
        index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())
            {
                unique5[index++] = Short.parseShort(st.nextToken());
            }
        }
        
        f = new BufferedReader(new FileReader("products.pct"));
        index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())
            {
                products[index++] = Integer.parseInt(st.nextToken());
            }
        }
        
        f = new BufferedReader(new FileReader("values.pct"));
        index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())
            {
                values[index++] = Short.parseShort(st.nextToken());
            }
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
            if (this.twoPair.compareTo(myHand) < 0)//(rating > 4000)
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
