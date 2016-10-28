package ai2016.group5;

import java.util.Arrays;
import java.util.HashMap;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.EvaluatorDiscrete;

/**
 * Represents an opponent in the negotiation
 */
public class Opponent {
	// Bidding history of the opponent
	BidHistory bidHistory;
	// Number of issues in the domain
	Integer nrIssues;
	// IDs of the issues
	int[] issueIds;
	// Values of the issues
	EvaluatorDiscrete[] issues;

	/**
	 * Constructor
	 * @param exampleBid An example bid used to populate the issue space
	 */
	public Opponent(Bid exampleBid) {
		this.bidHistory = new BidHistory();
		this.nrIssues = exampleBid.getIssues().size();
		this.issueIds = new int[this.nrIssues];

		// Assign the issue IDs
		for (int i = 0; i < exampleBid.getIssues().size(); i++) {
			this.issueIds[i] = exampleBid.getIssues().get(i).getNumber();
		}

		// Create the evaluators for each issue
		this.issues = new EvaluatorDiscrete[this.nrIssues];
		for (int i = 0; i < this.nrIssues; i++) {
			this.issues[i] = new EvaluatorDiscrete();
		}
	}

	/**
	 * Add a bid to the opponent's bidding history
	 * @param bid The bid to add to the history
	 */
	public void addBid(Bid bid) {
		this.bidHistory.add(new BidDetails(bid, 0));
		this.setWeights();
	}

	/**
	 * Computes the estimated utility for the opponent
	 * for a given bid
	 * @param bid The bid of which we want to estimate the utility for the opponent
	 * @return estimated utility for the given bid
	 */
	public double getUtility(Bid bid) {
		double utility = 0.0;

		HashMap<Integer, Value> bidValues = bid.getValues();
		// Iterate over the issues
		for (int i = 0; i < this.nrIssues; i++) {
			// Get weight of current issue
			double weight = this.issues[i].getWeight();

			ValueDiscrete value = (ValueDiscrete) bidValues.get(this.issueIds[i]);

			if (((EvaluatorDiscrete) this.issues[i]).getValues().contains(value)) {
				utility += this.issues[i].getDoubleValue(value).doubleValue() * weight;
			}
		}
		return utility;
	}

	/**
	 * Set the weights of the issues and the values per issue
	 */
	public void setWeights() {
		this.setWeightsIssues();
		this.setWeightsIssueValues();
	}

	/**
	 * Set the weights for the values of each issues using frequency analysis
	 */
	private void setWeightsIssueValues() {
		// Iterate over the issues
		for (int i = 0; i < this.nrIssues; i++) {
			// The keys of the map are the possible values of the issues and the values of the map
			// are the times each value of the issue was used
			HashMap<ValueDiscrete, Double> values = new HashMap<ValueDiscrete, Double>();
			// Iterate over the bidding history
			for (int j = 0; j < this.bidHistory.size(); j++) {
				ValueDiscrete value = (ValueDiscrete) (this.bidHistory.getHistory().get(j).getBid()
						.getValue(this.issueIds[i]));
				if (values.containsKey(value)) {
					values.put(value, values.get(value) + 1);
				} else {
					values.put(value, 1.0);
				}
			}

			// Get the maximum number of times a value was used
			double max = 0.0;
			for (ValueDiscrete value : values.keySet()) {
				if (values.get(value) > max)
					max = values.get(value);
			}

			// Set the evaluation values of each issue as the number of times each value was used
			// divided by the maximum number of times a value was used (so that the max is 1)
			for (ValueDiscrete value : values.keySet()) {
				try {
					this.issues[i].setEvaluationDouble(value, values.get(value) / max);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Set the weights for each issue using frequency analysis
	 */
	private void setWeightsIssues() {
		int[] changesIssues = getChangesIssues();
		double[] weights = new double[this.nrIssues];
		double totalWeight = 0.0;

		// Iterate over all the issues
		for (int i = 0; i < this.nrIssues; i++) {
			weights[i] = 1.0 / this.nrIssues + (this.bidHistory.size() - changesIssues[i] - 1) / 10.0;

			// Keep the total weight to normalize
			totalWeight += weights[i];
		}

		// Normalize the weights of the issues
		for (int i = 0; i < this.nrIssues; i++) {
			this.issues[i].setWeight(weights[i] / totalWeight);
		}
	}
	
	/**
	 * Return an array which represents the frequency of change of each issue
	 * @return an array in which each elements represents the number of changes of that issue
	 */
	private int[] getChangesIssues()
	{
		return getChangesIssues(this.bidHistory.size());
	}

	/**
	 * Return an array which represents the frequency of change of each issue only considering the last $rounds rounds
	 * @return an array in which each elements represents the number of changes of that issue
	 */
	private int[] getChangesIssues(int rounds) {
		// In this array we store the number of times each issue has changed
		int[] changes = new int[this.nrIssues];
		
		// Iterate over all the issues
		for (int i = 0; i < this.nrIssues; i++) {
			Value oldValue = null;
			Value currentValue = null;
			int count = 0;
			
			// Iterate over the last $rounds bids from the bidding history
			for (int j=this.bidHistory.size()-1; j>this.bidHistory.size()-rounds-1; j--){
				currentValue = this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i]);

				// If it's not the first value and the current value is different from the previous one,
				// it means it has changed. We then increment the changes count.
				if (oldValue != null && !oldValue.equals(currentValue))  count++;

				oldValue = currentValue;
			}
			changes[i] = count;
		}
		return changes;
	}
	
	/**
	 * Return how hardHeaded the agent is. 1 = bids do not change in the last $rounds, 0 = bid change every time in the last $rounds
	 * @param only consider the last $rounds rounds
	 * @return range 0-1
	 */
	public Double hardHeaded(int rounds)
	{
		if (this.bidHistory.size() < rounds) return null;
		
		int[] changes = this.getChangesIssues(rounds);
		int sum = 0;
		for (int change: changes){
			sum += change;
		}
		return 1 - (sum/(double)this.nrIssues)/(double)rounds;
	}
}
