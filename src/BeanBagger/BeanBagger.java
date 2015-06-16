/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BeanBagger;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.*;
import javax.management.ObjectInstance;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.json.simple.*;

/**
 *
 * @author s.ostenberg
 */
public class BeanBagger {
	private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
        public static  String  TargetJVM = "";
        public static String TARGETBEAN = "";
        public static VirtualMachineDescriptor TARGETDESCRIPTOR ;
        static JMXConnector myJMXconnector = null;
        public static boolean ExactMatchRequired = false; // ALlows matching TargetVM process based on substring
        public static String OUTFILE = "//tmp//output.yml";
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        if(args.length>0 && args[0].length()>0  )
        {
            TargetJVM=args[0];
            if(args.length>1)
            {
            TARGETBEAN=args[1];
            }
            else
            {
                TARGETBEAN="*";
            }
        }
        else
        {
           TargetJVM="*";
            
            System.out.println("Beanbagger {process} {bean}");
            System.out.println("  process: Process Name to try to connect to:");
            System.out.println("  bean:  optional, restrict data to just one bean. Default is all beans ");
            System.out.println("\nProcesses found:");
            List<VirtualMachineDescriptor> list = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd: list)
            {
                if(vmd.displayName().equals(""))
                {
                System.out.println("   {Unamed Instance}");
                }
                else
                {
                 System.out.println("   "+ vmd.displayName());   
                }

            }
            System.out.println("");
            //System.exit(1);
        }
        
               
        
        //The following code grabs a list of running VMs and sees if they match our target--------------------------------------
        Map<String, VirtualMachine> result = new HashMap<>();
        
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        List<VirtualMachineDescriptor> MATCHINGLIST = new ArrayList<VirtualMachineDescriptor>();
        
        Boolean gotit = false;
        String listofjvs = "";
        
        System.out.println("Searching for matching VM instances");
        for (VirtualMachineDescriptor vmd: list) {

            String desc = vmd.toString();
            try {
               result.put(desc, VirtualMachine.attach(vmd));
               String DN = vmd.displayName();
               if(DN.contains(TargetJVM) || TargetJVM.equalsIgnoreCase("*"))
                     {
                      if(DN.equals(""))   
                      {
                      System.out.println("  Skipping unnamed JVM");    
                      }
                      else
                      {
                      System.out.println("  Matching JVM instance found: " + DN);
                      TARGETDESCRIPTOR=vmd;
                      gotit=true;
                      MATCHINGLIST.add(vmd);
                      }
               } 
               else
               {
                  listofjvs+=DN + "  \n" ;
               }
            }
            catch (IOException | AttachNotSupportedException e) {
               
               }

            
        }
             if(!gotit)//If we dont find the instance.
            {
              System.out.println("No JVM Processes matching " + TargetJVM + " were found."); 
              System.out.println("Found instances: " + listofjvs );
              System.exit(1);
            }
            System.out.println("");
        
 ///-------------If we get here, we have identified at least one instance matching our criteria   
                    
                    
for(VirtualMachineDescriptor avmd: MATCHINGLIST)     
{
                    
                    
myJMXconnector = getLocalConnection(VirtualMachine.attach(avmd));// Connects to the process containing our beans
MBeanServerConnection myJMXConnection = myJMXconnector.getMBeanServerConnection(); //Connects to the MBean server for that process.

System.out.println("Number of beans found in " + avmd.displayName()+":" + myJMXConnection.getMBeanCount());

String getDefaultDomain = myJMXConnection.getDefaultDomain();
String[] getDomains=myJMXConnection.getDomains();


Set<ObjectInstance> beans = myJMXConnection.queryMBeans(null, null);
for( ObjectInstance instance : beans )
{
    String daclassname = instance.getClassName();
    if(daclassname.contains(TARGETBEAN) || TARGETBEAN.contentEquals("*"))
    {

    System.out.println("  Processing me a bean: " + daclassname);   

    MBeanInfo info = myJMXConnection.getMBeanInfo( instance.getObjectName() );
    MBeanAttributeInfo[] myAttributeArray = info.getAttributes();
    for(MBeanAttributeInfo thisAttributeInfo : myAttributeArray)
    {
        String attvalue = "";
        String myname = thisAttributeInfo.getName();
        //String mydesc = thisAttributeInfo.getDescription();
        String mytype = thisAttributeInfo.getType();

        try{
        switch (mytype) {
            case "String":
                attvalue = (String)myJMXConnection.getAttribute(instance.getObjectName(), myname );
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break;
            case "java.lang.String":
                attvalue = (String)myJMXConnection.getAttribute(instance.getObjectName(), myname );
                System.out.println("    Name:" + myname + "  Type;" + mytype + "  Value:"  + attvalue);
                break;    
            case "boolean":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break;
            case "int":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break;  
            case "long":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break;
            case "double":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break; 
            default:
                attvalue = "Unsupported type";
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
                break;  
        }
        } 
        catch(UnsupportedOperationException | RuntimeMBeanException ex)
                {
                attvalue = "Unsupported Operation";
                System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);  
                }

    }
    }
   
 
}


}//End VM iteration




System.out.println("Stiv's Beanbagger Finished");         
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
