/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004, 21.12.2012, 01.02.2012
 */
package net.finmath.experiments.montecarlo.assetderivativevaluation;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOption2;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionDeltaLikelihood;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionDeltaPathwise;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionGammaLikelihood;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionGammaPathwise;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionRhoLikelihood;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionRhoPathwise;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionVegaLikelihood;
import net.finmath.experiments.montecarlo.assetderivativevaluation.products.EuropeanOptionVegaPathwise;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

import org.junit.Test;


/**
 * This class represents a collection of several "tests" illustrating different aspects
 * related to the Monte-Carlo Simulation and derivative pricing (using a simple
 * Black-Scholes model.
 * 
 * @author Christian Fries
 */
public class BlackScholesMonteCarloValuationTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.02;
	private final double	volatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 10000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;


	private AssetModelMonteCarloSimulationInterface model = null;

	/**
	 * This main method will test a Monte-Carlo simulation of a Black-Scholes model and some valuations
	 * performed with this model.
	 * 
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, CalculationException, InterruptedException
	{
		BlackScholesMonteCarloValuationTest pricingTest = new BlackScholesMonteCarloValuationTest();

		/*
		 * Read input
		 */
		int testNumber = readTestNumber();



		long start = System.currentTimeMillis();

		switch(testNumber) {
		case 1:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCall();
			break;
		case 2:
			pricingTest.testModelProperties();
			break;
		case 3:
			pricingTest.testModelRandomVariable();
			break;
		case 4:
			pricingTest.testEuropeanAsianBermudanOption();
			break;
		case 5:
			pricingTest.testMultiThreaddedValuation();
			break;
		case 6:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCallDelta();    	
			break;
		case 7:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCallVega();    	
			break;
		case 8:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCallGamma();    	
			break;
		}

		long end = System.currentTimeMillis();

		System.out.println("\nCalculation time required: " + ((double)(end-start))/1000.0 + " seconds.");
	}

	public BlackScholesMonteCarloValuationTest() {
		super();

		// Create a Model (see method getModel)
		model = getModel();
	}

	private static int readTestNumber() {
		System.out.println("Please select a test to run (click in this window and enter a number):");
		System.out.println("\t 1: Valuation of European call options (with different strikes).");
		System.out.println("\t 2: Some model properties.");
		System.out.println("\t 3: Print some realizations of the S(1).");
		System.out.println("\t 4: Valuation of European, Asian, Bermudan option.");
		System.out.println("\t 5: Multi-Threadded valuation of some ten thousand Asian options.");
		System.out.println("\t 6: Sensitivity (Delta) of European call options (with different strikes) using different methods.");
		System.out.println("\t 7: Sensitivity (Vega) of European call options (with different strikes) using different methods.");
		System.out.println("\t 8: Sensitivity (Gamma) of European call options (with different strikes) using different methods.");
		System.out.println("");
		System.out.print("Test to run: ");

		//  open up standard input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		int testNumber = 0;
		try {
			String test = br.readLine();
			testNumber = Integer.valueOf(test);
		} catch (IOException ioe) {
			System.out.println("IO error trying to read test number!");
			System.exit(1);
		}

		System.out.println("");
		return testNumber;
	}

	public AssetModelMonteCarloSimulationInterface getModel()
	{
		// Create the time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0, numberOfTimeSteps, deltaT);

		// Create an instance of a black scholes monte carlo model
		AssetModelMonteCarloSimulationInterface model = new MonteCarloBlackScholesModel(
				timeDiscretization,
				numberOfPaths,
				initialValue,
				riskFreeRate,
				volatility);

		return model;
	}

	@Test
	public void testEuropeanCall() throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = (MonteCarloBlackScholesModel)model;

		// Java DecimalFormat for our output format
		DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		// Test options with different strike
		System.out.println("Valuation of European Options");
		System.out.println(" Strike \t Monte-Carlo \t Analytic \t Deviation \t Monte-Carlo (alternative implementation)");

		double initialValue	= blackScholesModel.getAssetValue(0.0, 0).get(0);
		double riskFreeRate	= blackScholesModel.getModel().getRiskFreeRate();
		double volatility	= blackScholesModel.getModel().getVolatility();

		double optionMaturity	= 1.0;
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);
			// Value the product with Monte Carlo
			double valueMonteCarlo	= callOption.getValue(blackScholesModel);

			// Create a product
			EuropeanOption2	callOption2	= new EuropeanOption2(optionMaturity, optionStrike);
			// Value the product with Monte Carlo
			double valueMonteCarlo2	= callOption2.getValue(blackScholesModel);

			// Calculate the analytic value
			double valueAnalytic	= net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) + 
					"\t" + numberFormatValue.format(valueMonteCarlo) +
					"\t" + numberFormatValue.format(valueAnalytic) +
					"\t" + numberFormatDeviation.format(valueMonteCarlo-valueAnalytic) +
					"\t" + numberFormatValue.format(valueMonteCarlo2));

		}
	}

	/**
	 * Test some properties of the model
	 */
	@Test
	public void testModelProperties() throws CalculationException {

		System.out.println("Time \tAverage \t\tVariance");

		TimeDiscretizationInterface modelTimeDiscretization = model.getTimeDiscretization();
		for(double time : modelTimeDiscretization) {
			RandomVariableInterface assetValue = model.getAssetValue(time, 0);

			double average	= assetValue.getAverage();
			double variance	= assetValue.getVariance();
			double error	= assetValue.getStandardError();

			DecimalFormat formater2Digits = new DecimalFormat("0.00");
			DecimalFormat formater4Digits = new DecimalFormat("0.0000");
			System.out.println(formater2Digits.format(time) + " \t" + formater4Digits.format(average) + "\t+/- " + formater4Digits.format(error) + "\t" + formater4Digits.format(variance));
		}
	}

	@Test
	public void testModelRandomVariable() throws CalculationException {
		RandomVariableInterface stockAtTimeOne = model.getAssetValue(1.0, 0);

		System.out.println("The first 100 realizations of the " + stockAtTimeOne.size() + " realizations of S(1) are:");
		System.out.println("Path\tValue");
		for(int i=0; i<100;i++) {
			System.out.println(i + "\t" + stockAtTimeOne.get(i));
		}
	}

	/**
	 * Evaluates different options (European, Asian, Bermudan) using the given model.
	 * 
	 * The options share the same maturity and strike for the at t=3.0.
	 * Observations which can be made:
	 * <ul>
	 * <li>The Asian is cheaper than the European since averaging reduces the volatility.
	 * <li>The European is cheaper than the Bermudan since exercises into the European is one (out of may) exercises strategies of the Bermudan.
	 * </ul>
	 */
	@Test
	public void testEuropeanAsianBermudanOption() throws CalculationException {
		/*
		 * Common parameters
		 */
		double maturity = 3.0;
		double strike = 1.06;

		/*
		 * European Option
		 */
		EuropeanOption myEuropeanOption = new EuropeanOption(maturity,strike);
		double valueOfEuropeanOption = myEuropeanOption.getValue(model);

		/*
		 * Asian Option
		 */
		double[] averagingPoints = { 1.0, 1.5, 2.0, 2.5 , 3.0 };

		AsianOption myAsianOption = new AsianOption(maturity,strike, new TimeDiscretization(averagingPoints));
		double valueOfAsianOption = myAsianOption.getValue(model);

		/*
		 * Bermudan Option
		 */
		double[] exerciseDates	= { 1.0,  2.0,  3.0};
		double[] notionals		= { 1.20, 1.10, 1.0};
		double[] strikes		= { 1.03, 1.05, 1.06 };

		// Lower bound method
		BermudanOption myBermudanOptionLowerBound = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.ESTIMATE_COND_EXPECTATION);
		double valueOfBermudanOptionLowerBound = myBermudanOptionLowerBound.getValue(model);

		// Upper bound method
		BermudanOption myBermudanOptionUpperBound = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.UPPER_BOUND_METHOD);
		double valueOfBermudanOptionUpperBound = myBermudanOptionUpperBound.getValue(model);

		/*
		 * Output
		 */
		System.out.println("Value of Asian Option is \t"	+ valueOfAsianOption);
		System.out.println("Value of European Option is \t"	+ valueOfEuropeanOption);
		System.out.println("Value of Bermudan Option is \t"	+ "(" + valueOfBermudanOptionLowerBound + "," + valueOfBermudanOptionUpperBound + ")");

		assertTrue(valueOfAsianOption < valueOfEuropeanOption);
		assertTrue(valueOfBermudanOptionLowerBound < valueOfBermudanOptionUpperBound);
		assertTrue(valueOfEuropeanOption < valueOfBermudanOptionUpperBound);
	}

	/**
	 * Evaluates 100000 Asian options in 10 parallel threads (each valuing 10000 options)
	 * 
	 * @throws InterruptedException 
	 */
	public void testMultiThreaddedValuation() throws InterruptedException {
		final double[] averagingPoints = { 0.5, 1.0, 1.5, 2.0, 2.5, 2.5, 3.0, 3.0 , 3.0, 3.5, 4.5, 5.0 };
		final double maturity = 5.0;
		final double strike = 1.07;

		int			numberOfThreads	= 10;		
		Thread[]	myThreads		= new Thread[numberOfThreads];

		for(int k=0; k<myThreads.length; k++) {

			final int threadNummer = k;

			// Create a runnable - piece of code which can be run in parallel.
			Runnable myRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						for(int i=0;i<10000; i++) {
							AsianOption myAsianOption = new AsianOption(maturity,strike, new TimeDiscretization(averagingPoints));
							double valueOfAsianOption = myAsianOption.getValue(model);
							System.out.println("Thread " + threadNummer + ": Value of Asian Option " + i + " is " + valueOfAsianOption);
						}
					} catch (CalculationException e) {
					}
				}
			};

			// Create a thread (will run asynchronously)
			myThreads[k] = new Thread(myRunnable);
			myThreads[k].start();
		}

		// Wait for all threads to complete
		for(int i=0; i<myThreads.length; i++) {
			myThreads[i].join();
		}

		// Threads are completed at this point
	}

	@Test
	public void testEuropeanCallDelta() throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = (MonteCarloBlackScholesModel)model;

		// Java DecimalFormat for our output format
		DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		double initialValue	= blackScholesModel.getAssetValue(0.0, 0).get(0);
		double riskFreeRate	= blackScholesModel.getModel().getRiskFreeRate();
		double volatility	= blackScholesModel.getModel().getVolatility();

		double optionMaturity	= 1.0;

		// Test options with different strike
		System.out.println("Calculation of Option Delta (European options with maturity " + optionMaturity + "):");
		System.out.println(" Strike \t MC Fin.Diff.\t MC Pathwise\t MC Likelihood\t Analytic \t Diff MC-FD \t Diff MC-PW \t Diff MC-LR");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			double shift = initialValue * 1E-6;

			Map<String,Object> dataUpShift = new HashMap<String,Object>();
			dataUpShift.put("initialValue", initialValue + shift);

			double valueUpShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataUpShift).get("value"));

			Map<String,Object> dataDownShift = new HashMap<String,Object>();
			dataDownShift.put("initialValue", initialValue - shift);
			double valueDownShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			double delta = (valueUpShift-valueDownShift) / ( 2 * shift );

			// Calculate the finite difference of the analytic value
			double deltaFiniteDiffAnalytic	=
					(
							net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue+shift, riskFreeRate, volatility, optionMaturity, optionStrike)
							- net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue-shift, riskFreeRate, volatility, optionMaturity, optionStrike)
							)/(2*shift);

			// Calculate the analytic value
			double deltaAnalytic	= net.finmath.functions.AnalyticFormulas.blackScholesOptionDelta(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Calculate the value using pathwise differentiation
			EuropeanOptionDeltaPathwise		callOptionDeltaPathwise	= new EuropeanOptionDeltaPathwise(optionMaturity, optionStrike);
			double							deltaPathwise				= callOptionDeltaPathwise.getValue(blackScholesModel);

			// Calculate the value using likelihood differentiation
			EuropeanOptionDeltaLikelihood	callOptionDeltaLikelihood	= new EuropeanOptionDeltaLikelihood(optionMaturity, optionStrike);
			double							deltaLikelihood				= callOptionDeltaLikelihood.getValue(blackScholesModel);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) + 
					"\t" + numberFormatValue.format(delta) +
					"\t" + numberFormatValue.format(deltaPathwise) +
					"\t" + numberFormatValue.format(deltaLikelihood) +
					"\t" + numberFormatValue.format(deltaAnalytic) +
					"\t" + numberFormatDeviation.format(delta-deltaAnalytic) +
					"\t" + numberFormatDeviation.format(deltaPathwise-deltaAnalytic) +
					"\t" + numberFormatDeviation.format(deltaLikelihood-deltaAnalytic));
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testEuropeanCallVega() throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = (MonteCarloBlackScholesModel)model;

		// Java DecimalFormat for our output format
		DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		double initialValue	= blackScholesModel.getAssetValue(0.0, 0).get(0);
		double riskFreeRate	= blackScholesModel.getModel().getRiskFreeRate();
		double volatility	= blackScholesModel.getModel().getVolatility();

		double optionMaturity	= 5.0;

		// Test options with different strike
		System.out.println("Calculation of Option Vega (European options with maturity " + optionMaturity + "):");
		System.out.println(" Strike \t MC Fin.Diff.\t MC Pathwise\t MC Likelihood\t Analytic \t Diff MC-FD \t Diff MC-PW \t Diff MC-LR");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			double shift = volatility * 1E-6;

			Map<String,Object> dataUpShift = new HashMap<String,Object>();
			dataUpShift.put("volatility", volatility + shift);
			double valueUpShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataUpShift).get("value"));

			Map<String,Object> dataDownShift = new HashMap<String,Object>();
			dataDownShift.put("volatility", volatility - shift);
			double valueDownShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			double vega = (valueUpShift-valueDownShift) / ( 2 * shift );

			// Calculate the finite difference of the analytic value
			double vegaFiniteDiffAnalytic	=
					(
							net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility+shift, optionMaturity, optionStrike)
							- net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility-shift, optionMaturity, optionStrike)
							)/(2*shift);

			// Calculate the analytic value
			double vegaAnalytic	= net.finmath.functions.AnalyticFormulas.blackScholesOptionVega(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Calculate the value using pathwise differentiation
			EuropeanOptionVegaPathwise		callOptionVegaPathwise	= new EuropeanOptionVegaPathwise(optionMaturity, optionStrike);
			double							vegaPathwise				= callOptionVegaPathwise.getValue(blackScholesModel);

			// Calculate the value using likelihood differentiation
			EuropeanOptionVegaLikelihood	callOptionVegaLikelihood	= new EuropeanOptionVegaLikelihood(optionMaturity, optionStrike);
			double							vegaLikelihood				= callOptionVegaLikelihood.getValue(blackScholesModel);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) + 
					"\t" + numberFormatValue.format(vega) +
					"\t" + numberFormatValue.format(vegaPathwise) +
					"\t" + numberFormatValue.format(vegaLikelihood) +
					"\t" + numberFormatValue.format(vegaAnalytic) +
					"\t" + numberFormatDeviation.format(vega-vegaAnalytic) +
					"\t" + numberFormatDeviation.format(vegaPathwise-vegaAnalytic) +
					"\t" + numberFormatDeviation.format(vegaLikelihood-vegaAnalytic));

		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testEuropeanCallRho() throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = (MonteCarloBlackScholesModel)model;

		// Java DecimalFormat for our output format
		DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		double initialValue	= blackScholesModel.getAssetValue(0.0, 0).get(0);
		double riskFreeRate	= blackScholesModel.getModel().getRiskFreeRate();
		double volatility	= blackScholesModel.getModel().getVolatility();

		double optionMaturity	= 5.0;

		// Test options with different strike
		System.out.println("Calculation of Option Rho (European options with maturity " + optionMaturity + "):");
		System.out.println(" Strike \t MC Fin.Diff.\t MC Pathwise\t MC Likelihood\t Analytic \t Diff MC-FD \t Diff MC-PW \t Diff MC-LR");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			double shift = riskFreeRate * 1E-6;

			Map<String,Object> dataUpShift = new HashMap<String,Object>();
			dataUpShift.put("riskFreeRate", riskFreeRate + shift);
			double valueUpShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataUpShift).get("value"));

			Map<String,Object> dataDownShift = new HashMap<String,Object>();
			dataDownShift.put("riskFreeRate", riskFreeRate - shift);
			double valueDownShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			double rho = (valueUpShift-valueDownShift) / ( 2 * shift );

			// Calculate the finite difference of the analytic value
			double rhoFiniteDiffAnalytic	=
					(
							net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate+shift, volatility, optionMaturity, optionStrike)
							- net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate-shift, volatility, optionMaturity, optionStrike)
							)/(2*shift);

			// Calculate the analytic value
			double rhoAnalytic	= net.finmath.functions.AnalyticFormulas.blackScholesOptionRho(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Calculate the value using pathwise differentiation
			EuropeanOptionRhoPathwise		callOptionRhoPathwise		= new EuropeanOptionRhoPathwise(optionMaturity, optionStrike);
			double							rhoPathwise				= callOptionRhoPathwise.getValue(blackScholesModel);

			// Calculate the value using likelihood differentiation
			EuropeanOptionRhoLikelihood		callOptionRhoLikelihood		= new EuropeanOptionRhoLikelihood(optionMaturity, optionStrike);
			double							rhoLikelihood				= callOptionRhoLikelihood.getValue(blackScholesModel);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) + 
					"\t" + numberFormatValue.format(rho) +
					"\t" + numberFormatValue.format(rhoPathwise) +
					"\t" + numberFormatValue.format(rhoLikelihood) +
					"\t" + numberFormatValue.format(rhoAnalytic) +
					"\t" + numberFormatDeviation.format(rho-rhoAnalytic) +
					"\t" + numberFormatDeviation.format(rhoPathwise-rhoAnalytic) +
					"\t" + numberFormatDeviation.format(rhoLikelihood-rhoAnalytic));

		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testEuropeanCallGamma() throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = (MonteCarloBlackScholesModel)model;

		// Java DecimalFormat for our output format
		DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		double initialValue	= blackScholesModel.getAssetValue(0.0, 0).get(0);
		double riskFreeRate	= blackScholesModel.getModel().getRiskFreeRate();
		double volatility	= blackScholesModel.getModel().getVolatility();

		// Test options with different strike
		System.out.println("Calculation of Option Gamma (European options with maturity 1.0):");
		System.out.println(" Strike \t MC Fin.Diff.\t MC Pathwise\t MC Likelihood\t Analytic \t Diff MC-FD \t Diff MC-PW \t Diff MC-LR");

		double optionMaturity	= 5.0;
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			double value	= callOption.getValue(blackScholesModel);

			// For gamma the shift has to be comparably large (otherwise FD of MC is very noisy). Try it.
			double shift = initialValue * 1E-2;

			Map<String,Object> dataUpShift = new HashMap<String,Object>();
			dataUpShift.put("initialValue", initialValue + shift);
			double valueUpShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataUpShift).get("value"));

			Map<String,Object> dataDownShift = new HashMap<String,Object>();
			dataDownShift.put("initialValue", initialValue - shift);
			double valueDownShift	= (Double)(callOption.getValuesForModifiedData(blackScholesModel, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			double gamma = (valueUpShift-2*value+valueDownShift) / (shift * shift);

			// Calculate the finite difference of the analytic value
			double gammaFiniteDiffAnalytic	=
					(
							net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue+shift, riskFreeRate, volatility, optionMaturity, optionStrike)
							- 2 * net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike)
							+ net.finmath.functions.AnalyticFormulas.blackScholesOptionValue(initialValue-shift, riskFreeRate, volatility, optionMaturity, optionStrike)
							) / (shift * shift);

			// Calculate the analytic value
			double gammaAnalytic	= net.finmath.functions.AnalyticFormulas.blackScholesOptionGamma(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Calculate the value using pathwise differentiation
			EuropeanOptionGammaPathwise		callOptionGammaPathwise		= new EuropeanOptionGammaPathwise(optionMaturity, optionStrike);
			double							gammaPathwise				= callOptionGammaPathwise.getValue(blackScholesModel);

			// Calculate the value using likelihood differentiation
			EuropeanOptionGammaLikelihood		callOptionGammaLikelihood	= new EuropeanOptionGammaLikelihood(optionMaturity, optionStrike);
			double								gammaLikelihood				= callOptionGammaLikelihood.getValue(blackScholesModel);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) + 
					"\t" + numberFormatValue.format(gamma) +
					"\t" + numberFormatValue.format(gammaPathwise) +
					"\t" + numberFormatValue.format(gammaLikelihood) +
					"\t" + numberFormatValue.format(gammaAnalytic) +
					"\t" + numberFormatDeviation.format(gamma-gammaAnalytic) +
					"\t" + numberFormatDeviation.format(gammaPathwise-gammaAnalytic) +
					"\t" + numberFormatDeviation.format(gammaLikelihood-gammaAnalytic));

		}
		System.out.println("__________________________________________________________________________________________\n");
	}
}
