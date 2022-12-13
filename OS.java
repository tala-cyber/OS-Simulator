import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatPrecisionException;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//To quickly view *MAJOR* changes: ctrl+F "Project 3"

public class OS {
    static int ID=0; 
    static ArrayList<Process> All_Processes = new ArrayList<Process>();
    static ArrayList<Process> All_Ready = new ArrayList<Process>();
    static ArrayList<Process> All_IO = new ArrayList<Process>();
    //Project 3 : Memory is static OS variable -> shared!
    static ArrayList<PCB> Memory=new ArrayList<PCB>();
    //two CPUs!
    static CPU CPU1 = new CPU();
    static CPU CPU2 = new CPU();
    static ArrayList<Process> CPU1_Processes = new ArrayList<Process>();
    static ArrayList<Process> CPU2_Processes = new ArrayList<Process>();
    static ArrayList<Process> CPU1_Ready = new ArrayList<Process>();
    static ArrayList<Process> CPU2_Ready = new ArrayList<Process>();
    static ArrayList<Process> CPU1_IO = new ArrayList<Process>();
    static ArrayList<Process> CPU2_IO = new ArrayList<Process>();
    //Done
    static int stats; static int lock1;static int lock2; static int lock3; static int lock4; static int[] locks1={lock1, lock2, lock3, lock4};
    static String pf1; static String pf2; static String pf3;
    static int lock5;static int lock6; static int lock7; static int lock8; static int[] locks2={lock5, lock6, lock7, lock8};
    
    static PageTable pt = new PageTable();
    static NoncontiguousMemory mem= new NoncontiguousMemory();

    //Project 3
    static class CPU{
        static int counter=1;
        public int CPU_number;
        public ArrayList<Process> Processes;public ArrayList<Process> Ready;public ArrayList<Process> IO; int[] locks;

        public CPU(){
            CPU_number=counter++;
        }

        public void assign_processes(){
            if(CPU_number==1){
                Processes=CPU1_Processes;
                Ready=CPU1_Ready;
                IO=CPU1_IO;
                locks=locks1;
            }else{
                Processes=CPU2_Processes;
                Ready=CPU2_Ready;
                IO=CPU2_IO;
                locks=locks2;
            }
        }

        public void RR_Multithreaded_Scheduler() throws InterruptedException, SAXException, IOException, ParserConfigurationException, InterruptedException{
            Process p; Operation op;
            Threadd thread1 = new Threadd("RR",CPU_number);
            Threadd thread2 = new Threadd("RR",CPU_number);
            Threadd thread3 = new Threadd("RR",CPU_number);
            Threadd thread4 = new Threadd("RR",CPU_number);
            Threadd[] active_threads={thread1, thread2, thread3, thread4};
            while(!Processes.isEmpty()){
                if(!Ready.isEmpty()){
                    //We want to keep all threads BUSY.
                        for(int i=0; i<4; i++){
                            //if it is NOT busy, assign a process to it
                            if(active_threads[i].busy==0 && !Ready.isEmpty()){
                                active_threads[i]=new Threadd("RR", CPU_number);
                                active_threads[i].busy=1;
                                p=Ready.get(0); Ready.remove(0);
                                active_threads[i].assign_process(p);
                                //store thread nb in PCB
                                PCB block = Memory.get(searchMemory(p));
                                block.assign_thread(i);
                                //run threads in parallel!
                                active_threads[i].start();
                            }
                        }
                        thread1.join(); thread2.join(); thread3.join(); thread4.join();
                        printStatsRR();
                }else if(IO.isEmpty()){
                    break;
                }else{ 
                    updateIO();
                    printStatsRR();
                }
            }
        }    
        public void FCFS_Multithreaded_Scheduler() throws InterruptedException, SAXException, IOException, ParserConfigurationException, InterruptedException{
            counter=0;
            Threadd thread1 = new Threadd("FCFS",CPU_number);
            Threadd thread2 = new Threadd("FCFS",CPU_number);
            Threadd thread3 = new Threadd("FCFS",CPU_number);
            Threadd thread4 = new Threadd("FCFS",CPU_number);
            Threadd[] active_threads={thread1, thread2, thread3, thread4};
            while(!Processes.isEmpty()){
                    for(int i=0; i<4; i++){
                        if(active_threads[i].busy==0 && !Processes.isEmpty()){
                            active_threads[i]=new Threadd("FCFS", CPU_number);
                            active_threads[i].busy=1;
                            Process p = Processes.get(0); Processes.remove(p);
                            active_threads[i].assign_process(p);
                            active_threads[i].assign_index(All_Processes.indexOf(p));
                            //store thread nb in PCB
                            PCB block = Memory.get(searchMemory(p));
                            block.assign_thread(i+1);
                            //running in parallel:
                                active_threads[i].start();
                                Processes.remove(p);
 
                            
                        }
                    }
            }
        }
        //Done
        public synchronized void Run_Process_On_CPU(Process p,int quantum, int thread_number) throws InterruptedException{
            Acquire(thread_number);
            PCB block = Memory.get(searchMemory(p));
            Operation op=block.ops.get(block.curr_op);
            // each cycle is 10 milliseconds and the time quantum is 2 cycles
            if(quantum==0 || op.cycles<quantum){
                for(int i=0; i<op.cycles; i++){
                    //ob.wait(10);
                }
                op.cycles=0;
            }else{
                for(int i=0; i<quantum; i++){
                    //ob.wait(10);
                }
                op.cycles=op.cycles-quantum;
            }
            Release(thread_number);
        }
        public void Acquire(int n){
            locks[n]++;
        }
        public void Release(int n){
            locks[n]--;
        }
    
