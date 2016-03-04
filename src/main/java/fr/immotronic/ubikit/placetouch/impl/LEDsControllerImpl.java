/*
 *
 * Copyright (c) Immotronic, 2012
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of placetouch, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.placetouch.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.ubikit.Logger;

public final class LEDsControllerImpl implements LEDsController 
{	
	private final String off = "0";
	private final String on = "1";
	private final FileWriter[] leds;
	private final ScheduledFuture<?>[] flasherScheduledFuture = new ScheduledFuture<?>[2];
	
	private ScheduledExecutorService executor;
	
	private final class LEDBlinkerStateImpl implements LEDBlinkerState
	{
		private final Color color;
		private final int durationInNumberOfSteps;
		private int stepCount = 0;
		
		public LEDBlinkerStateImpl(LEDsController.Color color, int durationInNumberOfSteps)
		{
			this.color = color;
			this.durationInNumberOfSteps = durationInNumberOfSteps;
		}
		
		public Color getColor()
		{
			return color;
		}
		
		public boolean isTimeToNextState()
		{
			stepCount = (stepCount + 1) % durationInNumberOfSteps;
			return (stepCount == 0);
		}
	}
	
	private final class LEDAdvancedBlinker implements Runnable, LEDBlinker
	{
		private final LEDsController.LED led;
		private final LEDsController.LEDBlinkerState[] states;
		private final int stateCardinality;
		private final int stepTimeInMilliseconds;
		private int previousState = -1;
		private int state = 0;
		
		public LEDAdvancedBlinker(LEDsController.LED led, LEDsController.LEDBlinkerState[] states, int stepTimeInMilliseconds)
		{
			this.led = led;
			this.states = states;
			this.stepTimeInMilliseconds = stepTimeInMilliseconds;
			
			if(states == null) {
				stateCardinality = 0;
			}
			else {
				stateCardinality = states.length;
			}
		}
		
		public int getStepTimeInMilliseconds()
		{
			return stepTimeInMilliseconds;
		}
		
		public LEDsController.LED getLED()
		{
			return led;
		}
		
		@Override
		public void run()
		{
			if(stateCardinality == 0) {
				return;
			}
			
			LEDsController.LEDBlinkerState currentState = states[state];
			if(currentState != null && previousState != state)
			{
				turnLED(led, currentState.getColor());
				previousState = state;
			}
			
			if(currentState == null || currentState.isTimeToNextState()) {
				state = (state + 1) % stateCardinality;
			}
		}
	};
	
	public LEDsControllerImpl(BundleContext bundleContext)
	{
		this.executor = null;
		leds = new FileWriter[4];
		
		String led1_green = bundleContext.getProperty("fr.immotronic.placetouch.led1.green");
		String led1_red = bundleContext.getProperty("fr.immotronic.placetouch.led1.red");
		String led2_green = bundleContext.getProperty("fr.immotronic.placetouch.led2.green");
		String led2_red = bundleContext.getProperty("fr.immotronic.placetouch.led2.red");
		
		try 
		{
			leds[0] = new FileWriter(led1_green);
			leds[1] = new FileWriter(led1_red);
			leds[2] = new FileWriter(led2_green);
			leds[3] = new FileWriter(led2_red);
		}
		catch (Exception e) 
		{
			Logger.info(LC.gi(), this, "Cannot create links to PlaceTouch LEDs. This is expected if you are not running on PlaceTouch.");
			leds[0] = null;
			leds[1] = null;
			leds[2] = null;
			leds[3] = null;
			throw new RuntimeException("Invalid GPIO device references in the configuration file");
		}
		
		flasherScheduledFuture[0] = null;
		flasherScheduledFuture[1] = null;
	}
	
	@Override
	public void setExecutorService(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}
	
	
	
	@Override
	public void animate(LEDBlinker blinker) 
	{
		if(executor != null && blinker != null)
		{
			int ledIndex = 0;
			if(blinker.getLED() == LEDsController.LED.LED_2) {
				ledIndex = 1;
			}
			
			stopFlasherOrBlinker(ledIndex);
			flasherScheduledFuture[ledIndex] = executor.scheduleAtFixedRate(blinker, 0, blinker.getStepTimeInMilliseconds(), TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public LEDBlinker createNewBlinker(LEDsController.LED led, LEDBlinkerState[] steps, int stepTimeInMilliseconds) 
	{
		return new LEDAdvancedBlinker(led, steps, stepTimeInMilliseconds);
	}

	@Override
	public LEDBlinkerState createNewBlinkerState(Color color, int durationInNumberOfSteps) 
	{
		return new LEDBlinkerStateImpl(color, durationInNumberOfSteps);
	}
	
	@Override
	public void set(LEDsController.LED led, LEDsController.Color color) 
	{
		int ledIndex = 0;
		if(led == LEDsController.LED.LED_2) {
			ledIndex = 1;
		}
		
		stopFlasherOrBlinker(ledIndex);
		turnLED(led, color);
	}
	
	private void stopFlasherOrBlinker(int index)
	{
		if(flasherScheduledFuture[index] != null) {
			if(!flasherScheduledFuture[index].cancel(true)) {
				Logger.error(LC.gi(), this, "Cannot cancel a flashing LED: index="+index);
			}
			
			flasherScheduledFuture[index] = null;
		}
	}
	
	private void turnLED(LEDsController.LED led, LEDsController.Color color) 
	{
		int green = 0;
		int red = 1; 
		
		if(led == LEDsController.LED.LED_2) {
			green = 2;
			red = 3;
		}
		
		try
		{
			if(color == LEDsController.Color.OFF || color == LEDsController.Color.RED) {
				leds[green].write(off, 0, 1);
			}
			else {
				leds[green].write(on, 0, 1);
			}
			
			if(color == LEDsController.Color.OFF || color == LEDsController.Color.GREEN) {
				leds[red].write(off, 0, 1);
			}
			else {
				leds[red].write(on, 0, 1);
			}
			
			leds[green].flush();
			leds[red].flush();
		}
		catch (IOException e) 
		{
			Logger.error(LC.gi(), this, "Cannot turn PlaceTouch "+led.name()+" "+color.name(), e);
		}
		catch(NullPointerException e)
		{ 
			Logger.error(LC.gi(), this, "Cannot turn PlaceTouch "+led.name()+" "+color.name()+" because of null");
		}
		catch(Exception e)
		{
			Logger.error(LC.gi(), this, "Cannot turn PlaceTouch "+led.name()+" "+color.name(), e);
		}
	}
}
