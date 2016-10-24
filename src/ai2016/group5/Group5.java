package ai2016.group5;

import java.util.HashMap;
import java.util.List;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

/**
 * The negotiation party.
 */
public class Group5 extends AbstractNegotiationParty {
	// Latest bid received by the opponents
	private Bid lastReceivedBid = null;
	// Map of the opponents
	private HashMap<AgentID, Opponent> opponentsMap;
	// Percentage of time in which we'll just keep offering the maximum utility bid
	private final double TIME_OFFERING_MAX_UTILITY_BID = 0.85D;
	// Utility above which all of our offers will be
	private final double RESERVATION_VALUE = 0.8D;

	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId) {

		super.init(utilSpace, dl, tl, randomSeed, agentId);

		this.opponentsMap = new HashMap<AgentID, Opponent>();
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {
		// For the first part of the negotiation, just keep offering the maximum
		// utility bid
		if (isMaxUtilityOfferTime()) {
			Bid maxUtilityBid = null;
			try {
				maxUtilityBid = this.utilitySpace.getMaxUtilityBid();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Cannot generate max utility bid");
			}
			System.out.println("It's max utility bid time!");
			return new Offer(getPartyId(), maxUtilityBid);
		}
		
		System.out.format("Last received bid had utility of [%f] for me%n", getUtility(this.lastReceivedBid));

		// If we're towards the end of the negotiation, generate a bid
		Bid proposedBid = generateBid();

		// Check if we should accept the latest offer, given the bid we're
		// proposing
		if (isAcceptable(proposedBid)) {
			System.out.println("I'm going to accept the latest offer!");
			return new Accept(getPartyId(), this.lastReceivedBid);
		}

		// Offer the proposed bid
		System.out.format("I'm going to offer the bid that I generated, which has utility [%f]%n", getUtility(proposedBid));
		return new Offer(getPartyId(), proposedBid);
	}

	/**
	 * Determines whether we should accept the latest opponent's bid
	 * @param proposedBid The bid that we're going to offer
	 * @return boolean indicating whether we should accept the latest bid
	 */
	private boolean isAcceptable(Bid proposedBid) {
		// Check if the utility of the latest received offer is higher than the utility
		// of the bid we are going to offer
		boolean aNext = getUtility(this.lastReceivedBid) >= getUtility(proposedBid);
		
		double timeLeft = 1 - getTimeLine().getTime();
		// At the beginning of the negotiation the minimum utility will be 0.9,
		// when 90% of the time has elapsed it will be ~0.83
		// when 99% of the time has elapsed it will be ~0.77
		// and then it will drop to 0 very rapidly (it will be exactly 0 when no time is left)
		double minUtility = Math.log10(timeLeft) / 15D + 0.9D;
		
		System.out.format("Min utility: [%f]%n", minUtility);
		
		// We accept the latest offer if it has a greater utility than the one we are proposing,
		// or if its utility is higher than our minUtility
		return (aNext || getUtility(this.lastReceivedBid) > minUtility);
	}
	
	/**
	 * Determines if we're in the time in which we should just keep
	 * offering the max utility bid
	 * @return boolean indicating whether we should offer the maximum utility bid
	 */
	private boolean isMaxUtilityOfferTime() {
		return getTimeLine().getTime() < this.TIME_OFFERING_MAX_UTILITY_BID;
	}

	/**
	 * Reception of offers made by other parties.
	 * @param sender The party that did the action. Can be null.
	 * @param action The action that party did.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);

		// If we're receiving an offer
		if (sender != null && action instanceof Offer) {
			// Store the bid as the latest received bid
			this.lastReceivedBid = ((Offer) action).getBid();

			// Store the bid in the opponent's history
			if (opponentsMap.containsKey(sender)) {
				opponentsMap.get(sender).addBid(this.lastReceivedBid);
			} else {
				// If it's the first time we see this opponent, create a new
				// entry in the opponent map
				try {
					Opponent newOpponent = new Opponent(generateRandomBid());
					newOpponent.addBid(this.lastReceivedBid);
					opponentsMap.put(sender, newOpponent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Generates a random bid that has an higher utility than
	 * our own reservation value
	 * @return the random bid with utility higher than the reservation value
	 */
	private Bid genValidRandomBid() {
		Bid randomBid;

		// Keep generating random bids until we find one that has an higher utility
		// than our reservation value
		do {
			randomBid = generateRandomBid();
		} while (getUtility(randomBid) < this.RESERVATION_VALUE);

		return randomBid;
	}
	
	/**
	 * Generates a new bid basing on our own utility and the estimated utility
	 * of the opponents, through frequency analysis
	 * @return the generated bid, which has always a utility higher than our reservation value
	 */
	private Bid generateBid() {
		double ownUtility;
		double opponentsTotalUtility;
		double averageOpponentsUtility;
		double overallUtility;
		Bid randomBid;

		Bid bestBid = genValidRandomBid();
		double bestOverallUtility = 0;

		// Generate 100 times random (valid) bids and see which one has a better overall utility
		for (int i = 0; i < 100; i++) {
			// Generate a valid random bid
			randomBid = genValidRandomBid();
			// Get utility for the generated random bid
			ownUtility = getUtility(randomBid);

			opponentsTotalUtility = 0;
			for (AgentID agent : this.opponentsMap.keySet()) {
				opponentsTotalUtility += this.opponentsMap.get(agent).getUtility(randomBid);
			}

			// Get the average utility for the opponents
			averageOpponentsUtility = opponentsTotalUtility / this.opponentsMap.size();

			// We value our own utility 0.7 and the opponents utility 0.3
			overallUtility = 0.3 * averageOpponentsUtility + 0.7 * ownUtility;
			if (overallUtility > bestOverallUtility) {
				bestBid = randomBid;
			}
		}
		return bestBid;
	}

	/**
	 * Description of the agent
	 */
	@Override
	public String getDescription() {
		return "Party Group 5";
	}

}
