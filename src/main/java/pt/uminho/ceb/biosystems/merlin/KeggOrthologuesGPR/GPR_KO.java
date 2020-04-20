package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.kegg.KeggAPI;
import pt.uminho.ceb.biosystems.merlin.core.containers.gpr.GeneAssociation;
import pt.uminho.ceb.biosystems.merlin.core.containers.gpr.ModuleCI;
import pt.uminho.ceb.biosystems.merlin.core.containers.gpr.ProteinGeneAssociation;
import pt.uminho.ceb.biosystems.merlin.core.containers.gpr.ReactionProteinGeneAssociation;
import pt.uminho.ceb.biosystems.merlin.core.containers.gpr.ReactionsGPR_CI;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.ModuleType;
import pt.uminho.ceb.biosystems.merlin.gpr.rules.core.input.KeggModulesParser;
import pt.uminho.ceb.biosystems.merlin.gpr.rules.grammar.KEGGOrthologyParser;

public class GPR_KO {

	public static void main(String[] args) {

		try {

			List<String> orthologues = readFile(args[0]);
			List<Object[]> gprResults = new ArrayList<Object[]>();

			for(String ko:orthologues) {
				List<String[]> results = runGPR_KO(ko);
				gprResults.addAll(results);
			}

			List<Object[]> gprResultsFinal = mergeParsedDefinitionsByModuleAndReaction(gprResults);
			String[] header = {"Pathway Names" ,"Pathway IDs" , "Module Name", "Module", "Reaction", "Protein", "Genes", "Definition","Parsed Definition", "Parsed Definitions Merged by Module", "Parsed Definitions Merged by Module and Reaction"};
			ExcelWriter.main(header, gprResultsFinal, args[1]);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	public static List<String> readFile(String file) throws IOException {

		String line = null;
		File input = new File(file);
		BufferedReader br = new BufferedReader(new FileReader(input));
		List<String> lines = new ArrayList<String>();
		while((line = br.readLine()) != null)
			lines.add(line.trim());
		br.close();
		return lines;

	}


	private static List<String[]> runGPR_KO(String ko) throws Exception {

		String modulesQuery = KeggAPI.getModulesStringByQuery(ko);
		Set<String> enzymes = KeggAPI.getECnumbersByOrthology(ko);
		Set<String> reactionsKO = KeggAPI.getReactionsByOrthology(ko);

		List<String> modules = GPR_KO.parseModules(modulesQuery);
		List<String[]> results = new ArrayList<String[]>();

		for(String ecNumber : enzymes) {

			Set<String> reactions = KeggAPI.getReactionsByEnzymes(ecNumber);

			if(!reactions.isEmpty()) {



				reactions.retainAll(reactionsKO);

				List<ReactionProteinGeneAssociation> rpga = null;

				for(String reaction : reactions) {

					rpga = GPR_KO.verifyModules(modules, reaction, ecNumber, ko);

				}

				if(rpga!=null)
					for(int ruleIndex = 0; ruleIndex < rpga.size() ; ruleIndex++) {

						ReactionsGPR_CI rpg = new ReactionsGPR_CI(rpga.get(ruleIndex).getReaction());

						String reaction = rpg.getReaction();

						for(ProteinGeneAssociation proteinRule : rpga.get(ruleIndex).getProteinGeneAssociation().values()) {

							String protein = proteinRule.getProtein();

							for(GeneAssociation geneAssociation : proteinRule.getGenes()) {

								List<String> gene = geneAssociation.getGenes();
								String parsedDefinitionsByModule = "";
								List<String> parsedDefinitionsByModuleAux = new ArrayList<String>();
								int moduleCounter = 0;

								for(ModuleCI module : geneAssociation.getModules().values()) {

									moduleCounter++;
									String[] koGeneResults = new String[11];

									String definition = module.getDefinition();

									koGeneResults[0] = String.join("|", module.getPathwaysNames());
									koGeneResults[1] = String.join("|", module.getPathways());
									koGeneResults[2] = module.getName();
									koGeneResults[3] = module.getModule();
									koGeneResults[4] = reaction;
									koGeneResults[5] = protein;

									String genesAsStr = "";
									for(int geneIndex = 0; geneIndex < gene.size() ; geneIndex++)
										if(geneIndex != gene.size() -1)
											genesAsStr = genesAsStr + gene.get(geneIndex) + ",";
										else
											genesAsStr = genesAsStr + gene.get(geneIndex);

									koGeneResults[6] = genesAsStr.trim();

									koGeneResults[7] = definition;

									koGeneResults[8] = parseDefinition(definition);


									char[] alphabeticallySortedDefinitionAsArray = koGeneResults[8].toCharArray();
									Arrays.sort(alphabeticallySortedDefinitionAsArray);
									String alphabeticallySortedDefinition = new String(alphabeticallySortedDefinitionAsArray);


									if(!parsedDefinitionsByModule.isEmpty()) {

										// avoid repeated definitions 
										if(!parsedDefinitionsByModuleAux.contains(alphabeticallySortedDefinition)) {

											parsedDefinitionsByModule += "," + koGeneResults[8];
											parsedDefinitionsByModuleAux.add(alphabeticallySortedDefinition);
										}
									}

									else {
										parsedDefinitionsByModule += koGeneResults[8];
										parsedDefinitionsByModuleAux.add(alphabeticallySortedDefinition);
									}
									results.add(koGeneResults);
								}

								parsedDefinitionsByModule = mergeOrRules(parsedDefinitionsByModule);

								for(int index = 0; index < moduleCounter; index++) {

									results.get(results.size() - index -1)[9] = parsedDefinitionsByModule;
								}
							}
						}
					}
			}
		}
		return results;

	}


	private static String mergeOrRules(String parsedDefinitionByModule) throws Exception{



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
			return andRules;
		else if(!finalOrRules.isEmpty() && !andRules.isEmpty())
			andRules = "," + andRules;
		return finalOrRules + andRules;

	}

	private static List<Object[]> mergeParsedDefinitionsByModuleAndReaction(List<Object[]> gprRulesFinal) throws Exception{


		HashMap<String, String> reactionsParsedDefinitions = new HashMap<String, String>();

		// Compile all definitions by reaction
		for(Object[] entry:gprRulesFinal) {
			String reaction = (String) entry[4];
			String parsedDefinition = (String) entry[8];
			if(!reactionsParsedDefinitions.containsKey(reaction))
				reactionsParsedDefinitions.put(reaction, parsedDefinition);
			else {
				String oldDefinition = reactionsParsedDefinitions.get(reaction);
				reactionsParsedDefinitions.replace(reaction, oldDefinition + "," + parsedDefinition);
			}
		}

		// Merge all definitions with the same algorithm used for merging definitions by module
		for(Map.Entry<String, String> entry : reactionsParsedDefinitions.entrySet())
			reactionsParsedDefinitions.replace(entry.getKey(), mergeOrRules(entry.getValue()));

		// Change the last value of the results (which were previosuly null) to the new merged definition
		for(Object[] entry:gprRulesFinal) 
			entry[10] = reactionsParsedDefinitions.get(entry[4]);

		return gprRulesFinal;
	}




	private static List<String> parseModules(String modules) throws Exception{

		String[] rows = modules.split("\n");
		List<String> returnModules = new ArrayList<String>();

		for(String row : rows) {

			StringTokenizer sTokmod = new StringTokenizer(row,"md:");  
			while (sTokmod.hasMoreTokens()){

				String tokmod = sTokmod.nextToken();
				Pattern patternmod = Pattern.compile("(M\\d{5})");
				Matcher matchermod = patternmod.matcher(tokmod);

				if (matchermod.find()) {

					if(!returnModules.contains(matchermod.group())) {

						returnModules.add(matchermod.group());
					}
				}
			}
		}
		return returnModules;
	}




	public static String parseDefinition(String definition) throws Exception {


		String parsedDefinition = "";

		// definition does not have "or" nor "and", only a KO
		if(!definition.contains("and") & !definition.contains("or"))
			return "[" + definition + "]";

		// definition has only "and" rules
		if(definition.contains("and") & !definition.contains("or"))
			return "[[" + definition.replace(" and ", ",").replace(" ", "") + "]]";

		// definition has only "or" rules
		if(!definition.contains("and") & definition.contains("or"))
			return "[" + definition.replace(" or ", ",").replace(" ", "") + "]";

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
		return parsedDefinition;
	}






	private static List<ReactionProteinGeneAssociation> verifyModules(List<String> modules, String reaction, String ecNumber, String ortholog) throws Exception{

		List<ReactionProteinGeneAssociation> gpr_list = new ArrayList<>();

		ReactionProteinGeneAssociation gpr = GPR_KO.verifyModule(modules, reaction, ecNumber, ortholog);

		if(gpr!=null)
			gpr_list.add(gpr);

		return gpr_list;
	}


	private static ReactionProteinGeneAssociation verifyModule(List<String> modules, String reaction, String ec_number, String ortholog) throws Exception {

		if(ortholog.equalsIgnoreCase("K01666"))
			System.out.println("here");
		ReactionProteinGeneAssociation gpr = new ReactionProteinGeneAssociation(reaction);
		ProteinGeneAssociation protein_rule = new ProteinGeneAssociation(ec_number);

		for(String module : modules) {

			//System.out.println("Module\t"+module+"\t for reaction\t"+reaction+"\t:\t"+ModuleType.Pathway);
			ModuleType moduleType = ModuleType.Pathway; 

			if(moduleType != null) {

				String s = KeggAPI.getModuleEntry(module);

				KeggModulesParser k = new KeggModulesParser(s);

				ModuleCI mic = new ModuleCI(module, moduleType);
				mic.setDefinition(k.getDefinition());
				mic.setName(k.getName());


				List<GeneAssociation> geneAssociationList = GPR_KO.getdefinition(ortholog, reaction, module, moduleType, mic);

				if(geneAssociationList!=null)
					protein_rule.addAllGeneAssociation(geneAssociationList);
			}
		}

		gpr.addProteinGeneAssociation(protein_rule);

		if(gpr.getProteinGeneAssociation().get(ec_number).getGenes().isEmpty())
			return null;

		return gpr;
	}


	private static List<GeneAssociation> getdefinition(String ortholog, String reaction, String module, ModuleType moduleType, ModuleCI mic) throws Exception {


		InputStream is = new ByteArrayInputStream(mic.getDefinition().getBytes());
		KEGGOrthologyParser parser = new KEGGOrthologyParser(is);

		List<List<String>> ret = parser.parseDefinition();

		List<GeneAssociation> geneAssociationList = null;

		if(moduleType.equals(ModuleType.Pathway))
			geneAssociationList = GPR_KO.getFinalRule(ret, reaction, mic, ortholog);

		return geneAssociationList;
	}

	private static List<List<String>> normalize(List<List<String>> lists){

		ArrayList<String> res = new ArrayList<String>();
		ArrayList<List<String>> result = new ArrayList<List<String>>();

		for(List<String> out : lists) {

			for (String s : out) {

				res.addAll(funcAux(s));
			}
			result.add(res);
			res = new ArrayList<String>();
		}
		return result;
	}

	// Auxiliary functions
	/**
	 * @param frase
	 * @return
	 */
	private static List<String> funcAux(String frase) {

		ArrayList<String> dividemais = new ArrayList<String>();
		dividemais.addAll(Arrays.asList(frase.split("\\+")));
		return dividemais;
	}

	private static List<GeneAssociation> getFinalRule(List<List<String>> definition, String reaction, ModuleCI mic, String ortholog) throws Exception {

		List<List<String>> normalizedDefinition = GPR_KO.normalize(definition);
		int index = -1;

		for(int i = 0; i<normalizedDefinition.size(); i++) {

			List<String> orthologsList = normalizedDefinition.get(i);

			if(orthologsList.contains(ortholog))
				index = normalizedDefinition.indexOf(orthologsList);
		}

		return GPR_KO.getFinalRule(index, definition, reaction, mic, ortholog);
	}

	/**
	 * @param index
	 * @param definition
	 * @param reaction
	 * @param mic
	 * @param ortholog 
	 * @return
	 * @throws Exception
	 */
	private static List<GeneAssociation> getFinalRule(int index, List<List<String>> definition, String reaction, ModuleCI mic, String ortholog) throws Exception {

		List<GeneAssociation> gene_rules = new ArrayList<>();

		if(index>-1) {

			List<String> pathways = KeggAPI.getPathwaysIDByReaction(reaction);
			List<String> pathways_module = KeggAPI.getPathwaysByModule(mic.getModule());
			pathways.retainAll(pathways_module);
			mic.setPathways(pathways_module);

			List<String> pathwayNames = new ArrayList<>();
			for(String pathwayId:pathways_module) {

				String pathName = KeggAPI.getPathwayName("map" + pathwayId);
				if(pathName != null & !pathName.isEmpty())
					pathwayNames.add(pathName);
				else
					pathwayNames.add("-");
			}

			mic.setPathwaysNames(pathwayNames);


			String express = definition.get(index).toString().replaceAll(",", " or ").replaceAll("\\+", " and ").replaceAll("\\[", "").replaceAll("\\]", "");

			String rule = express.trim();
			Set<String> geneList = new HashSet<>();


			if(rule.contains(" or ")) {
				String[] or_rules = rule.split(" or ");
				for(String gene : or_rules)
					if(gene.trim().contains(ortholog))
						geneList.add(gene.trim());
			}

			if(rule.contains(" and ")) {

				String[] and_rules = rule.split(" and ");

				for(String gene : and_rules)
					if(gene.trim().contains(ortholog))
						geneList.add(gene.trim());
			}

			if(rule.equalsIgnoreCase(ortholog) && geneList.isEmpty())
				geneList.add(ortholog);


			if(geneList.size()>0) {
				GeneAssociation gene_rule = new GeneAssociation(mic);
				mic.setDefinition(rule);
				gene_rule.addAllGenes(geneList);

				if(gene_rule.getGenes().size()>0)
					gene_rules.add(gene_rule);
			}

			return gene_rules;
		}
		return null;
	}
}
