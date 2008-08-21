/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;
import poker.engine.*;

public class Precog extends Player
{
    private Hand myHand;
    private static HashMap<Long, Integer> bitPosition = new HashMap<Long, Integer>();
    
    private static short[] flushes = new short[7937];
    private static short[] unique5 = new short[7937];
    //private static int[] products = new int[4888];
    //private static short[] values = new short[4888];
    private static short[] hash_values = new short[8192];
    private static short[] hash_adjust = new short[512];
    
    //distinct -> unique multiplying factor
    private static final int MFACTOR_STRAIGHT_FLUSH = 4;
    private static final int MFACTOR_FOUR_OF_A_KIND = 4;
    private static final int MFACTOR_FULL_HOUSE = 24;
    private static final int MFACTOR_FLUSH = 4;
    private static final int MFACTOR_STRAIGHT = 1020;
    private static final int MFACTOR_THREE_OF_A_KIND = 64;
    private static final int MFACTOR_TWO_PAIR = 144;
    private static final int MFACTOR_PAIR = 1020;
    
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
    
    private BufferedReader getBR(String filename)
    {
        InputStream is = this.getClass().getResourceAsStream(filename);
        InputStreamReader isr = new InputStreamReader(is);
        return new BufferedReader(isr);
    }
    
    private void populateArrayFromPCT(String filename, short[] array) throws IOException
    {
        BufferedReader f = getBR(filename);
        String curLine;
        int index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())            
                array[index++] = Short.parseShort(st.nextToken());            
        }
    }
    
    private void populateArrayFromPCT(String filename, int[] array) throws IOException
    {
        BufferedReader f = getBR(filename);
        String curLine;
        int index = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())            
                array[index++] = Integer.parseInt(st.nextToken());            
        }
    }
    
    private void initiate() throws FileNotFoundException, IOException
    {
        for (int i = 0; i < 52; i++)                             
            bitPosition.put(1L<<i, i+1);                     
        populateArrayFromPCT("flushes.pct", flushes);         
        populateArrayFromPCT("unique5.pct", unique5);        
        //populateArrayFromPCT("products.pct", products);    
        //populateArrayFromPCT("values.pct", values);        
        populateArrayFromPCT("hash_values.pct", hash_values);
        populateArrayFromPCT("hash_adjust.pct", hash_adjust);
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
            Hand highHand = Hand.getHighestHand(myHand, gi.getBoard());
            int rating = rate(highHand);
            //System.out.println("precog beginTurn(): " + highHand + " -rating: " + rating);
            if (rating < 4000)
            {
                if (gi.getBet(this).getAmount() < 5)
                {
                    double raise = (gi.getStash(this).getAmount() < 10.0)
                            ? gi.getStash(this).getAmount() : 10.0;
                    if (raise == 0.)
                    {
                        Action a = new Call(gi.getId(this));
                        if (gi.isValid(a))
                        {
                            return a;
                        }
                        else return new Check(gi.getId(this));
                    }
                    return new Raise(gi.getId(this), new Money(raise, Money.Currency.DOLLARS));
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
        assert (h.size() == 5);
        int slh = suitlessHand(h.getBitCards());
        if (Hand.hasFlush(h))
        {
            System.out.println("we have a flush!");
            return flushes[slh]; //5 unique, garunteed to be in scope
        }
        if (unique5[slh] != 0)
        { //5 unique card values (got example: ace, king, six)
            System.out.println("unique 5!");
            return unique5[slh];
        }
        //int idx = binarySearch(multBits(h), 0, products.length-1);
        return hash_values[perfect_hash(multBits(h))];
    }
        
    // amazing...
    private int perfect_hash(int u)
    {
      int a, b, r;
      u += 0xE91AAA35;
      u ^= u >>> 16;
      u += u << 8;
      u ^= u >>> 4;
      b  = (u >>> 8) & 0x1FF;
      a  = (u + (u << 2)) >>> 19;
      r  = a ^ hash_adjust[b];
      return r;
    }

    
    /*returns an index to look at for hand rank
     * deprecated since we use perfect hash function instead
     */
    /*private static int binarySearch(int target, int left, int right)
    {
        if (right < left)
        {
            return -1;
        }
        int mid = (left+right) >>> 1;
        if (products[mid] == target)
            return mid;
        if (products[mid] > target)
            return binarySearch(target, left, mid-1);
        else
            return binarySearch(target, mid+1, right);
    }*/
    
    /**
     * converts a long in which 52 bits are used to an int in which 13 bits are used.
     * use this only for the special case of flush: each of the 5 cards
     * are unique in value and have the same suit
     * 
     * if passed a 5 card hand, this returns a value between
     * 0x1F00 and 0x001F
     */
    public static int suitlessHand(long h)
    {
        int slh = 0;
        int i = 0;
        while (h != 0L)
        {
            if ((h & 0xFL) != 0) //if right most 4 bits have a 1 set
            {
                slh |= (1 << i);
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
        assert h.size() == 5 : "multBits - h size is not 5!!!";
        long bit;
        while (bits != 0)
        {
            result *= bc_to_prime[bitPosition.get(bit = (bits & -bits))]; //rightmost bit
            bits ^= bit;
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
