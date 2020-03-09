package pt.uminho.ceb.biosystems.merlin.KeggOrthologuesGPR;


public class GPR_KO_TEST {


	public void runGPR_KO() {

		try {

			String inputFile = "C://Users//diogo//Desktop//kos//kos.txt";
			String outputFilePath = "C://Users//diogo//Desktop//kos//kos.xlsx";
			
			String inputFile2 = "C://Users//diogo//Desktop//kos//kos2.txt";
			String outputFilePath2 = "C://Users//diogo//Desktop//kos//kos2.xlsx";
			
			String inputFile3 = "C://Users//diogo//Desktop//kos//kos3.txt";
			String outputFilePath3 = "C://Users//diogo//Desktop//kos//kos3.xlsx";
			
			String[] input = new String[2];
			input[0] = inputFile;
			input[1] = outputFilePath;
			GPR_KO.main(input);
			//GPR_KO.main(inputFile2, outputFilePath2);
			//GPR_KO.main(inputFile3, outputFilePath3);



		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

