/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */
package net.finmath.experiments.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariableAccumulatorInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements calculation of the delta of a European option.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class EuropeanOptionGammaLikelihood extends AbstractAssetMonteCarloProduct {

	private double	maturity;
	private double	strike;
	
	private boolean	isLikelihoodByFiniteDifference = false;
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * 
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionGammaLikelihood(double maturity, double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
	}
	
	/**
	 * Calculates the value of the option under a given model.
	 * 
	 * @param model A reference to a model
	 * @return the value
	 * @throws CalculationException 
	 */
	public double getValue(AssetModelMonteCarloSimulationInterface model) throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = null;
		try {
			blackScholesModel = (MonteCarloBlackScholesModel)model;
		}
		catch(Exception e) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity,0);
		RandomVariableInterface numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariableInterface underlyingAtToday		= model.getAssetValue(0.0,0);
		RandomVariableInterface numeraireAtToday		= model.getNumeraire(0);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);
		
		/*
		 *  The following way of calculating the expected value (average) is discouraged since it makes too strong
		 *  assumptions on the internals of the <code>RandomVariableInterface</code>. Instead you should use
		 *  the mutators sub, div, mult and the getter getAverage. This code is provided for illustrative purposes.
		 */
		double average = 0.0;
		for(int path=0; path<model.getNumberOfPaths(); path++)
		{
			if(underlyingAtMaturity.get(path) > strike)
			{
				// Get some model parameters
				double T		= maturity;
				double S0		= underlyingAtToday.get(path);
				double r		= blackScholesModel.getModel().getRiskFreeRate();
				double sigma	= blackScholesModel.getModel().getVolatility();

				double ST		= underlyingAtMaturity.get(path);

				double lr;
				if(isLikelihoodByFiniteDifference) {
					double h		= 1E-2;

					double x		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));
					double phi		= (1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x*x/2.0) / (ST * (sigma) * Math.sqrt(T)) );
	
					double xDown	= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0-h)));
					double phiDown	= (1.0/Math.sqrt(2 * Math.PI) * Math.exp(-xDown*xDown/2.0) / (ST * (sigma) * Math.sqrt(T)) );
					
					double xUp		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0+h)));
					double phiUp	= (1.0/Math.sqrt(2 * Math.PI) * Math.exp(-xUp*xUp/2.0) / (ST * (sigma) * Math.sqrt(T)) );
					
					lr	= (phiUp - 2 * phi + phiDown) / (h * h) / phi;
				}
				else {
					double x			= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));
					double phi			= (1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x*x/2.0) / (ST * (sigma) * Math.sqrt(T)) );
					double dPhidS0		= x / (sigma * Math.sqrt(T)) / S0 * phi;
					double d2PhidS02	= -1 / (sigma * Math.sqrt(T)) / (sigma * Math.sqrt(T)) / S0 / S0 * phi - x / (sigma * Math.sqrt(T)) / S0 / S0 * phi
							+ x / (sigma * Math.sqrt(T)) / S0 * dPhidS0;
					lr = d2PhidS02 / phi;
				}

				double payOff			= (underlyingAtMaturity.get(path) - strike);
				double modifiedPayoff	= payOff * lr;

				average += modifiedPayoff / numeraireAtMaturity.get(path) * monteCarloWeights.get(path) * numeraireAtToday.get(path);
			}
		}

		return average;
	}

	@Override
	public RandomVariableAccumulatorInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) {
		throw new RuntimeException("Method not supported.");
	}
}