        public void bringToMemory(int PID, int opID){
            int frame_TBReplaced= VictimSelection(mem);
            pt.mapping[PID][opID][1]=frame_TBReplaced;
        }
        public int VictimSelection(NoncontiguousMemory mem){
            int date=999999; int index=0;
            for(int i=0; i<64; i++){
                if(mem.Frames.get(i).date_used<date){
                    date=mem.Frames.get(i).date_used;
                    index=i;
                }
            }
            return index;
        }

        public void updateIO() throws InterruptedException, SAXException, IOException, ParserConfigurationException, InterruptedException{
            for(int i=0; i<IO.size(); i++){
                Process p =IO.get(i);
                PCB block = Memory.get(searchMemory(p));
                Operation op=block.ops.get(block.curr_op);
                op.cycles=op.cycles-2;
            }
            ArrayList<Process> copyIO = new ArrayList<>(IO);
            int size=copyIO.size();
            for(int i=0; i<size; i++){
                Process p =copyIO.get(i);
                ClassifyProcess(p);
            }
        }

        public void RoundRobin(Process p, int thread_number) throws InterruptedException, SAXException, IOException, ParserConfigurationException{
            Operation op;
            PCB block = Memory.get(searchMemory(p));
            op=block.ops.get(block.curr_op);
        
            if(op.virtual_address>512){
                bringToMemory(p.PID, block.curr_op);
            }
            while(locks[thread_number]==1);
            Run_Process_On_CPU(p, 2, thread_number);
            ClassifyProcess(p);
            updateIO();
        }
        public void ClassifyProcess(Process p) throws InterruptedException, SAXException, IOException, ParserConfigurationException{
            PCB block = Memory.get(searchMemory(p));
            Operation op=block.ops.get(block.curr_op);
            IO.remove(p); 
            //Ready.remove(p);
            if(op.cycles>0){
                if(op.type.equals("calculate")){
                    Ready.add(p);
                }else if(op.type.equals("io")){
                    IO.add(p);
                }
            }else{
                if(block.curr_op==4){
                    Processes.remove(p); 
                }else{
                    block.curr_op++;
                    op=block.ops.get(block.curr_op);
                    if(op.type.equals("calculate")){
                        Ready.add(p);
                    }else if(op.type.equals("io")){
                        IO.add(p);
                    }else{
                        Fork(p);
                        ClassifyProcess(p);
                    }
                }
            }
        }
        public void Organize(){    
            for(int i=0; i<Processes.size(); i++){
                Process p = Processes.get(i);
                Operation op=p.ops.get(0);
                if(op.type.equals("calculate")){
                    
                    Ready.add(p);
                }else if(op.type.equals("io")){
                    IO.add(p);
                }
            }
        }


