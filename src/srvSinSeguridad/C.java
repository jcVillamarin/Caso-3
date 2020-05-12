package srvSinSeguridad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import srvSinSeguridad.D;
import srv202010.medidorCarga;


public class C {
	private static ServerSocket ss;	
	private static final String MAESTRO = "MAESTRO: ";
	private static X509Certificate certSer; /* acceso default */
	private static KeyPair keyPairServidor; /* acceso default */

	private static int finishTransactions=0;

	public static synchronized void finishTran() {
		finishTransactions++;
		System.out.println(finishTransactions+" clientes atendidos");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub

		System.out.println(MAESTRO + "Establezca puerto de conexion:");
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int ip = Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Establezca numero de threads para conexiones:");
		int nThreads=Integer.parseInt(br.readLine());
		System.out.println(MAESTRO + "Empezando servidor maestro en puerto " + ip);
		// Adiciona la libreria como un proveedor de seguridad.
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());	
		final ExecutorService pool;

		// Crea el archivo de log
		File file = null;
		keyPairServidor = S.grsa();
		certSer = S.gc(keyPairServidor); 
		String ruta = "./20resultados.txt";

		file = new File(ruta);
		if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file);
		fw.close();


		// Crea archivo tiempo transaccion
		File fileT=null;
		String rutaT="./20time.txt";
		fileT = new File(rutaT);
		if (!fileT.exists()) {
			fileT.createNewFile();
		}
		FileWriter fwT = new FileWriter(fileT);
		fwT.close();

		D.init(certSer, keyPairServidor,file,fileT);

		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println(MAESTRO + "Socket creado.");
		pool= Executors.newFixedThreadPool(nThreads);

		medidorCarga m=new medidorCarga();
		m.start();

		for (int i=0;i<400;i++) {
			try { 
				pool.execute(new D(ss.accept(),i));
				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
			} catch (IOException e) {
				pool.shutdown();
				System.out.println(MAESTRO + "Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
		pool.shutdown();
		/*
		try { 
			t1=new D(ss.accept(),0);
			pool.execute(t1);
			System.out.println(MAESTRO + "Cliente " + 0 + " aceptado.");
			t2=new D(ss.accept(),1);
			pool.execute(t2);
			System.out.println(MAESTRO + "Cliente " + 1 + " aceptado.");
			for (int i=2;i<400;i++) {
				t1.addRquest(ss.accept(),i);
				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
				i++;
				t2.addRquest(ss.accept(),i);
				System.out.println(MAESTRO + "Cliente " + i + " aceptado.");
			}
			t1.finish();
			t2.finish();
			
		} catch (IOException e) {
			pool.shutdown();
			System.out.println(MAESTRO + "Error creando el socket cliente.");
			e.printStackTrace();
		}
		*/
		
		
	}
}
