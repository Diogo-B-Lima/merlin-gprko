package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

			String[] header = {"Module Name", "Module", "Reaction", "Protein", "Genes", "Definition"};
			ExcelWriter.main(header, gprResults, args[1]);

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

								for(ModuleCI module : geneAssociation.getModules().values()) {

									String[] koGeneResults = new String[6];

									String definition = module.getDefinition();

									koGeneResults[0] = module.getName();
									koGeneResults[1] = module.getModule();
									koGeneResults[2] = reaction;
									koGeneResults[3] = protein;

									String genesAsStr = "";
									for(int geneIndex = 0; geneIndex < gene.size() ; geneIndex++)
										if(geneIndex != gene.size() -1)
											genesAsStr = genesAsStr + gene.get(geneIndex) + ",";
										else
											genesAsStr = genesAsStr + gene.get(geneIndex);

									koGeneResults[4] = genesAsStr;

									koGeneResults[5] = definition;

									results.add(koGeneResults);
								}
							}
						}
					}
			}
		}
		return results;

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

	private static List<ReactionProteinGeneAssociation> verifyModules(List<String> modules, String reaction, String ecNumber, String ortholog) throws Exception{

		List<ReactionProteinGeneAssociation> gpr_list = new ArrayList<>();

		ReactionProteinGeneAssociation gpr = GPR_KO.verifyModule(modules, reaction, ecNumber, ortholog);

		if(gpr!=null)
			gpr_list.add(gpr);

		return gpr_list;
	}


	private static ReactionProteinGeneAssociation verifyModule(List<String> modules, String reaction, String ec_number, String ortholog) throws Exception {

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

			String express = definition.get(index).toString().replaceAll(",", " or ").replaceAll("\\+", " and ").replaceAll("\\[", "").replaceAll("\\]", "");

			String rule = express.trim();
			Set<String> geneList = new HashSet<>();


			if(rule.contains(" or ")) {
				String[] or_rules = rule.split(" or ");
				for(String gene : or_rules)
					if(gene.trim().contains(ortholog))
						geneList.add(gene);
			}

			if(rule.contains(" and ")) {

				String[] and_rules = rule.split(" and ");

				for(String gene : and_rules)
					if(gene.trim().contains(ortholog))
						geneList.add(gene);
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
