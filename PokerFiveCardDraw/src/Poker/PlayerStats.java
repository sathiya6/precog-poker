package Poker;

import java.util.LinkedList;

public class PlayerStats
{
    public String name;
    public int chips;
    public int totalBid;
    public int totalWon;
    public boolean folded;
    public boolean allIn;
    public int drew;
    public LinkedList<Integer> bids;
    
    public PlayerStats(int chips)
    {
        this.chips = chips;
        this.drew = -1;  // draw hasn't happened yet
        this.bids = new LinkedList<Integer>();
    }
    
    public PlayerStats(PlayerStats ps)
    {
        this.chips = ps.chips;
        this.totalBid = ps.totalBid;
        this.totalWon = ps.totalWon;
        this.folded = ps.folded;
        this.allIn = ps.allIn;
        this.drew = ps.drew;
        this.bids = new LinkedList<Integer>();
        bids.addAll(ps.bids);
        this.name = ps.toString(); 
    }

    public String toString()
    {
        return name;
    }
    
    int getBid(int round)
    {
        if (round == -1)
            round = Math.max(0, bids.size() - 1);  // last bid
        if (round >= bids.size())
        {
            if (folded)
                return -1;
            if (allIn)
                return 0;
            return -2;  // special code meaning no bid at all
        }
        return bids.get(round);
    }


}
