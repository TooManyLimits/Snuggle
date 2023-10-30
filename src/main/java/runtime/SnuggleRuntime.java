package runtime;

import exceptions.runtime.SnuggleException;

/**
 * When we compile to JVM bytecode, what we ultimately want to produce is
 * a class that implements this. Calling its run() method will run the
 * compiled code.
 */
public interface SnuggleRuntime {


    //Run the code
    void run() throws SnuggleException;

//    //Instruction cap
//    void setInstructionCap(long cap);
//    long getInstructionCap();
//
//    //Used instructions
//    void setUsedInstructions(long instructions);
//    long getUsedInstructions();
//
//    //Memory
//    void recountMemory();
//    void setMemoryCap(long cap);
//    long getMemoryUsed();
}