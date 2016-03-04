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

package fr.immotronic.ubikit.placetouch.system.impl;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import fr.immotronic.ubikit.placetouch.system.SystemEvent.Severity;

public class BatteryObserver
{
	private final Pin shutdownPin = RaspiPin.GPIO_10; // This corresponds to gpio8 on raspberry pi.
	private GpioController gpioController;
	private GpioPinDigitalInput shutdownGpio;
	private long lastPinChange = 0;
	
	public BatteryObserver(final SystemEventObserverImpl systemEventObserver) throws LinkageError
	{
		gpioController = GpioFactory.getInstance();
		shutdownGpio = gpioController.provisionDigitalInputPin(shutdownPin);
		
		shutdownGpio.addListener(new GpioPinListenerDigital() {
			
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
			{
				long now = System.currentTimeMillis();
				
				// If the last pin change was less than 400 ms ago, we ignore it.
				if (now - lastPinChange > 400 /*ms*/)
				{
					if (event.getState() == PinState.LOW)
						systemEventObserver.logSystemEvent("Power lost, running on battery.", Severity.HIGH);
					else if (event.getState() == PinState.HIGH)
						systemEventObserver.logSystemEvent("Power did come back.", Severity.NORMAL);
				}
				lastPinChange = now;
			}
			
		});
	}
	
	public void stop()
	{
		if (shutdownGpio != null)
			shutdownGpio.removeAllListeners();
		
		shutdownGpio = null;
		gpioController = null;
	}

}