        public void FCFS(Process p, int i, int thread_number) throws InterruptedException, SAXException, IOException, ParserConfigurationException{
                for(int j=0; j<5; j++){
                    Operation op = p.ops.get(j);
                    if(op.type.equals("calculate")){
                        while(locks[thread_number]==1);
                        Run_Process_On_CPU(p, 0, thread_number);
                    }else if(op.type.equals("io")){
                        IO(op, 0);
                    }else{
                        Fork(p);
                    }
                }
                printStatsFCFS(i);    
        }
        public void OrganizeFCFS(){
        ContiguousMemory mem=new ContiguousMemory();
        for(int i=0; i<Processes.size(); i++){
            Process p = Processes.get(i);
            int success =  mem.addprocess(p.size);
            if(success==1){
                Ready.add(p);
                Processes.remove(i);
            }else{
                //nothing
            }
        }
    }

        public void printStatsRR(){
                if(stats==0){
                    //do nothing
                }else{
                    System.out.println("CPU"+CPU_number+" Ready: ");
                    for(int i=0; i<Ready.size(); i++){
                        System.out.print(Ready.get(i).PID);
                        System.out.print("("+Memory.get(searchMemory(Ready.get(i))).curr_op+")----");
                    }
                    System.out.println();
                    System.out.println("CPU"+CPU_number+" Waiting:   ");
                    for(int i=0; i<IO.size(); i++){
                        System.out.print(IO.get(i).PID);
                        System.out.print("("+Memory.get(searchMemory(IO.get(i))).curr_op+")----");
                    }
                    System.out.println();
                    if(Ready.size()>=1){
                        System.out.println("CPU"+CPU_number+" Running:  "+Ready.get(0).PID+"----");
                    }else{
                        System.out.println("CPU"+CPU_number+" Running:  ");
                    }
                    System.out.println(); System.out.println();

                }
        }
        public void printStatsFCFS(int i){
            if(stats==0){
                //do nothing
            }else{
                System.out.println("Just fnished executing: Process "+i+" ----CPU"+CPU_number);
            }
        }

        public int customRR(int n1, int n2, int n3) throws InterruptedException, SAXException, IOException,ParserConfigurationException{
            createProcesses("pf1.xml", n1);
            createProcesses("pf2.xml", n2);
            createProcesses("pf3.xml", n3);
            splitProcesses();
            System.out.println("Executing RR...");
            Organize();
            RR_Multithreaded_Scheduler();
            return 1;  
        }
        public int customFCFS(int n1, int n2, int n3) throws SAXException, IOException,ParserConfigurationException,InterruptedException{
        createProcesses("pf1.xml", n1);
        createProcesses("pf2.xml", n2);
        createProcesses("pf3.xml", n3);
        splitProcesses();
        System.out.println("Executing FCFS...");
        OrganizeFCFS();
        FCFS_Multithreaded_Scheduler();
        return 1;
    }
}

    static class ContiguousMemory{
        int memory[];
        public ContiguousMemory(){memory= new int[512];}

        public int[] BestFit(int size){
            int fit=0; int bestfit=513; int index=-1;
            for(int i=0; i<512; i++){
                if(memory[i]==1){
                    if(fit<bestfit && fit>size){
                        bestfit=fit; fit=0;
                    }
                }else{
                    if(fit==0){
                        index=i;
                    }
                    fit++;
                }
            }
            int[] ans = new int[2];
            ans[0]=bestfit; ans[1]=index;
            return ans;
        }

        public int addprocess(int size){
            int[] ans = BestFit(size);
            int bestfit = ans[0];
            int index = ans[1];
            if(bestfit<513){
                for(int i=index; i<index+size; i++){
                    memory[i]=1;
                }
                return 1;
            }
            return -1;
        }
    }

    static class NoncontiguousMemory{
        public static ArrayList<Frame> Frames = new ArrayList<Frame>(64);
        public NoncontiguousMemory(){}

        public int addprocess(int size){
            int frames = size/8 +1; int free=0;
            for(int i=0; i<Frames.size(); i++){
                if(Frames.get(i).occ==false){
                    free++;
                }
            }
            if(free>=frames){
                for(int i =0; i<Frames.size(); i++){
                    if(Frames.get(i).occ==false && frames>0){
                        Frames.get(i).occ=true;
                        frames--;
                    }
                }
                return 1;
            }
            return -1;

        }


    }

    static class VirtualMemory{
        NoncontiguousMemory mem;
        public VirtualMemory(){
            mem =  new NoncontiguousMemory();
        }
    }

    static class Frame{
        int size; Boolean occ; int date_used;
        public Frame(){size=8;occ=false; date_used=0;}
    }

