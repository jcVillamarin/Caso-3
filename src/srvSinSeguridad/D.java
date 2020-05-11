package srvSinSeguridad;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Queue;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

public class D extends Thread {

	public static final String OK = "OK";
	public static final String ALGORITMOS = "ALGORITMOS";
	public static final String CERTSRV = "CERTSRV";
	public static final String CERCLNT = "CERCLNT";
	public static final String SEPARADOR = ":";
	public static final String HOLA = "HOLA";
	public static final String INICIO = "INICIO";
	public static final String ERROR = "ERROR";
	public static final String REC = "recibio-";
	public static final String ENVIO = "envio-";

	// Atributos
	private Socket sc = null;
	private String dlg;
	private byte[] mybyte;
	private static X509Certificate certSer;
	private static KeyPair keyPairServidor;
	private static File file;
	private static File fileT;
	public static final int numCadenas = 13;

	private Queue<Socket> socks;
	private Queue<Integer> ids;
	private boolean finish=false;


	public static void init(X509Certificate pCertSer, KeyPair pKeyPairServidor, File pFile,File pFileT) {
		certSer = pCertSer;
		keyPairServidor = pKeyPairServidor;
		file = pFile;
		fileT=pFileT;
	}

	public D (Socket csP, int idP) {
		sc = csP;
		dlg = new String("delegado " + idP + ": ");
		try {
			mybyte = new byte[520]; 
			mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando el thread" + dlg);
			e.printStackTrace();
		}
	}

	public synchronized void addRquest(Socket csP, int idP) {
		socks.add(csP);
		ids.add(idP);
	}

	public synchronized void handleRequest() {
		sc=socks.remove();
		dlg=new String("delegado " + ids.remove() + ": ");
		run();
	}

