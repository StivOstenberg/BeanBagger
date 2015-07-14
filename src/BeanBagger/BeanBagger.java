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
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author s.ostenberg
 */
public class BeanBagger {
        
 

public static final String ANSI_RESET = "\u001B[0m";
public static final String ANSI_BLACK = "\u001B[30m";
public static final String ANSI_RED = "\u001B[31m";
public static final String ANSI_GREEN = "\u001B[32m";
public static final String ANSI_YELLOW = "\u001B[33m";
public static final String ANSI_BLUE = "\u001B[34m";
public static final String ANSI_PURPLE = "\u001B[35m";
public static final String ANSI_CYAN = "\u001B[36m";
public static final String ANSI_WHITE = "\u001B[37m";
 
	private static final String CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
        public static VirtualMachineDescriptor TARGETDESCRIPTOR ;
        static JMXConnector myJMXconnector = null;
        
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
                //Get the MBean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        //register the MBean
        BBConfig mBean = new BBConfig();
        ObjectName name = new ObjectName("com.stiv.jmx:type=BBConfig");
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
              case "-pp"  :
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
                  mBean.setconsoleout(false);
                  break;
              case "-u":
                  mBean.setsuppresscomplex(true);
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
                         if(mBean.getconsoleout())System.out.println("creating directory: " + mBean.getLogDir());
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
                        String rmf = mBean.getLogDir() + "/BeanBaggerreadme.txt";
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
                
