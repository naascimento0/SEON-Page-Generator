package seon2html.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import seon2html.model.Package;

/* Responsable for manage the parsing from XML reading to HTML writing. */
public class SeonParser {
	public static final String	PATH	= System.getProperty("user.dir");
	public static String		VERSION;
	public static boolean		STABLE;

	/* Main method. */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println("Working Path: " + PATH);
		// Reading the properties file
		Properties props = readProperties(PATH + "/parser.properties");
		String astahfile = PATH + "/" + props.getProperty("seon.file");
		VERSION = props.getProperty("seon.version");
		STABLE = Boolean.parseBoolean(props.getProperty("seon.stable"));

		// Setting the log file (only for the JAR execution)
		if (props.getProperty("log.output").equals("log")) {
			setLogOutput();
		}
		System.out.println("Properties: " + props);

		// Exporting images from astah and copying to the proper location for the page
		recoverAstahImages(props.getProperty("astah.location"), astahfile, props.getProperty("images.export").equals("auto"));

		// Reading the Astah file and building the Seon Model
		ModelReader reader = new ModelReader();
		Package seon = reader.parseAstah2Seon(astahfile);

		// Reading the Seon Model and generating the HTML
		PageWriter pwriter = new PageWriter();
		pwriter.generateSeonPages(seon);

		System.out.print("\nTHE END! (" + (System.currentTimeMillis() - start) / 1000.0 + "s)");
		finishMessage();
	}

	private static void finishMessage() {
		int option = JOptionPane.showConfirmDialog(null, "SEON Page Generated! Would you like to open it now?", "SEON Parser", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			try {
				File htmlFile = new File(PATH + "/page/index.html");
	    		String command = "xdg-open " + htmlFile.toURI();
    			Runtime.getRuntime().exec(command);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* Read the Properties file. */
	private static Properties readProperties(String filename) {
		Properties props = new Properties();
		FileInputStream input;
		try {
			input = new FileInputStream(filename);
			props.load(input);
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return props;
	}


private static void recoverAstahImages(String location, String astahfile, boolean export) {
	String exportPath = PATH + "/page/astahdoc/";
	File dir = new File(exportPath);
	if (!dir.exists()) dir.mkdirs();
	try {
		// Exportando imagens do arquivo Astah (usando linha de comando)
		if (export) {
			System.out.println("\n# Exporting images from Astah to " + exportPath);
			String command = location + "/astah-command.sh";
			command += " -image cl"; // selecionando apenas diagramas de Classe
			command += " -f " + astahfile; // definindo arquivo astah de entrada
			command += " -o " + exportPath; // definindo diretório de saída
			System.out.println("$ " + command);

			long start = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", command });
			process.waitFor();
			System.out.print("[-] Time: " + (System.currentTimeMillis() - start) + " - ");

			// Verificação da exportação de imagens
			int files = 0;
			int before = 0;
			int diff = 0;
			while (files == 0 || diff > 0) {
				Utils.waitFor(3, 1000);
				files = FileUtils.listFiles(dir, new String[] { "png" }, true).size();
				diff = files - before;
				before = files;
				System.out.print("[" + files + "] Time: " + (System.currentTimeMillis() - start) + " - ");
			}
		}

		// Copiando todos os arquivos .PNG para o diretório de imagens da página SEON
		String target = PATH + "/page/images/";
		int count = 0;
		System.out.println("\nCopying all .PNG files in " + dir.getPath() + " and subdirectories to " + target);
		List<File> files = (List<File>) FileUtils.listFiles(dir, new String[] { "png" }, true);
		for (File file : files) {
			File dest = new File(target + file.getName());
			FileUtils.copyFile(file, dest); // copia os arquivos PNG
			System.out.print(++count + " ");
		}
		System.out.println();

		// Excluindo o diretório temporário astahdoc
		if (export) {
			System.out.println("Deleting " + dir.getName());
			FileUtils.forceDeleteOnExit(dir);
		}
	} catch (IOException | InterruptedException e) {
		e.printStackTrace();
	}
}

	/* Defines the output log file. */
	private static void setLogOutput() {
		try {
			System.setOut(new PrintStream("SeonParserLog.log"));
			System.out.println("SEON Parser log file - " + new java.util.Date());
			System.out.println("---------------------------------------------------\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}