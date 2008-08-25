/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.StringTokenizer;
import poker.engine.*;

public class Precog extends Player
{
    private Hand myHand;
    
    /**
     *  Perfect Hashtable for bitposition. This table returns the position of the bit set.
     *  Usage: bitpos64[(int)((bit*0x07EDD5E59A4E28C2L)>>>58)];
     */
    private static int[] bitpos64 =
    {
	63,  0, 58,  1, 59, 47, 53,  2,
	60, 39, 48, 27, 54, 33, 42,  3,
	61, 51, 37, 40, 49, 18, 28, 20,
	55, 30, 34, 11, 43, 14, 22,  4,
	62, 57, 46, 52, 38, 26, 32, 41,
	50, 36, 17, 19, 29, 10, 13, 21,
	56, 45, 25, 31, 35, 16,  9, 12,
	44, 24, 15,  8, 23,  7,  6,  5
    };
    
    private static short[] flushes = new short[7937];
    private static short[] unique5 = new short[7937];
    //private static int[] products = new int[4888];
    //private static short[] values = new short[4888];
    private static short[] hash_values = new short[8192];
    private static short[] hash_adjust = new short[512];
    private static byte[][] chen_scores;
    
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
    
    private void populateArrayFromPCT(String filename, byte[][] array) throws IOException
    {
        BufferedReader f = getBR(filename);
        String curLine;
        int index1 = 0;
        int index2 = 0;
        while ((curLine = f.readLine()) != null)
        {
            StringTokenizer st = new StringTokenizer(curLine);
            while (st.hasMoreTokens())            
                array[index1][index2++] = Byte.parseByte(st.nextToken());    
            index1++;
            index2 = 0;
        }
    }
        
    private void initializeChenArray()
    {
        int j = 51;
        chen_scores = new byte[j][];
        for (int i = 0; i < 51; i++)        
            chen_scores[i] = new byte[j--];                               
    }
    
