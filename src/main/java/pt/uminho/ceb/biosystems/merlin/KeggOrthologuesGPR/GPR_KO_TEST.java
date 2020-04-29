package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class GPR_KO_TEST {


	@Test
	public void runGPR_KO() {

		try {
			
			String inputFileDebug = "C://Users//diogo//Desktop//kos//kosFinal.txt";
			String outputFilePathDebug = "C://Users//diogo//Desktop//kos//kosFinal.xlsx";

			String[] input = new String[2];
			input[0] = inputFileDebug;
			input[1] = outputFilePathDebug;
			GPR_KO.main(input);
			//GPR_KO.main(inputFile2, outputFilePath2);
			//GPR_KO.main(inputFile3, outputFilePath3);
			//mergeOrRules("[K01692,K07511,K13767,K01825,K01782,K07514,K07515,K10527],[K01692],[[[K1,K2,K01692]],K1,K2]");


		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	//@Test
	public void mergeOrRules(String parsedDefinitionByModule) throws Exception{

		

		//Split definition in a list of rules
		String[] def = parsedDefinitionByModule.split(",\\[");
		if(def.length > 1) {
			for(int index = 1; index < def.length; index++) {
				def[index] = "[" + def[index];
			}
		}

		ArrayList<String[]> orRules = new ArrayList<String[]>();
		String andRules = "";
		for(int index = 0; index < def.length; index++) {

			// verify if the rule is a "or" rule
			if(!def[index].startsWith("[[")) {

				String newDef = def[index].replace("[", "").replace("]", "");
				String[] newDefArray = newDef.split(",");
				orRules.add(newDefArray);
			}
			else {
				if(andRules.isEmpty())
					andRules += def[index];
				else
					andRules += "," + def[index];
			}
		}
		List<String> firstOrRule = new ArrayList<String>();
		String finalOrRules = "";

		if(orRules.size() > 1) {

			firstOrRule.addAll(Arrays.asList(orRules.get(0)));
			for(int index = 1; index < orRules.size(); index++) {
				Set<String> set = new LinkedHashSet<>(firstOrRule);
		        set.addAll(Arrays.asList(orRules.get(index)));
				firstOrRule = (new ArrayList<>(set));
			}
			finalOrRules = "[" + String.join(",", firstOrRule) + "]";

		}

		else if (orRules.size() == 1) {
			firstOrRule.addAll(Arrays.asList(orRules.get(0)));
			finalOrRules = "[" + String.join(",", firstOrRule) + "]";
		}

		if(finalOrRules.isEmpty())
			System.out.println(andRules);
		else if(!finalOrRules.isEmpty() && !andRules.isEmpty())
				andRules = "," + andRules;
		System.out.println(finalOrRules + andRules);


	}

	//@Test
	public void parseDefinition() throws Exception {

		String definition = "K00022 and  K07516 or  K01825 and  K01782 and  K07514 or  K07515 and  K10527 or K01234";

		String parsedDefinition = "";

		// definition does not have "or" nor "and", only a KO
		if(!definition.contains("and") & !definition.contains("or"))
			parsedDefinition =  "[" + definition + "]";

		// definition has only "and" rules
		if(definition.contains("and") & !definition.contains("or"))
			parsedDefinition =  "[[" + definition.replace(" and ", ",") + "]]";

		// definition has only "or" rules
		if(!definition.contains("and") & definition.contains("or"))
			parsedDefinition =  "[" + definition.replace(" or ", ",") + "]";

		// definition has a combination of "and" and "or" rules 
		if(definition.contains("and") & definition.contains("or")) {

			// remove pesky extra blanks
			ArrayList<String> definitionAsArrayFiltering = new ArrayList<String>(Arrays.asList(definition.split(" ")));
			definitionAsArrayFiltering.removeAll(Arrays.asList(""));
			String[] definitionAsArray = definitionAsArrayFiltering.toArray(new String[0]);;

			parsedDefinition = "[";
			boolean insideDoubleBracket = false;
			boolean beginDoubleBracket = false;


			// iterate over every item but stop at the second to last item, because we are always looking at the current and next item of the array
			for(int index = 0; index < definitionAsArray.length -1; index++) {

				String item = definitionAsArray[index];
				String nextItem = definitionAsArray[index + 1];


				if(!item.equalsIgnoreCase("and") && !item.equalsIgnoreCase("or")) {


					// next item is an "and" rule or it is a KO
					if(nextItem.equalsIgnoreCase("and") || !nextItem.equalsIgnoreCase("or")) {

						// if a double bracket set of "and" rules has not begun yet
						if (!beginDoubleBracket) {

							// if it is not in the beggining of the parsed definition
							if(parsedDefinition.length()>1) {

								parsedDefinition += "," + "[[" + item;
								beginDoubleBracket = true;
								insideDoubleBracket = true;

							}

							//if it is in the beggining of the parsed definition
							else {

								parsedDefinition += "[[" + item;
								beginDoubleBracket = true;
								insideDoubleBracket = true;

							}
						}
						// a double bracket set has been opened and not yet closed
						else {
							parsedDefinition += "," + item;
						}
					}
					// next item is an "or" rule
					else {

						// if we are inside a double bracket set we need to close it
						if(insideDoubleBracket) {
							parsedDefinition += "," + item + "]]";
							insideDoubleBracket = false;
							beginDoubleBracket = false;
						}

						// we are not inside a double bracket set, therefore we are just adding "or" rules
						else {
							if(parsedDefinition.length()>1)
								parsedDefinition += "," + item;
							else
								parsedDefinition += item;
						}
					}
				}

				// if index is currently the second to last item (the second to last item is always a "and" or "or" rule and the last item is always a KO
				if(index + 1 == definitionAsArray.length -1) {

					// if we are currently inside a double bracket set we are going to add the KO and close it
					if(insideDoubleBracket)
						parsedDefinition += "," + nextItem + "]]";

					// else, we are simply adding more "or" rules
					else
						parsedDefinition += "," + nextItem;						
				}
			}
			parsedDefinition += "]";
		}	
		System.out.println(parsedDefinition);
	}

}



