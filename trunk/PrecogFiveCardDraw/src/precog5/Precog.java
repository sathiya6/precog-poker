package precog5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import Poker.*;

public class Precog implements Player
{
	/*
	 * Uses 52 bits of the 64-bit long to represent all 52 cards.
	 * The right-most 4 bits shall represent TWO, the 4 bits to the left shall 
	 * represent 3, and so on. Of the 4 bits, the order of suits shall be: 
	 * spades, hearts, diamonds, clubs.
	 */
	public static final long TWO_MASK =    0x000000000000FL;
	public static final long THREE_MASK =  0x00000000000F0L; 
	public static final long FOUR_MASK =   0x0000000000F00L;
	public static final long FIVE_MASK =   0x000000000F000L;
	public static final long SIX_MASK =    0x00000000F0000L;
	public static final long SEVEN_MASK =  0x0000000F00000L;
	public static final long EIGHT_MASK =  0x000000F000000L;
	public static final long NINE_MASK =   0x00000F0000000L;
	public static final long TEN_MASK =    0x0000F00000000L;
	public static final long JACK_MASK =   0x000F000000000L;
	public static final long QUEEN_MASK =  0x00F0000000000L;
	public static final long KING_MASK =   0x0F00000000000L;
	public static final long ACE_MASK =    0xF000000000000L;
	/*
	 * suit masks
	 */
	public static final long SPADES_MASK =   0x8888888888888L; // ...100010001000
	public static final long HEARTS_MASK =   0x4444444444444L; // ...010001000100
	public static final long DIAMONDS_MASK = 0x2222222222222L;
	public static final long CLUBS_MASK =    0x1111111111111L;
	
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
    