	private boolean validoAlgHMAC(String nombre) {
		return ((nombre.equals(S.HMACMD5) || 
				nombre.equals(S.HMACSHA1) ||
				nombre.equals(S.HMACSHA256) ||
				nombre.equals(S.HMACSHA384) ||
				nombre.equals(S.HMACSHA512)
				));
	}

	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo . 
	 * - Es el Ãºnico metodo permitido para escribir en el log.
	 */
	private synchronized void escribirMensaje(String pCadena) {

		try {
			FileWriter fw = new FileWriter(file,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private synchronized void escribirT(String pCadena) {

		try {
			FileWriter fw = new FileWriter(fileT,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void finish() {
		finish=true;
	}

	public void run() {
		String[] cadenas;
		cadenas = new String[numCadenas];

		String feedback;
		String linea;
		System.out.println(dlg + "Empezando atencion.");
		try {

			PrintWriter ac = new PrintWriter(sc.getOutputStream() , true);
			BufferedReader dc = new BufferedReader(new InputStreamReader(sc.getInputStream()));

			/***** Fase 1:  *****/
			linea = dc.readLine();
			if (!linea.equals(HOLA)) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			} else {
				ac.println(OK);
				cadenas[0] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[0]);
			}

			/***** Fase 2:  *****/
			linea = dc.readLine();
			if (!(linea.contains(SEPARADOR) && linea.split(SEPARADOR)[0].equals(ALGORITMOS))) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			}

			String[] algoritmos = linea.split(SEPARADOR);
			if (!algoritmos[1].equals(S.DES) && !algoritmos[1].equals(S.AES) &&
					!algoritmos[1].equals(S.BLOWFISH) && !algoritmos[1].equals(S.RC4)){
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Simetrico" + REC + algoritmos + "-terminando.");
			}
			if (!algoritmos[2].equals(S.RSA) ) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Asimetrico." + REC + algoritmos + "-terminando.");
			}
			if (!validoAlgHMAC(algoritmos[3])) {
				ac.println(ERROR);
				sc.close();
				throw new Exception(dlg + ERROR + "AlgHash." + REC + algoritmos + "-terminando.");
			}
			cadenas[1] = dlg + REC + linea + "-continuando.";
			System.out.println(cadenas[1]);
			ac.println(OK);
			cadenas[2] = dlg + ENVIO + OK + "-continuando.";
			System.out.println(cadenas[2]);

			long ini=System.currentTimeMillis();
			/***** Fase 3: Recibe certificado del cliente *****/				
			String strCertificadoCliente = dc.readLine();
			byte[] certificadoClienteBytes = new byte[520];
			certificadoClienteBytes = toByteArray(strCertificadoCliente);
			CertificateFactory creador = CertificateFactory.getInstance("X.509");
			InputStream in = new ByteArrayInputStream(certificadoClienteBytes);
			X509Certificate certificadoCliente = (X509Certificate)creador.generateCertificate(in);
			cadenas[3] = dlg + REC + "certificado del cliente. continuando.";
			System.out.println(cadenas[3]);
			ac.println(OK);
			cadenas[4] = dlg + ENVIO + OK + "-continuando.";
			System.out.println(cadenas[4]);

			/***** Fase 4: Envia certificado del servidor *****/
			String strSerCert = toHexString(mybyte);
			ac.println(strSerCert);
			cadenas[5] = dlg + ENVIO + " certificado del servidor. continuando.";
			System.out.println(cadenas[5]);	
			linea = dc.readLine();
			if (!linea.equals(OK)) {
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			} else {
				cadenas[6] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[6]);
			}

			/***** Fase 5: Envia llave simetrica *****/
			SecretKey simetrica = S.kgg(algoritmos[1]);
			//byte [ ] ciphertext1 = S.ae(simetrica.getEncoded(), certificadoCliente.getPublicKey(), algoritmos[2]);
			ac.println(toHexString(simetrica.getEncoded()));
			cadenas[7] = dlg +  ENVIO + "llave K_SC al cliente. continuado.";
			System.out.println(cadenas[7]);

			/***** Fase 5: Envia reto *****/
			Random rand = new Random(); 
			int intReto = rand.nextInt(999);
			String strReto = intReto+"";
			while (strReto.length()%4!=0) strReto += "0";

			String reto = strReto;
			//byte[] bytereto = toByteArray(reto);
			//byte [] cipherreto = S.se(bytereto, simetrica, algoritmos[1]);
			ac.println(reto);
			cadenas[8] = dlg + ENVIO + reto + "-reto al cliente. continuando ";
			System.out.println(cadenas[8]);

			/***** Fase 6: Recibe reto del cliente *****/
			linea = dc.readLine();
			//byte[] retodelcliente = S.ad(toByteArray(linea),keyPairServidor.getPrivate(), algoritmos[2] );
			//String strdelcliente = toHexString(retodelcliente);
			if (linea.equals(reto)) {
				cadenas[9] = dlg + REC + linea + "-reto correcto. continuado.";
				System.out.println(cadenas[9]);
				ac.println("OK");
			} else {
				ac.println("ERROR");
				sc.close();
				throw new Exception(dlg + REC + linea + "-ERROR en reto. terminando");
			}

			/***** Fase 7: Recibe identificador de usuario *****/
			linea = dc.readLine();
			//byte[] retoByte = toByteArray(linea);
			//byte [ ] ciphertext2 = S.sd(retoByte, simetrica, algoritmos[1]);
			//String nombre = toHexString(ciphertext2);
			cadenas[10] = dlg + REC + linea + "-continuando";
			System.out.println(cadenas[10]);

			/***** Fase 8: Envia hora de registro *****/
			Calendar rightNow = Calendar.getInstance();
			int hora = rightNow.get(Calendar.HOUR_OF_DAY);
			int minuto = rightNow.get(Calendar.MINUTE);
			String strvalor;
			if (hora<10)
				strvalor = "0" + ((hora) * 100 + minuto);
			else
				strvalor = ((hora) * 100 + minuto) + "";
			while (strvalor.length()%4!=0) strvalor = "0" + strvalor;
			//byte[] valorByte = toByteArray(strvalor);
			//byte [ ] ciphertext3 = S.se(valorByte, simetrica, algoritmos[1]);
			ac.println(strvalor);
			cadenas[11] = dlg + ENVIO + strvalor + "-cifrado con K_SC. continuado.";
			System.out.println(cadenas[11]);

			linea = dc.readLine();	
			if (linea.equals(OK)) {
				cadenas[12] = dlg + REC + linea + "-Terminando exitosamente.";
				long fin=System.currentTimeMillis();
				long to=fin-ini;
				escribirT(Long.toString(to));
				C.finishTran();
				System.out.println(cadenas[12]);
			} else {
				cadenas[12] = dlg + REC + linea + "-Terminando con error";
				System.out.println(cadenas[12]);
			}
			sc.close();
			String log="";
			for (int i=0;i<numCadenas;i++) {
				log+=(cadenas[i]+"\n");
			}
			escribirMensaje(log);

			while(true) {
				if(!socks.isEmpty()) {
					handleRequest();
					}
				else {
					if(finish) {
						break;
					}
					wait();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printBase64Binary(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseBase64Binary(s);
	}

}
