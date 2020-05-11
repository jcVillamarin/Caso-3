package GeneradorCarga;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;

public class Generator {

	public LoadGenerator generator;
	
	public Generator() {
		Task work=createTask();
		int numberOfTask=100;
		int gapBetweenTask=1000;
		generator = new LoadGenerator("Client - Server Load Test", numberOfTask, work,gapBetweenTask );
		generator.generate();
	}
	
	public Task createTask() {
		return new ClientServerTask();
	}
	
	public static void main (String ... args) {
		Generator gen=new Generator();
	}
}
