/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BeanBagger;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.remote.*;





import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
/**
 *
 * @author s.ostenberg
 */
public class BeanBagger {
	private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
        public static  String  TARGET = "";
        public static String BEAN = "";
        public static VirtualMachineDescriptor TARGETDESCRIPTOR ;
        static JMXConnector myJMXconnector = null;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        if(args[0].length()>0  )
        {
            TARGET=args[0];
            if(args.length>1)
            {
            BEAN=args[1];
            }
            else
            {
                BEAN="*";
            }
        }
        else
        {
            System.out.println("Beanbagger {process} {bean}");
            System.out.println("process: Process Name to try to connect to:");
            System.out.println("bean:  restrict data to just one bean. Default is all beans ");
        }
        
               
        
        //The following code grabs a list of running VMs and sees if they match our target--------------------------------------
        Map<String, VirtualMachine> result = new HashMap<>();
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        Boolean gotit = false;
        String listofjvs = "";
        for (VirtualMachineDescriptor vmd: list) {

            String desc = vmd.toString();
            try {
               result.put(desc, VirtualMachine.attach(vmd));
               String DN = vmd.displayName();
               if(DN.equals(TARGET))
                     {
                      System.out.println("Target instance found: " + TARGET);
                      TARGETDESCRIPTOR=vmd;
                      gotit=true;
                      break;
               } 
               else
               {
                  listofjvs+=DN + "\n" ;
               }
            }
            catch (IOException | AttachNotSupportedException e) {
               
               }

            
        }
                    if(!gotit)//If we dont find the instance.
            {
              System.out.println("Target instance not found: " + TARGET); 
              System.out.println("Found: " + listofjvs );
              System.exit(1);
            }
        
 ///-------------If we get here, we have identified an instance matching our criteria       
myJMXconnector = getLocalConnection(VirtualMachine.attach(TARGETDESCRIPTOR));// Connects to the process containing our beans
MBeanServerConnection myJMXConnection = myJMXconnector.getMBeanServerConnection(); //Connects to the MBean server for that process.


System.out.println("Beans found: " +  myJMXConnection.getMBeanCount());

String getDefaultDomain = myJMXConnection.getDefaultDomain();
String[] getDomains=myJMXConnection.getDomains();





Set<ObjectInstance> beans = myJMXConnection.queryMBeans(null, null);
for( ObjectInstance instance : beans )
{
    String daclassname = instance.getClassName();
    if(daclassname.contains(BEAN) || BEAN.contentEquals("*"))
    {
    System.out.println("Processing me a bean: " + daclassname);   

    MBeanInfo info = myJMXConnection.getMBeanInfo( instance.getObjectName() );
    
    MBeanAttributeInfo[] myAttributeArray = info.getAttributes();
    for(MBeanAttributeInfo thisAttributeInfo : myAttributeArray)
    {
        String attvalue = "";
        String myname = thisAttributeInfo.getName();
        //String mydesc = thisAttributeInfo.getDescription();
        String mytype = thisAttributeInfo.getType();

        
        
        //attvalue = (String)myJMXConnection.getAttribute(instance.getObjectName(), );
        
        System.out.println("  Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue); 
    }

    
    


    
    
    
    
    
    
    
    
    
    
    }
   
 
}


//mbsc.getMBeanInfo(BEAN);














System.out.println("Th th thats all folks!");         
    }
    
static JMXConnector getLocalConnection(VirtualMachine vm) throws Exception {
   Properties props = vm.getAgentProperties();
   String connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
   if (connectorAddress == null) {
      props = vm.getSystemProperties();
      String home = props.getProperty("java.home");
      String agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
      vm.loadAgent(agent);
      props = vm.getAgentProperties();
      connectorAddress = props.getProperty(CONNECTOR_ADDRESS_PROPERTY);
   }
   JMXServiceURL url = new JMXServiceURL(connectorAddress);
   return JMXConnectorFactory.connect(url);
}
    
}