    // 1-512 are in MM, 513-1024 are in VM
    static class PageTable{
        int[][][] mapping; static int global=0;
        public PageTable(){mapping= new int[All_Processes.size()][5][2];}

        public void fill(){
            for(int i=0; i<All_Processes.size(); i++){
                Process p = All_Processes.get(i);
                for(int j=0; j<5; j++){
                    Operation op = p.ops.get(j);
                    mapping[i][j][0]=op.address;
                    mapping[i][j][1]=op.virtual_address;
                }
            }
        }
    }


    static class Operation{
        public String type; public int cycles; 
     
        public int size; public int address; public int virtual_address;
     
        public Operation(String t, int c, int s, int d){type=t; cycles=c; size=s; address=d;}
    }

    static class Process{
        public ArrayList<Operation> ops; public String state; public int template; public int PID; public int cpu_affinity;
      
        public int size;
     
        public Process(ArrayList<Operation> operations, int t, int s, int cpu){ops=operations; state="READY"; template =t;PID=ID++;size=s;cpu_affinity=cpu;}
    }

    static class PCB{
        public int PID; public ArrayList<Operation> ops; public String state; public static int curr_op;public int thread;
        public PCB(Process p){
            PID=p.PID; ops=p.ops; state=p.state; curr_op=0;
        }
        //Project 3
        public void assign_thread(int thread_number){
            thread=thread_number;
        }
        //Done
    }

    //Project 3
    static class Threadd extends Thread{
        static int counter=0;
        public int thread_ID;
        public int busy;
        public Process p_running;
        public String type;
        public int CPU_nb;
        public int proc_index;

        public Threadd(String algo, int num){
            thread_ID=counter++;
            busy=0;
            type=algo;
            CPU_nb=num;
        }
        public void assign_process(Process p){
            p_running=p;
        }
        public void assign_index(int i){
            proc_index=i;
        }

        @Override
        public void run(){
            try{
                if(type.equals("RR")){
                    if(CPU_nb==1){
                        CPU1.RoundRobin(p_running, thread_ID%4);
                    }else{
                        CPU2.RoundRobin(p_running, thread_ID%4);
                    }
                }else if(type.equals("FCFS")){
                    if(CPU_nb==1){
                        CPU1.FCFS(p_running, proc_index, thread_ID%4);
                    }else{
                        CPU2.FCFS(p_running, proc_index, thread_ID%4);
                    }
                }else if(type.equals("customRR")){
                    if(CPU_nb==1){
                        CPU1.customRR(Integer.parseInt(pf1), Integer.parseInt(pf2), 0);
                    }else{
                        CPU2.customRR(0, 0, Integer.parseInt(pf3));
                    }
                }else if(type.equals("customFCFS")){
                    if(CPU_nb==1){
                        CPU1.customFCFS(Integer.parseInt(pf1), Integer.parseInt(pf2), 0);
                    }else{
                        CPU2.customFCFS(0, 0, Integer.parseInt(pf3));
                    }
                }   
            }catch(InterruptedException e){

            }catch(SAXException e){

            }catch(IOException e){

            }catch(ParserConfigurationException e){

            }

            busy=0; //idle for now..
        }

    }
    // Done

    public static int searchMemory(Process p){
        for(int i=0; i<Memory.size(); i++){
            if(Memory.get(i).PID==p.PID){
                return i;
            }
        }
        Memory.add(new PCB(p));
        return Memory.size()-1;
    }

