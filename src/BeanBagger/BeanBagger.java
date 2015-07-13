/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BeanBagger;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.BufferedWriter;

import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author s.ostenberg
 */
public class BeanBagger {
        
	private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
        public static VirtualMachineDescriptor TARGETDESCRIPTOR ;
        static JMXConnector myJMXconnector = null;
        
        
        //replace these with MBean
       // public static  String  TargetJVM = "";
        //public static String TARGETBEAN = "";

       // public static boolean ExactMatchRequired = false; // ALlows matching TargetVM process based on substring
       // public static String OUTFILE = "//tmp//output.yml";
       // public static boolean suppresscomplex=true;
       // public static boolean ignoreunreadable=false;
       // public static boolean supressSun=false;
       // public static String JSONFile = "";//The file we will output to.
       // public static boolean outJSON=false;//Turn on JSON output
       // public static boolean prettyprint=false;
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
                //Get the MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        //register the MBean
        BBConfig mBean = new BBConfig();
        ObjectName name = new ObjectName("com.stiv.jmx:type=SystemConfig");
        mbs.registerMBean(mBean, name);

        for(int x =0;x<args.length;x++)
        {
         String disarg = args[x];
         switch(disarg)
         {
              case "-p":
                if(x==args.length && !args[x+1].startsWith("-"))Usage();
                mBean.setTargetJVM(args[x+1]);
                x++;
                break;
              case "-ppj"  :
                 mBean.setoutJSON(true);
                 mBean.setprettyprint(true);
                 break; 
              case "-j":
                mBean.setoutJSON(true);
                if(args.length-1>x && !args[x+1].startsWith("-"))//If next item on line exists and is not an option
                {
                    mBean.setJSONFile(args[x+1]);
                    x++;
                }
                break;
              case "-b":
                if(args.length-1>x && !args[x+1].startsWith("-"))
                {
                    mBean.setTARGETBEAN(args[x+1]);
                    x++;
                }
                break;
                case "-r":
                  mBean.setignoreunreadable(true);
                  break;
              case "-q":
                  mBean.setsuppresscomplex(false);
                  break;
               case "-m":
                   mBean.setsupressSun(true);
                  break;
               case "-x":
                   mBean.setExactMatchRequired(true);
                  break; 
               case "-log": 
 
                if(args.length-1>x && !args[x+1].startsWith("-"))//If next item on line exists and is not an option
                {
                    mBean.setLogDir(args[x+1]);
                    try
                    {
                      File theDir = new File(mBean.getLogDir());
 
                      // if the directory does not exist, create it
                     if (!theDir.exists())
                       {
                         System.out.println("creating directory: " + mBean.getLogDir());
                          theDir.mkdir();
                       }

                    }
                    catch(Exception ex)
                    {
                       System.out.println("Error creating directory: " + mBean.getLogDir()); 
                       System.exit(1);
                    }
                    // Write a readme file to the directory as a test
                    try{
                        String rmf = mBean.getLogDir() + "\\BeanBaggerreadme.txt";
                        try (PrintWriter out = new PrintWriter(rmf)) {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            Date date = new Date();
                            out.println("Beanbagger started "  + dateFormat.format(date) );
                            
                            //consider adding options outlining the arguments started with.
                            
                            
                            out.close();
                        }
                    }
                    catch(Exception ex){
                      System.out.println("Error creating files in: " + mBean.getLogDir());  
                    }

                    x++;
                }
                break;    
               case "-l":
                   mBean.setLoop(true);
                   if(args.length-1>x && !args[x+1].startsWith("-"))
                {
                    try{
                    mBean.setLoopDelaySeconds(Integer.parseInt(args[x+1]));}
                    catch(Exception ex){
                       System.out.println("You call " + args[x+1] + " an integer" +mBean.getInsult()+"?"); 
                       Usage();
                    }
                    x++;

                }
                   else{mBean.setLoopDelaySeconds(30); }
                   break; 
                case "-c":
                    mBean.setLoop(true);
                   if(args.length-1>x && !args[x+1].startsWith("-"))
                {
                    try{
                    mBean.setIterations(Integer.parseInt(args[x+1]));}
                    catch(Exception ex){
                       System.out.println("You call " + args[x+1] + " an integer" +mBean.getInsult()+"?"); 
                       Usage();
                    }
                    x++;
                    
                }
                   else{mBean.setIterations(5);}
                  break; 


               default:
                   Usage();
                
                  
         }//End switch
        }

        
     //Variables have been set.     We are done with intitial config
        Boolean loopagain=false;
        do { //Here we go,  into da loop

            
            try {

                //The following code grabs a list of running VMs and sees if they match our target--------------------------------------
                Map<String, VirtualMachine> result = new HashMap<>();
                
                List<VirtualMachineDescriptor> list = VirtualMachine.list();
                List<VirtualMachineDescriptor> MATCHINGLIST = new ArrayList<VirtualMachineDescriptor>();
                
                Boolean gotit = false;
                String listofjvs = "";
                
                System.out.println("Searching for matching VM instances");
                for (VirtualMachineDescriptor vmd : list) {
                    
                    String desc = vmd.toString();
                    try {
                        result.put(desc, VirtualMachine.attach(vmd));
                        String DN = vmd.displayName();
                        if (DN.contains(mBean.getTargetJVM()) || mBean.getTargetJVM().equalsIgnoreCase("*")) {
                            if (DN.equals("")) {
                                System.out.println("  Skipping unnamed JVM");                                
                            } //else if(!TargetJVM.startsWith("BeanBagger")  && DN.contains("BeanBagger")){ System.out.println("  Skipping BeanBagger JVM");  }
                            else {
                                System.out.println("  Matching JVM instance found: " + DN);
                                TARGETDESCRIPTOR = vmd;
                                gotit = true;
                                MATCHINGLIST.add(vmd);
                            }
                        } else {
                            listofjvs += DN + "  \n";
                        }
                    } catch (IOException | AttachNotSupportedException e) {
                        
                    }
                    
                }
                if (!gotit)//If we dont find the instance.
                {
                    System.out.println("No JVM Processes matching " + mBean.getTargetJVM() + " were found.");                    
                    System.out.println("Found instances: " + listofjvs);
                    System.exit(1);
                }
                System.out.println("");

 ///-------------If we get here, we have identified at least one instance matching our criteria  
                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                org.json.JSONObject Jinfrascan = new org.json.JSONObject();//Contains Hosts
                org.json.JSONArray Hosts = new org.json.JSONArray();
                org.json.JSONObject Host = new org.json.JSONObject();
                
                org.json.JSONArray JVMs = new org.json.JSONArray();//JVMs on host
                
                for (VirtualMachineDescriptor avmd : MATCHINGLIST) {                    
                    myJMXconnector = getLocalConnection(VirtualMachine.attach(avmd));// Connects to the process containing our beans
                    MBeanServerConnection myJMXConnection = myJMXconnector.getMBeanServerConnection(); //Connects to the MBean server for that process.
                    System.out.println("Number of beans found in " + avmd.displayName() + ":" + myJMXConnection.getMBeanCount());
                    
                    String getDefaultDomain = myJMXConnection.getDefaultDomain();
                    String[] getDomains = myJMXConnection.getDomains();
                    Set<ObjectInstance> beans = myJMXConnection.queryMBeans(null, null);
                    
                    org.json.JSONObject JVM = new org.json.JSONObject();
                    org.json.JSONArray JBeans = new org.json.JSONArray();
                    for (ObjectInstance instance : beans) {
                        String daclassname = instance.getClassName();
                        if (mBean.getsupressSun() & (daclassname.startsWith("sun.") | daclassname.startsWith("com.sun."))) {
                            continue;
                        }
                        if (daclassname.contains(mBean.getTARGETBEAN()) || mBean.getTARGETBEAN().contentEquals("*")) {
                            MBeanAttributeInfo[] myAttributeArray = null;                            
                            org.json.JSONObject Beanboy = new org.json.JSONObject();
                            org.json.JSONArray BeanieButes = new org.json.JSONArray();
                            try {
                                MBeanInfo info = myJMXConnection.getMBeanInfo(instance.getObjectName());
                                myAttributeArray = info.getAttributes();                                
                                System.out.println("  Processing me a bean: " + daclassname);
                            } catch (UnsupportedOperationException | RuntimeMBeanException | IllegalStateException ex) {
                                System.out.println("  Error processing bean: " + daclassname);                                
                            }
                            
                            for (MBeanAttributeInfo thisAttributeInfo : myAttributeArray) {
                                String attvalue = "";
                                String myname = "";
                                String mytype = "";
                                String mydesc = "";
                                boolean myread = false;
                                boolean mywrite = false;
                                
                                try {
                                    myname = thisAttributeInfo.getName();
                                    mydesc = thisAttributeInfo.getDescription();
                                    mytype = thisAttributeInfo.getType();
                                    myread = thisAttributeInfo.isReadable();
                                    mywrite = thisAttributeInfo.isWritable();
                                    if (myread) {
                                        switch (mytype) {
                                            case "String":
                                                attvalue = (String) myJMXConnection.getAttribute(instance.getObjectName(), myname);
                                                break;
                                            case "java.lang.String":
                                                attvalue = (String) myJMXConnection.getAttribute(instance.getObjectName(), myname);
                                                break;                                            
                                            case "boolean":
                                                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname).toString();
                                                break;
                                            case "int":
                                                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname).toString();
                                                break;                                            
                                            case "long":
                                                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname).toString();
                                                break;
                                            case "double":
                                                attvalue = myJMXConnection.getAttribute(instance.getObjectName(), myname).toString();
                                                break;                                            
                                            default:
                                                attvalue = "*-Unsupported: complex type-*";
                                                break;                                            
                                        }//end switch
                                    }//end if
                                    else {
                                        attvalue = "";
                                    }                                    
                                } catch (Exception ex) {
                                    attvalue = "*-Exception accessing value-*";
                                }

                                //THis section is where we determine if we are going to record the value or not.
                                boolean dooutput = false;
                                
                                if (mBean.getsuppresscomplex()) {
                                    dooutput = true;
                                } else {
                                    try {
                                        if (!attvalue.startsWith("*-")) {
                                            dooutput = true;
                                        }                                        
                                    } catch (Exception ex)//For attributes with no values.
                                    {
                                        attvalue = "*-No value-*";
                                        dooutput = true;
                                    }
                                }
                                if (mBean.getignoreunreadable() && !myread) {
                                    dooutput = false;
                                }                                
                                
                                if (dooutput) {
                                    
                                    org.json.JSONObject AtDatas = new org.json.JSONObject();// Create the list of attributes and values into an object.
                                    AtDatas.put("Name", myname);
                                    AtDatas.put("Type", mytype);
                                    if (myread) {
                                        AtDatas.put("Value", attvalue);
                                        System.out.println("    Name:" + myname + "  Type:" + mytype + "  Value:" + attvalue + "  Writeable:" + mywrite);
                                    } else {
                                        System.out.println("    Name:" + myname + "  Type:" + mytype + "  Readable:" + myread + "  Writeable:" + mywrite);
                                        AtDatas.put("Readable", myread);
                                    }
                                    AtDatas.put("Desc", mydesc);
                                    if (mywrite) {
                                        AtDatas.put("Writable", mywrite);
                                    }
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
                if (mBean.getoutJSON()) {
                    if (!mBean.getJSONFile().equals("")) {
                        try {                            
                            PrintWriter writer = new PrintWriter(mBean.getJSONFile(), "UTF-8");
                            if (mBean.getprettyprint()) {
                                writer.println(Jinfrascan.toString(4));
                            } else {
                                writer.println(Jinfrascan);
                            }
                            writer.close();                            
                        } catch (Exception ex) {
                            System.out.print("Error processing file!");
                            System.out.print(ex);
                        }
                    } else {
                        System.out.println("JSON Output:");                        
                        if (mBean.getprettyprint()) {
                            System.out.print(Jinfrascan.toString(4));
                        } else {
                            System.out.print(Jinfrascan);
                        }
                    }
 
                }
                if(mBean.getDoLogging())
                    {
                       String rmf = mBean.getLogDir() + "\\BeanBagger" + time + ".txt";
                       PrintWriter writer = new PrintWriter(rmf, "UTF-8");
                       if (mBean.getprettyprint()) {
                            writer.println(Jinfrascan.toString(4));
                            } 
                       else {
                            writer.println(Jinfrascan);
                            }  
                       writer.close();   
                    }
                
            } catch (Exception exception) {
                
              //Error handling to come.  
            }

        
            
            
            
            
        loopagain=false;
            
        if(mBean.getLoop())loopagain=true;
        
        if( mBean.getIterations()>0 &&  mBean.getIterationsCount() < mBean.getIterations()){//If we are counting and havent reached limit
            loopagain=true;
        }
        if( mBean.getIterations()>0 &&  mBean.getIterationsCount() >= mBean.getIterations()){//If we are counting and havent reached limit
            loopagain=false;
        }
          
        
        
        if(!mBean.getLoop())loopagain=false;   //If mBean says stop looping, stop it!
        
        
        if(loopagain){
            mBean.setIterationsCount(mBean.getIterationsCount() + 1);
            System.out.println("Sleeping " + mBean.getLoopDelaySeconds() + " seconds. This was run " + mBean.getIterationsCount());
            TimeUnit.SECONDS.sleep(mBean.getLoopDelaySeconds());

        }
        } while (loopagain);



System.out.println("");  
System.out.println("Stiv's Beanbagger Finished");  


    }
    
    public static void Usage()
    {
      System.out.println("java -jar Beanbagger [-p {process}] [-b {bean}] -q -m [-j {filename}] -ppj");
                  System.out.println("  -p {process}: Optional, VM Process Name or substring of process to try to connect to. Defaults to all");
                  System.out.println("  -b {bean}:  Optional , restrict data to just one bean. Default is all beans ");
                  System.out.println("  -j {optionalfilename}:  Optional: Output results to filename in JSON format, or to console if no file specified.");
                  System.out.println("  -x  :Requires exact match of VM Process Name");
                  System.out.println("  -q  :Filter. Suppresses output of unsupported types or operations.");
                  System.out.println("  -m  :Filter. Suppresses iteration of Sun beans (sun.*  and com.sun.*");
                  System.out.println("  -r  :Filter. Suppresses logging of unreadable attributes");
                  System.out.println("  -l  {seconds} :Loop continously.   After completion, the dump will rerun in x seconds, default is 30");
                  System.out.println("  -c  {iterations} :Count number of times to run. -c with no options sets to 5. Automatically sets -l");
                  
                  System.out.println("  -ppj :  Prettyprint JSON output, sets -j but not j- filename." );
                  System.out.println("  -log {logdir} :  Write each pass to a file in logdir." );

                  
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
