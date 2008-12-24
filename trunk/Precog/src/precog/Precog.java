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
	private static final long serialVersionUID = 8284825252713953453L;
	private transient Hand myHand;
    private transient int myID;
    private static final int THREADS = 2;
    
    /**
     *  Perfect Hashtable for bitposition. This table returns the position of the bit set.
     *  Usage: bitpos64[(int)((bit*0x07EDD5E59A4E28C2L)>>>58)];
     */
    private static final int[] bitpos64 =
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
    private transient LinkedList<Double> portions_cache; //pertains to a single round
    
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
    	portions_cache = new LinkedList<Double>();
    }

    public void endRound(GameInfo gi) 
    {
    	//change nn weights at this point
    	/* mean squared error: sum of (Desired n - Output n)^2. in our case we only have one
    	 * output, so MSE = (desired portion - output portion)^2
    	 */
    	boolean iWon = false;
    	for (Integer i : gi.getWinnerIDs())
    	{
    		if (i == myID)
    		{
    			iWon = true; break;
    		}
    	}
    	if (!portions_cache.isEmpty())
    	{
    		double actual_out = portions_cache.getLast();
    		double desired_out;
    		if (iWon)
    		{
    			if (gi.getActivePlayerIds().size() > 1)
    				desired_out = (actual_out + 1.) / 2.;
    			else
    				desired_out = actual_out;
    		}
    		else
    		{ 
    			if (gi.getActivePlayerIds().contains(myID))
    				desired_out = (actual_out - 1.) / 2.;
    			else //lost from folding
    			{
    				double last_avg_perc = -0.2;
    				if (pf_avg_perc_cache > 0)
    					last_avg_perc = pf_avg_perc_cache;
    				if (pt_avg_perc_cache > 0)
    					last_avg_perc = pt_avg_perc_cache;
    				if (pr_avg_perc_cache > 0)
    					last_avg_perc = pr_avg_perc_cache;

    				if (last_avg_perc > expectPC(gi.getNumberOfPlayersThatStartedThisRound()))
    					desired_out = (actual_out + 1.) / 2.;
    				else
    					desired_out = actual_out;
    			}
    		}
    		if (actual_out != desired_out)
    		{
    			nn.adjustNN(actual_out, desired_out);
    			System.out.println("nn weights adjusted.");
    		}
    	}
    	
    	try {
			save();
			System.out.println("save is successful");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
    }

    public void acceptHand(Hand h) 
    {
        myHand = h;
        System.out.println(this.toString() + " says, my hand is: " + myHand.toString());
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
    
    /**
     * use for 2nd, 3rd, or 4th bets, not for pre-flop bet
     * TODO: fix so that if others raise, we don't keep raising. 
     */
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
    	portions_cache.add(portion);
    	if (portion < 0.)
    	{
    		return new Fold(myID);
    	}
    	if (portion < 0.02) //to prevent two precogs from infinite betting loop
    	{
    		Action a;
    		if (gi.isValid(a = new Check(myID)))
    			return a;
    		return new Call(myID);
    	}
    	double myStashAmt = gi.getStash(myID).getAmount();
    	double addtlAmt = (gi.getMinimumCallAmount().getAmount() - gi.getBet(this).getAmount());
    	if (addtlAmt < 1.) //prevent two precogs from infinite betting loop
    	{
    		Action a;
    		if (gi.isValid(a = new Check(myID)))
    			return a;
    		return new Call(myID);
    	}
    	if (myStashAmt <= addtlAmt)
    	{
    		Action a;
    		if (gi.isValid(a = new Check(myID)))
    			return a;
    		return new Call(myID);
    	}
    	return new Raise(myID, new Money(portion * (myStashAmt - addtlAmt), Currency.DOLLARS));
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

    /****************************************************
     * 				Utility Methods	   			        *
     ****************************************************/ 
  	
  	/**
     * This field contains the rating of the return value of
     * getHighestHand(). it is accessed only in the single-threaded
     * avg perc calc methods in this class, so we don't have to worry
     * about synchronize/volatile... unless two precogs are playing,
     * both using single-threaded avg perc calc. ugh.
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
     *                         NEURAL NETWORK
     *************************************************************************/
   
    private NeuralNet nn;
   
    //initialize neural net perceptrons
    private void initialize_nn() throws URISyntaxException
    {    	
		try 
		{
			String file_name;
	    	if ("precog-A".equals(this.toString()))
	    	{
	    		file_name = "precog_weights-A.nnw";
	    	}
	    	else file_name = "precog_weights-B.nnw";
			FileInputStream fis = new FileInputStream(file_name);//("precog_weights.nnw");
			ObjectInputStream ois = new ObjectInputStream(fis);
			NeuralNet loadedNN = (NeuralNet)ois.readObject();
	    	nn = loadedNN;
	    	nn.printWeights();
	    	System.out.println("NN loaded from file");
		} catch (FileNotFoundException e)
		{
			nn = new NeuralNet();
			System.out.println("filenotfoundexception - creating new nn object");
		} catch (IOException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    }
    

    private void save() throws IOException, URISyntaxException
    {
    	String file_name;
    	if ("precog-A".equals(this.toString()))
    	{
    		file_name = "precog_weights-A.nnw";
    	}
    	else file_name = "precog_weights-B.nnw";
    	FileOutputStream fos = new FileOutputStream(file_name);//("precog_weights.nnw");
    	ObjectOutputStream oos = new ObjectOutputStream(fos);
    	if (nn == null) throw new IllegalStateException("nn == null. crap!");
    	oos.writeObject(nn);
    	oos.close();
    }
    
}
