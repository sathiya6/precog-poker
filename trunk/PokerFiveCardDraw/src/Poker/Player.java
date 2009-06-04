package Poker;

public interface Player
{
    /**
     * At the beginning of this hand, who are the other players, and how much money do they have?
     * Players are listed in the order they will bid.
     * @param players     names of all players
     * @param chips       how much they have to bid with (corresponds to players array)
     * @param yourIndex   the index of this player in those arrays
     */
    void initHand(PlayerStats[] stats, int yourIndex);
    
    /**
     * Tells you what cards you have, and whether they are face up or face down.
     * @param faceUp   an array of cards other players can see
     * @param faceDown an array of cards other players cannot see
     */
    void deal(Card[] cards);
    
    /**
     * Opportunity to give back some cards.
     * Afterwards, deal() will be called again with the full set of 5 cards you now have.
     * @return an array of the cards you want to give back.
     */
    Card[] draw(PlayerStats[] stats);
    
    /**
     * Asks for a bid.
     * @param allFaceUp       All face-up cards of all players
     * @param biddingHistory  The number of chips every player bid
     *                        where biddingHistory[0] is the bid of players[0].
     *                        biddingHistory[players.length] is the 2nd bid by player 0.
     *                        -1 indicates fold.  All non-negative numbers represent raises or calls (or checks)
     * @param callBid         The amount you must bid not to fold.
     * @return                Either callBid to call, or more than callBid to raise, or -1 to fold.
     */
    int getBid(PlayerStats[] stats, int callBid);
    
    /**
     * Report who won.  Show all the cards of anyone still in at the end.
     * @param allCards
     * @param biddingHistory
     * @param winnerIndex
     */
    void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex);
}
