package ai2016.group5;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.boaframework.SortedOutcomeSpace;
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
	private HashMap<AgentID, StrategyModel>  strategies;
	private Party own;
	private SortedOutcomeSpace SOS = new SortedOutcomeSpace(this.utilitySpace);
	
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
		if (this.turn > 1)
		{
			this.initializeStrategyModels();
		}
		if (this.getUtility(lastReceivedBid) > 0.85)
		{
			return new Accept(getPartyId(), lastReceivedBid);
		}
		else
		{
			Bid bid = this.generateBid();
			if (this.getUtility(bid) > 0.8)
			{
				this.own.addBid(bid, this.getUtility(bid));
				return new Offer(getPartyId(), bid);
			}
		}

		try {
			this.own.addBid(utilitySpace.getMaxUtilityBid(), 1.0);
			return new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());

		} catch (Exception e) {
			return new Offer(getPartyId(), generateRandomBid());
		}
	}
	
	private void initializeStrategyModels()
	{
		Set<AgentID> ids = this.agents.keySet();
		for (AgentID id1 : ids) {
			Party[] parties = new Party[ids.size()]; 
			int i = 0;
			for(AgentID id2 : ids){
				if (!id1.equals(id2)){
					parties[i] = this.agents.get(id2);
				}
				i++;
			}
			parties[ids.size()-1] = this.own;
			this.strategies.put(id1, new StrategyModel(parties, this.agents.get(id1)));
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
			if (this.turn > 22){
				System.out.println("Conceding rate:");
				System.out.println(strategies.get(sender).getConcedingRate(20));
			}
			lastReceivedBid = ((Offer) action).getBid();
		}
	}

	private Bid generateBid()
	{

		double ownUtility = 0.0;
		double totalUtility = 0.0;
		double overallUtility = 0.0;
		Bid randomBid;

		Bid bestBid = this.generateRandomBid();
		double bestOverallUtility = 0;
		for (int i=0; i<100; i++)
		{
			randomBid = this.generateRandomBid();
			ownUtility = this.getUtility(randomBid);
			totalUtility = 0;
			for (AgentID agent : this.agents.keySet()){
				totalUtility += this.agents.get(agent).getUtility(randomBid);
			}
			//System.out.println(totalUtility);

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
		return "Party Group 5";
	}

}
