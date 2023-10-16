package com.mk.garaze.model;

// ciselnik na typ nebytoveho priestoru
public class TitleDeed /* list vlastnictva */ {

	// LV - 453
	private String title;

	// vchod
	// supisne cislo
	// druh nebytoveho priestoru (2 - garaz, 12 - iny nebytovy priestor)
	// vlastnik => rozbit na meno a datum narodenia..ak je vlastnikov viac, tak one to many..v excely sa zvoli len jeden z nich potom pre generovanie listu
	// 

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