    private void initiate() throws FileNotFoundException, IOException
    {                            
        initializeChenArray();
        populateArrayFromPCT("flushes.pct", flushes);         
        populateArrayFromPCT("unique5.pct", unique5);        
        //populateArrayFromPCT("products.pct", products);    
        //populateArrayFromPCT("values.pct", values);        
        populateArrayFromPCT("hash_values.pct", hash_values);
        populateArrayFromPCT("hash_adjust.pct", hash_adjust);
        populateArrayFromPCT("chenFormula.pct", chen_scores);
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
    
    /**
     * use monte carlo approach
     * @return double between 0 and 1 representing % of hands could beat
     */
    private double percentileRank(GameInfo gi)
    {
        Hand remaining = new Hand(0xFFFFFFFFFFFFFL, 52, 52);
        for (Card c : gi.getBoard().getCards())
        {
            remaining.remove(c);
        }
        for (Card c : myHand.getCards())
        {
            remaining.remove(c);
        }
        //now enumerate. 
        LinkedList<Hand> possibilities = new LinkedList<Hand>();
        Card[] cards = remaining.getCards();
        for (int i = 0; i < cards.length-1; i++)
        {
            for (int j = i + 1; j < cards.length; j++)
            {
                Hand aHand = new Hand(cards[i], cards[j]);
                possibilities.add(aHand);
            }
        }
        Hand myHighest = Hand.getHighestHand(myHand, gi.getBoard());
        int myRating = rate(myHighest);
        double notbigger = 0; //# of hands less than or equal to us
        for (Hand h : possibilities)
        {
            if (rate(Hand.getHighestHand(h, gi.getBoard())) >= myRating)
                notbigger++;
        }
        return (notbigger/possibilities.size());
    }
                  
    /**
     * There are 7462 distinct poker hands, in these categories (not in order of rank):
     * 
     * Straight Flush (includes Royal Flush)
     * Flush
     * 
     * Straight
     * High Card
     * 
     * Full House
     * Four of a Kind
     * Three of a Kind
     * Two Pair
     * One Pair
     * 
     * @param h the hand to rate. for now, 5 card hand
     * @return rating.
     */    
    private static int rate(Hand h)
    {
        assert (h.size() == 5);
        
        int slh = suitlessHand(h.getBitCards());
        
        // Takes care of the Flush and Straight Flush
        if (Hand.hasFlush(h))        
            return flushes[slh];
        
        // Takes care of Straight and High Card
        if (unique5[slh] != 0) //5 unique card values                   
            return unique5[slh];
        
        // Takes care of the rest (used to be binary search, now using perfect hash)
        //int idx = binarySearch(multBits(h), 0, products.length-1);
        return hash_values[perfect_hash(multBits(h))];
    }
        
    // amazing...
    private static int perfect_hash(int u)
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
    private static int suitlessHand(long h)
    {
        int slh = 0;
        int i = 0;
        while (h != 0L)
        {
            if ((h & 0xFL) != 0) //if right most 4 bits have a 1 set            
                slh |= (1 << i);            
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
            result *= bc_to_prime[bitpos64[(int)(((bit = (bits & -bits))*0x07EDD5E59A4E28C2L)>>>58)]];
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
              
    
    
    /**
     * Chen Formula: devised by Poker Champion William Chen
     * Used for scoring Pocket cards
     * Approximately 5x faster than scorePocket_original
     */
    public int scorePocket(Hand h) 
    {
        int indexLow;
        long l;
        long c = h.getBitCards();        
        c ^= l = c & -c;               
        return chen_scores[indexLow = bitpos64[(int)((l*0x07EDD5E59A4E28C2L)>>>58)]][bitpos64[(int)((c*0x07EDD5E59A4E28C2L)>>>58)] - indexLow - 1];
    }
    
    private int scorePocket_original(Hand h)
    {        
        double score = 0.d;
        boolean paired = false;
        Card high = Hand.getHighestCard(h);
        h.remove(high);
        Card low = Hand.getHighestCard(h);
        
        switch (high.getNumber())
        {
            case 14:
                score += 10.d;           
                break;
            case 13:
                score += 8.d;           
                break;
            case 12:
                score += 7.d;           
                break;
            case 11:
                score += 6.d;           
                break;
            default:
                score += ((double)high.getNumber()) / 2;
        }
        
        // pairs
        if (low.getNumber() == high.getNumber())
        {
            paired = true;
            score *= 2;
            if (score < 5.d)
                score = 5.d;
        }
        
        // round up half points
        score = Math.round(score);               
             
        if (!paired)
        {
            // gap
            switch (high.getNumber() - low.getNumber() - 1)
            {
                case 0: 
                    if (high.getNumber() <= 11)
                        score += 1.d;
                    break;
                case 1:  
                    if (high.getNumber() <= 11)
                        score += 1.d;
                    score -= 1.d;
                    break;
                case 2: 
                    score -= 2.d;
                    break;
                case 3: 
                    score -= 4.d;
                    break;
                default: 
                    score -= 5.d;
            }
        }
        
        // suited
        if (high.getSuit().equals(low.getSuit()))
            score += 2.d;
        
        return (int)score;
    }
            
    private void printChenFormulaArray()
    {
        for (int i = 0; i < 51; i++)
        {
            for (int j = i + 1; j < 52; j++)
            {                                
                Hand h = new Hand((0x1L << j) | (0x1L << i), 2, 2);                
                int score = scorePocket_original(h);
                System.out.print(score + " ");
            }
            System.out.print("\n");
        }
    }
    
    // this method belongs in Test...too lazy today
    public boolean verifyScorePocket()
    {
        for (int i = 0; i < 51; i++)
        {
            for (int j = i + 1; j < 52; j++)
            {                                
                Hand h = new Hand((0x1L << j) | (0x1L << i), 2, 2);
                int score1 = scorePocket_original(h);          
                h = new Hand((0x1L << j) | (0x1L << i), 2, 2);
                int score2 = scorePocket(h);                
                if (score1 != score2)
                    return false;
            }            
        }
        return true;
    }
    
}
