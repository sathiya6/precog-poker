package precog5;

public class Enum_Pos_Opp_Hands_47_41 implements Runnable 
{
	private long pool;
	private long[] pos_opp_hands;
	/**
	 * Constructs a runnable calculator that finds possible 5-card opponent hands given a pool
	 * of candidates.
	 * 
	 * @param pool a 41-bit long that contains possible opponent cards
	 */
	Enum_Pos_Opp_Hands_47_41(long pool, long[] pos_opp_hands)
	{
		this.pool = pool;
		// 41 choose 5 = 749398;
		this.pos_opp_hands = pos_opp_hands;
	}
	
	long[] get_pos_opp_hands()
	{
		return pos_opp_hands;
	}
	
	@Override
	public void run() 
	{
		// since 1533939 - 749398 = 784541,
		// this index is the start of the second half, which is what we're working on
		int index = 784541;		
		long c1, c2, c3, c4, c5, p1, p2, p3, p4, p5;
		c1 = pool;
		
		for (int i = 0; i < 37; i++)
		{
			c2 = c1 ^= p1 = c1 & -c1;
			for (int j = i + 1; j < 38; j++)
			{
				c3 = c2 ^= p2 = c2 & -c2;
				for (int k = j + 1; k < 39; k++)
				{
					c4 = c3 ^= p3 = c3 & -c3;
					for (int l = k + 1; l < 40; l++)
					{
						c5 = c4 ^= p4 = c4 & -c4;
						for (int m = l + 1; m < 41; m++)
						{
							c5 ^= p5 = c5 & -c5;
							pos_opp_hands[index++] = p1 | p2 | p3 | p4 | p5;							
						}
					}
				}
			}
		}
	}
}
