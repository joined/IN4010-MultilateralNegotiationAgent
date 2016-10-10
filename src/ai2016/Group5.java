package ai2016;

import java.util.HashMap;
import java.util.List;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.actions.DefaultAction;

import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group5 extends AbstractNegotiationParty {

	private Bid lastReceivedBid = null;
	private HashMap<AgentID, Party> agents;
	int turn;

	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl,
			TimeLineInfo tl, long randomSeed, AgentID agentId) {

		super.init(utilSpace, dl, tl, randomSeed, agentId);

		System.out.println("Discount Factor is "
				+ utilSpace.getDiscountFactor());
		System.out.println("Reservation Value is "
				+ utilSpace.getReservationValueUndiscounted());

		// if you need to initialize some variables, please initialize them
		// below
		agents = new HashMap<AgentID, Party>();
		this.turn = 0;
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {
		this.turn ++;
		if (this.getUtility(lastReceivedBid) > 0.85)
		{
			return new Accept(getPartyId(), lastReceivedBid);
		}
		else if (this.turn > 10)
		{
			Bid bid = this.generateBid();
			if (this.getUtility(bid) > 0.8)
			{
				return new Offer(getPartyId(), bid);
			}
		}

		try {
			return new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());
		} catch (Exception e) {
			return new Offer(getPartyId(), generateRandomBid());
		}
	}

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action. Can be null.
	 * @param action
	 *            The action that party did.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		if (action instanceof Offer) {
			Bid b = DefaultAction.getBidFromAction(action);
			if (agents.containsKey(sender))
			{
				agents.get(sender).addBid(b, getUtility(b));
			}
			else
			{
				try {
					agents.put(sender, new Party(sender, this.utilitySpace.getMaxUtilityBid()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			lastReceivedBid = ((Offer) action).getBid();
		}
	}

	private Bid generateBid()
	{

		double ownUtility = 0;
		double totalUtility = 0;
		double overallUtility = 0;
		Bid randomBid;

		Bid bestBid = this.generateRandomBid();
		double bestOverallUtility = 0;
		for (int i=0; i<100; i++)
		{
			randomBid = this.generateRandomBid();
			ownUtility = this.getUtility(randomBid);
			for (AgentID agent : this.agents.keySet()){
				totalUtility += this.agents.get(agent).getUtility(randomBid);
			}
			overallUtility = 0.3 * totalUtility/this.agents.size() + 0.7 * ownUtility;
			if (overallUtility > bestOverallUtility)
			{
				bestBid = randomBid;
			}
		}
		return bestBid;
	}

	@Override
	public String getDescription() {
		return "example party group N";
	}

}
