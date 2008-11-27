package precog;

public class Pf_Avg_Perc_Calc implements Runnable
{
	private double total;
	private int count;
	private long[] pos_comb_set;
	private int start_idx;
	private int end_idx;
	private long hand;
	private long board;
	
	public Pf_Avg_Perc_Calc(long[] _pos_comb_set, int _start_idx, int _end_idx, long _hand, long _board)
	{
		pos_comb_set = _pos_comb_set;
		start_idx = _start_idx;
	    end_idx = _end_idx;
	    hand = _hand;
	    board = _board;
	    count = 0;
	    total = 0.d;
	}
	
	public double getTotal()
	{
		return total;
	}
	
	public int getCount()
	{
		return count;
	}
		
	@Override
	public void run()
	{		
		for (int i = start_idx; i <= end_idx ; i++)
	    {			    	
			long posBoard = board | pos_comb_set[i];
		    int myRating = Precog.rate(Precog.getHighestHand(hand, posBoard));
			//myRating used to be type double - strange. i changed to int.
			
	    	int totalNum = 0;
		    double notBigger = 0.d;
		    		    
	    	for (int j = 0; j < pos_comb_set.length; j++)
	    	{ 		   				    				    				    				    				    
		        if ((posBoard & pos_comb_set[j]) != 0L)
		        	continue;
			    totalNum++;
			    
			    if (Precog.rate(Precog.getHighestHand(pos_comb_set[j], posBoard)) >= myRating)
			    	notBigger++;			    
	    	}	
	    	
		    total += notBigger / totalNum;
		    count++;	    	
	    }			
	}
	
}