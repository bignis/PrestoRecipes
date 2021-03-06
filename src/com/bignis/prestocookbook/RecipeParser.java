package com.bignis.prestocookbook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.Adler32;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RecipeParser {
	
	/*
<recipeml version="0.5">
  <recipe>
    <head>
      <title>2 Minute Chocolate Cake</title>
      <categories>
        <cat>Cake</cat>
      </categories>
      <yield>8</yield>
    </head>
    <description>Mgn <!ELEMENT recipe (head, description*, equipment?, ingredients, directions,
  nutrition?, diet-exchanges?)></description>
    <ingredients>
      <ing>
        <amt>
          <qty>2</qty>
          <unit>tablespoons</unit>
        </amt>
        <item>(level) cocoa</item>
      </ing>
      <ing>
        <amt>
          <qty>4</qty>
          <unit>ounces</unit>
        </amt>
        <item>Juice</item>
      </ing>
      <ing>
        <amt>
          <qty>4</qty>
          <unit>ounces</unit>
        </amt>
        <item>Castor sugar (granulated)</item>
      </ing>
      <ing>
        <amt>
          <qty />
          <unit>
          </unit>
        </amt>
        <item>Salt</item>
      </ing>
      <ing>
        <amt>
          <qty>1/2</qty>
          <unit>cups</unit>
        </amt>
        <item>Milk</item>
      </ing>
      <ing>
        <amt>
          <qty>1</qty>
          <unit />
        </amt>
        <item>Egg</item>
      </ing>
      <ing>
        <amt>
          <qty />
          <unit />
        </amt>
        <item>A little vanilla essence</item>
      </ing>
    </ingredients>
    <directions>
      <step>  From: hz225wu@unidui.uni-duisburg.de (Micaela Pantke)
  
  Date: Thu, 12 Aug 93 09:26:15 +0200
  
  From: ynnuf@yetti.amigans.gen.nz (Doreen Randal)
  
  Place all ingredients into a basin in the above order. Beat well for 2
  minutes. Pour into a greased 7" cake tin. Bake for 35-40 minutes in
  moderate oven.
  
  REC.FOOD.RECIPES ARCHIVES
  
  From rec.food.cooking archives.  Downloaded from Glen's MM Recipe Archive,
  http://www.erols.com/hosey.
 
</step>
      <step>Enjoy it dude!</step>
    </directions>
  </recipe>
</recipeml>
	 */
	
	public static Recipe ParseFromXmlFile(String xmlFilePath) throws Exception
	{
		if (xmlFilePath == null || xmlFilePath.length() == 0)
    	{
    		throw new RuntimeException("ParseFromXmlFile called with null parameter");
    	}
    	/* old, fails on UTF stuff mgn
    	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    	builderFactory.setNamespaceAware(true);
    	DocumentBuilder builder = builderFactory.newDocumentBuilder();
    	
    	// If you get a "Unexpected token (position:TEXT ﻿@1:2 in java.io.StringReader@41182568)"
    	// error, make sure the "UTF-8" in the header of the XML file is uppercase!
    	
    	//Document document = builder.parse(new File(xmlFilePath));
    	Document document = builder.parse(new InputSource(new InputStreamReader(
                new FileInputStream(xmlFilePath), "UTF-8")));
    	*/

        String xml = RecipesLoader.getStringFromFile(new File(xmlFilePath));

        return ParseFromXmlString(xml);
	}
	
	public static Recipe ParseFromXmlString(String xml) throws Exception
	{
		if (xml == null || xml.length() == 0)
    	{
    		throw new RuntimeException("ParseFromXmlString called with null parameter");
    	}
    	
    	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    	builderFactory.setNamespaceAware(true);
    	DocumentBuilder builder = builderFactory.newDocumentBuilder();
    	
    	// If you get a "Unexpected token (position:TEXT ﻿@1:2 in java.io.StringReader@41182568)"
    	// error, make sure the "UTF-8" in the header of the XML file is uppercase!
    	//Document document = builder.parse(new InputSource(new StringReader(xml)));
    	Document document = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        Recipe recipe = Parse(document);

        recipe.XmlHash = GetHash(xml); // Needs the actual string for this

    	return recipe;
	}
	
	private static Recipe Parse(Document document) throws XPathExpressionException
	{
		if (document == null)
		{
			throw new RuntimeException("document is null");
		}
		
		Recipe recipe = new Recipe();

		XPath xpath = XPathFactory.newInstance().newXPath();
		
		recipe.Title = xpath.evaluate("/recipeml/recipe/head/title", document);
    	
    	if (recipe.Title == null || recipe.Title.length() == 0)
    	{
    		throw new RuntimeException("Recipe title not found in xml content");
    	}

        recipe.Description = xpath.evaluate("/recipeml/recipe/head/description", document);
        recipe.Source = xpath.evaluate("/recipeml/recipe/head/source", document);
        recipe.Yield = xpath.evaluate("/recipeml/recipe/head/yield", document);
    	
    	recipe.Category = xpath.evaluate("/recipeml/recipe/head/categories/cat", document);
    		
    	NodeList ingredientNodes = (NodeList) xpath.evaluate("/recipeml/recipe/ingredients/ing", document, XPathConstants.NODESET);
    	
    	for (int i = 0; i < ingredientNodes.getLength(); i++) {
    		Element element = (Element)ingredientNodes.item(i);
    		Element amount = (Element) element.getElementsByTagName("amt").item(0);
    		
    		String combined = "";

            if (amount != null) {
                NodeList quantityNodes = amount.getElementsByTagName("qty");

                if (quantityNodes.getLength() != 0) {
                    combined += quantityNodes.item(0).getTextContent().trim();
                }

                NodeList unitNodes = amount.getElementsByTagName("unit");

                if (unitNodes.getLength() != 0) {
                    String unitText = unitNodes.item(0).getTextContent().trim();

                    if (combined.length() != 0 && unitText.length() != 0) {
                        combined += " ";
                    }

                    combined += unitText;
                }
            }
    		
    		// Assume item is non-null, otherwise the recipe is borked
    		NodeList itemNodes = element.getElementsByTagName("item");
    		
    		if (itemNodes.getLength() == 0)
    		{
    			throw new RuntimeException("An ingredient Item was null, the RecipeML spec does not allow this");
    		}
    		
    		String itemText = itemNodes.item(0).getTextContent().trim();
    		
    		if (combined.length() != 0 && itemText.length() != 0)
			{
				combined += " ";
			}
    		
            combined += itemText;
            			
            recipe.Ingredients.add(combined);
        }
    	
    	NodeList stepNodes = (NodeList) xpath.evaluate("/recipeml/recipe/directions/step", document, XPathConstants.NODESET);
    	
    	for (int i = 0; i < stepNodes.getLength(); i++) {
            recipe.Steps.add(stepNodes.item(i).getTextContent().trim());
        }
    	
    	//recipe.Title = nodes.item(0).getTextContent();
    	
    	
    
    	return recipe;
	}

    private static long GetHash(String input)
    {
        input = input.trim();

        Adler32 adler = new Adler32();

        adler.update(input.getBytes());

        return adler.getValue();
    }
	
	static String join(Collection<?> s, String delimiter) {
	     StringBuilder builder = new StringBuilder();
	     Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) {
	         builder.append(iter.next());
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }
}
