package ai2016.group5;

import java.util.List;
import negotiator.bidding.BidDetails;

public class StrategyModel {
	private Opponent[] allParty;
	private Opponent own;
	
	public StrategyModel(Opponent[] parties, Opponent own)
	{
		this.allParty = parties;
		this.own = own;
	}
	
	// Rate range 0-1, 1 = concedes to opponents bids, 0 = does not concede
	public Double getConcedingRate(int rounds)
	{
		Double[][] changes = new Double[this.allParty.length][];
		int i=0;
		for (Opponent p: this.allParty){
			changes[i] = this.getChangesInUtility(p,rounds);
			if (changes[i] == null){
				return null;
			}
			i++;
		}
		Double result = 0.0;
		Double utility;
		for (int j=0; j<rounds; j++){
			utility = 0.0;
			for (int k=0; k<this.allParty.length; k++){
				utility += changes[k][j];
			}
			result += utility/this.allParty.length; 
		}
		
		return result/rounds;
	}
	
	// Return an array which give the change in utility in the last consecutive $rounds
	public Double[] getChangesInUtility(Opponent model, int rounds)
	{
		List<BidDetails> bids =  model.bidHistory.getHistory();
		
		if (bids.size() < rounds) return null;
		
		bids = bids.subList(bids.size()-rounds-1, bids.size()-1);
		
		if (bids.size() < 2) return null;
		
		Double[] changeInUtility = new Double[bids.size()-1];
		Double previous = model.getUtility(bids.get(0).getBid());
		Double next;
		int i = 0;
		for(BidDetails bid : bids.subList(1, bids.size()-1))
		{
			next = model.getUtility(bid.getBid());
			changeInUtility[i] = next - previous;
			i++;
		}
		return changeInUtility;
	}
	

}
