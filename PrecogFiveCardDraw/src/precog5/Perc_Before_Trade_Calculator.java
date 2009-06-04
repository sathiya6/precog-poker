package precog5;

public class Perc_Before_Trade_Calculator implements Runnable 
{
	// Data needed for calculations
	private long[] pos_opp_hands;
	private int start_idx;
	private int end_idx;
	private int my_rating;
	// Results
	private int not_bigger;
	
	
	public Perc_Before_Trade_Calculator(long[] pos_opp_hands, int start_idx, int end_idx, int my_rating)
	{
		this.pos_opp_hands = pos_opp_hands;
		this.start_idx = start_idx;
		this.end_idx = end_idx;
		this.my_rating = my_rating;
		not_bigger = 0;
	}
	
	@Override
	public void run() 
	{
		for (int i = start_idx; i <= end_idx; i++)
		{
			if (Precog.rate(pos_opp_hands[i]) >= my_rating)
				not_bigger++;
		}		
	}
	
	public int get_not_bigger()
	{
		return not_bigger;
	}

}
