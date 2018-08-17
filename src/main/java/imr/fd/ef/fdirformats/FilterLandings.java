/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import LandingsTypes.v1.LandingsdataType;

/**
 * Example of stream-filters for landings. These save considerable parsing time, compared to in-memory processing for large data sets.
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class FilterLandings {

    /**
     *
     * @param species_codes_retained
     * @return
     */
    public LandingsdataType readLandingsSpecies(String[] species_codes_retained) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * Read only selected tags
     */
    public LandingsdataType readBasic(String[] elements_excluded) {
        throw new UnsupportedOperationException("NotImplemented");
    }

    /**
     * Do not read explanatory text fields that are redundant to codes.
     */
    public LandingsdataType readCodesOnly() {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