    public static Process createProcess(String template) throws SAXException, IOException, ParserConfigurationException{
        // Get file and parse
        File xml = new File(template);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = (Document) builder.parse(xml);
        doc.getDocumentElement().normalize();

        //Prepare list to iterate
        NodeList op_list = doc.getElementsByTagName("operation");

        //Prepare list to return
        ArrayList<Operation> operations = new ArrayList<Operation>();

        //Iterate over the list of operations
        int total_size=0; int cpu_affinity=1;
        for(int i=0; i<op_list.getLength(); i++){
            Node op  = op_list.item(i);
            if(op.getNodeType() == Node.ELEMENT_NODE){
                Element operation = (Element) op;

                    String operationType = operation.getElementsByTagName("type").item(0).getTextContent();
                    String min = operation.getElementsByTagName("min").item(0).getTextContent();
                    String max = operation.getElementsByTagName("max").item(0).getTextContent();
                    String smin = operation.getElementsByTagName("smin").item(0).getTextContent();
                    String smax = operation.getElementsByTagName("smax").item(0).getTextContent();
    
                    int int_min = Integer.valueOf(min);
                    int int_max = Integer.valueOf(max);
                 
                    int int_smin = Integer.valueOf(smin);
                    int int_smax = Integer.valueOf(smax);
    
                    int address = Integer.valueOf(999999-100000)+100000;
                  
                    Random rand = new Random();
                    int cycles = rand.nextInt(int_max-int_min);
                 
                    int size = rand.nextInt(int_smax-int_smin);
                    total_size=total_size+size;
                 
                    Operation opp = new Operation(operationType, cycles+int_min, size+int_smin, address);
                    operations.add(opp);

                


            }
        }
        if(template=="pf1.xml"){
            return new Process(operations, 1, total_size, 1);
        }else if(template=="pf2.xml"){
            return new Process(operations, 2, total_size, 1);
        }
        return new Process(operations, 3, total_size, 2);
    }
    public static void createProcesses(String template, int n) throws SAXException, IOException, ParserConfigurationException{
        for(int i=0; i<n; i++){
            All_Processes.add(createProcess(template));
        }  
    }
    public static void splitProcesses(){
        Process p;
        for(int i=0; i<All_Processes.size(); i++){
            p=All_Processes.get(i);
            if(p.cpu_affinity==1){
                CPU1_Processes.add(p);
            }else{
                CPU2_Processes.add(p);
            }
        }
        CPU1.assign_processes(); CPU2.assign_processes();
    }



    public static Process Fork(Process p) throws SAXException, IOException, ParserConfigurationException{
        PCB block = Memory.get(searchMemory(p));
        Operation op=block.ops.get(block.curr_op);
        op.cycles=0;
        if(p.template==1){
            return createProcess("pf1.xml");
        }else if(p.template==2){
            return createProcess("pf2.xml");
        }
        return createProcess("pf3.xml");
    }
    public static void IO(Operation op, int quantum) throws InterruptedException{
        Object ob = new Object();

        // each cycle is 10 milliseconds
        if(quantum==0 || op.cycles<quantum){
            for(int i=0; i<op.cycles; i++){
                //ob.wait(10);
            }
            op.cycles=0;
        }else{
            for(int i=0; i<quantum; i++){
                //ob.wait(10);
            }
            op.cycles=op.cycles-quantum;
        }
    }


    public static void main(String args[]) throws SAXException, IOException, ParserConfigurationException, InterruptedException{
        System.out.println("REMINDER: Do not forget that you have to type x to exist :) !!");
        System.out.println("Type the number of processes you wish to generate.");
        System.out.println("Program File 1: ");
        Scanner myObj = new Scanner(System.in);
        pf1 = myObj.nextLine();
        System.out.println("Program File 2: ");
        pf2 = myObj.nextLine();
        System.out.println("Program File 3: ");
        pf3 = myObj.nextLine();
        System.out.println("First CPU? Type RR for Round Robin or FCFS for First Come First Serve:");
        String algo1 = myObj.nextLine();
        System.out.println("Second CPU? Type RR for Round Robin or FCFS for First Come First Serve:");
        String algo2 = myObj.nextLine();
        System.out.println("Stats? Type 1 for yes or 0 for no.");
        stats = Integer.parseInt(myObj.nextLine());
        OS OS_Instance =  new OS();
        if(algo1.equals("RR")){
            Threadd thread_custom_RR1= new Threadd("customRR",1);
            thread_custom_RR1.start();
        }else if(algo1.equals("FCFS")){
            Threadd thread_custom_FCFS1 = new Threadd("customFCFS",1);
            thread_custom_FCFS1.start();
        }else{
            System.out.println("Try Again");
        }
        if(algo2.equals("RR")){
            Threadd thread_custom_RR2= new Threadd("customRR",2);
            thread_custom_RR2.start();
        }else if(algo2.equals("FCFS")){
            Threadd thread_custom_FCFS2 = new Threadd("customFCFS",2);
            thread_custom_FCFS2.start();
        }else{
            System.out.println("Try Again");
        }

        System.out.println("Type x to exit");
        String exit = myObj.nextLine();
        if(exit.equals("x")){

        }
                
        

    }
}

