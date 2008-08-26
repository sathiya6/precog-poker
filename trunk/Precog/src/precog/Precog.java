/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 *          Shawn Xu (talkingraisin@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;
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
            System.out.println("my hand : " + myHand + " " + this.percentileRank(gi));
            Hand highHand = Precog.getHighestHand(myHand, gi.getBoard());
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
        cache_poss = null;
    }

    public void endRound(GameInfo gi) 
    {
    }

    public void acceptHand(Hand h) 
    {
        myHand = h;        
    }
    
    /*
     * chen score to tolerance array
     * will be populated with cached tolerances. access with the chen score + 1
     * so a chen score of -1 will have its corresponding value in cs_tolerance[0]
     */ 
    private double[] cs_tolerance = new double[22];
    
    /*
     * returns the max that we're willing to bet
     * this should consider the proportion of our money.. so if we were
     * playing games where each player starts with 1 million, we won't always
     * fold when people bet in the hundreds
     * 
     * this function is currently very rudimentery
     */
    private double pocket_tolerance(int pocketscore)
    {
        if (pocketscore == -1 || pocketscore == 0)
        {
            return 0.;
        }
        //we can move these values out to become fields later as neccessary
        double m = 0.3; //coefficient
        double n = 2; //base of the exponent
        double c = 0.0; //verticle shift
        return (m * Math.pow(m, pocketscore) - c);
    }
    
    //this field is used with percentileRank()
    private LinkedList<Hand> cache_poss;
    
    private LinkedList<Long> cache_poss_long;
    /**
     * use monte carlo approach. this should only be used after the flop comes out
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
        
        int totalOthers = 0;
        double notbigger = 0.d; //# of hands less than or equal to us
        Hand myHighest = Precog.getHighestHand(myHand, gi.getBoard());
        int myRating = rate(myHighest);
        if (cache_poss == null)
        {
            LinkedList<Hand> possibilities = new LinkedList<Hand>();
            Card[] cards = remaining.getCards();
            for (int i = 0; i < cards.length-1; i++)
            {
                for (int j = i + 1; j < cards.length; j++)
                {
                    Hand aHand = new Hand(cards[i], cards[j]);
                    possibilities.add(aHand);
                    totalOthers++;
                    if (rate(Precog.getHighestHand(aHand, gi.getBoard())) >= myRating)
                        notbigger++;
                }
            }
            cache_poss = possibilities;
        }
        else
        {
            Card last = gi.getRiver() == null ? gi.getTurn() : gi.getRiver();
            for (ListIterator<Hand> iter = cache_poss.listIterator(); iter.hasNext();)
            {
                Hand cur = iter.next();
                if (cur.has(last))
                {
                    iter.remove();
                    continue;
                }
                totalOthers++;
                if (rate(Precog.getHighestHand(cur, gi.getBoard())) >= myRating)
                    notbigger++;
            }
        }
        
        return (notbigger/totalOthers);
    }
    
    private double percentileRank2(GameInfo gi)
    {
        long remaining = 0xFFFFFFFFFFFFFL;
        long hand = myHand.getBitCards();
        long board = gi.getBoard().getBitCards();
        remaining ^= (hand | board);
        
        int totalOthers = 0;
        double notbigger = 0.d; //# of hands less than or equal to us
        long myHighest = Precog.getHighestHand(hand, board);
        int myRating = rate(myHighest);
        
        int count = bitCount_dense(remaining);               
        if (cache_poss_long == null)
        {
            LinkedList<Long> possibilities = new LinkedList<Long>();
            long a = remaining;
            long b1, b2;
            long c;
            for (int i = 0; i < count - 1; i++)
            {
                c = a ^= b1 = a & -a; // b1 is the lowest bit                                
                for (int j = i + 1; j < count; j++)
                {
                    c ^= b2 = c & -c;                    
                    long aHand = b1 | b2;
                    possibilities.add(aHand);
                    totalOthers++;
                    if (rate(Precog.getHighestHand(aHand, board)) >= myRating)
                        notbigger++;
                }
            }
            cache_poss_long = possibilities;
        }
        else
        {
            Card last = gi.getTurn() == null ? gi.getRiver() : gi.getTurn();
            for (ListIterator<Long> iter = cache_poss_long.listIterator(); iter.hasNext();)
            {
                Long cur = iter.next();
                if ((cur & last.getValue()) != 0)
                {
                    iter.remove();
                    continue;
                }
                totalOthers++;
                if (rate(Precog.getHighestHand(cur, board)) >= myRating)
                    notbigger++;
            }
        }
        
        return 0.d;
    }
    
    	/**
	 * @param pHand Player's 2 card hand.
	 * @param bHand Board's 3-5 cards. must have 3-5 cards
	 * @return The strongest poker hand that can be made from pHand and bHand.
	 */
    public static Hand getHighestHand(Hand pHand, Hand bHand)
    {
        return new Hand(getHighestHand(pHand.getBitCards(), bHand.getBitCards()), 5, 5);
    }
    
    public static long getHighestHand(long pHand, long bHand)
    {
        long bigHand = pHand | bHand;
        long[] combos;
        int count = bitCount(bigHand);
        if (count == 7)
            combos = getCombinations7(bigHand);
        else if (count == 6)
            combos = getCombinations6(bigHand);
        else
            return bigHand;
        
        long highest = 0L;
        for (long candidate : combos)
        {
            if (highest == 0L)
            {
                highest = candidate;
                continue;
            }
            if (rate(candidate) < rate(highest))
                highest = candidate;
        }
        return highest;
    }    
    
    /**
	 * @param bigHand a long with 6 bits set
	 * @return All combinations of 5 bits set out of bigHand
     */
    private static long[] getCombinations6(long bigHand)
    {
        long copy = bigHand;      
        long[] ret = new long[6];        
        
        long bit;
        for (int i = 0; i < 6; i++)        
        {
            ret[i] = copy ^ (bit = (bigHand & -bigHand));
            bigHand ^= bit;
        }        
        
        return ret;
    }

    /**
	 * @param bigHand a long with 7 bits set
	 * @return All combinations of 5 bits set out of bigHand
     */
    private static long[] getCombinations7(long bigHand)
    {
        long copy = bigHand;      
        long[] bits = new long[7];
        long[] ret = new long[21];        
        
        for (int i = 0; i < 7; i++)        
            bigHand ^= bits[i] = bigHand & -bigHand;
        
        int idx = 0;
        for (int i = 0; i < 6; i++)
            for (int j = i + 1; j < 7; j++)
                ret[idx++] = copy ^ (bits[i] | bits[j]);
        
        return ret;
    }
    
    private static int bitCount(long l)
    {
        int c;
        for (c = 0; l != 0; c++)
            l &= l - 1;        
        return c;
    }
    
    private static int bitCount_dense(long n)   
    {
       int count = 64;
       n ^= 0xFFFFFFFFFFFFFFFFL;
       while (n != 0) 
       {
          count-- ;
          n &= (n - 1); 
       }
       return count;
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
        assert h.size() == 5 : "rate() passed a hand whose size != 5";
        
        return rate(h.getBitCards());
    }
    
    private static int rate(long h)
    {
        int slh = suitlessHand(h);
        
        // Takes care of the Flush and Straight Flush
        if (hasFlush(h))        
            return flushes[slh];
        
        // Takes care of Straight and High Card
        if (unique5[slh] != 0) //5 unique card values                   
            return unique5[slh];
        
        // Takes care of the rest (used to be binary search, now using perfect hash)
        //int idx = binarySearch(multBits(h), 0, products.length-1);
        return hash_values[perfect_hash(multBits(h))];
    }
        
    private static boolean hasFlush(long h)
    {
            if ((h | Card.SPADES_MASK) == Card.SPADES_MASK) return true;
            if ((h | Card.CLUBS_MASK) == Card.CLUBS_MASK) return true;
            if ((h | Card.DIAMONDS_MASK) == Card.DIAMONDS_MASK) return true;
            if ((h | Card.HEARTS_MASK) == Card.HEARTS_MASK) return true;
            return false;
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
        assert h.size() == 5 : "multBits(): h size is not 5!!!";
        return multBits(h.getBitCards());        
    }
    
    private static int multBits(long h)
    {
        int result = 1;
        long bits = h;        
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
    private static int scorePocket(Hand h) 
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
