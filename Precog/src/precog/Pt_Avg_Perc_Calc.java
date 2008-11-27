package precog;

public class Pt_Avg_Perc_Calc implements Runnable
{
	private double total;
	private int count;
	private long[] pos_opp_hand_set;
	private long[] pos_river_set;
	private int start_idx;
	private int end_idx;
	private long hand;
	private long board;	
	
	public Pt_Avg_Perc_Calc(long[] _pos_opp_hand_set, long[] _pos_river_set, int _start_idx, int _end_idx, long _hand, long _board)
	{
		pos_opp_hand_set = _pos_opp_hand_set;
		pos_river_set = _pos_river_set;
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
		// TODO Auto-generated method stub
		for (int i = start_idx; i <= end_idx; i++)
		{
			long posBoard = board | pos_river_set[i];
			int myRating = Precog.rate(Precog.getHighestHand(hand, posBoard));
			
			int totalNum = 0;
		    double notBigger = 0.d;
		    		  
			for (int j = 0; j < pos_opp_hand_set.length; j++)
			{
				if ((pos_opp_hand_set[j] & posBoard) != 0)
					continue;
				
				totalNum++;
				
				if (Precog.rate(Precog.getHighestHand(pos_opp_hand_set[j], posBoard)) >= myRating)
			    	notBigger++;
			}
			
			total += notBigger / totalNum;
		    count++;	  
		}
	}
}
