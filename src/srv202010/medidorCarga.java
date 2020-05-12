package srv202010;

import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class medidorCarga extends Thread{

	public double getSystemCpuLoad() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
		if (list.isEmpty()) return Double.NaN;
		Attribute att = (Attribute)list.get(0);
		Double value = (Double)att.getValue();
		// usually takes a couple of seconds before we get real values
		if (value == -1.0) return Double.NaN;
		// returns a percentage value with 1 decimal point precision
		return ((int)(value * 1000) / 10.0);
	}

	public medidorCarga() {

	}

	private boolean activo=true;

	private synchronized void espera() throws InterruptedException {
		wait(100);
	}
	public void run() {
		try {
			File fileT=null;
			String rutaT="./20carga.txt";
			fileT = new File(rutaT);
			if (!fileT.exists()) {
				fileT.createNewFile();
			}
			FileWriter fwT = new FileWriter(rutaT, true);
			double carga=0;
			
			while(activo) {
				carga=getSystemCpuLoad();
				fwT.write(Double.toString(carga)+"\n");
				fwT.flush();
				espera();
			}
			fwT.close();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
