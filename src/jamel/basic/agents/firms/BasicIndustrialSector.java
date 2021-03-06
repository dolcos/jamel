package jamel.basic.agents.firms;

import jamel.Simulator;
import jamel.basic.agents.roles.CapitalOwner;
import jamel.basic.agents.util.AgentSet;
import jamel.basic.agents.util.BasicAgentSet;
import jamel.basic.util.BankAccount;
import jamel.basic.util.JobOffer;
import jamel.basic.util.Supply;
import jamel.util.Circuit;
import jamel.util.Sector;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A basic industrial sector.
 */
public class BasicIndustrialSector implements Sector, IndustrialSector {

	/**
	 * The keys of the parameters of the sector.
	 */
	private static class PARAM {

		/** The key for the type of agents to create*/
		public static final String FIRMS_TYPE = "agents.type";

		/** REGENERATION_MAX */
		public static final String REGENERATION_MAX = "regenerationLapse.max";

		/** REGENERATION_MIN */
		public static final String REGENERATION_MIN = "regenerationLapse.min";

	}

	@SuppressWarnings("javadoc")
	private static final String MSG_PUT_DATA = "putData";

	/** The circuit. */
	private final Circuit circuit;

	/** To count the number of firms created since the start of the simulation. */
	private int countFirms;

	/** The collection of firms. */
	private final AgentSet<Firm> firms;

	/** The sector name. */
	private final String name;

	/** A scheduler for the regeneration of firms. */
	private final Map<Integer,Integer> regeneration = new HashMap<Integer,Integer>();

	/**
	 * Creates a new banking sector.
	 * @param name the name of the sector.
	 * @param circuit the circuit.
	 */
	public BasicIndustrialSector(String name, Circuit circuit) {
		this.name=name;
		this.circuit=circuit;
		this.firms=new BasicAgentSet<Firm>();
	}

	/**
	 * Closes the sector at the end of the period.
	 */
	private void close() {
		List<Firm> bankrupted = new LinkedList<Firm>();
		for (final Firm firm:firms.getList()) {
			firm.close();
			if (firm.isBankrupted()) {
				bankrupted.add(firm);
				prepareRegeneration();
			}
		}
		this.circuit.forward(MSG_PUT_DATA,this.name,this.firms.collectData());
		this.firms.removeAll(bankrupted);
	}

	/**
	 * Creates firms.
	 * @param type the type of firms to create.
	 * @param lim the number of firms to create.
	 * @return a list containing the new firms. 
	 */
	private List<Firm> createFirms(String type, int lim) {
		final List<Firm> result = new ArrayList<Firm>(lim);
		try {
			for(int index=0;index<lim;index++) {
				this.countFirms++;
				final String name = "Firm"+this.countFirms;
				final Firm firm = (Firm) Class.forName(type,false,ClassLoader.getSystemClassLoader()).getConstructor(String.class,IndustrialSector.class).newInstance(name,this);;
				result.add(firm);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Class not found:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>No such method:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Illegal argument:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 			
		} catch (SecurityException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Security exception:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 			
		} catch (InstantiationException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Instantiation exception:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 			
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Illegal access:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure"); 			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Simulator.showErrorDialog("Error while creating the firms.<br>Invocation target exception:<br>"+e.getMessage());
			throw new RuntimeException("Firm creation failure");
		}
		return result;
	}

	/**
	 * Prepares the regeneration of a firm some periods later.
	 */
	private void prepareRegeneration() {
		final int min = Integer.parseInt(circuit.getParameter(this.name,PARAM.REGENERATION_MIN));
		final int max = Integer.parseInt(circuit.getParameter(this.name,PARAM.REGENERATION_MAX));		
		final int now = Circuit.getCurrentPeriod().getValue();
		final int later = now + min + Circuit.getRandom().nextInt(max);
		Integer creations = this.regeneration.get(later);
		if (creations!=null){
			creations++;
		}
		else {
			creations=1;
		}
		this.regeneration.put(later,creations);
	}


	/**
	 * 
	 */
	private void regenerate() {
		final Integer lim = this.regeneration.get(Circuit.getCurrentPeriod().getValue());
		if (lim != null) {
			this.firms.putAll(this.createFirms(this.circuit.getParameter(this.name,PARAM.FIRMS_TYPE),lim));
		}
	}

	@Override
	public boolean doPhase(String phaseName) {

		if (phaseName.equals("opening")) {
			regenerate();
			for (final Firm firm:firms.getList()) {
				firm.open();
			}
		} 

		else if (phaseName.equals("pay_dividend")) {
			for (final Firm firm:firms.getList()) {
				firm.payDividend();
			}
		}

		else if (phaseName.equals("plan_production")) {
			for (final Firm firm:firms.getShuffledList()) {
				firm.prepareProduction();
			}			
		}

		else if (phaseName.equals("production")) {
			for (final Firm firm:firms.getShuffledList()) {
				firm.production();
			}			
		}

		else if (phaseName.equals("closure")) {
			this.close();
		}

		else {
			throw new IllegalArgumentException("Unknown phase <"+phaseName+">");			
		}

		return true;
	}

	@Override
	public Object forward(String request, Object ... args) {

		final Object result;

		if (request.equals("getJobOffers")) {
			final int size = (Integer) (args[0]);
			final ArrayList<JobOffer> jobOffersList=new ArrayList<JobOffer>(size);
			for (final Firm firm:firms.getRandomList(size)) {
				final JobOffer jobOffer = firm.getJobOffer();
				if (jobOffer!=null) {
					jobOffersList.add(jobOffer);
				}
			};
			result = jobOffersList.toArray(new JobOffer[jobOffersList.size()]);
		}

		else if (request.equals("getSupplies")) {
			final int size = (Integer) (args[0]);
			final ArrayList<Supply> list=new ArrayList<Supply>(size);
			for (final Firm firm:firms.getRandomList(size)) {
				final Supply supply = firm.getSupply();
				if (supply!=null) {
					list.add(supply);
				}
			};
			result = list.toArray(new Supply[list.size()]);
		}

		else if (request.equals("addDataKey")) {
			// DELETE
			throw new RuntimeException("This request is obsolete.");
			//result = this.dataKeys .add((String) args[0]);
		}

		else if (request.equals("change in parameters")) {
			for (final Firm firm:firms.getList()) {
				firm.updateParameters();
			}
			result = null;
		}

		else if (request.equals("new")) {
			this.firms.putAll(this.createFirms(this.circuit.getParameter(this.name,PARAM.FIRMS_TYPE),Integer.parseInt((String) args[0])));			
			result = null;
		}

		else {
			throw new IllegalArgumentException(this.name+": Unknown request <"+request+">");
		}

		return result;

	}

	/**
	 * Returns the sector name.
	 * @return the sector name.
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public BankAccount getNewAccount(Firm firm) {
		return (BankAccount) circuit.forward("getNewAccount", firm);
	}

	@Override
	public String getParameter(String key) {
		return this.circuit.getParameter(this.name,key);
	}

	@Override
	public void pause() {
		// Does nothing.		
	}

	@Override
	public CapitalOwner selectCapitalOwner() {
		return (CapitalOwner) circuit.forward("selectCapitalOwner");
	}

}

// ***