                if(mBean.getconsoleout())System.out.println("Searching for matching VM instances");
                for (VirtualMachineDescriptor vmd : list) {
                    
                    String desc = vmd.toString();
                    try {
                        result.put(desc, VirtualMachine.attach(vmd));
                        String DN = vmd.displayName();
                        if (DN.contains(mBean.getTargetJVM()) || mBean.getTargetJVM().equalsIgnoreCase("*")) {
                            if (DN.equals("")) {
                                if(mBean.getconsoleout())System.out.println("  Skipping unnamed JVM");                                
                            } else if(!mBean.getTargetJVM().startsWith("BeanBagger")  && DN.contains("BeanBagger")){
                                if(mBean.getconsoleout())System.out.println("  Skipping BeanBagger JVM");  }
                            else {
                                if(mBean.getconsoleout())System.out.println("  Matching JVM instance found: " + DN);
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
                    if(mBean.getconsoleout())System.out.println("Number of beans found in " +ANSI_CYAN+ avmd.displayName() + ANSI_RESET+ ":  " + myJMXConnection.getMBeanCount());
                    
                    String getDefaultDomain = myJMXConnection.getDefaultDomain();
                    String[] getDomains = myJMXConnection.getDomains();
                    Set<ObjectInstance> beans = myJMXConnection.queryMBeans(null, null);
                    
                    org.json.JSONObject JVM = new org.json.JSONObject();
                    org.json.JSONArray JBeans = new org.json.JSONArray();
                    for (ObjectInstance instance : beans) {
                        String daclassname = instance.getClassName();
                        ObjectName oname = instance.getObjectName();
                        String BeanName = oname.getCanonicalName();
                        Hashtable<String,String> harry =  oname.getKeyPropertyList();
                        
                        
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
                                if(mBean.getconsoleout())System.out.println("     Processing me a bean: "  +ANSI_GREEN+BeanName+ANSI_RESET);
                            } catch (UnsupportedOperationException | RuntimeMBeanException | IllegalStateException ex) {
                                if(mBean.getconsoleout())System.out.println("     Error processing bean: " + BeanName);                                
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
                                
                                if (!mBean.getsuppresscomplex()) {
                                    dooutput = true;
                                } else {
                                    try {
                                        if (!attvalue.startsWith("*-") ) {
                                            dooutput = true;
                                        }                                        
                                    } catch (Exception ex)//For attributes with no values.
                                    {
                                        attvalue = "*-Unavailable-*";
                                        if (!mBean.getsuppresscomplex())dooutput = true;
                                    }
                                }
                                if (mBean.getignoreunreadable() && !myread) {
                                    dooutput = false;
                                }                                
                                
                                if (dooutput) {
                                    
                                    org.json.JSONObject AtDatas = new org.json.JSONObject();// Create the list of attributes and values into an object.
                                    AtDatas.put("Name", myname);
                                    AtDatas.put("Type", mytype);
                                    String attvaluecolor="";
                                    if(attvalue.startsWith("*-")){
                                        attvaluecolor=ANSI_RED+attvalue+ANSI_RESET;}
                                    else attvaluecolor=attvalue;
                                    if(attvalue.equals("")){
                                        attvaluecolor=ANSI_YELLOW+"\"\""+ANSI_RESET;}
                                    if (myread) {
                                        AtDatas.put("Value", attvalue);
                                        if(mBean.getconsoleout())System.out.println("       Name:" + myname + "  Type:" + mytype +  "  Writeable:" + mywrite + "  Readable:" + myread + "  Value:" + attvaluecolor );
                                    } else {
                                        if(mBean.getconsoleout())System.out.println("       Name:" + myname + "  Type:" + mytype + "  Writeable:" + mywrite+ "  Readable:" + myread );
                                        AtDatas.put("Readable", myread);
                                    }
                                    AtDatas.put("Desc", mydesc);
                                    if (mywrite) {
                                        AtDatas.put("Writable", mywrite);
                                    }
                                    BeanieButes.put(AtDatas);
                                    
                                }
                                
                            }//End processing Bean Attributes, add attributes to bean array.
                            Beanboy.put(BeanName, BeanieButes);//add attributes to the bean
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
                       String rmf = mBean.getLogDir() + "/BeanBagger" + time + ".txt";
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
        mBean.setIterationsCount(mBean.getIterationsCount() + 1);    
        if(mBean.getLoop())loopagain=true;
        
        if( mBean.getIterations()>0 &&  mBean.getIterationsCount() < mBean.getIterations()){//If we are counting and havent reached limit
            loopagain=true;
        }
        if( mBean.getIterations()>0 &&  mBean.getIterationsCount() >= mBean.getIterations()){//If we are counting and havent reached limit
            loopagain=false;
        }
          
        
        
        if(!mBean.getLoop())loopagain=false;   //If mBean says stop looping, stop it!
        
        
        if(loopagain){
            System.out.println("Sleeping " + mBean.getLoopDelaySeconds() + " seconds. This was run " + mBean.getIterationsCount());
            
            for(int x=0; x<mBean.getLoopDelaySeconds();x++)
            {
             TimeUnit.SECONDS.sleep(1);  
             if(!mBean.getLoop()){
             loopagain=false;
             break;}
            }
            
            
            
            
        }
        } while (loopagain);



System.out.println("");  
System.out.println("Stiv's Beanbagger Finished: " + mBean.getIterationsCount() + " iterations.");  


    }
    
    public static void Usage()
    {
      System.out.println("java -jar Beanbagger [-p {process}] [-b {bean}] -q -m [-j {filename}] -ppj");
                  System.out.println("  -p {process}: VM Process Name or substring of process to try to connect to. Defaults to all");
                  System.out.println("  -b {bean}:  Restrict data to just one bean. Default is all beans ");
                  System.out.println("  -j {optionalfilename}:  Output results to single file in JSON format, or to console if no file specified.");
                  System.out.println("                        File will be overwritten each pass.");
                  System.out.println("  -x  :Requires exact match of VM Process Name");
                  System.out.println("  -u  :Filter. Suppresses output of unsupported types or operations.");
                  System.out.println("  -m  :Filter. Suppresses iteration of Sun beans (sun.*  and com.sun.*");
                  System.out.println("  -r  :Filter. Suppresses logging of unreadable attributes");
                  System.out.println("  -l  {seconds} :Loop continously.   After completion, the dump will rerun in x seconds, default is 30");
                  System.out.println("  -c  {iterations} :Count number of times to run. -c with no options sets to 5. Automatically sets -l");
                  System.out.println("  -q  :Quiet.  Suppresses most console output.");
                  
                  System.out.println("  -pp  :  Prettyprint JSON output" );
                  System.out.println("  -log {logdir} :  Write each pass to a new file in logdir with epoch time in the filename." );

                  
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
