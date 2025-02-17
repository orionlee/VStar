/**
 * VStar: a statistical analysis tool for variable star data.
 * Copyright (C) 2010  AAVSO (http://www.aavso.org/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package org.aavso.tools.vstar.external.plugin;

import java.util.List;
import org.aavso.tools.vstar.data.ValidObservation;
import org.aavso.tools.vstar.data.ValidObservation.JDflavour;
import org.aavso.tools.vstar.plugin.ObservationToolPluginBase;
import org.aavso.tools.vstar.ui.dialog.MessageBox;
import org.aavso.tools.vstar.ui.mediator.AnalysisType;
import org.aavso.tools.vstar.ui.mediator.Mediator;
import org.aavso.tools.vstar.ui.mediator.StarInfo;
import org.aavso.tools.vstar.ui.mediator.message.NewStarMessage;
import org.aavso.tools.vstar.ui.model.plot.ISeriesInfoProvider;
import org.aavso.tools.vstar.util.Pair;
import org.aavso.tools.vstar.util.coords.DecInfo;
import org.aavso.tools.vstar.util.coords.RAInfo;

/**
 * Converts currently loaded observations to HJD if they are not already
 * Heliocentric.
 * 
 * TODO:<br/>
 * - undoable edits!
 * 
 * PMAK (2021-09-29):
 * - Plug-in was simplified because currently there is no way to 
 * 		distinguish JD/HJD/BJD observations by loading methods and/or by series.
 */
public class HJDConverter extends ObservationToolPluginBase {

	@Override
	public String getDisplayName() {
		return "Heliocentric JD Converter";
	}

	@Override
	public String getDescription() {
		return "Heliocentric JD Converter";
	}

	@Override
	public void invoke(ISeriesInfoProvider seriesInfo) {
		// The most recent new star message will have all obs and series
		// (whether a single or additive dataset).
		// [comment from InfoDialog.java]
		NewStarMessage msg = Mediator.getInstance().getLatestNewStarMessage();
		if (msg != null) {
			List<ValidObservation> obs = msg.getObservations();
			int count = 0;			
			for (ValidObservation ob : obs) {
				if (ob.getJDflavour() == JDflavour.JD) {
					count++;
				}
			}
			if (count == 0) {
				MessageBox.showMessageDialog("Non-Heliocentric Observations",
						"No observations with Julian Date");
				return;
			}
			if (!MessageBox.showConfirmDialog("Non-Heliocentric Observations", count + " Julian Date observations found. Convert them to HJD?"))
				return;
			Pair<RAInfo, DecInfo> coords = getCoordinates(msg.getStarInfo());
			if (coords != null) {
				count = Mediator.getInstance().convertObsToHJD(obs, coords.first, coords.second);
				if (count != 0) {				
					updateUI();
					MessageBox.showMessageDialog("HJD Conversion",
							String.format("%d observations converted.", count));
				} else {
					// We should never be here
					MessageBox.showWarningDialog("HJD Conversion",
							"The previously loaded observations have NOT been converted to HJD.");
					
				}
			} else {
				MessageBox.showWarningDialog("HJD Conversion",
						"Canceled by user: the previously loaded observations have NOT been converted to HJD.");
			}
		}
	}

	/**
	 * Return RA and Dec. First look for coordinates in any of our loaded
	 * datasets. Use the first coordinates found. We are making the simplifying
	 * assumption that all data sets correspond to the same object! If not
	 * found, ask the user to enter them. If none are supplied, null is
	 * returned.
	 * 
	 * @param info
	 *            a StarInfo object possibly containing coordinates
	 * @param otherCoords
	 *            Coordinates to use if info contains none.
	 * @return A pair of coordinates: RA and Declination
	 */
	private Pair<RAInfo, DecInfo> getCoordinates(StarInfo info) {
		RAInfo ra = info.getRA();
		DecInfo dec = info.getDec();
		Pair<RAInfo, DecInfo> coords = null;

		if (ra == null || dec == null) {
			// Ask the user for J2000.0 RA/DEC and if that is cancelled,
			// indicate that HJD conversion cannot take place.
			ra = Mediator.getInstance().requestRA();
			if (ra != null)
				dec = Mediator.getInstance().requestDec();
		}

		if (ra != null && dec != null) {
			coords = new Pair<RAInfo, DecInfo>(ra, dec);
		}		return coords;
	}
	
	/**
	 * Update UI
	 */
	private void updateUI() {
		
		// PMAK (2021-06-03):
		// There is no way to recalculate observation phases (as for VStar 2.21.3)
		// So we are switching to RAW plot and trying to delete existing phase plot.
		
		Mediator mediator = Mediator.getInstance();
		
		mediator.changeAnalysisType(AnalysisType.RAW_DATA);

		try {
			mediator.dropPhasePlotAnalysis();
		} catch (Exception e) {
			MessageBox.showWarningDialog("HJD Conversion", 
				"Cannot delete current Phase Plot. Please recreate it to reflect changes.");
		}
	
		// Updates RAW plot and data table.
		Mediator.getInstance().updatePlotsAndTables();
		
	}
	
}
