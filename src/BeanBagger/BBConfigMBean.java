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
public interface BBConfigMBean {
    

        //public static boolean ExactMatchRequired = false; // ALlows matching TargetVM process based on substring
            public void setExactMatchRequired(boolean ExactMatchRequired);
            public boolean getExactMatchRequired();

            public String getLogDir();
            public boolean getDoLogging();
            public void setLogDir(String directory);

            public void setsuppresscomplex(boolean suppresscomplex);
            public boolean getsuppresscomplex();

            public void setignoreunreadable(boolean ignoreunreadable);
            public boolean getignoreunreadable();

            public void setsupressSun(boolean supressSun);
            public boolean getsupressSun();

            public String getJSONFile();
            public void setJSONFile(String JSONFile);

            public void setoutJSON(boolean outJSON);
            public boolean getoutJSON();

            public void setprettyprint(boolean prettyprint);
            public boolean getprettyprint();
            
            public void setconsoleout(boolean consoleout);
            public boolean getconsoleout();
            
            public void setTargetJVM(String TargetJVM);
            public String getTargetJVM();
    
            public void setTARGETBEAN(String TARGETBEAN);
            public String getTARGETBEAN();
            
            public void setLoop(boolean loop);
            public boolean getLoop();
            public void StopLoop();
            
            public void setLoopDelaySeconds(int delay);
            public int getLoopDelaySeconds();
            
    public void setIterations(int in);
    public int getIterations();
    
   public void setIterationsCount(int in);
    public int getIterationsCount();
    
    public void setInsult(String aninsult);
    public String getInsult();

    


}
