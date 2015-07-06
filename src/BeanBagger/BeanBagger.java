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
import java.io.PrintWriter;
import java.io.Writer;
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

import java.net.InetAddress;
import java.net.UnknownHostException;


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
        public static org.json.JSONObject Jason = new org.json.JSONObject();
        public static String JSONFile = "";
        public static boolean outJSON=false;
        public static boolean prettyprint=false;
        
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
                if(x==args.length && !args[x+1].startsWith("-"))Usage();
                TargetJVM=args[x+1];
                x++;
                break;
              case "-ppj"  :
                 outJSON=true; 
                 prettyprint=true; 
                 break; 
              case "-j":
                outJSON=true;
                if(args.length-1>x && !args[x+1].startsWith("-"))//If next item on line exists and is not an option
                {
                    JSONFile=args[x+1];
                    x++;
                }
                break;
              case "-b":
                if(args.length-1>x && !args[x+1].startsWith("-"))
                {
                    TARGETBEAN=args[x+1];
                    x++;
                }
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
                   Usage();
                  
                  
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
                      else if(DN.contains("BeanBagger")){
                      System.out.println("  Skipping BeanBagger JVM");  
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
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

 org.json.JSONObject Jinfrascan = new org.json.JSONObject();//Contains Hosts
 org.json.JSONArray Hosts = new org.json.JSONArray();
 org.json.JSONObject Host = new org.json.JSONObject();
 
 org.json.JSONArray JVMs = new org.json.JSONArray();//JVMs on host
           
for(VirtualMachineDescriptor avmd: MATCHINGLIST)     
{                
myJMXconnector = getLocalConnection(VirtualMachine.attach(avmd));// Connects to the process containing our beans
MBeanServerConnection myJMXConnection = myJMXconnector.getMBeanServerConnection(); //Connects to the MBean server for that process.
System.out.println("Number of beans found in " + avmd.displayName()+":" + myJMXConnection.getMBeanCount());

String getDefaultDomain = myJMXConnection.getDefaultDomain();
String[] getDomains=myJMXConnection.getDomains();
Set<ObjectInstance> beans = myJMXConnection.queryMBeans(null, null);

org.json.JSONObject JVM = new org.json.JSONObject();
org.json.JSONArray JBeans = new org.json.JSONArray();
for( ObjectInstance instance : beans )
{
    String daclassname = instance.getClassName();
    if(supressSun & (daclassname.startsWith("sun.") | daclassname.startsWith("com.sun."))) continue;
    if(daclassname.contains(TARGETBEAN) || TARGETBEAN.contentEquals("*"))
    {
      MBeanAttributeInfo[] myAttributeArray = null;   
      org.json.JSONObject Beanboy = new org.json.JSONObject();
      org.json.JSONArray BeanieButes = new org.json.JSONArray();
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
       String mydesc = "";
       boolean myread=false;
       
       try{
        myname = thisAttributeInfo.getName();
        mydesc = thisAttributeInfo.getDescription();
        mytype = thisAttributeInfo.getType();
        myread = thisAttributeInfo.isReadable();

        if(myread)
        {
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
                attvalue = "*-Unsupported: complex type-*";
                break;  
        }//end switch
        }//end if
        else{
        attvalue = "*-Not readable!-*";
        } 
        }
        catch(Exception ex )
                {
                attvalue = "*-Exception accessing value-*";
                }
       boolean dooutput=false;
       if(notquiet)dooutput=true;
       else
           try 
           {
               if(!attvalue.startsWith("*-"))
                dooutput=true; 
           }
          catch(Exception ex)//For attributes with no values.
          {
              attvalue="*-No value-*";
              dooutput=true;
          }
               
       if(dooutput)
       {
           System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:"  + attvalue);
           org.json.JSONObject AtDatas = new org.json.JSONObject();// Create the list of attributes and values into an object.
           AtDatas.put("Name", myname);
           AtDatas.put("Type", mytype);
           AtDatas.put("Value", attvalue);
           AtDatas.put("Desc", mydesc);
           BeanieButes.put(AtDatas);
           
       }
           
    }//End processing Bean Attributes, add attributes to bean array.
    Beanboy.put(daclassname, BeanieButes);//add attributes to the bean
    JBeans.put(Beanboy);//add bean to VM
    }//End if this bean was skipped.

}//End of process JVM instance beans
JVM.put(avmd.displayName(), JBeans);
JVMs.put(JVM);


}//End JVM iteration
java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
String mename = addr.getHostName();
Host.put(mename, JVMs);
//add vms to host
Hosts.put(Host);
//add server(s) to infra
String time = String.valueOf(System.currentTimeMillis());
Jinfrascan.put(time, Hosts);


// OK. How do I dump the JSON?
if(outJSON)
{
if(!JSONFile.equals(""))
{
try
{  
 PrintWriter writer = new PrintWriter(JSONFile, "UTF-8");
 if(prettyprint)writer.println(Jinfrascan.toString(4));
 else writer.println(Jinfrascan);
 writer.close();  
}
catch(Exception ex)
        {
        System.out.print("Error processing file!") ;
        System.out.print(ex);
        }
}
else
{
    System.out.println("JSON Output:");  
    if(prettyprint)System.out.print(Jinfrascan.toString(4));
    else System.out.print(Jinfrascan);
}
    
}
System.out.println("");  
System.out.println("Stiv's Beanbagger Finished");  


    }
    
    public static void Usage()
    {
      System.out.println("java -jar Beanbagger [-p {process}] [-b {bean}] -q -m [-j {filename}] -ppj");
                  System.out.println("  -p {process}: Optional, VM Process Name or substring of process to try to connect to. Defaults to all");
                  System.out.println("  -b {bean}:  Optional, restrict data to just one bean. Default is all beans ");
                  System.out.println("  -j {optionalfilename}:  Optional: Output results to filename in JSON format, or to console if no file specified.");
                  System.out.println("  -x  :Requires exact match of VM Process Name");
                  System.out.println("  -q  :Filter. Suppresses output of unsupported types or operations.");
                  System.out.println("  -m  :Filter. Suppresses iteration of Sun beans (sun.*  and com.sun.*");
                  System.out.println("  -ppj :  Prettyprint JSON output");
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
