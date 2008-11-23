/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 *          Shawn Xu (talkingraisin@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import poker.engine.*;
import poker.engine.Money.Currency;

public class Precog extends Player
{
    private Hand myHand;
    private int myID;
    private static final int THREADS = 2;
    
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
    
    // 2 3 4 5 6  7  8  9   10  j   q   k   a
    // 2 3 5 7 11 13 17 19  23  29  31  37  41
    /* access rate_hc[0] through rate_hc[51]
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
    
    private static short[] flushes = new short[7937];
    private static short[] unique5 = new short[7937];
    //private static int[] products = new int[4888];
    //private static short[] values = new short[4888];
    private static short[] hash_values = new short[8192];
    private static short[] hash_adjust = new short[512];
    private static byte[][] chen_scores;
    
    /**
     * chen score to tolerance array
     * will be populated with cached tolerances. access with the chen score + 1
     * so a chen score of -1 will have its corresponding value in cs_tolerance[0]
     */ 
    private double[] cs_tolerance = new double[22];
    
    private double pf_avg_perc_cache;
    
    
    /****************************************************
     * 					Constructor						*
     ****************************************************/
    
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
            System.err.println("needed file(s) not found");
            System.exit(-1);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-2);
        }
    }    
    
    /****************************************************
     * 			Initialization Methods					*
     ****************************************************/
    
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
    
    private void fill_cs_tol()
    {
        for (int i = 0; i < 22; i++)
        {
            cs_tolerance[i] = calculate_cs_tol(i-1);
        }
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
        fill_cs_tol();
    }
    
    /****************************************************
     * 				Overridden Player Methods			*
     ****************************************************/

    public Action beginTurn(GameInfo gi)
    {
        switch (gi.getCurrentState())
        {
	        case FIRSTBET:
	        	return first_bet(gi);
	        case SECONDBET:
	        	return second_bet(gi);
	        case THIRDBET:
	        	return third_bet(gi);
	        case FINALBET:
	        	return fourth_bet(gi);
	        default:
	        	throw new IllegalStateException("Precog beginTurn state is messed up");
        }
    }

    public void beginRound(GameInfo gi)
    {       
    	myID = gi.getId(this);
    	pf_avg_perc_cache = -3.14;
    }

    public void endRound(GameInfo gi) 
    {
    	pf_avg_perc_cache = -3.14;
    }

    public void acceptHand(Hand h) 
    {
        myHand = h;        
    }

    /****************************************************
     * 				State Specific Methods				*
     ****************************************************/
    
    private Action first_bet(GameInfo gi)
    {
    	int chenscore = scorePocket(myHand);
    	//System.out.println("chenscore: " + chenscore);
    	double mytol = get_cs_tol(chenscore);
   	   	double myStash = gi.getStash(myID).getAmount();    
    	double raise_factor = 1.d / 3.d;
   	   	
		Action a;
		if (gi.isValid(a = new Check(myID))) // This means we are the first to raise (potentially)
		{
			// Remember we don't want to fold if we're the first to go, only later depending on the stake
			// But should we check or raise?
			
			if (mytol >= myStash)
			{
				if (myStash < 1)
					return new Check(myID);
				// if our tolerance is great, bet a third of our stash
				return new Raise(myID, new Money(myStash * raise_factor, Currency.DOLLARS));
			}
			else if (mytol > (1 / raise_factor))
			{
				// our tolerance is less than stash, but substantial
				return new Raise(myID, new Money(mytol * raise_factor, Currency.DOLLARS));
			}
			else
			{
				// horrible, we check for now, and see what other people do
				return a;
			}
		}
		else
		{
			// not first to raise
			
			// let's see what's at stake here
			double minCall = gi.getMinimumCallAmount().getAmount();
			double alreadyPutIn = gi.getBet(myID).getAmount();
			double extraNeeded = minCall - alreadyPutIn;
			
			if (alreadyPutIn <= 0)
			{
				// either we checked before, or it just got to us
				if (mytol > minCall)
				{
					// we have a good hand, we're in					
					double raise_amount;
					if (mytol > myStash)
					{
						// we don't wanna go all in, just call it
						return new Call(myID);
					}
					else
					{
						raise_amount = mytol - minCall;
						if (raise_amount < 1.d)
							raise_amount = 1.d;
						return new Raise(myID, new Money(raise_amount, Currency.DOLLARS));
					}
				}
				else if (mytol == minCall)
				{
					// we can tolerate exactly how much we need, just call it
					return new Call(myID);
				}
				else
				{
					// the stakes are too high, let's play it safe
					return new Fold(myID);
				}
			}
			else
			{
				// since we've raised before, we don't want to raise again
				return new Call(myID);
			}
		}    	
    }
    
    //after flop
    private Action second_bet(GameInfo gi)
    {
    	double p;
    	if (pf_avg_perc_cache < 0)
    		p = pf_avg_perc_cache = pf_avg_perc_multithread(myHand.getBitCards(), gi.getBoard().getBitCards(), THREADS);
    	else
    		p = pf_avg_perc_cache;
    	
    	System.out.println("pf avg perc: " + p);
    	    	
   	   	double myStash = gi.getStash(myID).getAmount();
   	   	double mytol = (p - expectPC(gi.getNumberOfPlayersThatStartedThisRound())) * myStash;    	
    	
    	Action a;
		if (gi.isValid(a = new Check(myID))) // This means we are the first to raise (potentially)
		{
			// Remember we don't want to fold if we're the first to go, only later depending on the stake
			// But should we check or raise?
			
			if (mytol >= myStash)
			{
				// if our tolerance is great, bet a third of our stash
				if (myStash < 1)
					return new Check(myID);
				return new Raise(myID, new Money(myStash, Currency.DOLLARS));
			}
			else if (mytol > 1)
			{
				// our tolerance is less than stash, but substantial
				return new Raise(myID, new Money(mytol, Currency.DOLLARS));
			}
			else
			{
				// horrible, we check for now, and see what other people do
				return a;
			}
		}
		else
		{
			// not first to raise
			
			// let's see what's at stake here
			double minCall = gi.getMinimumCallAmount().getAmount();
			double alreadyPutIn = gi.getBet(myID).getAmount();
			double extraNeeded = minCall - alreadyPutIn;
			
			if (alreadyPutIn <= 0)
			{
				// either we checked before, or it just got to us
				if (mytol > minCall)
				{
					// we have a good hand, we're in					
					double raise_amount;
					if (mytol > myStash)
					{
						// we don't wanna go all in, just call it
						return new Call(myID);
					}
					else
					{
						raise_amount = mytol - minCall;
						if (raise_amount < 1.d)
							raise_amount = 1.d;
						return new Raise(myID, new Money(raise_amount, Currency.DOLLARS));
					}
				}
				else if (mytol == minCall)
				{
					// we can tolerate exactly how much we need, just call it
					return new Call(myID);
				}
				else
				{
					// the stakes are too high, let's play it safe
					return new Fold(myID);
				}
			}
			else
			{
				// since we've raised before, we don't want to raise again
				
				
				
					return new Call(myID);
			}
		}    	    	
    }
    
    //after turn
    private Action third_bet(GameInfo gi)
    {
    	Action a;
		if (gi.isValid(a = new Call(myID)))
		{
			return a; 
		}
		else if (gi.isValid(a = new Check(myID)))
		{
			return a;
		}
    	return null;
    }
    
    //after river
    private Action fourth_bet(GameInfo gi)
    {
    	Action a;
		if (gi.isValid(a = new Call(myID)))
		{
			return a;
		}
		else if (gi.isValid(a = new Check(myID)))
		{
			return a;
		}
    	return null;
    }
    
    /****************************************************
     * 				Calculation Methods					*
     ****************************************************/      

    /**
     * Calculates the expected percentile cutoff given the number of active players
     * 
     * @param numPlayers number of active players
     * @return a double between 0 and 1 representing expected percentile cutoff 
     */
    private static double expectPC(int numPlayers)
    {
    	return 1 - (0.975d / numPlayers);
    }
    
    /**   
     * This method is the cached version of get_cs_tol_original()  
     * 
     * @param chenscore the Chen score
     * @return the max amount of chips that we're willing to bet    
     */
    public double get_cs_tol(int chenscore)
    {
        return cs_tolerance[chenscore+1];
    }     
    
    /**
     *   
     * This method is the original version of get_cs_tol()
     * returns the max that we're willing to bet
     * TODO: this should consider the proportion of our money.. so if we were
     * playing games where each player starts with 1 million, we won't always
     * fold when people bet in the hundreds 
     * this function is currently very rudimentery
     * 
     * @param chenscore the Chen score
     * @return the max amount of chips that we're willing to bet    
     */
    private static double calculate_cs_tol(int pocketscore)
    {
        if (pocketscore < 4)
        {
            return 0.;
        }
        //we can move these values out to become fields later as neccessary
        double m = 0.3; //coefficient
        double n = 1.34; //base of the exponent
        double c = 0.0; //vertical shift
        return (m * Math.pow(n, pocketscore) + c);
    }
        
    /**
     * Calculates the percentile rank of the current hand + board situation
     * Warning: this should only be used after the flop comes out
     * Update: This method is now only used after the river card comes out
     *
     * @param hand a long representing my hand
     * @param board a long representing the board
     * @return double between 0 and 1 representing % of hands could beat or tie
     */   
    public static double pr_perc_calc(long hand, long board)
    {                
	    int totalOthers = 0;
	    double notbigger = 0.d; //# of hands less than or equal to us
	    
	    Precog.getHighestHand(hand, board);
	    int myRating = hRating;       	     
	    
        long a = 0xFFFFFFFFFFFFFL ^ (hand | board);
        long b1, b2, c;                    
        int count = bitCount_dense(a);       
        for (int i = 0; i < count - 1; i++)
        {
            c = a ^= b1 = a & -a; // b1 is the lowest bit                                
            for (int j = i + 1; j < count; j++)
            {
                c ^= b2 = c & -c;
                totalOthers++;
                getHighestHand(b1 | b2, board);
                if (hRating >= myRating)
                    notbigger++;
            }
        }            
        
        return (notbigger/totalOthers);
    }      
        
    /**
     * Enumerates all possible 2 card combinations based on the taken cards
     * @param takenCards cards that cannot be part of the set
     * @return an array of longs representing all possible combinations
     */
    private static long[] enum_pos_opp_hands(long taken)
    {        	  
    	int idx = 0;
		long c1, c2, p1, p2;	  
		c1 = 0xFFFFFFFFFFFFFL ^ (taken);
		int count = bitCount_dense(c1);   
		long[] ret;
		switch (count)
		{
		  	case 47: 
		  		ret = new long[1081];
		  		break;
		  	case 46: 
		  		ret = new long[1035];
		  		break;
		  	default:
		  		ret = null;
		}
		for (int i = 0; i < count - 1; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
		    for (int j = i + 1; j < count; j++)
		    {
		    	c2 ^= p2 = c2 & -c2;
		    	ret[idx++] = p1 | p2;
		    }
		}
		return ret;	
    }
          	
    private static long[] enum_pos_river_cards(long taken)
    {
    	long[] ret = new long[46];
    	long remain = 0xFFFFFFFFFFFFFL ^ taken;
    	for (int i = 0; i < 46; i++)    	
    		remain ^= ret[i] = remain & -remain;
    	return ret;
    }
    
    /**
     * Calculates the average percentile cutoff after the flop, considering
     * potential hand combinations from turn and river
     * @param hand a long representing my hand
     * @param board a long representing the board after flop
     * @return average percentile
     */ 
  	public static double pf_avg_perc(long hand, long board)
    {		  
  		long[] oppHands = enum_pos_opp_hands(hand | board); 		  		  
	    double percentileSum = 0.d;
	    int percentileCount = 0;
	  
	    // Note: oppHands[i] == Turn and River
		//       oppHands[j] == opponent hands
				    
	    for (int i = 0; i < oppHands.length; i++)
	    {				    	
	    	getHighestHand(hand, board | oppHands[i]);
		    double myRating = hRating;
	    
	    	int total = 0;
		    double notBigger = 0.d;
		    
		    long tr = oppHands[i];
		    
	    	for (int j = 0; j < oppHands.length; j++)
	    	{ 		   				    				    				    				    				    
		        if ((tr & oppHands[j]) != 0L)
		        	continue;
			    total++;
			    getHighestHand(oppHands[j], tr | board);
			    if (hRating >= myRating)
			    	notBigger++;			    
	    	}	
	    	
		    percentileSum += notBigger / total;
		    percentileCount++;	    	
	    }

	    return (percentileSum / percentileCount);
    }
  	
  	//average percentile calculator
  	
  	/**
     * Calculates the average percentile cutoff after the flop, considering
     * potential hand combinations from turn and river
     * @param hand a long representing my hand
     * @param board a long representing the board after flop
     * @return average percentile
     */ 
  	public static double pf_avg_perc_multithread(long hand, long board, int numProcesses)
    {		  
  		long[] oppHands = enum_pos_opp_hands(hand | board); 		 
  		Thread[] t = new Thread[numProcesses];
  		Pf_Avg_Perc_Calc[] apc = new Pf_Avg_Perc_Calc[numProcesses];
  		
  		int chunk = 1081 / numProcesses;
  		chunk--;
  		int lastIdx = 0;
  		int finalIdx = numProcesses - 1;
  		int temp;
  		for (int i = 0; i < numProcesses; i++)
  		{
  			if (i == finalIdx)
  			{
  				apc[i] = new Pf_Avg_Perc_Calc(oppHands, lastIdx, 1080, hand, board);
  				t[i] = new Thread(apc[i]);
  				t[i].start();  				
  				break;
  			}
  			apc[i] = new Pf_Avg_Perc_Calc(oppHands, lastIdx, temp = (lastIdx + chunk), hand, board);
  			t[i] = new Thread(apc[i]);
  			t[i].start();
  			lastIdx = temp + 1;
  		}
  		
  		double percentileSum = 0.d;
	    int percentileCount = 0;
	    	    
	    try
	    {
	    	for (int i = 0; i < numProcesses; i++)
	    	{
	    		t[i].join();
	    		percentileSum += apc[i].getTotal();
	    		percentileCount += apc[i].getCount();
	    	}
	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }	    	    	    
	    
	    return (percentileSum / percentileCount);
    }
  	
  	public static double pt_avg_perc(long hand, long board)
  	{
  		long[] oppHands = enum_pos_opp_hands(hand | board); 		  		  
	    double percentileSum = 0.d;
	    int percentileCount = 0;
	    
	    long c, p;	  
	    c = 0xFFFFFFFFFFFFFL ^ (hand | board); 
	    for (int i = 0; i < 46; i++)
	    {			    		    	
	    	c ^= p = c & -c;
	    	long boardP = board | p;
	    	getHighestHand(hand, boardP);
		    double myRating = hRating;
	    
	    	int total = 0;
		    double notBigger = 0.d;
		    	    		    
	    	for (int j = 0; j < oppHands.length; j++)
	    	{ 		   				    				    				    				    				    
		        if ((p & oppHands[j]) != 0L)
		        	continue;
			    total++;
			    getHighestHand(oppHands[j], boardP);
			    if (hRating >= myRating)
			    	notBigger++;			    
	    	}	
	    	
		    percentileSum += notBigger / total;
		    percentileCount++;	    	
	    }

	    return (percentileSum / percentileCount);
  	}
  	
  	public static double pt_avg_perc_multithread(long hand, long board, int numProcesses)
    {		  
  		long[] oppHands = enum_pos_opp_hands(hand | board); 		 
  		long[] river_set = enum_pos_river_cards(hand | board);
  		
  		Thread[] t = new Thread[numProcesses];
  		Pt_Avg_Perc_Calc[] apc = new Pt_Avg_Perc_Calc[numProcesses];
  		
  		int chunk = 46 / numProcesses;
  		chunk--;
  		int lastIdx = 0;
  		int finalIdx = numProcesses - 1;
  		int temp;
  		for (int i = 0; i < numProcesses; i++)
  		{
  			if (i == finalIdx)
  			{
  				apc[i] = new Pt_Avg_Perc_Calc(oppHands, river_set, lastIdx, 45, hand, board);
  				t[i] = new Thread(apc[i]);
  				t[i].start();  				
  				break;
  			}
  			apc[i] = new Pt_Avg_Perc_Calc(oppHands, river_set, lastIdx, temp = (lastIdx + chunk), hand, board);
  			t[i] = new Thread(apc[i]);
  			t[i].start();
  			lastIdx = temp + 1;
  		}
  		
  		double percentileSum = 0.d;
	    int percentileCount = 0;
	    	    
	    try
	    {
	    	for (int i = 0; i < numProcesses; i++)
	    	{
	    		t[i].join();
	    		percentileSum += apc[i].getTotal();
	    		percentileCount += apc[i].getCount();
	    	}
	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }	    	    	    
	    
	    return (percentileSum / percentileCount);
    }

  	/**
     * This field contains the rating of the return value of
     * getHighestHand()
     */
    private static int hRating;
  	
    /**
	 * @param pHand Player's 2 card hand.
	 * @param bHand Board's 3-5 cards. must have 3-5 cards
	 * @return The strongest poker hand that can be made from pHand and bHand.
	 */
    public static Hand getHighestHand(Hand pHand, Hand bHand)
    {
        return new Hand(getHighestHand(pHand.getBitCards(), bHand.getBitCards()), 5, 5);
    }    
    
    /**
	 * @param pHand a long representing Player's 2 card hand.
	 * @param bHand a long representing Board's 3-5 cards. must have 3-5 cards
	 * @return a long representing strongest poker hand that can be made from pHand and bHand.
	 */
    public static long getHighestHand(long pHand, long bHand)
    {
        long bigHand = pHand | bHand;
        long[] combos;
        int count = bitCount(bigHand);
        switch (count)
        {
	        case 7:
	        	combos = getCombinations7(bigHand); break;
	        case 6: 
	        	combos = getCombinations6(bigHand); break;
	        default:
	        	return bigHand;
        }
        
        long highest = 0L;
        int highestRating = 0;
        for (long candidate : combos)
        {
            if (highest == 0L)
            {
                highest = candidate;
                highestRating = rate(candidate);
                continue;
            }
            
            int candidateRating;
            
            if ((candidateRating = rate(candidate)) < highestRating)
            {
                highest = candidate;
                highestRating = candidateRating;
            }
        }
        hRating = highestRating;
        return highest;
    }    
    
    /**
	 * @param bigHand a long with 6 bits set
	 * @return All combinations of 5 bits set out of bigHand
     */
    public static long[] getCombinations6(long bigHand)
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
    public static long[] getCombinations7(long bigHand)
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
    
    public static int bitCount(long l)
    {
        int c;
        for (c = 0; l != 0; c++)
            l &= l - 1;        
        return c;
    }
    
    public static int bitCount_dense(long n)   
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

    private static int rate(Hand h)
    {        
        return rate(h.getBitCards());
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
     * @param h a long representing the hand to rate.
     * @return rating - the lower the stronger
     */ 
    public static int rate(long h)
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
        
    /**
     * Tests if the hand has a flush
     * @param h a long representing the hand to test
     * @return true if the hand has a flush
     */
    private static boolean hasFlush(long h)
    {
        if ((h | Card.SPADES_MASK) == Card.SPADES_MASK) return true;
        if ((h | Card.CLUBS_MASK) == Card.CLUBS_MASK) return true;
        if ((h | Card.DIAMONDS_MASK) == Card.DIAMONDS_MASK) return true;
        if ((h | Card.HEARTS_MASK) == Card.HEARTS_MASK) return true;
        return false;
    }
    
    /**
     * perfect hash for cactus kev's rate algorithm
     * @param u
     * @return
     */
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
    
    /**
     * 
     * @param h a long with exactly five bits set
     * @return
     */
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
    
    /**
     * Chen Formula: devised by Poker Champion William Chen
     * Used for scoring Pocket cards
     * Approximately 5x faster than scorePocket_original
     * @param h the 2-card hand to score
     * @return the Chen score
     */
    public static int scorePocket(Hand h) 
    {
        int indexLow;
        long l;
        long c = h.getBitCards();        
        c ^= l = c & -c;               
        return chen_scores[indexLow = bitpos64[(int)((l*0x07EDD5E59A4E28C2L)>>>58)]][bitpos64[(int)((c*0x07EDD5E59A4E28C2L)>>>58)] - indexLow - 1];
    }
    
    /*************************************************************************
     * 
     * NEURAL NETWORK
     * 
     *************************************************************************/
   
    //initialize neural net perceptrons
    private void initialize_nn(String filename, short[] array) throws IOException
    {
        //BufferedReader f = getBR(filename);
    	BufferedReader f = getBR("precog_weights.nnw"); //neural net weights
    	StringTokenizer st = new StringTokenizer(f.readLine());
    	//set weights...
    	Double.parseDouble(st.nextToken());
    }
    
    //needs work. overwrite or adding?
    private void saveWeights() throws IOException
    {
    	PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("precog_weights.nnw")));
    	//write weights..
	    out.println();
	    out.close();
    }
    
    
    
    private class Perceptron
    {
    	public static final int INITIAL_CAPACITY = 10; //default begin size of arraylist
    	private ArrayList<Perceptron> successors;
    	private ArrayList<Double> weights;
    	private double bias;
    	private double curVal;
    	private double threshold;
    	//may need to know parent
    	
    	public Perceptron()
    	{
    		successors = new ArrayList<Perceptron>(INITIAL_CAPACITY);
    		weights = new ArrayList<Double>(INITIAL_CAPACITY);
    		curVal = 0.;
    		threshold = 0.;
    		bias = 0.;
    	}
    	
    	//must add to same index
    	public void addChild(Perceptron p, double _weight)
    	{
    		successors.add(p);
    		weights.add(_weight);
    	}
    	
    	public void setWeight(Perceptron p, double _weight)
    	{
    		weights.set(successors.indexOf(p), _weight);
    	}
    	
    	public ArrayList<Perceptron> getSuccessors()
    	{
    		return successors;
    	}
    	
    	public double getWeight(Perceptron successor)
    	{
    		return weights.get(successors.indexOf(successor));
    	}
    	
    	public void evaluate()
    	{
    		if (curVal >= threshold) fire();
    	}
    	
    	public void fire()
    	{
    		
    	}
    	
    	public void receive(double val)
    	{
    		curVal += val;
    	}
    	
    	public void setThreshold(double val)
    	{
    		threshold = val;
    	}
    	
    	public void setBias(double val)
    	{
    		bias = val;
    	}
    	
    	public double getCurVal()
    	{
    		return curVal;
    	}
    	
    	public void reset()
    	{
    		curVal = 0.;
    	}
    }
    
    
    
       
}
