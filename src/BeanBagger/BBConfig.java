/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BeanBagger;

/**
 *
 * @author s.ostenberg
 */
public class BBConfig implements BBConfigMBean {

     private String leinsult="";//Just a way to make the program more insulting.
     private static boolean prettyprint=false;//Prettyprint the output to make it human readable.
     private static boolean ExactMatchRequired = false; // ALlows matching TargetVM process based on substring
     private static boolean DoLogging=false;
     public static String LogDir = "//tmp//";//Name of directory for logs to output to.
     public static boolean suppresscomplex=false;//Dont bother reporting on complex types we cannot dump as text
     public static boolean ignoreunreadable=false;//Set to pass over beans with unreadable attributes (set only)
     public static boolean supressSun=false;//Suppress the default Sun counters
     public static boolean consoleout=true;//Do we spit out data to console?
     public static String JSONFile = "";//The file we will output to.
     public static boolean outJSON=false;//Turn on JSON output
     public static  String  TargetJVM = "";//The target JVM
     public static String TARGETBEAN = "";//The target bean in the JVM
     public static int LoopDelaySeconds=30;//How long do we wait between runs
     public static boolean Loop=false;//SHould we rerun the dump?
     public static int Iterations=0;//How many times to run?
     public static int IterationsCount=0;//How many times to run?

    public BBConfig(){ }
    @Override
    public boolean getDoLogging(){return DoLogging;}
    
    @Override
    public int getLoopDelaySeconds(){return LoopDelaySeconds;}
    @Override
    public void setLoopDelaySeconds(int in){LoopDelaySeconds=in;} 
    
    @Override
    public String getJSONFile(){return JSONFile;}
    @Override
    public void setJSONFile(String in){JSONFile=in;}    
    
     @Override
    public String getTARGETBEAN(){return TARGETBEAN;}
    @Override
    public void setTARGETBEAN(String in){TARGETBEAN=in;}    
    
    
    @Override
    public String getTargetJVM(){return TargetJVM;}
    @Override
    public void setTargetJVM(String in){TargetJVM=in;}
    @Override
    public void setsupressSun(boolean in){supressSun=in;}
    @Override
    public boolean getsupressSun(){return supressSun;}  
    @Override
    public void setoutJSON(boolean in){outJSON=in;}
    @Override
    public boolean getoutJSON(){return outJSON;}
    @Override
    public void setignoreunreadable(boolean in){ignoreunreadable=in;}
    @Override
    public boolean getignoreunreadable(){return ignoreunreadable;}   
    @Override
   public void setprettyprint(boolean in){prettyprint=in;}
   @Override
   public boolean getprettyprint(){return prettyprint;}   
     @Override
     public void setsuppresscomplex(boolean in){suppresscomplex=in;}
     @Override
    public boolean getsuppresscomplex(){return suppresscomplex;}
      @Override
     public void setLoop(boolean in){Loop=in;}
     @Override
     public void StopLoop(){Loop=false;}
     @Override
    public boolean getLoop(){return Loop;}
     @Override
    public void setconsoleout(boolean in){consoleout=in;}
    @Override
    public boolean getconsoleout(){return consoleout;}   

    
    
    
    
    @Override
    public String getLogDir(){return LogDir;}
    @Override
    public void setLogDir(String in){
        LogDir=in;
        DoLogging=true;
    } ;
    
    @Override
    public void setExactMatchRequired(boolean in)
    {
    ExactMatchRequired = in;  
    }
    @Override
    public boolean getExactMatchRequired() {return ExactMatchRequired ;}
    
    
    @Override
    public void setInsult(String aninsult)
    {
       this.leinsult=" " +aninsult +" "; 
    }
   @Override
   public String getInsult() {
       return leinsult;
   }
    

   
    @Override
    public void setIterations(int in) {
        this.Iterations=in;
    }
    @Override
    public int getIterations() {
        return this.Iterations;
    }
 
    @Override
    public void setIterationsCount(int in) {
        this.IterationsCount=in;
    }
    @Override
    public int getIterationsCount() {
        return this.IterationsCount;
    }

}
