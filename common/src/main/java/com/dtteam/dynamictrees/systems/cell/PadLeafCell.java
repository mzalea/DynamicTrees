package com.dtteam.dynamictrees.systems.cell;

/**
 * Leaf cell for flat crown pads: hydration spreads sideways in full, rises one thin layer from the strongest
 * leaves only, and never runs down, so each twig carries a wide flat pad with open air under it.
 */
public class PadLeafCell extends MatrixCell {

    public PadLeafCell(int value) {
        super(value, valMap);
    }

    static final byte[] valMap = {
            0, 0, 0, 0, 0, 0, 0, 0, //D Maps * -> 0
            0, 0, 0, 0, 3, 3, 3, 3, //U Maps 4+ -> 3, * -> 0
            0, 1, 2, 3, 4, 5, 6, 7, //N Maps * -> *
            0, 1, 2, 3, 4, 5, 6, 7, //S Maps * -> *
            0, 1, 2, 3, 4, 5, 6, 7, //W Maps * -> *
            0, 1, 2, 3, 4, 5, 6, 7  //E Maps * -> *
    };

}
