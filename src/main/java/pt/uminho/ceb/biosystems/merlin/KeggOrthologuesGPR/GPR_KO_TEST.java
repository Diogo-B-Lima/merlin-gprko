package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class GPR_KO_TEST {


	@Test
	public void runGPR_KO() {

		try {

			String inputFile = "C://Users//diogo//Desktop//kos//kos.txt";
			String outputFilePath = "C://Users//diogo//Desktop//kos//kos.xlsx";

			String inputFile2 = "C://Users//diogo//Desktop//kos//kos2.txt";
			String outputFilePath2 = "C://Users//diogo//Desktop//kos//kos2.xlsx";

			String inputFile3 = "C://Users//diogo//Desktop//kos//kos3.txt";
			String outputFilePath3 = "C://Users//diogo//Desktop//kos//kos3.xlsx";

			String inputFileDebug = "C://Users//diogo//Desktop//kos//kosDebug.txt";
			String outputFilePathDebug = "C://Users//diogo//Desktop//kos//kosDebug.xlsx";

			String[] input = new String[2];
			input[0] = inputFileDebug;
			input[1] = outputFilePathDebug;
			GPR_KO.main(input);
			//GPR_KO.main(inputFile2, outputFilePath2);
			//GPR_KO.main(inputFile3, outputFilePath3);



		} catch (Exception e) {
			e.printStackTrace();
		}
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



