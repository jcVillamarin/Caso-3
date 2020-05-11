package GeneradorCarga;

import cliSinSeguridad.Cliente;
import uniandes.gload.core.Task;

public class ClientServerTask extends Task{

	public void execute() {
		Cliente cliente=new Cliente("", 5000, (int) Math.random());
		
		try {
			cliente.conectar();
			cliente.protocolo();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void fail() {
		// TODO Auto-generated method stub
		System.out.println(Task.MENSAJE_FAIL);
	}

	@Override
	public void success() {
		// TODO Auto-generated method stub
		System.out.println(Task.OK_MESSAGE);
	}
}