    public Precog()
    {      
    	initialize();         
        
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
    
    private void initialize()
    {
    	try
    	{
	    	populateArrayFromPCT("flushes.pct", flushes);         
	        populateArrayFromPCT("unique5.pct", unique5);        
	        //populateArrayFromPCT("products.pct", products);    
	        //populateArrayFromPCT("values.pct", values);        
	        populateArrayFromPCT("hash_values.pct", hash_values);
	        populateArrayFromPCT("hash_adjust.pct", hash_adjust);
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
    }
    
	/*
	 * Algorithm for Precog.pf_avg_perc:
	 * First, enumerate all possible 2 card combinations of remaining cards after flop
	 * Now, loop through the combinations:
	 * 1: combine hand and possible board to get best hand
	 * 2: Loop through remaining 2 card combinations and pretend they are opponent hands
	 * 3: compare
	 * 
	 * Basically, we are looking at our hand and board and saying, out of all remaining
	 * possible hands, how many are worse than ours. Now, to modify this for five card draw,
	 * we are given 5 cards, and so 52 - 5 = 47 remaining cards. 
	 * 47 choose 5 = 1533939 combinations. 
	 * that's a shitload.
	 * 
	 * However, not all hope is lost. If you look carefully at the pf_avg_perc function,
	 * it has a nested loop, each going through the entire opponent hands array, which 
	 * means it's 1081^2 = 1168561, which is over a million iterations anyways. 
	 */
	
	public static double percentile_before_trade(long hand)
	{
		// first, we enumerate possible opponent hands		
		long[] pos_opp_hands = enum_pos_opp_hands_before_trade(hand);		
		int myRating = rate(hand);		
		int notBigger = 0;
		
		for (long opp_hand : pos_opp_hands)
		{
			if (rate(opp_hand) >= myRating)
			{
				notBigger++;
			}
		}
		
		return (double)notBigger / 1533939;
	}
	
	public static double percentile_after_trade(long hand, long taken, int num_traded)
	{
		long[] pos_opp_hands;
		switch(num_traded)
		{
		case 1:
			pos_opp_hands = enum_pos_opp_hands_1_traded_2_threads(taken);
			break;
		case 2: 
			pos_opp_hands = enum_pos_opp_hands_2_traded_2_threads(taken);
			break;
		case 3:
			pos_opp_hands = enum_pos_opp_hands_3_traded_2_threads(taken);
			break;
		case 4:
			pos_opp_hands = enum_pos_opp_hands_4_traded_2_threads(taken);
			break;
		default:
			return -1.d;
		}
		
		int myRating = rate(hand);		
		int notBigger = 0;
		
		for (long opp_hand : pos_opp_hands)
		{
			if (rate(opp_hand) >= myRating)
			{
				notBigger++;
			}
		}
		
		return (double)notBigger / pos_opp_hands.length;
	}
	
	public static double percentile_before_trade_multithread(long hand, int num_processes)
	{
		long[] pos_opp_hands = enum_pos_opp_hands_before_trade_2_threads(hand);
		int myRating = rate(hand);
		Thread[] threads = new Thread[num_processes];
		Percentile_Calculator[] pbtcs = new Percentile_Calculator[num_processes];
				
		int chunk = 1533939 / num_processes;
		chunk--;
		int last_idx = 0;
		int temp;
		int final_idx = num_processes - 1;
		
		for (int i = 0; i < final_idx; i++)
		{
			pbtcs[i] = new Percentile_Calculator(pos_opp_hands, last_idx, temp = (last_idx + chunk), myRating);
			threads[i] = new Thread(pbtcs[i]);
			threads[i].start();
			last_idx = temp + 1;
		}
		pbtcs[final_idx] = new Percentile_Calculator(pos_opp_hands, last_idx, 1533938, myRating);		
		threads[final_idx] = new Thread(pbtcs[final_idx]);
		threads[final_idx].start();
		
		int not_bigger = 0;
		
		try
		{
			for (int i = 0; i < num_processes; i++)
			{
				threads[i].join();
				not_bigger += pbtcs[i].get_not_bigger();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return (double) not_bigger / 1533939;
	}
	
	public static double percentile_after_trade_multithreaded(long hand, long taken, int num_traded, int num_processes)
	{
		long[] pos_opp_hands;
		switch(num_traded)
		{
		case 1:
			pos_opp_hands = enum_pos_opp_hands_1_traded_2_threads(taken);
			break;
		case 2: 
			pos_opp_hands = enum_pos_opp_hands_2_traded_2_threads(taken);
			break;
		case 3:
			pos_opp_hands = enum_pos_opp_hands_3_traded_2_threads(taken);
			break;
		case 4:
			pos_opp_hands = enum_pos_opp_hands_4_traded_2_threads(taken);
			break;
		default:
			return -1.d;
		}
		
		int myRating = rate(hand);
		Thread[] threads = new Thread[num_processes];
		Percentile_Calculator[] pbtcs = new Percentile_Calculator[num_processes];
		
		int chunk = pos_opp_hands.length / num_processes;
		chunk--;
		int last_idx = 0;
		int temp;
		int final_idx = num_processes - 1;
		
		for (int i = 0; i < final_idx; i++)
		{
			pbtcs[i] = new Percentile_Calculator(pos_opp_hands, last_idx, temp = (last_idx + chunk), myRating);
			threads[i] = new Thread(pbtcs[i]);
			threads[i].start();
			last_idx = temp + 1;
		}
		pbtcs[final_idx] = new Percentile_Calculator(pos_opp_hands, last_idx, pos_opp_hands.length - 1, myRating);		
		threads[final_idx] = new Thread(pbtcs[final_idx]);
		threads[final_idx].start();
		
		int not_bigger = 0;
		
		try
		{
			for (int i = 0; i < num_processes; i++)
			{
				threads[i].join();
				not_bigger += pbtcs[i].get_not_bigger();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return (double) not_bigger / pos_opp_hands.length;
	}
	
	public static long[] enum_pos_opp_hands_before_trade(long taken)
	{
		// 47 choose 5 = 1533939
		long[] ret = new long[1533939];
		int index = 0;
		
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		
		c1 = 0xFFFFFFFFFFFFFL ^ taken;
		
		for (int i = 0; i < 43; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 44; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 45; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 46; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 47; m++)
						{
							c5 ^= p5 = c5 & -c5;
							ret[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static long[] enum_pos_opp_hands_1_traded(long taken)
	{
		// taken = 6 cards
		// 46 choose 5 = 1370754
		long[] ret = new long[1370754];
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		
		c1 = 0xFFFFFFFFFFFFFL ^ taken;
		
		for (int i = 0; i < 42; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 43; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 44; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 45; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 46; m++)
						{
							c5 ^= p5 = c5 & -c5;
							ret[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static long[] enum_pos_opp_hands_2_traded(long taken)
	{
		// taken = 7 cards
		// 45 choose 5 = 1221759
		long[] ret = new long[1221759];
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		
		c1 = 0xFFFFFFFFFFFFFL ^ taken;
		
		for (int i = 0; i < 41; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 42; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 43; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 44; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 45; m++)
						{
							c5 ^= p5 = c5 & -c5;
							ret[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static long[] enum_pos_opp_hands_3_traded(long taken)
	{
		// taken = 8 cards
		// 44 choose 5 = 1086008
		long[] ret = new long[1086008];
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		
		c1 = 0xFFFFFFFFFFFFFL ^ taken;
		
		for (int i = 0; i < 40; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 41; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 42; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 43; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 44; m++)
						{
							c5 ^= p5 = c5 & -c5;
							ret[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static long[] enum_pos_opp_hands_4_traded(long taken)
	{
		// taken = 9 cards
		// 43 choose 5 = 962598
		long[] ret = new long[962598];
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		
		c1 = 0xFFFFFFFFFFFFFL ^ taken;
		
		for (int i = 0; i < 39; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 40; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 41; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 42; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 43; m++)
						{
							c5 ^= p5 = c5 & -c5;
							ret[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * 47 choose 5 = 1533939
	 * 41 choose 5 = 749398
	 * Therefore, 41 choose 5 is approximately half of 41 choose 5.
	 * Therefore, we can multithread this by finding the 7th bit, 
	 * erase the first 6th bits in the process,
	 * and pass the result into a runnable to find half of the combinations.
	 * The other half can be found by using the normal enum_pos_opp_hands_before_trade
	 * until we reach the 7th bit in the outmost for loop   
	 * 
	 * @param taken the bits discluded from possible opponent combinations
	 * @return
	 */
	public static long[] enum_pos_opp_hands_before_trade_2_threads(long taken)
	{
		// initialize the return array
		long[] pos_opp_hands = new long[1533939];
		// first we find the pool of possible cards that could be our opponents 
		long pool = 0xFFFFFFFFFFFFFL ^ taken;
		
		// now, we erase the first 6 bits of the pool
		long pool_clone = pool;
		for (int i = 0; i < 6; i++)
		{
			// this operation erases the right-most bit of pool_clone 
			pool_clone &= (pool_clone - 1);
		}
				
		// now, we pass the pool_clone to a separate thread to calculate
		// meanwhile, we'll pass our return array to the thread to populate
		// WARNING: this could be potentially disastrous, since both threads are
		// accessing the same array at the same time, but our algorithm should
		// have 0 collisions, since the 2 threads are working on separate halves. 
		// This way, we can avoid the extra System.arraycopy()
		Enum_Pos_Opp_Hands_47_41 calc_41bit = new Enum_Pos_Opp_Hands_47_41(pool_clone, pos_opp_hands);
		Thread thread = new Thread(calc_41bit);
		thread.start();
		
		// while we're waiting, let's calculate the other half				
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		// our outermost loop should go up till the 7th bit, which has an index of 6
		for (int i = 0; i < 6; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 44; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 45; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 46; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 47; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
				
		// now that we're done, let's wait for the other thread to finish
		try
		{
			thread.join();			
		}
		catch(Exception e)
		{
			// Hopefully we won't get to this point
			e.printStackTrace();
		}
		
		// Yay, we're done!
		return pos_opp_hands;
	}
		
	public static long[] enum_pos_opp_hands_1_traded_2_threads(long taken)
	{
		long[] pos_opp_hands = new long[1370754];
		long pool = 0xFFFFFFFFFFFFFL ^ taken;
		// now, we erase the first 6 bits of the pool
		long pool_clone = pool;
		for (int i = 0; i < 6; i++)
		{
			// this operation erases the right-most bit of pool_clone 
			pool_clone &= (pool_clone - 1);
		}
		
		Enum_Pos_Opp_Hands_46_40 calc_40bit = new Enum_Pos_Opp_Hands_46_40(pool_clone, pos_opp_hands);
		Thread thread = new Thread(calc_40bit);
		thread.start();
		
		// while we're waiting, let's calculate the other half				
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		// our outermost loop should go up till the 7th bit, which has an index of 6
		for (int i = 0; i < 6; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 43; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 44; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 45; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 46; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
				
		// now that we're done, let's wait for the other thread to finish
		try
		{
			thread.join();			
		}
		catch(Exception e)
		{
			// Hopefully we won't get to this point
			e.printStackTrace();
		}
		
		// Yay, we're done!
		return pos_opp_hands;
	}
	
	public static long[] enum_pos_opp_hands_2_traded_2_threads(long taken)
	{
		long[] pos_opp_hands = new long[1221759];
		long pool = 0xFFFFFFFFFFFFFL ^ taken;
		// now, we erase the first 6 bits of the pool
		long pool_clone = pool;
		for (int i = 0; i < 6; i++)
		{
			// this operation erases the right-most bit of pool_clone 
			pool_clone &= (pool_clone - 1);
		}
		
		Enum_Pos_Opp_Hands_45_39 calc_39bit = new Enum_Pos_Opp_Hands_45_39(pool_clone, pos_opp_hands);
		Thread thread = new Thread(calc_39bit);
		thread.start();
		
		// while we're waiting, let's calculate the other half				
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		// our outermost loop should go up till the 7th bit, which has an index of 6
		for (int i = 0; i < 6; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 42; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 43; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 44; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 45; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
				
		// now that we're done, let's wait for the other thread to finish
		try
		{
			thread.join();			
		}
		catch(Exception e)
		{
			// Hopefully we won't get to this point
			e.printStackTrace();
		}
		
		// Yay, we're done!
		return pos_opp_hands;
	}
	
	public static long[] enum_pos_opp_hands_3_traded_2_threads(long taken)
	{
		long[] pos_opp_hands = new long[1086008];
		long pool = 0xFFFFFFFFFFFFFL ^ taken;
		// now, we erase the first 5 bits of the pool
		long pool_clone = pool;
		for (int i = 0; i < 5; i++)
		{
			// this operation erases the right-most bit of pool_clone 
			pool_clone &= (pool_clone - 1);
		}
		
		Enum_Pos_Opp_Hands_44_39 calc_39bit = new Enum_Pos_Opp_Hands_44_39(pool_clone, pos_opp_hands);
		Thread thread = new Thread(calc_39bit);
		thread.start();
		
		// while we're waiting, let's calculate the other half				
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		// our outermost loop should go up till the 6th bit, which has an index of 5
		for (int i = 0; i < 5; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 41; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 42; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 43; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 44; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
				
		// now that we're done, let's wait for the other thread to finish
		try
		{
			thread.join();			
		}
		catch(Exception e)
		{
			// Hopefully we won't get to this point
			e.printStackTrace();
		}
		
		// Yay, we're done!
		return pos_opp_hands;
	}
	
	public static long[] enum_pos_opp_hands_4_traded_2_threads(long taken)
	{
		long[] pos_opp_hands = new long[962598];
		long pool = 0xFFFFFFFFFFFFFL ^ taken;
		// now, we erase the first 5 bits of the pool
		long pool_clone = pool;
		for (int i = 0; i < 5; i++)
		{
			// this operation erases the right-most bit of pool_clone 
			pool_clone &= (pool_clone - 1);
		}
		
		Enum_Pos_Opp_Hands_43_38 calc_38bit = new Enum_Pos_Opp_Hands_43_38(pool_clone, pos_opp_hands);
		Thread thread = new Thread(calc_38bit);
		thread.start();
		
		// while we're waiting, let's calculate the other half				
		int index = 0;
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		// our outermost loop should go up till the 6th bit, which has an index of 5
		for (int i = 0; i < 5; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 40; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 41; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 42; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 43; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;
						}
					}
				}
			}
		}
				
		// now that we're done, let's wait for the other thread to finish
		try
		{
			thread.join();			
		}
		catch(Exception e)
		{
			// Hopefully we won't get to this point
			e.printStackTrace();
		}
		
		// Yay, we're done!
		return pos_opp_hands;
	}
		
	private static long[] enumerate_discards(int number_of_discards, long hand)
	{
		long[] ret;
		int index = 0;
		long c1, c2, p1, p2;
		
		switch(number_of_discards)
		{
		case 1:
			ret = new long[5];
			while (hand != 0)
			{
				hand ^= ret[index] = hand & -hand;
				index++;
			}
			break;
		case 2:
			ret = new long[10];
			
			for (int i = 0; i < 4; i++)
			{
				c2 = hand ^= p1 = hand & -hand;
				for (int j = i + 1; j < 5; j++)
				{
					c2 ^= p2 = c2 & -c2;
					ret[index++] = p1 | p2;
				}
			}
			break;
		case 3:
			ret = new long[10];
			c1 = hand;
			for (int i = 0; i < 4; i++)
			{
				c2 = c1 ^= p1 = c1 & -c1;
				for (int j = i + 1; j < 5; j++)
				{
					c2 ^= p2 = c2 & -c2;
					ret[index++] = hand ^(p1 | p2); // 3 discards is just the flip of 2 discards
				}
			}
			break;
		case 4:
			ret = new long[5];
			c1 = hand;
			while (c1 != 0)
			{
				ret[index++] = (hand ^ (p1 = c1 & -c1)) ;
				c1 ^= p1;
			}
			break;
		default:
			return null;
		}
		
		return ret;
	}
	
	static double chance_of_getting_better_hand(long current_hand, long discard, int number_of_discards)
	{
		long[] pos_returns = enumerate_discard_returns(current_hand, number_of_discards);
		long incomplete_hand = current_hand ^ discard;
		
		int myRating = rate(current_hand);
		
		int better_hands = 0;
		int total = 0;
		
		for (long pos_return : pos_returns)
		{
			total++;			
			int rating = rate(incomplete_hand | pos_return);
			if (rating < myRating)
			{
				better_hands++;
			}						
		}
				
		return (double)better_hands / total;
	}	
	
	/**
	 * the cached best chance of the best discard option
	 */
	private static double best_chance;
	
	public static long find_best_discard_option(long hand)
	{
		best_chance = -1d;
		long best_discard = 0;
		
		for (int num_discards = 1; num_discards < 5; num_discards++)
		{
			long[] pos_discards = enumerate_discards(num_discards, hand);
			
			for (long pos_discard : pos_discards)
			{
				double chance = chance_of_getting_better_hand(hand, pos_discard, num_discards);
				if (chance > best_chance)
				{
					best_chance = chance;
					best_discard = pos_discard;
				}
			}
		}
		//System.out.println("Chance for Better Hand: " + best_chance);
		
		return best_discard;
	}
	
	/**
	 * There are 4 types of discards: discard 1, discard 2, discard 3, and discard 4.
	 * We don't explore discard 5 because it's akin to giving up and will only be used in
	 * the most dire situations.
	 * 
	 * For each discard type, there are _combinations of possible discards 
	 * * combinations of possible returns_ 
	 * = # of nodes to explore.
	 * 
	 * Discard 1: 5 combinations of possible discards * (47 choose 1) possible returns = 235 nodes
	 * Discard 2: 10810 nodes
	 * Discard 3: 162150 nodes
	 * Sum of Discard 1, 2, and 3 = 173195 nodes
	 * Discard 4: 178365 * 5 nodes
	 * 
	 * Since sum of 1 through 3 is approximately equivalent to one branch of 4, we can divide 
	 * discard 1-3 + 2 branches of discard 4 into one thread, and the other 3 branches
	 * of discard 4 into the second thread.
	 * 
	 * @param hand
	 * @return
	 */
	public static long find_best_discard_option_2_threads(long hand)
	{
		// first we enumerate all possible discard-4's (there are 5 combinations)
		long[] pos_discards_4 = enumerate_discards(4, hand);
		
		// now, we pass this array to the Best_Discard_Option_Finder thread, which will
		// search through the first three branches of the array.
		Best_Discard_Option_Finder finder = new Best_Discard_Option_Finder(pos_discards_4, hand);
		Thread thread = new Thread(finder);
		thread.start();
		
		// while we're waiting, we can calculate the other 2 branches as well as discards 1 - 3.
		double best_chance = 0;
		long best_discard = 0;
		
		// first start with the last 2 branches of discard-4's
		for (int i = 3; i < 5; i++)
		{
			double chance = chance_of_getting_better_hand(hand, pos_discards_4[i], 4);
			if (chance > best_chance)
			{
				best_chance = chance;
				best_discard = pos_discards_4[i];
			}
		}
		
		// now calculate discards 1 - 3
		for (int num_discards = 1; num_discards < 4; num_discards++)
		{
			long[] pos_discards = enumerate_discards(num_discards, hand);
			
			for (long pos_discard : pos_discards)
			{
				double chance = chance_of_getting_better_hand(hand, pos_discard, num_discards);
				if (chance > best_chance)
				{
					best_chance = chance;
					best_discard = pos_discard;
				}
			}
		}
		
		// now we get our results from the other thread
		try
		{
			thread.join();
			double finder_best_chance = finder.get_best_chance();
			if (finder_best_chance > best_chance)
			{
				best_chance = finder_best_chance;
				best_discard = finder.get_best_discard();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		Precog.best_chance = best_chance;
		
		return best_discard;
	}
	
	private static long[] enumerate_discard_returns(long taken, int num_discards)
	{
		long[] pos_returns;
		long remaining = 0xFFFFFFFFFFFFFL ^ taken;
		long c1, c2, c3, p1, p2, p3, p4;
		int index = 0;
		
		switch(num_discards)
		{
		case 1:
			pos_returns = new long[47];
			while (remaining != 0)
			{
				remaining ^= pos_returns[index] = remaining & -remaining;
				index++;
			}
			break;
		case 2:
			pos_returns = new long[1081];
			for (int i = 0; i < 46; i++)
			{
				c1 = remaining ^= p1 = remaining & -remaining;
				for (int j = i + 1; j < 47; j++)
				{
					c1 ^= p2 = c1 & -c1;
					pos_returns[index++] = p1 | p2;
				}
			}
			break;
		case 3:
			pos_returns = new long[16215];
			for (int i = 0; i < 45; i++)
			{
				c1 = remaining ^= p1 = remaining & -remaining;
				for (int j = i + 1; j < 46; j++)
				{					
					c2 = c1 ^= p2 = c1 & -c1;
					for (int k = j + 1; k < 47; k++)
					{
						c2 ^= p3 = c2 & -c2;
						pos_returns[index++] = p1 | p2 | p3;
					}
				}
			}
			break;
		case 4:
			pos_returns = new long[178365];
			for (int i = 0; i < 44; i++)
			{
				c1 = remaining ^= p1 = remaining & -remaining;
				for (int j = i + 1; j < 45; j++)
				{					
					c2 = c1 ^= p2 = c1 & -c1;
					for (int k = j + 1; k < 46; k++)
					{
						c3 = c2 ^= p3 = c2 & -c2;
						for (int l = k + 1; l < 47; l++)
						{
							c3 ^= p4 = c3 & -c3;
							pos_returns[index++] = p1 | p2 | p3 | p4;
						}
					}
				}
			}
			break;
		default:
			return null;
		}
		
		return pos_returns;
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
    static int rate(long h)
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
        if ((h | SPADES_MASK) == SPADES_MASK) return true;
        if ((h | CLUBS_MASK) == CLUBS_MASK) return true;
        if ((h | DIAMONDS_MASK) == DIAMONDS_MASK) return true;
        if ((h | HEARTS_MASK) == HEARTS_MASK) return true;
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

    public static long convert_card_array_to_long(Card[] cards)
    {
    	long ret = 0;
    	for (Card c : cards)
    	{
    		ret |= get_suit_mask(c.getSuit()) & get_value_mask(c.getRank());
    	}
    	return ret;
    }
    
    public static Card[] convert_long_to_card_array(long cards)
    {
    	Card[] ret;
    	
    	int count = 0;
    	long clone = cards;
    	while (clone != 0)
    	{
    		clone ^= clone & -clone;
    		count++;
    	}
    	
    	ret = new Card[count];
    	int index = 0;
    	
    	while (cards != 0)
    	{
    		long card = cards & -cards;
    		ret[index++] = new Card(getSuit(card), getNumber(card));
    		cards ^= card;
    	}
    	
    	return ret;
    }
    
    private static int getNumber(long v)
	{
		if ((v & TWO_MASK) != 0L) return 0;
		if ((v & THREE_MASK) != 0L) return 1;
		if ((v & FOUR_MASK) != 0L) return 2;
		if ((v & FIVE_MASK) != 0L) return 3;
		if ((v & SIX_MASK) != 0L) return 4;
		if ((v & SEVEN_MASK) != 0L) return 5;
		if ((v & EIGHT_MASK) != 0L) return 6;
		if ((v & NINE_MASK) != 0L) return 7;
		if ((v & TEN_MASK) != 0L) return 8;
		if ((v & JACK_MASK) != 0L) return 9;
		if ((v & QUEEN_MASK) != 0L) return 10;
		if ((v & KING_MASK) != 0L) return 11;
		return 12;
	}
    
    private static int getSuit(long v)
	{
		if ((v & SPADES_MASK) != 0L) return 3;
		if ((v & HEARTS_MASK) != 0L) return 2;
		if ((v & DIAMONDS_MASK) != 0L) return 1;
		return 0;
	}
    
    private static long get_suit_mask(int s)
	{
		switch (s)
		{
		case 0: return CLUBS_MASK;
		case 1: return DIAMONDS_MASK;
		case 2: return HEARTS_MASK;
		case 3:	return SPADES_MASK;
		}
		return 0;
	}
    
    private static long get_value_mask(int value)
    {
		switch (value)
		{
		case 0: return TWO_MASK;
		case 1: return THREE_MASK;
		case 2: return FOUR_MASK;
		case 3: return FIVE_MASK;
		case 4: return SIX_MASK;
		case 5: return SEVEN_MASK;
		case 6: return EIGHT_MASK;
		case 7: return NINE_MASK;
		case 8: return TEN_MASK;
		case 9: return JACK_MASK;
		case 10: return QUEEN_MASK;
		case 11: return KING_MASK;
		case 12: return ACE_MASK;
		}
		return 0; // This point should never be reached.
    }
    
    /*
     * Members relevant to interface methods are kept here
     */
    private static final boolean MULTITHREADED = true;
    private PlayerStats[] stats;
    private int myIndex;
    private double expected_percentile_cutoff;
    private Card[] initial_hand;
    private long initial_hand_long;
    private double initial_percentile;
    private int dealIndex;
    private long best_discard_option;
    private long discarded_cards;
    private int discarded_cards_num;
    private Card[] final_hand;
    private long final_hand_long;
    private double final_percentile;
    private boolean draw;
    
    
    private static double expected_percentile_cutoff(int numPlayers)
    {
    	// This is the minimum cutoff
    	double minimum_cutoff = 1.d - (1.d / numPlayers);
    	// we wanna play it safe and raise our cutoff to somewhere inbetween minimum and maximum, let's say
    	// midpoint
    	return minimum_cutoff;
    	//return minimum_cutoff + ((1.d - minimum_cutoff) / 2);
    }
    
	@Override
	public void deal(Card[] cards)
	{			
		dealIndex++;
		if (dealIndex == 1)
		{
			initial_hand = cards;
			initial_hand_long = convert_card_array_to_long(initial_hand);

			// let's calculate initial chances here
			if (MULTITHREADED)
			{
				initial_percentile = percentile_before_trade_multithread(initial_hand_long, 2);
			}
			else
			{
				initial_percentile = percentile_before_trade(initial_hand_long);
			}			
			
			// now let's calculate best discard option
			
			if (MULTITHREADED)
			{
				best_discard_option = find_best_discard_option_2_threads(initial_hand_long);
			}
			else
			{
				best_discard_option = find_best_discard_option(initial_hand_long);
			}
		}
		else if (dealIndex == 2)
		{
			final_hand = cards;
			final_hand_long = convert_card_array_to_long(final_hand);
			
			// let's calculate final chances here			
			if (MULTITHREADED)
			{
				if (discarded_cards != 0)
				{
					final_percentile = percentile_after_trade_multithreaded(final_hand_long, final_hand_long | discarded_cards, discarded_cards_num, 2);
				}
				else
				{
					final_percentile = initial_percentile;
				}
			}
			else
			{
				if (discarded_cards != 0)
				{
					final_percentile = percentile_after_trade(final_hand_long, final_hand_long | discarded_cards, discarded_cards_num);
				}
				else
				{
					final_percentile = initial_percentile;
				}
			}		
		}
	}

	@Override
	public Card[] draw(PlayerStats[] stats)
	{		
		if (draw)
		{
			discarded_cards = best_discard_option;
			Card[] discards = convert_long_to_card_array(discarded_cards);
			discarded_cards_num = discards.length;
			return discards;
		}
		return null;
	}

	static int max_investment;
	// we set the ideal diffP amount to achieve max_investment here
	static double ideal_diffP_percentage = 1.0d;
	// we set the ideal diffI amount to achieve max_investment here
	static double ideal_diffI = 0.5d;
	
	// we set the importance of diffP versus importance of diffI here	
	static double diffP_importance = 19.0d;
	static double diffI_importance = 1.0d;		
	static
	{				
		// diffP_importance + diffI_importantce = 1
		double importance_sum = diffP_importance + diffI_importance;
		diffP_importance /= importance_sum;
		diffI_importance /= importance_sum;		
	}
	
	static double diffP_weight;
	static double diffI_weight = (diffI_importance) / ideal_diffI;
		
	@Override
	public int getBid(PlayerStats[] stats, int callBid) 
	{
		if (dealIndex == 1)
		{						
			if (initial_percentile < expected_percentile_cutoff)
			{
				
				// This means our initial hand is poised to lose.
				// In this situation, it is too risky to raise. We should either call if we
				// are confident that we can improve our hand, or fold
				// If the difference in percentile is huge, we should fold
				// If we have a high chance of improving our hand, we should keep playing
				
				// diffP = difference in percentile, the smaller the better for us
				// 0 < diffP <= expected_percentile_cutoff
				
				// if the callBid is 0, meaning no one has bet yet, we check
				if (callBid == 0)
					return callBid;
				
				double diffP = expected_percentile_cutoff - initial_percentile;
				
				// if our chance of improving is > 50%, diffI is positive
				// The more positive diffI is, the better it is for us
				// -0.5 <= best_chance <= 0.5
				double diffI = best_chance - 0.5d;
				
				double diffP_weight = 2.0d;
				double diffI_weight = 1.0d;
				
				double eval_call_or_fold = (-diffP * diffP_weight) + (diffI * diffI_weight);
				
				if (eval_call_or_fold >= 0)
				{
					// we only bet if it's not too risky
					int call_bid_threshold = 1;
					if (callBid <= call_bid_threshold)
					{
						draw = true;
						return callBid;
					}
					else
					{
						return -1;
					}
				}
				else
				{
					return -1;
				}
			}
			else
			{
				// our hand is poised to at least break even
				// In this situation, we should either raise or call
				
				// the bigger the diffP, the better for us
				// 0 <= diffP <= 1.0 - expected_percentile_cutoff
				double diffP = initial_percentile - expected_percentile_cutoff;
				
				// The more positive diffI is, the better it is for us
				// -0.5 <= diffI <= 0.5
				double diffI = best_chance - 0.5d;
				
				max_investment = 10;
				
				if (diffI > 0.3d)
				{					
					double investment_percentage = (diffP * diffP_weight) + (diffI * diffI_weight);
					int investment = (int) investment_percentage * max_investment;
					
					// investment is for the entire game, so we must first consider how much we've already put in
					investment -= stats[myIndex].totalBid;
					if (investment >= callBid)
					{
						// we should raise. 
						draw = true;
						return investment;
					}
					else
					{
						// we fold. we NEVER violate our own threshold
						//return -1;
						draw = true;
						return callBid;
					}
				}
				else
				{										
					// we don't consider a potentially better hand
											
					double investment_percentage = (diffP * diffP_weight);
					int investment = (int) Math.round(investment_percentage * max_investment);
					
					// investment is for the entire game, so we must first consider how much we've already put in
					investment -= stats[myIndex].totalBid;
					if (investment >= callBid)
					{
						// we should raise. 						
						return investment;
					}
					else
					{
						// we fold. we NEVER violate our own threshold
						//return -1;
						return callBid;
					}
				}
			}
			
		}
		else if (dealIndex == 2)
		{
			// if no one bets, we check.
			if (callBid == 0)
				return callBid;
			
			if (final_percentile > expected_percentile_cutoff)
			{
				return callBid;
			}
			else
			{
				if (final_percentile < expected_percentile_cutoff)
					return -1;
				return callBid;
			}
		}
		
		return 0;
	}
	
	//@Override
	/*
	public int getBid_old(PlayerStats[] stats, int callBid) 
	{
		if (dealIndex == 1)
		{
						
			if (initial_percentile < expected_percentile_cutoff)
			{
				
				// This means our initial hand is poised to lose.
				// In this situation, it is too risky to raise. We should either call if we
				// are confident that we can improve our hand, or fold
				// If the difference in percentile is huge, we should fold
				// If we have a high chance of improving our hand, we should keep playing
				
				// diffP = difference in percentile, the smaller the better for us
				// 0 < diffP <= expected_percentile_cutoff
				
				// if the callBid is 0, meaning no one has bet yet, we check
				if (callBid == 0)
					return callBid;
				
				double diffP = expected_percentile_cutoff - initial_percentile;
				
				// if our chance of improving is > 50%, diffI is positive
				// The more positive diffI is, the better it is for us
				// -0.5 <= best_chance <= 0.5
				double diffI = best_chance - 0.5d;
				
				double diffP_weight = 6.0d;
				double diffI_weight = 1.0d;
				
				// The standard: if diffP = 0.05, and diffI = 0.3, we should bet.
				double eval_call_or_fold = (-diffP * diffP_weight) + (diffI * diffI_weight);
				
				if (eval_call_or_fold >= 0)
				{
					draw = true;
					return callBid;
				}
				else
				{
					return -1;
				}
			}
			else
			{
				// our hand is poised to at least break even
				// In this situation, we should either raise or call
				
				// the bigger the diffP, the better for us
				// 0 <= diffP <= 1.0 - expected_percentile_cutoff
				double diffP = initial_percentile - expected_percentile_cutoff;
				
				// The more positive diffI is, the better it is for us
				// -0.5 <= diffI <= 0.5
				double diffI = best_chance - 0.5d;
				
				int max_raise_amount = 3;
				
				if (diffI > 0.3d)
				{
					// we factor in the chance of improving our hand
					
					// the standard: a 70% of max diffP and a diffI of 0.4 should bet an eighth of our max_raise_amount
					
					// we set our percentage to raise here. 
					double raise_percentage = (1.0d / 2);
					// we set what percent of max diffP we want to achieve raise_percentage here
					double max_diffP_percentage = 0.99d;
					// we set the ideal diffI amount to achieve raise_percentage here
					double ideal_diffI = 0.4d;
					// we set the importance of diffP versus importance of diffI here
					// diffP_importance + diffI_importantce = 1
					double diffP_importance = 9.0d;
					double diffI_importance = 1.0d;
					
					double importance_sum = diffP_importance + diffI_importance;
					diffP_importance = (diffP_importance) / importance_sum;
					diffI_importance = (diffI_importance) / importance_sum;
					
					// now we calculate our weights based on set parameters above
					double diffP_weight = (diffP_importance * raise_percentage) / (max_diffP_percentage * (1 - expected_percentile_cutoff));
					double diffI_weight = (diffI_importance * raise_percentage) / ideal_diffI;
					
					
					double eval_raise_percentage = (diffP * diffP_weight) + (diffI * diffI_weight);
					
					draw = true;
					
					return (int)Math.round(eval_raise_percentage * (max_raise_amount)) + callBid;
				}
				else
				{
					// the standard: a diffP of 60% should bet an eighth of max_raise_amount
					
					// we set our percentage to raise here. 
					double raise_percentage = (1.0d / 12);
					// we set what percent of max diffP we want to achieve raise_percentage here
					double max_diffP_percentage = 0.99d;
					
					// we don't consider a potentially better hand
					double diffP_weight = (raise_percentage) / (max_diffP_percentage * (1 - expected_percentile_cutoff));
											
					double eval_raise_percentage = (diffP * diffP_weight);
					
					return (int)Math.round(eval_raise_percentage * max_raise_amount) + callBid;
				}
			}
			
		}
		else if (dealIndex == 2)
		{
			// if no one bets, we check.
			if (callBid == 0)
				return callBid;
			
			if (final_percentile > expected_percentile_cutoff)
			{
				return callBid;
			}
			else
			{
				if (final_percentile < expected_percentile_cutoff - 0.3d)
					return -1;
				return callBid;
			}
		}
		
		return 0;
	}
	*/
	
	@Override
	public void initHand(PlayerStats[] stats, int yourIndex)
	{
		this.stats = stats;
		myIndex = yourIndex;
		dealIndex = 0;
		expected_percentile_cutoff = expected_percentile_cutoff(stats.length);
		diffP_weight = (diffP_importance) / (ideal_diffP_percentage * (1 - expected_percentile_cutoff));		
		discarded_cards = 0;
		draw = false;
	}

	@Override
	public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex) 
	{
		
	}           
}
