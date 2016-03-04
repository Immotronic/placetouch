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

import fr.immotronic.ubikit.placetouch.cloud.NetworkManagerObserver;
import org.ubikit.Logger;

public class LEDsManagerImpl implements NetworkManagerObserver 
{
	private final LEDsController ledsController;
	
	private final LEDsController.LEDBlinker led2blinkerRedGreen;
	private final LEDsController.LEDBlinker led2blinkerGreenOff;
	private final LEDsController.LEDBlinker led1blinkerGreenOff;
	private final LEDsController.LEDBlinker led1blinkerRedOff;
	private final LEDsController.LEDBlinker led1blinkerOrangeOff;
	private final LEDsController.LEDBlinker led1blinkerGreenRed;
	private final LEDsController.LEDBlinker led1blinkerGreenOrange;
	private final LEDsController.LEDBlinker led1blinkerGreenOrangeRed;
	
	private int distantAccess = -1; // -1=n/a, 0: no distant access, 1: distant access ok 
	private int appstore = -1; // -1=n/a, 0: no appstore, 1: appstore ok
	private boolean internet = false; // false: no internet, true: internet ok
	
	public LEDsManagerImpl(LEDsController ledsController)
	{
		this.ledsController = ledsController;
		ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.OFF);
		ledsController.set(LEDsController.LED.LED_2, LEDsController.Color.ORANGE);
		
		LEDsController.LEDBlinkerState[] RedGreenStatesLED2 = {
				ledsController.createNewBlinkerState(LEDsController.Color.RED, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1)
		};
		led2blinkerRedGreen = ledsController.createNewBlinker(LEDsController.LED.LED_2, RedGreenStatesLED2, 250);
		
		LEDsController.LEDBlinkerState[] GreenOffStatesLED2 = {
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.OFF, 3)
		};
		led2blinkerGreenOff = ledsController.createNewBlinker(LEDsController.LED.LED_2, GreenOffStatesLED2, 1000);
		
		LEDsController.LEDBlinkerState[] GreenOffStatesLED1 = {
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.OFF, 1)
		};
		led1blinkerGreenOff = ledsController.createNewBlinker(LEDsController.LED.LED_1, GreenOffStatesLED1, 250);
		
		LEDsController.LEDBlinkerState[] OrangeOffStates = {
				ledsController.createNewBlinkerState(LEDsController.Color.ORANGE, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.OFF, 1)
		};	
		led1blinkerOrangeOff = ledsController.createNewBlinker(LEDsController.LED.LED_1, OrangeOffStates, 250);
		
		LEDsController.LEDBlinkerState[] RedOffStates = {
				ledsController.createNewBlinkerState(LEDsController.Color.RED, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.OFF, 1)
		};	
		led1blinkerRedOff = ledsController.createNewBlinker(LEDsController.LED.LED_1, RedOffStates, 250);
		
		LEDsController.LEDBlinkerState[] GreenRedStates = {
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.RED, 1)
		};
		led1blinkerGreenRed = ledsController.createNewBlinker(LEDsController.LED.LED_1, GreenRedStates, 350);
		
		LEDsController.LEDBlinkerState[] GreenOrangeStates = {
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.ORANGE, 1)
		};
		led1blinkerGreenOrange = ledsController.createNewBlinker(LEDsController.LED.LED_1, GreenOrangeStates, 350);
		
		LEDsController.LEDBlinkerState[] GreenOrangeRedStates = {
				ledsController.createNewBlinkerState(LEDsController.Color.GREEN, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.ORANGE, 1),
				ledsController.createNewBlinkerState(LEDsController.Color.RED, 1)
		};
		led1blinkerGreenOrangeRed = ledsController.createNewBlinker(LEDsController.LED.LED_1, GreenOrangeRedStates, 350);
	}
	
	public void showHardwareLinkStatus(boolean status)
	{
		if(status) {
			ledsController.animate(led2blinkerGreenOff);
		}
		else {
			ledsController.animate(led2blinkerRedGreen);
		}
	}
	
	public void placetouchDidStarted()
	{
		Logger.debug(LC.gi(), this, "placetouchDidStarted()");
		ledsController.animate(led2blinkerGreenOff);
	}

	@Override
	public void noNetwork() 
	{
		Logger.debug(LC.gi(), this, "ethernetCableIsUnplugged()");
		ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.RED);
	}

	@Override
	public void noIPAddressAssigned() 
	{
		Logger.debug(LC.gi(), this, "noIPAddressAssigned()");
		ledsController.animate(led1blinkerRedOff);
	}

	@Override
	public void localNetworkIsUp()
	{
		Logger.debug(LC.gi(), this, "localNetworkIsUp()");
		ledsController.animate(led1blinkerOrangeOff);
	}
	
	@Override
	public void internetIsReachable() 
	{
		Logger.debug(LC.gi(), this, "internetIsReachable()");
		internet = true;
		ledsController.animate(led1blinkerGreenOff);
	}

	@Override
	public void noInternetAccess() 
	{
		Logger.debug(LC.gi(), this, "noInternetAccess()");
		internet = false;
		/*if(appstore == 0) {
			ledsController.animate(led1blinkerOrangeOff);
			return;
		}*/
		
		ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.ORANGE);
	}

	@Override
	public void distantAccessIsAvailable() 
	{
		Logger.debug(LC.gi(), this, "distantAccessIsAvailable()");
		distantAccess = 1;
		if(appstore == 0) {
			ledsController.animate(led1blinkerGreenOrange);
			return;
		}
		
		ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.GREEN);
	}

	@Override
	public void noDistantAccess() 
	{
		Logger.debug(LC.gi(), this, "noDistantAccess()");
		distantAccess = 0;
		if(appstore == 0) {
			ledsController.animate(led1blinkerGreenOrangeRed);
			return;
		}
		
		ledsController.animate(led1blinkerGreenRed);
		Logger.info(LC.gi(), this, "LED BLINK GREEN RED because of noDistantAccess()");
	}
	
	@Override
	public void networkIsUp()
	{
		Logger.debug(LC.gi(), this, "networkIsUp()");
		distantAccess = -1;
		
		if(appstore == 0) {
			ledsController.animate(led1blinkerGreenOrange);
			return;
		}
		
		ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.GREEN);
	}
	
	@Override
	public void noAppstore()
	{
		Logger.debug(LC.gi(), this, "noAppstore()");
		appstore = 0;
		if(!internet) {
			//ledsController.animate(led1blinkerOrangeOff);
			ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.ORANGE);
		}
		else if(distantAccess == 0) {
			ledsController.animate(led1blinkerGreenOrangeRed);
		}
		else {
			ledsController.animate(led1blinkerGreenOrange);
		}
	}
	
	@Override
	public void appstoreConnected()
	{
		Logger.debug(LC.gi(), this, "appstoreConnected()");
		appstore = 1;
		if(!internet) {
			ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.ORANGE);
		}
		else if(distantAccess == 0) {
			ledsController.animate(led1blinkerGreenRed);
			Logger.info(LC.gi(), this, "LED BLINK GREEN RED because of appstoreConnected()");
		}
		else {
			ledsController.set(LEDsController.LED.LED_1, LEDsController.Color.GREEN);
		}
	}
}
