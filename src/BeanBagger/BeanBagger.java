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
        public static boolean notquiet=true;
        public static boolean supressSun=false;
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        
        for(int x =0;x<args.length;x++)
        {
         String disarg = args[x];
         switch(disarg)
         {
              case "-p":
                TargetJVM=args[x+1];
                x++;
                break;
              case "-b":
                TARGETBEAN=args[x+1];
                x++;
                break;
              case "-q":
                  notquiet=false;
                  break;
               case "-m":
                  supressSun=true;
                  break;
               case "-x":
                  ExactMatchRequired=true;
                  break;    
               default:
                  System.out.println("Beanbagger [-p {process}] [-b {bean}] -q -m ");
                  System.out.println("-p {process}: VM Process Name or substring to try to connect to:");
                  System.out.println("-b {bean}:  optional, restrict data to just one bean. Default is all beans ");
                  System.out.println("-x  Requires exact match of VM Process Name");
                  System.out.println("-q  Filter. Suppresses output of unsupported types or operations.");
                  System.out.println("-m  Filter. Suppresses iteration of Sun beans (sun.*  and com.sun.*");
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
                      System.exit(1);
                  
         }//End switch
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
    if(supressSun & (daclassname.startsWith("sun.") | daclassname.startsWith("com.sun."))) continue;
    
    if(daclassname.contains(TARGETBEAN) || TARGETBEAN.contentEquals("*"))
    {
    MBeanAttributeInfo[] myAttributeArray = null;   
    
    try
    {
    MBeanInfo info = myJMXConnection.getMBeanInfo( instance.getObjectName() );
    myAttributeArray = info.getAttributes(); 
    System.out.println("  Processing me a bean: " + daclassname);
    }   
    catch( UnsupportedOperationException | RuntimeMBeanException | IllegalStateException ex)
    {
    System.out.println("  Error processing bean: " + daclassname);  
    }
        
    for(MBeanAttributeInfo thisAttributeInfo : myAttributeArray)
    {
       String attvalue = "";
       String myname = "";
       String mytype = "";
       try{
        myname = thisAttributeInfo.getName();
        //String mydesc = thisAttributeInfo.getDescription();
        mytype = thisAttributeInfo.getType();


        switch (mytype) {
            case "String":
                attvalue = (String)myJMXConnection.getAttribute(instance.getObjectName(), myname );
                break;
            case "java.lang.String":
                attvalue = (String)myJMXConnection.getAttribute(instance.getObjectName(), myname );
                break;    
            case "boolean":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                break;
            case "int":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                break;  
            case "long":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                break;
            case "double":
                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname ).toString();
                break; 
            default:
                attvalue = "Unsupported type";
                break;  
        }
      
        } 
        catch(Exception ex )
                {
                attvalue = "Unsupported Operation";
                }
       if(notquiet)
       {
       System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
       }
       else
           if(!attvalue.startsWith("Unsupported") )
               {
                 System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);  
               }
    }
    }
   
 
}//End of process JVM instance beans


}//End JVM iteration




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
