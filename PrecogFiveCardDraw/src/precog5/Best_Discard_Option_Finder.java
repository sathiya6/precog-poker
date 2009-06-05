package precog5;

public class Best_Discard_Option_Finder implements Runnable 
{
	private double best_chance;
	private long best_discard;
	private long[] pos_discards;
	private long hand;
	
	Best_Discard_Option_Finder(long[] pos_discards, long hand) 
	{
		this.pos_discards = pos_discards;
		this.hand = hand;
		best_chance = 0;
		best_discard = 0;
	}
	
	double get_best_chance()
	{
		return best_chance;
	}
	
	long get_best_discard()
	{
		return best_discard;
	}
	
	@Override
	public void run()
	{
		// we search through the first 3 branches of discard-4's 
		for (int i = 0; i < 3; i++)
		{
			double chance = Precog.chance_of_getting_better_hand(hand, pos_discards[i], 4);
			if (chance > best_chance)
			{
				best_chance = chance;
				best_discard = pos_discards[i];
			}
		}
	}

}
