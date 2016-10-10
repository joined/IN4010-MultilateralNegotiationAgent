package ai2016;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.AgentID;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;

import java.util.Arrays;
import java.util.HashMap;



public class Party {
	AgentID id;
	BidHistory bidHistory;
	Integer nrIssues;
	int[] issueIds;
	EvaluatorDiscrete[] issues;

	/*
	 * Create a new Party object
	 */
	public Party(AgentID id, Bid example)
	{
		this.id = id;
		this.bidHistory = new BidHistory();
		this.nrIssues = example.getIssues().size();
		this.issueIds = new int[this.nrIssues];

		for (int i=0; i < example.getIssues().size(); i++)
		{
			this.issueIds[i] = example.getIssues().get(i).getNumber();
		}

		this.issues = new EvaluatorDiscrete[this.nrIssues];
		for (int i=0; i < this.nrIssues; i++)
		{
			this.issues[i] = new EvaluatorDiscrete();
		}
	}

	/*
	 * Add a bid to the bid history
	 */
	public void addBid(Bid bid,  double utility)
	{
		this.bidHistory.add(new BidDetails(bid,utility));
	}
	
	/*
	 * Get the utility of the given bid
	 */
	public double getUtility(Bid bid)
	{
		double utility = 0.0;

		HashMap<Integer, Value> bidValues = bid.getValues();
		for (int i = 0; i < this.nrIssues; i++) {
			double weight = this.issues[i].getWeight();
			ValueDiscrete value = (ValueDiscrete)bidValues.get(this.issueIds[i]);
			if (((EvaluatorDiscrete)this.issues[i]).getValues().contains(value))
			{
				utility = utility + this.issues[i].getDoubleValue(value).doubleValue() * weight; }
		}
		return utility;
	}
	/*
	 * Set the weights of the issues and the values per issue
	 */
	public void setWeights()
	{
		this.setWeightsIssues();
		this.setWeightsIssueValues();
	}

	/*
	 * Set the weights for the values of each issues using frequency analysis
	 */
	private void setWeightsIssueValues()
	{
		for (int i=0; i<this.nrIssues; i++)
		{
			HashMap<ValueDiscrete, Double> values = new HashMap<ValueDiscrete, Double>();
			for (int j=0; j<this.bidHistory.size(); j++)
			{
				ValueDiscrete value = (ValueDiscrete)(this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i]));
				if (values.containsKey(value))
				{
					if (j==0) values.put(value, values.get(value) + 5);
					else values.put(value, values.get(value) + 1);
				}
				else
				{
					if (j==0) values.put(value, 1.0);
					else values.put(value, 1.0);
				}
			}
			for (ValueDiscrete value : values.keySet())
			{
				try {
					this.issues[i].setEvaluationDouble(value, values.get(value));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * Set the weights for each issue using frequency analysis
	 */

	private void setWeightsIssues()
	{
		int[] changesIssues = getChangesIssues();
		double[] weights = new double[this.nrIssues];
		Arrays.fill(weights, 1/this.nrIssues);

		for (int i=0; i<this.nrIssues; i++)
		{
			this.issues[i].setWeight(weights[i] + (this.bidHistory.size() - changesIssues[i] -1)/10);
		}
	}
	
	/*
	 * Return an array which represents the frequency of change of each issue
	 */

	private int[] getChangesIssues()
	{
		int[] changes = new int[this.nrIssues];
		for (int i=0; i<this.nrIssues; i++)
		{
			Value old = null;
			int count = 0;
			for (int j=0; j<this.bidHistory.size(); j++)
			{	
				if (old != null)
				{
					if (old.equals(this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i])))
					{
						count ++;
					}
				}
				old = this.bidHistory.getHistory().get(j).getBid().getValue(this.issueIds[i]);
			}
			changes[i] = count;
		}
		return changes;
	}

}
