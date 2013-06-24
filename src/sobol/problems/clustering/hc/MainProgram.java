package sobol.problems.clustering.hc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;
import javax.management.modelmbean.XMLParseException;
import sobol.base.random.RandomGeneratorFactory;
import sobol.base.random.pseudo.PseudoRandomGeneratorFactory;
import sobol.problems.clustering.generic.model.Project;
import sobol.problems.clustering.generic.reader.CDAReader;

public class MainProgram
{
	private static String[] instanceFilenamesReals =
	{
		/*"data\\cluster\\jstl 18C.odem",
		"data\\cluster\\jnanoxml 25C.odem",*/
		"data\\cluster\\jodamoney 26C.odem",
		"data\\cluster\\jxlsreader 27C.odem",
		"data\\cluster\\seemp 31C.odem",
		"data\\cluster\\apache_zip 36C.odem",
		"data\\cluster\\udtjava 56C.odem",
		"data\\cluster\\javaocr 59C.odem",
		"data\\cluster\\pfcda_base 67C.odem",
		"data\\cluster\\forms 68C.odem",
 		"data\\cluster\\servletapi 74C.odem",
		"data\\cluster\\jscatterplot 74C.odem",
		"data\\cluster\\jfluid 82C.odem",
		"data\\cluster\\jxlscore 83C.odem",
		"data\\cluster\\jpassword 96C.odem",
		"data\\cluster\\junit 100C.odem",
		"data\\cluster\\xmldom 119C.odem",
		"data\\cluster\\tinytim 134C.odem",
		"data\\cluster\\jkaryoscope 136C.odem",
		"data\\cluster\\gae_core 140C.odem",
		"data\\cluster\\javacc 154C.odem",
		"data\\cluster\\javageom 172C.odem",
		"data\\cluster\\jdendogram 177C.odem",
		"data\\cluster\\xmlapi 184C.odem",
		"data\\cluster\\jmetal 190C.odem",
		"data\\cluster\\dom4j 195C.odem",
		"data\\cluster\\pdf_renderer 199C.odem",
		"data\\cluster\\jung_model 207C.odem",
		"data\\cluster\\jconsole 220C.odem",
		"data\\cluster\\jung_visualization 221C.odem",
		"data\\cluster\\pfcda_swing 252C.odem",
		"data\\cluster\\jpassword 269C.odem",
		"data\\cluster\\jml 270C.odem",
		"data\\cluster\\notepad_full 299C.odem",
		/*"data\\cluster\\poormans 304C.odem",
		"data\\cluster\\log4j 308C.odem",
		"data\\cluster\\jtreeview 329C.odem",
		"data\\cluster\\jace 340C.odem",
		"data\\cluster\\javaws 378C.odem",
		"data\\cluster\\res_cobol 483C.odem",
		"data\\cluster\\ybase 558C.odem",
		"data\\cluster\\lwjgl 569C.odem",
		"data\\cluster\\apache_ant_taskdef 629C.odem",
		"data\\cluster\\iTextPDF 657C.odem",
		"data\\cluster\\apache_lucene_core 741C.odem",
		"data\\cluster\\eclipse_jgit 912C.odem",*/
		//"data\\cluster\\apache_ant 1090C.odem",
		//"data\\cluster\\ylayout 1262C.odem",	
		//"data\\cluster\\ycomplete 2898C.odem",
		""
	};
	
	private Project readInstance(String filename) throws XMLParseException
	{
		CDAReader reader = new CDAReader();
		Project project = reader.execute(filename);
		System.out.println(project.getName() + " " + project.getClassCount() + " " + project.getDependencyCount());
		return project;
	}
	
	private Vector<Project> readInstances(String[] filenames) throws XMLParseException
	{
		Vector<Project> instances = new Vector<Project>();
		
		for (String filename : filenames)
			if (filename.length() > 0)
				instances.add (readInstance(filename));
		
		return instances;
	}
	
	private void runInstance(PrintWriter out, PrintWriter details, String tipo, Project instance, int cycles, int popSize) throws Exception
	{
		for (int i = 0; i < cycles; i++)
		{
			int maxEvaluations = popSize * instance.getClassCount() * instance.getClassCount();
			HillClimbingClustering hcc = new HillClimbingClustering(details, instance, maxEvaluations);
			
			long initTime = System.currentTimeMillis();
			details.println(tipo + " " + instance.getName() + " #" + cycles);
			int[] solution = hcc.execute();
			details.println();
			long executionTime = (System.currentTimeMillis() - initTime);
			
			String s = tipo + "; " + instance.getName() + " #" + i + "; " + executionTime + "; " + hcc.getFitness() + "; " + hcc.getRandomRestarts() + "; " + hcc.getRandomRestartBestFound() + "; " + hcc.printSolution(solution);
			System.out.println(s);
			out.println(s);
		}
	}
	
	public static final void main(String[] args) throws Exception
	{
		MainProgram mp = new MainProgram();

		Vector<Project> instances = new Vector<Project>();
		instances.addAll(mp.readInstances(instanceFilenamesReals));
		
		FileWriter outFile = new FileWriter("saida.txt");
		PrintWriter out = new PrintWriter(outFile);
		
		FileWriter detailsFile = new FileWriter("saida details.txt");
		PrintWriter details = new PrintWriter(detailsFile);
		
		for (Project instance : instances)
		{
			RandomGeneratorFactory.setRandomFactoryForPopulation(new PseudoRandomGeneratorFactory());
			mp.runInstance(out, details, "PSEUDO", instance, 15, 2000);
		}
		
		out.close();
		details.close();
	}
}