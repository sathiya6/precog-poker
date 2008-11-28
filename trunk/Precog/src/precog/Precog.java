/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 *          Shawn Xu (talkingraisin@gmail.com)
 */

package precog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import poker.engine.*;
import poker.engine.GameState.State;
import poker.engine.Money.Currency;

public class Precog extends Player implements Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8284825252713953453L;
	private transient Hand myHand;
    private transient int myID;
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
    private transient double[] cs_tolerance = new double[22];
    
    private transient double pf_avg_perc_cache;
    private transient double pt_avg_perc_cache;
    private transient double pr_avg_perc_cache;
    
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
        try {
			initialize_nn();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }
    
    /****************************************************
     * 				Overridden Player Methods			*
     ****************************************************/

    public Action beginTurn(GameInfo gi)
    {
        /*switch (gi.getCurrentState())
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
        }*/
    	switch (gi.getCurrentState())
    	{
    	case FIRSTBET: return first_bet(gi);
    	case SECONDBET:
    	case THIRDBET:
    	case FINALBET:
    		return omni_bet(gi);
    	default:
    		throw new IllegalStateException("Precog beginTurn state is messed up");
    	}
    }

    public void beginRound(GameInfo gi)
    {       
    	myID = gi.getId(this);
    	pf_avg_perc_cache = -3.14;
    	pt_avg_perc_cache = -3.14;
    	pr_avg_perc_cache = -3.14;
    }

    public void endRound(GameInfo gi) 
    {
    	pf_avg_perc_cache = -3.14;
    	pt_avg_perc_cache = -3.14;
    	pr_avg_perc_cache = -3.14;
    	//change nn weights at this point
    	
    	try {
			save();
			System.out.println("save is successful");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} //save nn
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
    /*
    //after flop
    private Action second_bet(GameInfo gi)
    {
    	double p;
    	if (pf_avg_perc_cache < 0)
    	{
    		p = pf_avg_perc_cache = pf_avg_perc_multithread(myHand.getBitCards(), gi.getBoard().getBitCards(), THREADS);
    		System.out.println("pf avg perc: " + p);
    	}
    	else
    		p = pf_avg_perc_cache;
    	
   	   	double myStash = gi.getStash(myID).getAmount();
   	   	double mytol = (p - expectPC(gi.getNumberOfPlayersThatStartedThisRound())) * myStash;    	
    	
    	Action a;
		if (gi.isValid(a = new Check(myID))) // This means we are the first to raise (potentially)
		{
			// Remember we don't want to fold if we're the first to go, only later depending on the stake
			// But should we check or raise?
			if (mytol >= myStash)
			{ //TODO: the above will never eval to true, i think -kevin
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
    */
    
    //use for 2nd, 3rd, or 4th bets, not for pre-flop bet
    private Action omni_bet(GameInfo gi)
    {
    	double p;
    	switch (gi.getCurrentState())
    	{
    	case SECONDBET: 
    		if (pf_avg_perc_cache < 0)
    		{
    			p = pf_avg_perc_cache = pf_avg_perc_multithread(myHand.getBitCards(), gi.getBoard().getBitCards(), THREADS);
        		System.out.println("pf avg perc: " + p);
    		}
    		else p = pf_avg_perc_cache;
    		break;
    	case THIRDBET:
    		if (pt_avg_perc_cache < 0)
    		{
    			p = pt_avg_perc_cache = pt_avg_perc_multithread(myHand.getBitCards(), gi.getBoard().getBitCards(), THREADS);
        		System.out.println("pt avg perc: " + p);
    		}
    		else p = pt_avg_perc_cache;
    		break;
    	case FINALBET:
    		if (pr_avg_perc_cache < 0)
    		{
    			p = pr_avg_perc_cache = pr_perc_calc(myHand.getBitCards(), gi.getBoard().getBitCards());
        		System.out.println("pr avg perc: " + p);
    		}
    		else p = pr_avg_perc_cache;
    		break;
    	default: throw new IllegalStateException("state in omni_bet incorrect");
    	}
    	
    	double portion = nn.execute(p, gi.getBet(this).getAmount(), gi.getMinimumCallAmount().getAmount(),
    			gi.getNumberOfPlayersThatStartedThisRound(), gi.getNumberOfPlayers(), 
    			gi.getPotValue().getAmount());
    	if (portion < 0.)
    	{
    		return new Fold(myID);
    	}
    	if (portion == 0.)
    	{
    		Action a;
    		if (gi.isValid(a = new Check(myID)))
    			return a;
    		return new Call(myID);
    	}
    	return new Raise(myID, new Money(portion * (gi.getStash(myID).getAmount()
    			- gi.getMinimumCallAmount().getAmount()), Currency.DOLLARS));
    }
    
    private Action second_bet(GameInfo gi)
    {
    	double p;
    	if (pf_avg_perc_cache < 0)
    	{
    		p = pf_avg_perc_cache = pf_avg_perc_multithread(myHand.getBitCards(), gi.getBoard().getBitCards(), THREADS);
    		System.out.println("pf avg perc: " + p);
    	}
    	else
    		p = pf_avg_perc_cache;
    	
    	double portion = nn.execute(p, gi.getBet(this).getAmount(), gi.getMinimumCallAmount().getAmount(),
    			gi.getNumberOfPlayersThatStartedThisRound(), gi.getNumberOfPlayers(), 
    			gi.getPotValue().getAmount());
    	if (portion < 0.)
    	{
    		return new Fold(myID);
    	}
    	if (portion == 0.)
    	{
    		Action a;
    		if (gi.isValid(a = new Check(myID)))
    			return a;
    		return new Call(myID);
    	}
    	return new Raise(myID, new Money(portion * (gi.getStash(myID).getAmount() - 
    			gi.getMinimumCallAmount().getAmount()), Currency.DOLLARS));
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
     * getHighestHand(). it is accessed only in the single-threaded
     * avg perc calc methods in this class, so we don't have to worry
     * about synchronize/volatile
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
    
   private NeuralNet nn;
   
    //initialize neural net perceptrons
    private void initialize_nn() throws URISyntaxException
    {
		try 
		{
			FileInputStream fis = new FileInputStream("precog_weights.nnw");
			ObjectInputStream ois = new ObjectInputStream(fis);
			NeuralNet loadedNN = (NeuralNet)ois.readObject();
	    	nn = loadedNN;
	    	System.out.println("NN loaded from file");
		} catch (FileNotFoundException e)
		{
			nn = new NeuralNet();
			System.out.println("filenotfoundexception - creating new nn object");
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    }
    

    private void save() throws IOException, URISyntaxException
    {
    	FileOutputStream fos = new FileOutputStream("precog_weights.nnw");
    	ObjectOutputStream oos = new ObjectOutputStream(fos);
    	if (nn == null) throw new IllegalStateException("nn == null. crap!");
    	oos.writeObject(nn);
    	oos.close();
    }
    
    /**
     * 1. avg perc
     * 2. our money amount in
     * 3. raise amount (money we'd have to put in)
     * 4. number of starting players in round
     * 5. number of players left in round
     * 6. potsize
     * @author liuk
     *
     */
    private class NeuralNet implements Serializable
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = -2529332382699798332L;
		private Perceptron a1;
    	private Perceptron a2;
    	private Perceptron a3;
    	private Perceptron a4;
    	private Perceptron a5;
    	private Perceptron a6;
    	private Perceptron b1;
    	private Perceptron b2;
    	private Perceptron b3;
    	private Perceptron b4;
    	private Perceptron b5;
    	private Perceptron b6;
    	private Perceptron output;
    	//between 0 and 1 as a percentage of current amount chips owned
    	
    	public NeuralNet()
    	{
    		a1 = new Perceptron(); a1.randomize();
    		a2 = new Perceptron(); a2.randomize();
    		a3 = new Perceptron(); a3.randomize();
    		a4 = new Perceptron(); a4.randomize();
    		a5 = new Perceptron(); a5.randomize();
    		a6 = new Perceptron(); a6.randomize();
    		b1 = new Perceptron(); b1.randomize();
    		b2 = new Perceptron(); b2.randomize();
    		b3 = new Perceptron(); b3.randomize();
    		b4 = new Perceptron(); b4.randomize();
    		b5 = new Perceptron(); b5.randomize();
    		b6 = new Perceptron(); b6.randomize();
    		output = new Perceptron(); output.randomize();
    		
    		a1.addChild(b1, Math.random());
    		a1.addChild(b2, Math.random());
    		a1.addChild(b3, Math.random());
    		a1.addChild(b4, Math.random());
    		a1.addChild(b5, Math.random());
    		a1.addChild(b6, Math.random());
    		
    		a2.addChild(b1, Math.random());
    		a2.addChild(b2, Math.random());
    		a2.addChild(b3, Math.random());
    		a2.addChild(b4, Math.random());
    		a2.addChild(b5, Math.random());
    		a2.addChild(b6, Math.random());
    		
    		a3.addChild(b1, Math.random());
    		a3.addChild(b2, Math.random());
    		a3.addChild(b3, Math.random());
    		a3.addChild(b4, Math.random());
    		a3.addChild(b5, Math.random());
    		a3.addChild(b6, Math.random());
    		
    		a4.addChild(b1, Math.random());
    		a4.addChild(b2, Math.random());
    		a4.addChild(b3, Math.random());
    		a4.addChild(b4, Math.random());
    		a4.addChild(b5, Math.random());
    		a4.addChild(b6, Math.random());
    		
    		a5.addChild(b1, Math.random());
    		a5.addChild(b2, Math.random());
    		a5.addChild(b3, Math.random());
    		a5.addChild(b4, Math.random());
    		a5.addChild(b5, Math.random());
    		a5.addChild(b6, Math.random());
    		
    		a6.addChild(b1, Math.random());
    		a6.addChild(b2, Math.random());
    		a6.addChild(b3, Math.random());
    		a6.addChild(b4, Math.random());
    		a6.addChild(b5, Math.random());
    		a6.addChild(b6, Math.random());
    		
    		b1.addChild(output, Math.random());
    		b2.addChild(output, Math.random());
    		b3.addChild(output, Math.random());
    		b4.addChild(output, Math.random());
    		b5.addChild(output, Math.random());
    		b6.addChild(output, Math.random());
    	}
    	
    	/** 1. avg perc
        * 2. our money amount in
        * 3. raise amount (money we'd have to put in)
        * 4. number of starting players in round
        * 5. number of players left in round
        * 6. potsize
        * 
        * should return a double between 0 and 1. proportion of chips left to raise.
        * 0 means check/call, a negative number should mean to fold
        */
    	public double execute(double avg_perc, double chips_in, double raise_amt, 
    			int starting_players, int cur_players, double potsize)
    	{
    		resetAll();
    		a1.receive(avg_perc); a2.receive(chips_in); a3.receive(raise_amt);
    		a4.receive((double)starting_players); a5.receive((double)cur_players);
    		a6.receive(potsize);
    		evaluate(a1, a2, a3, a4, a5, a6);
    		evaluate(b1, b2, b3, b4, b5, b6);
    		return output.outputSmooth();
    		//return 0.;
    	}
    	
    	private void evaluate(Perceptron ... a)
    	{
    		for (Perceptron p : a) p.evaluate();
    	}
    	
    	//makes all curVals 0
    	private void resetAll()
    	{
    		a1.reset(); a2.reset();
    		a3.reset(); a4.reset();
    		a5.reset(); a6.reset();
    		b1.reset(); b2.reset();
    		b3.reset(); b4.reset();
    		b5.reset(); b6.reset(); 
    		output.reset();
    	}
    	
    	public void printWeights()
    	{
    		
    	}
    	
    	
    }
    
    private class Perceptron implements Serializable
    {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 3941480927560107580L;
		private static final int INITIAL_CAPACITY = 6; //default begin size of arraylist
    	//need learning rate when doing backprop
    	private ArrayList<Perceptron> successors;
    	private ArrayList<Double> weights;
    	private ArrayList<Perceptron> parents;
    	private ArrayList<Double> parent_weights;
    	private double bias;
    	private transient double curVal;
    	private double threshold;
    	
    	public Perceptron()
    	{
    		successors = new ArrayList<Perceptron>(INITIAL_CAPACITY);
    		weights = new ArrayList<Double>(INITIAL_CAPACITY);
    		parents = new ArrayList<Perceptron>(INITIAL_CAPACITY);
    		parent_weights = new ArrayList<Double>(INITIAL_CAPACITY);
    		curVal = 0.;
    		threshold = 0.;
    		bias = 0.;
    	}
    	
    	/**
    	 * when getting output from the output field of NeuralNet, use this function
    	 * to smooth the values (in the form of a sigmoid) so that the return value of
    	 * this function will be either negative, 0, or if positive, < 1.0
    	 * 
    	 * the graph of this function is a sigmoid with asymptotes y = +1 and y = -1
    	 * and f(0) = 0
    	 */
    	public double outputSmooth()
    	{
    		return (2. / (1. + Math.exp(-curVal))) - 1.;
    	}
    	
    	//must add to same index
    	public void addChild(Perceptron p, double _weight)
    	{
    		successors.add(p);
    		weights.add(_weight);
    		p.addParent(this, _weight);
    	}
    	
    	public void addParent(Perceptron p, double _weight)
    	{
    		parents.add(p);
    		parent_weights.add(_weight);
    	}
    	
    	public void setWeight(Perceptron p, double _weight)
    	{
    		weights.set(successors.indexOf(p), _weight);
    	}
    	
    	public ArrayList<Perceptron> getSuccessors()
    	{
    		return successors;
    	}
    	
    	public String formattedWeights()
    	{
    		return "implement this";
    	}
    	
    	public double getWeight(Perceptron successor)
    	{
    		return weights.get(successors.indexOf(successor));
    	}
    	
    	public void evaluate()
    	{
    		if (curVal + bias >= threshold) fire(); //sigmoid...
    	}
    	
    	public void fire()
    	{
    		for (Perceptron s : successors)
    		{ //multiply by curVal or 1?
    			s.receive(weights.get(successors.indexOf(s)));
    		}
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
    	
    	public void randomize()
    	{
    		bias = 0.;//Math.random(); for now, ignore bias to simplify life
    		threshold = Math.random();
    	}
    }
    
    
    
       
}
