package Poker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class SimplePoker
{
    class PlayerData extends PlayerStats implements Comparable<PlayerData>
    {
        Player player;
        Hand hand;
        
        PlayerData(Player p, int chips)
        {
            super(chips);
            this.player = p;
            sweep();
        }
        
        public String toString()
        {
            return player.toString();
        }
        
        void setHand(Hand h)
        {
            hand = h;
        }
        
        Card[] copyHand(int faceDown)
        {
            if (faceDown > 0)
            {
                if (faceDown > hand.numCards())
                    throw new IllegalArgumentException("There aren't " + faceDown + " cards in " + player + "'s hand!");
                return hand.copyCards(0, faceDown);
            }
            else
            {
                // negative number means that # from the end of the array
                return hand.copyCards(-faceDown, hand.numCards() + faceDown);
            }
        }
        
        void bid(int c)
        {
            if (c > chips)
                throw new IllegalArgumentException("Player " + player + " does not have " + c + " chips!  They have only " + chips + ".");
            totalBid += c;
            chips -= c;
            if (chips == 0)
                allIn = true;
        }

        void pay(int c)
        {
            if (c < 0)
                throw new IllegalArgumentException("Cannot pay negative chips");
            chips += c;
        }
        
        void sweep()
        {
            totalBid = 0;
            totalBid = 0;
            folded = allIn = false;
        }

        @Override
        public int compareTo(PlayerData ps)
        {
            // A folded hand is always worst
            if (ps.folded)
                return -1;
            // A better hand is always preffered
            int comp = this.hand.compareTo(ps.hand);
            if (comp != 0)
                return comp;
            // Among ties, handle the all-in hand first
            return ps.totalBid - this.totalBid;
        }
        
        boolean replaceCard(Card remove, Card add)
        {
        	return hand.replaceCard(remove, add);
        }
    }
    
    ArrayList<PlayerData> data = new ArrayList<PlayerData>();
    ArrayList<Player> allPlayers = new ArrayList<Player>();
    Deck deck = new Deck();
    int firstBidder;
    int currentBidder;
    int numPlayersIn;
    int currentBid;
    
    void addPlayer(Player p, int chips)
    {
        allPlayers.add(p);
        PlayerData pd = new PlayerData(p, chips);
        data.add(pd);
    }
    
    public int countPlayers()
    {
        return data.size();
    }
    
    public void playHand()
    {
        beforeHand();
        dealCards();
        bidding();
        resolveWinner();
        removeBankrupt();
    }
    
    void beforeHand()
    {
        int yourIndex = 0;
        for (PlayerData pd : data)
        {
            pd.sweep();
            // ante
            pd.bid(1);
        }
        for (PlayerData pd : data)
        {
            pd.player.initHand(getPlayerStats(), yourIndex++);
        }
    }

    public void endGame()
    {
        assert(data.size() == 1);
        System.out.println("We have a WINNER: " + data.get(0));
        for (Player p : allPlayers)
        {
            if (p instanceof HumanPlayer)
                p.initHand(getPlayerStats(), 0);
        }
        firstBidder++;
    }
    
    void dealCards()
    {
        deck.shuffle();
        
        // 5-card stud
        for (PlayerData pd : data)
        {
            Card[] cards = new Card[5];
            for (int i = 0; i < cards.length; i++)
            {
                cards[i] = deck.dealOne();
            }
            Hand hand = new Hand(cards);
            pd.setHand(hand);
            
            Card[] copy = pd.copyHand(5);
            pd.player.deal(copy);
        }
    }
    
    void bidding()
    {
        int maxBid = 0;  // max amount bid by any one player this round
        int pot = 0;
        int[] previousBids = new int[data.size()];
        for (int i = 0; i < data.size(); i++)
        {
            previousBids[i] = data.get(i).totalBid;
            pot += data.get(i).totalBid;
            data.get(i).totalBid = 0;
        }
        currentBidder = firstBidder % data.size();
        while (data.get(currentBidder).folded)
            currentBidder++;
        numPlayersIn = data.size();
        int consecutiveCalls = 0;
        while (numPlayersIn > 1 && consecutiveCalls < numPlayersIn)
        {
            PlayerData pd = data.get(currentBidder);
            currentBidder = (currentBidder + 1) % data.size();
            if (pd.folded)
                continue;
            if (pd.allIn)
            {
                pd.bids.add(0);
                System.out.println("Player " + pd + " is still all in");
                consecutiveCalls++;
            }
            else
            {
                int callBid = Math.min(pd.chips, maxBid - pd.totalBid);
                assert(callBid >= 0);
                int bid = pd.player.getBid(getPlayerStats(), callBid);
                
                if (bid >= 0 && bid < callBid)
                {
//                    throw new IllegalStateException("Insufficient bid!  Must bid at least " + callBid + " or fold (-1).");
                    System.out.println("Insufficient bid (" + bid + ") by " + pd + "!  Must bid at least " + callBid + " or fold (-1).");
                    bid = -1;
                }
                pd.bids.add(bid);
                if (bid < 0)
                {
                    pd.folded = true;
                    numPlayersIn--;
                    System.out.println("Player " + pd + " folds");
                }
                else if (bid == callBid)
                {
                    pot += bid;
                    pd.bid(bid);
                    consecutiveCalls++;
                    if (bid == 0)
                        System.out.println("Player " + pd + " checks (" + pd.totalBid + ")");
                    else
                        System.out.println("Player " + pd + " calls (+" + bid + " =" + pd.totalBid + ")");
                }
                else  // raise
                {
                    // I'm not going to require AIs to know when they've run out of chips
                    if (bid >= pd.chips)
                        bid = pd.chips;
                    pot += bid;
                    maxBid += bid - callBid;
                    pd.bid(bid);
                    consecutiveCalls = 1;
                    if (callBid == 0)
                        System.out.println("Player " + pd + " bids " + bid + "(=" + pd.totalBid + ")");
                    else
                        System.out.println("Player " + pd + " sees the " + callBid + " and raises " + (bid - callBid) + " more to " + pd.totalBid);
                }
                if (pd.allIn)
                    System.out.println("Player " + pd + " goes all in");
            }
        }
        System.out.println("Pot is " + pot + ".  " + numPlayersIn + " players are still in.");
        // Combine previous bid with bid from this round
        for (int i = 0; i < data.size(); i++)
            data.get(i).totalBid += previousBids[i];
    }
    
    void resolveWinner()
    {
        LinkedList<PlayerData> winners = new LinkedList<PlayerData>();

        Card[][] allCards = new Card[data.size()][];
        
        int totalPot = 0;
        int thisPot = 0;
        int iPlayer = -1;
        for (PlayerData pd : data)
        {
            iPlayer++;
            totalPot += pd.totalBid;
            if (pd.folded)
                continue;  // don't reveal folded hands
            pd.hand.evaluateAndSortHand();
            System.out.println(pd.player + ": " + pd.hand);
            
            allCards[iPlayer] = new Card[5];
            for (int i = 0; i < 5; i++)
                allCards[iPlayer][i] = pd.hand.getCard(i);
            
            if (winners.isEmpty())
            {
                winners.add(pd);
                thisPot = pd.totalBid;
            }
            else
            {
                int comp = winners.get(0).hand.compareTo(pd.hand);
                if (comp == 0)
                {
                    winners.add(pd);  // tie
                    thisPot = Math.min(thisPot, pd.totalBid);
                }
                else if (comp < 0)
                {
                    winners.clear();  // better than anything previous
                    winners.add(pd);
                    thisPot = pd.totalBid;
                }
            }
        }
        
        // Add up the pot for this win, given that there might be a secondary pot if the winner was all in
        int pot = 0;
        for (PlayerData pd : data)
        {
            int fromBid = Math.min(pd.totalBid, thisPot);
            pot += fromBid;
            pd.totalBid -= fromBid;
            
            int winnerIndex = winners.contains(pd) ? data.indexOf(pd) : data.indexOf(winners.get(0));
            pd.player.outcome(allCards, getPlayerStats(), winnerIndex);
        }
        totalPot -= pot;
        System.out.println("Total pot is " + totalPot);

        if (winners.size() > 1)
        {
            // If there is a tie, any fractions get either passed to a secondary pot, or eaten by the house
            if (totalPot > 0)
                totalPot += pot % winners.size();
            pot /= winners.size();
            System.out.println("There is a " + winners.size() + "-way tie!");
        }
        for (PlayerData pd : winners)
        {
            System.out.println(pd + " wins " + pot);
            pd.pay(pot);
            // Once this player has been paid, take them out of the algorithm
            if (pd.totalBid == 0)
                pd.folded = true;
        }
        
        // If there is a secondary pot, recurse to deal with it
        // Any hands that were all in and paid up have been removed at this point.
        if (totalPot > 0)
        {
            System.out.println("Resolving secondary pot...");
            resolveWinner();
        }
    }
    
    void removeBankrupt()
    {
        Iterator<PlayerData> iter = data.iterator();
        while (iter.hasNext())
        {
            if (iter.next().chips == 0)
                iter.remove();
        }
    }
    
    public String toString()
    {
        String str = "";
        for (PlayerData pd : data)
        {
            if (str.length() > 0)
                str += "\n";
            str += pd.player + "\t" + pd.chips;
        }
        return str;
    }
    
    PlayerStats[] getPlayerStats()
    {
        PlayerStats[] stats = new PlayerStats[data.size()];
        for (int i = 0; i < data.size(); i++)
            stats[i] = new PlayerStats(data.get(i));
        return stats;
    }
}
