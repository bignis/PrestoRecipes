package com.bignis.prestocookbook;

import java.util.ArrayList;

public class Recipe {
	public int Id;
	public String Title;
	public String Category;
	public String Description;
	public String Source;
	public String Yield;
	public ArrayList<String> Ingredients = new ArrayList<String>();
	public ArrayList<String> Steps = new ArrayList<String>();
	
	public Recipe()
	{
	}
}
